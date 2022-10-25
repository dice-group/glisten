package org.dice_research.glisten;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.fc.data.Predicate;
import org.dice_research.fc.data.QRestrictedPath;
import org.dice_research.fc.paths.FactPreprocessor;
import org.dice_research.fc.paths.FileBasedPredicateFactory;
import org.dice_research.fc.paths.IPropertyBasedPathScorer;
import org.dice_research.fc.paths.scorer.ICountRetriever;
import org.dice_research.fc.paths.scorer.NPMIBasedScorer;
import org.dice_research.fc.paths.scorer.count.TentrisBasedCountRetriever;
import org.dice_research.fc.paths.scorer.count.decorate.CachingCountRetrieverDecorator;
import org.dice_research.fc.paths.scorer.count.max.TentrisBasedMaxCounter;
import org.dice_research.fc.serialization.PathDeserializer;
import org.dice_research.fc.serialization.PathSerializer;
import org.dice_research.fc.sparql.path.BGPBasedPathClauseGenerator;
import org.dice_research.fc.tentris.ClientBasedTentrisAdapter;
import org.dice_research.fc.tentris.TentrisAdapter;
import org.dice_research.glisten.COPAAL_Preprocessor4Tentris.PathCounter;
import org.dice_research.glisten.COPAAL_Preprocessor4Tentris.PathWritingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * This class implements the scoring part, i.e., the second part of the
 * {@link COPAAL_Preprocessor4Tentris} class.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class COPAAL_Preprocessor4Tentris_Scorer {

    private static final Logger LOGGER = LoggerFactory.getLogger(COPAAL_Preprocessor4Tentris_Scorer.class);

    public static final int MAX_PATH_LENGTH = 3;
    public static final String SCORED_PATHS_SUFFIX = "_scored.paths";

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println(
                    "Error: wrong usage. COPAAL_Preprocessor4Tentris_Scorer <property-file> <tentris-endpoint> "
                            + "<property-histrogram-file> <frequent-property-threshold> <output-directory> "
                            + "[known-scored-paths] [old-dataset-p-histogram] [old-dataset-c-histogram] "
                            + "[new-dataset-c-histogram]");
            return;
        }
        File propertiesFile = new File(args[0]);
        String endpoint = args[1];
        File propertiesHistogramFile = new File(args[2]);
        int frequentPropThreshold = Integer.parseInt(args[3]);
        File outputDir = new File(args[4]);
        File knownPaths = null;
        if (args.length > 5) {
            knownPaths = new File(args[5]);
        }
        File oldPHistogramFile = null;
        File oldCHistogramFile = null;
        File newCHistogramFile = null;
        if (args.length > 6) {
            oldPHistogramFile = new File(args[6]);
            oldCHistogramFile = new File(args[7]);
            newCHistogramFile = new File(args[8]);
        }

        // Init JSON IO
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(QRestrictedPath.class, new PathSerializer());
        module.addDeserializer(QRestrictedPath.class, new PathDeserializer());
        mapper.registerModule(module);

        LOGGER.info("Reading predicates file...");
        FactPreprocessor pFactory = null;
        try (InputStream is = COPAAL_Preprocessor4Tentris_Scorer.class.getClassLoader()
                .getResourceAsStream("collected_dbo_predicates.json");) {
            pFactory = FileBasedPredicateFactory.create(is);
        }

        LOGGER.info("Reading chosen properties file...");
        List<String> properties = FileUtils.readLines(propertiesFile, StandardCharsets.UTF_8);

        LOGGER.info("Reading properties histogram...");
        Map<String, Integer> newPHistogram = PropertyHistogramGenerator.createFromFile(propertiesHistogramFile).getCounts();
        Set<String> frequentProperties = newPHistogram.entrySet().stream()
                .filter(e -> e.getValue() >= frequentPropThreshold).map(Entry::getKey).collect(Collectors.toSet());

        Map<String, Integer> oldPHistogram = oldPHistogramFile != null
                ? PropertyHistogramGenerator.createFromFile(oldPHistogramFile).getCounts()
                : null;
        Map<String, Integer> oldCHistogram = oldCHistogramFile != null
                ? PropertyHistogramGenerator.createFromFile(oldCHistogramFile).getCounts()
                : null;
        Map<String, Integer> newCHistogram = newCHistogramFile != null
                ? PropertyHistogramGenerator.createFromFile(newCHistogramFile).getCounts()
                : null;
        Map<List<Pair<Property, Boolean>>, Double> oldPath2ScoreMapping = new HashMap<>();

        try (TentrisAdapter tentris = new ClientBasedTentrisAdapter(
                HttpClientBuilder.create().setMaxConnPerRoute(64).build(), endpoint);) {
            ExecutorService executor = Executors.newWorkStealingPool();
            for (String property : properties) {
                LOGGER.info("Starting property {} ...", property);
                String outputFilePrefix = outputDir.getAbsolutePath() + File.separator
                        + property.substring(Util.splitNamespaceXML(property));
                String foundPathsFileName = outputFilePrefix + "_found.paths";
                String scoredPathsFileName = outputFilePrefix + SCORED_PATHS_SUFFIX;
                File scoredPathsFile = new File(scoredPathsFileName);
                List<QRestrictedPath> paths = null;
                long numberOfFaultyPaths = 0;
                if (scoredPathsFile.exists()) {
                    LOGGER.info("Trying to read scored paths from file...");
                    paths = readPaths(scoredPathsFile, mapper);
                    if (paths != null) {
                        numberOfFaultyPaths = paths.parallelStream()
                                .filter(p -> (Double.isNaN(p.getScore()) || p.getScore() == 0.0)).count();
                        LOGGER.info("Read {} scored paths. {} of them have a faulty score (0.0 or NaN).", paths.size(),
                                numberOfFaultyPaths);
                    } else {
                        LOGGER.info("Couldn't read scored paths for property {}...", property);
                    }
                }
                // If paths need to be scored (either because there are no scored paths or some
                // of them have a faulty score)
                if ((paths == null) || (numberOfFaultyPaths > 0)) {
                    // Subject and object do not matter. We only need them to avoid null pointer
                    // exceptions
                    Predicate predicate = pFactory.generatePredicate(
                            new StatementImpl(RDF.Statement, new PropertyImpl(property), RDF.Statement));
                    File pathsFile = new File(foundPathsFileName);

                    // We have no scored paths, try to read paths that are not scored
                    if (paths == null) {
                        if (pathsFile.exists()) {
                            LOGGER.info("Trying to read found paths from file...");
                            MappingIterator<QRestrictedPath> iterator = mapper.readerFor(QRestrictedPath.class)
                                    .readValues(pathsFile);
                            if (iterator.hasNext()) {
                                paths = new ArrayList<>();
                                while (iterator.hasNext()) {
                                    paths.add(iterator.next());
                                }
                            }
                        }
                    }

                    if (paths != null) {
                        // Check whether we have scored paths in another directory (if this directory is
                        // given)
                        if (knownPaths != null && knownPaths.exists()) {
                            File knownScoredPathsFile = new File(knownPaths.getAbsolutePath() + File.separator
                                    + property.substring(Util.splitNamespaceXML(property)) + SCORED_PATHS_SUFFIX);
                            if (knownScoredPathsFile.exists()) {
                                LOGGER.info("Found {}. Trying to reuse paths from this file...",
                                        knownScoredPathsFile.getAbsolutePath());
                                List<QRestrictedPath> scoredPaths = readPaths(knownScoredPathsFile, mapper);
                                if (scoredPaths != null) {
                                    Map<List<Pair<Property, Boolean>>, Double> path2ScoreMapping = new HashMap<>();
                                    for (QRestrictedPath path : scoredPaths) {
                                        path2ScoreMapping.put(path.getPathElements(), path.getScore());
                                    }
                                    // If we didn't get histograms in addition, we can directly use the scores
                                    if (oldPHistogramFile == null && oldCHistogramFile == null
                                            && newCHistogramFile == null) {
                                        for (QRestrictedPath path : paths) {
                                            if (path2ScoreMapping.containsKey(path.getPathElements())) {
                                                path.setScore(path2ScoreMapping.get(path.getPathElements()));
                                            }
                                        }
                                    } else {
                                        // We can reuse the scores only if the histograms allow that. So use this
                                        // information later on.
                                        oldPath2ScoreMapping = path2ScoreMapping;
                                    }
                                }
                            }
                        }

                        LOGGER.info("Scoring paths...");
                        // Separate paths into categories based on their properties
                        Set<QRestrictedPath> pathsToScore = paths.parallelStream()
                                .filter(p -> (Double.isNaN(p.getScore()) || p.getScore() == 0.0))
                                .collect(Collectors.toSet());
                        Set<QRestrictedPath> hardPaths = pathsToScore.parallelStream()
                                .filter(p -> pathCreatesHighWorkload(p, frequentProperties))
                                .collect(Collectors.toSet());

                        // For each path in the final set, run count queries
                        // Count the occurrence of the path
                        // Count the co-occurrence of the path and the property
                        ICountRetriever retriever = new TentrisBasedCountRetriever(tentris,
                                new TentrisBasedMaxCounter(tentris), new BGPBasedPathClauseGenerator());
                        retriever = new CachingCountRetrieverDecorator(retriever, 2, 20, 5, 20);
                        // Calculate the NPMI / PNPMI and write the scored paths to a file
                        IPropertyBasedPathScorer pathScorer = new NPMIBasedScorer(retriever);
                        File tempFile = File.createTempFile("COPAAL_", ".scored");
                        boolean success = false;
                        PathCounter pathCounter = new PathCounter();
                        final Map<String, Integer> fOldPHistogram = oldPHistogram;
                        final Map<String, Integer> fNewPHistogram = newPHistogram;
                        final Map<List<Pair<Property, Boolean>>, Double> fOldPath2ScoreMapping = oldPath2ScoreMapping;
                        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(tempFile));
                                JsonGenerator generator = mapper.createGenerator(os);) {
                            // check whether the number of subject or object instances has changed
                            boolean couldReusePreviousResults = false;
                            if (oldCHistogram != null && newCHistogram != null) {
                                couldReusePreviousResults = true;
                                Set<String> classIris = (Set<String>) predicate.getDomain().getRestriction();
                                for (String classIri : classIris) {
                                    couldReusePreviousResults = couldReusePreviousResults
                                            && oldCHistogram.containsKey(classIri)
                                            && newCHistogram.containsKey(classIri)
                                            && (oldCHistogram.get(classIri).equals(newCHistogram.get(classIri)));
                                }
                                if (couldReusePreviousResults) {
                                    classIris = (Set<String>) predicate.getDomain().getRestriction();
                                    for (String classIri : classIris) {
                                        couldReusePreviousResults = couldReusePreviousResults
                                                && oldCHistogram.containsKey(classIri)
                                                && newCHistogram.containsKey(classIri)
                                                && (oldCHistogram.get(classIri).equals(newCHistogram.get(classIri)));
                                    }
                                }
                            }
                            LOGGER.info("Could reuse scores for {}: {}", predicate.getProperty().getURI(), couldReusePreviousResults);
                            final boolean fCouldReusePreviousResults = couldReusePreviousResults;

                            generator.writeStartArray();
                            PathWritingConsumer consumer = new PathWritingConsumer(generator);
                            // Write paths that are already scored
                            if (numberOfFaultyPaths > 0) {
                                LOGGER.info("Writing {}/{} already scored paths...", paths.size() - pathsToScore.size(),
                                        paths.size());
                                paths.parallelStream().filter(p -> !pathsToScore.contains(p)).filter(pathCounter)
                                        .forEach(consumer);
                            }
                            // Score and write easy paths
                            LOGGER.info("Identified {}/{} paths as easy. Scoring them...",
                                    pathsToScore.size() - hardPaths.size(), paths.size());
                            List<Future<QRestrictedPath>> jobResults = pathsToScore.parallelStream()
                                    .filter(p -> !hardPaths.contains(p))
                                    .sorted(new PathPotFreqBasedComparator(newPHistogram))
                                    .map(p -> executor.submit(createJob(fCouldReusePreviousResults, pathScorer,
                                            predicate, p, fOldPHistogram, fNewPHistogram, fOldPath2ScoreMapping)))
                                    .collect(Collectors.toList());
                            QRestrictedPath path;
                            for (Future<QRestrictedPath> jobResult : jobResults) {
                                path = jobResult.get();
                                pathCounter.test(path);
                                consumer.accept(path);
                            }
                            if (!consumer.isSuccess()) {
                                LOGGER.error("Encountered an error while writing paths. Aborting.");
                                return;
                            }
                            // Score and write hard paths
                            LOGGER.info("Identified {}/{} paths as hard. Scoring them...", hardPaths.size(),
                                    paths.size());
                            hardPaths.stream().map(p -> {
                                try {
                                    return createJob(fCouldReusePreviousResults, pathScorer, predicate, p,
                                            fOldPHistogram, fNewPHistogram, fOldPath2ScoreMapping).call();
                                } catch (Exception e1) {
                                    throw new IllegalStateException(
                                            "Got an exception while executing a callable locally.", e1);
                                }
                            }).filter(pathCounter).forEach(consumer);
                            if (!consumer.isSuccess()) {
                                LOGGER.error("Encountered an error while writing paths. Aborting.");
                                return;
                            }
                            // Done, write the end
                            generator.writeEndArray();
                            success = true;
                        }
                        if (success) {
                            LOGGER.info("Paths have been scored and serialized. Moving file...");
                            if (!tempFile.renameTo(scoredPathsFile.getAbsoluteFile())) {
                                LOGGER.error("Couldn't rename file {} to {}. I will try to copy it.",
                                        tempFile.getAbsolutePath(), scoredPathsFile.getAbsolutePath());
                                try {
                                    FileUtils.copyFile(tempFile, scoredPathsFile);
                                } catch (IOException e) {
                                    LOGGER.error("Exception while trying to copy file.");
                                }
                            }
                        }
                        // Print summary
                        if ((pathCounter.getNaNCount() > 0) || (pathCounter.getZeroCount() > 0)) {
                            LOGGER.warn("Summary counts: 0.0 / NaN / all : {} / {} / {}", pathCounter.getZeroCount(),
                                    pathCounter.getNaNCount(), pathCounter.getPathCount());
                        } else if ((pathCounter.getNaNCount() + pathCounter.getZeroCount()) >= pathCounter
                                .getPathCount()) {
                            LOGGER.error(
                                    "Summary counts: 0.0 / NaN / all : {} / {} / {} Seems like all counts are wrong",
                                    pathCounter.getZeroCount(), pathCounter.getNaNCount(), pathCounter.getPathCount());
                        } else {
                            LOGGER.info("Summary counts: 0.0 / NaN / all : {} / {} / {}", pathCounter.getZeroCount(),
                                    pathCounter.getNaNCount(), pathCounter.getPathCount());
                        }
                    } else {
                        LOGGER.error("Couldn't find paths for {}. Aborting.", property);
                    }
                }
            }
            executor.shutdown();
        } // try qef, tentris
        LOGGER.info("Finished.");
    }

    private static Callable<QRestrictedPath> createJob(boolean couldReusePreviousResults,
            IPropertyBasedPathScorer pathScorer, Predicate predicate, QRestrictedPath path,
            Map<String, Integer> oldHistogram, Map<String, Integer> newHistogram,
            Map<List<Pair<Property, Boolean>>, Double> oldPath2ScoreMapping) {
        if (couldReusePreviousResults && oldHistogram != null && newHistogram != null && oldPath2ScoreMapping != null) {
            return new ScoreReusingJob(pathScorer, predicate, path, oldHistogram, newHistogram, oldPath2ScoreMapping);
        } else {
            return new ScoreJob(pathScorer, predicate, path);
        }
    }

    protected static boolean pathCreatesHighWorkload(QRestrictedPath path, Set<String> frequentProperties) {
        int frequentPropCount = 0;
        for (Pair<Property, Boolean> p : path.getPathElements()) {
            if (frequentProperties.contains(p.getKey().getURI())) {
                ++frequentPropCount;
            }
        }
        return frequentPropCount > 1;
    }

    public static class ScoreJob implements Callable<QRestrictedPath> {

        protected IPropertyBasedPathScorer pathScorer;
        protected Predicate predicate;
        protected QRestrictedPath path;

        public ScoreJob(IPropertyBasedPathScorer pathScorer, Predicate predicate, QRestrictedPath path) {
            this.pathScorer = pathScorer;
            this.predicate = predicate;
            this.path = path;
        }

        @Override
        public QRestrictedPath call() throws Exception {
            LOGGER.info("Executing job for {}.", path.toString());
            return pathScorer.score(predicate, path);
        }

    }

    public static class ScoreReusingJob extends ScoreJob {
        protected Map<String, Integer> oldHistogram;
        protected Map<String, Integer> newHistogram;
        protected Map<List<Pair<Property, Boolean>>, Double> oldPath2ScoreMapping;

        public ScoreReusingJob(IPropertyBasedPathScorer pathScorer, Predicate predicate, QRestrictedPath path,
                Map<String, Integer> oldHistogram, Map<String, Integer> newHistogram,
                Map<List<Pair<Property, Boolean>>, Double> oldPath2ScoreMapping) {
            super(pathScorer, predicate, path);
            this.oldHistogram = oldHistogram;
            this.newHistogram = newHistogram;
            this.oldPath2ScoreMapping = oldPath2ScoreMapping;
        }

        @Override
        public QRestrictedPath call() throws Exception {
            boolean reuseOldScore = false;
            if (oldPath2ScoreMapping.containsKey(this.path.getPathElements())) {
                reuseOldScore = true;
                for (Pair<Property, Boolean> pathPart : this.path.getPathElements()) {
                    if (!oldHistogram.containsKey(pathPart.getKey().getURI()) || (!oldHistogram
                            .get(pathPart.getKey().getURI()).equals(newHistogram.get(pathPart.getKey().getURI())))) {
                        reuseOldScore = false;
                    }
                }
            }
            if (reuseOldScore) {
                LOGGER.info("Reusing scores from previous data for {}.", path.toString());
                path.setScore(oldPath2ScoreMapping.get(this.path.getPathElements()));
                return path;
            } else {
                return super.call();
            }
        }
    }

    public static List<QRestrictedPath> readPaths(File file, ObjectMapper mapper) {
        List<QRestrictedPath> paths = null;
        if (file.exists()) {
            try {
                MappingIterator<QRestrictedPath> iterator = mapper.readerFor(QRestrictedPath.class).readValues(file);
                if (iterator.hasNext()) {
                    paths = new ArrayList<>();
                    while (iterator.hasNext()) {
                        paths.add(iterator.next());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error while reading paths from \"" + file.getAbsolutePath() + "\". Aborting.", e);
                return null;
            }
        }
        return paths;
    }

    /**
     * Calculates the potential number of paths and sorts the paths descending
     * according to that.
     * 
     * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
     *
     */
    public static class PathPotFreqBasedComparator implements Comparator<QRestrictedPath> {
        protected Map<String, Integer> histogram;

        public PathPotFreqBasedComparator(Map<String, Integer> histogram) {
            super();
            this.histogram = histogram;
        }

        @Override
        public int compare(QRestrictedPath p1, QRestrictedPath p2) {
            return -Integer.compare(calcPotFreq(p1), calcPotFreq(p2));
        }

        protected int calcPotFreq(QRestrictedPath p1) {
            return p1.getPathElements().stream().map(pair -> pair.getKey())
                    .mapToInt(p -> histogram.getOrDefault(p.getURI(), 0)).reduce(0, (a, b) -> a * b);
        }

    }
}
