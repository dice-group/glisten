package org.dice_research.glisten.collect;

import java.util.Map;
import java.util.Set;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.jena.riot.Lang;
import org.dice_research.fc.data.QRestrictedPath;

public interface PathCollector extends AutoCloseable {

    public void collectPaths(String inputFile, Lang lang, QueryExecutionFactory qef);

    public Map<String, Set<QRestrictedPath>> getPaths();
  }