package com.dabomstew.pkrandom.romhandlers.romentries;

import java.io.IOException;

public class AbstractDSRomEntry extends RomEntry {

    protected abstract static class DSRomEntryReader<T extends AbstractDSRomEntry> extends RomEntryReader<T> {

        public DSRomEntryReader(String fileName) throws IOException {
            super(fileName);
        }
    }

    public AbstractDSRomEntry(String name) {
        super(name);
    }

}
