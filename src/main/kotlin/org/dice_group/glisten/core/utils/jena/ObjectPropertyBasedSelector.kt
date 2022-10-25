package org.dice_group.glisten.core.utils.jena

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.Statement;

/**
 * This {@link Selector} selects triples that have the given {@link Property} as
 * predicate and not a literal as object. If the given property is {@code null}
 * only the second requirement is checked.
 *
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
class ObjectPropertyBasedSelector(val property: Property?) : Selector {

    override fun test(s: Statement): Boolean {
        return (property == null || property.equals(s.getPredicate())) && (!s.getObject().isLiteral());
    }

    override fun isSimple(): Boolean {
        return false;
    }

    override fun getSubject(): Resource? {
        return null;
    }

    override fun getPredicate(): Property? {
        return property;
    }

    override fun getObject(): RDFNode? {
        return null;
    }

}