package org.dice_group.glisten.hobbit

import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.dice_group.glisten.core.config.CONSTANTS
import org.dice_group.glisten.core.config.ConfigurationFactory
import org.dice_group.glisten.core.config.EvaluationParameters
import org.dice_group.glisten.core.evaluation.CoreEvaluator
import org.dice_group.glisten.core.scorer.ScorerFactory
import org.dice_group.glisten.core.utils.RDFUtils
import org.hobbit.core.components.AbstractCommandReceivingComponent
import org.hobbit.core.rabbit.RabbitMQUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileOutputStream
import java.util.concurrent.Semaphore

class Scorer : AbstractCommandReceivingComponent() {

    private var params: EvaluationParameters = EvaluationParameters.createDefault()
    private var coreEvaluator: CoreEvaluator? = null
    private var source = ""
    private val rdfEndpoint = "http://localhost:3030/ds/sparql"

    private var facts = mutableListOf<Pair<Statement, Double>>()
    private val datasets = mutableListOf<String>()
    private var id = 0

    private var timeout = 30L

    private val factsReceivedMutex = Semaphore(0)

    override fun run() {
        //send ready signal
        this.sendToCmdQueue(CONSTANTS.COMMAND_SCORER_READY)
        //wait for facts
        factsReceivedMutex.acquire()
        factsReceivedMutex.release()
        //send score
        evaluate()
    }

    override fun init() {
        super.init()
        //get environment variables
        id = System.getenv()[CONSTANTS.ID]!!.toInt()
        source = System.getenv()[CONSTANTS.SOURCE_NAME]!!

        val scorerName = System.getenv()[CONSTANTS.SCORER_ALGORITHM]!!
        val sampleSize = System.getenv()[CONSTANTS.SAMPLE_SIZE]!!.toInt()
        val seed = System.getenv()[CONSTANTS.SEED]!!.toLong()
        val benchmarkName = System.getenv()[CONSTANTS.BENCHMARK_NAME]!!
        val recommendationsString = System.getenv()[CONSTANTS.RECOMMENDATIONS]!!
        val timeout = System.getenv()[CONSTANTS.TIMEOUT]!!.toLong()

        createDatasets(recommendationsString)

        val conf = ConfigurationFactory.findCorrectConfiguration(CONSTANTS.CONFIG_NAME, benchmarkName)

        val scorer = ScorerFactory.createScorerOrDefault(scorerName, conf.namespaces, timeout, seed, sampleSize)
        coreEvaluator = CoreEvaluator(conf, params, rdfEndpoint, scorer)
        coreEvaluator?.init(".")
    }

    private fun createSource(source: String): String {
        val sourceModel = RDFUtils.streamNoLiterals(source)
        facts.forEach {
            if(it.second==1.0) {
                sourceModel.remove(it.first)
            }
        }
        val cleanedSource = "tmp_source.nt"
        RDFDataMgr.write(FileOutputStream(cleanedSource), sourceModel, Lang.NT)
        sourceModel.removeAll()
        return cleanedSource

    }

    private fun createDatasets(recommendationsString: String) {
        recommendationsString.split(" ").forEach {
            datasets.add(it)
        }
    }

    private fun evaluate() {
        val actualSource = createSource(source)

        print("[-] loading source into triplestore now")
        RDFUtils.loadTripleStoreFromScript(actualSource, params.triplestoreLoaderScript)
        println("\r[+] finished loading source into triplestore.")

        val score = coreEvaluator!!.getScore(facts, actualSource, datasets)

        //"{ score: SCORE, scorerID: id}"
        val json = JSONObject()
        json.put("score", score)
        json.put("scorerID", id)
        this.sendToCmdQueue(CONSTANTS.COMMAND_SCORER_FINISHED, RabbitMQUtils.writeString(json.toString()))
    }

    override fun receiveCommand(command: Byte, data: ByteArray?) {
        if(command == CONSTANTS.COMMAND_FACTS){
            createFactsFromJson(RabbitMQUtils.readString(data))
            factsReceivedMutex.release()
        }
    }

    private fun createFactsFromJson(readString: String) {
        val root = JSONArray(readString)
        root.forEach{
            val stmtObj = it as JSONObject
            val trueness = stmtObj.getDouble("value")
            val subject = ResourceFactory.createResource(stmtObj.getString("subject"))
            val predicate = ResourceFactory.createProperty(stmtObj.getString("predicate"))
            val `object` = ResourceFactory.createResource(stmtObj.getString("object"))
            facts.add(Pair(ResourceFactory.createStatement(subject, predicate, `object`), trueness))
        }
    }
}