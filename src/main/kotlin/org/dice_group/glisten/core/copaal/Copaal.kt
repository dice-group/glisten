package org.dice_group.glisten.core.copaal

//import org.dice_research.fc.sparql.filter.NamespaceFilter
import org.aksw.jena_sparql_api.core.QueryExecutionFactory
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel
import org.aksw.jena_sparql_api.timeout.QueryExecutionFactoryTimeout
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Statement
import org.dice_group.glisten.core.evaluation.ROCCurve
import org.dice_research.fc.paths.PathBasedFactChecker
import org.dice_research.fc.paths.PredicateFactory
import org.dice_research.fc.paths.scorer.NPMIBasedScorer
import org.dice_research.fc.paths.scorer.count.ApproximatingCountRetriever
import org.dice_research.fc.paths.scorer.count.decorate.CachingCountRetrieverDecorator
import org.dice_research.fc.paths.scorer.count.max.DefaultMaxCounter
import org.dice_research.fc.paths.search.SPARQLBasedSOPathSearcher
import org.dice_research.fc.sparql.filter.IRIFilter
import org.dice_research.fc.sum.FixedSummarist
import java.util.concurrent.TimeUnit

fun main(){
    var spaces = NamespaceFilter(listOf("abc", "def", "ghi"))
    val queryBuilder = StringBuilder()
    spaces.addFilter("var", queryBuilder)
    println(queryBuilder)
    queryBuilder.clear()
    spaces = NamespaceFilter(listOf("abc"))
    spaces.addFilter("var", queryBuilder)
    println(queryBuilder)
    queryBuilder.clear()
    spaces = NamespaceFilter(listOf("abc", "def"))
    spaces.addFilter("var", queryBuilder)
    println(queryBuilder)
    queryBuilder.clear()
}


class Copaal {



    val rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    val owl = "http://www.w3.org/2002/07/owl#"
    val rdfs = "http://www.w3.org/2000/01/rdf-schema#"

    val filteredProperties =
            arrayOf("http://dbpedia.org/ontology/wikiPageExternalLink", "http://dbpedia.org/ontology/wikiPageWikiLink")


    fun factChecker(dataset: String, facts: List<Pair<Statement, Double>>) : Double{
        //var qef: QueryExecutionFactory = QueryExecutionFactoryModel(dataset)
        var qef : QueryExecutionFactory = QueryExecutionFactoryHttp(dataset)
        qef = QueryExecutionFactoryDelay(qef, 0L)
        qef = QueryExecutionFactoryTimeout(qef, 15L, TimeUnit.SECONDS, 15L, TimeUnit.SECONDS)

        val checker = PathBasedFactChecker(
            PredicateFactory(qef),
            SPARQLBasedSOPathSearcher(
                qef,
                3,
                listOf(

                    NamespaceFilter(listOf("http://dbpedia.org/ontology","http://dbpedia.org/properties"))
                    //NamespaceFilter(rdf, true),
                    //NamespaceFilter(rdfs, true),
                    //NamespaceFilter(owl, true),
                    //EqualsFilter(filteredProperties)
                )
            ),
            NPMIBasedScorer(CachingCountRetrieverDecorator(ApproximatingCountRetriever(qef, DefaultMaxCounter(qef)))),
            FixedSummarist()
        )

        //get the veracity value to a list as Pair<Value, VeracityScore>
        var trueStmts = 0
        var falseStmts = 0
        val scores = mutableListOf<Pair<Double, Double>>()
        for((fact, value) in facts ){
            if(value>0){
                trueStmts++
            }
            else{
                falseStmts++
            }
            print("[-] checking fact %s".format(fact))
            val veracity = try {
                checker.check(fact).veracityValue
            }catch (e: Exception){
                0.0
            }
            println("\r[+] checking fact %s , score %f, orig %f".format(fact, veracity, value))
            val ret = Pair<Double, Double>(value, veracity)
            scores.add(ret)
        }

        //sort by veracity score s.t. highest score is on top
        scores.sortByDescending { it.second }
        //println("[*] scores %s".format(scores))
        return getAUC(scores, trueStmts, falseStmts)
    }

    //TODO make use of the better up/right/diagonally functions
    private fun getAUC(scores: List<Pair<Double, Double>>, trueStmts: Int, falseStmts: Int) : Double{
        val roc = ROCCurve(trueStmts,falseStmts) // stmts doesn't matter
        //roc.addPoint(0.0, 0.0)

        scores.forEach { (value, _) ->
            if(value>0){
                //found true fact
                roc.addUp()
                println("up")
            }else{
                //found false fact
                roc.addRight()
                println("right")
            }
        }
        println(roc)
        return roc.calculateAUC()

    }
}

class NamespaceFilter(val namespaces: List<String>) : IRIFilter{

    override fun addFilter(variableName: String, queryBuilder: StringBuilder) {

        queryBuilder.append(" FILTER(")

        for (i in 0 until namespaces.size-1){
            queryBuilder.append("strstarts(str(?").append(variableName).append("),\"")
                .append(namespaces[i]).append("\") || ")

        }
        queryBuilder.append("strstarts(str(?").append(variableName).append("),\"")
            .append(namespaces.last()).append("\")) \n")
    }

}