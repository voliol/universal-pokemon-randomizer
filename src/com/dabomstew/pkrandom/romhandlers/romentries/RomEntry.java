package com.dabomstew.pkrandom.romhandlers.romentries;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A description of a ROM
 */
public abstract class RomEntry {

    protected abstract static class RomEntryReader<T extends RomEntry> extends BaseRomEntryReader<T> {

        public RomEntryReader(String fileName, DefaultReadMode defaultReadMode, CopyFromMode copyFromMode)
                throws IOException {
            super(fileName, defaultReadMode, copyFromMode);
            putSpecialKeyMethod("Game", RomEntry::setRomCode);
            putSpecialKeyMethod("Version", RomEntry::setVersion);
            putSpecialKeyMethod("Type", RomEntry::setRomType);
            putKeySuffixMethod("Tweak", RomEntry::putTweakFile);
        }

    }

    protected final String name;
    protected String romCode;
    protected int version;
    protected int romType;
    protected Map<String, Integer> intValues = new HashMap<>();
    protected Map<String, String> stringValues = new HashMap<>();
    protected Map<String, int[]> arrayValues = new HashMap<>();
    protected Map<String, String> tweakFiles = new HashMap<>(); // TODO: is this a good name?

    public RomEntry(String name) {
        this.name = name;
    }

    public RomEntry(RomEntry original) {
        this.name = original.name;
        this.romCode = original.romCode;
        this.version = original.version;
        this.romType = original.romType;
        intValues.putAll(original.intValues);
        stringValues.putAll(original.stringValues);
        arrayValues.putAll(original.arrayValues);
        tweakFiles.putAll(original.tweakFiles);
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

    public int getVersion() {
        return version;
    }

    private void setVersion(String s) {
        this.version = BaseRomEntryReader.parseInt(s);
    }

    public int getRomType() {
        return romType;
    }

    public void setRomType(int romType) {
        this.romType = romType;
    }

    protected abstract void setRomType(String s);

    public int getIntValue(String key) {
        if (!intValues.containsKey(key)) {
            intValues.put(key, 0);
        }
        return intValues.get(key);
    }

    public void putIntValue(String key, int value) {
        intValues.put(key, value);
    }

    protected void putIntValue(String[] valuePair) {
        putIntValue(valuePair[0], BaseRomEntryReader.parseInt(valuePair[1]));
    }

    public String getStringValue(String key) {
        if (!stringValues.containsKey(key)) {
            stringValues.put(key, "");
        }
        return stringValues.get(key);
    }

    public void putStringValue(String key, String value) {
        stringValues.put(key, value);
    }

    protected void putStringValue(String[] valuePair) {
        putStringValue(valuePair[0], valuePair[1]);
    }

    public int[] getArrayValue(String key) {
        if (!arrayValues.containsKey(key)) {
            arrayValues.put(key, new int[0]);
        }
        return arrayValues.get(key);
    }

    public void putArrayValue(String key, int[] value) {
        arrayValues.put(key, value);
    }

    public String getTweakFile(String key) {
        if (!tweakFiles.containsKey(key)) {
            tweakFiles.put(key, "");
        }
        return tweakFiles.get(key);
    }

    public boolean hasTweakFile(String key) {
        return getTweakFile(key).equals("");
    }

    public void putTweakFile(String key, String value) {
        tweakFiles.put(key, value);
    }

    private void putTweakFile(String[] valuePair) {
        putTweakFile(valuePair[0], valuePair[1]);
    }

    public abstract boolean hasStaticPokemonSupport();

    public void copyFrom(RomEntry other) {
        intValues.putAll(other.intValues);
        stringValues.putAll(other.stringValues);
        arrayValues.putAll(other.arrayValues);
    }

}
