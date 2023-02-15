package com.dabomstew.pkrandom.romhandlers.romentries;

import java.io.IOException;

public abstract class AbstractGBRomEntry extends RomEntry {

    protected abstract static class GBRomEntryReader<T extends AbstractGBRomEntry> extends RomEntryReader<T> {

        public GBRomEntryReader(String fileName) throws IOException {
            super(fileName);
            putSpecialKeyMethod("CRC32", AbstractGBRomEntry::setExpectedCRC32);
        }
    }

    private int version;
    private long expectedCRC32 = -1;

    public AbstractGBRomEntry(String name) {
        super(name);
    }

    public AbstractGBRomEntry(AbstractGBRomEntry original) {
        super(original);
        this.version = original.version;
        this.expectedCRC32 = original.expectedCRC32;
    }

    public long getExpectedCRC32() {
        return expectedCRC32;
    }

    private void setExpectedCRC32(String s) {
        this.expectedCRC32 = BaseRomEntryReader.parseLong("0x" + s);
    }

}
