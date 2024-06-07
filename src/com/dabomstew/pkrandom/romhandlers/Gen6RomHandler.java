package com.dabomstew.pkrandom.romhandlers;

/*----------------------------------------------------------------------------*/
/*--  Part of "Universal Pokemon Randomizer ZX" by the UPR-ZX team          --*/
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
import com.dabomstew.pkrandom.constants.*;
import com.dabomstew.pkrandom.ctr.AMX;
import com.dabomstew.pkrandom.ctr.GARCArchive;
import com.dabomstew.pkrandom.ctr.Mini;
import com.dabomstew.pkrandom.exceptions.RomIOException;
import com.dabomstew.pkrandom.graphics.palettes.Palette;
import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.romhandlers.romentries.Gen6RomEntry;
import com.dabomstew.pkrandom.romhandlers.romentries.ThreeDSLinkedEncounter;
import pptxt.N3DSTxtHandler;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link RomHandler} for X, Y, Omega Ruby, Alpha Sapphire.
 */
public class Gen6RomHandler extends Abstract3DSRomHandler {

    public static class Factory extends RomHandler.Factory {

        @Override
        public Gen6RomHandler create() {
            return new Gen6RomHandler();
        }

        public boolean isLoadable(String filename) {
            return detect3DSRomInner(getProductCodeFromFile(filename), getTitleIdFromFile(filename));
        }
    }

    private static List<Gen6RomEntry> roms;

    static {
        loadROMInfo();
    }

    private static void loadROMInfo() {
        roms = new ArrayList<>();
        try {
            roms = Gen6RomEntry.READER.readEntriesFromFile("gen6_offsets.ini");
        } catch (IOException e) {
            throw new RuntimeException("Could not read Rom Entries.", e);
        }
    }

    // This ROM
    private Pokemon[] pokes;
    // it's important formeMappings is a TreeMap because we want to iterate through it in order
    private final TreeMap<Integer, Integer> formeMappings = new TreeMap<>();
    private List<Pokemon> pokemonList;
    private List<Pokemon> pokemonListInclFormes;
    private List<MegaEvolution> megaEvolutions;
    private Move[] moves;
    private Gen6RomEntry romEntry;
    private byte[] code;
    private List<String> abilityNames;
    private boolean loadedWildMapNames;
    private Map<Integer, String> wildMapNames;
    private int moveTutorMovesOffset;
    private List<String> itemNames;
    private List<String> shopNames;
    private int shopItemsOffset;
    private ItemList allowedItems, nonBadItems;
    private int pickupItemsTableOffset;
    private long actualCodeCRC32;
    private Map<String, Long> actualFileCRC32s;

    private GARCArchive pokeGarc, moveGarc, stringsGarc, storyTextGarc;

    @Override
    protected boolean detect3DSRom(String productCode, String titleId) {
        return detect3DSRomInner(productCode, titleId);
    }

    private static boolean detect3DSRomInner(String productCode, String titleId) {
        return entryFor(productCode, titleId) != null;
    }

    private static Gen6RomEntry entryFor(String productCode, String titleID) {
        if (productCode == null || titleID == null) {
            return null;
        }

        for (Gen6RomEntry re : roms) {
            if (productCode.equals(re.getRomCode()) && titleID.equals(re.getTitleID())) {
                return re;
            }
        }
        return null;
    }

    @Override
    protected void loadedROM(String productCode, String titleId) {
        this.romEntry = entryFor(productCode, titleId);

        try {
            code = readCode();
        } catch (IOException e) {
            throw new RomIOException(e);
        }

        try {
            stringsGarc = readGARC(romEntry.getFile("TextStrings"),true);
            storyTextGarc = readGARC(romEntry.getFile("StoryText"), true);
        } catch (IOException e) {
            throw new RomIOException(e);
        }

        loadPokemonStats();
        loadMoves();

        pokemonListInclFormes = Arrays.asList(pokes);
        pokemonList = Arrays.asList(Arrays.copyOfRange(pokes,0,Gen6Constants.pokemonCount + 1));

        abilityNames = getStrings(false,romEntry.getIntValue("AbilityNamesTextOffset"));
        itemNames = getStrings(false,romEntry.getIntValue("ItemNamesTextOffset"));
        shopNames = Gen6Constants.getShopNames(romEntry.getRomType());

        loadedWildMapNames = false;

        allowedItems = Gen6Constants.getAllowedItems(romEntry.getRomType()).copy();
        nonBadItems = Gen6Constants.getNonBadItems(romEntry.getRomType()).copy();

        try {
            computeCRC32sForRom();
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    private void loadPokemonStats() {
        try {
            pokeGarc = this.readGARC(romEntry.getFile("PokemonStats"),true);
            String[] pokeNames = readPokemonNames();
            int formeCount = Gen6Constants.getFormeCount(romEntry.getRomType());
            pokes = new Pokemon[Gen6Constants.pokemonCount + formeCount + 1];
            for (int i = 1; i <= Gen6Constants.pokemonCount; i++) {
                pokes[i] = new Pokemon(i);
                loadBasicPokeStats(pokes[i],pokeGarc.files.get(i).get(0));
                pokes[i].setName(pokeNames[i]);
                pokes[i].setGeneration(generationOf(pokes[i]));
            }

            int i = Gen6Constants.pokemonCount + 1;
            for (int k: formeMappings.keySet()) {
                pokes[i] = new Pokemon(i);
                loadBasicPokeStats(pokes[i], pokeGarc.files.get(k).get(0));

                Pokemon baseForme = pokes[formeMappings.get(k)];
                baseForme.addAltForme(pokes[i], Gen6Constants.formeSuffixesByBaseForme);
                pokes[i].setGeneration(generationOf(pokes[i]));
                i++;
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }
        populateEvolutions();
        populateMegaEvolutions();
    }

    private int generationOf(Pokemon pk) {
        if (pk.getFormeSuffix().startsWith("-Mega") || pk.getFormeSuffix().equals("-Primal")) {
            return 6;
        }
        if (pk.isAltForme()) {
            if (pk.getBaseNumber() == Species.pikachu) {
                return 6; // contest pikachu
            }
            return pk.getBaseForme().getGeneration();
        }
        if (pk.getNumber() >= Species.chespin) {
            return 6;
        } else if (pk.getNumber() >= Species.victini) {
            return 5;
        } else if (pk.getNumber() >= Species.turtwig) {
            return 4;
        } else if (pk.getNumber() >= Species.treecko) {
            return 3;
        } else if (pk.getNumber() >= Species.chikorita) {
            return 2;
        }
        return 1;
    }

    private void loadBasicPokeStats(Pokemon pkmn, byte[] stats) {
        pkmn.setHp(stats[Gen6Constants.bsHPOffset] & 0xFF);
        pkmn.setAttack(stats[Gen6Constants.bsAttackOffset] & 0xFF);
        pkmn.setDefense(stats[Gen6Constants.bsDefenseOffset] & 0xFF);
        pkmn.setSpeed(stats[Gen6Constants.bsSpeedOffset] & 0xFF);
        pkmn.setSpatk(stats[Gen6Constants.bsSpAtkOffset] & 0xFF);
        pkmn.setSpdef(stats[Gen6Constants.bsSpDefOffset] & 0xFF);
        // Type
        pkmn.setPrimaryType(Gen6Constants.typeTable[stats[Gen6Constants.bsPrimaryTypeOffset] & 0xFF]);
        pkmn.setSecondaryType(Gen6Constants.typeTable[stats[Gen6Constants.bsSecondaryTypeOffset] & 0xFF]);
        // Only one type?
        if (pkmn.getSecondaryType() == pkmn.getPrimaryType()) {
            pkmn.setSecondaryType(null);
        }
        pkmn.setCatchRate(stats[Gen6Constants.bsCatchRateOffset] & 0xFF);
        pkmn.setGrowthCurve(ExpCurve.fromByte(stats[Gen6Constants.bsGrowthCurveOffset]));

        pkmn.setAbility1(stats[Gen6Constants.bsAbility1Offset] & 0xFF);
        pkmn.setAbility2(stats[Gen6Constants.bsAbility2Offset] & 0xFF);
        pkmn.setAbility3(stats[Gen6Constants.bsAbility3Offset] & 0xFF);
        if (pkmn.getAbility1() == pkmn.getAbility2()) {
            pkmn.setAbility2(0);
        }

        // Held Items?
        int item1 = FileFunctions.read2ByteInt(stats, Gen6Constants.bsCommonHeldItemOffset);
        int item2 = FileFunctions.read2ByteInt(stats, Gen6Constants.bsRareHeldItemOffset);

        if (item1 == item2) {
            // guaranteed
            pkmn.setGuaranteedHeldItem(item1);
            pkmn.setCommonHeldItem(0);
            pkmn.setRareHeldItem(0);
            pkmn.setDarkGrassHeldItem(-1);
        } else {
            pkmn.setGuaranteedHeldItem(0);
            pkmn.setCommonHeldItem(item1);
            pkmn.setRareHeldItem(item2);
            pkmn.setDarkGrassHeldItem(-1);
        }

        int formeCount = stats[Gen6Constants.bsFormeCountOffset] & 0xFF;
        if (formeCount > 1) {
            if (!formeMappings.containsKey(pkmn.getNumber())) { // alt formes don't get to have alt formes of their own
                System.out.println(pkmn);
                System.out.println("visited: " + formeMappings.containsKey(pkmn.getNumber()));
                int firstForme = FileFunctions.read2ByteInt(stats, Gen6Constants.bsFormeOffset);
                System.out.println("firstForme: " + firstForme);
                if (firstForme != 0) {
                    for (int i = 1; i < formeCount; i++) {
                        formeMappings.put(firstForme + i - 1, pkmn.getNumber()); // Assumes that formes are in memory in the same order as their numbers
                        if (Gen6Constants.cosmeticForms.contains(firstForme + i - 1)) {
                            if (pkmn.getNumber() != Species.pikachu && pkmn.getNumber() != Species.cherrim) { // No Pikachu/Cherrim
                                pkmn.setCosmeticFormCount(pkmn.getCosmeticFormCount() + 1);
                            }
                        }
                    }
                } else {
                    if (pkmn.getNumber() != Species.arceus && pkmn.getNumber() != Species.genesect && pkmn.getNumber() != Species.xerneas) {
                        // Reason for exclusions:
                        // Arceus/Genesect: to avoid confusion
                        // Xerneas: Should be handled automatically?
                        pkmn.setCosmeticFormCount(formeCount);
                    }
                }
            } else {
                if (Gen6Constants.cosmeticForms.contains(pkmn.getNumber())) {
                    pkmn.setCosmeticForme(true);
                }
            }
        }
    }

    private String[] readPokemonNames() {
        String[] pokeNames = new String[Gen6Constants.pokemonCount + 1];
        List<String> nameList = getStrings(false, romEntry.getIntValue("PokemonNamesTextOffset"));
        for (int i = 1; i <= Gen6Constants.pokemonCount; i++) {
            pokeNames[i] = nameList.get(i);
        }
        return pokeNames;
    }

    private void populateEvolutions() {
        for (Pokemon pkmn : pokes) {
            if (pkmn != null) {
                pkmn.getEvolutionsFrom().clear();
                pkmn.getEvolutionsTo().clear();
            }
        }

        // Read GARC
        try {
            GARCArchive evoGARC = readGARC(romEntry.getFile("PokemonEvolutions"),true);
            for (int i = 1; i <= Gen6Constants.pokemonCount + Gen6Constants.getFormeCount(romEntry.getRomType()); i++) {
                Pokemon pk = pokes[i];
                byte[] evoEntry = evoGARC.files.get(i).get(0);
                for (int evo = 0; evo < 8; evo++) {
                    int method = readWord(evoEntry, evo * 6);
                    int species = readWord(evoEntry, evo * 6 + 4);
                    if (method >= 1 && method <= Gen6Constants.evolutionMethodCount && species >= 1) {
                        EvolutionType et = Gen6Constants.evolutionTypeFromIndex(method);
                        if (et.equals(EvolutionType.LEVEL_HIGH_BEAUTY)) continue; // Remove Feebas "split" evolution
                        int extraInfo = readWord(evoEntry, evo * 6 + 2);
                        Evolution evol = new Evolution(pk, pokes[species], true, et, extraInfo);
                        if (!pk.getEvolutionsFrom().contains(evol)) {
                            pk.getEvolutionsFrom().add(evol);
                            if (!pk.isCosmeticForme()) pokes[species].getEvolutionsTo().add(evol);
                        }
                    }
                }
                // Nincada's Shedinja evo is hardcoded into the game's executable, so
                // if the Pokemon is Nincada, then let's put it as one of its evolutions
                if (pk.getNumber() == Species.nincada) {
                    Pokemon shedinja = pokes[Species.shedinja];
                    Evolution evol = new Evolution(pk, shedinja, false, EvolutionType.LEVEL_IS_EXTRA, 20);
                    pk.getEvolutionsFrom().add(evol);
                    shedinja.getEvolutionsTo().add(evol);
                }

                // Split evos shouldn't carry stats unless the evo is Nincada's
                // In that case, we should have Ninjask carry stats
                if (pk.getEvolutionsFrom().size() > 1) {
                    for (Evolution e : pk.getEvolutionsFrom()) {
                        if (e.getType() != EvolutionType.LEVEL_CREATE_EXTRA) {
                            e.setCarryStats(false);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    private void populateMegaEvolutions() {
        for (Pokemon pkmn : pokes) {
            if (pkmn != null) {
                pkmn.getMegaEvolutionsFrom().clear();
                pkmn.getMegaEvolutionsTo().clear();
            }
        }

        // Read GARC
        try {
            megaEvolutions = new ArrayList<>();
            GARCArchive megaEvoGARC = readGARC(romEntry.getFile("MegaEvolutions"),true);
            for (int i = 1; i <= Gen6Constants.pokemonCount; i++) {
                Pokemon pk = pokes[i];
                byte[] megaEvoEntry = megaEvoGARC.files.get(i).get(0);
                for (int evo = 0; evo < 3; evo++) {
                    int formNum = readWord(megaEvoEntry, evo * 8);
                    int method = readWord(megaEvoEntry, evo * 8 + 2);
                    if (method >= 1) {
                        int argument = readWord(megaEvoEntry, evo * 8 + 4);
                        Pokemon megaPk = getAltFormeOfPokemon(pk, formNum);
                        MegaEvolution megaEvo = new MegaEvolution(pk, megaPk, method, argument);
                        if (!pk.getMegaEvolutionsFrom().contains(megaEvo)) {
                            pk.getMegaEvolutionsFrom().add(megaEvo);
                            megaPk.getMegaEvolutionsTo().add(megaEvo);
                        }
                        megaEvolutions.add(megaEvo);
                    }
                }
                // split evos don't carry stats
                if (pk.getMegaEvolutionsFrom().size() > 1) {
                    for (MegaEvolution e : pk.getMegaEvolutionsFrom()) {
                        e.carryStats = false;
                    }
                }
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    private List<String> getStrings(boolean isStoryText, int index) {
        GARCArchive baseGARC = isStoryText ? storyTextGarc : stringsGarc;
        return getStrings(baseGARC, index);
    }

    private List<String> getStrings(GARCArchive textGARC, int index) {
        byte[] rawFile = textGARC.files.get(index).get(0);
        return new ArrayList<>(N3DSTxtHandler.readTexts(rawFile,true,romEntry.getRomType()));
    }

    private void setStrings(boolean isStoryText, int index, List<String> strings) {
        GARCArchive baseGARC = isStoryText ? storyTextGarc : stringsGarc;
        setStrings(baseGARC, index, strings);
    }

    private void setStrings(GARCArchive textGARC, int index, List<String> strings) {
        byte[] oldRawFile = textGARC.files.get(index).get(0);
        try {
            byte[] newRawFile = N3DSTxtHandler.saveEntry(oldRawFile, strings, romEntry.getRomType());
            textGARC.setFile(index, newRawFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadMoves() {
        try {
            moveGarc = this.readGARC(romEntry.getFile("MoveData"),true);
            int moveCount = Gen6Constants.getMoveCount(romEntry.getRomType());
            moves = new Move[moveCount + 1];
            List<String> moveNames = getStrings(false, romEntry.getIntValue("MoveNamesTextOffset"));
            for (int i = 1; i <= moveCount; i++) {
                byte[] moveData;
                if (romEntry.getRomType() == Gen6Constants.Type_ORAS) {
                    moveData = Mini.UnpackMini(moveGarc.files.get(0).get(0), "WD")[i];
                } else {
                    moveData = moveGarc.files.get(i).get(0);
                }
                moves[i] = new Move();
                moves[i].name = moveNames.get(i);
                moves[i].number = i;
                moves[i].internalId = i;
                moves[i].effectIndex = readWord(moveData, 16);
                moves[i].hitratio = (moveData[4] & 0xFF);
                moves[i].power = moveData[3] & 0xFF;
                moves[i].pp = moveData[5] & 0xFF;
                moves[i].type = Gen6Constants.typeTable[moveData[0] & 0xFF];
                moves[i].flinchPercentChance = moveData[15] & 0xFF;
                moves[i].target = moveData[20] & 0xFF;
                moves[i].category = Gen6Constants.moveCategoryIndices[moveData[2] & 0xFF];
                moves[i].priority = moveData[6];

                int critStages = moveData[14] & 0xFF;
                if (critStages == 6) {
                    moves[i].criticalChance = CriticalChance.GUARANTEED;
                } else if (critStages > 0) {
                    moves[i].criticalChance = CriticalChance.INCREASED;
                }

                int internalStatusType = readWord(moveData, 8);
                int flags = FileFunctions.readFullInt(moveData, 32);
                moves[i].makesContact = (flags & 0x001) != 0;
                moves[i].isChargeMove = (flags & 0x002) != 0;
                moves[i].isRechargeMove = (flags & 0x004) != 0;
                moves[i].isPunchMove = (flags & 0x080) != 0;
                moves[i].isSoundMove = (flags & 0x100) != 0;
                moves[i].isTrapMove = internalStatusType == 8;
                switch (moves[i].effectIndex) {
                    case Gen6Constants.noDamageTargetTrappingEffect:
                    case Gen6Constants.noDamageFieldTrappingEffect:
                    case Gen6Constants.damageAdjacentFoesTrappingEffect:
                        moves[i].isTrapMove = true;
                        break;
                }

                int qualities = moveData[1];
                int recoilOrAbsorbPercent = moveData[18];
                if (qualities == Gen6Constants.damageAbsorbQuality) {
                    moves[i].absorbPercent = recoilOrAbsorbPercent;
                } else {
                    moves[i].recoilPercent = -recoilOrAbsorbPercent;
                }

                if (i == Moves.swift) {
                    perfectAccuracy = (int)moves[i].hitratio;
                }

                if (GlobalConstants.normalMultihitMoves.contains(i)) {
                    moves[i].hitCount = 19 / 6.0;
                } else if (GlobalConstants.doubleHitMoves.contains(i)) {
                    moves[i].hitCount = 2;
                } else if (i == Moves.tripleKick) {
                    moves[i].hitCount = 2.71; // this assumes the first hit lands
                }

                switch (qualities) {
                    case Gen6Constants.noDamageStatChangeQuality:
                    case Gen6Constants.noDamageStatusAndStatChangeQuality:
                        // All Allies or Self
                        if (moves[i].target == 6 || moves[i].target == 7) {
                            moves[i].statChangeMoveType = StatChangeMoveType.NO_DAMAGE_USER;
                        } else if (moves[i].target == 2) {
                            moves[i].statChangeMoveType = StatChangeMoveType.NO_DAMAGE_ALLY;
                        } else if (moves[i].target == 8) {
                            moves[i].statChangeMoveType = StatChangeMoveType.NO_DAMAGE_ALL;
                        } else {
                            moves[i].statChangeMoveType = StatChangeMoveType.NO_DAMAGE_TARGET;
                        }
                        break;
                    case Gen6Constants.damageTargetDebuffQuality:
                        moves[i].statChangeMoveType = StatChangeMoveType.DAMAGE_TARGET;
                        break;
                    case Gen6Constants.damageUserBuffQuality:
                        moves[i].statChangeMoveType = StatChangeMoveType.DAMAGE_USER;
                        break;
                    default:
                        moves[i].statChangeMoveType = StatChangeMoveType.NONE_OR_UNKNOWN;
                        break;
                }

                for (int statChange = 0; statChange < 3; statChange++) {
                    moves[i].statChanges[statChange].type = StatChangeType.values()[moveData[21 + statChange]];
                    moves[i].statChanges[statChange].stages = moveData[24 + statChange];
                    moves[i].statChanges[statChange].percentChance = moveData[27 + statChange];
                }

                // Exclude status types that aren't in the StatusType enum.
                if (internalStatusType < 7) {
                    moves[i].statusType = StatusType.values()[internalStatusType];
                    if (moves[i].statusType == StatusType.POISON && (i == Moves.toxic || i == Moves.poisonFang)) {
                        moves[i].statusType = StatusType.TOXIC_POISON;
                    }
                    moves[i].statusPercentChance = moveData[10] & 0xFF;
                    switch (qualities) {
                        case Gen6Constants.noDamageStatusQuality:
                        case Gen6Constants.noDamageStatusAndStatChangeQuality:
                            moves[i].statusMoveType = StatusMoveType.NO_DAMAGE;
                            break;
                        case Gen6Constants.damageStatusQuality:
                            moves[i].statusMoveType = StatusMoveType.DAMAGE;
                            break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    protected void prepareSaveRom() {
    	super.prepareSaveRom();
        try {
            writeCode(code);
            writeGARC(romEntry.getFile("TextStrings"), stringsGarc);
            writeGARC(romEntry.getFile("StoryText"), storyTextGarc);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    protected String getGameAcronym() {
        return romEntry.getAcronym();
    }

    @Override
    protected boolean isGameUpdateSupported(int version) {
        return version == romEntry.getIntValue("FullyUpdatedVersionNumber");
    }

    @Override
    protected String getGameVersion() {
        List<String> titleScreenText = getStrings(false, romEntry.getIntValue("TitleScreenTextOffset"));
        if (titleScreenText.size() > romEntry.getIntValue("UpdateStringOffset")) {
            return titleScreenText.get(romEntry.getIntValue("UpdateStringOffset"));
        }
        // This shouldn't be seen by users, but is correct assuming we accidentally show it to them.
        return "Unpatched";
    }

    @Override
    public void savePokemonStats() {
        int k = Gen6Constants.getBsSize(romEntry.getRomType());
        byte[] duplicateData = pokeGarc.files.get(Gen6Constants.pokemonCount + Gen6Constants.getFormeCount(romEntry.getRomType()) + 1).get(0);
        for (int i = 1; i <= Gen6Constants.pokemonCount + Gen6Constants.getFormeCount(romEntry.getRomType()); i++) {
            byte[] pokeData = pokeGarc.files.get(i).get(0);
            saveBasicPokeStats(pokes[i], pokeData);
            for (byte pokeDataByte : pokeData) {
                duplicateData[k] = pokeDataByte;
                k++;
            }
        }

        try {
            this.writeGARC(romEntry.getFile("PokemonStats"),pokeGarc);
        } catch (IOException e) {
            throw new RomIOException(e);
        }

        writeEvolutions();
    }

    private void saveBasicPokeStats(Pokemon pkmn, byte[] stats) {
        stats[Gen6Constants.bsHPOffset] = (byte) pkmn.getHp();
        stats[Gen6Constants.bsAttackOffset] = (byte) pkmn.getAttack();
        stats[Gen6Constants.bsDefenseOffset] = (byte) pkmn.getDefense();
        stats[Gen6Constants.bsSpeedOffset] = (byte) pkmn.getSpeed();
        stats[Gen6Constants.bsSpAtkOffset] = (byte) pkmn.getSpatk();
        stats[Gen6Constants.bsSpDefOffset] = (byte) pkmn.getSpdef();
        stats[Gen6Constants.bsPrimaryTypeOffset] = Gen6Constants.typeToByte(pkmn.getPrimaryType());
        if (pkmn.getSecondaryType() == null) {
            stats[Gen6Constants.bsSecondaryTypeOffset] = stats[Gen6Constants.bsPrimaryTypeOffset];
        } else {
            stats[Gen6Constants.bsSecondaryTypeOffset] = Gen6Constants.typeToByte(pkmn.getSecondaryType());
        }
        stats[Gen6Constants.bsCatchRateOffset] = (byte) pkmn.getCatchRate();
        stats[Gen6Constants.bsGrowthCurveOffset] = pkmn.getGrowthCurve().toByte();

        stats[Gen6Constants.bsAbility1Offset] = (byte) pkmn.getAbility1();
        stats[Gen6Constants.bsAbility2Offset] = pkmn.getAbility2() != 0 ? (byte) pkmn.getAbility2() : (byte) pkmn.getAbility1();
        stats[Gen6Constants.bsAbility3Offset] = (byte) pkmn.getAbility3();

        // Held items
        if (pkmn.getGuaranteedHeldItem() > 0) {
            FileFunctions.write2ByteInt(stats, Gen6Constants.bsCommonHeldItemOffset, pkmn.getGuaranteedHeldItem());
            FileFunctions.write2ByteInt(stats, Gen6Constants.bsRareHeldItemOffset, pkmn.getGuaranteedHeldItem());
            FileFunctions.write2ByteInt(stats, Gen6Constants.bsDarkGrassHeldItemOffset, 0);
        } else {
            FileFunctions.write2ByteInt(stats, Gen6Constants.bsCommonHeldItemOffset, pkmn.getCommonHeldItem());
            FileFunctions.write2ByteInt(stats, Gen6Constants.bsRareHeldItemOffset, pkmn.getRareHeldItem());
            FileFunctions.write2ByteInt(stats, Gen6Constants.bsDarkGrassHeldItemOffset, 0);
        }

        if (pkmn.fullName().equals("Meowstic")) {
            stats[Gen6Constants.bsGenderOffset] = 0;
        } else if (pkmn.fullName().equals("Meowstic-F")) {
            stats[Gen6Constants.bsGenderOffset] = (byte)0xFE;
        }
    }

    private void writeEvolutions() {
        try {
            GARCArchive evoGARC = readGARC(romEntry.getFile("PokemonEvolutions"),true);
            for (int i = 1; i <= Gen6Constants.pokemonCount + Gen6Constants.getFormeCount(romEntry.getRomType()); i++) {
                byte[] evoEntry = evoGARC.files.get(i).get(0);
                Pokemon pk = pokes[i];
                if (pk.getNumber() == Species.nincada) {
                    writeShedinjaEvolution();
                } else if (pk.getNumber() == Species.feebas && romEntry.getRomType() == Gen6Constants.Type_ORAS) {
                    recreateFeebasBeautyEvolution();
                }
                int evosWritten = 0;
                for (Evolution evo : pk.getEvolutionsFrom()) {
                    writeWord(evoEntry, evosWritten * 6, Gen6Constants.evolutionTypeToIndex(evo.getType()));
                    writeWord(evoEntry, evosWritten * 6 + 2, evo.getExtraInfo());
                    writeWord(evoEntry, evosWritten * 6 + 4, evo.getTo().getNumber());
                    evosWritten++;
                    if (evosWritten == 8) {
                        break;
                    }
                }
                while (evosWritten < 8) {
                    writeWord(evoEntry, evosWritten * 6, 0);
                    writeWord(evoEntry, evosWritten * 6 + 2, 0);
                    writeWord(evoEntry, evosWritten * 6 + 4, 0);
                    evosWritten++;
                }
            }
            writeGARC(romEntry.getFile("PokemonEvolutions"), evoGARC);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    private void writeShedinjaEvolution() throws IOException {
        Pokemon nincada = pokes[Species.nincada];

        // When the "Limit Pokemon" setting is enabled and Gen 3 is disabled, or when
        // "Random Every Level" evolutions are selected, we end up clearing out Nincada's
        // vanilla evolutions. In that case, there's no point in even worrying about
        // Shedinja, so just return.
        if (nincada.getEvolutionsFrom().size() < 2) {
            return;
        }
        Pokemon primaryEvolution = nincada.getEvolutionsFrom().get(0).getTo();
        Pokemon extraEvolution = nincada.getEvolutionsFrom().get(1).getTo();

        // In the CRO that handles the evolution cutscene, there's a hardcoded check to
        // see if the Pokemon that just evolved is now a Ninjask after evolving. It
        // performs that check using the following instructions:
        // sub    r0, r1, #0x100
        // subs   r0, r0, #0x23
        // bne    skipMakingShedinja
        // The below code tweaks these instructions to use the species ID of Nincada's
        // new primary evolution; that way, evolving Nincada will still produce an "extra"
        // Pokemon like in older generations.
        byte[] evolutionCRO = readFile(romEntry.getFile("Evolution"));
        int offset = find(evolutionCRO, Gen6Constants.ninjaskSpeciesPrefix);
        if (offset > 0) {
            offset += Gen6Constants.ninjaskSpeciesPrefix.length() / 2; // because it was a prefix
            int primaryEvoLower = primaryEvolution.getNumber() & 0x00FF;
            int primaryEvoUpper = (primaryEvolution.getNumber() & 0xFF00) >> 8;
            evolutionCRO[offset] = (byte) primaryEvoUpper;
            evolutionCRO[offset + 4] = (byte) primaryEvoLower;
        }

        // In the game's executable, there's a hardcoded value to indicate what "extra"
        // Pokemon to create. It produces a Shedinja using the following instruction:
        // mov r1, #0x124, where 0x124 = 292 in decimal, which is Shedinja's species ID.
        // We can't just blindly replace it, though, because certain constants (for example,
        // 0x125) cannot be moved without using the movw instruction. This works fine in
        // Citra, but crashes on real hardware. Instead, we have to annoyingly shift up a
        // big chunk of code to fill in a nop; we can then do a pc-relative load to a
        // constant in the new free space.
        offset = find(code, Gen6Constants.shedinjaSpeciesPrefix);
        if (offset > 0) {
            offset += Gen6Constants.shedinjaSpeciesPrefix.length() / 2; // because it was a prefix

            // Shift up everything below the last nop to make some room at the bottom of the function.
            for (int i = 80; i < 188; i++) {
                code[offset + i] = code[offset + i + 4];
            }

            // For every bl that we shifted up, patch them so they're now pointing to the same place they
            // were before (without this, they will be pointing to 0x4 before where they're supposed to).
            List<Integer> blOffsetsToPatch = Arrays.asList(80, 92, 104, 116, 128, 140, 152, 164, 176);
            for (int blOffsetToPatch : blOffsetsToPatch) {
                code[offset + blOffsetToPatch] += 1;
            }

            // Write Nincada's new extra evolution in the new free space.
            writeLong(code, offset + 188, extraEvolution.getNumber());

            // Now write the pc-relative load over the original mov instruction.
            code[offset] = (byte) 0xB4;
            code[offset + 1] = 0x10;
            code[offset + 2] = (byte) 0x9F;
            code[offset + 3] = (byte) 0xE5;
        }

        // Now that we've handled the hardcoded Shedinja evolution, delete it so that
        // we do *not* handle it in WriteEvolutions
        nincada.getEvolutionsFrom().remove(1);
        extraEvolution.getEvolutionsTo().remove(0);
        writeFile(romEntry.getFile("Evolution"), evolutionCRO);
    }

    private void recreateFeebasBeautyEvolution() {
        Pokemon feebas = pokes[Species.feebas];

        // When the "Limit Pokemon" setting is enabled, we clear out the evolutions of
        // everything *not* in the pool, which could include Feebas. In that case,
        // there's no point in even worrying about its evolutions, so just return.
        if (feebas.getEvolutionsFrom().size() == 0) {
            return;
        }

        Evolution prismScaleEvo = feebas.getEvolutionsFrom().get(0);
        Pokemon feebasEvolution = prismScaleEvo.getTo();
        int beautyNeededToEvolve = 170;
        Evolution beautyEvolution = new Evolution(feebas, feebasEvolution, true,
                EvolutionType.LEVEL_HIGH_BEAUTY, beautyNeededToEvolve);
        feebas.getEvolutionsFrom().add(beautyEvolution);
        feebasEvolution.getEvolutionsTo().add(beautyEvolution);
    }

    @Override
    public void saveMoves() {
        int moveCount = Gen6Constants.getMoveCount(romEntry.getRomType());
        byte[][] miniArchive = new byte[0][0];
        if (romEntry.getRomType() == Gen6Constants.Type_ORAS) {
            miniArchive = Mini.UnpackMini(moveGarc.files.get(0).get(0), "WD");
        }
        for (int i = 1; i <= moveCount; i++) {
            byte[] data;
            if (romEntry.getRomType() == Gen6Constants.Type_ORAS) {
                data = miniArchive[i];
            } else {
                data = moveGarc.files.get(i).get(0);
            }
            data[2] = Gen6Constants.moveCategoryToByte(moves[i].category);
            data[3] = (byte) moves[i].power;
            data[0] = Gen6Constants.typeToByte(moves[i].type);
            int hitratio = (int) Math.round(moves[i].hitratio);
            if (hitratio < 0) {
                hitratio = 0;
            }
            if (hitratio > 101) {
                hitratio = 100;
            }
            data[4] = (byte) hitratio;
            data[5] = (byte) moves[i].pp;
        }
        try {
            if (romEntry.getRomType() == Gen6Constants.Type_ORAS) {
                moveGarc.setFile(0, Mini.PackMini(miniArchive, "WD"));
            }
            this.writeGARC(romEntry.getFile("MoveData"), moveGarc);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    private void patchFormeReversion() throws IOException {
        // Upon loading a save, all Mega Pokemon and all Primal Reversions
        // in the player's party are set back to their base forme. This
        // patches .code such that this reversion does not happen.
        String saveLoadFormeReversionPrefix = Gen6Constants.getSaveLoadFormeReversionPrefix(romEntry.getRomType());
        int offset = find(code, saveLoadFormeReversionPrefix);
        if (offset > 0) {
            offset += saveLoadFormeReversionPrefix.length() / 2; // because it was a prefix

            // The actual offset of the code we want to patch is 0x10 bytes from the end of
            // the prefix. We have to do this because these 0x10 bytes differ between the
            // base game and all game updates, so we cannot use them as part of our prefix.
            offset += 0x10;

            // Stubs the call to the function that checks for Primal Reversions and
            // Mega Pokemon
            code[offset] = 0x00;
            code[offset + 1] = 0x00;
            code[offset + 2] = 0x00;
            code[offset + 3] = 0x00;
        }

        // In ORAS, the game also has hardcoded checks to revert Primal Groudon and Primal Kyogre
        // immediately after catching them.
        if (romEntry.getRomType() == Gen6Constants.Type_ORAS) {
            byte[] battleCRO = readFile(romEntry.getFile("Battle"));
            offset = find(battleCRO, Gen6Constants.afterBattleFormeReversionPrefix);
            if (offset > 0) {
                offset += Gen6Constants.afterBattleFormeReversionPrefix.length() / 2; // because it was a prefix

                // The game checks for Primal Kyogre and Primal Groudon by pc-relative loading 0x17E,
                // which is Kyogre's species ID. The call to pml::pokepara::CoreParam::ChangeFormNo
                // is used by other species which we probably don't want to break, so instead of
                // stubbing the call to the function, just break the hardcoded species ID check by
                // making the game pc-relative load a total nonsense ID.
                battleCRO[offset] = (byte) 0xFF;
                battleCRO[offset + 1] = (byte) 0xFF;

                writeFile(romEntry.getFile("Battle"), battleCRO);
            }
        }
    }

    @Override
    public List<Pokemon> getPokemon() {
        return pokemonList;
    }

    @Override
    public List<Pokemon> getPokemonInclFormes() {
        return pokemonListInclFormes;
    }

	@Override
	public PokemonSet<Pokemon> getAltFormes() {
		int formeCount = Gen6Constants.getFormeCount(romEntry.getRomType());
		return new PokemonSet<>(pokemonListInclFormes.subList(Gen6Constants.pokemonCount + 1,
				Gen6Constants.pokemonCount + formeCount + 1));
	}

    @Override
    public List<MegaEvolution> getMegaEvolutions() {
        return megaEvolutions;
    }

    @Override
    public Pokemon getAltFormeOfPokemon(Pokemon pk, int forme) {
        return pk.getAltFormes().size() > forme ? pk.getAltFormes().get(forme - 1) : pk;
    }

	@Override
	public PokemonSet<Pokemon> getIrregularFormes() {
		return Gen6Constants.getIrregularFormes(romEntry.getRomType())
				.stream().map(i -> pokes[i])
				.collect(Collectors.toCollection(PokemonSet::new));
	}

    @Override
    public boolean hasFunctionalFormes() {
        return true;
    }

    @Override
    public List<Pokemon> getStarters() {
        List<StaticEncounter> starters = new ArrayList<>();
        try {
            byte[] staticCRO = readFile(romEntry.getFile("StaticPokemon"));

            List<Integer> starterIndices = Arrays.stream(romEntry.getArrayValue("StarterIndices")).boxed().toList();

            // Gift Pokemon
            int count = Gen6Constants.getGiftPokemonCount(romEntry.getRomType());
            int size = Gen6Constants.getGiftPokemonSize(romEntry.getRomType());
            int offset = romEntry.getIntValue("GiftPokemonOffset");
            for (int i = 0; i < count; i++) {
                if (!starterIndices.contains(i)) continue;
                StaticEncounter se = new StaticEncounter();
                int species = FileFunctions.read2ByteInt(staticCRO,offset+i*size);
                Pokemon pokemon = pokes[species];
                int forme = staticCRO[offset+i*size + 4];
                if (forme > pokemon.getCosmeticFormCount() && forme != 30 && forme != 31) {
                    pokemon = getAltFormeOfPokemon(pokemon, forme);
                }
                se.pkmn = pokemon;
                se.forme = forme;
                se.level = staticCRO[offset+i*size + 5];
                starters.add(se);
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }

        return starters.stream().map(pk -> pk.pkmn).collect(Collectors.toList());
    }

    @Override
    public boolean setStarters(List<Pokemon> newStarters) {
        // Old code had the starters be set to a random cosmetic forme, if they had any.
        // However, since randomness was factored out of the RomHandlers, that functionality was simply removed.
        // If you want to reimplement it, do so outside this method.
        try {
            byte[] staticCRO = readFile(romEntry.getFile("StaticPokemon"));
            byte[] displayCRO = readFile(romEntry.getFile("StarterDisplay"));

            List<Integer> starterIndices = Arrays.stream(romEntry.getArrayValue("StarterIndices")).boxed().toList();

            // Gift Pokemon
            int count = Gen6Constants.getGiftPokemonCount(romEntry.getRomType());
            int size = Gen6Constants.getGiftPokemonSize(romEntry.getRomType());
            int offset = romEntry.getIntValue("GiftPokemonOffset");
            int displayOffset = readWord(displayCRO,romEntry.getIntValue("StarterOffsetOffset")) + romEntry.getIntValue("StarterExtraOffset");

            Iterator<Pokemon> starterIter = newStarters.iterator();

            int displayIndex = 0;

            List<String> starterText = getStrings(false,romEntry.getIntValue("StarterTextOffset"));
            int[] starterTextIndices = romEntry.getArrayValue("SpecificStarterTextOffsets");

            for (int i = 0; i < count; i++) {
                if (!starterIndices.contains(i)) continue;

                StaticEncounter newStatic = new StaticEncounter();
                Pokemon starter = starterIter.next();
                if (starter.isAltForme()) {
                    newStatic.forme = starter.getFormeNumber();
                    starter = starter.getBaseForme();
                }
                newStatic.pkmn = starter;
                writeWord(staticCRO,offset+i*size, newStatic.pkmn.getNumber());
                staticCRO[offset+i*size + 4] = (byte)newStatic.forme;
//                staticCRO[offset+i*size + 5] = (byte)newStatic.level;
                writeWord(displayCRO,displayOffset+displayIndex*0x54, newStatic.pkmn.getNumber());
                displayCRO[displayOffset+displayIndex*0x54+2] = (byte)newStatic.forme;
                if (displayIndex < 3) {
                    starterText.set(starterTextIndices[displayIndex],
                            "[VAR PKNAME(0000)]");
                }
                displayIndex++;
            }
            writeFile(romEntry.getFile("StaticPokemon"),staticCRO);
            writeFile(romEntry.getFile("StarterDisplay"),displayCRO);
            setStrings(false, romEntry.getIntValue("StarterTextOffset"), starterText);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
        return true;
    }

    @Override
    public boolean hasStarterTypeTriangleSupport() {
        return false;
    }

    @Override
    public boolean hasStarterAltFormes() {
        return true;
    }

    @Override
    public int starterCount() {
        return romEntry.getRomType() == Gen6Constants.Type_XY ? 6 : 12;
    }

    @Override
    public boolean supportsStarterHeldItems() {
        return true;
    }

    @Override
    public List<Integer> getStarterHeldItems() {
        List<Integer> starterHeldItems = new ArrayList<>();
        try {
            byte[] staticCRO = readFile(romEntry.getFile("StaticPokemon"));

            List<Integer> starterIndices = Arrays.stream(romEntry.getArrayValue("StarterIndices")).boxed().toList();

            // Gift Pokemon
            int count = Gen6Constants.getGiftPokemonCount(romEntry.getRomType());
            int size = Gen6Constants.getGiftPokemonSize(romEntry.getRomType());
            int offset = romEntry.getIntValue("GiftPokemonOffset");
            for (int i = 0; i < count; i++) {
                if (!starterIndices.contains(i)) continue;
                int heldItem = FileFunctions.readFullInt(staticCRO,offset+i*size + 12);
                if (heldItem < 0) {
                    heldItem = 0;
                }
                starterHeldItems.add(heldItem);
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }

        return starterHeldItems;
    }

    @Override
    public void setStarterHeldItems(List<Integer> items) {
        try {
            byte[] staticCRO = readFile(romEntry.getFile("StaticPokemon"));

            List<Integer> starterIndices = Arrays.stream(romEntry.getArrayValue("StarterIndices")).boxed().toList();

            // Gift Pokemon
            int count = Gen6Constants.getGiftPokemonCount(romEntry.getRomType());
            int size = Gen6Constants.getGiftPokemonSize(romEntry.getRomType());
            int offset = romEntry.getIntValue("GiftPokemonOffset");

            Iterator<Integer> itemsIter = items.iterator();

            for (int i = 0; i < count; i++) {
                if (!starterIndices.contains(i)) continue;
                int item = itemsIter.next();
                if (item == 0) {
                    FileFunctions.writeFullInt(staticCRO,offset+i*size + 12,-1);
                } else {
                    FileFunctions.writeFullInt(staticCRO,offset+i*size + 12,item);
                }
            }
            writeFile(romEntry.getFile("StaticPokemon"),staticCRO);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    public List<Move> getMoves() {
        return Arrays.asList(moves);
    }

    @Override
    public List<EncounterArea> getEncounters(boolean useTimeOfDay) {
        if (!loadedWildMapNames) {
            loadWildMapNames();
        }

        List<EncounterArea> encounterAreas;
        try {
            if (romEntry.getRomType() == Gen6Constants.Type_ORAS) {
                encounterAreas = getEncountersORAS();
            } else {
                encounterAreas = getEncountersXY();
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }

        Gen6Constants.tagEncounterAreas(encounterAreas, romEntry.getRomType());

        return encounterAreas;
    }

    private List<EncounterArea> getEncountersXY() throws IOException {
        GARCArchive encounterGarc = readGARC(romEntry.getFile("WildPokemon"), false);
        List<EncounterArea> encounterAreas = new ArrayList<>();
        for (int i = 0; i < encounterGarc.files.size() - 1; i++) {
            byte[] b = encounterGarc.files.get(i).get(0);
            if (!wildMapNames.containsKey(i)) {
                wildMapNames.put(i, "? Unknown ?");
            }
            String mapName = wildMapNames.get(i);
            int offset = FileFunctions.readFullInt(b, 0x10) + 0x10;
            int length = b.length - offset;
            if (length < 0x178) { // No encounters in this map
                continue;
            }
            byte[] encounterData = new byte[0x178];
            System.arraycopy(b, offset, encounterData, 0, 0x178);

            // TODO: Is there some rate we can check like in older gens?
            // First, 12 grass encounters, 12 rough terrain encounters, and 12 encounters each for yellow/purple/red flowers
            EncounterArea grassArea = readEncounterArea(encounterData, 0, 12);
            if (grassArea.size() > 0) {
                grassArea.setDisplayName(mapName + " Grass/Cave");
                encounterAreas.add(grassArea);
            }
            EncounterArea yellowFlowerArea = readEncounterArea(encounterData, 48, 12);
            if (yellowFlowerArea.size() > 0) {
                yellowFlowerArea.setDisplayName(mapName + " Yellow Flowers");
                encounterAreas.add(yellowFlowerArea);
            }
            EncounterArea purpleFlowerArea = readEncounterArea(encounterData, 96, 12);
            if (purpleFlowerArea.size() > 0) {
                purpleFlowerArea.setDisplayName(mapName + " Purple Flowers");
                encounterAreas.add(purpleFlowerArea);
            }
            EncounterArea redFlowerArea = readEncounterArea(encounterData, 144, 12);
            if (redFlowerArea.size() > 0) {
                redFlowerArea.setDisplayName(mapName + " Red Flowers");
                encounterAreas.add(redFlowerArea);
            }
            EncounterArea roughTerrainArea = readEncounterArea(encounterData, 192, 12);
            if (roughTerrainArea.size() > 0) {
                roughTerrainArea.setDisplayName(mapName + " Rough Terrain/Tall Grass");
                encounterAreas.add(roughTerrainArea);
            }

            // 5 surf and 5 rock smash encounters
            EncounterArea surfArea = readEncounterArea(encounterData, 240, 5);
            if (surfArea.size() > 0) {
                surfArea.setDisplayName(mapName + " Surf");
                encounterAreas.add(surfArea);
            }
            EncounterArea rockSmashArea = readEncounterArea(encounterData, 260, 5);
            if (rockSmashArea.size() > 0) {
                rockSmashArea.setDisplayName(mapName + " Rock Smash");
                encounterAreas.add(rockSmashArea);
            }

            // 3 Encounters for each type of rod
            EncounterArea oldRodArea = readEncounterArea(encounterData, 280, 3);
            if (oldRodArea.size() > 0) {
                oldRodArea.setDisplayName(mapName + " Old Rod");
                encounterAreas.add(oldRodArea);
            }
            EncounterArea goodRodArea = readEncounterArea(encounterData, 292, 3);
            if (goodRodArea.size() > 0) {
                goodRodArea.setDisplayName(mapName + " Good Rod");
                encounterAreas.add(goodRodArea);
            }
            EncounterArea superRodArea = readEncounterArea(encounterData, 304, 3);
            if (superRodArea.size() > 0) {
                superRodArea.setDisplayName(mapName + " Super Rod");
                encounterAreas.add(superRodArea);
            }

            // Lastly, 5 for each kind of Horde
            EncounterArea hordeCommonArea = readEncounterArea(encounterData, 316, 5);
            if (hordeCommonArea.size() > 0) {
                hordeCommonArea.setDisplayName(mapName + " Common Horde");
                encounterAreas.add(hordeCommonArea);
            }
            EncounterArea hordeUncommonArea = readEncounterArea(encounterData, 336, 5);
            if (hordeUncommonArea.size() > 0) {
                hordeUncommonArea.setDisplayName(mapName + " Uncommon Horde");
                encounterAreas.add(hordeUncommonArea);
            }
            EncounterArea hordeRareArea = readEncounterArea(encounterData, 356, 5);
            if (hordeRareArea.size() > 0) {
                hordeRareArea.setDisplayName(mapName + " Rare Horde");
                encounterAreas.add(hordeRareArea);
            }
        }

        // The ceiling/flying/rustling bush encounters are hardcoded in the Field CRO
        byte[] fieldCRO = readFile(romEntry.getFile("Field"));
        String currentName = Gen6Constants.fallingEncounterNameMap.get(0);
        int startingOffsetOfCurrentName = 0;
        for (int i = 0; i < Gen6Constants.fallingEncounterCount; i++) {
            int offset = Gen6Constants.fallingEncounterOffset + i * Gen6Constants.fieldEncounterSize;
            EncounterArea fallingArea = readFieldEncounterArea(fieldCRO, offset);
            if (Gen6Constants.fallingEncounterNameMap.containsKey(i)) {
                currentName = Gen6Constants.fallingEncounterNameMap.get(i);
                startingOffsetOfCurrentName = i;
            }
            int encounterNumber = (i - startingOffsetOfCurrentName) + 1;
            fallingArea.setDisplayName(currentName + " #" + encounterNumber);
            encounterAreas.add(fallingArea);
        }
        currentName = Gen6Constants.rustlingBushEncounterNameMap.get(0);
        startingOffsetOfCurrentName = 0;
        for (int i = 0; i < Gen6Constants.rustlingBushEncounterCount; i++) {
            int offset = Gen6Constants.rustlingBushEncounterOffset + i * Gen6Constants.fieldEncounterSize;
            EncounterArea rustlingBushArea = readFieldEncounterArea(fieldCRO, offset);
            if (Gen6Constants.rustlingBushEncounterNameMap.containsKey(i)) {
                currentName = Gen6Constants.rustlingBushEncounterNameMap.get(i);
                startingOffsetOfCurrentName = i;
            }
            int encounterNumber = (i - startingOffsetOfCurrentName) + 1;
            rustlingBushArea.setDisplayName(currentName + " #" + encounterNumber);
            encounterAreas.add(rustlingBushArea);
        }
        return encounterAreas;
    }

    private List<EncounterArea> getEncountersORAS() throws IOException {
        GARCArchive encounterGarc = readGARC(romEntry.getFile("WildPokemon"), false);
        List<EncounterArea> encounterAreas = new ArrayList<>();
        for (int i = 0; i < encounterGarc.files.size() - 2; i++) {
            byte[] b = encounterGarc.files.get(i).get(0);
            if (!wildMapNames.containsKey(i)) {
                wildMapNames.put(i, "? Unknown ?");
            }
            String mapName = wildMapNames.get(i);
            int offset = FileFunctions.readFullInt(b, 0x10) + 0xE;
            int offset2 = FileFunctions.readFullInt(b, 0x14);
            int length = offset2 - offset;
            if (length < 0xF6) { // No encounters in this map
                continue;
            }
            byte[] encounterData = new byte[0xF6];
            System.arraycopy(b, offset, encounterData, 0, 0xF6);

            // First, read 12 grass encounters and 12 long grass encounters
            EncounterArea grassArea = readEncounterArea(encounterData, 0, 12);
            if (grassArea.size() > 0) {
                grassArea.setDisplayName(mapName + " Grass/Cave");
                grassArea.setOffset(i);
                encounterAreas.add(grassArea);
            }
            EncounterArea longGrassArea = readEncounterArea(encounterData, 48, 12);
            if (longGrassArea.size() > 0) {
                longGrassArea.setDisplayName(mapName + " Long Grass");
                longGrassArea.setOffset(i);
                encounterAreas.add(longGrassArea);
            }

            // Now, 3 DexNav Foreign encounters
            EncounterArea dexNavForeignArea = readEncounterArea(encounterData, 96, 3);
            if (dexNavForeignArea.size() > 0) {
                dexNavForeignArea.setDisplayName(mapName + " DexNav Foreign Encounter");
                dexNavForeignArea.setOffset(i);
                encounterAreas.add(dexNavForeignArea);
            }

            // 5 surf and 5 rock smash encounters
            EncounterArea surfArea = readEncounterArea(encounterData, 108, 5);
            if (surfArea.size() > 0) {
                surfArea.setDisplayName(mapName + " Surf");
                surfArea.setOffset(i);
                encounterAreas.add(surfArea);
            }
            EncounterArea rockSmashArea = readEncounterArea(encounterData, 128, 5);
            if (rockSmashArea.size() > 0) {
                rockSmashArea.setDisplayName(mapName + " Rock Smash");
                rockSmashArea.setOffset(i);
                encounterAreas.add(rockSmashArea);
            }

            // 3 Encounters for each type of rod
            EncounterArea oldRodArea = readEncounterArea(encounterData, 148, 3);
            if (oldRodArea.size() > 0) {
                oldRodArea.setDisplayName(mapName + " Old Rod");
                oldRodArea.setOffset(i);
                encounterAreas.add(oldRodArea);
            }
            EncounterArea goodRodArea = readEncounterArea(encounterData, 160, 3);
            if (goodRodArea.size() > 0) {
                goodRodArea.setDisplayName(mapName + " Good Rod");
                goodRodArea.setOffset(i);
                encounterAreas.add(goodRodArea);
            }
            EncounterArea superRodArea = readEncounterArea(encounterData, 172, 3);
            if (superRodArea.size() > 0) {
                superRodArea.setDisplayName(mapName + " Super Rod");
                superRodArea.setOffset(i);
                encounterAreas.add(superRodArea);
            }

            // Lastly, 5 for each kind of Horde
            EncounterArea hordeCommonArea = readEncounterArea(encounterData, 184, 5);
            if (hordeCommonArea.size() > 0) {
                hordeCommonArea.setDisplayName(mapName + " Common Horde");
                hordeCommonArea.setOffset(i);
                encounterAreas.add(hordeCommonArea);
            }
            EncounterArea hordeUncommonArea = readEncounterArea(encounterData, 204, 5);
            if (hordeUncommonArea.size() > 0) {
                hordeUncommonArea.setDisplayName(mapName + " Uncommon Horde");
                hordeUncommonArea.setOffset(i);
                encounterAreas.add(hordeUncommonArea);
            }
            EncounterArea hordeRareArea = readEncounterArea(encounterData, 224, 5);
            if (hordeRareArea.size() > 0) {
                hordeRareArea.setDisplayName(mapName + " Rare Horde");
                hordeRareArea.setOffset(i);
                encounterAreas.add(hordeRareArea);
            }
        }
        return encounterAreas;
    }

    private EncounterArea readEncounterArea(byte[] data, int offset, int amount) {
        EncounterArea area = new EncounterArea();
        area.setRate(1);
        for (int i = 0; i < amount; i++) {
            int species = readWord(data, offset + i * 4) & 0x7FF;
            int forme = readWord(data, offset + i * 4) >> 11;
            if (species != 0) {
                Encounter e = new Encounter();
                Pokemon baseForme = pokes[species];

                // If the forme is purely cosmetic, just use the base forme as the Pokemon
                // for this encounter (the cosmetic forme will be stored in the encounter).
                // Do the same for formes 30 and 31, because they actually aren't formes, but
                // rather act as indicators for what forme should appear when encountered:
                // 30 = Spawn the cosmetic forme specific to the user's region (Scatterbug line)
                // 31 = Spawn *any* cosmetic forme with equal probability (Unown Mirage Cave)
                if (forme <= baseForme.getCosmeticFormCount() || forme == 30 || forme == 31) {
                    e.setPokemon(baseForme);
                } else {
                    e.setPokemon(getAltFormeOfPokemon(baseForme, forme));
                }
                e.setFormeNumber(forme);
                e.setLevel(data[offset + 2 + i * 4]);
                e.setMaxLevel(data[offset + 3 + i * 4]);
                area.add(e);
            }
        }
        return area;
    }

    private EncounterArea readFieldEncounterArea(byte[] data, int offset) {
        EncounterArea area = new EncounterArea();
        for (int i = 0; i < 7; i++) {
            int species = readWord(data, offset + 4 + i * 8);
            int level = data[offset + 8 + i * 8];
            if (species != 0) {
                Encounter e = new Encounter();
                e.setPokemon(pokes[species]);
                e.setFormeNumber(0);
                e.setLevel(level);
                e.setMaxLevel(level);
                area.add(e);
            }
        }
        return area;
    }

    @Override
    public void setEncounters(boolean useTimeOfDay, List<EncounterArea> encounterAreas) {
        try {
            if (romEntry.getRomType() == Gen6Constants.Type_ORAS) {
                setEncountersORAS(encounterAreas);
            } else {
                setEncountersXY(encounterAreas);
            }
        } catch (IOException ex) {
            throw new RomIOException(ex);
        }
    }

    private void setEncountersXY(List<EncounterArea> encounterAreas) throws IOException {
        String encountersFile = romEntry.getFile("WildPokemon");
        GARCArchive encounterGarc = readGARC(encountersFile, false);
        Iterator<EncounterArea> areaIterator = encounterAreas.iterator();
        for (int i = 0; i < encounterGarc.files.size() - 1; i++) {
            byte[] b = encounterGarc.files.get(i).get(0);
            int offset = FileFunctions.readFullInt(b, 0x10) + 0x10;
            int length = b.length - offset;
            if (length < 0x178) { // No encounters in this map
                continue;
            }
            byte[] encounterData = new byte[0x178];
            System.arraycopy(b, offset, encounterData, 0, 0x178);

            // First, 12 grass encounters, 12 rough terrain encounters, and 12 encounters each for yellow/purple/red flowers
            if (readEncounterArea(encounterData, 0, 12).size() > 0) {
                EncounterArea grassArea = areaIterator.next();
                writeEncounters(encounterData, 0, grassArea);
            }
            if (readEncounterArea(encounterData, 48, 12).size() > 0) {
                EncounterArea yellowFlowersArea = areaIterator.next();
                writeEncounters(encounterData, 48, yellowFlowersArea);
            }
            if (readEncounterArea(encounterData, 96, 12).size() > 0) {
                EncounterArea purpleFlowersArea = areaIterator.next();
                writeEncounters(encounterData, 96, purpleFlowersArea);
            }
            if (readEncounterArea(encounterData, 144, 12).size() > 0) {
                EncounterArea redFlowersArea = areaIterator.next();
                writeEncounters(encounterData, 144, redFlowersArea);
            }
            if (readEncounterArea(encounterData, 192, 12).size() > 0) {
                EncounterArea roughTerrainArea = areaIterator.next();
                writeEncounters(encounterData, 192, roughTerrainArea);
            }

            // 5 surf and 5 rock smash encounters
            if (readEncounterArea(encounterData, 240, 5).size() > 0) {
                EncounterArea surfArea = areaIterator.next();
                writeEncounters(encounterData, 240, surfArea);
            }
            if (readEncounterArea(encounterData, 260, 5).size() > 0) {
                EncounterArea rockSmashArea = areaIterator.next();
                writeEncounters(encounterData, 260, rockSmashArea);
            }

            // 3 Encounters for each type of rod
            if (readEncounterArea(encounterData, 280, 3).size() > 0) {
                EncounterArea oldRodArea = areaIterator.next();
                writeEncounters(encounterData, 280, oldRodArea);
            }
            if (readEncounterArea(encounterData, 292, 3).size() > 0) {
                EncounterArea goodRodArea = areaIterator.next();
                writeEncounters(encounterData, 292, goodRodArea);
            }
            if (readEncounterArea(encounterData, 304, 3).size() > 0) {
                EncounterArea superRodArea = areaIterator.next();
                writeEncounters(encounterData, 304, superRodArea);
            }

            // Lastly, 5 for each kind of Horde
            if (readEncounterArea(encounterData, 316, 5).size() > 0) {
                EncounterArea commonHordeArea = areaIterator.next();
                writeEncounters(encounterData, 316, commonHordeArea);
            }
            if (readEncounterArea(encounterData, 336, 5).size() > 0) {
                EncounterArea uncommonHordeArea = areaIterator.next();
                writeEncounters(encounterData, 336, uncommonHordeArea);
            }
            if (readEncounterArea(encounterData, 356, 5).size() > 0) {
                EncounterArea rareHordeArea = areaIterator.next();
                writeEncounters(encounterData, 356, rareHordeArea);
            }

            // Write the encounter data back to the file
            System.arraycopy(encounterData, 0, b, offset, 0x178);
        }

        // Save
        writeGARC(encountersFile, encounterGarc);

        // Now write the encounters hardcoded in the Field CRO
        byte[] fieldCRO = readFile(romEntry.getFile("Field"));
        for (int i = 0; i < Gen6Constants.fallingEncounterCount; i++) {
            int offset = Gen6Constants.fallingEncounterOffset + i * Gen6Constants.fieldEncounterSize;
            EncounterArea fallingArea = areaIterator.next();
            writeFieldEncounters(fieldCRO, offset, fallingArea);
        }
        for (int i = 0; i < Gen6Constants.rustlingBushEncounterCount; i++) {
            int offset = Gen6Constants.rustlingBushEncounterOffset + i * Gen6Constants.fieldEncounterSize;
            EncounterArea rustlingBushArea = areaIterator.next();
            writeFieldEncounters(fieldCRO, offset, rustlingBushArea);
        }

        // Save
        writeFile(romEntry.getFile("Field"), fieldCRO);

        this.updatePokedexAreaDataXY(encounterGarc, fieldCRO);
    }

    private void setEncountersORAS(List<EncounterArea> encounterAreas) throws IOException {
        String encountersFile = romEntry.getFile("WildPokemon");
        GARCArchive encounterGarc = readGARC(encountersFile, false);
        Iterator<EncounterArea> areaIterator = encounterAreas.iterator();
        byte[] decStorage = encounterGarc.files.get(encounterGarc.files.size() - 1).get(0);
        for (int i = 0; i < encounterGarc.files.size() - 2; i++) {
            byte[] b = encounterGarc.files.get(i).get(0);
            int offset = FileFunctions.readFullInt(b, 0x10) + 0xE;
            int offset2 = FileFunctions.readFullInt(b, 0x14);
            int length = offset2 - offset;
            if (length < 0xF6) { // No encounters in this map
                continue;
            }
            byte[] encounterData = new byte[0xF6];
            System.arraycopy(b, offset, encounterData, 0, 0xF6);

            // First, 12 grass encounters and 12 long grass encounters
            if (readEncounterArea(encounterData, 0, 12).size() > 0) {
                EncounterArea grassArea = areaIterator.next();
                writeEncounters(encounterData, 0, grassArea);
            }
            if (readEncounterArea(encounterData, 48, 12).size() > 0) {
                EncounterArea longGrassArea = areaIterator.next();
                writeEncounters(encounterData, 48, longGrassArea);
            }

            // Now, 3 DexNav Foreign encounters
            if (readEncounterArea(encounterData, 96, 3).size() > 0) {
                EncounterArea dexNavArea = areaIterator.next();
                writeEncounters(encounterData, 96, dexNavArea);
            }

            // 5 surf and 5 rock smash encounters
            if (readEncounterArea(encounterData, 108, 5).size() > 0) {
                EncounterArea surfArea = areaIterator.next();
                writeEncounters(encounterData, 108, surfArea);
            }
            if (readEncounterArea(encounterData, 128, 5).size() > 0) {
                EncounterArea rockSmashArea = areaIterator.next();
                writeEncounters(encounterData, 128, rockSmashArea);
            }

            // 3 Encounters for each type of rod
            if (readEncounterArea(encounterData, 148, 3).size() > 0) {
                EncounterArea oldRodArea = areaIterator.next();
                writeEncounters(encounterData, 148, oldRodArea);
            }
            if (readEncounterArea(encounterData, 160, 3).size() > 0) {
                EncounterArea goodRodArea = areaIterator.next();
                writeEncounters(encounterData, 160, goodRodArea);
            }
            if (readEncounterArea(encounterData, 172, 3).size() > 0) {
                EncounterArea superRodArea = areaIterator.next();
                writeEncounters(encounterData, 172, superRodArea);
            }

            // Lastly, 5 for each kind of Horde
            if (readEncounterArea(encounterData, 184, 5).size() > 0) {
                EncounterArea commonHordeArea = areaIterator.next();
                writeEncounters(encounterData, 184, commonHordeArea);
            }
            if (readEncounterArea(encounterData, 204, 5).size() > 0) {
                EncounterArea uncommonHordeArea = areaIterator.next();
                writeEncounters(encounterData, 204, uncommonHordeArea);
            }
            if (readEncounterArea(encounterData, 224, 5).size() > 0) {
                EncounterArea rareHordeArea = areaIterator.next();
                writeEncounters(encounterData, 224, rareHordeArea);
            }

            // Write the encounter data back to the file
            System.arraycopy(encounterData, 0, b, offset, 0xF6);

            // Also write the encounter data to the decStorage file
            int decStorageOffset = FileFunctions.readFullInt(decStorage, (i + 1) * 4) + 0xE;
            System.arraycopy(encounterData, 0, decStorage, decStorageOffset, 0xF4);
        }

        // Save
        writeGARC(encountersFile, encounterGarc);

        this.updatePokedexAreaDataORAS(encounterGarc);
    }

    private void updatePokedexAreaDataXY(GARCArchive encounterGarc, byte[] fieldCRO) throws IOException {
        byte[] pokedexAreaData = new byte[(Gen6Constants.pokemonCount + 1) * Gen6Constants.perPokemonAreaDataLengthXY];
        for (int i = 0; i < pokedexAreaData.length; i += Gen6Constants.perPokemonAreaDataLengthXY) {
            // This byte is 0x10 for *every* Pokemon. Why? No clue, but let's copy it.
            pokedexAreaData[i + 133] = 0x10;
        }
        int currentMapNum = 0;

        // Read all the "normal" encounters in the encounters GARC.
        for (int i = 0; i < encounterGarc.files.size() - 1; i++) {
            byte[] b = encounterGarc.files.get(i).get(0);
            int offset = FileFunctions.readFullInt(b, 0x10) + 0x10;
            int length = b.length - offset;
            if (length < 0x178) { // No encounters in this map
                continue;
            }
            int areaIndex = Gen6Constants.xyMapNumToPokedexIndex[currentMapNum];
            byte[] encounterData = new byte[0x178];
            System.arraycopy(b, offset, encounterData, 0, 0x178);

            EncounterArea grassArea = readEncounterArea(encounterData, 0, 12);
            updatePokedexAreaDataFromEncounterArea(grassArea, pokedexAreaData, areaIndex, 0x1);
            EncounterArea yellowFlowerArea = readEncounterArea(encounterData, 48, 12);
            updatePokedexAreaDataFromEncounterArea(yellowFlowerArea, pokedexAreaData, areaIndex, 0x2);
            EncounterArea purpleFlowerArea = readEncounterArea(encounterData, 96, 12);
            updatePokedexAreaDataFromEncounterArea(purpleFlowerArea, pokedexAreaData, areaIndex, 0x4);
            EncounterArea redFlowerArea = readEncounterArea(encounterData, 144, 12);
            updatePokedexAreaDataFromEncounterArea(redFlowerArea, pokedexAreaData, areaIndex, 0x8);
            EncounterArea roughTerrainArea = readEncounterArea(encounterData, 192, 12);
            updatePokedexAreaDataFromEncounterArea(roughTerrainArea, pokedexAreaData, areaIndex, 0x10);
            EncounterArea surfArea = readEncounterArea(encounterData, 240, 5);
            updatePokedexAreaDataFromEncounterArea(surfArea, pokedexAreaData, areaIndex, 0x20);
            EncounterArea rockSmashArea = readEncounterArea(encounterData, 260, 5);
            updatePokedexAreaDataFromEncounterArea(rockSmashArea, pokedexAreaData, areaIndex, 0x40);
            EncounterArea oldRodArea = readEncounterArea(encounterData, 280, 3);
            updatePokedexAreaDataFromEncounterArea(oldRodArea, pokedexAreaData, areaIndex, 0x80);
            EncounterArea goodRodArea = readEncounterArea(encounterData, 292, 3);
            updatePokedexAreaDataFromEncounterArea(goodRodArea, pokedexAreaData, areaIndex, 0x100);
            EncounterArea superRodArea = readEncounterArea(encounterData, 304, 3);
            updatePokedexAreaDataFromEncounterArea(superRodArea, pokedexAreaData, areaIndex, 0x200);
            EncounterArea hordeCommonArea = readEncounterArea(encounterData, 316, 5);
            updatePokedexAreaDataFromEncounterArea(hordeCommonArea, pokedexAreaData, areaIndex, 0x400);
            EncounterArea hordeUncommonArea = readEncounterArea(encounterData, 336, 5);
            updatePokedexAreaDataFromEncounterArea(hordeUncommonArea, pokedexAreaData, areaIndex, 0x400);
            EncounterArea hordeRareArea = readEncounterArea(encounterData, 356, 5);
            updatePokedexAreaDataFromEncounterArea(hordeRareArea, pokedexAreaData, areaIndex, 0x400);
            currentMapNum++;
        }

        // Now read all the stuff that's hardcoded in the Field CRO
        for (int i = 0; i < Gen6Constants.fallingEncounterCount; i++) {
            int areaIndex = Gen6Constants.xyMapNumToPokedexIndex[currentMapNum];
            int offset = Gen6Constants.fallingEncounterOffset + i * Gen6Constants.fieldEncounterSize;
            EncounterArea fallingArea = readFieldEncounterArea(fieldCRO, offset);
            updatePokedexAreaDataFromEncounterArea(fallingArea, pokedexAreaData, areaIndex, 0x800);
            currentMapNum++;
        }
        for (int i = 0; i < Gen6Constants.rustlingBushEncounterCount; i++) {
            int areaIndex = Gen6Constants.xyMapNumToPokedexIndex[currentMapNum];
            int offset = Gen6Constants.rustlingBushEncounterOffset + i * Gen6Constants.fieldEncounterSize;
            EncounterArea rustlingBushArea = readFieldEncounterArea(fieldCRO, offset);
            updatePokedexAreaDataFromEncounterArea(rustlingBushArea, pokedexAreaData, areaIndex, 0x800);
            currentMapNum++;
        }

        // Write out the newly-created area data to the GARC
        GARCArchive pokedexAreaGarc = readGARC(romEntry.getFile("PokedexAreaData"), true);
        pokedexAreaGarc.setFile(0, pokedexAreaData);
        writeGARC(romEntry.getFile("PokedexAreaData"), pokedexAreaGarc);
    }

    private void updatePokedexAreaDataORAS(GARCArchive encounterGarc) throws IOException {
        byte[] pokedexAreaData = new byte[(Gen6Constants.pokemonCount + 1) * Gen6Constants.perPokemonAreaDataLengthORAS];
        int currentMapNum = 0;
        for (int i = 0; i < encounterGarc.files.size() - 2; i++) {
            byte[] b = encounterGarc.files.get(i).get(0);
            int offset = FileFunctions.readFullInt(b, 0x10) + 0xE;
            int offset2 = FileFunctions.readFullInt(b, 0x14);
            int length = offset2 - offset;
            if (length < 0xF6) { // No encounters in this map
                continue;
            }
            int areaIndex = Gen6Constants.orasMapNumToPokedexIndex[currentMapNum];
            if (areaIndex == -1) { // Current encounters are not taken into account for the Pokedex
                currentMapNum++;
                continue;
            }
            byte[] encounterData = new byte[0xF6];
            System.arraycopy(b, offset, encounterData, 0, 0xF6);

            EncounterArea grassArea = readEncounterArea(encounterData, 0, 12);
            updatePokedexAreaDataFromEncounterArea(grassArea, pokedexAreaData, areaIndex, 0x1);
            EncounterArea longGrassArea = readEncounterArea(encounterData, 48, 12);
            updatePokedexAreaDataFromEncounterArea(longGrassArea, pokedexAreaData, areaIndex, 0x2);
            int foreignEncounterType = grassArea.size() > 0 ? 0x04 : 0x08;
            EncounterArea dexNavForeignArea = readEncounterArea(encounterData, 96, 3);
            updatePokedexAreaDataFromEncounterArea(dexNavForeignArea, pokedexAreaData, areaIndex, foreignEncounterType);
            EncounterArea surfArea = readEncounterArea(encounterData, 108, 5);
            updatePokedexAreaDataFromEncounterArea(surfArea, pokedexAreaData, areaIndex, 0x10);
            EncounterArea rockSmashArea = readEncounterArea(encounterData, 128, 5);
            updatePokedexAreaDataFromEncounterArea(rockSmashArea, pokedexAreaData, areaIndex, 0x20);
            EncounterArea oldRodArea = readEncounterArea(encounterData, 148, 3);
            updatePokedexAreaDataFromEncounterArea(oldRodArea, pokedexAreaData, areaIndex, 0x40);
            EncounterArea goodRodArea = readEncounterArea(encounterData, 160, 3);
            updatePokedexAreaDataFromEncounterArea(goodRodArea, pokedexAreaData, areaIndex, 0x80);
            EncounterArea superRodArea = readEncounterArea(encounterData, 172, 3);
            updatePokedexAreaDataFromEncounterArea(superRodArea, pokedexAreaData, areaIndex, 0x100);
            EncounterArea hordeCommonArea = readEncounterArea(encounterData, 184, 5);
            updatePokedexAreaDataFromEncounterArea(hordeCommonArea, pokedexAreaData, areaIndex, 0x200);
            EncounterArea hordeUncommonArea = readEncounterArea(encounterData, 204, 5);
            updatePokedexAreaDataFromEncounterArea(hordeUncommonArea, pokedexAreaData, areaIndex, 0x200);
            EncounterArea hordeRareArea = readEncounterArea(encounterData, 224, 5);
            updatePokedexAreaDataFromEncounterArea(hordeRareArea, pokedexAreaData, areaIndex, 0x200);
            currentMapNum++;
        }

        GARCArchive pokedexAreaGarc = readGARC(romEntry.getFile("PokedexAreaData"), true);
        pokedexAreaGarc.setFile(0, pokedexAreaData);
        writeGARC(romEntry.getFile("PokedexAreaData"), pokedexAreaGarc);
    }

    private void updatePokedexAreaDataFromEncounterArea(EncounterArea area, byte[] pokedexAreaData, int areaIndex, int encounterType) {
        for (Encounter enc : area) {
            Pokemon pk = enc.getPokemon();
            int perPokemonAreaDataLength = romEntry.getRomType() == Gen6Constants.Type_XY ?
                    Gen6Constants.perPokemonAreaDataLengthXY : Gen6Constants.perPokemonAreaDataLengthORAS;
            int offset = pk.getBaseNumber() * perPokemonAreaDataLength + areaIndex * 4;
            int value = FileFunctions.readFullInt(pokedexAreaData, offset);
            value |= encounterType;
            FileFunctions.writeFullInt(pokedexAreaData, offset, value);
        }
    }

    private void writeEncounters(byte[] data, int offset, List<Encounter> encounters) {
        for (int i = 0; i < encounters.size(); i++) {
            Encounter encounter = encounters.get(i);
            int speciesAndFormeData = (encounter.getFormeNumber() << 11) + encounter.getPokemon().getBaseNumber();
            writeWord(data, offset + i * 4, speciesAndFormeData);
            data[offset + 2 + i * 4] = (byte) encounter.getLevel();
            data[offset + 3 + i * 4] = (byte) encounter.getMaxLevel();
        }
    }

    private void writeFieldEncounters(byte[] data, int offset, List<Encounter> encounters) {
        for (int i = 0; i < encounters.size(); i++) {
            Encounter encounter = encounters.get(i);
            writeWord(data, offset + 4 + i * 8, encounter.getPokemon().getBaseNumber());
            data[offset + 8 + i * 8] = (byte) encounter.getLevel();
        }
    }

    private void loadWildMapNames() {
        try {
            wildMapNames = new HashMap<>();
            GARCArchive encounterGarc = this.readGARC(romEntry.getFile("WildPokemon"), false);
            int zoneDataOffset = romEntry.getIntValue("MapTableFileOffset");
            byte[] zoneData = encounterGarc.files.get(zoneDataOffset).get(0);
            List<String> allMapNames = getStrings(false, romEntry.getIntValue("MapNamesTextOffset"));
            for (int map = 0; map < zoneDataOffset; map++) {
                int indexNum = (map * 56) + 0x1C;
                int nameIndex1 = zoneData[indexNum] & 0xFF;
                int nameIndex2 = 0x100 * ((int) (zoneData[indexNum + 1]) & 1);
                String mapName = allMapNames.get(nameIndex1 + nameIndex2);
                wildMapNames.put(map, mapName);
            }
            loadedWildMapNames = true;
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    public List<Trainer> getTrainers() {
        List<Trainer> allTrainers = new ArrayList<>();
        boolean isORAS = romEntry.getRomType() == Gen6Constants.Type_ORAS;
        try {
            GARCArchive trainers = this.readGARC(romEntry.getFile("TrainerData"),true);
            GARCArchive trpokes = this.readGARC(romEntry.getFile("TrainerPokemon"),true);
            int trainernum = trainers.files.size();
            List<String> tclasses = this.getTrainerClassNames();
            List<String> tnames = this.getTrainerNames();
            Map<Integer,String> tnamesMap = new TreeMap<>();
            for (int i = 0; i < tnames.size(); i++) {
                tnamesMap.put(i,tnames.get(i));
            }
            for (int i = 1; i < trainernum; i++) {
                // Trainer entries are 20 bytes in X/Y, 24 bytes in ORAS
                // Team flags; 1 byte; 0x01 = custom moves, 0x02 = held item
                // [ORAS only] 1 byte unused
                // Class; 1 byte
                // [ORAS only] 1 byte unknown
                // [ORAS only] 2 bytes unused
                // Battle Mode; 1 byte; 0=single, 1=double, 2=triple, 3=rotation, 4=???
                // Number of pokemon in team; 1 byte
                // Items; 2 bytes each, 4 item slots
                // AI Flags; 2 byte
                // 3 bytes not used
                // Victory Money; 1 byte; The money given out after defeat =
                //         4 * this value * highest level poke in party
                // Victory Item; 2 bytes; The item given out after defeat.
                //         In X/Y, these are berries, nuggets, pearls (e.g. Battle Chateau)
                //         In ORAS, none of these are set.
                byte[] trainer = trainers.files.get(i).get(0);
                byte[] trpoke = trpokes.files.get(i).get(0);
                Trainer tr = new Trainer();
                tr.poketype = isORAS ? readWord(trainer,0) : trainer[0] & 0xFF;
                tr.index = i;
                tr.trainerclass = isORAS ? readWord(trainer,2) : trainer[1] & 0xFF;
                int offset = isORAS ? 6 : 2;
                int battleType = trainer[offset] & 0xFF;
                int numPokes = trainer[offset+1] & 0xFF;
                boolean healer = trainer[offset+13] != 0;
                int pokeOffs = 0;
                String trainerClass = tclasses.get(tr.trainerclass);
                String trainerName = tnamesMap.getOrDefault(i - 1, "UNKNOWN");
                tr.fullDisplayName = trainerClass + " " + trainerName;

                for (int poke = 0; poke < numPokes; poke++) {
                    // Structure is
                    // ST SB LV LV SP SP FRM FRM
                    // (HI HI)
                    // (M1 M1 M2 M2 M3 M3 M4 M4)
                    // ST (strength) corresponds to the IVs of a trainer's pokemon.
                    //   In ORAS, this value is like previous gens, a number 0-255
                    //   to represent 0 to 31 IVs. In the vanilla games, the top
                    //   leaders/champions have 29.
                    //   In X/Y, the bottom 5 bits are the IVs. It is unknown what
                    //   the top 3 bits correspond to, perhaps EV spread?
                    // The second byte, SB = 0 0 Ab Ab 0 0 Fm Ml
                    //   Ab Ab = ability number, 0 for random
                    //   Fm = 1 for forced female
                    //   Ml = 1 for forced male
                    // There's also a trainer flag to force gender, but
                    // this allows fixed teams with mixed genders.

                    int level = readWord(trpoke, pokeOffs + 2);
                    int species = readWord(trpoke, pokeOffs + 4);
                    int formnum = readWord(trpoke, pokeOffs + 6);
                    TrainerPokemon tpk = new TrainerPokemon();
                    tpk.level = level;
                    tpk.pokemon = pokes[species];
                    tpk.strength = trpoke[pokeOffs];
                    if (isORAS) {
                        tpk.IVs = (tpk.strength * 31 / 255);
                    } else {
                        tpk.IVs = tpk.strength & 0x1F;
                    }
                    int abilityAndFlag = trpoke[pokeOffs + 1];
                    tpk.abilitySlot = (abilityAndFlag >>> 4) & 0xF;
                    tpk.forcedGenderFlag = (abilityAndFlag & 0xF);
                    tpk.forme = formnum;
                    tpk.formeSuffix = Gen6Constants.getFormeSuffixByBaseForme(species,formnum);
                    pokeOffs += 8;
                    if (tr.pokemonHaveItems()) {
                        tpk.heldItem = readWord(trpoke, pokeOffs);
                        pokeOffs += 2;
                        tpk.hasMegaStone = Gen6Constants.isMegaStone(tpk.heldItem);
                    }
                    if (tr.pokemonHaveCustomMoves()) {
                        for (int move = 0; move < 4; move++) {
                            tpk.moves[move] = readWord(trpoke, pokeOffs + (move*2));
                        }
                        pokeOffs += 8;
                    }
                    tr.pokemon.add(tpk);
                }
                allTrainers.add(tr);
            }
            if (romEntry.getRomType() == Gen6Constants.Type_XY) {
                Gen6Constants.tagTrainersXY(allTrainers);
                Gen6Constants.setMultiBattleStatusXY(allTrainers);
            } else {
                Gen6Constants.tagTrainersORAS(allTrainers);
                Gen6Constants.setMultiBattleStatusORAS(allTrainers);
            }
        } catch (IOException ex) {
            throw new RomIOException(ex);
        }
        return allTrainers;
    }

    @Override
    public List<Integer> getMainPlaythroughTrainers() {
        return new ArrayList<>();
    }

    @Override
    public List<Integer> getEliteFourTrainers(boolean isChallengeMode) {
        return Arrays.stream(romEntry.getArrayValue("EliteFourIndices")).boxed().collect(Collectors.toList());
    }

    @Override
    public List<Integer> getEvolutionItems() {
        return Gen6Constants.evolutionItems;
    }

    @Override
    public void setTrainers(List<Trainer> trainerData) {
        Iterator<Trainer> allTrainers = trainerData.iterator();
        boolean isORAS = romEntry.getRomType() == Gen6Constants.Type_ORAS;
        try {
            GARCArchive trainers = this.readGARC(romEntry.getFile("TrainerData"),true);
            GARCArchive trpokes = this.readGARC(romEntry.getFile("TrainerPokemon"),true);
            // Get current movesets in case we need to reset them for certain
            // trainer mons.
            Map<Integer, List<MoveLearnt>> movesets = this.getMovesLearnt();
            int trainernum = trainers.files.size();
            for (int i = 1; i < trainernum; i++) {
                byte[] trainer = trainers.files.get(i).get(0);
                Trainer tr = allTrainers.next();
                // preserve original poketype for held item & moves
                int offset = 0;
                if (isORAS) {
                    writeWord(trainer,0,tr.poketype);
                    offset = 4;
                } else {
                    trainer[0] = (byte) tr.poketype;
                }
                int numPokes = tr.pokemon.size();
                trainer[offset+3] = (byte) numPokes;

                if (tr.forcedDoubleBattle) {
                    if (trainer[offset+2] == 0) {
                        trainer[offset+2] = 1;
                        trainer[offset+12] |= 0x80; // Flag that needs to be set for trainers not to attack their own pokes
                    }
                }

                int bytesNeeded = 8 * numPokes;
                if (tr.pokemonHaveCustomMoves()) {
                    bytesNeeded += 8 * numPokes;
                }
                if (tr.pokemonHaveItems()) {
                    bytesNeeded += 2 * numPokes;
                }
                byte[] trpoke = new byte[bytesNeeded];
                int pokeOffs = 0;
                Iterator<TrainerPokemon> tpokes = tr.pokemon.iterator();
                for (int poke = 0; poke < numPokes; poke++) {
                    TrainerPokemon tp = tpokes.next();
                    byte abilityAndFlag = (byte)((tp.abilitySlot << 4) | tp.forcedGenderFlag);
                    trpoke[pokeOffs] = (byte) tp.strength;
                    trpoke[pokeOffs + 1] = abilityAndFlag;
                    writeWord(trpoke, pokeOffs + 2, tp.level);
                    writeWord(trpoke, pokeOffs + 4, tp.pokemon.getNumber());
                    writeWord(trpoke, pokeOffs + 6, tp.forme);
                    pokeOffs += 8;
                    if (tr.pokemonHaveItems()) {
                        writeWord(trpoke, pokeOffs, tp.heldItem);
                        pokeOffs += 2;
                    }
                    if (tr.pokemonHaveCustomMoves()) {
                        if (tp.resetMoves) {
                            int[] pokeMoves = RomFunctions.getMovesAtLevel(getAltFormeOfPokemon(tp.pokemon, tp.forme).getNumber(), movesets, tp.level);
                            for (int m = 0; m < 4; m++) {
                                writeWord(trpoke, pokeOffs + m * 2, pokeMoves[m]);
                            }
                        } else {
                            writeWord(trpoke, pokeOffs, tp.moves[0]);
                            writeWord(trpoke, pokeOffs + 2, tp.moves[1]);
                            writeWord(trpoke, pokeOffs + 4, tp.moves[2]);
                            writeWord(trpoke, pokeOffs + 6, tp.moves[3]);
                        }
                        pokeOffs += 8;
                    }
                }
                trpokes.setFile(i,trpoke);
            }
            this.writeGARC(romEntry.getFile("TrainerData"), trainers);
            this.writeGARC(romEntry.getFile("TrainerPokemon"), trpokes);
        } catch (IOException ex) {
            throw new RomIOException(ex);
        }
    }

    @Override
    public Map<Integer, List<MoveLearnt>> getMovesLearnt() {
        Map<Integer, List<MoveLearnt>> movesets = new TreeMap<>();
        try {
            GARCArchive movesLearnt = this.readGARC(romEntry.getFile("PokemonMovesets"),true);
            int formeCount = Gen6Constants.getFormeCount(romEntry.getRomType());
//            int formeOffset = Gen5Constants.getFormeMovesetOffset(romEntry.getRomType());
            for (int i = 1; i <= Gen6Constants.pokemonCount + formeCount; i++) {
                Pokemon pkmn = pokes[i];
                byte[] movedata;
//                if (i > Gen6Constants.pokemonCount) {
//                    movedata = movesLearnt.files.get(i + formeOffset);
//                } else {
//                    movedata = movesLearnt.files.get(i);
//                }
                movedata = movesLearnt.files.get(i).get(0);
                int moveDataLoc = 0;
                List<MoveLearnt> learnt = new ArrayList<>();
                while (readWord(movedata, moveDataLoc) != 0xFFFF || readWord(movedata, moveDataLoc + 2) != 0xFFFF) {
                    int move = readWord(movedata, moveDataLoc);
                    int level = readWord(movedata, moveDataLoc + 2);
                    MoveLearnt ml = new MoveLearnt();
                    ml.level = level;
                    ml.move = move;
                    learnt.add(ml);
                    moveDataLoc += 4;
                }
                movesets.put(pkmn.getNumber(), learnt);
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }
        return movesets;
    }

    @Override
    public void setMovesLearnt(Map<Integer, List<MoveLearnt>> movesets) {
        try {
            GARCArchive movesLearnt = readGARC(romEntry.getFile("PokemonMovesets"),true);
            int formeCount = Gen6Constants.getFormeCount(romEntry.getRomType());
//            int formeOffset = Gen6Constants.getFormeMovesetOffset(romEntry.getRomType());
            for (int i = 1; i <= Gen6Constants.pokemonCount + formeCount; i++) {
                Pokemon pkmn = pokes[i];
                List<MoveLearnt> learnt = movesets.get(pkmn.getNumber());
                int sizeNeeded = learnt.size() * 4 + 4;
                byte[] moveset = new byte[sizeNeeded];
                int j = 0;
                for (; j < learnt.size(); j++) {
                    MoveLearnt ml = learnt.get(j);
                    writeWord(moveset, j * 4, ml.move);
                    writeWord(moveset, j * 4 + 2, ml.level);
                }
                writeWord(moveset, j * 4, 0xFFFF);
                writeWord(moveset, j * 4 + 2, 0xFFFF);
//                if (i > Gen5Constants.pokemonCount) {
//                    movesLearnt.files.set(i + formeOffset, moveset);
//                } else {
//                    movesLearnt.files.set(i, moveset);
//                }
                movesLearnt.setFile(i, moveset);
            }
            // Save
            this.writeGARC(romEntry.getFile("PokemonMovesets"), movesLearnt);
        } catch (IOException e) {
            throw new RomIOException(e);
        }

    }

    @Override
    public Map<Integer, List<Integer>> getEggMoves() {
        Map<Integer, List<Integer>> eggMoves = new TreeMap<>();
        try {
            GARCArchive eggMovesGarc = this.readGARC(romEntry.getFile("EggMoves"),true);
            for (int i = 1; i <= Gen6Constants.pokemonCount; i++) {
                Pokemon pkmn = pokes[i];
                byte[] movedata = eggMovesGarc.files.get(i).get(0);
                int numberOfEggMoves = readWord(movedata, 0);
                List<Integer> moves = new ArrayList<>();
                for (int j = 0; j < numberOfEggMoves; j++) {
                    int move = readWord(movedata, 2 + (j * 2));
                    moves.add(move);
                }
                eggMoves.put(pkmn.getNumber(), moves);
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }
        return eggMoves;
    }

    @Override
    public void setEggMoves(Map<Integer, List<Integer>> eggMoves) {
        try {
            GARCArchive eggMovesGarc = this.readGARC(romEntry.getFile("EggMoves"), true);
            for (int i = 1; i <= Gen6Constants.pokemonCount; i++) {
                Pokemon pkmn = pokes[i];
                byte[] movedata = eggMovesGarc.files.get(i).get(0);
                List<Integer> moves = eggMoves.get(pkmn.getNumber());
                for (int j = 0; j < moves.size(); j++) {
                    writeWord(movedata, 2 + (j * 2), moves.get(j));
                }
            }
            // Save
            this.writeGARC(romEntry.getFile("EggMoves"), eggMovesGarc);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    public boolean hasStaticAltFormes() {
        return true;
    }

    @Override
    public List<StaticEncounter> getStaticPokemon() {
        List<StaticEncounter> statics = new ArrayList<>();
        try {
            byte[] staticCRO = readFile(romEntry.getFile("StaticPokemon"));

            // Static Pokemon
            int count = Gen6Constants.getStaticPokemonCount(romEntry.getRomType());
            int size = Gen6Constants.staticPokemonSize;
            int offset = romEntry.getIntValue("StaticPokemonOffset");
            for (int i = 0; i < count; i++) {
                StaticEncounter se = new StaticEncounter();
                int species = FileFunctions.read2ByteInt(staticCRO,offset+i*size);
                Pokemon pokemon = pokes[species];
                int forme = staticCRO[offset+i*size + 2];
                if (forme > pokemon.getCosmeticFormCount() && forme != 30 && forme != 31) {
                    pokemon = getAltFormeOfPokemon(pokemon, forme);
                }
                se.pkmn = pokemon;
                se.forme = forme;
                se.level = staticCRO[offset+i*size + 3];
                short heldItem = (short)FileFunctions.read2ByteInt(staticCRO,offset+i*size + 4);
                if (heldItem < 0) {
                    heldItem = 0;
                }
                se.heldItem = heldItem;
                statics.add(se);
            }

            List<Integer> skipStarters = Arrays.stream(romEntry.getArrayValue("StarterIndices")).boxed().toList();

            // Gift Pokemon
            count = Gen6Constants.getGiftPokemonCount(romEntry.getRomType());
            size = Gen6Constants.getGiftPokemonSize(romEntry.getRomType());
            offset = romEntry.getIntValue("GiftPokemonOffset");
            for (int i = 0; i < count; i++) {
                if (skipStarters.contains(i)) continue;
                StaticEncounter se = new StaticEncounter();
                int species = FileFunctions.read2ByteInt(staticCRO,offset+i*size);
                Pokemon pokemon = pokes[species];
                int forme = staticCRO[offset+i*size + 4];
                if (forme > pokemon.getCosmeticFormCount() && forme != 30 && forme != 31) {
                    pokemon = getAltFormeOfPokemon(pokemon, forme);
                }
                se.pkmn = pokemon;
                se.forme = forme;
                se.level = staticCRO[offset+i*size + 5];
                int heldItem = FileFunctions.readFullInt(staticCRO,offset+i*size + 12);
                if (heldItem < 0) {
                    heldItem = 0;
                }
                se.heldItem = heldItem;
                if (romEntry.getRomType() == Gen6Constants.Type_ORAS) {
                    int metLocation = FileFunctions.read2ByteInt(staticCRO, offset + i * size + 18);
                    if (metLocation == 0xEA64) {
                        se.isEgg = true;
                    }
                }
                statics.add(se);
            }

            // X/Y Trash Can Pokemon
            if (romEntry.getRomType() == Gen6Constants.Type_XY) {
                int tableBaseOffset = find(code, Gen6Constants.xyTrashEncountersTablePrefix);
                if (tableBaseOffset > 0) {
                    tableBaseOffset += Gen6Constants.xyTrashEncountersTablePrefix.length() / 2; // because it was a prefix
                    statics.addAll(readTrashCanEncounterSet(tableBaseOffset, Gen6Constants.pokemonVillageGarbadorOffset, Gen6Constants.pokemonVillageGarbadorCount, true));
                    statics.addAll(readTrashCanEncounterSet(tableBaseOffset, Gen6Constants.pokemonVillageBanetteOffset, Gen6Constants.pokemonVillageBanetteCount, true));
                    statics.addAll(readTrashCanEncounterSet(tableBaseOffset, Gen6Constants.lostHotelGarbadorOffset, Gen6Constants.lostHotelGarbadorCount, true));
                    statics.addAll(readTrashCanEncounterSet(tableBaseOffset, Gen6Constants.lostHotelTrubbishOffset, Gen6Constants.lostHotelTrubbishCount, true));
                    statics.addAll(readTrashCanEncounterSet(tableBaseOffset, Gen6Constants.lostHotelRotomOffset, Gen6Constants.lostHotelRotomCount, false));
                }
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }

        consolidateLinkedEncounters(statics);
        return statics;
    }

    private void consolidateLinkedEncounters(List<StaticEncounter> statics) {
        List<StaticEncounter> encountersToRemove = new ArrayList<>();
        for (ThreeDSLinkedEncounter le : romEntry.getLinkedEncounters()) {
            StaticEncounter baseEncounter = statics.get(le.getBase());
            StaticEncounter linkedEncounter = statics.get(le.getLinked());
            baseEncounter.linkedEncounters.add(linkedEncounter);
            encountersToRemove.add(linkedEncounter);
        }
        for (StaticEncounter encounter : encountersToRemove) {
            statics.remove(encounter);
        }
    }

    private List<StaticEncounter> readTrashCanEncounterSet(int tableBaseOffset, int offsetWithinTable, int count,
                                                           boolean consolidateSameSpeciesEncounters) {
        List<StaticEncounter> statics = new ArrayList<>();
        Map<Pokemon, StaticEncounter> encounterSet = new HashMap<>();
        int offset = tableBaseOffset + (offsetWithinTable * Gen6Constants.xyTrashEncounterDataLength);
        for (int i = offsetWithinTable; i < offsetWithinTable + count; i++) {
            StaticEncounter se = readTrashCanEncounter(offset);
            if (consolidateSameSpeciesEncounters && encounterSet.containsKey(se.pkmn)) {
                StaticEncounter mainEncounter = encounterSet.get(se.pkmn);
                mainEncounter.linkedEncounters.add(se);
            } else {
                statics.add(se);
                encounterSet.put(se.pkmn, se);
            }
            offset += Gen6Constants.xyTrashEncounterDataLength;
        }
        return statics;
    }

    private StaticEncounter readTrashCanEncounter(int offset) {
        int species = FileFunctions.readFullInt(code, offset);
        int forme = FileFunctions.readFullInt(code, offset + 4);
        int level = FileFunctions.readFullInt(code, offset + 8);
        StaticEncounter se = new StaticEncounter();
        Pokemon pokemon = pokes[species];
        if (forme > pokemon.getCosmeticFormCount() && forme != 30 && forme != 31) {
            pokemon = getAltFormeOfPokemon(pokemon, forme);
        }
        se.pkmn = pokemon;
        se.forme = forme;
        se.level = level;
        return se;
    }

    @Override
    public boolean setStaticPokemon(List<StaticEncounter> staticPokemon) {
        // Static Pokemon
        try {
            byte[] staticCRO = readFile(romEntry.getFile("StaticPokemon"));

            unlinkStaticEncounters(staticPokemon);
            Iterator<StaticEncounter> staticIter = staticPokemon.iterator();

            int staticCount = Gen6Constants.getStaticPokemonCount(romEntry.getRomType());
            int size = Gen6Constants.staticPokemonSize;
            int offset = romEntry.getIntValue("StaticPokemonOffset");
            for (int i = 0; i < staticCount; i++) {
                StaticEncounter se = staticIter.next();
                writeWord(staticCRO,offset+i*size, se.pkmn.getBaseNumber());
                staticCRO[offset+i*size + 2] = (byte)se.forme;
                staticCRO[offset+i*size + 3] = (byte)se.level;
                if (se.heldItem == 0) {
                    writeWord(staticCRO,offset+i*size + 4,-1);
                } else {
                    writeWord(staticCRO,offset+i*size + 4,se.heldItem);
                }
            }

            List<Integer> skipStarters = Arrays.stream(romEntry.getArrayValue("StarterIndices")).boxed().toList();

            // Gift Pokemon
            int giftCount = Gen6Constants.getGiftPokemonCount(romEntry.getRomType());
            size = Gen6Constants.getGiftPokemonSize(romEntry.getRomType());
            offset = romEntry.getIntValue("GiftPokemonOffset");
            for (int i = 0; i < giftCount; i++) {
                if (skipStarters.contains(i)) continue;
                StaticEncounter se = staticIter.next();
                writeWord(staticCRO,offset+i*size, se.pkmn.getBaseNumber());
                staticCRO[offset+i*size + 4] = (byte)se.forme;
                staticCRO[offset+i*size + 5] = (byte)se.level;
                if (se.heldItem == 0) {
                    FileFunctions.writeFullInt(staticCRO,offset+i*size + 12,-1);
                } else {
                    FileFunctions.writeFullInt(staticCRO,offset+i*size + 12,se.heldItem);
                }
            }
            writeFile(romEntry.getFile("StaticPokemon"),staticCRO);

            // X/Y Trash Can Pokemon
            if (romEntry.getRomType() == Gen6Constants.Type_XY) {
                offset = find(code, Gen6Constants.xyTrashEncountersTablePrefix);
                if (offset > 0) {
                    offset += Gen6Constants.xyTrashEncountersTablePrefix.length() / 2; // because it was a prefix
                    int currentCount = 0;
                    while (currentCount != Gen6Constants.xyTrashCanEncounterCount) {
                        StaticEncounter se = staticIter.next();
                        FileFunctions.writeFullInt(code, offset, se.pkmn.getBaseNumber());
                        FileFunctions.writeFullInt(code, offset + 4, se.forme);
                        FileFunctions.writeFullInt(code, offset + 8, se.level);
                        offset += Gen6Constants.xyTrashEncounterDataLength;
                        currentCount++;
                        for (int i = 0; i < se.linkedEncounters.size(); i++) {
                            StaticEncounter linkedEncounter = se.linkedEncounters.get(i);
                            FileFunctions.writeFullInt(code, offset, linkedEncounter.pkmn.getBaseNumber());
                            FileFunctions.writeFullInt(code, offset + 4, linkedEncounter.forme);
                            FileFunctions.writeFullInt(code, offset + 8, linkedEncounter.level);
                            offset += Gen6Constants.xyTrashEncounterDataLength;
                            currentCount++;
                        }
                    }
                }
            }

            if (romEntry.getRomType() == Gen6Constants.Type_XY) {
                int[] boxLegendaryOffsets = romEntry.getArrayValue("BoxLegendaryOffsets");
                StaticEncounter boxLegendaryEncounter = staticPokemon.get(boxLegendaryOffsets[0]);
                fixBoxLegendariesXY(boxLegendaryEncounter.pkmn.getNumber());
                setRoamersXY(staticPokemon);
            } else {
                StaticEncounter rayquazaEncounter = staticPokemon.get(romEntry.getIntValue("RayquazaEncounterNumber"));
                fixRayquazaORAS(rayquazaEncounter.pkmn.getNumber());
            }

            return true;
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    private void unlinkStaticEncounters(List<StaticEncounter> statics) {
        List<Integer> offsetsToInsert = new ArrayList<>();
        for (ThreeDSLinkedEncounter le : romEntry.getLinkedEncounters()) {
            offsetsToInsert.add(le.getLinked());
        }
        Collections.sort(offsetsToInsert);
        for (Integer offsetToInsert : offsetsToInsert) {
            statics.add(offsetToInsert, new StaticEncounter());
        }
        for (ThreeDSLinkedEncounter le : romEntry.getLinkedEncounters()) {
            StaticEncounter baseEncounter = statics.get(le.getBase());
            statics.set(le.getLinked(), baseEncounter.linkedEncounters.get(0));
        }
    }

    private void fixBoxLegendariesXY(int boxLegendarySpecies) throws IOException {
        // We need to edit the script file or otherwise the text will still say "Xerneas" or "Yveltal"
        GARCArchive encounterGarc = readGARC(romEntry.getFile("WildPokemon"), false);
        byte[] boxLegendaryRoomData = encounterGarc.getFile(Gen6Constants.boxLegendaryEncounterFileXY);
        AMX localScript = new AMX(boxLegendaryRoomData, 1);
        byte[] data = localScript.decData;
        int[] boxLegendaryScriptOffsets = romEntry.getArrayValue("BoxLegendaryScriptOffsets");
        for (int offset : boxLegendaryScriptOffsets) {
            FileFunctions.write2ByteInt(data, offset, boxLegendarySpecies);
        }
        byte[] modifiedScript = localScript.getBytes();
        System.arraycopy(modifiedScript, 0, boxLegendaryRoomData, Gen6Constants.boxLegendaryLocalScriptOffsetXY, modifiedScript.length);
        encounterGarc.setFile(Gen6Constants.boxLegendaryEncounterFileXY, boxLegendaryRoomData);
        writeGARC(romEntry.getFile("WildPokemon"), encounterGarc);

        // We also need to edit DllField.cro so that the hardcoded checks for
        // Xerneas's/Yveltal's ID will instead be checks for our randomized species ID.
        byte[] staticCRO = readFile(romEntry.getFile("StaticPokemon"));
        int functionOffset = find(staticCRO, Gen6Constants.boxLegendaryFunctionPrefixXY);
        if (functionOffset > 0) {
            functionOffset += Gen6Constants.boxLegendaryFunctionPrefixXY.length() / 2; // because it was a prefix

            // At multiple points in the function, the game calls pml::pokepara::CoreParam::GetMonNo
            // and compares the result to r8; every single one of these comparisons is followed by a
            // nop. However, the way in which the species ID is loaded into r8 differs depending on
            // the game. We'd prefer to write the same assembly for both games, and there's a trick
            // we can abuse to do so. Since the species ID is never used outside of this comparison,
            // we can feel free to mutate it however we please. The below code allows us to write any
            // arbitrary species ID and make the proper comparison like this:
            // sub r0, r0, (speciesLower x 0x100)
            // subs r0, r0, speciesUpper
            int speciesUpper = boxLegendarySpecies & 0x00FF;
            int speciesLower = (boxLegendarySpecies & 0xFF00) >> 8;
            for (int i = 0; i < Gen6Constants.boxLegendaryCodeOffsetsXY.length; i++) {
                int codeOffset = functionOffset + Gen6Constants.boxLegendaryCodeOffsetsXY[i];
                staticCRO[codeOffset] = (byte) speciesLower;
                staticCRO[codeOffset + 1] = 0x0C;
                staticCRO[codeOffset + 2] = 0x40;
                staticCRO[codeOffset + 3] = (byte) 0xE2;
                staticCRO[codeOffset + 4] = (byte) speciesUpper;
                staticCRO[codeOffset + 5] = 0x00;
                staticCRO[codeOffset + 6] = 0x50;
                staticCRO[codeOffset + 7] = (byte) 0xE2;
            }
        }
        writeFile(romEntry.getFile("StaticPokemon"), staticCRO);
    }

    private void setRoamersXY(List<StaticEncounter> staticPokemon) throws IOException {
        int[] roamingLegendaryOffsets = romEntry.getArrayValue("RoamingLegendaryOffsets");
        StaticEncounter[] roamers = new StaticEncounter[roamingLegendaryOffsets.length];
        for (int i = 0; i < roamers.length; i++) {
            roamers[i] = staticPokemon.get(roamingLegendaryOffsets[i]);
        }
        int roamerSpeciesOffset = find(code, Gen6Constants.xyRoamerSpeciesLocator);
        int freeSpaceOffset = find(code, Gen6Constants.xyRoamerFreeSpacePostfix);
        if (roamerSpeciesOffset > 0 && freeSpaceOffset > 0) {
            // In order to make this code work with all versions of XY, we had to find the *end* of our free space.
            // The beginning is five instructions back.
            freeSpaceOffset -= 20;

            // The unmodified code looks like this:
            // nop
            // bl FUN_0041b710
            // nop
            // nop
            // b LAB_003b7d1c
            // We want to move both branches to the top so that we have 12 bytes of space to work with.
            // Start by moving "bl FUN_0041b710" up one instruction, making sure to adjust the branch accordingly.
            code[freeSpaceOffset] = (byte)(code[freeSpaceOffset + 4] + 1);
            code[freeSpaceOffset + 1] = code[freeSpaceOffset + 5];
            code[freeSpaceOffset + 2] = code[freeSpaceOffset + 6];
            code[freeSpaceOffset + 3] = code[freeSpaceOffset + 7];

            // Now move "b LAB_003b7d1c" up three instructions, again adjusting the branch accordingly.
            code[freeSpaceOffset + 4] = (byte)(code[freeSpaceOffset + 16] + 3);
            code[freeSpaceOffset + 5] = code[freeSpaceOffset + 17];
            code[freeSpaceOffset + 6] = code[freeSpaceOffset + 18];
            code[freeSpaceOffset + 7] = code[freeSpaceOffset + 19];

            // In the free space now opened up, write the three roamer species.
            for (int i = 0; i < roamers.length; i++) {
                int offset = freeSpaceOffset + 8 + (i * 4);
                int species = roamers[i].pkmn.getBaseNumber();
                FileFunctions.writeFullInt(code, offset, species);
            }

            // To load the species ID, the game currently does "moveq r4, #0x90" for Articuno and similar
            // things for Zapdos and Moltres. Instead, just pc-relative load what we wrote before. The fact
            // that we change the conditional moveq to the unconditional pc-relative load only matters for
            // the case where the player's starter index is *not* 0, 1, or 2, but that can't happen outside
            // of save editing.
            for (int i = 0; i < roamers.length; i++) {
                int offset = roamerSpeciesOffset + (i * 12);
                code[offset] = (byte)(0xAC - (8 * i));
                code[offset + 1] = 0x41;
                code[offset + 2] = (byte) 0x9F;
                code[offset + 3] = (byte) 0xE5;
            }
        }

        // The level of the roamer is set by a separate function in DllField.
        byte[] fieldCRO = readFile(romEntry.getFile("Field"));
        int levelOffset = find(fieldCRO, Gen6Constants.xyRoamerLevelPrefix);
        if (levelOffset > 0) {
            levelOffset += Gen6Constants.xyRoamerLevelPrefix.length() / 2; // because it was a prefix
            fieldCRO[levelOffset] = (byte) roamers[0].level;
        }
        writeFile(romEntry.getFile("Field"), fieldCRO);

        // We also need to change the Sea Spirit's Den script in order for it to spawn
        // the correct static version of the roamer.
        try {
            GARCArchive encounterGarc = readGARC(romEntry.getFile("WildPokemon"), false);
            byte[] seaSpiritsDenAreaFile = encounterGarc.getFile(Gen6Constants.seaSpiritsDenEncounterFileXY);
            AMX seaSpiritsDenAreaScript = new AMX(seaSpiritsDenAreaFile, 1);
            for (int i = 0; i < roamers.length; i++) {
                int offset = Gen6Constants.seaSpiritsDenScriptOffsetsXY[i];
                int species = roamers[i].pkmn.getBaseNumber();
                FileFunctions.write2ByteInt(seaSpiritsDenAreaScript.decData, offset, species);
            }
            byte[] modifiedScript = seaSpiritsDenAreaScript.getBytes();
            System.arraycopy(modifiedScript, 0, seaSpiritsDenAreaFile, Gen6Constants.seaSpiritsDenLocalScriptOffsetXY, modifiedScript.length);
            encounterGarc.setFile(Gen6Constants.seaSpiritsDenEncounterFileXY, seaSpiritsDenAreaFile);
            writeGARC(romEntry.getFile("WildPokemon"), encounterGarc);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    private void fixRayquazaORAS(int rayquazaEncounterSpecies) throws IOException {
        // We need to edit the script file or otherwise the text will still say "Rayquaza"
        int rayquazaScriptFile = romEntry.getIntValue("RayquazaEncounterScriptNumber");
        GARCArchive scriptGarc = readGARC(romEntry.getFile("Scripts"), true);
        AMX rayquazaAMX = new AMX(scriptGarc.files.get(rayquazaScriptFile).get(0));
        byte[] data = rayquazaAMX.decData;
        for (int i = 0; i < Gen6Constants.rayquazaScriptOffsetsORAS.length; i++) {
            FileFunctions.write2ByteInt(data, Gen6Constants.rayquazaScriptOffsetsORAS[i], rayquazaEncounterSpecies);
        }
        scriptGarc.setFile(rayquazaScriptFile, rayquazaAMX.getBytes());
        writeGARC(romEntry.getFile("Scripts"), scriptGarc);

        // We also need to edit DllField.cro so that the hardcoded checks for Rayquaza's species
        // ID will instead be checks for our randomized species ID.
        byte[] staticCRO = readFile(romEntry.getFile("StaticPokemon"));
        int functionOffset = find(staticCRO, Gen6Constants.rayquazaFunctionPrefixORAS);
        if (functionOffset > 0) {
            functionOffset += Gen6Constants.rayquazaFunctionPrefixORAS.length() / 2; // because it was a prefix

            // Every Rayquaza check consists of "cmp r0, #0x180" followed by a nop. Replace
            // all three checks with a sub and subs instructions so that we can write any
            // random species ID.
            int speciesUpper = rayquazaEncounterSpecies & 0x00FF;
            int speciesLower = (rayquazaEncounterSpecies & 0xFF00) >> 8;
            for (int i = 0; i < Gen6Constants.rayquazaCodeOffsetsORAS.length; i++) {
                int codeOffset = functionOffset + Gen6Constants.rayquazaCodeOffsetsORAS[i];
                staticCRO[codeOffset] = (byte) speciesLower;
                staticCRO[codeOffset + 1] = 0x0C;
                staticCRO[codeOffset + 2] = 0x40;
                staticCRO[codeOffset + 3] = (byte) 0xE2;
                staticCRO[codeOffset + 4] = (byte) speciesUpper;
                staticCRO[codeOffset + 5] = 0x00;
                staticCRO[codeOffset + 6] = 0x50;
                staticCRO[codeOffset + 7] = (byte) 0xE2;
            }
        }
        writeFile(romEntry.getFile("StaticPokemon"), staticCRO);
    }

    @Override
    public int miscTweaksAvailable() {
        int available = 0;
        available |= MiscTweak.FASTEST_TEXT.getValue();
        available |= MiscTweak.BAN_LUCKY_EGG.getValue();
        available |= MiscTweak.RETAIN_ALT_FORMES.getValue();
        available |= MiscTweak.NATIONAL_DEX_AT_START.getValue();
        return available;
    }

    @Override
    public void applyMiscTweak(MiscTweak tweak) {
        if (tweak == MiscTweak.FASTEST_TEXT) {
            applyFastestText();
        } else if (tweak == MiscTweak.BAN_LUCKY_EGG) {
            allowedItems.banSingles(Items.luckyEgg);
            nonBadItems.banSingles(Items.luckyEgg);
        } else if (tweak == MiscTweak.RETAIN_ALT_FORMES) {
            try {
                patchFormeReversion();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (tweak == MiscTweak.NATIONAL_DEX_AT_START) {
            patchForNationalDex();
        }
    }

    private void applyFastestText() {
        int offset = find(code, Gen6Constants.fastestTextPrefixes[0]);
        if (offset > 0) {
            offset += Gen6Constants.fastestTextPrefixes[0].length() / 2; // because it was a prefix
            code[offset] = 0x03;
            code[offset + 1] = 0x40;
            code[offset + 2] = (byte) 0xA0;
            code[offset + 3] = (byte) 0xE3;
        }
        offset = find(code, Gen6Constants.fastestTextPrefixes[1]);
        if (offset > 0) {
            offset += Gen6Constants.fastestTextPrefixes[1].length() / 2; // because it was a prefix
            code[offset] = 0x03;
            code[offset + 1] = 0x50;
            code[offset + 2] = (byte) 0xA0;
            code[offset + 3] = (byte) 0xE3;
        }
    }

    private void patchForNationalDex() {
        int offset = find(code, Gen6Constants.nationalDexFunctionLocator);
        if (offset > 0) {
            // In Savedata::ZukanData::GetZenkokuZukanFlag, we load a flag into r0 and
            // then AND it with 0x1 to get a boolean that determines if the player has
            // the National Dex. The below code patches this piece of code so that
            // instead of loading the flag, we simply "mov r0, #0x1".
            code[offset] = 0x01;
            code[offset + 1] = 0x00;
            code[offset + 2] = (byte) 0xA0;
            code[offset + 3] = (byte) 0xE3;
        }

        if (romEntry.getRomType() == Gen6Constants.Type_XY) {
            offset = find(code, Gen6Constants.xyGetDexFlagFunctionLocator);
            if (offset > 0) {
                // In addition to the code listed above, XY also use a function that I'm
                // calling Savedata::ZukanData::GetDexFlag(int) to determine what Pokedexes
                // the player owns. It can be called with 0 (Central), 1 (Coastal), 2 (Mountain),
                // or 3 (National). Since the player *always* has the Central Dex, the code has
                // a short-circuit for it that looks like this:
                // cmp r5, #0x0
                // moveq r0, #0x1
                // beq returnFromFunction
                // The below code nops out that comparison and makes the move and branch instructions
                // non-conditional; no matter what's on the save file, the player will have all dexes.
                FileFunctions.writeFullInt(code, offset, 0);
                code[offset + 7] = (byte) 0xE3;
                code[offset + 11] = (byte) 0xEA;
            }
        } else {
            // DllSangoZukan.cro will refuse to let you open either the Hoenn or National Pokedex if you have
            // caught 0 Pokemon in the Hoenn Pokedex; it is unknown *how* it does this, though. Instead, let's
            // just hack up the function that determines how many Pokemon in the Hoenn Pokedex you've caught so
            // it returns 1 if you haven't caught anything.
            offset = find(code, Gen6Constants.orasGetHoennDexCaughtFunctionPrefix);
            if (offset > 0) {
                offset += Gen6Constants.orasGetHoennDexCaughtFunctionPrefix.length() / 2; // because it was a prefix

                // At the start of the function, there's a check that the Zukan block on the save data is valid;
                // this is obviously generated by either a macro or inlined function, since literally every function
                // relating to the Pokedex has this too. First, it checks if the checksum is correct then does a beq
                // to branch to the main body of the function; let's replace this with an unconditional branch.
                code[offset + 31] = (byte) 0xEA;

                // Now, in the space where the function would normally handle the call to the assert function
                // to crash the game if the checksum is invalid, we can write the following code:
                // mov r0, r7
                // cmp r0, #0x0
                // moveq r0, #0x1
                // ldmia sp!,{r4 r5 r6 r7 r8 r9 r10 r11 r12 pc}
                FileFunctions.writeFullIntBigEndian(code, offset + 32, 0x0700A0E1);
                FileFunctions.writeFullIntBigEndian(code, offset + 36, 0x000050E3);
                FileFunctions.writeFullIntBigEndian(code, offset + 40, 0x0100A003);
                FileFunctions.writeFullIntBigEndian(code, offset + 44, 0xF09FBDE8);

                // At the end of the function, the game normally does "mov r0, r7" and then returns, where r7
                // contains the number of Pokemon caught in the Hoenn Pokedex. Instead, branch to the code we
                // wrote above.
                FileFunctions.writeFullIntBigEndian(code, offset + 208, 0xD2FFFFEA);
            }
        }
    }

    @Override
    public void enableGuaranteedPokemonCatching() {
        try {
            byte[] battleCRO = readFile(romEntry.getFile("Battle"));
            int offset = find(battleCRO, Gen6Constants.perfectOddsBranchLocator);
            if (offset > 0) {
                // The game checks to see if your odds are greater then or equal to 255 using the following
                // code. Note that they compare to 0xFF000 instead of 0xFF; it looks like all catching code
                // probabilities are shifted like this?
                // cmp r6, #0xFF000
                // blt oddsLessThanOrEqualTo254
                // The below code just nops the branch out so it always acts like our odds are 255, and
                // Pokemon are automatically caught no matter what.
                battleCRO[offset] = 0x00;
                battleCRO[offset + 1] = 0x00;
                battleCRO[offset + 2] = 0x00;
                battleCRO[offset + 3] = 0x00;
                writeFile(romEntry.getFile("Battle"), battleCRO);
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    public List<Integer> getTMMoves() {
        String tmDataPrefix = Gen6Constants.tmDataPrefix;
        int offset = find(code, tmDataPrefix);
        if (offset != 0) {
            offset += Gen6Constants.tmDataPrefix.length() / 2; // because it was a prefix
            List<Integer> tms = new ArrayList<>();
            for (int i = 0; i < Gen6Constants.tmBlockOneCount; i++) {
                tms.add(readWord(code, offset + i * 2));
            }
            offset += (Gen6Constants.getTMBlockTwoStartingOffset(romEntry.getRomType()) * 2);
            for (int i = 0; i < (Gen6Constants.tmCount - Gen6Constants.tmBlockOneCount); i++) {
                tms.add(readWord(code, offset + i * 2));
            }
            return tms;
        } else {
            return null;
        }
    }

    @Override
    public List<Integer> getHMMoves() {
        String tmDataPrefix = Gen6Constants.tmDataPrefix;
        int offset = find(code, tmDataPrefix);
        if (offset != 0) {
            offset += Gen6Constants.tmDataPrefix.length() / 2; // because it was a prefix
            offset += Gen6Constants.tmBlockOneCount * 2; // TM data
            List<Integer> hms = new ArrayList<>();
            for (int i = 0; i < Gen6Constants.hmBlockOneCount; i++) {
                hms.add(readWord(code, offset + i * 2));
            }
            if (romEntry.getRomType() == Gen6Constants.Type_ORAS) {
                hms.add(readWord(code, offset + Gen6Constants.rockSmashOffsetORAS));
                hms.add(readWord(code, offset + Gen6Constants.diveOffsetORAS));
            }
            return hms;
        } else {
            return null;
        }
    }

    @Override
    public void setTMMoves(List<Integer> moveIndexes) {
        String tmDataPrefix = Gen6Constants.tmDataPrefix;
        int offset = find(code, tmDataPrefix);
        if (offset > 0) {
            offset += Gen6Constants.tmDataPrefix.length() / 2; // because it was a prefix
            for (int i = 0; i < Gen6Constants.tmBlockOneCount; i++) {
                writeWord(code, offset + i * 2, moveIndexes.get(i));
            }
            offset += (Gen6Constants.getTMBlockTwoStartingOffset(romEntry.getRomType()) * 2);
            for (int i = 0; i < (Gen6Constants.tmCount - Gen6Constants.tmBlockOneCount); i++) {
                writeWord(code, offset + i * 2, moveIndexes.get(i + Gen6Constants.tmBlockOneCount));
            }

            // Update TM item descriptions
            List<String> itemDescriptions = getStrings(false, romEntry.getIntValue("ItemDescriptionsTextOffset"));
            List<String> moveDescriptions = getStrings(false, romEntry.getIntValue("MoveDescriptionsTextOffset"));
            // TM01 is item 328 and so on
            for (int i = 0; i < Gen6Constants.tmBlockOneCount; i++) {
                itemDescriptions.set(i + Gen6Constants.tmBlockOneOffset, moveDescriptions.get(moveIndexes.get(i)));
            }
            // TM93-95 are 618-620
            for (int i = 0; i < Gen6Constants.tmBlockTwoCount; i++) {
                itemDescriptions.set(i + Gen6Constants.tmBlockTwoOffset,
                        moveDescriptions.get(moveIndexes.get(i + Gen6Constants.tmBlockOneCount)));
            }
            // TM96-100 are 690 and so on
            for (int i = 0; i < Gen6Constants.tmBlockThreeCount; i++) {
                itemDescriptions.set(i + Gen6Constants.tmBlockThreeOffset,
                        moveDescriptions.get(moveIndexes.get(i + Gen6Constants.tmBlockOneCount + Gen6Constants.tmBlockTwoCount)));
            }
            // Save the new item descriptions
            setStrings(false, romEntry.getIntValue("ItemDescriptionsTextOffset"), itemDescriptions);
            // Palettes
            String palettePrefix = Gen6Constants.itemPalettesPrefix;
            int offsPals = find(code, palettePrefix);
            if (offsPals > 0) {
                offsPals += Gen6Constants.itemPalettesPrefix.length() / 2; // because it was a prefix
                // Write pals
                for (int i = 0; i < Gen6Constants.tmBlockOneCount; i++) {
                    int itmNum = Gen6Constants.tmBlockOneOffset + i;
                    Move m = this.moves[moveIndexes.get(i)];
                    int pal = this.typeTMPaletteNumber(m.type, false);
                    writeWord(code, offsPals + itmNum * 4, pal);
                }
                for (int i = 0; i < (Gen6Constants.tmBlockTwoCount); i++) {
                    int itmNum = Gen6Constants.tmBlockTwoOffset + i;
                    Move m = this.moves[moveIndexes.get(i + Gen6Constants.tmBlockOneCount)];
                    int pal = this.typeTMPaletteNumber(m.type, false);
                    writeWord(code, offsPals + itmNum * 4, pal);
                }
                for (int i = 0; i < (Gen6Constants.tmBlockThreeCount); i++) {
                    int itmNum = Gen6Constants.tmBlockThreeOffset + i;
                    Move m = this.moves[moveIndexes.get(i + Gen6Constants.tmBlockOneCount + Gen6Constants.tmBlockTwoCount)];
                    int pal = this.typeTMPaletteNumber(m.type, false);
                    writeWord(code, offsPals + itmNum * 4, pal);
                }
            }
        }
    }

    private int find(byte[] data, String hexString) {
        if (hexString.length() % 2 != 0) {
            return -3; // error
        }
        byte[] searchFor = new byte[hexString.length() / 2];
        for (int i = 0; i < searchFor.length; i++) {
            searchFor[i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
        }
        List<Integer> found = RomFunctions.search(data, searchFor);
        if (found.size() == 0) {
            return -1; // not found
        } else if (found.size() > 1) {
            return -2; // not unique
        } else {
            return found.get(0);
        }
    }

    @Override
    public int getTMCount() {
        return Gen6Constants.tmCount;
    }

    @Override
    public int getHMCount() {
        return Gen6Constants.getHMCount(romEntry.getRomType());
    }

    @Override
    public Map<Pokemon, boolean[]> getTMHMCompatibility() {
        Map<Pokemon, boolean[]> compat = new TreeMap<>();
        int formeCount = Gen6Constants.getFormeCount(romEntry.getRomType());
        for (int i = 1; i <= Gen6Constants.pokemonCount + formeCount; i++) {
            byte[] data;
            data = pokeGarc.files.get(i).get(0);
            Pokemon pkmn = pokes[i];
            boolean[] flags = new boolean[Gen6Constants.tmCount + Gen6Constants.getHMCount(romEntry.getRomType()) + 1];
            for (int j = 0; j < 14; j++) {
                readByteIntoFlags(data, flags, j * 8 + 1, Gen6Constants.bsTMHMCompatOffset + j);
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
            byte[] data = pokeGarc.files.get(pkmn.getNumber()).get(0);
            for (int j = 0; j < 14; j++) {
                data[Gen6Constants.bsTMHMCompatOffset + j] = getByteFromFlags(flags, j * 8 + 1);
            }
        }
    }

    @Override
    public boolean hasMoveTutors() {
        return romEntry.getRomType() == Gen6Constants.Type_ORAS;
    }

    @Override
    public List<Integer> getMoveTutorMoves() {
        List<Integer> mtMoves = new ArrayList<>();

        int mtOffset = getMoveTutorMovesOffset();
        if (mtOffset > 0) {
            int val = 0;
            while (val != 0xFFFF) {
                val = FileFunctions.read2ByteInt(code,mtOffset);
                mtOffset += 2;
                if (val == 0x26E || val == 0xFFFF) continue;
                mtMoves.add(val);
            }
        }

        return mtMoves;
    }

    @Override
    public void setMoveTutorMoves(List<Integer> moves) {

        int mtOffset = find(code, Gen6Constants.tutorsShopPrefix);
        if (mtOffset > 0) {
            mtOffset += Gen6Constants.tutorsShopPrefix.length() / 2; // because it was a prefix
            for (int i = 0; i < Gen6Constants.tutorMoveCount; i++) {
                FileFunctions.write2ByteInt(code,mtOffset + i*8, moves.get(i));
            }
        }

        mtOffset = getMoveTutorMovesOffset();
        if (mtOffset > 0) {
            for (int move: moves) {
                int val = FileFunctions.read2ByteInt(code,mtOffset);
                if (val == 0x26E) mtOffset += 2;
                FileFunctions.write2ByteInt(code,mtOffset,move);
                mtOffset += 2;
            }
        }
    }

    private int getMoveTutorMovesOffset() {
        int offset = moveTutorMovesOffset;
        if (offset == 0) {
            offset = find(code, Gen6Constants.tutorsLocator);
            moveTutorMovesOffset = offset;
        }
        return offset;
    }

    @Override
    public Map<Pokemon, boolean[]> getMoveTutorCompatibility() {
        Map<Pokemon, boolean[]> compat = new TreeMap<>();
        int[] sizes = Gen6Constants.tutorSize;
        int formeCount = Gen6Constants.getFormeCount(romEntry.getRomType());
        for (int i = 1; i <= Gen6Constants.pokemonCount + formeCount; i++) {
            byte[] data;
            data = pokeGarc.files.get(i).get(0);
            Pokemon pkmn = pokes[i];
            boolean[] flags = new boolean[Arrays.stream(sizes).sum() + 1];
            int offset = 0;
            for (int mt = 0; mt < 4; mt++) {
                for (int j = 0; j < 4; j++) {
                    readByteIntoFlags(data, flags, offset + j * 8 + 1, Gen6Constants.bsMTCompatOffset + mt * 4 + j);
                }
                offset += sizes[mt];
            }
            compat.put(pkmn, flags);
        }
        return compat;
    }

    @Override
    public void setMoveTutorCompatibility(Map<Pokemon, boolean[]> compatData) {
        if (!hasMoveTutors()) return;
        int[] sizes = Gen6Constants.tutorSize;
        int formeCount = Gen6Constants.getFormeCount(romEntry.getRomType());
        for (int i = 1; i <= Gen6Constants.pokemonCount + formeCount; i++) {
            byte[] data;
            data = pokeGarc.files.get(i).get(0);
            Pokemon pkmn = pokes[i];
            boolean[] flags = compatData.get(pkmn);
            int offset = 0;
            for (int mt = 0; mt < 4; mt++) {
                boolean[] mtflags = new boolean[sizes[mt] + 1];
                System.arraycopy(flags, offset + 1, mtflags, 1, sizes[mt]);
                for (int j = 0; j < 4; j++) {
                    data[Gen6Constants.bsMTCompatOffset + mt * 4 + j] = getByteFromFlags(mtflags, j * 8 + 1);
                }
                offset += sizes[mt];
            }
        }
    }

    @Override
    public boolean hasTimeBasedEncounters() {
        return false;
    }

    @Override
    public boolean isORAS() {
        return romEntry.getRomType() == Gen6Constants.Type_ORAS;
    }

    @Override
    public List<Integer> getMovesBannedFromLevelup() {
        return Gen6Constants.bannedMoves;
    }

    @Override
    public boolean hasWildAltFormes() {
        return true;
    }

    @Override
    public PokemonSet<Pokemon> getBannedForStaticPokemon() {
        return new PokemonSet<>(Gen6Constants.cosmeticForms
                .stream()
                .filter(index -> index < Gen6Constants.pokemonCount + Gen6Constants.getFormeCount(romEntry.getRomType()))
                .map(index -> pokes[index])
                .collect(Collectors.toList()));
    }

    @Override
    public boolean forceSwapStaticMegaEvos() {
        return romEntry.getRomType() == Gen6Constants.Type_XY;
    }

    @Override
    public boolean hasMainGameLegendaries() {
        return true;
    }

    @Override
    public List<Integer> getMainGameLegendaries() {
        return Arrays.stream(romEntry.getArrayValue("MainGameLegendaries")).boxed().collect(Collectors.toList());
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

    @Override
    public void removeImpossibleEvolutions(Settings settings) {
        boolean changeMoveEvos = !(settings.getMovesetsMod() == Settings.MovesetsMod.UNCHANGED);

        Map<Integer, List<MoveLearnt>> movesets = this.getMovesLearnt();
        Set<Evolution> extraEvolutions = new HashSet<>();
        for (Pokemon pkmn : pokes) {
            if (pkmn != null) {
                extraEvolutions.clear();
                for (Evolution evo : pkmn.getEvolutionsFrom()) {
                    if (changeMoveEvos && evo.getType() == EvolutionType.LEVEL_WITH_MOVE) {
                        // read move
                        int move = evo.getExtraInfo();
                        int levelLearntAt = 1;
                        for (MoveLearnt ml : movesets.get(evo.getFrom().getNumber())) {
                            if (ml.move == move) {
                                levelLearntAt = ml.level;
                                break;
                            }
                        }
                        if (levelLearntAt == 1) {
                            // override for piloswine
                            levelLearntAt = 45;
                        }
                        // change to pure level evo
                        evo.setType(EvolutionType.LEVEL);
                        evo.setExtraInfo(levelLearntAt);
                        addEvoUpdateLevel(impossibleEvolutionUpdates, evo);
                    }
                    // Pure Trade
                    if (evo.getType() == EvolutionType.TRADE) {
                        // Replace w/ level 37
                        evo.setType(EvolutionType.LEVEL);
                        evo.setExtraInfo(37);
                        addEvoUpdateLevel(impossibleEvolutionUpdates, evo);
                    }
                    // Trade w/ Item
                    if (evo.getType() == EvolutionType.TRADE_ITEM) {
                        // Get the current item & evolution
                        int item = evo.getExtraInfo();
                        if (evo.getFrom().getNumber() == Species.slowpoke) {
                            // Slowpoke is awkward - he already has a level evo
                            // So we can't do Level up w/ Held Item for him
                            // Put Water Stone instead
                            evo.setType(EvolutionType.STONE);
                            evo.setExtraInfo(Items.waterStone);
                            addEvoUpdateStone(impossibleEvolutionUpdates, evo, itemNames.get(evo.getExtraInfo()));
                        } else {
                            addEvoUpdateHeldItem(impossibleEvolutionUpdates, evo, itemNames.get(item));
                            // Replace, for this entry, w/
                            // Level up w/ Held Item at Day
                            evo.setType(EvolutionType.LEVEL_ITEM_DAY);
                            // now add an extra evo for
                            // Level up w/ Held Item at Night
                            Evolution extraEntry = new Evolution(evo.getFrom(), evo.getTo(), true,
                                    EvolutionType.LEVEL_ITEM_NIGHT, item);
                            extraEvolutions.add(extraEntry);
                        }
                    }
                    if (evo.getType() == EvolutionType.TRADE_SPECIAL) {
                        // This is the karrablast <-> shelmet trade
                        // Replace it with Level up w/ Other Species in Party
                        // (22)
                        // Based on what species we're currently dealing with
                        evo.setType(EvolutionType.LEVEL_WITH_OTHER);
                        evo.setExtraInfo((evo.getFrom().getNumber() == Species.karrablast ? Species.shelmet : Species.karrablast));
                        addEvoUpdateParty(impossibleEvolutionUpdates, evo, pokes[evo.getExtraInfo()].fullName());
                    }
                    // TBD: Pancham, Sliggoo? Sylveon?
                }

                pkmn.getEvolutionsFrom().addAll(extraEvolutions);
                for (Evolution ev : extraEvolutions) {
                    ev.getTo().getEvolutionsTo().add(ev);
                }
            }
        }

    }

    @Override
    public void makeEvolutionsEasier(Settings settings) {
        boolean wildsRandomized = !settings.getWildPokemonMod().equals(Settings.WildPokemonMod.UNCHANGED);

        // Reduce the amount of happiness required to evolve.
        int offset = find(code, Gen6Constants.friendshipValueForEvoLocator);
        if (offset > 0) {
            // Amount of required happiness for HAPPINESS evolutions.
            if (code[offset] == (byte) GlobalConstants.vanillaHappinessToEvolve) {
                code[offset] = (byte) GlobalConstants.easierHappinessToEvolve;
            }
            // Amount of required happiness for HAPPINESS_DAY evolutions.
            if (code[offset + 12] == (byte) GlobalConstants.vanillaHappinessToEvolve) {
                code[offset + 12] = (byte) GlobalConstants.easierHappinessToEvolve;
            }
            // Amount of required happiness for HAPPINESS_NIGHT evolutions.
            if (code[offset + 36] == (byte) GlobalConstants.vanillaHappinessToEvolve) {
                code[offset + 36] = (byte) GlobalConstants.easierHappinessToEvolve;
            }
        }

        if (wildsRandomized) {
            for (Pokemon pkmn : pokes) {
                if (pkmn != null) {
                    for (Evolution evo : pkmn.getEvolutionsFrom()) {
                        if (evo.getType() == EvolutionType.LEVEL_WITH_OTHER) {
                            // Replace w/ level 35
                            evo.setType(EvolutionType.LEVEL);
                            evo.setExtraInfo(35);
                            addEvoUpdateCondensed(easierEvolutionUpdates, evo, false);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void removeTimeBasedEvolutions() {
        Set<Evolution> extraEvolutions = new HashSet<>();
        for (Pokemon pkmn : pokes) {
            if (pkmn != null) {
                extraEvolutions.clear();
                for (Evolution evo : pkmn.getEvolutionsFrom()) {
                    if (evo.getType() == EvolutionType.HAPPINESS_DAY) {
                        if (evo.getFrom().getNumber() == Species.eevee) {
                            // We can't set Eevee to evolve into Espeon with happiness at night because that's how
                            // Umbreon works in the original game. Instead, make Eevee: == sun stone => Espeon
                            evo.setType(EvolutionType.STONE);
                            evo.setExtraInfo(Items.sunStone);
                            addEvoUpdateStone(timeBasedEvolutionUpdates, evo, itemNames.get(evo.getExtraInfo()));
                        } else {
                            // Add an extra evo for Happiness at Night
                            addEvoUpdateHappiness(timeBasedEvolutionUpdates, evo);
                            Evolution extraEntry = new Evolution(evo.getFrom(), evo.getTo(), true,
                                    EvolutionType.HAPPINESS_NIGHT, 0);
                            extraEvolutions.add(extraEntry);
                        }
                    } else if (evo.getType() == EvolutionType.HAPPINESS_NIGHT) {
                        if (evo.getFrom().getNumber() == Species.eevee) {
                            // We can't set Eevee to evolve into Umbreon with happiness at day because that's how
                            // Espeon works in the original game. Instead, make Eevee: == moon stone => Umbreon
                            evo.setType(EvolutionType.STONE);
                            evo.setExtraInfo(Items.moonStone);
                            addEvoUpdateStone(timeBasedEvolutionUpdates, evo, itemNames.get(evo.getExtraInfo()));
                        } else {
                            // Add an extra evo for Happiness at Day
                            addEvoUpdateHappiness(timeBasedEvolutionUpdates, evo);
                            Evolution extraEntry = new Evolution(evo.getFrom(), evo.getTo(), true,
                                    EvolutionType.HAPPINESS_DAY, 0);
                            extraEvolutions.add(extraEntry);
                        }
                    } else if (evo.getType() == EvolutionType.LEVEL_ITEM_DAY) {
                        int item = evo.getExtraInfo();
                        // Make sure we don't already have an evo for the same item at night (e.g., when using Change Impossible Evos)
                        if (evo.getFrom().getEvolutionsFrom().stream().noneMatch(e -> e.getType() == EvolutionType.LEVEL_ITEM_NIGHT && e.getExtraInfo() == item)) {
                            // Add an extra evo for Level w/ Item During Night
                            addEvoUpdateHeldItem(timeBasedEvolutionUpdates, evo, itemNames.get(item));
                            Evolution extraEntry = new Evolution(evo.getFrom(), evo.getTo(), true,
                                    EvolutionType.LEVEL_ITEM_NIGHT, item);
                            extraEvolutions.add(extraEntry);
                        }
                    } else if (evo.getType() == EvolutionType.LEVEL_ITEM_NIGHT) {
                        int item = evo.getExtraInfo();
                        // Make sure we don't already have an evo for the same item at day (e.g., when using Change Impossible Evos)
                        if (evo.getFrom().getEvolutionsFrom().stream().noneMatch(e -> e.getType() == EvolutionType.LEVEL_ITEM_DAY && e.getExtraInfo() == item)) {
                            // Add an extra evo for Level w/ Item During Day
                            addEvoUpdateHeldItem(timeBasedEvolutionUpdates, evo, itemNames.get(item));
                            Evolution extraEntry = new Evolution(evo.getFrom(), evo.getTo(), true,
                                    EvolutionType.LEVEL_ITEM_DAY, item);
                            extraEvolutions.add(extraEntry);
                        }
                    } else if (evo.getType() == EvolutionType.LEVEL_DAY || evo.getType() == EvolutionType.LEVEL_NIGHT) {
                        addEvoUpdateLevel(timeBasedEvolutionUpdates, evo);
                        evo.setType(EvolutionType.LEVEL);
                    }
                }
                pkmn.getEvolutionsFrom().addAll(extraEvolutions);
                for (Evolution ev : extraEvolutions) {
                    ev.getTo().getEvolutionsTo().add(ev);
                }
            }
        }

    }

    @Override
    public boolean hasShopSupport() {
        return true;
    }

    @Override
    public boolean canChangeTrainerText() {
        return true;
    }

    @Override
    public List<String> getTrainerNames() {
        List<String> tnames = getStrings(false, romEntry.getIntValue("TrainerNamesTextOffset"));
        tnames.remove(0); // blank one

        return tnames;
    }

    @Override
    public int maxTrainerNameLength() {
        return 10;
    }

    @Override
    public void setTrainerNames(List<String> trainerNames) {
        List<String> tnames = getStrings(false, romEntry.getIntValue("TrainerNamesTextOffset"));
        List<String> newTNames = new ArrayList<>(trainerNames);
        newTNames.add(0, tnames.get(0)); // the 0-entry, preserve it
        setStrings(false, romEntry.getIntValue("TrainerNamesTextOffset"), newTNames);
        try {
            writeStringsForAllLanguages(newTNames, romEntry.getIntValue("TrainerNamesTextOffset"));
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    private void writeStringsForAllLanguages(List<String> strings, int index) throws IOException {
        List<String> nonEnglishLanguages = Arrays.asList("JaKana", "JaKanji", "Fr", "It", "De", "Es", "Ko");
        for (String nonEnglishLanguage : nonEnglishLanguages) {
            String key = "TextStrings" + nonEnglishLanguage;
            GARCArchive stringsGarcForLanguage = readGARC(romEntry.getFile(key),true);
            setStrings(stringsGarcForLanguage, index, strings);
            writeGARC(romEntry.getFile(key), stringsGarcForLanguage);
        }
    }

    @Override
    public TrainerNameMode trainerNameMode() {
        return TrainerNameMode.MAX_LENGTH;
    }

    @Override
    public List<Integer> getTCNameLengthsByTrainer() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getTrainerClassNames() {
        return getStrings(false, romEntry.getIntValue("TrainerClassesTextOffset"));
    }

    @Override
    public void setTrainerClassNames(List<String> trainerClassNames) {
        setStrings(false, romEntry.getIntValue("TrainerClassesTextOffset"), trainerClassNames);
        try {
            writeStringsForAllLanguages(trainerClassNames, romEntry.getIntValue("TrainerClassesTextOffset"));
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    public int maxTrainerClassNameLength() {
        return 15; // "Pokémon Breeder" is possible, so,
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
    public String getDefaultExtension() {
        return "cxi";
    }

    @Override
    public int abilitiesPerPokemon() {
        return 3;
    }

    @Override
    public int highestAbilityIndex() {
        return Gen6Constants.getHighestAbilityIndex(romEntry.getRomType());
    }

    @Override
    public int internalStringLength(String string) {
        return string.length();
    }

    @Override
    public boolean setIntroPokemon(Pokemon pk) {
        // Doesn't do anything for ORAS
        if (romEntry.getRomType() == Gen6Constants.Type_XY) {

            int introPokemonNum = pk.getNumber();
            int introPokemonForme = 0;
            if (pk.isAltForme()) {
                introPokemonForme = pk.getFormeNumber();
                introPokemonNum = pk.getBaseForme().getNumber();
            }

            // Find the value for the Pokemon's cry

            int baseAddr = find(code, Gen6Constants.criesTablePrefixXY);
            baseAddr += Gen6Constants.criesTablePrefixXY.length() / 2;

            if (introPokemonForme != 0) {
                int extraOffset = readLong(code, baseAddr + (introPokemonNum * 0x14));
                introPokemonNum = extraOffset + (introPokemonForme - 1);
            }

            int initialCry = readLong(code, baseAddr + (introPokemonNum * 0x14) + 0x4);
            int repeatedCry = readLong(code, baseAddr + (introPokemonNum * 0x14) + 0x10);

            // Write to DLLIntro.cro
            try {
                byte[] introCRO = readFile(romEntry.getFile("Intro"));

                // Replace the Pokemon model that's loaded, and set its forme

                int croModelOffset = find(introCRO, Gen6Constants.introPokemonModelOffsetXY);
                croModelOffset += Gen6Constants.introPokemonModelOffsetXY.length() / 2;

                writeWord(introCRO, croModelOffset, introPokemonNum);
                introCRO[croModelOffset + 2] = (byte) introPokemonForme;

                // Replace the initial cry when the Pokemon exits the ball
                // First, re-point two branches

                int croInitialCryOffset1 = find(introCRO, Gen6Constants.introInitialCryOffset1XY);
                croInitialCryOffset1 += Gen6Constants.introInitialCryOffset1XY.length() / 2;

                introCRO[croInitialCryOffset1] = 0x5E;

                int croInitialCryOffset2 = find(introCRO, Gen6Constants.introInitialCryOffset2XY);
                croInitialCryOffset2 += Gen6Constants.introInitialCryOffset2XY.length() / 2;

                introCRO[croInitialCryOffset2] = 0x2F;

                // Then change the parameters that are loaded for a function call, and also change the function call
                // itself to a function that uses the "cry value" instead of Pokemon ID + forme + emotion (same function
                // that is used for the repeated cries)

                int croInitialCryOffset3 = find(introCRO, Gen6Constants.introInitialCryOffset3XY);
                croInitialCryOffset3 += Gen6Constants.introInitialCryOffset3XY.length() / 2;

                writeLong(introCRO, croInitialCryOffset3, 0xE1A02000);  // cpy r2,r0
                writeLong(introCRO, croInitialCryOffset3 + 0x4, 0xE59F100C);    // ldr r1,=#CRY_VALUE
                writeLong(introCRO, croInitialCryOffset3 + 0x8, 0xE58D0000);    // str r0,[sp]
                writeLong(introCRO, croInitialCryOffset3 + 0xC, 0xEBFFFDE9);    // bl FUN_006a51d4
                writeLong(introCRO, croInitialCryOffset3 + 0x10, readLong(introCRO, croInitialCryOffset3 + 0x14)); // Move these two instructions up four bytes
                writeLong(introCRO, croInitialCryOffset3 + 0x14, readLong(introCRO, croInitialCryOffset3 + 0x18));
                writeLong(introCRO, croInitialCryOffset3 + 0x18, initialCry);   // CRY_VALUE pool

                // Replace the repeated cry that the Pokemon does while standing around
                // Just replace a pool value
                int croRepeatedCryOffset = find(introCRO, Gen6Constants.introRepeatedCryOffsetXY);
                croRepeatedCryOffset += Gen6Constants.introRepeatedCryOffsetXY.length() / 2;
                writeLong(introCRO, croRepeatedCryOffset, repeatedCry);

                writeFile(romEntry.getFile("Intro"), introCRO);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
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
        return Gen6Constants.uniqueNoSellItems;
    }

    @Override
    public List<Integer> getRegularShopItems() {
        return Gen6Constants.regularShopItems;
    }

    @Override
    public List<Integer> getOPShopItems() {
        return Gen6Constants.opShopItems;
    }

    @Override
    public String[] getItemNames() {
        return itemNames.toArray(new String[0]);
    }

    @Override
    public String abilityName(int number) {
        return abilityNames.get(number);
    }

    @Override
    public Map<Integer, List<Integer>> getAbilityVariations() {
        return Gen5Constants.abilityVariations;
    }

    @Override
    public List<Integer> getUselessAbilities() {
        return new ArrayList<>(Gen6Constants.uselessAbilities);
    }

    @Override
    public int getAbilityForTrainerPokemon(TrainerPokemon tp) {
        // Before randomizing Trainer Pokemon, one possible value for abilitySlot is 0,
        // which represents "Either Ability 1 or 2". During randomization, we make sure to
        // to set abilitySlot to some non-zero value, but if you call this method without
        // randomization, then you'll hit this case.
        if (tp.abilitySlot < 1 || tp.abilitySlot > 3) {
            return 0;
        }

        List<Integer> abilityList = Arrays.asList(tp.pokemon.getAbility1(), tp.pokemon.getAbility2(), tp.pokemon.getAbility3());
        return abilityList.get(tp.abilitySlot - 1);
    }

    @Override
    public boolean hasMegaEvolutions() {
        return true;
    }

    private int tmFromIndex(int index) {

        if (index >= Gen6Constants.tmBlockOneOffset
                && index < Gen6Constants.tmBlockOneOffset + Gen6Constants.tmBlockOneCount) {
            return index - (Gen6Constants.tmBlockOneOffset - 1);
        } else if (index >= Gen6Constants.tmBlockTwoOffset
                && index < Gen6Constants.tmBlockTwoOffset + Gen6Constants.tmBlockTwoCount) {
            return (index + Gen6Constants.tmBlockOneCount) - (Gen6Constants.tmBlockTwoOffset - 1);
        } else {
            return (index + Gen6Constants.tmBlockOneCount + Gen6Constants.tmBlockTwoCount) - (Gen6Constants.tmBlockThreeOffset - 1);
        }
    }

    private int indexFromTM(int tm) {
        if (tm >= 1 && tm <= Gen6Constants.tmBlockOneCount) {
            return tm + (Gen6Constants.tmBlockOneOffset - 1);
        } else if (tm > Gen6Constants.tmBlockOneCount && tm <= Gen6Constants.tmBlockOneCount + Gen6Constants.tmBlockTwoCount) {
            return tm + (Gen6Constants.tmBlockTwoOffset - 1 - Gen6Constants.tmBlockOneCount);
        } else {
            return tm + (Gen6Constants.tmBlockThreeOffset - 1 - (Gen6Constants.tmBlockOneCount + Gen6Constants.tmBlockTwoCount));
        }
    }

    @Override
    public List<Integer> getCurrentFieldTMs() {
        List<Integer> fieldItems = this.getFieldItems();
        List<Integer> fieldTMs = new ArrayList<>();

        ItemList allowedItems = Gen6Constants.getAllowedItems(romEntry.getRomType());
        for (int item : fieldItems) {
            if (allowedItems.isTM(item)) {
                fieldTMs.add(tmFromIndex(item));
            }
        }

        return fieldTMs;
    }

    @Override
    public void setFieldTMs(List<Integer> fieldTMs) {
        List<Integer> fieldItems = this.getFieldItems();
        int fiLength = fieldItems.size();
        Iterator<Integer> iterTMs = fieldTMs.iterator();

        ItemList allowedItems = Gen6Constants.getAllowedItems(romEntry.getRomType());
        for (int i = 0; i < fiLength; i++) {
            int oldItem = fieldItems.get(i);
            if (allowedItems.isTM(oldItem)) {
                int newItem = indexFromTM(iterTMs.next());
                fieldItems.set(i, newItem);
            }
        }

        this.setFieldItems(fieldItems);
    }

    @Override
    public List<Integer> getRegularFieldItems() {
        List<Integer> fieldItems = this.getFieldItems();
        List<Integer> fieldRegItems = new ArrayList<>();

        ItemList allowedItems = Gen6Constants.getAllowedItems(romEntry.getRomType());
        for (int item : fieldItems) {
            if (allowedItems.isAllowed(item) && !(allowedItems.isTM(item))) {
                fieldRegItems.add(item);
            }
        }

        return fieldRegItems;
    }

    @Override
    public void setRegularFieldItems(List<Integer> items) {
        List<Integer> fieldItems = this.getFieldItems();
        int fiLength = fieldItems.size();
        Iterator<Integer> iterNewItems = items.iterator();

        ItemList allowedItems = Gen6Constants.getAllowedItems(romEntry.getRomType());
        for (int i = 0; i < fiLength; i++) {
            int oldItem = fieldItems.get(i);
            if (!(allowedItems.isTM(oldItem)) && allowedItems.isAllowed(oldItem) && oldItem != Items.masterBall) {
                int newItem = iterNewItems.next();
                fieldItems.set(i, newItem);
            }
        }

        this.setFieldItems(fieldItems);
    }

    @Override
    public List<Integer> getRequiredFieldTMs() {
        return Gen6Constants.getRequiredFieldTMs(romEntry.getRomType());
    }

    public List<Integer> getFieldItems() {
        List<Integer> fieldItems = new ArrayList<>();
        try {
            // normal items
            int normalItemsFile = romEntry.getIntValue("FieldItemsScriptNumber");
            int normalItemsOffset = romEntry.getIntValue("FieldItemsOffset");
            GARCArchive scriptGarc = readGARC(romEntry.getFile("Scripts"),true);
            AMX normalItemAMX = new AMX(scriptGarc.files.get(normalItemsFile).get(0));
            byte[] data = normalItemAMX.decData;
            for (int i = normalItemsOffset; i < data.length; i += 12) {
                int item = FileFunctions.read2ByteInt(data,i);
                fieldItems.add(item);
            }

            // hidden items - separate handling for XY and ORAS
            if (romEntry.getRomType() == Gen6Constants.Type_XY) {
                int hiddenItemsFile = romEntry.getIntValue("HiddenItemsScriptNumber");
                int hiddenItemsOffset = romEntry.getIntValue("HiddenItemsOffset");
                AMX hiddenItemAMX = new AMX(scriptGarc.files.get(hiddenItemsFile).get(0));
                data = hiddenItemAMX.decData;
                for (int i = hiddenItemsOffset; i < data.length; i += 12) {
                    int item = FileFunctions.read2ByteInt(data,i);
                    fieldItems.add(item);
                }
            } else {
                String hiddenItemsPrefix = Gen6Constants.hiddenItemsPrefixORAS;
                int offsHidden = find(code,hiddenItemsPrefix);
                if (offsHidden > 0) {
                    offsHidden += hiddenItemsPrefix.length() / 2;
                    for (int i = 0; i < Gen6Constants.hiddenItemCountORAS; i++) {
                        int item = FileFunctions.read2ByteInt(code, offsHidden + (i * 0xE) + 2);
                        fieldItems.add(item);
                    }
                }
            }

            // In ORAS, it's possible to encounter the sparkling Mega Stone items on the field
            // before you finish the game. Thus, we want to randomize them as well.
            if (romEntry.getRomType() == Gen6Constants.Type_ORAS) {
                List<Integer> fieldMegaStones = this.getFieldMegaStonesORAS(scriptGarc);
                fieldItems.addAll(fieldMegaStones);
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }

        return fieldItems;
    }

    private List<Integer> getFieldMegaStonesORAS(GARCArchive scriptGarc) throws IOException {
        List<Integer> fieldMegaStones = new ArrayList<>();
        int megaStoneItemScriptFile = romEntry.getIntValue("MegaStoneItemScriptNumber");
        byte[] megaStoneItemEventBytes = scriptGarc.getFile(megaStoneItemScriptFile);
        AMX megaStoneItemEvent = new AMX(megaStoneItemEventBytes);
        for (int i = 0; i < Gen6Constants.megastoneTableLengthORAS; i++) {
            int offset = Gen6Constants.megastoneTableStartingOffsetORAS + (i * Gen6Constants.megastoneTableEntrySizeORAS);
            int item = FileFunctions.read2ByteInt(megaStoneItemEvent.decData, offset);
            fieldMegaStones.add(item);
        }
        return fieldMegaStones;
    }

    public void setFieldItems(List<Integer> items) {
        try {
            Iterator<Integer> iterItems = items.iterator();
            // normal items
            int normalItemsFile = romEntry.getIntValue("FieldItemsScriptNumber");
            int normalItemsOffset = romEntry.getIntValue("FieldItemsOffset");
            GARCArchive scriptGarc = readGARC(romEntry.getFile("Scripts"),true);
            AMX normalItemAMX = new AMX(scriptGarc.files.get(normalItemsFile).get(0));
            byte[] data = normalItemAMX.decData;
            for (int i = normalItemsOffset; i < data.length; i += 12) {
                int item = iterItems.next();
                FileFunctions.write2ByteInt(data,i,item);
            }
            scriptGarc.setFile(normalItemsFile,normalItemAMX.getBytes());

            // hidden items - separate handling for XY and ORAS
            if (romEntry.getRomType() == Gen6Constants.Type_XY) {
                int hiddenItemsFile = romEntry.getIntValue("HiddenItemsScriptNumber");
                int hiddenItemsOffset = romEntry.getIntValue("HiddenItemsOffset");
                AMX hiddenItemAMX = new AMX(scriptGarc.files.get(hiddenItemsFile).get(0));
                data = hiddenItemAMX.decData;
                for (int i = hiddenItemsOffset; i < data.length; i += 12) {
                    int item = iterItems.next();
                    FileFunctions.write2ByteInt(data,i,item);
                }
                scriptGarc.setFile(hiddenItemsFile,hiddenItemAMX.getBytes());
            } else {
                String hiddenItemsPrefix = Gen6Constants.hiddenItemsPrefixORAS;
                int offsHidden = find(code,hiddenItemsPrefix);
                if (offsHidden > 0) {
                    offsHidden += hiddenItemsPrefix.length() / 2;
                    for (int i = 0; i < Gen6Constants.hiddenItemCountORAS; i++) {
                        int item = iterItems.next();
                        FileFunctions.write2ByteInt(code,offsHidden + (i * 0xE) + 2, item);
                    }
                }
            }

            // Sparkling Mega Stone items for ORAS only
            if (romEntry.getRomType() == Gen6Constants.Type_ORAS) {
                List<Integer> fieldMegaStones = this.getFieldMegaStonesORAS(scriptGarc);
                Map<Integer, Integer> megaStoneMap = new HashMap<>();
                int megaStoneItemScriptFile = romEntry.getIntValue("MegaStoneItemScriptNumber");
                byte[] megaStoneItemEventBytes = scriptGarc.getFile(megaStoneItemScriptFile);
                AMX megaStoneItemEvent = new AMX(megaStoneItemEventBytes);
                for (int i = 0; i < Gen6Constants.megastoneTableLengthORAS; i++) {
                    int offset = Gen6Constants.megastoneTableStartingOffsetORAS + (i * Gen6Constants.megastoneTableEntrySizeORAS);
                    int oldItem = fieldMegaStones.get(i);
                    int newItem = iterItems.next();
                    if (megaStoneMap.containsKey(oldItem)) {
                        // There are some duplicate entries for certain Mega Stones, and we're not quite sure why.
                        // Set them to the same item for sanity's sake.
                        int replacementItem = megaStoneMap.get(oldItem);
                        FileFunctions.write2ByteInt(megaStoneItemEvent.decData, offset, replacementItem);
                    } else {
                        FileFunctions.write2ByteInt(megaStoneItemEvent.decData, offset, newItem);
                        megaStoneMap.put(oldItem, newItem);
                    }
                }
                scriptGarc.setFile(megaStoneItemScriptFile, megaStoneItemEvent.getBytes());
            }

            writeGARC(romEntry.getFile("Scripts"),scriptGarc);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    public List<IngameTrade> getIngameTrades() {
        List<IngameTrade> trades = new ArrayList<>();

        int count = romEntry.getIntValue("IngameTradeCount");
        String prefix = Gen6Constants.getIngameTradesPrefix(romEntry.getRomType());
        List<String> tradeStrings = getStrings(false, romEntry.getIntValue("IngameTradesTextOffset"));
        int textOffset = romEntry.getIntValue("IngameTradesTextExtraOffset");
        int offset = find(code,prefix);
        if (offset > 0) {
            offset += prefix.length() / 2;
            for (int i = 0; i < count; i++) {
                IngameTrade trade = new IngameTrade();
                trade.nickname = tradeStrings.get(textOffset + i);
                trade.givenPokemon = pokes[FileFunctions.read2ByteInt(code,offset)];
                trade.ivs = new int[6];
                for (int iv = 0; iv < 6; iv++) {
                    trade.ivs[iv] = code[offset + 5 + iv];
                }
                trade.otId = FileFunctions.read2ByteInt(code,offset + 0xE);
                trade.item = FileFunctions.read2ByteInt(code,offset + 0x10);
                trade.otName = tradeStrings.get(textOffset + count + i);
                trade.requestedPokemon = pokes[FileFunctions.read2ByteInt(code,offset + 0x20)];
                trades.add(trade);
                offset += Gen6Constants.ingameTradeSize;
            }
        }
        return trades;
    }

    @Override
    public void setIngameTrades(List<IngameTrade> trades) {
        List<IngameTrade> oldTrades = this.getIngameTrades();
        int count = romEntry.getIntValue("IngameTradeCount");
        String prefix = Gen6Constants.getIngameTradesPrefix(romEntry.getRomType());
        List<String> tradeStrings = getStrings(false, romEntry.getIntValue("IngameTradesTextOffset"));
        int textOffset = romEntry.getIntValue("IngameTradesTextExtraOffset");
        int offset = find(code,prefix);
        if (offset > 0) {
            offset += prefix.length() / 2;
            for (int i = 0; i < count; i++) {
                IngameTrade trade = trades.get(i);
                tradeStrings.set(textOffset + i, trade.nickname);
                FileFunctions.write2ByteInt(code,offset, trade.givenPokemon.getNumber());
                for (int iv = 0; iv < 6; iv++) {
                    code[offset + 5 + iv] = (byte)trade.ivs[iv];
                }
                FileFunctions.write2ByteInt(code,offset + 0xE,trade.otId);
                FileFunctions.write2ByteInt(code,offset + 0x10,trade.item);
                tradeStrings.set(textOffset + count + i, trade.otName);
                FileFunctions.write2ByteInt(code,offset + 0x20,
                        trade.requestedPokemon == null ? 0 : trade.requestedPokemon.getNumber());
                offset += Gen6Constants.ingameTradeSize;

                // In XY, there are some trades that use hardcoded strings. Go and forcibly update
                // the story text so that the trainer says what they want to trade.
                if (romEntry.getRomType() == Gen6Constants.Type_XY && Gen6Constants.xyHardcodedTradeOffsets.contains(i)) {
                    int hardcodedTradeIndex = Gen6Constants.xyHardcodedTradeOffsets.indexOf(i);
                    updateHardcodedTradeText(oldTrades.get(i), trade, Gen6Constants.xyHardcodedTradeTexts.get(hardcodedTradeIndex));
                }
            }
            this.setStrings(false, romEntry.getIntValue("IngameTradesTextOffset"), tradeStrings);
        }
    }

    // NOTE: This method is kind of stupid, in that it doesn't try to reflow the text to better fit; it just
    // blindly replaces the Pokemon's name. However, it seems to work well enough for what we need.
    private void updateHardcodedTradeText(IngameTrade oldTrade, IngameTrade newTrade, int hardcodedTradeTextFile) {
        List<String> hardcodedTradeStrings = getStrings(true, hardcodedTradeTextFile);
        Pokemon oldRequested = oldTrade.requestedPokemon;
        String oldRequestedName = oldRequested != null ? oldRequested.getName() : null;
        String oldGivenName = oldTrade.givenPokemon.getName();
        Pokemon newRequested = newTrade.requestedPokemon;
        String newRequestedName = newRequested != null ? newRequested.getName() : null;
        String newGivenName = newTrade.givenPokemon.getName();
        for (int i = 0; i < hardcodedTradeStrings.size(); i++) {
            String hardcodedTradeString = hardcodedTradeStrings.get(i);
            if (oldRequestedName != null && newRequestedName != null && hardcodedTradeString.contains(oldRequestedName)) {
                hardcodedTradeString = hardcodedTradeString.replace(oldRequestedName, newRequestedName);
            }
            if (hardcodedTradeString.contains(oldGivenName)) {
                hardcodedTradeString = hardcodedTradeString.replace(oldGivenName, newGivenName);
            }
            hardcodedTradeStrings.set(i, hardcodedTradeString);
        }
        this.setStrings(true, hardcodedTradeTextFile, hardcodedTradeStrings);
    }

    @Override
    public boolean hasDVs() {
        return false;
    }

    @Override
    public int generationOfPokemon() {
        return 6;
    }

    @Override
    public void removeEvosForPokemonPool() {
        // slightly more complicated than gen2/3
        // we have to update a "baby table" too
        PokemonSet<Pokemon> pokemonIncluded = rPokeService.getAll(true);
        Set<Evolution> keepEvos = new HashSet<>();
        for (Pokemon pk : pokes) {
            if (pk != null) {
                keepEvos.clear();
                for (Evolution evol : pk.getEvolutionsFrom()) {
                    if (pokemonIncluded.contains(evol.getFrom()) && pokemonIncluded.contains(evol.getTo())) {
                        keepEvos.add(evol);
                    } else {
                        evol.getTo().getEvolutionsTo().remove(evol);
                    }
                }
                pk.getEvolutionsFrom().retainAll(keepEvos);
            }
        }

        try {
            // baby pokemon
            GARCArchive babyGarc = readGARC(romEntry.getFile("BabyPokemon"), true);
            byte[] masterFile = babyGarc.getFile(Gen6Constants.pokemonCount + 1);
            for (int i = 1; i <= Gen6Constants.pokemonCount; i++) {
                byte[] babyFile = babyGarc.getFile(i);
                Pokemon baby = pokes[i];
                while (baby.getEvolutionsTo().size() > 0) {
                    // Grab the first "to evolution" even if there are multiple
                    baby = baby.getEvolutionsTo().get(0).getFrom();
                }
                writeWord(babyFile, 0, baby.getNumber());
                writeWord(masterFile, i * 2, baby.getNumber());
                babyGarc.setFile(i, babyFile);
            }
            babyGarc.setFile(Gen6Constants.pokemonCount + 1, masterFile);
            writeGARC(romEntry.getFile("BabyPokemon"), babyGarc);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    public boolean supportsFourStartingMoves() {
        return true;
    }

    @Override
    public List<Integer> getFieldMoves() {
        if (romEntry.getRomType() == Gen6Constants.Type_XY) {
            return Gen6Constants.fieldMovesXY;
        } else {
            return Gen6Constants.fieldMovesORAS;
        }
    }

    @Override
    public List<Integer> getEarlyRequiredHMMoves() {
        return new ArrayList<>();
    }

    @Override
    public Map<Integer, Shop> getShopItems() {
        int[] tmShops = romEntry.getArrayValue("TMShops");
        int[] regularShops = romEntry.getArrayValue("RegularShops");
        int[] shopItemSizes = romEntry.getArrayValue("ShopItemSizes");
        int shopCount = romEntry.getIntValue("ShopCount");
        Map<Integer, Shop> shopItemsMap = new TreeMap<>();

        int offset = getShopItemsOffset();
        if (offset <= 0) {
            return shopItemsMap;
        }
        for (int i = 0; i < shopCount; i++) {
            boolean badShop = false;
            for (int tmShop: tmShops) {
                if (i == tmShop) {
                    badShop = true;
                    offset += (shopItemSizes[i] * 2);
                    break;
                }
            }
            for (int regularShop: regularShops) {
                if (badShop) break;
                if (i == regularShop) {
                    badShop = true;
                    offset += (shopItemSizes[i] * 2);
                    break;
                }
            }
            if (!badShop) {
                List<Integer> items = new ArrayList<>();
                for (int j = 0; j < shopItemSizes[i]; j++) {
                    items.add(FileFunctions.read2ByteInt(code,offset));
                    offset += 2;
                }
                Shop shop = new Shop();
                shop.items = items;
                shop.name = shopNames.get(i);
                shop.isMainGame = Gen6Constants.getMainGameShops(romEntry.getRomType()).contains(i);
                shopItemsMap.put(i, shop);
            }
        }
        return shopItemsMap;
    }

    @Override
    public void setShopItems(Map<Integer, Shop> shopItems) {
        int[] shopItemSizes = romEntry.getArrayValue("ShopItemSizes");
        int[] tmShops = romEntry.getArrayValue("TMShops");
        int[] regularShops = romEntry.getArrayValue("RegularShops");
        int shopCount = romEntry.getIntValue("ShopCount");

        int offset = getShopItemsOffset();
        if (offset <= 0) {
            return;
        }
        for (int i = 0; i < shopCount; i++) {
            boolean badShop = false;
            for (int tmShop: tmShops) {
                if (badShop) break;
                if (i == tmShop) {
                    badShop = true;
                    offset += (shopItemSizes[i] * 2);
                    break;
                }
            }
            for (int regularShop: regularShops) {
                if (badShop) break;
                if (i == regularShop) {
                    badShop = true;
                    offset += (shopItemSizes[i] * 2);
                    break;
                }
            }
            if (!badShop) {
                List<Integer> shopContents = shopItems.get(i).items;
                Iterator<Integer> iterItems = shopContents.iterator();
                for (int j = 0; j < shopItemSizes[i]; j++) {
                    Integer item = iterItems.next();
                    FileFunctions.write2ByteInt(code,offset,item);
                    offset += 2;
                }
            }
        }
    }

    private int getShopItemsOffset() {
        int offset = shopItemsOffset;
        if (offset == 0) {
            String locator = Gen6Constants.getShopItemsLocator(romEntry.getRomType());
            offset = find(code, locator);
            shopItemsOffset = offset;
        }
        return offset;
    }

    @Override
    public void setBalancedShopPrices() {
        try {
            GARCArchive itemPriceGarc = this.readGARC(romEntry.getFile("ItemData"),true);
            for (int i = 1; i < itemPriceGarc.files.size(); i++) {
                writeWord(itemPriceGarc.files.get(i).get(0),0,Gen6Constants.balancedItemPrices.get(i));
            }
            writeGARC(romEntry.getFile("ItemData"),itemPriceGarc);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    public List<PickupItem> getPickupItems() {
        List<PickupItem> pickupItems = new ArrayList<>();

        // If we haven't found the pickup table for this ROM already, find it.
        if (pickupItemsTableOffset == 0) {
            int offset = find(code, Gen6Constants.pickupTableLocator);
            if (offset > 0) {
                pickupItemsTableOffset = offset;
            }
        }

        // Assuming we've found the pickup table, extract the items out of it.
        if (pickupItemsTableOffset > 0) {
            for (int i = 0; i < Gen6Constants.numberOfPickupItems; i++) {
                int itemOffset = pickupItemsTableOffset + (2 * i);
                int item = FileFunctions.read2ByteInt(code, itemOffset);
                PickupItem pickupItem = new PickupItem(item);
                pickupItems.add(pickupItem);
            }
        }

        // Assuming we got the items from the last step, fill out the probabilities.
        if (pickupItems.size() > 0) {
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
        return pickupItems;
    }

    @Override
    public void setPickupItems(List<PickupItem> pickupItems) {
        if (pickupItemsTableOffset > 0) {
            for (int i = 0; i < Gen6Constants.numberOfPickupItems; i++) {
                int itemOffset = pickupItemsTableOffset + (2 * i);
                int item = pickupItems.get(i).item;
                FileFunctions.write2ByteInt(code, itemOffset, item);
            }
        }
    }

    private void computeCRC32sForRom() throws IOException {
        this.actualFileCRC32s = new HashMap<>();
        this.actualCodeCRC32 = FileFunctions.getCRC32(code);
        for (String fileKey : romEntry.getFileKeys()) {
            byte[] file = readFile(romEntry.getFile(fileKey));
            long crc32 = FileFunctions.getCRC32(file);
            this.actualFileCRC32s.put(fileKey, crc32);
        }
    }

    @Override
    public boolean isRomValid() {
        int index = this.hasGameUpdateLoaded() ? 1 : 0;
        if (romEntry.getExpectedCodeCRC32s()[index] != actualCodeCRC32) {
            return false;
        }

        for (String fileKey : romEntry.getFileKeys()) {
            long expectedCRC32 = romEntry.getFileExpectedCRC32s(fileKey)[index];
            long actualCRC32 = actualFileCRC32s.get(fileKey);
            if (expectedCRC32 != actualCRC32) {
                System.out.println(actualCRC32);
                return false;
            }
        }

        return true;
    }

    @Override
    public String getGARCPath(String fileName) {
        return romEntry.getFile(fileName);
    }

    @Override
    public BufferedImage getPokemonIcon(int iconIndex, GARCArchive pokeGraphicsGARC,
                                        boolean transparentBackground, boolean includePalette) {
        byte[] icon = pokeGraphicsGARC.files.get(iconIndex).get(0);
        Palette palette = Palette.read3DSIconPalette(icon);

        int width = 64;
        int height = 32;
        // Get the picture and uncompress it.
        byte[] uncompressedPic = Arrays.copyOfRange(icon, 4 + palette.size() * 2,
                4 + palette.size() * 2 + width * height);

        int bpp = palette.size() <= 0x10 ? 4 : 8;
        BufferedImage bim = GFXFunctions.drawTiledZOrderImage(uncompressedPic, palette.toARGB(), 0, width, height, bpp);

        // Unscramble the above onto a 96x96 canvas
        BufferedImage finalImage = new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics g = finalImage.getGraphics();
        g.drawImage(bim, 0, 0, 64, 64, 0, 0, 64, 64, null);
        g.drawImage(bim, 64, 0, 96, 8, 0, 64, 32, 72, null);
        g.drawImage(bim, 64, 8, 96, 16, 32, 64, 64, 72, null);
        g.drawImage(bim, 64, 16, 96, 24, 0, 72, 32, 80, null);
        g.drawImage(bim, 64, 24, 96, 32, 32, 72, 64, 80, null);
        g.drawImage(bim, 64, 32, 96, 40, 0, 80, 32, 88, null);
        g.drawImage(bim, 64, 40, 96, 48, 32, 80, 64, 88, null);
        g.drawImage(bim, 64, 48, 96, 56, 0, 88, 32, 96, null);
        g.drawImage(bim, 64, 56, 96, 64, 32, 88, 64, 96, null);
        g.drawImage(bim, 0, 64, 64, 96, 0, 96, 64, 128, null);
        g.drawImage(bim, 64, 64, 96, 72, 0, 128, 32, 136, null);
        g.drawImage(bim, 64, 72, 96, 80, 32, 128, 64, 136, null);
        g.drawImage(bim, 64, 80, 96, 88, 0, 136, 32, 144, null);
        g.drawImage(bim, 64, 88, 96, 96, 32, 136, 64, 144, null);

        finalImage = GFXFunctions.pseudoTransparent(finalImage, palette.get(0).toARGB());

        // Phew, all done.
        return finalImage;
    }

    @Override
    public List<Integer> getAllHeldItems() {
        return Gen6Constants.allHeldItems;
    }

    @Override
    public List<Integer> getAllConsumableHeldItems() {
        return Gen6Constants.consumableHeldItems;
    }

    @Override
    public List<Integer> getSensibleHeldItemsFor(TrainerPokemon tp, boolean consumableOnly, List<Move> moves, int[] pokeMoves) {
        List<Integer> items = new ArrayList<>(Gen6Constants.generalPurposeConsumableItems);
        int frequencyBoostCount = 6; // Make some very good items more common, but not too common
        if (!consumableOnly) {
            frequencyBoostCount = 8; // bigger to account for larger item pool.
            items.addAll(Gen6Constants.generalPurposeItems);
        }
        int numDamagingMoves = 0;
        for (int moveIdx : pokeMoves) {
            Move move = moves.get(moveIdx);
            if (move == null) {
                continue;
            }
            if (move.category == MoveCategory.PHYSICAL) {
                numDamagingMoves++;
                items.add(Items.liechiBerry);
                items.add(Gen6Constants.consumableTypeBoostingItems.get(move.type));
                if (!consumableOnly) {
                    items.addAll(Gen6Constants.typeBoostingItems.get(move.type));
                    items.add(Items.choiceBand);
                    items.add(Items.muscleBand);
                }
            }
            if (move.category == MoveCategory.SPECIAL) {
                numDamagingMoves++;
                items.add(Items.petayaBerry);
                items.add(Gen6Constants.consumableTypeBoostingItems.get(move.type));
                if (!consumableOnly) {
                    items.addAll(Gen6Constants.typeBoostingItems.get(move.type));
                    items.add(Items.wiseGlasses);
                    items.add(Items.choiceSpecs);
                }
            }
            if (!consumableOnly && Gen6Constants.moveBoostingItems.containsKey(moveIdx)) {
                items.addAll(Gen6Constants.moveBoostingItems.get(moveIdx));
            }
        }
        if (numDamagingMoves >= 2) {
            items.add(Items.assaultVest);
        }
        Map<Type, Effectiveness> byType = getTypeTable().against(tp.pokemon.getPrimaryType(), tp.pokemon.getSecondaryType());
        for(Map.Entry<Type, Effectiveness> entry : byType.entrySet()) {
            Integer berry = Gen6Constants.weaknessReducingBerries.get(entry.getKey());
            if (entry.getValue() == Effectiveness.DOUBLE) {
                items.add(berry);
            } else if (entry.getValue() == Effectiveness.QUADRUPLE) {
                for (int i = 0; i < frequencyBoostCount; i++) {
                    items.add(berry);
                }
            }
        }
        if (byType.get(Type.NORMAL) == Effectiveness.NEUTRAL) {
            items.add(Items.chilanBerry);
        }

        int ability = this.getAbilityForTrainerPokemon(tp);
        if (ability == Abilities.levitate) {
            items.removeAll(List.of(Items.shucaBerry));
        } else if (byType.get(Type.GROUND) == Effectiveness.DOUBLE || byType.get(Type.GROUND) == Effectiveness.QUADRUPLE) {
            items.add(Items.airBalloon);
        }

        if (!consumableOnly) {
            if (Gen6Constants.abilityBoostingItems.containsKey(ability)) {
                items.addAll(Gen6Constants.abilityBoostingItems.get(ability));
            }
            if (tp.pokemon.getPrimaryType() == Type.POISON || tp.pokemon.getSecondaryType() == Type.POISON) {
                items.add(Items.blackSludge);
            }
            List<Integer> speciesItems = Gen6Constants.speciesBoostingItems.get(tp.pokemon.getNumber());
            if (speciesItems != null) {
                for (int i = 0; i < frequencyBoostCount; i++) {
                    items.addAll(speciesItems);
                }
            }
            if (!tp.pokemon.getEvolutionsFrom().isEmpty() && tp.level >= 20) {
                // eviolite can be too good for early game, so we gate it behind a minimum level.
                // We go with the same level as the option for "No early wonder guard".
                items.add(Items.eviolite);
            }
        }
        return items;
    }

    @Override
    protected Gen6RomEntry getRomEntry() {
        return romEntry;
    }
}
