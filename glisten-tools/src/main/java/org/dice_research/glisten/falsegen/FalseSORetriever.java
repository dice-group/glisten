package org.dice_research.glisten.falsegen;

import java.util.Collection;

import org.dice_research.fc.data.Predicate;

public interface FalseSORetriever {

    /**
     * This method returns subject object pairs that are false for a given
     * predicate. Pairs that are returned fulfill the restrictions of the given
     * predicate but are <b>not</b> connected with the property of the given
     * predicate. The result contains up to pairCount pairs. However, it is not
     * guaranteed that it will return exactly this number of pairs.
     * 
     * @param predicate The {@link Predicate} instance defining the property and
     *                  further restrictions for the subject and object
     * @param pairCount The number of subject object pairs that should be retrieved
     * @return A collection of subject object pairs where each pair is a String array with
     *         two elements. (0 = subject, 1 = object)
     */
    Collection<String[]> retrieveSOPairsForPredicate(Predicate predicate, int pairCount);
}
