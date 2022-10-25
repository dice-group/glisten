package org.dice_research.glisten;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.system.StreamRDFOps;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.rdf.stream.map.RDFStreamTripleFlatMapper;
import org.dice_research.rdf.test.ModelComparisonHelper;
import org.junit.Test;

public class GlistenDomainRangeAdderTest {

    private final String NAMESPACE_C = "http://example.org/class/";
    private final String C_1 = NAMESPACE_C + "c1";
    private final String C_2 = NAMESPACE_C + "c2";
    private final String C_3 = NAMESPACE_C + "c3";

    private final String NAMESPACE_P = "http://example.org/property/";
    private final String P_DR = NAMESPACE_P + "dr";
    private final String P_D = NAMESPACE_P + "d";
    private final String P_R = NAMESPACE_P + "r";
    private final String P_DDRR = NAMESPACE_P + "ddrr";

    private final String NAMESPACE_I = "http://example.org/instances/";
    private final String I_1 = NAMESPACE_I + "i1";
    private final String I_2 = NAMESPACE_I + "i2";

    @Test
    public void createAndRunTestCases() {
        Map<String, List<Set<String>>> domainRangeInfo = new HashMap<>();
        domainRangeInfo.put(P_DR,
                Arrays.asList(new HashSet<String>(Arrays.asList(C_1)), new HashSet<String>(Arrays.asList(C_2))));
        domainRangeInfo.put(P_D, Arrays.asList(new HashSet<String>(Arrays.asList(C_1)), new HashSet<String>()));
        domainRangeInfo.put(P_R, Arrays.asList(new HashSet<String>(), new HashSet<String>(Arrays.asList(C_2))));
        domainRangeInfo.put(P_DDRR, Arrays.asList(new HashSet<String>(Arrays.asList(C_1, C_3)),
                new HashSet<String>(Arrays.asList(C_2, C_3))));

        runtTestCase(domainRangeInfo, new String[] { I_1, P_DR, I_2 }, new String[] { C_1 }, new String[] { C_2 });
        runtTestCase(domainRangeInfo, new String[] { I_1, P_D, I_2 }, new String[] { C_1 }, new String[0]);
        runtTestCase(domainRangeInfo, new String[] { I_1, P_R, I_2 }, new String[0], new String[] { C_2 });
        runtTestCase(domainRangeInfo, new String[] { I_1, P_DDRR, I_2 }, new String[] { C_1, C_3 },
                new String[] { C_2, C_3 });
    }

    protected void runtTestCase(Map<String, List<Set<String>>> domainRangeInfo, String[] triple,
            String[] classesSubject, String[] classesObject) {
        GlistenDomainRangeAdder adder = new GlistenDomainRangeAdder(domainRangeInfo);

        Model input = ModelFactory.createDefaultModel();
        input.add(input.createResource(triple[0]), input.createProperty(triple[1]), input.createResource(triple[2]));

        Model output = ModelFactory.createDefaultModel();
        StreamRDFOps.graphToStream(input.getGraph(),
                new RDFStreamTripleFlatMapper(adder, StreamRDFLib.graph(output.getGraph())));

        Model expectedOutput = ModelFactory.createDefaultModel();
        for (int i = 0; i < classesSubject.length; ++i) {
            expectedOutput.add(expectedOutput.createResource(triple[0]), RDF.type,
                    expectedOutput.createResource(classesSubject[i]));
        }
        for (int i = 0; i < classesObject.length; ++i) {
            expectedOutput.add(expectedOutput.createResource(triple[2]), RDF.type,
                    expectedOutput.createResource(classesObject[i]));
        }
        
        ModelComparisonHelper.assertModelsEqual(expectedOutput, output);
    }
}
