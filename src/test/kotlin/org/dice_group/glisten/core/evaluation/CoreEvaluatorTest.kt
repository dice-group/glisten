package org.dice_group.glisten.core.evaluation

import net.lingala.zip4j.exception.ZipException
import org.apache.commons.io.FileUtils
import org.apache.jena.rdf.model.Statement
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
    // correct normalizing (remove baseline)



    companion object {

        private fun createMockConfig(): Configuration{
            val ret = Configuration()
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