package com.udit;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * HighConcurrencyIdGenerator is a distributed sequence generator.
 * It uses snowflake algorithm to achieve concurrency, fault tolerance, and scalability.
 */
public class HighConcurrencyIdGenerator {

    private static final Logger logger = LoggerFactory.getLogger(HighConcurrencyIdGenerator.class);

    private static final int timestampBits = AppConstant.TIMESTAMP_BITS;
    private static final int serverIdBits = AppConstant.SERVER_ID_BITS;
    private static final int counterBits = AppConstant.COUNTER_BITS;

    private static final long maxServerId = (1L << serverIdBits) - 1;
    private static final long maxCounter = (1L << counterBits) - 1;

    private static final long defaultEpoch = AppConstant.DEFAULT_EPOCH;
    private static final String suffix = "TD";
    private static final String prefix = "IND";

    private final long serverId;
    private final long customEpoch;

    private volatile long lastTimestamp = -1L;
    private volatile long counter = 0L;

    /**
     * Creates an instance with a given serverId and custom epoch.
     *
     * @param serverId the unique identifier for the server.
     * @param customEpoch the custom epoch to be used.
     */
    public HighConcurrencyIdGenerator(long serverId, long customEpoch) {
        if (serverId < 0 || serverId > maxServerId) {
            throw new IllegalArgumentException(String.format("ServerId must be between %d and %d", 0, maxServerId));
        }
        this.serverId = serverId;
        this.customEpoch = customEpoch;
    }

    /**
     * Creates an instance with a given serverId and default epoch.
     *
     * @param serverId the unique identifier for the server.
     */
    public HighConcurrencyIdGenerator(long serverId) {
        this(serverId, defaultEpoch);
    }

    /**
     * Generates the next unique identifier.
     *
     * @return a unique identifier.
     */
    public synchronized String getNextId() {
        long currentTimestamp = getCurrentTimestamp();

        if (currentTimestamp < lastTimestamp) {
            logger.error("Invalid System Clock detected!");
            throw new IllegalStateException("Invalid System Clock!");
        }

        if (currentTimestamp == lastTimestamp) {
            counter = (counter + 1) & maxCounter;
            if (counter == 0) {
                currentTimestamp = waitUntilNextMillis(currentTimestamp);
            }
        } else {
            counter = 0;
        }

        lastTimestamp = currentTimestamp;

        long id = (currentTimestamp << (serverIdBits + counterBits))
                | (serverId << counterBits)
                | counter;

//        logger.info("Generated ID: {}", id);
        return prefix + id + suffix;
    }

    /**
     * Gets the current timestamp in milliseconds, adjusted for the custom epoch.
     *
     * @return the current timestamp in milliseconds.
     */
    private long getCurrentTimestamp() {
        return Instant.now().toEpochMilli() - customEpoch;
    }

    /**
     * Blocks until the next millisecond.
     *
     * @param currentTimestamp the current timestamp.
     * @return the next timestamp.
     */
    private long waitUntilNextMillis(long currentTimestamp) {
        while (currentTimestamp == lastTimestamp) {
            currentTimestamp = getCurrentTimestamp();
        }
        return currentTimestamp;
    }

    /**
     * Parses a generated ID to extract the timestamp, server ID, and sequence.
     *
     * @param id the generated ID.
     * @return an array containing the timestamp, server ID, and sequence.
     */
    public long[] fetchIdParts(long id) {
        long maskServerId = ((1L << serverIdBits) - 1) << counterBits;
        long maskCounter = (1L << counterBits) - 1;

        long timestamp = (id >> (serverIdBits + counterBits)) + customEpoch;
        long serverId = (id & maskServerId) >> counterBits;
        long sequence = id & maskCounter;

        return new long[]{timestamp, serverId, sequence};
    }

    @Override
    public String toString() {
        return "HighConcurrencyIdGenerator Settings [TIMESTAMP_BITS=" + timestampBits + ", SERVER_ID_BITS=" + serverIdBits
                + ", COUNTER_BITS=" + counterBits + ", CUSTOM_EPOCH=" + customEpoch
                + ", ServerId=" + serverId + "]";
    }

    public static void main(String[] args) {
        HighConcurrencyIdGenerator idGenerator = new HighConcurrencyIdGenerator(455);
        for (int i = 0; i < 10; i++) {
            System.out.println(idGenerator.getNextId());
        }
    }
}
