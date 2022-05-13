package org.dice_research.fc.ks;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RSIterator;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;

public class KStream2RDFTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KStream2RDFTransformer.class);

    public static final int SUBJECT_COLUMN = 1;
    public static final int PREDICATE_COLUMN = 3;
    public static final int OBJECT_COLUMN = 5;
    public static final int SCORE_COLUMN = 7;
    public static final int SOFTMAX_SCORE_COLUMN = 11;

    protected Model testModel;

    protected void readModel(File testFile) throws IOException {
        testModel = RDF2KStreamTransformer.readFileToModel(testFile);
    }

    protected void transform(File resultFile, int scoreColumnId, File outputFile) throws IOException {
        Map<String, String> results = new HashMap<>();
        try (Reader reader = new FileReader(resultFile, StandardCharsets.UTF_8);
                CSVReader csvReader = new CSVReader(reader)) {
            // ignore first line
            csvReader.readNext();
            String[] values = csvReader.readNext();
            while (values != null) {
                if (values.length >= scoreColumnId) {
                    results.put(values[SUBJECT_COLUMN] + values[PREDICATE_COLUMN] + values[OBJECT_COLUMN],
                            values[scoreColumnId]);
                }
                values = csvReader.readNext();
            }
        }
        try (Writer out = new FileWriter(outputFile)) {
            StreamRDF outStream = StreamRDFLib.writer(out);
            outStream.start();
            RSIterator iterator = testModel.listReifiedStatements();
            ReifiedStatement rs;
            Statement s;
            while (iterator.hasNext()) {
                rs = iterator.next();
                s = rs.getStatement();
                Resource subject = s.getSubject();
                Resource object = s.getObject().asResource();
                Property predicate = s.getPredicate();
                String key = subject.getURI() + predicate.getURI() + object.getURI();
                if (!results.containsKey(key)) {
                    throw new IllegalStateException("Couldn't find " + key);
                }
                outStream.triple(new Triple(rs.asNode(), RDF2KStreamTransformer.TRUTH_PROPERTY.asNode(),
                        NodeFactory.createLiteral(results.get(key), XSDDatatype.XSDdouble)));
            }
            outStream.finish();
            LOGGER.info("Finished");
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println(
                    "Wrong number of arguments.\nUsage: RDF2KStreamTransformer <result-file> <test-file> <output-file> [softmax-output-file]");
            return;
        }
        File resultFile = new File(args[0]);
        File testFile = new File(args[1]);
        File outputFile = new File(args[2]);

        KStream2RDFTransformer transformer = new KStream2RDFTransformer();
        transformer.readModel(testFile);

        transformer.transform(resultFile, SCORE_COLUMN, outputFile);
        if (args.length > 3) {
            File softOutputFile = new File(args[3]);
            transformer.transform(resultFile, SOFTMAX_SCORE_COLUMN, softOutputFile);
        }
    }
}
