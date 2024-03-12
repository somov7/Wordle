package ru.catghoti.wordle;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.common.util.concurrent.Futures;
import org.jooq.lambda.Unchecked;

public class Solver {

    private final List<String> allWords;
    private final Set<String> possibleAnswers;
    private final WordCrossTable table;

    private final EnthropyCache enthropyCache;
    private final SolutionCache solutionCache;

    private AtomicInteger counter;

    public Solver(List<String> allWords, Set<String> possibleAnswers, WordCrossTable table, SolutionCache solutionCache) {
        this.allWords = allWords;
        this.possibleAnswers = possibleAnswers;
        this.table = table;
        this.enthropyCache = new EnthropyCache(allWords.size());
        this.solutionCache = solutionCache;
    }

    public void solve() {
        System.out.println("Sorting starting words");
        List<WordWithBuckets> bestGuesses = getBestGuesses(allWords, possibleAnswers);
        AtomicInteger index = new AtomicInteger(0);
        System.out.println("Best sorted staring words: " +
                bestGuesses.stream()
                        .limit(500)
                        .map(w -> index.incrementAndGet() + ". " + w.word() + ": " + w.enthropy())
                        .collect(Collectors.joining("\n"))
        );
        AtomicDouble cutoff = new AtomicDouble(Double.POSITIVE_INFINITY);

        bestGuesses.forEach(guess -> {
            if (Files.exists(Path.of("solution_trees", guess.word() + ".txt"))) {
                System.out.println("Skipping " + guess.word());
                return;
            }
            counter = new AtomicInteger(0);
            System.out.println("Solving for " + guess.word());
            SolutionTree solutionTree = solveForGuess(
                    guess.word(),
                    GuessResultProvider.EMPTY,
                    possibleAnswers,
                    guess.buckets(),
                    1,
                    cutoff
            );
            System.out.println("Score for " + guess.word() + " = " + solutionTree.getScore());
            if (Double.isFinite(solutionTree.getScore())) {
                try (FileWriter writer = new FileWriter("solution_trees/" + guess.word() + ".txt")) {
                    solutionTree.write(writer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try (FileWriter writer = new FileWriter("possible_games/" + guess.word() + ".txt")) {
                    new PossibleGamesWriter().write(writer, solutionTree, possibleAnswers, table);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private SolutionTree solveForGuess(
            String guess,
            byte prevGuess,
            Set<String> bucket,
            Map<Byte, Set<String>> subBuckets,
            int step,
            AtomicDouble cutoff
    ) {
        if (step == 2) {
            int cnt = counter.incrementAndGet();
            System.out.println(cnt + ": " +
                    "solving for guess " + guess +
                    " with first guess resulting in " + GuessResultProvider.getAsString(prevGuess) +
                    ". Total words left: " + bucket.size() +
                    ". Buckets (" + subBuckets.size() + ") are: " +
                    subBuckets.entrySet().stream()
                            .sorted(Comparator.comparingLong(e -> -e.getValue().size()))
                            .map(sb -> GuessResultProvider.getAsString(sb.getKey()) + ": " + sb.getValue().size())
                            .collect(Collectors.joining(" "))
            );
        }

        if (subBuckets.size() == 1) {
            return SolutionTree.INFINITY;
        }

        if (subBuckets.values().stream().anyMatch(b -> (step >= 5 && b.size() > 1) || (step == 4 && b.size() > 238))) {
            return SolutionTree.INFINITY;
        }

        double currentWeight = 1.0 +
                subBuckets.values().stream()
                        .mapToInt(Set::size)
                        .mapToDouble(sz -> (2.0 * sz - 1.0) / sz )
                        .sum();

        if (currentWeight > cutoff.get()) {
            return SolutionTree.INFINITY;
        }

        Map<Byte, SolutionTree> subSolutions = new HashMap<>();
        Iterator<Map.Entry<Byte, Set<String>>> bucketIterator = subBuckets.entrySet().stream()
                .sorted(Comparator.comparingLong(e -> -e.getValue().size()))
                .iterator();
        while (bucketIterator.hasNext()) {
            var b = bucketIterator.next();
            SolutionTree solution = getBucketWeight(step, b.getKey(), b.getValue());
            if (Double.isInfinite(solution.getScore())) {
                return SolutionTree.INFINITY;
            }
            double probability = ((double) b.getValue().size()) / bucket.size();
            currentWeight += (solution.getScore() - (2.0 * b.getValue().size() - 1.0) / b.getValue().size()) * probability;
            if (currentWeight > cutoff.get()) {
                return SolutionTree.INFINITY;
            }
            subSolutions.put(b.getKey(), solution);
        }
        double finalWeight = currentWeight;
        cutoff.updateAndGet(value -> Math.min(finalWeight, value));

        if (step == 2) {
            System.out.println("Score for " + guess +
                    " with previous pattern " + GuessResultProvider.getAsString(prevGuess) +
                    " is " + finalWeight
            );
        }
        return new SolutionTree(guess, finalWeight, bucket.size(), subSolutions);
    }

    private SolutionTree getBucketWeight(int step, byte guessResult, Set<String> bucket) {
        if (guessResult == GuessResultProvider.WIN) {
            return SolutionTree.win(bucket.iterator().next());
        }
        if (step > 6) {
            return SolutionTree.INFINITY;
        }
        if (step == 6 && bucket.size() != 1) {
            return SolutionTree.INFINITY;
        }
        Iterator<String> it = bucket.iterator();
        String first = it.next();
        if (bucket.size() == 1) {
            return new SolutionTree(
                    first,
                    1.0,
                    1,
                    SolutionTree.winMap(first)
            );
        }
        String second = it.next();
        if (bucket.size() == 2) {
            return new SolutionTree(
                    bucket.iterator().next(),
                    1.5,
                    2,
                    Map.of(
                            GuessResultProvider.WIN,
                            SolutionTree.win(first),
                            table.get(first, second),
                            new SolutionTree(
                                    second,
                                    1.0,
                                    1,
                                    SolutionTree.winMap(second)
                            )
                    )
            );
        }
        SolutionTree solution = solutionCache.get(bucket, step);
        if (solution != null) {
            return solution;
        }

        List<WordWithBuckets> bestGuesses;

        if (bucket.size() <= 238) {
            bestGuesses = getBestGuesses(bucket, bucket);
            if (bestGuesses.size() == 1) {
                return new SolutionTree(
                        bestGuesses.get(0).word(),
                        (bucket.size() * 2.0 - 1.0) / (bucket.size()),
                        bucket.size(),
                        bestGuesses.get(0).buckets().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> e.getKey() == GuessResultProvider.WIN
                                                ? SolutionTree.win(e.getValue().iterator().next())
                                                : new SolutionTree(e.getValue().iterator().next(), 1.0, 1, SolutionTree.winMap(e.getValue().iterator().next()))
                                ))
                );
            } else {
                bestGuesses = getBestGuesses(allWords, bucket);
                if (bestGuesses.size() == 1) {
                    return new SolutionTree(
                            bestGuesses.get(0).word(),
                            2.0,
                            bucket.size(),
                            bestGuesses.get(0).buckets().entrySet().stream()
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> new SolutionTree(e.getValue().iterator().next(), 1.0, 1, SolutionTree.winMap(e.getValue().iterator().next()))
                                    ))
                    );
                }
            }
        } else {
            bestGuesses = getBestGuesses(allWords, bucket);
        }

        AtomicDouble cutoff = new AtomicDouble(Double.POSITIVE_INFINITY);

        Semaphore semaphore = new Semaphore(2, true);
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<SolutionTree>> callables = bestGuesses.stream()
                    .map(g -> (Callable<SolutionTree>) () -> {
                        try {
                            semaphore.acquire();
                            return solveForGuess(
                                    g.word(),
                                    guessResult,
                                    bucket,
                                    g.buckets(),
                                    step + 1,
                                    cutoff
                            );
                        } finally {
                            semaphore.release();
                        }
                    })
                    .toList();

            SolutionTree bestWord = exec.invokeAll(callables).stream()
                    .map(Unchecked.function(Future::get))
                    .min(Comparator.comparingDouble(SolutionTree::getScore))
                    .orElseThrow();

            solutionCache.put(bucket, step, bestWord);
            return bestWord;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Byte, Set<String>> getBuckets(String guess, Set<String> bucket) {
        return bucket.stream()
                .collect(
                        Collectors.groupingBy(
                                answer -> table.get(guess, answer),
                                Collectors.toSet()
                        )
                );
    }

    private List<WordWithBuckets> getBestGuesses(Collection<String> wordsToTest, Set<String> bucket) {
        try {
            return wordsToTest.parallelStream().unordered()
                    .map(g -> {
                        Map<Byte, Set<String>> buckets = getBuckets(g, bucket);
                        if (buckets.values().size() == bucket.size()) {
                            throw new AllSingletonBucketsFound(new WordWithBuckets(g, buckets, Double.POSITIVE_INFINITY));
                        }
                        double score = getScore(bucket, buckets);
                        return new WordWithBuckets(g, buckets, score);
                    })
                    .sorted(Comparator.comparingDouble(WordWithBuckets::enthropy).reversed())
                    .toList();
        } catch (AllSingletonBucketsFound e) {
            return List.of(e.getWordWithBuckets());
        }
    }

    private double getScore(Set<String> bucket, Map<Byte, Set<String>> buckets) {
        return buckets.values().stream()
                .mapToDouble(s -> enthropyCache.getEnthropy(s.size(), bucket.size()))
                .sum();
    }

    public static class AllSingletonBucketsFound extends RuntimeException {
        private final WordWithBuckets wordWithBuckets;
        public AllSingletonBucketsFound(WordWithBuckets wordWithBuckets) {
            super("", null, false, false);
            this.wordWithBuckets = wordWithBuckets;
        }

        public WordWithBuckets getWordWithBuckets() {
            return wordWithBuckets;
        }
    }

    record WordWithBuckets(String word, Map<Byte, Set<String>> buckets, double enthropy) { }
}
