/**
 * This file is part of glisten.
 *
 * glisten is free software: you can redistribute it and/or modify
 * it under the terms of the Affero GNU Lesser General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * glisten is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Affero GNU Lesser General Public License version 3 for more details.
 *
 * You should have received a copy of the Affero GNU Lesser General Public License
 * along with glisten.  If not, see <http://www.gnu.org/licenses/>.
 * Created by Lixi Alié Conrads, 7/30/21
 */
package org.dice_group.glisten.hobbit

import com.google.gson.JsonParser
import net.lingala.zip4j.ZipFile
import org.apache.commons.math3.stat.StatUtils
import org.apache.jena.rdf.model.*
import org.apache.jena.vocabulary.RDF
import org.dice_group.glisten.core.config.CONSTANTS
import org.dice_group.glisten.core.config.Configuration
import org.dice_group.glisten.core.config.ConfigurationFactory
import org.dice_group.glisten.core.copaal.Copaal
import org.dice_group.glisten.core.copaal.FactGenerator
import org.dice_group.glisten.core.evaluation.ROCCurve
import org.dice_group.glisten.core.utils.DownloadFiles
import org.dice_group.glisten.core.utils.RDFUtils
import org.hobbit.core.components.AbstractEvaluationModule
import org.hobbit.core.rabbit.RabbitMQUtils
import org.hobbit.vocab.HOBBIT
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL


class Evaluator : AbstractEvaluationModule() {

    private var conf: Configuration? = null

    var linkedPath = "/links"
    //set max Recommendations to TOP 10
    var maxRecommendations=10
    private val aucList = mutableListOf<Double>()
    private val times = mutableListOf<Double>()
    var seed = 1234L
    var numberOfFalseStatements = 10
    var numberOfTrueStatements = 10
    var minPropOcc = 10
    var maxPropertyLimit = 10

    var debug = false

    override fun init() {
        if(!debug) {
            super.init()
        }
        var benchmarkName = ""
        if(System.getenv().containsKey(CONSTANTS.BENCHMARK_NAME)){
            benchmarkName = System.getenv()[CONSTANTS.BENCHMARK_NAME]!!
        }
        if(System.getenv().containsKey(CONSTANTS.NUMBER_OF_TRUE_STATEMENTS)){
            seed = System.getenv()[CONSTANTS.NUMBER_OF_TRUE_STATEMENTS]!!.toLong()
        }
        if(System.getenv().containsKey(CONSTANTS.NUMBER_OF_FALSE_STATEMENTS)){
            seed = System.getenv()[CONSTANTS.NUMBER_OF_FALSE_STATEMENTS]!!.toLong()
        }
        if(System.getenv().containsKey(CONSTANTS.MIN_PROP_OCC)){
            seed = System.getenv()[CONSTANTS.MIN_PROP_OCC]!!.toLong()
        }
        if(System.getenv().containsKey(CONSTANTS.MAX_PROPERTY_LIMIT)){
            seed = System.getenv()[CONSTANTS.MAX_PROPERTY_LIMIT]!!.toLong()
        }

        if(System.getenv().containsKey(CONSTANTS.SEED)){
            seed = System.getenv()[CONSTANTS.SEED]!!.toLong()
        }
        conf = ConfigurationFactory.findCorrectConfiguration(benchmarkName)
        //DOWNLOAD LINKS and unzip to /links/
        val zipFile = DownloadFiles.download(conf?.linksUrlZip?:"", "/")
        //val zipFile = "links.zip"
        val linksZip = ZipFile(zipFile)
        linksZip.extractAll("/links/")
        //val linksZip = ZipFile(zipFile)
        //linksZip.ex
    }

    override fun evaluateResponse(
        expectedData: ByteArray?,
        receivedData: ByteArray?,
        taskSentTimestamp: Long,
        responseReceivedTimestamp: Long
    ) {
        val recommendations = mutableListOf<Pair<String, Double>>()
        // we get a recommendation array and basically no expected data ? < is that right?
        val recommendationJSON = JsonParser.parseString(RabbitMQUtils.readString(receivedData))
        recommendationJSON.asJsonArray.forEach{
            recommendations.add(Pair(it.asJsonObject.get("dataset").asString, it.asJsonObject.get("score").asDouble))
        }
        val source = RabbitMQUtils.readString(expectedData)
        val auc = getAUC(source, recommendations)
        aucList.add(auc)
        times.add(1.0*(responseReceivedTimestamp-taskSentTimestamp))

    }



    fun getAUC(source: String, recommendations: MutableList<Pair<String, Double>>) : Double {
        //create source model
        val sourceModel = ModelFactory.createDefaultModel()
        //download file
        sourceModel.read(URL(source).openStream(), null, "NT")
        //create facts for source model
        // BE AWARE this changes sourceModel
        val facts = FactGenerator.createFacts(seed,
            numberOfTrueStatements,
            numberOfFalseStatements,
            conf!!.createTrueStmtDrawer(seed, sourceModel, minPropOcc, maxPropertyLimit),
            conf!!.createFalseStmtDrawer(seed, sourceModel, minPropOcc, maxPropertyLimit)
        ).map { (stmt, d) -> stmt }
        //create baseline (w/o recommendations)
        val baseline = getScore(facts, source, sourceModel, "")
        val normalizer = 1.0/(1-baseline)
        val roc = getROC(source, sourceModel, facts, recommendations)
        return normalizer*(roc.calculateAUC()-baseline)
    }


    fun getROC(source: String, sourceModel: Model, facts: List<Statement>, recommendations: MutableList<Pair<String, Double>>): ROCCurve{
        recommendations.sortByDescending { it.second }
        //steps doesn't ,matter in our case, we don't know the first either way
        val roc = ROCCurve(0, recommendations.size)

        var counter=1.0
        //val currentDatasets = mutableListOf<String>()
        //for each recommendation create score and add to roc
        //recommendations.forEach { (dataset, value) -> {
        for((dataset, value) in recommendations){
            if (counter>maxRecommendations && maxRecommendations!=-1){
                //if maxRecom not -1 (disabling this check) and the the TOP N datasets were checked, break and return
                return roc
            }

            //set correct datasets to add
            //currentDatasets.add(dataset)
            //get next point
            val score = getScore(facts, source, sourceModel, dataset)
            println("Score: %f".format(score))
            println("Current dataset to add %s".format(dataset))
            //FIXME make use of better functions addUP, ...
            roc.addPoint(counter/recommendations.size, score)
            counter += 1.0
        }

        return roc
    }

    fun getScore(facts: List<Statement>, sourceName: String, source: Model, currentDataset: String) : Double{
        //create Model out of source and current datasets -> combinedDataset
        //val combinedDataset = ModelFactory.createDefaultModel()
        //combinedDataset.add(source)
        //currentDatasets.forEach{
            //var currentModel = ModelFactory.createDefaultModel()
            //FIXME
        if(currentDataset.isNotEmpty()) {
            //(Download is in init)
            val linkdataset = "file://$linkedPath/" + sourceName.substringAfterLast("/")
                .removeSuffix(".nt") + "_" + currentDataset.substringAfterLast("/")
            val currentModel = RDFUtils.streamNoLiterals(linkdataset) //.read(FileInputStream(linkdataset), null, "NT")
            source.add(currentModel)
            println("Current Model size: %d".format(source.size()))
        }        //}
        //Let Copaal run
        val copaal = Copaal()
        return copaal.factChecker(source, facts)
    }

    override fun summarizeEvaluation(): Model {
        LOGGER.info("Summarizing Results")
        // calculate spearman and so on.
        val avgAUC = StatUtils.mean(aucList.toDoubleArray())
        //calculate some time statistics
        val timeAsDouble = this.times.toDoubleArray()
        val minTime = StatUtils.min(timeAsDouble)
        val maxTime = StatUtils.max(timeAsDouble)
        val avgTime = StatUtils.mean(timeAsDouble)
        val sumTime = StatUtils.sum(timeAsDouble)
        //val firstPercentileTime = StatUtils.percentile(timeAsDouble, 25.0)
        //val secondPercentileTime = StatUtils.percentile(timeAsDouble, 50.0)
        //val thirdPercentileTime = StatUtils.percentile(timeAsDouble, 75.0)

        LOGGER.info("Creating model with [average AUC: {}, mean Time MS: {}, min Time MS: {}, max Time MS: {}]",
            avgAUC, avgTime, minTime, maxTime)
        //create model and add values.
        val model = ModelFactory.createDefaultModel()

        val experiment = model.getResource(experimentUri)

        model.add(experiment, RDF.type, HOBBIT.Experiment)
        model.addLiteral(experiment, Evaluator.avgAUC, avgAUC)

        model.addLiteral(experiment, Evaluator.avgTime, avgTime)
        model.addLiteral(experiment, Evaluator.minTime, minTime)
        model.addLiteral(experiment, Evaluator.maxTime, maxTime)
        model.addLiteral(experiment, Evaluator.sumTime, sumTime)

        return model

    }

    companion object {

        val LOGGER: Logger = LoggerFactory.getLogger(Evaluator::class.java)
        val avgAUC: Property? = ResourceFactory.createProperty(CONSTANTS.GLISTEN_PREFIX+"avgAUC")
        val sumTime: Property?= ResourceFactory.createProperty(CONSTANTS.GLISTEN_PREFIX+"SumTime")
        val maxTime: Property?= ResourceFactory.createProperty(CONSTANTS.GLISTEN_PREFIX+"maxTime")
        val minTime: Property?= ResourceFactory.createProperty(CONSTANTS.GLISTEN_PREFIX+"minTime")
        val avgTime: Property?= ResourceFactory.createProperty(CONSTANTS.GLISTEN_PREFIX+"avgTime")

    }
}