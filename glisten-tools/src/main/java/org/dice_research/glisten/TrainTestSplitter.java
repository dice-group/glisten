package org.dice_research.glisten;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.jena.atlas.lib.ProgressMonitor;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.ProgressStreamRDF;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.riot.system.StreamRDFLib;
import org.dice_research.rdf.stream.filter.NodeFilterBasedTripleFilter;
import org.dice_research.rdf.stream.filter.PropertyBasedTripleFilter;
import org.dice_research.rdf.stream.filter.RDFStreamTripleFilter;
import org.dice_research.rdf.stream.filter.SamplingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class takes an RDF dataset, a list of properties, a probability and an
 * output file. It will stream the given dataset, randomly sample triples with
 * the given properties based on the given probability and write them to the
 * output file.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class TrainTestSplitter extends StreamRDFBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrainTestSplitter.class);

    public static void main(String[] args) throws IOException {
        if (args.length < 5) {
            System.err.println(
                    "Error: wrong usage. TrainTestSplitter <input-file> <predicate-list-file> <test-data-prob> <pos-test-data-output-file> <remaining-data-output-file> [seed] [max-triple-per-prop]");
            return;
        }
        String inputFile = args[0];
        String predicatesFile = args[1];
        double probability = Double.parseDouble(args[2]);
        String chosenTriplesFile = args[3];
        String otherTriplesFile = args[4];
        Long seed = null;
        if (args.length > 5) {
            seed = Long.parseLong(args[5]);
        }
        int maxNumberOfTriples = -1;
        if (args.length > 6) {
            maxNumberOfTriples = Integer.parseInt(args[6]);
        }

        Set<String> properties = new HashSet<>(FileUtils.readLines(new File(predicatesFile), StandardCharsets.UTF_8));

        try (Writer chosenOut = new FileWriter(chosenTriplesFile); Writer otherOut = new FileWriter(otherTriplesFile)) {
            // Create stream starting from the end!
            StreamRDF chosenStream = StreamRDFLib.writer(chosenOut);
            ProgressMonitor monitor1 = ProgressMonitor.create(LOGGER, "Chosen triples", 100000, 10);
            chosenStream = new ProgressStreamRDF(chosenStream, monitor1);
            StreamRDF otherStream = StreamRDFLib.writer(otherOut);

            // We only want triples with one of the given properties
            // Instead of filtering them all at once, we will filter one after the other
            // stream = new RDFStreamTripleFilter(new PropertyBasedTripleFilter(properties),
            // stream, outStream2);
            StreamRDF stream = otherStream;
            for (String property : properties) {
                // Sample triples from the stream
                StreamRDF streamForProp = new RDFStreamTripleFilter(
                        seed != null ? new SamplingFilter<Triple>(probability, maxNumberOfTriples, seed)
                                : new SamplingFilter<Triple>(probability),
                        chosenStream, otherStream);
                // For each property, choose the facts in a separated stream
                stream = new RDFStreamTripleFilter(new PropertyBasedTripleFilter(property), streamForProp, stream);
            }
            // We don't want any triples with literals
            stream = new RDFStreamTripleFilter(new NodeFilterBasedTripleFilter(null, null, Node::isURI), stream,
                    otherStream);

            // Add monitor at the beginning of the stream
            ProgressMonitor monitorS = ProgressMonitor.create(LOGGER, "Processed triples", 100000, 10);
            stream = new ProgressStreamRDF(stream, monitorS);

            // Start reading triples from the input file
            monitorS.start();
            monitor1.start();
            stream.start();
            RDFDataMgr.parse(stream, inputFile, Lang.NT);
            monitorS.finish();
            monitor1.finish();
            stream.finish();
            LOGGER.info("Finished");
        }
    }
}
