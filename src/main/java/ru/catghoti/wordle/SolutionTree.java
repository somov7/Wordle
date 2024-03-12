package ru.catghoti.wordle;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.Map;

public class SolutionTree {

    public static final SolutionTree INFINITY = new SolutionTree(null, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, null);
    private final String guess;
    private final double score;
    private final int bucketSize;
    private final Map<Byte, SolutionTree> children;

    public SolutionTree(String guess, double score, int bucketSize, Map<Byte, SolutionTree> children) {
        this.guess = guess;
        this.score = score;
        this.bucketSize = bucketSize;
        this.children = children;
    }

    public static SolutionTree win(String guess) {
        return new SolutionTree(guess, 0.0, 0, null);
    }

    public static Map<Byte, SolutionTree> winMap(String guess) {
        return Map.of(GuessResultProvider.WIN, SolutionTree.win(guess));
    }

    public void write(OutputStreamWriter writer) throws IOException {
        write(writer, 0);
    }

    private void write(OutputStreamWriter writer, int level) throws IOException {
        if (bucketSize == 0) {
            writer.write("You won on guess %d. The word is %s!\n".formatted(level, guess));
        } else {
            writer.write("Guess %d. Total %d possibilities left. Best word is %s with %f\n".formatted(level + 1, bucketSize, guess, score));
        }
        if (children != null && !children.isEmpty()) {
            children.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(SolutionTree::getBucketSize)))
                    .forEach(e -> {
                        try {
                            writer.write("\t".repeat(level + 1));
                            writer.write(GuessResultProvider.getAsString(e.getKey()));
                            writer.write(" -> ");
                            e.getValue().write(writer, level + 1);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }
    }

    public String getGuess() {
        return this.guess;
    }

    public double getScore() {
        return this.score;
    }

    public int getBucketSize() {
        return this.bucketSize;
    }

    public SolutionTree getChild(byte guessResult) {
        return this.children.get(guessResult);
    }
}
