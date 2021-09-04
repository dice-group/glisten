package org.dice_group.glisten

import com.jamonapi.utils.FileUtils
import org.apache.jena.query.ARQ
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.RIOT
import org.dice_group.glisten.core.ConfigurationLoadException
import org.dice_group.glisten.core.config.CONSTANTS
import org.dice_group.glisten.core.config.Configuration
import org.dice_group.glisten.core.config.ConfigurationFactory
import org.dice_group.glisten.core.scorer.FactGenerator
import org.dice_group.glisten.core.evaluation.CoreEvaluator
import org.dice_group.glisten.core.scorer.ScorerFactory
import org.dice_group.glisten.core.utils.DownloadUtils
import org.dice_group.glisten.core.utils.RDFUtils
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable
import kotlin.Comparator
import kotlin.jvm.Throws
import kotlin.random.Random


/**
 * Creates a complete run without Hobbit.
 */
fun main(args: Array<String>) {
    val executor = Test()
    //*args converts array to vararg in kotlin
    CommandLine(executor).execute(*args)
}

@CommandLine.Command(name = "glisten-test", mixinStandardHelpOptions = true, version = ["glisten 1.0"],
    description = ["Executes the glisten workflow without Hobbit and prints the ROC curve at the end. Mostly useful for debugging."])
class Test : Callable<Int> {

    @CommandLine.Option(names = ["-S", "--seed"], description = ["the seed to use for anything random we do. Default=1234L"])
    var seed = 1234L

    @CommandLine.Option(names = ["--max-property-limit"], description = ["the maximum a property is allowed to be added for performance reasons. Default=30"])
    var maxPopertyLimit = 30;

    @CommandLine.Option(names = ["--min-prop-occ"], description = ["the minimum a property has to occur to be considered for the fact generation. Default=10"])
    var minPropOcc = 10;

    @CommandLine.Option(names = ["-m", "--max-recommendations"], description = ["the no. of max recommendations, 0 or lower means that all recommendations will be looked at. Default=10"])
    var maxRecommendations = 10;



    @CommandLine.Option(names = ["-nots", "--no-of-true-stmts"], description = ["the no. of true statements to generate. Default=5"])
    var numberOfTrueStatements = 5;

    @CommandLine.Option(names = ["-nofs", "--no-of-false-stmts"], description = ["the no. of false statements to generate. Default=5"])
    var numberOfFalseStatements = 5;

    @CommandLine.Parameters(description = ["the rdf endpoint to use"])
    lateinit var rdfEndpoint : String

    @CommandLine.Option(names = ["-c", "--config"], description = ["the config file for glisten. Default: data_config.yml"])
    var configFile = "data_config.yml"

    @CommandLine.Option(names = ["-o", "--order-file"], description = ["A file containing the order of the recommendations, if not set, will be random"])
    var orderFile = ""

    @CommandLine.Option(names = ["-s", "--scorer"], description = ["The Scorer algorithm to use. Algorithms: [Copaal] "])
    var scorerAlgorithm = "Copaal"



    @CommandLine.Option(names = ["--clean-up"], description = ["if set, will remove the testing directory which includes all downloaded and extracted datasets."])
    var cleanUp = false


    /**
     * Executes the whole workflow.
     *
     * @throws ConfigurationLoadException If the Configuration is not found or the test_benchmark name doesn't exists
     */
    @Throws(ConfigurationLoadException::class)
    override fun call(): Int{
        //not sure why we need these, but on some servers there are some problems other wise.
        ARQ.init()
        RIOT.init()
        //read the configuration, if the config is not found or the benchmarkName doesn't exists, will throw an exception.
        val conf = ConfigurationFactory.findCorrectConfiguration(configFile, "test_benchmark")
        //download all files
        simpleNaiveCache(conf)

        val recommendations = mutableListOf<Pair<String, Double>>()
        File("testing/target/").listFiles()!!.forEach {
            recommendations.add(Pair(it.absolutePath, 1.0))
        }


        if(orderFile.isEmpty()) {
            //create random order (it really doesn't matter tbh)
            recommendations.shuffle(Random(seed))
        }else{
            orderRecommendations(recommendations, orderFile)
        }

        //Set parameters
        val evaluator = createEvaluator(conf)

        print("[-] loading source into triplestore now")
        val sourceFile = conf.sources[0]
        RDFUtils.loadTripleStoreFromScript(sourceFile, CONSTANTS.SCRIPT_FILE)
        println("\r[+] finished loading source into triplestore.")

        //create source model for fact generation
        println("[+] reading source Model now")
        val sourceModel = RDFUtils.streamNoLiterals(sourceFile)
        //create facts for source model
        val facts = createFacts(conf, evaluator, sourceModel)
        //calculate ROC (we do this outside of the CoreEvaluator as we want the ROC
        calculateROC(evaluator, sourceFile, facts, recommendations)

        //cleanup -> remove testing directory
        if(cleanUp){
            print("[-] Removing testing directory now.")
            FileUtils.delete("testing/")
            println("\r[+] Deleted testing directory.")
        }
        return 0
    }

    /**
     * Will order the recommendations according to the order file
     *
     * Each file in the recommendations needs to be in the order file and each file per line
     */
    private fun orderRecommendations(recommendations: MutableList<Pair<String, Double>>, orderFile: String) {
        //create order map for lookup
        val orderMap = mutableMapOf<String, Int>()
        org.apache.commons.io.FileUtils.readLines(File(orderFile)).forEachIndexed { index, s -> orderMap[s] = index }
        try   {
        recommendations.sortWith(Comparator { recom1, recom2 ->
            orderMap[recom1.first.substringAfterLast("/")]!!.compareTo(
                orderMap[recom2.first.substringAfterLast("/")]!!
            )
        })
        }catch(e : Exception){
            //if the order doesn't contain a string
            //just ignore the ordering
            System.err.println("Order file is incomplete. Will use native file reading order.")
        }
    }

    private fun createFacts(conf: Configuration, evaluator: CoreEvaluator, sourceModel: Model): MutableList<Pair<Statement, Double>>{
        print("[-] generating %d positive and %d negative facts now ".format(evaluator.numberOfTrueStatements, evaluator.numberOfFalseStatements))
        val facts = FactGenerator.createFacts(evaluator.seed,
            evaluator.numberOfTrueStatements,
            evaluator.numberOfFalseStatements,
            conf.createTrueStmtDrawer(evaluator.seed, sourceModel, evaluator.minPropOcc, evaluator.maxPropertyLimit),
            conf.createFalseStmtDrawer(evaluator.seed, sourceModel, evaluator.minPropOcc, evaluator.maxPropertyLimit)
        )
        //Save memory remove all statements from the source model
        sourceModel.removeAll()
        println("\r[+] Done generating [%d positives, %d negative]facts. Facts are %s".format(evaluator.numberOfTrueStatements, evaluator.numberOfFalseStatements, facts))
        return facts
    }

    private fun createEvaluator(conf: Configuration): CoreEvaluator{
        val scorer = ScorerFactory.createScorerOrDefault(System.getenv()[CONSTANTS.SCORER_ALGORITHM]!!, conf.namespaces)
        val evaluator = CoreEvaluator(conf, rdfEndpoint, scorer)
        evaluator.seed= seed
        evaluator.maxPropertyLimit =maxPopertyLimit
        evaluator.maxRecommendations=maxRecommendations
        evaluator.minPropOcc=minPropOcc
        evaluator.numberOfTrueStatements=numberOfTrueStatements
        evaluator.numberOfFalseStatements=numberOfFalseStatements
        return evaluator
    }

    /**
     * A very simple and idiotic cache, but this is enough for testing purposes
     */
    private fun simpleNaiveCache(conf: Configuration){
        if(!FileUtils.exists("testing")) {
            print("[-] testing folder doesn't exists. creating it")
            FileUtils.mkdirs("testing/links/")
            FileUtils.mkdirs("testing/target/")
            DownloadUtils.unzipFile(DownloadUtils.download(conf.linksUrlZip, "testing/"), "testing/links/")

            //we basically need the targets just for ordering.
            DownloadUtils.unzipFile(DownloadUtils.download(conf.targetUrlZip, "testing/"), "testing/target/")
            println("\r[+] testing folder created, downloaded links and unzipped them.")
        }
        else{
            println("[+] Found testing folder. Won't download links and targets again. If you changed the links/targets. Please delete the testing folder first.")
        }
    }

    /**
     * Calculates the ROC curve
     */
    private fun calculateROC(evaluator: CoreEvaluator, source: String, facts: List<Pair<Statement, Double>>, recommendations: MutableList<Pair<String, Double>>){
        val baseline = evaluator.getScore(facts, source, "")
        println("\n[+] Baseline: %f".format(baseline))
        evaluator.linkedPath = File("testing/links/").absolutePath
        val roc = evaluator.getBetterROC(baseline, source, facts, recommendations)
        //y needs to get bigger.
        print(roc)

    }
}


