package com.dabomstew.pkrandom.randomizers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.exceptions.RandomizationException;
import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.util.*;

public class EncounterRandomizer extends Randomizer {

    public EncounterRandomizer(RomHandler romHandler, Settings settings, Random random) {
        super(romHandler, settings, random);
    }

    public void randomizeEncounters() {
        Settings.WildPokemonMod mode = settings.getWildPokemonMod();
        boolean useTimeOfDay = settings.isUseTimeBasedEncounters();
        boolean randomTypeThemes = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.THEMED_AREAS;
        boolean keepTypeThemes = settings.isKeepWildTypeThemes();
        boolean keepPrimaryType = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.KEEP_PRIMARY;
        boolean catchEmAll = settings.isCatchEmAllEncounters();
        boolean similarStrength = settings.isSimilarStrengthEncounters();
        boolean noLegendaries = settings.isBlockWildLegendaries();
        boolean balanceShakingGrass = settings.isBalanceShakingGrass();
        int levelModifier = settings.isWildLevelsModified() ? settings.getWildLevelModifier() : 0;
        boolean allowAltFormes = settings.isAllowWildAltFormes();
        boolean banIrregularAltFormes = settings.isBanIrregularAltFormes();
        boolean abilitiesAreRandomized = settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE;

        randomizeEncounters(mode, useTimeOfDay,
                randomTypeThemes, keepTypeThemes, keepPrimaryType, catchEmAll, similarStrength, noLegendaries,
                balanceShakingGrass, levelModifier, allowAltFormes, banIrregularAltFormes, abilitiesAreRandomized);
        changesMade = true;
    }

    // only exists for some old test cases, please don't use
    public void randomizeEncounters(Settings.WildPokemonMod mode, Settings.WildPokemonTypeMod typeMode,
                                    boolean useTimeOfDay,
                                    boolean catchEmAll, boolean similarStrength,
                                    boolean noLegendaries, boolean balanceShakingGrass, int levelModifier,
                                    boolean allowAltFormes, boolean banIrregularAltFormes,
                                    boolean abilitiesAreRandomized) {
        randomizeEncounters(mode,
                useTimeOfDay,
                typeMode == Settings.WildPokemonTypeMod.THEMED_AREAS,
                false,
                typeMode == Settings.WildPokemonTypeMod.KEEP_PRIMARY,
                catchEmAll, similarStrength,
                noLegendaries, balanceShakingGrass, levelModifier,
                allowAltFormes, banIrregularAltFormes,
                abilitiesAreRandomized);
    }

    // only public for some old test cases, please don't use
    public void randomizeEncounters(Settings.WildPokemonMod mode,
                                     boolean useTimeOfDay,
                                     boolean randomTypeThemes, boolean keepTypeThemes, boolean keepPrimaryType,
                                     boolean catchEmAll, boolean similarStrength,
                                     boolean noLegendaries, boolean balanceShakingGrass, int levelModifier,
                                     boolean allowAltFormes, boolean banIrregularAltFormes,
                                     boolean abilitiesAreRandomized) {
        // - prep settings
        // - get encounters
        // - setup banned + allowed
        // - randomize inner
        // - apply level modifier
        // - set encounters

        rPokeService.setRestrictions(settings);

        List<EncounterArea> encounterAreas = romHandler.getEncounters(useTimeOfDay);

        PokemonSet banned = getBannedForWildEncounters(banIrregularAltFormes, abilitiesAreRandomized);
        PokemonSet allowed = new PokemonSet(rPokeService.getPokemon(noLegendaries, allowAltFormes, false));
        allowed.removeAll(banned);



        InnerRandomizer ir = new InnerRandomizer(allowed, banned,
                randomTypeThemes, keepTypeThemes, keepPrimaryType, catchEmAll, similarStrength, balanceShakingGrass);
        switch (mode) {
            case RANDOM:
                if(romHandler.isORAS()) {
                    //this mode crashes ORAS and needs special handling to approximate
                    ir.randomEncountersORAS(encounterAreas);
                } else {
                    ir.randomEncounters(encounterAreas);
                }
                break;
            case AREA_MAPPING:
                ir.area1to1Encounters(encounterAreas);
                break;
            case LOCATION_MAPPING:
                ir.location1to1Encounters(encounterAreas);
                break;
            case GLOBAL_MAPPING:
                ir.game1to1Encounters(encounterAreas);
                break;
            case FAMILY_MAPPING:
                ir.gameFamilyToFamilyEncounters(encounterAreas);
                break;
        }

        applyLevelModifier(levelModifier, encounterAreas);
        romHandler.setEncounters(useTimeOfDay, encounterAreas);
    }

    private PokemonSet getBannedForWildEncounters(boolean banIrregularAltFormes,
                                                           boolean abilitiesAreRandomized) {
        PokemonSet banned = new PokemonSet();
        banned.addAll(romHandler.getBannedForWildEncounters());
        banned.addAll(rPokeService.getBannedFormesForPlayerPokemon());
        if (!abilitiesAreRandomized) {
            PokemonSet abilityDependentFormes = rPokeService.getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(romHandler.getIrregularFormes());
        }
        return banned;
    }

    protected void applyLevelModifier(int levelModifier, List<EncounterArea> currentEncounterAreas) {
        if (levelModifier != 0) {
            for (EncounterArea area : currentEncounterAreas) {
                for (Encounter enc : area) {
                    enc.setLevel(Math.min(100, (int) Math.round(enc.getLevel() * (1 + levelModifier / 100.0))));
                    enc.setMaxLevel(Math.min(100, (int) Math.round(enc.getMaxLevel() * (1 + levelModifier / 100.0))));
                }
            }
        }
    }

    private class InnerRandomizer {
        private final boolean randomTypeThemes;
        private final boolean keepTypeThemes;
        private final boolean keepPrimaryType;
        private final boolean catchEmAll;
        private final boolean similarStrength;
        private final boolean balanceShakingGrass;

        private boolean map1to1;
        private boolean useLocations;

        private Map<Type, PokemonSet> allowedByType;
        private final PokemonSet allowed;
        private final PokemonSet banned;

        private Map<Type, PokemonSet> remainingByType;
        private PokemonSet remaining;

        private Type areaType;
        private PokemonSet allowedForArea;
        private Map<Pokemon, Pokemon> areaMap;
        private PokemonSet allowedForReplacement;

        //ORAS's DexNav will crash if the load is higher than this value.
        final int ORAS_CRASH_THRESHOLD = 18;

        //Similar Strength will keep expanding until it reaches the smaller of
        //MINIMUM_POOL or total_pool / MINIMUM_POOL_FACTOR
        final int SS_MINIMUM_POOL = 5;
        final int SS_MINIMUM_POOL_FACTOR = 4;

        public InnerRandomizer(PokemonSet allowed, PokemonSet banned,
                               boolean randomTypeThemes, boolean keepTypeThemes, boolean keepPrimaryType,
                               boolean catchEmAll, boolean similarStrength, boolean balanceShakingGrass) {
            if ((randomTypeThemes || keepTypeThemes) && keepPrimaryType) {
                throw new IllegalArgumentException("Can't use keepPrimaryType with randomTypeThemes and/or keepTypeThemes.");
            }
            this.randomTypeThemes = randomTypeThemes;
            this.keepTypeThemes = keepTypeThemes;
            this.keepPrimaryType = keepPrimaryType;
            this.catchEmAll = catchEmAll;
            this.similarStrength = similarStrength;
            this.balanceShakingGrass = balanceShakingGrass;
            this.allowed = allowed;
            this.banned = banned;
            if (randomTypeThemes || keepTypeThemes || keepPrimaryType) {
                this.allowedByType = allowed.sortByType(false, typeService.getTypes());
            }
            if (catchEmAll) {
                refillRemainingPokemon();
            }
        }

        private void refillRemainingPokemon() {
            remaining = new PokemonSet(allowed);
            if (randomTypeThemes || keepTypeThemes || keepPrimaryType) {
                remainingByType = new EnumMap<>(Type.class);
                for (Type t : typeService.getTypes()) {
                    remainingByType.put(t, new PokemonSet(allowedByType.get(t)));
                }
            }
        }

        public void randomEncounters(List<EncounterArea> encounterAreas) {
            map1to1 = false;
            useLocations = false;
            randomEncountersInner(encounterAreas);
        }

        public void area1to1Encounters(List<EncounterArea> encounterAreas) {
            map1to1 = true;
            useLocations = false;
            randomEncountersInner(encounterAreas);
        }

        public void location1to1Encounters(List<EncounterArea> encounterAreas) {
            map1to1 = true;
            useLocations = true;
            randomEncountersInner(encounterAreas);
        }

        private void randomEncountersInner(List<EncounterArea> encounterAreas) {
            List<EncounterArea> preppedEncounterAreas = prepEncounterAreas(encounterAreas);
            for (EncounterArea area : preppedEncounterAreas) {
                areaType = pickAreaType(area);
                allowedForArea = setupAllowedForArea();

                areaMap = new TreeMap<>();

                for (Encounter enc : area) {
                    Pokemon replacement = pickReplacement(enc);
                    if (map1to1) {
                        areaMap.put(enc.getPokemon(), replacement);
                    }

                    enc.setPokemon(replacement);
                    setFormeForEncounter(enc, replacement);

                    if (catchEmAll) {
                        removeFromRemaining(replacement);
                        if (allowedForArea.isEmpty()) {
                            refillAllowedForArea();
                        }
                    }
                }
                if (area.isForceMultipleSpecies()) {
                    enforceMultipleSpecies(area);
                }
            }
        }

        /**
         * Special case to approximate random encounters in ORAS, since they crash if the
         * normal algorithm is used.
         * @param encounterAreas The list of EncounterAreas to randomize.
         */
        private void randomEncountersORAS(List<EncounterArea> encounterAreas) {

            List<EncounterArea> collapsedEncounters = flattenEncounterTypesInMaps(encounterAreas);
            Map<Integer, List<EncounterArea>> areasByMapIndex = groupAreasByMapIndex(collapsedEncounters);
            //Awkwardly, the grouping is run twice...

            //sort out Rock Smash areas
            List<EncounterArea> rockSmashAreas = new ArrayList<>();
            List<EncounterArea> nonRockSmashAreas = new ArrayList<>();
            for(Map.Entry<Integer, List<EncounterArea>> mapEntry : areasByMapIndex.entrySet()) {
                Iterator<EncounterArea> mapIterator = mapEntry.getValue().iterator();
                while(mapIterator.hasNext()) {
                    EncounterArea area = mapIterator.next();
                    if(area.getEncounterType() == EncounterArea.EncounterType.INTERACT) {
                        //rock smash is the only INTERACT type in ORAS
                        rockSmashAreas.add(area);
                        mapIterator.remove();
                    } else {
                        nonRockSmashAreas.add(area);
                    }
                }
            }

            //Rock smash is not affected by the crashing, so we can run the standard RandomEncounters on it.
            this.randomEncounters(rockSmashAreas);

            //For other areas, we start with area mapping
            this.area1to1Encounters(nonRockSmashAreas);
            //and then see how many more Pokemon we can add
            this.enhanceRandomEncountersORAS(areasByMapIndex);
            //(nonRockSmashAreas and areasByMapIndex contain all the same areas, just organized differently.)
        }

        /**
         * Given a list of areas, grouped by map index, which have already undergone area 1-to-1 mapping,
         * adds as many distinct Pokemon to each map as possible without crashing ORAS's DexNav.
         * @param areasByMapIndex The areas to randomize, grouped by map index.
         */
        private void enhanceRandomEncountersORAS(Map<Integer, List<EncounterArea>> areasByMapIndex) {
            map1to1 = false;
            useLocations = false;

            List<Integer> mapIndices = new ArrayList<>(areasByMapIndex.keySet());
            Collections.shuffle(mapIndices);

            for(int mapIndex : mapIndices) {
                List<EncounterArea> map = areasByMapIndex.get(mapIndex);

                //set up a "queue" of encounters to randomize (to ensure loop termination)
                Map<Integer, List<Encounter>> encountersToRandomize = new HashMap<>();
                for(int i = 0; i < map.size(); i++) {
                    encountersToRandomize.put(i, new ArrayList<>(map.get(i)));
                }

                while (getORASDexNavLoad(map) < ORAS_CRASH_THRESHOLD && !encountersToRandomize.isEmpty()){

                    //choose a random encounter, and remove it from the "queue"
                    int index = random.nextInt(map.size());
                    List<Encounter> encsLeftInArea = encountersToRandomize.get(index);
                    Encounter enc = encsLeftInArea.remove(random.nextInt(encsLeftInArea.size()));
                    if(encsLeftInArea.isEmpty()) {
                        encountersToRandomize.remove(index);
                    }

                    //check if there's another encounter with the same Pokemon - otherwise, this is a waste of time
                    //(And, if we're using catchEmAll, a removal of a used Pokemon, which is bad.)
                    boolean anotherExists = false;
                    for(Encounter otherEnc : encsLeftInArea) {
                        if(enc.getPokemon() == otherEnc.getPokemon()) {
                            anotherExists = true;
                            break;
                        }
                    }
                    if(!anotherExists) {
                        continue;
                    }

                    //now the standard replacement logic
                    areaType = findExistingAreaType(map.get(index));
                    allowedForArea = setupAllowedForArea();

                    Pokemon replacement = pickReplacement(enc);
                    enc.setPokemon(replacement);
                    setFormeForEncounter(enc, replacement);

                    if (catchEmAll) {
                        removeFromRemaining(replacement);
                    }
                }
            }
        }

        /**
         * Prepares the EncounterAreas for randomization by shuffling the order and flattening them if appropriate.
         * @param unprepped The List of EncounterAreas to prepare.
         * @return A new List of all the same Encounters, with the areas shuffled and possibly merged as appropriate.
         */
        private List<EncounterArea> prepEncounterAreas(List<EncounterArea> unprepped) {
            if (useLocations) {
                unprepped = flattenLocations(unprepped);
            } else if (romHandler.isORAS()) {
                //some modes crash in ORAS if the maps aren't flattened
                unprepped = flattenEncounterTypesInMaps(unprepped);
            }
            // Shuffling the EncounterAreas leads to less predictable results for various modifiers.
            // Need to keep the original ordering around for saving though.
            List<EncounterArea> prepped = new ArrayList<>(unprepped);
            Collections.shuffle(prepped, random);
            return prepped;
        }

        /**
         * Given a List of EncounterAreas, merges those that have the same Location tag.
         * @param unflattened The set of EncounterAreas to merge.
         * @return A List of EncounterAreas with the specified areas merged.
         */
        private List<EncounterArea> flattenLocations(List<EncounterArea> unflattened) {
            Map<String, List<EncounterArea>> grouped = groupAreasByLocation(unflattened);
            List<EncounterArea> flattenedLocations = new ArrayList<>();
            for (Map.Entry<String, List<EncounterArea>> locEntry : grouped.entrySet()) {
                EncounterArea flattened = new EncounterArea();
                flattened.setDisplayName("All of location " + locEntry.getKey());
                for(EncounterArea area : locEntry.getValue()) {
                    flattened.addAll(area);
                    flattened.banAllPokemon(area.getBannedPokemon());
                }
                flattenedLocations.add(flattened);
            }
            return flattenedLocations;
        }

        /**
         * Given a List of EncounterAreas, merges those that have the same map index and encounter type.
         * @param unflattened The set of EncounterAreas to merge.
         * @return A List of EncounterAreas with the specified areas merged.
         */
        private List<EncounterArea> flattenEncounterTypesInMaps(List<EncounterArea> unflattened) {
            Map<Integer, List<EncounterArea>> grouped = groupAreasByMapIndex(unflattened);
            List<EncounterArea> flattenedEncounters = new ArrayList<>();
            int unnamed = 1;
            for(Map.Entry<Integer, List<EncounterArea>> mapEntry : grouped.entrySet()) {
                Map<EncounterArea.EncounterType, List<EncounterArea>> mapGrouped =
                        groupAreasByEncounterType(mapEntry.getValue());
                String mapName = mapEntry.getValue().get(0).getLocationTag();
                if(mapName == null) {
                    mapName = "Unknown Map " + unnamed;
                    unnamed++;
                }
                for(Map.Entry<EncounterArea.EncounterType, List<EncounterArea>> typeEntry : mapGrouped.entrySet()) {
                    EncounterArea flattened = new EncounterArea();
                    flattened.setDisplayName(mapName + typeEntry.getKey().name());
                    flattened.setEncounterType(typeEntry.getKey());
                    flattened.setMapIndex(mapEntry.getKey());
                    for(EncounterArea area : typeEntry.getValue()) {
                        flattened.addAll(area);
                        flattened.banAllPokemon(area.getBannedPokemon());
                    }
                    flattenedEncounters.add(flattened);
                }
            }
            return flattenedEncounters;
        }

        /**
         * Given a List of EncounterAreas, groups those that have the same map index.
         * @param ungrouped The set of EncounterAreas to group.
         * @return A Map of mapIndexes to EncounterAreas.
         */
        private Map<Integer, List<EncounterArea>> groupAreasByMapIndex(List<EncounterArea> ungrouped) {
            Map<Integer, List<EncounterArea>> grouped = new HashMap<>();
            for (EncounterArea area : ungrouped) {
                int index = area.getMapIndex();
                if (!grouped.containsKey(index)) {
                    grouped.put(index, new ArrayList<>());
                }
                grouped.get(index).add(area);
            }
            return grouped;
        }

        /**
         * Given a List of EncounterAreas, groups those that have the same location tag.
         * @param ungrouped The set of EncounterAreas to group.
         * @return A Map of locationTags to EncounterAreas.
         */
        private Map<String, List<EncounterArea>> groupAreasByLocation(List<EncounterArea> ungrouped) {
            Map<String, List<EncounterArea>> grouped = new HashMap<>();
            int untagged = 1;
            for (EncounterArea area : ungrouped) {
                String tag = area.getLocationTag();
                if (tag == null) {
                    tag = "UNTAGGED-" + untagged;
                    untagged++;
                }
                if (!grouped.containsKey(tag)) {
                    grouped.put(tag, new ArrayList<>());
                }
                grouped.get(tag).add(area);
            }
            return grouped;
        }

        /**
         * Given a List of EncounterAreas, groups those that have the same encounter type.
         * @param ungrouped The set of EncounterAreas to group.
         * @return A Map of encounterTypes to EncounterAreas.
         */
        private Map<EncounterArea.EncounterType, List<EncounterArea>>
                        groupAreasByEncounterType(List<EncounterArea> ungrouped) {
            Map<EncounterArea.EncounterType, List<EncounterArea>> grouped = new HashMap<>();
            for (EncounterArea area : ungrouped) {
                EncounterArea.EncounterType encType = area.getEncounterType();
                if (!grouped.containsKey(encType)) {
                    grouped.put(encType, new ArrayList<>());
                }
                grouped.get(encType).add(area);
            }
            return grouped;
        }

        //should the above functions instead be in EncounterArea?

        private Type pickAreaType(EncounterArea area) {
            Type picked = null;
            if (keepTypeThemes) {
                picked = area.getPokemonInArea().getSharedType(true);
            }
            if (randomTypeThemes && picked == null) {
                picked = pickRandomAreaType();

                // Unown clause - since Unown (and other banned Pokemon) aren't randomized with catchEmAll active,
                // the "random" type theme must be one of the banned's types.
                // The implementation below supports multiple banned Pokemon of the same type in the same area,
                // because why not?
                if (catchEmAll) {
                    PokemonSet bannedInArea = new PokemonSet(banned);
                    bannedInArea.retainAll(area.getPokemonInArea());
                    Type themeOfBanned = bannedInArea.getSharedType(false);
                    if (themeOfBanned != null) {
                        picked = themeOfBanned;
                    }
                }
            }
            return picked;
        }

        private Type pickRandomAreaType() {
            Map<Type, PokemonSet> byType = catchEmAll ? remainingByType : allowedByType;
            Type areaType;
            do {
                areaType = typeService.randomType(random);
            } while (byType.get(areaType).isEmpty());
            //TODO: ensure loop terminates
            return areaType;
        }

        private Type findExistingAreaType(EncounterArea area) {
            Type areaType = null;
            if(keepTypeThemes || randomTypeThemes) {
                PokemonSet inArea = area.getPokemonInArea();
                areaType = inArea.getSharedType(false);
            }
            return areaType;
        }

        private PokemonSet setupAllowedForArea() {
            if (areaType != null) {
                return catchEmAll && !remainingByType.get(areaType).isEmpty()
                        ? remainingByType.get(areaType) : allowedByType.get(areaType);
            } else {
                return catchEmAll ? remaining : allowed;
            }

            //TODO: remove locally banned Pokemon
        }

        private Pokemon pickReplacement(Encounter enc) {
            allowedForReplacement = allowedForArea;
            if (keepPrimaryType && areaType == null) {
                allowedForReplacement = getAllowedReplacementPreservePrimaryType(enc);
            }

            if (map1to1) {
                return pickReplacement1to1(enc);
            } else {
                return pickReplacementInner(enc);
            }
        }


        private PokemonSet getAllowedReplacementPreservePrimaryType(Encounter enc) {
            //TODO: ensure this works correctly with area bans
            Pokemon current = enc.getPokemon();
            Type primaryType = current.getPrimaryType(true);
            return catchEmAll && !remainingByType.get(primaryType).isEmpty()
                    ? remainingByType.get(primaryType) : allowedByType.get(primaryType);
        }

        private Pokemon pickReplacementInner(Encounter enc) {
            if (allowedForReplacement.isEmpty()) {
                throw new IllegalStateException("No allowed Pokemon to pick as replacement.");
            }
            Pokemon current = enc.getPokemon();

            Pokemon replacement;
            // In Catch 'Em All mode, don't randomize encounters for Pokemon that are banned for
            // wild encounters. Otherwise, it may be impossible to obtain this Pokemon unless it
            // randomly appears as a static or unless it becomes a random evolution.
            if (catchEmAll && banned.contains(current)) {
                replacement = current;
            } else if (similarStrength) {
                replacement = balanceShakingGrass ?
                        pickWildPowerLvlReplacement(allowedForReplacement, current, false,
                                (enc.getLevel() + enc.getMaxLevel()) / 2) :
                        pickWildPowerLvlReplacement(allowedForReplacement, current, false,
                                100);
            } else {
                replacement = allowedForReplacement.getRandomPokemon(random);
            }
            return replacement;
        }

        private Pokemon pickReplacement1to1(Encounter enc) {
            Pokemon current = enc.getPokemon();
            if (areaMap.containsKey(current)) {
                return areaMap.get(current);
            } else {
                // the below loop ensures no two Pokemon are given the same replacement,
                // unless that's impossible due to the (small) size of allowedForArea
                Pokemon replacement;
                do {
                    replacement = pickReplacementInner(enc);
                } while (areaMap.containsValue(replacement) && areaMap.size() < allowedForArea.size());
                return replacement;
            }
        }

        private void removeFromRemaining(Pokemon replacement) {
            remaining.remove(replacement);
            if (areaType != null || keepPrimaryType) {
                remainingByType.get(replacement.getPrimaryType(false)).remove(replacement);
                if (replacement.getSecondaryType(false) != null) {
                    remainingByType.get(replacement.getSecondaryType(false)).remove(replacement);
                }
            }
        }

        private void refillAllowedForArea() {
            if (remaining.isEmpty()) {
                refillRemainingPokemon();
            }
            allowedForArea = areaType != null ? allowedByType.get(areaType) : remaining;
        }

        private void enforceMultipleSpecies(EncounterArea area) {
            // If an area with forceMultipleSpecies yet has a single species,
            // just randomly pick a different species for one of the Encounters.
            // This is very unlikely to happen in practice, even with very
            // restrictive settings, so it should be okay to break logic here.
            while (area.stream().distinct().count() == 1) {
                area.get(0).setPokemon(rPokeService.randomPokemon(random));
            }
        }

        // quite different functionally from the other random encounter methods,
        // but still grouped in this inner class due to conceptual cohesion
        public void game1to1Encounters(List<EncounterArea> encounterAreas) {
            remaining = new PokemonSet(allowed);
            Map<Pokemon, Pokemon> translateMap = new HashMap<>();

            Map<Pokemon, PokemonAreaInformation> pokemonWithAreaInfo =
                    setupAreaInfoMap(encounterAreas, null);
            //encounterAreas.forEach(area -> area.forEach(enc -> extant.add(enc.getPokemon())));
            // shuffle to not give certain Pokémon priority when picking replacements
            // matters for similar strength
            List<PokemonAreaInformation> shuffled = new ArrayList<>(pokemonWithAreaInfo.values());
            Collections.shuffle(shuffled, random);

            for (PokemonAreaInformation current : shuffled) {
                Pokemon replacement = pickGame1to1Replacement(current);
                translateMap.put(current.getPokemon(), replacement);
            }

            for (EncounterArea area : encounterAreas) {
                for (Encounter enc : area) {
                    Pokemon replacement = translateMap.get(enc.getPokemon());
                    if (area.getBannedPokemon().contains(replacement)) {
                        // This should never happen, since we already checked all the areas' banned Pokemon.
                        throw new RuntimeException("Chose a banned Pokemon " + replacement +
                                " as replacement for " + enc.getPokemon() + "?! (Shouldn't be possible)");
                    }
                    enc.setPokemon(replacement);
                    setFormeForEncounter(enc, replacement);
                }
            }
        }

        private Pokemon pickGame1to1Replacement(PokemonAreaInformation current) {
            Type theme = null;
            if(keepTypeThemes) {
                theme = current.getTheme(keepPrimaryType);
            } else if(keepPrimaryType) {
                theme = current.getPokemon().getPrimaryType(true);
            }

            PokemonSet allowedForPokemon;
            if(theme != null) {
                allowedForPokemon = remainingByType.get(theme);
            } else {
                allowedForPokemon = remaining;
            }
            allowedForPokemon.removeAll(current.getBannedForReplacement());

            Pokemon replacement = pickGame1to1ReplacementInner(current.pokemon, allowedForPokemon);
            // In case it runs out of unique Pokémon, picks something already mapped to.
            // Shouldn't happen unless restrictions are really harsh, normally [#allowed Pokémon] > [#Pokémon which appear in the wild]
            if (replacement == null) {
                allowedForPokemon = theme != null ? allowedByType.get(theme) : allowed;
                allowedForPokemon.removeAll(current.getBannedForReplacement());
                replacement = pickGame1to1ReplacementInner(current.pokemon, allowedForPokemon);
            } else {
                remaining.remove(replacement);
            }
            return replacement;
        }

        private Pokemon pickGame1to1ReplacementInner(Pokemon pokemon, PokemonSet pool) {
            return similarStrength ?
                    pickWildPowerLvlReplacement(pool, pokemon, true, 100) :
                    pool.getRandomPokemon(random);
        }

        /**
         * Chooses a Pokemon similar in power level to the given Pokemon.
         * @param pokemonPool The set of Pokemon to choose from.
         * @param current The Pokemon to match the power level of.
         * @param banSamePokemon Whether to disallow choosing the Pokemon the power level is matching to.
         *                       Ignored if this is the only Pokemon in the pool.
         * @param bstBalanceLevel A factor used to calculate a maximum BST using the formula 10 * level + 250.
         *                        If the given Pokemon's BST is higher than the calculated value, looks for Pokemon
         *                        of strength similar to the calculated value instead.
         * @return A Pokemon of similar strength to the given Pokemon.
         */
        private Pokemon pickWildPowerLvlReplacement(PokemonSet pokemonPool, Pokemon current, boolean banSamePokemon,
                                                    int bstBalanceLevel) {
            PokemonSet availablePool = new PokemonSet(pokemonPool); //clone for draining
            if(banSamePokemon) {
                availablePool.remove(current);
            }

            if(availablePool.isEmpty()) {
                if(pokemonPool.isEmpty()) {
                    throw new RandomizationException("Attempted to choose a wild Pokemon from an empty set!");
                } else {
                    //if availablePool is empty, but pokemonPool isn't, the current Pokemon must be the only one.
                    return current;
                }
            }

            int minimumPool = Math.min(SS_MINIMUM_POOL, pokemonPool.size() / SS_MINIMUM_POOL_FACTOR);
            if(minimumPool < 1) {
                minimumPool = 1;
            }
            if (minimumPool >= availablePool.size()) {
                //must use the whole pool
                //(I think this only happens if there's exactly one Pokemon to choose.)
                return availablePool.getRandomPokemon(random);
            }

            // start with within 10% and add 5% either direction until the pool is big enough
            int balancedBST = bstBalanceLevel * 10 + 250;
            int currentBST = Math.min(current.bstForPowerLevels(), balancedBST);
            int minTarget = currentBST - currentBST / 10;
            int maxTarget = currentBST + currentBST / 10;
            PokemonSet canPick = new PokemonSet();
            while (canPick.size() < minimumPool) {
                Iterator<Pokemon> itor = availablePool.iterator();
                while (itor.hasNext()) {
                    Pokemon poke = itor.next();
                    if(poke.bstForPowerLevels() >= minTarget && poke.bstForPowerLevels() <= maxTarget) {
                        canPick.add(poke);
                        itor.remove();
                    }
                }
                minTarget -= currentBST / 20;
                maxTarget += currentBST / 20;
            }
            return canPick.getRandomPokemon(random);
        }

        /**
         * Given the EncounterAreas for a single map, calculates the DexNav load for that map.
         * The DexNav crashes if this load is above ORAS_CRASH_THRESHOLD.
         * @param areasInMap A List of EncounterAreas, all of which are from the same map.
         * @return The DexNav load for that map.
         */
        private int getORASDexNavLoad(List<EncounterArea> areasInMap) {
            //If the previous implementation is to be believed,
            //the load is equal to the number of distinct Pokemon in each area summed.
            //(Not the total number of unique Pokemon).
            //I am not going to attempt to verify this (yet).
            int load = 0;
            for(EncounterArea area : areasInMap) {
                if(area.getEncounterType() == EncounterArea.EncounterType.INTERACT) {
                    //Rock Smash doesn't contribute to DexNav load.
                    continue;
                }

                PokemonSet pokemonInArea = new PokemonSet();
                for (Pokemon poke : area.getPokemonInArea()) {
                    //Different formes of the same Pokemon do not contribute to load
                    if(poke.isBaseForme()) {
                        pokemonInArea.add(poke);
                    } else {
                        pokemonInArea.add(poke.getBaseForme());
                    }
                }

                load += pokemonInArea.size();
            }
            return load;
        }

        /**
         * Given a set of EncounterAreas, creates a map of every Pokemon in the areas to
         * information about the areas that Pokemon is contained in.
         * If addAllPokemonTo is not null, also adds every Pokemon to it.
         * @param areas The list of EncounterAreas to explore.
         * @param addAllPokemonTo A PokemonSet to add every Pokemon found to. Can be null.
         * @return A new Map containing information for every Pokemon in the areas.
         */
        private Map<Pokemon, PokemonAreaInformation> setupAreaInfoMap(List<EncounterArea> areas,
                                                                      PokemonSet addAllPokemonTo) {
            //TODO: flatten to encounter types in maps (for improved theme detection)
            //(Need that information to be consistently populated first.)

            Map<Pokemon, PokemonAreaInformation> areaInfoMap = new HashMap<>();
            for(EncounterArea area : areas) {
                Type areaTheme = pickAreaType(area);

                if(area.getPokemonInArea().size() <= 1) {
                    //a temporary measure to stop swarms from polluting the type pool.
                    //TODO: remove this if block after adding flattening
                    areaTheme = null;
                }

                for(Pokemon pokemon : area.getPokemonInArea()) {
                    PokemonAreaInformation info = areaInfoMap.get(pokemon);
                    if(info == null) {
                        info = new PokemonAreaInformation(pokemon);
                        areaInfoMap.put(pokemon, info);
                    }
                    info.addTypeTheme(areaTheme);
                    info.banAll(area.getBannedPokemon());

                    if(addAllPokemonTo != null) {
                        addAllPokemonTo.add(pokemon);
                    }
                }
            }
            return areaInfoMap;
        }

        /**
         * A class which stores some information about the areas a Pokemon was found in,
         * in order to allow us to use this information later.
         */
        private class PokemonAreaInformation {
            private Set<Type> possibleThemes;
            private PokemonSet bannedForReplacement;
            private Pokemon pokemon;

            /**
             * Creates a new RandomizationInformation with the given data.
             * @param pk The Pokemon this RandomizationInformation is about.
             */
            PokemonAreaInformation(Pokemon pk) {
                possibleThemes = EnumSet.noneOf(Type.class);
                bannedForReplacement = new PokemonSet();
                pokemon = pk;
            }

            /**
             * Adds all Pokemon in the given collection to the set of Pokemon banned for replacement.
             * @param banned The Collection of Pokemon to add.
             */
            public void banAll(Collection<Pokemon> banned) {
                bannedForReplacement.addAll(banned);
            }

            /**
             * Get the list of all Pokemon banned as replacements for this Pokemon.
             * @return A new unmodifiable {@link PokemonSet} containing the banned Pokemon.
             */
            public PokemonSet getBannedForReplacement() {
                return PokemonSet.unmodifiable(bannedForReplacement);
            }

            /**
             * Adds the given type to the list of possible type themes for this Pokemon's replacement.
             * If the given type is null, has no effect.
             * @param theme The type to add.
             */
            public void addTypeTheme(Type theme) {
                if(theme != null) {
                    possibleThemes.add(theme);
                }
            }

            /**
             * Gets the type of this Pokemon's area theming.
             * If there are two themes, it will always default to the original primary type.
             * If there are no themes, it will default to the original primary only if defaultToPrimary is true;
             * otherwise, it will default to null.
             * @param defaultToPrimary Whether the type should default to the Pokemon's primary type
             *                         if there are no themes.
             * @return The type that should be used, or null for any type.
             */
            Type getTheme(boolean defaultToPrimary) {
                int themeCount = possibleThemes.size();
                if(themeCount == 0) {
                    if(defaultToPrimary) {
                        return pokemon.getPrimaryType(true);
                    } else {
                        return null;
                    }
                } else if(themeCount == 1) {
                    return possibleThemes.iterator().next();
                } else if(themeCount == 2) {
                    return pokemon.getPrimaryType(true);
                } else {
                    throw new IllegalStateException("Too many themes for one Pokemon!");
                }
            }

            /**
             * Gets the Pokemon that this PokemonAreaInformation is about.
             * @return The Pokemon.
             */
            public Pokemon getPokemon() {
                return pokemon;
            }
        }
    }

    private void setFormeForEncounter(Encounter enc, Pokemon pk) {
        boolean checkCosmetics = true;
        enc.setFormeNumber(0);
        if (enc.getPokemon().getFormeNumber() > 0) {
            enc.setFormeNumber(enc.getPokemon().getFormeNumber());
            enc.setPokemon(enc.getPokemon().getBaseForme());
            checkCosmetics = false;
        }
        if (checkCosmetics && enc.getPokemon().getCosmeticForms() > 0) {
            enc.setFormeNumber(enc.getPokemon().getCosmeticFormNumber(this.random.nextInt(enc.getPokemon().getCosmeticForms())));
        } else if (!checkCosmetics && pk.getCosmeticForms() > 0) {
            enc.setFormeNumber(enc.getFormeNumber() + pk.getCosmeticFormNumber(this.random.nextInt(pk.getCosmeticForms())));
        }
    }

    public void changeCatchRates() {
        int minimumCatchRateLevel = settings.getMinimumCatchRateLevel();

        if (minimumCatchRateLevel == 5) {
            romHandler.enableGuaranteedPokemonCatching();
        } else {
            int normalMin, legendaryMin;
            switch (minimumCatchRateLevel) {
                case 1:
                default:
                    normalMin = 75;
                    legendaryMin = 37;
                    break;
                case 2:
                    normalMin = 128;
                    legendaryMin = 64;
                    break;
                case 3:
                    normalMin = 200;
                    legendaryMin = 100;
                    break;
                case 4:
                    normalMin = legendaryMin = 255;
                    break;
            }
            minimumCatchRate(normalMin, legendaryMin);
        }
    }

    private void minimumCatchRate(int rateNonLegendary, int rateLegendary) {
        for (Pokemon pk : romHandler.getPokemonSetInclFormes()) {
            int minCatchRate = pk.isLegendary() ? rateLegendary : rateNonLegendary;
            pk.setCatchRate(Math.max(pk.getCatchRate(), minCatchRate));
        }

    }

}
