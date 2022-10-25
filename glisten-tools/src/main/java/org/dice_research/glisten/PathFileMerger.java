package org.dice_research.glisten;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

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
public class PathFileMerger {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathFileMerger.class);

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Error: wrong usage. PathFileMerger <scored-file> <found-file> <output-file>");
            return;
        }
        File scoredFile = new File(args[0]);
        File foundFile = new File(args[1]);
        File outputFile = new File(args[2]);

        // Init JSON IO
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(QRestrictedPath.class, new PathSerializer());
        module.addDeserializer(QRestrictedPath.class, new PathDeserializer());
        mapper.registerModule(module);

        Map<String, QRestrictedPath> paths = new HashMap<>();
        QRestrictedPath path;
        int count = 0;
        if (!outputFile.exists()) {
            MappingIterator<QRestrictedPath> iterator = mapper.readerFor(QRestrictedPath.class).readValues(scoredFile);
            if (iterator.hasNext()) {
                while (iterator.hasNext()) {
                    path = iterator.next();
                    paths.put(path.toString(), path);
                    ++count;
                }
            }
            LOGGER.info("Found {} scored paths", count);
            
            count = 0;
            iterator = mapper.readerFor(QRestrictedPath.class).readValues(foundFile);
            String pathString;
            if (iterator.hasNext()) {
                while (iterator.hasNext()) {
                    path = iterator.next();
                    pathString = path.toString();
                    if(!paths.containsKey(pathString)) {
                        paths.put(pathString, path);
                        ++count;
                    }
                }
            }
            LOGGER.info("Found {} additional paths", count);

            LOGGER.info("Writing {}...", outputFile);
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));
                    JsonGenerator generator = mapper.createGenerator(os);) {
                generator.writeStartArray();
                PathWritingConsumer consumer = new PathWritingConsumer(generator);
                paths.values().stream().forEach(consumer);
                generator.writeEndArray();
            }
        } else {
            LOGGER.info("Skipping {} since it already exists.", outputFile);
        }
    }

}
