package org.dice_research.glisten;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.opencsv.CSVReader;

/**
 * This class takes the output of the {@link TapiocaResultProcessor} class and a
 * CSV file with dataset (short) name and GERBIL results for COPAAL.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class GlistenResultGenerator {

    private static final String DATASET_BASE_NAME = "Place";
    private static final String DATASET_VARIANT_EXTENSION = "-a";

    public static void main(String[] args) throws IOException {
        GlistenResultGenerator generator = new GlistenResultGenerator();
        generator.run(
                new File("/home/micha/data/glisten/phd-experiment/tapioca-results/" + DATASET_BASE_NAME
                        + DATASET_VARIANT_EXTENSION + "-summary.csv"),
                new File("/home/micha/data/glisten/phd-experiment/tapioca-results/COPAAL-results-summary-"
                        + DATASET_BASE_NAME + ".csv"),
                new File("/home/micha/data/glisten/phd-experiment/tapioca-results" + DATASET_BASE_NAME
                        + DATASET_VARIANT_EXTENSION + "-delta-auc.csv"),
                DATASET_BASE_NAME);
    }

    public void run(File tapResultsFile, File gerbilResultCsv, File outputFile, String datasetBaseName)
            throws IOException {
        Map<String, Double> scores = readAUCROC(gerbilResultCsv);
        scores = calculateDeltas(scores, datasetBaseName);
        try (FileReader fReader = new FileReader(tapResultsFile, StandardCharsets.UTF_8);
                CSVReader reader = new CSVReader(fReader);
                PrintStream pout = new PrintStream(outputFile)) {
            String[] line = reader.readNext();
            while (line != null) {
                // print number of topics
                pout.print(line[0]);
                // print delta and calculate delta AUC
                double auc = 0;
                double delta;
                for (int i = 6; i < line.length; ++i) {
                    if (scores.containsKey(line[i])) {
                        delta = scores.get(line[i]);
                    } else {
                        System.err.println("score for " + line[i] + " is missing!");
                        delta = Double.NaN;
                    }
                    pout.print(',');
                    pout.print(delta);
                    auc += delta;
                }
                pout.print(',');
                pout.println(auc);

                line = reader.readNext();
            }
        }
    }

    private Map<String, Double> calculateDeltas(Map<String, Double> aucScores, String datasetBaseName) {
        Map<String, Double> deltaScores = new HashMap<String, Double>();
        double origScore = aucScores.get(datasetBaseName);
        for (String key : aucScores.keySet()) {
            deltaScores.put(key, aucScores.get(key) - origScore);
        }
        return deltaScores;
    }

    private Map<String, Double> readAUCROC(File gerbilResultCsv) throws IOException {
        Map<String, Double> scores = new HashMap<String, Double>();
        try (FileReader fReader = new FileReader(gerbilResultCsv, StandardCharsets.UTF_8);
                CSVReader reader = new CSVReader(fReader);) {
            String[] line = reader.readNext();
            while (line != null) {
                scores.put(line[0], Double.parseDouble(line[1]));
                line = reader.readNext();
            }
        }
        return scores;
    }
}
