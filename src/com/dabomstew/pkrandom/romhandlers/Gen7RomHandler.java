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

import com.dabomstew.pkrandom.FileFunctions;
import com.dabomstew.pkrandom.MiscTweak;
import com.dabomstew.pkrandom.RomFunctions;
import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.constants.*;
import com.dabomstew.pkrandom.ctr.AMX;
import com.dabomstew.pkrandom.ctr.BFLIM;
import com.dabomstew.pkrandom.ctr.GARCArchive;
import com.dabomstew.pkrandom.ctr.Mini;
import com.dabomstew.pkrandom.exceptions.RomIOException;
import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.romhandlers.romentries.Gen7RomEntry;
import com.dabomstew.pkrandom.romhandlers.romentries.ThreeDSLinkedEncounter;
import pptxt.N3DSTxtHandler;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * {@link RomHandler} for Sun, Moon, Ultra Sun, Ultra Moon.
 */
public class Gen7RomHandler extends Abstract3DSRomHandler {

    public static class Factory extends RomHandler.Factory {

        @Override
        public Gen7RomHandler create() {
            return new Gen7RomHandler();
        }

        public boolean isLoadable(String filename) {
            return detect3DSRomInner(getProductCodeFromFile(filename), getTitleIdFromFile(filename));
        }
    }
    
    private static List<Gen7RomEntry> roms;

    static {
        loadROMInfo();
    }

    private static void loadROMInfo() {
        try {
            roms = Gen7RomEntry.READER.readEntriesFromFile("gen7_offsets.ini");
        } catch (IOException e) {
            throw new RuntimeException("Could not read Rom Entries.", e);
        }
    }

    // This ROM
    private Pokemon[] pokes;
    private final TreeMap<Integer, Integer> formeMappings = new TreeMap<>();
    private Map<Integer,Map<Integer,Integer>> absolutePokeNumByBaseForme;
    private Map<Integer,Integer> dummyAbsolutePokeNums;
    private List<Pokemon> pokemonList;
    private List<Pokemon> pokemonListInclFormes;
    private List<MegaEvolution> megaEvolutions;
    private List<AreaData> areaDataList;
    private Move[] moves;
    private Gen7RomEntry romEntry;
    private byte[] code;
    private List<String> itemNames;
    private List<String> shopNames;
    private List<String> abilityNames;
    private ItemList allowedItems, nonBadItems;
    private long actualCodeCRC32;
    private Map<String, Long> actualFileCRC32s;

    private GARCArchive pokeGarc, moveGarc, encounterGarc, stringsGarc, storyTextGarc;

    @Override
    protected boolean detect3DSRom(String productCode, String titleId) {
        return detect3DSRomInner(productCode, titleId);
    }

    private static boolean detect3DSRomInner(String productCode, String titleId) {
        return entryFor(productCode, titleId) != null;
    }

    private static Gen7RomEntry entryFor(String productCode, String titleId) {
        if (productCode == null || titleId == null) {
            return null;
        }

        for (Gen7RomEntry re : roms) {
            if (productCode.equals(re.getRomCode()) && titleId.equals(re.getTitleID())) {
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
            stringsGarc = readGARC(romEntry.getFile("TextStrings"), true);
            storyTextGarc = readGARC(romEntry.getFile("StoryText"), true);
            areaDataList = getAreaData();
        } catch (IOException e) {
            throw new RomIOException(e);
        }

        loadPokemonStats();
        loadMoves();

        pokemonListInclFormes = Arrays.asList(pokes);
        pokemonList = Arrays.asList(Arrays.copyOfRange(pokes,0,Gen7Constants.getPokemonCount(romEntry.getRomType()) + 1));

        itemNames = getStrings(false,romEntry.getIntValue("ItemNamesTextOffset"));
        abilityNames = getStrings(false,romEntry.getIntValue("AbilityNamesTextOffset"));
        shopNames = Gen7Constants.getShopNames(romEntry.getRomType());

        allowedItems = Gen7Constants.getAllowedItems(romEntry.getRomType()).copy();
        nonBadItems = Gen7Constants.nonBadItems.copy();

        try {
            computeCRC32sForRom();
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

    private void loadPokemonStats() {
        try {
            pokeGarc = this.readGARC(romEntry.getFile("PokemonStats"),true);
            String[] pokeNames = readPokemonNames();
            int pokemonCount = Gen7Constants.getPokemonCount(romEntry.getRomType());
            int formeCount = Gen7Constants.getFormeCount(romEntry.getRomType());
            pokes = new Pokemon[pokemonCount + formeCount + 1];
            for (int i = 1; i <= pokemonCount; i++) {
                pokes[i] = new Pokemon(i);
                loadBasicPokeStats(pokes[i],pokeGarc.files.get(i).get(0));
                pokes[i].setName(pokeNames[i]);
                pokes[i].setGeneration(generationOf(pokes[i]));
            }

            absolutePokeNumByBaseForme = new HashMap<>();
            dummyAbsolutePokeNums = new HashMap<>();
            dummyAbsolutePokeNums.put(255,0);

            int i = pokemonCount + 1;
            int formNum = 1;
            int prevSpecies = 0;
            Map<Integer,Integer> currentMap = new HashMap<>();
            for (int k: formeMappings.keySet()) {
                pokes[i] = new Pokemon(i);
                loadBasicPokeStats(pokes[i], pokeGarc.files.get(k).get(0));
                Pokemon baseForme = pokes[formeMappings.get(k)];
                baseForme.addAltForme(pokes[i], Gen7Constants.formeSuffixesByBaseForme);

                int realBaseForme = baseForme.isAltForme() ? baseForme.getBaseForme().getNumber() : baseForme.getNumber();
                if (realBaseForme == prevSpecies) {
                    formNum++;
                    currentMap.put(formNum,i);
                } else {
                    if (prevSpecies != 0) {
                        absolutePokeNumByBaseForme.put(prevSpecies,currentMap);
                    }
                    prevSpecies = realBaseForme;
                    formNum = 1;
                    currentMap = new HashMap<>();
                    currentMap.put(formNum,i);
                }
                pokes[i].setGeneration(generationOf(pokes[i]));
                i++;
            }
            if (prevSpecies != 0) {
                absolutePokeNumByBaseForme.put(prevSpecies,currentMap);
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }
        populateEvolutions();
        populateMegaEvolutions();
    }

    private int generationOf(Pokemon pk) {
        if (pk.getFormeSuffix().equals("-Alolan") || pk.getFormeSuffix().equals("-Ash") ||
                pk.getFormeSuffix().equals("-10%") || pk.getFormeSuffix().equals("-Complete")) {
            return 7;
        }
        if (pk.getFormeSuffix().startsWith("-Mega") || pk.getFormeSuffix().equals("-Primal")) {
            return 6;
        }
        if (pk.isAltForme()) {
            if (pk.getBaseNumber() == Species.pikachu) {
                return 6; // contest pikachu
            }
            return pk.getBaseForme().getGeneration();
        }
        if (pk.getNumber() >= Species.rowlet) {
            return 7;
        } else if (pk.getNumber() >= Species.chespin) {
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
        pkmn.setHp(stats[Gen7Constants.bsHPOffset] & 0xFF);
        pkmn.setAttack(stats[Gen7Constants.bsAttackOffset] & 0xFF);
        pkmn.setDefense(stats[Gen7Constants.bsDefenseOffset] & 0xFF);
        pkmn.setSpeed(stats[Gen7Constants.bsSpeedOffset] & 0xFF);
        pkmn.setSpatk(stats[Gen7Constants.bsSpAtkOffset] & 0xFF);
        pkmn.setSpdef(stats[Gen7Constants.bsSpDefOffset] & 0xFF);
        // Type
        pkmn.setPrimaryType(Gen7Constants.typeTable[stats[Gen7Constants.bsPrimaryTypeOffset] & 0xFF]);
        pkmn.setSecondaryType(Gen7Constants.typeTable[stats[Gen7Constants.bsSecondaryTypeOffset] & 0xFF]);
        // Only one type?
        if (pkmn.getSecondaryType() == pkmn.getPrimaryType()) {
            pkmn.setSecondaryType(null);
        }
        pkmn.setCatchRate(stats[Gen7Constants.bsCatchRateOffset] & 0xFF);
        pkmn.setGrowthCurve(ExpCurve.fromByte(stats[Gen7Constants.bsGrowthCurveOffset]));

        pkmn.setAbility1(stats[Gen7Constants.bsAbility1Offset] & 0xFF);
        pkmn.setAbility2(stats[Gen7Constants.bsAbility2Offset] & 0xFF);
        pkmn.setAbility3(stats[Gen7Constants.bsAbility3Offset] & 0xFF);
        if (pkmn.getAbility1() == pkmn.getAbility2()) {
            pkmn.setAbility2(0);
        }

        pkmn.setCallRate(stats[Gen7Constants.bsCallRateOffset] & 0xFF);

        // Held Items?
        int item1 = FileFunctions.read2ByteInt(stats, Gen7Constants.bsCommonHeldItemOffset);
        int item2 = FileFunctions.read2ByteInt(stats, Gen7Constants.bsRareHeldItemOffset);

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

        int formeCount = stats[Gen7Constants.bsFormeCountOffset] & 0xFF;
        if (formeCount > 1) {
            if (!formeMappings.containsKey(pkmn.getNumber())) {
                int firstForme = FileFunctions.read2ByteInt(stats, Gen7Constants.bsFormeOffset);
                if (firstForme != 0) {
                    int j = 0;
                    int jMax = 0;
                    int theAltForme = 0;
                    Set<Integer> altFormesWithCosmeticForms = Gen7Constants.getAltFormesWithCosmeticForms(romEntry.getRomType()).keySet();
                    for (int i = 1; i < formeCount; i++) {
                        if (j == 0 || j > jMax) {
                            formeMappings.put(firstForme + i - 1, pkmn.getNumber()); // Assumes that formes are in memory in the same order as their numbers
                            if (Gen7Constants.getCosmeticForms(romEntry.getRomType()).contains(firstForme + i - 1)) {
                                if (!Gen7Constants.getIgnoreForms(romEntry.getRomType()).contains(firstForme + i - 1)) { // Skip ignored forms (identical or confusing cosmetic forms)
                                    pkmn.setCosmeticFormCount(pkmn.getCosmeticFormCount() + 1);
                                    pkmn.getRealCosmeticFormNumbers().add(i);
                                }
                            }
                        } else {
                            formeMappings.put(firstForme + i - 1, theAltForme);
                            j++;
                        }
                        if (altFormesWithCosmeticForms.contains(firstForme + i - 1)) {
                            j = 1;
                            jMax = Gen7Constants.getAltFormesWithCosmeticForms(romEntry.getRomType()).get(firstForme + i - 1);
                            theAltForme = firstForme + i - 1;
                        }
                    }
                } else {
                    if (pkmn.getNumber() != Species.arceus && pkmn.getNumber() != Species.genesect && pkmn.getNumber() != Species.xerneas && pkmn.getNumber() != Species.silvally) {
                        // Reason for exclusions:
                        // Arceus/Genesect/Silvally: to avoid confusion
                        // Xerneas: Should be handled automatically?
                        pkmn.setCosmeticFormCount(formeCount);
                    }
                }
            } else {
                if (!Gen7Constants.getIgnoreForms(romEntry.getRomType()).contains(pkmn.getNumber())) {
                    pkmn.setCosmeticFormCount(Gen7Constants.getAltFormesWithCosmeticForms(romEntry.getRomType()).getOrDefault(pkmn.getNumber(), 0));
                }
                if (Gen7Constants.getCosmeticForms(romEntry.getRomType()).contains(pkmn.getNumber())) {
                    pkmn.setCosmeticForme(true);
                }
            }
        }

        // The above code will add all alternate cosmetic forms to realCosmeticFormNumbers as necessary, but it will
        // NOT add the base form. For example, if we are currently looking at Mimikyu, it will add Totem Mimikyu to
        // the list of realCosmeticFormNumbers, but it will not add normal-sized Mimikyu. Without any corrections,
        // this will make base Mimikyu impossible to randomly select. The simplest way to fix this is to just add
        // the base form to the realCosmeticFormNumbers here if that list was populated above.
        if (pkmn.getRealCosmeticFormNumbers().size() > 0) {
            pkmn.getRealCosmeticFormNumbers().add(0);
            pkmn.setCosmeticFormCount(pkmn.getCosmeticFormCount() + 1); // getCosmeticForms++;
        }
    }

    private String[] readPokemonNames() {
        int pokemonCount = Gen7Constants.getPokemonCount(romEntry.getRomType());
        String[] pokeNames = new String[pokemonCount + 1];
        List<String> nameList = getStrings(false, romEntry.getIntValue("PokemonNamesTextOffset"));
        for (int i = 1; i <= pokemonCount; i++) {
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
            for (int i = 1; i <= Gen7Constants.getPokemonCount(romEntry.getRomType()) + Gen7Constants.getFormeCount(romEntry.getRomType()); i++) {
                Pokemon pk = pokes[i];
                byte[] evoEntry = evoGARC.files.get(i).get(0);
                boolean skipNext = false;
                for (int evo = 0; evo < 8; evo++) {
                    int method = readWord(evoEntry, evo * 8);
                    int species = readWord(evoEntry, evo * 8 + 4);
                    if (method >= 1 && method <= Gen7Constants.evolutionMethodCount && species >= 1) {
                        EvolutionType et = Gen7Constants.evolutionTypeFromIndex(method);
                        if (et.skipSplitEvo()) continue; // Remove Feebas "split" evolution
                        if (skipNext) {
                            skipNext = false;
                            continue;
                        }
                        if (et == EvolutionType.LEVEL_GAME) {
                            skipNext = true;
                        }

                        int extraInfo = readWord(evoEntry, evo * 8 + 2);
                        int forme = evoEntry[evo * 8 + 6];
                        int level = evoEntry[evo * 8 + 7];
                        Evolution evol = new Evolution(pk, getPokemonForEncounter(species,forme), true, et, extraInfo);
                        evol.setForme(forme);
                        evol.setLevel(level);
                        if (et.usesLevel()) {
                            evol.setExtraInfo(level);
                        }
                        switch (et) {
                            case LEVEL_GAME:
                                evol.setType(EvolutionType.LEVEL);
                                evol.setTo(pokes[romEntry.getIntValue("CosmoemEvolutionNumber")]);
                                break;
                            case LEVEL_DAY_GAME:
                                evol.setType(EvolutionType.LEVEL_DAY);
                                break;
                            case LEVEL_NIGHT_GAME:
                                evol.setType(EvolutionType.LEVEL_NIGHT);
                                break;
                            default:
                                break;
                        }
                        if (pk.isAltForme() && pk.getBaseForme().getNumber() == Species.rockruff && pk.isAltForme()) {
                            evol.setFrom(pk.getBaseForme());
                            pk.getBaseForme().getEvolutionsFrom().add(evol);
                            pokes[absolutePokeNumByBaseForme.get(species).get(evol.getForme())].getEvolutionsTo().add(evol);
                        }
                        if (!pk.getEvolutionsFrom().contains(evol)) {
                            pk.getEvolutionsFrom().add(evol);
                            if (!pk.isCosmeticForme()) {
                                if (evol.getForme() > 0) {
                                    // The forme number for the evolution might represent an actual alt forme, or it
                                    // might simply represent a cosmetic forme. If it represents an actual alt forme,
                                    // we'll need to figure out what the absolute species ID for that alt forme is
                                    // and update its evolutions. If it instead represents a cosmetic forme, then the
                                    // absolutePokeNumByBaseFormeMap will be null, since there's no secondary species
                                    // entry for this forme.
                                    Map<Integer, Integer> absolutePokeNumByBaseFormeMap = absolutePokeNumByBaseForme.get(species);
                                    if (absolutePokeNumByBaseFormeMap != null) {
                                        species = absolutePokeNumByBaseFormeMap.get(evol.getForme());
                                    }
                                }
                                pokes[species].getEvolutionsTo().add(evol);
                            }
                        }
                    }
                }

                // Nincada's Shedinja evo is hardcoded into the game's executable,
                // so if the Pokemon is Nincada, then let's and put it as one of its evolutions
                if (pk.getNumber() == Species.nincada) {
                    Pokemon shedinja = pokes[Species.shedinja];
                    Evolution evol = new Evolution(pk, shedinja, false, EvolutionType.LEVEL_IS_EXTRA, 20);
                    evol.setForme(-1);
                    evol.setLevel(20);
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
            for (int i = 1; i <= Gen7Constants.getPokemonCount(romEntry.getRomType()); i++) {
                Pokemon pk = pokes[i];
                byte[] megaEvoEntry = megaEvoGARC.files.get(i).get(0);
                for (int evo = 0; evo < 2; evo++) {
                    int formNum = readWord(megaEvoEntry, evo * 8);
                    int method = readWord(megaEvoEntry, evo * 8 + 2);
                    if (method >= 1) {
                        int argument = readWord(megaEvoEntry, evo * 8 + 4);
                        int megaSpecies = absolutePokeNumByBaseForme
                                .getOrDefault(pk.getNumber(),dummyAbsolutePokeNums)
                                .getOrDefault(formNum,0);
                        MegaEvolution megaEvo = new MegaEvolution(pk, pokes[megaSpecies], method, argument);
                        if (!pk.getMegaEvolutionsFrom().contains(megaEvo)) {
                            pk.getMegaEvolutionsFrom().add(megaEvo);
                            pokes[megaSpecies].getMegaEvolutionsTo().add(megaEvo);
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

    private void loadMoves() {
        try {
            moveGarc = this.readGARC(romEntry.getFile("MoveData"),true);
            int moveCount = Gen7Constants.getMoveCount(romEntry.getRomType());
            moves = new Move[moveCount + 1];
            List<String> moveNames = getStrings(false, romEntry.getIntValue("MoveNamesTextOffset"));
            byte[][] movesData = Mini.UnpackMini(moveGarc.files.get(0).get(0), "WD");
            for (int i = 1; i <= moveCount; i++) {
                byte[] moveData = movesData[i];
                moves[i] = new Move();
                moves[i].name = moveNames.get(i);
                moves[i].number = i;
                moves[i].internalId = i;
                moves[i].effectIndex = readWord(moveData, 16);
                moves[i].hitratio = (moveData[4] & 0xFF);
                moves[i].power = moveData[3] & 0xFF;
                moves[i].pp = moveData[5] & 0xFF;
                moves[i].type = Gen7Constants.typeTable[moveData[0] & 0xFF];
                moves[i].flinchPercentChance = moveData[15] & 0xFF;
                moves[i].target = moveData[20] & 0xFF;
                moves[i].category = Gen7Constants.moveCategoryIndices[moveData[2] & 0xFF];
                moves[i].priority = moveData[6];

                int critStages = moveData[14] & 0xFF;
                if (critStages == 6) {
                    moves[i].criticalChance = CriticalChance.GUARANTEED;
                } else if (critStages > 0) {
                    moves[i].criticalChance = CriticalChance.INCREASED;
                }

                int internalStatusType = readWord(moveData, 8);
                int flags = FileFunctions.readFullInt(moveData, 36);
                moves[i].makesContact = (flags & 0x001) != 0;
                moves[i].isChargeMove = (flags & 0x002) != 0;
                moves[i].isRechargeMove = (flags & 0x004) != 0;
                moves[i].isPunchMove = (flags & 0x080) != 0;
                moves[i].isSoundMove = (flags & 0x100) != 0;
                moves[i].isTrapMove = internalStatusType == 8;
                switch (moves[i].effectIndex) {
                    case Gen7Constants.noDamageTargetTrappingEffect:
                    case Gen7Constants.noDamageFieldTrappingEffect:
                    case Gen7Constants.damageAdjacentFoesTrappingEffect:
                    case Gen7Constants.damageTargetTrappingEffect:
                        moves[i].isTrapMove = true;
                        break;
                }

                int qualities = moveData[1];
                int recoilOrAbsorbPercent = moveData[18];
                if (qualities == Gen7Constants.damageAbsorbQuality) {
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
                    case Gen7Constants.noDamageStatChangeQuality:
                    case Gen7Constants.noDamageStatusAndStatChangeQuality:
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
                    case Gen7Constants.damageTargetDebuffQuality:
                        moves[i].statChangeMoveType = StatChangeMoveType.DAMAGE_TARGET;
                        break;
                    case Gen7Constants.damageUserBuffQuality:
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
                        case Gen7Constants.noDamageStatusQuality:
                        case Gen7Constants.noDamageStatusAndStatChangeQuality:
                            moves[i].statusMoveType = StatusMoveType.NO_DAMAGE;
                            break;
                        case Gen7Constants.damageStatusQuality:
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
            writeGARC(romEntry.getFile("WildPokemon"), encounterGarc);
            writeGARC(romEntry.getFile("TextStrings"), stringsGarc);
            writeGARC(romEntry.getFile("StoryText"), storyTextGarc);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    public void savePokemonStats() {
        int k = Gen7Constants.bsSize;
        int pokemonCount = Gen7Constants.getPokemonCount(romEntry.getRomType());
        int formeCount = Gen7Constants.getFormeCount(romEntry.getRomType());
        byte[] duplicateData = pokeGarc.files.get(pokemonCount + formeCount + 1).get(0);
        for (int i = 1; i <= pokemonCount + formeCount; i++) {
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
        stats[Gen7Constants.bsHPOffset] = (byte) pkmn.getHp();
        stats[Gen7Constants.bsAttackOffset] = (byte) pkmn.getAttack();
        stats[Gen7Constants.bsDefenseOffset] = (byte) pkmn.getDefense();
        stats[Gen7Constants.bsSpeedOffset] = (byte) pkmn.getSpeed();
        stats[Gen7Constants.bsSpAtkOffset] = (byte) pkmn.getSpatk();
        stats[Gen7Constants.bsSpDefOffset] = (byte) pkmn.getSpdef();
        stats[Gen7Constants.bsPrimaryTypeOffset] = Gen7Constants.typeToByte(pkmn.getPrimaryType());
        if (pkmn.getSecondaryType() == null) {
            stats[Gen7Constants.bsSecondaryTypeOffset] = stats[Gen7Constants.bsPrimaryTypeOffset];
        } else {
            stats[Gen7Constants.bsSecondaryTypeOffset] = Gen7Constants.typeToByte(pkmn.getSecondaryType());
        }
        stats[Gen7Constants.bsCatchRateOffset] = (byte) pkmn.getCatchRate();
        stats[Gen7Constants.bsGrowthCurveOffset] = pkmn.getGrowthCurve().toByte();

        stats[Gen7Constants.bsAbility1Offset] = (byte) pkmn.getAbility1();
        stats[Gen7Constants.bsAbility2Offset] = pkmn.getAbility2() != 0 ? (byte) pkmn.getAbility2() : (byte) pkmn.getAbility1();
        stats[Gen7Constants.bsAbility3Offset] = (byte) pkmn.getAbility3();

        stats[Gen7Constants.bsCallRateOffset] = (byte) pkmn.getCallRate();

        // Held items
        if (pkmn.getGuaranteedHeldItem() > 0) {
            FileFunctions.write2ByteInt(stats, Gen7Constants.bsCommonHeldItemOffset, pkmn.getGuaranteedHeldItem());
            FileFunctions.write2ByteInt(stats, Gen7Constants.bsRareHeldItemOffset, pkmn.getGuaranteedHeldItem());
            FileFunctions.write2ByteInt(stats, Gen7Constants.bsDarkGrassHeldItemOffset, 0);
        } else {
            FileFunctions.write2ByteInt(stats, Gen7Constants.bsCommonHeldItemOffset, pkmn.getCommonHeldItem());
            FileFunctions.write2ByteInt(stats, Gen7Constants.bsRareHeldItemOffset, pkmn.getRareHeldItem());
            FileFunctions.write2ByteInt(stats, Gen7Constants.bsDarkGrassHeldItemOffset, 0);
        }

        if (pkmn.fullName().equals("Meowstic")) {
            stats[Gen7Constants.bsGenderOffset] = 0;
        } else if (pkmn.fullName().equals("Meowstic-F")) {
            stats[Gen7Constants.bsGenderOffset] = (byte)0xFE;
        }
    }

    private void writeEvolutions() {
        try {
            GARCArchive evoGARC = readGARC(romEntry.getFile("PokemonEvolutions"),true);
            for (int i = 1; i <= Gen7Constants.getPokemonCount(romEntry.getRomType()) + Gen7Constants.getFormeCount(romEntry.getRomType()); i++) {
                byte[] evoEntry = evoGARC.files.get(i).get(0);
                Pokemon pk = pokes[i];
                if (pk.getNumber() == Species.nincada) {
                    writeShedinjaEvolution();
                }
                int evosWritten = 0;
                for (Evolution evo : pk.getEvolutionsFrom()) {
                    Pokemon toPK = evo.getTo();
                    writeWord(evoEntry, evosWritten * 8, Gen7Constants.evolutionTypeToIndex(evo.getType()));
                    writeWord(evoEntry, evosWritten * 8 + 2, evo.getType().usesLevel() ? 0 : evo.getExtraInfo());
                    writeWord(evoEntry, evosWritten * 8 + 4, toPK.getBaseNumber());
                    evoEntry[evosWritten * 8 + 6] = (byte) evo.getForme();
                    evoEntry[evosWritten * 8 + 7] = evo.getType().usesLevel() ? (byte) evo.getExtraInfo() : (byte) evo.getLevel();
                    evosWritten++;
                    if (evosWritten == 8) {
                        break;
                    }
                }
                while (evosWritten < 8) {
                    writeWord(evoEntry, evosWritten * 8, 0);
                    writeWord(evoEntry, evosWritten * 8 + 2, 0);
                    writeWord(evoEntry, evosWritten * 8 + 4, 0);
                    writeWord(evoEntry, evosWritten * 8 + 6, 0);
                    evosWritten++;
                }
            }
            writeGARC(romEntry.getFile("PokemonEvolutions"), evoGARC);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    private void writeShedinjaEvolution() {
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

        // In the game's executable, there's a hardcoded check to see if the Pokemon
        // that just evolved is now a Ninjask after evolving; if it is, then we start
        // going down the path of creating a Shedinja. To accomplish this check, they
        // hardcoded Ninjask's species ID as a constant. We replace this constant
        // with the species ID of Nincada's new primary evolution; that way, evolving
        // Nincada will still produce an "extra" Pokemon like in older generations.
        int offset = find(code, Gen7Constants.ninjaskSpeciesPrefix);
        if (offset > 0) {
            offset += Gen7Constants.ninjaskSpeciesPrefix.length() / 2; // because it was a prefix
            FileFunctions.writeFullInt(code, offset, primaryEvolution.getBaseNumber());
        }

        // In the game's executable, there's a hardcoded value to indicate what "extra"
        // Pokemon to create. It produces a Shedinja using the following instruction:
        // mov r1, #0x124, where 0x124 = 292 in decimal, which is Shedinja's species ID.
        // We can't just blindly replace it, though, because certain constants (for example,
        // 0x125) cannot be moved without using the movw instruction. This works fine in
        // Citra, but crashes on real hardware. Instead, we have to annoyingly shift up a
        // big chunk of code to fill in a nop; we can then do a pc-relative load to a
        // constant in the new free space.
        offset = find(code, Gen7Constants.shedinjaPrefix);
        if (offset > 0) {
            offset += Gen7Constants.shedinjaPrefix.length() / 2; // because it was a prefix

            // Shift up everything below the last nop to make some room at the bottom of the function.
            for (int i = 84; i < 120; i++) {
                code[offset + i] = code[offset + i + 4];
            }

            // For every bl that we shifted up, patch them so they're now pointing to the same place they
            // were before (without this, they will be pointing to 0x4 before where they're supposed to).
            List<Integer> blOffsetsToPatch = Arrays.asList(84, 96, 108);
            for (int blOffsetToPatch : blOffsetsToPatch) {
                code[offset + blOffsetToPatch] += 1;
            }

            // Write Nincada's new extra evolution in the new free space.
            writeLong(code, offset + 120, extraEvolution.getBaseNumber());

            // Second parameter of pml::pokepara::CoreParam::ChangeMonsNo is the
            // new forme number
            code[offset] = (byte) extraEvolution.getFormeNumber();

            // First parameter of pml::pokepara::CoreParam::ChangeMonsNo is the
            // new species number. Write a pc-relative load to what we wrote before.
            code[offset + 4] = (byte) 0x6C;
            code[offset + 5] = 0x10;
            code[offset + 6] = (byte) 0x9F;
            code[offset + 7] = (byte) 0xE5;
        }

        // Now that we've handled the hardcoded Shedinja evolution, delete it so that
        // we do *not* handle it in WriteEvolutions
        nincada.getEvolutionsFrom().remove(1);
        extraEvolution.getEvolutionsTo().remove(0);
    }

    @Override
    public void saveMoves() {
        int moveCount = Gen7Constants.getMoveCount(romEntry.getRomType());
        byte[][] movesData = Mini.UnpackMini(moveGarc.files.get(0).get(0), "WD");
        for (int i = 1; i <= moveCount; i++) {
            byte[] moveData = movesData[i];
            moveData[2] = Gen7Constants.moveCategoryToByte(moves[i].category);
            moveData[3] = (byte) moves[i].power;
            moveData[0] = Gen7Constants.typeToByte(moves[i].type);
            int hitratio = (int) Math.round(moves[i].hitratio);
            if (hitratio < 0) {
                hitratio = 0;
            }
            if (hitratio > 101) {
                hitratio = 100;
            }
            moveData[4] = (byte) hitratio;
            moveData[5] = (byte) moves[i].pp;
        }
        try {
            moveGarc.setFile(0, Mini.PackMini(movesData, "WD"));
            this.writeGARC(romEntry.getFile("MoveData"), moveGarc);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    private void patchFormeReversion() throws IOException {
        // Upon loading a save, all Mega Pokemon, all Primal Reversions,
        // all Greninja-A, all Zygarde-C, and all Necrozma-U in the player's
        // party are set back to their base forme. This patches .code such
        // that this reversion does not happen.
        String saveLoadFormeReversionPrefix = Gen7Constants.getSaveLoadFormeReversionPrefix(romEntry.getRomType());
        int offset = find(code, saveLoadFormeReversionPrefix);
        if (offset > 0) {
            offset += saveLoadFormeReversionPrefix.length() / 2; // because it was a prefix

            // The actual offset of the code we want to patch is 8 bytes from the end of
            // the prefix. We have to do this because these 8 bytes differ between the
            // base game and all game updates, so we cannot use them as part of our prefix.
            offset += 8;

            // Stubs the call to the function that checks for Primal Reversions and
            // Mega Pokemon
            code[offset] = 0x00;
            code[offset + 1] = 0x00;
            code[offset + 2] = 0x00;
            code[offset + 3] = 0x00;

            if (romEntry.getRomType() == Gen7Constants.Type_USUM) {
                // In Sun/Moon, Greninja-A and Zygarde-C are treated as Mega Pokemon
                // and handled by the function above. In USUM, they are handled by a
                // different function, along with Necrozma-U. This stubs the call
                // to that function.
                code[offset + 8] = 0x00;
                code[offset + 9] = 0x00;
                code[offset + 10] = 0x00;
                code[offset + 11] = 0x00;
            }
        }

        // Additionally, upon completing a battle, Kyogre-P, Groudon-P,
        // and Wishiwashi-S are forcibly returned to their base forme.
        // Minior is also forcibly set to the "correct" Core forme.
        // This patches the Battle CRO to prevent this from happening.
        byte[] battleCRO = readFile(romEntry.getFile("Battle"));
        offset = find(battleCRO, Gen7Constants.afterBattleFormeReversionPrefix);
        if (offset > 0) {
            offset += Gen7Constants.afterBattleFormeReversionPrefix.length() / 2; // because it was a prefix

            // Stubs the call to pml::pokepara::CoreParam::ChangeFormNo for Kyogre
            battleCRO[offset] = 0x00;
            battleCRO[offset + 1] = 0x00;
            battleCRO[offset + 2] = 0x00;
            battleCRO[offset + 3] = 0x00;

            // Stubs the call to pml::pokepara::CoreParam::ChangeFormNo for Groudon
            battleCRO[offset + 60] = 0x00;
            battleCRO[offset + 61] = 0x00;
            battleCRO[offset + 62] = 0x00;
            battleCRO[offset + 63] = 0x00;

            // Stubs the call to pml::pokepara::CoreParam::ChangeFormNo for Wishiwashi
            battleCRO[offset + 92] = 0x00;
            battleCRO[offset + 93] = 0x00;
            battleCRO[offset + 94] = 0x00;
            battleCRO[offset + 95] = 0x00;

            // Stubs the call to pml::pokepara::CoreParam::ChangeFormNo for Minior
            battleCRO[offset + 148] = 0x00;
            battleCRO[offset + 149] = 0x00;
            battleCRO[offset + 150] = 0x00;
            battleCRO[offset + 151] = 0x00;

            writeFile(romEntry.getFile("Battle"), battleCRO);
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
    public List<Pokemon> getPokemon() {
        return pokemonList;
    }

    @Override
    public List<Pokemon> getPokemonInclFormes() {
        return pokemonListInclFormes;
    }

	@Override
	public PokemonSet<Pokemon> getAltFormes() {
		int formeCount = Gen7Constants.getFormeCount(romEntry.getRomType());
		int pokemonCount = Gen7Constants.getPokemonCount(romEntry.getRomType());
		return new PokemonSet<>(pokemonListInclFormes.subList(pokemonCount + 1, pokemonCount + formeCount + 1));
	}

    @Override
    public List<MegaEvolution> getMegaEvolutions() {
        return megaEvolutions;
    }

    @Override
    public Pokemon getAltFormeOfPokemon(Pokemon pk, int forme) {
        int pokeNum = absolutePokeNumByBaseForme.getOrDefault(pk.getNumber(),dummyAbsolutePokeNums).getOrDefault(forme,0);
        return pokeNum != 0 ? !pokes[pokeNum].isCosmeticForme() ? pokes[pokeNum] : pokes[pokeNum].getBaseForme() : pk;
    }

	@Override
	public PokemonSet<Pokemon> getIrregularFormes() {
		return Gen7Constants.getIrregularFormes(romEntry.getRomType())
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
            GARCArchive staticGarc = readGARC(romEntry.getFile("StaticPokemon"), true);
            byte[] giftsFile = staticGarc.files.get(0).get(0);
            for (int i = 0; i < 3; i++) {
                int offset = i * 0x14;
                StaticEncounter se = new StaticEncounter();
                int species = FileFunctions.read2ByteInt(giftsFile, offset);
                Pokemon pokemon = pokes[species];
                int forme = giftsFile[offset + 2];
                if (forme > pokemon.getCosmeticFormCount() && forme != 30 && forme != 31) {
                    int speciesWithForme = absolutePokeNumByBaseForme
                            .getOrDefault(species, dummyAbsolutePokeNums)
                            .getOrDefault(forme, 0);
                    pokemon = pokes[speciesWithForme];
                }
                se.pkmn = pokemon;
                se.forme = forme;
                se.level = giftsFile[offset + 3];
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
            GARCArchive staticGarc = readGARC(romEntry.getFile("StaticPokemon"), true);
            byte[] giftsFile = staticGarc.files.get(0).get(0);
            for (int i = 0; i < 3; i++) {
                int offset = i * 0x14;
                Pokemon starter = newStarters.get(i);
                int forme = 0;
                if (starter.isAltForme()) {
                    forme = starter.getFormeNumber();
                    starter = starter.getBaseForme();
                }
                writeWord(giftsFile, offset, starter.getNumber());
                giftsFile[offset + 2] = (byte) forme;
            }
            writeGARC(romEntry.getFile("StaticPokemon"), staticGarc);
            setStarterText(newStarters);
            return true;
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    // TODO: We should be editing the script file so that the game reads in our new
    // starters; this way, strings that depend on the starter defined in the script
    // would work without any modification. Instead, we're just manually editing all
    // strings here, and if a string originally referred to the starter in the script,
    // we just hardcode the starter's name if we can get away with it.
    private void setStarterText(List<Pokemon> newStarters) {
        int starterTextIndex = romEntry.getIntValue("StarterTextOffset");
        List<String> starterText = getStrings(true, starterTextIndex);
        if (romEntry.getRomType() == Gen7Constants.Type_USUM) {
            String rowletDescriptor = newStarters.get(0).getName() + starterText.get(1).substring(6);
            String littenDescriptor = newStarters.get(1).getName() + starterText.get(2).substring(6);
            String popplioDescriptor = newStarters.get(2).getName() + starterText.get(3).substring(7);
            starterText.set(1, rowletDescriptor);
            starterText.set(2, littenDescriptor);
            starterText.set(3, popplioDescriptor);
            for (int i = 0; i < 3; i++) {
                int confirmationOffset = i + 7;
                int optionOffset = i + 14;
                Pokemon starter = newStarters.get(i);
                String confirmationText = String.format("So, you wanna go with the %s-type Pokémon\\n%s?[VAR 0114(0005)]",
                        starter.getPrimaryType().camelCase(), starter.getName());
                String optionText = starter.getName();
                starterText.set(confirmationOffset, confirmationText);
                starterText.set(optionOffset, optionText);
            }
        } else {
            String rowletDescriptor = newStarters.get(0).getName() + starterText.get(11).substring(6);
            String littenDescriptor = newStarters.get(1).getName() + starterText.get(12).substring(6);
            String popplioDescriptor = newStarters.get(2).getName() + starterText.get(13).substring(7);
            starterText.set(11, rowletDescriptor);
            starterText.set(12, littenDescriptor);
            starterText.set(13, popplioDescriptor);
            for (int i = 0; i < 3; i++) {
                int optionOffset = i + 1;
                int confirmationOffset = i + 4;
                int flavorOffset = i + 35;
                Pokemon starter = newStarters.get(i);
                String optionText = String.format("The %s-type %s", starter.getPrimaryType().camelCase(), starter.getName());
                String confirmationText = String.format("Will you choose the %s-type Pokémon\\n%s?[VAR 0114(0008)]",
                        starter.getPrimaryType().camelCase(), starter.getName());
                String flavorSubstring = starterText.get(flavorOffset).substring(starterText.get(flavorOffset).indexOf("\\n"));
                String flavorText = String.format("The %s-type %s", starter.getPrimaryType().camelCase(), starter.getName()) + flavorSubstring;
                starterText.set(optionOffset, optionText);
                starterText.set(confirmationOffset, confirmationText);
                starterText.set(flavorOffset, flavorText);
            }
        }
        setStrings(true, starterTextIndex, starterText);
    }

    @Override
    public boolean hasStarterAltFormes() {
        return true;
    }

    @Override
    public int starterCount() {
        return 3;
    }

    @Override
    public boolean supportsStarterHeldItems() {
        return true;
    }

    @Override
    public List<Integer> getStarterHeldItems() {
        List<Integer> starterHeldItems = new ArrayList<>();
        try {
            GARCArchive staticGarc = readGARC(romEntry.getFile("StaticPokemon"), true);
            byte[] giftsFile = staticGarc.files.get(0).get(0);
            for (int i = 0; i < 3; i++) {
                int offset = i * 0x14;
                int item = FileFunctions.read2ByteInt(giftsFile, offset + 8);
                starterHeldItems.add(item);
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }
        return starterHeldItems;
    }

    @Override
    public void setStarterHeldItems(List<Integer> items) {
        try {
            GARCArchive staticGarc = readGARC(romEntry.getFile("StaticPokemon"), true);
            byte[] giftsFile = staticGarc.files.get(0).get(0);
            for (int i = 0; i < 3; i++) {
                int offset = i * 0x14;
                int item = items.get(i);
                FileFunctions.write2ByteInt(giftsFile, offset + 8, item);
            }
            writeGARC(romEntry.getFile("StaticPokemon"), staticGarc);
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
        List<EncounterArea> encounterAreas = new ArrayList<>();
        for (AreaData areaData : areaDataList) {
            if (!areaData.hasTables) {
                continue;
            }
            for (int i = 0; i < areaData.encounterTables.size(); i++) {
                byte[] encounterTable = areaData.encounterTables.get(i);
                byte[] dayTable = new byte[0x164];
                System.arraycopy(encounterTable, 0, dayTable, 0, 0x164);
                EncounterArea dayArea = readEncounterTable(dayTable);
                if (!useTimeOfDay) {
                    dayArea.setDisplayName(areaData.name + ", Table " + (i + 1));
                    encounterAreas.add(dayArea);
                } else {
                    dayArea.setDisplayName(areaData.name + ", Table " + (i + 1) + " (Day)");
                    encounterAreas.add(dayArea);
                    byte[] nightTable = new byte[0x164];
                    System.arraycopy(encounterTable, 0x164, nightTable, 0, 0x164);
                    EncounterArea nightArea = readEncounterTable(nightTable);
                    nightArea.setDisplayName(areaData.name + ", Table " + (i + 1) + " (Night)");
                    encounterAreas.add(nightArea);
                }
            }
        }
        Gen7Constants.tagEncounterAreas(encounterAreas, romEntry.getRomType(), useTimeOfDay);
        return encounterAreas;
    }

    private EncounterArea readEncounterTable(byte[] encounterTable) {
        int minLevel = encounterTable[0];
        int maxLevel = encounterTable[1];
        EncounterArea area = new EncounterArea();
        area.setRate(1);
        for (int i = 0; i < 10; i++) {
            int offset = 0xC + (i * 4);
            int speciesAndFormeData = readWord(encounterTable, offset);
            int species = speciesAndFormeData & 0x7FF;
            int forme = speciesAndFormeData >> 11;
            if (species != 0) {
                Encounter enc = new Encounter();
                enc.setPokemon(getPokemonForEncounter(species, forme));
                enc.setFormeNumber(forme);
                enc.setLevel(minLevel);
                enc.setMaxLevel(maxLevel);
                area.add(enc);

                // Get all the SOS encounters for this non-SOS encounter
                for (int j = 1; j < 8; j++) {
                    species = readWord(encounterTable, offset + (40 * j)) & 0x7FF;
                    forme = readWord(encounterTable, offset + (40 * j)) >> 11;
                    Encounter sos = new Encounter();
                    sos.setPokemon(getPokemonForEncounter(species, forme));
                    sos.setFormeNumber(forme);
                    sos.setLevel(minLevel);
                    sos.setMaxLevel(maxLevel);
                    sos.setSOS(true);
                    sos.setSosType(SOSType.GENERIC);
                    area.add(sos);
                }
            }
        }

        // Get the weather SOS encounters for this area
        for (int i = 0; i < 6; i++) {
            int offset = 0x14C + (i * 4);
            int species = readWord(encounterTable, offset) & 0x7FF;
            int forme = readWord(encounterTable, offset) >> 11;
            if (species != 0) {
                Encounter weatherSOS = new Encounter();
                weatherSOS.setPokemon(getPokemonForEncounter(species, forme));
                weatherSOS.setFormeNumber(forme);
                weatherSOS.setLevel(minLevel);
                weatherSOS.setMaxLevel(maxLevel);
                weatherSOS.setSOS(true);
                weatherSOS.setSosType(getSOSTypeForIndex(i));
                area.add(weatherSOS);
            }
        }
        return area;
    }

    private SOSType getSOSTypeForIndex(int index) {
        if (index / 2 == 0) {
            return SOSType.RAIN;
        } else if (index / 2 == 1) {
            return SOSType.HAIL;
        } else {
            return SOSType.SAND;
        }
    }

    private Pokemon getPokemonForEncounter(int species, int forme) {
        Pokemon pokemon = pokes[species];

        // If the forme is purely cosmetic, just use the base forme as the Pokemon
        // for this encounter (the cosmetic forme will be stored in the encounter).
        if (forme <= pokemon.getCosmeticFormCount() || forme == 30 || forme == 31) {
            return pokemon;
        } else {
            int speciesWithForme = absolutePokeNumByBaseForme
                    .getOrDefault(species, dummyAbsolutePokeNums)
                    .getOrDefault(forme, 0);
            return pokes[speciesWithForme];
        }
    }

    @Override
    public void setEncounters(boolean useTimeOfDay, List<EncounterArea> encounterAreas) {
        Iterator<EncounterArea> areaIterator = encounterAreas.iterator();
        for (AreaData areaData : areaDataList) {
            if (!areaData.hasTables) {
                continue;
            }

            for (int i = 0; i < areaData.encounterTables.size(); i++) {
                byte[] encounterTable = areaData.encounterTables.get(i);
                EncounterArea dayArea = areaIterator.next();
                writeEncounterTable(encounterTable, 0, dayArea);
                EncounterArea nightArea = useTimeOfDay ? areaIterator.next() : dayArea;
                writeEncounterTable(encounterTable, 0x164, nightArea);
            }
        }

        try {
            saveAreaData();
            patchMiniorEncounterCode();
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    private void writeEncounterTable(byte[] encounterTable, int offset, List<Encounter> encounters) {
        Iterator<Encounter> encounterIterator = encounters.iterator();
        Encounter firstEncounter = encounters.get(0);
        encounterTable[offset] = (byte) firstEncounter.getLevel();
        encounterTable[offset + 1] = (byte) firstEncounter.getMaxLevel();
        int numberOfEncounterSlots = encounters.size() / 8;
        for (int i = 0; i < numberOfEncounterSlots; i++) {
            int currentOffset = offset + 0xC + (i * 4);
            Encounter enc = encounterIterator.next();
            int speciesAndFormeData = (enc.getFormeNumber() << 11) + enc.getPokemon().getBaseNumber();
            writeWord(encounterTable, currentOffset, speciesAndFormeData);

            // SOS encounters for this encounter
            for (int j = 1; j < 8; j++) {
                Encounter sosEncounter = encounterIterator.next();
                speciesAndFormeData = (sosEncounter.getFormeNumber() << 11) + sosEncounter.getPokemon().getBaseNumber();
                writeWord(encounterTable, currentOffset + (40 * j), speciesAndFormeData);
            }
        }

        // Weather SOS encounters
        if (encounters.size() != numberOfEncounterSlots * 8) {
            for (int i = 0; i < 6; i++) {
                int currentOffset = offset + 0x14C + (i * 4);
                Encounter weatherSOSEncounter = encounterIterator.next();
                int speciesAndFormeData = (weatherSOSEncounter.getFormeNumber() << 11) + weatherSOSEncounter.getPokemon().getBaseNumber();
                writeWord(encounterTable, currentOffset, speciesAndFormeData);
            }
        }
    }

    private List<AreaData> getAreaData() throws IOException {
        GARCArchive worldDataGarc = readGARC(romEntry.getFile("WorldData"), false);
        List<byte[]> worlds = new ArrayList<>();
        for (Map<Integer, byte[]> file : worldDataGarc.files) {
            byte[] world = Mini.UnpackMini(file.get(0), "WD")[0];
            worlds.add(world);
        }
        GARCArchive zoneDataGarc = readGARC(romEntry.getFile("ZoneData"), false);
        byte[] zoneDataBytes = zoneDataGarc.getFile(0);
        byte[] worldData = zoneDataGarc.getFile(1);
        List<String> locationList = createGoodLocationList();
        ZoneData[] zoneData = getZoneData(zoneDataBytes, worldData, locationList, worlds);
        encounterGarc = readGARC(romEntry.getFile("WildPokemon"), Gen7Constants.getRelevantEncounterFiles(romEntry.getRomType()));
        int fileCount = encounterGarc.files.size();
        int numberOfAreas = fileCount / 11;
        AreaData[] areaData = new AreaData[numberOfAreas];
        for (int i = 0; i < numberOfAreas; i++) {
            int areaOffset = i;
            areaData[i] = new AreaData();
            areaData[i].fileNumber = 9 + (11 * i);
            areaData[i].zones = Arrays.stream(zoneData).filter((zone -> zone.areaIndex == areaOffset)).collect(Collectors.toList());
            areaData[i].name = getAreaNameFromZones(areaData[i].zones);
            byte[] encounterData = encounterGarc.getFile(areaData[i].fileNumber);
            if (encounterData.length == 0) {
                areaData[i].hasTables = false;
            } else {
                byte[][] encounterTables = Mini.UnpackMini(encounterData, "EA");
                areaData[i].hasTables = Arrays.stream(encounterTables).anyMatch(t -> t.length > 0);
                if (!areaData[i].hasTables) {
                    continue;
                }

                for (byte[] encounterTable : encounterTables) {
                    byte[] trimmedEncounterTable = new byte[0x2C8];
                    System.arraycopy(encounterTable, 4, trimmedEncounterTable, 0, 0x2C8);
                    areaData[i].encounterTables.add(trimmedEncounterTable);
                }
            }
        }

        return Arrays.asList(areaData);
    }

    private void saveAreaData() throws IOException {
        for (AreaData areaData : areaDataList) {
            if (areaData.hasTables) {
                byte[] encounterData = encounterGarc.getFile(areaData.fileNumber);
                byte[][] encounterTables = Mini.UnpackMini(encounterData, "EA");
                for (int i = 0; i < encounterTables.length; i++) {
                    byte[] originalEncounterTable = encounterTables[i];
                    byte[] newEncounterTable = areaData.encounterTables.get(i);
                    System.arraycopy(newEncounterTable, 0, originalEncounterTable, 4, newEncounterTable.length);
                }
                byte[] newEncounterData = Mini.PackMini(encounterTables, "EA");
                encounterGarc.setFile(areaData.fileNumber, newEncounterData);
            }
        }
    }

    private List<String> createGoodLocationList() {
        List<String> locationList = getStrings(false, romEntry.getIntValue("MapNamesTextOffset"));
        List<String> goodLocationList = new ArrayList<>(locationList);
        for (int i = 0; i < locationList.size(); i += 2) {
            // The location list contains both areas and subareas. If a subarea is associated with an area, it will
            // appear directly after it. This code combines these subarea and area names.
            String subarea = locationList.get(i + 1);
            if (!subarea.isEmpty() && subarea.charAt(0) != '[') {
                String updatedLocation = goodLocationList.get(i) + " (" + subarea + ")";
                goodLocationList.set(i, updatedLocation);
            }

            // Some areas appear in the location list multiple times and don't have any subarea name to distinguish
            // them. This code distinguishes them by appending the number of times they've appeared previously to
            // the area name.
            if (i > 0) {
                List<String> goodLocationUpToCurrent = goodLocationList.stream().limit(i - 1).toList();
                if (!goodLocationList.get(i).isEmpty() && goodLocationUpToCurrent.contains(goodLocationList.get(i))) {
                    int numberOfUsages = Collections.frequency(goodLocationUpToCurrent, goodLocationList.get(i));
                    String updatedLocation = goodLocationList.get(i) + " (" + (numberOfUsages + 1) + ")";
                    goodLocationList.set(i, updatedLocation);
                }
            }
        }
        return goodLocationList;
    }

    private ZoneData[] getZoneData(byte[] zoneDataBytes, byte[] worldData, List<String> locationList, List<byte[]> worlds) {
        ZoneData[] zoneData = new ZoneData[zoneDataBytes.length / ZoneData.size];
        for (int i = 0; i < zoneData.length; i++) {
            zoneData[i] = new ZoneData(zoneDataBytes, i);
            zoneData[i].worldIndex = FileFunctions.read2ByteInt(worldData, i * 0x2);
            zoneData[i].locationName = locationList.get(zoneData[i].parentMap);

            byte[] world = worlds.get(zoneData[i].worldIndex);
            int mappingOffset = FileFunctions.readFullInt(world, 0x8);
            for (int offset = mappingOffset; offset < world.length; offset += 4) {
                int potentialZoneIndex = FileFunctions.read2ByteInt(world, offset);
                if (potentialZoneIndex == i) {
                    zoneData[i].areaIndex = FileFunctions.read2ByteInt(world, offset + 0x2);
                    break;
                }
            }
        }
        return zoneData;
    }

    private String getAreaNameFromZones(List<ZoneData> zoneData) {
        Set<String> uniqueZoneNames = new HashSet<>();
        for (ZoneData zone : zoneData) {
            uniqueZoneNames.add(zone.locationName);
        }
        return String.join(" / ", uniqueZoneNames);
    }

    private void patchMiniorEncounterCode() {
        int offset = find(code, Gen7Constants.miniorWildEncounterPatchPrefix);
        if (offset > 0) {
            offset += Gen7Constants.miniorWildEncounterPatchPrefix.length() / 2;

            // When deciding the *actual* forme for a wild encounter (versus the forme stored
            // in the encounter data), the game has a hardcoded check for Minior's species ID.
            // If the species is Minior, then it branches to code that randomly selects a forme
            // for one of Minior's seven Meteor forms. As a consequence, you can't directly
            // spawn Minior's Core forms; the forme number will just be replaced. The below
            // code nops out the beq instruction so that Minior-C can be spawned directly.
            code[offset] = 0x00;
            code[offset + 1] = 0x00;
            code[offset + 2] = 0x00;
            code[offset + 3] = 0x00;
        }
    }

    @Override
    public List<Trainer> getTrainers() {
        List<Trainer> allTrainers = new ArrayList<>();
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
                byte[] trainer = trainers.files.get(i).get(0);
                byte[] trpoke = trpokes.files.get(i).get(0);
                Trainer tr = new Trainer();
                tr.poketype = trainer[13] & 0xFF;
                tr.index = i;
                tr.trainerclass = trainer[0] & 0xFF;
                int battleType = trainer[2] & 0xFF;
                int numPokes = trainer[3] & 0xFF;
                int trainerAILevel = trainer[12] & 0xFF;
                boolean healer = trainer[15] != 0;
                int pokeOffs = 0;
                String trainerClass = tclasses.get(tr.trainerclass);
                String trainerName = tnamesMap.getOrDefault(i - 1, "UNKNOWN");
                tr.fullDisplayName = trainerClass + " " + trainerName;

                for (int poke = 0; poke < numPokes; poke++) {
                    // Structure is
                    // IV SB LV LV SP SP FRM FRM
                    // (HI HI)
                    // (M1 M1 M2 M2 M3 M3 M4 M4)
                    // where SB = 0 0 Ab Ab 0 0 Fm Ml
                    // Ab Ab = ability number, 0 for random
                    // Fm = 1 for forced female
                    // Ml = 1 for forced male
                    // There's also a trainer flag to force gender, but
                    // this allows fixed teams with mixed genders.

                    // int secondbyte = trpoke[pokeOffs + 1] & 0xFF;
                    int abilityAndFlag = trpoke[pokeOffs];
                    int level = readWord(trpoke, pokeOffs + 14);
                    int species = readWord(trpoke, pokeOffs + 16);
                    int formnum = readWord(trpoke, pokeOffs + 18);
                    TrainerPokemon tpk = new TrainerPokemon();
                    tpk.abilitySlot = (abilityAndFlag >>> 4) & 0xF;
                    tpk.forcedGenderFlag = (abilityAndFlag & 0xF);
                    tpk.nature = trpoke[pokeOffs + 1];
                    tpk.hpEVs = trpoke[pokeOffs + 2];
                    tpk.atkEVs = trpoke[pokeOffs + 3];
                    tpk.defEVs = trpoke[pokeOffs + 4];
                    tpk.spatkEVs = trpoke[pokeOffs + 5];
                    tpk.spdefEVs = trpoke[pokeOffs + 6];
                    tpk.speedEVs = trpoke[pokeOffs + 7];
                    tpk.IVs = FileFunctions.readFullInt(trpoke, pokeOffs + 8);
                    tpk.level = level;
                    if (romEntry.getRomType() == Gen7Constants.Type_USUM) {
                        if (i == 78) {
                            if (poke == 3 && tpk.level == 16 && tr.pokemon.get(0).level == 16) {
                                tpk.level = 14;
                            }
                        }
                    }
                    tpk.pokemon = pokes[species];
                    tpk.forme = formnum;
                    tpk.formeSuffix = Gen7Constants.getFormeSuffixByBaseForme(species,formnum);
                    pokeOffs += 20;
                    tpk.heldItem = readWord(trpoke, pokeOffs);
                    tpk.hasMegaStone = Gen6Constants.isMegaStone(tpk.heldItem);
                    tpk.hasZCrystal = Gen7Constants.isZCrystal(tpk.heldItem);
                    pokeOffs += 4;
                    for (int move = 0; move < 4; move++) {
                        tpk.moves[move] = readWord(trpoke, pokeOffs + (move*2));
                    }
                    pokeOffs += 8;
                    tr.pokemon.add(tpk);
                }
                allTrainers.add(tr);
            }
            if (romEntry.getRomType() == Gen7Constants.Type_SM) {
                Gen7Constants.tagTrainersSM(allTrainers);
                Gen7Constants.setMultiBattleStatusSM(allTrainers);
            } else {
                Gen7Constants.tagTrainersUSUM(allTrainers);
                Gen7Constants.setMultiBattleStatusUSUM(allTrainers);
                Gen7Constants.setForcedRivalStarterPositionsUSUM(allTrainers);
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
    public void setTrainers(List<Trainer> trainerData) {
        Iterator<Trainer> allTrainers = trainerData.iterator();
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
                int offset = 0;
                trainer[13] = (byte) tr.poketype;
                int numPokes = tr.pokemon.size();
                trainer[offset+3] = (byte) numPokes;

                if (tr.forcedDoubleBattle) {
                    if (trainer[offset+2] == 0) {
                        trainer[offset+2] = 1;
                        trainer[offset+12] |= 0x8; // Flag that needs to be set for trainers not to attack their own pokes
                    }
                }

                int bytesNeeded = 32 * numPokes;
                byte[] trpoke = new byte[bytesNeeded];
                int pokeOffs = 0;
                Iterator<TrainerPokemon> tpokes = tr.pokemon.iterator();
                for (int poke = 0; poke < numPokes; poke++) {
                    TrainerPokemon tp = tpokes.next();
                    byte abilityAndFlag = (byte)((tp.abilitySlot << 4) | tp.forcedGenderFlag);
                    trpoke[pokeOffs] = abilityAndFlag;
                    trpoke[pokeOffs + 1] = tp.nature;
                    trpoke[pokeOffs + 2] = tp.hpEVs;
                    trpoke[pokeOffs + 3] = tp.atkEVs;
                    trpoke[pokeOffs + 4] = tp.defEVs;
                    trpoke[pokeOffs + 5] = tp.spatkEVs;
                    trpoke[pokeOffs + 6] = tp.spdefEVs;
                    trpoke[pokeOffs + 7] = tp.speedEVs;
                    FileFunctions.writeFullInt(trpoke, pokeOffs + 8, tp.IVs);
                    writeWord(trpoke, pokeOffs + 14, tp.level);
                    writeWord(trpoke, pokeOffs + 16, tp.pokemon.getNumber());
                    writeWord(trpoke, pokeOffs + 18, tp.forme);
                    pokeOffs += 20;
                    writeWord(trpoke, pokeOffs, tp.heldItem);
                    pokeOffs += 4;
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
                trpokes.setFile(i,trpoke);
            }
            this.writeGARC(romEntry.getFile("TrainerData"), trainers);
            this.writeGARC(romEntry.getFile("TrainerPokemon"), trpokes);

            // In Sun/Moon, Beast Lusamine's Pokemon have aura boosts that are hardcoded.
            if (romEntry.getRomType() == Gen7Constants.Type_SM) {
                Trainer beastLusamine = trainerData.get(Gen7Constants.beastLusamineTrainerIndex);
                setBeastLusaminePokemonBuffs(beastLusamine);
            }
        } catch (IOException ex) {
            throw new RomIOException(ex);
        }
    }

    private void setBeastLusaminePokemonBuffs(Trainer beastLusamine) throws IOException {
        byte[] battleCRO = readFile(romEntry.getFile("Battle"));
        int offset = find(battleCRO, Gen7Constants.beastLusaminePokemonBoostsPrefix);
        if (offset > 0) {
            offset += Gen7Constants.beastLusaminePokemonBoostsPrefix.length() / 2; // because it was a prefix

            // The game only has room for five boost entries, where each boost entry is determined by species ID.
            // However, Beast Lusamine might have duplicates in her party, meaning that two Pokemon can share the
            // same boost entry. First, figure out all the unique Pokemon in her party. We avoid using a Set here
            // in order to preserve the original ordering; we want to make sure to boost the *first* five Pokemon
            List<Pokemon> uniquePokemon = new ArrayList<>();
            for (int i = 0; i < beastLusamine.pokemon.size(); i++) {
                if (!uniquePokemon.contains(beastLusamine.pokemon.get(i).pokemon)) {
                    uniquePokemon.add(beastLusamine.pokemon.get(i).pokemon);
                }
            }
            int numberOfBoostEntries = Math.min(uniquePokemon.size(), 5);
            for (int i = 0; i < numberOfBoostEntries; i++) {
                Pokemon boostedPokemon = uniquePokemon.get(i);
                int auraNumber = getAuraNumberForHighestStat(boostedPokemon);
                int speciesNumber = boostedPokemon.getBaseNumber();
                FileFunctions.write2ByteInt(battleCRO, offset + (i * 0x10), speciesNumber);
                battleCRO[offset + (i * 0x10) + 2] = (byte) auraNumber;
            }
            writeFile(romEntry.getFile("Battle"), battleCRO);
        }
    }

    // Finds the highest stat for the purposes of setting the aura boost on Beast Lusamine's Pokemon.
    // In the case where two or more stats are tied for the highest stat, it randomly selects one.
    private int getAuraNumberForHighestStat(Pokemon boostedPokemon) {

        List<Supplier<Integer>> statSuppliers = List.of(boostedPokemon::getAttack, boostedPokemon::getDefense,
                boostedPokemon::getSpatk, boostedPokemon::getSpdef, boostedPokemon::getSpeed);

        // finds the highest stat(s)
        int currentBestStat = -1;
        List<Integer> possibleAuras = new ArrayList<>();
        for (int auraNum = 1; auraNum <= statSuppliers.size(); auraNum++) {
            Supplier<Integer> statSupplier = statSuppliers.get(auraNum - 1);
            if (statSupplier.get() > currentBestStat) {
                possibleAuras.clear();
                currentBestStat = statSupplier.get();
            }
            if (statSupplier.get() >= currentBestStat) {
                possibleAuras.add(auraNum);
            }
        }

        // Makes a "random" choice between tied highest stats using the Pokemon's number
        // (not actual randomness since that's not permitted in the RomHandler classes).
        // This means the aura chosen will be the same for the same species being boosted
        // the same way, but that's 100% fine.
        return possibleAuras.get(boostedPokemon.getNumber() % possibleAuras.size());
    }

    @Override
    public List<Integer> getEvolutionItems() {
        return Gen7Constants.evolutionItems;
    }

    @Override
    public Map<Integer, List<MoveLearnt>> getMovesLearnt() {
        Map<Integer, List<MoveLearnt>> movesets = new TreeMap<>();
        try {
            GARCArchive movesLearnt = this.readGARC(romEntry.getFile("PokemonMovesets"),true);
            int formeCount = Gen7Constants.getFormeCount(romEntry.getRomType());
            for (int i = 1; i <= Gen7Constants.getPokemonCount(romEntry.getRomType()) + formeCount; i++) {
                Pokemon pkmn = pokes[i];
                byte[] movedata;
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
            int formeCount = Gen7Constants.getFormeCount(romEntry.getRomType());
            for (int i = 1; i <= Gen7Constants.getPokemonCount(romEntry.getRomType()) + formeCount; i++) {
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
            TreeMap<Pokemon, Integer> altFormeEggMoveFiles = new TreeMap<>();
            for (int i = 1; i <= Gen7Constants.getPokemonCount(romEntry.getRomType()); i++) {
                Pokemon pkmn = pokes[i];
                byte[] movedata = eggMovesGarc.files.get(i).get(0);
                int formeReference = readWord(movedata, 0);
                if (formeReference != pkmn.getNumber()) {
                    altFormeEggMoveFiles.put(pkmn, formeReference);
                }
                int numberOfEggMoves = readWord(movedata, 2);
                List<Integer> moves = new ArrayList<>();
                for (int j = 0; j < numberOfEggMoves; j++) {
                    int move = readWord(movedata, 4 + (j * 2));
                    moves.add(move);
                }
                eggMoves.put(pkmn.getNumber(), moves);
            }
            Iterator<Pokemon> iter = altFormeEggMoveFiles.keySet().iterator();
            while (iter.hasNext()) {
                Pokemon originalForme = iter.next();
                int formeNumber = 1;
                int fileNumber = altFormeEggMoveFiles.get(originalForme);
                Pokemon altForme = getAltFormeOfPokemon(originalForme, formeNumber);
                while (!originalForme.equals(altForme)) {
                    byte[] movedata = eggMovesGarc.files.get(fileNumber).get(0);
                    int numberOfEggMoves = readWord(movedata, 2);
                    List<Integer> moves = new ArrayList<>();
                    for (int j = 0; j < numberOfEggMoves; j++) {
                        int move = readWord(movedata, 4 + (j * 2));
                        moves.add(move);
                    }
                    eggMoves.put(altForme.getNumber(), moves);
                    formeNumber++;
                    fileNumber++;
                    altForme = getAltFormeOfPokemon(originalForme, formeNumber);
                }
                iter.remove();
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
            TreeMap<Pokemon, Integer> altFormeEggMoveFiles = new TreeMap<>();
            for (int i = 1; i <= Gen7Constants.getPokemonCount(romEntry.getRomType()); i++) {
                Pokemon pkmn = pokes[i];
                byte[] movedata = eggMovesGarc.files.get(i).get(0);
                int formeReference = readWord(movedata, 0);
                if (formeReference != pkmn.getNumber()) {
                    altFormeEggMoveFiles.put(pkmn, formeReference);
                }
                List<Integer> moves = eggMoves.get(pkmn.getNumber());
                for (int j = 0; j < moves.size(); j++) {
                    writeWord(movedata, 4 + (j * 2), moves.get(j));
                }
            }
            Iterator<Pokemon> iter = altFormeEggMoveFiles.keySet().iterator();
            while (iter.hasNext()) {
                Pokemon originalForme = iter.next();
                int formeNumber = 1;
                int fileNumber = altFormeEggMoveFiles.get(originalForme);
                Pokemon altForme = getAltFormeOfPokemon(originalForme, formeNumber);
                while (!originalForme.equals(altForme)) {
                    byte[] movedata = eggMovesGarc.files.get(fileNumber).get(0);
                    List<Integer> moves = eggMoves.get(altForme.getNumber());
                    for (int j = 0; j < moves.size(); j++) {
                        writeWord(movedata, 4 + (j * 2), moves.get(j));
                    }
                    formeNumber++;
                    fileNumber++;
                    altForme = getAltFormeOfPokemon(originalForme, formeNumber);
                }
                iter.remove();
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
    public boolean hasTotemPokemon() {
        return true;
    }

    @Override
    public List<TotemPokemon> getTotemPokemon() {
        List<TotemPokemon> totems = new ArrayList<>();
        try {
            GARCArchive staticGarc = readGARC(romEntry.getFile("StaticPokemon"), true);
            List<Integer> totemIndices = Arrays.stream(romEntry.getArrayValue("TotemPokemonIndices")).boxed().toList();

            // Static encounters
            byte[] staticEncountersFile = staticGarc.files.get(1).get(0);
            for (int i: totemIndices) {
                int offset = i * 0x38;
                TotemPokemon totem = new TotemPokemon();
                int species = FileFunctions.read2ByteInt(staticEncountersFile, offset);
                Pokemon pokemon = pokes[species];
                int forme = staticEncountersFile[offset + 2];
                if (forme > pokemon.getCosmeticFormCount() && forme != 30 && forme != 31) {
                    int speciesWithForme = absolutePokeNumByBaseForme
                            .getOrDefault(species, dummyAbsolutePokeNums)
                            .getOrDefault(forme, 0);
                    pokemon = pokes[speciesWithForme];
                }
                totem.pkmn = pokemon;
                totem.forme = forme;
                totem.level = staticEncountersFile[offset + 3];
                int heldItem = FileFunctions.read2ByteInt(staticEncountersFile, offset + 4);
                if (heldItem == 0xFFFF) {
                    heldItem = 0;
                }
                totem.heldItem = heldItem;
                totem.aura = new Aura(staticEncountersFile[offset + 0x25]);
                int allies = staticEncountersFile[offset + 0x27];
                for (int j = 0; j < allies; j++) {
                    int allyIndex = (staticEncountersFile[offset + 0x28 + 4*j] - 1) & 0xFF;
                    totem.allies.put(allyIndex,readStaticEncounter(staticEncountersFile, allyIndex * 0x38));
                }
                totems.add(totem);
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }
        return totems;
    }

    @Override
    public void setTotemPokemon(List<TotemPokemon> totemPokemon) {
        try {
            GARCArchive staticGarc = readGARC(romEntry.getFile("StaticPokemon"), true);
            List<Integer> totemIndices = Arrays.stream(romEntry.getArrayValue("TotemPokemonIndices")).boxed().toList();
            Iterator<TotemPokemon> totemIter = totemPokemon.iterator();

            // Static encounters
            byte[] staticEncountersFile = staticGarc.files.get(1).get(0);
            for (int i: totemIndices) {
                int offset = i * 0x38;
                TotemPokemon totem = totemIter.next();
                if (totem.pkmn.isAltForme()) {
                    totem.forme = totem.pkmn.getFormeNumber();
                    totem.pkmn = totem.pkmn.getBaseForme();
                }
                writeWord(staticEncountersFile, offset, totem.pkmn.getNumber());
                staticEncountersFile[offset + 2] = (byte) totem.forme;
                staticEncountersFile[offset + 3] = (byte) totem.level;
                if (totem.heldItem == 0) {
                    writeWord(staticEncountersFile, offset + 4, -1);
                } else {
                    writeWord(staticEncountersFile, offset + 4, totem.heldItem);
                }
                if (totem.resetMoves) {
                    writeWord(staticEncountersFile, offset + 12, 0);
                    writeWord(staticEncountersFile, offset + 14, 0);
                    writeWord(staticEncountersFile, offset + 16, 0);
                    writeWord(staticEncountersFile, offset + 18, 0);
                }
                staticEncountersFile[offset + 0x25] = totem.aura.toByte();
                for (Integer allyIndex: totem.allies.keySet()) {
                    offset = allyIndex * 0x38;
                    StaticEncounter ally = totem.allies.get(allyIndex);
                    if (ally.pkmn.isAltForme()) {
                        ally.forme = ally.pkmn.getFormeNumber();
                        ally.pkmn = ally.pkmn.getBaseForme();
                    }
                    writeWord(staticEncountersFile, offset, ally.pkmn.getNumber());
                    staticEncountersFile[offset + 2] = (byte) ally.forme;
                    staticEncountersFile[offset + 3] = (byte) ally.level;
                    if (ally.heldItem == 0) {
                        writeWord(staticEncountersFile, offset + 4, -1);
                    } else {
                        writeWord(staticEncountersFile, offset + 4, ally.heldItem);
                    }
                    if (ally.resetMoves) {
                        writeWord(staticEncountersFile, offset + 12, 0);
                        writeWord(staticEncountersFile, offset + 14, 0);
                        writeWord(staticEncountersFile, offset + 16, 0);
                        writeWord(staticEncountersFile, offset + 18, 0);
                    }
                }
            }

            writeGARC(romEntry.getFile("StaticPokemon"), staticGarc);
        } catch (IOException e) {
            throw new RomIOException(e);
        }

    }

    @Override
    public List<StaticEncounter> getStaticPokemon() {
        List<StaticEncounter> statics = new ArrayList<>();
        try {
            GARCArchive staticGarc = readGARC(romEntry.getFile("StaticPokemon"), true);
            List<Integer> skipIndices = new ArrayList<>( // Arrays.stream.toList() is immutable so we have to wrap it.
                    Arrays.stream(romEntry.getArrayValue("TotemPokemonIndices")).boxed().toList());
            skipIndices.addAll(Arrays.stream(romEntry.getArrayValue("AllyPokemonIndices")).boxed().toList());

            // Gifts, start at 3 to skip the starters
            byte[] giftsFile = staticGarc.files.get(0).get(0);
            int numberOfGifts = giftsFile.length / 0x14;
            for (int i = 3; i < numberOfGifts; i++) {
                int offset = i * 0x14;
                StaticEncounter se = new StaticEncounter();
                int species = FileFunctions.read2ByteInt(giftsFile, offset);
                Pokemon pokemon = pokes[species];
                int forme = giftsFile[offset + 2];
                if (forme > pokemon.getCosmeticFormCount() && forme != 30 && forme != 31) {
                    int speciesWithForme = absolutePokeNumByBaseForme
                            .getOrDefault(species, dummyAbsolutePokeNums)
                            .getOrDefault(forme, 0);
                    pokemon = pokes[speciesWithForme];
                }
                se.pkmn = pokemon;
                se.forme = forme;
                se.level = giftsFile[offset + 3];
                se.heldItem = FileFunctions.read2ByteInt(giftsFile, offset + 8);
                se.isEgg = giftsFile[offset + 10] == 1;
                statics.add(se);
            }

            // Static encounters
            byte[] staticEncountersFile = staticGarc.files.get(1).get(0);
            int numberOfStaticEncounters = staticEncountersFile.length / 0x38;
            for (int i = 0; i < numberOfStaticEncounters; i++) {
                if (skipIndices.contains(i)) continue;
                int offset = i * 0x38;
                StaticEncounter se = readStaticEncounter(staticEncountersFile, offset);
                statics.add(se);
            }

            // Zygarde created via Assembly on Route 16 is hardcoded
            readAssemblyZygarde(statics);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
        consolidateLinkedEncounters(statics);
        return statics;
    }

    private StaticEncounter readStaticEncounter(byte[] staticEncountersFile, int offset) {
        StaticEncounter se = new StaticEncounter();
        int species = FileFunctions.read2ByteInt(staticEncountersFile, offset);
        Pokemon pokemon = pokes[species];
        int forme = staticEncountersFile[offset + 2];
        if (forme > pokemon.getCosmeticFormCount() && forme != 30 && forme != 31) {
            int speciesWithForme = absolutePokeNumByBaseForme
                    .getOrDefault(species, dummyAbsolutePokeNums)
                    .getOrDefault(forme, 0);
            pokemon = pokes[speciesWithForme];
        }
        se.pkmn = pokemon;
        se.forme = forme;
        se.level = staticEncountersFile[offset + 3];
        int heldItem = FileFunctions.read2ByteInt(staticEncountersFile, offset + 4);
        if (heldItem == 0xFFFF) {
            heldItem = 0;
        }
        se.heldItem = heldItem;
        return se;
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

    private void readAssemblyZygarde(List<StaticEncounter> statics) throws IOException {
        GARCArchive scriptGarc = readGARC(romEntry.getFile("Scripts"), true);
        int[] scriptLevelOffsets = romEntry.getArrayValue("ZygardeScriptLevelOffsets");
        int[] levels = new int[scriptLevelOffsets.length];
        byte[] zygardeAssemblyScriptBytes = scriptGarc.getFile(Gen7Constants.zygardeAssemblyScriptFile);
        AMX zygardeAssemblyScript = new AMX(zygardeAssemblyScriptBytes);
        for (int i = 0; i < scriptLevelOffsets.length; i++) {
            levels[i] = zygardeAssemblyScript.decData[scriptLevelOffsets[i]];
        }

        int speciesOffset = find(code, Gen7Constants.zygardeAssemblySpeciesPrefix);
        int formeOffset = find(code, Gen7Constants.zygardeAssemblyFormePrefix);
        if (speciesOffset > 0 && formeOffset > 0) {
            speciesOffset += Gen7Constants.zygardeAssemblySpeciesPrefix.length() / 2; // because it was a prefix
            formeOffset += Gen7Constants.zygardeAssemblyFormePrefix.length() / 2; // because it was a prefix
            int species = FileFunctions.read2ByteInt(code, speciesOffset);

            // The original code for this passed in the forme via a parameter, stored that onto
            // the stack, then did a ldr to put that stack variable into r0 before finally
            // storing that value in the right place. If we already modified this code, then we
            // don't care about all of this; we just wrote a "mov r0, #forme" over the ldr instead.
            // Thus, if the original ldr instruction is still there, assume we haven't touched it.
            int forme;
            if (FileFunctions.readFullInt(code, formeOffset) == 0xE59D0040) {
                // Since we haven't modified the code yet, this is Zygarde. For SM, use 10%,
                // since you can get it fairly early. For USUM, use 50%, since it's only
                // obtainable in the postgame.
                forme = romEntry.getRomType() == Gen7Constants.Type_SM ? 1 : 0;
            } else {
                // We have modified the code, so just read the constant forme number we wrote.
                forme = code[formeOffset];
            }

            StaticEncounter lowLevelAssembly = new StaticEncounter();
            Pokemon pokemon = pokes[species];
            if (forme > pokemon.getCosmeticFormCount() && forme != 30 && forme != 31) {
                int speciesWithForme = absolutePokeNumByBaseForme
                        .getOrDefault(species, dummyAbsolutePokeNums)
                        .getOrDefault(forme, 0);
                pokemon = pokes[speciesWithForme];
            }
            lowLevelAssembly.pkmn = pokemon;
            lowLevelAssembly.forme = forme;
            lowLevelAssembly.level = levels[0];
            for (int i = 1; i < levels.length; i++) {
                StaticEncounter higherLevelAssembly = new StaticEncounter();
                higherLevelAssembly.pkmn = pokemon;
                higherLevelAssembly.forme = forme;
                higherLevelAssembly.level = levels[i];
                lowLevelAssembly.linkedEncounters.add(higherLevelAssembly);
            }

            statics.add(lowLevelAssembly);
        }
    }

    @Override
    public boolean setStaticPokemon(List<StaticEncounter> staticPokemon) {
        try {
            unlinkStaticEncounters(staticPokemon);
            GARCArchive staticGarc = readGARC(romEntry.getFile("StaticPokemon"), true);
            List<Integer> skipIndices = new ArrayList<>(
                    Arrays.stream(romEntry.getArrayValue("TotemPokemonIndices")).boxed().toList());
            skipIndices.addAll(Arrays.stream(romEntry.getArrayValue("AllyPokemonIndices")).boxed().toList());
            Iterator<StaticEncounter> staticIter = staticPokemon.iterator();

            // Gifts, start at 3 to skip the starters
            byte[] giftsFile = staticGarc.files.get(0).get(0);
            int numberOfGifts = giftsFile.length / 0x14;
            for (int i = 3; i < numberOfGifts; i++) {
                int offset = i * 0x14;
                StaticEncounter se = staticIter.next();
                writeWord(giftsFile, offset, se.pkmn.getBaseNumber());
                giftsFile[offset + 2] = (byte) se.forme;
                giftsFile[offset + 3] = (byte) se.level;
                writeWord(giftsFile, offset + 8, se.heldItem);
            }

            // Static encounters
            byte[] staticEncountersFile = staticGarc.files.get(1).get(0);
            int numberOfStaticEncounters = staticEncountersFile.length / 0x38;
            for (int i = 0; i < numberOfStaticEncounters; i++) {
                if (skipIndices.contains(i)) continue;
                int offset = i * 0x38;
                StaticEncounter se = staticIter.next();
                writeWord(staticEncountersFile, offset, se.pkmn.getBaseNumber());
                staticEncountersFile[offset + 2] = (byte) se.forme;
                staticEncountersFile[offset + 3] = (byte) se.level;
                if (se.heldItem == 0) {
                    writeWord(staticEncountersFile, offset + 4, -1);
                } else {
                    writeWord(staticEncountersFile, offset + 4, se.heldItem);
                }
                if (se.resetMoves) {
                    writeWord(staticEncountersFile, offset + 12, 0);
                    writeWord(staticEncountersFile, offset + 14, 0);
                    writeWord(staticEncountersFile, offset + 16, 0);
                    writeWord(staticEncountersFile, offset + 18, 0);
                }
            }

            // Zygarde created via Assembly on Route 16 is hardcoded
            writeAssemblyZygarde(staticIter.next());

            writeGARC(romEntry.getFile("StaticPokemon"), staticGarc);
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

    private void writeAssemblyZygarde(StaticEncounter se) throws IOException {
        int[] levels = new int[se.linkedEncounters.size() + 1];
        levels[0] = se.level;
        for (int i = 0; i < se.linkedEncounters.size(); i++) {
            levels[i + 1] = se.linkedEncounters.get(i).level;
        }

        GARCArchive scriptGarc = readGARC(romEntry.getFile("Scripts"), true);
        int[] scriptLevelOffsets = romEntry.getArrayValue("ZygardeScriptLevelOffsets");
        byte[] zygardeAssemblyScriptBytes = scriptGarc.getFile(Gen7Constants.zygardeAssemblyScriptFile);
        AMX zygardeAssemblyScript = new AMX(zygardeAssemblyScriptBytes);
        for (int i = 0; i < scriptLevelOffsets.length; i++) {
            zygardeAssemblyScript.decData[scriptLevelOffsets[i]] = (byte) levels[i];
        }
        scriptGarc.setFile(Gen7Constants.zygardeAssemblyScriptFile, zygardeAssemblyScript.getBytes());
        writeGARC(romEntry.getFile("Scripts"), scriptGarc);

        int speciesOffset = find(code, Gen7Constants.zygardeAssemblySpeciesPrefix);
        int formeOffset = find(code, Gen7Constants.zygardeAssemblyFormePrefix);
        if (speciesOffset > 0 && formeOffset > 0) {
            speciesOffset += Gen7Constants.zygardeAssemblySpeciesPrefix.length() / 2; // because it was a prefix
            formeOffset += Gen7Constants.zygardeAssemblyFormePrefix.length() / 2; // because it was a prefix
            FileFunctions.write2ByteInt(code, speciesOffset, se.pkmn.getBaseNumber());

            // Just write "mov r0, #forme" to where the game originally loaded the forme.
            code[formeOffset] = (byte) se.forme;
            code[formeOffset + 1] = 0x00;
            code[formeOffset + 2] = (byte) 0xA0;
            code[formeOffset + 3] = (byte) 0xE3;
        }
    }

    @Override
    public int miscTweaksAvailable() {
        int available = 0;
        available |= MiscTweak.FASTEST_TEXT.getValue();
        available |= MiscTweak.BAN_LUCKY_EGG.getValue();
        available |= MiscTweak.SOS_BATTLES_FOR_ALL.getValue();
        available |= MiscTweak.RETAIN_ALT_FORMES.getValue();
        return available;
    }

    @Override
    public void applyMiscTweak(MiscTweak tweak) {
        if (tweak == MiscTweak.FASTEST_TEXT) {
            applyFastestText();
        } else if (tweak == MiscTweak.BAN_LUCKY_EGG) {
            allowedItems.banSingles(Items.luckyEgg);
            nonBadItems.banSingles(Items.luckyEgg);
        } else if (tweak == MiscTweak.SOS_BATTLES_FOR_ALL) {
            positiveCallRates();
        } else if (tweak == MiscTweak.RETAIN_ALT_FORMES) {
            try {
                patchFormeReversion();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void applyFastestText() {
        int offset = find(code, Gen7Constants.fastestTextPrefixes[0]);
        if (offset > 0) {
            offset += Gen7Constants.fastestTextPrefixes[0].length() / 2; // because it was a prefix
            code[offset] = 0x03;
            code[offset + 1] = 0x40;
            code[offset + 2] = (byte) 0xA0;
            code[offset + 3] = (byte) 0xE3;
        }
        offset = find(code, Gen7Constants.fastestTextPrefixes[1]);
        if (offset > 0) {
            offset += Gen7Constants.fastestTextPrefixes[1].length() / 2; // because it was a prefix
            code[offset] = 0x03;
            code[offset + 1] = 0x50;
            code[offset + 2] = (byte) 0xA0;
            code[offset + 3] = (byte) 0xE3;
        }
    }

    private void positiveCallRates() {
        for (Pokemon pk: pokes) {
            if (pk == null) continue;
            if (pk.getCallRate() <= 0) {
                pk.setCallRate(5);
            }
        }
    }

    public void enableGuaranteedPokemonCatching() {
        try {
            byte[] battleCRO = readFile(romEntry.getFile("Battle"));
            int offset = find(battleCRO, Gen7Constants.perfectOddsBranchLocator);
            if (offset > 0) {
                // The game checks to see if your odds are greater then or equal to 255 using the following
                // code. Note that they compare to 0xFF000 instead of 0xFF; it looks like all catching code
                // probabilities are shifted like this?
                // cmp r7, #0xFF000
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
        String tmDataPrefix = Gen7Constants.getTmDataPrefix(romEntry.getRomType());
        int offset = find(code, tmDataPrefix);
        if (offset != 0) {
            offset += tmDataPrefix.length() / 2; // because it was a prefix
            List<Integer> tms = new ArrayList<>();
            for (int i = 0; i < Gen7Constants.tmCount; i++) {
                tms.add(readWord(code, offset + i * 2));
            }
            return tms;
        } else {
            return null;
        }
    }

    @Override
    public List<Integer> getHMMoves() {
        // Gen 7 does not have any HMs
        return new ArrayList<>();
    }

    @Override
    public void setTMMoves(List<Integer> moveIndexes) {
        String tmDataPrefix = Gen7Constants.getTmDataPrefix(romEntry.getRomType());
        int offset = find(code, tmDataPrefix);
        if (offset > 0) {
            offset += tmDataPrefix.length() / 2; // because it was a prefix
            for (int i = 0; i < Gen7Constants.tmCount; i++) {
                writeWord(code, offset + i * 2, moveIndexes.get(i));
            }

            // Update TM item descriptions
            List<String> itemDescriptions = getStrings(false, romEntry.getIntValue("ItemDescriptionsTextOffset"));
            List<String> moveDescriptions = getStrings(false, romEntry.getIntValue("MoveDescriptionsTextOffset"));
            // TM01 is item 328 and so on
            for (int i = 0; i < Gen7Constants.tmBlockOneCount; i++) {
                itemDescriptions.set(i + Gen7Constants.tmBlockOneOffset, moveDescriptions.get(moveIndexes.get(i)));
            }
            // TM93-95 are 618-620
            for (int i = 0; i < Gen7Constants.tmBlockTwoCount; i++) {
                itemDescriptions.set(i + Gen7Constants.tmBlockTwoOffset,
                        moveDescriptions.get(moveIndexes.get(i + Gen7Constants.tmBlockOneCount)));
            }
            // TM96-100 are 690 and so on
            for (int i = 0; i < Gen7Constants.tmBlockThreeCount; i++) {
                itemDescriptions.set(i + Gen7Constants.tmBlockThreeOffset,
                        moveDescriptions.get(moveIndexes.get(i + Gen7Constants.tmBlockOneCount + Gen7Constants.tmBlockTwoCount)));
            }
            // Save the new item descriptions
            setStrings(false, romEntry.getIntValue("ItemDescriptionsTextOffset"), itemDescriptions);
            // Palettes
            String palettePrefix = Gen7Constants.itemPalettesPrefix;
            int offsPals = find(code, palettePrefix);
            if (offsPals > 0) {
                offsPals += Gen7Constants.itemPalettesPrefix.length() / 2; // because it was a prefix
                // Write pals
                for (int i = 0; i < Gen7Constants.tmBlockOneCount; i++) {
                    int itmNum = Gen7Constants.tmBlockOneOffset + i;
                    Move m = this.moves[moveIndexes.get(i)];
                    int pal = this.typeTMPaletteNumber(m.type, true);
                    writeWord(code, offsPals + itmNum * 4, pal);
                }
                for (int i = 0; i < (Gen7Constants.tmBlockTwoCount); i++) {
                    int itmNum = Gen7Constants.tmBlockTwoOffset + i;
                    Move m = this.moves[moveIndexes.get(i + Gen7Constants.tmBlockOneCount)];
                    int pal = this.typeTMPaletteNumber(m.type, true);
                    writeWord(code, offsPals + itmNum * 4, pal);
                }
                for (int i = 0; i < (Gen7Constants.tmBlockThreeCount); i++) {
                    int itmNum = Gen7Constants.tmBlockThreeOffset + i;
                    Move m = this.moves[moveIndexes.get(i + Gen7Constants.tmBlockOneCount + Gen7Constants.tmBlockTwoCount)];
                    int pal = this.typeTMPaletteNumber(m.type, true);
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
        return Gen7Constants.tmCount;
    }

    @Override
    public int getHMCount() {
        // Gen 7 does not have any HMs
        return 0;
    }

    @Override
    public Map<Pokemon, boolean[]> getTMHMCompatibility() {
        Map<Pokemon, boolean[]> compat = new TreeMap<>();
        int pokemonCount = Gen7Constants.getPokemonCount(romEntry.getRomType());
        int formeCount = Gen7Constants.getFormeCount(romEntry.getRomType());
        for (int i = 1; i <= pokemonCount + formeCount; i++) {
            byte[] data;
            data = pokeGarc.files.get(i).get(0);
            Pokemon pkmn = pokes[i];
            boolean[] flags = new boolean[Gen7Constants.tmCount + 1];
            for (int j = 0; j < 13; j++) {
                readByteIntoFlags(data, flags, j * 8 + 1, Gen7Constants.bsTMHMCompatOffset + j);
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
            for (int j = 0; j < 13; j++) {
                data[Gen7Constants.bsTMHMCompatOffset + j] = getByteFromFlags(flags, j * 8 + 1);
            }
        }
    }

    @Override
    public boolean hasMoveTutors() {
        return romEntry.getRomType() == Gen7Constants.Type_USUM;
    }

    @Override
    public List<Integer> getMoveTutorMoves() {
        List<Integer> mtMoves = new ArrayList<>();

        int mtOffset = find(code, Gen7Constants.tutorsPrefix);
        if (mtOffset > 0) {
            mtOffset += Gen7Constants.tutorsPrefix.length() / 2;
            int val = 0;
            while (val != 0xFFFF) {
                val = FileFunctions.read2ByteInt(code, mtOffset);
                mtOffset += 2;
                if (val == 0xFFFF) continue;
                mtMoves.add(val);
            }
        }

        return mtMoves;
    }

    @Override
    public void setMoveTutorMoves(List<Integer> moves) {
        int mtOffset = find(code, Gen7Constants.tutorsPrefix);
        if (mtOffset > 0) {
            mtOffset += Gen7Constants.tutorsPrefix.length() / 2;
            for (int move: moves) {
                FileFunctions.write2ByteInt(code,mtOffset, move);
                mtOffset += 2;
            }
        }

        try {
            byte[] tutorCRO = readFile(romEntry.getFile("ShopsAndTutors"));
            for (int i = 0; i < moves.size(); i++) {
                int offset = Gen7Constants.tutorsOffset + i * 4;
                FileFunctions.write2ByteInt(tutorCRO, offset, moves.get(i));
            }
            writeFile(romEntry.getFile("ShopsAndTutors"), tutorCRO);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    public Map<Pokemon, boolean[]> getMoveTutorCompatibility() {
        Map<Pokemon, boolean[]> compat = new TreeMap<>();
        int pokemonCount = Gen7Constants.getPokemonCount(romEntry.getRomType());
        int formeCount = Gen7Constants.getFormeCount(romEntry.getRomType());
        for (int i = 1; i <= pokemonCount + formeCount; i++) {
            byte[] data;
            data = pokeGarc.files.get(i).get(0);
            Pokemon pkmn = pokes[i];
            boolean[] flags = new boolean[Gen7Constants.tutorMoveCount + 1];
            for (int j = 0; j < 10; j++) {
                readByteIntoFlags(data, flags, j * 8 + 1, Gen7Constants.bsMTCompatOffset + j);
            }
            compat.put(pkmn, flags);
        }
        return compat;
    }

    @Override
    public void setMoveTutorCompatibility(Map<Pokemon, boolean[]> compatData) {
        if (!hasMoveTutors()) return;
        int pokemonCount = Gen7Constants.getPokemonCount(romEntry.getRomType());
        int formeCount = Gen7Constants.getFormeCount(romEntry.getRomType());
        for (int i = 1; i <= pokemonCount + formeCount; i++) {
            byte[] data;
            data = pokeGarc.files.get(i).get(0);
            Pokemon pkmn = pokes[i];
            boolean[] flags = compatData.get(pkmn);
            for (int j = 0; j < 10; j++) {
                data[Gen7Constants.bsMTCompatOffset + j] = getByteFromFlags(flags, j * 8 + 1);
            }
        }
    }

    @Override
    public boolean hasTimeBasedEncounters() {
        return true;
    }

    @Override
    public boolean isUSUM() {
        return romEntry.getRomType() == Gen7Constants.Type_USUM;
    }

    @Override
    public List<Integer> getMovesBannedFromLevelup() {
        return Gen7Constants.bannedMoves;
    }

    @Override
    public boolean hasWildAltFormes() {
        return true;
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
                            extraEntry.setForme(evo.getForme());
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
        int offset = find(code, Gen7Constants.friendshipValueForEvoLocator);
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

        for (Pokemon pkmn : pokes) {
            if (pkmn != null) {
                Evolution extraEntry = null;
                for (Evolution evo : pkmn.getEvolutionsFrom()) {
                    if (wildsRandomized) {
                        if (evo.getType() == EvolutionType.LEVEL_WITH_OTHER) {
                            // Replace w/ level 35
                            evo.setType(EvolutionType.LEVEL);
                            evo.setExtraInfo(35);
                            addEvoUpdateCondensed(easierEvolutionUpdates, evo, false);
                        }
                    }
                    if (romEntry.getRomType() == Gen7Constants.Type_SM) {
                        if (evo.getType() == EvolutionType.LEVEL_SNOWY) {
                            extraEntry = new Evolution(evo.getFrom(), evo.getTo(), true,
                                    EvolutionType.LEVEL, 35);
                            extraEntry.setForme(evo.getForme());
                            addEvoUpdateCondensed(easierEvolutionUpdates, extraEntry, true);
                        } else if (evo.getType() == EvolutionType.LEVEL_ELECTRIFIED_AREA) {
                            extraEntry = new Evolution(evo.getFrom(), evo.getTo(), true,
                                    EvolutionType.LEVEL, 35);
                            extraEntry.setForme(evo.getForme());
                            addEvoUpdateCondensed(easierEvolutionUpdates, extraEntry, true);
                        }
                    }
                }
                if (extraEntry != null) {
                    pkmn.getEvolutionsFrom().add(extraEntry);
                    extraEntry.getTo().getEvolutionsTo().add(extraEntry);
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
                            extraEntry.setForme(evo.getForme());
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
                            extraEntry.setForme(evo.getForme());
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
                            extraEntry.setForme(evo.getForme());
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
                            extraEntry.setForme(evo.getForme());
                            extraEvolutions.add(extraEntry);
                        }
                    } else if (evo.getType() == EvolutionType.LEVEL_DAY) {
                        if (evo.getFrom().getNumber() == Species.rockruff) {
                            // We can't set Rockruff to evolve into Lycanroc-Midday with level at night because that's how
                            // Lycanroc-Midnight works in the original game. Instead, make Rockruff: == sun stone => Lycanroc-Midday
                            evo.setType(EvolutionType.STONE);
                            evo.setExtraInfo(Items.sunStone);
                            addEvoUpdateStone(timeBasedEvolutionUpdates, evo, itemNames.get(evo.getExtraInfo()));
                        } else {
                            addEvoUpdateLevel(timeBasedEvolutionUpdates, evo);
                            evo.setType(EvolutionType.LEVEL);
                        }
                    } else if (evo.getType() == EvolutionType.LEVEL_NIGHT) {
                        if (evo.getFrom().getNumber() == Species.rockruff) {
                            // We can't set Rockruff to evolve into Lycanroc-Midnight with level at night because that's how
                            // Lycanroc-Midday works in the original game. Instead, make Rockruff: == moon stone => Lycanroc-Midnight
                            evo.setType(EvolutionType.STONE);
                            evo.setExtraInfo(Items.moonStone);
                            addEvoUpdateStone(timeBasedEvolutionUpdates, evo, itemNames.get(evo.getExtraInfo()));
                        } else {
                            addEvoUpdateLevel(timeBasedEvolutionUpdates, evo);
                            evo.setType(EvolutionType.LEVEL);
                        }
                    } else if (evo.getType() == EvolutionType.LEVEL_DUSK) {
                        // This is the Rockruff => Lycanroc-Dusk evolution. We can't set it to evolve with level at other
                        // times because the other Lycanroc formes work like that in the original game. Instead, make
                        // Rockruff: == dusk stone => Lycanroc-Dusk
                        evo.setType(EvolutionType.STONE);
                        evo.setExtraInfo(Items.duskStone);
                        addEvoUpdateStone(timeBasedEvolutionUpdates, evo, itemNames.get(evo.getExtraInfo()));
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
    public boolean altFormesCanHaveDifferentEvolutions() {
        return true;
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
        List<String> nonEnglishLanguages = Arrays.asList("JaKana", "JaKanji", "Fr", "It", "De", "Es", "Ko", "ZhSimplified", "ZhTraditional");
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
        return 15;
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
        return Gen7Constants.getHighestAbilityIndex(romEntry.getRomType());
    }

    @Override
    public int internalStringLength(String string) {
        return string.length();
    }

    @Override
    public boolean setIntroPokemon(Pokemon pk) {
        // For now, do nothing.
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
        return new ArrayList<>();
    }

    @Override
    public List<Integer> getRegularShopItems() {
        return Gen7Constants.getRegularShopItems(romEntry.getRomType());
    }

    @Override
    public List<Integer> getOPShopItems() {
        return Gen7Constants.opShopItems;
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
        return Gen7Constants.abilityVariations;
    }

    @Override
    public List<Integer> getUselessAbilities() {
        return new ArrayList<>(Gen7Constants.uselessAbilities);
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

        if (index >= Gen7Constants.tmBlockOneOffset
                && index < Gen7Constants.tmBlockOneOffset + Gen7Constants.tmBlockOneCount) {
            return index - (Gen7Constants.tmBlockOneOffset - 1);
        } else if (index >= Gen7Constants.tmBlockTwoOffset
                && index < Gen7Constants.tmBlockTwoOffset + Gen7Constants.tmBlockTwoCount) {
            return (index + Gen7Constants.tmBlockOneCount) - (Gen7Constants.tmBlockTwoOffset - 1);
        } else {
            return (index + Gen7Constants.tmBlockOneCount + Gen7Constants.tmBlockTwoCount) - (Gen7Constants.tmBlockThreeOffset - 1);
        }
    }

    private int indexFromTM(int tm) {
        if (tm >= 1 && tm <= Gen7Constants.tmBlockOneCount) {
            return tm + (Gen7Constants.tmBlockOneOffset - 1);
        } else if (tm > Gen7Constants.tmBlockOneCount && tm <= Gen7Constants.tmBlockOneCount + Gen7Constants.tmBlockTwoCount) {
            return tm + (Gen7Constants.tmBlockTwoOffset - 1 - Gen7Constants.tmBlockOneCount);
        } else {
            return tm + (Gen7Constants.tmBlockThreeOffset - 1 - (Gen7Constants.tmBlockOneCount + Gen7Constants.tmBlockTwoCount));
        }
    }

    @Override
    public List<Integer> getCurrentFieldTMs() {
        List<Integer> fieldItems = this.getFieldItems();
        List<Integer> fieldTMs = new ArrayList<>();

        ItemList allowedItems = Gen7Constants.getAllowedItems(romEntry.getRomType());
        for (int item : fieldItems) {
            if (allowedItems.isTM(item)) {
                fieldTMs.add(tmFromIndex(item));
            }
        }

        return fieldTMs.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public void setFieldTMs(List<Integer> fieldTMs) {
        List<Integer> fieldItems = this.getFieldItems();
        int fiLength = fieldItems.size();
        Iterator<Integer> iterTMs = fieldTMs.iterator();
        Map<Integer,Integer> tmMap = new HashMap<>();

        ItemList allowedItems = Gen7Constants.getAllowedItems(romEntry.getRomType());
        for (int i = 0; i < fiLength; i++) {
            int oldItem = fieldItems.get(i);
            if (allowedItems.isTM(oldItem)) {
                if (tmMap.get(oldItem) != null) {
                    fieldItems.set(i,tmMap.get(oldItem));
                    continue;
                }
                int newItem = indexFromTM(iterTMs.next());
                fieldItems.set(i, newItem);
                tmMap.put(oldItem,newItem);
            }
        }

        this.setFieldItems(fieldItems);
    }

    @Override
    public List<Integer> getRegularFieldItems() {
        List<Integer> fieldItems = this.getFieldItems();
        List<Integer> fieldRegItems = new ArrayList<>();

        ItemList allowedItems = Gen7Constants.getAllowedItems(romEntry.getRomType());
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

        ItemList allowedItems = Gen7Constants.getAllowedItems(romEntry.getRomType());
        for (int i = 0; i < fiLength; i++) {
            int oldItem = fieldItems.get(i);
            if (!(allowedItems.isTM(oldItem)) && allowedItems.isAllowed(oldItem)) {
                int newItem = iterNewItems.next();
                fieldItems.set(i, newItem);
            }
        }

        this.setFieldItems(fieldItems);
    }

    @Override
    public List<Integer> getRequiredFieldTMs() {
        return Gen7Constants.getRequiredFieldTMs(romEntry.getRomType());
    }

    public List<Integer> getFieldItems() {
        List<Integer> fieldItems = new ArrayList<>();
        int numberOfAreas = encounterGarc.files.size() / 11;
        for (int i = 0; i < numberOfAreas; i++) {
            byte[][] environmentData = Mini.UnpackMini(encounterGarc.getFile(i * 11),"ED");
            if (environmentData == null) continue;

            byte[][] itemDataFull = Mini.UnpackMini(environmentData[10],"EI");

            byte[][] berryPileDataFull = Mini.UnpackMini(environmentData[11],"EB");

            // Field/hidden items
            for (byte[] itemData: itemDataFull) {
                if (itemData.length > 0) {
                    int itemCount = itemData[0];

                    for (int j = 0; j < itemCount; j++) {
                        fieldItems.add(FileFunctions.read2ByteInt(itemData,(j * 64) + 52));
                    }
                }
            }

            // Berry piles
            for (byte[] berryPileData: berryPileDataFull) {
                if (berryPileData.length > 0) {
                    int pileCount = berryPileData[0];
                    for (int j = 0; j < pileCount; j++) {
                        for (int k = 0; k < 7; k++) {
                            fieldItems.add(FileFunctions.read2ByteInt(berryPileData,4 + j*68 + 54 + k*2));
                        }
                    }
                }
            }
        }
        return fieldItems;
    }

    public void setFieldItems(List<Integer> items) {
        try {
            int numberOfAreas = encounterGarc.files.size() / 11;
            Iterator<Integer> iterItems = items.iterator();
            for (int i = 0; i < numberOfAreas; i++) {
                byte[][] environmentData = Mini.UnpackMini(encounterGarc.getFile(i * 11),"ED");
                if (environmentData == null) continue;

                byte[][] itemDataFull = Mini.UnpackMini(environmentData[10],"EI");

                byte[][] berryPileDataFull = Mini.UnpackMini(environmentData[11],"EB");

                // Field/hidden items
                for (byte[] itemData: itemDataFull) {
                    if (itemData.length > 0) {
                        int itemCount = itemData[0];

                        for (int j = 0; j < itemCount; j++) {
                            FileFunctions.write2ByteInt(itemData,(j * 64) + 52,iterItems.next());
                        }
                    }
                }

                byte[] itemDataPacked = Mini.PackMini(itemDataFull,"EI");
                environmentData[10] = itemDataPacked;

                // Berry piles
                for (byte[] berryPileData: berryPileDataFull) {
                    if (berryPileData.length > 0) {
                        int pileCount = berryPileData[0];

                        for (int j = 0; j < pileCount; j++) {
                            for (int k = 0; k < 7; k++) {
                                FileFunctions.write2ByteInt(berryPileData,4 + j*68 + 54 + k*2,iterItems.next());
                            }
                        }
                    }
                }

                byte[] berryPileDataPacked = Mini.PackMini(berryPileDataFull,"EB");
                environmentData[11] = berryPileDataPacked;

                encounterGarc.setFile(i * 11, Mini.PackMini(environmentData,"ED"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<IngameTrade> getIngameTrades() {
        List<IngameTrade> ingameTrades = new ArrayList<>();
        try {
            GARCArchive staticGarc = readGARC(romEntry.getFile("StaticPokemon"), true);
            List<String> tradeStrings = getStrings(true, romEntry.getIntValue("IngameTradesTextOffset"));
            byte[] tradesFile = staticGarc.files.get(4).get(0);
            int numberOfIngameTrades = tradesFile.length / 0x34;
            for (int i = 0; i < numberOfIngameTrades; i++) {
                int offset = i * 0x34;
                IngameTrade trade = new IngameTrade();
                int givenSpecies = FileFunctions.read2ByteInt(tradesFile, offset);
                int requestedSpecies = FileFunctions.read2ByteInt(tradesFile, offset + 0x2C);
                Pokemon givenPokemon = pokes[givenSpecies];
                Pokemon requestedPokemon = pokes[requestedSpecies];
                int forme = tradesFile[offset + 4];
                if (forme > givenPokemon.getCosmeticFormCount() && forme != 30 && forme != 31) {
                    int speciesWithForme = absolutePokeNumByBaseForme
                            .getOrDefault(givenSpecies, dummyAbsolutePokeNums)
                            .getOrDefault(forme, 0);
                    givenPokemon = pokes[speciesWithForme];
                }
                trade.givenPokemon = givenPokemon;
                trade.requestedPokemon = requestedPokemon;
                trade.nickname = tradeStrings.get(FileFunctions.read2ByteInt(tradesFile, offset + 2));
                trade.otName = tradeStrings.get(FileFunctions.read2ByteInt(tradesFile, offset + 0x18));
                trade.otId = FileFunctions.readFullInt(tradesFile, offset + 0x10);
                trade.ivs = new int[6];
                for (int iv = 0; iv < 6; iv++) {
                    trade.ivs[iv] = tradesFile[offset + 6 + iv];
                }
                trade.item = FileFunctions.read2ByteInt(tradesFile, offset + 0x14);
                if (trade.item < 0) {
                    trade.item = 0;
                }
                ingameTrades.add(trade);
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }
        return ingameTrades;
    }

    @Override
    public void setIngameTrades(List<IngameTrade> trades) {
        try {
            List<IngameTrade> oldTrades = this.getIngameTrades();
            GARCArchive staticGarc = readGARC(romEntry.getFile("StaticPokemon"), true);
            List<String> tradeStrings = getStrings(true, romEntry.getIntValue("IngameTradesTextOffset"));
            Map<Integer, List<Integer>> hardcodedTradeTextOffsets = Gen7Constants.getHardcodedTradeTextOffsets(romEntry.getRomType());
            byte[] tradesFile = staticGarc.files.get(4).get(0);
            int numberOfIngameTrades = tradesFile.length / 0x34;
            for (int i = 0; i < numberOfIngameTrades; i++) {
                IngameTrade trade = trades.get(i);
                int offset = i * 0x34;
                Pokemon givenPokemon = trade.givenPokemon;
                int forme = 0;
                if (givenPokemon.getFormeNumber() > 0) {
                    forme = givenPokemon.getFormeNumber();
                    givenPokemon = givenPokemon.getBaseForme();
                }
                FileFunctions.write2ByteInt(tradesFile, offset, givenPokemon.getNumber());
                tradesFile[offset + 4] = (byte) forme;
                FileFunctions.write2ByteInt(tradesFile, offset + 0x2C, trade.requestedPokemon.getNumber());
                tradeStrings.set(FileFunctions.read2ByteInt(tradesFile, offset + 2), trade.nickname);
                tradeStrings.set(FileFunctions.read2ByteInt(tradesFile, offset + 0x18), trade.otName);
                FileFunctions.writeFullInt(tradesFile, offset + 0x10, trade.otId);
                for (int iv = 0; iv < 6; iv++) {
                    tradesFile[offset + 6 + iv] = (byte) trade.ivs[iv];
                }
                FileFunctions.write2ByteInt(tradesFile, offset + 0x14, trade.item);

                List<Integer> hardcodedTextOffsetsForThisTrade = hardcodedTradeTextOffsets.get(i);
                if (hardcodedTextOffsetsForThisTrade != null) {
                    updateHardcodedTradeText(oldTrades.get(i), trade, tradeStrings, hardcodedTextOffsetsForThisTrade);
                }
            }
            writeGARC(romEntry.getFile("StaticPokemon"), staticGarc);
            setStrings(true, romEntry.getIntValue("IngameTradesTextOffset"), tradeStrings);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    // NOTE: This method is kind of stupid, in that it doesn't try to reflow the text to better fit; it just
    // blindly replaces the Pokemon's name. However, it seems to work well enough for what we need.
    private void updateHardcodedTradeText(IngameTrade oldTrade, IngameTrade newTrade, List<String> tradeStrings, List<Integer> hardcodedTextOffsets) {
        for (int offset : hardcodedTextOffsets) {
            String hardcodedText = tradeStrings.get(offset);
            String oldRequestedName = oldTrade.requestedPokemon.getName();
            String oldGivenName = oldTrade.givenPokemon.getName();
            String newRequestedName = newTrade.requestedPokemon.getName();
            String newGivenName = newTrade.givenPokemon.getName();
            hardcodedText = hardcodedText.replace(oldRequestedName, newRequestedName);
            hardcodedText = hardcodedText.replace(oldGivenName, newGivenName);
            tradeStrings.set(offset, hardcodedText);
        }
    }

    @Override
    public boolean hasDVs() {
        return false;
    }

    @Override
    public int generationOfPokemon() {
        return 7;
    }

    @Override
    // TODO: almost identical to Gen 6 implementation (and very similar to Gen 4/5) => merge (?)
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
            int pokemonCount = Gen7Constants.getPokemonCount(romEntry.getRomType());
            byte[] masterFile = babyGarc.getFile(pokemonCount + 1);
            for (int i = 1; i <= pokemonCount; i++) {
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
            babyGarc.setFile(pokemonCount + 1, masterFile);
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
        // Gen 7 does not have field moves
        return new ArrayList<>();
    }

    @Override
    public List<Integer> getEarlyRequiredHMMoves() {
        // Gen 7 does not have any HMs
        return new ArrayList<>();
    }

    @Override
    public Map<Integer, Shop> getShopItems() {
        int[] tmShops = romEntry.getArrayValue("TMShops");
        int[] regularShops = romEntry.getArrayValue("RegularShops");
        int[] shopItemSizes = romEntry.getArrayValue("ShopItemSizes");
        int shopCount = romEntry.getIntValue("ShopCount");
        Map<Integer, Shop> shopItemsMap = new TreeMap<>();
        try {
            byte[] shopsCRO = readFile(romEntry.getFile("ShopsAndTutors"));
            int offset = Gen7Constants.getShopItemsOffset(romEntry.getRomType());
            for (int i = 0; i < shopCount; i++) {
                boolean badShop = false;
                for (int tmShop : tmShops) {
                    if (i == tmShop) {
                        badShop = true;
                        offset += (shopItemSizes[i] * 2);
                        break;
                    }
                }
                for (int regularShop : regularShops) {
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
                        items.add(FileFunctions.read2ByteInt(shopsCRO, offset));
                        offset += 2;
                    }
                    Shop shop = new Shop();
                    shop.items = items;
                    shop.name = shopNames.get(i);
                    shop.isMainGame = Gen7Constants.getMainGameShops(romEntry.getRomType()).contains(i);
                    shopItemsMap.put(i, shop);
                }
            }
            return shopItemsMap;
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    public void setShopItems(Map<Integer, Shop> shopItems) {
        int[] tmShops = romEntry.getArrayValue("TMShops");
        int[] regularShops = romEntry.getArrayValue("RegularShops");
        int[] shopItemSizes = romEntry.getArrayValue("ShopItemSizes");
        int shopCount = romEntry.getIntValue("ShopCount");
        try {
            byte[] shopsCRO = readFile(romEntry.getFile("ShopsAndTutors"));
            int offset = Gen7Constants.getShopItemsOffset(romEntry.getRomType());
            for (int i = 0; i < shopCount; i++) {
                boolean badShop = false;
                for (int tmShop : tmShops) {
                    if (i == tmShop) {
                        badShop = true;
                        offset += (shopItemSizes[i] * 2);
                        break;
                    }
                }
                for (int regularShop : regularShops) {
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
                        FileFunctions.write2ByteInt(shopsCRO, offset, item);
                        offset += 2;
                    }
                }
            }
            writeFile(romEntry.getFile("ShopsAndTutors"), shopsCRO);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    public void setBalancedShopPrices() {
        try {
            GARCArchive itemPriceGarc = this.readGARC(romEntry.getFile("ItemData"),true);
            for (int i = 1; i < itemPriceGarc.files.size(); i++) {
                writeWord(itemPriceGarc.files.get(i).get(0),0, Gen7Constants.balancedItemPrices.get(i));
            }
            writeGARC(romEntry.getFile("ItemData"),itemPriceGarc);
        } catch (IOException e) {
            throw new RomIOException(e);
        }
    }

    @Override
    public List<PickupItem> getPickupItems() {
        List<PickupItem> pickupItems = new ArrayList<>();
        try {
            GARCArchive pickupGarc = this.readGARC(romEntry.getFile("PickupData"), false);
            byte[] pickupData = pickupGarc.getFile(0);
            int numberOfPickupItems = FileFunctions.readFullInt(pickupData, 0) - 1; // GameFreak why???
            for (int i = 0; i < numberOfPickupItems; i++) {
                int offset = 4 + (i * 0xC);
                int item = FileFunctions.read2ByteInt(pickupData, offset);
                PickupItem pickupItem = new PickupItem(item);
                for (int levelRange = 0; levelRange < 10; levelRange++) {
                    pickupItem.probabilities[levelRange] = pickupData[offset + levelRange + 2];
                }
                pickupItems.add(pickupItem);
            }
        } catch (IOException e) {
            throw new RomIOException(e);
        }
        return pickupItems;
    }

    @Override
    public void setPickupItems(List<PickupItem> pickupItems) {
        try {
            GARCArchive pickupGarc = this.readGARC(romEntry.getFile("PickupData"), false);
            byte[] pickupData = pickupGarc.getFile(0);
            for (int i = 0; i < pickupItems.size(); i++) {
                int offset = 4 + (i * 0xC);
                int item = pickupItems.get(i).item;
                FileFunctions.write2ByteInt(pickupData, offset, item);
            }
            this.writeGARC(romEntry.getFile("PickupData"), pickupGarc);
        } catch (IOException e) {
            throw new RomIOException(e);
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
        byte[] iconBytes = pokeGraphicsGARC.files.get(iconIndex).get(0);
        BFLIM icon = new BFLIM(iconBytes);
        return icon.getImage();
    }

    private class ZoneData {
        public int worldIndex;
        public int areaIndex;
        public int parentMap;
        public String locationName;
        private byte[] data;

        public static final int size = 0x54;

        public ZoneData(byte[] zoneDataBytes, int index) {
            data = new byte[size];
            System.arraycopy(zoneDataBytes, index * size, data, 0, size);
            parentMap = FileFunctions.readFullInt(data, 0x1C);
        }
    }

    private class AreaData {
        public int fileNumber;
        public boolean hasTables;
        public List<byte[]> encounterTables;
        public List<ZoneData> zones;
        public String name;

        public AreaData() {
            encounterTables = new ArrayList<>();
        }
    }

    @Override
    public List<Integer> getAllConsumableHeldItems() {
        return Gen7Constants.consumableHeldItems;
    }

    @Override
    public List<Integer> getAllHeldItems() {
        return Gen7Constants.allHeldItems;
    }

    @Override
    public boolean hasRivalFinalBattle() {
        return true;
    }

    @Override
    public List<Integer> getSensibleHeldItemsFor(TrainerPokemon tp, boolean consumableOnly, List<Move> moves, int[] pokeMoves) {
        List<Integer> items = new ArrayList<>(Gen7Constants.generalPurposeConsumableItems);
        int frequencyBoostCount = 6; // Make some very good items more common, but not too common
        if (!consumableOnly) {
            frequencyBoostCount = 8; // bigger to account for larger item pool.
            items.addAll(Gen7Constants.generalPurposeItems);
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
                items.add(Gen7Constants.consumableTypeBoostingItems.get(move.type));
                if (!consumableOnly) {
                    items.addAll(Gen7Constants.typeBoostingItems.get(move.type));
                    items.add(Items.choiceBand);
                    items.add(Items.muscleBand);
                }
            }
            if (move.category == MoveCategory.SPECIAL) {
                numDamagingMoves++;
                items.add(Items.petayaBerry);
                items.add(Gen7Constants.consumableTypeBoostingItems.get(move.type));
                if (!consumableOnly) {
                    items.addAll(Gen7Constants.typeBoostingItems.get(move.type));
                    items.add(Items.wiseGlasses);
                    items.add(Items.choiceSpecs);
                }
            }
            if (!consumableOnly && Gen7Constants.moveBoostingItems.containsKey(moveIdx)) {
                items.addAll(Gen7Constants.moveBoostingItems.get(moveIdx));
            }
        }
        if (numDamagingMoves >= 2) {
            items.add(Items.assaultVest);
        }
        Map<Type, Effectiveness> byType = getTypeTable().against(tp.pokemon.getPrimaryType(), tp.pokemon.getSecondaryType());
        for(Map.Entry<Type, Effectiveness> entry : byType.entrySet()) {
            Integer berry = Gen7Constants.weaknessReducingBerries.get(entry.getKey());
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
        if (Gen7Constants.consumableAbilityBoostingItems.containsKey(ability)) {
            items.add(Gen7Constants.consumableAbilityBoostingItems.get(ability));
        }

        if (!consumableOnly) {
            if (Gen7Constants.abilityBoostingItems.containsKey(ability)) {
                items.addAll(Gen7Constants.abilityBoostingItems.get(ability));
            }
            if (tp.pokemon.getPrimaryType() == Type.POISON || tp.pokemon.getSecondaryType() == Type.POISON) {
                items.add(Items.blackSludge);
            }
            List<Integer> speciesItems = Gen7Constants.speciesBoostingItems.get(tp.pokemon.getNumber());
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
    protected Gen7RomEntry getRomEntry() {
        return romEntry;
    }
}
