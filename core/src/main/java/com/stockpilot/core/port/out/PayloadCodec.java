package com.stockpilot.core.port.out;

/** Turns requests and responses into text and back, so they can be hashed and stored as snapshots. */
public interface PayloadCodec {

    String write(Object payload);

    <T> T read(String json, Class<T> type);
}
