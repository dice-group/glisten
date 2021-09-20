package org.dice_group.glisten.core.config

import com.fasterxml.jackson.annotation.JsonProperty
import org.dice_group.glisten.core.scorer.Scorer
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.apache.jena.rdf.model.Model
import org.dice_group.glisten.core.ConfigurationLoadException
import org.dice_group.glisten.core.task.drawer.BlockListDrawer
import org.dice_group.glisten.core.task.drawer.StmtDrawer
import org.dice_group.glisten.core.task.drawer.AllowListDrawer
import java.io.File
import java.io.IOException
import java.util.*

/**
 * ## Description
 *
 * The Configuration class containing the
 *
 * * benchmark Name
 * * the list of sources
 * * the url link to the zip file containing all targets
 * * the url link to the zip file containing all linked datasets
 * * the namespaces to use in the [Scorer] algorithm
 * * The true anf false statement drawer options
 *
 * The Configuration shall be created using the [ConfigurationFactory.findCorrectConfiguration] method
 * providing a direct benchmarkName.
 *
 * Further on the Configuration creates the actual true statement drawer and respectively the false statement drawer by
 * calling the [createTrueStmtDrawer] and resp. [createFalseStmtDrawer] methods.
 *
 * The map for the [trueStmtDrawerOpt] and resp [falseStmtDrawerOpt] should include the following two key, value pairs
 *
 * * [CONSTANTS.STMTDRAWER_TYPE] - which defines if to use an [AllowListDrawer] (put in the string `allowlist`) or a [BlockListDrawer] (use any other string)
 * * "list" - containing a [Collection] object which contains the string representation of allowed or blocked properties.
 *
 * ```
 * ```
 * ## Example
 *
 * ```kotlin
 * val config = Configuration()
 * config.name = "MyBenchmark"
 * config.sources = listOf("file:///path/to/source1.nt","file:///path/to/source2.nt")
 *
 * //these can be locally using file:/// or remote using http[s]://
 * config.targetUrlZip = "file:///path/to/targets.zip"
 * config.linksUrlZip = "file:///path/to/linked_datasets.zip"
 *
 * config.trueStmtDrawerOpt = mapOf(
 *              Pair(CONSTANTS.STMTDRAWER_TYPE, "AllowList"),
 *              Pair("list", listOf(
 *                  "http://example.com/property/Allowed1",
 *                  "http://example.com/property/Allowed2"
 *              )))
 * config.falseStmtDrawerOpt = mapOf(
 *              Pair(CONSTANTS.STMTDRAWER_TYPE, "BlockList"),
 *              Pair("list", listOf(
 *                  "http://example.com/property/Blocked1",
 *                  "http://example.com/property/Blocked2"
 *              )))
 *
 * // load your source model , we will use the first source model of the config here, normally you want to loop that
 *
 * val model = RDFDataMgr.loadModel(config.sources[0], Lang.NT)
 *
 * val trueStmtDrawer = config.createTrueStmtDrawer(seed = 123, model = model, minPropOcc = 1, maxPropertyLimit = 10)
 * val falseStmtDrawer = config.createTrueStmtDrawer(seed = 123, model = model, minPropOcc = 1, maxPropertyLimit = 10)
 * ```
 *
 * @see ConfigurationFactory
 */
class Configuration{
    lateinit var name: String
    lateinit var sources: List<String>
    lateinit var targetUrlZip: String
    lateinit var linksUrlZip: String
    lateinit var trueStmtDrawerOpt: Map<String, Any>
    lateinit var falseStmtDrawerOpt: Map<String, Any>

    var namespaces: List<String> = emptyList()

    private fun createStmtDrawer(type: String, list: Collection<String>, seed: Long, model: Model, minPropOcc: Int, maxPropertyLimit: Int  ): StmtDrawer {
        //Add your new statement drawer type here to the `when` clause
        return when{
            type.lowercase(Locale.getDefault()) == "allowlist" ->
                AllowListDrawer(list, seed, model, minPropOcc, maxPropertyLimit)
            else ->
                BlockListDrawer(list, seed, model, minPropOcc, maxPropertyLimit)
        }
    }

    /**
     * Creates a true statement drawer which can randomly generate true statements from a list.
     * The list is either a allowlist or a blocklist, depending on what was stated in the configuration under stmtDrawerType
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
        return createStmtDrawer(trueStmtDrawerOpt.getOrDefault(CONSTANTS.STMTDRAWER_TYPE, "allowlist" as Any).toString(),
                trueStmtDrawerOpt["list"] as Collection<String>, seed, model, minPropOcc, maxPropertyLimit
            )
    }

    /**
     * Creates a false statement drawer which can randomly generate wrong statements from a list.
     * The list is either a allowlist or a blocklist, depending on what was stated in the configuration under stmtDrawerType
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
        return createStmtDrawer(falseStmtDrawerOpt.getOrDefault(CONSTANTS.STMTDRAWER_TYPE, "allowlist" as Any).toString(),
            falseStmtDrawerOpt["list"] as Collection<String>, seed, model, minPropOcc, maxPropertyLimit
        )
    }


    override fun toString(): String {
        return "$name : [$sources, $linksUrlZip, $targetUrlZip, $trueStmtDrawerOpt, $falseStmtDrawerOpt, $namespaces] "
    }


    override fun equals(other: Any?): Boolean {
        if(other is Configuration){
            var check = other.name == name
            check = check && (other.sources == sources)
            check = check && (other.linksUrlZip == linksUrlZip)
            check = check && (other.targetUrlZip == targetUrlZip)
            check = check && (other.falseStmtDrawerOpt == falseStmtDrawerOpt)
            check = check && (other.trueStmtDrawerOpt == trueStmtDrawerOpt)
            check = check && (other.namespaces == namespaces)
            return check
        }
        return false
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + sources.hashCode()
        result = 31 * result + targetUrlZip.hashCode()
        result = 31 * result + linksUrlZip.hashCode()
        result = 31 * result + trueStmtDrawerOpt.hashCode()
        result = 31 * result + falseStmtDrawerOpt.hashCode()
        result = 31 * result + namespaces.hashCode()
        return result
    }

}

/**
 * Wrapper class which contains several [Configuration]s
 */
class Configurations{
    var configurations: List<Configuration> = emptyList()
}

/**
 * Factory class to create Configurations holding the relevant information for Glisten to execute a benchmark
 */
object ConfigurationFactory{

    /**
     * ## Description
     *
     * Reads the [Configurations] from the given file, and retrieves the [Configuration] where the name attribute is equals to the benchmarkName parameter
     *
     * If either the file doesn't exists or cannot be loaded or the configurations file doesn't hold a configuration with the given name throws a
     * [ConfigurationLoadException]
     *
     * ## Example
     *
     * Assuming our file is called "config.yaml" and looks similar to this
     *
     * ```yaml
     * configurations:
     *  - name: "myBenchmark"
     *    linksUrlZip: "file:///path/to/links.zip"
     *    targetUrlZip: "file:///path/to/targets.zip"
     *    sources:
     *    - "file:///path/to/sources/source1.nt"
     *    trueStmtDrawerOpt:
     *      stmtDrawerType: "allowlist"
     *      list:
     *        - "http://dbpedia.org/ontology/mythology"
     *    falseStmtDrawerOpt:
     *      stmtDrawerType: "allowlist"
     *      list:
     *        - "http://dbpedia.org/ontology/mythology"
     *    namespaces:
     *      - "http://dbpedia.org/ontology/"
     * ```
     *
     * Then the following code will yield the configuration with the name attribute myBenchmark
     *
     * ```kotlin
     * // the config file will get the configuration with the name 'myBenchmark'
     * val config = ConfigurationFactory.findCorrectConfiguration("config.yaml", "myBenchmark")
     *
     * assertEquals("myBenchmark", config.name)
     * assertEquals("file:///path/to/links.zip", config.links.zip)
     * //and so on.
     * ```
     *
     * @param configurationsFile The File name containing all configurations
     * @param benchmarkName The name of the benchmark to get the configuration for
     * @return the configuration mapped to the benchmarkName
     *
     * @throws ConfigurationLoadException if the configuration cannot be found, loaded or doesn't map correctly to the [Configuration] object
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
            System.err.println("Couldn't find benchmark Config with name {}".format(benchmarkName))
        }catch (e: IOException){
            e.printStackTrace()
            System.err.println("Couldn't find config file $configurationsFile")
        }catch (e: JsonParseException){
            System.err.println("Config file is not valid json/yaml")
            e.printStackTrace()
        }catch (e: JsonMappingException){
            System.err.println("Config file cannot be mapped to the Configuration objects")
            e.printStackTrace()
        }
        throw ConfigurationLoadException("Configuration couldn't be loaded properly")
    }


    /**
     * Creates a Configurations from a given File.
     *
     *
     * @param config The File containing the configurations
     * @return The Configurations object containing all configurations.
     * @throws IOException if the file couldn't be read or doesn't exists
     * @throws JsonParseException If the file is not in json or Yaml format
     * @throws JsonMappingException If the json/yaml file cannot be mapped to the configuration
     */
    @Throws(IOException::class, JsonParseException::class, JsonMappingException::class)
    fun create(config: File): Configurations {
        if (config.name.endsWith(".yml") || config.name.endsWith(".yaml")) {
            return parse(config, YAMLFactory())
        } else if (config.name.endsWith(".json")) {
            return parse(config, JsonFactory())
        }
        throw ConfigurationLoadException("Couldn't guess if yaml or json from extension.")
    }

    /**
     * Will use an ObjectMapper to parse the configuration file and a JSONFactory
     */
    @Throws(IOException::class, JsonParseException::class, JsonMappingException::class)
    private fun parse(config: File, factory: JsonFactory) : Configurations {
        val mapper = ObjectMapper(factory)
        return mapper.readValue(config, Configurations::class.java)
    }

}