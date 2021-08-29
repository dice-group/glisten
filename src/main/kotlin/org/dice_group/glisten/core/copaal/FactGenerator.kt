package org.dice_group.glisten.core.copaal

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Statement
import org.dice_group.glisten.core.task.TaskDrawer
import org.dice_group.glisten.core.task.drawer.StmtDrawer

object FactGenerator {

    /**
     * Creates some true and false facts from source and removes them from source
     */
    fun createFacts(seed: Long, numberOfTrueStatements: Int,  numberOfFalseStatements: Int,
                    trueStmtDrawer: StmtDrawer, falseStmtDrawer: StmtDrawer
                    ) : MutableList<Pair<Statement, Double>> {
        val taskDrawer = TaskDrawer(seed, numberOfTrueStatements, numberOfFalseStatements, trueStmtDrawer, falseStmtDrawer)
        trueStmtDrawer.init()
        falseStmtDrawer.init()
        taskDrawer.generateFalseStatements()
        taskDrawer.generateTrueStatements()
        return taskDrawer.task.statements
    }
}