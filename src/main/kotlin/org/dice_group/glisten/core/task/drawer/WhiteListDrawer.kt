package org.dice_group.glisten.core.task.drawer

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Statement

class WhiteListDrawer(private val whiteList: Collection<String>, private val seed: Long, override val model : Model, private val minPropOcc: Int, private val maxPropertyLimit: Int) : StmtDrawer(seed, model, minPropOcc, maxPropertyLimit) {


    override fun getStmts(): MutableList<Statement> {
        val ret = mutableListOf<Statement>()
        whiteList.forEach{
            ret.addAll(super.getStmts(it))
        }
        return ret
    }


}