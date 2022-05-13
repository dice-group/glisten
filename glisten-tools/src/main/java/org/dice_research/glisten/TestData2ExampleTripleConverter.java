package org.dice_research.glisten;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RSIterator;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;

/**
 * A simple converter of test triple file to a directory of example triples
 * which will be used to identify paths.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class TestData2ExampleTripleConverter {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Error: wrong usage. TestDataFilter <test-file> <output-directory>");
            return;
        }
        File testFile = new File(args[0]);
        Lang testFileLang = Lang.NT;
        File exampleDirectory = new File(args[1]);

        Model testModel = ModelFactory.createDefaultModel();
        testModel.read(testFile.toURI().toURL().toString(), testFileLang.getName());

        Map<String, Writer> writers = new HashMap<>();
        Map<String, StreamRDF> streams = new HashMap<>();

        RSIterator iterator = testModel.listReifiedStatements();
        ReifiedStatement rs;
        Statement s;
        String property;
        StreamRDF stream;
        while (iterator.hasNext()) {
            rs = iterator.next();
            //if (TestDataFilter.isStatementTrue(rs, testModel)) {
                s = rs.getStatement();
                property = s.getPredicate().getURI();
                if (streams.containsKey(property)) {
                    stream = streams.get(property);
                } else {
                    String exampleFile = exampleDirectory.getAbsolutePath() + File.separator
                            + property.substring(Util.splitNamespaceXML(property)) + ".nt";
                    Writer out = new FileWriter(exampleFile);
                    writers.put(property, out);
                    stream = StreamRDFLib.writer(out);
                    stream.start();
                    streams.put(property, stream);
                }
                stream.triple(s.asTriple());
            //}
        }
        streams.values().stream().forEach(st -> st.finish());
        writers.values().stream().forEach(w -> closeWriter(w));
    }

    private static void closeWriter(Writer writer) {
        try {
            writer.close();
        } catch (Exception e) {
        }
    }
}
