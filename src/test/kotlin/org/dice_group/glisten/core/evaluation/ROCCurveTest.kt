package org.dice_group.glisten.core.evaluation

import org.junit.jupiter.api.Order
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

class ROCCurveTest {

    @ParameterizedTest(name = "given \"{0}\", create the correct ROCCurve \"{3}\"")
    @MethodSource("directions")
    @Order(0)
    fun `given a set of directions, create the correct ROCCurve`(
        directions: List<DIRECTION>,
        trueStmts: Int,
        falseStmts: Int,
        expected: ROCCurve
    ){
        val roc = ROCCurve(trueStmts, falseStmts)
        directions.forEach{
            when(it){
                DIRECTION.UP -> roc.addUp()
                DIRECTION.RIGHT -> roc.addRight()
            }
        }
        assertEquals(expected, roc)
    }

    @ParameterizedTest(name = "given \"{0}\", calculate the correct AUC\"{1}\"")
    @MethodSource("rocCurves")
    @Order(1)
    fun `given a roc curve, calculate the AUC correctly`(
        rocCurve: ROCCurve,
        expectedAUC: Double
        ){
        assertEquals(expectedAUC, rocCurve.calculateAUC())
    }

    companion object  {

        fun createROCFromPoints(points: List<Point>, trueStmts: Int, falseStmts: Int): ROCCurve{
            val ret = ROCCurve(trueStmts, falseStmts)
            points.forEach{
                ret.addPoint(it)
            }
            return ret
        }

        @JvmStatic
        fun rocCurves() = Stream.of(
            Arguments.of(createROCFromPoints(listOf(
                Point(0.0, 1.0), Point(1.0, 1.0)), 2,2),
                1.0
            ),
            //only ups
            Arguments.of(createROCFromPoints(listOf(
                    Point(0.0, 1.0), Point(1.0, 1.0)), 2,0),
                1.0
            ),
            Arguments.of(createROCFromPoints(listOf(
                Point(0.0, 1.0)), 1,0),
                1.0
            ),
            //only rights
            Arguments.of(createROCFromPoints(listOf(
                    Point(1.0, 0.0), Point(1.0, 1.0)), 0,2),
                0.0
            ),
            Arguments.of(createROCFromPoints(listOf(
                Point(1.0, 0.0)), 0,1),
                0.0
            ),
            Arguments.of(createROCFromPoints(listOf(Point(0.0, 0.5),
                    Point(1.0, 0.5), Point(1.0, 1.0)), 2,2),
                0.5
            ),
        )

        @JvmStatic
        fun directions() = Stream.of(
            //correct ones
            //same size, rectangle
            Arguments.of(listOf(DIRECTION.UP, DIRECTION.UP, DIRECTION.RIGHT, DIRECTION.RIGHT), 2, 2,
                    createROCFromPoints(listOf(
                        Point(0.0, 1.0), Point(1.0, 1.0)), 2, 2)
                ),
            //same size, no rectangle
            Arguments.of(listOf(DIRECTION.UP, DIRECTION.RIGHT, DIRECTION.RIGHT, DIRECTION.UP), 2, 2,
                createROCFromPoints(listOf(Point(0.0, 0.5),
                    Point(1.0, 0.5), Point(1.0, 1.0)), 2,2)
            ),
            //different size
            Arguments.of(listOf(DIRECTION.UP, DIRECTION.RIGHT, DIRECTION.UP), 2, 1,
                createROCFromPoints(listOf(Point(0.0, 0.5), Point(1.0, 0.5),
                    Point(1.0, 1.0)), 2,1)
            ),
            //different size
            Arguments.of(listOf(DIRECTION.UP, DIRECTION.RIGHT, DIRECTION.RIGHT), 1, 2,
                createROCFromPoints(listOf(Point(0.0, 1.0),
                    Point(1.0, 1.0)), 1,2)
            ),
            //wrong size (truestmts, false stmts) // we expect to end up at 1/1
            Arguments.of(listOf(DIRECTION.UP, DIRECTION.UP, DIRECTION.RIGHT, DIRECTION.RIGHT), 1, 1,
                createROCFromPoints(listOf(
                    Point(0.0, 1.0), Point(1.0, 1.0)), 1,1)
            ),
            //only ups
            Arguments.of(listOf(DIRECTION.UP, DIRECTION.UP), 2, 0,
                createROCFromPoints(listOf(Point(0.0, 1.0)), 2,0)
            ),
            //only rights
            Arguments.of(listOf(DIRECTION.RIGHT, DIRECTION.RIGHT), 0, 2,
                createROCFromPoints(listOf(Point(1.0, 0.0)), 0,2)
            )
        )
    }

}