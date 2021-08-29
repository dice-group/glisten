package org.dice_group.glisten.core.copaal

import org.aksw.jena_sparql_api.core.QueryExecutionFactory
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay
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
import org.dice_research.fc.sparql.filter.EqualsFilter
import org.dice_research.fc.sparql.filter.NamespaceFilter
import org.dice_research.fc.sum.FixedSummarist
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors


class Copaal {

    val rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    val owl = "http://www.w3.org/2002/07/owl#"
    val rdfs = "http://www.w3.org/2000/01/rdf-schema#"

    val filteredProperties =
            arrayOf("http://dbpedia.org/ontology/wikiPageExternalLink", "http://dbpedia.org/ontology/wikiPageWikiLink")


    fun factChecker(dataset: Model, facts: List<Statement>) : Double{
        var qef: QueryExecutionFactory = QueryExecutionFactoryModel(dataset)
        qef = QueryExecutionFactoryDelay(qef, 200L)
        qef = QueryExecutionFactoryTimeout(qef, 30L, TimeUnit.SECONDS, 30L, TimeUnit.SECONDS)

        val checker = PathBasedFactChecker(
            PredicateFactory(qef),
            SPARQLBasedSOPathSearcher(
                qef,
                2,
                listOf(
                    NamespaceFilter(rdf, true),
                    NamespaceFilter(rdfs, true),
                    NamespaceFilter(owl, true),
                    EqualsFilter(filteredProperties)
                )
            ),
            NPMIBasedScorer(CachingCountRetrieverDecorator(ApproximatingCountRetriever(qef, DefaultMaxCounter(qef)))),
            FixedSummarist()
        )

        //get the veracity value to a list
        val scores = facts.stream().map{
            print("[-] checking fact %s".format(it))
            val ret = checker.check(it).veracityValue
            println("\r[+] checking fact %s , score %f, normalized %f".format(it, ret, (ret+1)/2.0))
            ret
        }.collect(Collectors.toList())
        //scores.sortByDescending { it }
        return getAUC(scores)
    }

    //TODO make use of the better up/right/diagonally functions
    private fun getAUC(scores: List<Double>) : Double{
        val roc = ROCCurve(0,0) // stmts doesn't matter
        roc.addPoint(0.0, 0.0)

        scores.forEachIndexed { i, value ->
            //TODO value_i+value_{i-1}???
            roc.addPoint(i/(1.0*scores.size), (value+1)/2.0) // value in -1..1 so +1/2 is normalizing it
        }
        return roc.calculateAUC()
    }
}