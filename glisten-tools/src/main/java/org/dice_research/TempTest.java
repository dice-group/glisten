package org.dice_research;

import java.io.File;
import java.net.MalformedURLException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class TempTest {

    public static void main(String[] args) throws MalformedURLException {
        Model model = ModelFactory.createDefaultModel();
        model.read((new File("/home/micha/workspace/KGV/KGEvalData/YAGO/yago_subset.ttl")).toURI().toURL().toString());
        System.out.println(model.size());
    }
}
