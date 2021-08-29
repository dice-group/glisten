package org.dice_group.glisten.core.utils

import org.apache.jena.graph.Triple
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.system.*
import org.apache.jena.shared.JenaException
import java.net.URL


fun main(){
    val model  =  RDFUtils.streamNoLiterals("file:///home/minimal/IdeaProjects/glisten/glisten/model.nt")
    println(model)
}

object RDFUtils {

    fun streamNoLiterals(file: String) : Model{
        val streamer = StreamRDFNoLiteral()
        RDFDataMgr.parse(streamer, file)
        return streamer.model
        //val model = ModelFactory.createDefaultModel()
        //model.read(URL(file).openStream(), null, "NT")
        //removeNonURIObjects(model)
        //return model
    }

    fun isStatementsSame(stmt1: Statement, stmt2: Statement) : Boolean{
        var same = stmt1.subject.toString() == stmt2.subject.toString()
        same = same && stmt1.predicate.toString() == stmt2.predicate.toString()
        same = same && stmt1.`object`.toString() == stmt2.`object`.toString()
        return same
    }


    fun removeNonURIObjects(model: Model?){
        val remove = model?.listStatements()?.toList()?.filter { it.`object`.isLiteral }
        model?.remove(remove)
    }

}

class StreamRDFNoLiteral : StreamRDFBase() {

    val model: Model = ModelFactory.createDefaultModel()

    override fun prefix(prefix: String?, iri: String?) {
        try {
            model.graph.prefixMapping.setNsPrefix(prefix, iri)
        } catch (var4: JenaException) {
        }
    }

    override fun triple(p0: Triple) {
        if(!p0.`object`.isLiteral){
            model.graph.add(p0)
        }
        if((model.size() % 10000000) == 0L){
            println("Processed: %s triples".format(model.size()))
        }
    }


}