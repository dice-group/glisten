/**
 * This file is part of glisten.
 *
 * glisten is free software: you can redistribute it and/or modify
 * it under the terms of the Affero GNU Lesser General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * glisten is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Affero GNU Lesser General Public License version 3 for more details.
 *
 * You should have received a copy of the Affero GNU Lesser General Public License
 * along with glisten.  If not, see <http://www.gnu.org/licenses/>.
 * Created by Lixi Alié Conrads, 8/3/21
 */
package org.dice_group.glisten.core.config

/**
 * Constants which are mainly used inside the Hobbit workflow.
 */
object CONSTANTS {



    //Some default values for the configuration and hobbit

    const val SCORER_JSON_ID = "scorerId"
    const val SCORER_JSON_SCORE = "score"
    const val COMMAND_SCORER_FINISHED: Byte = 51
    const val COMMAND_SCORER_READY: Byte = 41
    const val COMMAND_FACTS: Byte = 61
    const val SCORER_QUEUE = "scorerName"
    const val ID = "id"
    const val RECOMMENDATIONS = "recommendations"
    const val SOURCE_NAME = "sourceName"
    const val SCRIPT_FILE = "./load_triplestore.sh"
    const val MAX_RECOMMENDATIONS = "maxRecommendations"
    const val STMTDRAWER_TYPE = "stmtDrawerType"
    const val NUMBER_OF_STATEMENTS = "numberOfStatements"
    const val SEED = "seed"
    const val CONFIG_NAME = "data_config.yaml"
    const val BENCHMARK_NAME = "benchmarkName"
    const val GLISTEN_PREFIX = "http://w3id.org/glisten/hobbit/vocab#"
    const val NUMBER_OF_TRUE_STATEMENTS = "noOfTrueStmts"
    const val NUMBER_OF_FALSE_STATEMENTS = "noOfFalseStmts"
    const val MIN_PROP_OCC = "minPropOcc"
    const val MAX_PROPERTY_LIMIT = "maxPropertyLimit"
    const val SCORER_ALGORITHM = "scorerAlgorithm"
    const val SAMPLE_SIZE= "sampleSize"
}