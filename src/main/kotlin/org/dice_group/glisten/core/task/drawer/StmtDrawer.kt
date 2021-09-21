package org.dice_group.glisten.core.task.drawer

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.rdf.model.Statement
import kotlin.random.Random

/**
 * Abstract Statement Drawer class to use to retrieve facts
 *
 */
abstract class StmtDrawer(seed: Long, open val model : Model, val minPropOcc: Int, private val maxPropertyLimit: Int) {

    var stmtList: MutableList<Statement> = mutableListOf()
    val random = Random(seed)

    /**
     * ## Description
     *
     * Retrieves all Statements which fits the given constraints provided by the actual implementation of the [StmtDrawer]
     * as well as having at least a minimum of property occurrences ([minPropOcc]).
     *
     * If a property  occurs more than [maxPropertyLimit], than it will only draw the limited amount of [Statement]s randomly using the provided seed.
     *
     * ```
     * ```
     * ## Example
     *
     * For this Example we will use the [AllowListDrawer], however any Implementation of the [StmtDrawer] will work
     *
     * ```kotlin
     *  //use any implementation of the StmtDrawer here
     * val drawer = AllowListDrawer(allowList = listOf("http://allowed/1"),
     *                              seed = 123L,
     *                              model = myModel,
     *                              minPropOcc = 10,
     *                              maxPropertyLimit = 20
     *                              )
     *
     * drawer.init()
     * val stmts = drawer.getStmts()
     * ```
     *
     * @return List of Statements fitting the constraints given by the statement drawers
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
     *
     * @return true if there are still [Statement]s left, false otherwise
     */
    fun hasStatement(): Boolean{
        return stmtList.isNotEmpty()
    }

    /**
     * Retrieves a random statement from the statement list generated in [init]
     *
     * @return A randomly chosen statement.
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
     *
     * @param property The property to retrieve statements for
     * @return The list of Statements fitting the provided constraints.
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