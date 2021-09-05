package org.dice_group.glisten.core.task

import org.apache.jena.rdf.model.*
import org.dice_group.glisten.core.task.drawer.StmtDrawer
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet


/**
 *
 * The TaskDrawer generates true and false facts, provided a [trueStmtDrawer] and a [falseStmtDrawer]
 * The true facts will be retrieved of the [trueStmtDrawer] and the false facts will be retrieved as true facts by the [falseStmtDrawer]
 * and then mutated s.t. the facts are wrong.
 *
 * Disclaimer: Code is shamelessly stolen from sven and just converted to Kotlin and adjusted for glisten.
 *
 * @param seed The seed to use in any random activity
 * @param numberOfTrueStatements The no. of true statements to generate
 * @param numberOfFalseStatements The nu. of false statements to generate
 * @param trueStmtDrawer The [StmtDrawer] to generate true facts from
 * @param falseStmtDrawer The [StmtDrawer] to generate false facts from
 *
 */
class TaskDrawer(seed: Long, private val numberOfTrueStatements: Int, private val numberOfFalseStatements: Int, private val trueStmtDrawer: StmtDrawer, private val falseStmtDrawer: StmtDrawer ) {

    private val random = Random(seed)

    companion object{
        val LOGGER = LoggerFactory.getLogger(TaskDrawer::class.java)
    }
    private var stmtObjectMap: HashMap<String, Set<Resource>?>? = HashMap()

    val task = Task()

    /**
     * Generates the full list of false statements and adds them to the [task]
     *
     * To retrieve all generated statements use
     *
     * ```kotlin
     * task.statements
     * ```
     *
     */
    fun generateFalseStatements() {
        if (numberOfFalseStatements > 0) {
            val falseStatements: Set<Statement> = retrieveFalseStatements()
            task.addFalseStatements(falseStatements)
            LOGGER.debug(
                "False Statement Generation Requested: " + numberOfFalseStatements // + ", Listed by Drawer: " + falseStmtDrawer.getStmtList().size()
                    .toString() + ", Added to Task " + falseStatements.size
            )
        } else {
            LOGGER.info("No False Statements were generated")
        }
    }


    /**
     * Generates the full list of true statements and stores them inside the [task]
     *
     * To retrieve all generated statements use
     *
     * ```kotlin
     * task.statements
     * ```
     */
    fun generateTrueStatements() {
        val trueStmts: Set<Statement> = retrieveTrueStatements()
        task.addTrueStatements(trueStmts)
        LOGGER.debug(
            ("True Statement Generation Requested: " + numberOfTrueStatements // + ", Listed by Drawer: " + trueStmtDrawer.getStmtList().size()
                .toString() + ", Added to Task " + trueStmts.size)
        )
    }

    /**
     * ## Description
     *
     * Generates true statements until either
     * * the amount specified in [numberOfTrueStatements] is reached, or
     * * there are no more statements to generate from
     *
     * Every statement will be drawn from a list of true statements inside the [trueStmtDrawer]
     *
     * @return the set of true statements
     */
    private fun retrieveTrueStatements(): Set<Statement> {
        val stmts: MutableSet<Statement> = HashSet()
        while (stmts.size < numberOfTrueStatements && trueStmtDrawer.hasStatement()) {
            stmts.add(trueStmtDrawer.drawRandomStmt())
        }
        return stmts
    }


    /**
     * ## Description
     *
     * Generates false statements until either
     * * the amount specified in [numberOfFalseStatements] is reached, or
     * * there are no more statements to generate from
     *
     * Every statement will be drawn from a list of true statements from the [falseStmtDrawer] and then be mutated.
     * The mutation will remove the object of the [Statement] and replace it with a random object, s.t.
     * the new statement is false wrt the underlying [Model]
     *
     * @return the set of false statements
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


    /**
     * ## Description
     *
     * Given the [Statement] stmt, this will retrieve all objects that are
     * * associated with stmt.predicate (a statement (?, stmt.predicate, object) exists inside the [model], and
     * * the statement (stmt.subject, stmt.predicate, object) does not exist in the [model]
     *
     * @param stmt The statement to use
     * @param model The model to check existence against.
     * @return The list of objects as explained above
     */
    private fun getObjectsOfProperty(stmt: Statement, model: Model): List<Resource?> {
        val objects: Set<Resource>? = getObjects(stmt.predicate, model)
        return withoutExistingStatements(stmt.subject, stmt.predicate, objects, model)
    }

    /**
     * Replaces the object of the [Statement] with a random [Resource] listed in [replacer]
     *
     * @param stmt The statement to exchange the object of
     * @param replacer A list of resources to exchange the object with.
     * @return the mutated statement
     */
    @Throws(java.lang.IllegalArgumentException::class)
    private fun randomObjectReplacement(stmt: Statement, replacer: List<Resource?>): Statement {
        val replacerObject = replacer[random.nextInt(replacer.size)]
        return ResourceFactory.createStatement(stmt.subject, stmt.predicate, replacerObject)
    }

    /**
     * Gets a list of all objects associated with the provided [predicate]
     *
     * @param predicate The predicate to retrieve all objects from
     * @param model The rdf [Model] to retrieve the objects from
     *
     * @return The set of Objects associated with the predicate
     */
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

    /**
     * Creates a list of objects from [objects] s.t. the [Statement] ([subject], [predicate], object) doesn't exist inside the given [model]
     *
     * @param subject the Subject to use
     * @param predicate The predicate to user
     * @param objects The list of objects to check against.
     * @param model The rdf model to check if the statement exist
     * @return A list of all objects from [objects] where ([subject], [predicate], object) is not inside [model]
     *
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


}