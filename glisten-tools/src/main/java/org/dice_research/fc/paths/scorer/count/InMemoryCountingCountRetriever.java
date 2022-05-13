package org.dice_research.fc.paths.scorer.count;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.commons.math3.util.Pair;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.dice_research.fc.data.Predicate;
import org.dice_research.fc.data.QRestrictedPath;
import org.dice_research.fc.paths.scorer.ICountRetriever;
import org.dice_research.fc.paths.scorer.count.max.IMaxCounter;
import org.dice_research.fc.sparql.restrict.ITypeRestriction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryCountingCountRetriever extends AbstractSPARQLBasedCountRetriever
        implements ICountRetriever, IMaxCounter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryCountingCountRetriever.class);

    protected Model model;

    protected Predicate lastPredicate;
    protected ITypeRestriction sRestriction;
    protected ITypeRestriction oRestriction;
    protected Set<Resource> potSubjects = new HashSet<>();
    protected Set<Resource> potObjects = new HashSet<>();
    protected Set<Resource> predSubjects = new HashSet<>();
    protected Set<Resource> predObjects = new HashSet<>();

    public InMemoryCountingCountRetriever(Model model, QueryExecutionFactory qef) {
        super(qef, null);
        this.model = model;
    }

    @Override
    public long countCooccurrences(Predicate predicate, QRestrictedPath path) {
        updateSOLists(predicate);
        return countPath(path, predicate);
    }

    @Override
    public long countPathInstances(QRestrictedPath path, ITypeRestriction domainRestriction,
            ITypeRestriction rangeRestriction) {
        updateSRestriction(domainRestriction);
        updateORestriction(rangeRestriction);
        return countPath(path);
    }

    @Override
    public long countPredicateInstances(Predicate predicate) {
        updateSOLists(predicate);
        return countPath(
                new QRestrictedPath(Arrays.asList(new Pair<Property, Boolean>(predicate.getProperty(), true))));
    }

    public long deriveMaxCount(Predicate predicate) {
        updateSRestriction(predicate.getDomain());
        updateORestriction(predicate.getRange());
        return potSubjects.size() * potObjects.size();
    }

    private void updateSRestriction(ITypeRestriction newSRestriction) {
        if ((sRestriction == null) || (!sRestriction.equals(newSRestriction))) {
            potSubjects.clear();
            derivePotentialInstances(newSRestriction, potSubjects);
        }
    }

    private void updateORestriction(ITypeRestriction newORestriction) {
        if ((oRestriction == null) || (!oRestriction.equals(newORestriction))) {
            potObjects.clear();
            derivePotentialInstances(newORestriction, potSubjects);
        }
    }

    private void updateSOLists(Predicate predicate) {
        if ((lastPredicate == null) || (!lastPredicate.equals(predicate))) {
            updateSRestriction(predicate.getDomain());
            updateORestriction(predicate.getRange());
            predSubjects.clear();
            predObjects.clear();
            deriveSOInstances(predicate.getProperty());
        }
    }

    private void derivePotentialInstances(ITypeRestriction restriction, Set<Resource> potObjects) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ?s WHERE { ");
        restriction.addRestrictionToQuery("s", queryBuilder);
        queryBuilder.append(" }");
        String query = queryBuilder.toString();

        try (QueryExecution qe = qef.createQueryExecution(query)) {
            ResultSet result = qe.execSelect();
            while (result.hasNext()) {
                QuerySolution qs = result.next();
                potObjects.add(qs.get("s").asResource());
            }
        } catch (Exception e) {
            LOGGER.error("Got an exception while running count query \"" + query + "\".", e);
        }
    }

    private void deriveSOInstances(Property property) {
        StmtIterator iterator = model.listStatements(null, property, (RDFNode) null);
        Statement stmt;
        Resource s;
        RDFNode o;
        while (iterator.hasNext()) {
            stmt = iterator.next();
            s = stmt.getSubject();
            if (s.isURIResource()) {
                predSubjects.add(s);
            }
            o = stmt.getObject();
            if (o.isURIResource()) {
                predObjects.add(o.asResource());
            }
        }
    }

    private long countPath(QRestrictedPath qRestrictedPath) {
        return countPath(qRestrictedPath, potSubjects, potObjects);
    }

    private long countPath(QRestrictedPath qRestrictedPath, Predicate predicate) {
        return countPath(qRestrictedPath, predSubjects, predObjects);
    }

    private long countPath(QRestrictedPath qRestrictedPath, Set<Resource> allowedSubjects,
            Set<Resource> allowedObjects) {
        List<Pair<Property, Boolean>> path = qRestrictedPath.getPathElements();
        switch (path.size()) {
        case 1:
            return countPath1(path.get(0), allowedSubjects, allowedObjects);
        case 2:
            return countPath2(path.get(0), path.get(1), allowedSubjects, allowedObjects);
        case 3:
            return countPath3(path.get(0), path.get(1), path.get(2), allowedSubjects, allowedObjects);
        default:
            throw new IllegalArgumentException("This method does not support paths with the length " + path.size());
        }
    }

    private long countPath1(Pair<Property, Boolean> p1, Set<Resource> allowedSubjects, Set<Resource> allowedObjects) {
        Set<Resource> allowedLocalSubjects;
        Set<Resource> allowedLocalObjects;
        if (p1.getSecond()) {
            allowedLocalSubjects = allowedSubjects;
            allowedLocalObjects = allowedObjects;
        } else {
            allowedLocalObjects = allowedSubjects;
            allowedLocalSubjects = allowedObjects;
        }
        return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(model.listStatements(null, p1.getKey(), (RDFNode) null), 0),
                        false)
                .filter(s -> allowedLocalSubjects.contains(s.getSubject()))
                .filter(s -> s.getObject().isURIResource() && allowedLocalObjects.contains(s.getObject().asResource()))
                .map(s -> new Pair<>(s.getSubject(), s.getObject().asResource())).collect(Collectors.toSet()).size();
    }

    private long countPath2(Pair<Property, Boolean> p1, Pair<Property, Boolean> p2, Set<Resource> allowedSubjects,
            Set<Resource> allowedObjects) {
        Map<Resource, Set<Resource>> intermediate1;
        if (p1.getSecond()) {
            intermediate1 = deriveMappedObjects(p1.getFirst(), allowedSubjects);
        } else {
            intermediate1 = deriveMappedSubjects(p1.getFirst(), allowedSubjects);
        }
        Map<Resource, Set<Resource>> intermediate2;
        if (p2.getSecond()) {
            intermediate2 = deriveMappedSubjects(p2.getFirst(), allowedObjects);
        } else {
            intermediate2 = deriveMappedObjects(p2.getFirst(), allowedObjects);
        }
        return intermediate1.entrySet().parallelStream().filter(e -> intermediate2.containsKey(e.getKey()))
                .flatMap(e -> ((Set<Resource>) e.getValue()).parallelStream()
                        .flatMap(s -> intermediate2.get(e.getKey()).stream().map(o -> new Pair<>(s, o))))
                .collect(Collectors.toSet()).size();
    }

    private Set<String> deriveSubjects(Pair<Property, Boolean> p, Set<String> allowedObjects) {
        if (p.getSecond()) {
            return deriveSubjects(p.getKey(), allowedObjects);
        } else {
            return deriveObjects(p.getKey(), allowedObjects);
        }
    }

    private Set<String> deriveSubjects(Property p, Set<String> allowedObjects) {
        return allowedObjects.parallelStream()
                .map(o -> (StmtIterator) model.listStatements(null, p, model.getResource(o)))
                .filter(StmtIterator::hasNext)
                .flatMap(sIter -> (Stream<Statement>) StreamSupport
                        .stream(Spliterators.spliteratorUnknownSize(sIter, 0), false))
                .map(Statement::getSubject).filter(Resource::isURIResource).map(Resource::getURI)
                .collect(Collectors.toSet());
    }

    private Set<String> deriveObjects(Pair<Property, Boolean> p, Set<String> allowedSubjects) {
        if (p.getSecond()) {
            return deriveObjects(p.getKey(), allowedSubjects);
        } else {
            return deriveSubjects(p.getKey(), allowedSubjects);
        }
    }

    private Set<String> deriveObjects(Property p, Set<String> allowedSubjects) {
        return allowedSubjects.parallelStream()
                .map(s -> (StmtIterator) model.listStatements(model.getResource(s), p, (RDFNode) null))
                .filter(StmtIterator::hasNext)
                .flatMap(sIter -> (Stream<Statement>) StreamSupport
                        .stream(Spliterators.spliteratorUnknownSize(sIter, 0), false))
                .map(Statement::getObject).filter(RDFNode::isURIResource).map(o -> o.asResource().getURI())
                .collect(Collectors.toSet());
    }

    private Map<Resource, Set<Resource>> deriveMappedSubjects(Property p, Set<Resource> allowedObjects) {
        return allowedObjects.parallelStream().map(o -> new Pair<>(o, model.listStatements(null, p, o)))
                .filter(pair -> pair.getSecond().hasNext())
                .flatMap(pair -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(pair.getSecond(), 0), false)
                        .map(Statement::getSubject).map(s -> new Pair<Resource, Resource>(s, pair.getFirst())))
                .collect(
                        Collectors.groupingBy(Pair::getFirst, Collectors.mapping(Pair::getSecond, Collectors.toSet())));
    }

    private Map<Resource, Set<Resource>> deriveMappedObjects(Property p, Set<Resource> allowedSubjects) {
        return allowedSubjects.parallelStream().map(s -> new Pair<>(s, model.listStatements(s, p, (RDFNode) null)))
                .filter(pair -> pair.getSecond().hasNext())
                .flatMap(pair -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(pair.getSecond(), 0), false)
                        .map(Statement::getObject).filter(RDFNode::isURIResource).map(RDFNode::asResource)
                        .map(s -> new Pair<Resource, Resource>(s, pair.getFirst())))
                .collect(
                        Collectors.groupingBy(Pair::getFirst, Collectors.mapping(Pair::getSecond, Collectors.toSet())));
    }

    private long countPath3(Pair<Property, Boolean> p1, Pair<Property, Boolean> p2, Pair<Property, Boolean> p3,
            Set<Resource> allowedSubjects, Set<Resource> allowedObjects) {
        // x1 -> s
        Map<Resource, Set<Resource>> intermediate1;
        if (p1.getSecond()) {
            intermediate1 = deriveMappedObjects(p1.getFirst(), allowedSubjects);
        } else {
            intermediate1 = deriveMappedSubjects(p1.getFirst(), allowedSubjects);
        }
        // x2 -> x1
        Map<Resource, Set<Resource>> intermediate2;
        if (p2.getSecond()) {
            intermediate2 = deriveMappedSubjects(p2.getFirst(), intermediate1.keySet());
        } else {
            intermediate2 = deriveMappedObjects(p2.getFirst(), intermediate1.keySet());
        }
        // x2 -> o
        Map<Resource, Set<Resource>> intermediate3;
        if (p3.getSecond()) {
            intermediate3 = deriveMappedSubjects(p3.getFirst(), allowedObjects);
        } else {
            intermediate3 = deriveMappedObjects(p3.getFirst(), allowedObjects);
        }
        return intermediate3.entrySet().parallelStream().filter(e -> intermediate2.containsKey(e.getKey()))
                .flatMap(e -> ((Set<Resource>) e.getValue()).parallelStream()
                        .flatMap(x1 -> intermediate2.get(e.getKey()).stream().map(o -> new Pair<>(x1, o))))
                .flatMap(pair -> intermediate1.get(pair.getFirst()).stream().map(s -> new Pair<>(s, pair.getSecond())))
                .collect(Collectors.toSet()).size();
    }

//    protected long count(StmtIterator iterator) {
//        long count = 0;
//        while (iterator.hasNext()) {
//            ++count;
//        }
//        return count;
//    }

//    protected static class Counter<T> {
//        protected Map<Object, Integer> potSubjects = new HashMap<>();
//        protected Map<Object, Integer> potObjects = new HashMap<>();
//    }

//    protected static class Pair {
//        private int key;
//        private int value;
//
//        public Pair(int key, int value) {
//            super();
//            this.key = key;
//            this.value = value;
//        }
//
//        @Override
//        public int hashCode() {
//            return 31 * key + value;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (this == obj)
//                return true;
//            if (obj == null)
//                return false;
//            if (getClass() != obj.getClass())
//                return false;
//            Pair other = (Pair) obj;
//            return (key == other.key) && (value != other.value);
//        }
//    }
}
