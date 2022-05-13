package org.dice_group.glisten.core.scorer

import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.dice_group.glisten.core.task.drawer.StmtDrawer
import org.dice_group.glisten.core.task.drawer.AllowListDrawer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream
import kotlin.test.assertEquals

class FactGeneratorTest {

    @ParameterizedTest(name = "given \"{0}\" true statements, \"{1}\" false statements to generate, generate the correct distinct amount")
    @MethodSource("generateStatements")
    fun `given a number of true and false statements to generate, generate the correct distinct amount`(
        noOfTrueStmts: Int, noOfFalseStmts: Int,
        trueStmtDrawer: StmtDrawer, falseStmtDrawer: StmtDrawer
    ){
        val facts = FactGenerator.createFacts(123L, noOfTrueStmts, noOfFalseStmts, trueStmtDrawer, falseStmtDrawer)
        assertEquals(noOfFalseStmts+noOfTrueStmts, facts.size, "The amount of generated facts is not correct.")
        assertEquals(noOfFalseStmts+noOfTrueStmts, facts.toSet().size, "The generated facts are not distinct.")
        assertEquals(noOfFalseStmts, facts.filter { it.second == -1.0 }.size, "The amount of false generated facts are wrong.")
        assertEquals(noOfTrueStmts, facts.filter { it.second == 1.0 }.size, "The amount of true generated facts are wrong.")

        //check if sorting is correct for debugging, this checks the correct amount of true (resp false) generated facts also, but this way it can be deleted,
        // if the sorting will be removed
        for (i in 0 until noOfFalseStmts){
            assertEquals(-1.0, facts[i].second, "The sorting of the facts is not correct.")
        }
        //This is also pretty unnecessary, as the previous tests would automatically make this true.
        for (i in 0 until noOfTrueStmts){
            assertEquals(1.0, facts[i+noOfFalseStmts].second, "The sorting of the facts is not correct.")
        }

    }

    companion object {

        private val model = RDFDataMgr.loadModel(File("src/test/resources/models/facts.nt").toURI().toString(), Lang.NT)
        private val trueStmtDrawer = AllowListDrawer(listOf("http://example.com/1"), 123L, model ,1, 10)
        private val falseStmtDrawer = AllowListDrawer(listOf("http://example.com/2"), 123L, model ,1, 10)


        @JvmStatic
        fun generateStatements() = Stream.of(
            Arguments.of(5, 5, trueStmtDrawer, falseStmtDrawer),
            Arguments.of(3, 2, trueStmtDrawer, falseStmtDrawer),
            Arguments.of(2, 3, trueStmtDrawer, falseStmtDrawer),
            Arguments.of(5, 0, trueStmtDrawer, falseStmtDrawer),
            Arguments.of(0, 5, trueStmtDrawer, falseStmtDrawer),
            )
    }
}