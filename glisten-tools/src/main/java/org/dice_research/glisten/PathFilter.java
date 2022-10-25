package org.dice_research.glisten;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.dice_research.fc.data.QRestrictedPath;
import org.dice_research.fc.serialization.PathDeserializer;
import org.dice_research.fc.serialization.PathSerializer;
import org.dice_research.glisten.COPAAL_Preprocessor4Tentris.PathWritingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * This class copies serialized paths from a given directory into a second
 * directory. It filters the paths based on a defined filter (at the moment, it
 * only accepts paths with dbo: properties).
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class PathFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathFilter.class);

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Error: wrong usage. PathFilter <input_directory> <output_directory>");
            return;
        }
        File inputDir = new File(args[0]);
        File outputDir = new File(args[1]);

        // Init JSON IO
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(QRestrictedPath.class, new PathSerializer());
        module.addDeserializer(QRestrictedPath.class, new PathDeserializer());
        mapper.registerModule(module);

        List<QRestrictedPath> paths = new ArrayList<>();
        for (File inputFile : inputDir.listFiles()) {
            paths.clear();
            File outputFile = new File(outputDir.getAbsolutePath() + File.separator + inputFile.getName());
            if (!outputFile.exists()) {
                MappingIterator<QRestrictedPath> iterator = mapper.readerFor(QRestrictedPath.class)
                        .readValues(inputFile);
                if (iterator.hasNext()) {
                    while (iterator.hasNext()) {
                        paths.add(iterator.next());
                    }
                }
                if (paths.size() > 0) {
                    LOGGER.info("Writing {}...", outputFile);
                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));
                            JsonGenerator generator = mapper.createGenerator(os);) {
                        generator.writeStartArray();
                        PathWritingConsumer consumer = new PathWritingConsumer(generator);
                        paths.stream().filter(PathFilter::checkPath).forEach(consumer);
                        generator.writeEndArray();
                    }
                } else {
                    LOGGER.warn("Couldn't get any paths from {}...", inputFile);
                }
            } else {
                LOGGER.info("Skipping {} since it already exists.", outputFile);
            }
        }
    }

    public static boolean checkPath(QRestrictedPath path) {
        return path.getPathElements().stream()
                .allMatch(pair -> pair.getFirst().getURI().startsWith("http://dbpedia.org/ontology/"));
    }

}
