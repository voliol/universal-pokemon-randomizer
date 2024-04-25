package com.dabomstew.pkrandom.randomizers;

import com.dabomstew.pkrandom.MiscTweak;
import com.dabomstew.pkrandom.RomFunctions;
import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.constants.Abilities;
import com.dabomstew.pkrandom.constants.GlobalConstants;
import com.dabomstew.pkrandom.exceptions.RandomizationException;
import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.romhandlers.RomHandler;
import com.dabomstew.pkrandom.services.RestrictedPokemonService;
import com.dabomstew.pkrandom.services.TypeService;

import java.util.*;
import java.util.stream.Collectors;

public class TrainerPokemonRandomizer {

    private final RomHandler romHandler;
    private final RestrictedPokemonService rPokeService;
    private final TypeService typeService;
    private final Settings settings;
    private final Random random;

    private Map<Type, PokemonSet<Pokemon>> cachedReplacements;
    private PokemonSet<Pokemon> cachedAll;
    private PokemonSet<Pokemon> banned = new PokemonSet<>();
    private final PokemonSet<Pokemon> usedAsUnique = new PokemonSet<>();

    private Map<Type, Integer> typeWeightings;
    private int totalTypeWeighting;

    private final Map<Pokemon, Integer> placementHistory = new HashMap<>();

    private int fullyEvolvedRandomSeed = -1;

    private Map<Integer, List<MoveLearnt>> allLevelUpMoves;
    private Map<Integer, List<Integer>> allEggMoves;
    private Map<Pokemon, boolean[]> allTMCompat, allTutorCompat;
    private List<Integer> allTMMoves, allTutorMoves;

    public TrainerPokemonRandomizer(RomHandler romHandler, Settings settings, Random random) {
        this.romHandler = romHandler;
        this.rPokeService = romHandler.getRestrictedPokemonService();
        this.typeService = romHandler.getTypeService();
        this.settings = settings;
        this.random = random;
    }

    public void onlyChangeTrainerLevels() {
        int levelModifier = settings.getTrainersLevelModifier();

        List<Trainer> currentTrainers = romHandler.getTrainers();
        for (Trainer t : currentTrainers) {
            applyLevelModifierToTrainerPokemon(t, levelModifier);
        }
        romHandler.setTrainers(currentTrainers);
    }

    public void randomizeTrainerPokes() {
        boolean usePowerLevels = settings.isTrainersUsePokemonOfSimilarStrength();
        boolean weightByFrequency = settings.isTrainersMatchTypingDistribution();
        boolean useLocalPokemon = settings.isTrainersUseLocalPokemon();
        boolean noLegendaries = settings.isTrainersBlockLegendaries();
        boolean noEarlyWonderGuard = settings.isTrainersBlockEarlyWonderGuard();
        int levelModifier = settings.isTrainersLevelModified() ? settings.getTrainersLevelModifier() : 0;
        boolean isTypeThemed = settings.getTrainersMod() == Settings.TrainersMod.TYPE_THEMED;
        boolean isTypeThemedEliteFourGymOnly = settings.getTrainersMod() == Settings.TrainersMod.TYPE_THEMED_ELITE4_GYMS;
        boolean keepTypeThemes = settings.getTrainersMod() == Settings.TrainersMod.KEEP_THEMED;
        boolean distributionSetting = settings.getTrainersMod() == Settings.TrainersMod.DISTRIBUTED;
        boolean mainPlaythroughSetting = settings.getTrainersMod() == Settings.TrainersMod.MAINPLAYTHROUGH;
        boolean includeFormes = settings.isAllowTrainerAlternateFormes();
        boolean banIrregularAltFormes = settings.isBanIrregularAltFormes();
        boolean swapMegaEvos = settings.isSwapTrainerMegaEvos();
        boolean shinyChance = settings.isShinyChance();
        boolean abilitiesAreRandomized = settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE;
        int eliteFourUniquePokemonNumber = settings.getEliteFourUniquePokemonNumber();
        boolean forceFullyEvolved = settings.isTrainersForceFullyEvolved();
        int forceFullyEvolvedLevel = settings.getTrainersForceFullyEvolvedLevel();
        boolean forceChallengeMode = (settings.getCurrentMiscTweaks() & MiscTweak.FORCE_CHALLENGE_MODE.getValue()) > 0;
        boolean rivalCarriesStarter = settings.isRivalCarriesStarterThroughout();

        // Set up Pokemon pool
        cachedReplacements = new TreeMap<>();
        cachedAll = rPokeService.getPokemon(noLegendaries, includeFormes, false);

        if (useLocalPokemon) {
            PokemonSet<Pokemon> localWithRelatives = new PokemonSet<>();
            for (Pokemon pk : romHandler.getMainGameWildPokemon(settings.isUseTimeBasedEncounters())) {
                if (!localWithRelatives.contains(pk)) {
                    localWithRelatives.addAll(PokemonSet.related(pk));
                }
            }
            cachedAll.retainAll(localWithRelatives);
        }

        banned = romHandler.getBannedFormesForTrainerPokemon();
        if (!abilitiesAreRandomized) {
            PokemonSet<Pokemon> abilityDependentFormes = rPokeService.getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(romHandler.getIrregularFormes());
        }
        cachedAll.removeAll(banned);

        List<Trainer> currentTrainers = romHandler.getTrainers();

        // Type Themed related
        Map<Trainer, Type> trainerTypes = new TreeMap<>();
        Set<Type> usedUberTypes = new TreeSet<>();
        if (isTypeThemed || isTypeThemedEliteFourGymOnly) {
            typeWeightings = new TreeMap<>();
            totalTypeWeighting = 0;
            // Construct groupings for types
            // Anything starting with GYM or ELITE or CHAMPION is a group
            Map<String, List<Trainer>> groups = new TreeMap<>();
            for (Trainer t : currentTrainers) {
                if (t.tag != null && t.tag.equals("IRIVAL")) {
                    // This is the first rival in Yellow. His Pokemon is used to determine the non-player
                    // starter, so we can't change it here. Just skip it.
                    continue;
                }
                String group = t.tag == null ? "" : t.tag;
                if (group.contains("-")) {
                    group = group.substring(0, group.indexOf('-'));
                }
                if (group.startsWith("GYM") || group.startsWith("ELITE") ||
                        ((group.startsWith("CHAMPION") || group.startsWith("THEMED")) && !isTypeThemedEliteFourGymOnly)) {
                    // Yep this is a group
                    if (!groups.containsKey(group)) {
                        groups.put(group, new ArrayList<>());
                    }
                    groups.get(group).add(t);
                } else if (group.startsWith("GIO")) {
                    // Giovanni has same grouping as his gym, gym 8
                    if (!groups.containsKey("GYM8")) {
                        groups.put("GYM8", new ArrayList<>());
                    }
                    groups.get("GYM8").add(t);
                }
            }

            // Give a type to each group
            // Gym & elite types have to be unique
            // So do uber types, including the type we pick for champion
            Set<Type> usedGymTypes = new TreeSet<>();
            Set<Type> usedEliteTypes = new TreeSet<>();
            for (String group : groups.keySet()) {
                List<Trainer> trainersInGroup = groups.get(group);
                // Shuffle ordering within group to promote randomness
                Collections.shuffle(trainersInGroup, random);
                Type typeForGroup = pickType(weightByFrequency, noLegendaries, includeFormes);
                if (group.startsWith("GYM")) {
                    while (usedGymTypes.contains(typeForGroup)) {
                        typeForGroup = pickType(weightByFrequency, noLegendaries, includeFormes);
                    }
                    usedGymTypes.add(typeForGroup);
                }
                if (group.startsWith("ELITE")) {
                    while (usedEliteTypes.contains(typeForGroup)) {
                        typeForGroup = pickType(weightByFrequency, noLegendaries, includeFormes);
                    }
                    usedEliteTypes.add(typeForGroup);
                }
                if (group.equals("CHAMPION")) {
                    usedUberTypes.add(typeForGroup);
                }

                for (Trainer t : trainersInGroup) {
                    trainerTypes.put(t, typeForGroup);
                }
            }
        }

        // Randomize the order trainers are randomized in.
        // Leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<Trainer> scrambledTrainers = new ArrayList<>(currentTrainers);
        Collections.shuffle(scrambledTrainers, random);

        // Elite Four Unique Pokemon related
        boolean eliteFourUniquePokemon = eliteFourUniquePokemonNumber > 0;
        PokemonSet<Pokemon> illegalIfEvolved = new PokemonSet<>();
        PokemonSet<Pokemon> bannedFromUnique = new PokemonSet<>();
        boolean illegalEvoChains = false;
        List<Integer> eliteFourIndices = romHandler.getEliteFourTrainers(forceChallengeMode);
        PokemonSet<Pokemon> eliteFourExceptions = null;
        PokemonSet<Pokemon> nonEliteFourExceptions = null;
        if (eliteFourUniquePokemon) {
            // Sort Elite Four Trainers to the start of the list
            scrambledTrainers.sort((t1, t2) ->
                    Boolean.compare(eliteFourIndices.contains(currentTrainers.indexOf(t2) + 1), eliteFourIndices.contains(currentTrainers.indexOf(t1) + 1)));
            illegalEvoChains = forceFullyEvolved;
            if (rivalCarriesStarter) {
                List<Pokemon> starterList = romHandler.getStarters().subList(0, 3);
                for (Pokemon starter : starterList) {
                    // If rival/friend carries starter, the starters cannot be set as unique
                    bannedFromUnique.add(starter);
                    setEvoChainAsIllegal(starter, bannedFromUnique, true);

                    // If the final boss is a rival/friend, the fully evolved starters will be unique
                    if (romHandler.hasRivalFinalBattle()) {
                        cachedAll.removeAll(getFinalEvos(starter));
                        if (illegalEvoChains) {
                            illegalIfEvolved.add(starter);
                            setEvoChainAsIllegal(starter, illegalIfEvolved, true);
                        }
                    }
                }
            }
            if (useLocalPokemon) {
                //elite four unique pokemon are excepted from local requirement
                //and in fact, non-local pokemon should be chosen first
                eliteFourExceptions = rPokeService.getPokemon(noLegendaries, includeFormes, false);
                eliteFourExceptions.removeAll(banned);
                eliteFourExceptions.removeAll(cachedAll); // i.e. retains only non-local pokes

                nonEliteFourExceptions = cachedAll;
            }
        }

        List<Integer> mainPlaythroughTrainers = romHandler.getMainPlaythroughTrainers();

        // Randomize Trainer Pokemon
        // The result after this is done will not be final if "Force Fully Evolved" or "Rival Carries Starter"
        // are used, as they are applied later
        for (Trainer t : scrambledTrainers) {
            applyLevelModifierToTrainerPokemon(t, levelModifier);
            if (t.tag != null && t.tag.equals("IRIVAL")) {
                // This is the first rival in Yellow. His Pokemon is used to determine the non-player
                // starter, so we can't change it here. Just skip it.
                continue;
            }

            // If type themed, give a type to each unassigned trainer
            Type typeForTrainer = trainerTypes.get(t);
            if (typeForTrainer == null && isTypeThemed) {
                typeForTrainer = pickType(weightByFrequency, noLegendaries, includeFormes);
                // Ubers: can't have the same type as each other
                if (t.tag != null && t.tag.equals("UBER")) {
                    while (usedUberTypes.contains(typeForTrainer)) {
                        typeForTrainer = pickType(weightByFrequency, noLegendaries, includeFormes);
                    }
                    usedUberTypes.add(typeForTrainer);
                }
            }

            PokemonSet<Pokemon> evolvesIntoTheWrongType = new PokemonSet<>();
            if (typeForTrainer != null) {
                PokemonSet<Pokemon> pokemonOfType = rPokeService.getPokemon(noLegendaries, includeFormes, false)
                        .filterByType(typeForTrainer);
                for (Pokemon pk : pokemonOfType) {
                    if (!pokemonOfType.contains(fullyEvolve(pk, t.index))) {
                        evolvesIntoTheWrongType.add(pk);
                    }
                }
            }

            List<TrainerPokemon> trainerPokemonList = new ArrayList<>(t.pokemon);

            // Elite Four Unique Pokemon related
            boolean eliteFourTrackPokemon = false;
            boolean eliteFourRival = false;
            if (eliteFourUniquePokemon && eliteFourIndices.contains(t.index)) {
                eliteFourTrackPokemon = true;

                // Sort Pokemon list back to front, and then put highest level Pokemon first
                // (Only while randomizing, does not affect order in game)
                Collections.reverse(trainerPokemonList);
                trainerPokemonList.sort((tp1, tp2) -> Integer.compare(tp2.level, tp1.level));
                if (rivalCarriesStarter && (t.tag.contains("RIVAL") || t.tag.contains("FRIEND"))) {
                    eliteFourRival = true;
                }
            }

            if (keepTypeThemes) {
                PokemonSet<Pokemon> trainerPokemonSpecies = trainerPokemonList.stream().map(tp -> tp.pokemon)
                        .collect(Collectors.toCollection(PokemonSet::new));
                typeForTrainer = trainerPokemonSpecies.getOriginalTypeTheme();
            }

            for (TrainerPokemon tp : trainerPokemonList) {
                boolean swapThisMegaEvo = swapMegaEvos && tp.canMegaEvolve();
                boolean wgAllowed = (!noEarlyWonderGuard) || tp.level >= 20;
                boolean eliteFourSetUniquePokemon =
                        eliteFourTrackPokemon && eliteFourUniquePokemonNumber > trainerPokemonList.indexOf(tp);
                boolean willForceEvolve = forceFullyEvolved && tp.level >= forceFullyEvolvedLevel;

                Pokemon oldPK = tp.pokemon;
                if (tp.forme > 0) {
                    oldPK = romHandler.getAltFormeOfPokemon(oldPK, tp.forme);
                }

                banned = new PokemonSet<>(usedAsUnique);
                if (illegalEvoChains && willForceEvolve) {
                    banned.addAll(illegalIfEvolved);
                }
                if (eliteFourSetUniquePokemon) {
                    banned.addAll(bannedFromUnique);
                    if (useLocalPokemon) {
                        cachedAll = eliteFourExceptions;
                        banned.addAll(nonEliteFourExceptions);
                    }
                }
                if (willForceEvolve) {
                    banned.addAll(evolvesIntoTheWrongType);
                }

                Pokemon newPK = pickTrainerPokeReplacement(
                        oldPK,
                        usePowerLevels,
                        typeForTrainer,
                        noLegendaries,
                        wgAllowed,
                        distributionSetting || (mainPlaythroughSetting && mainPlaythroughTrainers.contains(t.index)),
                        swapThisMegaEvo,
                        abilitiesAreRandomized,
                        includeFormes,
                        banIrregularAltFormes
                );

                // Chosen Pokemon is locked in past here
                if (distributionSetting || (mainPlaythroughSetting && mainPlaythroughTrainers.contains(t.index))) {
                    setPlacementHistory(newPK);
                }
                tp.pokemon = newPK;
                setFormeForTrainerPokemon(tp, newPK);
                tp.abilitySlot = getRandomAbilitySlot(newPK);
                tp.resetMoves = true;

                if (!eliteFourRival) {
                    if (eliteFourSetUniquePokemon) {
                        PokemonSet<Pokemon> actualPKList;
                        if (willForceEvolve) {
                            actualPKList = getFinalEvos(newPK);
                        } else {
                            actualPKList = new PokemonSet<>();
                            actualPKList.add(newPK);
                        }
                        // If the unique Pokemon will evolve, we have to set all its potential evolutions as unique
                        for (Pokemon actualPK : actualPKList) {
                            usedAsUnique.add(actualPK);
                            if (illegalEvoChains) {
                                setEvoChainAsIllegal(actualPK, illegalIfEvolved, willForceEvolve);
                            }
                        }

                        if (useLocalPokemon) {
                            // return to normal list
                            cachedAll = nonEliteFourExceptions;
                        }
                    }
                    if (eliteFourTrackPokemon) {
                        bannedFromUnique.add(newPK);
                        if (illegalEvoChains) {
                            setEvoChainAsIllegal(newPK, bannedFromUnique, willForceEvolve);
                        }
                    }
                } else {
                    // If the champion is a rival, the first Pokemon will be skipped - it's already
                    // set as unique since it's a starter
                    eliteFourRival = false;
                }

                if (swapThisMegaEvo) {
                    tp.heldItem = newPK
                            .getMegaEvolutionsFrom()
                            .get(random.nextInt(newPK.getMegaEvolutionsFrom().size()))
                            .argument;
                }

                if (shinyChance) {
                    if (random.nextInt(256) == 0) {
                        tp.IVs |= (1 << 30);
                    }
                }
            }
        }

        // Save it all up
        romHandler.setTrainers(currentTrainers);
    }


    private Pokemon pickTrainerPokeReplacement(Pokemon current, boolean usePowerLevels, Type type,
                                               boolean noLegendaries, boolean wonderGuardAllowed,
                                               boolean usePlacementHistory, boolean swapMegaEvos,
                                               boolean abilitiesAreRandomized, boolean allowAltFormes,
                                               boolean banIrregularAltFormes) {
        PokemonSet<Pokemon> pickFrom;
        PokemonSet<Pokemon> withoutBannedPokemon;

        if (swapMegaEvos) {
            pickFrom = rPokeService.getMegaEvolutions()
                    .stream()
                    .filter(mega -> mega.method == 1)
                    .map(mega -> mega.from)
                    .collect(Collectors.toCollection(PokemonSet::new));
        } else {
            pickFrom = cachedAll;
        }

        if (usePlacementHistory) {
            // "Distributed" settings
            double placementAverage = getPlacementAverage();
            pickFrom = pickFrom.filter(pk -> getPlacementHistory(pk) < placementAverage * 2);
            if (pickFrom.isEmpty()) {
                pickFrom = cachedAll;
            }
        } else if (type != null && cachedReplacements != null) {
            // "Type Themed" settings
            if (!cachedReplacements.containsKey(type)) {
                PokemonSet<Pokemon> pokemonOfType = rPokeService.getPokemon(noLegendaries, allowAltFormes, false)
                        .filterByType(type);
                pokemonOfType.removeAll(rPokeService.getBannedFormesForPlayerPokemon());
                if (!abilitiesAreRandomized) {
                    PokemonSet<Pokemon> abilityDependentFormes = rPokeService.getAbilityDependentFormes();
                    pokemonOfType.removeAll(abilityDependentFormes);
                }
                if (banIrregularAltFormes) {
                    romHandler.getIrregularFormes().forEach(pokemonOfType::remove);
                }
                cachedReplacements.put(type, pokemonOfType);
            }
            if (swapMegaEvos) {
                pickFrom = cachedReplacements.get(type).filter(pickFrom::contains);
                if (pickFrom.isEmpty()) {
                    pickFrom = cachedReplacements.get(type);
                }
            } else {
                pickFrom = cachedReplacements.get(type);
            }
        }

        withoutBannedPokemon = pickFrom.filter(pk -> !banned.contains(pk));
        if (!withoutBannedPokemon.isEmpty()) {
            pickFrom = withoutBannedPokemon;
        }

        if (usePowerLevels) {
            // start with within 10% and add 5% either direction till we find
            // something
            int currentBST = current.bstForPowerLevels();
            int minTarget = currentBST - currentBST / 10;
            int maxTarget = currentBST + currentBST / 10;
            PokemonSet<Pokemon> canPick = new PokemonSet<>();
            int expandRounds = 0;
            while (canPick.isEmpty() || (canPick.size() < 3 && expandRounds < 2)) {
                for (Pokemon pk : pickFrom) {
                    if (pk.bstForPowerLevels() >= minTarget
                            && pk.bstForPowerLevels() <= maxTarget
                            && (wonderGuardAllowed || (pk.getAbility1() != Abilities.wonderGuard
                            && pk.getAbility2() != Abilities.wonderGuard && pk.getAbility3() != Abilities.wonderGuard))) {
                        canPick.add(pk);
                    }
                }
                minTarget -= currentBST / 20;
                maxTarget += currentBST / 20;
                expandRounds++;
            }
            // If usePlacementHistory is True, then we need to do some
            // extra checking to make sure the randomly chosen pokemon
            // is actually below the current average placement
            // if not, re-roll

            Pokemon chosenPokemon = canPick.getRandom(random);
            if (usePlacementHistory) {
                double placementAverage = getPlacementAverage();
                PokemonSet<Pokemon> filteredPickList = canPick.filter(pk -> getPlacementHistory(pk) < placementAverage);
                if (filteredPickList.isEmpty()) {
                    filteredPickList = canPick;
                }
                chosenPokemon = filteredPickList.getRandom(random);
            }
            return chosenPokemon;
        } else {
            if (wonderGuardAllowed) {
                return pickFrom.getRandom(random);
            } else {
                Pokemon pk = pickFrom.getRandom(random);
                while (pk.getAbility1() == Abilities.wonderGuard
                        || pk.getAbility2() == Abilities.wonderGuard
                        || pk.getAbility3() == Abilities.wonderGuard) {
                    pk = pickFrom.getRandom(random);
                }
                return pk;
            }
        }
    }

    /**
     * Picks a type, sometimes based on frequency of non-banned Pokémon of that type. Compare with randomType().
     * Never picks a type with no non-banned Pokémon, even when weightByFrequency == false.
     */
    private Type pickType(boolean weightByFrequency, boolean noLegendaries, boolean allowAltFormes) {
        if (totalTypeWeighting == 0) {
            initTypeWeightings(noLegendaries, allowAltFormes);
        }

        if (weightByFrequency) {
            int typePick = random.nextInt(totalTypeWeighting);
            int typePos = 0;
            for (Type t : typeWeightings.keySet()) {
                int weight = typeWeightings.get(t);
                if (typePos + weight > typePick) {
                    return t;
                }
                typePos += weight;
            }
            return null;
        } else {
            // assumes some type has non-banned Pokémon
            Type picked;
            do {
                picked = typeService.randomType(random);
            } while (typeWeightings.get(picked) == 0);
            return picked;
        }
    }

    private void initTypeWeightings(boolean noLegendaries, boolean allowAltFormes) {
        // Determine weightings
        for (Type t : typeService.getTypes()) {
            PokemonSet<Pokemon> pokemonOfType = rPokeService.getPokemon(noLegendaries, allowAltFormes, true)
                    .filterByType(t);
            int pkWithTyping = pokemonOfType.size();
            typeWeightings.put(t, pkWithTyping);
            totalTypeWeighting += pkWithTyping;
        }
    }

    public int getRandomAbilitySlot(Pokemon pokemon) {
        if (romHandler.abilitiesPerPokemon() == 0) {
            return 0;
        }
        List<Integer> abilitiesList = Arrays.asList(pokemon.getAbility1(), pokemon.getAbility2(), pokemon.getAbility3());
        int slot = random.nextInt(romHandler.abilitiesPerPokemon());
        while (abilitiesList.get(slot) == 0) {
            slot = random.nextInt(romHandler.abilitiesPerPokemon());
        }
        return slot + 1;
    }

    public int getValidAbilitySlotFromOriginal(Pokemon pokemon, int originalAbilitySlot) {
        // This is used in cases where one Trainer Pokemon evolves into another. If the unevolved Pokemon
        // is using slot 2, but the evolved Pokemon doesn't actually have a second ability, then we
        // want the evolved Pokemon to use slot 1 for safety's sake.
        if (originalAbilitySlot == 2 && pokemon.getAbility2() == 0) {
            return 1;
        }
        return originalAbilitySlot;
    }

    private Pokemon pickRandomEvolutionOf(Pokemon base, boolean mustEvolveItself) {
        // Used for "rival carries starter"
        // Pick a random evolution of base Pokemon, subject to
        // "must evolve itself" if appropriate.
        PokemonSet<Pokemon> candidates = new PokemonSet<>();
        for (Evolution ev : base.getEvolutionsFrom()) {
            if (!mustEvolveItself || ev.getTo().getEvolutionsFrom().size() > 0) {
                candidates.add(ev.getTo());
            }
        }

        if (candidates.size() == 0) {
            throw new RandomizationException("Random evolution called on a Pokemon without any usable evolutions.");
        }

        return candidates.getRandom(random);
    }

    private int getLevelOfStarter(List<Trainer> currentTrainers, String tag) {
        for (Trainer t : currentTrainers) {
            if (t.tag != null && t.tag.equals(tag)) {
                // Bingo, get highest level
                // last pokemon is given priority +2 but equal priority
                // = first pokemon wins, so its effectively +1
                // If it's tagged the same we can assume it's the same team
                // just the opposite gender or something like that...
                // So no need to check other trainers with same tag.
                int highestLevel = t.pokemon.get(0).level;
                int trainerPkmnCount = t.pokemon.size();
                for (int i = 1; i < trainerPkmnCount; i++) {
                    int levelBonus = (i == trainerPkmnCount - 1) ? 2 : 0;
                    if (t.pokemon.get(i).level + levelBonus > highestLevel) {
                        highestLevel = t.pokemon.get(i).level;
                    }
                }
                return highestLevel;
            }
        }
        return 0;
    }

    private void changeStarterWithTag(List<Trainer> currentTrainers, String tag, Pokemon starter, int abilitySlot) {
        for (Trainer t : currentTrainers) {
            if (t.tag != null && t.tag.equals(tag)) {

                // Bingo
                TrainerPokemon bestPoke = t.pokemon.get(0);

                if (t.forceStarterPosition >= 0) {
                    bestPoke = t.pokemon.get(t.forceStarterPosition);
                } else {
                    // Change the highest level pokemon, not the last.
                    // BUT: last gets +2 lvl priority (effectively +1)
                    // same as above, equal priority = earlier wins
                    int trainerPkmnCount = t.pokemon.size();
                    for (int i = 1; i < trainerPkmnCount; i++) {
                        int levelBonus = (i == trainerPkmnCount - 1) ? 2 : 0;
                        if (t.pokemon.get(i).level + levelBonus > bestPoke.level) {
                            bestPoke = t.pokemon.get(i);
                        }
                    }
                }
                bestPoke.pokemon = starter;
                setFormeForTrainerPokemon(bestPoke, starter);
                bestPoke.resetMoves = true;
                bestPoke.abilitySlot = abilitySlot;
            }
        }

    }

    private int numEvolutions(Pokemon pk, int maxInterested) {
        return numEvolutions(pk, 0, maxInterested);
    }

    private int numEvolutions(Pokemon pk, int depth, int maxInterested) {
        if (pk.getEvolutionsFrom().size() == 0) {
            return 0;
        } else {
            if (depth == maxInterested - 1) {
                return 1;
            } else {
                int maxEvos = 0;
                for (Evolution ev : pk.getEvolutionsFrom()) {
                    maxEvos = Math.max(maxEvos, numEvolutions(ev.getTo(), depth + 1, maxInterested) + 1);
                }
                return maxEvos;
            }
        }
    }

    private Pokemon fullyEvolve(Pokemon pokemon, int trainerIndex) {
        // If the fullyEvolvedRandomSeed hasn't been set yet, set it here.
        if (this.fullyEvolvedRandomSeed == -1) {
            this.fullyEvolvedRandomSeed = random.nextInt(GlobalConstants.LARGEST_NUMBER_OF_SPLIT_EVOS);
        }

        Set<Pokemon> seenMons = new HashSet<>();
        seenMons.add(pokemon);

        while (true) {
            if (pokemon.getEvolutionsFrom().size() == 0) {
                // fully evolved
                break;
            }

            // check for cyclic evolutions from what we've already seen
            boolean cyclic = false;
            for (Evolution ev : pokemon.getEvolutionsFrom()) {
                if (seenMons.contains(ev.getTo())) {
                    // cyclic evolution detected - bail now
                    cyclic = true;
                    break;
                }
            }

            if (cyclic) {
                break;
            }

            // We want to make split evolutions deterministic, but still random on a seed-to-seed basis.
            // Therefore, we take a random value (which is generated once per seed) and add it to the trainer's
            // index to get a pseudorandom number that can be used to decide which split to take.
            int evolutionIndex = (this.fullyEvolvedRandomSeed + trainerIndex) % pokemon.getEvolutionsFrom().size();
            pokemon = pokemon.getEvolutionsFrom().get(evolutionIndex).getTo();
            seenMons.add(pokemon);
        }

        return pokemon;
    }

    private void setEvoChainAsIllegal(Pokemon newPK, PokemonSet<Pokemon> illegalList, boolean willForceEvolve) {
        // set pre-evos as illegal
        setIllegalPreEvos(newPK, illegalList);

        // if the placed Pokemon will be forced fully evolved, set its evolutions as illegal
        if (willForceEvolve) {
            setIllegalEvos(newPK, illegalList);
        }
    }

    private void setIllegalPreEvos(Pokemon pk, PokemonSet<Pokemon> illegalList) {
        for (Evolution evo : pk.getEvolutionsTo()) {
            pk = evo.getFrom();
            illegalList.add(pk);
            setIllegalPreEvos(pk, illegalList);
        }
    }

    private void setIllegalEvos(Pokemon pk, PokemonSet<Pokemon> illegalList) {
        for (Evolution evo : pk.getEvolutionsFrom()) {
            pk = evo.getTo();
            illegalList.add(pk);
            setIllegalEvos(pk, illegalList);
        }
    }

    private PokemonSet<Pokemon> getFinalEvos(Pokemon pk) {
        PokemonSet<Pokemon> finalEvos = new PokemonSet<>();
        traverseEvolutions(pk, finalEvos);
        return finalEvos;
    }

    private void traverseEvolutions(Pokemon pk, PokemonSet<Pokemon> finalEvos) {
        if (!pk.getEvolutionsFrom().isEmpty()) {
            for (Evolution evo : pk.getEvolutionsFrom()) {
                pk = evo.getTo();
                traverseEvolutions(pk, finalEvos);
            }
        } else {
            finalEvos.add(pk);
        }
    }

    private void setFormeForTrainerPokemon(TrainerPokemon tp, Pokemon pk) {
        boolean checkCosmetics = true;
        tp.formeSuffix = "";
        tp.forme = 0;
        if (pk.getFormeNumber() > 0) {
            tp.forme = pk.getFormeNumber();
            tp.formeSuffix = pk.getFormeSuffix();
            tp.pokemon = pk.getBaseForme();
            checkCosmetics = false;
        }
        if (checkCosmetics && tp.pokemon.getCosmeticForms() > 0) {
            tp.forme = tp.pokemon.getCosmeticFormNumber(random.nextInt(tp.pokemon.getCosmeticForms()));
        } else if (!checkCosmetics && pk.getCosmeticForms() > 0) {
            tp.forme += pk.getCosmeticFormNumber(random.nextInt(pk.getCosmeticForms()));
        }
    }

    private void applyLevelModifierToTrainerPokemon(Trainer trainer, int levelModifier) {
        if (levelModifier != 0) {
            for (TrainerPokemon tp : trainer.pokemon) {
                tp.level = Math.min(100, (int) Math.round(tp.level * (1 + levelModifier / 100.0)));
            }
        }
    }

    private void setPlacementHistory(Pokemon newPK) {
        int history = getPlacementHistory(newPK);
        placementHistory.put(newPK, history + 1);
    }

    private int getPlacementHistory(Pokemon newPK) {
        return placementHistory.getOrDefault(newPK, 0);
    }

    private double getPlacementAverage() {
        return placementHistory.values().stream().mapToInt(e -> e).average().orElse(0);
    }

    public void makeRivalCarryStarter() {
        List<Trainer> currentTrainers = romHandler.getTrainers();
        rivalCarriesStarterUpdate(currentTrainers, "RIVAL", romHandler.isORAS() ? 0 : 1);
        rivalCarriesStarterUpdate(currentTrainers, "FRIEND", 2);
        romHandler.setTrainers(currentTrainers);
    }

    private void rivalCarriesStarterUpdate(List<Trainer> currentTrainers, String prefix, int pokemonOffset) {
        // Find the highest rival battle #
        int highestRivalNum = 0;
        for (Trainer t : currentTrainers) {
            if (t.tag != null && t.tag.startsWith(prefix)) {
                highestRivalNum = Math.max(highestRivalNum,
                        Integer.parseInt(t.tag.substring(prefix.length(), t.tag.indexOf('-'))));
            }
        }

        if (highestRivalNum == 0) {
            // This rival type not used in this game
            return;
        }

        // Get the starters
        // us 0 1 2 => them 0+n 1+n 2+n
        List<Pokemon> starters = romHandler.getStarters();

        // Yellow needs its own case, unfortunately.
        if (romHandler.isYellow()) {
            // The rival's starter is index 1
            Pokemon rivalStarter = starters.get(1);
            int timesEvolves = numEvolutions(rivalStarter, 2);
            // Yellow does not have abilities
            int abilitySlot = 0;
            // Apply evolutions as appropriate
            if (timesEvolves == 0) {
                for (int j = 1; j <= 3; j++) {
                    changeStarterWithTag(currentTrainers, prefix + j + "-0", rivalStarter, abilitySlot);
                }
                for (int j = 4; j <= 7; j++) {
                    for (int i = 0; i < 3; i++) {
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, rivalStarter, abilitySlot);
                    }
                }
            } else if (timesEvolves == 1) {
                for (int j = 1; j <= 3; j++) {
                    changeStarterWithTag(currentTrainers, prefix + j + "-0", rivalStarter, abilitySlot);
                }
                rivalStarter = pickRandomEvolutionOf(rivalStarter, false);
                for (int j = 4; j <= 7; j++) {
                    for (int i = 0; i < 3; i++) {
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, rivalStarter, abilitySlot);
                    }
                }
            } else if (timesEvolves == 2) {
                for (int j = 1; j <= 2; j++) {
                    changeStarterWithTag(currentTrainers, prefix + j + "-" + 0, rivalStarter, abilitySlot);
                }
                rivalStarter = pickRandomEvolutionOf(rivalStarter, true);
                changeStarterWithTag(currentTrainers, prefix + "3-0", rivalStarter, abilitySlot);
                for (int i = 0; i < 3; i++) {
                    changeStarterWithTag(currentTrainers, prefix + "4-" + i, rivalStarter, abilitySlot);
                }
                rivalStarter = pickRandomEvolutionOf(rivalStarter, false);
                for (int j = 5; j <= 7; j++) {
                    for (int i = 0; i < 3; i++) {
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, rivalStarter, abilitySlot);
                    }
                }
            }
        } else {
            // Replace each starter as appropriate
            // Use level to determine when to evolve, not number anymore
            for (int i = 0; i < 3; i++) {
                // Rival's starters are pokemonOffset over from each of ours
                int starterToUse = (i + pokemonOffset) % 3;
                Pokemon thisStarter = starters.get(starterToUse);
                int timesEvolves = numEvolutions(thisStarter, 2);
                int abilitySlot = getRandomAbilitySlot(thisStarter);
                while (abilitySlot == 3) {
                    // Since starters never have hidden abilities, the rival's starter shouldn't either
                    abilitySlot = getRandomAbilitySlot(thisStarter);
                }
                // If a fully evolved pokemon, use throughout
                // Otherwise split by evolutions as appropriate
                if (timesEvolves == 0) {
                    for (int j = 1; j <= highestRivalNum; j++) {
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter, abilitySlot);
                    }
                } else if (timesEvolves == 1) {
                    int j = 1;
                    for (; j <= highestRivalNum / 2; j++) {
                        if (getLevelOfStarter(currentTrainers, prefix + j + "-" + i) >= 30) {
                            break;
                        }
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter, abilitySlot);
                    }
                    thisStarter = pickRandomEvolutionOf(thisStarter, false);
                    int evolvedAbilitySlot = getValidAbilitySlotFromOriginal(thisStarter, abilitySlot);
                    for (; j <= highestRivalNum; j++) {
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter, evolvedAbilitySlot);
                    }
                } else if (timesEvolves == 2) {
                    int j = 1;
                    for (; j <= highestRivalNum; j++) {
                        if (getLevelOfStarter(currentTrainers, prefix + j + "-" + i) >= 16) {
                            break;
                        }
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter, abilitySlot);
                    }
                    thisStarter = pickRandomEvolutionOf(thisStarter, true);
                    int evolvedAbilitySlot = getValidAbilitySlotFromOriginal(thisStarter, abilitySlot);
                    for (; j <= highestRivalNum; j++) {
                        if (getLevelOfStarter(currentTrainers, prefix + j + "-" + i) >= 36) {
                            break;
                        }
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter, evolvedAbilitySlot);
                    }
                    thisStarter = pickRandomEvolutionOf(thisStarter, false);
                    evolvedAbilitySlot = getValidAbilitySlotFromOriginal(thisStarter, abilitySlot);
                    for (; j <= highestRivalNum; j++) {
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter, evolvedAbilitySlot);
                    }
                }
            }
        }

    }


    public void forceFullyEvolvedTrainerPokes(Settings settings) {
        int minLevel = settings.getTrainersForceFullyEvolvedLevel();

        List<Trainer> currentTrainers = romHandler.getTrainers();
        for (Trainer t : currentTrainers) {
            for (TrainerPokemon tp : t.pokemon) {
                if (tp.level >= minLevel) {
                    Pokemon newPokemon = fullyEvolve(tp.pokemon, t.index);
                    if (newPokemon != tp.pokemon) {
                        tp.pokemon = newPokemon;
                        setFormeForTrainerPokemon(tp, newPokemon);
                        tp.abilitySlot = getValidAbilitySlotFromOriginal(newPokemon, tp.abilitySlot);
                        tp.resetMoves = true;
                    }
                }
            }
        }
        romHandler.setTrainers(currentTrainers);
    }

    public void addTrainerPokemon() {
        int additionalNormal = settings.getAdditionalRegularTrainerPokemon();
        int additionalImportant = settings.getAdditionalImportantTrainerPokemon();
        int additionalBoss = settings.getAdditionalBossTrainerPokemon();

        List<Trainer> currentTrainers = romHandler.getTrainers();
        for (Trainer t : currentTrainers) {
            int additional;
            if (t.isBoss()) {
                additional = additionalBoss;
            } else if (t.isImportant()) {
                if (t.shouldNotGetBuffs()) continue;
                additional = additionalImportant;
            } else {
                additional = additionalNormal;
            }

            if (additional == 0) {
                continue;
            }

            int lowest = 100;
            List<TrainerPokemon> potentialPokes = new ArrayList<>();

            // First pass: find lowest level
            for (TrainerPokemon tpk : t.pokemon) {
                if (tpk.level < lowest) {
                    lowest = tpk.level;
                }
            }

            // Second pass: find all Pokemon at lowest level
            for (TrainerPokemon tpk : t.pokemon) {
                if (tpk.level == lowest) {
                    potentialPokes.add(tpk);
                }
            }

            // If a trainer can appear in a Multi Battle (i.e., a Double Battle where the enemy consists
            // of two independent trainers), we want to be aware of that so we don't give them a team of
            // six Pokemon and have a 6v12 battle
            int maxPokemon = t.multiBattleStatus != Trainer.MultiBattleStatus.NEVER ? 3 : 6;
            for (int i = 0; i < additional; i++) {
                if (t.pokemon.size() >= maxPokemon) break;

                // We want to preserve the original last Pokemon because the order is sometimes used to
                // determine the rival's starter
                int secondToLastIndex = t.pokemon.size() - 1;
                TrainerPokemon newPokemon = potentialPokes.get(i % potentialPokes.size()).copy();

                // Clear out the held item because we only want one Pokemon with a mega stone if we're
                // swapping mega evolvables
                newPokemon.heldItem = 0;
                t.pokemon.add(secondToLastIndex, newPokemon);
            }
        }
        romHandler.setTrainers(currentTrainers);
    }

    public void setDoubleBattleMode() {
        List<Trainer> trainers = romHandler.getTrainers();
        for (Trainer tr : trainers) {
            if (!(tr.multiBattleStatus == Trainer.MultiBattleStatus.ALWAYS || tr.shouldNotGetBuffs())) {
                if (tr.pokemon.size() == 1) {
                    tr.pokemon.add(tr.pokemon.get(0).copy());
                }
                tr.forcedDoubleBattle = true;
            }
        }
        romHandler.setTrainers(trainers);
        romHandler.makeDoubleBattleModePossible();
    }

    public void randomizeTrainerHeldItems() {
        boolean giveToBossPokemon = settings.isRandomizeHeldItemsForBossTrainerPokemon();
        boolean giveToImportantPokemon = settings.isRandomizeHeldItemsForImportantTrainerPokemon();
        boolean giveToRegularPokemon = settings.isRandomizeHeldItemsForRegularTrainerPokemon();
        boolean highestLevelOnly = settings.isHighestLevelGetsItemsForTrainers();

        List<Move> moves = romHandler.getMoves();
        Map<Integer, List<MoveLearnt>> movesets = romHandler.getMovesLearnt();
        List<Trainer> currentTrainers = romHandler.getTrainers();
        for (Trainer t : currentTrainers) {
            if (t.shouldNotGetBuffs()) {
                continue;
            }
            if (!giveToRegularPokemon && (!t.isImportant() && !t.isBoss())) {
                continue;
            }
            if (!giveToImportantPokemon && t.isImportant()) {
                continue;
            }
            if (!giveToBossPokemon && t.isBoss()) {
                continue;
            }
            t.setPokemonHaveItems(true);
            if (highestLevelOnly) {
                int maxLevel = -1;
                TrainerPokemon highestLevelPoke = null;
                for (TrainerPokemon tp : t.pokemon) {
                    if (tp.level > maxLevel) {
                        highestLevelPoke = tp;
                        maxLevel = tp.level;
                    }
                }
                if (highestLevelPoke == null) {
                    continue; // should never happen - trainer had zero pokes
                }
                int[] moveset = highestLevelPoke.resetMoves ?
                        RomFunctions.getMovesAtLevel(romHandler.getAltFormeOfPokemon(
                                        highestLevelPoke.pokemon, highestLevelPoke.forme).getNumber(),
                                movesets,
                                highestLevelPoke.level) :
                        highestLevelPoke.moves;
                randomizeHeldItem(highestLevelPoke, settings, moves, moveset);
            } else {
                for (TrainerPokemon tp : t.pokemon) {
                    int[] moveset = tp.resetMoves ?
                            RomFunctions.getMovesAtLevel(romHandler.getAltFormeOfPokemon(
                                            tp.pokemon, tp.forme).getNumber(),
                                    movesets,
                                    tp.level) :
                            tp.moves;
                    randomizeHeldItem(tp, settings, moves, moveset);
                    if (t.requiresUniqueHeldItems) {
                        while (!t.pokemonHaveUniqueHeldItems()) {
                            randomizeHeldItem(tp, settings, moves, moveset);
                        }
                    }
                }
            }
        }
        romHandler.setTrainers(currentTrainers);
    }

    private void randomizeHeldItem(TrainerPokemon tp, Settings settings, List<Move> moves, int[] moveset) {
        boolean sensibleItemsOnly = settings.isSensibleItemsOnlyForTrainers();
        boolean consumableItemsOnly = settings.isConsumableItemsOnlyForTrainers();
        boolean swapMegaEvolutions = settings.isSwapTrainerMegaEvos();
        if (tp.hasZCrystal) {
            return; // Don't overwrite existing Z Crystals.
        }
        if (tp.hasMegaStone && swapMegaEvolutions) {
            return; // Don't overwrite mega stones if another setting handled that.
        }
        List<Integer> toChooseFrom;
        if (sensibleItemsOnly) {
            toChooseFrom = romHandler.getSensibleHeldItemsFor(tp, consumableItemsOnly, moves, moveset);
        } else if (consumableItemsOnly) {
            toChooseFrom = romHandler.getAllConsumableHeldItems();
        } else {
            toChooseFrom = romHandler.getAllHeldItems();
        }
        tp.heldItem = toChooseFrom.get(random.nextInt(toChooseFrom.size()));
    }

    public void pickTrainerMovesets() {
        boolean isCyclicEvolutions = settings.getEvolutionsMod() == Settings.EvolutionsMod.RANDOM_EVERY_LEVEL;
        boolean doubleBattleMode = settings.isDoubleBattleMode();

        List<Trainer> trainers = romHandler.getTrainers();

        for (Trainer t : trainers) {
            t.setPokemonHaveCustomMoves(true);

            for (TrainerPokemon tp : t.pokemon) {
                tp.resetMoves = false;

                List<Move> movesAtLevel = getMoveSelectionPoolAtLevel(tp, isCyclicEvolutions);

                movesAtLevel = trimMoveList(tp, movesAtLevel, doubleBattleMode);

                if (movesAtLevel.isEmpty()) {
                    continue;
                }

                double trainerTypeModifier = 1;
                if (t.isImportant()) {
                    trainerTypeModifier = 1.5;
                } else if (t.isBoss()) {
                    trainerTypeModifier = 2;
                }
                double movePoolSizeModifier = movesAtLevel.size() / 10.0;
                double bonusModifier = trainerTypeModifier * movePoolSizeModifier;

                double atkSpatkRatioModifier = 0.75;
                double stabMoveBias = 0.25 * bonusModifier;
                double hardAbilityMoveBias = 1 * bonusModifier;
                double softAbilityMoveBias = 0.5 * bonusModifier;
                double statBias = 0.5 * bonusModifier;
                double softMoveBias = 0.25 * bonusModifier;
                double hardMoveBias = 1 * bonusModifier;
                double softMoveAntiBias = 0.5;

                // Add bias for STAB

                Pokemon pk = romHandler.getAltFormeOfPokemon(tp.pokemon, tp.forme);

                List<Move> stabMoves = new ArrayList<>(movesAtLevel)
                        .stream()
                        .filter(mv -> mv.type == pk.getPrimaryType() && mv.category != MoveCategory.STATUS)
                        .collect(Collectors.toList());
                Collections.shuffle(stabMoves, random);

                for (int i = 0; i < stabMoveBias * stabMoves.size(); i++) {
                    int j = i % stabMoves.size();
                    movesAtLevel.add(stabMoves.get(j));
                }

                if (pk.getSecondaryType() != null) {
                    stabMoves = new ArrayList<>(movesAtLevel)
                            .stream()
                            .filter(mv -> mv.type == pk.getSecondaryType() && mv.category != MoveCategory.STATUS)
                            .collect(Collectors.toList());
                    Collections.shuffle(stabMoves, random);

                    for (int i = 0; i < stabMoveBias * stabMoves.size(); i++) {
                        int j = i % stabMoves.size();
                        movesAtLevel.add(stabMoves.get(j));
                    }
                }

                // Hard ability/move synergy

                List<Move> abilityMoveSynergyList = MoveSynergy.getHardAbilityMoveSynergy(
                        romHandler.getAbilityForTrainerPokemon(tp),
                        pk.getPrimaryType(),
                        pk.getSecondaryType(),
                        movesAtLevel,
                        romHandler.generationOfPokemon(),
                        romHandler.getPerfectAccuracy());
                Collections.shuffle(abilityMoveSynergyList, random);
                for (int i = 0; i < hardAbilityMoveBias * abilityMoveSynergyList.size(); i++) {
                    int j = i % abilityMoveSynergyList.size();
                    movesAtLevel.add(abilityMoveSynergyList.get(j));
                }

                // Soft ability/move synergy

                List<Move> softAbilityMoveSynergyList = MoveSynergy.getSoftAbilityMoveSynergy(
                        romHandler.getAbilityForTrainerPokemon(tp),
                        movesAtLevel,
                        pk.getPrimaryType(),
                        pk.getSecondaryType());

                Collections.shuffle(softAbilityMoveSynergyList, random);
                for (int i = 0; i < softAbilityMoveBias * softAbilityMoveSynergyList.size(); i++) {
                    int j = i % softAbilityMoveSynergyList.size();
                    movesAtLevel.add(softAbilityMoveSynergyList.get(j));
                }

                // Soft ability/move anti-synergy

                List<Move> softAbilityMoveAntiSynergyList = MoveSynergy.getSoftAbilityMoveAntiSynergy(
                        romHandler.getAbilityForTrainerPokemon(tp), movesAtLevel);
                List<Move> withoutSoftAntiSynergy = new ArrayList<>(movesAtLevel);
                for (Move mv : softAbilityMoveAntiSynergyList) {
                    withoutSoftAntiSynergy.remove(mv);
                }
                if (withoutSoftAntiSynergy.size() > 0) {
                    movesAtLevel = withoutSoftAntiSynergy;
                }

                List<Move> distinctMoveList = movesAtLevel.stream().distinct().collect(Collectors.toList());
                int movesLeft = distinctMoveList.size();

                if (movesLeft <= 4) {

                    for (int i = 0; i < 4; i++) {
                        if (i < movesLeft) {
                            tp.moves[i] = distinctMoveList.get(i).number;
                        } else {
                            tp.moves[i] = 0;
                        }
                    }
                    continue;
                }

                // Stat/move synergy

                List<Move> statSynergyList = MoveSynergy.getStatMoveSynergy(pk, movesAtLevel);
                Collections.shuffle(statSynergyList, random);
                for (int i = 0; i < statBias * statSynergyList.size(); i++) {
                    int j = i % statSynergyList.size();
                    movesAtLevel.add(statSynergyList.get(j));
                }

                // Stat/move anti-synergy

                List<Move> statAntiSynergyList = MoveSynergy.getStatMoveAntiSynergy(pk, movesAtLevel);
                List<Move> withoutStatAntiSynergy = new ArrayList<>(movesAtLevel);
                for (Move mv : statAntiSynergyList) {
                    withoutStatAntiSynergy.remove(mv);
                }
                if (withoutStatAntiSynergy.size() > 0) {
                    movesAtLevel = withoutStatAntiSynergy;
                }

                distinctMoveList = movesAtLevel.stream().distinct().collect(Collectors.toList());
                movesLeft = distinctMoveList.size();

                if (movesLeft <= 4) {

                    for (int i = 0; i < 4; i++) {
                        if (i < movesLeft) {
                            tp.moves[i] = distinctMoveList.get(i).number;
                        } else {
                            tp.moves[i] = 0;
                        }
                    }
                    continue;
                }

                // Add bias for atk/spatk ratio

                double atkSpatkRatio = (double) pk.getAttack() / (double) pk.getSpatk();
                switch (romHandler.getAbilityForTrainerPokemon(tp)) {
                    case Abilities.hugePower:
                    case Abilities.purePower:
                        atkSpatkRatio *= 2;
                        break;
                    case Abilities.hustle:
                    case Abilities.gorillaTactics:
                        atkSpatkRatio *= 1.5;
                        break;
                    case Abilities.moxie:
                        atkSpatkRatio *= 1.1;
                        break;
                    case Abilities.soulHeart:
                        atkSpatkRatio *= 0.9;
                        break;
                }

                List<Move> physicalMoves = new ArrayList<>(movesAtLevel)
                        .stream()
                        .filter(mv -> mv.category == MoveCategory.PHYSICAL).toList();
                List<Move> specialMoves = new ArrayList<>(movesAtLevel)
                        .stream()
                        .filter(mv -> mv.category == MoveCategory.SPECIAL).toList();

                if (atkSpatkRatio < 1 && specialMoves.size() > 0) {
                    atkSpatkRatio = 1 / atkSpatkRatio;
                    double acceptedRatio = atkSpatkRatioModifier * atkSpatkRatio;
                    int additionalMoves = (int) (physicalMoves.size() * acceptedRatio) - specialMoves.size();
                    for (int i = 0; i < additionalMoves; i++) {
                        Move mv = specialMoves.get(random.nextInt(specialMoves.size()));
                        movesAtLevel.add(mv);
                    }
                } else if (physicalMoves.size() > 0) {
                    double acceptedRatio = atkSpatkRatioModifier * atkSpatkRatio;
                    int additionalMoves = (int) (specialMoves.size() * acceptedRatio) - physicalMoves.size();
                    for (int i = 0; i < additionalMoves; i++) {
                        Move mv = physicalMoves.get(random.nextInt(physicalMoves.size()));
                        movesAtLevel.add(mv);
                    }
                }

                // Pick moves

                List<Move> pickedMoves = new ArrayList<>();

                for (int i = 1; i <= 4; i++) {
                    Move move;
                    List<Move> pickFrom;

                    if (i == 1) {
                        pickFrom = movesAtLevel
                                .stream()
                                .filter(mv -> mv.isGoodDamaging(romHandler.getPerfectAccuracy()))
                                .collect(Collectors.toList());
                        if (pickFrom.isEmpty()) {
                            pickFrom = movesAtLevel;
                        }
                    } else {
                        pickFrom = movesAtLevel;
                    }

                    if (i == 4) {
                        List<Move> requiresOtherMove = movesAtLevel
                                .stream()
                                .filter(mv -> GlobalConstants.requiresOtherMove.contains(mv.number))
                                .distinct().toList();

                        for (Move dependentMove : requiresOtherMove) {
                            boolean hasRequiredMove = false;
                            for (Move requiredMove : MoveSynergy.requiresOtherMove(dependentMove, movesAtLevel)) {
                                if (pickedMoves.contains(requiredMove)) {
                                    hasRequiredMove = true;
                                    break;
                                }
                            }
                            if (!hasRequiredMove) {
                                movesAtLevel.removeAll(Collections.singletonList(dependentMove));
                            }
                        }
                    }

                    move = pickFrom.get(random.nextInt(pickFrom.size()));
                    pickedMoves.add(move);

                    if (i == 4) {
                        break;
                    }

                    movesAtLevel.removeAll(Collections.singletonList(move));

                    movesAtLevel.removeAll(MoveSynergy.getHardMoveAntiSynergy(move, movesAtLevel));

                    distinctMoveList = movesAtLevel.stream().distinct().collect(Collectors.toList());
                    movesLeft = distinctMoveList.size();

                    if (movesLeft <= (4 - i)) {
                        pickedMoves.addAll(distinctMoveList);
                        break;
                    }

                    List<Move> hardMoveSynergyList = MoveSynergy.getMoveSynergy(
                            move,
                            movesAtLevel,
                            romHandler.generationOfPokemon());
                    Collections.shuffle(hardMoveSynergyList, random);
                    for (int j = 0; j < hardMoveBias * hardMoveSynergyList.size(); j++) {
                        int k = j % hardMoveSynergyList.size();
                        movesAtLevel.add(hardMoveSynergyList.get(k));
                    }

                    List<Move> softMoveSynergyList = MoveSynergy.getSoftMoveSynergy(
                            move,
                            movesAtLevel,
                            romHandler.getTypeTable());
                    Collections.shuffle(softMoveSynergyList, random);
                    for (int j = 0; j < softMoveBias * softMoveSynergyList.size(); j++) {
                        int k = j % softMoveSynergyList.size();
                        movesAtLevel.add(softMoveSynergyList.get(k));
                    }

                    List<Move> softMoveAntiSynergyList = MoveSynergy.getSoftMoveAntiSynergy(move, movesAtLevel);
                    Collections.shuffle(softMoveAntiSynergyList, random);
                    for (int j = 0; j < softMoveAntiBias * softMoveAntiSynergyList.size(); j++) {
                        distinctMoveList = movesAtLevel.stream().distinct().collect(Collectors.toList());
                        if (distinctMoveList.size() <= (4 - i)) {
                            break;
                        }
                        int k = j % softMoveAntiSynergyList.size();
                        movesAtLevel.remove(softMoveAntiSynergyList.get(k));
                    }

                    distinctMoveList = movesAtLevel.stream().distinct().collect(Collectors.toList());
                    movesLeft = distinctMoveList.size();

                    if (movesLeft <= (4 - i)) {
                        pickedMoves.addAll(distinctMoveList);
                        break;
                    }
                }

                int movesPicked = pickedMoves.size();

                for (int i = 0; i < 4; i++) {
                    if (i < movesPicked) {
                        tp.moves[i] = pickedMoves.get(i).number;
                    } else {
                        tp.moves[i] = 0;
                    }
                }
            }
        }
        romHandler.setTrainers(trainers);
    }

    private List<Move> trimMoveList(TrainerPokemon tp, List<Move> movesAtLevel, boolean doubleBattleMode) {
        int movesLeft = movesAtLevel.size();

        if (movesLeft <= 4) {
            for (int i = 0; i < 4; i++) {
                if (i < movesLeft) {
                    tp.moves[i] = movesAtLevel.get(i).number;
                } else {
                    tp.moves[i] = 0;
                }
            }
            return new ArrayList<>();
        }

        movesAtLevel = movesAtLevel
                .stream()
                .filter(mv -> !GlobalConstants.uselessMoves.contains(mv.number) &&
                        (doubleBattleMode || !GlobalConstants.doubleBattleMoves.contains(mv.number)))
                .collect(Collectors.toList());

        movesLeft = movesAtLevel.size();

        if (movesLeft <= 4) {
            for (int i = 0; i < 4; i++) {
                if (i < movesLeft) {
                    tp.moves[i] = movesAtLevel.get(i).number;
                } else {
                    tp.moves[i] = 0;
                }
            }
            return new ArrayList<>();
        }

        List<Move> obsoletedMoves = getObsoleteMoves(movesAtLevel);

        // Remove obsoleted moves

        movesAtLevel.removeAll(obsoletedMoves);

        movesLeft = movesAtLevel.size();

        if (movesLeft <= 4) {
            for (int i = 0; i < 4; i++) {
                if (i < movesLeft) {
                    tp.moves[i] = movesAtLevel.get(i).number;
                } else {
                    tp.moves[i] = 0;
                }
            }
            return new ArrayList<>();
        }

        List<Move> requiresOtherMove = movesAtLevel
                .stream()
                .filter(mv -> GlobalConstants.requiresOtherMove.contains(mv.number)).toList();

        for (Move dependentMove : requiresOtherMove) {
            if (MoveSynergy.requiresOtherMove(dependentMove, movesAtLevel).isEmpty()) {
                movesAtLevel.remove(dependentMove);
            }
        }

        movesLeft = movesAtLevel.size();

        if (movesLeft <= 4) {
            for (int i = 0; i < 4; i++) {
                if (i < movesLeft) {
                    tp.moves[i] = movesAtLevel.get(i).number;
                } else {
                    tp.moves[i] = 0;
                }
            }
            return new ArrayList<>();
        }

        // Remove hard ability anti-synergy moves

        List<Move> withoutHardAntiSynergy = new ArrayList<>(movesAtLevel);
        withoutHardAntiSynergy.removeAll(MoveSynergy.getHardAbilityMoveAntiSynergy(
                romHandler.getAbilityForTrainerPokemon(tp),
                movesAtLevel));

        if (withoutHardAntiSynergy.size() > 0) {
            movesAtLevel = withoutHardAntiSynergy;
        }

        movesLeft = movesAtLevel.size();

        if (movesLeft <= 4) {
            for (int i = 0; i < 4; i++) {
                if (i < movesLeft) {
                    tp.moves[i] = movesAtLevel.get(i).number;
                } else {
                    tp.moves[i] = 0;
                }
            }
            return new ArrayList<>();
        }
        return movesAtLevel;
    }

    private List<Move> getObsoleteMoves(List<Move> movesAtLevel) {
        List<Move> obsoletedMoves = new ArrayList<>();
        for (Move mv : movesAtLevel) {
            if (GlobalConstants.cannotObsoleteMoves.contains(mv.number)) {
                continue;
            }
            if (mv.power > 0) {
                List<Move> obsoleteThis = movesAtLevel
                        .stream()
                        .filter(mv2 -> !GlobalConstants.cannotBeObsoletedMoves.contains(mv2.number) &&
                                mv.type == mv2.type &&
                                ((((mv.statChangeMoveType == mv2.statChangeMoveType &&
                                        mv.statChanges[0].equals(mv2.statChanges[0])) ||
                                        (mv2.statChangeMoveType == StatChangeMoveType.NONE_OR_UNKNOWN &&
                                                mv.hasBeneficialStatChange())) &&
                                        mv.absorbPercent >= mv2.absorbPercent &&
                                        !mv.isChargeMove &&
                                        !mv.isRechargeMove) ||
                                        mv2.power * mv2.hitCount <= 30) &&
                                mv.hitratio >= mv2.hitratio &&
                                mv.category == mv2.category &&
                                mv.priority >= mv2.priority &&
                                mv2.power > 0 &&
                                mv.power * mv.hitCount > mv2.power * mv2.hitCount).toList();
//                for (Move obsoleted: obsoleteThis) {
//                    System.out.println(obsoleted.name + " obsoleted by " + mv.name);
//                }
                obsoletedMoves.addAll(obsoleteThis);
            } else if (mv.statChangeMoveType == StatChangeMoveType.NO_DAMAGE_USER ||
                    mv.statChangeMoveType == StatChangeMoveType.NO_DAMAGE_TARGET) {
                List<Move> obsoleteThis = new ArrayList<>();
                List<Move.StatChange> statChanges1 = new ArrayList<>();
                for (Move.StatChange sc : mv.statChanges) {
                    if (sc.type != StatChangeType.NONE) {
                        statChanges1.add(sc);
                    }
                }
                for (Move mv2 : movesAtLevel
                        .stream()
                        .filter(otherMv -> !otherMv.equals(mv) &&
                                otherMv.power <= 0 &&
                                otherMv.statChangeMoveType == mv.statChangeMoveType &&
                                (otherMv.statusType == mv.statusType ||
                                        otherMv.statusType == StatusType.NONE)).toList()) {
                    List<Move.StatChange> statChanges2 = new ArrayList<>();
                    for (Move.StatChange sc : mv2.statChanges) {
                        if (sc.type != StatChangeType.NONE) {
                            statChanges2.add(sc);
                        }
                    }
                    if (statChanges2.size() > statChanges1.size()) {
                        continue;
                    }
                    List<Move.StatChange> statChanges1Filtered = statChanges1
                            .stream()
                            .filter(sc -> !statChanges2.contains(sc)).toList();
                    statChanges2.removeAll(statChanges1);
                    if (!statChanges1Filtered.isEmpty() && statChanges2.isEmpty()) {
                        if (!GlobalConstants.cannotBeObsoletedMoves.contains(mv2.number)) {
                            obsoleteThis.add(mv2);
                        }
                        continue;
                    }
                    if (statChanges1Filtered.isEmpty() && statChanges2.isEmpty()) {
                        continue;
                    }
                    boolean maybeBetter = false;
                    for (Move.StatChange sc1 : statChanges1Filtered) {
                        boolean canStillBeBetter = false;
                        for (Move.StatChange sc2 : statChanges2) {
                            if (sc1.type == sc2.type) {
                                canStillBeBetter = true;
                                if ((mv.statChangeMoveType == StatChangeMoveType.NO_DAMAGE_USER && sc1.stages > sc2.stages) ||
                                        (mv.statChangeMoveType == StatChangeMoveType.NO_DAMAGE_TARGET && sc1.stages < sc2.stages)) {
                                    maybeBetter = true;
                                } else {
                                    canStillBeBetter = false;
                                }
                            }
                        }
                        if (!canStillBeBetter) {
                            maybeBetter = false;
                            break;
                        }
                    }
                    if (maybeBetter) {
                        if (!GlobalConstants.cannotBeObsoletedMoves.contains(mv2.number)) {
                            obsoleteThis.add(mv2);
                        }
                    }
                }
//                for (Move obsoleted : obsoleteThis) {
//                    System.out.println(obsoleted.name + " obsoleted by " + mv.name);
//                }
                obsoletedMoves.addAll(obsoleteThis);
            }
        }

        return obsoletedMoves.stream().distinct().collect(Collectors.toList());
    }

    private List<Move> getMoveSelectionPoolAtLevel(TrainerPokemon tp, boolean cyclicEvolutions) {

        List<Move> moves = romHandler.getMoves();
        double eggMoveProbability = 0.1;
        double preEvoMoveProbability = 0.5;
        double tmMoveProbability = 0.6;
        double tutorMoveProbability = 0.6;

        if (allLevelUpMoves == null) {
            allLevelUpMoves = romHandler.getMovesLearnt();
        }

        if (allEggMoves == null) {
            allEggMoves = romHandler.getEggMoves();
        }

        if (allTMCompat == null) {
            allTMCompat = romHandler.getTMHMCompatibility();
        }

        if (allTMMoves == null) {
            allTMMoves = romHandler.getTMMoves();
        }

        if (allTutorCompat == null && romHandler.hasMoveTutors()) {
            allTutorCompat = romHandler.getMoveTutorCompatibility();
        }

        if (allTutorMoves == null) {
            allTutorMoves = romHandler.getMoveTutorMoves();
        }

        // Level-up Moves
        List<Move> moveSelectionPoolAtLevel = allLevelUpMoves.get(romHandler.getAltFormeOfPokemon(tp.pokemon, tp.forme).getNumber())
                .stream()
                .filter(ml -> (ml.level <= tp.level && ml.level != 0) || (ml.level == 0 && tp.level >= 30))
                .map(ml -> moves.get(ml.move))
                .distinct()
                .collect(Collectors.toList());

        // Pre-Evo Moves
        if (!cyclicEvolutions) {
            Pokemon preEvo;
            if (romHandler.altFormesCanHaveDifferentEvolutions()) {
                preEvo = romHandler.getAltFormeOfPokemon(tp.pokemon, tp.forme);
            } else {
                preEvo = tp.pokemon;
            }
            while (!preEvo.getEvolutionsTo().isEmpty()) {
                preEvo = preEvo.getEvolutionsTo().get(0).getFrom();
                moveSelectionPoolAtLevel.addAll(allLevelUpMoves.get(preEvo.getNumber())
                        .stream()
                        .filter(ml -> ml.level <= tp.level)
                        .filter(ml -> this.random.nextDouble() < preEvoMoveProbability)
                        .map(ml -> moves.get(ml.move))
                        .distinct().toList());
            }
        }

        // TM Moves
        boolean[] tmCompat = allTMCompat.get(romHandler.getAltFormeOfPokemon(tp.pokemon, tp.forme));
        for (int tmMove: allTMMoves) {
            if (tmCompat[allTMMoves.indexOf(tmMove) + 1]) {
                Move thisMove = moves.get(tmMove);
                if (thisMove.power > 1 && tp.level * 3 > thisMove.power * thisMove.hitCount &&
                        this.random.nextDouble() < tmMoveProbability) {
                    moveSelectionPoolAtLevel.add(thisMove);
                } else if ((thisMove.power <= 1 && this.random.nextInt(100) < tp.level) ||
                        this.random.nextInt(200) < tp.level) {
                    moveSelectionPoolAtLevel.add(thisMove);
                }
            }
        }

        // Move Tutor Moves
        if (romHandler.hasMoveTutors()) {
            boolean[] tutorCompat = allTutorCompat.get(romHandler.getAltFormeOfPokemon(tp.pokemon, tp.forme));
            for (int tutorMove: allTutorMoves) {
                if (tutorCompat[allTutorMoves.indexOf(tutorMove) + 1]) {
                    Move thisMove = moves.get(tutorMove);
                    if (thisMove.power > 1 && tp.level * 3 > thisMove.power * thisMove.hitCount &&
                            this.random.nextDouble() < tutorMoveProbability) {
                        moveSelectionPoolAtLevel.add(thisMove);
                    } else if ((thisMove.power <= 1 && this.random.nextInt(100) < tp.level) ||
                            this.random.nextInt(200) < tp.level) {
                        moveSelectionPoolAtLevel.add(thisMove);
                    }
                }
            }
        }

        // Egg Moves
        if (!cyclicEvolutions) {
            Pokemon firstEvo;
            if (romHandler.altFormesCanHaveDifferentEvolutions()) {
                firstEvo = romHandler.getAltFormeOfPokemon(tp.pokemon, tp.forme);
            } else {
                firstEvo = tp.pokemon;
            }
            while (!firstEvo.getEvolutionsTo().isEmpty()) {
                firstEvo = firstEvo.getEvolutionsTo().get(0).getFrom();
            }
            if (allEggMoves.get(firstEvo.getNumber()) != null) {
                moveSelectionPoolAtLevel.addAll(allEggMoves.get(firstEvo.getNumber())
                        .stream()
                        .filter(egm -> this.random.nextDouble() < eggMoveProbability)
                        .map(moves::get).toList());
            }
        }

        return moveSelectionPoolAtLevel.stream().distinct().collect(Collectors.toList());
    }
}
