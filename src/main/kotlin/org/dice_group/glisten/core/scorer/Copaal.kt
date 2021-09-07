package org.dice_group.glisten.core.scorer

import org.aksw.jena_sparql_api.core.QueryExecutionFactory
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp
import org.aksw.jena_sparql_api.timeout.QueryExecutionFactoryTimeout
import org.apache.jena.rdf.model.Statement
import org.dice_research.fc.IFactChecker
import org.dice_research.fc.paths.PathBasedFactChecker
import org.dice_research.fc.paths.PredicateFactory
import org.dice_research.fc.paths.scorer.ICountRetriever
import org.dice_research.fc.paths.scorer.NPMIBasedScorer
import org.dice_research.fc.paths.scorer.count.PropPathBasedPairCountRetriever
import org.dice_research.fc.paths.scorer.count.decorate.CachingCountRetrieverDecorator
import org.dice_research.fc.paths.scorer.count.max.DefaultMaxCounter
import org.dice_research.fc.paths.search.SPARQLBasedSOPathSearcher
import org.dice_research.fc.sparql.filter.IRIFilter
import org.dice_research.fc.sum.FixedSummarist
import java.util.concurrent.TimeUnit


/**
 * ## Description
 *
 * The Scorer algorithm uses the Copaal Path Based Fact Checker to calculate the score of each fact.
 *
 * For more details on how Copaal works, have a look at [https://github.com/dice-group/COPAAL/](https://github.com/dice-group/COPAAL/)
 *
 * @param namespaces The namespaces to consider while fact checking.
 */
class Copaal(namespaces: List<String>) : Scorer(namespaces){

    /**
     * Creates a COPAAL [PathBasedFactChecker] using an [QueryExecutionFactoryHttp] with a delay of 200ms and a timeout of 30s
     *
     * It uses an [NPMIBasedScorer] using caching and the [PropPathBasedPairCountRetriever] and a [FixedSummarist].
     *
     * @param endpoint the SPARQL endpoint to use
     * @param namespaces the allowed namespaces, if empty will allow all namespaces
     */
    private fun createFactChecker(endpoint: String, namespaces: List<String>): IFactChecker{
        var qef : QueryExecutionFactory = QueryExecutionFactoryHttp(endpoint)
        qef = QueryExecutionFactoryDelay(qef, 200L)
        qef = QueryExecutionFactoryTimeout(qef, 30L, TimeUnit.SECONDS, 30L, TimeUnit.SECONDS)

        return PathBasedFactChecker(
            NamespaceBasedPredicateFactory(namespaces, qef),
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
            var veracity = try {
                val factscore = checker.check(fact)
                factscore.veracityValue
            }catch (e: Exception){
                //if an error occures (f.e. timeout, simply set to 0 and print the error)
                e.printStackTrace()
                0.0
            }
            if(veracity.isNaN()){
                veracity = 0.0
            }
            println("\r[+] checking fact %s , score %f, orig %f".format(fact, veracity, value))

            //add the pair original trueness value and the veracity score (we need the latter one simply for sorting later on)
            val ret = Pair(value, veracity)
            scores.add(ret)
        }
       return scores
    }
}

/**
 * A Predicate Factory allowing domain and ranges only if they are within one of the provided namespaces
 *
 * @param namespaces The namespaces which are allowd
 * @param qef The QUeryExecutionFactory to execute queries against
 */
class NamespaceBasedPredicateFactory(private val namespaces: List<String>, qef: QueryExecutionFactory) : PredicateFactory(qef){

    override fun getDomain(triple: Statement?): MutableSet<String> {
        return super.getDomain(triple).filter {
            isInNamespace(it)
        }.toMutableSet()
    }

    override fun getRange(triple: Statement?): MutableSet<String> {
        return super.getRange(triple).filter {
            isInNamespace(it)
        }.toMutableSet()
    }

    /**
     * Checks if the iri string is in one of the given [namespaces]
     *
     * @param iri String to check if it is inside one of the namespaces
     * @return true if [iri] is in one of the namespaces, false otherwise
     */
    private fun isInNamespace(iri: String) : Boolean{
        var check = false
        namespaces.forEach {
            if(iri.startsWith(it)) {
                check = true
            }
        }
        return check
    }
}

/**
 * The namespace Filter creates a FILTER clause allowing all namespaces.
 *
 * @param namespaces the namespaces which should be allowed
 */
class NamespaceFilter(private val namespaces: List<String>) : IRIFilter{

    /**
     * ## Description
     *
     * Adds a SPARQL FILTER clause to the [queryBuilder] checking that
     * the solution for the [variableName] starts with the one of the [namespaces]
     *
     * Note that if namespaces is an empty list it will add no FILTER add all allowing all namespaces.
     *
     * ```
     * ```
     * ## Example
     *
     * ```kotlin
     * val filter = NamespaceFilter(listOf("http://example.com", "http://test.org"))
     *
     * val queryBuilder = StringBuilder("SELECT * {?var ?p ?o.")
     *
     * filter.addFilter("var", queryBuilder)
     *
     * queryBuilder.append("}")
     *
     * assertEquals(
     *      "SELECT * {?var ?p ?o. FILTER(strstarts(str(?var),"http://example.com") || strstarts(str(?var),"http://test.org")) \n}",
     *      queryBuilder.toString()
     * )
     *
     * ```
     *
     */
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