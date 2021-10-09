package org.dice_group.glisten.core.utils.jena;

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
//public class ObjectPropertyBasedSelector implements Selector {
//
//    protected Property property;
//
//    public ObjectPropertyBasedSelector(Property property) {
//        this.property = property;
//    }
//
//    @Override
//    public boolean test(Statement t) {
//        return (property == null || property.equals(t.getPredicate())) && (!t.getObject().isLiteral());
//    }
//
//    @Override
//    public boolean isSimple() {
//        return false;
//    }
//
//    @Override
//    public Resource getSubject() {
//        return null;
//    }
//
//    @Override
//    public Property getPredicate() {
//        return property;
//    }
//
//    @Override
//    public RDFNode getObject() {
//        return null;
//    }
//
//    @Override
//    public int hashCode() {
//        final int prime = 31;
//        int result = 1;
//        result = prime * result + ((property == null) ? 0 : property.hashCode());
//        return result;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj)
//            return true;
//        if (obj == null)
//            return false;
//        if (getClass() != obj.getClass())
//            return false;
//        ObjectPropertyBasedSelector other = (ObjectPropertyBasedSelector) obj;
//        if (property == null) {
//            if (other.property != null)
//                return false;
//        } else if (!property.equals(other.property))
//            return false;
//        return true;
//    }
//}
