package com.udit;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HighConcurrencyIdGenerator.
 */
public class HighConcurrencyIdGeneratorTest {

    /**
     * Test to ensure that generated ID has correct parts filled.
     */
    @Test
    public void generateId_shouldGenerateIdWithCorrectBitsFilled() {
        HighConcurrencyIdGenerator idGenerator = new HighConcurrencyIdGenerator(784);

        long beforeTimestamp = Instant.now().toEpochMilli();
        String id = idGenerator.getNextId();
        long idNumber = 0;

        // Validate different parts of the ID
        // Define the regular expression pattern to extract the long value
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(id);

        // Find and extract the long value
        if (matcher.find()) {
            String longValueStr = matcher.group();
            try {
                idNumber = Long.parseLong(longValueStr);
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse long value: " + e.getMessage());
            }
        } else {
            System.err.println("No long value found in the input string.");
        }
        long[] parts = idGenerator.fetchIdParts(idNumber);
        assertTrue(parts[0] >= beforeTimestamp, "Timestamp part is incorrect.");
        assertEquals(784, parts[1], "Server ID part is incorrect.");
        assertEquals(0, parts[2], "Sequence part should start from 0.");
    }

    /**
     * Test to ensure that each generated ID is unique.
     */
    @Test
    public void generateId_shouldGenerateUniqueId() {
        HighConcurrencyIdGenerator idGenerator = new HighConcurrencyIdGenerator(234);
        int iterations = 5000;

        // Validate that the IDs are unique even if they are generated in the same millisecond
        String[] ids = new String[iterations];
        for (int i = 0; i < iterations; i++) {
            ids[i] = idGenerator.getNextId();
        }

        for (int i = 0; i < ids.length; i++) {
            for (int j = i + 1; j < ids.length; j++) {
                assertNotEquals(ids[i], ids[j], "Duplicate ID found: " + ids[i]);
            }
        }
    }

    /**
     * Test to ensure that each generated ID is unique when called from multiple threads.
     */
    @Test
    public void generateId_shouldGenerateUniqueIdIfCalledFromMultipleThreads() throws InterruptedException, ExecutionException {
        int numThreads = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        int iterations = 50000;
        CountDownLatch latch = new CountDownLatch(iterations);

        HighConcurrencyIdGenerator idGenerator = new HighConcurrencyIdGenerator(234);

        // Validate that the IDs are unique even if they are generated in the same millisecond in different threads
        Future<String>[] futures = new Future[iterations];
        for (int i = 0; i < iterations; i++) {
            futures[i] = executorService.submit(() -> {
                String id = idGenerator.getNextId();
                latch.countDown();
                return id;
            });
        }

        latch.await();
        for (int i = 0; i < futures.length; i++) {
            for (int j = i + 1; j < futures.length; j++) {
                assertNotSame(futures[i].get(), futures[j].get(), "Duplicate ID found between threads.");
            }
        }

        executorService.shutdown();
    }
}
