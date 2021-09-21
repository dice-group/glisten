package org.dice_group.glisten.core.evaluation

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.dice_group.glisten.core.config.Configuration
import org.dice_group.glisten.core.config.EvaluationParameters
import org.dice_group.glisten.core.scorer.Copaal
import org.dice_group.glisten.core.scorer.FactGenerator
import org.dice_group.glisten.core.scorer.Scorer
import org.dice_group.glisten.core.utils.DownloadUtils
import org.dice_group.glisten.core.utils.RDFUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * ## Description
 *
 * The [CoreEvaluator] creates he core glisten evaluator.
 * It is responsible for executing the benchmark given by  a [Configuration] using
 * the provided [EvaluationParameters].
 *
 * The first step is to call the [init] function downloading the specified files inside the [conf]
 *
 * The nest step is to call the actual algorithm inside the [getAUC] method.
 *
 * Which will get the source file from the provided [conf] and loads the source into the provided
 * [rdfEndpoint], which resembles the SPARQL endpoint of a triplestore.
 * The source file will be loaded into the store using the script declared in the provided [EvaluationParameters.triplestoreLoaderScript]
 *
 * Using the provided [Scorer] algorithm, the baseline will be calculated using the [rdfEndpoint].
 *
 * The recommendations provided to the [getAUC] method will then be sorted after the provided values (the highest score first),
 * and for each recommendation
 * the linked dataset for that recommendation (linking source and recommendation together) will be loaded into the
 * [rdfEndpoint] and the AUC score will be calculated using the script declared in the provided [EvaluationParameters.triplestoreLoaderScript].
 *
 * For each AUC score the algorithm will then look if the AUC score got better as the previous score.
 * If the score got better, the recommendation provided more insight and seems topical more near to the source.
 * Each change will be stored and everytime the score changes for the better, the [ROCCurve] will go up
 * and each time the score doesn't get better, the [ROCCurve] will go right.
 *
 * Finally the AUC score for that [ROCCurve] will be calculated and provides the result of the benchmark
 *
 * ```
 * ```
 *
 * ## Example
 *
 * ```kotlin
 *
 * //1. create a Configuration (we will load one from file and uses the configuration with the name testBenchmark)
 * val conf = ConfigurationFactory.findCorrectConfiguration("/path/to/config.yaml", "testBenchmark")
 *
 * //2. create a Scorer Algorithm we want to use. (we will use Copaal for this one)
 * val scorer : Scorer = Copaal(conf.namespaces)
 *
 * //3. lets assume our triplestore is started at http://localhost:9999/sparql
 * val rdfEndpoint = "http://localhost:9999/sparql"
 *
 * //Be aware to load the datasets into the triplestore the script `load_triplestore.sh` is used  with the arguments
 * //   `/path/to/` and `file.nt`
 * // change that script to so that it loads the given file into your triplestore. An example is provided for Virtuoso
 *
 * //4. let's create the CoreEvaluator
 *
 * val evaluator = CoreEvaluator(conf, rdfEndpoint, scorer)
 *
 * //init and download the files into the /tmp folder, we can choose a different one.
 * evaluator.init("/tmp/")
 *
 * //now we need a source file and some recommendations
 * // for the source file we will use the first one inside our configuration
 * val source = conf.sources[0]
 *
 * //for our recommendations, we will just use a mock here, here is the part where you can set your recommendation system
 * // important is only that you create a list containing all recommendations which are listed in the
 * // zip file stated in the configuration under conf.targetUrlZip
 * val recommendations = listOf(Pair("file:///path/to/target1", 0.1), Pair("file:///path/to/target2", 0.05))
 *
 * //now let's start the benchmark
 * val auc = evaluator.getAUC(source, recommendations)
 * println("AUC score is $auc")
 * ```
 *
 * @param conf The [Configuration] to use
 * @param params The parameters used inside the evaluation using the [EvaluationParameters]
 * @param rdfEndpoint The SPARQL endpoint to use
 * @param scorer The Scorer Algorithm to use
 */
class CoreEvaluator(private val conf: Configuration, val params: EvaluationParameters, private val rdfEndpoint: String, private val scorer: Scorer) {


    /**
     * Downloads the linked datasets and extract them to the linkedPath
     *
     * @param zipDest the destination folder to download the zip file to
     * @throws ZipException if the downloaded file is not a zip file
     * @throws IOException If the links url in the configuration doesn't exist
     * @throws SecurityException if one of the files cannot be written to the location due to a permission error
     */
    @Throws(ZipException::class, IOException::class, SecurityException::class)
    fun init(zipDest: String){
        val zipFile = DownloadUtils.download(conf.linksUrlZip, zipDest)
        val linksZip = ZipFile(zipFile)
        try {
            File(params.linkedPath).mkdirs()
            linksZip.extractAll(params.linkedPath)
        }catch(e: ZipException){
            //cleanup, delete wrong file
            File(zipFile).delete()
            System.err.println("[!!] Zip file $zipFile couldn't be unzipped. Deleted file for safety reasons.")
            throw e
        }
    }

    /**
     * ### Description
     *
     * Calculates the AUC score for all recommendations (datasets)
     * Will use the name of source and the names in recommendations to get the downloaded linked dataset and add them to the triplestore
     *
     * The source model will be loaded into the triple store at first.
     *
     * The recommendations will be ordered (desc) after the double value (representing how good the recommendation fits to the source)
     * Thus the best recommendation will be added first, the score for that recommendation will be calculated added to the ROC curve and so on.
     *
     * The source model will be read in memory to generate correct and false facts to be used to execute against a Scorer like Copaal.
     *
     * ### Example:
     *
     * ```kotlin
     * //create your evaluator here
     * //val evaluator = CoreEvaluator(...)
     *
     * val recommendations = mutableListOf(
     *                          Pair("file:///path/to/target1.nt", 0.1),
     *                          Pair("file:///path/to/target2.nt", 0.2),
     *                          Pair("file:///path/to/target3.nt", -0.5))
     * val aucScore : Double = evaluator.getAUC("file:///path/to/source.nt", recommendations)
     * ```
     *
     *
     * @param source The url of the source string (if locally use file:// as the schema)
     * @param recommendations the recommendation list, where each target dataset is annotated with a double value from -1 to 1, 1 ,means the dataset is very recommended.
     * @return the normalized AUC score
     */
    fun getAUC(source: String, recommendations: MutableList<Pair<String, Double>>) : Double {

        //create source model and
        // download file from URL stream. (is in file:/// format if locally)
        val sourceModel = RDFUtils.streamNoLiterals(source)

        //create facts for source model
        val facts = FactGenerator.createFacts(params.seed,
            params.numberOfTrueStatements,
            params.numberOfFalseStatements,
            conf.createTrueStmtDrawer(params.seed, sourceModel, params.minPropertyOccurrences, params.maxPropertyLimit),
            conf.createFalseStmtDrawer(params.seed, sourceModel, params.minPropertyOccurrences, params.maxPropertyLimit)
        )
        //we can now delete the sourceModel from memory
        facts.forEach {
            if(it.second==1.0) {
                sourceModel.remove(it.first)
            }
        }
        val cleanedSource = "tmp_source.nt"
        RDFDataMgr.write(FileOutputStream(cleanedSource), sourceModel, Lang.NT)
        sourceModel.removeAll()

        print("[-] loading source into triplestore now")
        RDFUtils.loadTripleStoreFromScript(cleanedSource, params.triplestoreLoaderScript)
        println("\r[+] finished loading source into triplestore.")

        //create baseline (w/o recommendations)
        val baseline = getScore(facts, source, "")

        // normalize it
        //if we use the better ROC, we don't need to normalize anymore.
        //val normalizer = 1.0/(1-baseline)
        //val roc = getROC(source, facts, recommendations)
        //return normalizer*(roc.calculateAUC()-baseline)

        val roc = getBetterROC(baseline, source, facts, recommendations)
        println("[+] ROC Curve created: $roc")
        return roc.calculateAUC()
    }

    /**
     * Creates a [ROCCurve] based upon if the score got better with the next recommendation
     *
     * E.g. lets look at the scores `[baseline: 0.1, +recom1: 0.2, +recom2: 0.2, +recom3: 0.3, , +recom4: 0.3]`
     * the scores got better with the recommendations 1 and recommendations 3
     * hence the score will go up there and to the right on the addition of recommendation 2 and 4
     *
     * The ROC will look like:
     * ```
     * ROC [(0, 0.25), (0.25, 0.25), (0.25, 0.5), (0.5, 0.5)]
     *```
     *
     * Afterwards we can normalize the ROC to
     * ```
     * ROC [(0, 0.5), (0.5, 0.5), (1.0, 0.5), (1.0, 1.0)]
     * ```
     *
     * @param baseline the baseline score
     * @param source The url of the source string (if locally use file:// as the schema)
     * @param recommendations the recommendation list, where each target dataset is annotated with a double value from -1 to 1, 1 ,means the dataset is very recommended.
     * @return The calculated [ROCCurve]
     */
    fun getBetterROC(baseline : Double, source: String, facts: List<Pair<Statement, Double>>, recommendations: MutableList<Pair<String, Double>>): ROCCurve {
        // sort recommendations, s.t. highest recommendation value is on top/first
        recommendations.sortByDescending { it.second }
        //We will store only the directions we want to go so we can calculate the upwards and rightwards steps in the ROC later
        val directions = mutableListOf<DIRECTION>()
        var counter = 1.0
        var oldScore = baseline
        for((dataset, _) in recommendations){
            if (counter > params.maxRecommendations && params.maxRecommendations > 0){
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
                oldScore = score
            }else{
                directions.add(DIRECTION.RIGHT)
            }
            counter += 1.0
        }
        println(directions)
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
        if(params.maxRecommendations > 0) {
            maxSize = recommendations.size.coerceAtMost(params.maxRecommendations)
        }
        val roc = ROCCurve(0, maxSize)

        var counter=1.0
        for((dataset, _) in recommendations){
            if (counter > params.maxRecommendations && params.maxRecommendations > 0){
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
     * Gets the AUC score of running the [Scorer] algorithm against the combination of the current Datasets loaded into the triple store
     * and the currentDataset.
     * Will add the linked dataset `${linkedPath}/sourceName_currentDataset` to the triple store using the [RDFUtils.loadTripleStoreFromScript] method.
     * The script which will be used is declared in the provided [EvaluationParameters.triplestoreLoaderScript]
     *
     * If you want to use just the source model to generate a baseline, set the currentDataset parameter to an empty string
     *
     * @param facts The facts to run the Scorer against.
     * @param sourceName the old source name will be used to figure out the correct linked dataset.
     * @param currentDataset the target dataset name, which link to the source will be added to the store. To use no dataset, use an empty string.
     * @return the AUC score of the Scorer algorithm
     */
    fun getScore(facts: List<Pair<Statement, Double>>, sourceName: String, currentDataset: String) : Double{
        if(currentDataset.isNotEmpty()) {
            //(Download is in init)
            val linkdataset = "${File(params.linkedPath).toURI()}/" + sourceName.substringAfterLast("/")
                .removeSuffix(".nt") + "_" + currentDataset.substringAfterLast("/")
            RDFUtils.loadTripleStoreFromScript(linkdataset, params.triplestoreLoaderScript)
        }
        //Let the Scorer run
        return scorer.getScore(rdfEndpoint, facts)
    }

}