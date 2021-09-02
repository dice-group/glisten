package org.dice_group.glisten.core.scorer

import org.apache.jena.rdf.model.Statement
import org.dice_group.glisten.core.task.TaskDrawer
import org.dice_group.glisten.core.task.drawer.StmtDrawer

/**
 * The Fact Generator generates some true and some wrong facts for the glisten workflow
 */
object FactGenerator {

    /**
     * Retrieving randomly true and wrong statements from the true statement drawer and resp. the false statement drawer
     * and returns a list of Pairs containing the fact (statement) and the either 1 if the fact is true or -1 if the fact is false.
     *
     * @param seed the seed to use for the random retrieval
     * @param numberOfTrueStatements the number of true statements to generate
     * @param numberOfFalseStatements the number of false statements to generate
     * @return list of pairs containing a fact and the trueness value (1=true, -1=false)
     */
    fun createFacts(seed: Long, numberOfTrueStatements: Int,  numberOfFalseStatements: Int,
                    trueStmtDrawer: StmtDrawer, falseStmtDrawer: StmtDrawer
                    ) : MutableList<Pair<Statement, Double>> {

        val taskDrawer = TaskDrawer(seed, numberOfTrueStatements, numberOfFalseStatements, trueStmtDrawer, falseStmtDrawer)
        //initialize the drawers
        trueStmtDrawer.init()
        falseStmtDrawer.init()
        //internally generate the statements
        taskDrawer.generateFalseStatements()
        taskDrawer.generateTrueStatements()
        //sort them so we have a better overview in the logging, the false statements will be on top aka processed first
        taskDrawer.task.statements.sortBy { it.second }
        return taskDrawer.task.statements

    }
}