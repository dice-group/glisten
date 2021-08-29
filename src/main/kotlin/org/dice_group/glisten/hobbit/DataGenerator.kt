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

import org.apache.commons.configuration2.FileBasedConfiguration
import org.apache.commons.configuration2.YAMLConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.ex.ConfigurationException
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.dice_group.glisten.core.config.CONSTANTS
import org.dice_group.glisten.core.config.ConfigurationFactory
import org.hobbit.core.components.AbstractDataGenerator
import org.hobbit.core.rabbit.RabbitMQUtils
import org.hobbit.core.rabbit.SimpleFileSender
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream


class DataGenerator : AbstractDataGenerator() {

    companion object{
        private val LOGGER = LoggerFactory.getLogger(this::class.java.name)
    }

    private var benchmarkName = ""
    private var systemQueue = "systemQueue"
    private var tgQueue = "tgQueue"
    private var sourceUrls = emptyList<String>()
    private var targetUrl = ""

    override fun init(){
        super.init()
        //get env <- benchmark name
        if(System.getenv().containsKey(CONSTANTS.BENCHMARK_NAME)){
            benchmarkName = System.getenv()[CONSTANTS.BENCHMARK_NAME]!!
        }
        if(System.getenv().containsKey(CONSTANTS.ONTOLOGY_2_TG_QUEUE_NAME)){
            tgQueue = System.getenv()[CONSTANTS.ONTOLOGY_2_TG_QUEUE_NAME]!!
        }
        if(System.getenv().containsKey(CONSTANTS.ONTOLOGY_2_SYSTEM_QUEUE_NAME)){
            systemQueue = System.getenv()[CONSTANTS.ONTOLOGY_2_SYSTEM_QUEUE_NAME]!!
        }

        //read config <- get sourceUrls and targetUrl from there
        val config = ConfigurationFactory.findCorrectConfiguration(benchmarkName)
        sourceUrls = config.sources
        targetUrl = config.targetUrlZip
        //send TargetURL (zip to system)
        sendDataToSystemAdapter(RabbitMQUtils.writeString(targetUrl))
    }

    override fun generateData() {
        //send source url
        for(sourceUrl in sourceUrls) {
            sendDataToTaskGenerator(RabbitMQUtils.writeString(sourceUrl))
        }
    }



    private fun readConfiguration(fileName: String) : YAMLConfiguration? {
        val propertiesFile = File(fileName)
        val params = Parameters()
        val builder = FileBasedConfigurationBuilder<FileBasedConfiguration>(
            YAMLConfiguration::class.java
        )
            .configure(
                params.fileBased()
                    .setFile(propertiesFile)
            )
        return try {
            (builder.configuration as YAMLConfiguration)
            // config contains all properties read from the file
        } catch (cex: ConfigurationException) {
            LOGGER.error("Could not read dataset/benchmark property file")
            return null
        }

    }




}