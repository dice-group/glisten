package org.dice_research.glisten;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactoryDataset;
import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.fc.data.Predicate;
import org.dice_research.fc.data.QRestrictedPath;
import org.dice_research.fc.paths.FactPreprocessor;
import org.dice_research.fc.paths.FileBasedPredicateFactory;
import org.dice_research.fc.paths.IPropertyBasedPathScorer;
import org.dice_research.fc.paths.scorer.ICountRetriever;
import org.dice_research.fc.paths.scorer.NPMIBasedScorer;
import org.dice_research.fc.paths.scorer.count.InMemoryCountingCountRetriever;
import org.dice_research.fc.paths.scorer.count.TentrisBasedCountRetriever;
import org.dice_research.fc.paths.scorer.count.decorate.CachingCountRetrieverDecorator;
import org.dice_research.fc.paths.scorer.count.max.TentrisBasedMaxCounter;
import org.dice_research.fc.serialization.PathDeserializer;
import org.dice_research.fc.serialization.PathSerializer;
import org.dice_research.fc.sparql.path.BGPBasedPathClauseGenerator;
import org.dice_research.fc.tentris.ClientBasedTentrisAdapter;
import org.dice_research.fc.tentris.TentrisAdapter;
import org.dice_research.glisten.collect.PathCollector;
import org.dice_research.glisten.collect.SequentialPathCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * General preprocessor. For tentris, it is suggested to use the "4Tentris"
 * variant.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class COPAAL_Preprocessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(COPAAL_Preprocessor.class);

    public static final int MAX_PATH_LENGTH = 3;
    public static final String SCORED_PATHS_SUFFIX = "_scored.paths";
    public static final boolean USE_TENTRIS = true;

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println(
                    "Error: wrong usage. COPAAL_Preprocessor <property-file> <example-triple-directory> <RDF-dataset-file> <count-SPARQL-endpoint> <output-directory> [max-triples-per-property]");
            return;
        }
        File propertiesFile = new File(args[0]);
        File exampleDirectory = new File(args[1]);
        Lang exampleLang = Lang.NT;
        File datasetFile = new File(args[2]);
        String endpoint = args[3];
        File outputDir = new File(args[4]);
        int maxNumberOfTriples = -1;
        if (args.length > 5) {
            maxNumberOfTriples = Integer.parseInt(args[5]);
        }

        // Init JSON IO
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(QRestrictedPath.class, new PathSerializer());
        module.addDeserializer(QRestrictedPath.class, new PathDeserializer());
        mapper.registerModule(module);

        LOGGER.info("Reading predicates file...");
        FactPreprocessor pFactory = null;
        try (InputStream is = COPAAL_Preprocessor.class.getClassLoader()
                .getResourceAsStream("collected_dbo_predicates.json");) {
            pFactory = FileBasedPredicateFactory.create(is);
        }

        List<String> properties = FileUtils.readLines(propertiesFile, StandardCharsets.UTF_8);

        LOGGER.info("Reading Model...");
        Model model = ModelFactory.createDefaultModel();
        model.read(datasetFile.toURI().toURL().toString());
        Dataset dataset = DatasetFactory.create(model);
        try (QueryExecutionFactory qef = new QueryExecutionFactoryDataset(dataset)) {

//      Model model = ModelFactory.createDefaultModel();
//      model.read(datasetFile.toURI().toURL().toString());
//      Dataset dataset = DatasetFactory.create(model);
//      try (QueryExecutionFactory qef = new QueryExecutionFactoryDataset(dataset)) {
            // try (QueryExecutionFactory qefClient = new
            // QueryExecutionFactoryHttp(endpoint)) {
            try (TentrisAdapter tentris = new ClientBasedTentrisAdapter(HttpClients.createDefault(), endpoint);) {
                // QueryExecutionFactory qef = new QueryExecutionFactoryDelay(qefClient, 100);
                // qef = new QueryExecutionFactoryTimeout(qef, 30, TimeUnit.SECONDS, 30,
                // TimeUnit.SECONDS);

                for (String property : properties) {
                    String outputFilePrefix = outputDir.getAbsolutePath() + File.separator
                            + property.substring(Util.splitNamespaceXML(property));
                    String foundPathsFileName = outputFilePrefix + "_found.paths";
                    String scoredPathsFileName = outputFilePrefix + SCORED_PATHS_SUFFIX;
                    File scoredPathsFile = new File(scoredPathsFileName);
                    if (scoredPathsFile.exists()) {
                        LOGGER.info("{} already exists. The property {} won't be handled further.", scoredPathsFileName,
                                property);
                    } else {
                        LOGGER.info("Starting property {} ...", property);
                        // Subject and object do not matter. We only need them to avoid null pointer
                        // exceptions
                        Predicate predicate = pFactory.generatePredicate(
                                new StatementImpl(RDF.Statement, new PropertyImpl(property), RDF.Statement));

                        File pathsFile = new File(foundPathsFileName);
                        Set<QRestrictedPath> pathsForProperty = null;
                        Map<String, Set<QRestrictedPath>> paths = null;
                        if (pathsFile.exists()) {
                            LOGGER.info("Trying to read found paths from file...");
                            MappingIterator<QRestrictedPath> iterator = mapper.readerFor(QRestrictedPath.class)
                                    .readValues(pathsFile);
                            if (iterator.hasNext()) {
                                pathsForProperty = new HashSet<>();
                                while (iterator.hasNext()) {
                                    pathsForProperty.add(iterator.next());
                                }
                            }
                        }
                        if (pathsForProperty == null) {
                            LOGGER.info("Collecting paths...");

                            String exampleFile = exampleDirectory.getAbsolutePath() + File.separator
                                    + property.substring(Util.splitNamespaceXML(property)) + ".nt";
//                        try (QueryExecutionFactory qef = new QueryExecutionFactoryDataset(dataset)) {
                            // For each example triple search for paths given that triple and put the paths
                            // into a set
                            try (PathCollector collector = new SequentialPathCollector(pFactory, qef,
                                    maxNumberOfTriples);) {
                                collector.collectPaths(exampleFile, exampleLang, qef);
                                paths = collector.getPaths();
                            }
                            pathsForProperty = paths.get(property);
                            if (pathsForProperty == null) {
                                pathsForProperty = new HashSet<>();
                            }
                            LOGGER.info("{} paths collected.", pathsForProperty.size());
                            mapper.writeValue(pathsFile, pathsForProperty);
//                        }
                        }

                        LOGGER.info("Scoring paths...");
                        // For each path in the final set, run count queries
                        // Count the occurrence of the path
                        // Count the co-occurrence of the path and the property
                        ICountRetriever retriever;
                        if (USE_TENTRIS) {
                            retriever = new TentrisBasedCountRetriever(tentris, new TentrisBasedMaxCounter(tentris),
                                    new BGPBasedPathClauseGenerator());
                        } else {
                            retriever = new InMemoryCountingCountRetriever(model, qef);
                        }

//            ICountRetriever retriever = new PairCountRetriever(qef, new DefaultMaxCounter(qef),
//                    new BGPBasedPathClauseGenerator());
                        retriever = new CachingCountRetrieverDecorator(retriever, 2, 2, 5, 5);
                        // Calculate the NPMI / PNPMI and write the scored paths to a file
                        IPropertyBasedPathScorer pathScorer = new NPMIBasedScorer(retriever);
                        List<QRestrictedPath> scoredPaths = new ArrayList<>();
                        int n = 0;
                        for (QRestrictedPath path : pathsForProperty) {
                            scoredPaths.add(pathScorer.score(predicate, path));
                            ++n;
                            LOGGER.debug("Scored {}/{} paths", n, pathsForProperty.size());
                        }
                        LOGGER.info("Paths have been scored. Serializing paths...");
                        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(scoredPathsFileName))) {
                            mapper.writeValue(os, scoredPaths);
                        }
                    }
                }
            } // try tentris
        } // try qef
        LOGGER.info("Finished.");
    }
}
