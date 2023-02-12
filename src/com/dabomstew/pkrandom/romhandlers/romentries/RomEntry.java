package com.dabomstew.pkrandom.romhandlers.romentries;

import java.util.HashMap;
import java.util.Map;

public abstract class RomEntry {
    protected final String name;
    protected String romCode;
    protected int romType;
    protected Map<String, Integer> intEntries = new HashMap<>();
    protected Map<String, String> stringEntries = new HashMap<>();
    protected Map<String, int[]> arrayEntries = new HashMap<>();

    public RomEntry(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getRomCode() {
        return romCode;
    }

    public void setRomCode(String romCode) {
        this.romCode = romCode;
    }

    public int getIntValue(String key) {
        if (!intEntries.containsKey(key)) {
            intEntries.put(key, 0);
        }
        return intEntries.get(key);
    }

    public String getStringValue(String key) {
        if (!stringEntries.containsKey(key)) {
            stringEntries.put(key, "");
        }
        return stringEntries.get(key);
    }

    public int[] getArrayValue(String key) {
        if (!arrayEntries.containsKey(key)) {
            arrayEntries.put(key, new int[0]);
        }
        return arrayEntries.get(key);
    }

}
