package org.dice_research.glisten;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactoryDataset;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.riot.Lang;
import org.dice_research.fc.data.QRestrictedPath;
import org.dice_research.fc.paths.FactPreprocessor;
import org.dice_research.fc.paths.FileBasedPredicateFactory;
import org.dice_research.fc.serialization.PathDeserializer;
import org.dice_research.fc.serialization.PathSerializer;
import org.dice_research.glisten.collect.ParallelPathCollector;
import org.dice_research.glisten.collect.PathCollector;
import org.dice_research.glisten.collect.SequentialPathCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * This class implements solely the first part of the COPAAL preprocessing,
 * i.e., the search for paths. It is an alternative to the
 * {@link COPAAL_Preprocessor} and {@link COPAAL_Preprocessor4Tentris} classes
 * and covers their first part.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class COPAAL_Preprocessor_PathSearch {

    private static final Logger LOGGER = LoggerFactory.getLogger(COPAAL_Preprocessor_PathSearch.class);

    public static final int MAX_PATH_LENGTH = 3;
    
    public static final String NAMESPACES[] = new String[] { "http://dbpedia.org/ontology/" };

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println(
                    "Error: wrong usage. COPAAL_Preprocessor <property-file> <example-triple-directory> <RDF-dataset-file> <output-directory> <max-triples-per-property> [use-thread-pool(true/false)]");
            return;
        }
        File propertiesFile = new File(args[0]);
        File exampleDirectory = new File(args[1]);
        Lang exampleLang = Lang.NT;
        File datasetFile = new File(args[2]);
        File outputDir = new File(args[3]);
        int maxNumberOfTriples = Integer.parseInt(args[4]);
        boolean tempFlag = false;
        if (args.length > 5) {
            tempFlag = Boolean.parseBoolean(args[5]);
        }
        final boolean useParallelSearch = tempFlag;
        LOGGER.info("Parallel search set to {}.", useParallelSearch);

        LOGGER.info("Reading predicates file...");
        FactPreprocessor pFactory = null;
        try (InputStream is = COPAAL_Preprocessor_PathSearch.class.getClassLoader()
                .getResourceAsStream("collected_dbo_predicates.json");) {
            pFactory = FileBasedPredicateFactory.create(is);
        }
        if (pFactory == null) {
            LOGGER.error("Could't load ontology file. Aborting.");
            return;
        }

        LOGGER.info("Reading chosen properties file...");
        List<String> properties = FileUtils.readLines(propertiesFile, StandardCharsets.UTF_8);

        LOGGER.info("Reading Model...");
        Model model = ModelFactory.createDefaultModel();
        model.read(datasetFile.toURI().toURL().toString());
        Dataset dataset = DatasetFactory.create(model);
        final FactPreprocessor fPFactory = pFactory;
        try (QueryExecutionFactory qef = new QueryExecutionFactoryDataset(dataset);) {
            properties.parallelStream().forEach(p -> performPathSearch(p, qef, outputDir, exampleDirectory, exampleLang,
                    fPFactory, maxNumberOfTriples, useParallelSearch));
        } // try qef
        LOGGER.info("Finished.");
    }

    public static void performPathSearch(String property, QueryExecutionFactory qef, File outputDir,
            File exampleDirectory, Lang exampleLang, FactPreprocessor pFactory, int maxNumberOfTriples,
            boolean useParallelSearch) {
        LOGGER.info("Starting property {} ...", property);
        String outputFilePrefix = outputDir.getAbsolutePath() + File.separator
                + property.substring(Util.splitNamespaceXML(property));
        String foundPathsFileName = outputFilePrefix + "_found.paths";

        // Subject and object do not matter. We only need them to avoid null pointer
        // exceptions
        File pathsFile = new File(foundPathsFileName);
        if (!pathsFile.exists()) {
            try (PathCollector collector = useParallelSearch
                    ? new ParallelPathCollector(pFactory, qef, NAMESPACES, maxNumberOfTriples)
                    : new SequentialPathCollector(pFactory, qef, NAMESPACES, maxNumberOfTriples);) {
                // Init JSON IO
                ObjectMapper mapper = new ObjectMapper();
                SimpleModule module = new SimpleModule();
                module.addSerializer(QRestrictedPath.class, new PathSerializer());
                module.addDeserializer(QRestrictedPath.class, new PathDeserializer());
                mapper.registerModule(module);

                LOGGER.info("Collecting paths...");
                String exampleFile = exampleDirectory.getAbsolutePath() + File.separator
                        + property.substring(Util.splitNamespaceXML(property)) + ".nt";
                // For each example triple search for paths given that triple and put the paths
                // into a set
                collector.collectPaths(exampleFile, exampleLang, qef);
                Map<String, Set<QRestrictedPath>> pathsPerProperty = collector.getPaths();
                Set<QRestrictedPath> pathsForProperty = pathsPerProperty.get(property);
                if (pathsForProperty == null) {
                    pathsForProperty = new HashSet<>();
                }
                LOGGER.info("{} paths collected.", pathsForProperty.size());
                mapper.writeValue(pathsFile, pathsForProperty);
            } catch (Exception e) {
                LOGGER.error("Error while searching paths for property " + property + ".", e);
                if (pathsFile.exists()) {
                    pathsFile.delete();
                }
            }
        }
        LOGGER.info("Finished property {}.", property);
    }

}
