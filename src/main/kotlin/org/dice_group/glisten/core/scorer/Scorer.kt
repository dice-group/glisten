package org.dice_group.glisten.core.scorer

import org.apache.jena.rdf.model.Statement
import org.dice_group.glisten.core.evaluation.ROCCurve
import java.util.*
import kotlin.math.sqrt

/**
 * ## Description
 *
 * The scorer algorithm interface.
 *
 * Creates the AUC score from  a list of given facts and an SPARQL endpoint.
 *
 * To create a Scorer use the [ScorerFactory].
 *
 * @param namespaces The namespaces to use/consider creating the score.
 * @param timeout the timeout in seconds to use
 * @param scoreMethod The score method to use. [AUC, RootMeanSqrt, AverageScore]. Default is AUC.
 *
 * @see ScorerFactory
 */
abstract class Scorer(val namespaces: List<String>, val timeout: Long, val scoreMethods: Array<String> = arrayOf<String>("AUC")) {

    var threshold = 0.0

    /**
     * Calculates the AUC from calculating the scores for each fact.
     *
     * The [ROCCurve] which is used to calculate the score will be build by going one step up if the fact was true,
     * or right if it was a false fact.
     *
     * The scores will be sorted descending
     * Thus the score will present the order of the facts.
     * If the true facts have a higher score than the false facts the ROC curve will go up earlier and the AUC will be higher.
     *
     * @param endpoint the rdf endpoint to use
     * @param facts the facts to calculate the scores against , each pair consists of the statement and its trueness value
     * @return the AUC score
     */
    open fun getScore(endpoint: String, facts: List<Pair<Statement, Double>>) : Map<String, Double> {
        //get the actual score
        val scores = getScores(endpoint, facts)
        //sort by veracity score s.t. the highest score is on top
        scores.sortByDescending {
            if(it.first == 1.0){
                it.second-threshold
            }
            else{
                it.second
            }
        }
        //choose one method and return the method
        val results = emptyMap<String, Double>()
        for(scoreMethod in scoreMethods) {
            results.put(scoreMethod, when(scoreMethod.lowercase(Locale.getDefault())){
                "rootmeansqrt" -> getSqrtMeanScore(scores)
                "auc" -> getAUC(scores)
                "averagescore" -> getAverageScore(scores)
                else -> getAUC(scores)
            })
        }
        return results
    }

    /**
     * Gets the Scores for each Fact as a Pair whereas the returned Pairs consists of the original trueness Value as the first element
     * and the score as the second.
     *
     * @param endpoint the rdf endpoint to use
     * @param facts the facts to calculate the scores against, each pair consists of the statement and its trueness value
     *
     * @return a list of pairs containing the trueness value of the fact and the score
     */
    abstract fun getScores(endpoint: String, facts: List<Pair<Statement, Double>>): MutableList<Pair<Double, Double>>

    /**
     * AUC helper function which will create a [ROCCurve] and calculates its AUC.
     * the ROC will be calculated by the given scores in the given order
     *
     * @param scores the scores containing pairs of th trueness value and the fact scorer value
     * @return the AUC score
     */
    fun getAUC(scores: List<Pair<Double, Double>>) : Double{
        //count true and false statements
        val trueStmts = scores.count{ it.first > 0.0 }
        val falseStmts = scores.count{ it.first <= 0.0 }

        val roc = ROCCurve(trueStmts,falseStmts)
        scores.forEach { (value, _) ->
            if(value>0){
                //found true fact
                roc.addUp()
            }else{
                //found false fact
                roc.addRight()
            }
        }
        return roc.calculateAUC()
    }

    /**
     * Gets the average normalized score
     *
     * @param scores the scores containing pairs of th trueness value and the fact scorer value
     * @return the average normalized score
     */
    fun getAverageScore(scores: List<Pair<Double, Double>>) : Double {
        var score = 0.0
        scores.forEach { (trueness, scorerScore) ->
            var normalizedScore = scorerScore.coerceAtLeast(0.0)
            if(trueness == -1.0){
                normalizedScore = 1-scorerScore
            }
            score += normalizedScore
        }
        return score/scores.size
    }

    /**
     * Gets the root mean sqrt of the scores
     *
     * @param scores the scores containing pairs of th trueness value and the fact scorer value
     * @return the root mean sqrt error
     */
    fun getSqrtMeanScore(scores: List<Pair<Double, Double>>) : Double {
        var score = 0.0
        scores.forEach { (trueness, scorerScore) ->
            var normalizedScore = scorerScore.coerceAtLeast(0.0)
            if(trueness == -1.0){
                normalizedScore = 1-scorerScore
            }
            score += normalizedScore*normalizedScore
        }
        return sqrt(score/scores.size)
    }
}


/**
 * ## Description
 *
 * Factory to create a [Scorer] Algorithm.
 *
 * Use the [createScorerOrDefault] method to create a [Scorer] from the scorer algorithm name and some namespaces
 *  ```
 *  ```
 *
 * ## Example
 *
 * ```kotlin
 *
 * //This will create a copaal scorer using the namespace http://example.com
 * val scorer = ScorerFactory.createScorerOrDefault("copaal", listOf("http://example.com"))
 *
 * //If your scorer isn't implemented Copaal will be choosen by default using the provided namespaces
 * val defaultScorer = ScorerFactory.createScorerOrDefault("MyScorerDoesntExists", listOf())
 *
 *
 * ```
 *
 * @see Scorer
 */
object ScorerFactory{

    /**
     * Create a [Scorer] Algorithm from a String representing the algorithm and the namespaces to use
     * An empty namespaces list will lead to all namespaces being accepted.
     *
     * Current Implemented Algorithms:
     *
     * - Copaal : Using Copaal with ROC/AUC
     * - SampleCopaal : Using SampleCopaal with ROC/AUC
     * - Copaal_RootMeanSquare : Using Copaal with the Root Mean Square error metric
     * - SampleCopaal_RootMeanSquare : Using SampleCopaal with the Root Mean Square error metric
     * - Copaal_AvgScore : Using Copaal with the Average Score metric
     * - SampleCopaal_AvgScore : Using SampleCopaal with the Average Score metric
     *
     * @param scorerAlgorithm The name of the scorer algorithm e.g COPAAL
     * @param namespaces The list of namespaces which should be used
     * @param timeout the timeout in seconds to use
     * @param seed seed to use for any random activity
     * @param sampleSize If the Scorer uses samples, describes the sample size
     * @return The Scorer Algorithm with the name or the Default scorer algorithm: Copaal
     */
    fun createScorerOrDefault(scorerAlgorithm: String, namespaces: List<String>, timeout: Long, seed: Long, sampleSize: Int) : Scorer  {
        var scorer : Scorer = Copaal(namespaces, timeout)
        when(scorerAlgorithm.lowercase(Locale.getDefault())){
            "copaal" -> scorer = Copaal(namespaces, timeout, arrayOf<String>("AUC", "RootMeanSquare", "AverageScore"))
            "samplecopaal" -> {println("[+] Using SampleCopaal"); scorer = SampleCopaal(seed, sampleSize, namespaces, timeout, arrayOf<String>("AUC", "RootMeanSquare", "AverageScore"))}
            "copaal_rootmeansquare" -> scorer = Copaal(namespaces, timeout, arrayOf<String>("RootMeanSquare"))
            "samplecopaal_rootmeansquare" -> {println("[+] Using SampleCopaal[RootMeanSquare]"); scorer = SampleCopaal(seed, sampleSize, namespaces, timeout, arrayOf<String>("RootMeanSquare"))}
            "copaal_avgscore" -> scorer = Copaal(namespaces, timeout, arrayOf<String>("AverageScore"))
            "samplecopaal_avgscore" -> {println("[+] Using SampleCopaal[AvgScore]"); scorer = SampleCopaal(seed, sampleSize, namespaces,timeout, arrayOf<String>("AverageScore"))}
        }
        return scorer
    }
}
