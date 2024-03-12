package ru.catghoti.wordle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.gson.Gson;

public class Wordle {
    public static void main(String[] args) throws IOException {
        InputStream is = Wordle.class.getClassLoader().getResourceAsStream("word_list.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        List<String> words = reader.lines().sorted().toList();
        WordCrossTable table = new WordCrossTable(words);
        is.close();

        is = Wordle.class.getClassLoader().getResourceAsStream("word_list.txt");
        reader = new BufferedReader(new InputStreamReader(is));
        Set<String> answers = reader.lines().collect(Collectors.toSet());

        SolutionCache solutionCache = new SolutionCache();

        Solver solver = new Solver(words, answers, table, solutionCache);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println(new Gson().toJson(solutionCache.cache.size()))));

        solver.solve();
    }
}
