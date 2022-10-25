package org.dice_group.glisten;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDFBase;

/**
 * Not thread safe!
 */
public class PropertyHistogramGenerator extends StreamRDFBase {

    private Map<String, Integer> counts = new HashMap<String, Integer>();

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
            if (counts.containsKey(p)) {
                counts.put(p, counts.get(p) + 1);
            } else {
                counts.put(p, 1);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println(
                    "Wrong number of arguments.\nUsage: PropertyHistogramGenerator <dataset-fil> <output-file>");
            return;
        }
        File datasetFile = new File(args[0]);
        File outputFile = new File(args[1]);

        PropertyHistogramGenerator generator = new PropertyHistogramGenerator();
        generator.run(datasetFile, outputFile);
    }
}
