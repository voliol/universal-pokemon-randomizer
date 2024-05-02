package com.dabomstew.pkrandom.randomizers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.exceptions.RandomizationException;
import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.util.*;

public class StarterRandomizer extends Randomizer {

    private static final int MAX_TYPE_TRIANGLE_STARTER_TRIES = 500;

    public StarterRandomizer(RomHandler romHandler, Settings settings, Random random) {
        super(romHandler, settings, random);
    }

    public void randomizeStarters() {
        boolean abilitiesUnchanged = settings.getAbilitiesMod() == Settings.AbilitiesMod.UNCHANGED;
        boolean allowAltFormes = settings.isAllowStarterAltFormes();
        boolean banIrregularAltFormes = settings.isBanIrregularAltFormes();
        boolean noLegendaries = settings.isStartersNoLegendaries();
        boolean noDualTypes = settings.isStartersNoDualTypes();
        boolean useCustomStarters = settings.getStartersMod() == Settings.StartersMod.CUSTOM;
        boolean triStageOnly = settings.getStartersMod() == Settings.StartersMod.RANDOM_WITH_TWO_EVOLUTIONS;
        boolean basicOnly = triStageOnly || settings.getStartersMod() == Settings.StartersMod.RANDOM_BASIC;
        boolean typeFwg = settings.getStartersTypeMod() == Settings.StartersTypeMod.FIRE_WATER_GRASS;
        boolean typeUnique = settings.getStartersTypeMod() == Settings.StartersTypeMod.UNIQUE;
        boolean typeTriangle = settings.getStartersTypeMod() == Settings.StartersTypeMod.TRIANGLE;
        boolean typeSingle = settings.getStartersTypeMod() == Settings.StartersTypeMod.SINGLE_TYPE;
        boolean hasTypeRestriction = typeFwg || typeUnique || typeTriangle || typeSingle;
        Type singleType = settings.getStartersSingleType();
        int[] customStarters = settings.getCustomStarters();
        int starterCount = romHandler.starterCount();

        PokemonSet<Pokemon> choosable;

        if (allowAltFormes) {
            choosable = new PokemonSet<>(noLegendaries ? rPokeService.getNonLegendaries(true) : rPokeService.getAll(true));
            if (abilitiesUnchanged) {
                choosable.removeAll(rPokeService.getAbilityDependentFormes());
            }
            if (banIrregularAltFormes) {
                choosable.removeAll(romHandler.getIrregularFormes());
            }
            choosable.removeIf(Pokemon::isActuallyCosmetic);
        } else {
            choosable = new PokemonSet<>(noLegendaries ? rPokeService.getNonLegendaries(false) : rPokeService.getAll(false));
        }

        List<Pokemon> pickedStarters = new ArrayList<>();

        if (useCustomStarters) {
            List<Pokemon> romPokemon = romHandler.getPokemonInclFormes()
                    .stream()
                    .filter(pk -> pk == null || !pk.isActuallyCosmetic())
                    .toList();

            for (int customStarter : customStarters) {
                if (!(customStarter == 0)) {
                    Pokemon starter = romPokemon.get(customStarter);
                    choosable.remove(starter);
                    pickedStarters.add(starter);
                }
            }

            if (pickedStarters.size() == starterCount) {
                romHandler.setStarters(pickedStarters);
                return;
            } else if (pickedStarters.size() > starterCount) {
                //what.
                throw new RandomizationException("Custom starter list exceeded starter count?!");
            }
        }

        if (noDualTypes) {
            choosable.removeIf(p -> p.getSecondaryType() != null);
        }
        if (basicOnly) {
            choosable.removeIf(p -> !p.getEvolutionsTo().isEmpty());
        }
        if (triStageOnly) {
            List<Pokemon> invalids = new ArrayList<>();
            for (Pokemon poke : choosable) {
                boolean isTriStage = false;
                for (Evolution evo : poke.getEvolutionsFrom()) {
                    if (!evo.getTo().getEvolutionsFrom().isEmpty()) {
                        isTriStage = true;
                        break;
                    }
                }
                if (!isTriStage) {
                    invalids.add(poke);
                }
            }
            choosable.removeAll(invalids);
            //there's probably a better way to do this but im too sleepy to think of it
        }

        //all constraints except type done!
        //sanity check
        if (choosable.size() < starterCount - pickedStarters.size()) {
            throw new RandomizationException("Not enough valid starters");
        }

        if (!hasTypeRestriction) {
            while (pickedStarters.size() < starterCount) {
                Pokemon picked = choosable.getRandom(random);
                pickedStarters.add(picked);
                choosable.remove(picked);
            }
        } else if (typeUnique) {
            //we don't actually need a type map for this one
            while (pickedStarters.size() < starterCount) {
                Pokemon picked = choosable.getRandom(random);
                pickedStarters.add(picked);
                choosable.remove(picked);
                choosable.removeIf(p -> (p.getPrimaryType() == picked.getPrimaryType() || p.getSecondaryType() == picked.getPrimaryType()));
                if (picked.getSecondaryType() != null) {
                    choosable.removeIf(p -> (p.getPrimaryType() == picked.getSecondaryType() || p.getSecondaryType() == picked.getSecondaryType()));
                    //probably could combine these into one removeIf—it would be more efficient, even—but it's not worth it.
                }
            }
        } else {

            //build type map
            Map<Type, List<Pokemon>> typeListMap = new EnumMap<>(Type.class);
            for (Type type : typeService.getTypes()) {
                typeListMap.put(type, new ArrayList<>());
            }
            for (Pokemon poke : choosable) {
                typeListMap.get(poke.getPrimaryType()).add(poke);
                if (poke.getSecondaryType() != null) {
                    typeListMap.get(poke.getSecondaryType()).add(poke);
                }
            }

            //assuming only one type restriction (not counting noDualTypes)
            //also assuming that the triangle restrictions (typeTriangle, fireWaterGrass)
            //are not used with custom starters
            if (typeTriangle) {
                Set<List<Type>> typeTriangles = findTypeTriangles();
                if (typeTriangles.isEmpty()) {
                    throw new RandomizationException("Could not find any type triangles");
                }
                // to pick randomly from
                List<List<Type>> typeTriangleList = new ArrayList<>(typeTriangles);

                int tries = 0;
                // okay, we found our triangles! now pick one and pick starters from it.
                // loop because we might find that there isn't a pokemon set of the appropriate types
                while (pickedStarters.isEmpty() && tries < MAX_TYPE_TRIANGLE_STARTER_TRIES) {
                    List<Type> triangle = typeTriangleList.get(random.nextInt(typeTriangleList.size()));
                    for (Type type : triangle) {
                        List<Pokemon> typeList = new ArrayList<>(typeListMap.get(type));
                        //clone so we can safely drain it
                        boolean noPick = true;
                        while (noPick && !typeList.isEmpty()) {
                            Pokemon picked = typeList.get(random.nextInt(typeList.size()));
                            typeList.remove(picked);
                            Type otherType;
                            if (picked.getPrimaryType() == type) {
                                otherType = picked.getSecondaryType();
                            } else {
                                otherType = picked.getPrimaryType();
                            }
                            if (!triangle.contains(otherType)) {
                                //this pokemon works
                                noPick = false;
                                pickedStarters.add(picked);
                            }
                        }
                        if (noPick) {
                            pickedStarters = new ArrayList<>();
                            break;
                        }
                    }
                    if (pickedStarters.isEmpty()) {
                        typeTriangles.remove(triangle);
                    }
                    tries++;
                }

                if (pickedStarters.isEmpty()) {
                    throw new RandomizationException("No valid starter set with a type triangle could be found within "
                            + MAX_TYPE_TRIANGLE_STARTER_TRIES + " tries!");
                }

            } else if (typeFwg) {
                //Fire
                List<Pokemon> typeList = new ArrayList<>(typeListMap.get(Type.FIRE));
                //clone so we can safely drain it
                boolean noPick = true;
                while (noPick && !typeList.isEmpty()) {
                    Pokemon picked = typeList.get(this.random.nextInt(typeList.size()));
                    typeList.remove(picked);
                    Type otherType;
                    if (picked.getPrimaryType() == Type.FIRE) {
                        otherType = picked.getSecondaryType();
                    } else {
                        otherType = picked.getPrimaryType();
                    }
                    if (otherType != Type.WATER && otherType != Type.GRASS) {
                        //this pokemon works
                        noPick = false;
                        pickedStarters.add(picked);
                    }
                }
                if (noPick) {
                    throw new RandomizationException("No valid Fire-type starter found!");
                }

                //Water
                typeList = new ArrayList<>(typeListMap.get(Type.WATER));
                //clone so we can safely drain it
                noPick = true;
                while (noPick && !typeList.isEmpty()) {
                    Pokemon picked = typeList.get(this.random.nextInt(typeList.size()));
                    typeList.remove(picked);
                    Type otherType;
                    if (picked.getPrimaryType() == Type.WATER) {
                        otherType = picked.getSecondaryType();
                    } else {
                        otherType = picked.getPrimaryType();
                    }
                    if (otherType != Type.FIRE && otherType != Type.GRASS) {
                        //this pokemon works
                        noPick = false;
                        pickedStarters.add(picked);
                    }
                }
                if (noPick) {
                    throw new RandomizationException("No valid Water-type starter found!");
                }

                //Grass
                typeList = new ArrayList<>(typeListMap.get(Type.GRASS));
                //clone so we can safely drain it
                noPick = true;
                while (noPick && !typeList.isEmpty()) {
                    Pokemon picked = typeList.get(this.random.nextInt(typeList.size()));
                    typeList.remove(picked);
                    Type otherType;
                    if (picked.getPrimaryType() == Type.GRASS) {
                        otherType = picked.getSecondaryType();
                    } else {
                        otherType = picked.getPrimaryType();
                    }
                    if (otherType != Type.FIRE && otherType != Type.WATER) {
                        //this pokemon works
                        noPick = false;
                        pickedStarters.add(picked);
                    }
                }
                if (noPick) {
                    throw new RandomizationException("No valid Grass-type starter found!");
                }

                //done.
            } else if (typeSingle) {
                int iterLoops = 0;
                while (singleType == null && iterLoops < 10000) {
                    singleType = typeService.randomType(random);
                    if (typeListMap.get(singleType).size() < (starterCount - pickedStarters.size())) {
                        singleType = null;
                    }
                    iterLoops++;
                }

                List<Pokemon> typeList = typeListMap.get(singleType);

                while (pickedStarters.size() < starterCount) {
                    Pokemon picked = typeList.get(this.random.nextInt(typeList.size()));
                    pickedStarters.add(picked);
                    typeList.remove(picked);
                    //there is no longer anything that can invalidate this pokemon
                }
            } //no other case
        }

        romHandler.setStarters(pickedStarters);
    }

    private Set<List<Type>> findTypeTriangles() {
        TypeTable typeTable = romHandler.getTypeTable();
        Set<List<Type>> typeTriangles;
        typeTriangles = new HashSet<>();
        for (Type typeOne : typeTable.getTypes()) {
            List<Type> superEffectiveOne = typeTable.superEffectiveWhenAttacking(typeOne);
            superEffectiveOne.remove(typeOne);
            //don't want a Ghost-Ghost-Ghost "triangle"
            //(although it would be funny)
            for (Type typeTwo : superEffectiveOne) {
                List<Type> superEffectiveTwo = typeTable.superEffectiveWhenAttacking(typeTwo);
                superEffectiveTwo.remove(typeOne);
                superEffectiveTwo.remove(typeTwo);
                for (Type typeThree : superEffectiveTwo) {
                    List<Type> superEffectiveThree = typeTable.superEffectiveWhenAttacking(typeThree);
                    if (superEffectiveThree.contains(typeOne)) {
                        // The below is an ArrayList because the immutable list created by List.of throws a
                        // NullPointerException when you check whether it contains null.

                        // It is "reverse" direction because it's used for starter generation,
                        // and the starter list expects type triangles to be this way
                        // (it's [Fire, Water, Grass] in vanilla)
                        List<Type> triangle = new ArrayList<>(List.of(typeThree, typeTwo, typeOne));
                        typeTriangles.add(triangle);
                    }
                }
            }
        }
        return typeTriangles;
    }

    public void randomizeStarterHeldItems() {
        boolean banBadItems = settings.isBanBadRandomStarterHeldItems();

        List<Integer> oldHeldItems = romHandler.getStarterHeldItems();
        List<Integer> newHeldItems = new ArrayList<>();
        ItemList possibleItems = banBadItems ? romHandler.getNonBadItems() : romHandler.getAllowedItems();
        for (int i = 0; i < oldHeldItems.size(); i++) {
            newHeldItems.add(possibleItems.randomItem(this.random));
        }
        romHandler.setStarterHeldItems(newHeldItems);
    }
}
