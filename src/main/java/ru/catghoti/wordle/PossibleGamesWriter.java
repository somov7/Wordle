package ru.catghoti.wordle;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PossibleGamesWriter {

    public void write(FileWriter writer, SolutionTree tree, Collection<String> possibleAnswers, WordCrossTable table) {
        possibleAnswers.stream().sorted().forEach(answer -> {
            SolutionTree currentTree = tree;
            List<String> guesses = new ArrayList<>();
            while (currentTree.getBucketSize() != 0) {
                guesses.add(currentTree.getGuess());
                currentTree = currentTree.getChild(table.get(currentTree.getGuess(), answer));
            }
            try {
                writer.write(String.join(",", guesses) + "\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
