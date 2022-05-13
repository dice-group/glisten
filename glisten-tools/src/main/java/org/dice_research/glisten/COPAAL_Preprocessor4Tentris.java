package org.dice_research.glisten;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactoryDataset;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.fc.data.Predicate;
import org.dice_research.fc.data.QRestrictedPath;
import org.dice_research.fc.paths.FactPreprocessor;
import org.dice_research.fc.paths.FileBasedPredicateFactory;
import org.dice_research.fc.paths.IPathSearcher;
import org.dice_research.fc.paths.IPropertyBasedPathScorer;
import org.dice_research.fc.paths.scorer.ICountRetriever;
import org.dice_research.fc.paths.scorer.NPMIBasedScorer;
import org.dice_research.fc.paths.scorer.count.TentrisBasedCountRetriever;
import org.dice_research.fc.paths.scorer.count.decorate.CachingCountRetrieverDecorator;
import org.dice_research.fc.paths.scorer.count.max.TentrisBasedMaxCounter;
import org.dice_research.fc.paths.search.SPARQLBasedSOPathSearcher;
import org.dice_research.fc.serialization.PathDeserializer;
import org.dice_research.fc.serialization.PathSerializer;
import org.dice_research.fc.sparql.filter.NamespaceFilter;
import org.dice_research.fc.sparql.path.BGPBasedPathClauseGenerator;
import org.dice_research.fc.tentris.ClientBasedTentrisAdapter;
import org.dice_research.fc.tentris.TentrisAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class COPAAL_Preprocessor4Tentris {

  private static final Logger LOGGER = LoggerFactory.getLogger(COPAAL_Preprocessor4Tentris.class);

  public static final int MAX_PATH_LENGTH = 3;
  public static final String SCORED_PATHS_SUFFIX = "_scored.paths";

  public static void main(String[] args) throws Exception {
    if (args.length < 7) {
      System.err.println(
          "Error: wrong usage. COPAAL_Preprocessor <property-file> <example-triple-directory> <RDF-dataset-file> <tentris-endpoint> <property-histrogram-file> <frequent-property-threshold> <output-directory> [max-triples-per-property]");
      return;
    }
    File propertiesFile = new File(args[0]);
    File exampleDirectory = new File(args[1]);
    Lang exampleLang = Lang.NT;
    File datasetFile = new File(args[2]);
    String endpoint = args[3];
    File propertiesHistogramFile = new File(args[4]);
    int frequentPropThreshold = Integer.parseInt(args[5]);
    File outputDir = new File(args[6]);
    int maxNumberOfTriples = -1;
    if (args.length > 7) {
      maxNumberOfTriples = Integer.parseInt(args[7]);
    }

    // Init JSON IO
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addSerializer(QRestrictedPath.class, new PathSerializer());
    module.addDeserializer(QRestrictedPath.class, new PathDeserializer());
    mapper.registerModule(module);

    LOGGER.info("Reading predicates file...");
    FactPreprocessor pFactory = null;
    try (InputStream is = COPAAL_Preprocessor4Tentris.class.getClassLoader()
        .getResourceAsStream("collected_dbo_predicates.json");) {
      pFactory = FileBasedPredicateFactory.create(is);
    }

    LOGGER.info("Reading chosen properties file...");
    List<String> properties = FileUtils.readLines(propertiesFile, StandardCharsets.UTF_8);

    LOGGER.info("Reading properties histogram...");
    Map<String, Integer> histogram = PropertyHistogramGenerator.createFromFile(propertiesHistogramFile).getCounts();
    Set<String> frequentProperties = histogram.entrySet().stream().filter(e -> e.getValue() >= frequentPropThreshold)
        .map(Entry::getKey).collect(Collectors.toSet());
//    Set<String> rareProperties = SetUtils.difference(histogram.keySet(), frequentProperties);

    LOGGER.info("Reading Model...");
    Model model = ModelFactory.createDefaultModel();
    model.read(datasetFile.toURI().toURL().toString());
    Dataset dataset = DatasetFactory.create(model);
    try (QueryExecutionFactory qef = new QueryExecutionFactoryDataset(dataset);
        TentrisAdapter tentris = new ClientBasedTentrisAdapter(HttpClients.createDefault(), endpoint);) {

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
          MappingIterator<QRestrictedPath> iterator = mapper.readerFor(QRestrictedPath.class)
              .readValues(scoredPathsFile);
          if (iterator.hasNext()) {
            paths = new ArrayList<>();
            while (iterator.hasNext()) {
              paths.add(iterator.next());
            }
          }
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
          Predicate predicate = pFactory
              .generatePredicate(new StatementImpl(RDF.Statement, new PropertyImpl(property), RDF.Statement));
          File pathsFile = new File(foundPathsFileName);

          // We have no scored paths, try to read paths that are not scored
          if (paths == null) {
            if (pathsFile.exists()) {
              LOGGER.info("Trying to read found paths from file...");
              MappingIterator<QRestrictedPath> iterator = mapper.readerFor(QRestrictedPath.class).readValues(pathsFile);
              if (iterator.hasNext()) {
                paths = new ArrayList<>();
                while (iterator.hasNext()) {
                  paths.add(iterator.next());
                }
              }
            }
          }
          if (paths == null) {
            LOGGER.info("Collecting paths...");
            String exampleFile = exampleDirectory.getAbsolutePath() + File.separator
                + property.substring(Util.splitNamespaceXML(property)) + ".nt";
            // For each example triple search for paths given that triple and put the paths
            // into a set
            PathCollector collector = new PathCollector(pFactory, qef, maxNumberOfTriples);
            collector.collectPaths(exampleFile, exampleLang, qef);
            Map<String, Set<QRestrictedPath>> pathsPerProperty = collector.getPaths();
            Set<QRestrictedPath> pathsForProperty = pathsPerProperty.get(property);
            if (pathsForProperty == null) {
              pathsForProperty = new HashSet<>();
            }
            LOGGER.info("{} paths collected.", pathsForProperty.size());
            mapper.writeValue(pathsFile, pathsForProperty);
            paths = new ArrayList<>(pathsForProperty);
          }

          LOGGER.info("Scoring paths...");
          // Separate paths into categories based on their properties
          Set<QRestrictedPath> pathsToScore = paths.parallelStream()
              .filter(p -> (Double.isNaN(p.getScore()) || p.getScore() == 0.0)).collect(Collectors.toSet());
          Set<QRestrictedPath> hardPaths = pathsToScore.parallelStream()
              .filter(p -> pathCreatesHighWorkload(p, frequentProperties)).collect(Collectors.toSet());

          // For each path in the final set, run count queries
          // Count the occurrence of the path
          // Count the co-occurrence of the path and the property
          ICountRetriever retriever = new TentrisBasedCountRetriever(tentris, new TentrisBasedMaxCounter(tentris),
              new BGPBasedPathClauseGenerator());
          retriever = new CachingCountRetrieverDecorator(retriever, 2, 20, 5, 20);
          // Calculate the NPMI / PNPMI and write the scored paths to a file
          IPropertyBasedPathScorer pathScorer = new NPMIBasedScorer(retriever);
          File tempFile = File.createTempFile("COPAAL_", ".scored");
          boolean success = false;
          PathCounter pathCounter = new PathCounter();
          try (OutputStream os = new BufferedOutputStream(new FileOutputStream(tempFile));
              JsonGenerator generator = mapper.createGenerator(os);) {
            generator.writeStartArray();
            PathWritingConsumer consumer = new PathWritingConsumer(generator);
            // Write paths that are already scored
            if (numberOfFaultyPaths > 0) {
              LOGGER.info("Writing {}/{} already scored paths...", paths.size() - pathsToScore.size(), paths.size());
              paths.parallelStream().filter(p -> !pathsToScore.contains(p)).filter(pathCounter).forEach(consumer);
            }
            // Score and write easy paths
            LOGGER.info("Identified {}/{} paths as easy. Scoring them...", pathsToScore.size() - hardPaths.size(),
                paths.size());
            pathsToScore.parallelStream().filter(p -> !hardPaths.contains(p)).map(p -> pathScorer.score(predicate, p))
                .filter(pathCounter).forEach(consumer);
            if (!consumer.isSuccess()) {
              LOGGER.error("Encountered an error while writing paths. Aborting.");
              return;
            }
            // Score and write hard paths
            LOGGER.info("Identified {}/{} paths as hard. Scoring them...", hardPaths.size(), paths.size());
            hardPaths.stream().map(p -> pathScorer.score(predicate, p)).filter(pathCounter).forEach(consumer);
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
            tempFile.renameTo(scoredPathsFile.getAbsoluteFile());
          }
          // Print summary
          if ((pathCounter.getNaNCount() > 0) || (pathCounter.getZeroCount() > 0)) {
            LOGGER.warn("Summary counts: 0.0 / NaN / all : {} / {} / {}", pathCounter.getZeroCount(),
                pathCounter.getNaNCount(), pathCounter.getPathCount());
          } else if ((pathCounter.getNaNCount() + pathCounter.getZeroCount()) >= pathCounter.getPathCount()) {
            LOGGER.error("Summary counts: 0.0 / NaN / all : {} / {} / {} Seems like all counts are wrong",
                pathCounter.getZeroCount(), pathCounter.getNaNCount(), pathCounter.getPathCount());
          } else {
            LOGGER.info("Summary counts: 0.0 / NaN / all : {} / {} / {}", pathCounter.getZeroCount(),
                pathCounter.getNaNCount(), pathCounter.getPathCount());
          }
        }
      }
    } // try qef, tentris
    LOGGER.info("Finished.");
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

  /**
   * This class will always return {@code true}. It is only used to count paths
   * that have faulty scores.
   * 
   * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
   *
   */
  public static class PathCounter implements java.util.function.Predicate<QRestrictedPath> {

    private AtomicInteger pathCount = new AtomicInteger(0);
    private AtomicInteger zeroCount = new AtomicInteger(0);
    private AtomicInteger nanCount = new AtomicInteger(0);

    @Override
    public boolean test(QRestrictedPath path) {
      pathCount.incrementAndGet();
      if (Double.isNaN(path.getScore())) {
        nanCount.incrementAndGet();
      } else if (path.getScore() == 0.0) {
        zeroCount.incrementAndGet();
      }
      return true;
    }

    public int getPathCount() {
      return pathCount.get();
    }

    public int getZeroCount() {
      return zeroCount.get();
    }

    public int getNaNCount() {
      return nanCount.get();
    }

  }

  public static class PathWritingConsumer implements Consumer<QRestrictedPath> {
    protected JsonGenerator generator;
    protected boolean success = true;
    protected boolean reportedError = false;

    public PathWritingConsumer(JsonGenerator generator) {
      this.generator = generator;
    }

    @Override
    public void accept(QRestrictedPath p) {
      try {
        writeSynchronized(p);
      } catch (Exception e) {
        success = false;
        if (reportedError) {
          LOGGER.error("Got an exception while serializing paths!");
        } else {
          LOGGER.error("Got an exception while serializing paths!", e);
          reportedError = true;
        }
      }
    }

    protected void writeSynchronized(QRestrictedPath path)
        throws JsonGenerationException, JsonMappingException, IOException {
      synchronized (generator) {
        generator.writeObject(path);
      }
    }

    /**
     * @return the success
     */
    public boolean isSuccess() {
      return success;
    }
  }

  public static class PathCollector extends StreamRDFBase {

    protected Map<String, Set<QRestrictedPath>> paths = new HashMap<>();
    protected FactPreprocessor pFactory;
    protected IPathSearcher searcher;
    protected Model transformModel = ModelFactory.createDefaultModel();
    protected int maxNumberOfTriples = -1;
    protected int count = 0;

    public PathCollector(FactPreprocessor pFactory, QueryExecutionFactory qef) {
      searcher = new SPARQLBasedSOPathSearcher(qef, MAX_PATH_LENGTH, Arrays.asList(
          new NamespaceFilter(new String[] { "http://dbpedia.org/property/", "http://dbpedia.org/ontology/" }, false)));
      this.pFactory = pFactory;
    }

    public PathCollector(FactPreprocessor pFactory, QueryExecutionFactory qef, int maxNumberOfTriples) {
      this(pFactory, qef);
      this.maxNumberOfTriples = maxNumberOfTriples;
    }

    public void collectPaths(String inputFile, Lang lang, QueryExecutionFactory qef) {
      RDFDataMgr.parse(this, inputFile, lang);
    }

    @Override
    public void quad(Quad quad) {
      triple(quad.asTriple());
    }

    @Override
    public void triple(Triple triple) {
      Resource s = new ResourceImpl(triple.getSubject().getURI());
      Property p = new PropertyImpl(triple.getPredicate().getURI());
      Resource o = new ResourceImpl(triple.getObject().getURI());
      // If there is no maximum or the maximum hasn't been reached yet
      if ((maxNumberOfTriples > 0) && (count < maxNumberOfTriples)) {
        ++count;
        // Search paths for triple
        Set<QRestrictedPath> pathsOfP;
        String pIRI = p.getURI();
        if (paths.containsKey(pIRI)) {
          pathsOfP = paths.get(pIRI);
        } else {
          pathsOfP = new HashSet<>();
          paths.put(pIRI, pathsOfP);
        }
        pathsOfP.addAll(searcher.search(s, pFactory.generatePredicate(new StatementImpl(s, p, o)), o));
      }
    }

    public Map<String, Set<QRestrictedPath>> getPaths() {
      return paths;
    }
  }
}
