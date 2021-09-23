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
import com.jamonapi.utils.FileUtils
import org.apache.commons.math3.stat.StatUtils
import org.apache.jena.rdf.model.*
import org.apache.jena.vocabulary.RDF
import org.dice_group.glisten.core.config.CONSTANTS
import org.dice_group.glisten.core.config.Configuration
import org.dice_group.glisten.core.config.ConfigurationFactory
import org.dice_group.glisten.core.config.EvaluationParameters
import org.dice_group.glisten.core.evaluation.CoreEvaluator
import org.dice_group.glisten.core.scorer.Copaal
import org.dice_group.glisten.core.scorer.SampleCopaal
import org.dice_group.glisten.core.scorer.Scorer
import org.dice_group.glisten.core.scorer.ScorerFactory
import org.hobbit.core.components.AbstractEvaluationModule
import org.hobbit.core.rabbit.RabbitMQUtils
import org.hobbit.vocab.HOBBIT
import org.slf4j.Logger
import org.slf4j.LoggerFactory


sealed class Evaluator : AbstractEvaluationModule() {

    var scorerName: String = "Copaal_AvgScore"
    var sampleSize: Int = 5000
    lateinit var conf: Configuration
    var benchmarkName = ""

    val aucList = mutableListOf<Double>()
    val times = mutableListOf<Double>()

    var maxPropertyLimit = 10
    var timeout = 30L

    val params = EvaluationParameters.createDefault()
    lateinit var coreEvaluator: CoreEvaluator

    override fun init() {
        super.init()

        if(System.getenv().containsKey(CONSTANTS.BENCHMARK_NAME)){
            benchmarkName = System.getenv()[CONSTANTS.BENCHMARK_NAME]!!
        }
        if(System.getenv().containsKey(CONSTANTS.NUMBER_OF_TRUE_STATEMENTS)){
            params.numberOfTrueStatements = System.getenv()[CONSTANTS.NUMBER_OF_TRUE_STATEMENTS]!!.toInt()
        }
        if(System.getenv().containsKey(CONSTANTS.NUMBER_OF_FALSE_STATEMENTS)){
            params.numberOfFalseStatements = System.getenv()[CONSTANTS.NUMBER_OF_FALSE_STATEMENTS]!!.toInt()
        }
        if(System.getenv().containsKey(CONSTANTS.MIN_PROP_OCC)){
            params.minPropertyOccurrences = System.getenv()[CONSTANTS.MIN_PROP_OCC]!!.toInt()
        }
        if(System.getenv().containsKey(CONSTANTS.MAX_PROPERTY_LIMIT)){
            params.maxPropertyLimit = System.getenv()[CONSTANTS.MAX_PROPERTY_LIMIT]!!.toInt()
        }
        if(System.getenv().containsKey(CONSTANTS.MAX_RECOMMENDATIONS)){
            params.maxRecommendations = System.getenv()[CONSTANTS.MAX_RECOMMENDATIONS]!!.toInt()
        }
        sampleSize = 1000
        if(System.getenv().containsKey(CONSTANTS.SAMPLE_SIZE)){
            sampleSize = System.getenv()[CONSTANTS.SAMPLE_SIZE]!!.toInt()
        }
        if(System.getenv().containsKey(CONSTANTS.SEED)){
            params.seed = System.getenv()[CONSTANTS.SEED]!!.toLong()
        }
        if(System.getenv().containsKey(CONSTANTS.TIMEOUT)){
            timeout = System.getenv()[CONSTANTS.TIMEOUT]!!.toLong()
        }
        var scorer : Scorer = SampleCopaal(params.seed, sampleSize, conf.namespaces, timeout)
        if(System.getenv().containsKey(CONSTANTS.SCORER_ALGORITHM)){
            scorerName = System.getenv()[CONSTANTS.SCORER_ALGORITHM]!!
            scorer = ScorerFactory.createScorerOrDefault(System.getenv()[CONSTANTS.SCORER_ALGORITHM]!!, conf.namespaces, timeout, params.seed, sampleSize)
        }
        params.linkedPath="/glisten/links/"
        FileUtils.mkdirs("/glisten/links/")

        createEvaluator(scorer)

    }

    open fun createEvaluator(scorer: Scorer){
        //read config, if config doesn't exist or benchamarkName is not in config will throw an exception
        conf = ConfigurationFactory.findCorrectConfiguration(CONSTANTS.CONFIG_NAME, benchmarkName)
        // create core evaluator using a standard virtuoso endpoint for now.
        coreEvaluator = CoreEvaluator(conf, params,"http://localhost:3030/ds/sparql", scorer)

        //download zips and extract them
        coreEvaluator.init("/")
    }


    override fun evaluateResponse(
        expectedData: ByteArray?,
        receivedData: ByteArray?,
        taskSentTimestamp: Long,
        responseReceivedTimestamp: Long
    ) {
        val recommendations = mutableListOf<Pair<String, Double>>()

        val recommendationJSON = JsonParser.parseString(RabbitMQUtils.readString(receivedData))
        recommendationJSON.asJsonArray.forEach{
            recommendations.add(Pair(it.asJsonObject.get("dataset").asString, it.asJsonObject.get("score").asDouble))
        }
        val source = RabbitMQUtils.readString(expectedData)
        val auc = coreEvaluator.getAUC(source, recommendations)
        aucList.add(auc)
        times.add(1.0*(responseReceivedTimestamp-taskSentTimestamp))

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
        val sumTime: Property?= ResourceFactory.createProperty(CONSTANTS.GLISTEN_PREFIX+"sumTime")
        val maxTime: Property?= ResourceFactory.createProperty(CONSTANTS.GLISTEN_PREFIX+"maxTime")
        val minTime: Property?= ResourceFactory.createProperty(CONSTANTS.GLISTEN_PREFIX+"minTime")
        val avgTime: Property?= ResourceFactory.createProperty(CONSTANTS.GLISTEN_PREFIX+"avgTime")

    }
}