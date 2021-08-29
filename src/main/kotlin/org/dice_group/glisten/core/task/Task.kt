package org.dice_group.glisten.core.task

import org.apache.jena.rdf.model.Statement
import org.dice_group.glisten.core.utils.RDFUtils
import org.dice_group.glisten.core.SameStmtTwiceException

class Task {

    val statements = mutableListOf<Pair<Statement, Double>>()


    fun addTrueStatement(stmt: Statement){
        this.statements.add(Pair(stmt, 1.0))
    }

    fun addFalseStatement(stmt: Statement){
        this.statements.add(Pair(stmt, -1.0))
    }

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

    companion object {
        fun checkTask(task: Task){
            task.statements.forEach { stmt1 ->
                var exists = false
                task.statements.forEach { stmt2 ->
                    if (exists){
                        throw SameStmtTwiceException("Statement %s at least twice in Task".format(stmt1.first))
                    }
                    //perfect english :D
                    if (RDFUtils.isStatementsSame(stmt1.first, stmt2.first)){
                        exists = true
                    }
                }
            }
        }
    }

}