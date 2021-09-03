package org.dice_group.glisten.core.scorer

import org.aksw.jena_sparql_api.core.QueryExecutionFactory
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp
import org.aksw.jena_sparql_api.timeout.QueryExecutionFactoryTimeout
import org.apache.jena.rdf.model.Statement
import org.apache.jena.vocabulary.RDF
import org.dice_group.glisten.core.evaluation.ROCCurve
import org.dice_research.fc.IFactChecker
import org.dice_research.fc.paths.PathBasedFactChecker
import org.dice_research.fc.paths.PredicateFactory
import org.dice_research.fc.paths.scorer.NPMIBasedScorer
import org.dice_research.fc.paths.scorer.count.PropPathBasedPairCountRetriever
import org.dice_research.fc.paths.scorer.count.decorate.CachingCountRetrieverDecorator
import org.dice_research.fc.paths.scorer.count.max.DefaultMaxCounter
import org.dice_research.fc.paths.search.SPARQLBasedSOPathSearcher
import org.dice_research.fc.sparql.filter.IRIFilter
import org.dice_research.fc.sum.FixedSummarist
import java.util.concurrent.TimeUnit



class Copaal(namespaces: List<String>) : Scorer(namespaces){

    private fun createFactChecker(endpoint: String, namespaces: List<String>): IFactChecker{
        var qef : QueryExecutionFactory = QueryExecutionFactoryHttp(endpoint)
        qef = QueryExecutionFactoryDelay(qef, 200L)
        qef = QueryExecutionFactoryTimeout(qef, 30L, TimeUnit.SECONDS, 30L, TimeUnit.SECONDS)

        return PathBasedFactChecker(
            PredicateFactory(qef),
            SPARQLBasedSOPathSearcher(
                qef,
                3,
                listOf(
                    NamespaceFilter(namespaces)
                )
            ),
            NPMIBasedScorer(CachingCountRetrieverDecorator(PropPathBasedPairCountRetriever(qef, DefaultMaxCounter(qef)))),
            FixedSummarist()
        )
    }

    override fun getScores(endpoint: String, facts: List<Pair<Statement, Double>>) : MutableList<Pair<Double, Double>>{
        //create the fact checker
        val checker = createFactChecker(endpoint, namespaces)

        //calculate scores
        val scores = mutableListOf<Pair<Double, Double>>()
        for((fact, value) in facts ){
            print("[-] checking fact %s".format(fact))
            val veracity = try {
                val factscore = checker.check(fact)
                factscore.veracityValue
            }catch (e: Exception){
                //if an error occures (f.e. timeout, simply set to 0 and print the error)
                e.printStackTrace()
                0.0
            }
            println("\r[+] checking fact %s , score %f, orig %f".format(fact, veracity, value))
            //add the pair original trueness value and the veracity score (we need the latter one simply for sorting later on)
            val ret = Pair(value, veracity)
            scores.add(ret)
        }
       return scores
    }



}

class NamespaceFilter(private val namespaces: List<String>) : IRIFilter{

    override fun addFilter(variableName: String, queryBuilder: StringBuilder) {
        if(namespaces.isNotEmpty()) {
            queryBuilder.append(" FILTER(")

            for (i in 0 until namespaces.size - 1) {
                queryBuilder.append("strstarts(str(?").append(variableName).append("),\"")
                    .append(namespaces[i]).append("\") || ")

            }
            queryBuilder.append("strstarts(str(?").append(variableName).append("),\"")
                .append(namespaces.last()).append("\")) \n")
        }
    }

}