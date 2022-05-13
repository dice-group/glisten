package org.dice_research.glisten;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.jena.atlas.lib.ProgressMonitor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.system.ProgressStreamRDF;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple class that loads reified statements from a test file and removes
 * statements that have a property that is not listed in the (updated)
 * properties file.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class TestDataFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataFilter.class);

    public static final Property TRUTH_PROPERTY = ResourceFactory
            .createProperty("http://swc2017.aksw.org/hasTruthValue");

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println(
                    "Error: wrong usage. TestDataFilter <test-file> <properties-file> <output-file> [maxCountPerProperty]");
            return;
        }
        File testFile = new File(args[0]);
        Lang testFileLang = Lang.NT;
        File propertiesFile = new File(args[1]);
        File outputFile = new File(args[2]);
        int maxCountPerProperty = 0;
        if (args.length > 3) {
            maxCountPerProperty = Integer.parseInt(args[3]);
        }

        Model testModel = ModelFactory.createDefaultModel();
        testModel.read(testFile.toURI().toURL().toString(), testFileLang.getName());

        Set<String> properties = new HashSet<>(FileUtils.readLines(propertiesFile, StandardCharsets.UTF_8));

        Map<String, int[]> counts = new HashMap<>();
        for (String p : properties) {
            counts.put(p, new int[2]);
        }

        try (Writer out = new FileWriter(outputFile)) {
            // Create stream starting from the end
            StreamRDF stream = StreamRDFLib.writer(out);
            ProgressMonitor monitor = ProgressMonitor.create(LOGGER, "Written triples", 1000, 10);
            stream = new ProgressStreamRDF(stream, monitor);

            stream.start();
            monitor.start();
            StmtIterator stmts = testModel.listStatements(null, RDF.predicate, (RDFNode) null);
            Statement s;
            RDFNode o;
            boolean shouldBeWritten = false;
            while (stmts.hasNext()) {
                s = stmts.next();
                o = s.getObject();
                shouldBeWritten = o.isResource() && properties.contains(o.asResource().getURI());
                if ((maxCountPerProperty > 0) && (shouldBeWritten)) {
                    int flagId = isStatementTrue(s.getSubject(), testModel) ? 1 : 0;
                    int pCounts[] = counts.get(o.asResource().getURI());
                    if (pCounts[flagId] < maxCountPerProperty) {
                        ++pCounts[flagId];
                    } else {
                        shouldBeWritten = false;
                    }
                }
                if (shouldBeWritten) {
                    printStatement(s.getSubject(), testModel, stream);
                }
            }
            monitor.finish();
            stream.finish();
        }

    }

    private static void printStatement(Resource subject, Model facts, StreamRDF writer) {
        StmtIterator stmts = facts.listStatements(subject, null, (RDFNode) null);
        while (stmts.hasNext()) {
            writer.triple(stmts.next().asTriple());
        }
    }

    public static boolean isStatementTrue(Resource subject, Model facts) {
        StmtIterator stmts = facts.listStatements(subject, TRUTH_PROPERTY, (RDFNode) null);
        while (stmts.hasNext()) {
            Statement s = stmts.next();
            if (s.getObject().isLiteral() && s.getDouble() > 0) {
                return true;
            }
        }
        return false;
    }
}
