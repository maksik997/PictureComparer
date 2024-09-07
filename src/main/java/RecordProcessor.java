import pl.magzik.algorithms.Algorithm;
import pl.magzik.structures.Record;
import pl.magzik.utils.LoggingInterface;

import java.io.File;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Processor for handling records and applying algorithms to group and analyze files.
 */
public class RecordProcessor implements LoggingInterface {

    /**
     * Constructs a RecordProcessor.
     */
    public RecordProcessor() {}

    /**
     * Processes a collection of files by first grouping them based on a checksum function
     * and then applying a series of algorithms.
     *
     * @param files the collection of files to process
     * @param checksumFunction function to create a checksum for each file
     * @param algorithms algorithms to apply in sequence
     * @param <R> the type of record
     * @return a map of grouped records after applying the algorithms
     */
    @SafeVarargs
    public final <R extends Record<?>> Map<?, List<R>> process(Collection<File> files, Function<File, R> checksumFunction, Algorithm<?, R>... algorithms) {
        log("Mapping input files...");

        Map<Object, List<R>> records = checksumGrouping(files, checksumFunction);
        log("Checksum grouping finished.");

        log("Processing algorithms...");
        return processAlgorithms(records, algorithms);
    }

    /**
     * Applies a series of algorithms to group records further based on the results of each algorithm.
     *
     * @param groupedRecords the initial-grouped records
     * @param algorithms the algorithms to apply
     * @param <R> the type of record
     * @return a map of grouped records after applying the algorithms
     */
    private <R extends Record<?>> Map<?, List<R>> processAlgorithms(Map<?, List<R>> groupedRecords, Algorithm<?, R>[] algorithms) {
        Map<?, List<R>> records = groupedRecords;
        for (Algorithm<?, R> algorithm : algorithms) {
            log("Processing using algorithm: " + algorithm.getClass().getSimpleName());

            records = records.values().parallelStream()
            .map(algorithm::apply)
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                this::combineLists
            ));
        }

        log("Algorithms processed.");
        return records;
    }

    /**
     * Groups files by their checksums using a checksum function.
     *
     * @param files the collection of files to group
     * @param checksumFunction function to create a checksum for each file
     * @param <R> the type of record
     * @return a map of grouped records by their checksum
     */
    private <R extends Record<?>> Map<Object, List<R>> checksumGrouping(Collection<File> files, Function<File, R> checksumFunction) {
        Map<Object, List<R>> groupedRecords = files.parallelStream()
                .map(f -> applyChecksum(f, checksumFunction))
                .filter(Objects::nonNull)
                .filter(r -> r.getChecksum() != 0L)
                .collect(Collectors.groupingBy(r -> r.getChecksum()));

        return groupedRecords.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Applies the checksum function to a file and handles potential exceptions.
     *
     * @param file the file to process
     * @param checksumFunction function to create a checksum for the file
     * @param <R> the type of record
     * @return the record created from the file, or null if an exception occurred
     */
    private <R extends Record<?>> R applyChecksum(File file, Function<File, R> checksumFunction) {
        try {
            return checksumFunction.apply(file);
        } catch (UncheckedIOException e) {
            log(e);
            return null;
        }
    }

    /**
     * Combines two lists of records into one.
     *
     * @param list1 the first list
     * @param list2 the second list
     * @param <R> the type of record
     * @return a combined list of records
     */
    private <R extends Record<?>> List<R> combineLists(List<R> list1, List<R> list2) {
        List<R> combined = new ArrayList<>(list1);
        combined.addAll(list2);
        return combined;
    }
}
