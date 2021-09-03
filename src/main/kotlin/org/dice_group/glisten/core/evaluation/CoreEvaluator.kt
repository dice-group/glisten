package org.dice_group.glisten.core.evaluation

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Statement
import org.dice_group.glisten.core.config.CONSTANTS
import org.dice_group.glisten.core.config.Configuration
import org.dice_group.glisten.core.scorer.Copaal
import org.dice_group.glisten.core.scorer.FactGenerator
import org.dice_group.glisten.core.scorer.Scorer
import org.dice_group.glisten.core.utils.DownloadUtils
import org.dice_group.glisten.core.utils.RDFUtils
import java.io.File
import java.io.IOException
import java.net.URL
import kotlin.jvm.Throws

class CoreEvaluator(private val conf: Configuration, private val rdfEndpoint: String, private val scorer: Scorer) {

    var linkedPath = "./links"
    //set max Recommendations to TOP 10
    var maxRecommendations=10

    var seed = 1234L
    var numberOfFalseStatements = 10
    var numberOfTrueStatements = 10
    var minPropOcc = 10
    var maxPropertyLimit = 10


    /**
     * Downloads the linked datasets and extract them to the linkedPath
     *
     * @param zipDest the destination folder to download the zip file to
     * @throws ZipException if the downloaded file is not a zip file
     * @throws IOException If the links url in the configuration doesn't exist
     */
    @Throws(ZipException::class, IOException::class)
    fun init(zipDest: String){
        val zipFile = DownloadUtils.download(conf.linksUrlZip, zipDest)
        val linksZip = ZipFile(zipFile)
        try {
            linksZip.extractAll(linkedPath)
        }catch(e: ZipException){
            //cleanup, delete wrong file
            File(zipFile).delete()
            System.err.println("[!!] Zip file $zipFile couldn't be unzipped. Deleted file for safety reasons.")
            throw e
        }
    }

    /**
     * Calculates the AUC score for all recommendations (datasets)
     * Will use the name of source and the names in recommendations to get the downloaded linked dataset and add them to the triplestore
     *
     * The recommendations will be ordered (desc) after the double value (representing how good the recommendation fits to the source)
     * Thus the best recommendation will be added first, the score for that recommendation will be calculated added to the ROC curve and so on.
     *
     * The source model will be read in memory to generate correct and false facts to be used to execute against a Scorer like Copaal.
     *
     *
     * @param source The url of the source string (if locally use file:// as the schema)
     * @param recommendations the recommendation list, where each target dataset is annotated with a double value from -1 to 1, 1 ,means the dataset is very recommended.
     * @return the normalized AUC score
     */
    fun getAUC(source: String, recommendations: MutableList<Pair<String, Double>>) : Double {
        //create source model
        val sourceModel = ModelFactory.createDefaultModel()
        //download file from URL stream. (is in file:/// format if locally)
        sourceModel.read(URL(source).openStream(), null, "NT")
        //create facts for source model
        val facts = FactGenerator.createFacts(seed,
            numberOfTrueStatements,
            numberOfFalseStatements,
            conf.createTrueStmtDrawer(seed, sourceModel, minPropOcc, maxPropertyLimit),
            conf.createFalseStmtDrawer(seed, sourceModel, minPropOcc, maxPropertyLimit)
        )
        //we can now delete the sourceModel from memory
        sourceModel.removeAll()
        //create baseline (w/o recommendations)
        val baseline = getScore(facts, source, "")
        // normalize it
        val normalizer = 1.0/(1-baseline)
        val roc = getROC(source, facts, recommendations)
        return normalizer*(roc.calculateAUC()-baseline)
    }

    /**
     * Creates a ROC based upon if the score got better with the next recommendation
     *
     * E.g. lets look at the scores [baseline: 0.1, +recom1: 0.2, +recom2: 0.2, +recom3: 0.3, , +recom4: 0.3]
     * the scores got better with the recommendations 1 and recommendations 3
     * hence the score will go up there and to the right on the addition of recommendation 2 and 4
     *
     * The ROC will look like:
     * ROC[(0, 0.25), (0.25, 0.25), (0.25, 0.5), (0.5, 0.5)]
     *
     * Afterwards we can normalize the ROC to
     * ROC[(0, 0.5), (0.5, 0.5), (1.0, 0.5), (1.0, 1.0)]
     */
    fun getBetterROC(source: String, facts: List<Pair<Statement, Double>>, recommendations: MutableList<Pair<String, Double>>): ROCCurve {
        // sort recommendations, s.t. highest recommendation value is on top/first
        recommendations.sortByDescending { it.second }
        //We will store only the directions we want to go so we can calculate the upwards and rightwards steps in the ROC later
        val directions = mutableListOf<DIRECTION>()
        var counter = 1.0
        var oldScore = 0.0
        for((dataset, _) in recommendations){
            if (counter>maxRecommendations && maxRecommendations>0){
                //if maxRecommendations > 0 and the the TOP N datasets were checked, break and return
                break
            }
            println("[*] Current dataset to add %s".format(dataset))
            val score = getScore(facts, source, dataset)
            println("[+] %s added, Score: %f".format(dataset, score))

            //store the directions
            // if the score didn't get better, go right, otherwise go up
            if(score > oldScore){
                directions.add(DIRECTION.UP)
            }else{
                directions.add(DIRECTION.RIGHT)
            }
            oldScore = score
            counter += 1.0
        }
        //now we can calculate the upwards steps and rightwards steps
        val upCount = directions.count {it == DIRECTION.UP}
        val rightCount = directions.count {it == DIRECTION.RIGHT}
        //and create our ROC curve
        val roc =ROCCurve(upCount, rightCount)
        //now we can simply say go up and right each time the score got better (resp. didn't get better) and will land at Point(1.0, 1.0)
        directions.forEach {
            if(it == DIRECTION.UP){
                roc.addUp()
            }else{
                roc.addRight()
            }
        }
        return roc
    }
        //TODO clean me up
    //FIXME the ROC curve can still get worse
    fun getROC(source: String, facts: List<Pair<Statement, Double>>, recommendations: MutableList<Pair<String, Double>>): ROCCurve{
        recommendations.sortByDescending { it.second }
        //steps doesn't ,matter in our case, we don't know the first either way
        var maxSize = recommendations.size
        if(maxRecommendations>0) {
            maxSize = recommendations.size.coerceAtMost(maxRecommendations)
        }
        val roc = ROCCurve(0, maxSize)

        var counter=1.0
        for((dataset, _) in recommendations){
            if (counter>maxRecommendations && maxRecommendations>0){
                //if maxRecommendations > 0 and the the TOP N datasets were checked, break and return
                return roc
            }
            println("[*] Current dataset to add %s".format(dataset))
            val score = getScore(facts, source, dataset)
            println("[+] %s added, Score: %f".format(dataset, score))
            //FIXME make use of better functions addUP, ...
            roc.addPoint(counter/maxSize, score)
            counter += 1.0
        }

        return roc
    }

    /**
     * Gets the AUC score of running copaal against the combination of the current Datasets loaded into the triple store
     * and the currentDataset.
     * Will add the linked dataset ${linkedPath}/sourceName_currentDataset to the triple store using the RDFUtils.loadStoreViaScript method.
     *
     * If you want to use just the source model to generate a baseline, set the currentDataset parameter to an empty string
     *
     * @param facts The facts to run Copaal against.
     * @param sourceName the old source name will be used to figure out the correct linked dataset.
     * @param currentDataset the target dataset name, which link to the source will be added to the store. To use no dataset, use an empty string.
     * @return the AUC score of Copaal
     */
    fun getScore(facts: List<Pair<Statement, Double>>, sourceName: String, currentDataset: String) : Double{
        if(currentDataset.isNotEmpty()) {
            //(Download is in init)
            val linkdataset = "file://$linkedPath/" + sourceName.substringAfterLast("/")
                .removeSuffix(".nt") + "_" + currentDataset.substringAfterLast("/")
            RDFUtils.loadTripleStoreFromScript(linkdataset, CONSTANTS.SCRIPT_FILE)
        }
        //Let the Scorer run
        return scorer.getScore(rdfEndpoint, facts)
    }

}