package org.dice_research.glisten;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactoryDataset;
import org.apache.jena.atlas.lib.ProgressMonitor;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.ProgressStreamRDF;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDF2;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.fc.data.Predicate;
import org.dice_research.fc.paths.FactPreprocessor;
import org.dice_research.fc.paths.FileBasedPredicateFactory;
import org.dice_research.glisten.falsegen.FalseSORetriever;
import org.dice_research.glisten.falsegen.SamplingFalseSORetriever;
import org.dice_research.glisten.stream.StmtMetadataGenerator;
import org.dice_research.rdf.stream.map.RDFStreamTripleFlatMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class takes the previously selected true triples (e.g., generated with
 * the TrainTestSplitter) and generates false test triples for each of the given
 * true triples. The output contains the facts as re-ified rdf:Statement
 * instances.
 */
public class TestDataGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataGenerator.class);

    private static final Node TRUTH_PREDICATE_NODE = NodeFactory.createURI("http://swc2017.aksw.org/hasTruthValue");
    private static final Node TRUE_NODE = NodeFactory.createLiteral("1.0", XSDDatatype.XSDdouble);
    private static final Node FALSE_NODE = NodeFactory.createLiteral("0.0", XSDDatatype.XSDdouble);

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println(
                    "Wrong number of arguments.\nUsage: TestDataGenerator <dataset-file> <pos-test-file> <output-file> <seed>");
            return;
        }
        File datasetFile = new File(args[0]);
        File posTestFile = new File(args[1]);
        File outputFile = new File(args[2]);
        long seed = Long.parseLong(args[3]);

        LOGGER.info("Reading predicates file...");
        FactPreprocessor pFactory = null;
        try (InputStream is = COPAAL_Preprocessor.class.getClassLoader()
                .getResourceAsStream("collected_dbo_predicates.json");) {
            pFactory = FileBasedPredicateFactory.create(is);
        }

        try (Writer out = new FileWriter(outputFile)) {
            // Create stream starting from the end
            StreamRDF outStream = StreamRDFLib.writer(out);
            ProgressMonitor monitor = ProgressMonitor.create(LOGGER, "Test triples", 10000, 10);
            outStream = new ProgressStreamRDF(outStream, monitor);

            StmtMetadataGenerator metadataAdder = new StmtMetadataGenerator("http://dice-research.org/glisten-test/");
            metadataAdder.setPredicates(TRUTH_PREDICATE_NODE);
            metadataAdder.setObjects(TRUE_NODE);
            outStream = new RDFStreamTripleFlatMapper(metadataAdder, outStream);

            LOGGER.info("Stream true triples...");
            PropertyHistogramGenerator histGenerator = new PropertyHistogramGenerator();
            StreamRDF histStream = new StreamRDF2(outStream, histGenerator);

            monitor.start();
            histStream.start();
            RDFDataMgr.parse(histStream, posTestFile.toURI().toURL().toString(), Lang.NT);

            Map<String, Integer> counts = histGenerator.getCounts();
            List<String> properties = new ArrayList<>(counts.keySet());
            LOGGER.info("Counted {} facts with {} different properties...",
                    counts.values().stream().mapToInt(i -> i).sum(), properties.size());
            Collections.sort(properties);
            histGenerator = null;
            histStream = null;

            metadataAdder.setObjects(FALSE_NODE);
            LOGGER.info("Reading Model ...");
            Model model = ModelFactory.createDefaultModel();
            model.read(datasetFile.toURI().toURL().toString());
            Dataset dataset = DatasetFactory.create(model);
            LOGGER.info("Generating false triples...");
            try (QueryExecutionFactory qef = new QueryExecutionFactoryDataset(dataset)) {
                FalseSORetriever retriever = new SamplingFalseSORetriever(qef, seed);
                for (String property : properties) {
                    LOGGER.info("Starting with property {} ...", property);
                    // Subject and object do not matter. We only need them to avoid null pointer
                    // exceptions
                    Predicate predicate = pFactory.generatePredicate(
                            new StatementImpl(RDF.Statement, new PropertyImpl(property), RDF.Statement));
                    streamPairs(retriever.retrieveSOPairsForPredicate(predicate, counts.get(property)), property, outStream);
                }
            }

            outStream.finish();
            monitor.finish();
            LOGGER.info("Finished");
        }
    }

    public static void streamPairs(Collection<String[]> pairs, String property, StreamRDF stream) {
        Node pNode = NodeFactory.createURI(property);
        for (String[] pair : pairs) {
            stream.triple(new Triple(NodeFactory.createURI(pair[0]), pNode, NodeFactory.createURI(pair[1])));
        }
    }
}
