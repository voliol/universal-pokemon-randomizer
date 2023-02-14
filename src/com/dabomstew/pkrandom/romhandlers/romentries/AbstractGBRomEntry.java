package com.dabomstew.pkrandom.romhandlers.romentries;

import java.io.IOException;

public abstract class AbstractGBRomEntry extends RomEntry {

    protected abstract static class GBCRomEntryReader<T extends AbstractGBCRomEntry> extends RomEntryReader<T> {

        public GBCRomEntryReader(String fileName) throws IOException {
            super(fileName);
            putSpecialKeyMethod("Version", AbstractGBRomEntry::setVersion);
            putSpecialKeyMethod("CRC32", AbstractGBRomEntry::setExpectedCRC32);
        }
    }

    private int version;
    private long expectedCRC32 = -1;

    public AbstractGBRomEntry(String name) {
        super(name);
    }

    public int getVersion() {
        return version;
    }

    private void setVersion(String unparsed) {
        this.version = RomEntryReader.parseInt(unparsed);
    }

    public long getExpectedCRC32() {
        return expectedCRC32;
    }

    private void setExpectedCRC32(String unparsed) {
        this.expectedCRC32 = RomEntryReader.parseLong("0x" + unparsed);
    }

}
