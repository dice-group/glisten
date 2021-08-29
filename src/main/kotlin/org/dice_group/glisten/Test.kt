package org.dice_group.glisten

import com.jamonapi.utils.FileUtils
import org.apache.jena.query.ARQ
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.RIOT
import org.dice_group.glisten.core.config.ConfigurationFactory
import org.dice_group.glisten.core.copaal.FactGenerator
import org.dice_group.glisten.core.utils.DownloadFiles
import org.dice_group.glisten.core.utils.RDFUtils
import org.dice_group.glisten.hobbit.Evaluator
import java.io.File


/**
 * Creates a complete run without Hobbit.
 */
fun main(args: Array<String>){
    ARQ.init()
    RIOT.init()
    val conf = ConfigurationFactory.findCorrectConfiguration("test_benchmark")
    //download all files
    if(!FileUtils.exists("testing")) {
        print("[-] testing folder doesn't exists. creating it")
        FileUtils.mkdirs("testing/links/")
        FileUtils.mkdirs("testing/target/")
        DownloadFiles.unzipFile(DownloadFiles.download(conf.linksUrlZip, "testing/"), "testing/links/")

        //we basically need the targets just for ordering.
        DownloadFiles.unzipFile(DownloadFiles.download(conf.targetUrlZip, "testing/"), "testing/target/")
        println("\r[+] testing folder created, downloaded links and unzipped them.")
    }
    else{
        println("[+] Found testing folder. Won't download links and targets again.")
    }
    //create random order (it really doesn't matter)
    val recommendations = mutableListOf<Pair<String, Double>>()
    File("testing/target/").listFiles()!!.forEach {
        recommendations.add(Pair(it.absolutePath, 1.0))
    }
    val evaluator = Evaluator()
    //avoid hobbit init
    evaluator.debug = true
    evaluator.seed= 1234L
    evaluator.maxPropertyLimit =30
    //TOP TEN
    evaluator.maxRecommendations=20
    evaluator.minPropOcc=10
    evaluator.numberOfTrueStatements=args[0].toInt()
    evaluator.numberOfFalseStatements=args[1].toInt()

    val sourceFile = conf.sources[0]

    //create source model
    println("[+] reading source Model now")
    val sourceModel = RDFUtils.streamNoLiterals(sourceFile)
    //create facts for source model
    print("[-] generating %d positive and %d negative facts now ".format(evaluator.numberOfTrueStatements, evaluator.numberOfFalseStatements))
    val facts = FactGenerator.createFacts(evaluator.seed,
        evaluator.numberOfTrueStatements,
        evaluator.numberOfFalseStatements,
        conf.createTrueStmtDrawer(evaluator.seed, sourceModel, evaluator.minPropOcc, evaluator.maxPropertyLimit),
        conf.createFalseStmtDrawer(evaluator.seed, sourceModel, evaluator.minPropOcc, evaluator.maxPropertyLimit)
    ).map { (stmt, d) -> stmt }
    println("\r[+] Done generating [%d positives, %d negative]facts. Facts are %s".format(evaluator.numberOfTrueStatements, evaluator.numberOfFalseStatements, facts))
    calculateROC(evaluator, sourceFile, sourceModel, facts, recommendations)
    //evaluator.getAUC(sourceFile, recommendations)

}

fun calculateROC(evaluator: Evaluator, source: String, sourceModel: Model, facts: List<Statement>, recommendations: MutableList<Pair<String, Double>>){
    val baseline = evaluator.getScore(facts, source, sourceModel, "")
    println("[+] Baseline: %f".format(baseline))
    evaluator.linkedPath = File("testing/links/").absolutePath
    val roc = evaluator.getROC(source, sourceModel, facts, recommendations)
    //y needs to get bigger.
    print(roc)

}
