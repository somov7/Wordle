package ru.catghoti.wordle;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum LetterGuessResult {
    GREY("⬛", (byte) 0),
    YELLOW("\uD83D\uDFE8", (byte) 1),
    GREEN("\uD83D\uDFE9", (byte) 2),
    ;

    private final String representation;
    private final byte code;

    private static final LetterGuessResult[] byCode = {GREY, YELLOW, GREEN};

    LetterGuessResult(String representation, byte code) {
        this.representation = representation;
        this.code = code;
    }

    @Override
    public String toString() {
        return representation;
    }

    public byte getCode() {
        return code;
    }

    public static LetterGuessResult getByCode(int code) {
        return byCode[code];
    }
}
