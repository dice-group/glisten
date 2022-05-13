package org.dice_research.fc.ks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RSIterator;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDF2KStreamTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDF2KStreamTransformer.class);

    public static final Property TRUTH_PROPERTY = ResourceFactory
            .createProperty("http://swc2017.aksw.org/hasTruthValue");

    protected Map<String, Integer> entities2ID = new HashMap<String, Integer>();
    protected Map<String, Integer> relations2ID = new HashMap<String, Integer>();
    protected Map<Integer, String> ID2entities = new HashMap<Integer, String>();
    protected Map<Integer, String> ID2relations = new HashMap<Integer, String>();

    public void run(File trainingDataFile, File testFactsFile, File outputDir) {
        Model model = null;
        Model testFacts = null;
        try {
            model = readFileToModel(trainingDataFile);
            if (testFactsFile != null) {
                testFacts = readFileToModel(testFactsFile);
            }
        } catch (IOException e) {
            LOGGER.error("Error while reading input models. Aborting.", e);
            return;
        }

        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                LOGGER.error("Couldn't create output directory. Aborting.");
                return;
            }
        }

        // build entity and relations dictionary
        try (PrintStream pout = new PrintStream(outputDir.getAbsolutePath() + File.separator + "adjacency.tsv")) {
            StmtIterator iter = model.listStatements();
            while (iter.hasNext()) {
                Statement curStmt = iter.next();

                if (curStmt.getObject().isResource()) {
                    Resource subject = curStmt.getSubject();
                    Property predicate = curStmt.getPredicate();
                    Resource object = curStmt.getObject().asResource();

                    entities2ID.putIfAbsent(subject.toString(), entities2ID.size());
                    entities2ID.putIfAbsent(object.toString(), entities2ID.size());
                    relations2ID.putIfAbsent(predicate.toString(), relations2ID.size());

                    ID2entities.putIfAbsent(entities2ID.get(subject.toString()), subject.toString());
                    ID2entities.putIfAbsent(entities2ID.get(object.toString()), object.toString());
                    ID2relations.putIfAbsent(relations2ID.get(predicate.toString()), predicate.toString());

                    Integer subjID = entities2ID.get(subject.toString());
                    Integer objID = entities2ID.get(object.toString());
                    Integer predID = relations2ID.get(predicate.toString());

                    if (subjID == null || objID == null || predID == null) {
                        continue;
                    }

                    // subject - object - predicate format
                    pout.print(subjID);
                    pout.print('\t');
                    pout.print(objID);
                    pout.print('\t');
                    pout.println(predID);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error while writing training data. Aborting.", e);
            return;
        }

        try {
            printIDsMap(ID2entities, outputDir.getAbsolutePath() + File.separator + "nodes.tsv");
            printIDsMap(ID2relations, outputDir.getAbsolutePath() + File.separator + "relations.tsv");
        } catch (IOException e) {
            LOGGER.error("Error while writing ID mapping files. Aborting.", e);
            return;
        }

        if (testFacts != null) {
            try (PrintStream pout = new PrintStream(outputDir.getAbsolutePath() + File.separator + "test_facts.csv")) {
                printTestFacts(pout, testFacts);
            } catch (FileNotFoundException e) {
                LOGGER.error("Error while writing test facts. Aborting.", e);
                return;
            }
        }

        // NOT NEEDED ANYMORE, USE KS CODE INSTEAD
        // computeCoOccurrenceMatrix(new ArrayList<Integer>(ID2relations.keySet()),
        // ID2relations);
    }

    public static void printStringToFile(String string, File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(string);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints an id to object map as a TSV file: ID /tab/ Object
     * 
     * @param ID2Object
     * @param fileName
     * @throws IOException
     */
    public static void printIDsMap(Map<Integer, String> ID2Object, String fileName) throws IOException {
        try (PrintStream pout = new PrintStream(new File(fileName))) {
            ID2Object.keySet().stream().sorted().forEach(id -> printMapping(id, ID2Object.get(id), pout));
        }
    }

    protected static void printMapping(Integer id, String value, PrintStream pout) {
        pout.print(id);
        pout.print('\t');
        pout.println(value);
    }

    /**
     * Gets CSV of facts in the form of sid,sub,pid,pred,oid,obj,class
     * 
     * @param builder
     * @param facts
     * @param isTrue
     * @return
     */
    public void printTestFacts(PrintStream pout, Model facts) {
        pout.println("sid,sub,pid,pred,oid,obj,class");
        RSIterator iterator = facts.listReifiedStatements();
        ReifiedStatement rs;
        Statement s;
        while (iterator.hasNext()) {
            rs = iterator.next();
            s = rs.getStatement();
            Resource subject = s.getSubject();
            Resource object = s.getObject().asResource();
            Property predicate = s.getPredicate();

            int sid = entities2ID.get(subject.getURI());
            int oid = entities2ID.get(object.getURI());
            int pid = relations2ID.get(predicate.getURI());

            boolean isTrue = false;
            if (rs.hasLiteral(TRUTH_PROPERTY, 1.0)) {
                isTrue = true;
            } else {
                if (!rs.hasLiteral(TRUTH_PROPERTY, 0.0)) {
                    LOGGER.error("Found a reified statement without the expected truth value: {}", rs);
                    return;
                }
            }

            pout.print(sid);
            pout.print(",\"");
            pout.print(subject.toString());
            pout.print("\",");
            pout.print(pid);
            pout.print(",\"");
            pout.print(predicate.toString());
            pout.print("\",");
            pout.print(oid);
            pout.print(",\"");
            pout.print(object.toString());
            pout.print("\",");
            pout.println(isTrue);
        }
    }

    public static Model readFileToModel(File file) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        model.read(file.toURI().toURL().toString());
        return model;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println(
                    "Wrong number of arguments.\nUsage: RDF2KStreamTransformer <dataset-file> <output-dir> [test-file]");
            return;
        }
        File datasetFile = new File(args[0]);
        File outputDir = new File(args[1]);
        if(!outputDir.exists()) {
            outputDir.mkdir();
        }
        File testFile = null;
        if (args.length > 2) {
            testFile = new File(args[2]);
        }

        RDF2KStreamTransformer transformer = new RDF2KStreamTransformer();
        transformer.run(datasetFile, testFile, outputDir);
    }
}
