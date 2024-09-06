package pl.magzik.algorithms;

import pl.magzik.structures.Record;

import java.util.List;
import java.util.Map;

/**
 * Functional interface representing an algorithm that processes a group of records
 * and returns a mapping of a key to lists of records.
 *
 * @param <T> the type of the key used in the resulting map
 * @param <V> the type of the records being processed, which extends {@link Record}
 */
@FunctionalInterface
public interface Algorithm <T, V extends Record<?>> {

    /**
     * Applies the algorithm to a list of records and returns a mapping of keys to lists of records.
     *
     * @param group the list of records to process
     * @return a map where each key is associated with a list of records
     */
    Map<T, List<V>> apply(List<V> group);
}
