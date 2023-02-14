package com.dabomstew.pkrandom.romhandlers.romentries;

import com.dabomstew.pkrandom.romhandlers.Gen2RomHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Gen2RomEntry extends AbstractGBCRomEntry {

    protected static class Gen2RomEntryReader<T extends Gen2RomEntry> extends GBCRomEntryReader<T> {

        public Gen2RomEntryReader(String fileName) throws IOException {
            super(fileName);
            putSpecialKeyMethod("Type", Gen2RomEntry::setCrystal);
            putSpecialKeyMethod("StaticPokemon{}", Gen2RomEntry::addStaticPokemon);
            putSpecialKeyMethod("StaticPokemonGameCorner{}", Gen2RomEntry::addStaticPokemonGameCorner);
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
            return (T) new Gen2RomEntry(name);
        }

        public static Gen2RomHandler.StaticPokemon parseStaticPokemon(String unparsed, boolean gameCorner) {
            int[] speciesOffsets = new int[0];
            int[] levelOffsets = new int[0];
            String pattern = "[A-z]+=\\[(0x[0-9a-fA-F]+,?\\s?)+]";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(unparsed);
            while (m.find()) {
                String[] segments = m.group().split("=");
                String[] romOffsets = segments[1].substring(1, segments[1].length() - 1).split(",");
                int[] offsets = new int[romOffsets.length];
                for (int i = 0; i < offsets.length; i++) {
                    offsets[i] = RomEntryReader.parseInt(romOffsets[i]);
                }
                switch (segments[0]) {
                    case "Species" -> speciesOffsets = offsets;
                    case "Level" -> levelOffsets = offsets;
                }
            }
            // the only part which differs from Gen1RomEntryReader.parseStaticPokemon()
            Gen2RomHandler.StaticPokemon sp;
            if (gameCorner) {
                sp = new Gen2RomHandler.StaticPokemonGameCorner(speciesOffsets, levelOffsets);
            } else {
                sp = new Gen2RomHandler.StaticPokemon(speciesOffsets, levelOffsets);
            }
            return sp;
        }
    }

    public static void readEntriesFromInfoFile(String fileName, Collection<Gen2RomEntry> romEntries) throws IOException {
        RomEntryReader<Gen2RomEntry> rer = new Gen2RomEntry.Gen2RomEntryReader<>(fileName);
        rer.readAllRomEntries(romEntries);
    }

    private boolean crystal;
    private final List<Gen2RomHandler.StaticPokemon> staticPokemon = new ArrayList<>();

    public Gen2RomEntry(String name) {
        super(name);
    }

    public boolean isCrystal() {
        return crystal;
    }

    private void setCrystal(String unparsed) {
        crystal = unparsed.equalsIgnoreCase("Crystal");
    }

    public List<Gen2RomHandler.StaticPokemon> getStaticPokemon() {
        return Collections.unmodifiableList(staticPokemon);
    }

    private void addStaticPokemon(String unparsed) {
        staticPokemon.add(Gen2RomEntryReader.parseStaticPokemon(unparsed, false));
    }

    private void addStaticPokemonGameCorner(String unparsed) {
        staticPokemon.add(Gen2RomEntryReader.parseStaticPokemon(unparsed, true));
    }

    @Override
    public void copyFrom(RomEntry other) {
        super.copyFrom(other);
        if (other instanceof Gen2RomEntry gen2Other) {
            if (getIntValue("CopyStaticPokemon") == 1) {
                staticPokemon.addAll(gen2Other.staticPokemon);
                intValues.put("StaticPokemonSupport", 1);
            } else {
                intValues.put("StaticPokemonSupport", 0);
                intValues.remove("StaticPokemonOddEggOffset");
                intValues.remove("StaticPokemonOddEggDataSize");
            }
        }
    }
}
