/*
* Virtual Threading advised.
* */

import pl.magzik.algorithms.Algorithm;
import pl.magzik.structures.Record;
import pl.magzik.utils.LoggingInterface;

import java.io.File;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecordProcessor implements LoggingInterface {

    private final ExecutorService executorService;

    public RecordProcessor(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public RecordProcessor() {
        this(Executors.newVirtualThreadPerTaskExecutor());
    }

    @SafeVarargs
    public final <R extends Record<?>> Map<?, List<R>> process(Collection<File> files, Function<File, R> checksumFunction, Algorithm<?, R>... algorithms) {
        log("Mapping input files...");

        Map<Object, List<R>> records = checksumGrouping(files, checksumFunction);
        log("Checksum grouping finished.");

        log("Processing algorithms...");
        return processAlgorithms(records, algorithms);
    }

    private <R extends Record<?>> Map<?, List<R>> processAlgorithms(Map<?, List<R>> groupedRecords, Algorithm<?, R>[] algorithms) {
        Map<?, List<R>> records = groupedRecords;
        for (Algorithm<?, R> algorithm : algorithms) {
            log("Processing algorithm " + algorithm.getClass().getSimpleName());

            records = records.values().parallelStream()
                .map(algorithm::apply)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    this::combineLists
                )
            );
        }

        log("Algorithms processed.");
        return records;
    }

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

    private <R extends Record<?>> R applyChecksum(File file, Function<File, R> checksumFunction) {
        try {
            return checksumFunction.apply(file);
        } catch (UncheckedIOException e) {
            log(e);
            return null;
        }
    }

    private <R extends Record<?>> List<R> combineLists(List<R> list1, List<R> list2) {
        List<R> combined = new ArrayList<>(list1);
        combined.addAll(list2);
        return combined;
    }
}
