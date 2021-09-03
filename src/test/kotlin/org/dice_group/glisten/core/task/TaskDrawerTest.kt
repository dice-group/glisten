package org.dice_group.glisten.core.task

import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.dice_group.glisten.core.task.drawer.AllowListDrawer
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskDrawerTest {


    private val model = RDFDataMgr.loadModel(
        File("src/test/resources/models/drawer.nt").toURI().toString(),
        Lang.NT
    )

    fun generateDrawer(): TaskDrawer{
        val trueDrawer = AllowListDrawer(listOf("http://example.com/1", "http://example.com/2"),
            123L,
            model,
            1,
            10
        )
        val falseDrawer = AllowListDrawer(listOf("http://example.com/1", "http://example.com/2"),
            123L,
            model,
            1,
            10
        )
        trueDrawer.init()
        falseDrawer.init()
        return TaskDrawer(123L, 5, 5, trueDrawer, falseDrawer)
    }

    // all false should not exists, and only the object should be mutated, however both s,p,? and ?,?,o should exists or so?
    @Test
    fun `given a task drawer and a model, all false facts retrieved should not exists in that model`(){
        val drawer = generateDrawer()

        drawer.generateFalseStatements()
        //check for each false statement that, the fact doesn't exists in the model, however it is just mutated
        drawer.task.statements.filter { it.second == -1.0 }.forEach{
                (stmt, _) ->
            run {
                assertFalse(model.contains(stmt))
                assertTrue(model.listStatements(stmt.subject, stmt.predicate, null as RDFNode?).toList().size > 0, "Model should contain subject, predicate, ? triple. ")
                assertTrue(model.listStatements(null, null, stmt.`object`).toList().size > 0, "Model should contain object at least once. ")

            }
        }
    }

    // all trues should exists
    @Test
    fun `given a task drawer and a model, all true facts retrieved should exists in that model`(){
        val drawer = generateDrawer()

        drawer.generateTrueStatements()
        //check for each true statement, that the model contains that statement
        drawer.task.statements.filter { it.second == 1.0 }.forEach{
            (stmt, _) ->
            assertTrue(model.contains(stmt), "Model does not contain statement $stmt, but it should.")
        }
    }
}