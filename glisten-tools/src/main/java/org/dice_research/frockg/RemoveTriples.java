package org.dice_research.frockg;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.jena.atlas.lib.ProgressMonitor;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.ProgressStreamRDF;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.dice_research.rdf.stream.filter.RDFStreamTripleFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveTriples {

private static final Logger LOGGER = LoggerFactory.getLogger(RemoveTriples.class);

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println(
                    "Error: wrong usage. RemoveTriples <input-file> <triples-to-remove> <output-file>");
            return;
        }
        Lang fileLang = Lang.NT;
        File inputFile = new File(args[0]);
        File triplesToRemoveFile = new File(args[1]);
        File outputFile = new File(args[2]);
        
        Model model = ModelFactory.createDefaultModel();
        model.read(triplesToRemoveFile.toURI().toURL().toString(), fileLang.getName());
        Graph graph = model.getGraph();

        try (Writer writer = new FileWriter(outputFile)) {
            StreamRDF stream = StreamRDFLib.writer(writer);
            ProgressMonitor monitorW = ProgressMonitor.create(LOGGER, "Written triples", 100000, 10);
            stream = new ProgressStreamRDF(stream, monitorW);
            // Write triples with the chosen property to the currently created output
            stream = new RDFStreamTripleFilter(t -> !graph.contains(t), stream);

            // Add monitor at the beginning of the stream
            ProgressMonitor monitorR = ProgressMonitor.create(LOGGER, "Read triples", 100000, 10);
            stream = new ProgressStreamRDF(stream, monitorR);

            // Start reading triples from the input file
            monitorR.start();
            monitorW.start();
            stream.start();
            RDFDataMgr.parse(stream, inputFile.toURI().toURL().toString(), fileLang);
            monitorR.finish();
            monitorW.finish();
            stream.finish();
            LOGGER.info("Finished");
        }
    }
}
