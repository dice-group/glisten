package org.dice_group.glisten.core.config

import org.dice_group.glisten.core.evaluation.CoreEvaluator

/**
 * The EvaluationParameters provides all the parameters used inside the [CoreEvaluator]
 *
 * @param seed the seed to use for any random activity
 * @param numberOfTrueStatements the number of true statements to create
 * @param numberOfFalseStatements the number of false statements to create
 * @param minPropertyOccurrences the minimum a property has to occur to be considered
 * @param maxPropertyLimit the maximum a property will be retrieved in the fact generation (for performance)
 * @param maxRecommendations the maximum recommendations to test against (top-N) use 0 or smaller for all recommendations
 * @param linkedPath the path to save/look up the linked datasets into
 * @param triplestoreLoaderScript the script to load datasets into the triplestore
 */
data class EvaluationParameters(var seed: Long,
                                var numberOfTrueStatements: Int,
                                var numberOfFalseStatements: Int,
                                var minPropertyOccurrences: Int,
                                var maxPropertyLimit: Int,
                                var maxRecommendations: Int,
                                var linkedPath: String,
                                var triplestoreLoaderScript: String,
                                ) {
    companion object {

        /**
         * Creates the default Parameters for the CoreEvaluator
         *
         * * seed = 1234
         * * numberOfTrueStatements = 10
         * * numberOfFalseStatements = 10
         * * minPropertyOccurrences = 1
         * * maxPropertyLimit = 30
         * * maxRecommendations = 10
         * * linkedPath = "./links/"
         * * triplestoreLoaderScript = [CONSTANTS.SCRIPT_FILE]
         */
        fun createDefault(): EvaluationParameters {
            return EvaluationParameters(
                1234L,
                10,
                10,
                1,
                30,
                10,
                "./links/",
                CONSTANTS.SCRIPT_FILE
            )
        }
    }
}