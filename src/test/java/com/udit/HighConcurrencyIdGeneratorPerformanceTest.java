package com.udit;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HighConcurrencyIdGeneratorPerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(HighConcurrencyIdGeneratorTest.class);

    @Test
    public void generateId_withSingleThread() {
        int iterations = 1000000; // 1 million

        HighConcurrencyIdGenerator idGenerator = new HighConcurrencyIdGenerator(897);
        long beginTimestamp = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            idGenerator.getNextId();
        }
        long endTimestamp = System.currentTimeMillis();

        long duration = (endTimestamp - beginTimestamp);
        long idsPerMs = iterations / duration;
        logger.info("Single Thread:: IDs per ms: {}", idsPerMs);
    }

    @Test
    public void generateId_withMultipleThreads() throws InterruptedException {
        int iterations = 1000000; // 1 million
        int numThreads = 50;

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(iterations);

        HighConcurrencyIdGenerator idGenerator = new HighConcurrencyIdGenerator(897);

        long beginTimestamp = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            executorService.submit(() -> {
                idGenerator.getNextId();
                latch.countDown();
            });
        }

        latch.await();
        long endTimestamp = System.currentTimeMillis();
        long duration = (endTimestamp - beginTimestamp);
        long idsPerMs = iterations / duration;
        logger.info("{} Threads:: IDs per ms: {}", numThreads, idsPerMs);

        executorService.shutdown();
    }
}
