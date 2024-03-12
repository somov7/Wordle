package ru.catghoti.wordle;

import lombok.Builder;

@Builder
public class EnthropyCache {

    private final double[][] cache;

    public EnthropyCache(int size) {
        this.cache = new double[size + 1][];
        for (int i = 0; i <= size; i++) {
            this.cache[i] = new double[i + 1];
            for (int j = 0; j <= i; j++) {
                double ratio = ((double) j) / i;
                cache[i][j] = -ratio * Math.log(ratio);
            }
        }
    }

    public double getEnthropy(int num, int denum) {
        return cache[denum][num];
    }
}
