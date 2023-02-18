package com.dabomstew.pkrandom.romhandlers.romentries;

public class ThreeDSFileEntry {
    private String path;
    private long[] expectedCRC32s;

    public ThreeDSFileEntry() {
        this("", new long[2]);
    }
    public ThreeDSFileEntry(String path, long[] expectedCRC32s) {
        this.path = path;
        this.expectedCRC32s = expectedCRC32s;
    }

    public String getPath() {
        return path;
    }

    public long[] getExpectedCRC32s() {
        return expectedCRC32s;
    }
}
