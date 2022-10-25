package org.dice_research.glisten.collect;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.dice_research.fc.data.Predicate;
import org.dice_research.fc.data.QRestrictedPath;
import org.dice_research.fc.paths.FactPreprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParallelPathCollector extends AbstractPathcollector implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelPathCollector.class);

    protected ExecutorService executor;
    protected Queue<Future<?>> futures = new LinkedList<>();

    public ParallelPathCollector(FactPreprocessor pFactory, QueryExecutionFactory qef) {
        super(pFactory, qef);
        init();
    }

    public ParallelPathCollector(FactPreprocessor pFactory, QueryExecutionFactory qef, int maxNumberOfTriples) {
        super(pFactory, qef, maxNumberOfTriples);
        init();
    }

    public ParallelPathCollector(FactPreprocessor pFactory, QueryExecutionFactory qef, String[] namespaces) {
        super(pFactory, qef, namespaces);
        init();
    }

    public ParallelPathCollector(FactPreprocessor pFactory, QueryExecutionFactory qef, String[] namespaces,
            int maxNumberOfTriples) {
        super(pFactory, qef, namespaces, maxNumberOfTriples);
        init();
    }

    private void init() {
        paths = Collections.synchronizedMap(paths);
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    protected Set<QRestrictedPath> getPathsOfProperty(String propertyIRI) {
        if (paths.containsKey(propertyIRI)) {
            return paths.get(propertyIRI);
        } else {
            Set<QRestrictedPath> pathsOfP = Collections.synchronizedSet(new HashSet<>());
            paths.put(propertyIRI, pathsOfP);
            return pathsOfP;
        }
    }

    public void collectPaths(String inputFile, Lang lang, QueryExecutionFactory qef) {
        super.collectPaths(inputFile, lang, qef);
        try {
            while (futures.size() > 0) {
                futures.poll().get();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Got interrupted while waiting for path search to end. Returning without checking "
                    + futures.size() + " job states.", e);
        } catch (ExecutionException e) {
            LOGGER.error("Got an execution exception during the path search. Returning.", e);
        }
    }

    @Override
    protected void searchPaths(Resource s, Predicate predicate, Resource o, Set<QRestrictedPath> pathsOfP) {
        futures.add(executor.submit(new Runnable() {
            @Override
            public void run() {
                pathsOfP.addAll(searcher.search(s, predicate, o));
            }
        }));
    }

    protected boolean performPathSearch(Resource s, Property p, Resource o) {
        if ((maxNumberOfTriples <= 0) || (count < maxNumberOfTriples)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() throws Exception {
        if (executor != null) {
            executor.shutdown();
        }
    }
}