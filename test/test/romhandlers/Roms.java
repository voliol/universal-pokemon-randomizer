package test.romhandlers;

import com.dabomstew.pkrandom.romhandlers.romentries.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Constants for all ROM names, to easily test sets of ROMs.
 */
public class Roms {

    private static List<String> readGen1RomEntryNames() {
        List<Gen1RomEntry> roms = new ArrayList<>();
        try {
            Gen1RomEntry.readEntriesFromInfoFile("gen1_offsets.ini", roms);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return roms.stream().map(RomEntry::getName).collect(Collectors.toList());
    }

    private static List<String> readGen2RomEntryNames() {
        List<Gen2RomEntry> roms = new ArrayList<>();
        try {
            Gen2RomEntry.readEntriesFromInfoFile("gen2_offsets.ini", roms);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return roms.stream().map(RomEntry::getName).collect(Collectors.toList());
    }

    private static List<String> readGen3RomEntryNames() {
        List<Gen3RomEntry> roms = new ArrayList<>();
        try {
            Gen3RomEntry.readEntriesFromInfoFile("gen3_offsets.ini", roms);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return roms.stream().map(RomEntry::getName).collect(Collectors.toList());
    }

    private static List<String> readGen4RomEntryNames() {
        List<Gen4RomEntry> roms = new ArrayList<>();
        try {
            Gen4RomEntry.readEntriesFromInfoFile("gen4_offsets.ini", roms);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return roms.stream().map(RomEntry::getName).collect(Collectors.toList());
    }

    private static List<String> readGen5RomEntryNames() {
        List<Gen5RomEntry> roms = new ArrayList<>();
        try {
            Gen5RomEntry.readEntriesFromInfoFile("gen5_offsets.ini", roms);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return roms.stream().map(RomEntry::getName).collect(Collectors.toList());
    }

    private static List<String> readGen6RomEntryNames() {
        List<Gen6RomEntry> roms = new ArrayList<>();
        try {
            Gen6RomEntry.readEntriesFromInfoFile("gen6_offsets.ini", roms);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return roms.stream().map(RomEntry::getName).collect(Collectors.toList());
    }

    private static List<String> readGen7RomEntryNames() {
        List<Gen7RomEntry> roms = new ArrayList<>();
        try {
            Gen7RomEntry.readEntriesFromInfoFile("gen7_offsets.ini", roms);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return roms.stream().map(RomEntry::getName).collect(Collectors.toList());
    }

    private static final List<String> ALL_GEN_1_ROMS = readGen1RomEntryNames();
    private static final List<String> ALL_GEN_2_ROMS = readGen2RomEntryNames();
    private static final List<String> ALL_GEN_3_ROMS = readGen3RomEntryNames();
    private static final List<String> ALL_GEN_4_ROMS = readGen4RomEntryNames();
    private static final List<String> ALL_GEN_5_ROMS = readGen5RomEntryNames();
    private static final List<String> ALL_GEN_6_ROMS = readGen6RomEntryNames();
    private static final List<String> ALL_GEN_7_ROMS = readGen7RomEntryNames();

    private static final List<String> ALL_ROMS = combine(ALL_GEN_1_ROMS, ALL_GEN_2_ROMS, ALL_GEN_3_ROMS,
            ALL_GEN_4_ROMS, ALL_GEN_5_ROMS, ALL_GEN_6_ROMS, ALL_GEN_7_ROMS);

    public static String[] getAllRoms() {
        return ALL_ROMS.toArray(String[]::new);
    }

    @SafeVarargs
    private static List<String> combine(List<String> first, List<String>... others) {
        List<String> combined = new ArrayList<>(first);
        for (List<String> other : others) {
            combined.addAll(other);
        }
        return combined;
    }
}
