package org.dice_research.glisten;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.atlas.lib.ProgressMonitor;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.ProgressStreamRDF;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDF2;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.rdf.stream.filter.NodeFilterBasedTripleFilter;
import org.dice_research.rdf.stream.filter.PropertyBasedTripleFilter;
import org.dice_research.rdf.stream.filter.RDFStreamTripleFilter;
import org.dice_research.rdf.stream.filter.node.EqualityNodeFilter;
import org.dice_research.rdf.stream.filter.node.StringBasedNamespaceNodeFilter;
import org.dice_research.rdf.stream.map.RDFStreamTripleFlatMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlistenClassAdder implements Function<Triple, Stream<Triple>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlistenClassAdder.class);

    protected Map<String, ArrayList<String>> classHierarchy;

    public GlistenClassAdder(Map<String, ArrayList<String>> classHierarchy) {
        this.classHierarchy = classHierarchy;
    }

    @Override
    public Stream<Triple> apply(Triple t) {
        Node object = t.getObject();
        if (!object.isURI()) {
            return Stream.empty();
        }
        if (classHierarchy.containsKey(object.getURI())) {
            return classHierarchy.get(object.getURI()).stream().map(NodeFactory::createURI)
                    .map(classNode -> new Triple(t.getSubject(), t.getPredicate(), classNode));
        } else {
            return Stream.empty();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            LOGGER.error("Wrong usage! GlistenClassAdder <input-file> <class-map-file> <output-file>");
            return;
        }
        String inputFile = args[0];
        String classMapFile = args[1];
        String outputFile = args[2];
        Map<String, ArrayList<String>> classHierarchy = readClassHierarchy(new File(classMapFile));

        try (Writer out1 = new FileWriter(outputFile)) {
            // Create stream starting from the end!
            StreamRDF outStream = StreamRDFLib.writer(out1);
            ProgressMonitor monitor1 = ProgressMonitor.create(LOGGER, "Added triples", 100000, 10);
            outStream = new ProgressStreamRDF(outStream, monitor1);

            // First stream, extend existing or newly created rdf:type statements with super
            // classes
            // Filter type triples again just in case we added owl:Thing or similar
            StreamRDF typeStream = new RDFStreamTripleFilter(
                    new NodeFilterBasedTripleFilter(null, new EqualityNodeFilter(RDF.type.asNode()),
                            new StringBasedNamespaceNodeFilter("http://dbpedia.org/ontology/")),
                    outStream);

            // Map the incoming triples to newly generated triples
            typeStream = new RDFStreamTripleFlatMapper(new GlistenClassAdder(classHierarchy), typeStream);

            // Second stream: we add domain and range information; we write it to the type
            // stream so that this stream can add more classes if necessary
            GlistenDomainRangeAdder gdra = GlistenDomainRangeAdder.create();
            StreamRDF drStream = new RDFStreamTripleFlatMapper(gdra, new StreamRDF2(outStream, typeStream));
            // Filter triples; we are only interested in dbo: properties for which we have
            // domain and range information
            drStream = new RDFStreamTripleFilter(new PropertyBasedTripleFilter(gdra.getDomainRangeInfo().keySet()),
                    drStream);

            // The type stream is only interested in the rdf:type triples with dbo classes;
            // all other triples are forwarded to the dr stream
            StreamRDF stream = new RDFStreamTripleFilter(
                    new NodeFilterBasedTripleFilter(null, new EqualityNodeFilter(RDF.type.asNode()),
                            new StringBasedNamespaceNodeFilter("http://dbpedia.org/ontology/")),
                    typeStream, drStream);

            // Add monitor at the beginning of the stream
            ProgressMonitor monitorS = ProgressMonitor.create(LOGGER, "Processed triples", 100000, 10);
            stream = new ProgressStreamRDF(stream, monitorS);

            // Start reading triples from the input file
            monitorS.start();
            monitor1.start();
            stream.start();
            RDFDataMgr.parse(stream, inputFile, Lang.NT);
            monitorS.finish();
            monitor1.finish();
            stream.finish();
            LOGGER.info("Finished");
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, ArrayList<String>> readClassHierarchy(File file) throws IOException {
        try (ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            return (Map<String, ArrayList<String>>) oin.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
}
