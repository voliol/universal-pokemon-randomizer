package com.dabomstew.pkrandom.romhandlers.romentries;

import com.dabomstew.pkrandom.constants.Gen6Constants;

import java.io.IOException;
import java.util.Collection;

public class Gen6RomEntry extends Abstract3DSRomEntry {

    private static class Gen6RomEntryReader<T extends Gen6RomEntry> extends ThreeDSRomEntryReader<T> {

        public Gen6RomEntryReader(String fileName) throws IOException {
            super(fileName);
            putSpecialKeyMethod("Type", Gen6RomEntry::setRomType);
        }

        /**
         * Initiates a RomEntry of this class, since RomEntryReader can't do it on its own.<br>
         * MUST be overridden by any subclass.
         *
         * @param name The name of the RomEntry
         */
        @Override
        @SuppressWarnings("unchecked")
        protected T initiateRomEntry(String name) {
            return (T) new Gen6RomEntry(name);
        }
    }

    public static void readEntriesFromInfoFile(String fileName, Collection<Gen6RomEntry> romEntries) throws IOException {
        BaseRomEntryReader<Gen6RomEntry> rer = new Gen6RomEntry.Gen6RomEntryReader<>(fileName);
        rer.readAllRomEntries(romEntries);
    }

    public Gen6RomEntry(String name) {
        super(name);
    }

    private void setRomType(String s) {
        if (s.equalsIgnoreCase("ORAS")) {
            setRomType(Gen6Constants.Type_ORAS);
        } else {
            setRomType(Gen6Constants.Type_XY);
        }
    }

}
