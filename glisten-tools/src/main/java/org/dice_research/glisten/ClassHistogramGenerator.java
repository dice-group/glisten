package org.dice_research.glisten;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.vocabulary.RDF;

/**
 * Not thread safe!
 */
public class ClassHistogramGenerator extends StreamRDFBase {

    private Map<String, Integer> counts = new HashMap<String, Integer>();
    
    public ClassHistogramGenerator() {
    }
    
    public ClassHistogramGenerator(Map<String, Integer> counts) {
        this.counts = counts;
    }

    private void run(File datasetFile, File outputFile) throws IOException {
        RDFDataMgr.parse(this, datasetFile.toURI().toString());

        try (PrintStream pout = new PrintStream(new FileOutputStream(outputFile))) {
            counts.entrySet().stream().sorted(new Comparator<Map.Entry<String, Integer>>() {
                @Override
                public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                    return Integer.compare(o1.getValue(), o2.getValue());
                }
            }).forEach(e -> {
                pout.print(e.getKey());
                pout.print("\t");
                pout.println(e.getValue());
            });
        }
    }

    @Override
    public void triple(Triple triple) {
        if (!triple.getObject().isLiteral()) {
            String p = triple.getPredicate().getURI();
            if(RDF.type.getURI().equals(p)) {
                String c = triple.getObject().getURI();
                if (counts.containsKey(c)) {
                    counts.put(c, counts.get(c) + 1);
                } else {
                    counts.put(c, 1);
                }
            }
        }
    }

    public Map<String, Integer> getCounts() {
        return counts;
    }

    public static ClassHistogramGenerator createFromFile(File inputFile) throws IOException {
      List<String> lines = FileUtils.readLines(inputFile, StandardCharsets.UTF_8);
      Map<String, Integer> counts = lines.parallelStream().map(l -> l.split("\t")).filter(l -> l != null && l.length > 1)
          .collect(Collectors.toMap(l -> (String) l[0], l -> Integer.parseInt(l[1])));
      return new ClassHistogramGenerator(counts);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println(
                    "Wrong number of arguments.\nUsage: ClassHistogramGenerator <dataset-file> <output-file>");
            return;
        }
        File datasetFile = new File(args[0]);
        File outputFile = new File(args[1]);

        ClassHistogramGenerator generator = new ClassHistogramGenerator();
        generator.run(datasetFile, outputFile);

        if (args.length >= 5) {
            File propertiesInFile = new File(args[2]);
            int minPropCount = Integer.parseInt(args[3]);
            File propertiesOutFile = new File(args[4]);

            Set<String> properties = new HashSet<>(FileUtils.readLines(propertiesInFile, StandardCharsets.UTF_8));
            Map<String, Integer> counts = generator.getCounts();
            Set<String> chosenProperties = properties.stream()
                    .filter(s -> counts.containsKey(s) && (counts.get(s) >= minPropCount)).collect(Collectors.toSet());

            FileUtils.writeLines(propertiesOutFile, chosenProperties);
        }
    }
}
