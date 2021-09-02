package org.dice_group.glisten.core.config

import org.apache.jena.rdf.model.ModelFactory
import org.dice_group.glisten.core.ConfigurationLoadException
import org.dice_group.glisten.core.task.drawer.BlackListDrawer
import org.dice_group.glisten.core.task.drawer.WhiteListDrawer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.io.IOException
import java.util.stream.Stream
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs


class ConfigurationTest {


    @Test
    @Order(0)
    fun `load all configurations and check if size eq 3 and benchmark1, benchmark2 and actualBenchmark are the names of the benchmarks`(){
        assertThrows<ConfigurationLoadException>{  ConfigurationFactory.create(File("doesntexists")) }
        assertThrows<IOException>{  ConfigurationFactory.create(File("doesntexists.yaml")) }

        val confs = ConfigurationFactory.create(File("src/test/resources/configs/simple.yaml"))
        assertEquals(3, confs.configurations.size, "Configuration size is not correct")
        val names = confs.configurations.map{ it.name }
        //check all names
        assertContains(names, "benchmark1")
        assertContains(names, "benchmark2")
        assertContains(names, "benchmark3")
    }

    @ParameterizedTest(name = "given \"{0}\", it should generate the correct statement drawer type \"{1}\", with the correct list \"{2}\"")
    @MethodSource("statementDrawers")
    @Order(2)
    fun `given a configuration, it should generate the correct statement drawers`(
        conf: Configuration,
        trueTypeIsWhitelist: Boolean,
        falseTypeIsWhitelist: Boolean
    ){
        val trueStmtDrawer = conf.createTrueStmtDrawer(123, ModelFactory.createDefaultModel(), 12, 12)
        val falseStmtDrawer = conf.createFalseStmtDrawer(123, ModelFactory.createDefaultModel(), 12, 12)
        // type correct
        if(trueTypeIsWhitelist){
            assertIs<WhiteListDrawer>(trueStmtDrawer)
        }else{
            assertIs<BlackListDrawer>(trueStmtDrawer)
        }
        if(falseTypeIsWhitelist){
            assertIs<WhiteListDrawer>(falseStmtDrawer)
        }else{
            assertIs<BlackListDrawer>(falseStmtDrawer)
        }
        // the list is correctly read

    }

    @ParameterizedTest(name = "given \"{0}\" and \"{1}\" it should provide \"{2}\"")
    @MethodSource("configurationNames")
    @Order(1)
    fun `given a configuration file and a benchmarkName, it should provide the correct Configuration`(
        configFile: String,
        benchmarkName: String,
        expected: Configuration,
        exists: Boolean
    ) {
        if(exists){
            val actual = ConfigurationFactory.findCorrectConfiguration(configFile, benchmarkName)
            assertEquals(expected, actual)
        }
        else{
           assertThrows<ConfigurationLoadException>{ ConfigurationFactory.findCorrectConfiguration(configFile, benchmarkName)}
        }

    }

    companion object {

        fun createConfiguration(name: String) : Configuration{
            val conf = Configuration()
            conf.name=name
            conf.linksUrlZip = "https://${name}_links.zip"
            conf.targetUrlZip = "https://${name}_targets.zip"
            conf.sources = arrayListOf(name+"_1", name+"_2", name+"_3")
            conf.falseStmtDrawerOpt = mapOf(Pair("stmtDrawerType", "whitelist"), Pair("list", arrayListOf(name)))
            conf.trueStmtDrawerOpt  = mapOf(Pair("stmtDrawerType", "blacklist"), Pair("list", arrayListOf(name)))
            return conf
        }

        @JvmStatic
        fun statementDrawers() = Stream.of(
            Arguments.of(ConfigurationFactory.findCorrectConfiguration("src/test/resources/configs/simple.yaml","benchmark1"), false, true,
                arrayListOf("benchmark1"), arrayListOf("benchmark1")
                ),
            Arguments.of(ConfigurationFactory.findCorrectConfiguration("src/test/resources/configs/simple.yaml","benchmark2"), false, true,
                arrayListOf("benchmark2"), arrayListOf("benchmark2")
            ),
            Arguments.of(ConfigurationFactory.findCorrectConfiguration("src/test/resources/configs/simple.yaml","benchmark3"), true, false,
                arrayListOf("benchmark3", "3kramhcneb"), arrayListOf("benchmark3_false", "eslaf_3kramhcneb")
            )
        )

        @JvmStatic
        fun configurationNames() = Stream.of(
            Arguments.of("src/test/resources/configs/simple.yaml", "benchmark1", createConfiguration("benchmark1"), true),
            Arguments.of("src/test/resources/configs/simple.yaml", "benchmark2", createConfiguration("benchmark2"), true),
            //shouldn't exists
            Arguments.of("src/test/resources/configs/simple.yaml", "doesntexists", createConfiguration("doesntexists"), false)
        )
    }

}