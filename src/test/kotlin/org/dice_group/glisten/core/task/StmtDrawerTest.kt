package org.dice_group.glisten.core.task

import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.dice_group.glisten.core.task.drawer.AllowListDrawer
import java.io.File
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class StmtDrawerTest {

    private val model = RDFDataMgr.loadModel(File("src/test/resources/models/drawer.nt").toURI().toString(),
                                Lang.NT
        )

    //1. same seed, same facts
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
    fun `given different seeds, the facts should be different`(){
        //1. check if the two seeds have different random ints, otherwise increase one seed until
        val seed1 = 1L
        var seed2 = 2L
        while(Random(seed1).nextInt(20) == Random(seed2).nextInt(20)){
            seed2++
        }
        //create drawers with different seeds, but otherwise same structure
        val drawer1 = AllowListDrawer(listOf("http://example.com/1"), seed1, model, 1, 10)
        val drawer2 = AllowListDrawer(listOf("http://example.com/1"), seed2, model, 1, 10)
        drawer1.init()
        drawer2.init()
        val stmt1 = drawer1.drawRandomStmt()
        val stmt2 = drawer2.drawRandomStmt()
        assertNotEquals()
    }

    //3. allowlist -> in list

    //6. blocklist -> not in list



    //4. min prop occ
    fun `given a minimum of property occurrences, only these should be yield`(){

    }

    //5. max limit
    fun `given a maximum property limit, no property should exceed that limit`(){

    }

}