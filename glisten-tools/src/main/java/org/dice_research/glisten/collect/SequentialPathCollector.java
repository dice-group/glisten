package org.dice_research.glisten.collect;

import java.util.Set;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.dice_research.fc.data.Predicate;
import org.dice_research.fc.data.QRestrictedPath;
import org.dice_research.fc.paths.FactPreprocessor;

public class SequentialPathCollector extends AbstractPathcollector {

    public SequentialPathCollector(FactPreprocessor pFactory, QueryExecutionFactory qef) {
        super(pFactory, qef);
    }

    public SequentialPathCollector(FactPreprocessor pFactory, QueryExecutionFactory qef, int maxNumberOfTriples) {
        super(pFactory, qef, maxNumberOfTriples);
    }

    public SequentialPathCollector(FactPreprocessor pFactory, QueryExecutionFactory qef, String[] namespaces) {
        super(pFactory, qef, namespaces);
    }

    public SequentialPathCollector(FactPreprocessor pFactory, QueryExecutionFactory qef, String[] namespaces, int maxNumberOfTriples) {
        super(pFactory, qef, namespaces, maxNumberOfTriples);
    }

    @Override
    protected void searchPaths(Resource s, Predicate predicate, Resource o, Set<QRestrictedPath> pathsOfP) {
        pathsOfP.addAll(searcher.search(s, predicate, o));
    }

    protected boolean performPathSearch(Resource s, Property p, Resource o) {
        if((maxNumberOfTriples <= 0) || (count < maxNumberOfTriples)) {
            return true;
        } else {
            return false;
        }
    }
}