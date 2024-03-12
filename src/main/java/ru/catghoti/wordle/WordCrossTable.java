package ru.catghoti.wordle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WordCrossTable {

    private final byte[][] table;
    private final Map<String, Integer> orderMap;

    public WordCrossTable(List<String> allWords) {
        System.out.println("Building cross word table");
        int wordsCount = allWords.size();
        this.orderMap = new HashMap<>(allWords.size());
        this.table = new byte[wordsCount][];
        for (int i = 0; i < allWords.size(); i++) {
            this.orderMap.put(allWords.get(i), i);
            this.table[i] = new byte[wordsCount];
        }
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int row = 0; row < wordsCount; row++) {
            int i = row;
            futures.add(CompletableFuture.runAsync(() -> {
                this.table[i][i] = GuessResultProvider.WIN;
                for (int j = 0; j < wordsCount; j++) {
                    this.table[i][j] = GuessResultProvider.getGuessResult(allWords.get(i), allWords.get(j));
                }
            }));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        System.out.println("Cross word table built");
    }

    public byte get(String guess, String answer) {
        return table[orderMap.get(guess)][orderMap.get(answer)];
    }
}
