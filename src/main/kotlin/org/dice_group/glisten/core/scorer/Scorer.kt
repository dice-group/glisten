package org.dice_group.glisten.core.scorer

import org.apache.jena.rdf.model.Statement
import org.dice_group.glisten.core.evaluation.ROCCurve

/**
 * The scorer/fact checker interface to use if you want to use something else than Copaal.
 */
abstract class Scorer(val namespaces: List<String>) {


    /**
     * Calculates the AUC from calculating the scores for each fact.
     *
     * The ROC Curve which is used to calculate the score will be build by going one step up if the fact was true,
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
    fun getScore(endpoint: String, facts: List<Pair<Statement, Double>>) : Double{
        //get the actual score
        val scores = getScores(endpoint, facts)
        //sort by veracity score s.t. highest score is on top
        scores.sortByDescending { it.second }

        return getAUC(scores)
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
     * AUC helper function which will create a ROC curve and calculates its AUC.
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
        println(roc)
        return roc.calculateAUC()

    }
}
