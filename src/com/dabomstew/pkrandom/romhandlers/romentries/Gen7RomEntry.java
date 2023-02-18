package com.dabomstew.pkrandom.romhandlers.romentries;

import com.dabomstew.pkrandom.constants.Gen6Constants;
import com.dabomstew.pkrandom.constants.Gen7Constants;

import java.io.IOException;
import java.util.Collection;

public class Gen7RomEntry extends Abstract3DSRomEntry {

    private static class Gen7RomEntryReader<T extends Gen7RomEntry> extends ThreeDSRomEntryReader<T> {

        public Gen7RomEntryReader(String fileName) throws IOException {
            super(fileName);
            putSpecialKeyMethod("Type", Gen7RomEntry::setRomType);
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
            return (T) new Gen7RomEntry(name);
        }
    }

    public static void readEntriesFromInfoFile(String fileName, Collection<Gen7RomEntry> romEntries) throws IOException {
        BaseRomEntryReader<Gen7RomEntry> rer = new Gen7RomEntry.Gen7RomEntryReader<>(fileName);
        rer.readAllRomEntries(romEntries);
    }

    public Gen7RomEntry(String name) {
        super(name);
    }

    @Override
    protected void setRomType(String s) {
        if (s.equalsIgnoreCase("USUM")) {
            setRomType(Gen7Constants.Type_USUM);
        } else {
            setRomType(Gen7Constants.Type_SM);
        }
    }

}
