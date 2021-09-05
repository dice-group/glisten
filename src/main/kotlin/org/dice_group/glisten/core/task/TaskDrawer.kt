package org.dice_group.glisten.core.task

import org.apache.jena.rdf.model.*
import org.dice_group.glisten.core.task.drawer.StmtDrawer
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet


/**
 * Code is shamelessly stolen from sven and just converted to Kotlin and adjusted for glisten.
 * Hence, I didn't write any doc for it.
 */
class TaskDrawer(seed: Long, private val numberOfTrueStatements: Int, private val numberOfFalseStatements: Int, private val trueStmtDrawer: StmtDrawer, private val falseStmtDrawer: StmtDrawer ) {

    private val random = Random(seed)

    companion object{
        val LOGGER = LoggerFactory.getLogger(TaskDrawer::class.java)
    }
    private var stmtObjectMap: HashMap<String, Set<Resource>?>? = HashMap()

    val task = Task()

    fun generateFalseStatements() {
        if (numberOfFalseStatements > 0) {
            val falseStatements: Set<Statement> = retrieveFalseStatements()
            task.addFalseStatements(falseStatements)
            LOGGER.info(
                "False Statement Generation Requested: " + numberOfFalseStatements // + ", Listed by Drawer: " + falseStmtDrawer.getStmtList().size()
                    .toString() + ", Added to Task " + falseStatements.size
            )
        } else {
            LOGGER.info("No False Statements were generated")
        }
    }


    fun generateTrueStatements() {
        val trueStmts: Set<Statement> = retrieveTrueStatements()
        task.addTrueStatements(trueStmts)
        LOGGER.info(
            ("True Statement Generation Requested: " + numberOfTrueStatements // + ", Listed by Drawer: " + trueStmtDrawer.getStmtList().size()
                .toString() + ", Added to Task " + trueStmts.size)
        )
    }



    /**Tries to generate False Statements until the requested Number is reached,
     * or no further statements can be drawn at random.
     * @return Set of False Statement
     */
    private fun retrieveFalseStatements(): Set<Statement> {
        val stmts: MutableSet<Statement> = HashSet()
        while (stmts.size < numberOfFalseStatements && falseStmtDrawer.hasStatement()) {
            try {
                val stmt: Statement = falseStmtDrawer.drawRandomStmt()
                randomObjectReplacement(stmt, getObjectsOfProperty(stmt, falseStmtDrawer.model))
                    .let { stmts.add(it) }
            } catch (noObjectOfClass: IllegalArgumentException) {
                LOGGER.debug("no Object in Class to replace")
            } catch (noStmtCanBeDrawn: IndexOutOfBoundsException) {
                LOGGER.debug(noStmtCanBeDrawn.message)
            }
        }
        stmtObjectMap = null
        return stmts
    }


    /**@return List of Objects of this property in the model.
     * Excluding those which have the same subject as the provided statement.
     */
    private fun getObjectsOfProperty(stmt: Statement, model: Model): List<Resource?> {
        val objects: Set<Resource>? = getObjects(stmt.predicate, model)
        return withoutExistingStatements(stmt.subject, stmt.predicate, objects, model)
    }

    @Throws(java.lang.IllegalArgumentException::class)
    private fun randomObjectReplacement(stmt: Statement, replacer: List<Resource?>): Statement {
        val replacerObject = replacer[random.nextInt(replacer.size)]
        return ResourceFactory.createStatement(stmt.subject, stmt.predicate, replacerObject)
    }


    private fun getObjects(predicate: Property, model: Model): Set<Resource>? {
        if (!stmtObjectMap!!.containsKey(predicate.toString())) {
            val objects: Set<Resource> = model.listStatements(null, predicate, null as RDFNode?)
                .mapWith { s -> s.getObject().asResource() }.toSet()
            if (objects.isEmpty()) {
                println("what")
            }
            stmtObjectMap!![predicate.toString()] = objects
        }
        return stmtObjectMap!![predicate.toString()]
    }

    /**@return list of Resources except those for which a Statement with the given subject
     * and predicate exsits in the provided model
     */
    private fun withoutExistingStatements(
        subject: Resource,
        predicate: Property,
        objects: Set<Resource>?,
        model: Model
    ): List<Resource?> {
        val without: MutableList<Resource?> = ArrayList()
        if (objects!!.isNotEmpty()) {
            for (`object`: Resource in objects) {
                if (!model.contains(subject, predicate, `object`)) {
                    without.add(`object`)
                }
            }
        } else {
            println("")
        }
        return without
    }


    private fun retrieveTrueStatements(): Set<Statement> {
        val stmts: MutableSet<Statement> = HashSet()
        while (stmts.size < numberOfTrueStatements && trueStmtDrawer.hasStatement()) {
            stmts.add(trueStmtDrawer.drawRandomStmt())
        }
        return stmts
    }

}