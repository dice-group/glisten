package org.dice_research.glisten;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.dice_research.topicmodeling.commons.sort.AssociativeSort;

import com.opencsv.CSVReader;

public class TapiocaResultProcessor {

    private static final Set<String> DATASET_BLACKLIST = new HashSet<>(Arrays.asList("TopicalConcept.nt"));
    private static final int DATASET_NAME_ID = 0;
    private static final int SIMILARITY_ID = 1;

    private static final Map<String, String> DATASET_NAME_MAPPING = new HashMap<>();

    private static final char SEPARATOR = ',';

    public static void main(String[] args) {
        DATASET_NAME_MAPPING.put("Activity.nt", "Ac");
        DATASET_NAME_MAPPING.put("Award.nt", "Aw");
        DATASET_NAME_MAPPING.put("Biomolecule.nt", "Bi");
        DATASET_NAME_MAPPING.put("ChemicalSubstance.nt", "Ch");
        DATASET_NAME_MAPPING.put("Device.nt", "De");
        DATASET_NAME_MAPPING.put("Event.nt", "Ev");
        DATASET_NAME_MAPPING.put("FictionalCharacter.nt", "Fi");
        DATASET_NAME_MAPPING.put("Language.nt", "La");
        DATASET_NAME_MAPPING.put("MeanOfTransportation.nt", "Me");
        DATASET_NAME_MAPPING.put("Organisation.nt", "Or");
        DATASET_NAME_MAPPING.put("Person.nt", "Pe");
        DATASET_NAME_MAPPING.put("Place.nt", "Pl");
        DATASET_NAME_MAPPING.put("Species.nt", "Sp");
        DATASET_NAME_MAPPING.put("SportsSeason.nt", "Ss");
        DATASET_NAME_MAPPING.put("TimePeriod.nt", "Ti");
        DATASET_NAME_MAPPING.put("Work.nt", "Wo");

        String queryDataset = "Place";
        String corpusName = queryDataset + "-a";
        File[] files = new File[3];
        try (PrintStream topOut = new PrintStream(
                "/home/micha/data/glisten/phd-experiment/tapioca-results/" + corpusName + "-summary.csv")) {
            for (int i = 2; i <= 20; ++i) {
                for (int j = 1; j < 4; ++j) {
                    files[j - 1] = new File(
                            String.format("/home/micha/data/glisten/phd-experiment/tapioca-results/%s/TM-%s-%d-%d.csv",
                                    corpusName, queryDataset, i, j));
                }
                topOut.print(i);
                run(files, topOut);
                topOut.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void run(File[] files, PrintStream topOut) throws IOException {
        Map<String, double[]> values = new HashMap<>();
        for (int i = 0; i < files.length; ++i) {
            readFile(files, i, values);
        }
        String datasetNames[] = values.keySet().toArray(String[]::new);
        double averageSims[] = new double[datasetNames.length];
        for (int i = 0; i < datasetNames.length; ++i) {
            averageSims[i] = Arrays.stream(values.get(datasetNames[i])).average().getAsDouble();
        }
        AssociativeSort.quickSort(averageSims, datasetNames);

        String[] topShortNames = new String[5];

        for (int i = 0; i < topShortNames.length; ++i) {
            topOut.print(SEPARATOR);
            topOut.print(datasetNames[datasetNames.length - (i + 1)]);
            topShortNames[i] = DATASET_NAME_MAPPING.get(datasetNames[datasetNames.length - (i + 1)]);
        }
        for (int i = 1; i <= topShortNames.length; ++i) {
            topOut.print(SEPARATOR);
            Arrays.sort(topShortNames, 0, i);
            topOut.print(join(topShortNames, 0, i));
        }
    }

    private static String join(String[] topShortNames, int start, int end) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < end; ++i) {
            builder.append(topShortNames[i]);
        }
        return builder.toString();
    }

    private static void readFile(File[] files, int fileId, Map<String, double[]> values) throws IOException {
        String[] line;
        try (FileReader freader = new FileReader(files[fileId], StandardCharsets.UTF_8);
                CSVReader reader = new CSVReader(freader)) {
            line = reader.readNext();
            do {
                String dataset = extractFileName(line[DATASET_NAME_ID]);
                double sim = Double.parseDouble(line[SIMILARITY_ID]);
                if (!DATASET_BLACKLIST.contains(dataset)) {
                    if (!values.containsKey(dataset)) {
                        values.put(dataset, new double[files.length]);
                    }
                    values.get(dataset)[fileId] = sim;
                }
                line = reader.readNext();
            } while (line != null);
        }
    }

    private static String extractFileName(String value) {
        int pos = value.lastIndexOf('/');
        if (pos < 0) {
            return value;
        }
        return value.substring(pos + 1);
    }
}
