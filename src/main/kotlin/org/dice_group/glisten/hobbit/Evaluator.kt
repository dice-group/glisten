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
import org.dice_group.glisten.core.evaluation.CoreEvaluator
import org.dice_group.glisten.core.scorer.Copaal
import org.dice_group.glisten.core.scorer.Scorer
import org.dice_group.glisten.core.scorer.ScorerFactory
import org.hobbit.core.components.AbstractEvaluationModule
import org.hobbit.core.rabbit.RabbitMQUtils
import org.hobbit.vocab.HOBBIT
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


class Evaluator : AbstractEvaluationModule() {

    lateinit var conf: Configuration

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

    lateinit var coreEvaluator: CoreEvaluator

    override fun init() {
        super.init()

        var benchmarkName = ""
        if(System.getenv().containsKey(CONSTANTS.BENCHMARK_NAME)){
            benchmarkName = System.getenv()[CONSTANTS.BENCHMARK_NAME]!!
        }
        if(System.getenv().containsKey(CONSTANTS.NUMBER_OF_TRUE_STATEMENTS)){
            numberOfTrueStatements = System.getenv()[CONSTANTS.NUMBER_OF_TRUE_STATEMENTS]!!.toInt()
        }
        if(System.getenv().containsKey(CONSTANTS.NUMBER_OF_FALSE_STATEMENTS)){
            numberOfFalseStatements = System.getenv()[CONSTANTS.NUMBER_OF_FALSE_STATEMENTS]!!.toInt()
        }
        if(System.getenv().containsKey(CONSTANTS.MIN_PROP_OCC)){
            minPropOcc = System.getenv()[CONSTANTS.MIN_PROP_OCC]!!.toInt()
        }
        if(System.getenv().containsKey(CONSTANTS.MAX_PROPERTY_LIMIT)){
            maxPropertyLimit = System.getenv()[CONSTANTS.MAX_PROPERTY_LIMIT]!!.toInt()
        }
        if(System.getenv().containsKey(CONSTANTS.MAX_RECOMMENDATIONS)){
            maxRecommendations = System.getenv()[CONSTANTS.MAX_RECOMMENDATIONS]!!.toInt()
        }
        if(System.getenv().containsKey(CONSTANTS.SEED)){
            seed = System.getenv()[CONSTANTS.SEED]!!.toLong()
        }
        var scorer : Scorer = Copaal(conf.namespaces)
        if(System.getenv().containsKey(CONSTANTS.SCORER_ALGORITHM)){
            scorer = ScorerFactory.createScorer(System.getenv()[CONSTANTS.SCORER_ALGORITHM]!!, conf.namespaces)
        }
        //read config, if config doesn't exists or benchamarName is not in config will throw an exception
        conf = ConfigurationFactory.findCorrectConfiguration(CONSTANTS.CONFIG_NAME, benchmarkName)
        // create core evaluator using a standard virtuoso endpoint for now.
        coreEvaluator = CoreEvaluator(conf, "http://localhost:8890/sparql", scorer)
        FileUtils.mkdirs("/links/")
        coreEvaluator.linkedPath="/links/"

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
        val sumTime: Property?= ResourceFactory.createProperty(CONSTANTS.GLISTEN_PREFIX+"SumTime")
        val maxTime: Property?= ResourceFactory.createProperty(CONSTANTS.GLISTEN_PREFIX+"maxTime")
        val minTime: Property?= ResourceFactory.createProperty(CONSTANTS.GLISTEN_PREFIX+"minTime")
        val avgTime: Property?= ResourceFactory.createProperty(CONSTANTS.GLISTEN_PREFIX+"avgTime")

    }
}