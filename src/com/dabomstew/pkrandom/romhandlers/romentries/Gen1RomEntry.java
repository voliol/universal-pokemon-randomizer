package com.dabomstew.pkrandom.romhandlers.romentries;

import com.dabomstew.pkrandom.constants.Gen1Constants;
import com.dabomstew.pkrandom.pokemon.Type;
import com.dabomstew.pkrandom.romhandlers.Gen1RomHandler;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link RomEntry} for Gen 1.
 */
public class Gen1RomEntry extends AbstractGBCRomEntry {

    public static class Gen1RomEntryReader<T extends Gen1RomEntry> extends GBCRomEntryReader<T> {

        protected Gen1RomEntryReader() {
            super();
            putSpecialKeyMethod("StaticPokemon{}", Gen1RomEntry::addStaticPokemon);
            putSpecialKeyMethod("StaticPokemonGhostMarowak{}", Gen1RomEntry::addStaticPokemonGhostMarowak);
            putSpecialKeyMethod("ExtraTypes", Gen1RomEntry::addExtraTypes);
        }

        /**
         * Initiates a RomEntry of this class, since RomEntryReader can't do it on its own.<br>
         * MUST be overridden by any subclass.
         *
         * @param name The name of the RomEntry
         */
        @Override
        @SuppressWarnings("unchecked")
        protected T initiateEntry(String name) {
            return (T) new Gen1RomEntry(name);
        }

        protected static Gen1RomHandler.StaticPokemon parseStaticPokemon(String s) {
            int[] speciesOffsets = new int[0];
            int[] levelOffsets = new int[0];
            String pattern = "[A-z]+=\\[(0x[0-9a-fA-F]+,?\\s?)+]";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(s);
            while (m.find()) {
                String[] segments = m.group().split("=");
                String[] romOffsets = segments[1].substring(1, segments[1].length() - 1).split(",");
                int[] offsets = new int[romOffsets.length];
                for (int i = 0; i < offsets.length; i++) {
                    offsets[i] = IniEntryReader.parseInt(romOffsets[i]);
                }
                switch (segments[0]) {
                    case "Species" -> speciesOffsets = offsets;
                    case "Level" -> levelOffsets = offsets;
                }
            }
            return new Gen1RomHandler.StaticPokemon(speciesOffsets, levelOffsets);
        }
    }

    public static final Gen1RomEntryReader<Gen1RomEntry> READER = new Gen1RomEntryReader<>();

    private final List<Gen1RomHandler.StaticPokemon> staticPokemon = new ArrayList<>();
    private int[] ghostMarowakOffsets = new int[0];
    private final Map<Integer, Type> extraTypeLookup = new HashMap<>();
    private final Map<Type, Integer> extraTypeReverse = new HashMap<>();

    public Gen1RomEntry(String name) {
        super(name);
    }

    public boolean isYellow() {
        return romType == Gen1Constants.Type_Yellow;
    }
    @Override
    protected void setRomType(String s) {
        if (s.equalsIgnoreCase("RB")) {
            setRomType(Gen1Constants.Type_RB);
        } else if (s.equalsIgnoreCase("Yellow")) {
            setRomType(Gen1Constants.Type_Yellow);
        } else {
            System.err.println("unrecognised rom type: " + s);
        }
    }

    public List<Gen1RomHandler.StaticPokemon> getStaticPokemon() {
        return Collections.unmodifiableList(staticPokemon);
    }

    private void addStaticPokemon(String s) {
        staticPokemon.add(Gen1RomEntryReader.parseStaticPokemon(s));
    }

    public int[] getGhostMarowakOffsets() {
        return ghostMarowakOffsets;
    }

    private void addStaticPokemonGhostMarowak(String s) {
        Gen1RomHandler.StaticPokemon ghostMarowak = Gen1RomEntryReader.parseStaticPokemon(s);
        staticPokemon.add(ghostMarowak);
        ghostMarowakOffsets = ghostMarowak.getSpeciesOffsets();
    }

    public Map<Integer, Type> getExtraTypeLookup() {
        return extraTypeLookup;
    }

    public Map<Type, Integer> getExtraTypeReverse() {
        return extraTypeReverse;
    }

    private void addExtraTypes(String s) {
        // remove the containers
        s = s.substring(1, s.length() - 1);
        String[] parts = s.split(",");
        for (String part : parts) {
            String[] iParts = part.split("=");
            int typeId = Integer.parseInt(iParts[0], 16);
            String typeName = iParts[1].trim();
            Type theType = Type.valueOf(typeName);
            extraTypeLookup.put(typeId, theType);
            extraTypeReverse.put(theType, typeId);
        }
    }

    @Override
    public void copyFrom(IniEntry other) {
        super.copyFrom(other);
        if (other instanceof Gen1RomEntry gen1Other) {
            if (getIntValue("CopyStaticPokemon") == 1) {
                staticPokemon.addAll(gen1Other.staticPokemon);
                ghostMarowakOffsets = gen1Other.ghostMarowakOffsets;
                intValues.put("StaticPokemonSupport", 1);
            } else {
                intValues.put("StaticPokemonSupport", 0);
            }
        }

    }
}
