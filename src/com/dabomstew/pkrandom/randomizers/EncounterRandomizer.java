package com.dabomstew.pkrandom.randomizers;

import com.dabomstew.pkrandom.Settings;
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
        // TODO: - do something special in ORAS, the code needed is found in old commits of AbstractRomHandler
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
                    //this mode crashes ORAS and needs special handling
                    ir.area1to1Encounters(encounterAreas);
                    ir.enhanceEncountersORAS(encounterAreas);
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
                locEntry.getValue().forEach(flattened::addAll);
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
            for(List<EncounterArea> map : grouped.values()) {
                Map<EncounterArea.EncounterType, List<EncounterArea>> mapGrouped =
                        groupAreasByEncounterType(map);
                String mapName = map.get(0).getLocationTag();
                if(mapName == null) {
                    mapName = "Unknown Map " + unnamed;
                    unnamed++;
                }
                for(Map.Entry<EncounterArea.EncounterType, List<EncounterArea>> typeEntry : mapGrouped.entrySet()) {
                    EncounterArea flattened = new EncounterArea();
                    flattened.setDisplayName(mapName + typeEntry.getKey().name());
                    typeEntry.getValue().forEach(flattened::addAll);
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

        private PokemonSet setupAllowedForArea() {
            if (areaType != null) {
                return catchEmAll && !remainingByType.get(areaType).isEmpty()
                        ? remainingByType.get(areaType) : allowedByType.get(areaType);
            } else {
                return catchEmAll ? remaining : allowed;
            }
        }

        private Pokemon pickReplacement(Encounter enc) {
            allowedForReplacement = allowedForArea;
            if (keepPrimaryType) {
                //TODO: have keepThemes override keepPrimary rather than the opposite
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

            PokemonSet extant = new PokemonSet();
            encounterAreas.forEach(area -> area.forEach(enc -> extant.add(enc.getPokemon())));
            // shuffle to not give certain Pokémon priority when picking replacements
            // matters for similar strength
            List<Pokemon> shuffled = new ArrayList<>(extant);
            Collections.shuffle(shuffled, random);

            for (Pokemon current : shuffled) {
                Pokemon replacement = pickGame1to1Replacement(current);
                translateMap.put(current, replacement);
            }

            for (EncounterArea area : encounterAreas) {
                for (Encounter enc : area) {
                    Pokemon replacement = translateMap.get(enc.getPokemon());
                    enc.setPokemon(replacement);
                    setFormeForEncounter(enc, replacement);
                }
            }
        }

        private Pokemon pickGame1to1Replacement(Pokemon current) {
            Pokemon replacement = similarStrength ?
                    pickWildPowerLvlReplacement(remaining, current, true, 100) :
                    remaining.getRandomPokemon(random);
            // In case it runs out of unique Pokémon, picks something already mapped to.
            // Shouldn't happen unless restrictions are really harsh, normally [#allowed Pokémon] > [#Pokémon which appear in the wild]
            if (replacement == null) {
                replacement = allowed.getRandomPokemon(random);
            } else {
                remaining.remove(replacement);
            }
            return replacement;
        }

        private Pokemon pickWildPowerLvlReplacement(PokemonSet pokemonPool, Pokemon current, boolean banSamePokemon,
                                                    int bstBalanceLevel) {
            // start with within 10% and add 5% either direction till we find
            // something
            int balancedBST = bstBalanceLevel * 10 + 250;
            int currentBST = Math.min(current.bstForPowerLevels(), balancedBST);
            int minTarget = currentBST - currentBST / 10;
            int maxTarget = currentBST + currentBST / 10;
            PokemonSet canPick = new PokemonSet();
            int expandRounds = 0;
            while (canPick.isEmpty() || (canPick.size() < 3 && expandRounds < 3)) {
                for (Pokemon pk : pokemonPool) {
                    if (pk.bstForPowerLevels() >= minTarget && pk.bstForPowerLevels() <= maxTarget
                            && (!banSamePokemon || pk != current)
                            && !canPick.contains(pk)) {
                        canPick.add(pk);
                    }
                }
                minTarget -= currentBST / 20;
                maxTarget += currentBST / 20;
                expandRounds++;
            }
            return canPick.getRandomPokemon(random);
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
