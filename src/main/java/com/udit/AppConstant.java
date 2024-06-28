package com.udit;

public class AppConstant {

    // 41 bits reserved for timestamp bits.
    public static final int TIMESTAMP_BITS = 41;

    // We have reserved 10 bits for server. Thus, we can have 1024 different servers running in parallel.
    public static final int SERVER_ID_BITS = 10;

    // 12 bit for sequence. This means that we can generate 2^12 = 4096 in one milliseconds on each server.
    public static final int COUNTER_BITS = 12;

    // Custom Epoch (Thu Nov 04 2010 07:12:54 GMT+0530 (India Standard Time))
    public static final long DEFAULT_EPOCH = 1288834974657L;
}