package org.dice_group.glisten.core.task.drawer

import org.apache.jena.rdf.model.*
import org.apache.jena.util.iterator.ExtendedIterator
import org.dice_group.glisten.core.utils.jena.ObjectPropertyBasedSelector

/**
 * ## Description
 *
 * The RandomPropertiesDrawer gets random [Statement]s having a property which occurs at least a certain amount and are in of the provided namespaces. It should be noted that only object properties are used, i.e., statements that have a literal as object are not taken into account.
 *
 * Furthermore, it uses a given [Model] to retrieve all Statements using the namespaces list and checks
 * at the random draw of a statement if the property fits the constraint having at least the minimum amount of occurrences and
 * only retrieves a maximum limit per property.
 *
 * ```
 * ```
 * ## Example
 *
 * Let's create an RandomPropertiesDrawer only allowing the namespace `https://example.com/`.
 * Further on we only want properties which occur at least 10 times, and we want only retrieve a maximum of 20 statements per property
 *
 * As a seed we use 123L
 *
 * ```kotlin
 * val myModel = ...// create your model here
 *
 * val drawer = RandomPropertiesDrawer(allowList = listOf("https://example.com/"),
 *                              seed = 123L,
 *                              model = myModel,
 *                              minPropOcc = 10,
 *                              maxPropertyLimit = 20
 *          )
 * ```
 *
 * If we draw statements and `https://example.com/prop/1` only occurs 9 times then the Drawer will ignore `https://example.com/prop/1`
 * If however `https://example.com/prop/2` occurs more than 20 times, we will only retrieve it 20 times regardless.
 * The provided will be used to choose 20 random items in this case.
 *
 * @param namespaces The namespaces the properties are allowed to be in
 * @param seed The seed to use for any random activity
 * @param model The [Model] to use to retrieve the Statements for each property listed in the [namespaces]
 * @param minPropOcc The minimum a property has to occur to be considered.
 * @param maxPropertyLimit The maximum a property should be retrieved, if more are available the seed will be used to choose random ones
 *
 */
class RandomPropertiesDrawer(private val namespaces: Collection<String>, seed: Long, override val model: Model, minPropOcc: Int, maxPropertyLimit: Int) : StmtDrawer(seed, model, minPropOcc, maxPropertyLimit) {


    override fun getStmts(): MutableList<Statement> {
        val ret = mutableListOf<Statement>()
        //predicates are occurring at least minOcc times and is in one of the namespaces

        var predicatesToUse = getPredicates()
        predicatesToUse = predicatesToUse.shuffled(random)
        println("\r[+] Considering $predicatesToUse predicates")
        predicatesToUse.forEach {
            ret.addAll(super.getStmts(it.toString()))
        }
        return ret
    }

    private fun getPredicates(): List<Property> {
        val predicates = model.listStatements().mapWith { it.predicate }.toSet()
        return predicates.filter {
            (model.listStatements(null, it, null as RDFNode?).toList().size >= minPropOcc) &&
                    namespaces.any { p -> it.toString().startsWith(p) }
        }.toList()
    }

}