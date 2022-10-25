package org.dice_research.glisten;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.jena.atlas.lib.ProgressMonitor;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.ProgressStreamRDF;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.riot.system.StreamRDFLib;
import org.dice_research.rdf.stream.filter.NodeFilterBasedTripleFilter;
import org.dice_research.rdf.stream.filter.RDFStreamTripleFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selects only triples that have IRIs on all positions.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class IriTripleSelector extends StreamRDFBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(IriTripleSelector.class);

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Error: wrong usage. IriTripleSelector <input-file> <output-file>");
            return;
        }
        String inputFile = args[0];
        String outputFile = args[1];

        try (Writer writer = new FileWriter(outputFile)) {
            StreamRDF stream = StreamRDFLib.writer(writer);
            // Write triples with the chosen property to the currently created output
            stream = new RDFStreamTripleFilter(new NodeFilterBasedTripleFilter(Node::isURI, null, Node::isURI), stream);

            // Add monitor at the beginning of the stream
            ProgressMonitor monitorS = ProgressMonitor.create(LOGGER, "Processed triples", 100000, 10);
            stream = new ProgressStreamRDF(stream, monitorS);

            // Start reading triples from the input file
            monitorS.start();
            stream.start();
            RDFDataMgr.parse(stream, inputFile, Lang.NT);
            monitorS.finish();
            stream.finish();
            LOGGER.info("Finished");
        }
    }
}
