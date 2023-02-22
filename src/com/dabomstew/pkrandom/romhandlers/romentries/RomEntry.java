package com.dabomstew.pkrandom.romhandlers.romentries;

import java.util.HashMap;
import java.util.Map;

/**
 * A description of a ROM
 */
public abstract class RomEntry extends IniEntry {

    protected abstract static class RomEntryReader<T extends RomEntry> extends IniEntryReader<T> {

        protected enum CopyFromMode { NAME, ROMCODE }

        private final CopyFromMode copyFromMode;

        public RomEntryReader(DefaultReadMode defaultReadMode, CopyFromMode copyFromMode) {
            super(defaultReadMode);
            this.copyFromMode = copyFromMode;
            putSpecialKeyMethod("Game", RomEntry::setRomCode);
            putSpecialKeyMethod("Version", RomEntry::setVersion);
            putSpecialKeyMethod("Type", RomEntry::setRomType);
            putKeySuffixMethod("Tweak", RomEntry::putTweakFile);
        }

        @Override
        protected boolean matchesCopyFromValue(T other, String value) {
            return switch (copyFromMode) {
                case NAME -> value.equalsIgnoreCase(other.getName());
                case ROMCODE -> value.equals(other.getRomCode());
            };
        }

    }

    protected String romCode;
    protected int version;
    protected int romType;
    protected Map<String, String> tweakFiles = new HashMap<>(); // TODO: is this a good name?

    public RomEntry(String name) {
        super(name);
    }

    public RomEntry(RomEntry original) {
        super(original.name);
        this.romCode = original.romCode;
        this.version = original.version;
        this.romType = original.romType;
        intValues.putAll(original.intValues);
        stringValues.putAll(original.stringValues);
        arrayValues.putAll(original.arrayValues);
        tweakFiles.putAll(original.tweakFiles);
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
        this.version = IniEntryReader.parseInt(s);
    }

    public int getRomType() {
        return romType;
    }

    public void setRomType(int romType) {
        this.romType = romType;
    }

    protected abstract void setRomType(String s);

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

}
