package org.dice_research.glisten;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.RDF;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class GlistenDomainRangeAdder implements Function<Triple, Stream<Triple>> {

    protected static final int DOMAIN_ID = 0;
    protected static final int RANGE_ID = 1;

    protected Map<String, List<Set<String>>> domainRangeInfo;

    public GlistenDomainRangeAdder(Map<String, List<Set<String>>> domainRangeInfo) {
        this.domainRangeInfo = domainRangeInfo;
    }

    @Override
    public Stream<Triple> apply(Triple t) {
        Node predicate = t.getPredicate();
        if (!domainRangeInfo.containsKey(predicate.getURI())) {
            return Stream.empty();
        }
        List<Set<String>> information = domainRangeInfo.get(predicate.getURI());

        Node subject = t.getSubject();
        Stream<Triple> result;
        if (subject.isURI()) {
            result = information.get(DOMAIN_ID).stream().map(NodeFactory::createURI)
                    .map(c -> new Triple(subject, RDF.type.asNode(), c));
        } else {
            result = Stream.empty();
        }
        Node object = t.getObject();
        if (object.isURI()) {
            result = Stream.concat(result, information.get(RANGE_ID).stream().map(NodeFactory::createURI)
                    .map(c -> new Triple(object, RDF.type.asNode(), c)));
        }
        return result;
    }

    public Map<String, List<Set<String>>> getDomainRangeInfo() {
        return domainRangeInfo;
    }

    public static GlistenDomainRangeAdder create() throws IOException {
        try (InputStream is = COPAAL_Preprocessor.class.getClassLoader()
                .getResourceAsStream("collected_dbo_predicates.json");) {
            return new GlistenDomainRangeAdder(readDomainRangeMapping(is));
        }
    }

    public static Map<String, List<Set<String>>> readDomainRangeMapping(InputStream is) throws IOException {
        String content = IOUtils.toString(is, StandardCharsets.UTF_8);
        Map<String, List<Set<String>>> knownProperties = new HashMap<>();
        // Parse the json file and generate all known predicate objects
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(content);
            // A JSON object. Key value pairs are unordered.
            JSONArray Predicates = (JSONArray) obj;
            @SuppressWarnings("unchecked")
            Iterator<JSONObject> iterator = Predicates.iterator();
            while (iterator.hasNext()) {
                JSONObject jsonPredicate = iterator.next();
                // get domain
                Set<String> domainsSet = new HashSet<>();
                JSONArray domainsJson = (JSONArray) jsonPredicate.get("Domain");
                for (int i = 0; i < domainsJson.size(); i++) {
                    domainsSet.add(domainsJson.get(i).toString());
                }
                // get range
                Set<String> rangesSet = new HashSet<>();
                JSONArray rangesJson = (JSONArray) jsonPredicate.get("Range");
                for (int i = 0; i < rangesJson.size(); i++) {
                    rangesSet.add(rangesJson.get(i).toString());
                }
                knownProperties.put(jsonPredicate.get("Predicate").toString(), new ArrayList<Set<String>>(Arrays.asList(domainsSet, rangesSet)));
            }
            return knownProperties;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
