package org.dice_group.glisten.core.task

import org.apache.jena.rdf.model.Statement

/**
 * Simple Wrapper class containing true and false statements and their corresponding trueness value (1=true fact, -1 = false fact)
 */
class Task {

    val statements = mutableListOf<Pair<Statement, Double>>()


    /**
     * Adds all provided [Statement]s to the internal [statements] list using the [Pair] of the statement and 1.0
     *
     * @param statements the list of true statements
     */
    fun addTrueStatements(statements: Collection<Statement>){
        statements.forEach{
            this.statements.add(Pair(it, 1.0))
        }
    }

    /**
     * Adds all provided [Statement]s to the internal [statements] list using the [Pair] of the statement and -1.0
     *
     * @param statements the list of false statements
     */
    fun addFalseStatements(statements: Collection<Statement>){
        statements.forEach{
            this.statements.add(Pair(it, -1.0))
        }
    }

}