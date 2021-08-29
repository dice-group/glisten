package org.dice_group.glisten.core.task.fuser

import org.apache.jena.rdf.model.*
import org.apache.jena.vocabulary.OWL
import org.dice_group.glisten.core.utils.RDFUtils
import java.net.URL
import java.util.*
import java.util.stream.Collectors


/**
 * mostly just copy paste svens code.
 * Don't know it don't care
 *
 * Eff this Fuser BS
 *
 *
 * Does somehting a long the line of merging nodes, for performance issues or similar.
 *
 * Honestly don't touch it, it's not worth it.
 */
object Fuser {

    private val equivalenceProperties = listOf<String>()

    fun performInitialFusion(target: Model?, links: Model?) {
        cleanModel(target)
        addModel(target!!, links)
        fusion(target, target) //important if model was not fused before
    }

    private fun fusion(target: Model, source: Model) {
        //get eequivalence statements
        val equivalenceStmts = getEquivalenceStmts(source)
        source.remove(equivalenceStmts)
        equivalenceStmts.forEach {
            stmt ->
            //something merging bla
            val mergeTask: MergeTask = getMergeTask(target, stmt.subject, stmt.`object`.asResource())
            defineMergeNodeInModel(target, mergeTask)

        }
        target.add(source)
        //TODO move stmts to merge node whatever that means again
        moveStmtsToMergeNode(target)
    }

    private fun getMergeTask(model: Model, target: Resource, source: Resource): MergeTask {
        val mergeTask = MergeTask(model, target, source)
        if (hasMergeNodeIn(model, target) && hasMergeNodeIn(model, source)) {
            mergeTask.mergeNode = getMergeNode(model, target)
        }
        if (!hasMergeNodeIn(model, target) && hasMergeNodeIn(model, source)) {
            mergeTask.mergeNode = getMergeNode(model, source)
        }
        if (hasMergeNodeIn(model, target) && !hasMergeNodeIn(model, source)) {
            mergeTask.mergeNode = getMergeNode(model, target)
        }
        if (!hasMergeNodeIn(model, target) && !hasMergeNodeIn(model, source)) {
            mergeTask.mergeNode = getFirstMergeNodeAlphabetically(target, source, model)
        }
        return mergeTask
    }


    /**Inserts sameAs relation ships into the model
     * such that all entities which are equivalent have a single sameAs relationship pointing to their merge Node */
    private fun defineMergeNodeInModel(model: Model, mergeTask: MergeTask) {
        for (toMerge in mergeTask.toBeMerged) {
            checkFuseModelInvariant(model, toMerge)
            model.removeAll(toMerge, OWL.sameAs, null)
            model.removeAll(null, OWL.sameAs, toMerge)
        }
        for (toMerge in mergeTask.toBeMerged) {
            model.add(toMerge, OWL.sameAs, mergeTask.mergeNode)
        }
    }


    private fun getEquivalenceStmts(source: Model): List<Statement> {
        val sameAsStmts: HashSet<Statement> = HashSet()
        for (property in equivalenceProperties) {
            val eqProperty: Property = ResourceFactory.createProperty(property)
            sameAsStmts.addAll(source.listStatements(null, eqProperty, null as RDFNode?).toSet())
        }
        return sameAsStmts.stream().collect(Collectors.toList())

    }

    private fun moveStmtsToMergeNode(model: Model) {
        for (stmt in model.listStatements(null, OWL.sameAs, null as RDFNode?).toList()) {
            val subject: Resource = stmt.subject
            val mergeNode: Resource? = getMergeNode(model, subject)
            passStatements(model, mergeNode, subject)
        }
        sameAsDomainHasNoStmts(model)
    }

    private fun sameAsDomainHasNoStmts(model: Model) {
        for (stmt in model.listStatements(null, OWL.sameAs, null as RDFNode?).toList()) {
            val domain = stmt.subject
            hasOnlyOneSameAsRelations(model, domain)
        }
    }

    private fun hasOnlyOneSameAsRelations(model: Model, res: Resource) {
        val stmts = model.listStatements(res, null, null as RDFNode?).toList()
        //stmts.addAll(model.listStatements(null, ResourceFactory.createProperty(res.toString()), (RDFNode)null).toList());
        stmts.addAll(model.listStatements(null, null, res).toList())
        if (1 != stmts.size) {
            println("Statements not allowed :::::::::::::::::::::::::::::::::")
            for (stmt in stmts) {
                println(stmt.toString())
            }
            throw IllegalArgumentException("Passing failed")
        }
        require(OWL.sameAs.toString() == stmts[0].predicate.toString()) { "Passing failed" }
        require(res.toString() == stmts[0].subject.toString()) { "Passing failed" }
    }


    private fun getMergeNode(model: Model, res: Resource): Resource? {
        val eqObjects: List<Resource> =
            model.listObjectsOfProperty(res, OWL.sameAs).mapWith { o: RDFNode -> o.asResource() }
                .toList()
        checkMergeNodeIsOneNode(eqObjects, res)
        return eqObjects[0]
    }

    private fun checkMergeNodeIsOneNode(eqObjects: List<Resource>, res: Resource) {
        if (eqObjects.size > 1) {
            println("Multiple Equivalent Resources are defined for $res")
            for (eqRes in eqObjects) {
                println(eqRes.toString())
            }
            throw IllegalArgumentException("Multiple eq NOdes : ")
        } else require(eqObjects.isNotEmpty()) { "no merge node" }
    }



    private fun passStatements(target: Model, mergeNode: Resource?, res: Resource) {
        for (sourceStmt in target.listStatements(res, null, null as RDFNode?).toList()) {
            if (sourceStmt.predicate != OWL.sameAs) {
                target.remove(sourceStmt)
                target.add(mergeNode, sourceStmt.predicate, sourceStmt.getObject())
            }
        }
        for (sourceStmt in target.listStatements(null, null, res).toList()) {
            if (sourceStmt.predicate != OWL.sameAs) {
                target.remove(sourceStmt)
                target.add(sourceStmt.subject, sourceStmt.predicate, mergeNode)
            }
        }
    }


    private fun addModel(target: Model, links: Model?) {
        if (links != null) {
            cleanModel(links)
            target.add(links)
        }
    }

    private fun cleanModel(model: Model?) {
        //remove non uri resources
        RDFUtils.removeNonURIObjects(model)
        // can;t be bothered more
        val remove = model?.listStatements()?.toList()?.filter {
            URL(it.subject.uri.toString()).host == URL(it.`object`.asResource().uri.toString()).host
        }
        model?.remove(remove)
    }


    private fun hasMergeNodeIn(model: Model, res: Resource): Boolean {
        val eqObjects = model.listObjectsOfProperty(res, OWL.sameAs).toList()
        require(eqObjects.size <= 1) { "Multiple eq NOdes for $res" }
        return if (eqObjects.size == 1) {
            true
        } else false
    }


    private fun getFirstMergeNodeAlphabetically(target: Resource, source: Resource, model: Model): Resource? {
        val sorter: List<String> = ArrayList(Arrays.asList(target.toString(), source.toString()))
        Collections.sort(sorter)
        return ResourceFactory.createResource(sorter[0])
    }





    private fun checkFuseModelInvariant(model: Model, node: Resource?) {
        val sameAsSubjectList = model.listStatements(node, OWL.sameAs, null as RDFNode?).toList() //TODO remove check
        val sameAsObjectList = model.listStatements(null, OWL.sameAs, node).toList()
        if (sameAsSubjectList.size > 1) {
            println("Illegal Combination")
            throw IllegalArgumentException("Node has multiple merge Node $node")
        } else if (!sameAsObjectList.isEmpty() && !sameAsSubjectList.isEmpty()) {
            throw IllegalArgumentException("SameAs relationship is transitive : $node")
        }
    }


    private class MergeTask(model: Model, target: Resource, source: Resource) {
        var mergeNode: Resource? = null
        var toBeMerged = HashSet<Resource?>()
        private val sourceMergeNode: Resource? = null
        private val targetMergeNode: Resource? = null
        private fun addMergedNodes(model: Model, res: Resource?) {
            toBeMerged.addAll(model.listStatements(null, OWL.sameAs, res).mapWith { s: Statement -> s.subject }
                .toList())
        }

        private fun addMergeNode(model: Model, node: Resource) {
            if (Fuser.hasMergeNodeIn(model, node)) {
                val mergeNode = Fuser.getMergeNode(model, node)
                addMergedNodes(model, mergeNode) // TODO BOTH????
                Fuser.checkFuseModelInvariant(model, mergeNode)
                toBeMerged.add(mergeNode)
            }
        }


        init {
            Fuser.checkFuseModelInvariant(model, target)
            toBeMerged.add(target)
            Fuser.checkFuseModelInvariant(model, source)
            toBeMerged.add(source)
            addMergeNode(model, target)
            addMergeNode(model, source)
            addMergedNodes(model, target)
            addMergedNodes(model, source)
        }
    }

}
