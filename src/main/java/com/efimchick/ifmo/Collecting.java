package com.efimchick.ifmo;


import com.efimchick.ifmo.util.CourseResult;
import com.efimchick.ifmo.util.Person;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Collecting {

    public int sum(IntStream intStream) {
        return intStream.sum();
    }

    public int production(IntStream intStream) {
        return intStream.reduce(1, (subtotal, element) -> subtotal * element);
    }

    public int oddSum(IntStream intStream) {
        return intStream.filter(i -> i % 2 != 0).sum();
    }

    public Map<Integer, Integer> sumByRemainder(int divider, IntStream intStream) {
        return intStream.boxed().collect(Collectors.groupingBy(i -> i % divider, Collectors.summingInt(i -> i)));
    }

    public double progAvg(CourseResult courseResult) {
        return courseResult.getTaskResults().values().stream().collect(Collectors.summarizingInt(Integer::intValue)).getAverage();
    }

    public double histAvg(CourseResult courseResult) {
        return Stream.concat(Stream.of(0), courseResult.getTaskResults().values().stream()).collect(Collectors.summarizingInt(Integer::intValue)).getAverage();
    }

    public Map<Person, Double> totalScores(Stream<CourseResult> results) {
        return results.collect(Collectors.toMap
                (CourseResult::getPerson,
                        x -> {
                            boolean areProgramTasks = x.getTaskResults().keySet().stream().allMatch(key -> key.startsWith("Lab "));
                            if (areProgramTasks) {
                                return progAvg(x);
                            } else
                                return histAvg(x);
                        }
                ));
    }

    public double averageTotalScore(Stream<CourseResult> results) {
        return results.mapToDouble(
                x -> {
                    boolean areProgramTasks = x.getTaskResults().keySet().stream().allMatch(key -> key.startsWith("Lab "));
                    if (areProgramTasks) {
                        return progAvg(x);
                    } else
                        return histAvg(x);
                }).average().orElse(0);
    }

    public Map<String, Double> averageScoresPerTask(Stream<CourseResult> results) {
        return results.map(CourseResult::getTaskResults)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)),
                        map -> map.entrySet()
                                .stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> e.getValue() / 3.))
                ));
    }

    String mark(double avg){
        if (avg > 90) return "A";
        if (avg >= 83) return "B";
        if (avg >= 75) return "C";
        if (avg >= 68) return "D";
        if (avg >= 60) return "E";
        else return "F";
    }

    public Map<Person, String> defineMarks(Stream<CourseResult> results) {
        return results.collect(
                Collectors.toMap(
                        CourseResult::getPerson,
                        x -> {
                            double avg;
                            if (areProgramming(x)) {
                                avg = x.getTaskResults().values().stream().collect(Collectors.summarizingInt(Integer::intValue)).getAverage();
                            } else {
                                avg = Stream.concat(Stream.of(0), x.getTaskResults().values().stream()).collect(Collectors.summarizingInt(Integer::intValue)).getAverage();
                            }
                            return mark(avg);
                        }
                ));
    }


    public String easiestTask(Stream<CourseResult> results) {
        return averageScoresPerTask(results).entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("No easy task found");
    }

    public Collector<CourseResult, ?, String> printableStringCollector() {
        return new Collector<CourseResult, List<CourseResult>, String>() {
            @Override
            public Supplier<List<CourseResult>> supplier() {
                return ArrayList::new;
            }

            @Override
            public BiConsumer<List<CourseResult>, CourseResult> accumulator() {
//                return (personHashMapHashMap, courseResult) -> personHashMapHashMap.put(courseResult.getPerson(),courseResult.getTaskResults());
                return List::add;
            }

            @Override
            public BinaryOperator<List<CourseResult>> combiner() {
                return null;
            }

            @Override
            public Function<List<CourseResult>, String> finisher() {

                return courseResults -> {
                    String tasks;
                    if (areProgramming(courseResults.get(0)))
                        tasks="Lab 1. Figures | Lab 2. War and Peace | Lab 3. File Tree";
                    else
                        tasks="Phalanxing | Shieldwalling | Tercioing | Wedging";

                    double averageTotalScore = averageTotalScore(Stream.of(courseResults.toArray(new CourseResult[0])));

                    String summary="\nAverage         |          "+averageScoresPerTask(Stream.of(courseResults.toArray
                            (new CourseResult[0]))).entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(t->String.format(Locale.US,"%.2f", t.getValue())).collect(Collectors.joining(" |              "))
                            +" | "+String.format(Locale.US,"%.2f",averageTotalScore) +" |    "+mark(averageTotalScore)+" |";


                    return "Student         | "+tasks+" | Total | Mark |\n"+courseResults.stream()
                        .sorted(Comparator.comparing(p -> p.getPerson().getLastName()))
                        .map(D -> D.getPerson().getLastName() + " " + D.getPerson().getFirstName() + " |"+
                                D.getTaskResults().entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getValue().toString()).collect(Collectors.joining("              |"))+"              | "+
                                totalScores(Stream.of(D)).values().stream().map(aDouble -> String.format(Locale.US,"%.2f", aDouble)).collect(Collectors.joining())+"         |    "+
                                String.join("", defineMarks(Stream.of(D)).values()) +" |")
                        .collect(Collectors.joining("\n"))+summary;
                };
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Set.of(Characteristics.UNORDERED);
            }
        };
    }

    public boolean areProgramming(CourseResult courseResult){
        return courseResult.getTaskResults().keySet().stream().allMatch(key -> key.startsWith("Lab "));
    }

}

