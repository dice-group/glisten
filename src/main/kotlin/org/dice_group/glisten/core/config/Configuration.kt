package org.dice_group.glisten.core.config

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.apache.jena.rdf.model.Model
import org.dice_group.glisten.core.ConfigurationLoadException
import org.dice_group.glisten.core.task.drawer.BlackListDrawer
import org.dice_group.glisten.core.task.drawer.StmtDrawer
import org.dice_group.glisten.core.task.drawer.WhiteListDrawer
import java.io.File
import java.io.IOException
import java.util.*


/**
 *
- name: "benchmarkName"
source :
- ...
target : URL.zip
TrueStmtDrawer
FalseStmtDrawer
 */
class Configuration(){
    lateinit var name: String
    lateinit var sources: List<String>
    lateinit var targetUrlZip: String
    lateinit var linksUrlZip: String
    lateinit var trueStmtDrawerOpt: Map<String, Any>
    lateinit var falseStmtDrawerOpt: Map<String, Any>

    private fun createStmtDrawer(type: String, list: Collection<String>, seed: Long, model: Model, minPropOcc: Int, maxPropertyLimit: Int  ): StmtDrawer {
        return if( type.lowercase(Locale.getDefault()) == "whitelist"){
            WhiteListDrawer(list, seed, model, minPropOcc, maxPropertyLimit)
        }else{
            BlackListDrawer(list, seed, model, minPropOcc, maxPropertyLimit)
        }
    }

    /**
     * Creates a true statement drawer which can randomly generate true statements from a list.
     * The list is either a whitelist or a blacklist, depending on what was stated in the configuration under stmtDrawerType
     *
     * @param seed the seed to use for any random activity which will be included in the drawer
     * @param model the model to retrieve the true statements against
     * @param minPropOcc the minimum a property has to occur
     * @param maxPropertyLimit the maximum a property is allowed to be retrieved.
     * @return A Statement Drawer which returns true facts
     */
    fun createTrueStmtDrawer(seed: Long, model: Model, minPropOcc: Int, maxPropertyLimit: Int  ): StmtDrawer {
        if(!trueStmtDrawerOpt.containsKey("list")){
            error("True Statement Drawer is missing list argument")
        }
        return createStmtDrawer(trueStmtDrawerOpt.getOrDefault(CONSTANTS.STMTDRAWER_TYPE, "whitelist" as Any).toString(),
                trueStmtDrawerOpt["list"] as Collection<String>, seed, model, minPropOcc, maxPropertyLimit
            )
    }

    /**
     * Creates a false statement drawer which can randomly generate wrong statements from a list.
     * The list is either a whitelist or a blacklist, depending on what was stated in the configuration under stmtDrawerType
     *
     * @param seed the seed to use for any random activity which will be included in the drawer
     * @param model the model to retrieve the wrong/false statements against
     * @param minPropOcc the minimum a property has to occur
     * @param maxPropertyLimit the maximum a property is allowed to be retrieved.
     * @return A Statement Drawer which returns wrong/false facts
     */
    fun createFalseStmtDrawer(seed: Long, model: Model, minPropOcc: Int, maxPropertyLimit: Int  ): StmtDrawer {
        if(!falseStmtDrawerOpt.containsKey("list")){
            error("False Statement Drawer is missing list argument")
        }
        return createStmtDrawer(falseStmtDrawerOpt.getOrDefault(CONSTANTS.STMTDRAWER_TYPE, "whitelist" as Any).toString(),
            falseStmtDrawerOpt["list"] as Collection<String>, seed, model, minPropOcc, maxPropertyLimit
        )
    }

}

class Configurations(){
    var configurations: List<Configuration> = emptyList()
}

/**
 * Factory class to create Configurations holding the relevant information for Glisten to execute a benchmark
 */
object ConfigurationFactory{

    /**
     * Reads the Configurations from the given file, and retrieves the Configuration where the name attribute is equals to the benchmarkName parameter
     *
     * If either the file doesn't exists or cannot be loaded or the configurations file doesn't hold a configuration with the given name throws a
     * ConfigurationLoadException
     *
     *
     * @param configurationsFile The File name containing all configurations
     * @param benchmarkName The name of the benchmark to get the configuration for
     * @return the
     */
    @kotlin.jvm.Throws(ConfigurationLoadException::class)
    fun findCorrectConfiguration(configurationsFile: String, benchmarkName: String): Configuration{
        //read yaml configuration
        try {
            val conf = create(File(configurationsFile))
            //get correct configuration
            conf.configurations.forEach {
                if (it.name == benchmarkName) {
                    return it
                }
            }
        }catch(e: IOException){
            e.printStackTrace()
        }
        System.err.println("Couldn't find benchmark Config with name {}".format(benchmarkName))
        throw ConfigurationLoadException("Couldn't find Configuration")
    }


    /**
     * Creates a Configurations from a given File.
     *
     *
     * @param config The File containing the configurations
     * @return The Configurations object containing all configurations.
     * @throws IOException if the file couldn't be read or doesn't exists
     *
     */
    @Throws(IOException::class)
    fun create(config: File): Configurations {
        if (config.name.endsWith(".yml") || config.name.endsWith(".yaml")) {
            return parse(config, YAMLFactory())
        } else if (config.name.endsWith(".json")) {
            return parse(config, JsonFactory())
        }
        return Configurations()
    }

    /**
     * Will use an ObjectMapper to parse the configuration file and a JSONFactory
     */
    @Throws(IOException::class)
    private fun parse(config: File, factory: JsonFactory) : Configurations {
        val mapper = ObjectMapper(factory)
        return mapper.readValue(config, Configurations::class.java)
    }

}