package org.dice_group.glisten.core.utils

import org.apache.jena.graph.Triple
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.system.StreamRDFBase
import org.apache.jena.shared.JenaException
import java.io.File
import java.io.FileNotFoundException
import kotlin.jvm.Throws

object RDFUtils {

    /**
     * Streams a file to an RDF Model ignoring all Literals.
     * @param file the rdf file to read
     * @return the Model containing all triples from the rdf file except Literals
     */
    fun streamNoLiterals(file: String) : Model{
        val streamer = StreamRDFNoLiteral()
        RDFDataMgr.parse(streamer, file)
        println("\r[+] Finished Processing: %s triples".format(streamer.model.size()))
        return streamer.model
    }

    /**
     * Removes all literals from a model
     * @param model the model to remove all triples from
     */
    fun removeNonURIObjects(model: Model?){
        val remove = model?.listStatements()?.toList()?.filter { it.`object`.isLiteral }
        model?.remove(remove)
    }


    /**
     * Loads a file into the triplestore using the load_triplestore.sh script.
     * The script needs to be in the same folder as this was executed.
     *
     * The rdffile should be set to URL syntax (meaning file://path/to/file.nt)
     * However file.nt would also work as long as the file is in the local directory
     *
     * @param rdffile the rdf file to load into the triplestore (use URL syntax, aka file://..)
     * @param scriptFile the script file to use to load the triplesotre
     * @throws FileNotFoundException if the script file is not found.
     */
    @Throws(FileNotFoundException::class)
    fun loadTripleStoreFromScript(rdffile: String, scriptFile: String){
        if(!File(scriptFile).exists()){
            throw FileNotFoundException("$scriptFile couldn't be located")
        }
        var file = rdffile
        //simply set to current directory
        if(!rdffile.contains("/")){
            file = File(rdffile).absolutePath
        }
        //replace url schema file:// just in case
        var path = file.replace("file://", "")
        // we need to arguments the path and the filename,
        path = path.substringBeforeLast("/")+"/"
        val p = ProcessBuilder().command(scriptFile, path, file.substringAfterLast("/"))
        //this will stop scripts from not returning after exit. some java bug i guess
        p.inheritIO()
        //wait for the script to finish
        p.start().waitFor()
    }

}

/**
 * Simple streamer class which ignores literals
 */
class StreamRDFNoLiteral : StreamRDFBase() {

    val model: Model = ModelFactory.createDefaultModel()

    //we need this to work with prefixes
    override fun prefix(prefix: String?, iri: String?) {
        try {
            model.graph.prefixMapping.setNsPrefix(prefix, iri)
        } catch (var4: JenaException) {
        }
    }


    override fun triple(p0: Triple) {
        //simply ignore literals
        if(!p0.`object`.isLiteral){
            model.graph.add(p0)
        }
        //some output, to see that it's still working
        if((model.size() % 100000) == 0L){
            print("\r[-] Processed: %s triples".format(model.size()))
        }
    }


}