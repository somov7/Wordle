package ru.catghoti.wordle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;

public class SolutionCache {

    Cache<BucketAndStep, SolutionTree> cache;

    public SolutionCache() {
        this.cache = CacheBuilder.newBuilder()
                .softValues()
                .build();
    }

    public SolutionTree get(Set<String> bucket, int step) {
        return cache.getIfPresent(new BucketAndStep(bucket, step));
    }

    public void put(Set<String> bucket, int step, SolutionTree solutionTree) {
        cache.put(new BucketAndStep(bucket, step), solutionTree);
    }

    public CacheStats getStats() {
        return cache.stats();
    }

    private record BucketAndStep(Set<String> bucket, int step) {

    }
}
