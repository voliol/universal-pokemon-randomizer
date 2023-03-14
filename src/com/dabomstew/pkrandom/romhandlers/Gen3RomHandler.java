package com.dabomstew.pkrandom.romhandlers;

/*----------------------------------------------------------------------------*/
/*--  Gen3RomHandler.java - randomizer handler for R/S/E/FR/LG.             --*/
/*--                                                                        --*/
/*--  Part of "Universal Pokemon Randomizer ZX" by the UPR-ZX team          --*/
/*--  Originally part of "Universal Pokemon Randomizer" by Dabomstew        --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2020.                                 --*/
/*--                                                                        --*/
/*--  The custom code written here is licensed under the terms of the GPL:  --*/
/*--                                                                        --*/
/*--  This program is free software: you can redistribute it and/or modify  --*/
/*--  it under the terms of the GNU General Public License as published by  --*/
/*--  the Free Software Foundation, either version 3 of the License, or     --*/
/*--  (at your option) any later version.                                   --*/
/*--                                                                        --*/
/*--  This program is distributed in the hope that it will be useful,       --*/
/*--  but WITHOUT ANY WARRANTY; without even the implied warranty of        --*/
/*--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          --*/
/*--  GNU General Public License for more details.                          --*/
/*--                                                                        --*/
/*--  You should have received a copy of the GNU General Public License     --*/
/*--  along with this program. If not, see <http://www.gnu.org/licenses/>.  --*/
/*----------------------------------------------------------------------------*/

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.dabomstew.pkrandom.*;
import com.dabomstew.pkrandom.romhandlers.romentries.Gen3RomEntry;
import com.dabomstew.pkrandom.constants.*;
import com.dabomstew.pkrandom.exceptions.RandomizerIOException;
import com.dabomstew.pkrandom.gbspace.FreedSpace;
import com.dabomstew.pkrandom.graphics.Gen3to5PaletteHandler;
import com.dabomstew.pkrandom.graphics.Palette;
import com.dabomstew.pkrandom.graphics.PaletteHandler;
import com.dabomstew.pkrandom.pokemon.*;

import compressors.DSCmp;
import compressors.DSDecmp;

public class Gen3RomHandler extends AbstractGBRomHandler {

    public static class Factory extends RomHandler.Factory {

        @Override
        public Gen3RomHandler create(Random random, PrintStream logStream) {
            return new Gen3RomHandler(random, logStream);
        }

        public boolean isLoadable(String filename) {
            long fileLength = new File(filename).length();
            if (fileLength > 32 * 1024 * 1024) {
                return false;
            }
            byte[] loaded = loadFilePartial(filename, 0x100000);
            // nope
            return loaded.length != 0 && detectRomInner(loaded, (int) fileLength);
        }
    }

    public Gen3RomHandler(Random random) {
        super(random, null);
    }

    public Gen3RomHandler(Random random, PrintStream logStream) {
        super(random, logStream);
    }
    
    public static class TMOrMTTextEntry { //TODO: figure out where to put this, public for now
        public int number;
        public int mapBank, mapNumber;
        public int personNum;
        public int offsetInScript;
        public int actualOffset;
        public String template;
        public boolean isMoveTutor;
    }

    private static List<Gen3RomEntry> roms;

    static {
        loadROMInfo();
    }

    private static void loadROMInfo() {
        try {
            roms = Gen3RomEntry.READER.readEntriesFromFile("gen3_offsets.ini");
        } catch (IOException e) {
            throw new RuntimeException("Could not read Rom Entries.", e);
        }
    }

    private void loadTextTable(String filename) {
        try {
            Scanner sc = new Scanner(FileFunctions.openConfig(filename + ".tbl"), StandardCharsets.UTF_8);
            while (sc.hasNextLine()) {
                String q = sc.nextLine();
                if (!q.trim().isEmpty()) {
                    String[] r = q.split("=", 2);
                    if (r[1].endsWith("\r\n")) {
                        r[1] = r[1].substring(0, r[1].length() - 2);
                    }
                    tb[Integer.parseInt(r[0], 16)] = r[1];
                    d.put(r[1], (byte) Integer.parseInt(r[0], 16));
                }
            }
            sc.close();
        } catch (FileNotFoundException e) {
            System.err.println("File not found!");
        }

    }

    // Sub-handlers
    private PaletteHandler paletteHandler;

    // This ROM's data
    private Gen3RomEntry romEntry;
    private Pokemon[] pokes, pokesInternal;
    private List<Pokemon> pokemonList;
    private int numRealPokemon;
    private List<Trainer> trainers;
    private Move[] moves;
    private boolean jamboMovesetHack;
    private boolean havePatchedObedience;
    private String[] tb;
    public Map<String, Byte> d;
    private String[] abilityNames;
    private String[] itemNames;
    private boolean mapLoadingDone;
    private List<Integer> itemOffs;
    private String[][] mapNames;
    private boolean isRomHack;
    private int[] internalToPokedex, pokedexToInternal;
    private int pokedexCount;
    private String[] pokeNames;
    private ItemList allowedItems, nonBadItems;
    private int pickupItemsTableOffset;
    private boolean effectivenessUpdated;

    // Misc.
    private final FreedSpace freedSpace = new FreedSpace();

    @Override
    public boolean detectRom(byte[] rom) {
        return detectRomInner(rom, rom.length);
    }

    private static boolean detectRomInner(byte[] rom, int romSize) {
        if (romSize != Gen3Constants.size8M && romSize != Gen3Constants.size16M && romSize != Gen3Constants.size32M) {
            return false; // size check
        }
        // Special case for Emerald unofficial translation
        if (romName(rom, Gen3Constants.unofficialEmeraldROMName)) {
            // give it a rom code so it can be detected
            rom[Gen3Constants.romCodeOffset] = 'B';
            rom[Gen3Constants.romCodeOffset + 1] = 'P';
            rom[Gen3Constants.romCodeOffset + 2] = 'E';
            rom[Gen3Constants.romCodeOffset + 3] = 'T';
            rom[Gen3Constants.headerChecksumOffset] = 0x66;
        }
        // Wild Pokemon header
        if (find(rom, Gen3Constants.wildPokemonPointerPrefix) == -1) {
            return false;
        }
        // Map Banks header
        if (find(rom, Gen3Constants.mapBanksPointerPrefix) == -1) {
            return false;
        }
        // Pokedex Order header
        if (findMultiple(rom, Gen3Constants.pokedexOrderPointerPrefix).size() != 3) {
            return false;
        }
        for (Gen3RomEntry re : roms) {
            if (romCode(rom, re.getRomCode()) && (rom[Gen3Constants.romVersionOffset] & 0xFF) == re.getVersion()) {
                return true; // match
            }
        }
        return false; // GBA rom we don't support yet
    }

    @Override
    public void midLoadingSetUp() {
        super.midLoadingSetUp();
        isRomHack = false;
        jamboMovesetHack = false;
        if (romEntry.getRomCode().equals("BPRE") && romEntry.getVersion() == 0) {
            basicBPRE10HackSupport();
        }

        mapLoadingDone = false;

        freeAllUnusedSpace();
        
        // Having this in the constructor would be preferred, 
        // but getPaletteFilesID() depends on the romEntry, which isn't loaded then...
        this.paletteHandler = new Gen3to5PaletteHandler(random, getPaletteFilesID());
    }

    @Override
    protected void loadGameData() {
        super.loadGameData();
        loadAbilityNames();
        allowedItems = Gen3Constants.allowedItems.copy();
        nonBadItems = Gen3Constants.getNonBadItems(romEntry.getRomType()).copy();
    }

    @Override
    protected void initRomEntry() {
        for (Gen3RomEntry re : roms) {
            if (romCode(rom, re.getRomCode()) && (rom[0xBC] & 0xFF) == re.getVersion()) {
                romEntry = new Gen3RomEntry(re); // clone so we can modify
                break;
            }
        }
        addPokedexOrderToRomEntry();
        addPointerBlocksToRomEntry();
        addMoveTutorInfoToRomEntry();
        addEncounterInfoToRomEntry();
        addMapInfoToRomEntry();
    }

    private void addPokedexOrderToRomEntry() {
        List<Integer> pokedexOrderPrefixes = findMultiple(rom, Gen3Constants.pokedexOrderPointerPrefix);
        romEntry.putIntValue("PokedexOrder", readPointer(pokedexOrderPrefixes.get(1) + 16));
    }

    private void addPointerBlocksToRomEntry() {
        if (romEntry.getIntValue("HasPointerBlock1") == 1) {
            addPointerBlock1ToRomEntry();
        } else {
            int baseNomOffset = find(rom, Gen3Constants.rsPokemonNamesPointerSuffix);
            romEntry.putIntValue("PokemonNames", readPointer(baseNomOffset - 4));
        }
        if (romEntry.getIntValue("HasPointerBlock2") == 1) {
            addPointerBlock2ToRomEntry();
        }
    }

    private void addPointerBlock1ToRomEntry() {
        romEntry.putIntValue("PokemonFrontSprites", readPointer(Gen3Constants.pokemonFrontSpritesPointer));
        romEntry.putIntValue("PokemonBackSprites", readPointer(Gen3Constants.pokemonBackSpritesPointer));
        romEntry.putIntValue("PokemonNormalPalettes", readPointer(Gen3Constants.pokemonNormalPalettesPointer));
        romEntry.putIntValue("PokemonShinyPalettes", readPointer(Gen3Constants.pokemonShinyPalettesPointer));
        romEntry.putIntValue("PokemonIconSprites", readPointer(Gen3Constants.pokemonIconSpritesPointer));
        romEntry.putIntValue("PokemonIconPalettes", readPointer(Gen3Constants.pokemonIconPalettesPointer));
        romEntry.putIntValue("PokemonNames", readPointer(Gen3Constants.pokemonNamesPointer));
        romEntry.putIntValue("MoveNames", readPointer(Gen3Constants.moveNamesPointer));
        romEntry.putIntValue("DecorationNames", readPointer(Gen3Constants.decorationNamesPointer));
    }

    private void addPointerBlock2ToRomEntry() {
        romEntry.putIntValue("PokemonStats", readPointer(Gen3Constants.pokemonStatsPointer));
        romEntry.putIntValue("AbilityNames", readPointer(Gen3Constants.abilityNamesPointer));
        romEntry.putIntValue("AbilityDescriptions", readPointer(Gen3Constants.abilityDescriptionsPointer));
        romEntry.putIntValue("ItemData", readPointer(Gen3Constants.itemDataPointer));
        romEntry.putIntValue("MoveData", readPointer(Gen3Constants.moveDataPointer));
        romEntry.putIntValue("BallSpritesPointer", readPointer(Gen3Constants.ballSpritesPointer));
        romEntry.putIntValue("BallPalettesPointer", readPointer(Gen3Constants.ballPalettesPointer));
    }

    private void addMoveTutorInfoToRomEntry() {
        if (romEntry.getRomType() == Gen3Constants.RomType_Em || romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
            romEntry.putIntValue("MoveTutorCompatibility",
                    romEntry.getIntValue("MoveTutorData") + romEntry.getIntValue("MoveTutorMoves") * 2);
        }
    }

    private void addEncounterInfoToRomEntry() {
        // Get wild Pokemon offset
        int baseWPOffset = findMultiple(rom, Gen3Constants.wildPokemonPointerPrefix).get(0);
        romEntry.putIntValue("WildPokemon", readPointer(baseWPOffset + 12));
    }

    private void addMapInfoToRomEntry() {
        // map banks
        int baseMapsOffset = findMultiple(rom, Gen3Constants.mapBanksPointerPrefix).get(0);
        romEntry.putIntValue("MapHeaders", readPointer(baseMapsOffset + 12));
        this.determineMapBankSizes();

        // map labels
        String mapLabelsPointerPrefix = romEntry.getRomType() == Gen3Constants.RomType_FRLG ?
                Gen3Constants.frlgMapLabelsPointerPrefix : Gen3Constants.rseMapLabelsPointerPrefix;
        int baseMLOffset = find(rom, mapLabelsPointerPrefix);
        romEntry.putIntValue("MapLabels", readPointer(baseMLOffset + 12));
    }

    @Override
    protected void initTextTables() {
        tb = new String[256];
        d = new HashMap<>();
        loadTextTable(romEntry.getTableFile());
    }

    private void basicBPRE10HackSupport() {
        if (basicBPRE10HackDetection()) {
            this.isRomHack = true;
            // NUMBER OF POKEMON DETECTION

            // this is the most annoying bit
            // we'll try to get it from the pokemon names,
            // and sanity check it using other things
            // this of course means we can't support
            // any hack with extended length names

            int iPokemonCount = 0;
            int namesOffset = romEntry.getIntValue("PokemonNames");
            int nameLen = romEntry.getIntValue("PokemonNameLength");
            while (true) {
                int nameOffset = namesOffset + (iPokemonCount + 1) * nameLen;
                int nameStrLen = lengthOfStringAt(nameOffset);
                if (nameStrLen > 0 && nameStrLen < nameLen && rom[nameOffset] != 0) {
                    iPokemonCount++;
                } else {
                    break;
                }
            }

            // Is there an unused egg slot at the end?
            String lastName = readVariableLengthString(namesOffset + iPokemonCount * nameLen);
            if (lastName.equals("?") || lastName.equals("-")) {
                iPokemonCount--;
            }

            // Jambo's Moves Learnt table hack?
            // need to check this before using moveset pointers
            int movesetsTable;
            if (readLong(0x3EB20) == 0x47084918) {
                // Hack applied, adjust accordingly
                int firstRoutinePtr = readPointer(0x3EB84);
                movesetsTable = readPointer(firstRoutinePtr + 75);
                jamboMovesetHack = true;
            } else {
                movesetsTable = readPointer(0x3EA7C);
                jamboMovesetHack = false;
            }

            // secondary check: moveset pointers
            // if a slot has an invalid moveset pointer, it's not a real slot
            // Before that, grab the moveset table from a known pointer to it.
            romEntry.putIntValue("PokemonMovesets", movesetsTable);
            while (iPokemonCount >= 0) {
                int movesetPtr = readPointer(movesetsTable + iPokemonCount * 4);
                if (movesetPtr < 0 || movesetPtr >= rom.length) {
                    iPokemonCount--;
                } else {
                    break;
                }
            }

            // sanity check: pokedex order
            // pokedex entries have to be within 0-1023
            // even after extending the dex
            // (at least with conventional methods)
            // so if we run into an invalid one
            // then we can cut off the count
            int pdOffset = romEntry.getIntValue("PokedexOrder");
            for (int i = 1; i <= iPokemonCount; i++) {
                int pdEntry = readWord(pdOffset + (i - 1) * 2);
                if (pdEntry > 1023) {
                    iPokemonCount = i - 1;
                    break;
                }
            }

            // write new pokemon count
            romEntry.putIntValue("PokemonCount", iPokemonCount);

            // update some key offsets from known pointers
            romEntry.putIntValue("PokemonTMHMCompat", readPointer(0x43C68));
            romEntry.putIntValue("PokemonEvolutions", readPointer(0x42F6C));
            romEntry.putIntValue("MoveTutorCompatibility", readPointer(0x120C30));
            int descsTable = readPointer(0xE5440);
            romEntry.putIntValue("MoveDescriptions", descsTable);
            int trainersTable = readPointer(0xFC00);
            romEntry.putIntValue("TrainerData", trainersTable);

            // try to detect number of moves using the descriptions
            int moveCount = 0;
            while (true) {
                int descPointer = readPointer(descsTable + (moveCount) * 4);
                if (descPointer >= 0 && descPointer < rom.length) {
                    int descStrLen = lengthOfStringAt(descPointer);
                    if (descStrLen > 0 && descStrLen < 100) {
                        // okay, this does seem fine
                        moveCount++;
                        continue;
                    }
                }
                break;
            }
            romEntry.putIntValue("MoveCount", moveCount);

            // attempt to detect number of trainers using various tells
            int trainerCount = 1;
            int tEntryLen = romEntry.getIntValue("TrainerEntrySize");
            int tNameLen = romEntry.getIntValue("TrainerNameLength");
            while (true) {
                int trOffset = trainersTable + tEntryLen * trainerCount;
                int pokeDataType = rom[trOffset] & 0xFF;
                if (pokeDataType >= 4) {
                    // only allowed 0-3
                    break;
                }
                int numPokes = rom[trOffset + (tEntryLen - 8)] & 0xFF;
                if (numPokes == 0 || numPokes > 6) {
                    break;
                }
                int pointerToPokes = readPointer(trOffset + (tEntryLen - 4));
                if (pointerToPokes < 0 || pointerToPokes >= rom.length) {
                    break;
                }
                int nameLength = lengthOfStringAt(trOffset + 4);
                if (nameLength >= tNameLen) {
                    break;
                }
                // found a valid trainer entry, recognize it
                trainerCount++;
            }
            romEntry.putIntValue("TrainerCount", trainerCount);
        }

    }

    private boolean basicBPRE10HackDetection() {
        if (rom.length != Gen3Constants.size16M) {
            return true;
        }
        long csum = FileFunctions.getCRC32(rom);
        return csum != 3716707868L;
    }

    private void loadPokedexOrder() {
        int pdOffset = romEntry.getIntValue("PokedexOrder");
        int numInternalPokes = romEntry.getIntValue("PokemonCount");
        int maxPokedex = 0;
        internalToPokedex = new int[numInternalPokes + 1];
        pokedexToInternal = new int[numInternalPokes + 1];
        for (int i = 1; i <= numInternalPokes; i++) {
            int dexEntry = readWord(rom, pdOffset + (i - 1) * 2);
            if (dexEntry != 0) {
                internalToPokedex[i] = dexEntry;
                // take the first pokemon only for each dex entry
                if (pokedexToInternal[dexEntry] == 0) {
                    pokedexToInternal[dexEntry] = i;
                }
                maxPokedex = Math.max(maxPokedex, dexEntry);
            }
        }
        if (maxPokedex == Gen3Constants.unhackedMaxPokedex) {
            // see if the slots between johto and hoenn are in use
            // old rom hacks use them instead of expanding pokes
            int offs = romEntry.getIntValue("PokemonStats");
            int usedSlots = 0;
            for (int i = 0; i < Gen3Constants.unhackedMaxPokedex - Gen3Constants.unhackedRealPokedex; i++) {
                int pokeSlot = Gen3Constants.hoennPokesStart + i;
                int pokeOffs = offs + pokeSlot * Gen3Constants.baseStatsEntrySize;
                String lowerName = pokeNames[pokeSlot].toLowerCase();
                if (!this.matches(rom, pokeOffs, Gen3Constants.emptyPokemonSig) && !lowerName.contains("unused")
                        && !lowerName.equals("?") && !lowerName.equals("-")) {
                    usedSlots++;
                    pokedexToInternal[Gen3Constants.unhackedRealPokedex + usedSlots] = pokeSlot;
                    internalToPokedex[pokeSlot] = Gen3Constants.unhackedRealPokedex + usedSlots;
                } else {
                    internalToPokedex[pokeSlot] = 0;
                }
            }
            // remove the fake extra slots
            for (int i = usedSlots + 1; i <= Gen3Constants.unhackedMaxPokedex - Gen3Constants.unhackedRealPokedex; i++) {
                pokedexToInternal[Gen3Constants.unhackedRealPokedex + i] = 0;
            }
            // if any slots were used at all, this is a rom hack
            if (usedSlots > 0) {
                this.isRomHack = true;
            }
            this.pokedexCount = Gen3Constants.unhackedRealPokedex + usedSlots;
        } else {
            this.isRomHack = true;
            this.pokedexCount = maxPokedex;
        }

    }

    private void constructPokemonList() {
        if (!this.isRomHack) {
            // simple behavior: all pokes in the dex are valid
            pokemonList = Arrays.asList(pokes);
        } else {
            // only include "valid" pokes
            pokemonList = new ArrayList<>();
            pokemonList.add(null);
            for (int i = 1; i < pokes.length; i++) {
                Pokemon pk = pokes[i];
                if (pk != null) {
                    String lowerName = pk.getName().toLowerCase();
                    if (!lowerName.contains("unused") && !lowerName.equals("?")) {
                        pokemonList.add(pk);
                    }
                }
            }
        }
        numRealPokemon = pokemonList.size() - 1;

    }

    @Override
    public void loadPokemonStats() {
        loadPokemonNames();
        loadPokedexOrder();

        pokes = new Pokemon[this.pokedexCount + 1];
        int numInternalPokes = romEntry.getIntValue("PokemonCount");
        pokesInternal = new Pokemon[numInternalPokes + 1];
        int offs = romEntry.getIntValue("PokemonStats");
        for (int i = 1; i <= numInternalPokes; i++) {
            int number = internalToPokedex[i];
            Pokemon pk = new Pokemon(number);
            pk.setName(pokeNames[i]);
            if (pk.getNumber() != 0) {
                pokes[pk.getNumber()] = pk;
            }
            pokesInternal[i] = pk;
            int pkoffs = offs + i * Gen3Constants.baseStatsEntrySize;
            loadBasicPokeStats(pk, pkoffs);
        }

        // In these games, the alternate formes of Deoxys have hardcoded stats that are used 99% of the time;
        // the only times these hardcoded stats are ignored are during Link Battles. Since not many people
        // are using the randomizer to battle against others, let's just always use these stats.
        if (romEntry.getRomType() == Gen3Constants.RomType_FRLG || romEntry.getRomType() == Gen3Constants.RomType_Em) {
            String deoxysStatPrefix = romEntry.getStringValue("DeoxysStatPrefix");
            int offset = find(deoxysStatPrefix);
            if (offset > 0) {
                offset += deoxysStatPrefix.length() / 2; // because it was a prefix
                Pokemon deoxys = pokes[Species.deoxys];
                deoxys.setHp(readWord(offset));
                deoxys.setAttack(readWord(offset + 2));
                deoxys.setDefense(readWord(offset + 4));
                deoxys.setSpeed(readWord(offset + 6));
                deoxys.setSpatk(readWord(offset + 8));
                deoxys.setSpdef(readWord(offset + 10));
            }
        }

        constructPokemonList();
    }

    @Override
    public void savePokemonStats() {
        // Write pokemon names & stats
        int offs = romEntry.getIntValue("PokemonNames");
        int nameLen = romEntry.getIntValue("PokemonNameLength");
        int offs2 = romEntry.getIntValue("PokemonStats");
        int numInternalPokes = romEntry.getIntValue("PokemonCount");
        for (int i = 1; i <= numInternalPokes; i++) {
            Pokemon pk = pokesInternal[i];
            int stringOffset = offs + i * nameLen;
            writeFixedLengthString(pk.getName(), stringOffset, nameLen);
            saveBasicPokeStats(pk, offs2 + i * Gen3Constants.baseStatsEntrySize);
        }

        // Make sure to write to the hardcoded Deoxys stat location, since otherwise it will just have vanilla
        // stats no matter what settings the user selected.
        if (romEntry.getRomType() == Gen3Constants.RomType_FRLG || romEntry.getRomType() == Gen3Constants.RomType_Em) {
            String deoxysStatPrefix = romEntry.getStringValue("DeoxysStatPrefix");
            int offset = find(deoxysStatPrefix);
            if (offset > 0) {
                offset += deoxysStatPrefix.length() / 2; // because it was a prefix
                Pokemon deoxys = pokes[Species.deoxys];
                writeWord(offset, deoxys.getHp());
                writeWord(offset + 2, deoxys.getAttack());
                writeWord(offset + 4, deoxys.getDefense());
                writeWord(offset + 6, deoxys.getSpeed());
                writeWord(offset + 8, deoxys.getSpatk());
                writeWord(offset + 10, deoxys.getSpdef());
            }
        }

        writeEvolutions();
    }

    @Override
    public void loadMoves() {
        int moveCount = romEntry.getIntValue("MoveCount");
        moves = new Move[moveCount + 1];
        int offs = romEntry.getIntValue("MoveData");
        int nameoffs = romEntry.getIntValue("MoveNames");
        int namelen = romEntry.getIntValue("MoveNameLength");
        for (int i = 1; i <= moveCount; i++) {
            moves[i] = new Move();
            moves[i].name = readFixedLengthString(nameoffs + i * namelen, namelen);
            moves[i].number = i;
            moves[i].internalId = i;
            moves[i].effectIndex = rom[offs + i * 0xC] & 0xFF;
            moves[i].hitratio = ((rom[offs + i * 0xC + 3] & 0xFF));
            moves[i].power = rom[offs + i * 0xC + 1] & 0xFF;
            moves[i].pp = rom[offs + i * 0xC + 4] & 0xFF;
            moves[i].type = Gen3Constants.typeTable[rom[offs + i * 0xC + 2]];
            moves[i].target = rom[offs + i * 0xC + 6] & 0xFF;
            moves[i].category = GBConstants.physicalTypes.contains(moves[i].type) ? MoveCategory.PHYSICAL : MoveCategory.SPECIAL;
            if (moves[i].power == 0 && !GlobalConstants.noPowerNonStatusMoves.contains(i)) {
                moves[i].category = MoveCategory.STATUS;
            }
            moves[i].priority = rom[offs + i * 0xC + 7];
            int flags = rom[offs + i * 0xC + 8] & 0xFF;
            moves[i].makesContact = (flags & 1) != 0;
            moves[i].isSoundMove = Gen3Constants.soundMoves.contains(moves[i].number);

            if (i == Moves.swift) {
                perfectAccuracy = (int)moves[i].hitratio;
            }

            if (GlobalConstants.normalMultihitMoves.contains(i)) {
                moves[i].hitCount = 3;
            } else if (GlobalConstants.doubleHitMoves.contains(i)) {
                moves[i].hitCount = 2;
            } else if (i == Moves.tripleKick) {
                moves[i].hitCount = 2.71; // this assumes the first hit lands
            }

            int secondaryEffectChance = rom[offs + i * 0xC + 5] & 0xFF;
            loadStatChangesFromEffect(moves[i], secondaryEffectChance);
            loadStatusFromEffect(moves[i], secondaryEffectChance);
            loadMiscMoveInfoFromEffect(moves[i], secondaryEffectChance);
        }
    }

    private void loadStatChangesFromEffect(Move move, int secondaryEffectChance) {
        switch (move.effectIndex) {
            case Gen3Constants.noDamageAtkPlusOneEffect:
            case Gen3Constants.noDamageDefPlusOneEffect:
            case Gen3Constants.noDamageSpAtkPlusOneEffect:
            case Gen3Constants.noDamageEvasionPlusOneEffect:
            case Gen3Constants.noDamageAtkMinusOneEffect:
            case Gen3Constants.noDamageDefMinusOneEffect:
            case Gen3Constants.noDamageSpeMinusOneEffect:
            case Gen3Constants.noDamageAccuracyMinusOneEffect:
            case Gen3Constants.noDamageEvasionMinusOneEffect:
            case Gen3Constants.noDamageAtkPlusTwoEffect:
            case Gen3Constants.noDamageDefPlusTwoEffect:
            case Gen3Constants.noDamageSpePlusTwoEffect:
            case Gen3Constants.noDamageSpAtkPlusTwoEffect:
            case Gen3Constants.noDamageSpDefPlusTwoEffect:
            case Gen3Constants.noDamageAtkMinusTwoEffect:
            case Gen3Constants.noDamageDefMinusTwoEffect:
            case Gen3Constants.noDamageSpeMinusTwoEffect:
            case Gen3Constants.noDamageSpDefMinusTwoEffect:
            case Gen3Constants.minimizeEffect:
            case Gen3Constants.swaggerEffect:
            case Gen3Constants.defenseCurlEffect:
            case Gen3Constants.flatterEffect:
            case Gen3Constants.chargeEffect:
            case Gen3Constants.noDamageAtkAndDefMinusOneEffect:
            case Gen3Constants.noDamageDefAndSpDefPlusOneEffect:
            case Gen3Constants.noDamageAtkAndDefPlusOneEffect:
            case Gen3Constants.noDamageSpAtkAndSpDefPlusOneEffect:
            case Gen3Constants.noDamageAtkAndSpePlusOneEffect:
                if (move.target == 16) {
                    move.statChangeMoveType = StatChangeMoveType.NO_DAMAGE_USER;
                } else {
                    move.statChangeMoveType = StatChangeMoveType.NO_DAMAGE_TARGET;
                }
                break;

            case Gen3Constants.damageAtkMinusOneEffect:
            case Gen3Constants.damageDefMinusOneEffect:
            case Gen3Constants.damageSpeMinusOneEffect:
            case Gen3Constants.damageSpAtkMinusOneEffect:
            case Gen3Constants.damageSpDefMinusOneEffect:
            case Gen3Constants.damageAccuracyMinusOneEffect:
                move.statChangeMoveType = StatChangeMoveType.DAMAGE_TARGET;
                break;

            case Gen3Constants.damageUserDefPlusOneEffect:
            case Gen3Constants.damageUserAtkPlusOneEffect:
            case Gen3Constants.damageUserAllPlusOneEffect:
            case Gen3Constants.damageUserAtkAndDefMinusOneEffect:
            case Gen3Constants.damageUserSpAtkMinusTwoEffect:
                move.statChangeMoveType = StatChangeMoveType.DAMAGE_USER;
                break;

            default:
                // Move does not have a stat-changing effect
                return;
        }

        switch (move.effectIndex) {
            case Gen3Constants.noDamageAtkPlusOneEffect, Gen3Constants.damageUserAtkPlusOneEffect -> {
                move.statChanges[0].type = StatChangeType.ATTACK;
                move.statChanges[0].stages = 1;
            }
            case Gen3Constants.noDamageDefPlusOneEffect, Gen3Constants.damageUserDefPlusOneEffect,
                    Gen3Constants.defenseCurlEffect -> {
                move.statChanges[0].type = StatChangeType.DEFENSE;
                move.statChanges[0].stages = 1;
            }
            case Gen3Constants.noDamageSpAtkPlusOneEffect, Gen3Constants.flatterEffect -> {
                move.statChanges[0].type = StatChangeType.SPECIAL_ATTACK;
                move.statChanges[0].stages = 1;
            }
            case Gen3Constants.noDamageEvasionPlusOneEffect, Gen3Constants.minimizeEffect -> {
                move.statChanges[0].type = StatChangeType.EVASION;
                move.statChanges[0].stages = 1;
            }
            case Gen3Constants.noDamageAtkMinusOneEffect, Gen3Constants.damageAtkMinusOneEffect -> {
                move.statChanges[0].type = StatChangeType.ATTACK;
                move.statChanges[0].stages = -1;
            }
            case Gen3Constants.noDamageDefMinusOneEffect, Gen3Constants.damageDefMinusOneEffect -> {
                move.statChanges[0].type = StatChangeType.DEFENSE;
                move.statChanges[0].stages = -1;
            }
            case Gen3Constants.noDamageSpeMinusOneEffect, Gen3Constants.damageSpeMinusOneEffect -> {
                move.statChanges[0].type = StatChangeType.SPEED;
                move.statChanges[0].stages = -1;
            }
            case Gen3Constants.noDamageAccuracyMinusOneEffect, Gen3Constants.damageAccuracyMinusOneEffect -> {
                move.statChanges[0].type = StatChangeType.ACCURACY;
                move.statChanges[0].stages = -1;
            }
            case Gen3Constants.noDamageEvasionMinusOneEffect -> {
                move.statChanges[0].type = StatChangeType.EVASION;
                move.statChanges[0].stages = -1;
            }
            case Gen3Constants.noDamageAtkPlusTwoEffect, Gen3Constants.swaggerEffect -> {
                move.statChanges[0].type = StatChangeType.ATTACK;
                move.statChanges[0].stages = 2;
            }
            case Gen3Constants.noDamageDefPlusTwoEffect -> {
                move.statChanges[0].type = StatChangeType.DEFENSE;
                move.statChanges[0].stages = 2;
            }
            case Gen3Constants.noDamageSpePlusTwoEffect -> {
                move.statChanges[0].type = StatChangeType.SPEED;
                move.statChanges[0].stages = 2;
            }
            case Gen3Constants.noDamageSpAtkPlusTwoEffect -> {
                move.statChanges[0].type = StatChangeType.SPECIAL_ATTACK;
                move.statChanges[0].stages = 2;
            }
            case Gen3Constants.noDamageSpDefPlusTwoEffect -> {
                move.statChanges[0].type = StatChangeType.SPECIAL_DEFENSE;
                move.statChanges[0].stages = 2;
            }
            case Gen3Constants.noDamageAtkMinusTwoEffect -> {
                move.statChanges[0].type = StatChangeType.ATTACK;
                move.statChanges[0].stages = -2;
            }
            case Gen3Constants.noDamageDefMinusTwoEffect -> {
                move.statChanges[0].type = StatChangeType.DEFENSE;
                move.statChanges[0].stages = -2;
            }
            case Gen3Constants.noDamageSpeMinusTwoEffect -> {
                move.statChanges[0].type = StatChangeType.SPEED;
                move.statChanges[0].stages = -2;
            }
            case Gen3Constants.noDamageSpDefMinusTwoEffect -> {
                move.statChanges[0].type = StatChangeType.SPECIAL_DEFENSE;
                move.statChanges[0].stages = -2;
            }
            case Gen3Constants.damageSpAtkMinusOneEffect -> {
                move.statChanges[0].type = StatChangeType.SPECIAL_ATTACK;
                move.statChanges[0].stages = -1;
            }
            case Gen3Constants.damageSpDefMinusOneEffect -> {
                move.statChanges[0].type = StatChangeType.SPECIAL_DEFENSE;
                move.statChanges[0].stages = -1;
            }
            case Gen3Constants.damageUserAllPlusOneEffect -> {
                move.statChanges[0].type = StatChangeType.ALL;
                move.statChanges[0].stages = 1;
            }
            case Gen3Constants.chargeEffect -> {
                move.statChanges[0].type = StatChangeType.SPECIAL_DEFENSE;
                move.statChanges[0].stages = 1;
            }
            case Gen3Constants.damageUserAtkAndDefMinusOneEffect, Gen3Constants.noDamageAtkAndDefMinusOneEffect -> {
                move.statChanges[0].type = StatChangeType.ATTACK;
                move.statChanges[0].stages = -1;
                move.statChanges[1].type = StatChangeType.DEFENSE;
                move.statChanges[1].stages = -1;
            }
            case Gen3Constants.damageUserSpAtkMinusTwoEffect -> {
                move.statChanges[0].type = StatChangeType.SPECIAL_ATTACK;
                move.statChanges[0].stages = -2;
            }
            case Gen3Constants.noDamageDefAndSpDefPlusOneEffect -> {
                move.statChanges[0].type = StatChangeType.DEFENSE;
                move.statChanges[0].stages = 1;
                move.statChanges[1].type = StatChangeType.SPECIAL_DEFENSE;
                move.statChanges[1].stages = 1;
            }
            case Gen3Constants.noDamageAtkAndDefPlusOneEffect -> {
                move.statChanges[0].type = StatChangeType.ATTACK;
                move.statChanges[0].stages = 1;
                move.statChanges[1].type = StatChangeType.DEFENSE;
                move.statChanges[1].stages = 1;
            }
            case Gen3Constants.noDamageSpAtkAndSpDefPlusOneEffect -> {
                move.statChanges[0].type = StatChangeType.SPECIAL_ATTACK;
                move.statChanges[0].stages = 1;
                move.statChanges[1].type = StatChangeType.SPECIAL_DEFENSE;
                move.statChanges[1].stages = 1;
            }
            case Gen3Constants.noDamageAtkAndSpePlusOneEffect -> {
                move.statChanges[0].type = StatChangeType.ATTACK;
                move.statChanges[0].stages = 1;
                move.statChanges[1].type = StatChangeType.SPEED;
                move.statChanges[1].stages = 1;
            }
        }

        if (move.statChangeMoveType == StatChangeMoveType.DAMAGE_TARGET || move.statChangeMoveType == StatChangeMoveType.DAMAGE_USER) {
            for (int i = 0; i < move.statChanges.length; i++) {
                if (move.statChanges[i].type != StatChangeType.NONE) {
                    move.statChanges[i].percentChance = secondaryEffectChance;
                    if (move.statChanges[i].percentChance == 0.0) {
                        move.statChanges[i].percentChance = 100.0;
                    }
                }
            }
        }
    }

    private void loadStatusFromEffect(Move move, int secondaryEffectChance) {
        if (move.number == Moves.bounce) {
            // GF hardcoded this, so we have to as well
            move.statusMoveType = StatusMoveType.DAMAGE;
            move.statusType = StatusType.PARALYZE;
            move.statusPercentChance = secondaryEffectChance;
            return;
        }

        switch (move.effectIndex) {
            case Gen3Constants.noDamageSleepEffect:
            case Gen3Constants.toxicEffect:
            case Gen3Constants.noDamageConfusionEffect:
            case Gen3Constants.noDamagePoisonEffect:
            case Gen3Constants.noDamageParalyzeEffect:
            case Gen3Constants.noDamageBurnEffect:
            case Gen3Constants.swaggerEffect:
            case Gen3Constants.flatterEffect:
            case Gen3Constants.teeterDanceEffect:
                move.statusMoveType = StatusMoveType.NO_DAMAGE;
                break;

            case Gen3Constants.damagePoisonEffect:
            case Gen3Constants.damageBurnEffect:
            case Gen3Constants.damageFreezeEffect:
            case Gen3Constants.damageParalyzeEffect:
            case Gen3Constants.damageConfusionEffect:
            case Gen3Constants.twineedleEffect:
            case Gen3Constants.damageBurnAndThawUserEffect:
            case Gen3Constants.thunderEffect:
            case Gen3Constants.blazeKickEffect:
            case Gen3Constants.poisonFangEffect:
            case Gen3Constants.poisonTailEffect:
                move.statusMoveType = StatusMoveType.DAMAGE;
                break;

            default:
                // Move does not have a status effect
                return;
        }

        switch (move.effectIndex) {
            case Gen3Constants.noDamageSleepEffect -> move.statusType = StatusType.SLEEP;
            case Gen3Constants.damagePoisonEffect, Gen3Constants.noDamagePoisonEffect, Gen3Constants.twineedleEffect,
                    Gen3Constants.poisonTailEffect ->
                    move.statusType = StatusType.POISON;
            case Gen3Constants.damageBurnEffect, Gen3Constants.damageBurnAndThawUserEffect,
                    Gen3Constants.noDamageBurnEffect, Gen3Constants.blazeKickEffect ->
                    move.statusType = StatusType.BURN;
            case Gen3Constants.damageFreezeEffect -> move.statusType = StatusType.FREEZE;
            case Gen3Constants.damageParalyzeEffect, Gen3Constants.noDamageParalyzeEffect, Gen3Constants.thunderEffect ->
                    move.statusType = StatusType.PARALYZE;
            case Gen3Constants.toxicEffect, Gen3Constants.poisonFangEffect -> move.statusType = StatusType.TOXIC_POISON;
            case Gen3Constants.noDamageConfusionEffect, Gen3Constants.damageConfusionEffect,
                    Gen3Constants.swaggerEffect, Gen3Constants.flatterEffect, Gen3Constants.teeterDanceEffect ->
                    move.statusType = StatusType.CONFUSION;
        }

        if (move.statusMoveType == StatusMoveType.DAMAGE) {
            move.statusPercentChance = secondaryEffectChance;
            if (move.statusPercentChance == 0.0) {
                move.statusPercentChance = 100.0;
            }
        }
    }

    private void loadMiscMoveInfoFromEffect(Move move, int secondaryEffectChance) {
        switch (move.effectIndex) {
            case Gen3Constants.increasedCritEffect:
            case Gen3Constants.blazeKickEffect:
            case Gen3Constants.poisonTailEffect:
                move.criticalChance = CriticalChance.INCREASED;
                break;

            case Gen3Constants.futureSightAndDoomDesireEffect:
            case Gen3Constants.spitUpEffect:
                move.criticalChance = CriticalChance.NONE;

            case Gen3Constants.flinchEffect:
            case Gen3Constants.snoreEffect:
            case Gen3Constants.twisterEffect:
            case Gen3Constants.flinchWithMinimizeBonusEffect:
            case Gen3Constants.fakeOutEffect:
                move.flinchPercentChance = secondaryEffectChance;
                break;

            case Gen3Constants.damageAbsorbEffect:
            case Gen3Constants.dreamEaterEffect:
                move.absorbPercent = 50;
                break;

            case Gen3Constants.damageRecoil25PercentEffect:
                move.recoilPercent = 25;
                break;

            case Gen3Constants.damageRecoil33PercentEffect:
                move.recoilPercent = 33;
                break;

            case Gen3Constants.bindingEffect:
            case Gen3Constants.trappingEffect:
                move.isTrapMove = true;
                break;

            case Gen3Constants.razorWindEffect:
            case Gen3Constants.skullBashEffect:
            case Gen3Constants.solarbeamEffect:
            case Gen3Constants.semiInvulnerableEffect:
                move.isChargeMove = true;
                break;

            case Gen3Constants.rechargeEffect:
                move.isRechargeMove = true;
                break;

            case Gen3Constants.skyAttackEffect:
                move.criticalChance = CriticalChance.INCREASED;
                move.flinchPercentChance = secondaryEffectChance;
                move.isChargeMove = true;
                break;
        }
    }

    @Override
    public void saveMoves() {
        int moveCount = romEntry.getIntValue("MoveCount");
        int offs = romEntry.getIntValue("MoveData");
        for (int i = 1; i <= moveCount; i++) {

            int hitratio = (int) Math.round(moves[i].hitratio);
            hitratio = Math.max(hitratio, 0);
            hitratio = Math.min(hitratio, 100);

            // TODO: where does this 0xC come from?
            writeBytes(offs + i * 0xC, new byte[] { (byte) moves[i].effectIndex,
                    (byte) moves[i].power, Gen3Constants.typeToByte(moves[i].type),
                    (byte) hitratio, (byte) moves[i].pp });
        }
    }

    public List<Move> getMoves() {
        return Arrays.asList(moves);
    }

    private void loadBasicPokeStats(Pokemon pkmn, int offset) {
        pkmn.setHp(rom[offset + Gen3Constants.bsHPOffset] & 0xFF);
        pkmn.setAttack(rom[offset + Gen3Constants.bsAttackOffset] & 0xFF);
        pkmn.setDefense(rom[offset + Gen3Constants.bsDefenseOffset] & 0xFF);
        pkmn.setSpeed(rom[offset + Gen3Constants.bsSpeedOffset] & 0xFF);
        pkmn.setSpatk(rom[offset + Gen3Constants.bsSpAtkOffset] & 0xFF);
        pkmn.setSpdef(rom[offset + Gen3Constants.bsSpDefOffset] & 0xFF);
        // Type
        pkmn.setPrimaryType(Gen3Constants.typeTable[rom[offset + Gen3Constants.bsPrimaryTypeOffset] & 0xFF]);
        pkmn.setSecondaryType(Gen3Constants.typeTable[rom[offset + Gen3Constants.bsSecondaryTypeOffset] & 0xFF]);
        // Only one type?
        if (pkmn.getSecondaryType() == pkmn.getPrimaryType()) {
            pkmn.setSecondaryType(null);
        }
        pkmn.setCatchRate(rom[offset + Gen3Constants.bsCatchRateOffset] & 0xFF);
        pkmn.setGrowthCurve(ExpCurve.fromByte(rom[offset + Gen3Constants.bsGrowthCurveOffset]));
        // Abilities
        pkmn.setAbility1(rom[offset + Gen3Constants.bsAbility1Offset] & 0xFF);
        pkmn.setAbility2(rom[offset + Gen3Constants.bsAbility2Offset] & 0xFF);

        // Held Items?
        int item1 = readWord(offset + Gen3Constants.bsCommonHeldItemOffset);
        int item2 = readWord(offset + Gen3Constants.bsRareHeldItemOffset);

        if (item1 == item2) {
            // guaranteed
            pkmn.setGuaranteedHeldItem(item1);
            pkmn.setCommonHeldItem(0);
            pkmn.setRareHeldItem(0);
        } else {
            pkmn.setGuaranteedHeldItem(0);
            pkmn.setCommonHeldItem(item1);
            pkmn.setRareHeldItem(item2);
        }
        pkmn.setDarkGrassHeldItem(-1);

        pkmn.setGenderRatio(rom[offset + Gen3Constants.bsGenderRatioOffset] & 0xFF);
    }

    private void saveBasicPokeStats(Pokemon pkmn, int offset) {
        writeByte(offset + Gen3Constants.bsHPOffset, (byte) pkmn.getHp());
        writeByte(offset + Gen3Constants.bsAttackOffset, (byte) pkmn.getAttack());
        writeByte(offset + Gen3Constants.bsDefenseOffset, (byte) pkmn.getDefense());
        writeByte(offset + Gen3Constants.bsSpeedOffset, (byte) pkmn.getSpeed());
        writeByte(offset + Gen3Constants.bsSpAtkOffset, (byte) pkmn.getSpatk());
        writeByte(offset + Gen3Constants.bsSpDefOffset, (byte) pkmn.getSpdef());
        writeByte(offset + Gen3Constants.bsPrimaryTypeOffset, Gen3Constants.typeToByte(pkmn.getPrimaryType()));
        writeByte(offset + Gen3Constants.bsSecondaryTypeOffset, Gen3Constants.typeToByte(
                pkmn.getSecondaryType() == null ? pkmn.getPrimaryType() : pkmn.getSecondaryType()
        ));
        writeByte(offset + Gen3Constants.bsCatchRateOffset, (byte) pkmn.getCatchRate());
        writeByte(offset + Gen3Constants.bsGrowthCurveOffset, pkmn.getGrowthCurve().toByte());

        writeByte(offset + Gen3Constants.bsAbility1Offset, (byte) pkmn.getAbility1());
        writeByte(offset + Gen3Constants.bsAbility2Offset, (byte) (
                pkmn.getAbility2() == 0 ? pkmn.getAbility1() :
                        pkmn.getAbility2())); // required to not break evos with random ability

        // Held items
        if (pkmn.getGuaranteedHeldItem() > 0) {
            writeWord(offset + Gen3Constants.bsCommonHeldItemOffset, pkmn.getGuaranteedHeldItem());
            writeWord(offset + Gen3Constants.bsRareHeldItemOffset, pkmn.getGuaranteedHeldItem());
        } else {
            writeWord(offset + Gen3Constants.bsCommonHeldItemOffset, pkmn.getCommonHeldItem());
            writeWord(offset + Gen3Constants.bsRareHeldItemOffset, pkmn.getRareHeldItem());
        }

        writeByte(offset + Gen3Constants.bsGenderRatioOffset, (byte) pkmn.getGenderRatio());
    }

    private void loadPokemonNames() {
        int offs = romEntry.getIntValue("PokemonNames");
        int nameLen = romEntry.getIntValue("PokemonNameLength");
        int numInternalPokes = romEntry.getIntValue("PokemonCount");
        pokeNames = new String[numInternalPokes + 1];
        for (int i = 1; i <= numInternalPokes; i++) {
            pokeNames[i] = readFixedLengthString(offs + i * nameLen, nameLen);
        }
    }

    private String readString(int offset, int maxLength) {
        StringBuilder string = new StringBuilder();
        for (int c = 0; c < maxLength; c++) {
            int currChar = rom[offset + c] & 0xFF;
            if (tb[currChar] != null) {
                string.append(tb[currChar]);
            } else {
                if (currChar == Gen3Constants.textTerminator) {
                    break;
                } else if (currChar == Gen3Constants.textVariable) {
                    int nextChar = rom[offset + c + 1] & 0xFF;
                    string.append("\\v").append(String.format("%02X", nextChar));
                    c++;
                } else {
                    string.append("\\x").append(String.format("%02X", currChar));
                }
            }
        }
        return string.toString();
    }

    private byte[] translateString(String text) {
        List<Byte> data = new ArrayList<>();
        while (text.length() != 0) {
            int i = Math.max(0, 4 - text.length());
            if (text.charAt(0) == '\\' && text.charAt(1) == 'x') {
                data.add((byte) Integer.parseInt(text.substring(2, 4), 16));
                text = text.substring(4);
            } else if (text.charAt(0) == '\\' && text.charAt(1) == 'v') {
                data.add((byte) Gen3Constants.textVariable);
                data.add((byte) Integer.parseInt(text.substring(2, 4), 16));
                text = text.substring(4);
            } else {
                while (!(d.containsKey(text.substring(0, 4 - i)) || (i == 4))) {
                    i++;
                }
                if (i == 4) {
                    text = text.substring(1);
                } else {
                    data.add(d.get(text.substring(0, 4 - i)));
                    text = text.substring(4 - i);
                }
            }
        }
        byte[] ret = new byte[data.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = data.get(i);
        }
        return ret;
    }

    private String readFixedLengthString(int offset, int length) {
        return readString(offset, length);
    }

    private String readVariableLengthString(int offset) {
        return readString(offset, Integer.MAX_VALUE);
    }

    private void writeFixedLengthString(String str, int offset, int length) {
        byte[] translated = translateString(str);
        int len = Math.min(translated.length, length);
        System.arraycopy(translated, 0, rom, offset, len);
        if (len < length) {
            writeByte(offset + len, (byte) Gen3Constants.textTerminator);
            len++;
        }
        while (len < length) {
            writeByte(offset + len, (byte) 0x00);
            len++;
        }
    }

    @Deprecated
    private void writeVariableLengthString(String str, int offset) {
        System.out.println("writeVariableLengthString() is deprecated in favor of rewriteVariableLengthString(). " +
                "A variable length string should always have a pointer to it, and rewriteVariableLengthString() " +
                "handles that better.");
        byte[] translated = translateString(str);
        System.arraycopy(translated, 0, rom, offset, translated.length);
        writeByte(offset + translated.length, (byte) 0xFF);
    }

    private int lengthOfStringAt(int offset) {
        int len = 0;
        while ((rom[offset + len] & 0xFF) != 0xFF) {
            len++;
        }
        System.out.println("String at offset " + offset + " is " + len + " bytes.");
        return len - 1;
    }

    private static boolean romName(byte[] rom, String name) {
        int sigOffset = Gen3Constants.romNameOffset;
        byte[] sigBytes = name.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < sigBytes.length; i++) {
            if (rom[sigOffset + i] != sigBytes[i]) {
                return false;
            }
        }
        return true;

    }

    private static boolean romCode(byte[] rom, String codeToCheck) {
        int sigOffset = Gen3Constants.romCodeOffset;
        byte[] sigBytes = codeToCheck.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < sigBytes.length; i++) {
            if (rom[sigOffset + i] != sigBytes[i]) {
                return false;
            }
        }
        return true;

    }

    @Override
    protected int readPointer(int offset) {
        return readLong(offset) - 0x8000000;
    }

    private int readLong(int offset) {
        return (rom[offset] & 0xFF) + ((rom[offset + 1] & 0xFF) << 8) + ((rom[offset + 2] & 0xFF) << 16)
                + (((rom[offset + 3] & 0xFF)) << 24);
    }

    @Override
    protected void writePointer(int offset, int pointer) {
        writeLong(offset, pointer + 0x8000000);
    }

    private void writeLong(int offset, int value) {
        writeBytes(offset, new byte[] { (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 24) & 0xFF) });
    }

    @Override
    public List<Pokemon> getStarters() {
        List<Pokemon> starters = new ArrayList<>();
        int baseOffset = romEntry.getIntValue("StarterPokemon");
        Pokemon starter1 = pokesInternal[readWord(baseOffset)];
        Pokemon starter2;
        Pokemon starter3;
        if (romEntry.getRomType() == Gen3Constants.RomType_Ruby || romEntry.getRomType() == Gen3Constants.RomType_Sapp
                || romEntry.getRomType() == Gen3Constants.RomType_Em) {
            // do something
            starter2 = pokesInternal[readWord(baseOffset + Gen3Constants.rseStarter2Offset)];
            starter3 = pokesInternal[readWord(baseOffset + Gen3Constants.rseStarter3Offset)];
        } else {
            // do something else
            starter2 = pokesInternal[readWord(baseOffset + Gen3Constants.frlgStarter2Offset)];
            starter3 = pokesInternal[readWord(baseOffset + Gen3Constants.frlgStarter3Offset)];
        }
        starters.add(starter1);
        starters.add(starter2);
        starters.add(starter3);
        return starters;
    }

    @Override
    public boolean setStarters(List<Pokemon> newStarters) {
        if (newStarters.size() != 3) {
            return false;
        }

        // Support Deoxys/Mew starters in E/FR/LG
        attemptObedienceEvolutionPatches();
        int baseOffset = romEntry.getIntValue("StarterPokemon");

        int starter0 = pokedexToInternal[newStarters.get(0).getNumber()];
        int starter1 = pokedexToInternal[newStarters.get(1).getNumber()];
        int starter2 = pokedexToInternal[newStarters.get(2).getNumber()];
        if (romEntry.getRomType() == Gen3Constants.RomType_Ruby || romEntry.getRomType() == Gen3Constants.RomType_Sapp
                || romEntry.getRomType() == Gen3Constants.RomType_Em) {

            // US
            // order: 0, 1, 2
            writeWord(baseOffset, starter0);
            writeWord(baseOffset + Gen3Constants.rseStarter2Offset, starter1);
            writeWord(baseOffset + Gen3Constants.rseStarter3Offset, starter2);

        } else {
            // frlg:
            // order: 0, 1, 2
            writeWord(baseOffset, starter0);
            writeWord(baseOffset + Gen3Constants.frlgStarterRepeatOffset, starter1);

            writeWord(baseOffset + Gen3Constants.frlgStarter2Offset, starter1);
            writeWord(baseOffset + Gen3Constants.frlgStarter2Offset + Gen3Constants.frlgStarterRepeatOffset, starter2);

            writeWord(baseOffset + Gen3Constants.frlgStarter3Offset, starter2);
            writeWord(baseOffset + Gen3Constants.frlgStarter3Offset + Gen3Constants.frlgStarterRepeatOffset, starter0);

            if (romEntry.getRomCode().charAt(3) != 'J' && romEntry.getRomCode().charAt(3) != 'B') {
                // Update PROF. Oak's descriptions for each starter
                // First result for each STARTERNAME is the text we need
                List<Integer> bulbasaurFoundTexts = RomFunctions.search(rom, translateString(pokes[Gen3Constants.frlgBaseStarter1].getName().toUpperCase()));
                List<Integer> charmanderFoundTexts = RomFunctions.search(rom, translateString(pokes[Gen3Constants.frlgBaseStarter2].getName().toUpperCase()));
                List<Integer> squirtleFoundTexts = RomFunctions.search(rom, translateString(pokes[Gen3Constants.frlgBaseStarter3].getName().toUpperCase()));
                writeFRLGStarterText(bulbasaurFoundTexts, newStarters.get(0), "you want to go with\\nthe ");
                writeFRLGStarterText(charmanderFoundTexts, newStarters.get(1), "you’re claiming the\\n");
                writeFRLGStarterText(squirtleFoundTexts, newStarters.get(2), "you’ve decided on the\\n");
            }
        }
        return true;

    }

    @Override
    public boolean hasStarterAltFormes() {
        return false;
    }

    @Override
    public int starterCount() {
        return 3;
    }

    @Override
    public Map<Integer, StatChange> getUpdatedPokemonStats(int generation) {
        return GlobalConstants.getStatChanges(generation);
    }

    @Override
    public boolean supportsStarterHeldItems() {
        return true;
    }

    @Override
    public List<Integer> getStarterHeldItems() {
        List<Integer> sHeldItems = new ArrayList<>();
        if (romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
            // offset from normal starter offset as a word
            int baseOffset = romEntry.getIntValue("StarterPokemon");
            sHeldItems.add(readWord(baseOffset + Gen3Constants.frlgStarterItemsOffset));
        } else {
            int baseOffset = romEntry.getIntValue("StarterItems");
            int i1 = rom[baseOffset] & 0xFF;
            int i2 = rom[baseOffset + 2] & 0xFF;
            if (i2 == 0) {
                sHeldItems.add(i1);
            } else {
                sHeldItems.add(i2 + 0xFF);
            }
        }
        return sHeldItems;
    }

    @Override
    public void setStarterHeldItems(List<Integer> items) {
        if (items.size() != 1) {
            return;
        }
        int item = items.get(0);
        if (romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
            // offset from normal starter offset as a word
            int baseOffset = romEntry.getIntValue("StarterPokemon");
            writeWord(baseOffset + Gen3Constants.frlgStarterItemsOffset, item);
        } else {
            int baseOffset = romEntry.getIntValue("StarterItems");
            if (item <= 0xFF) {
                rom[baseOffset] = (byte) item;
                rom[baseOffset + 2] = 0;
            } else {
                rom[baseOffset] = (byte) 0xFF;
                rom[baseOffset + 2] = (byte) (item - 0xFF);
            }
            rom[baseOffset + 3] = Gen3Constants.gbaAddRxOpcode | Gen3Constants.gbaR2;
        }
    }

    private void writeFRLGStarterText(List<Integer> foundTexts, Pokemon pkmn, String oakText) {
        if (foundTexts.size() > 0) {
            int offset = foundTexts.get(0);
            String pokeName = pkmn.getName();
            String pokeType = pkmn.getPrimaryType() == null ? "???" : pkmn.getPrimaryType().toString();
            if (pokeType.equals("NORMAL") && pkmn.getSecondaryType() != null) {
                pokeType = pkmn.getSecondaryType().toString();
            }
            String speech = pokeName + " is your choice.\\pSo, \\v01, " + oakText + pokeType + " POKéMON " + pokeName
                    + "?";
            writeFixedLengthString(speech, offset, lengthOfStringAt(offset) + 1);
        }
    }

    @Override
    public List<EncounterSet> getEncounters(boolean useTimeOfDay) {
        if (!mapLoadingDone) {
            preprocessMaps();
            mapLoadingDone = true;
        }

        int startOffs = romEntry.getIntValue("WildPokemon");
        List<EncounterSet> encounterAreas = new ArrayList<>();
        Set<Integer> seenOffsets = new TreeSet<>();
        int offs = startOffs;
        while (true) {
            // Read pointers
            int bank = rom[offs] & 0xFF;
            int map = rom[offs + 1] & 0xFF;
            if (bank == 0xFF && map == 0xFF) {
                break;
            }

            String mapName = mapNames[bank][map];

            int grassPokes = readPointer(offs + 4);
            int waterPokes = readPointer(offs + 8);
            int treePokes = readPointer(offs + 12);
            int fishPokes = readPointer(offs + 16);

            // Add pokemanz
            if (grassPokes >= 0 && grassPokes < rom.length && rom[grassPokes] != 0
                    && !seenOffsets.contains(readPointer(grassPokes + 4))) {
                encounterAreas.add(readWildArea(grassPokes, Gen3Constants.grassSlots, mapName + " Grass/Cave"));
                seenOffsets.add(readPointer(grassPokes + 4));
            }
            if (waterPokes >= 0 && waterPokes < rom.length && rom[waterPokes] != 0
                    && !seenOffsets.contains(readPointer(waterPokes + 4))) {
                encounterAreas.add(readWildArea(waterPokes, Gen3Constants.surfingSlots, mapName + " Surfing"));
                seenOffsets.add(readPointer(waterPokes + 4));
            }
            if (treePokes >= 0 && treePokes < rom.length && rom[treePokes] != 0
                    && !seenOffsets.contains(readPointer(treePokes + 4))) {
                encounterAreas.add(readWildArea(treePokes, Gen3Constants.rockSmashSlots, mapName + " Rock Smash"));
                seenOffsets.add(readPointer(treePokes + 4));
            }
            if (fishPokes >= 0 && fishPokes < rom.length && rom[fishPokes] != 0
                    && !seenOffsets.contains(readPointer(fishPokes + 4))) {
                encounterAreas.add(readWildArea(fishPokes, Gen3Constants.fishingSlots, mapName + " Fishing"));
                seenOffsets.add(readPointer(fishPokes + 4));
            }

            offs += 20;
        }
        int[] battleTrappersBannedAreas = romEntry.getArrayValue("BattleTrappersBanned");
        if (battleTrappersBannedAreas.length > 0) {
            // Some encounter sets aren't allowed to have Pokemon
            // with Arena Trap, Shadow Tag etc.
            Set<Pokemon> battleTrappers = new HashSet<>();
            for (Pokemon pk : getPokemon()) {
                if (hasBattleTrappingAbility(pk)) {
                    battleTrappers.add(pk);
                }
            }
            for (int areaIdx : battleTrappersBannedAreas) {
                encounterAreas.get(areaIdx).bannedPokemon.addAll(battleTrappers);
            }
        }
        return encounterAreas;
    }

    private boolean hasBattleTrappingAbility(Pokemon pokemon) {
        return pokemon != null
                && (GlobalConstants.battleTrappingAbilities.contains(pokemon.getAbility1()) || GlobalConstants.battleTrappingAbilities
                        .contains(pokemon.getAbility2()));
    }

    private EncounterSet readWildArea(int offset, int numOfEntries, String setName) {
        EncounterSet thisSet = new EncounterSet();
        thisSet.rate = rom[offset];
        thisSet.displayName = setName;
        // Grab the *real* pointer to data
        int dataOffset = readPointer(offset + 4);
        // Read the entries
        for (int i = 0; i < numOfEntries; i++) {
            // min, max, species, species
            Encounter enc = new Encounter();
            enc.level = rom[dataOffset + i * 4];
            enc.maxLevel = rom[dataOffset + i * 4 + 1];
            enc.pokemon = pokesInternal[readWord(dataOffset + i * 4 + 2)];
            thisSet.encounters.add(enc);
        }
        return thisSet;
    }

    @Override
    public void setEncounters(boolean useTimeOfDay, List<EncounterSet> encounters) {
        // Support Deoxys/Mew catches in E/FR/LG
        attemptObedienceEvolutionPatches();

        int startOffs = romEntry.getIntValue("WildPokemon");
        Iterator<EncounterSet> encounterAreas = encounters.iterator();
        Set<Integer> seenOffsets = new TreeSet<>();
        int offs = startOffs;
        while (true) {
            // Read pointers
            int bank = rom[offs] & 0xFF;
            int map = rom[offs + 1] & 0xFF;
            if (bank == 0xFF && map == 0xFF) {
                break;
            }

            int grassPokes = readPointer(offs + 4);
            int waterPokes = readPointer(offs + 8);
            int treePokes = readPointer(offs + 12);
            int fishPokes = readPointer(offs + 16);

            // Add pokemanz
            if (grassPokes >= 0 && grassPokes < rom.length && rom[grassPokes] != 0
                    && !seenOffsets.contains(readPointer(grassPokes + 4))) {
                writeWildArea(grassPokes, Gen3Constants.grassSlots, encounterAreas.next());
                seenOffsets.add(readPointer(grassPokes + 4));
            }
            if (waterPokes >= 0 && waterPokes < rom.length && rom[waterPokes] != 0
                    && !seenOffsets.contains(readPointer(waterPokes + 4))) {
                writeWildArea(waterPokes, Gen3Constants.surfingSlots, encounterAreas.next());
                seenOffsets.add(readPointer(waterPokes + 4));
            }
            if (treePokes >= 0 && treePokes < rom.length && rom[treePokes] != 0
                    && !seenOffsets.contains(readPointer(treePokes + 4))) {
                writeWildArea(treePokes, Gen3Constants.rockSmashSlots, encounterAreas.next());
                seenOffsets.add(readPointer(treePokes + 4));
            }
            if (fishPokes >= 0 && fishPokes < rom.length && rom[fishPokes] != 0
                    && !seenOffsets.contains(readPointer(fishPokes + 4))) {
                writeWildArea(fishPokes, Gen3Constants.fishingSlots, encounterAreas.next());
                seenOffsets.add(readPointer(fishPokes + 4));
            }

            offs += 20;
        }
    }

    @Override
    public boolean hasWildAltFormes() {
        return false;
    }

    @Override
    public PokemonSet<Pokemon> getBannedForWildEncounters() {
        PokemonSet<Pokemon> banned = new PokemonSet<>();
        if (romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
            // Ban Unown in FRLG because the game crashes if it is encountered outside of Tanoby Ruins.
            // See GenerateWildMon in wild_encounter.c in pokefirered
            banned.add(pokes[Species.unown]);
        }
        return banned;
    }

    @Override
    public List<Trainer> getTrainers() {
        if (trainers == null) {
            throw new IllegalStateException("Trainers have not been loaded.");
        }
        return trainers;
    }

    @Override
    public void loadTrainers() {
        trainers = new ArrayList<>();
        int baseOffset = romEntry.getIntValue("TrainerData");
        int amount = romEntry.getIntValue("TrainerCount");
        int entryLen = romEntry.getIntValue("TrainerEntrySize");
        List<String> tcnames = this.getTrainerClassNames();
        for (int i = 1; i < amount; i++) {
            // Trainer entries are 40 bytes
            // Team flags; 1 byte; 0x01 = custom moves, 0x02 = held item
            // Class; 1 byte
            // Encounter Music and gender; 1 byte
            // Battle Sprite; 1 byte
            // Name; 12 bytes; 0xff terminated
            // Items; 2 bytes each, 4 item slots
            // Battle Mode; 1 byte; 0 means single, 1 means double.
            // 3 bytes not used
            // AI Flags; 1 byte
            // 3 bytes not used
            // Number of pokemon in team; 1 byte
            // 3 bytes not used
            // Pointer to pokemon; 4 bytes
            // https://github.com/pret/pokefirered/blob/3dce3407d5f9bca69d61b1cf1b314fb1e921d572/include/battle.h#L111
            int trOffset = baseOffset + i * entryLen;
            Trainer tr = new Trainer();
            tr.offset = trOffset;
            tr.index = i;
            int trainerclass = rom[trOffset + 1] & 0xFF;
            tr.trainerclass = (rom[trOffset + 2] & 0x80) > 0 ? 1 : 0;

            int pokeDataType = rom[trOffset] & 0xFF;
            int numPokes = rom[trOffset + (entryLen - 8)] & 0xFF;
            int pointerToPokes = readPointer(trOffset + (entryLen - 4));
            tr.poketype = pokeDataType;
            tr.name = this.readVariableLengthString(trOffset + 4);
            tr.fullDisplayName = tcnames.get(trainerclass) + " " + tr.name;
            // Pokemon structure data is like
            // IV IV LV SP SP
            // (HI HI)
            // (M1 M1 M2 M2 M3 M3 M4 M4)
            // IV is a "difficulty" level between 0 and 255 to represent 0 to 31 IVs.
            //     These IVs affect all attributes. For the vanilla games, the majority
            //     of trainers have 0 IVs; Elite Four members will have 31 IVs.
            // https://github.com/pret/pokeemerald/blob/6c38837b266c0dd36ccdd04559199282daa7a8a0/include/data.h#L22
            if (pokeDataType == 0) {
                // blocks of 8 bytes
                for (int poke = 0; poke < numPokes; poke++) {
                    TrainerPokemon thisPoke = new TrainerPokemon();
                    thisPoke.IVs = ((readWord(pointerToPokes + poke * 8) & 0xFF) * 31) / 255;
                    thisPoke.level = readWord(pointerToPokes + poke * 8 + 2);
                    thisPoke.pokemon = pokesInternal[readWord(pointerToPokes + poke * 8 + 4)];
                    tr.pokemon.add(thisPoke);
                }
            } else if (pokeDataType == 2) {
                // blocks of 8 bytes
                for (int poke = 0; poke < numPokes; poke++) {
                    TrainerPokemon thisPoke = new TrainerPokemon();
                    thisPoke.IVs = ((readWord(pointerToPokes + poke * 8) & 0xFF) * 31) / 255;
                    thisPoke.level = readWord(pointerToPokes + poke * 8 + 2);
                    thisPoke.pokemon = pokesInternal[readWord(pointerToPokes + poke * 8 + 4)];
                    thisPoke.heldItem = readWord(pointerToPokes + poke * 8 + 6);
                    tr.pokemon.add(thisPoke);
                }
            } else if (pokeDataType == 1) {
                // blocks of 16 bytes
                for (int poke = 0; poke < numPokes; poke++) {
                    TrainerPokemon thisPoke = new TrainerPokemon();
                    thisPoke.IVs = ((readWord(pointerToPokes + poke * 16) & 0xFF) * 31) / 255;
                    thisPoke.level = readWord(pointerToPokes + poke * 16 + 2);
                    thisPoke.pokemon = pokesInternal[readWord(pointerToPokes + poke * 16 + 4)];
                    for (int move = 0; move < 4; move++) {
                        thisPoke.moves[move] = readWord(pointerToPokes + poke * 16 + 6 + (move*2));
                    }
                    tr.pokemon.add(thisPoke);
                }
            } else if (pokeDataType == 3) {
                // blocks of 16 bytes
                for (int poke = 0; poke < numPokes; poke++) {
                    TrainerPokemon thisPoke = new TrainerPokemon();
                    thisPoke.IVs = ((readWord(pointerToPokes + poke * 16) & 0xFF) * 31) / 255;
                    thisPoke.level = readWord(pointerToPokes + poke * 16 + 2);
                    thisPoke.pokemon = pokesInternal[readWord(pointerToPokes + poke * 16 + 4)];
                    thisPoke.heldItem = readWord(pointerToPokes + poke * 16 + 6);
                    for (int move = 0; move < 4; move++) {
                        thisPoke.moves[move] = readWord(pointerToPokes + poke * 16 + 8 + (move*2));
                    }
                    tr.pokemon.add(thisPoke);
                }
            }
            trainers.add(tr);
        }

        if (romEntry.getRomType() == Gen3Constants.RomType_Em) {
            readMossdeepStevenTrainer();
        }

        if (romEntry.getRomType() == Gen3Constants.RomType_Ruby || romEntry.getRomType() == Gen3Constants.RomType_Sapp) {
            Gen3Constants.trainerTagsRS(trainers, romEntry.getRomType());
        } else if (romEntry.getRomType() == Gen3Constants.RomType_Em) {
            Gen3Constants.trainerTagsE(trainers);
            Gen3Constants.setMultiBattleStatusEm(trainers);
        } else {
            Gen3Constants.trainerTagsFRLG(trainers);
        }
    }

    /**
     * Reads the Mossdeep Steven battle/{@link Trainer} from ROM and adds it to the end of the trainers list.
     */
	private void readMossdeepStevenTrainer() {
		int mossdeepStevenOffset = romEntry.getIntValue("MossdeepStevenTeamOffset");
		Trainer mossdeepSteven = new Trainer();
		mossdeepSteven.offset = mossdeepStevenOffset;
		mossdeepSteven.index = trainers.size();
		mossdeepSteven.poketype = 1; // Custom moves, but no held items

		// This is literally how the game does it too, lol. Have to subtract one because
		// the trainers internally are one-indexed, but then trainers is zero-indexed.
		Trainer meteorFallsSteven = trainers.get(Gen3Constants.emMeteorFallsStevenIndex - 1);
		mossdeepSteven.trainerclass = meteorFallsSteven.trainerclass;
		mossdeepSteven.name = meteorFallsSteven.name;
		mossdeepSteven.fullDisplayName = meteorFallsSteven.fullDisplayName;

		for (int i = 0; i < 3; i++) {
			int currentOffset = mossdeepStevenOffset + (i * 20);
			TrainerPokemon tp = new TrainerPokemon();
			tp.pokemon = pokesInternal[readWord(currentOffset)];
			tp.IVs = rom[currentOffset + 2];
			tp.level = rom[currentOffset + 3];
			for (int move = 0; move < 4; move++) {
				tp.moves[move] = readWord(currentOffset + 12 + (move * 2));
			}
			mossdeepSteven.pokemon.add(tp);
		}

		trainers.add(mossdeepSteven);
	}

    @Override
    public List<Integer> getEvolutionItems() {
        return Gen3Constants.evolutionItems;
    }

    @Override
    public List<Integer> getXItems() {
        return Gen3Constants.xItems;
    }

    @Override
    public List<Integer> getMainPlaythroughTrainers() {
        return new ArrayList<>(); // TODO: Not implemented
    }

    @Override
    public List<Integer> getEliteFourTrainers(boolean isChallengeMode) {
        return Arrays.stream(romEntry.getArrayValue("EliteFourIndices")).boxed().collect(Collectors.toList());
    }

	@Override
	public void setTrainers(List<Trainer> trainers) {
        this.trainers = trainers;
	}

    @Override
    public void saveTrainers() {
        if (trainers == null) {
            throw new IllegalStateException("Trainers are not loaded");
        }

        int baseOffset = romEntry.getIntValue("TrainerData");
        int amount = romEntry.getIntValue("TrainerCount");
        int entryLen = romEntry.getIntValue("TrainerEntrySize");
        Iterator<Trainer> trainerIterator = trainers.iterator();

        for (int i = 1; i < amount; i++) {
            int trOffset = baseOffset + i * entryLen;
            Trainer tr = trainerIterator.next();
            System.out.println(i + "" + tr);

            // When rewriting the Pokémon data (in particular the pointer),
            // it needs to use parts of the old trainer data - thus those are overwritten
            // after
            int pokemonPointerOffset = trOffset + (entryLen - 4);
            new DataRewriter<Trainer>().rewriteData(pokemonPointerOffset, tr, this::trainerPokemonToBytes,
                    (oldDataOffset) -> readTrainerPokemonDataLength(trOffset));

            writeByte(trOffset, (byte) tr.poketype);
            writeByte(trOffset + (entryLen - 8), (byte) tr.pokemon.size());
            if (tr.forcedDoubleBattle) {
                writeByte(trOffset + (entryLen - 16), (byte) 0x01);
            }
        }

        if (romEntry.getRomType() == Gen3Constants.RomType_Em) {
            writeMossdeepStevenTrainer();
        }
    }

	private byte[] trainerPokemonToBytes(Trainer trainer) {
		int dataSize = trainer.pokemon.size() * (trainer.pokemonHaveCustomMoves() ? 16 : 8);
		byte[] pokemonData = new byte[dataSize];

		// Get current movesets in case we need to reset them for certain
		// trainer mons.
		Map<Integer, List<MoveLearnt>> movesets = this.getMovesLearnt();

		if (trainer.pokemonHaveCustomMoves()) {
			// custom moves, blocks of 16 bytes
			for (int tpIndex = 0; tpIndex < trainer.pokemon.size(); tpIndex++) {
				TrainerPokemon tp = trainer.pokemon.get(tpIndex);
				// Add 1 to offset integer division truncation
				writeWord(pokemonData, tpIndex * 16, Math.min(255, 1 + (tp.IVs * 255) / 31));
				writeWord(pokemonData, tpIndex * 16 + 2, tp.level);
				writeWord(pokemonData, tpIndex * 16 + 4, pokedexToInternal[tp.pokemon.getNumber()]);
				int movesStart;
				if (trainer.pokemonHaveItems()) {
					writeWord(pokemonData, tpIndex * 16 + 6, tp.heldItem);
					movesStart = 8;
				} else {
					movesStart = 6;
					writeWord(pokemonData, tpIndex * 16 + 14, 0);
				}
				if (tp.resetMoves) {
					int[] pokeMoves = RomFunctions.getMovesAtLevel(tp.pokemon.getNumber(), movesets, tp.level);
					for (int m = 0; m < 4; m++) {
						writeWord(pokemonData, tpIndex * 16 + movesStart + m * 2, pokeMoves[m]);
					}
				} else {
					writeWord(pokemonData, tpIndex * 16 + movesStart, tp.moves[0]);
					writeWord(pokemonData, tpIndex * 16 + movesStart + 2, tp.moves[1]);
					writeWord(pokemonData, tpIndex * 16 + movesStart + 4, tp.moves[2]);
					writeWord(pokemonData, tpIndex * 16 + movesStart + 6, tp.moves[3]);
				}
			}
		} else {
			// no moves, blocks of 8 bytes
			for (int tpIndex = 0; tpIndex < trainer.pokemon.size(); tpIndex++) {
				TrainerPokemon tp = trainer.pokemon.get(tpIndex);
				writeWord(pokemonData, tpIndex * 8, Math.min(255, 1 + (tp.IVs * 255) / 31));
				writeWord(pokemonData, tpIndex * 8 + 2, tp.level);
				writeWord(pokemonData, tpIndex * 8 + 4, pokedexToInternal[tp.pokemon.getNumber()]);
                writeWord(pokemonData, tpIndex * 8 + 6, trainer.pokemonHaveItems() ? tp.heldItem : 0);
			}
		}

		return pokemonData;
	}

	private int readTrainerPokemonDataLength(int trainerOffset) {
		int entryLen = romEntry.getIntValue("TrainerEntrySize");

		int pokeType = rom[trainerOffset] & 0xFF;
		int pokeCount = rom[trainerOffset + (entryLen - 8)] & 0xFF;
		return pokeCount * ((pokeType & 1) == 1 ? 16 : 8);
	}

    /**
     * Writes the Mossdeep Steven battle/{@link Trainer} to ROM. Assumes this is always the last trainer in
     * trainers list.
     */
    private void writeMossdeepStevenTrainer() {
        // The Mossdeep Steven trainer is special because it is *not* really a trainer to the game, just Pokemon data.
        // The randomizer surrounds this data with a Trainer object so it can be randomized.
		int mossdeepStevenOffset = romEntry.getIntValue("MossdeepStevenTeamOffset");
		Trainer mossdeepSteven = trainers.get(trainers.size() - 1);

        // The below code *could* be implemented using trainerPokemonToBytes(mossdeepSteven), but then extra
        // precautions would need to be taken so the mossdeepSteven Trainer's properties aren't changed.
        // Adding an extra Pokémon is normally fine with Trainers; but it would cause corruption with mossdeepSteven.
        // ...thus the custom implementation below.
		for (int i = 0; i < 3; i++) {
			int currentOffset = mossdeepStevenOffset + (i * 20);
			TrainerPokemon tp = mossdeepSteven.pokemon.get(i);
			writeWord(currentOffset, pokedexToInternal[tp.pokemon.getNumber()]);
			writeByte(currentOffset + 2, (byte) tp.IVs);
			writeByte(currentOffset + 3, (byte) tp.level);
			for (int move = 0; move < 4; move++) {
				writeWord(currentOffset + 12 + (move * 2), tp.moves[move]);
			}
		}
	}

    private void writeWildArea(int offset, int numOfEntries, EncounterSet encounters) {
        // Grab the *real* pointer to data
        int dataOffset = readPointer(offset + 4);
        // Write the entries
        for (int i = 0; i < numOfEntries; i++) {
            Encounter enc = encounters.encounters.get(i);
            // min, max, species, species
            int levels = enc.level | (enc.maxLevel << 8);
            writeWord(dataOffset + i * 4, levels);
            writeWord(dataOffset + i * 4 + 2, pokedexToInternal[enc.pokemon.getNumber()]);
        }
    }

    @Override
    public List<Pokemon> getPokemon() {
        return pokemonList;
    }

    @Override
    public List<Pokemon> getPokemonInclFormes() {
        return pokemonList; // No alt formes for now, should include Deoxys formes in the future
    }

    @Override
    public PokemonSet<Pokemon> getAltFormes() {
        return new PokemonSet<>();
    }

    @Override
    public List<MegaEvolution> getMegaEvolutions() {
        return new ArrayList<>();
    }

    @Override
    public Pokemon getAltFormeOfPokemon(Pokemon pk, int forme) {
        return pk;
    }

    @Override
    public PokemonSet<Pokemon> getIrregularFormes() {
        return new PokemonSet<>();
    }

    @Override
    public boolean hasFunctionalFormes() {
        return false;
    }

    @Override
	public Map<Integer, List<MoveLearnt>> getMovesLearnt() {
		Map<Integer, List<MoveLearnt>> movesets = new TreeMap<>();
		int baseOffset = romEntry.getIntValue("PokemonMovesets");
		for (int i = 1; i <= numRealPokemon; i++) {
			Pokemon pk = pokemonList.get(i);
			int pointerOffset = baseOffset + (pokedexToInternal[pk.getNumber()]) * 4;
			int movesLearntOffset = readPointer(pointerOffset);
			List<MoveLearnt> moves = readMovesLearnt(movesLearntOffset);
			movesets.put(pk.getNumber(), moves);
		}
		return movesets;
	}

	private List<MoveLearnt> readMovesLearnt(int offset) {
		List<MoveLearnt> moves = new ArrayList<>();
		if (jamboMovesetHack) {
			while ((rom[offset] & 0xFF) != 0x00 || (rom[offset + 1] & 0xFF) != 0x00
					|| (rom[offset + 2] & 0xFF) != 0xFF) {
				MoveLearnt ml = new MoveLearnt();
				ml.level = rom[offset + 2] & 0xFF;
				ml.move = readWord(offset);
				moves.add(ml);
				offset += 3;
			}
		} else {
			while ((rom[offset] & 0xFF) != 0xFF || (rom[offset + 1] & 0xFF) != 0xFF) {
				int move = (rom[offset] & 0xFF);
				int level = (rom[offset + 1] & 0xFE) >> 1;
				if ((rom[offset + 1] & 0x01) == 0x01) {
					move += 0x100;
				}
				MoveLearnt ml = new MoveLearnt();
				ml.level = level;
				ml.move = move;
				moves.add(ml);
				offset += 2;
			}
		}
		return moves;
	}

	@Override
	public void setMovesLearnt(Map<Integer, List<MoveLearnt>> movesets) {
		int baseOffset = romEntry.getIntValue("PokemonMovesets");
		for (int i = 1; i <= numRealPokemon; i++) {
			Pokemon pk = pokemonList.get(i);
			int pointerOffset = baseOffset + (pokedexToInternal[pk.getNumber()]) * 4;
			List<MoveLearnt> moves = movesets.get(pk.getNumber());
			new DataRewriter<List<MoveLearnt>>().rewriteData(pointerOffset, moves, this::movesLearntToBytes,
					this::lengthOfMovesLearntAt);
		}
	}

	private byte[] movesLearntToBytes(List<MoveLearnt> movesLearnt) {
		int entrySize = jamboMovesetHack ? 3 : 2;
		byte[] terminator = jamboMovesetHack ? Gen3Constants.jamboMovesLearntTerminator
				: Gen3Constants.vanillaMovesLearntTerminator;

		int bytesNeeded = entrySize * movesLearnt.size() + terminator.length;
		byte[] bytes = new byte[bytesNeeded];

		for (int i = 0; i < movesLearnt.size(); i++) {
			writeMoveLearnt(bytes, i * entrySize, movesLearnt.get(i));
		}
		writeBytes(bytes, bytesNeeded - terminator.length, terminator);

		return bytes;
	}

	private void writeMoveLearnt(byte[] data, int offset, MoveLearnt ml) {
		if (jamboMovesetHack) {
			writeWord(data, offset, ml.move);
			data[offset + 2] = (byte) ml.level;
		} else {
			data[offset] = (byte) (ml.move & 0xFF);
			int levelPart = (ml.level << 1) & 0xFE;
			if (ml.move > 255) {
				levelPart++;
			}
			data[offset + 1] = (byte) levelPart;
		}
	}

	/**
	 * Reads the length of a MoveLearnt-s entry in bytes, including the terminator
	 * bytes.
	 */
	private int lengthOfMovesLearntAt(int offset) {
		List<MoveLearnt> moves = readMovesLearnt(offset);
		return movesLearntToBytes(moves).length;
	}

    @Override
    public Map<Integer, List<Integer>> getEggMoves() {
        Map<Integer, List<Integer>> eggMoves = new TreeMap<>();
        int baseOffset = romEntry.getIntValue("EggMoves");
        int currentOffset = baseOffset;
        int currentSpecies = 0;
        List<Integer> currentMoves = new ArrayList<>();
        int val = FileFunctions.read2ByteInt(rom, currentOffset);

        // Check egg_moves.h in the Gen 3 decomps for more info on how this algorithm works.
        while (val != 0xFFFF) {
            if (val > 20000) {
                int species = val - 20000;
                if (currentMoves.size() > 0) {
                    eggMoves.put(internalToPokedex[currentSpecies], currentMoves);
                }
                currentSpecies = species;
                currentMoves = new ArrayList<>();
            } else {
                currentMoves.add(val);
            }
            currentOffset += 2;
            val = FileFunctions.read2ByteInt(rom, currentOffset);
        }

        // Need to make sure the last entry gets recorded too
        if (currentMoves.size() > 0) {
            eggMoves.put(internalToPokedex[currentSpecies], currentMoves);
        }
        return eggMoves;
    }

    @Override
    public void setEggMoves(Map<Integer, List<Integer>> eggMoves) {
        int baseOffset = romEntry.getIntValue("EggMoves");
        int currentOffset = baseOffset;
        for (int species : eggMoves.keySet()) {
            FileFunctions.write2ByteInt(rom, currentOffset, pokedexToInternal[species] + 20000);
            currentOffset += 2;
            for (int move : eggMoves.get(species)) {
                FileFunctions.write2ByteInt(rom, currentOffset, move);
                currentOffset += 2;
            }
        }
    }

    public static class StaticPokemon {
        private final int[] speciesOffsets;
        private final int[] levelOffsets;

        public StaticPokemon(int[] speciesOffsets, int[] levelOffsets) {
            this.speciesOffsets = speciesOffsets;
            this.levelOffsets = levelOffsets;
        }

        public Pokemon getPokemon(Gen3RomHandler parent) {
            return parent.pokesInternal[parent.readWord(speciesOffsets[0])];
        }

        public void setPokemon(Gen3RomHandler parent, Pokemon pkmn) {
            int value = parent.pokedexToInternal[pkmn.getNumber()];
            for (int offset : speciesOffsets) {
                parent.writeWord(offset, value);
            }
        }

        public int getLevel(byte[] rom, int i) {
            if (levelOffsets.length <= i) {
                return 1;
            }
            return rom[levelOffsets[i]];
        }

        public void setLevel(byte[] rom, int level, int i) {
            if (levelOffsets.length > i) { // Might not have a level entry e.g., it's an egg
                rom[levelOffsets[i]] = (byte) level;
            }
        }
    }

    @Override
    public List<StaticEncounter> getStaticPokemon() {
        List<StaticEncounter> statics = new ArrayList<>();
        List<StaticPokemon> staticsHere = romEntry.getStaticPokemon();
        int[] staticEggOffsets = romEntry.getArrayValue("StaticEggPokemonOffsets");
        for (int i = 0; i < staticsHere.size(); i++) {
            int currentOffset = i;
            StaticPokemon staticPK = staticsHere.get(i);
            StaticEncounter se = new StaticEncounter();
            se.pkmn = staticPK.getPokemon(this);
            se.level = staticPK.getLevel(rom, 0);
            se.isEgg = Arrays.stream(staticEggOffsets).anyMatch(x-> x == currentOffset);
            statics.add(se);
        }

        if (romEntry.hasTweakFile("StaticFirstBattleTweak")) {
            // Read in and randomize the static starting Poochyena/Zigzagoon fight in RSE
            int startingSpeciesOffset = romEntry.getIntValue("StaticFirstBattleSpeciesOffset");
            int species = readWord(startingSpeciesOffset);
            if (species == 0xFFFF) {
                // Patch hasn't been applied, so apply it first
                try {
                    FileFunctions.applyPatch(rom, romEntry.getTweakFile("StaticFirstBattleTweak"));
                    species = readWord(startingSpeciesOffset);
                } catch (IOException e) {
                    throw new RandomizerIOException(e);
                }
            }
            Pokemon pkmn = pokesInternal[species];
            int startingLevelOffset = romEntry.getIntValue("StaticFirstBattleLevelOffset");
            int level = rom[startingLevelOffset];
            StaticEncounter se = new StaticEncounter();
            se.pkmn = pkmn;
            se.level = level;
            statics.add(se);
        } else if (romEntry.hasTweakFile("GhostMarowakTweak")) {
            // Read in and randomize the static Ghost Marowak fight in FRLG
            int[] ghostMarowakOffsets = romEntry.getArrayValue("GhostMarowakSpeciesOffsets");
            int species = readWord(ghostMarowakOffsets[0]);
            if (species == 0xFFFF) {
                // Patch hasn't been applied, so apply it first
                try {
                    FileFunctions.applyPatch(rom, romEntry.getTweakFile("GhostMarowakTweak"));
                    species = readWord(ghostMarowakOffsets[0]);
                } catch (IOException e) {
                    throw new RandomizerIOException(e);
                }
            }
            Pokemon pkmn = pokesInternal[species];
            int[] startingLevelOffsets = romEntry.getArrayValue("GhostMarowakLevelOffsets");
            int level = rom[startingLevelOffsets[0]];
            StaticEncounter se = new StaticEncounter();
            se.pkmn = pkmn;
            se.level = level;
            statics.add(se);
        }

        try {
            getRoamers(statics);
        } catch (Exception e) {
            throw new RandomizerIOException(e);
        }

        return statics;
    }

    @Override
    public boolean setStaticPokemon(List<StaticEncounter> staticPokemon) {
        // Support Deoxys/Mew gifts/catches in E/FR/LG
        attemptObedienceEvolutionPatches();

        List<StaticPokemon> staticsHere = romEntry.getStaticPokemon();
        int roamerSize = romEntry.getRoamingPokemon().size();
        if (romEntry.getRomType() == Gen3Constants.RomType_Em) {
            // Emerald roamers are set as linkedEncounters to their respective
            // Southern Island statics and thus don't count.
            roamerSize = 0;
        }
        int hardcodedStaticSize = 0;
        if (romEntry.hasTweakFile("StaticFirstBattleTweak") || romEntry.hasTweakFile("GhostMarowakTweak")) {
            hardcodedStaticSize = 1;
        }

        if (staticPokemon.size() != staticsHere.size() + hardcodedStaticSize + roamerSize) {
            return false;
        }

        for (int i = 0; i < staticsHere.size(); i++) {
            staticsHere.get(i).setPokemon(this, staticPokemon.get(i).pkmn);
            staticsHere.get(i).setLevel(rom, staticPokemon.get(i).level, 0);
        }

        if (romEntry.hasTweakFile("StaticFirstBattleTweak")) {
            StaticEncounter startingFirstBattle = staticPokemon.get(romEntry.getIntValue("StaticFirstBattleOffset"));
            int startingSpeciesOffset = romEntry.getIntValue("StaticFirstBattleSpeciesOffset");
            writeWord(startingSpeciesOffset, pokedexToInternal[startingFirstBattle.pkmn.getNumber()]);
            int startingLevelOffset = romEntry.getIntValue("StaticFirstBattleLevelOffset");
            writeByte(startingLevelOffset, (byte) startingFirstBattle.level);
        } else if (romEntry.hasTweakFile("GhostMarowakTweak")) {
            StaticEncounter ghostMarowak = staticPokemon.get(romEntry.getIntValue("GhostMarowakOffset"));
            int[] ghostMarowakSpeciesOffsets = romEntry.getArrayValue("GhostMarowakSpeciesOffsets");
            for (int offset : ghostMarowakSpeciesOffsets) {
                writeWord(offset, pokedexToInternal[ghostMarowak.pkmn.getNumber()]);
            }
            int[] ghostMarowakLevelOffsets = romEntry.getArrayValue("GhostMarowakLevelOffsets");
            for (int offset : ghostMarowakLevelOffsets) {
                writeByte(offset, (byte) ghostMarowak.level);
            }

            // The code for creating Ghost Marowak tries to ensure the Pokemon is female. If the Pokemon
            // cannot be female (because they are always male or an indeterminate gender), then the game
            // will infinite loop trying and failing to make the Pokemon female. For Pokemon that cannot
            // be female, change the specified gender to something that actually works.
            int ghostMarowakGenderOffset = romEntry.getIntValue("GhostMarowakGenderOffset");
            if (ghostMarowak.pkmn.getGenderRatio() == 0 || ghostMarowak.pkmn.getGenderRatio() == 0xFF) {
                // 0x00 is 100% male, and 0xFF is indeterminate gender
                writeByte(ghostMarowakGenderOffset, (byte) ghostMarowak.pkmn.getGenderRatio());
            }
        }

        setRoamers(staticPokemon);
        return true;
    }

    private void getRoamers(List<StaticEncounter> statics) throws IOException {
        if (romEntry.getRomType() == Gen3Constants.RomType_Ruby) {
            int firstSpecies = readWord(rom, romEntry.getRoamingPokemon().get(0).speciesOffsets[0]);
            if (firstSpecies == 0) {
                // Before applying the patch, the first species offset will be pointing to
                // the lower bytes of 0x2000000, so when it reads a word, it will be 0.
                applyRubyRoamerPatch();
            }
            StaticPokemon roamer = romEntry.getRoamingPokemon().get(0);
            StaticEncounter se = new StaticEncounter();
            se.pkmn = roamer.getPokemon(this);
            se.level = roamer.getLevel(rom, 0);
            statics.add(se);
        } else if (romEntry.getRomType() == Gen3Constants.RomType_Sapp) {
            StaticPokemon roamer = romEntry.getRoamingPokemon().get(0);
            StaticEncounter se = new StaticEncounter();
            se.pkmn = roamer.getPokemon(this);
            se.level = roamer.getLevel(rom, 0);
            statics.add(se);
        } else if (romEntry.getRomType() == Gen3Constants.RomType_FRLG && romEntry.hasTweakFile("RoamingPokemonTweak")) {
            int firstSpecies = readWord(rom, romEntry.getRoamingPokemon().get(0).speciesOffsets[0]);
            if (firstSpecies == 0xFFFF) {
                // This means that the IPS patch hasn't been applied yet, since the first species
                // ID location is free space.
                FileFunctions.applyPatch(rom, romEntry.getTweakFile("RoamingPokemonTweak"));
            }
            for (int i = 0; i < romEntry.getRoamingPokemon().size(); i++) {
                StaticPokemon roamer = romEntry.getRoamingPokemon().get(i);
                StaticEncounter se = new StaticEncounter();
                se.pkmn = roamer.getPokemon(this);
                se.level = roamer.getLevel(rom, 0);
                statics.add(se);
            }
        } else if (romEntry.getRomType() == Gen3Constants.RomType_Em) {
            int firstSpecies = readWord(rom, romEntry.getRoamingPokemon().get(0).speciesOffsets[0]);
            if (firstSpecies >= pokesInternal.length) {
                // Before applying the patch, the first species offset is a pointer with a huge value.
                // Thus, this check is a good indicator that the patch needs to be applied.
                applyEmeraldRoamerPatch();
            }
            int[] southernIslandOffsets = romEntry.getArrayValue("StaticSouthernIslandOffsets");
            for (int i = 0; i < romEntry.getRoamingPokemon().size(); i++) {
                StaticPokemon roamer = romEntry.getRoamingPokemon().get(i);
                StaticEncounter se = new StaticEncounter();
                se.pkmn = roamer.getPokemon(this);
                se.level = roamer.getLevel(rom, 0);

                // Link each roamer to their respective Southern Island static encounter so that
                // they randomize to the same species.
                StaticEncounter southernIslandEncounter = statics.get(southernIslandOffsets[i]);
                southernIslandEncounter.linkedEncounters.add(se);
            }
        }
    }

    private void setRoamers(List<StaticEncounter> statics) {
        if (romEntry.getRomType() == Gen3Constants.RomType_Ruby || romEntry.getRomType() == Gen3Constants.RomType_Sapp) {
            StaticEncounter roamerEncounter = statics.get(statics.size() - 1);
            StaticPokemon roamer = romEntry.getRoamingPokemon().get(0);
            roamer.setPokemon(this, roamerEncounter.pkmn);
            for (int i = 0; i < roamer.levelOffsets.length; i++) {
                roamer.setLevel(rom, roamerEncounter.level, i);
            }
        } else if (romEntry.getRomType() == Gen3Constants.RomType_FRLG && romEntry.hasTweakFile("RoamingPokemonTweak")) {
            for (int i = 0; i < romEntry.getRoamingPokemon().size(); i++) {
                int offsetInStaticList = statics.size() - 3 + i;
                StaticEncounter roamerEncounter = statics.get(offsetInStaticList);
                StaticPokemon roamer = romEntry.getRoamingPokemon().get(i);
                roamer.setPokemon(this, roamerEncounter.pkmn);
                for (int j = 0; j < roamer.levelOffsets.length; j++) {
                    roamer.setLevel(rom, roamerEncounter.level, j);
                }
            }
        } else if (romEntry.getRomType() == Gen3Constants.RomType_Em) {
            int[] southernIslandOffsets = romEntry.getArrayValue("StaticSouthernIslandOffsets");
            for (int i = 0; i < romEntry.getRoamingPokemon().size(); i++) {
                StaticEncounter southernIslandEncounter = statics.get(southernIslandOffsets[i]);
                StaticEncounter roamerEncounter = southernIslandEncounter.linkedEncounters.get(0);
                StaticPokemon roamer = romEntry.getRoamingPokemon().get(i);
                roamer.setPokemon(this, roamerEncounter.pkmn);
                for (int j = 0; j < roamer.levelOffsets.length; j++) {
                    roamer.setLevel(rom, roamerEncounter.level, j);
                }
            }
        }
    }

    private void applyRubyRoamerPatch() {
        int offset = romEntry.getIntValue("FindMapsWithMonFunctionStartOffset");

        // The constant 0x2000000 is actually in the function twice, so we'll replace the first instance
        // with Latios's ID. First, change the "ldr r2, [pc, #0x68]" near the start of the function to
        // "ldr r2, [pc, #0x15C]" so it points to the second usage of 0x2000000
        writeByte(offset + 22, (byte) 0x57);

        // In the space formerly occupied by the first 0x2000000, write Latios's ID
        FileFunctions.writeFullInt(rom, offset + 128, pokedexToInternal[Species.latios]);

        // Where the original function computes Latios's ID by setting r0 to 0xCC << 1, just pc-relative
        // load our constant. We have four bytes of space to play with, and we need to make sure the offset
        // from the pc is 4-byte aligned; we need to nop for alignment and then perform the load.
        writeBytes(offset + 12, new byte[] { 0x00, 0x00, 0x1C, 0x48 });

        offset = romEntry.getIntValue("CreateInitialRoamerMonFunctionStartOffset");

        // At the very end of the function, the game pops the lr from the stack and stores it in r0, then
        // it does "bx r0" to jump back to the caller, and then it has two bytes of padding afterwards. For
        // some reason, Ruby very rarely does "pop { pc }" even though that seemingly works fine. By doing
        // that, we only need one instruction to return to the caller, giving us four bytes to write
        // Latios's species ID.
        writeBytes(offset + 182, new byte[] { 0x00, (byte) 0xBD});
        FileFunctions.writeFullInt(rom, offset + 184, pokedexToInternal[Species.latios]);

        // Now write a pc-relative load to this new species ID constant over the original move and lsl. Similar
        // to before, we need to write a nop first for alignment, then pc-relative load into r6.
        writeBytes(offset + 10, new byte[] { 0x00, 0x00, 0x2A, 0x4E });
    }

    private void applyEmeraldRoamerPatch() {
        int offset = romEntry.getIntValue("CreateInitialRoamerMonFunctionStartOffset");

        // Latias's species ID is already a pc-relative loaded constant, but Latios's isn't. We need to make
        // some room for it; the constant 0x03005D8C is actually in the function twice, so we'll replace the first
        // instance with Latios's ID. First, change the "ldr r0, [pc, #0xC]" at the start of the function to
        // "ldr r0, [pc, #0x104]", so it points to the second usage of 0x03005D8C
        writeByte(offset + 14, (byte) 0x41);

        // In the space formerly occupied by the first 0x03005D8C, write Latios's ID
        FileFunctions.writeFullInt(rom, offset + 28, pokedexToInternal[Species.latios]);

        // In the original function, we "lsl r0, r0, #0x10" then compare r0 to 0. The thing is, this left
        // shift doesn't actually matter, because 0 << 0x10 = 0, and [non-zero] << 0x10 = [non-zero].
        // Let's move the compare up to take its place and then load Latios's ID into r3 for use in another
        // branch later.
        writeBytes(offset + 8, new byte[] { 0x00, 0x28, 0x04, 0x4B });

        // Lastly, in the branch that normally does r2 = 0xCC << 0x1 to compute Latios's ID, just mov r3
        // into r2, since it was loaded with his ID with the above code.
        writeBytes(offset + 48, new byte[] { 0x1A, 0x46, 0x00, 0x00 });
    }

    @Override
    public List<Integer> getTMMoves() {
        List<Integer> tms = new ArrayList<>();
        int offset = romEntry.getIntValue("TmMoves");
        for (int i = 1; i <= Gen3Constants.tmCount; i++) {
            tms.add(readWord(offset + (i - 1) * 2));
        }
        return tms;
    }

    @Override
    public List<Integer> getHMMoves() {
        return Gen3Constants.hmMoves;
    }

	@Override
	public void setTMMoves(List<Integer> moveIndexes) {
		if (!mapLoadingDone) {
			preprocessMaps();
			mapLoadingDone = true;
		}
		writeTMMoves(moveIndexes);
		writeTMItemPalettes(moveIndexes);
		writeTMItemText(moveIndexes);
		writeTMOrMTText(moveIndexes, false);
	}

	private void writeTMMoves(List<Integer> moveIndexes) {
		int offset = romEntry.getIntValue("TmMoves");
		for (int i = 1; i <= Gen3Constants.tmCount; i++) {
			writeWord(offset + (i - 1) * 2, moveIndexes.get(i - 1));
		}
		int otherOffset = romEntry.getIntValue("TmMovesDuplicate");
		if (otherOffset > 0) {
			// Emerald/FR/LG have *two* TM tables
			System.arraycopy(rom, offset, rom, otherOffset, Gen3Constants.tmCount * 2);
		}
	}

	private void writeTMItemPalettes(List<Integer> moveIndexes) {
		int iiOffset = romEntry.getIntValue("ItemImages");
		if (iiOffset > 0) {
			int[] pals = romEntry.getArrayValue("TmPals");
			// Update the item image palettes
			// Gen3 TMs are 289-338
			for (int i = 0; i < 50; i++) {
				Move mv = moves[moveIndexes.get(i)];
				int typeID = Gen3Constants.typeToByte(mv.type);
				writePointer(iiOffset + (Gen3Constants.tmItemOffset + i) * 8 + 4, pals[typeID]);
			}
		}
	}

	private boolean writeTMItemText(List<Integer> moveIndexes) {
		// Item descriptions
		if (romEntry.getIntValue("MoveDescriptions") > 0) {
			// JP blocked for now - uses different item structure anyway
			int idOffset = romEntry.getIntValue("ItemData");
			int mdOffset = romEntry.getIntValue("MoveDescriptions");
			int entrySize = romEntry.getIntValue("ItemEntrySize");
			int limitPerLine = (romEntry.getRomType() == Gen3Constants.RomType_FRLG) ? Gen3Constants.frlgItemDescCharsPerLine
					: Gen3Constants.rseItemDescCharsPerLine;
			for (int i = 0; i < Gen3Constants.tmCount; i++) {
				int itemBaseOffset = idOffset + (i + Gen3Constants.tmItemOffset) * entrySize;
				int moveBaseOffset = mdOffset + (moveIndexes.get(i) - 1) * 4;
				int moveTextPointer = readPointer(moveBaseOffset);
				String moveDesc = readVariableLengthString(moveTextPointer);
				String newItemDesc = RomFunctions.rewriteDescriptionForNewLineSize(moveDesc, "\\n", limitPerLine, ssd);

				int itemDescPointerOffset = itemBaseOffset + Gen3Constants.itemDataDescriptionOffset;
				try {
					rewriteVariableLengthString(itemDescPointerOffset, newItemDesc);
				} catch (RandomizerIOException e) {
					String nl = System.getProperty("line.separator");
					log("Couldn't insert new item description. " + e.getMessage() + nl);
					return true;
				}
			}
		}
		return false;
	}

	private int[] searchForPointerCopies(int pointerOffset) {
		// Somewhat foolhardy, since other data around *could* coincidentally be
		// identical to the pointer, and would then be erroneously overwritten.
		byte[] searchNeedle = new byte[4];
		System.arraycopy(rom, pointerOffset, searchNeedle, 0, 4);
		// find copies within pointerSearchRadius bytes either way of actualOffset
		int minOffset = Math.max(0, pointerOffset - Gen3Constants.pointerSearchRadius);
		int maxOffset = Math.min(rom.length, pointerOffset + Gen3Constants.pointerSearchRadius);
		return RomFunctions.search(rom, minOffset, maxOffset, searchNeedle).stream().mapToInt(i -> i).toArray();
	}

    private RomFunctions.StringSizeDeterminer ssd = encodedText -> translateString(encodedText).length;

    @Override
    public int getTMCount() {
        return Gen3Constants.tmCount;
    }

    @Override
    public int getHMCount() {
        return Gen3Constants.hmCount;
    }

    @Override
    public Map<Pokemon, boolean[]> getTMHMCompatibility() {
        Map<Pokemon, boolean[]> compat = new TreeMap<>();
        int offset = romEntry.getIntValue("PokemonTMHMCompat");
        for (int i = 1; i <= numRealPokemon; i++) {
            Pokemon pkmn = pokemonList.get(i);
            int compatOffset = offset + (pokedexToInternal[pkmn.getNumber()]) * 8;
            boolean[] flags = new boolean[Gen3Constants.tmCount + Gen3Constants.hmCount + 1];
            for (int j = 0; j < 8; j++) {
                readByteIntoFlags(flags, j * 8 + 1, compatOffset + j);
            }
            compat.put(pkmn, flags);
        }
        return compat;
    }

    @Override
    public void setTMHMCompatibility(Map<Pokemon, boolean[]> compatData) {
        int offset = romEntry.getIntValue("PokemonTMHMCompat");
        for (Map.Entry<Pokemon, boolean[]> compatEntry : compatData.entrySet()) {
            Pokemon pkmn = compatEntry.getKey();
            boolean[] flags = compatEntry.getValue();
            int compatOffset = offset + (pokedexToInternal[pkmn.getNumber()]) * 8;
            for (int j = 0; j < 8; j++) {
                writeByte(compatOffset + j, getByteFromFlags(flags, j * 8 + 1));
            }
        }
    }

    @Override
    public boolean hasMoveTutors() {
        return (romEntry.getRomType() == Gen3Constants.RomType_Em || romEntry.getRomType() == Gen3Constants.RomType_FRLG);
    }

    @Override
    public List<Integer> getMoveTutorMoves() {
        if (!hasMoveTutors()) {
            return new ArrayList<>();
        }
        List<Integer> mts = new ArrayList<>();
        int moveCount = romEntry.getIntValue("MoveTutorMoves");
        int offset = romEntry.getIntValue("MoveTutorData");
        for (int i = 0; i < moveCount; i++) {
            mts.add(readWord(offset + i * 2));
        }
        return mts;
    }

	@Override
	public void setMoveTutorMoves(List<Integer> moveIndexes) {
		if (!hasMoveTutors()) {
			return;
		}
        writeMoveTutorMoves(moveIndexes);
        writeTMOrMTText(moveIndexes, true);
	}

    private void writeMoveTutorMoves(List<Integer> moveIndexes) {
        int moveCount = romEntry.getIntValue("MoveTutorMoves");
        int offset = romEntry.getIntValue("MoveTutorData");
        if (moveCount != moveIndexes.size()) {
            throw new IllegalArgumentException("Wrong amount of move tutor moves.");
        }
        for (int i = 0; i < moveCount; i++) {
            writeWord(offset + i * 2, moveIndexes.get(i));
        }
    }

    /**
     * Writes the (event?) text for TM or move tutor moves.
     * @param moveIndexes A list of move indexes
     * @param moveTutor false: TM / true: Move tutor
     */
    private void writeTMOrMTText(List<Integer> moveIndexes, boolean moveTutor) {
        String nl = System.getProperty("line.separator"); // TODO: should just "/n" do?
        String desc = moveTutor ? "Move Tutor" : "TM";
		for (TMOrMTTextEntry tte : romEntry.getTMMTTexts()) {
			if (tte.actualOffset > 0 && (tte.isMoveTutor == moveTutor)) {
				// create the new text
				int oldPointer = readPointer(tte.actualOffset);
                if (oldPointer < 0 || oldPointer >= rom.length) {
                    throw new RuntimeException(desc + " text update failed: couldn't read a " + desc
                            + " text pointer." + nl);
                }
                int moveIndex = moveIndexes.get(moveTutor ? tte.number : tte.number - 1);
                String moveName = this.moves[moveIndex].name;
				// temporarily use underscores to stop the move name being split
				String tmpMoveName = moveName.replace(' ', '_');
				String unformatted = tte.template.replace("[move]", tmpMoveName);
				String newText = RomFunctions.formatTextWithReplacements(unformatted, null, "\\n",
                        "\\l", "\\p", Gen3Constants.regularTextboxCharsPerLine, ssd);
				// get rid of the underscores
				newText = newText.replace(tmpMoveName, moveName);

				int[] secondaryPointerOffsets = searchForPointerCopies(tte.actualOffset);
				try {
					rewriteVariableLengthString(tte.actualOffset, newText, secondaryPointerOffsets);
				} catch (RandomizerIOException e) {
					log("Couldn't insert new " + desc + " text. " + e.getMessage() + nl);
					return;
				}

			}
		}
	}

    @Override
    public Map<Pokemon, boolean[]> getMoveTutorCompatibility() {
        if (!hasMoveTutors()) {
            return new TreeMap<>();
        }
        Map<Pokemon, boolean[]> compat = new TreeMap<>();
        int moveCount = romEntry.getIntValue("MoveTutorMoves");
        int offset = romEntry.getIntValue("MoveTutorCompatibility");
        int bytesRequired = ((moveCount + 7) & ~7) / 8;
        for (int i = 1; i <= numRealPokemon; i++) {
            Pokemon pkmn = pokemonList.get(i);
            int compatOffset = offset + pokedexToInternal[pkmn.getNumber()] * bytesRequired;
            boolean[] flags = new boolean[moveCount + 1];
            for (int j = 0; j < bytesRequired; j++) {
                readByteIntoFlags(flags, j * 8 + 1, compatOffset + j);
            }
            compat.put(pkmn, flags);
        }
        return compat;
    }

    @Override
    public void setMoveTutorCompatibility(Map<Pokemon, boolean[]> compatData) {
        if (!hasMoveTutors()) {
            return;
        }
        int moveCount = romEntry.getIntValue("MoveTutorMoves");
        int offset = romEntry.getIntValue("MoveTutorCompatibility");
        int bytesRequired = ((moveCount + 7) & ~7) / 8;
        for (Map.Entry<Pokemon, boolean[]> compatEntry : compatData.entrySet()) {
            Pokemon pkmn = compatEntry.getKey();
            boolean[] flags = compatEntry.getValue();
            int compatOffset = offset + pokedexToInternal[pkmn.getNumber()] * bytesRequired;
            for (int j = 0; j < bytesRequired; j++) {
                writeByte(compatOffset + j, getByteFromFlags(flags, j * 8 + 1));
            }
        }
    }

    // For dynamic offsets later
    private int find(String hexString) {
        return find(rom, hexString);
    }

    private static int find(byte[] haystack, String hexString) {
        if (hexString.length() % 2 != 0) {
            return -3; // error
        }
        byte[] searchFor = new byte[hexString.length() / 2];
        for (int i = 0; i < searchFor.length; i++) {
            searchFor[i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
        }
        List<Integer> found = RomFunctions.search(haystack, searchFor);
        if (found.size() == 0) {
            return -1; // not found
        } else if (found.size() > 1) {
            return -2; // not unique
        } else {
            return found.get(0);
        }
    }

    private List<Integer> findMultiple(String hexString) {
        return findMultiple(rom, hexString);
    }

    private static List<Integer> findMultiple(byte[] haystack, String hexString) {
        if (hexString.length() % 2 != 0) {
            return new ArrayList<>(); // error
        }
        byte[] searchFor = new byte[hexString.length() / 2];
        for (int i = 0; i < searchFor.length; i++) {
            searchFor[i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
        }
        return RomFunctions.search(haystack, searchFor);
    }

    // TODO: should this not convert to byte[] and then use writeBytes()?
    private void writeHexString(String hexString, int offset) {
        if (hexString.length() % 2 != 0) {
            return; // error
        }
        for (int i = 0; i < hexString.length() / 2; i++) {
            rom[offset + i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
        }
    }

    private void attemptObedienceEvolutionPatches() {
        if (havePatchedObedience) {
            return;
        }

        havePatchedObedience = true;
        // This routine *appears* to only exist in E/FR/LG...
        // Look for the deoxys part which is
        // MOVS R1, 0x19A
        // CMP R0, R1
        // BEQ <mew/deoxys case>
        // Hex is CD214900 8842 0FD0
        int deoxysObOffset = find(Gen3Constants.deoxysObeyCode);
        if (deoxysObOffset > 0) {
            // We found the deoxys check...
            // Replacing it with MOVS R1, 0x0 would work fine.
            // This would make it so species 0x0 (glitch only) would disobey.
            // But MOVS R1, 0x0 (the version I know) is 2-byte
            // So we just use it twice...
            // the equivalent of nop'ing the second time.
            writeBytes(deoxysObOffset, new byte[] { 0x00, Gen3Constants.gbaSetRxOpcode | Gen3Constants.gbaR1,
                    0x00, Gen3Constants.gbaSetRxOpcode | Gen3Constants.gbaR1});
            // Look for the mew check too... it's 0x16 ahead
            if (readWord(deoxysObOffset + Gen3Constants.mewObeyOffsetFromDeoxysObey) == (((Gen3Constants.gbaCmpRxOpcode | Gen3Constants.gbaR0) << 8) | (Species.mew))) {
                // Bingo, thats CMP R0, 0x97
                // change to CMP R0, 0x0
                writeWord(deoxysObOffset + Gen3Constants.mewObeyOffsetFromDeoxysObey,
                        (((Gen3Constants.gbaCmpRxOpcode | Gen3Constants.gbaR0) << 8) | (0)));
            }
        }

        // Look for evolutions too
        if (romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
            int evoJumpOffset = find(Gen3Constants.levelEvoKantoDexCheckCode);
            if (evoJumpOffset > 0) {
                // This currently compares species to 0x97 and then allows
                // evolution if it's <= that.
                // Allow it regardless by using an unconditional jump instead
                writeWord(evoJumpOffset, Gen3Constants.gbaNopOpcode);
                writeWord(evoJumpOffset + 2,
                        ((Gen3Constants.gbaUnconditionalJumpOpcode << 8) | (Gen3Constants.levelEvoKantoDexJumpAmount)));
            }

            int stoneJumpOffset = find(Gen3Constants.stoneEvoKantoDexCheckCode);
            if (stoneJumpOffset > 0) {
                // same as the above, but for stone evos
                writeWord(stoneJumpOffset, Gen3Constants.gbaNopOpcode);
                writeWord(stoneJumpOffset + 2,
                        ((Gen3Constants.gbaUnconditionalJumpOpcode << 8) | (Gen3Constants.stoneEvoKantoDexJumpAmount)));
            }
        }
    }

    private void patchForNationalDex() {
        log("--Patching for National Dex at Start of Game--");
        String nl = System.getProperty("line.separator");
        if (romEntry.getRomType() == Gen3Constants.RomType_Ruby || romEntry.getRomType() == Gen3Constants.RomType_Sapp) {
            // Find the original pokedex script
            int pkDexOffset = find(Gen3Constants.rsPokedexScriptIdentifier);
            if (pkDexOffset < 0) {
                log("Patch unsuccessful." + nl);
                return;
            }
            int textPointer = readPointer(pkDexOffset - 4);
            int realScriptLocation = pkDexOffset - 8;
            int pointerLocToScript = find(pointerToHexString(realScriptLocation));
            if (pointerLocToScript < 0) {
                log("Patch unsuccessful." + nl);
                return;
            }
            // Find free space for our new routine
            int writeSpace;
            try {
                writeSpace = findAndUnfreeSpace(44); // TODO: "44" should be a constant
            } catch (RandomizerIOException e) {
                log("Patch unsuccessful. " + e.getMessage() + nl);
                return;
            }
            writePointer(pointerLocToScript, writeSpace);
            writeHexString(Gen3Constants.rsNatDexScriptPart1, writeSpace);
            writePointer(writeSpace + 4, textPointer);
            writeHexString(Gen3Constants.rsNatDexScriptPart2, writeSpace + 8);

        } else if (romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
            // Find the original pokedex script
            int pkDexOffset = find(Gen3Constants.frlgPokedexScriptIdentifier);
            if (pkDexOffset < 0) {
                log("Patch unsuccessful." + nl);
                return;
            }
            // Find free space for our new routine
            int writeSpace;
            try {
                writeSpace = findAndUnfreeSpace(10); // TODO: "10" should be a constant
            } catch (RandomizerIOException e) {
                log("Patch unsuccessful. " + e.getMessage() + nl);
                return;
            }
            writeByte(pkDexOffset, (byte) 4); // TODO: "4" should be a constant
            writePointer(pkDexOffset + 1, writeSpace);
            writeByte(pkDexOffset + 5, (byte) 0); // NOP

            // Now write our new routine
            writeHexString(Gen3Constants.frlgNatDexScript, writeSpace);

            // Fix people using the national dex flag
            List<Integer> ndexChecks = findMultiple(Gen3Constants.frlgNatDexFlagChecker);
            for (int ndexCheckOffset : ndexChecks) {
                // change to a flag-check
                // 82C = "beaten e4/gary once"
                writeHexString(Gen3Constants.frlgE4FlagChecker, ndexCheckOffset);
            }

            // Fix oak in his lab
            int oakLabCheckOffs = find(Gen3Constants.frlgOaksLabKantoDexChecker);
            if (oakLabCheckOffs > 0) {
                // replace it
                writeHexString(Gen3Constants.frlgOaksLabFix, oakLabCheckOffs);
            }

            // Fix oak outside your house
            int oakHouseCheckOffs = find(Gen3Constants.frlgOakOutsideHouseCheck);
            if (oakHouseCheckOffs > 0) {
                // fix him to use ndex count
                writeHexString(Gen3Constants.frlgOakOutsideHouseFix, oakHouseCheckOffs);
            }

            // Fix Oak's aides so they look for your National Dex seen/caught,
            // not your Kanto Dex seen/caught
            int oakAideCheckOffs = find(Gen3Constants.frlgOakAideCheckPrefix);
            if (oakAideCheckOffs > 0) {
                oakAideCheckOffs += Gen3Constants.frlgOakAideCheckPrefix.length() / 2; // because it was a prefix
                // Change the bne instruction to an unconditional branch to always use National Dex
                writeByte(oakAideCheckOffs + 1, (byte) 0xE0);
            }
        } else {
            // Find the original pokedex script
            int pkDexOffset = find(Gen3Constants.ePokedexScriptIdentifier);
            if (pkDexOffset < 0) {
                log("Patch unsuccessful." + nl);
                return;
            }
            int textPointer = readPointer(pkDexOffset - 4);
            int realScriptLocation = pkDexOffset - 8;
            int pointerLocToScript = find(pointerToHexString(realScriptLocation));
            if (pointerLocToScript < 0) {
                log("Patch unsuccessful." + nl);
                return;
            }
            // Find free space for our new routine
            int writeSpace;
            try {
                writeSpace = findAndUnfreeSpace(27); // TODO: "27" should be a constant
            } catch (RandomizerIOException e) {
                log("Patch unsuccessful. " + e.getMessage() + nl);
                return;
            }
            writePointer(pointerLocToScript, writeSpace);
            writeHexString(Gen3Constants.eNatDexScriptPart1, writeSpace);
            writePointer(writeSpace + 4, textPointer);
            writeHexString(Gen3Constants.eNatDexScriptPart2, writeSpace + 8);
        }
        log("Patch successful!" + nl);
    }

    private String pointerToHexString(int pointer) {
        String hex = String.format("%08X", pointer + 0x08000000);
        return new String(new char[] { hex.charAt(6), hex.charAt(7), hex.charAt(4), hex.charAt(5), hex.charAt(2),
                hex.charAt(3), hex.charAt(0), hex.charAt(1) });
    }

    @Override
    public void loadEvolutions() {
        for (Pokemon pkmn : pokes) {
            if (pkmn != null) {
                pkmn.getEvolutionsFrom().clear();
                pkmn.getEvolutionsTo().clear();
            }
        }

        int baseOffset = romEntry.getIntValue("PokemonEvolutions");
        int numInternalPokes = romEntry.getIntValue("PokemonCount");
        for (int i = 1; i <= numRealPokemon; i++) {
            Pokemon pk = pokemonList.get(i);
            int idx = pokedexToInternal[pk.getNumber()];
            int evoOffset = baseOffset + (idx) * 0x28;
            for (int j = 0; j < 5; j++) {
                int method = readWord(evoOffset + j * 8);
                int evolvingTo = readWord(evoOffset + j * 8 + 4);
                if (method >= 1 && method <= Gen3Constants.evolutionMethodCount && evolvingTo >= 1
                        && evolvingTo <= numInternalPokes) {
                    int extraInfo = readWord(evoOffset + j * 8 + 2);
                    EvolutionType et = EvolutionType.fromIndex(3, method);
                    Evolution evo = new Evolution(pk, pokesInternal[evolvingTo], true, et, extraInfo);
                    if (!pk.getEvolutionsFrom().contains(evo)) {
                        pk.getEvolutionsFrom().add(evo);
                        pokesInternal[evolvingTo].getEvolutionsTo().add(evo);
                    }
                }
            }
            // Split evos shouldn't carry stats unless the evo is Nincada's
            // In that case, we should have Ninjask carry stats
            if (pk.getEvolutionsFrom().size() > 1) {
                for (Evolution e : pk.getEvolutionsFrom()) {
                    if (e.type != EvolutionType.LEVEL_CREATE_EXTRA) {
                        e.carryStats = false;
                    }
                }
            }
        }
    }

    private void writeEvolutions() {
        int baseOffset = romEntry.getIntValue("PokemonEvolutions");
        for (int i = 1; i <= numRealPokemon; i++) {
            Pokemon pk = pokemonList.get(i);
            int idx = pokedexToInternal[pk.getNumber()];
            int evoOffset = baseOffset + (idx) * 0x28;
            int evosWritten = 0;
            for (Evolution evo : pk.getEvolutionsFrom()) {
                writeWord(evoOffset, evo.type.toIndex(3));
                writeWord(evoOffset + 2, evo.extraInfo);
                writeWord(evoOffset + 4, pokedexToInternal[evo.to.getNumber()]);
                writeWord(evoOffset + 6, 0);
                evoOffset += 8;
                evosWritten++;
                if (evosWritten == 5) {
                    break;
                }
            }
            while (evosWritten < 5) {
                writeWord(evoOffset, 0);
                writeWord(evoOffset + 2, 0);
                writeWord(evoOffset + 4, 0);
                writeWord(evoOffset + 6, 0);
                evoOffset += 8;
                evosWritten++;
            }
        }
    }

    @Override
    public void removeImpossibleEvolutions(Settings settings) {
        attemptObedienceEvolutionPatches();

        // no move evos, so no need to check for those
        for (Pokemon pkmn : pokes) {
            if (pkmn != null) {
                for (Evolution evo : pkmn.getEvolutionsFrom()) {
                    // Not trades, but impossible without trading
                    if (evo.type == EvolutionType.HAPPINESS_DAY && romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
                        // happiness day change to Sun Stone
                        evo.type = EvolutionType.STONE;
                        evo.extraInfo = Gen3Items.sunStone;
                        addEvoUpdateStone(impossibleEvolutionUpdates, evo, itemNames[Gen3Items.sunStone]);
                    }
                    if (evo.type == EvolutionType.HAPPINESS_NIGHT && romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
                        // happiness night change to Moon Stone
                        evo.type = EvolutionType.STONE;
                        evo.extraInfo = Gen3Items.moonStone;
                        addEvoUpdateStone(impossibleEvolutionUpdates, evo, itemNames[Gen3Items.moonStone]);
                    }
                    if (evo.type == EvolutionType.LEVEL_HIGH_BEAUTY && romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
                        // beauty change to level 35
                        evo.type = EvolutionType.LEVEL;
                        evo.extraInfo = 35;
                        addEvoUpdateLevel(impossibleEvolutionUpdates, evo);
                    }
                    // Pure Trade
                    if (evo.type == EvolutionType.TRADE) {
                        // Haunter, Machoke, Kadabra, Graveler
                        // Make it into level 37, we're done.
                        evo.type = EvolutionType.LEVEL;
                        evo.extraInfo = 37;
                        addEvoUpdateLevel(impossibleEvolutionUpdates, evo);
                    }
                    // Trade w/ Held Item
                    if (evo.type == EvolutionType.TRADE_ITEM) {
                        if (evo.from.getNumber() == Species.poliwhirl) {
                            // Poliwhirl: Lv 37
                            evo.type = EvolutionType.LEVEL;
                            evo.extraInfo = 37;
                            addEvoUpdateLevel(impossibleEvolutionUpdates, evo);
                        } else if (evo.from.getNumber() == Species.slowpoke) {
                            // Slowpoke: Water Stone
                            evo.type = EvolutionType.STONE;
                            evo.extraInfo = Gen3Items.waterStone;
                            addEvoUpdateStone(impossibleEvolutionUpdates, evo, itemNames[Gen3Items.waterStone]);
                        } else if (evo.from.getNumber() == Species.seadra) {
                            // Seadra: Lv 40
                            evo.type = EvolutionType.LEVEL;
                            evo.extraInfo = 40;
                            addEvoUpdateLevel(impossibleEvolutionUpdates, evo);
                        } else if (evo.from.getNumber() == Species.clamperl
                                && evo.extraInfo == Gen3Items.deepSeaTooth) {
                            // Clamperl -> Huntail: Lv30
                            evo.type = EvolutionType.LEVEL;
                            evo.extraInfo = 30;
                            addEvoUpdateLevel(impossibleEvolutionUpdates, evo);
                        } else if (evo.from.getNumber() == Species.clamperl
                                && evo.extraInfo == Gen3Items.deepSeaScale) {
                            // Clamperl -> Gorebyss: Water Stone
                            evo.type = EvolutionType.STONE;
                            evo.extraInfo = Gen3Items.waterStone;
                            addEvoUpdateStone(impossibleEvolutionUpdates, evo, itemNames[Gen3Items.waterStone]);
                        } else {
                            // Onix, Scyther or Porygon: Lv30
                            evo.type = EvolutionType.LEVEL;
                            evo.extraInfo = 30;
                            addEvoUpdateLevel(impossibleEvolutionUpdates, evo);
                        }
                    }
                }
            }
        }

    }

    @Override
    public void makeEvolutionsEasier(Settings settings) {
        // Reduce the amount of happiness required to evolve.
        int offset = find(rom, Gen3Constants.friendshipValueForEvoLocator);
        if (offset > 0) {
            // Amount of required happiness for HAPPINESS evolutions.
            if (rom[offset] == (byte) 219) {
                writeByte(offset, (byte) 159);
            }
            // FRLG doesn't have code to handle time-based evolutions.
            if (romEntry.getRomType() != Gen3Constants.RomType_FRLG) {
                // Amount of required happiness for HAPPINESS_DAY evolutions.
                if (rom[offset + 38] == (byte) 219) { // TODO: 219 (or 220) should be a constant
                    writeByte(offset + 38, (byte) 159); // TODO: 159 (or 160) should be a constant
                }
                // Amount of required happiness for HAPPINESS_NIGHT evolutions.
                if (rom[offset + 66] == (byte)219) {
                    writeByte(offset + 66, (byte) 159);
                }
            }
        }
    }

    @Override
    public void removeTimeBasedEvolutions() {
        for (Pokemon pkmn : pokes) {
            if (pkmn != null) {
                for (Evolution evol : pkmn.getEvolutionsFrom()) {
                    // In Gen 3, only Eevee has a time-based evolution.
                    if (evol.type == EvolutionType.HAPPINESS_DAY) {
                        // Eevee: Make sun stone => Espeon
                        evol.type = EvolutionType.STONE;
                        evol.extraInfo = Gen3Items.sunStone;
                        addEvoUpdateStone(timeBasedEvolutionUpdates, evol, itemNames[evol.extraInfo]);
                    } else if (evol.type == EvolutionType.HAPPINESS_NIGHT) {
                        // Eevee: Make moon stone => Umbreon
                        evol.type = EvolutionType.STONE;
                        evol.extraInfo = Gen3Items.moonStone;
                        addEvoUpdateStone(timeBasedEvolutionUpdates, evol, itemNames[evol.extraInfo]);
                    }
                }
            }
        }
    }

    @Override
    public boolean hasShopRandomization() {
        return true;
    }

    @Override
    public Map<Integer, Shop> getShopItems() {
        List<String> shopNames = Gen3Constants.getShopNames(romEntry.getRomType());
        List<Integer> mainGameShops = Arrays.stream(romEntry.getArrayValue("MainGameShops")).boxed().toList();
        List<Integer> skipShops = Arrays.stream(romEntry.getArrayValue("SkipShops")).boxed().toList();
        Map<Integer, Shop> shopItemsMap = new TreeMap<>();
        int[] shopItemOffsets = romEntry.getArrayValue("ShopItemOffsets");
        for (int i = 0; i < shopItemOffsets.length; i++) {
            if (!skipShops.contains(i)) {
                int offset = shopItemOffsets[i];
                List<Integer> items = new ArrayList<>();
                int val = FileFunctions.read2ByteInt(rom, offset);
                while (val != 0x0000) {
                    items.add(val);
                    offset += 2;
                    val = FileFunctions.read2ByteInt(rom, offset);
                }
                Shop shop = new Shop();
                shop.items = items;
                shop.name = shopNames.get(i);
                shop.isMainGame = mainGameShops.contains(i);
                shopItemsMap.put(i, shop);
            }
        }
        return shopItemsMap;
    }

    @Override
    public void setShopItems(Map<Integer, Shop> shopItems) {
        int[] shopItemOffsets = romEntry.getArrayValue("ShopItemOffsets");
        for (int i = 0; i < shopItemOffsets.length; i++) {
            Shop thisShop = shopItems.get(i);
            if (thisShop != null && thisShop.items != null) {
                int offset = shopItemOffsets[i];
                for (Integer integer : thisShop.items) {
                    FileFunctions.write2ByteInt(rom, offset, integer);
                    offset += 2;
                }
            }
        }
    }

    @Override
    public void setShopPrices() {
        int itemDataOffset = romEntry.getIntValue("ItemData");
        int entrySize = romEntry.getIntValue("ItemEntrySize");
        int itemCount = romEntry.getIntValue("ItemCount");
        for (int i = 1; i < itemCount; i++) {
            int balancedPrice = Gen3Constants.balancedItemPrices.get(i) * 10;
            int offset = itemDataOffset + (i * entrySize) + 16;
            FileFunctions.write2ByteInt(rom, offset, balancedPrice);
        }
    }

    @Override
    public List<PickupItem> getPickupItems() {
        List<PickupItem> pickupItems = new ArrayList<>();
        int pickupItemCount = romEntry.getIntValue("PickupItemCount");
        int sizeOfPickupEntry = romEntry.getRomType() == Gen3Constants.RomType_Em ? 2 : 4;

        // If we haven't found the pickup table for this ROM already, find it.
        if (pickupItemsTableOffset == 0) {
            String pickupTableStartLocator = romEntry.getStringValue("PickupTableStartLocator");
            int offset = find(pickupTableStartLocator);
            if (offset > 0) {
                pickupItemsTableOffset = offset;
            }
        }

        // Assuming we've found the pickup table, extract the items out of it.
        if (pickupItemsTableOffset > 0) {
            for (int i = 0; i < pickupItemCount; i++) {
                int itemOffset = pickupItemsTableOffset + (sizeOfPickupEntry * i);
                int item = FileFunctions.read2ByteInt(rom, itemOffset);
                PickupItem pickupItem = new PickupItem(item);
                pickupItems.add(pickupItem);
            }
        }

        // Assuming we got the items from the last step, fill out the probabilities based on the game.
        if (pickupItems.size() > 0) {
            if (romEntry.getRomType() == Gen3Constants.RomType_Ruby || romEntry.getRomType() == Gen3Constants.RomType_Sapp) {
                for (int levelRange = 0; levelRange < 10; levelRange++) {
                    pickupItems.get(0).probabilities[levelRange] = 30;
                    pickupItems.get(7).probabilities[levelRange] = 5;
                    pickupItems.get(8).probabilities[levelRange] = 4;
                    pickupItems.get(9).probabilities[levelRange] = 1;
                    for (int i = 1; i < 7; i++) {
                        pickupItems.get(i).probabilities[levelRange] = 10;
                    }
                }
            } else if (romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
                for (int levelRange = 0; levelRange < 10; levelRange++) {
                    pickupItems.get(0).probabilities[levelRange] = 15;
                    for (int i = 1; i < 7; i++) {
                        pickupItems.get(i).probabilities[levelRange] = 10;
                    }
                    for (int i = 7; i < 11; i++) {
                        pickupItems.get(i).probabilities[levelRange] = 5;
                    }
                    for (int i = 11; i < 16; i++) {
                        pickupItems.get(i).probabilities[levelRange] = 1;
                    }
                }
            } else {
                for (int levelRange = 0; levelRange < 10; levelRange++) {
                    int startingCommonItemOffset = levelRange;
                    int startingRareItemOffset = 18 + levelRange;
                    pickupItems.get(startingCommonItemOffset).probabilities[levelRange] = 30;
                    for (int i = 1; i < 7; i++) {
                        pickupItems.get(startingCommonItemOffset + i).probabilities[levelRange] = 10;
                    }
                    pickupItems.get(startingCommonItemOffset + 7).probabilities[levelRange] = 4;
                    pickupItems.get(startingCommonItemOffset + 8).probabilities[levelRange] = 4;
                    pickupItems.get(startingRareItemOffset).probabilities[levelRange] = 1;
                    pickupItems.get(startingRareItemOffset + 1).probabilities[levelRange] = 1;
                }
            }
        }
        return pickupItems;
    }

    @Override
    public void setPickupItems(List<PickupItem> pickupItems) {
        int sizeOfPickupEntry = romEntry.getRomType() == Gen3Constants.RomType_Em ? 2 : 4;
        if (pickupItemsTableOffset > 0) {
            for (int i = 0; i < pickupItems.size(); i++) {
                int itemOffset = pickupItemsTableOffset + (sizeOfPickupEntry * i);
                FileFunctions.write2ByteInt(rom, itemOffset, pickupItems.get(i).item);
            }
        }
    }

    @Override
    public boolean canChangeTrainerText() {
        return true;
    }

    @Override
    public TrainerNameMode trainerNameMode() {
        return TrainerNameMode.MAX_LENGTH;
    }

    @Override
    public List<Integer> getTCNameLengthsByTrainer() {
        // not needed
        return new ArrayList<>();
    }

    @Override
    public int maxTrainerNameLength() {
        return romEntry.getIntValue("TrainerNameLength") - 1;
    }

    @Override
    public List<String> getTrainerClassNames() {
        int baseOffset = romEntry.getIntValue("TrainerClassNames");
        int amount = romEntry.getIntValue("TrainerClassCount");
        int length = romEntry.getIntValue("TrainerClassNameLength");
        List<String> trainerClassNames = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            trainerClassNames.add(readVariableLengthString(baseOffset + i * length));
        }
        return trainerClassNames;
    }

    @Override
    public void setTrainerClassNames(List<String> trainerClassNames) {
        int baseOffset = romEntry.getIntValue("TrainerClassNames");
        int amount = romEntry.getIntValue("TrainerClassCount");
        int length = romEntry.getIntValue("TrainerClassNameLength");
        Iterator<String> trainerClassNamesIterator = trainerClassNames.iterator();
        for (int i = 0; i < amount; i++) {
            writeFixedLengthString(trainerClassNamesIterator.next(), baseOffset + i * length, length);
        }
    }

    @Override
    public int maxTrainerClassNameLength() {
        return romEntry.getIntValue("TrainerClassNameLength") - 1;
    }

    @Override
    public boolean fixedTrainerClassNamesLength() {
        return false;
    }

    @Override
    public List<Integer> getDoublesTrainerClasses() {
        int[] doublesClasses = romEntry.getArrayValue("DoublesTrainerClasses");
        List<Integer> doubles = new ArrayList<>();
        for (int tClass : doublesClasses) {
            doubles.add(tClass);
        }
        return doubles;
    }

    @Override
    public boolean hasStaticAltFormes() {
        return false;
    }

    @Override
    public boolean hasMainGameLegendaries() {
        return romEntry.getArrayValue("MainGameLegendaries") != null;
    }

    @Override
    public List<Integer> getMainGameLegendaries() {
        if (this.hasMainGameLegendaries()) {
            return Arrays.stream(romEntry.getArrayValue("MainGameLegendaries")).boxed().collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public List<Integer> getSpecialMusicStatics() {
        return Arrays.stream(romEntry.getArrayValue("SpecialMusicStatics")).boxed().collect(Collectors.toList());
    }

    @Override
    public void applyCorrectStaticMusic(Map<Integer, Integer> specialMusicStaticChanges) {
        List<Integer> replaced = new ArrayList<>();
        int newIndexToMusicPoolOffset;

        if (romEntry.hasTweakFile("NewIndexToMusicTweak")) {
            try {
                FileFunctions.applyPatch(rom, romEntry.getTweakFile("NewIndexToMusicTweak"));
            } catch (IOException e) {
                throw new RandomizerIOException(e);
            }

            newIndexToMusicPoolOffset  = romEntry.getIntValue("NewIndexToMusicPoolOffset");

            if (newIndexToMusicPoolOffset > 0) {

                for (int oldStatic: specialMusicStaticChanges.keySet()) {
                    int i = newIndexToMusicPoolOffset;
                    int index = internalToPokedex[readWord(rom, i)];
                    while (index != oldStatic || replaced.contains(i)) {
                        i += 4;
                        index = internalToPokedex[readWord(rom, i)];
                    }
                    writeWord(rom, i, pokedexToInternal[specialMusicStaticChanges.get(oldStatic)]);
                    replaced.add(i);
                }
            }
        }
    }

    @Override
    public boolean hasStaticMusicFix() {
        return romEntry.hasTweakFile("NewIndexToMusicTweak");
    }

    @Override
    public List<TotemPokemon> getTotemPokemon() {
        return new ArrayList<>();
    }

    @Override
    public void setTotemPokemon(List<TotemPokemon> totemPokemon) {

    }

    @Override
    public String getDefaultExtension() {
        return "gba";
    }

    @Override
    public int abilitiesPerPokemon() {
        return 2;
    }

    @Override
    public int highestAbilityIndex() {
        return Gen3Constants.highestAbilityIndex;
    }

    private void loadAbilityNames() {
        int nameoffs = romEntry.getIntValue("AbilityNames");
        int namelen = romEntry.getIntValue("AbilityNameLength");
        abilityNames = new String[Gen3Constants.highestAbilityIndex + 1];
        for (int i = 0; i <= Gen3Constants.highestAbilityIndex; i++) {
            abilityNames[i] = readFixedLengthString(nameoffs + namelen * i, namelen);
        }
    }

    @Override
    public String abilityName(int number) {
        return abilityNames[number];
    }

    @Override
    public Map<Integer, List<Integer>> getAbilityVariations() {
        return Gen3Constants.abilityVariations;
    }

    @Override
    public List<Integer> getUselessAbilities() {
        return new ArrayList<>(Gen3Constants.uselessAbilities);
    }

    @Override
    public int getAbilityForTrainerPokemon(TrainerPokemon tp) {
        // In Gen 3, Trainer Pokemon *always* use the first Ability, no matter what
        return tp.pokemon.getAbility1();
    }

    @Override
    public boolean hasMegaEvolutions() {
        return false;
    }

    @Override
    public int internalStringLength(String string) {
        return translateString(string).length;
    }

    @Override
    public void randomizeIntroPokemon() {
        // FRLG
        if (romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
            // intro sprites : first 255 only due to size
            Pokemon introPk = randomPokemonLimited(255, false);
            if (introPk == null) {
                return;
            }
            int introPokemon = pokedexToInternal[introPk.getNumber()];
            int frontSprites = romEntry.getIntValue("FrontSprites");
            int palettes = romEntry.getIntValue("PokemonPalettes");

            writeByte(romEntry.getIntValue("IntroCryOffset"), (byte) introPokemon);
            writeByte(romEntry.getIntValue("IntroOtherOffset"), (byte) introPokemon);

            int spriteBase = romEntry.getIntValue("IntroSpriteOffset");
            writePointer(spriteBase, frontSprites + introPokemon * 8);
            writePointer(spriteBase + 4, palettes + introPokemon * 8);
        } else if (romEntry.getRomType() == Gen3Constants.RomType_Ruby || romEntry.getRomType() == Gen3Constants.RomType_Sapp) {
            // intro sprites : any pokemon in the range 0-510 except bulbasaur
            int introPokemon = pokedexToInternal[randomPokemon().getNumber()];
            while (introPokemon == 1 || introPokemon > 510) {
                introPokemon = pokedexToInternal[randomPokemon().getNumber()];
            }
            int frontSprites = romEntry.getIntValue("PokemonFrontSprites");
            int palettes = romEntry.getIntValue("PokemonNormalPalettes");
            int cryCommand = romEntry.getIntValue("IntroCryOffset");
            int otherCommand = romEntry.getIntValue("IntroOtherOffset");

            if (introPokemon > 255) { // TODO: this pattern is recurring, maybe extractable into a method?
                rom[cryCommand] = (byte) 0xFF;
                rom[cryCommand + 1] = Gen3Constants.gbaSetRxOpcode | Gen3Constants.gbaR0;

                rom[cryCommand + 2] = (byte) (introPokemon - 0xFF);
                rom[cryCommand + 3] = Gen3Constants.gbaAddRxOpcode | Gen3Constants.gbaR0;

                rom[otherCommand] = (byte) 0xFF;
                rom[otherCommand + 1] = Gen3Constants.gbaSetRxOpcode | Gen3Constants.gbaR4;

                rom[otherCommand + 2] = (byte) (introPokemon - 0xFF);
                rom[otherCommand + 3] = Gen3Constants.gbaAddRxOpcode | Gen3Constants.gbaR4;
            } else {
                rom[cryCommand] = (byte) introPokemon;
                rom[cryCommand + 1] = Gen3Constants.gbaSetRxOpcode | Gen3Constants.gbaR0;

                writeWord(cryCommand + 2, Gen3Constants.gbaNopOpcode);

                rom[otherCommand] = (byte) introPokemon;
                rom[otherCommand + 1] = Gen3Constants.gbaSetRxOpcode | Gen3Constants.gbaR4;

                writeWord(otherCommand + 2, Gen3Constants.gbaNopOpcode);
            }

            writePointer(romEntry.getIntValue("IntroSpriteOffset"), frontSprites + introPokemon * 8);
            writePointer(romEntry.getIntValue("IntroPaletteOffset"), palettes + introPokemon * 8);
        } else {
            // Emerald, intro sprite: any Pokemon.
            int introPokemon = pokedexToInternal[randomPokemon().getNumber()];
            writeWord(romEntry.getIntValue("IntroSpriteOffset"), introPokemon);
            writeWord(romEntry.getIntValue("IntroCryOffset"), introPokemon);
        }

    }

    private Pokemon randomPokemonLimited(int maxValue, boolean blockNonMales) {
        checkPokemonRestrictions();
        List<Pokemon> validPokemon = new ArrayList<>();
        for (Pokemon pk : this.restrictedPokemon) {
            if (pokedexToInternal[pk.getNumber()] <= maxValue && (!blockNonMales || pk.getGenderRatio() <= 0xFD)) {
                validPokemon.add(pk);
            }
        }
        if (validPokemon.size() == 0) {
            return null;
        } else {
            return validPokemon.get(random.nextInt(validPokemon.size()));
        }
    }

    private void determineMapBankSizes() {
        int mbpsOffset = romEntry.getIntValue("MapHeaders");
        List<Integer> mapBankOffsets = new ArrayList<>();

        int offset = mbpsOffset;

        // find map banks
        while (true) {
            boolean valid = true;
            for (int mbOffset : mapBankOffsets) {
                if (mbpsOffset < mbOffset && offset >= mbOffset) {
                    valid = false;
                    break;
                }
            }
            if (!valid) {
                break;
            }
            int newMBOffset = readPointer(offset);
            if (newMBOffset < 0 || newMBOffset >= rom.length) {
                break;
            }
            mapBankOffsets.add(newMBOffset);
            offset += 4;
        }
        int bankCount = mapBankOffsets.size();
        int[] bankMapCounts = new int[bankCount];
        for (int bank = 0; bank < bankCount; bank++) {
            int baseBankOffset = mapBankOffsets.get(bank);
            int count = 0;
            offset = baseBankOffset;
            while (true) {
                boolean valid = true;
                for (int mbOffset : mapBankOffsets) {
                    if (baseBankOffset < mbOffset && offset >= mbOffset) {
                        valid = false;
                        break;
                    }
                }
                if (!valid) {
                    break;
                }
                if (baseBankOffset < mbpsOffset && offset >= mbpsOffset) {
                    break;
                }
                int newMapOffset = readPointer(offset);
                if (newMapOffset < 0 || newMapOffset >= rom.length) {
                    break;
                }
                count++;
                offset += 4;
            }
            bankMapCounts[bank] = count;
        }

        romEntry.putIntValue("MapBankCount", bankCount);
        romEntry.putArrayValue("MapBankSizes", bankMapCounts);
    }

    private void preprocessMaps() {
        itemOffs = new ArrayList<>();
        int bankCount = romEntry.getIntValue("MapBankCount");
        int[] bankMapCounts = romEntry.getArrayValue("MapBankSizes");
        int itemBall = romEntry.getIntValue("ItemBallPic");
        mapNames = new String[bankCount][];
        int mbpsOffset = romEntry.getIntValue("MapHeaders");
        int mapLabels = romEntry.getIntValue("MapLabels");
        Map<Integer, String> mapLabelsM = new HashMap<>();
        for (int bank = 0; bank < bankCount; bank++) {
            int bankOffset = readPointer(mbpsOffset + bank * 4);
            mapNames[bank] = new String[bankMapCounts[bank]];
            for (int map = 0; map < bankMapCounts[bank]; map++) {
                int mhOffset = readPointer(bankOffset + map * 4);

                // map name
                int mapLabel = rom[mhOffset + 0x14] & 0xFF;
                if (mapLabelsM.containsKey(mapLabel)) {
                    mapNames[bank][map] = mapLabelsM.get(mapLabel);
                } else {
                    if (romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
                        mapNames[bank][map] = readVariableLengthString(readPointer(mapLabels
                                + (mapLabel - Gen3Constants.frlgMapLabelsStart) * 4));
                    } else {
                        mapNames[bank][map] = readVariableLengthString(readPointer(mapLabels + mapLabel * 8 + 4));
                    }
                    mapLabelsM.put(mapLabel, mapNames[bank][map]);
                }

                // events
                int eventOffset = readPointer(mhOffset + 4);
                if (eventOffset >= 0 && eventOffset < rom.length) {

                    int pCount = rom[eventOffset] & 0xFF;
                    int spCount = rom[eventOffset + 3] & 0xFF;

                    if (pCount > 0) {
                        int peopleOffset = readPointer(eventOffset + 4);
                        for (int p = 0; p < pCount; p++) {
                            int pSprite = rom[peopleOffset + p * 24 + 1];
                            if (pSprite == itemBall && readPointer(peopleOffset + p * 24 + 16) >= 0) {
                                // Get script and look inside
                                int scriptOffset = readPointer(peopleOffset + p * 24 + 16);
                                if (rom[scriptOffset] == 0x1A && rom[scriptOffset + 1] == 0x00
                                        && (rom[scriptOffset + 2] & 0xFF) == 0x80 && rom[scriptOffset + 5] == 0x1A
                                        && rom[scriptOffset + 6] == 0x01 && (rom[scriptOffset + 7] & 0xFF) == 0x80
                                        && rom[scriptOffset + 10] == 0x09
                                        && (rom[scriptOffset + 11] == 0x00 || rom[scriptOffset + 11] == 0x01)) {
                                    // item ball script
                                    itemOffs.add(scriptOffset + 3);
                                }
                            }
                        }
                        // TM Text?
                        for (TMOrMTTextEntry tte : romEntry.getTMMTTexts()) {
                            if (tte.mapBank == bank && tte.mapNumber == map) {
                                // process this one
                                int scriptOffset = readPointer(peopleOffset + (tte.personNum - 1) * 24 + 16);
                                if (scriptOffset >= 0) {
                                    if (romEntry.getRomType() == Gen3Constants.RomType_FRLG && tte.isMoveTutor
                                            && (tte.number == 5 || (tte.number >= 8 && tte.number <= 11))) {
                                        scriptOffset = readPointer(scriptOffset + 1);
                                    } else if (romEntry.getRomType() == Gen3Constants.RomType_FRLG && tte.isMoveTutor
                                            && tte.number == 7) {
                                        scriptOffset = readPointer(scriptOffset + 0x1F);
                                    }
                                    int lookAt = scriptOffset + tte.offsetInScript;
                                    // make sure this actually looks like a text
                                    // pointer
                                    if (lookAt >= 0 && lookAt < rom.length - 2) {
                                        if (rom[lookAt + 3] == 0x08 || rom[lookAt + 3] == 0x09) {
                                            // okay, it passes the basic test
                                            tte.actualOffset = lookAt;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (spCount > 0) {
                        int signpostsOffset = readPointer(eventOffset + 16);
                        for (int sp = 0; sp < spCount; sp++) {
                            int spType = rom[signpostsOffset + sp * 12 + 5];
                            if (spType >= 5 && spType <= 7) {
                                // hidden item
                                int itemHere = readWord(signpostsOffset + sp * 12 + 8);
                                if (itemHere != 0) {
                                    // itemid 0 is coins
                                    itemOffs.add(signpostsOffset + sp * 12 + 8);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public ItemList getAllowedItems() {
        return allowedItems;
    }

    @Override
    public ItemList getNonBadItems() {
        return nonBadItems;
    }

    @Override
    public List<Integer> getUniqueNoSellItems() {
        return new ArrayList<>();
    }

    @Override
    public List<Integer> getRegularShopItems() {
        return Gen3Constants.regularShopItems;
    }

    @Override
    public List<Integer> getOPShopItems() {
        return Gen3Constants.opShopItems;
    }

    @Override
    public void loadItemNames() {
        int nameoffs = romEntry.getIntValue("ItemData");
        int structlen = romEntry.getIntValue("ItemEntrySize");
        int maxcount = romEntry.getIntValue("ItemCount");
        itemNames = new String[maxcount + 1];
        for (int i = 0; i <= maxcount; i++) {
            itemNames[i] = readVariableLengthString(nameoffs + structlen * i);
        }
    }

    @Override
    public String[] getItemNames() {
        return itemNames;
    }

    @Override
    public List<Integer> getRequiredFieldTMs() {
        if (romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
            return Gen3Constants.frlgRequiredFieldTMs;
        } else if (romEntry.getRomType() == Gen3Constants.RomType_Ruby || romEntry.getRomType() == Gen3Constants.RomType_Sapp) {
            return Gen3Constants.rsRequiredFieldTMs;
        } else {
            // emerald has a few TMs from pickup
            return Gen3Constants.eRequiredFieldTMs;
        }
    }

    @Override
    public List<Integer> getCurrentFieldTMs() {
        if (!mapLoadingDone) {
            preprocessMaps();
            mapLoadingDone = true;
        }
        List<Integer> fieldTMs = new ArrayList<>();

        for (int offset : itemOffs) {
            int itemHere = readWord(offset);
            if (Gen3Constants.allowedItems.isTM(itemHere)) {
                int thisTM = itemHere - Gen3Constants.tmItemOffset + 1;
                // hack for repeat TMs
                if (!fieldTMs.contains(thisTM)) {
                    fieldTMs.add(thisTM);
                }
            }
        }
        return fieldTMs;
    }

    @Override
    public void setFieldTMs(List<Integer> fieldTMs) {
        if (!mapLoadingDone) {
            preprocessMaps();
            mapLoadingDone = true;
        }
        Iterator<Integer> iterTMs = fieldTMs.iterator();
        int[] givenTMs = new int[512];

        for (int offset : itemOffs) {
            int itemHere = readWord(offset);
            if (Gen3Constants.allowedItems.isTM(itemHere)) {
                // Cache replaced TMs to duplicate repeats
                if (givenTMs[itemHere] != 0) {
                    writeByte(offset, (byte) givenTMs[itemHere]);
                } else {
                    // Replace this with a TM from the list
                    int tm = iterTMs.next();
                    tm += Gen3Constants.tmItemOffset - 1;
                    givenTMs[itemHere] = tm;
                    writeWord(offset, tm);
                }
            }
        }
    }

    @Override
    public List<Integer> getRegularFieldItems() {
        if (!mapLoadingDone) {
            preprocessMaps();
            mapLoadingDone = true;
        }
        List<Integer> fieldItems = new ArrayList<>();

        for (int offset : itemOffs) {
            int itemHere = readWord(offset);
            if (Gen3Constants.allowedItems.isAllowed(itemHere) && !(Gen3Constants.allowedItems.isTM(itemHere))) {
                fieldItems.add(itemHere);
            }
        }
        return fieldItems;
    }

    @Override
    public void setRegularFieldItems(List<Integer> items) {
        if (!mapLoadingDone) {
            preprocessMaps();
            mapLoadingDone = true;
        }
        Iterator<Integer> iterItems = items.iterator();

        for (int offset : itemOffs) {
            int itemHere = readWord(offset);
            if (Gen3Constants.allowedItems.isAllowed(itemHere) && !(Gen3Constants.allowedItems.isTM(itemHere))) {
                // Replace it
                writeWord(offset, iterItems.next());
            }
        }

    }

    @Override
    public List<IngameTrade> getIngameTrades() {
        List<IngameTrade> trades = new ArrayList<>();

        // info
        int tableOffset = romEntry.getIntValue("TradeTableOffset");
        int tableSize = romEntry.getIntValue("TradeTableSize");
        int[] unused = romEntry.getArrayValue("TradesUnused");
        int unusedOffset = 0;
        int entryLength = 60;

        for (int entry = 0; entry < tableSize; entry++) {
            if (unusedOffset < unused.length && unused[unusedOffset] == entry) {
                unusedOffset++;
                continue;
            }
            IngameTrade trade = new IngameTrade();
            int entryOffset = tableOffset + entry * entryLength;
            trade.nickname = readVariableLengthString(entryOffset);
            trade.givenPokemon = pokesInternal[readWord(entryOffset + 12)];
            trade.ivs = new int[6];
            for (int i = 0; i < 6; i++) {
                trade.ivs[i] = rom[entryOffset + 14 + i] & 0xFF;
            }
            trade.otId = readWord(entryOffset + 24);
            trade.item = readWord(entryOffset + 40);
            trade.otName = readVariableLengthString(entryOffset + 43);
            trade.requestedPokemon = pokesInternal[readWord(entryOffset + 56)];
            trades.add(trade);
        }

        return trades;

    }

    @Override
    public void setIngameTrades(List<IngameTrade> trades) {
        // info
        int tableOffset = romEntry.getIntValue("TradeTableOffset");
        int tableSize = romEntry.getIntValue("TradeTableSize");
        int[] unused = romEntry.getArrayValue("TradesUnused");
        int unusedOffset = 0;
        int entryLength = 60;
        int tradeOffset = 0;

        for (int entry = 0; entry < tableSize; entry++) {
            if (unusedOffset < unused.length && unused[unusedOffset] == entry) {
                unusedOffset++;
                continue;
            }
            IngameTrade trade = trades.get(tradeOffset++);
            int entryOffset = tableOffset + entry * entryLength;
            writeFixedLengthString(trade.nickname, entryOffset, 12);
            writeWord(entryOffset + 12, pokedexToInternal[trade.givenPokemon.getNumber()]);
            for (int i = 0; i < 6; i++) {
                writeByte(entryOffset + 14 + i, (byte) trade.ivs[i]);
            }
            writeWord(entryOffset + 24, trade.otId);
            writeWord(entryOffset + 40, trade.item);
            writeFixedLengthString(trade.otName, entryOffset + 43, 11);
            writeWord(entryOffset + 56, pokedexToInternal[trade.requestedPokemon.getNumber()]);
        }
    }

    @Override
    public boolean hasDVs() {
        return false;
    }

    @Override
    public int generationOfPokemon() {
        return 3;
    }

    @Override
    //TODO: this is identical to the Gen 2 implementation => merge (?)
    public void removeEvosForPokemonPool() {
        PokemonSet<Pokemon> pokemonIncluded = this.restrictedPokemon;
        Set<Evolution> keepEvos = new HashSet<>();
        for (Pokemon pk : pokes) {
            if (pk != null) {
                keepEvos.clear();
                for (Evolution evol : pk.getEvolutionsFrom()) {
                    if (pokemonIncluded.contains(evol.from) && pokemonIncluded.contains(evol.to)) {
                        keepEvos.add(evol);
                    } else {
                        evol.to.getEvolutionsTo().remove(evol);
                    }
                }
                pk.getEvolutionsFrom().retainAll(keepEvos);
            }
        }
    }

    @Override
    public boolean supportsFourStartingMoves() {
        return true;
    }

    @Override
    public List<Integer> getFieldMoves() {
        // cut, fly, surf, strength, flash,
        // dig, teleport, waterfall,
        // rock smash, sweet scent
        // not softboiled or milk drink
        // dive and secret power in RSE only
        if (romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
            return Gen3Constants.frlgFieldMoves;
        } else {
            return Gen3Constants.rseFieldMoves;
        }
    }

    @Override
    public List<Integer> getEarlyRequiredHMMoves() {
        // RSE: rock smash
        // FRLG: cut
        if (romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
            return Gen3Constants.frlgEarlyRequiredHMMoves;
        } else {
            return Gen3Constants.rseEarlyRequiredHMMoves;
        }
    }

    @Override
    public int miscTweaksAvailable() {
        int available = MiscTweak.LOWER_CASE_POKEMON_NAMES.getValue();
        available |= MiscTweak.NATIONAL_DEX_AT_START.getValue();
        available |= MiscTweak.UPDATE_TYPE_EFFECTIVENESS.getValue();
        if (romEntry.getIntValue("RunIndoorsTweakOffset") > 0) {
            available |= MiscTweak.RUNNING_SHOES_INDOORS.getValue();
        }
        if (romEntry.getIntValue("TextSpeedValuesOffset") > 0 || romEntry.hasTweakFile("InstantTextTweak")) {
            available |= MiscTweak.FASTEST_TEXT.getValue();
        }
        if (romEntry.getIntValue("CatchingTutorialOpponentMonOffset") > 0
                || romEntry.getIntValue("CatchingTutorialPlayerMonOffset") > 0) {
            available |= MiscTweak.RANDOMIZE_CATCHING_TUTORIAL.getValue();
        }
        if (romEntry.getIntValue("PCPotionOffset") != 0) {
            available |= MiscTweak.RANDOMIZE_PC_POTION.getValue();
        }
        available |= MiscTweak.BAN_LUCKY_EGG.getValue();
        available |= MiscTweak.RUN_WITHOUT_RUNNING_SHOES.getValue();
        if (romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
            available |= MiscTweak.BALANCE_STATIC_LEVELS.getValue();
        }
        return available;
    }

    @Override
    public void applyMiscTweak(MiscTweak tweak) {
        if (tweak == MiscTweak.RUNNING_SHOES_INDOORS) {
            applyRunningShoesIndoorsPatch();
        } else if (tweak == MiscTweak.FASTEST_TEXT) {
            applyFastestTextPatch();
        } else if (tweak == MiscTweak.LOWER_CASE_POKEMON_NAMES) {
            applyCamelCaseNames();
        } else if (tweak == MiscTweak.NATIONAL_DEX_AT_START) {
            patchForNationalDex();
        } else if (tweak == MiscTweak.RANDOMIZE_CATCHING_TUTORIAL) {
            randomizeCatchingTutorial();
        } else if (tweak == MiscTweak.BAN_LUCKY_EGG) {
            allowedItems.banSingles(Gen3Items.luckyEgg);
            nonBadItems.banSingles(Gen3Items.luckyEgg);
        } else if (tweak == MiscTweak.RANDOMIZE_PC_POTION) {
            randomizePCPotion();
        } else if (tweak == MiscTweak.RUN_WITHOUT_RUNNING_SHOES) {
            applyRunWithoutRunningShoesPatch();
        } else if (tweak == MiscTweak.BALANCE_STATIC_LEVELS) {
            int[] fossilLevelOffsets = romEntry.getArrayValue("FossilLevelOffsets");
            for (int fossilLevelOffset : fossilLevelOffsets) {
                writeWord(rom, fossilLevelOffset, 30);
            }
        } else if (tweak == MiscTweak.UPDATE_TYPE_EFFECTIVENESS) {
            updateTypeEffectiveness();
        }
    }

    @Override
    public boolean isEffectivenessUpdated() {
        return effectivenessUpdated;
    }

    private void randomizeCatchingTutorial() {
        if (romEntry.getIntValue("CatchingTutorialOpponentMonOffset") > 0) {
            int oppOffset = romEntry.getIntValue("CatchingTutorialOpponentMonOffset");
            if (romEntry.getRomType() == Gen3Constants.RomType_FRLG) {
                Pokemon opponent = randomPokemonLimited(255, true);
                if (opponent != null) {

                    int oppValue = pokedexToInternal[opponent.getNumber()];
                    writeBytes(oppOffset, new byte[] {(byte) oppValue,
                            Gen3Constants.gbaSetRxOpcode | Gen3Constants.gbaR1});
                }
            } else {
                Pokemon opponent = randomPokemonLimited(510, true);
                if (opponent != null) {
                    int oppValue = pokedexToInternal[opponent.getNumber()];
                    if (oppValue > 255) {
                        writeBytes(oppOffset, new byte[] {(byte) 0xFF,
                                Gen3Constants.gbaSetRxOpcode | Gen3Constants.gbaR1,
                                (byte) (oppValue - 0xFF),
                                Gen3Constants.gbaAddRxOpcode | Gen3Constants.gbaR1});
                    } else {
                        writeBytes(oppOffset, new byte[] {(byte) oppValue,
                                Gen3Constants.gbaSetRxOpcode | Gen3Constants.gbaR1});

                        writeWord(oppOffset + 2, Gen3Constants.gbaNopOpcode);
                    }
                }
            }
        }

        if (romEntry.getIntValue("CatchingTutorialPlayerMonOffset") > 0) {
            int playerOffset = romEntry.getIntValue("CatchingTutorialPlayerMonOffset");
            Pokemon playerMon = randomPokemonLimited(510, false);
            if (playerMon != null) {
                int plyValue = pokedexToInternal[playerMon.getNumber()];
                if (plyValue > 255) {
                    writeBytes(playerOffset, new byte[] {(byte) 0xFF,
                            Gen3Constants.gbaSetRxOpcode | Gen3Constants.gbaR1,
                            (byte) (plyValue - 0xFF),
                            Gen3Constants.gbaAddRxOpcode | Gen3Constants.gbaR1});
                } else {
                    writeBytes(playerOffset, new byte[] {(byte) plyValue,
                            Gen3Constants.gbaSetRxOpcode | Gen3Constants.gbaR1});
                    writeWord(playerOffset + 2, Gen3Constants.gbaNopOpcode);
                }
            }
        }

    }

    private void applyRunningShoesIndoorsPatch() {
        if (romEntry.getIntValue("RunIndoorsTweakOffset") != 0) {
            writeByte(romEntry.getIntValue("RunIndoorsTweakOffset"), (byte) 0x00);
        }
    }

    private void applyFastestTextPatch() {
        if(romEntry.hasTweakFile("InstantTextTweak")) {
            try {
                FileFunctions.applyPatch(rom, romEntry.getTweakFile("InstantTextTweak"));
            } catch (IOException e) {
                throw new RandomizerIOException(e);
            }
        } else if (romEntry.getIntValue("TextSpeedValuesOffset") > 0) {
            int tsvOffset = romEntry.getIntValue("TextSpeedValuesOffset");
            byte[] newTextSpeedValues = new byte[] {4, // slow = medium
                    1, // medium = fast
                    0}; // fast = instant
            writeBytes(tsvOffset, newTextSpeedValues);
        }
    }

    private void randomizePCPotion() {
        if (romEntry.getIntValue("PCPotionOffset") != 0) {
            writeWord(romEntry.getIntValue("PCPotionOffset"), this.getNonBadItems().randomNonTM(this.random));
        }
    }

    private void applyRunWithoutRunningShoesPatch() {
        String prefix = Gen3Constants.getRunningShoesCheckPrefix(romEntry.getRomType());
        int offset = find(prefix);
        if (offset != 0) {
            // The prefix starts 0x12 bytes from what we want to patch because what comes
            // between is region and revision dependent. To start running, the game checks:
            // 1. That you're not underwater (RSE only)
            // 2. That you're holding the B button
            // 3. That the FLAG_SYS_B_DASH flag is set (aka, you've acquired Running Shoes)
            // 4. That you're allowed to run in this location
            // For #3, if the flag is unset, it jumps to a different part of the
            // code to make you walk instead. This simply nops out this jump so the
            // game stops caring about the FLAG_SYS_B_DASH flag entirely.
            writeWord(offset + 0x12, 0);
        }
    }

    private void updateTypeEffectiveness() {
        List<TypeRelationship> typeEffectivenessTable = readTypeEffectivenessTable();
        log("--Updating Type Effectiveness--");
        for (TypeRelationship relationship : typeEffectivenessTable) {
            // Change Ghost 0.5x against Steel to Ghost 1x to Steel
            if (relationship.attacker == Type.GHOST && relationship.defender == Type.STEEL) {
                relationship.effectiveness = Effectiveness.NEUTRAL;
                log("Replaced: Ghost not very effective vs Steel => Ghost neutral vs Steel");
            }

            // Change Dark 0.5x against Steel to Dark 1x to Steel
            else if (relationship.attacker == Type.DARK && relationship.defender == Type.STEEL) {
                relationship.effectiveness = Effectiveness.NEUTRAL;
                log("Replaced: Dark not very effective vs Steel => Dark neutral vs Steel");
            }
        }
        logBlankLine();
        writeTypeEffectivenessTable(typeEffectivenessTable);
        effectivenessUpdated = true;
    }

    private List<TypeRelationship> readTypeEffectivenessTable() {
        List<TypeRelationship> typeEffectivenessTable = new ArrayList<>();
        int currentOffset = romEntry.getIntValue("TypeEffectivenessOffset");
        int attackingType = rom[currentOffset];
        // 0xFE marks the end of the table *not* affected by Foresight, while 0xFF marks
        // the actual end of the table. Since we don't care about Ghost immunities at all,
        // just stop once we reach the Foresight section.
        while (attackingType != (byte) 0xFE) {
            int defendingType = rom[currentOffset + 1];
            int effectivenessInternal = rom[currentOffset + 2];
            Type attacking = Gen3Constants.typeTable[attackingType];
            Type defending = Gen3Constants.typeTable[defendingType];
            Effectiveness effectiveness = null;
            switch (effectivenessInternal) {
                case 20:
                    effectiveness = Effectiveness.DOUBLE;
                    break;
                case 10:
                    effectiveness = Effectiveness.NEUTRAL;
                    break;
                case 5:
                    effectiveness = Effectiveness.HALF;
                    break;
                case 0:
                    effectiveness = Effectiveness.ZERO;
                    break;
            }
            if (effectiveness != null) {
                TypeRelationship relationship = new TypeRelationship(attacking, defending, effectiveness);
                typeEffectivenessTable.add(relationship);
            }
            currentOffset += 3;
            attackingType = rom[currentOffset];
        }
        return typeEffectivenessTable;
    }

    private void writeTypeEffectivenessTable(List<TypeRelationship> typeEffectivenessTable) {
        int currentOffset = romEntry.getIntValue("TypeEffectivenessOffset");
        for (TypeRelationship relationship : typeEffectivenessTable) {
            byte effectivenessInternal = switch (relationship.effectiveness) {
                case DOUBLE -> 20;
                case NEUTRAL -> 10;
                case HALF -> 5;
                case ZERO -> 0;
                default -> 0;
            };
            byte[] effectivenessData = new byte[] {Gen3Constants.typeToByte(relationship.attacker),
                    Gen3Constants.typeToByte(relationship.defender), effectivenessInternal};
            writeBytes(currentOffset, effectivenessData);
            currentOffset += 3;
        }
	}

	@Override
	public List<Integer> getAllHeldItems() {
		return Gen3Constants.allHeldItems;
	}

	@Override
	public boolean hasRivalFinalBattle() {
        return romEntry.getRomType() == Gen3Constants.RomType_FRLG;
    }

    @Override
    public List<Integer> getAllConsumableHeldItems() {
        return Gen3Constants.consumableHeldItems;
    }

    public void enableGuaranteedPokemonCatching() {
        int offset = find(rom, Gen3Constants.perfectOddsBranchLocator);
        if (offset > 0) {
            // In Cmd_handleballthrow, the middle of the function checks if the odds of catching a Pokemon
            // is greater than 254; if it is, then the Pokemon is automatically caught. In ASM, this is
            // represented by:
            // cmp r6, #0xFE
            // bls oddsLessThanOrEqualTo254
            // The below code just nops these two instructions so that we *always* act like our odds are 255,
            // and Pokemon are automatically caught no matter what.
            writeBytes(offset, new byte[] {0x00, 0x00, 0x00, 0x00});
        }
    }

    @Override
    public void loadPokemonPalettes() {
        int normalPaletteTableOffset = romEntry.getIntValue("PokemonNormalPalettes");
        int shinyPaletteTableOffset = romEntry.getIntValue("PokemonShinyPalettes");
        for (Pokemon pk : getPokemonSet()) {
            int pokeNumber = pokedexToInternal[pk.getNumber()];

            int normalPalOffset = readPointer(normalPaletteTableOffset + pokeNumber * 8);
            pk.setNormalPalette(readPalette(normalPalOffset));

            int shinyPalOffset = readPointer(shinyPaletteTableOffset + pokeNumber * 8);
            pk.setShinyPalette(readPalette(shinyPalOffset));
        }
    }

    private Palette readPalette(int palOffset) {
        byte[] paletteBytes = DSDecmp.Decompress(rom, palOffset);
        return new Palette(paletteBytes);
    }

    @Override
    public void savePokemonPalettes() {
        int normalPaletteTableOffset = romEntry.getIntValue("PokemonNormalPalettes");
        int shinyPaletteTableOffset = romEntry.getIntValue("PokemonShinyPalettes");
        for (Pokemon pk : getPokemonSet()) {
            int pokeNumber = pokedexToInternal[pk.getNumber()];

            int normalPalPointerOffset = normalPaletteTableOffset + pokeNumber * 8;
            rewritePalette(normalPalPointerOffset, pk.getNormalPalette());

            int shinyPalPointerOffset = shinyPaletteTableOffset + pokeNumber * 8;
            rewritePalette(shinyPalPointerOffset, pk.getShinyPalette());
        }
    }

    private void rewritePalette(int pointerOffset, Palette palette) {
        rewriteCompressedData(pointerOffset, palette.toBytes());
    }

    /*
     * Assumes there is only one pointer to the compressed data. If there are more,
     * use rewriteCompressedData(int, byte[], int[]) directly.
     */
    private void rewriteCompressedData(int pointerOffset, byte[] uncompressed) {
        rewriteCompressedData(pointerOffset, uncompressed, new int[0]);
    }

    private void rewriteCompressedData(int pointerOffset, byte[] uncompressed, int[] secondaryPointerOffsets) {
        new DataRewriter<byte[]>().rewriteData(pointerOffset, uncompressed, secondaryPointerOffsets,
                DSCmp::compressLZ10, this::compressedDataLength);
    }

    /*
     * Returns the length in bytes of the compressed data at the pointer. NOT the
     * length of the uncompressed data, but the length of it when compressed.
     */
    private int compressedDataLength(int pointer) {
        // Yes, the "easiest" way to check how long compressed data is to uncompress it
        // and then compress it back.
        // A better solution would require understanding the inner workings of the LZ10.
        byte[] uncompressed = DSDecmp.Decompress(rom, pointer);
        byte[] compressed = DSCmp.compressLZ10(uncompressed);
        return compressed.length;
    }

	private void rewriteVariableLengthString(int pointerOffset, String string) {
		rewriteVariableLengthString(pointerOffset, string, new int[0]);
	}

	private void rewriteVariableLengthString(int pointerOffset, String string, int[] secondaryPointerOffsets) {
		new DataRewriter<String>().rewriteData(pointerOffset, string, secondaryPointerOffsets,
				this::variableLengthStringToBytes, (oldDataOffset) -> lengthOfStringAt(oldDataOffset) + 1);
	}

	private byte[] variableLengthStringToBytes(String string) {
		byte[] translated = translateString(string);
		byte[] newData = Arrays.copyOf(translated, translated.length + 1);
		newData[newData.length - 1] = (byte) 0xFF;
		return newData;
	}

    @Override
    protected FreedSpace getFreedSpace() {
        return freedSpace;
    }

	@Override
	protected byte getFreeSpaceByte() {
		return Gen3Constants.freeSpaceByte;
	}

	/**
	 * Finds and "frees" space that was unused in the unrandomized ROM, i.e. marks
	 * it as available for writing to.<br>
	 * "Unused" in this context refers only to blank data of many "FF"s in a row,
	 * NOT stuff like unused graphics, maps, and music. <br>
	 * <br>
	 * The finding is automatic instead of decided wholly by manual offsets. This
	 * presents a risk in theory, of marking "unused" space which isn't, but there
	 * is a guiding manual offset and other precautions, so this risk should be
	 * slim.
	 */
	private void freeAllUnusedSpace() {
		int unusedSpaceStartOffset = romEntry.getIntValue("FreeSpace");
		int unusedSpaceOffset = unusedSpaceStartOffset;
		int chunkLength = Gen3Constants.unusedSpaceChunkLength;
		int frontMargin = Gen3Constants.unusedSpaceFrontMargin;

		boolean freedAllUnused = false;

		while (!freedAllUnused) {
			byte[] searchNeedle = new byte[chunkLength];
			for (int i = 0; i < chunkLength; i++) {
				searchNeedle[i] = getFreeSpaceByte();
			}
			int foundOffset = RomFunctions.searchForFirst(rom, unusedSpaceOffset, searchNeedle);

			if (foundOffset < unusedSpaceStartOffset) {
				freedAllUnused = true;
			} else {

				if (foundOffset > unusedSpaceOffset + chunkLength || foundOffset == unusedSpaceStartOffset) {
					freeSpace(foundOffset + frontMargin, chunkLength - frontMargin);
				} else {
					freeSpace(foundOffset, chunkLength);
				}
				unusedSpaceOffset = foundOffset + chunkLength;
			}
		}

	}
    
    @Override
    public BufferedImage getPokemonImage(Pokemon pk, boolean back, boolean shiny, boolean transparentBackground,
            boolean includePalette) {

        int num = pokedexToInternal[pk.getNumber()];
        int sprites = back ? romEntry.getIntValue("PokemonBackSprites") : romEntry.getIntValue("PokemonFrontSprites");

        int spriteOffset = readPointer(sprites + num * 8);
        byte[] trueSprite = DSDecmp.Decompress(rom, spriteOffset);
        // Uses the 0-index missingno sprite if the data failed to read, for debugging
        // purposes
        if (trueSprite == null) {
            spriteOffset = readPointer(sprites);
            trueSprite = DSDecmp.Decompress(rom, spriteOffset);
        }

        Palette palette = shiny ? pk.getShinyPalette() : pk.getNormalPalette();
        int[] convPalette = palette.toARGB();
        if (transparentBackground) {
            convPalette[0] = 0;
        }
        // Castform has a 64-color palette, 16 colors for each form.
        if (pk.getNumber() == Species.castform) {
        	convPalette = Arrays.copyOfRange(convPalette, 0, 16);
        }

        // Make image, 4bpp
        BufferedImage bim = GFXFunctions.drawTiledImage(trueSprite, convPalette, 64, 64, 4);

        if (includePalette) {
            for (int j = 0; j < convPalette.length; j++) {
                bim.setRGB(j, 0, convPalette[j]);
            }
        }

        return bim;
    }

    private String getPaletteFilesID() {
        return switch (romEntry.getRomType()) {
            case Gen3Constants.RomType_Ruby, Gen3Constants.RomType_Sapp ->
                // TODO: look at Blastoise, Caterpie, Kadabra, Deoxys.
                // otherwise all palettes are pretty much identical (in use).
                    "E";
            case Gen3Constants.RomType_Em -> "E";
            case Gen3Constants.RomType_FRLG -> "FRLG";
            default -> null;
        };
    }

    @Override
    public List<Integer> getSensibleHeldItemsFor(TrainerPokemon tp, boolean consumableOnly, List<Move> moves, int[] pokeMoves) {
        List<Integer> items = new ArrayList<>(Gen3Constants.generalPurposeConsumableItems);
        if (!consumableOnly) {
            items.addAll(Gen3Constants.generalPurposeItems);
        }
        for (int moveIdx : pokeMoves) {
            Move move = moves.get(moveIdx);
            if (move == null) {
                continue;
            }
            if (GBConstants.physicalTypes.contains(move.type) && move.power > 0) {
                items.add(Gen3Items.liechiBerry);
                if (!consumableOnly) {
                    items.addAll(Gen3Constants.typeBoostingItems.get(move.type));
                    items.add(Gen3Items.choiceBand);
                }
            }
            if (!GBConstants.physicalTypes.contains(move.type) && move.power > 0) {
                items.add(Gen3Items.petayaBerry);
                if (!consumableOnly) {
                    items.addAll(Gen3Constants.typeBoostingItems.get(move.type));
                }
            }
        }
        if (!consumableOnly) {
            List<Integer> speciesItems = Gen3Constants.speciesBoostingItems.get(tp.pokemon.getNumber());
            if (speciesItems != null) {
                for (int i = 0; i < 6; i++) {  // Increase the likelihood of using species specific items.
                    items.addAll(speciesItems);
                }
            }
        }
        return items;
    }
    
    @Override
    public PaletteHandler getPaletteHandler() {
        return paletteHandler;
    }

    @Override
    public Gen3RomEntry getRomEntry() {
        return romEntry;
    }
}
