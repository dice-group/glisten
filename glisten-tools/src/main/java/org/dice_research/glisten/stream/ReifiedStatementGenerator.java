package org.dice_research.glisten.stream;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.RDF;

public class ReifiedStatementGenerator implements Function<Triple, Stream<Triple>> {

    protected String stmtIriPrefix;
    protected int count = 0;

    public ReifiedStatementGenerator(String stmtIriPrefix) {
        this.stmtIriPrefix = stmtIriPrefix;
    }

    @Override
    public Stream<Triple> apply(Triple t) {
        return Arrays.stream(generate(t));
    }

    protected Triple[] generate(Triple t) {
        Node stmtNode = NodeFactory.createURI(generateNewIri());
        return new Triple[] { new Triple(stmtNode, RDF.Nodes.type, RDF.Nodes.Statement),
                new Triple(stmtNode, RDF.Nodes.subject, t.getSubject()),
                new Triple(stmtNode, RDF.Nodes.predicate, t.getPredicate()),
                new Triple(stmtNode, RDF.Nodes.object, t.getObject()) };
    }

    protected synchronized String generateNewIri() {
        return stmtIriPrefix + (count++);
    }

}
