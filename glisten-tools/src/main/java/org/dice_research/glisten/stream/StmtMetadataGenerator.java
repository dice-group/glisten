package org.dice_research.glisten.stream;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

/**
 * An extension of the {@link ReifiedStatementGenerator} that adds additional
 * metadata.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class StmtMetadataGenerator extends ReifiedStatementGenerator {

    protected Node[] predicates;
    protected Node[] objects;

    public StmtMetadataGenerator(String stmtIriPrefix) {
        super(stmtIriPrefix);
    }

    @Override
    public Stream<Triple> apply(Triple t) {
        Triple[] reifiedStmt = generate(t);
        Triple[] addedMetadata = new Triple[predicates.length];
        for (int i = 0; i < addedMetadata.length; ++i) {
            addedMetadata[i] = new Triple(reifiedStmt[0].getSubject(), predicates[i], objects[i]);
        }
        return Stream.concat(Arrays.stream(reifiedStmt), Arrays.stream(addedMetadata));
    }

    /**
     * @return the predicates
     */
    public Node[] getPredicates() {
        return predicates;
    }

    /**
     * @param predicates the predicates to set
     */
    public void setPredicates(Node... predicates) {
        this.predicates = predicates;
    }

    /**
     * @return the objects
     */
    public Node[] getObjects() {
        return objects;
    }

    /**
     * @param objects the objects to set
     */
    public void setObjects(Node... objects) {
        this.objects = objects;
    }

}
