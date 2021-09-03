package org.dice_group.glisten.core.task.drawer

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.rdf.model.Statement
import kotlin.random.Random

/**
 * Abstract Statement Drawer class to use to retrieve facts
 */
abstract class StmtDrawer(seed: Long, open val model : Model, private val minPropOcc: Int, private val maxPropertyLimit: Int) {

    var stmtList: MutableList<Statement> = mutableListOf()
    private val random = Random(seed)

    /**
     * Retrieve Statements that corresponds with the drawer type
     */
    abstract fun getStmts(): MutableList<Statement>

    /**
     * Initialize to retrieve statements
     */
    fun init(){
        stmtList = getStmts()
    }

    /**
     * checks if the statement list is empty or has at least one Statement
     */
    fun hasStatement(): Boolean{
        return stmtList.isNotEmpty()
    }

    /**
     * Retrieves a random statement from the statement list generated in init/getStmts
     */
    open fun drawRandomStmt(): Statement {
        val index = random.nextInt(stmtList.size)
        val stmt: Statement = stmtList[index]
        stmtList.removeAt(index)
        //Make a copy here to assure that we can delete the source Model afterwards to save memory.
        return ResourceFactory.createStatement(stmt.subject, stmt.predicate, stmt.`object`)
    }


    /**
     * Retrieves random statements (max = drawer.maxPropertyLimit) which occur at least drawer.minPropOcc
     * and returns them.
     */
    fun getStmts(property: String): Collection<Statement>{
        val stmts = model.listStatements(null, ResourceFactory.createProperty(property), null as RDFNode?).toSet()
        if(stmts.size<minPropOcc){
            return emptySet()
        }
        //get random statments by shuffeling and choosing [:limit]
        return stmts.shuffled(random).subList(0, maxPropertyLimit.coerceAtMost(stmts.size)).map { stmt -> ResourceFactory.createStatement(stmt.subject, stmt.predicate, stmt.`object`) }
    }


}