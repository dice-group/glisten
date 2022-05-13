package org.dice_research.glisten.collect;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.sparql.core.Quad;
import org.dice_research.fc.data.Predicate;
import org.dice_research.fc.data.QRestrictedPath;
import org.dice_research.fc.paths.FactPreprocessor;
import org.dice_research.fc.paths.IPathSearcher;
import org.dice_research.fc.paths.search.SPARQLBasedSOPathSearcher;
import org.dice_research.fc.sparql.filter.NamespaceFilter;
import org.dice_research.glisten.COPAAL_Preprocessor;

public abstract class AbstractPathcollector extends StreamRDFBase implements PathCollector {

    protected Map<String, Set<QRestrictedPath>> paths = new HashMap<>();
    protected FactPreprocessor pFactory;
    protected IPathSearcher searcher;
    protected int maxNumberOfTriples = -1;
    protected int count = 0;

    public AbstractPathcollector(FactPreprocessor pFactory, QueryExecutionFactory qef) {
        this(pFactory, qef, new String[] { "http://dbpedia.org/property/", "http://dbpedia.org/ontology/" });
    }

    public AbstractPathcollector(FactPreprocessor pFactory, QueryExecutionFactory qef, String[] namespaces) {
      searcher = new SPARQLBasedSOPathSearcher(qef, COPAAL_Preprocessor.MAX_PATH_LENGTH, Arrays.asList(
          new NamespaceFilter(namespaces, false)));
      this.pFactory = pFactory;
    }

    public AbstractPathcollector(FactPreprocessor pFactory, QueryExecutionFactory qef, int maxNumberOfTriples) {
      this.maxNumberOfTriples = maxNumberOfTriples;
    }

    public AbstractPathcollector(FactPreprocessor pFactory, QueryExecutionFactory qef, String[] namespaces, int maxNumberOfTriples) {
        this(pFactory, qef);
        this.maxNumberOfTriples = maxNumberOfTriples;
    }

    public void collectPaths(String inputFile, Lang lang, QueryExecutionFactory qef) {
        RDFDataMgr.parse(this, inputFile, lang);
    }

    @Override
    public void quad(Quad quad) {
        triple(quad.asTriple());
    }

    @Override
    public void triple(Triple triple) {
        Resource s = new ResourceImpl(triple.getSubject().getURI());
        Property p = new PropertyImpl(triple.getPredicate().getURI());
        Resource o = new ResourceImpl(triple.getObject().getURI());
        // If there is no maximum or the maximum hasn't been reached yet
        if (performPathSearch(s,p,o)) {
            ++count;
            // Search paths for triple
            Set<QRestrictedPath> pathsOfP = getPathsOfProperty(p.getURI());
            searchPaths(s, pFactory.generatePredicate(new StatementImpl(s, p, o)), o, pathsOfP);
        }
    }

    protected abstract boolean performPathSearch(Resource s, Property p, Resource o);

    protected Set<QRestrictedPath> getPathsOfProperty(String propertyIRI) {
        if (paths.containsKey(propertyIRI)) {
            return paths.get(propertyIRI);
        } else {
            Set<QRestrictedPath> pathsOfP = new HashSet<>();
            paths.put(propertyIRI, pathsOfP);
            return pathsOfP;
        }
    }

    protected abstract void searchPaths(Resource s, Predicate predicate, Resource o,
            Set<QRestrictedPath> pathsOfP);

    public Map<String, Set<QRestrictedPath>> getPaths() {
        return paths;
    }

    @Override
    public void close() throws Exception {
        // nothing to do
    }
}
