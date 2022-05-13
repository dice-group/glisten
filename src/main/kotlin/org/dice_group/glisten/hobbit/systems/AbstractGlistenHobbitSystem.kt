package org.dice_group.glisten.hobbit.systems

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.jamonapi.utils.FileUtils
import org.dice_group.glisten.core.utils.DownloadUtils
import org.hobbit.core.components.AbstractSystemAdapter
import org.hobbit.core.rabbit.RabbitMQUtils
import java.io.File

/**
 * Glistens abstract system.
 *
 * Is used to retrieve the target and source datasets.
 */
abstract class AbstractGlistenHobbitSystem : AbstractSystemAdapter(){

    private val targets = ArrayList<File>()

    override fun init() {
        super.init()
        //make sure ./sources/ exists
        FileUtils.mkdirs("./sources/")
        //make sure that ./targets exists
        FileUtils.mkdirs("./targets");
        //make sure that ./targets exists
        FileUtils.mkdirs("./tmp");
    }

    override fun receiveGeneratedData(p0: ByteArray?) {
        //Download targets
        val targetZipUrl = RabbitMQUtils.readString(p0);
        val targetZip = DownloadUtils.download(targetZipUrl, "./tmp");
        //unzip targets
        DownloadUtils.unzipFile(targetZip, "./targets/");
        //add them to the target lists
        targets.addAll(File("./targets/").listFiles()?: emptyArray())
    }

    override fun receiveGeneratedTask(p0: String?, p1: ByteArray?) {
        val sourceUrl = RabbitMQUtils.readString(p1);
        val source = File(DownloadUtils.download(sourceUrl, "./sources/"))

        //generate File-> score mapping
        val filePairs = generateRecommendationScores(source, targets)
        //we only want the name in
        val scores = filePairs.map { Pair(it.first.name, it.second) }

        //Create json
        val jsonScores = createJson(scores)
        // send json to evaluator
        sendResultToEvalStorage(p0, RabbitMQUtils.writeString(jsonScores))

    }

    /**
     * Creates the JSON string the Evaluation Module uses.
     *
     * The Pairs [Pair("persons.nt", 0.9), Pair("actions.nt", 0.2)]
     * will then be mapped to
     *
     * ```json
     * [
     *  {
     *      "dataset" : "persons.nt",
     *      "score": "0.9"
     *  },
     *  {
     *      "dataset" : "actions.nt",
     *      "score": "0.2"
     *  }
     * ]
     * ```
     *
     * @param scores The scores to convert to JSON
     * @return the json string representation
     */
    private fun createJson(scores: List<Pair<String, Double>>): String{
        val root = JsonArray()
        scores.forEach { (filename, score) ->
            val dataset = JsonObject()
            dataset.addProperty("dataset", filename)
            dataset.addProperty("score", score)
            root.add(dataset)
        }
        return root.asString
    }

    /**
     * Given a source file and a list of target files, the system shall create a score
     * for each target file.
     *
     * The score represents how fitting the target is for the source file (aka, is it a good recommendation)
     *
     * It shall return the mapping File->Score
     *
     * @param source The source to generate the recommendation scores with
     * @param targets The targets to generate the recommendations scores for
     * @return The mapping from target file to recommendation score
     */
    abstract fun generateRecommendationScores(source: File, targets: ArrayList<File>) : List<Pair<File, Double>>


}