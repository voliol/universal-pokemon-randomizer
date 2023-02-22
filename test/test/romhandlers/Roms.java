package test.romhandlers;

import com.dabomstew.pkrandom.romhandlers.romentries.*;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Constants for all ROM names, to easily test sets of ROMs.
 */
public class Roms {

    private static final List<String> ALL_GEN_1_ROMS;
    private static final List<String> ALL_GEN_2_ROMS;
    private static final List<String> ALL_GEN_3_ROMS;
    private static final List<String> ALL_GEN_4_ROMS;
    private static final List<String> ALL_GEN_5_ROMS;
    private static final List<String> ALL_GEN_6_ROMS;
    private static final List<String> ALL_GEN_7_ROMS;

    static {
        try {
            ALL_GEN_1_ROMS = getNames(Gen1RomEntry.READER.readEntriesFromFile("gen1_offsets.ini"));
            ALL_GEN_2_ROMS = getNames(Gen2RomEntry.READER.readEntriesFromFile("gen2_offsets.ini"));
            ALL_GEN_3_ROMS = getNames(Gen3RomEntry.READER.readEntriesFromFile("gen3_offsets.ini"));
            ALL_GEN_4_ROMS = getNames(Gen4RomEntry.READER.readEntriesFromFile("gen4_offsets.ini"));
            ALL_GEN_5_ROMS = getNames(Gen5RomEntry.READER.readEntriesFromFile("gen5_offsets.ini"));
            ALL_GEN_6_ROMS = getNames(Gen6RomEntry.READER.readEntriesFromFile("gen6_offsets.ini"));
            ALL_GEN_7_ROMS = getNames(Gen7RomEntry.READER.readEntriesFromFile("gen7_offsets.ini"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static final List<String> ALL_ROMS = combine(ALL_GEN_1_ROMS, ALL_GEN_2_ROMS, ALL_GEN_3_ROMS,
            ALL_GEN_4_ROMS, ALL_GEN_5_ROMS, ALL_GEN_6_ROMS, ALL_GEN_7_ROMS);

    private static List<String> getNames(List<? extends RomEntry> romEntries) {
        return romEntries.stream().map(RomEntry::getName).collect(Collectors.toList());
    }

    @SafeVarargs
    private static List<String> combine(List<String> first, List<String>... others) {
        List<String> combined = new ArrayList<>(first);
        for (List<String> other : others) {
            combined.addAll(other);
        }
        return combined;
    }

    public static String[] getAllRoms() {
        return ALL_ROMS.toArray(String[]::new);
    }



}
