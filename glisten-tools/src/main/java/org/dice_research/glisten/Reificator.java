package org.dice_research.glisten;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.jena.atlas.lib.ProgressMonitor;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.ProgressStreamRDF;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.dice_research.glisten.stream.StmtMetadataGenerator;
import org.dice_research.rdf.stream.map.RDFStreamTripleFlatMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a reification-based representation of all given triples and adds a
 * veracity value of 1.0.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class Reificator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Reificator.class);

    private static final Node TRUTH_PREDICATE_NODE = NodeFactory.createURI("http://swc2017.aksw.org/hasTruthValue");
    private static final Node TRUE_NODE = NodeFactory.createLiteral("1.0", XSDDatatype.XSDdouble);

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Wrong number of arguments.\nUsage: TestDataGenerator <triple-file> <output-file>");
            return;
        }
        File triplesFile = new File(args[0]);
        File outputFile = new File(args[1]);

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
            monitor.start();
            RDFDataMgr.parse(outStream, triplesFile.toURI().toURL().toString(), Lang.NT);

            outStream.finish();
            monitor.finish();
            LOGGER.info("Finished");
        }
    }
}
