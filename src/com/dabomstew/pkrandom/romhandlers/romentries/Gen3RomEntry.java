package com.dabomstew.pkrandom.romhandlers.romentries;

import com.dabomstew.pkrandom.constants.Gen3Constants;
import com.dabomstew.pkrandom.romhandlers.Gen3RomHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Gen3RomEntry extends AbstractGBRomEntry {

    protected static class Gen3RomEntryReader<T extends Gen3RomEntry> extends RomEntryReader<T> {

        public Gen3RomEntryReader(String fileName) throws IOException {
            super(fileName);
            putSpecialKeyMethod("Type", Gen3RomEntry::setRomType);
            putSpecialKeyMethod("TableFile", Gen3RomEntry::setTableFile);
            putSpecialKeyMethod("CopyStaticPokemon", Gen3RomEntry::setCopyStaticPokemon);
            putSpecialKeyMethod("StaticPokemon{}", Gen3RomEntry::addStaticPokemon);
            putSpecialKeyMethod("RoamingPokemon{}", Gen3RomEntry::addRoamingPokemon);
            putSpecialKeyMethod("TMText[]", Gen3RomEntry::addTMText);
            putSpecialKeyMethod("MoveTutorText[]", Gen3RomEntry::addMoveTutorText);
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
            return (T) new Gen3RomEntry(name);
        }

        private static Gen3RomHandler.StaticPokemon parseStaticPokemon(String staticPokemonString) {
            int[] speciesOffsets = new int[0];
            int[] levelOffsets = new int[0];
            String pattern = "[A-z]+=\\[(0x[0-9a-fA-F]+,?\\s?)+]";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(staticPokemonString);
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
            return new Gen3RomHandler.StaticPokemon(speciesOffsets, levelOffsets);
        }

        private static Gen3RomHandler.TMOrMTTextEntry parseTMOrMTEntry(String unparsed, boolean isMoveTutor) {
            if (unparsed.startsWith("[") && unparsed.endsWith("]")) {
                String[] parts = unparsed.substring(1, unparsed.length() - 1).split(",", 6);
                Gen3RomHandler.TMOrMTTextEntry tte = new Gen3RomHandler.TMOrMTTextEntry();
                tte.number = RomEntryReader.parseInt(parts[0]);
                tte.mapBank = RomEntryReader.parseInt(parts[1]);
                tte.mapNumber = RomEntryReader.parseInt(parts[2]);
                tte.personNum = RomEntryReader.parseInt(parts[3]);
                tte.offsetInScript = RomEntryReader.parseInt(parts[4]);
                tte.template = parts[5];
                tte.isMoveTutor = isMoveTutor;
                return tte;
            }
            return null;
        }
    }

    public static void readEntriesFromInfoFile(String fileName, Collection<Gen3RomEntry> romEntries) throws IOException {
        RomEntryReader<Gen3RomEntry> rer = new Gen3RomEntry.Gen3RomEntryReader<>(fileName);
        rer.readAllRomEntries(romEntries);
    }

    private String tableFile; // really same as "extraTableFile" used by the Gen1/2 games...
    private boolean copyStaticPokemon;
    private final List<Gen3RomHandler.StaticPokemon> staticPokemon = new ArrayList<>();
    private final List<Gen3RomHandler.StaticPokemon> roamingPokemon = new ArrayList<>();
    private final List<Gen3RomHandler.TMOrMTTextEntry> tmmtTexts = new ArrayList<>();

    public Gen3RomEntry(String name) {
        super(name);
    }

    private void setRomType(String unparsed) {
        if (unparsed.equalsIgnoreCase("Ruby")) {
            setRomType(Gen3Constants.RomType_Ruby);
        } else if (unparsed.equalsIgnoreCase("Sapp")) {
            setRomType(Gen3Constants.RomType_Sapp);
        } else if (unparsed.equalsIgnoreCase("Em")) {
            setRomType(Gen3Constants.RomType_Em);
        } else if (unparsed.equalsIgnoreCase("FRLG")) {
            setRomType(Gen3Constants.RomType_FRLG);
        } else {
            System.err.println("unrecognised rom type: " + unparsed);
        }
    }

    public String getTableFile() {
        return tableFile;
    }

    private void setTableFile(String tableFile) {
        this.tableFile = tableFile;
    }

    private void setCopyStaticPokemon(String unparsed) {
        int csp = RomEntryReader.parseInt(unparsed);
        copyStaticPokemon = (csp > 0);
    }

    public List<Gen3RomHandler.StaticPokemon> getStaticPokemon() {
        return Collections.unmodifiableList(staticPokemon);
    }

    private void addStaticPokemon(String unparsed) {
        staticPokemon.add(Gen3RomEntryReader.parseStaticPokemon(unparsed));
    }

    public List<Gen3RomHandler.StaticPokemon> getRoamingPokemon() {
        return Collections.unmodifiableList(roamingPokemon);
    }

    private void addRoamingPokemon(String unparsed) {
        roamingPokemon.add(Gen3RomEntryReader.parseStaticPokemon(unparsed));
    }

    public List<Gen3RomHandler.TMOrMTTextEntry> getTMMTTexts() {
        return Collections.unmodifiableList(tmmtTexts);
    }

    private void addTMText(String unparsed) {
        Gen3RomHandler.TMOrMTTextEntry tte = Gen3RomEntryReader.parseTMOrMTEntry(unparsed, false);
        if (tte != null) {
            tmmtTexts.add(tte);
        }
    }

    private void addMoveTutorText(String unparsed) {
        Gen3RomHandler.TMOrMTTextEntry tte = Gen3RomEntryReader.parseTMOrMTEntry(unparsed, true);
        if (tte != null) {
            tmmtTexts.add(tte);
        }
    }

    @Override
    public void copyFrom(RomEntry other) {
        super.copyFrom(other);
        if (other instanceof Gen3RomEntry gen3Other) {
            if (copyStaticPokemon) {
                staticPokemon.addAll(gen3Other.staticPokemon);
                roamingPokemon.addAll(gen3Other.roamingPokemon);
                putIntValue("StaticPokemonSupport", 1);
            } else {
                putIntValue("StaticPokemonSupport", 0);
            }
            if (getIntValue("CopyTMText") == 1) {
                tmmtTexts.addAll(gen3Other.tmmtTexts);
            }
            tableFile = gen3Other.tableFile;
        }
    }

}
