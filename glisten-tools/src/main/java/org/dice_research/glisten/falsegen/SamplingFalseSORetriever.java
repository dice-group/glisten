package org.dice_research.glisten.falsegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.dice_research.fc.data.Predicate;
import org.dice_research.fc.sparql.restrict.ITypeRestriction;

/**
 * A class that samples instances from the given restrictions. All queries are
 * restricted to the sampled instances instead of using the original
 * restrictions. The maximum count is adapted accordingly.
 * 
 * <p>
 * The sampling method ({@link #sampleInstances(List, int)} is implemented in a
 * way that with the same seed and the same given list of instances and the same
 * number of instances that should be sampled, the method will return the same
 * result if it is called several times.
 * </p>
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class SamplingFalseSORetriever extends AbstractFalseSORetriever {

    protected Random random;
    /**
     * The {@link QueryExecutionFactory} used to derive the list of instances.
     */
    protected QueryExecutionFactory qef;

    /**
     * Constructor.
     * 
     * @param qef           The {@link QueryExecutionFactory} used to derive the
     *                      list of instances and check the validity of pairs
     * @param seed          The seed that is used to initialize the random number
     *                      generator
     * @param maxSampleSize The maximum size of a list of instances
     */
    public SamplingFalseSORetriever(
            QueryExecutionFactory qef, long seed) {
        this.random = new Random(seed);
        this.qef = qef;
    }

    @Override
    protected List<String> retrieveInstances(ITypeRestriction restriction) {
        List<String> instances = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT ?s WHERE {");
        restriction.addRestrictionToQuery("s", builder);
        builder.append("}");
        QueryExecution qe = null;
        try {
            qe = qef.createQueryExecution(builder.toString());
            ResultSet rs = qe.execSelect();
            QuerySolution qs;
            while (rs.hasNext()) {
                qs = rs.next();
                RDFNode node = qs.get("s");
                if (node.isURIResource()) {
                    instances.add(node.asResource().getURI());
                } else if (node.isAnon()) {
                    instances.add(node.asResource().getId().getBlankNodeId().getLabelString());
                }
            }
        } finally {
            if (qe != null) {
                qe.close();
            }
        }
        // Sort the instances to ensure that we have the same order every time
        Collections.sort(instances);
        return instances;
    }

    @Override
    protected String[] selectPair(List<String> subjects, List<String> objects) {
        return new String[] { subjects.get(random.nextInt(subjects.size())),
                objects.get(random.nextInt(objects.size())) };
    }

    @Override
    protected boolean checkPair(String[] pair, Predicate predicate) {
        StringBuilder builder = new StringBuilder();
        builder.append("ASK { <");
        builder.append(pair[0]);
        builder.append("> <");
        builder.append(predicate.getProperty().getURI());
        builder.append("> <");
        builder.append(pair[1]);
        builder.append("> }");
        try (QueryExecution qe = qef.createQueryExecution(builder.toString())) {
            return !qe.execAsk();
        }
    }

}
