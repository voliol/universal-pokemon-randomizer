package com.dabomstew.pkrandom.pokemon;

/*----------------------------------------------------------------------------*/
/*--  Type.java - represents a Pokemon or move type.                        --*/
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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import com.dabomstew.pkrandom.RomFunctions;
import com.dabomstew.pkrandom.exceptions.RandomizationException;
import com.dabomstew.pkrandom.pokemon.TypeRelationship.Effectiveness;

public enum Type {

    NORMAL, FIGHTING, FLYING, GRASS, WATER, FIRE, ROCK, GROUND, PSYCHIC, BUG, DRAGON, ELECTRIC, GHOST, POISON, ICE, STEEL, DARK, GAS(
            true), FAIRY(true), WOOD(true), ABNORMAL(
                    true), WIND(true), SOUND(true), LIGHT(true), TRI(true), HACK(true);

    public boolean isHackOnly;

    private Type() {
        this(false);
    }

    private Type(boolean isHackOnly) {
        this.isHackOnly = isHackOnly;
    }

    private static final List<Type> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
    private static final int SIZE = VALUES.size();
    private static List<Type> shuffledList;

    public static final Map<Type, List<Type>> STRONG_AGAINST = new HashMap<Type, List<Type>>();
    public static final Map<Type, List<Type>> RESISTANT_TO = new HashMap<Type, List<Type>>();
    public static final Map<Type, List<Type>> IMMUNE_TO = new HashMap<Type, List<Type>>();

    // Setup a default list to support type matchups
    static {
        STRONG_AGAINST.put(BUG, TypeRelationship.STRONG_AGAINST_BUG);
        STRONG_AGAINST.put(DARK, TypeRelationship.STRONG_AGAINST_DARK);
        STRONG_AGAINST.put(DRAGON, TypeRelationship.STRONG_AGAINST_DRAGON);
        STRONG_AGAINST.put(ELECTRIC, TypeRelationship.STRONG_AGAINST_ELECTRIC);
        STRONG_AGAINST.put(FIGHTING, TypeRelationship.STRONG_AGAINST_FIGHTING);
        STRONG_AGAINST.put(FIRE, TypeRelationship.STRONG_AGAINST_FIRE);
        STRONG_AGAINST.put(FLYING, TypeRelationship.STRONG_AGAINST_FLYING);
        STRONG_AGAINST.put(GHOST, TypeRelationship.STRONG_AGAINST_GHOST);
        STRONG_AGAINST.put(GRASS, TypeRelationship.STRONG_AGAINST_GRASS);
        STRONG_AGAINST.put(GROUND, TypeRelationship.STRONG_AGAINST_GROUND);
        STRONG_AGAINST.put(ICE, TypeRelationship.STRONG_AGAINST_ICE);
        STRONG_AGAINST.put(NORMAL, TypeRelationship.STRONG_AGAINST_NORMAL);
        STRONG_AGAINST.put(POISON, TypeRelationship.STRONG_AGAINST_POISON);
        STRONG_AGAINST.put(PSYCHIC, TypeRelationship.STRONG_AGAINST_PSYCHIC);
        STRONG_AGAINST.put(ROCK, TypeRelationship.STRONG_AGAINST_ROCK);
        STRONG_AGAINST.put(STEEL, TypeRelationship.STRONG_AGAINST_STEEL);
        STRONG_AGAINST.put(WATER, TypeRelationship.STRONG_AGAINST_WATER);

        RESISTANT_TO.put(BUG, TypeRelationship.RESISTANT_TO_BUG);
        RESISTANT_TO.put(DARK, TypeRelationship.RESISTANT_TO_DARK);
        RESISTANT_TO.put(DRAGON, TypeRelationship.RESISTANT_TO_DRAGON);
        RESISTANT_TO.put(ELECTRIC, TypeRelationship.RESISTANT_TO_ELECTRIC);
        RESISTANT_TO.put(FIGHTING, TypeRelationship.RESISTANT_TO_FIGHTING);
        RESISTANT_TO.put(FIRE, TypeRelationship.RESISTANT_TO_FIRE);
        RESISTANT_TO.put(FLYING, TypeRelationship.RESISTANT_TO_FLYING);
        RESISTANT_TO.put(GHOST, TypeRelationship.RESISTANT_TO_GHOST);
        RESISTANT_TO.put(GRASS, TypeRelationship.RESISTANT_TO_GRASS);
        RESISTANT_TO.put(GROUND, TypeRelationship.RESISTANT_TO_GROUND);
        RESISTANT_TO.put(ICE, TypeRelationship.RESISTANT_TO_ICE);
        RESISTANT_TO.put(NORMAL, TypeRelationship.RESISTANT_TO_NORMAL);
        RESISTANT_TO.put(POISON, TypeRelationship.RESISTANT_TO_POISON);
        RESISTANT_TO.put(PSYCHIC, TypeRelationship.RESISTANT_TO_PSYCHIC);
        RESISTANT_TO.put(ROCK, TypeRelationship.RESISTANT_TO_ROCK);
        RESISTANT_TO.put(STEEL, TypeRelationship.RESISTANT_TO_STEEL);
        RESISTANT_TO.put(WATER, TypeRelationship.RESISTANT_TO_WATER);

        IMMUNE_TO.put(ELECTRIC, TypeRelationship.IMMUNE_TO_ELECTRIC);
        IMMUNE_TO.put(FIGHTING, TypeRelationship.IMMUNE_TO_FIGHTING);
        IMMUNE_TO.put(GHOST, TypeRelationship.IMMUNE_TO_GHOST);
        IMMUNE_TO.put(GROUND, TypeRelationship.IMMUNE_TO_GROUND);
        IMMUNE_TO.put(NORMAL, TypeRelationship.IMMUNE_TO_NORMAL);
        IMMUNE_TO.put(POISON, TypeRelationship.IMMUNE_TO_POISON);
        IMMUNE_TO.put(PSYCHIC, TypeRelationship.IMMUNE_TO_PSYCHIC);
    }

    public static Type randomType(Random random) {
        return VALUES.get(random.nextInt(SIZE));
    }

    public String camelCase() {
        return RomFunctions.camelCase(this.toString());
    }

    public static List<Type> getTypes(int size) {
        return VALUES.subList(0, size);
    }

    public static void setShuffledList(List<Type> list) {
        shuffledList = list;
    }

    public static List<Type> getShuffledList() {
        return shuffledList;
    }

    public static Type randomStrength(Random random, boolean useResistantType, Type... checkTypes) {
        // Safety check since varargs allow zero arguments
        if (checkTypes.length < 1) {
            throw new RandomizationException("Must provide at least 1 type to obtain a strength");
        }

        if (useResistantType) {
            return getStrengthFromList(random, getCombinedResistanceMap(), checkTypes);
        } else {
            return getStrengthFromList(random, STRONG_AGAINST, checkTypes);
        }
    }

    private static Type getStrengthFromList(Random random, Map<Type, List<Type>> checkMap,
            Type[] checkTypes) {
        List<Type> randomTypes = new ArrayList<Type>(VALUES);
        Type backupChoice = null;
        Collections.shuffle(randomTypes, random);

        // Attempt to find shared type
        for (Type checkType : randomTypes) {
            // Make sure the type is not null
            if (checkType == null || checkMap.get(checkType) == null) {
                continue;
            }

            // If everything is in a list, return it
            if (checkMap.get(checkType).containsAll(Arrays.asList(checkTypes))) {
                return checkType;
            }

            // If no backup set, and neither of the types appears, go to next iteration
            if (backupChoice == null
                    && Collections.disjoint(checkMap.get(checkType), Arrays.asList(checkTypes))) {
                continue;
            }

            // Set the backup choice since at least 1 is shared
            backupChoice = checkType;
        }

        // Return the backup choice since no shared type was found
        if (backupChoice != null) {
            return backupChoice;
        }

        // No match found (for instance, Normal-type)
        return null;
    }

    public static Type randomWeakness(Random random, boolean useResistantType, Type... checkTypes) {
        // Safety check since varargs allow zero arguments
        if (checkTypes.length < 1) {
            throw new RandomizationException("Must provide at least 1 type to obtain a weakness");
        }

        if (useResistantType) {
            return getWeaknessFromList(random, getCombinedResistanceMap(), checkTypes);
        } else {
            return getWeaknessFromList(random, STRONG_AGAINST, checkTypes);
        }
    }

    private static Type getWeaknessFromList(Random random, Map<Type, List<Type>> checkMap,
            Type[] checkTypes) {
        List<Type> pickList = new ArrayList<Type>();
        boolean initialized = false;

        // Loop through all given types to reduce to a list of common weaknesses
        for (Type checkType : checkTypes) {
            // Make sure the type is not null
            if (checkType == null) {
                continue;
            }
            // Initialize the list
            // This can happen multiple times if "retainAll" clears the list
            // due to no shared weakness, such as Ghost/Dark
            if (pickList.size() < 1) {
                pickList.addAll(checkMap.get(checkType));
                initialized = true;
            }
            // Otherwise only keep types shared in both lists
            else {
                pickList.retainAll(checkMap.get(checkType));
            }
        }
        // If the list has elements in it still, pick one
        if (pickList.size() > 0) {
            return pickList.get(random.nextInt(pickList.size()));
        }
        // Otherwise pick a random weakness for any of the types given
        else if (initialized) {
            Type randomType = checkTypes[random.nextInt(checkTypes.length)];
            List<Type> resistantList = checkMap.get(randomType);
            return resistantList.get(random.nextInt(resistantList.size()));
        }

        // No match found so return null
        return null;
    }

    public static List<Type> getWeaknesses(Type checkType, int maxNum) {
        if (maxNum < 0) {
            return Collections.emptyList();
        }
        List<Type> checkList = STRONG_AGAINST.get(checkType);
        return checkList.subList(0, maxNum > checkList.size() ? checkList.size() : maxNum);
    }

    public static int typesToInt(List<Type> types) {
        if (types == null || types.size() > 32) {
            // No can do
            return 0;
        }
        int initial = 0;
        int state = 1;
        for (Type t : VALUES) {
            initial |= types.contains(t) ? state : 0;
            state *= 2;
        }
        return initial;
    }

    public static List<Type> intToTypes(int types) {
        if (types == 0) {
            return null;
        }
        List<Type> typesList = new ArrayList<Type>();
        int state = 1;
        for (int i = 0; i < VALUES.size(); i++) {
            if ((types & state) > 0) {
                typesList.add(VALUES.get(i));
            }
            state *= 2;
        }
        return typesList;
    }

    public static Map<Type, List<Type>> getCombinedResistanceMap() {
        Map<Type, List<Type>> combineMap = new HashMap<Type, List<Type>>();
        for (Type type : RESISTANT_TO.keySet()) {
            ArrayList<Type> combineList = new ArrayList<Type>(RESISTANT_TO.get(type));
            Optional.ofNullable(IMMUNE_TO.get(type)).ifPresent(combineList::addAll);
            combineMap.put(type, combineList);
        }
        return combineMap;
    }

    /**
     * Update the STRONG_AGAINST map such that STRONG_AGAINST_<defender> includes attacker
     * 
     * @param attacker - Type of the attacker
     * @param defender - Type of the defender
     */
    public static void updateStrongAgainst(Type attacker, Type defender) {
        if (STRONG_AGAINST.get(defender) == null) {
            STRONG_AGAINST.put(defender, new ArrayList<Type>());
        }
        STRONG_AGAINST.get(defender).add(attacker);
    }

    /**
     * Update the RESISTANT_TO map such that RESISTANT_TO_<attacker> includes defender
     * 
     * @param attacker - Type of the attacker
     * @param defender - Type of the defender
     */
    public static void updateResistantTo(Type attacker, Type defender) {
        if (RESISTANT_TO.get(attacker) == null) {
            RESISTANT_TO.put(attacker, new ArrayList<Type>());
        }
        RESISTANT_TO.get(attacker).add(defender);
    }

    /**
     * Update the IMMUNE_TO map such that IMMUNE_TO_<attacker> includes defender
     * 
     * @param attacker - Type of the attacker
     * @param defender - Type of the defender
     */
    public static void updateImmuneTo(Type attacker, Type defender) {
        if (IMMUNE_TO.get(attacker) == null) {
            IMMUNE_TO.put(attacker, new ArrayList<Type>());
        }
        IMMUNE_TO.get(attacker).add(defender);
    }
}
