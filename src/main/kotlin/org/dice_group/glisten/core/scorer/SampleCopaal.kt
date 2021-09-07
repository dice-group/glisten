package org.dice_group.glisten.core.scorer

import org.aksw.jena_sparql_api.core.QueryExecutionFactory
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp
import org.aksw.jena_sparql_api.timeout.QueryExecutionFactoryTimeout
import org.dice_research.fc.IFactChecker
import org.dice_research.fc.paths.PathBasedFactChecker
import org.dice_research.fc.paths.scorer.NPMIBasedScorer
import org.dice_research.fc.paths.scorer.count.PropPathBasedPairCountRetriever
import org.dice_research.fc.paths.scorer.count.decorate.CachingCountRetrieverDecorator
import org.dice_research.fc.paths.scorer.count.decorate.SamplingCountRetrieverDecorator
import org.dice_research.fc.paths.scorer.count.max.DefaultMaxCounter
import org.dice_research.fc.paths.search.SPARQLBasedSOPathSearcher
import org.dice_research.fc.sum.FixedSummarist
import java.util.concurrent.TimeUnit

class SampleCopaal(private val seed : Long, private val sampleSize: Int, namespaces: List<String>) : Copaal(namespaces) {

    override fun createFactChecker(endpoint: String, namespaces: List<String>): IFactChecker {
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
            NPMIBasedScorer(CachingCountRetrieverDecorator(
                SamplingCountRetrieverDecorator(PropPathBasedPairCountRetriever(qef, DefaultMaxCounter(qef)),
                seed,
                sampleSize,
                qef
            )
            )),
            FixedSummarist()
        )
    }
}