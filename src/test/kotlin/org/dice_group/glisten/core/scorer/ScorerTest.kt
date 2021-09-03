package org.dice_group.glisten.core.scorer

import org.apache.jena.rdf.model.Statement
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

class ScorerTest {

    @ParameterizedTest(name = "given the order \"{0}\" calculate the correct score \"{1}\"")
    @MethodSource("rawScores")
    fun `given an order of scores, calculate the correct auc`(
        scores : List<Pair<Double, Double>>,
        expectedScore : Double
    ){
        val scorer = MockScorer()
        assertEquals(expectedScore, scorer.getAUC(scores), "Score wasn't calculated correctly.")
    }

    companion object {
        @JvmStatic
        fun rawScores(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf(Pair(1.0, 1.0), Pair(1.0, 0.8), Pair(1.0, 0.0), Pair(-1.0, -0.5), Pair(-1.0, -0.8)), 1.0),
            Arguments.of(listOf(Pair(-1.0, 1.0), Pair(-1.0, 0.8), Pair(-1.0, 0.0), Pair(1.0, -0.5), Pair(1.0, -0.8)), 0.0),
            Arguments.of(listOf(Pair(1.0, 1.0), Pair(-1.0, 0.8), Pair(1.0, 0.0), Pair(-1.0, -0.5)), 0.75)
        )
    }

}

class MockScorer : Scorer(emptyList()){

    override fun getScores(endpoint: String, facts: List<Pair<Statement, Double>>): MutableList<Pair<Double, Double>> {
        return mutableListOf()
    }

}