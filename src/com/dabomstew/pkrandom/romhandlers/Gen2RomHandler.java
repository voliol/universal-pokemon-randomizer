package com.dabomstew.pkrandom.romhandlers;

/*----------------------------------------------------------------------------*/
/*--  Gen2RomHandler.java - randomizer handler for G/S/C.                   --*/
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

import com.dabomstew.pkrandom.*;
import com.dabomstew.pkrandom.romhandlers.romentries.*;
import com.dabomstew.pkrandom.constants.*;
import com.dabomstew.pkrandom.exceptions.RandomizerIOException;
import com.dabomstew.pkrandom.graphics.Gen2PaletteHandler;
import com.dabomstew.pkrandom.graphics.Palette;
import com.dabomstew.pkrandom.graphics.PaletteHandler;
import com.dabomstew.pkrandom.pokemon.*;
import compressors.Gen2Decmp;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class Gen2RomHandler extends AbstractGBCRomHandler {

    public static class Factory extends RomHandler.Factory {

        @Override
        public Gen2RomHandler create(Random random, PrintStream logStream) {
            return new Gen2RomHandler(random, logStream);
        }

        public boolean isLoadable(String filename) {
            long fileLength = new File(filename).length();
            if (fileLength > 8 * 1024 * 1024) {
                return false;
            }
            byte[] loaded = loadFilePartial(filename, 0x1000);
            // nope
            return loaded.length != 0 && detectRomInner(loaded, (int) fileLength);
        }
    }

    public Gen2RomHandler(Random random) {
        super(random, null);
    }

    public Gen2RomHandler(Random random, PrintStream logStream) {
        super(random, logStream);
        this.paletteHandler = new Gen2PaletteHandler(random);
    }

    private static List<Gen2RomEntry> roms;

    static {
        loadROMInfo();
    }

    private static void loadROMInfo() {
        try {
            roms = Gen2RomEntry.READER.readEntriesFromFile("gen2_offsets.ini");
        } catch (IOException e) {
            throw new RuntimeException("Could not read Rom Entries.", e);
        }
    }

    // Sub-handlers
    private PaletteHandler paletteHandler;

    // This ROM's data
    private Gen2RomEntry romEntry;
    private Pokemon[] pokes;
    private List<Pokemon> pokemonList;
    private List<Trainer> trainers;
    private Move[] moves;
    private Map<Integer, List<MoveLearnt>> movesets;
    private boolean havePatchedFleeing;
    private String[] itemNames;
    private List<Integer> itemOffs;
    private String[][] mapNames;
    private String[] landmarkNames;
    private boolean isVietCrystal;
    private ItemList allowedItems, nonBadItems;
    private boolean effectivenessUpdated;

    @Override
    public boolean detectRom(byte[] rom) {
        return detectRomInner(rom, rom.length);
    }

    private static boolean detectRomInner(byte[] rom, int romSize) {
        // size check
        return romSize >= GBConstants.minRomSize && romSize <= GBConstants.maxRomSize && checkRomEntry(rom) != null;
    }

    @Override
    public void midLoadingSetUp() {
        super.midLoadingSetUp();
        havePatchedFleeing = false;
        loadLandmarkNames();
        preprocessMaps();
    }

    @Override
    protected void loadGameData() {
        super.loadGameData();
        allowedItems = Gen2Constants.allowedItems.copy();
        nonBadItems = Gen2Constants.nonBadItems.copy();
        // VietCrystal: exclude Burn Heal, Calcium, TwistedSpoon, and Elixir
        // crashes your game if used, glitches out your inventory if carried
        if (isVietCrystal) {
            allowedItems.banSingles(Gen2Items.burnHeal, Gen2Items.calcium, Gen2Items.elixer, Gen2Items.twistedSpoon);
        }
    }

    @Override
    protected void initRomEntry() {
        romEntry = checkRomEntry(this.rom);
        if (romEntry.getName().equals("Crystal (J)")
                && rom[Gen2Constants.vietCrystalCheckOffset] == Gen2Constants.vietCrystalCheckValue) {
            readTextTable("vietcrystal");
            isVietCrystal = true;
        } else {
            isVietCrystal = false;
        }
    }

    @Override
    protected void initTextTables() {
        clearTextTables();
        readTextTable("gameboy_jpn");
        String extraTableFile = romEntry.getExtraTableFile();
        if (extraTableFile != null && !extraTableFile.equalsIgnoreCase("none")) {
            readTextTable(extraTableFile);
        }
    }

    private static Gen2RomEntry checkRomEntry(byte[] rom) {
        int version = rom[GBConstants.versionOffset] & 0xFF;
        int nonjap = rom[GBConstants.jpFlagOffset] & 0xFF;
        // Check for specific CRC first
        int crcInHeader = ((rom[GBConstants.crcOffset] & 0xFF) << 8) | (rom[GBConstants.crcOffset + 1] & 0xFF);
        for (Gen2RomEntry re : roms) {
            if (romCode(rom, re.getRomCode()) && re.getVersion() == version && re.getNonJapanese() == nonjap
                    && re.getCRCInHeader() == crcInHeader) {
                return re;
            }
        }
        // Now check for non-specific-CRC entries
        for (Gen2RomEntry re : roms) {
            if (romCode(rom, re.getRomCode()) && re.getVersion() == version && re.getNonJapanese() == nonjap && re.getCRCInHeader() == -1) {
                return re;
            }
        }
        // Not found
        return null;
    }

    @Override
    public void loadPokemonStats() {
        pokes = new Pokemon[Gen2Constants.pokemonCount + 1];
        // Fetch our names
        String[] pokeNames = readPokemonNames();
        int offs = romEntry.getIntValue("PokemonStatsOffset");
        // Get base stats
        for (int i = 1; i <= Gen2Constants.pokemonCount; i++) {
            pokes[i] = new Pokemon(i);
            loadBasicPokeStats(pokes[i], offs + (i - 1) * Gen2Constants.baseStatsEntrySize);
            // Name?
            pokes[i].setName(pokeNames[i]);
        }
        this.pokemonList = Arrays.asList(pokes);
    }

    @Override
    public void savePokemonStats() {
        // Write pokemon names
        int offs = romEntry.getIntValue("PokemonNamesOffset");
        int len = romEntry.getIntValue("PokemonNamesLength");
        for (int i = 1; i <= Gen2Constants.pokemonCount; i++) {
            int stringOffset = offs + (i - 1) * len;
            writeFixedLengthString(pokes[i].getName(), stringOffset, len);
        }
        // Write pokemon stats
        int offs2 = romEntry.getIntValue("PokemonStatsOffset");
        for (int i = 1; i <= Gen2Constants.pokemonCount; i++) {
            saveBasicPokeStats(pokes[i], offs2 + (i - 1) * Gen2Constants.baseStatsEntrySize);
        }
        // Write evolutions and movesets
        saveEvosAndMovesLearnt();
    }

    private String[] readMoveNames() {
        int offset = romEntry.getIntValue("MoveNamesOffset");
        String[] moveNames = new String[Gen2Constants.moveCount + 1];
        for (int i = 1; i <= Gen2Constants.moveCount; i++) {
            moveNames[i] = readVariableLengthString(offset, false);
            offset += lengthOfStringAt(offset, false);
        }
        return moveNames;
    }

    @Override
    public void loadMoves() {
        moves = new Move[Gen2Constants.moveCount + 1];
        String[] moveNames = readMoveNames();
        int offs = romEntry.getIntValue("MoveDataOffset");
        for (int i = 1; i <= Gen2Constants.moveCount; i++) {
            moves[i] = new Move();
            moves[i].name = moveNames[i];
            moves[i].number = i;
            moves[i].internalId = i;
            moves[i].effectIndex = rom[offs + (i - 1) * 7 + 1] & 0xFF;
            moves[i].hitratio = ((rom[offs + (i - 1) * 7 + 4] & 0xFF)) / 255.0 * 100;
            moves[i].power = rom[offs + (i - 1) * 7 + 2] & 0xFF;
            moves[i].pp = rom[offs + (i - 1) * 7 + 5] & 0xFF;
            moves[i].type = Gen2Constants.typeTable[rom[offs + (i - 1) * 7 + 3]];
            moves[i].category = GBConstants.physicalTypes.contains(moves[i].type) ? MoveCategory.PHYSICAL : MoveCategory.SPECIAL;
            if (moves[i].power == 0 && !GlobalConstants.noPowerNonStatusMoves.contains(i)) {
                moves[i].category = MoveCategory.STATUS;
            }

            if (i == Moves.swift) {
                perfectAccuracy = (int) moves[i].hitratio;
            }

            if (GlobalConstants.normalMultihitMoves.contains(i)) {
                moves[i].hitCount = 3;
            } else if (GlobalConstants.doubleHitMoves.contains(i)) {
                moves[i].hitCount = 2;
            } else if (i == Moves.tripleKick) {
                moves[i].hitCount = 2.71; // this assumes the first hit lands
            }

            // Values taken from effect_priorities.asm from the Gen 2 disassemblies.
            if (moves[i].effectIndex == Gen2Constants.priorityHitEffectIndex) {
                moves[i].priority = 2;
            } else if (moves[i].effectIndex == Gen2Constants.protectEffectIndex ||
                    moves[i].effectIndex == Gen2Constants.endureEffectIndex) {
                moves[i].priority = 3;
            } else if (moves[i].effectIndex == Gen2Constants.forceSwitchEffectIndex ||
                    moves[i].effectIndex == Gen2Constants.counterEffectIndex ||
                    moves[i].effectIndex == Gen2Constants.mirrorCoatEffectIndex) {
                moves[i].priority = 0;
            } else {
                moves[i].priority = 1;
            }

            double secondaryEffectChance = ((rom[offs + (i - 1) * 7 + 6] & 0xFF)) / 255.0 * 100;
            loadStatChangesFromEffect(moves[i], secondaryEffectChance);
            loadStatusFromEffect(moves[i], secondaryEffectChance);
            loadMiscMoveInfoFromEffect(moves[i], secondaryEffectChance);
        }
    }

    private void loadStatChangesFromEffect(Move move, double secondaryEffectChance) {
        switch (move.effectIndex) {
            case Gen2Constants.noDamageAtkPlusOneEffect:
            case Gen2Constants.damageUserAtkPlusOneEffect:
                move.statChanges[0].type = StatChangeType.ATTACK;
                move.statChanges[0].stages = 1;
                break;
            case Gen2Constants.noDamageDefPlusOneEffect:
            case Gen2Constants.damageUserDefPlusOneEffect:
            case Gen2Constants.defenseCurlEffect:
                move.statChanges[0].type = StatChangeType.DEFENSE;
                move.statChanges[0].stages = 1;
                break;
            case Gen2Constants.noDamageSpAtkPlusOneEffect:
                move.statChanges[0].type = StatChangeType.SPECIAL_ATTACK;
                move.statChanges[0].stages = 1;
                break;
            case Gen2Constants.noDamageEvasionPlusOneEffect:
                move.statChanges[0].type = StatChangeType.EVASION;
                move.statChanges[0].stages = 1;
                break;
            case Gen2Constants.noDamageAtkMinusOneEffect:
            case Gen2Constants.damageAtkMinusOneEffect:
                move.statChanges[0].type = StatChangeType.ATTACK;
                move.statChanges[0].stages = -1;
                break;
            case Gen2Constants.noDamageDefMinusOneEffect:
            case Gen2Constants.damageDefMinusOneEffect:
                move.statChanges[0].type = StatChangeType.DEFENSE;
                move.statChanges[0].stages = -1;
                break;
            case Gen2Constants.noDamageSpeMinusOneEffect:
            case Gen2Constants.damageSpeMinusOneEffect:
                move.statChanges[0].type = StatChangeType.SPEED;
                move.statChanges[0].stages = -1;
                break;
            case Gen2Constants.noDamageAccuracyMinusOneEffect:
            case Gen2Constants.damageAccuracyMinusOneEffect:
                move.statChanges[0].type = StatChangeType.ACCURACY;
                move.statChanges[0].stages = -1;
                break;
            case Gen2Constants.noDamageEvasionMinusOneEffect:
                move.statChanges[0].type = StatChangeType.EVASION;
                move.statChanges[0].stages = -1;
                break;
            case Gen2Constants.noDamageAtkPlusTwoEffect:
            case Gen2Constants.swaggerEffect:
                move.statChanges[0].type = StatChangeType.ATTACK;
                move.statChanges[0].stages = 2;
                break;
            case Gen2Constants.noDamageDefPlusTwoEffect:
                move.statChanges[0].type = StatChangeType.DEFENSE;
                move.statChanges[0].stages = 2;
                break;
            case Gen2Constants.noDamageSpePlusTwoEffect:
                move.statChanges[0].type = StatChangeType.SPEED;
                move.statChanges[0].stages = 2;
                break;
            case Gen2Constants.noDamageSpDefPlusTwoEffect:
                move.statChanges[0].type = StatChangeType.SPECIAL_DEFENSE;
                move.statChanges[0].stages = 2;
                break;
            case Gen2Constants.noDamageAtkMinusTwoEffect:
                move.statChanges[0].type = StatChangeType.ATTACK;
                move.statChanges[0].stages = -2;
                break;
            case Gen2Constants.noDamageDefMinusTwoEffect:
                move.statChanges[0].type = StatChangeType.DEFENSE;
                move.statChanges[0].stages = -2;
                break;
            case Gen2Constants.noDamageSpeMinusTwoEffect:
                move.statChanges[0].type = StatChangeType.SPEED;
                move.statChanges[0].stages = -2;
                break;
            case Gen2Constants.noDamageSpDefMinusTwoEffect:
                move.statChanges[0].type = StatChangeType.SPECIAL_DEFENSE;
                move.statChanges[0].stages = -2;
                break;
            case Gen2Constants.damageSpDefMinusOneEffect:
                move.statChanges[0].type = StatChangeType.SPECIAL_DEFENSE;
                move.statChanges[0].stages = -1;
                break;
            case Gen2Constants.damageUserAllPlusOneEffect:
                move.statChanges[0].type = StatChangeType.ALL;
                move.statChanges[0].stages = 1;
                break;
            default:
                // Move does not have a stat-changing effect
                return;
        }

        switch (move.effectIndex) {
            case Gen2Constants.noDamageAtkPlusOneEffect:
            case Gen2Constants.noDamageDefPlusOneEffect:
            case Gen2Constants.noDamageSpAtkPlusOneEffect:
            case Gen2Constants.noDamageEvasionPlusOneEffect:
            case Gen2Constants.noDamageAtkMinusOneEffect:
            case Gen2Constants.noDamageDefMinusOneEffect:
            case Gen2Constants.noDamageSpeMinusOneEffect:
            case Gen2Constants.noDamageAccuracyMinusOneEffect:
            case Gen2Constants.noDamageEvasionMinusOneEffect:
            case Gen2Constants.noDamageAtkPlusTwoEffect:
            case Gen2Constants.noDamageDefPlusTwoEffect:
            case Gen2Constants.noDamageSpePlusTwoEffect:
            case Gen2Constants.noDamageSpDefPlusTwoEffect:
            case Gen2Constants.noDamageAtkMinusTwoEffect:
            case Gen2Constants.noDamageDefMinusTwoEffect:
            case Gen2Constants.noDamageSpeMinusTwoEffect:
            case Gen2Constants.noDamageSpDefMinusTwoEffect:
            case Gen2Constants.swaggerEffect:
            case Gen2Constants.defenseCurlEffect:
                if (move.statChanges[0].stages < 0 || move.effectIndex == Gen2Constants.swaggerEffect) {
                    move.statChangeMoveType = StatChangeMoveType.NO_DAMAGE_TARGET;
                } else {
                    move.statChangeMoveType = StatChangeMoveType.NO_DAMAGE_USER;
                }
                break;

            case Gen2Constants.damageAtkMinusOneEffect:
            case Gen2Constants.damageDefMinusOneEffect:
            case Gen2Constants.damageSpeMinusOneEffect:
            case Gen2Constants.damageSpDefMinusOneEffect:
            case Gen2Constants.damageAccuracyMinusOneEffect:
                move.statChangeMoveType = StatChangeMoveType.DAMAGE_TARGET;
                break;

            case Gen2Constants.damageUserDefPlusOneEffect:
            case Gen2Constants.damageUserAtkPlusOneEffect:
            case Gen2Constants.damageUserAllPlusOneEffect:
                move.statChangeMoveType = StatChangeMoveType.DAMAGE_USER;
                break;
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

    private void loadStatusFromEffect(Move move, double secondaryEffectChance) {
        switch (move.effectIndex) {
            case Gen2Constants.noDamageSleepEffect:
            case Gen2Constants.toxicEffect:
            case Gen2Constants.noDamageConfusionEffect:
            case Gen2Constants.noDamagePoisonEffect:
            case Gen2Constants.noDamageParalyzeEffect:
            case Gen2Constants.swaggerEffect:
                move.statusMoveType = StatusMoveType.NO_DAMAGE;
                break;

            case Gen2Constants.damagePoisonEffect:
            case Gen2Constants.damageBurnEffect:
            case Gen2Constants.damageFreezeEffect:
            case Gen2Constants.damageParalyzeEffect:
            case Gen2Constants.damageConfusionEffect:
            case Gen2Constants.twineedleEffect:
            case Gen2Constants.damageBurnAndThawUserEffect:
            case Gen2Constants.thunderEffect:
                move.statusMoveType = StatusMoveType.DAMAGE;
                break;

            default:
                // Move does not have a status effect
                return;
        }

        switch (move.effectIndex) {
            case Gen2Constants.noDamageSleepEffect -> move.statusType = StatusType.SLEEP;
            case Gen2Constants.damagePoisonEffect, Gen2Constants.noDamagePoisonEffect, Gen2Constants.twineedleEffect ->
                    move.statusType = StatusType.POISON;
            case Gen2Constants.damageBurnEffect, Gen2Constants.damageBurnAndThawUserEffect ->
                    move.statusType = StatusType.BURN;
            case Gen2Constants.damageFreezeEffect -> move.statusType = StatusType.FREEZE;
            case Gen2Constants.damageParalyzeEffect, Gen2Constants.noDamageParalyzeEffect, Gen2Constants.thunderEffect ->
                    move.statusType = StatusType.PARALYZE;
            case Gen2Constants.toxicEffect -> move.statusType = StatusType.TOXIC_POISON;
            case Gen2Constants.noDamageConfusionEffect, Gen2Constants.damageConfusionEffect, Gen2Constants.swaggerEffect ->
                    move.statusType = StatusType.CONFUSION;
        }

        if (move.statusMoveType == StatusMoveType.DAMAGE) {
            move.statusPercentChance = secondaryEffectChance;
            if (move.statusPercentChance == 0.0) {
                move.statusPercentChance = 100.0;
            }
        }
    }

    private void loadMiscMoveInfoFromEffect(Move move, double secondaryEffectChance) {
        switch (move.effectIndex) {
            case Gen2Constants.flinchEffect, Gen2Constants.snoreEffect, Gen2Constants.twisterEffect,
                    Gen2Constants.stompEffect ->
                    move.flinchPercentChance = secondaryEffectChance;
            case Gen2Constants.damageAbsorbEffect, Gen2Constants.dreamEaterEffect -> move.absorbPercent = 50;
            case Gen2Constants.damageRecoilEffect -> move.recoilPercent = 25;
            case Gen2Constants.flailAndReversalEffect, Gen2Constants.futureSightEffect ->
                    move.criticalChance = CriticalChance.NONE;
            case Gen2Constants.bindingEffect, Gen2Constants.trappingEffect -> move.isTrapMove = true;
            case Gen2Constants.razorWindEffect, Gen2Constants.skyAttackEffect, Gen2Constants.skullBashEffect,
                    Gen2Constants.solarbeamEffect, Gen2Constants.semiInvulnerableEffect ->
                    move.isChargeMove = true;
            case Gen2Constants.hyperBeamEffect -> move.isRechargeMove = true;
        }

        if (Gen2Constants.increasedCritMoves.contains(move.number)) {
            move.criticalChance = CriticalChance.INCREASED;
        }
    }

    @Override
    public void saveMoves() {
        int offs = romEntry.getIntValue("MoveDataOffset");
        for (int i = 1; i <= 251; i++) {
            int hitratio = (int) Math.round(moves[i].hitratio * 2.55);
            if (hitratio < 0) {
                hitratio = 0;
            }
            if (hitratio > 255) {
                hitratio = 255;
            }
            writeBytes(offs + (i - 1) * 7 + 1, new byte[]{(byte) moves[i].effectIndex, (byte) moves[i].power,
                    Gen2Constants.typeToByte(moves[i].type), (byte) hitratio, (byte) moves[i].pp});
        }
    }

    public List<Move> getMoves() {
        return Arrays.asList(moves);
    }

    private void loadBasicPokeStats(Pokemon pkmn, int offset) {
        pkmn.setHp(rom[offset + Gen2Constants.bsHPOffset] & 0xFF);
        pkmn.setAttack(rom[offset + Gen2Constants.bsAttackOffset] & 0xFF);
        pkmn.setDefense(rom[offset + Gen2Constants.bsDefenseOffset] & 0xFF);
        pkmn.setSpeed(rom[offset + Gen2Constants.bsSpeedOffset] & 0xFF);
        pkmn.setSpatk(rom[offset + Gen2Constants.bsSpAtkOffset] & 0xFF);
        pkmn.setSpdef(rom[offset + Gen2Constants.bsSpDefOffset] & 0xFF);
        // Type
        pkmn.setPrimaryType(Gen2Constants.typeTable[rom[offset + Gen2Constants.bsPrimaryTypeOffset] & 0xFF]);
        pkmn.setSecondaryType(Gen2Constants.typeTable[rom[offset + Gen2Constants.bsSecondaryTypeOffset] & 0xFF]);
        // Only one type?
        if (pkmn.getSecondaryType() == pkmn.getPrimaryType()) {
            pkmn.setSecondaryType(null);
        }
        pkmn.setCatchRate(rom[offset + Gen2Constants.bsCatchRateOffset] & 0xFF);
        pkmn.setGuaranteedHeldItem(-1);
        pkmn.setCommonHeldItem(rom[offset + Gen2Constants.bsCommonHeldItemOffset] & 0xFF);
        pkmn.setRareHeldItem(rom[offset + Gen2Constants.bsRareHeldItemOffset] & 0xFF);
        pkmn.setDarkGrassHeldItem(-1);
        pkmn.setGrowthCurve(ExpCurve.fromByte(rom[offset + Gen2Constants.bsGrowthCurveOffset]));
        pkmn.setPicDimensions(rom[offset + Gen2Constants.bsPicDimensionsOffset] & 0xFF);

    }

    private void saveBasicPokeStats(Pokemon pkmn, int offset) {
        writeByte(offset + Gen2Constants.bsHPOffset, (byte) pkmn.getHp());
        writeByte(offset + Gen2Constants.bsAttackOffset, (byte) pkmn.getAttack());
        writeByte(offset + Gen2Constants.bsDefenseOffset, (byte) pkmn.getDefense());
        writeByte(offset + Gen2Constants.bsSpeedOffset, (byte) pkmn.getSpeed());
        writeByte(offset + Gen2Constants.bsSpAtkOffset, (byte) pkmn.getSpatk());
        writeByte(offset + Gen2Constants.bsSpDefOffset, (byte) pkmn.getDefense());
        writeByte(offset + Gen2Constants.bsPrimaryTypeOffset, Gen2Constants.typeToByte(pkmn.getPrimaryType()));
        byte secondaryTypeByte = pkmn.getSecondaryType() == null ? rom[offset + Gen2Constants.bsPrimaryTypeOffset]
                : Gen2Constants.typeToByte(pkmn.getSecondaryType());
        writeByte(offset + Gen2Constants.bsSecondaryTypeOffset, secondaryTypeByte);
        writeByte(offset + Gen2Constants.bsCatchRateOffset, (byte) pkmn.getCatchRate());

        writeByte(offset + Gen2Constants.bsCommonHeldItemOffset, (byte) pkmn.getCommonHeldItem());
        writeByte(offset + Gen2Constants.bsRareHeldItemOffset, (byte) pkmn.getRareHeldItem());
        writeByte(offset + Gen2Constants.bsGrowthCurveOffset, pkmn.getGrowthCurve().toByte());
    }

    private String[] readPokemonNames() {
        int offs = romEntry.getIntValue("PokemonNamesOffset");
        int len = romEntry.getIntValue("PokemonNamesLength");
        String[] names = new String[Gen2Constants.pokemonCount + 1];
        for (int i = 1; i <= Gen2Constants.pokemonCount; i++) {
            names[i] = readFixedLengthString(offs + (i - 1) * len, len);
        }
        return names;
    }

    @Override
    public List<Pokemon> getStarters() {
        // Get the starters
        List<Pokemon> starters = new ArrayList<>();
        starters.add(pokes[rom[romEntry.getArrayValue("StarterOffsets1")[0]] & 0xFF]);
        starters.add(pokes[rom[romEntry.getArrayValue("StarterOffsets2")[0]] & 0xFF]);
        starters.add(pokes[rom[romEntry.getArrayValue("StarterOffsets3")[0]] & 0xFF]);
        return starters;
    }

    @Override
    public boolean setStarters(List<Pokemon> newStarters) {
        if (newStarters.size() != 3) {
            return false;
        }

        // Actually write

        for (int i = 0; i < 3; i++) {
            byte starter = (byte) newStarters.get(i).getNumber();
            int[] offsets = romEntry.getArrayValue("StarterOffsets" + (i + 1));
            for (int offset : offsets) {
                writeByte(offset, starter);
            }
        }

        // Attempt to replace text
        if (romEntry.getIntValue("CanChangeStarterText") > 0) {
            int[] starterTextOffsets = romEntry.getArrayValue("StarterTextOffsets");
            for (int i = 0; i < 3 && i < starterTextOffsets.length; i++) {
                writeVariableLengthString(String.format("%s?\\e", newStarters.get(i).getName()), starterTextOffsets[i], true);
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
        int[] shiOffsets = romEntry.getArrayValue("StarterHeldItems");
        for (int offset : shiOffsets) {
            sHeldItems.add(rom[offset] & 0xFF);
        }
        return sHeldItems;
    }

    @Override
    public void setStarterHeldItems(List<Integer> items) {
        int[] shiOffsets = romEntry.getArrayValue("StarterHeldItems");
        if (items.size() != shiOffsets.length) {
            return;
        }
        Iterator<Integer> sHeldItems = items.iterator();
        for (int offset : shiOffsets) {
            writeByte(offset, sHeldItems.next().byteValue());
        }
    }

    @Override
    public List<EncounterArea> getEncounters(boolean useTimeOfDay) {
        List<EncounterArea> encounterAreas = new ArrayList<>();

        readNormalEncounters(encounterAreas, useTimeOfDay);
        readFishingEncounters(encounterAreas, useTimeOfDay);
        readHeadbuttEncounters(encounterAreas);
        readBugCatchingContestEncounters(encounterAreas);

        return encounterAreas;
    }

    private void readNormalEncounters(List<EncounterArea> encounterAreas, boolean useTimeOfDay) {
        int offset = romEntry.getIntValue("WildPokemonOffset");
        offset = readLandEncounters(offset, encounterAreas, useTimeOfDay); // Johto
        offset = readSeaEncounters(offset, encounterAreas); // Johto
        offset = readLandEncounters(offset, encounterAreas, useTimeOfDay); // Kanto
        offset = readSeaEncounters(offset, encounterAreas); // Kanto
        offset = readLandEncounters(offset, encounterAreas, useTimeOfDay); // Specials
        readSeaEncounters(offset, encounterAreas); // Specials
    }

    private int readLandEncounters(int offset, List<EncounterArea> areas, boolean useTimeOfDay) {
        String[] todNames = new String[]{"Morning", "Day", "Night"};
        while ((rom[offset] & 0xFF) != 0xFF) {
            int mapBank = rom[offset] & 0xFF;
            int mapNumber = rom[offset + 1] & 0xFF;
            String mapName = mapNames[mapBank][mapNumber];
            if (useTimeOfDay) {
                for (int i = 0; i < 3; i++) {
                    EncounterArea area = new EncounterArea();
                    area.setRate(rom[offset + 2 + i] & 0xFF);
                    area.setDisplayName(mapName + " Grass/Cave (" + todNames[i] + ")");
                    for (int j = 0; j < Gen2Constants.landEncounterSlots; j++) {
                        Encounter enc = new Encounter();
                        enc.setLevel(rom[offset + 5 + (i * Gen2Constants.landEncounterSlots * 2) + (j * 2)] & 0xFF);
                        enc.setMaxLevel(0);
                        enc.setPokemon(pokes[rom[offset + 5 + (i * Gen2Constants.landEncounterSlots * 2) + (j * 2) + 1] & 0xFF]);
                        area.add(enc);
                    }
                    areas.add(area);
                }
            } else {
                // Use Day only
                EncounterArea area = new EncounterArea();
                area.setRate(rom[offset + 3] & 0xFF);
                area.setDisplayName(mapName + " Grass/Cave");
                for (int j = 0; j < Gen2Constants.landEncounterSlots; j++) {
                    Encounter enc = new Encounter();
                    enc.setLevel(rom[offset + 5 + Gen2Constants.landEncounterSlots * 2 + (j * 2)] & 0xFF);
                    enc.setMaxLevel(0);
                    enc.setPokemon(pokes[rom[offset + 5 + Gen2Constants.landEncounterSlots * 2 + (j * 2) + 1] & 0xFF]);
                    area.add(enc);
                }
                areas.add(area);
            }
            offset += 5 + 6 * Gen2Constants.landEncounterSlots;
        }
        return offset + 1;
    }

    private int readSeaEncounters(int offset, List<EncounterArea> areas) {
        while ((rom[offset] & 0xFF) != 0xFF) {
            int mapBank = rom[offset] & 0xFF;
            int mapNumber = rom[offset + 1] & 0xFF;
            String mapName = mapNames[mapBank][mapNumber];
            EncounterArea area = new EncounterArea();
            area.setRate(rom[offset + 2] & 0xFF);
            area.setDisplayName(mapName + " Surfing");
            for (int j = 0; j < Gen2Constants.seaEncounterSlots; j++) {
                Encounter enc = new Encounter();
                enc.setLevel(rom[offset + 3 + (j * 2)] & 0xFF);
                enc.setMaxLevel(0);
                enc.setPokemon(pokes[rom[offset + 3 + (j * 2) + 1] & 0xFF]);
                area.add(enc);
            }
            areas.add(area);
            offset += 3 + Gen2Constants.seaEncounterSlots * 2;
        }
        return offset + 1;
    }

    private void readFishingEncounters(List<EncounterArea> encounterAreas, boolean useTimeOfDay) {
        int offset = romEntry.getIntValue("FishingWildsOffset");
        int rootOffset = offset;
        for (int k = 0; k < Gen2Constants.fishingGroupCount; k++) {
            EncounterArea area = new EncounterArea();
            area.setDisplayName("Fishing Group " + (k + 1));
            for (int i = 0; i < Gen2Constants.pokesPerFishingGroup; i++) {
                offset++;
                int pokeNum = rom[offset++] & 0xFF;
                int level = rom[offset++] & 0xFF;
                if (pokeNum == 0) {
                    if (!useTimeOfDay) {
                        // read the encounter they put here for DAY
                        int specialOffset = rootOffset + Gen2Constants.fishingGroupEntryLength
                                * Gen2Constants.pokesPerFishingGroup * Gen2Constants.fishingGroupCount + level * 4 + 2;
                        Encounter enc = new Encounter();
                        enc.setPokemon(pokes[rom[specialOffset] & 0xFF]);
                        enc.setLevel(rom[specialOffset + 1] & 0xFF);
                        area.add(enc);
                    }
                    // else will be handled by code below
                } else {
                    Encounter enc = new Encounter();
                    enc.setPokemon(pokes[pokeNum]);
                    enc.setLevel(level);
                    area.add(enc);
                }
            }
            encounterAreas.add(area);
        }
        if (useTimeOfDay) {
            for (int k = 0; k < Gen2Constants.timeSpecificFishingGroupCount; k++) {
                EncounterArea area = new EncounterArea();
                area.setDisplayName("Time-Specific Fishing " + (k + 1));
                for (int i = 0; i < Gen2Constants.pokesPerTSFishingGroup; i++) {
                    int pokeNum = rom[offset++] & 0xFF;
                    int level = rom[offset++] & 0xFF;
                    Encounter enc = new Encounter();
                    enc.setPokemon(pokes[pokeNum]);
                    enc.setLevel(level);
                    area.add(enc);
                }
                encounterAreas.add(area);
            }
        }
    }

    private void readHeadbuttEncounters(List<EncounterArea> encounterAreas) {
        int offset = romEntry.getIntValue("HeadbuttWildsOffset");
        int limit = romEntry.getIntValue("HeadbuttTableSize");
        for (int i = 0; i < limit; i++) {
            EncounterArea area = new EncounterArea();
            area.setDisplayName("Headbutt Trees Set " + (i + 1));
            while ((rom[offset] & 0xFF) != 0xFF) {
                offset++;
                int pokeNum = rom[offset++] & 0xFF;
                int level = rom[offset++] & 0xFF;
                Encounter enc = new Encounter();
                enc.setPokemon(pokes[pokeNum]);
                enc.setLevel(level);
                area.add(enc);
            }
            offset++;
            encounterAreas.add(area);
        }
    }

    private void readBugCatchingContestEncounters(List<EncounterArea> encounterAreas) {
        int offset = romEntry.getIntValue("BCCWildsOffset");
        EncounterArea area = new EncounterArea();
        area.setDisplayName("Bug Catching Contest");
        while ((rom[offset] & 0xFF) != 0xFF) {
            offset++;
            Encounter enc = new Encounter();
            enc.setPokemon(pokes[rom[offset++] & 0xFF]);
            enc.setLevel(rom[offset++] & 0xFF);
            enc.setMaxLevel(rom[offset++] & 0xFF);
            area.add(enc);
        }
        // Unown is banned for Bug Catching Contest (5/8/2016)
        area.banPokemon(pokes[Species.unown]);
        encounterAreas.add(area);
    }

    @Override
    public void setEncounters(boolean useTimeOfDay, List<EncounterArea> encounters) {
        if (!havePatchedFleeing) {
            patchFleeing();
        }

        Iterator<EncounterArea> areaIterator = encounters.iterator();

        writeNormalEncounters(areaIterator, useTimeOfDay);
        writeFishingEncounters(areaIterator, useTimeOfDay);
        writeHeadbuttEncounters(areaIterator);
        writeBugCatchingContestEncounters(areaIterator);

    }

    private void writeNormalEncounters(Iterator<EncounterArea> areaIterator, boolean useTimeOfDay) {
        int offset = romEntry.getIntValue("WildPokemonOffset");
        offset = writeLandEncounters(offset, areaIterator, useTimeOfDay); // Johto
        offset = writeSeaEncounters(offset, areaIterator); // Johto
        offset = writeLandEncounters(offset, areaIterator, useTimeOfDay); // Kanto
        offset = writeSeaEncounters(offset, areaIterator); // Kanto
        offset = writeLandEncounters(offset, areaIterator, useTimeOfDay); // Specials
        writeSeaEncounters(offset, areaIterator); // Specials
    }

    private void writeFishingEncounters(Iterator<EncounterArea> areaIterator, boolean useTimeOfDay) {
        int offset;
        // Fishing Data
        offset = romEntry.getIntValue("FishingWildsOffset");
        for (int k = 0; k < Gen2Constants.fishingGroupCount; k++) {
            EncounterArea area = areaIterator.next();
            Iterator<Encounter> encounterIterator = area.iterator();
            for (int i = 0; i < Gen2Constants.pokesPerFishingGroup; i++) {
                offset++;
                if (rom[offset] == 0) {
                    if (!useTimeOfDay) {
                        // overwrite with a static encounter
                        Encounter enc = encounterIterator.next();
                        rom[offset++] = (byte) enc.getPokemon().getNumber();
                        rom[offset++] = (byte) enc.getLevel();
                    } else {
                        // else handle below
                        offset += 2;
                    }
                } else {
                    Encounter enc = encounterIterator.next();
                    rom[offset++] = (byte) enc.getPokemon().getNumber();
                    rom[offset++] = (byte) enc.getLevel();
                }
            }
        }
        if (useTimeOfDay) {
            for (int k = 0; k < Gen2Constants.timeSpecificFishingGroupCount; k++) {
                EncounterArea area = areaIterator.next();
                Iterator<Encounter> encounterIterator = area.iterator();
                for (int i = 0; i < Gen2Constants.pokesPerTSFishingGroup; i++) {
                    Encounter enc = encounterIterator.next();
                    rom[offset++] = (byte) enc.getPokemon().getNumber();
                    rom[offset++] = (byte) enc.getLevel();
                }
            }
        }
    }

    private void writeHeadbuttEncounters(Iterator<EncounterArea> areaIterator) {
        int offset;
        // Headbutt Data
        offset = romEntry.getIntValue("HeadbuttWildsOffset");
        int limit = romEntry.getIntValue("HeadbuttTableSize");
        for (int i = 0; i < limit; i++) {
            EncounterArea area = areaIterator.next();
            Iterator<Encounter> encounterIterator = area.iterator();
            while ((rom[offset] & 0xFF) != 0xFF) {
                Encounter enc = encounterIterator.next();
                offset++;
                rom[offset++] = (byte) enc.getPokemon().getNumber();
                rom[offset++] = (byte) enc.getLevel();
            }
            offset++;
        }
    }

    private void writeBugCatchingContestEncounters(Iterator<EncounterArea> areaIterator) {
        int offset;
        // Bug Catching Contest Data
        offset = romEntry.getIntValue("BCCWildsOffset");
        EncounterArea bugCatchingContestArea = areaIterator.next();
        Iterator<Encounter> bccEncounterIterator = bugCatchingContestArea.iterator();
        while ((rom[offset] & 0xFF) != 0xFF) {
            offset++;
            Encounter enc = bccEncounterIterator.next();
            rom[offset++] = (byte) enc.getPokemon().getNumber();
            rom[offset++] = (byte) enc.getLevel();
            rom[offset++] = (byte) enc.getMaxLevel();
        }
    }

    private int writeLandEncounters(int offset, Iterator<EncounterArea> areaIterator, boolean useTimeOfDay) {
        while ((rom[offset] & 0xFF) != 0xFF) {
            if (useTimeOfDay) {
                for (int i = 0; i < 3; i++) {
                    EncounterArea area = areaIterator.next();
                    Iterator<Encounter> encounterIterator = area.iterator();
                    for (int j = 0; j < Gen2Constants.landEncounterSlots; j++) {
                        Encounter enc = encounterIterator.next();
                        rom[offset + 5 + (i * Gen2Constants.landEncounterSlots * 2) + (j * 2)] = (byte) enc.getLevel();
                        rom[offset + 5 + (i * Gen2Constants.landEncounterSlots * 2) + (j * 2) + 1] = (byte) enc.getPokemon().getNumber();
                    }
                }
            } else {
                // Write the set to all 3 equally
                EncounterArea area = areaIterator.next();
                for (int i = 0; i < 3; i++) {
                    Iterator<Encounter> encounterIterator = area.iterator();
                    for (int j = 0; j < Gen2Constants.landEncounterSlots; j++) {
                        Encounter enc = encounterIterator.next();
                        rom[offset + 5 + (i * Gen2Constants.landEncounterSlots * 2) + (j * 2)] = (byte) enc.getLevel();
                        rom[offset + 5 + (i * Gen2Constants.landEncounterSlots * 2) + (j * 2) + 1] = (byte) enc.getPokemon().getNumber();
                    }
                }
            }
            offset += 5 + 6 * Gen2Constants.landEncounterSlots;
        }
        return offset + 1;
    }

    private int writeSeaEncounters(int offset, Iterator<EncounterArea> areaIterator) {
        while ((rom[offset] & 0xFF) != 0xFF) {
            EncounterArea area = areaIterator.next();
            Iterator<Encounter> encounterIterator = area.iterator();
            for (int j = 0; j < Gen2Constants.seaEncounterSlots; j++) {
                Encounter enc = encounterIterator.next();
                rom[offset + 3 + (j * 2)] = (byte) enc.getLevel();
                rom[offset + 3 + (j * 2) + 1] = (byte) enc.getPokemon().getNumber();
            }
            offset += 3 + Gen2Constants.seaEncounterSlots * 2;
        }
        return offset + 1;
    }

    @Override
    public List<Trainer> getTrainers() {
        if (trainers == null) {
            throw new IllegalStateException("Trainers have not been loaded.");
        }
        return trainers;
    }

    @Override
    // This is very similar to the implementation in Gen1RomHandler. As trainers is a private field though,
    // the two should only be reconciled during some bigger refactoring, where other private fields (e.g. pokemonList)
    // are considered.
    public void loadTrainers() {
        int trainerClassTableOffset = romEntry.getIntValue("TrainerDataTableOffset");
        int trainerClassAmount = romEntry.getIntValue("TrainerClassAmount");
        int[] trainersPerClass = romEntry.getArrayValue("TrainerDataClassCounts");
        List<String> tcnames = this.getTrainerClassNames();

        trainers = new ArrayList<>();

        int index = 0;
        for (int trainerClass = 0; trainerClass < trainerClassAmount; trainerClass++) {

            int offset = readPointer(trainerClassTableOffset + trainerClass * 2);

            for (int trainerNum = 0; trainerNum < trainersPerClass[trainerClass]; trainerNum++) {
                index++;
                Trainer tr = readTrainer(offset);
                tr.index = index;
                tr.trainerclass = trainerClass;
                tr.fullDisplayName = tcnames.get(trainerClass) + " " + tr.name;
                trainers.add(tr);

                offset += trainerToBytes(tr).length;
            }
        }

        tagTrainers();
    }

    private Trainer readTrainer(int offset) {
        Trainer tr = new Trainer();
        tr.offset = offset;
        tr.name = readVariableLengthString(offset, false);
        offset += lengthOfStringAt(offset, false);
        int dataType = rom[offset] & 0xFF;
        tr.poketype = dataType;
        offset++;
        while ((rom[offset] & 0xFF) != 0xFF) {
            //System.out.println(tr);
            TrainerPokemon tp = new TrainerPokemon();
            tp.level = rom[offset] & 0xFF;
            tp.pokemon = pokes[rom[offset + 1] & 0xFF];
            offset += 2;
            if ((dataType & 2) == 2) {
                tp.heldItem = rom[offset] & 0xFF;
                offset++;
            }
            if ((dataType & 1) == 1) {
                for (int move = 0; move < 4; move++) {
                    tp.moves[move] = rom[offset + move] & 0xFF;
                }
                offset += 4;
            }
            tr.pokemon.add(tp);
        }
        return tr;
    }

    private void tagTrainers() {
        Gen2Constants.universalTrainerTags(trainers);
        if (romEntry.isCrystal()) {
            Gen2Constants.crystalTags(trainers);
        } else {
            Gen2Constants.goldSilverTags(trainers);
        }
    }

    @Override
    public List<Integer> getMainPlaythroughTrainers() {
        return new ArrayList<>(); // Not implemented
    }

    @Override
    public List<Integer> getEliteFourTrainers(boolean isChallengeMode) {
        return new ArrayList<>(); // Not implemented
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

        int trainerClassTableOffset = romEntry.getIntValue("TrainerDataTableOffset");
        int trainerClassAmount = romEntry.getIntValue("TrainerClassAmount");
        int[] trainersPerClass = romEntry.getArrayValue("TrainerDataClassCounts");

        Iterator<Trainer> trainerIterator = getTrainers().iterator();
        for (int trainerClassNum = 0; trainerClassNum < trainerClassAmount; trainerClassNum++) {
            if (trainersPerClass[trainerClassNum] == 0) continue;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            for (int trainerNum = 0; trainerNum < trainersPerClass[trainerClassNum]; trainerNum++) {
                Trainer tr = trainerIterator.next();
                if (tr.trainerclass != trainerClassNum) {
                    System.err.println("Trainer mismatch: " + tr.name);
                }
                baos.writeBytes(trainerToBytes(tr));
                System.out.print(tr + " ");
            }
            System.out.print("\n");

            byte[] trainersOfClassBytes = baos.toByteArray();
            int pointerOffset = trainerClassTableOffset + trainerClassNum * 2;
            int trainersPerThisClass = trainersPerClass[trainerClassNum];
            new GBDataRewriter<byte[]>().rewriteData(pointerOffset, trainersOfClassBytes, b -> b, oldDataOffset ->
                    lengthOfTrainerClassAt(oldDataOffset, trainersPerThisClass));
        }
    }

    private byte[] trainerToBytes(Trainer trainer) {
        // sometimes it's practical to use a baos
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.writeBytes(trainerNameToBytes(trainer));
        baos.write(trainer.poketype);
        for (TrainerPokemon tp : trainer.pokemon) {
            baos.writeBytes(trainerPokemonToBytes(tp, trainer));
        }
        baos.write(Gen2Constants.trainerDataTerminator);

        return baos.toByteArray();
    }

    private byte[] trainerNameToBytes(Trainer trainer) {
        int trainerNameLength = internalStringLength(trainer.name) + 1;
        byte[] trainerNameBytes = new byte[trainerNameLength];
        writeFixedLengthString(trainerNameBytes, trainer.name, 0, trainerNameLength);
        return trainerNameBytes;
    }

    private byte[] trainerPokemonToBytes(TrainerPokemon tp, Trainer trainer) {
        byte[] data = new byte[trainerPokemonDataLength(trainer)];
        int offset = 0;
        data[offset] = (byte) tp.level;
        data[offset + 1] = (byte) tp.pokemon.getNumber();
        offset += 2;
        if (trainer.pokemonHaveItems()) {
            data[offset] = (byte) tp.heldItem;
            offset++;
        }
        if (trainer.pokemonHaveCustomMoves()) {
            if (tp.resetMoves) {
                resetTrainerPokemonMoves(tp);
            }
            data[offset] = (byte) tp.moves[0];
            data[offset + 1] = (byte) tp.moves[1];
            data[offset + 2] = (byte) tp.moves[2];
            data[offset + 3] = (byte) tp.moves[3];
        }
        return data;
    }

    private int trainerPokemonDataLength(Trainer trainer) {
        return 2 + (trainer.pokemonHaveItems() ? 1 : 0) + (trainer.pokemonHaveCustomMoves() ? 4 : 0);
    }

    private void resetTrainerPokemonMoves(TrainerPokemon tp) {
        // made quickly while refactoring trainer writing, might be applicable in more/better places
        // (including other gens)
        // TODO: look at the above
        tp.moves = RomFunctions.getMovesAtLevel(tp.pokemon.getNumber(), this.getMovesLearnt(), tp.level);
    }

    private int lengthOfTrainerClassAt(int offset, int numberOfTrainers) {
        int sum = 0;
        for (int i = 0; i < numberOfTrainers; i++) {
            Trainer trainer = readTrainer(offset);
            int trainerLength = trainerToBytes(trainer).length;
            sum += trainerLength;
            offset += trainerLength;
        }
        return sum;
    }

    @Override
    public boolean canAddPokemonToBossTrainers() {
        // because there isn't enough space in the bank with trainer data; the Japanese ROMs are smaller
        return romEntry.isNonJapanese();
    }

    @Override
    public boolean canAddPokemonToImportantTrainers() {
        // because there isn't enough space in the bank with trainer data; the Japanese ROMs are smaller
        return romEntry.isNonJapanese();
    }

    @Override
    public boolean canAddPokemonToRegularTrainers() {
        // because there isn't enough space in the bank with trainer data
        return false;
    }

    @Override
    public boolean supportsTrainerHeldItems() {
        // Not a technical issue nor a space-based one, Gen II does support held items for trainers.
        // Rather, getAllHeldItems() etc. needs to be filled. // TODO
        return false;
    }

    @Override
    public List<Pokemon> getPokemon() {
        return pokemonList;
    }

    @Override
    public List<Pokemon> getPokemonInclFormes() {
        return pokemonList;
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
    protected void loadMovesLearnt() {
        Map<Integer, List<MoveLearnt>> movesets = new TreeMap<>();
        int pointersOffset = romEntry.getIntValue("PokemonMovesetsTableOffset");
        for (int i = 1; i <= Gen2Constants.pokemonCount; i++) {
            int pointer = readPointer(pointersOffset + (i - 1) * 2);
            Pokemon pkmn = pokes[i];
            // Skip over evolution data
            while (rom[pointer] != 0) {
                if (rom[pointer] == 5) {
                    pointer += 4;
                } else {
                    pointer += 3;
                }
            }
            List<MoveLearnt> ourMoves = new ArrayList<>();
            pointer++;
            while (rom[pointer] != 0) {
                MoveLearnt learnt = new MoveLearnt();
                learnt.level = rom[pointer] & 0xFF;
                learnt.move = rom[pointer + 1] & 0xFF;
                ourMoves.add(learnt);
                pointer += 2;
            }
            movesets.put(pkmn.getNumber(), ourMoves);
        }
        setMovesLearnt(movesets);
    }

    @Override
    public Map<Integer, List<MoveLearnt>> getMovesLearnt() {
        return movesets;
    }

    @Override
    public void setMovesLearnt(Map<Integer, List<MoveLearnt>> movesets) {
        this.movesets = movesets;
    }

    @Override
    public List<Integer> getMovesBannedFromLevelup() {
        return Gen2Constants.bannedLevelupMoves;
    }

    @Override
    public Map<Integer, List<Integer>> getEggMoves() {
        Map<Integer, List<Integer>> eggMoves = new TreeMap<>();
        int tableOffset = romEntry.getIntValue("EggMovesTableOffset");
        for (int i = 1; i <= Gen2Constants.pokemonCount; i++) {
            int pointerOffset = tableOffset + (i - 1) * 2;
            int eggMoveOffset = readPointer(pointerOffset);
            List<Integer> moves = new ArrayList<>();
            int val = rom[eggMoveOffset] & 0xFF;
            while (val != 0xFF) {
                moves.add(val);
                eggMoveOffset++;
                val = rom[eggMoveOffset] & 0xFF;
            }
            if (moves.size() > 0) {
                eggMoves.put(i, moves);
            }
        }
        return eggMoves;
    }

    @Override
    public void setEggMoves(Map<Integer, List<Integer>> eggMoves) {
        int tableOffset = romEntry.getIntValue("EggMovesTableOffset");
        for (int i = 1; i <= Gen2Constants.pokemonCount; i++) {
            if (eggMoves.containsKey(i)) {
                System.out.println("\n" + getPokemon().get(i));
                int pointerOffset = tableOffset + (i - 1) * 2;
                new GBDataRewriter<List<Integer>>().rewriteData(pointerOffset, eggMoves.get(i), this::eggMovesToBytes,
                        oldDataOffset -> lengthOfDataWithTerminatorAt(oldDataOffset, Gen2Constants.eggMovesTerminator));
            }
        }
    }

    private byte[] eggMovesToBytes(List<Integer> eggMoves) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        eggMoves.forEach(baos::write);
        baos.write(Gen2Constants.eggMovesTerminator);
        return baos.toByteArray();
    }

    public static class StaticPokemon {
        protected int[] speciesOffsets;
        protected int[] levelOffsets;

        public StaticPokemon(int[] speciesOffsets, int[] levelOffsets) {
            this.speciesOffsets = speciesOffsets;
            this.levelOffsets = levelOffsets;
        }

        public Pokemon getPokemon(Gen2RomHandler rh) {
            return rh.pokes[rh.rom[speciesOffsets[0]] & 0xFF];
        }

        public void setPokemon(Gen2RomHandler rh, Pokemon pkmn) {
            for (int offset : speciesOffsets) {
                rh.writeByte(offset, (byte) pkmn.getNumber());
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

    public static class StaticPokemonGameCorner extends StaticPokemon {
        public StaticPokemonGameCorner(int[] speciesOffsets, int[] levelOffsets) {
            super(speciesOffsets, levelOffsets);
        }

        @Override
        public void setPokemon(Gen2RomHandler rh, Pokemon pkmn) {
            // Last offset is a pointer to the name
            int offsetSize = speciesOffsets.length;
            for (int i = 0; i < offsetSize - 1; i++) {
                rh.rom[speciesOffsets[i]] = (byte) pkmn.getNumber();
            }
            rh.writePaddedPokemonName(pkmn.getName(), rh.romEntry.getIntValue("GameCornerPokemonNameLength"),
                    speciesOffsets[offsetSize - 1]);
        }
    }

    @Override
    public List<StaticEncounter> getStaticPokemon() {
        List<StaticEncounter> statics = new ArrayList<>();
        int[] staticEggOffsets = romEntry.getArrayValue("StaticEggPokemonOffsets");
        if (romEntry.getIntValue("StaticPokemonSupport") > 0) {
            for (int i = 0; i < romEntry.getStaticPokemon().size(); i++) {
                int currentOffset = i;
                StaticPokemon sp = romEntry.getStaticPokemon().get(i);
                StaticEncounter se = new StaticEncounter();
                se.pkmn = sp.getPokemon(this);
                se.level = sp.getLevel(rom, 0);
                se.isEgg = Arrays.stream(staticEggOffsets).anyMatch(x -> x == currentOffset);
                statics.add(se);
            }
        }
        if (romEntry.getIntValue("StaticPokemonOddEggOffset") > 0) {
            int oeOffset = romEntry.getIntValue("StaticPokemonOddEggOffset");
            int oeSize = romEntry.getIntValue("StaticPokemonOddEggDataSize");
            for (int i = 0; i < Gen2Constants.oddEggPokemonCount; i++) {
                StaticEncounter se = new StaticEncounter();
                se.pkmn = pokes[rom[oeOffset + i * oeSize] & 0xFF];
                se.isEgg = true;
                statics.add(se);
            }
        }
        return statics;
    }

    @Override
    public boolean setStaticPokemon(List<StaticEncounter> staticPokemon) {
        if (romEntry.getIntValue("StaticPokemonSupport") == 0) {
            return false;
        }
        if (!havePatchedFleeing) {
            patchFleeing();
        }

        int desiredSize = romEntry.getStaticPokemon().size();
        if (romEntry.getIntValue("StaticPokemonOddEggOffset") > 0) {
            desiredSize += Gen2Constants.oddEggPokemonCount;
        }

        if (staticPokemon.size() != desiredSize) {
            return false;
        }

        Iterator<StaticEncounter> statics = staticPokemon.iterator();
        for (int i = 0; i < romEntry.getStaticPokemon().size(); i++) {
            StaticEncounter currentStatic = statics.next();
            StaticPokemon sp = romEntry.getStaticPokemon().get(i);
            sp.setPokemon(this, currentStatic.pkmn);
            sp.setLevel(rom, currentStatic.level, 0);
        }

        if (romEntry.getIntValue("StaticPokemonOddEggOffset") > 0) {
            int oeOffset = romEntry.getIntValue("StaticPokemonOddEggOffset");
            int oeSize = romEntry.getIntValue("StaticPokemonOddEggDataSize");
            for (int i = 0; i < Gen2Constants.oddEggPokemonCount; i++) {
                int oddEggPokemonNumber = statics.next().pkmn.getNumber();
                writeByte(oeOffset + i * oeSize, (byte) oddEggPokemonNumber);
                setMovesForOddEggPokemon(oddEggPokemonNumber, oeOffset + i * oeSize);
            }
        }

        return true;
    }

    // This method depends on movesets being randomized before static Pokemon. This is currently true,
    // but may not *always* be true, so take care.
    private void setMovesForOddEggPokemon(int oddEggPokemonNumber, int oddEggPokemonOffset) {
        // Determine the level 5 moveset, minus Dizzy Punch
        Map<Integer, List<MoveLearnt>> movesets = this.getMovesLearnt();
        List<Move> moves = this.getMoves();
        List<MoveLearnt> moveset = movesets.get(oddEggPokemonNumber);
        Queue<Integer> level5Moveset = new LinkedList<>();
        int currentMoveIndex = 0;
        while (moveset.size() > currentMoveIndex && moveset.get(currentMoveIndex).level <= 5) {
            if (level5Moveset.size() == 4) {
                level5Moveset.remove();
            }
            level5Moveset.add(moveset.get(currentMoveIndex).move);
            currentMoveIndex++;
        }

        // Now add Dizzy Punch and write the moveset and PP
        if (level5Moveset.size() == 4) {
            level5Moveset.remove();
        }
        level5Moveset.add(Moves.dizzyPunch);
        for (int i = 0; i < 4; i++) {
            int move = 0;
            int pp = 0;
            if (level5Moveset.size() > 0) {
                move = level5Moveset.remove();
                pp = moves.get(move).pp; // This assumes the ordering of moves matches the internal order
            }
            writeByte(oddEggPokemonOffset + 2 + i, (byte) move);
            writeByte(oddEggPokemonOffset + 23 + i, (byte) pp);
        }
    }

    @Override
    public PokemonSet<Pokemon> getBannedForWildEncounters() {
        // Ban Unown because they don't show up unless you complete a puzzle in the Ruins of Alph.
        return new PokemonSet<>(Collections.singletonList(pokes[Species.unown]));
    }

    @Override
    public boolean hasStaticAltFormes() {
        return false;
    }

    @Override
    public PokemonSet<Pokemon> getBannedForStaticPokemon() {
        return new PokemonSet<>(Collections.singletonList(pokes[Species.unown]));
    }

    @Override
    public boolean hasMainGameLegendaries() {
        return false;
    }

    @Override
    public List<Integer> getMainGameLegendaries() {
        return new ArrayList<>();
    }

    @Override
    public List<Integer> getSpecialMusicStatics() {
        return new ArrayList<>();
    }

    @Override
    public void applyCorrectStaticMusic(Map<Integer, Integer> specialMusicStaticChanges) {

    }

    @Override
    public boolean hasStaticMusicFix() {
        return false;
    }

    @Override
    public List<TotemPokemon> getTotemPokemon() {
        return new ArrayList<>();
    }

    @Override
    public void setTotemPokemon(List<TotemPokemon> totemPokemon) {

    }

    private void writePaddedPokemonName(String name, int length, int offset) {
        String paddedName = String.format("%-" + length + "s", name);
        byte[] rawData = translateString(paddedName);
        System.arraycopy(rawData, 0, rom, offset, length);
    }

    @Override
    public List<Integer> getTMMoves() {
        List<Integer> tms = new ArrayList<>();
        int offset = romEntry.getIntValue("TMMovesOffset");
        for (int i = 1; i <= Gen2Constants.tmCount; i++) {
            tms.add(rom[offset + (i - 1)] & 0xFF);
        }
        return tms;
    }

    @Override
    public List<Integer> getHMMoves() {
        List<Integer> hms = new ArrayList<>();
        int offset = romEntry.getIntValue("TMMovesOffset");
        for (int i = 1; i <= Gen2Constants.hmCount; i++) {
            hms.add(rom[offset + Gen2Constants.tmCount + (i - 1)] & 0xFF);
        }
        return hms;
    }

    @Override
    public void setTMMoves(List<Integer> moveIndexes) {
        int offset = romEntry.getIntValue("TMMovesOffset");
        for (int i = 1; i <= Gen2Constants.tmCount; i++) {
            rom[offset + (i - 1)] = moveIndexes.get(i - 1).byteValue();
        }

        // TM Text
        String[] moveNames = readMoveNames();
        for (GBCTMTextEntry tte : romEntry.getTMTexts()) {
            String moveName = moveNames[moveIndexes.get(tte.getNumber() - 1)];
            String text = tte.getTemplate().replace("%m", moveName);
            writeVariableLengthString(text, tte.getOffset(), true);
        }
    }

    @Override
    public int getTMCount() {
        return Gen2Constants.tmCount;
    }

    @Override
    public int getHMCount() {
        return Gen2Constants.hmCount;
    }

    @Override
    public Map<Pokemon, boolean[]> getTMHMCompatibility() {
        Map<Pokemon, boolean[]> compat = new TreeMap<>();
        for (int i = 1; i <= Gen2Constants.pokemonCount; i++) {
            int baseStatsOffset = romEntry.getIntValue("PokemonStatsOffset") + (i - 1) * Gen2Constants.baseStatsEntrySize;
            Pokemon pkmn = pokes[i];
            boolean[] flags = new boolean[Gen2Constants.tmCount + Gen2Constants.hmCount + 1];
            for (int j = 0; j < 8; j++) {
                readByteIntoFlags(flags, j * 8 + 1, baseStatsOffset + Gen2Constants.bsTMHMCompatOffset + j);
            }
            compat.put(pkmn, flags);
        }
        return compat;
    }

    @Override
    public void setTMHMCompatibility(Map<Pokemon, boolean[]> compatData) {
        for (Map.Entry<Pokemon, boolean[]> compatEntry : compatData.entrySet()) {
            Pokemon pkmn = compatEntry.getKey();
            boolean[] flags = compatEntry.getValue();
            int baseStatsOffset = romEntry.getIntValue("PokemonStatsOffset") + (pkmn.getNumber() - 1)
                    * Gen2Constants.baseStatsEntrySize;
            for (int j = 0; j < 8; j++) {
                if (!romEntry.isCrystal() || j != 7) {
                    writeByte(baseStatsOffset + Gen2Constants.bsTMHMCompatOffset + j,
                            getByteFromFlags(flags, j * 8 + 1));
                } else {
                    // Move tutor data
                    // bits 1,2,3 of byte 7
                    int changedByte = getByteFromFlags(flags, j * 8 + 1) & 0xFF;
                    int currentByte = rom[baseStatsOffset + Gen2Constants.bsTMHMCompatOffset + j];
                    changedByte |= ((currentByte >> 1) & 0x01) << 1;
                    changedByte |= ((currentByte >> 2) & 0x01) << 2;
                    changedByte |= ((currentByte >> 3) & 0x01) << 3;
                    writeByte(baseStatsOffset + 0x18 + j, (byte) changedByte);
                }
            }
        }
    }

    @Override
    public boolean hasMoveTutors() {
        return romEntry.isCrystal();
    }

    @Override
    public List<Integer> getMoveTutorMoves() {
        if (romEntry.isCrystal()) {
            List<Integer> mtMoves = new ArrayList<>();
            for (int offset : romEntry.getArrayValue("MoveTutorMoves")) {
                mtMoves.add(rom[offset] & 0xFF);
            }
            return mtMoves;
        }
        return new ArrayList<>();
    }

    @Override
    public void setMoveTutorMoves(List<Integer> moves) {
        if (!romEntry.isCrystal()) {
            return;
        }
        if (moves.size() != Gen2Constants.mtCount) {
            throw new IllegalArgumentException("Wrong number of move tutor moves. Should be " +
                    Gen2Constants.mtCount + ", is " + moves.size() + ".");
        }
        int menuPointerOffset = romEntry.getIntValue("MoveTutorMenuOffset");
        if (menuPointerOffset <= 0) {
            throw new IllegalStateException("ROM does not support move tutor randomization.");
        }

        Iterator<Integer> mvList = moves.iterator();
        for (int offset : romEntry.getArrayValue("MoveTutorMoves")) {
            writeByte(offset, mvList.next().byteValue());
        }

        new GBDataRewriter<List<Integer>>().rewriteData(menuPointerOffset, moves,
                this::moveTutorMovesToDialogueOptionBytes, this::lengthOfDialogueOptionAt);
    }

    private byte[] moveTutorMovesToDialogueOptionBytes(List<Integer> moves) {
        String[] moveNames = readMoveNames();
        String[] options = new String[]{moveNames[moves.get(0)], moveNames[moves.get(1)], moveNames[moves.get(2)],
                romEntry.getStringValue("CancelString")};
        return dialogueOptionToBytes(options);
    }

    private byte[] dialogueOptionToBytes(String[] options) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(Gen2Constants.dialogueOptionInitByte);
        baos.write(options.length);
        for (String option : options) {
            baos.writeBytes(translateString(option));
            baos.write(GBConstants.stringTerminator);
        }
        System.out.println(RomFunctions.bytesToHex(baos.toByteArray()));
        return baos.toByteArray();
    }

    private int lengthOfDialogueOptionAt(int offset) {
        if (rom[offset] != Gen2Constants.dialogueOptionInitByte) {
            throw new IllegalArgumentException("There is either no dialogue option at " + offset +
                    ", or it is in a format not supported by the randomizer.");
        }
        int numberOfOptions = rom[offset + 1];
        int length = 2;
        length += lengthOfDataWithTerminatorsAt(offset + length, GBConstants.stringTerminator,
                numberOfOptions);
        return length;
    }

    @Override
    public Map<Pokemon, boolean[]> getMoveTutorCompatibility() {
        if (!romEntry.isCrystal()) {
            return new TreeMap<>();
        }
        Map<Pokemon, boolean[]> compat = new TreeMap<>();
        for (int i = 1; i <= Gen2Constants.pokemonCount; i++) {
            int baseStatsOffset = romEntry.getIntValue("PokemonStatsOffset") + (i - 1) * Gen2Constants.baseStatsEntrySize;
            Pokemon pkmn = pokes[i];
            boolean[] flags = new boolean[4];
            int mtByte = rom[baseStatsOffset + Gen2Constants.bsMTCompatOffset] & 0xFF;
            for (int j = 1; j <= 3; j++) {
                flags[j] = ((mtByte >> j) & 0x01) > 0;
            }
            compat.put(pkmn, flags);
        }
        return compat;
    }

    @Override
    public void setMoveTutorCompatibility(Map<Pokemon, boolean[]> compatData) {
        if (!romEntry.isCrystal()) {
            return;
        }
        for (Map.Entry<Pokemon, boolean[]> compatEntry : compatData.entrySet()) {
            Pokemon pkmn = compatEntry.getKey();
            boolean[] flags = compatEntry.getValue();
            int baseStatsOffset = romEntry.getIntValue("PokemonStatsOffset") + (pkmn.getNumber() - 1)
                    * Gen2Constants.baseStatsEntrySize;
            int origMtByte = rom[baseStatsOffset + Gen2Constants.bsMTCompatOffset] & 0xFF;
            int mtByte = origMtByte & 0x01;
            for (int j = 1; j <= 3; j++) {
                mtByte |= flags[j] ? (1 << j) : 0;
            }
            writeByte(baseStatsOffset + Gen2Constants.bsMTCompatOffset, (byte) mtByte);
        }
    }

    @Override
    public String getROMName() {
        if (isVietCrystal) {
            return Gen2Constants.vietCrystalROMName;
        }
        return "Pokemon " + romEntry.getName();
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

    @Override
    public boolean hasTimeBasedEncounters() {
        return true; // All GSC do
    }

    @Override
    public boolean hasWildAltFormes() {
        return false;
    }

    @Override
    public void loadEvolutions() {
        for (Pokemon pkmn : pokes) {
            if (pkmn != null) {
                pkmn.getEvolutionsFrom().clear();
                pkmn.getEvolutionsTo().clear();
            }
        }

        int pointersOffset = romEntry.getIntValue("PokemonMovesetsTableOffset");
        for (int i = 1; i <= Gen2Constants.pokemonCount; i++) {
            int pointer = readPointer(pointersOffset + (i - 1) * 2);
            Pokemon pkmn = pokes[i];
            while (rom[pointer] != 0) {
                int method = rom[pointer] & 0xFF;
                int otherPoke = rom[pointer + 2 + (method == 5 ? 1 : 0)] & 0xFF;
                EvolutionType type = EvolutionType.fromIndex(2, method);
                int extraInfo = 0;
                if (type == EvolutionType.TRADE) {
                    int itemNeeded = rom[pointer + 1] & 0xFF;
                    if (itemNeeded != 0xFF) {
                        type = EvolutionType.TRADE_ITEM;
                        extraInfo = itemNeeded;
                    }
                } else if (type == EvolutionType.LEVEL_ATTACK_HIGHER) {
                    int tyrogueCond = rom[pointer + 2] & 0xFF;
                    if (tyrogueCond == 2) {
                        type = EvolutionType.LEVEL_DEFENSE_HIGHER;
                    } else if (tyrogueCond == 3) {
                        type = EvolutionType.LEVEL_ATK_DEF_SAME;
                    }
                    extraInfo = rom[pointer + 1] & 0xFF;
                } else if (type == EvolutionType.HAPPINESS) {
                    int happCond = rom[pointer + 1] & 0xFF;
                    if (happCond == 2) {
                        type = EvolutionType.HAPPINESS_DAY;
                    } else if (happCond == 3) {
                        type = EvolutionType.HAPPINESS_NIGHT;
                    }
                } else {
                    extraInfo = rom[pointer + 1] & 0xFF;
                }
                Evolution evo = new Evolution(pokes[i], pokes[otherPoke], true, type, extraInfo);
                if (!pkmn.getEvolutionsFrom().contains(evo)) {
                    pkmn.getEvolutionsFrom().add(evo);
                    pokes[otherPoke].getEvolutionsTo().add(evo);
                }
                pointer += (method == 5 ? 4 : 3);
            }
            // split evos don't carry stats
            if (pkmn.getEvolutionsFrom().size() > 1) {
                for (Evolution e : pkmn.getEvolutionsFrom()) {
                    e.carryStats = false;
                }
            }
        }
    }

    @Override
    public void removeImpossibleEvolutions(Settings settings) {
        // no move evos, so no need to check for those
        for (Pokemon pkmn : pokes) {
            if (pkmn != null) {
                for (Evolution evol : pkmn.getEvolutionsFrom()) {
                    if (evol.type == EvolutionType.TRADE || evol.type == EvolutionType.TRADE_ITEM) {
                        // change
                        if (evol.from.getNumber() == Species.slowpoke) {
                            // Slowpoke: Make water stone => Slowking
                            evol.type = EvolutionType.STONE;
                            evol.extraInfo = Gen2Items.waterStone;
                            addEvoUpdateStone(impossibleEvolutionUpdates, evol, itemNames[24]);
                        } else if (evol.from.getNumber() == Species.seadra) {
                            // Seadra: level 40
                            evol.type = EvolutionType.LEVEL;
                            evol.extraInfo = 40; // level
                            addEvoUpdateLevel(impossibleEvolutionUpdates, evol);
                        } else if (evol.from.getNumber() == Species.poliwhirl || evol.type == EvolutionType.TRADE) {
                            // Poliwhirl or any of the original 4 trade evos
                            // Level 37
                            evol.type = EvolutionType.LEVEL;
                            evol.extraInfo = 37; // level
                            addEvoUpdateLevel(impossibleEvolutionUpdates, evol);
                        } else {
                            // A new trade evo of a single stage Pokemon
                            // level 30
                            evol.type = EvolutionType.LEVEL;
                            evol.extraInfo = 30; // level
                            addEvoUpdateLevel(impossibleEvolutionUpdates, evol);
                        }
                    }
                }
            }
        }

    }

    @Override
    public void makeEvolutionsEasier(Settings settings) {
        // Reduce the amount of happiness required to evolve.
        int offset = find(rom, Gen2Constants.friendshipValueForEvoLocator);
        if (offset > 0) {
            // The thing we're looking at is actually one byte before what we
            // want to change; this makes it work in both G/S and Crystal.
            offset++;

            // Amount of required happiness for all happiness evolutions.
            if (rom[offset] == (byte) GlobalConstants.vanillaHappinessToEvolve) {
                writeByte(offset, (byte) GlobalConstants.easierHappinessToEvolve);
            }
        }
    }

    @Override
    public void removeTimeBasedEvolutions() {
        for (Pokemon pkmn : pokes) {
            if (pkmn != null) {
                for (Evolution evol : pkmn.getEvolutionsFrom()) {
                    // In Gen 2, only Eevee has a time-based evolution.
                    if (evol.type == EvolutionType.HAPPINESS_DAY) {
                        // Eevee: Make sun stone => Espeon
                        evol.type = EvolutionType.STONE;
                        evol.extraInfo = Gen2Items.sunStone;
                        addEvoUpdateStone(timeBasedEvolutionUpdates, evol, itemNames[169]);
                    } else if (evol.type == EvolutionType.HAPPINESS_NIGHT) {
                        // Eevee: Make moon stone => Umbreon
                        evol.type = EvolutionType.STONE;
                        evol.extraInfo = Gen2Items.moonStone;
                        addEvoUpdateStone(timeBasedEvolutionUpdates, evol, itemNames[8]);
                    }
                }
            }
        }

    }

    @Override
    public boolean hasShopRandomization() {
        return true; // obviously not yet, but needed for testing
    }

    @Override
    public Map<Integer, Shop> getShopItems() {
        List<Shop> shops = readShops();

        // TODO no notion of maingame/skip shops (the Mahogany pre-rocket shop should be a skip)
        Map<Integer, Shop> shopMap = new HashMap<>();
        for (int i = 0; i < shops.size(); i++) {
            shopMap.put(i, shops.get(i));
        }
        return shopMap;
    }

    private List<Shop> readShops() {
        List<Shop> shops = new ArrayList<>();

        int tableOffset = romEntry.getIntValue("ShopItemOffset");
        int shopAmount = romEntry.getIntValue("ShopAmount");
        int shopNum = 0;
        while (shopNum < shopAmount) {
            int shopOffset = readPointer(tableOffset + shopNum * 2, bankOf(tableOffset));
            Shop shop = readShop(shopOffset);
            shop.name = Gen2Constants.shopNames.get(shopNum);
            shops.add(shop);
            shopNum++;
        }
        return shops;
    }

    private Shop readShop(int offset) {
        Shop shop = new Shop();
        shop.items = new ArrayList<>();
        int itemAmount = rom[offset++];
        for (int itemNum = 0; itemNum < itemAmount; itemNum++) {
            shop.items.add((int) rom[offset++] & 0xFF);
        }
        if (rom[offset] != Gen2Constants.shopItemsTerminator) {
            throw new RandomizerIOException("Invalid shop data");
        }
        return shop;
    }

    @Override
    public void setShopItems(Map<Integer, Shop> shopItems) {
        int tableOffset = romEntry.getIntValue("ShopItemOffset");

        for (Map.Entry<Integer, Shop> entry : shopItems.entrySet()) {
            int shopNum = entry.getKey();
            Shop shop = entry.getValue();
            new GBDataRewriter<Shop>().rewriteData(tableOffset + shopNum * 2, shop, this::shopToBytes,
                    this::lengthOfShopAt);
        }
    }

    private byte[] shopToBytes(Shop shop) {
        byte[] data = new byte[shop.items.size() + 2];
        data[0] = (byte) shop.items.size();
        for (int i = 0; i < shop.items.size(); i++) {
            data[i + 1] = (byte) (shop.items.get(i) & 0xFF);
        }
        data[data.length - 1] = (byte) 0xFF;
        return data;
    }

    private int lengthOfShopAt(int offset) {
        return shopToBytes(readShop(offset)).length;
    }

    @Override
    public void setShopPrices() {
        // TODO: Not implemented
    }

    @Override
    public boolean canChangeTrainerText() {
        return romEntry.getIntValue("CanChangeTrainerText") > 0;
    }

    @Override
    public TrainerNameMode trainerNameMode() {
        return TrainerNameMode.MAX_LENGTH_WITH_CLASS;
    }

    @Override
    public int maxTrainerNameLength() {
        // line size minus one for space
        return Gen2Constants.maxTrainerNameLength;
    }

    @Override
    public int maxSumOfTrainerNameLengths() {
        return romEntry.getIntValue("MaxSumOfTrainerNameLengths");
    }

    @Override
    public List<Integer> getTCNameLengthsByTrainer() {
        int traineramount = romEntry.getIntValue("TrainerClassAmount");
        int[] trainerclasslimits = romEntry.getArrayValue("TrainerDataClassCounts");
        List<String> tcNames = this.getTrainerClassNames();
        List<Integer> tcLengthsByT = new ArrayList<>();

        for (int i = 0; i < traineramount; i++) {
            int len = internalStringLength(tcNames.get(i));
            for (int k = 0; k < trainerclasslimits[i]; k++) {
                tcLengthsByT.add(len);
            }
        }

        return tcLengthsByT;
    }

    @Override
    public List<String> getTrainerClassNames() {
        int amount = romEntry.getIntValue("TrainerClassAmount");
        int offset = romEntry.getIntValue("TrainerClassNamesOffset");
        List<String> trainerClassNames = new ArrayList<>();
        for (int j = 0; j < amount; j++) {
            String name = readVariableLengthString(offset, false);
            offset += lengthOfStringAt(offset, false);
            trainerClassNames.add(name);
        }
        return trainerClassNames;
    }

    @Override
    public List<Integer> getEvolutionItems() {
        return Gen2Constants.evolutionItems;
    }

    @Override
    public List<Integer> getXItems() {
        return Gen2Constants.xItems;
    }

    @Override
    public void setTrainerClassNames(List<String> trainerClassNames) {
        if (romEntry.getIntValue("CanChangeTrainerText") != 0) {
            int amount = romEntry.getIntValue("TrainerClassAmount");
            int offset = romEntry.getIntValue("TrainerClassNamesOffset");
            Iterator<String> trainerClassNamesI = trainerClassNames.iterator();
            for (int j = 0; j < amount; j++) {
                int len = lengthOfStringAt(offset, false);
                String newName = trainerClassNamesI.next();
                writeFixedLengthString(newName, offset, len);
                offset += len;
            }
        }
    }

    @Override
    public boolean fixedTrainerClassNamesLength() {
        return true;
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
    public String getDefaultExtension() {
        return "gbc";
    }

    @Override
    public int abilitiesPerPokemon() {
        return 0;
    }

    @Override
    public int highestAbilityIndex() {
        return 0;
    }

    @Override
    public Map<Integer, List<Integer>> getAbilityVariations() {
        return new HashMap<>();
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
    public int miscTweaksAvailable() {
        int available = MiscTweak.LOWER_CASE_POKEMON_NAMES.getValue();
        available |= MiscTweak.UPDATE_TYPE_EFFECTIVENESS.getValue();
        if (romEntry.hasTweakFile("BWXPTweak")) {
            available |= MiscTweak.BW_EXP_PATCH.getValue();
        }
        if (romEntry.getIntValue("TextDelayFunctionOffset") != 0) {
            available |= MiscTweak.FASTEST_TEXT.getValue();
        }
        if (romEntry.getArrayValue("CatchingTutorialOffsets").length != 0) {
            available |= MiscTweak.RANDOMIZE_CATCHING_TUTORIAL.getValue();
        }
        available |= MiscTweak.BAN_LUCKY_EGG.getValue();
        return available;
    }

    @Override
    public void applyMiscTweak(MiscTweak tweak) {
        if (tweak == MiscTweak.BW_EXP_PATCH) {
            applyBWEXPPatch();
        } else if (tweak == MiscTweak.FASTEST_TEXT) {
            applyFastestTextPatch();
        } else if (tweak == MiscTweak.LOWER_CASE_POKEMON_NAMES) {
            applyCamelCaseNames();
        } else if (tweak == MiscTweak.RANDOMIZE_CATCHING_TUTORIAL) {
            randomizeCatchingTutorial();
        } else if (tweak == MiscTweak.BAN_LUCKY_EGG) {
            allowedItems.banSingles(Gen2Items.luckyEgg);
            nonBadItems.banSingles(Gen2Items.luckyEgg);
        } else if (tweak == MiscTweak.UPDATE_TYPE_EFFECTIVENESS) {
            updateTypeEffectiveness();
        }
    }

    @Override
    public boolean isEffectivenessUpdated() {
        return effectivenessUpdated;
    }

    private void randomizeCatchingTutorial() {
        if (romEntry.getArrayValue("CatchingTutorialOffsets").length != 0) {
            // Pick a pokemon
            int pokemon = this.random.nextInt(Gen2Constants.pokemonCount) + 1;
            while (pokemon == Species.unown) {
                // Unown is banned
                pokemon = this.random.nextInt(Gen2Constants.pokemonCount) + 1;
            }

            int[] offsets = romEntry.getArrayValue("CatchingTutorialOffsets");
            for (int offset : offsets) {
                writeByte(offset, (byte) pokemon);
            }
        }

    }

    private void applyBWEXPPatch() {
        if (!romEntry.hasTweakFile("BWXPTweak")) {
            return;
        }
        String patchName = romEntry.getTweakFile("BWXPTweak");
        try {
            FileFunctions.applyPatch(rom, patchName);
        } catch (IOException e) {
            throw new RandomizerIOException(e);
        }
    }

    private void applyFastestTextPatch() {
        if (romEntry.getIntValue("TextDelayFunctionOffset") != 0) {
            writeByte(romEntry.getIntValue("TextDelayFunctionOffset"), GBConstants.gbZ80Ret);
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
            Type attacking = Gen2Constants.typeTable[attackingType];
            Type defending = Gen2Constants.typeTable[defendingType];
            Effectiveness effectiveness = switch (effectivenessInternal) {
                case 20 -> Effectiveness.DOUBLE;
                case 10 -> Effectiveness.NEUTRAL;
                case 5 -> Effectiveness.HALF;
                case 0 -> Effectiveness.ZERO;
                default -> null;
            };
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
        int tableOffset = romEntry.getIntValue("TypeEffectivenessOffset");
        for (int i = 0; i < typeEffectivenessTable.size(); i++) {
            byte[] relationshipBytes = typeRelationShipToBytes(typeEffectivenessTable.get(i));
            int relationshipOffset = tableOffset + i * 3; // this "3" could be a constant
            writeBytes(relationshipOffset, relationshipBytes);
        }
    }

    private byte[] typeRelationShipToBytes(TypeRelationship relationship) {
        byte effectivenessInternal = switch (relationship.effectiveness) {
            case DOUBLE -> 20;
            case NEUTRAL -> 10;
            case HALF -> 5;
            default -> 0;
        };
        return new byte[]{Gen2Constants.typeToByte(relationship.attacker), Gen2Constants.typeToByte(relationship.defender),
                effectivenessInternal};
    }

    @Override
    public void enableGuaranteedPokemonCatching() {
        String prefix = romEntry.getStringValue("GuaranteedCatchPrefix");
        int offset = find(rom, prefix);
        if (offset > 0) {
            offset += prefix.length() / 2; // because it was a prefix

            // The game guarantees that the catching tutorial always succeeds in catching by running
            // the following code:
            // ld a, [wBattleType]
            // cp BATTLETYPE_TUTORIAL
            // jp z, .catch_without_fail
            // By making the jump here unconditional, we can ensure that catching always succeeds no
            // matter the battle type. We check that the original condition is present just for safety.
            if (rom[offset] == (byte) 0xCA) {
                writeByte(offset, (byte) 0xC3);
            }
        }
    }

    @Override
    public void randomizeIntroPokemon() {
        // Intro sprite

        // Pick a pokemon
        int pokemon = this.random.nextInt(Gen2Constants.pokemonCount) + 1;
        while (pokemon == Species.unown) {
            // Unown is banned
            pokemon = this.random.nextInt(Gen2Constants.pokemonCount) + 1;
        }

        writeByte(romEntry.getIntValue("IntroSpriteOffset"), (byte) pokemon);
        writeByte(romEntry.getIntValue("IntroCryOffset"), (byte) pokemon);

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
        return null; // TODO: Not implemented
    }

    @Override
    public List<Integer> getOPShopItems() {
        return null; // TODO: Not implemented
    }

    @Override
    public void loadItemNames() {
        itemNames = new String[256];
        itemNames[0] = "glitch";
        // trying to emulate pretty much what the game does here
        // normal items
        int origOffset = romEntry.getIntValue("ItemNamesOffset");
        int itemNameOffset = origOffset;
        for (int index = 1; index <= 0x100; index++) {
            if (itemNameOffset / GBConstants.bankSize > origOffset / GBConstants.bankSize) {
                // the game would continue making its merry way into VRAM here,
                // but we don't have VRAM to simulate.
                // just give up.
                break;
            }
            int startOfText = itemNameOffset;
            while ((rom[itemNameOffset] & 0xFF) != GBConstants.stringTerminator) {
                itemNameOffset++;
            }
            itemNameOffset++;
            itemNames[index % 256] = readFixedLengthString(startOfText, 20);
        }
    }

    @Override
    public String[] getItemNames() {
        return itemNames;
    }

    private void patchFleeing() {
        havePatchedFleeing = true;
        int offset = romEntry.getIntValue("FleeingDataOffset");
        writeByte(offset, (byte) 0xFF);
        writeByte(offset + Gen2Constants.fleeingSetTwoOffset, (byte) 0xFF);
        writeByte(offset + Gen2Constants.fleeingSetThreeOffset, (byte) 0xFF);
    }

    private void loadLandmarkNames() {

        int lmOffset = romEntry.getIntValue("LandmarkTableOffset");
        int lmCount = romEntry.getIntValue("LandmarkCount");

        landmarkNames = new String[lmCount];

        for (int i = 0; i < lmCount; i++) {
            int lmNameOffset = readPointer(lmOffset + i * 4 + 2);
            landmarkNames[i] = readVariableLengthString(lmNameOffset, false).replace("\\x1F", " ");
        }

    }

    private void preprocessMaps() {
        itemOffs = new ArrayList<>();

        int mhOffset = romEntry.getIntValue("MapHeaders");
        int mapGroupCount = Gen2Constants.mapGroupCount;
        int mapsInLastGroup = Gen2Constants.mapsInLastGroup;
        int mhBank = bankOf(mhOffset);
        mapNames = new String[mapGroupCount + 1][100];

        int[] groupOffsets = new int[mapGroupCount];
        for (int i = 0; i < mapGroupCount; i++) {
            groupOffsets[i] = readPointer(mhOffset + i * 2);
        }

        // Read maps
        for (int mg = 0; mg < mapGroupCount; mg++) {
            int offset = groupOffsets[mg];
            int maxOffset = (mg == mapGroupCount - 1) ? (mhBank + 1) * GBConstants.bankSize : groupOffsets[mg + 1];
            int map = 0;
            int maxMap = (mg == mapGroupCount - 1) ? mapsInLastGroup : Integer.MAX_VALUE;
            while (offset < maxOffset && map < maxMap) {
                processMapAt(offset, mg + 1, map + 1);
                offset += 9;
                map++;
            }
        }
    }

    private void processMapAt(int offset, int mapBank, int mapNumber) {

        // second map header
        int smhBank = rom[offset] & 0xFF;
        int smhOffset = readPointer(offset + 3, smhBank);

        // map name
        int mapLandmark = rom[offset + 5] & 0xFF;
        mapNames[mapBank][mapNumber] = landmarkNames[mapLandmark];

        // event header
        // event header is in same bank as script header
        int ehBank = rom[smhOffset + 6] & 0xFF;
        int ehOffset = readPointer(smhOffset + 9, ehBank);

        // skip over filler
        ehOffset += 2;

        // warps
        int warpCount = rom[ehOffset++] & 0xFF;
        // warps are skipped
        ehOffset += warpCount * 5;

        // xy triggers
        int triggerCount = rom[ehOffset++] & 0xFF;
        // xy triggers are skipped
        ehOffset += triggerCount * 8;

        // signposts
        int signpostCount = rom[ehOffset++] & 0xFF;
        // we do care about these
        for (int sp = 0; sp < signpostCount; sp++) {
            // type=7 are hidden items
            int spType = rom[ehOffset + sp * 5 + 2] & 0xFF;
            if (spType == 7) {
                // get event pointer
                int spOffset = readPointer(ehOffset + sp * 5 + 3, ehBank);
                // item is at spOffset+2 (first two bytes are the flag id)
                itemOffs.add(spOffset + 2);
            }
        }
        // now skip past them
        ehOffset += signpostCount * 5;

        // visible objects/people
        int peopleCount = rom[ehOffset++] & 0xFF;
        // we also care about these
        for (int p = 0; p < peopleCount; p++) {
            // color_function & 1 = 1 if itemball
            int pColorFunction = rom[ehOffset + p * 13 + 7];
            if ((pColorFunction & 1) == 1) {
                // get event pointer
                int pOffset = readPointer(ehOffset + p * 13 + 9, ehBank);
                // item is at the pOffset for non-hidden items
                itemOffs.add(pOffset);
            }
        }

    }

    @Override
    public List<Integer> getRequiredFieldTMs() {
        return Gen2Constants.requiredFieldTMs;
    }

    @Override
    public List<Integer> getCurrentFieldTMs() {
        List<Integer> fieldTMs = new ArrayList<>();

        for (int offset : itemOffs) {
            int itemHere = rom[offset] & 0xFF;
            if (Gen2Constants.allowedItems.isTM(itemHere)) {
                int thisTM;
                if (itemHere >= Gen2Constants.tmBlockOneIndex
                        && itemHere < Gen2Constants.tmBlockOneIndex + Gen2Constants.tmBlockOneSize) {
                    thisTM = itemHere - Gen2Constants.tmBlockOneIndex + 1;
                } else if (itemHere >= Gen2Constants.tmBlockTwoIndex
                        && itemHere < Gen2Constants.tmBlockTwoIndex + Gen2Constants.tmBlockTwoSize) {
                    thisTM = itemHere - Gen2Constants.tmBlockTwoIndex + 1 + Gen2Constants.tmBlockOneSize; // TM
                    // block
                    // 2
                    // offset
                } else {
                    thisTM = itemHere - Gen2Constants.tmBlockThreeIndex + 1 + Gen2Constants.tmBlockOneSize
                            + Gen2Constants.tmBlockTwoSize; // TM block 3 offset
                }
                // hack for the bug catching contest repeat TM28
                if (!fieldTMs.contains(thisTM)) {
                    fieldTMs.add(thisTM);
                }
            }
        }
        return fieldTMs;
    }

    @Override
    public void setFieldTMs(List<Integer> fieldTMs) {
        Iterator<Integer> iterTMs = fieldTMs.iterator();
        int[] givenTMs = new int[256];

        for (int offset : itemOffs) {
            int itemHere = rom[offset] & 0xFF;
            if (Gen2Constants.allowedItems.isTM(itemHere)) {
                // Cache replaced TMs to duplicate bug catching contest TM
                if (givenTMs[itemHere] != 0) {
                    writeByte(offset, (byte) givenTMs[itemHere]);
                } else {
                    // Replace this with a TM from the list
                    int tm = iterTMs.next();
                    if (tm >= 1 && tm <= Gen2Constants.tmBlockOneSize) {
                        tm += Gen2Constants.tmBlockOneIndex - 1;
                    } else if (tm >= Gen2Constants.tmBlockOneSize + 1
                            && tm <= Gen2Constants.tmBlockOneSize + Gen2Constants.tmBlockTwoSize) {
                        tm += Gen2Constants.tmBlockTwoIndex - 1 - Gen2Constants.tmBlockOneSize;
                    } else {
                        tm += Gen2Constants.tmBlockThreeIndex - 1 - Gen2Constants.tmBlockOneSize
                                - Gen2Constants.tmBlockTwoSize;
                    }
                    givenTMs[itemHere] = tm;
                    writeByte(offset, (byte) tm);
                }
            }
        }
    }

    @Override
    public List<Integer> getRegularFieldItems() {
        List<Integer> fieldItems = new ArrayList<>();

        for (int offset : itemOffs) {
            int itemHere = rom[offset] & 0xFF;
            if (Gen2Constants.allowedItems.isAllowed(itemHere) && !(Gen2Constants.allowedItems.isTM(itemHere))) {
                fieldItems.add(itemHere);
            }
        }
        return fieldItems;
    }

    @Override
    public void setRegularFieldItems(List<Integer> items) {
        Iterator<Integer> iterItems = items.iterator();

        for (int offset : itemOffs) {
            int itemHere = rom[offset] & 0xFF;
            if (Gen2Constants.allowedItems.isAllowed(itemHere) && !(Gen2Constants.allowedItems.isTM(itemHere))) {
                // Replace it
                writeByte(offset, (byte) iterItems.next().intValue());
            }
        }

    }

    @Override
    public List<IngameTrade> getIngameTrades() {
        List<IngameTrade> trades = new ArrayList<>();

        // info
        int tableOffset = romEntry.getIntValue("TradeTableOffset");
        int tableSize = romEntry.getIntValue("TradeTableSize");
        int nicknameLength = romEntry.getIntValue("TradeNameLength");
        int otLength = romEntry.getIntValue("TradeOTLength");
        int[] unused = romEntry.getArrayValue("TradesUnused");
        int unusedOffset = 0;
        int entryLength = nicknameLength + otLength + 9;
        if (entryLength % 2 != 0) {
            entryLength++;
        }

        for (int entry = 0; entry < tableSize; entry++) {
            if (unusedOffset < unused.length && unused[unusedOffset] == entry) {
                unusedOffset++;
                continue;
            }
            IngameTrade trade = new IngameTrade();
            int entryOffset = tableOffset + entry * entryLength;
            trade.requestedPokemon = pokes[rom[entryOffset + 1] & 0xFF];
            trade.givenPokemon = pokes[rom[entryOffset + 2] & 0xFF];
            trade.nickname = readString(entryOffset + 3, nicknameLength, false);
            int atkdef = rom[entryOffset + 3 + nicknameLength] & 0xFF;
            int spdspc = rom[entryOffset + 4 + nicknameLength] & 0xFF;
            trade.ivs = new int[]{(atkdef >> 4) & 0xF, atkdef & 0xF, (spdspc >> 4) & 0xF, spdspc & 0xF};
            trade.item = rom[entryOffset + 5 + nicknameLength] & 0xFF;
            trade.otId = readWord(entryOffset + 6 + nicknameLength);
            trade.otName = readString(entryOffset + 8 + nicknameLength, otLength, false);
            trades.add(trade);
        }

        return trades;

    }

    @Override
    public void setIngameTrades(List<IngameTrade> trades) {
        // info
        int tableOffset = romEntry.getIntValue("TradeTableOffset");
        int tableSize = romEntry.getIntValue("TradeTableSize");
        int entryLength = getIngameTradeEntryLength();
        int[] unused = romEntry.getArrayValue("TradesUnused");
        int unusedIndex = 0;
        int tradeIndex = 0;

        for (int entry = 0; entry < tableSize; entry++) {
            if (unusedIndex < unused.length && unused[unusedIndex] == entry) {
                unusedIndex++;
            } else {
                IngameTrade trade = trades.get(tradeIndex);
                int entryOffset = tableOffset + entry * entryLength;
                writeBytes(entryOffset, ingameTradeToBytes(trade));
                tradeIndex++;
            }
        }
    }

    private byte[] ingameTradeToBytes(IngameTrade trade) {
        int nicknameLength = romEntry.getIntValue("TradeNameLength");
        int otLength = romEntry.getIntValue("TradeOTLength");

        byte[] data = new byte[getIngameTradeEntryLength()];
        data[0] = (byte) trade.requestedPokemon.getNumber();
        data[1] = (byte) trade.givenPokemon.getNumber();
        if (romEntry.getIntValue("CanChangeTrainerText") > 0) {
            writeFixedLengthString(data, trade.nickname, 2, nicknameLength);
        }
        data[2 + nicknameLength] = (byte) (trade.ivs[0] << 4 | trade.ivs[1]);
        data[3 + nicknameLength] = (byte) (trade.ivs[2] << 4 | trade.ivs[3]);
        data[4 + nicknameLength] = (byte) trade.item;
        writeWord(data, 5 + nicknameLength, trade.otId);
        if (romEntry.getIntValue("CanChangeTrainerText") > 0) {
            writeFixedLengthString(data, trade.otName, 7 + nicknameLength, otLength);
        }
        // remove gender req
        data[7 + nicknameLength + otLength] = 0;
        return data;
    }

    private int getIngameTradeEntryLength() {
        int entryLength = romEntry.getIntValue("TradeNameLength") + romEntry.getIntValue("TradeOTLength") + 9;
        if (entryLength % 2 != 0) {
            entryLength++;
        }
        return entryLength;
    }

    @Override
    public boolean hasDVs() {
        return true;
    }

    @Override
    public int generationOfPokemon() {
        return 2;
    }

    @Override
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

    private void saveEvosAndMovesLearnt() {
        int pointerTableOffset = romEntry.getIntValue("PokemonMovesetsTableOffset");

        for (Pokemon pk : pokemonList) {
            if (pk == null) continue;
            int pokeNum = pk.getNumber();
            int pointerOffset = pointerTableOffset + (pokeNum - 1) * 2;
            new GBDataRewriter<Pokemon>().rewriteData(pointerOffset, pk, this::pokemonToEvosAndMovesLearntBytes,
                    oldDataOffset -> lengthOfDataWithTerminatorsAt(oldDataOffset,
                            GBConstants.evosAndMovesTerminator, 2));
        }
    }

    private byte[] pokemonToEvosAndMovesLearntBytes(Pokemon pk) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Evolution evo : pk.getEvolutionsFrom()) {
            baos.writeBytes(evolutionToBytes(evo));
        }
        baos.write(GBConstants.evosAndMovesTerminator);
        for (MoveLearnt ml : movesets.get(pk.getNumber())) {
            baos.writeBytes(moveLearntToBytes(ml));
        }
        baos.write(GBConstants.evosAndMovesTerminator);
        return baos.toByteArray();
    }

    private byte[] evolutionToBytes(Evolution evo) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(evo.type.toIndex(2));
        baos.writeBytes(evoTypeExtraInfoToBytes(evo));
        baos.write(evo.to.getNumber());
        return baos.toByteArray();
    }

    private byte[] evoTypeExtraInfoToBytes(Evolution evo) {
        return switch (evo.type) {
            case LEVEL, STONE, TRADE_ITEM -> new byte[]{(byte) evo.extraInfo};
            case TRADE -> new byte[]{(byte) 0xFF};
            case HAPPINESS -> new byte[]{(byte) 0x01};
            case HAPPINESS_DAY -> new byte[]{(byte) 0x02};
            case HAPPINESS_NIGHT -> new byte[]{(byte) 0x03};
            case LEVEL_ATTACK_HIGHER -> new byte[]{(byte) evo.extraInfo, (byte) 0x01};
            case LEVEL_DEFENSE_HIGHER -> new byte[]{(byte) evo.extraInfo, (byte) 0x02};
            case LEVEL_ATK_DEF_SAME -> new byte[]{(byte) evo.extraInfo, (byte) 0x03};
            default -> throw new IllegalStateException("EvolutionType " + evo.type + " is not supported " +
                    "by Gen 2 games.");
        };
    }

    private byte[] moveLearntToBytes(MoveLearnt ml) {
        return new byte[] {(byte) ml.level, (byte) ml.move};
    }

    @Override
    public boolean supportsFourStartingMoves() {
        return (romEntry.getIntValue("SupportsFourStartingMoves") > 0);
    }

    @Override
    public List<Integer> getGameBreakingMoves() {
        // add OHKO moves for gen2 because x acc is still broken
        return Gen2Constants.brokenMoves;
    }

    @Override
    public List<Integer> getIllegalMoves() {
        // 3 moves that crash the game when used by self or opponent
        if (isVietCrystal) {
            return Gen2Constants.illegalVietCrystalMoves;
        }
        return new ArrayList<>();
    }

    @Override
    public List<Integer> getFieldMoves() {
        // cut, fly, surf, strength, flash,
        // dig, teleport, whirlpool, waterfall,
        // rock smash, headbutt, sweet scent
        // not softboiled or milk drink
        return Gen2Constants.fieldMoves;
    }

    @Override
    public List<Integer> getEarlyRequiredHMMoves() {
        // just cut
        return Gen2Constants.earlyRequiredHMMoves;
    }

    @Override
    public void loadPokemonPalettes() {
        // TODO: sort out when "palette" is shortened to "pal"
        int palOffset = romEntry.getIntValue("PokemonPalettes") + 8;
        for (Pokemon pk : getPokemonSet()) {
            int num = pk.getNumber() - 1;

            int normalPaletteOffset = palOffset + num * 8;
            pk.setNormalPalette(read2ColorPalette(normalPaletteOffset));

            int shinyPaletteOffset = palOffset + num * 8 + 4;
            pk.setShinyPalette(read2ColorPalette(shinyPaletteOffset));

        }
    }

    private Palette read2ColorPalette(int offset) {
        byte[] paletteBytes = new byte[]{rom[offset], rom[offset + 1], rom[offset + 2], rom[offset + 3]};
        return new Palette(paletteBytes);
    }

    @Override
    public void savePokemonPalettes() {
        int palOffset = romEntry.getIntValue("PokemonPalettes") + 8;
        for (Pokemon pk : getPokemonSet()) {
            int num = pk.getNumber() - 1;

            byte[] normalPaletteBytes = pk.getNormalPalette().toBytes();
            writeBytes(palOffset + num * 8, normalPaletteBytes);

            byte[] shinyPaletteBytes = pk.getShinyPalette().toBytes();
            writeBytes(palOffset + num * 8 + 4, shinyPaletteBytes);

        }
    }

    @Override
    public BufferedImage getPokemonImage(Pokemon pk, boolean back, boolean shiny, boolean transparentBackground, boolean includePalette) {
        // Each Pokemon has a front and back pic with a bank and a pointer (3*2=6)
        // There is no zero-entry.
        int picPointer;
        if (pk.getNumber() == Species.unown) {
            int unownLetter = random.nextInt(Gen2Constants.unownFormeCount);
            picPointer = romEntry.getIntValue("UnownPicPointers") + unownLetter * 6;
        } else {
            picPointer = romEntry.getIntValue("PicPointers") + (pk.getNumber() - 1) * 6;
        }
        if (back) {
            picPointer += 3;
        }

        int picWidth = back ? 6 : pk.getPicDimensions() & 0x0F;
        int picHeight = back ? 6 : (pk.getPicDimensions() >> 4) & 0x0F;

        byte[] data;
        try {
            data = readSpriteData(picPointer, picWidth, picHeight);
        } catch (Exception e) {
            return null;
        }
        int w = picWidth * 8;
        int h = picHeight * 8;

        // White and black are always in the palettes at positions 0 and 3, 
        // so only the middle colors are stored and need to be read.
        Palette palette = shiny ? pk.getShinyPalette() : pk.getNormalPalette();
        int[] convPalette = new int[]{0xFFFFFFFF, palette.toARGB()[0], palette.toARGB()[1], 0xFF000000};

        BufferedImage bim = GFXFunctions.drawTiledImage(data, convPalette, w, h, 8);

        if (transparentBackground) {
            bim = GFXFunctions.pseudoTransparent(bim, convPalette[0]);
        }
        if (includePalette) {
            for (int j = 0; j < convPalette.length; j++) {
                bim.setRGB(j, 0, convPalette[j]);
            }
        }

        return bim;
    }

    private byte[] readSpriteData(int picPointer, int picWidth, int picHeight) {
        int picBank = (rom[picPointer] & 0xFF);
        if (romEntry.isCrystal()) {
            // Crystal pic banks are offset by x36 for whatever reason.
            picBank += 0x36;
        } else {
            // Hey, G/S are dumb too! Arbitrarily redirected bank numbers.
            if (picBank == 0x13) {
                picBank = 0x1F;
            } else if (picBank == 0x14) {
                picBank = 0x20;
            } else if (picBank == 0x1F) {
                picBank = 0x2E;
            }
        }
        int picOffset = readPointer(picPointer + 1, picBank);

        Gen2Decmp mscSprite = new Gen2Decmp(rom, picOffset, picWidth, picHeight);
        return mscSprite.getFlattenedData();
    }

    @Override
    public PaletteHandler getPaletteHandler() {
        return paletteHandler;
    }

    @Override
    public Gen2RomEntry getRomEntry() {
        return romEntry;
    }

    @Override
    public void writeCheckValueToROM(int value) {
        if (romEntry.getIntValue("CheckValueOffset") > 0) {
            int cvOffset = romEntry.getIntValue("CheckValueOffset");
            byte[] cvBytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                rom[cvOffset + i] = (byte) ((value >> (3 - i) * 8) & 0xFF);
            }
            writeBytes(cvOffset, cvBytes);
        }
    }


}
