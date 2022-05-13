package org.dice_research.glisten;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.dice_research.fc.data.QRestrictedPath;
import org.dice_research.fc.serialization.PathDeserializer;
import org.dice_research.fc.serialization.PathSerializer;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class PathFileMergerTest {

    @Test
    public void test() throws Exception {
        File tempFile = File.createTempFile("glisten-test", "paths");
        tempFile.delete();
        File scoredPathsFile = new File("src/test/resources/partlyScoredPaths");
        File foundPathsFile = new File("src/test/resources/foundPaths");
        PathFileMerger.main(new String[] { scoredPathsFile.getAbsolutePath(),
                foundPathsFile.getAbsolutePath(), tempFile.getAbsolutePath() });
        //tempFile.deleteOnExit();

        // Init JSON IO
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(QRestrictedPath.class, new PathSerializer());
        module.addDeserializer(QRestrictedPath.class, new PathDeserializer());
        mapper.registerModule(module);

        List<QRestrictedPath> paths = new ArrayList<>();
        MappingIterator<QRestrictedPath> iterator = mapper.readerFor(QRestrictedPath.class).readValues(tempFile);
        while (iterator.hasNext()) {
            paths.add(iterator.next());
        }
        System.out.println(paths.toString());
        Assert.assertEquals(6, paths.size());
    }
}
