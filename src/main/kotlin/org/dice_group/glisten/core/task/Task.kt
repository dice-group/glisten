package org.dice_group.glisten.core.task

import org.apache.jena.rdf.model.Statement
import org.dice_group.glisten.core.utils.RDFUtils
import org.dice_group.glisten.core.SameStmtTwiceException

/**
 * Simple Wrapper class containing true and false statements and their corresponding trueness value (1=true fact, -1 = false fact)
 */
class Task {

    val statements = mutableListOf<Pair<Statement, Double>>()


    fun addTrueStatements(statements: Collection<Statement>){
        statements.forEach{
            this.statements.add(Pair(it, 1.0))
        }
    }

    fun addFalseStatements(statements: Collection<Statement>){
        statements.forEach{
            this.statements.add(Pair(it, -1.0))
        }
    }

}