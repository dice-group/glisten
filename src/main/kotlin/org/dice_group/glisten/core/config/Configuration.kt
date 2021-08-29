package org.dice_group.glisten.core.config

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.apache.jena.rdf.model.Model
import org.dice_group.glisten.core.task.drawer.BlackListDrawer
import org.dice_group.glisten.core.task.drawer.StmtDrawer
import org.dice_group.glisten.core.task.drawer.WhiteListDrawer
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.system.exitProcess


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
    //(private val blackList: Collection<String>, private val seed: Int, private val model : Model, private val minPropOcc: Int, private val maxPropertyLimit: Int) : StmtDrawer(seed, model, minPropOcc, maxPropertyLimit
    private fun createStmtDrawer(type: String, list: Collection<String>, seed: Long, model: Model, minPropOcc: Int, maxPropertyLimit: Int  ): StmtDrawer {
        return if( type.lowercase(Locale.getDefault()) == "whitelist"){
            WhiteListDrawer(list, seed, model, minPropOcc, maxPropertyLimit)
        }else{
            BlackListDrawer(list, seed, model, minPropOcc, maxPropertyLimit)
        }
    }

    fun createTrueStmtDrawer(seed: Long, model: Model, minPropOcc: Int, maxPropertyLimit: Int  ): StmtDrawer {
        if(!trueStmtDrawerOpt.containsKey("list")){
            error("True Statement Drawer is missing list argument")

        }
        return createStmtDrawer(trueStmtDrawerOpt.getOrDefault(CONSTANTS.STMTDRAWER_TYPE, "whitelist" as Any).toString(),
                trueStmtDrawerOpt["list"] as Collection<String>, seed, model, minPropOcc, maxPropertyLimit
            )
    }

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

object ConfigurationFactory{

    fun findCorrectConfiguration(benchmarkName: String): Configuration{
        //read yaml configuration
        val conf = create(File(CONSTANTS.CONFIG_NAME))
        //get correct configuration
        conf?.configurations?.forEach{
            if(it.name == benchmarkName){
                return it
            }
        }
        System.err.println("Couldn't find benchmark Config with name {}".format(benchmarkName))
        exitProcess(1)
    }


    @Throws(IOException::class)
    fun create(config: File): Configurations {
        if (config.name.endsWith(".yml") || config.name.endsWith(".yaml")) {
            return parse(config, YAMLFactory())
        } else if (config.name.endsWith(".json")) {
            return parse(config, JsonFactory())
        }
        return Configurations()
    }

    @Throws(IOException::class)
    private fun parse(config: File, factory: JsonFactory) : Configurations {
        val mapper = ObjectMapper(factory)
        return mapper.readValue(config, Configurations::class.java)
    }

}