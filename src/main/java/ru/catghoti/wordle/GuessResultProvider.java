package ru.catghoti.wordle;

import java.util.Arrays;
import java.util.stream.IntStream;

public class GuessResultProvider {
    public static final byte WIN = (byte) (242 + Byte.MIN_VALUE);
    public static final byte EMPTY = Byte.MIN_VALUE;

    public static byte getGuessResult(String guess, String answer) {
        LetterGuessResult[] colors = new LetterGuessResult[5];
        for (int i = 0; i < 5; i++) {
            if (guess.charAt(i) == answer.charAt(i)) {
                colors[i] = LetterGuessResult.GREEN;
            }
        }
        int[] validIndexes = IntStream.range(0, 5)
                .filter(j -> colors[j] != LetterGuessResult.GREEN)
                .toArray();
        for (int i = 0; i < 5; i++) {
            if (colors[i] == LetterGuessResult.GREEN) {
                continue;
            }
            int index = i;
            long guessCount = Arrays.stream(validIndexes)
                    .filter(j -> j <= index)
                    .filter(j -> guess.charAt(j) == guess.charAt(index))
                    .count();
            long answerCount = Arrays.stream(validIndexes)
                    .filter(j -> answer.charAt(j) == guess.charAt(index))
                    .count();
            if (guessCount <= answerCount) {
                colors[i] = LetterGuessResult.YELLOW;
            } else {
                colors[i] = LetterGuessResult.GREY;
            }
        }
        int guessResult = 0;
        for (int i = 0; i < 5; i++) {
            guessResult = guessResult * 3 + colors[i].getCode();
        }
        return (byte) (guessResult + Byte.MIN_VALUE);
    }

    public static String getAsString(byte guessResult) {
        int normed = guessResult - Byte.MIN_VALUE;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(LetterGuessResult.getByCode(normed % 3).toString());
            normed /= 3;
        }
        return sb.reverse().toString();
    }
}
