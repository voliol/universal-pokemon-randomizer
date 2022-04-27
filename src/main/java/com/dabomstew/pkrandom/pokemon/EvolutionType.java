package com.dabomstew.pkrandom.pokemon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*----------------------------------------------------------------------------*/
/*--  EvolutionType.java - represents an evolution method.                  --*/
/*--                                                                        --*/
/*--  Part of "Universal Pokemon Randomizer" by Dabomstew                   --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2012.                                 --*/
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

public enum EvolutionType {
    /* @formatter:off */
    LEVEL(1, 1, 4, 4, 4),
    STONE(2, 2, 7, 7, 8),
    TRADE(3, 3, 5, 5, 5),
    TRADE_ITEM(-1, 3, 6, 6, 6),
    HAPPINESS(-1, 4, 1, 1, 1),
    HAPPINESS_DAY(-1, 4, 2, 2, 2),
    HAPPINESS_NIGHT(-1, 4, 3, 3, 3),
    LEVEL_ATTACK_HIGHER(-1, 5, 8, 8, 9),
    LEVEL_DEFENSE_HIGHER(-1, 5, 10, 10, 11),
    LEVEL_ATK_DEF_SAME(-1, 5, 9, 9, 10),
    LEVEL_LOW_PV(-1, -1, 11, 11, 12),
    LEVEL_HIGH_PV(-1, -1, 12, 12, 13),
    LEVEL_CREATE_EXTRA(-1, -1, 13, 13, 14),
    LEVEL_IS_EXTRA(-1, -1, 14, 14, 15),
    LEVEL_HIGH_BEAUTY(-1, -1, 15, 15, 16),
    STONE_MALE_ONLY(-1, -1, -1, 16, 17),
    STONE_FEMALE_ONLY(-1, -1, -1, 17, 18),
    LEVEL_ITEM_DAY(-1, -1, -1, 18, 19),
    LEVEL_ITEM_NIGHT(-1, -1, -1, 19, 20),
    LEVEL_WITH_MOVE(-1, -1, -1, 20, 21),
    LEVEL_WITH_OTHER(-1, -1, -1, 21, 22),
    LEVEL_MALE_ONLY(-1, -1, -1, 22, 23),
    LEVEL_FEMALE_ONLY(-1, -1, -1, 23, 24),
    LEVEL_ELECTRIFIED_AREA(-1, -1, -1, 24, 25),
    LEVEL_MOSS_ROCK(-1, -1, -1, 25, 26),
    LEVEL_ICY_ROCK(-1, -1, -1, 26, 27),
    TRADE_SPECIAL(-1, -1, -1, -1, 7),
    FAIRY_AFFECTION(-1, -1, -1, -1, -1),
    LEVEL_WITH_DARK(-1, -1, -1, -1, -1),
    LEVEL_UPSIDE_DOWN(-1, -1, -1, -1, -1),
    LEVEL_RAIN(-1, -1, -1, -1, -1),
    LEVEL_DAY(-1, -1, -1, -1, -1),
    LEVEL_NIGHT(-1, -1, -1, -1, -1),
    MEGA_EVOLVE(-1, -1, -1, -1, -1),
    LEVEL_FEMALE_ESPURR(-1, -1, -1, -1, -1),
    LEVEL_GAME(-1, -1, -1, -1, -1),
    LEVEL_DAY_GAME(-1, -1, -1, -1, -1),
    LEVEL_NIGHT_GAME(-1, -1, -1, -1, -1),
    LEVEL_SNOWY(-1, -1, -1, -1, -1),
    LEVEL_DUSK(-1, -1, -1, -1, -1),
    LEVEL_NIGHT_ULTRA(-1, -1, -1, -1, -1),
    STONE_ULTRA(-1, -1, -1, -1, -1),
    NONE(-1, -1, -1, -1, -1);
    /* @formatter:on */

    private int[] indexNumbers;
    private static ArrayList<Integer> methodsAvailable = new ArrayList<Integer>();
    private static EvolutionType[][] reverseIndexes = new EvolutionType[5][256];
    private static ArrayList<EvolutionType> bannedMethods = new ArrayList<EvolutionType>(
            // These don't work for anything but Nincada -> Shedninja
            // And Karrablast -> Shelmet
            // And MEGA_EVOLVE is too complicated
            Arrays.asList(LEVEL_CREATE_EXTRA, LEVEL_IS_EXTRA, TRADE_SPECIAL, MEGA_EVOLVE));
    private static ArrayList<EvolutionType> usesDifferentData = new ArrayList<EvolutionType>(
            // These methods do not follow standard data structure - skip requirements and
            // just copy the data
            Arrays.asList(MEGA_EVOLVE));

    static {
        for (EvolutionType et : EvolutionType.values()) {
            for (int i = 0; i < et.indexNumbers.length; i++) {
                if (et.indexNumbers[i] > 0 && reverseIndexes[i][et.indexNumbers[i]] == null) {
                    reverseIndexes[i][et.indexNumbers[i]] = et;
                }
            }
        }
    }

    private EvolutionType(int... indexes) {
        this.indexNumbers = indexes;
    }

    public static void modifyEvolutionTypes(int generation,
            Map<Integer, EvolutionType> evolutionMethods) {
        evolutionMethods.forEach((k, v) -> {
            if (v != NONE) {
                v.indexNumbers[generation - 1] = k;
                reverseIndexes[generation - 1][k] = v;
                methodsAvailable.add(k);
            } else {
                EvolutionType oldByte = reverseIndexes[generation - 1][k];
                if (oldByte != null) {
                    oldByte.indexNumbers[generation - 1] = -1;
                    reverseIndexes[generation - 1][k] = null;
                }
            }
        });
    }

    public int toIndex(int generation) {
        return indexNumbers[generation - 1];
    }

    public boolean usesLevel() {
        return (this == LEVEL) || (this == LEVEL_ATTACK_HIGHER) || (this == LEVEL_DEFENSE_HIGHER)
                || (this == LEVEL_ATK_DEF_SAME) || (this == LEVEL_LOW_PV) || (this == LEVEL_HIGH_PV)
                || (this == LEVEL_CREATE_EXTRA) || (this == LEVEL_IS_EXTRA)
                || (this == LEVEL_MALE_ONLY) || (this == LEVEL_FEMALE_ONLY);
    }

    public static boolean isInGeneration(int generation, EvolutionType et) {
        return et.indexNumbers[generation - 1] > -1;
    }

    public static EvolutionType fromIndex(int generation, int index) {
        return reverseIndexes[generation - 1][index];
    }

    public static EvolutionType randomFromGeneration(Random random, int generation) {
        List<Integer> available = generationCount(generation);
        int choice = available.get(random.nextInt(available.size()));
        EvolutionType et = reverseIndexes[generation - 1][choice];
        while (bannedMethods.contains(et) || et == null) {
            choice = available.get(random.nextInt(available.size()));
            et = reverseIndexes[generation - 1][choice];
        }
        if (generation == 2) {
            // Since Gen 2 has a special pointer value for the version of happiness,
            // all indices are set at 4. We select a specific version by getting a
            // number from 0-2 and adding 4 to match the index of that type
            if (et == EvolutionType.HAPPINESS) {
                et = EvolutionType.values()[random.nextInt(3) + 4];
            }
            // Same thing here, but for atk-def methods
            else if (et == EvolutionType.LEVEL_ATTACK_HIGHER) {
                et = EvolutionType.values()[random.nextInt(3) + 7];
            }
            // Same thing here, but for trade and trade_item
            else if (et == EvolutionType.TRADE) {
                et = EvolutionType.values()[random.nextInt(2) + 2];
            }
        }
        return et;
    }

    public static List<Integer> generationCount(int generation) {
        if (methodsAvailable.size() > 0) {
            return methodsAvailable;
        } else {
            switch (generation) {
                case 1:
                    return IntStream.range(1, 4).boxed().collect(Collectors.toList());
                case 2:
                    return IntStream.range(1, 6).boxed().collect(Collectors.toList());
                case 3:
                    return IntStream.range(1, 16).boxed().collect(Collectors.toList());
                case 4:
                    return IntStream.range(1, 27).boxed().collect(Collectors.toList());
                case 5:
                    return IntStream.range(1, 28).boxed().collect(Collectors.toList());
                default:
                    return Collections.EMPTY_LIST;
            }
        }
    }

    public static ArrayList<EvolutionType> happinessEvos() {
        return new ArrayList<EvolutionType>(Arrays.asList(EvolutionType.HAPPINESS,
                EvolutionType.HAPPINESS_DAY, EvolutionType.HAPPINESS_NIGHT));
    }

    public static ArrayList<EvolutionType> uncontrolledLevelEvos() {
        return new ArrayList<EvolutionType>(Arrays.asList(EvolutionType.LEVEL,
                EvolutionType.LEVEL_HIGH_PV, EvolutionType.LEVEL_LOW_PV,
                EvolutionType.LEVEL_FEMALE_ONLY, EvolutionType.LEVEL_MALE_ONLY));
    }

    public static ArrayList<EvolutionType> tradeEvos() {
        return new ArrayList<EvolutionType>(
                Arrays.asList(EvolutionType.TRADE, EvolutionType.TRADE_ITEM));
    }

    public static ArrayList<EvolutionType> stoneEvos() {
        return new ArrayList<EvolutionType>(Arrays.asList(EvolutionType.STONE,
                EvolutionType.STONE_MALE_ONLY, EvolutionType.STONE_FEMALE_ONLY));
    }

    public static ArrayList<EvolutionType> itemEvos() {
        return new ArrayList<EvolutionType>(Arrays.asList(EvolutionType.LEVEL_ITEM_DAY,
                EvolutionType.LEVEL_ITEM_NIGHT, EvolutionType.TRADE_ITEM));
    }

    public static ArrayList<EvolutionType> partyEvos() {
        return new ArrayList<EvolutionType>(Arrays.asList(EvolutionType.LEVEL_WITH_OTHER));
    }

    /**
     * Check if an evolution type is of a particular group
     * 
     * @param method - The group name
     * @param toCheck - The evolution type to compare with the group
     * @return - True if it's in the group, false if it's not or if the method is unsupported
     */
    public static boolean isOfType(String method, EvolutionType toCheck) {
        switch (method.toUpperCase()) {
            case "HAPPINESS":
                return happinessEvos().contains(toCheck);
            case "UNCONTROLLED":
                return uncontrolledLevelEvos().contains(toCheck);
            case "BRANCHLEVEL":
                return uncontrolledLevelEvos().contains(toCheck) && toCheck != EvolutionType.LEVEL;
            case "TRADE":
                return tradeEvos().contains(toCheck);
            case "STONE":
                return stoneEvos().contains(toCheck);
            case "ITEM":
                return itemEvos().contains(toCheck);
            case "PARTY":
                return partyEvos().contains(toCheck);
            case "BANNED":
                return bannedMethods.contains(toCheck);
            case "DIFFERENT":
                return usesDifferentData.contains(toCheck);
            default:
                return false;
        }
    }

    /**
     * Check if a list of evolution types contains a type used in a method
     * 
     * @param method - The group to check
     * @param methodGroup - List of evolution types to compare with
     * @return - True if there is at least one shared method between the lists, false if not or if
     *         method is unsupported
     */
    public static boolean usesTypeOf(String method, List<EvolutionType> methodGroup) {
        switch (method.toUpperCase()) {
            case "HAPPINESS":
                return !Collections.disjoint(happinessEvos(), methodGroup);
            case "UNCONTROLLED":
                return !Collections.disjoint(uncontrolledLevelEvos(), methodGroup);
            case "BRANCHLEVEL":
                return !Collections.disjoint(uncontrolledLevelEvos(), methodGroup)
                        && !methodGroup.contains(EvolutionType.LEVEL);
            case "TRADE":
                return !Collections.disjoint(tradeEvos(), methodGroup);
            case "STONE":
                return !Collections.disjoint(stoneEvos(), methodGroup);
            case "ITEM":
                return !Collections.disjoint(itemEvos(), methodGroup);
            case "PARTY":
                return !Collections.disjoint(partyEvos(), methodGroup);
            case "BANNED":
                return !Collections.disjoint(bannedMethods, methodGroup);
            case "DIFFERENT":
                return !Collections.disjoint(usesDifferentData, methodGroup);
            default:
                return false;
        }
    }

    public static boolean usesTypeOf(String method, List<EvolutionType> methodGroup, int size) {
        switch (method.toUpperCase()) {
            case "HAPPINESS":
                return happinessEvos().stream().filter(ev -> methodGroup.contains(ev))
                        .count() >= size;
            case "UNCONTROLLED":
                return uncontrolledLevelEvos().stream().filter(ev -> methodGroup.contains(ev))
                        .count() >= size;
            case "BRANCHLEVEL":
                return uncontrolledLevelEvos().stream()
                        .filter(ev -> methodGroup.contains(ev) && ev != EvolutionType.LEVEL)
                        .count() >= size;
            case "TRADE":
                return tradeEvos().stream().filter(ev -> methodGroup.contains(ev)).count() >= size;
            case "STONE":
                return stoneEvos().stream().filter(ev -> methodGroup.contains(ev)).count() >= size;
            case "ITEM":
                return itemEvos().stream().filter(ev -> methodGroup.contains(ev)).count() >= size;
            case "PARTY":
                return partyEvos().stream().filter(ev -> methodGroup.contains(ev)).count() >= size;
            case "BANNED":
                return bannedMethods.stream().filter(ev -> methodGroup.contains(ev))
                        .count() >= size;
            case "DIFFERENT":
                return usesDifferentData.stream().filter(ev -> methodGroup.contains(ev))
                        .count() >= size;
            default:
                return false;
        }
    }
}
