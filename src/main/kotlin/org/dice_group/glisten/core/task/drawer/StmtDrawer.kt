package org.dice_group.glisten.core.task.drawer

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.rdf.model.Statement
import kotlin.random.Random

abstract class StmtDrawer(private val seed: Long, open val model : Model, private val minPropOcc: Int, private val maxPropertyLimit: Int) {

    var stmtList: MutableList<Statement> = mutableListOf()
    private val random = Random(seed)

    abstract fun getStmts(): MutableList<Statement>

    fun init(){
        stmtList = getStmts()
    }

    fun hasStatement(): Boolean{
        return stmtList.isNotEmpty()
    }

    open fun drawRandomStmt(): Statement {
        val index = random.nextInt(stmtList.size)
        val stmt: Statement = stmtList[index]
        stmtList.removeAt(index)
        return stmt
    }


    fun getStmts(property: String): Collection<Statement>{
        val stmts = model.listStatements(null, ResourceFactory.createProperty(property), null as RDFNode?).toSet()
        if(stmts.size<minPropOcc){
            return emptySet()
        }
        //get random statments by shuffeling and choosing [:limit]
        stmts.shuffled(random).subList(0, maxPropertyLimit.coerceAtMost(stmts.size))
        return stmts
    }


}