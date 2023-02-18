package com.dabomstew.pkrandom.romhandlers.romentries;

public class DSFileEntry {

    private final String path;
    private final long expectedCRC32;

    public DSFileEntry() {
        this("", 0);
    }

    public DSFileEntry(String path, long expectedCRC32) {
        this.path = path;
        this.expectedCRC32 = expectedCRC32;
    }

    public String getPath() {
        return path;
    }

    public long getExpectedCRC32() {
        return expectedCRC32;
    }
}
