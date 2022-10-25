package org.dice_group.glisten.core.task.drawer

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Statement


/**
 * ## Description
 *
 * The BlocklistDrawer gets only [Statement]s not having a property provided in a block list.
 *
 * The list blocks each property in it to be used. Thus, only [Statement]s having a property which is not in the block list will be considered.
 * Furthermore, it uses a given [Model] to retrieve all Statements using the block list and checks
 * at the random draw of a [Statement] if the property fits the constraint having at least the minimum amount of occurrences and
 * only retrieves a maximum limit per property.
 *
 * ```
 * ```
 * ## Example
 *
 * Let's create an BlocklistDrawer blocking the properties `https://example.com/prop/1` and `https://example.com/prop/2`.
 * Further on we only want properties which occur at least 10 times, and we want only retrieve a maximum of 20 statements per property
 *
 * As a seed we use 123L
 *
 * ```kotlin
 * val myModel = ...// create your model here
 *
 * val drawer = BlockListDrawer(blockList = listOf("https://example.com/prop/1", "https://example.com/prop/2"),
 *                              seed = 123L,
 *                              model = myModel,
 *                              minPropOcc = 10,
 *                              maxPropertyLimit = 20
 *          )
 * ```
 *
 * If we draw statements and `https://example.com/prop/3` only occurs 9 times then the Drawer will ignore `https://example.com/prop/1`
 * If however `https://example.com/prop/4` occurs more than 20 times, we will only retrieve it 20 times regardless.
 * The provided will be used to choose 20 random items in this case.
 *
 * @param blockList The list of properties which are blocked/not allowed to be drawn
 * @param seed The seed to use for any random activity
 * @param model The [Model] to use to retrieve the Statements for each property not listed in the [blockList]
 * @param minPropOcc The minimum a property, which is not in the [blockList], has to occur to be considered.
 * @param maxPropertyLimit The maximum a property, which is not in the [blockList], should be retrieved, if more are available the seed will be used to choose random ones
 *
 */
class BlockListDrawer(private val blockList: Collection<String>, private val seed: Long, override val model : Model, minPropOcc: Int, private val maxPropertyLimit: Int) : StmtDrawer(seed, model, minPropOcc, maxPropertyLimit) {

    override fun getStmts(): MutableList<Statement> {
        val ret = mutableListOf<Statement>()
        //get all statements
        val stmts =  model.listStatements().mapWith { stmt -> stmt.predicate.toString() }.toSet()
        //remove blocklists
        stmts.removeAll(blockList)
        stmts.forEach{
            ret.addAll(super.getStmts(it))
        }
        return ret
    }


}