package org.dice_research.glisten;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactoryDataset;
import org.apache.commons.io.FileUtils;
import org.apache.jena.atlas.lib.ProgressMonitor;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.system.ProgressStreamRDF;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.fc.IFactChecker;
import org.dice_research.fc.data.FactCheckingResult;
import org.dice_research.fc.data.IPieceOfEvidence;
import org.dice_research.fc.data.QRestrictedPath;
import org.dice_research.fc.paths.FactPreprocessor;
import org.dice_research.fc.paths.FileBasedPredicateFactory;
import org.dice_research.fc.paths.PathBasedFactChecker;
import org.dice_research.fc.paths.filter.SimpleScoreFilter;
import org.dice_research.fc.paths.scorer.AskQueryBasedScorer;
import org.dice_research.fc.paths.search.KnownPathsBasedPathSearcher;
import org.dice_research.fc.serialization.PathDeserializer;
import org.dice_research.fc.sparql.path.PropPathBasedPathClauseGenerator;
import org.dice_research.fc.sum.FixedSummarist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class COPAAL_Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(COPAAL_Executor.class);

    public static final int MAX_PATH_LENGTH = 3;

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println(
                    "Error: wrong usage. COPAAL_Executor <test_file> <path_directory> <properties_file> <RDF-dataset-file> <output-file> [evidence-output-file]");
            return;
        }
        File testFile = new File(args[0]);
        Lang testFileLang = Lang.NT;
        File pathDirectory = new File(args[1]);
        File propertiesFile = new File(args[2]);
        File datasetFile = new File(args[3]);
        File outputFile = new File(args[4]);
        File evidenceFile = null;
        if (args.length > 5) {
            evidenceFile = new File(args[5]);
        }

        // Read test data
        LOGGER.info("Read test data...");
        Model testModel = ModelFactory.createDefaultModel();
        testModel.read(testFile.toURI().toURL().toString(), testFileLang.toString());

        // Read scored paths
        LOGGER.info("Read scored paths...");
        KnownPathsBasedPathSearcher pathSearcher = loadPathsFromFile(pathDirectory, propertiesFile);

        LOGGER.info("Create Fact preprocessor...");
        FactPreprocessor pFactory = null;
        try (InputStream is = COPAAL_Executor.class.getClassLoader()
                .getResourceAsStream("collected_dbo_predicates.json");) {
            pFactory = FileBasedPredicateFactory.create(is);
        }

        LOGGER.info("Reading reference dataset paths...");
        Model refModel = ModelFactory.createDefaultModel();
        refModel.read(datasetFile.toURI().toURL().toString());
        Dataset dataset = DatasetFactory.create(refModel);
        try (QueryExecutionFactory qef = new QueryExecutionFactoryDataset(dataset)) {
            // Create path checker
            PathBasedFactChecker checker = new PathBasedFactChecker(pFactory, pathSearcher,
                    new AskQueryBasedScorer(qef, new PropPathBasedPathClauseGenerator()), new FixedSummarist());
            checker.setScoreFilter(new SimpleScoreFilter(s -> s != 0));

            try (Writer out = new FileWriter(outputFile);
                    PrintStream evidenceOut = evidenceFile != null ? new PrintStream(evidenceFile) : null) {
                // Create stream starting from the end
                StreamRDF stream = StreamRDFLib.writer(out);
                ProgressMonitor monitor = ProgressMonitor.create(LOGGER, "Checked triples", 1000, 10);
                stream = new ProgressStreamRDF(stream, monitor);

                stream.start();
                monitor.start();
                LOGGER.info("Start fact checking...");
                checkFacts(checker, testModel, stream, evidenceOut);
                monitor.finish();
                stream.finish();
            }
        }
        LOGGER.info("Finished.");
    }

    private static KnownPathsBasedPathSearcher loadPathsFromFile(File pathDirectory, File propertiesFile)
            throws IOException {
        KnownPathsBasedPathSearcher searcher = new KnownPathsBasedPathSearcher();
        // Init JSON IO
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(QRestrictedPath.class, new PathDeserializer());
        mapper.registerModule(module);

        List<String> properties = FileUtils.readLines(propertiesFile, StandardCharsets.UTF_8);

        for (String property : properties) {
            LOGGER.info("Reading paths for {}...", property);
            File propertyFile = new File(pathDirectory.getAbsolutePath() + File.separator
                    + property.substring(Util.splitNamespaceXML(property)) + COPAAL_Preprocessor.SCORED_PATHS_SUFFIX);
            if (propertyFile.exists()) {
                MappingIterator<QRestrictedPath> iterator = mapper.readerFor(QRestrictedPath.class)
                        .readValues(propertyFile);
                if (iterator.hasNext()) {
                    Set<QRestrictedPath> pathsForProperty = new HashSet<>();
                    while (iterator.hasNext()) {
                        pathsForProperty.add(iterator.next());
                    }
                    searcher.addPaths(property, pathsForProperty);
                } else {
                    LOGGER.info("Not paths available for {}", property);
                }
            } else {
                LOGGER.warn("Couldn't find expected file {}. The property {} will be mainly ignored.",
                        propertyFile.getAbsolutePath(), property);
            }
        }
        return searcher;
    }

    private static void checkFacts(IFactChecker factChecker, Model facts, StreamRDF writer, PrintStream evidenceOut) {
        Property VERACITY_PROPERTY = ResourceFactory.createProperty("http://swc2017.aksw.org/hasTruthValue");
        StmtIterator stmts = facts.listStatements(null, RDF.type, RDF.Statement);
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            Resource stmtR = stmt.getSubject();
            Statement fact = ResourceFactory.createStatement(stmtR.getPropertyResourceValue(RDF.subject),
                    stmtR.getPropertyResourceValue(RDF.predicate).as(Property.class),
                    stmtR.getPropertyResourceValue(RDF.object));

            FactCheckingResult result = factChecker.check(fact);
            if (evidenceOut != null) {
                evidenceOut.println("paths for " + fact.toString());
                for (IPieceOfEvidence e : result.getPiecesOfEvidence()) {
                    evidenceOut.print(e.getEvidence());
                    evidenceOut.print("->");
                    evidenceOut.print(e.getScore());
                }
            }

            writer.triple(new Triple(stmtR.asNode(), VERACITY_PROPERTY.asNode(), ResourceFactory
                    .createTypedLiteral(Double.toString(result.getVeracityValue()), XSDDatatype.XSDdouble).asNode()));
        }
    }

}
