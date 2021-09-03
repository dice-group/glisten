package org.dice_group.glisten.core.task

import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.dice_group.glisten.core.task.drawer.AllowListDrawer
import org.dice_group.glisten.core.task.drawer.BlockListDrawer
import org.dice_group.glisten.core.task.drawer.StmtDrawer
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class StmtDrawerTest {

    private val model = RDFDataMgr.loadModel(File("src/test/resources/models/drawer.nt").toURI().toString(),
                                Lang.NT
        )


    //1. same seed, same facts
    @Test
    fun `given a seed, the facts should be identical from two drawers`(){
        val seed = 123L
        val drawer1 = AllowListDrawer(listOf("http://example.com/1"), seed, model, 1, 10)
        val drawer2 = AllowListDrawer(listOf("http://example.com/1"), seed, model, 1, 10)
        drawer1.init()
        val drawer1Statements = mutableListOf<Statement>()
        for (i in 0 until 10){
           drawer1Statements.add(drawer1.drawRandomStmt())
        }
        drawer2.init()
        val drawer2Statements = mutableListOf<Statement>()
        for (i in 0 until 10){
            drawer2Statements.add(drawer2.drawRandomStmt())
        }

        assertEquals(drawer1Statements, drawer2Statements, "Same Seed doesn't provide same statements.")
    }

    //2. different seeds, that yields different ints - > different facts
    @Test
    fun `given different seeds, the facts should be different`(){
        //1. check if the two seeds have different random ints, otherwise increase one seed until
        val seed1 = 1L
        var seed2 = 2L

        //we need the actual size of the properties to check if the Randoms really draw a different integer
        val size = model.listStatements(null, ResourceFactory.createProperty("http://example.com/1"), null as RDFNode?).toList().size
        while(Random(seed1).nextInt(size) == Random(seed2).nextInt(size)){
            seed2++
        }
        //create drawers with different seeds, but otherwise same structure
        val drawer1 = AllowListDrawer(listOf("http://example.com/1"), seed1, model, 1, 10)
        val drawer2 = AllowListDrawer(listOf("http://example.com/1"), seed2, model, 1, 10)
        drawer1.init()
        drawer2.init()

        //get the statements of both drawers (they should not be the same)
        val stmt1 = drawer1.drawRandomStmt()
        val stmt2 = drawer2.drawRandomStmt()
        assertNotEquals(stmt1, stmt2)
    }

    //3. allowlist -> in list
    @Test
    fun `given an allow list, only retrieve properties in the list`(){
        val drawer = AllowListDrawer(listOf("http://example.com/1", "http://example.com/3"), 123L, model, 1, 10)
        drawer.init()
        val distinctProperties =  drawer.stmtList.map { it.predicate.toString() }.toSet()
        assertEquals(setOf("http://example.com/1", "http://example.com/3"), distinctProperties)
    }
    //4. blocklist -> not in list
    @Test
    fun `given a block list, only retrieve properties not in the list`(){
        val drawer = BlockListDrawer(listOf("http://example.com/1", "http://example.com/3"), 123L, model, 1, 10)
        drawer.init()
        val distinctProperties =  drawer.stmtList.map { it.predicate.toString() }.toSet()
        //should only contain http://example.com/2 as property
        assertEquals(setOf("http://example.com/2"), distinctProperties)
    }

    //5. min prop occ
    @Test
    fun `given a minimum of property occurrences, only these should be yield`(){
        var drawer : StmtDrawer = AllowListDrawer(listOf("http://example.com/1", "http://example.com/2", "http://example.com/3"), 123L, model, 5, 10)
        drawer.init()
        var distinctProperties =  drawer.stmtList.map { it.predicate.toString() }.toSet()
        //should only contain http://example.com/1 and http://example.com/2
        assertEquals(setOf("http://example.com/1", "http://example.com/2"), distinctProperties)

        drawer = BlockListDrawer(listOf( "http://example.com/2"), 123L, model, 5, 10)
        drawer.init()
        distinctProperties =  drawer.stmtList.map { it.predicate.toString() }.toSet()
        //should only contain http://example.com/1
        assertEquals(setOf("http://example.com/1"), distinctProperties)
    }

    //6. max limit
    @Test
    fun `given a maximum property limit, no property should exceed that limit`(){
        var drawer : StmtDrawer = AllowListDrawer(listOf("http://example.com/1", "http://example.com/2", "http://example.com/3"), 123L, model, 1, 2)
        drawer.init()
        var distinctProperties =  drawer.stmtList.map { it.predicate.toString() }.toList()

        //should only contain 2 of each
        assertEquals(2, distinctProperties.filter { it == "http://example.com/1" }.count(), "Max Property Limit didn't work")
        assertEquals(2, distinctProperties.filter { it == "http://example.com/2" }.count(), "Max Property Limit didn't work")
        assertEquals(2, distinctProperties.filter { it == "http://example.com/3" }.count(), "Max Property Limit didn't work")

        drawer = BlockListDrawer(listOf("http://example.com/1"), 123L, model, 1, 2)
        drawer.init()
        distinctProperties =  drawer.stmtList.map { it.predicate.toString() }.toList()

        //should only contain 2 of each
        assertEquals(2, distinctProperties.filter { it == "http://example.com/2" }.count(), "Max Property Limit didn't work")
        assertEquals(2, distinctProperties.filter { it == "http://example.com/3" }.count(), "Max Property Limit didn't work")

    }

}