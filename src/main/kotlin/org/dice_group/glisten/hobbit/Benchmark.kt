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
 * Created by Lixi Alié Conrads, 7/30/21
 */
package org.dice_group.glisten.hobbit

import org.apache.jena.rdf.model.NodeIterator
import org.apache.jena.rdf.model.Resource
import org.dice_group.glisten.core.config.CONSTANTS
import org.hobbit.core.Constants
import org.hobbit.core.components.AbstractBenchmarkController
import org.hobbit.vocab.HobbitExperiments
import org.slf4j.LoggerFactory


class Benchmark : AbstractBenchmarkController() {

    companion object {

        private const val EVALUATION_MODULE_CONTAINER_IMAGE =
            "git.project-hobbit.eu:4567/glisten/glisten-benchmark/glisten-evaluationmodule"
        private const val TASK_GENERATOR_CONTAINER_IMAGE =
            "git.project-hobbit.eu:4567/glisten/glisten-benchmark/glisten-taskgenerator"
        private const val DATA_GENERATOR_CONTAINER_IMAGE =
            "git.project-hobbit.eu:4567/glisten/glisten-benchmark/glisten-datagenerator"

        private val LOGGER = LoggerFactory.getLogger(this::class.java.name)
    }

    override fun init() {
        super.init()

        //init parameters
        val newExpResource: Resource = benchmarkParamModel.getResource(HobbitExperiments.New.uri)

        var iterator: NodeIterator = benchmarkParamModel
            .listObjectsOfProperty(benchmarkParamModel.getProperty(CONSTANTS.GLISTEN_PREFIX + "benchmarkName"))
        var datasetName = ""
        if (iterator.hasNext()) {
            datasetName = iterator.next().asResource().toString()
        }


        //tasg generator seed and #statements
        iterator = benchmarkParamModel
            .listObjectsOfProperty(benchmarkParamModel.getProperty(CONSTANTS.GLISTEN_PREFIX + "seed"))
        var seed = 1L
        if (iterator.hasNext()) {
            seed = iterator.next().asLiteral().long
        }

        //tasg generator seed and #statements
        iterator = benchmarkParamModel
            .listObjectsOfProperty(benchmarkParamModel.getProperty(CONSTANTS.GLISTEN_PREFIX + "minPropOcc"))
        var minPropOcc = 10
        if (iterator.hasNext()) {
            minPropOcc = iterator.next().asLiteral().int
        }

        iterator = benchmarkParamModel
            .listObjectsOfProperty(benchmarkParamModel.getProperty(CONSTANTS.GLISTEN_PREFIX + "maxPropertyLimit"))
        var maxPropertyLimit = 10
        if (iterator.hasNext()) {
            maxPropertyLimit = iterator.next().asLiteral().int
        }

        iterator = benchmarkParamModel
            .listObjectsOfProperty(benchmarkParamModel.getProperty(CONSTANTS.GLISTEN_PREFIX + "noOfTrueStatements"))
        var noOfTrueStatements = 10
        if (iterator.hasNext()) {
            noOfTrueStatements = iterator.next().asLiteral().int
        }

        iterator = benchmarkParamModel
            .listObjectsOfProperty(benchmarkParamModel.getProperty(CONSTANTS.GLISTEN_PREFIX + "noOfFalseStatements"))
        var noOfFalseStatements = 10
        if (iterator.hasNext()) {
            noOfFalseStatements = iterator.next().asLiteral().int
        }


        iterator = benchmarkParamModel
            .listObjectsOfProperty(benchmarkParamModel.getProperty(CONSTANTS.GLISTEN_PREFIX + "noOfStatements"))
        var noOfStatements = 1
        if (iterator.hasNext()) {
            noOfStatements = iterator.next().asLiteral().int
        }

        // create TG Module
        createTaskGenerators(
            TASK_GENERATOR_CONTAINER_IMAGE, 1, arrayOf(
            )
        )
        LOGGER.info("Finished creating Task Generator")
        // create DG Module
        createDataGenerators(
            DATA_GENERATOR_CONTAINER_IMAGE, 1, arrayOf(
                CONSTANTS.BENCHMARK_NAME + "=" + datasetName,
            )
        )
        LOGGER.info("Finished creating Data Generator")

        createEvaluationStorage(
            DEFAULT_EVAL_STORAGE_IMAGE, arrayOf(
                CONSTANTS.BENCHMARK_NAME + "=" + datasetName,
                Constants.ACKNOWLEDGEMENT_FLAG_KEY + "=true",
                DEFAULT_EVAL_STORAGE_PARAMETERS[0],
                CONSTANTS.SEED + "=" + seed,
                CONSTANTS.NUMBER_OF_STATEMENTS + "=" + noOfStatements,
                CONSTANTS.NUMBER_OF_TRUE_STATEMENTS + "=" + noOfTrueStatements,
                CONSTANTS.NUMBER_OF_FALSE_STATEMENTS + "=" + noOfFalseStatements,
                CONSTANTS.MIN_PROP_OCC + "=" + minPropOcc ,
                CONSTANTS.MAX_PROPERTY_LIMIT + "=" + maxPropertyLimit,
            )
        )

        waitForComponentsToInitialize()


    }

    override fun executeBenchmark() {

        // create eval module (we do not need the datasets here, so yey!)
        LOGGER.info("Finished creating Modules")
        waitForDataGenToFinish()
        // wait for the task generators to finish their work
        waitForTaskGenToFinish()
        // wait for system
        waitForSystemToFinish()

        createEvaluationModule(
            EVALUATION_MODULE_CONTAINER_IMAGE, emptyArray()
        )
        // wait for the evaluation to finish
        waitForEvalComponentsToFinish()


    }
}