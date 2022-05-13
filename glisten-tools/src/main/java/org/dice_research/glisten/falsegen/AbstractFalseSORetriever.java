package org.dice_research.glisten.falsegen;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dice_research.fc.data.Predicate;
import org.dice_research.fc.sparql.restrict.ITypeRestriction;
import org.dice_research.fc.sparql.restrict.TriplePositionRestriction;

public abstract class AbstractFalseSORetriever implements FalseSORetriever {

    protected int maxTriesFactor = 3;

    @Override
    public Collection<String[]> retrieveSOPairsForPredicate(Predicate predicate, int pairCount) {
        // Get all possible instances for the subject
        List<String> subjects = retrieveInstances(predicate.getDomain() != null ? predicate.getDomain()
                : new TriplePositionRestriction(true, false, false, true));
        // Get all possible instances for the object
        List<String> objects = retrieveInstances(predicate.getRange() != null ? predicate.getRange()
                : new TriplePositionRestriction(false, false, true, true));
        Set<String[]> result = new HashSet<>();
        String[] pair;
        int tries = 0;
        int maxTries = maxTriesFactor * pairCount;
        while ((result.size() < pairCount) && (tries < maxTries)) {
            pair = selectPair(subjects, objects);
            // It is (most probably) cheaper to check first whether we already have selected
            // this pair. After that, we should check whether the pair is valid to be
            // selected.
            if (!result.contains(pair) && checkPair(pair, predicate)) {
                result.add(pair);
            }
            ++tries;
        }
        return result;
    }

    protected abstract List<String> retrieveInstances(ITypeRestriction restriction);

    protected abstract String[] selectPair(List<String> subjects, List<String> objects);

    protected abstract boolean checkPair(String[] pair, Predicate predicate);

}
