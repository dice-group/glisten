package org.dice_research.glisten;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.jena.atlas.lib.ProgressMonitor;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.impl.Util;
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
 * Selects examples of triples with a certain predicate from a large set of
 * triples.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class PropertyExampleSelector extends StreamRDFBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyExampleSelector.class);

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println(
                    "Error: wrong usage. PropertyExampleSelector <input-file> <predicate-list-file> <example-data-prob> <examples-output-directory> [seed]");
            return;
        }
        String inputFile = args[0];
        String predicatesFile = args[1];
        double probability = Double.parseDouble(args[2]);
        File outputDir = new File(args[3]);
        Long seed = null;
        if (args.length > 5) {
            seed = Long.parseLong(args[4]);
        }

        Set<String> properties = new HashSet<>(FileUtils.readLines(new File(predicatesFile), StandardCharsets.UTF_8));
        List<Writer> writers = new ArrayList<>();

        StreamRDF stream = StreamRDFLib.sinkNull();
        try {
            // Create stream starting from the end!
            for (String property : properties) {
                String fileName = outputDir.getAbsolutePath() + File.separator
                        + property.substring(Util.splitNamespaceXML(property)) + ".nt";
                Writer writer = new FileWriter(fileName);
                writers.add(writer);
                StreamRDF outStream1 = StreamRDFLib.writer(writer);
                // Write triples with the chosen property to the currently created output
                stream = new RDFStreamTripleFilter(
                        new NodeFilterBasedTripleFilter(null, (p -> property.equals(p.getURI())), null), outStream1,
                        stream);
            }

            // Sample triples from the stream
            stream = new RDFStreamTripleFilter(seed != null ? new SamplingFilter<Triple>(probability, seed)
                    : new SamplingFilter<Triple>(probability), stream);
            // We can only want triples with one of the given properties
            stream = new RDFStreamTripleFilter(new PropertyBasedTripleFilter(properties), stream);

            // We don't want any triples with literals; we only want to have IRIs as objects
            stream = new RDFStreamTripleFilter(new NodeFilterBasedTripleFilter(null, null, Node::isURI), stream);

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
        } finally {
            for (Writer writer : writers) {
                try {
                    writer.close();
                } catch (Exception e) {
                    // Nothing to do
                }
            }
        }
    }
}
