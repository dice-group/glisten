package org.dice_group.glisten.core.evaluation

import net.lingala.zip4j.exception.ZipException
import org.apache.commons.io.FileUtils
import org.apache.jena.rdf.model.Statement
import org.dice_group.glisten.core.config.CONSTANTS
import org.dice_group.glisten.core.config.Configuration
import org.dice_group.glisten.core.scorer.Scorer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.io.IOException
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreEvaluatorTest {

    //test init
    // 1. correct download and unzip correctly
    // 2. no download but IOException because wrong url
    // 3. download but not zip -> ZipException -> file should be removed
    @Test
    fun `given an evaluator and a correct zip file the init should download and extract the zip file`(){
        val conf = createMockConfig()
        conf.linksUrlZip = File("src/test/resources/zips/correct.zip").toURI().toString()
        val eval = CoreEvaluator(conf,"",MockupScorer(emptyList()))
        eval.linkedPath = UUID.randomUUID().toString()

        val zipDest = UUID.randomUUID().toString()
        eval.init(zipDest)

        val extractedFileNames = File(eval.linkedPath).listFiles()?.map { it.name }
        assertEquals(listOf("test.txt", "file2.nt"), extractedFileNames, "ExtractingZip didn't work and the folder ${eval.linkedPath} doesn't contain the files `test.txt` and `file2.nt` ")

        //cleanup
        FileUtils.deleteDirectory(File(eval.linkedPath))
        FileUtils.deleteDirectory(File(zipDest))
    }

    @Test
    fun `given an evaluator and a wrong zip file the init should download and extract the zip file`() {
        val conf = createMockConfig()
        conf.linksUrlZip = File("src/test/resources/zips/wrong.zip").toURI().toString()
        val eval = CoreEvaluator(conf, "", MockupScorer(emptyList()))
        eval.linkedPath = UUID.randomUUID().toString()

        val zipDest = UUID.randomUUID().toString()
        assertThrows<ZipException>{ eval.init(zipDest) }

        //cleanup
        FileUtils.deleteDirectory(File(eval.linkedPath))
        FileUtils.deleteDirectory(File(zipDest))
    }

    @Test
    fun `given an evaluator and a wrong url it should throw an IOException`() {
        val conf = createMockConfig()
        conf.linksUrlZip = File("src/test/resources/zips/doesntexists.zip").toURI().toString()
        val eval = CoreEvaluator(conf, "", MockupScorer(emptyList()))
        eval.linkedPath = UUID.randomUUID().toString()

        val zipDest = UUID.randomUUID().toString()
        assertThrows<IOException>{ eval.init(zipDest) }

        //cleanup
        FileUtils.deleteDirectory(File(eval.linkedPath))
        FileUtils.deleteDirectory(File(zipDest))
    }


    // get ROC check if correct ROC is created
    // MockupScorer -> no full functionality, just return some previous defined no.
    // use empty strings in recommendation to not trigger the load function. we test that in another test
    // 1. correct no. of recommendations checked
    // 2. sine
    @ParameterizedTest
    @MethodSource("createRecommendations")
    fun `given a set of facts and recommendations calculate the ROC curve`(
        conf: Configuration,
        baseline: Double,
        scorer: Scorer,
        expected: ROCCurve,
        recommendations: MutableList<Pair<String, Double>>
    ){
        val evaluator = CoreEvaluator(conf, "", scorer)
        val roc = evaluator.getBetterROC(baseline, "", emptyList(), recommendations)
        assertEquals(expected, roc, "ROCCurves are not identical.")
    }

    //TODO getAUC - this is basically the whole workflow
    // MockupScript -> check if correct links will be used
    // MockupScorer -> simply return some scores (no full functionality)
    // we can ignore basically everything here.
    // model can be empty, we provide the actual scores through the mockup scorer
    // so we just need to test the returning AUC and if the script is correctly set using a mockup script
    @ParameterizedTest
    @MethodSource("createAUCArguments")
    fun `given an evaluator and some recommendations the correct auc should be returned and the correct scripts should be executed`(
        recommendations: MutableList<Pair<String, Double>>,
        scorer: Scorer,
        expected : Double
    ){
        File("ABCDEFGH_THIS_SHOULDNTEXSISTS.txt").deleteOnExit()

        val evaluator = CoreEvaluator(createMockConfig(), "doesntmatter", scorer)
        evaluator.triplestoreLoaderScript = "src/test/resources/scripts/write.sh"
        //we need to have an actual source here
        val source = File("src/test/resources/models/aucModel.nt").toURI().toString()
        val auc = evaluator.getAUC(source, recommendations)
        assertEquals(expected, auc)

        //check if script was correctly executed
        val lines = FileUtils.readLines(File("ABCDEFGH_THIS_SHOULDNTEXSISTS.txt"))

        assertEquals("${source.substringBeforeLast("/").replace("file:", "")}/ ${source.substringAfterLast("/")}", lines[0])

        //check if the script got the correct targets links
        for (i in 1 until lines.size){

            val file = recommendations[i-1].first
            //and the path as linked path! but without the file: schema
            val path = "${File(evaluator.linkedPath).toURI().toString().replace("file:","")}/"
            //we have the link aucModel_TARGET
            assertEquals("$path aucModel_${file.substringAfterLast("/")}", lines[i])
        }
        File("ABCDEFGH_THIS_SHOULDNTEXSISTS.txt").delete()

    }


    companion object {

        @JvmStatic
        fun createAUCArguments() = Stream.of(
            Arguments.of(mutableListOf(Pair("file:///path/to/target1", 1.0), Pair("file:///path/to/target2", 0.8), Pair("file:///path/to/target3", -1.0)),
                //we have 4 as we need to include the baseline
                MockupScorer(listOf(0.1, 0.2, 0.3, 0.3))
                ,1.0),
            Arguments.of(mutableListOf(Pair("file:///path/to/target1_2", 1.0), Pair("file:///path/to/target2_2", 0.8), Pair("file:///path/to/target3_2", -1.0)),
                //the mockup scorer acts as it has the actual order of the recommendations, we do not need to change anything at the recommendation list
                MockupScorer(listOf(0.1, 0.1, 0.1, 0.2))
                ,0.0),
            Arguments.of(mutableListOf(Pair("file:///path/to/target1_3", 1.0), Pair("file:///path/to/target2_3", 0.8), Pair("file:///path/to/target3_3", -1.0)),
                //the mockup scorer acts as it has the actual order of the recommendations, we do not need to change anything at the recommendation list
                MockupScorer(listOf(0.1, 0.3, 0.3, 0.4))
                ,0.5)
        )

        private fun createMockConfig(): Configuration{
            val ret = Configuration()
            ret.falseStmtDrawerOpt = mapOf(Pair(CONSTANTS.STMTDRAWER_TYPE, "allowlist"), Pair("list", listOf("http://example.com/1>")))
            ret.trueStmtDrawerOpt = mapOf(Pair(CONSTANTS.STMTDRAWER_TYPE, "allowlist"), Pair("list", listOf("http://example.com/1>")))
            return ret
        }

        private fun createROCCurve(directions: List<DIRECTION>): ROCCurve{
            val upDirections = directions.count { it == DIRECTION.UP }
            val rightDirections = directions.count { it == DIRECTION.RIGHT }
            val roc = ROCCurve(upDirections, rightDirections)
            directions.forEach {
                when(it){
                    DIRECTION.UP -> roc.addUp()
                    DIRECTION.RIGHT -> roc.addRight()
                }
            }
            return roc
        }


        @JvmStatic
        fun createRecommendations() = Stream.of(
            Arguments.of(createMockConfig(),
                0.0,
                //all ups
                MockupScorer(listOf(0.1, 0.2, 0.3, 0.4, 0.5)),
                createROCCurve(listOf(DIRECTION.UP,DIRECTION.UP,DIRECTION.UP,DIRECTION.UP,DIRECTION.UP)),
                // we just need 5 pairs, the rest is irrelevant for this
                mutableListOf(Pair("", 1.0), Pair("", 1.0),Pair("", 1.0),Pair("", 1.0),Pair("", 1.0))
            ),
            Arguments.of(createMockConfig(),
                0.1,
                MockupScorer(listOf(0.1, 0.1, 0.1, 0.4, 0.5)),
                createROCCurve(listOf(DIRECTION.RIGHT,DIRECTION.RIGHT,DIRECTION.RIGHT,DIRECTION.UP,DIRECTION.UP)),
                // we just need 5 pairs, the rest is irrelevant for this
                mutableListOf(Pair("", 1.0), Pair("", 1.0),Pair("", 1.0),Pair("", 1.0),Pair("", 1.0))
            ),
            Arguments.of(createMockConfig(),
                0.05,
                MockupScorer(listOf(0.1, 0.2, 0.3, 0.3, 0.3)),
                createROCCurve(listOf(DIRECTION.UP,DIRECTION.UP,DIRECTION.UP,DIRECTION.RIGHT,DIRECTION.RIGHT)),
                // we just need 5 pairs, the rest is irrelevant for this
                mutableListOf(Pair("", 1.0), Pair("", 1.0),Pair("", 1.0),Pair("", 1.0),Pair("", 1.0))
            ),
            Arguments.of(createMockConfig(),
                0.0,
                MockupScorer(listOf(0.1, 0.2, 0.2, 0.3, 0.3)),
                createROCCurve(listOf(DIRECTION.UP,DIRECTION.UP,DIRECTION.RIGHT,DIRECTION.UP,DIRECTION.RIGHT)),
                // we just need 5 pairs, the rest is irrelevant for this
                mutableListOf(Pair("", 1.0), Pair("", 1.0),Pair("", 1.0),Pair("", 1.0),Pair("", 1.0))
            ),
            Arguments.of(createMockConfig(),
                0.0,
                MockupScorer(listOf(0.4, 0.2, 0.3, 0.4, 0.3)),
                createROCCurve(listOf(DIRECTION.UP,DIRECTION.RIGHT,DIRECTION.UP,DIRECTION.UP,DIRECTION.RIGHT)),
                // we just need 5 pairs, the rest is irrelevant for this
                mutableListOf(Pair("", 1.0), Pair("", 1.0),Pair("", 1.0),Pair("", 1.0),Pair("", 1.0))
            )
        )
    }
}

class MockupScorer(private val scores: List<Double>) : Scorer(emptyList()){

    private var counter=0

    override fun getScore(endpoint: String, facts: List<Pair<Statement, Double>>) : Double{
        assertTrue(scores.size>counter, "Not enough mockup scores were provided.")
        return scores[counter++]
    }


    override fun getScores(endpoint: String, facts: List<Pair<Statement, Double>>): MutableList<Pair<Double, Double>> {
        return mutableListOf()
    }

}