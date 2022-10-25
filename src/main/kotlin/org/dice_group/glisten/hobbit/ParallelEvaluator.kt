package org.dice_group.glisten.hobbit

import com.google.gson.JsonParser
import org.apache.jena.rdf.model.Model
import org.dice_group.glisten.core.config.CONSTANTS
import org.dice_group.glisten.core.evaluation.ROCCurve
import org.dice_group.glisten.core.scorer.FactGenerator
import org.dice_group.glisten.core.utils.RDFUtils
import org.hobbit.core.rabbit.RabbitMQUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Semaphore


class ParallelEvaluator : Evaluator() {

    private val sourceToRecommsMap = mutableMapOf<String, List<Pair<String, Double>>>()

    private val scorerReadyMutex = Semaphore(0)
    private val scorerFinishedMutex = Semaphore(0)

    private var scorerReadyCount=0
    private var scorerReadyNeeded=0

    private val scores = mutableListOf<Pair<Int, Double>>()

    companion object {
        private const val SCORER_IMAGE_NAME = "git.project-hobbit.eu:4567/glisten/benchmark/scorer"

    }

    override fun receiveCommand(command: Byte, data: ByteArray?) {
        if(command == CONSTANTS.COMMAND_SCORER_READY){
            scorerReadyCount++
            if(scorerReadyCount==scorerReadyNeeded){
                //all scorer are ready, realese mutex
                scorerReadyMutex.release()
                //set ready count back to 0 so we can use this in the finished mutex
                scorerReadyCount=0
            }
        }
        if(command == CONSTANTS.COMMAND_SCORER_FINISHED){
            scorerReadyCount++
            //get score from json
            val idScorePair : Pair<Int, Double> = readScorerJson(data)
            LOGGER.info("Received Scorer score ${idScorePair.second} for id ${idScorePair.first}")
            scores.add(idScorePair)
            //check if all scorer returned
            if(scorerReadyCount==scorerReadyNeeded){
                //all scorer finished, realese mutex
                scorerFinishedMutex.release()
            }
        }
        super.receiveCommand(command, data)
    }

    private fun readScorerJson(data: ByteArray?): Pair<Int, Double> {
        val jsonString = RabbitMQUtils.readString(data)
        val json = JSONObject(jsonString)
        return Pair(json.getInt(CONSTANTS.SCORER_JSON_ID), json.getDouble(CONSTANTS.SCORER_JSON_SCORE))
    }


    override fun evaluateResponse(
        expectedData: ByteArray?,
        receivedData: ByteArray?,
        taskSentTimestamp: Long,
        responseReceivedTimestamp: Long
    ) {
        times.add(1.0*(responseReceivedTimestamp-taskSentTimestamp))

        val recommendations = mutableListOf<Pair<String, Double>>()
        val recommendationJSON = JsonParser.parseString(RabbitMQUtils.readString(receivedData))
        recommendationJSON.asJsonArray.forEach{
            recommendations.add(Pair(it.asJsonObject.get("dataset").asString, it.asJsonObject.get("score").asDouble))
        }
        val source = RabbitMQUtils.readString(expectedData)
        sourceToRecommsMap[source] = recommendations
    }

    override fun summarizeEvaluation(): Model {
        sourceToRecommsMap.forEach { source, recommendations ->
            //sort so we have the best score (highest score at top)
            recommendations.sortedByDescending { it.second }

            val jsonFactsBytes = RabbitMQUtils.writeString(createFactsAsJsonString(source))
            val recommList = mutableListOf<String>()
            recommendations.forEach recomLoop@  { (recommendation, _) ->
                //so we have all recomms in this up to this point
                if(recommList.size>params.maxRecommendations && params.maxRecommendations>0){
                    //maxRecommendation is set to some value, and we already checked the max recommendations
                    return@recomLoop
                }
                recommList.add(recommendation)
                val id = recommendations.size

                createScorerContainer(source, recommList, id)

            }
            scorerReadyNeeded = recommList.size

            //wait till all containers send ready signal
            scorerReadyMutex.acquire()
            scorerReadyMutex.release()

            this.sendToCmdQueue(CONSTANTS.COMMAND_FACTS, jsonFactsBytes)
            //all container have finished
            scorerFinishedMutex.acquire()
            scorerFinishedMutex.release()

            val auc = calculateAuc(scores)
            aucList.add(auc)
        }

        return super.summarizeEvaluation()
    }

    private fun calculateAuc(scores: MutableList<Pair<Int, Double>>): Double {
        //sort s.t. the scores are in order (first is baseline)
        scores.sortBy { it.first }
        val roc = ROCCurve(0, scorerReadyNeeded)
        var counter = 0.0
        val baseline = scores.first().second
        scores.removeFirst()
        scores.forEach { (_, score)->
            roc.addPoint(counter/scorerReadyNeeded, score-baseline)
            counter++
        }
        LOGGER.info("Created ROC $roc")
        return roc.calculateAUC()
    }

    private fun createFactsAsJsonString(source: String): String {
        val sourceModel = RDFUtils.streamNoLiterals(source)

        //create facts for source model
        val facts = FactGenerator.createFacts(params.seed,
            params.numberOfTrueStatements,
            params.numberOfFalseStatements,
            conf.createTrueStmtDrawer(params.seed, sourceModel, params.minPropertyOccurrences, params.maxPropertyLimit),
            conf.createFalseStmtDrawer(params.seed, sourceModel, params.minPropertyOccurrences, params.maxPropertyLimit)
        )
        val root = JSONArray()
        facts.forEach { (stmt, trueness) ->
            val stmtObj = JSONObject()
            stmtObj.put("value", trueness)
            stmtObj.put("subject", stmt.subject.toString())
            stmtObj.put("predicate", stmt.predicate.toString())
            stmtObj.put("object", stmt.`object`.toString())
            root.put(stmtObj)
        }
        return root.toString()
    }

    private fun createScorerContainer(source: String, recommList: MutableList<String>, id: Int) {

        val recommendationString = recommList.joinToString { " " }
        //Create the container
        createContainer(
            SCORER_IMAGE_NAME, arrayOf(
                CONSTANTS.ID+"="+id,
                CONSTANTS.SCORER_ALGORITHM + "=" + scorerName,
                CONSTANTS.SAMPLE_SIZE + "=" + sampleSize,
                CONSTANTS.SEED + "=" + params.seed,
                CONSTANTS.BENCHMARK_NAME + "=" + benchmarkName,
                CONSTANTS.SOURCE_NAME +"="+source,
                CONSTANTS.RECOMMENDATIONS +"="+recommendationString,
            )
        )
    }


}