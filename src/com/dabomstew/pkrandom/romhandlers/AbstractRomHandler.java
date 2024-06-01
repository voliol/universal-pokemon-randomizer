package com.dabomstew.pkrandom.romhandlers;

/*----------------------------------------------------------------------------*/
/*--  AbstractRomHandler.java - a base class for all rom handlers which     --*/
/*--                            implements the majority of the actual       --*/
/*--                            randomizer logic by building on the base    --*/
/*--                            getters & setters provided by each concrete --*/
/*--                            handler.                                    --*/
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

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

import com.dabomstew.pkrandom.*;
import com.dabomstew.pkrandom.constants.*;
import com.dabomstew.pkrandom.exceptions.RandomizationException;
import com.dabomstew.pkrandom.pokemon.*;

public abstract class AbstractRomHandler implements RomHandler {

    private boolean restrictionsSet;
    protected List<Pokemon> mainPokemonList;
    protected List<Pokemon> mainPokemonListInclFormes;
    private List<Pokemon> altFormesList;
    private List<MegaEvolution> megaEvolutionsList;
    private List<Pokemon> noLegendaryList, onlyLegendaryList, ultraBeastList;
    private List<Pokemon> noLegendaryListInclFormes, onlyLegendaryListInclFormes;
    private List<Pokemon> noLegendaryAltsList, onlyLegendaryAltsList;
    private List<Pokemon> pickedStarters;
    protected final Random random;
    private final Random cosmeticRandom;
    protected PrintStream logStream;
    private List<Pokemon> alreadyPicked = new ArrayList<>();
    private Map<Pokemon, Integer> placementHistory = new HashMap<>();
    private Map<Integer, Integer> itemPlacementHistory = new HashMap<>();
    private int fullyEvolvedRandomSeed;
    boolean isORAS = false;
    boolean isSM = false;
    int perfectAccuracy = 100;

    /* Constructor */

    public AbstractRomHandler(Random random, PrintStream logStream) {
        this.random = random;
        this.cosmeticRandom = RandomSource.cosmeticInstance();
        this.fullyEvolvedRandomSeed = -1;
        this.logStream = logStream;
    }

    /*
     * Public Methods, implemented here for all gens. Unlikely to be overridden.
     */

    public void setLog(PrintStream logStream) {
        this.logStream = logStream;
    }

    public void setPokemonPool(Settings settings) {
        GenRestrictions restrictions = null;
        if (settings != null) {
            restrictions = settings.getCurrentRestrictions();

            // restrictions should already be null if "Limit Pokemon" is disabled, but this is a safeguard
            if (!settings.isLimitPokemon()) {
                restrictions = null;
            }
        }

        restrictionsSet = true;
        mainPokemonList = this.allPokemonWithoutNull();
        mainPokemonListInclFormes = this.allPokemonInclFormesWithoutNull();
        altFormesList = this.getAltFormes();
        megaEvolutionsList = this.getMegaEvolutions();
        if (restrictions != null) {
            mainPokemonList = new ArrayList<>();
            mainPokemonListInclFormes = new ArrayList<>();
            megaEvolutionsList = new ArrayList<>();
            List<Pokemon> allPokemon = this.getPokemon();

            if (restrictions.allow_gen1) {
                addPokesFromRange(mainPokemonList, allPokemon, Species.bulbasaur, Species.mew);
            }

            if (restrictions.allow_gen2 && allPokemon.size() > Gen2Constants.pokemonCount) {
                addPokesFromRange(mainPokemonList, allPokemon, Species.chikorita, Species.celebi);
            }

            if (restrictions.allow_gen3 && allPokemon.size() > Gen3Constants.pokemonCount) {
                addPokesFromRange(mainPokemonList, allPokemon, Species.treecko, Species.deoxys);
            }

            if (restrictions.allow_gen4 && allPokemon.size() > Gen4Constants.pokemonCount) {
                addPokesFromRange(mainPokemonList, allPokemon, Species.turtwig, Species.arceus);
            }

            if (restrictions.allow_gen5 && allPokemon.size() > Gen5Constants.pokemonCount) {
                addPokesFromRange(mainPokemonList, allPokemon, Species.victini, Species.genesect);
            }

            if (restrictions.allow_gen6 && allPokemon.size() > Gen6Constants.pokemonCount) {
                addPokesFromRange(mainPokemonList, allPokemon, Species.chespin, Species.volcanion);
            }

            int maxGen7SpeciesID = isSM ? Species.marshadow : Species.zeraora;
            if (restrictions.allow_gen7 && allPokemon.size() > maxGen7SpeciesID) {
                addPokesFromRange(mainPokemonList, allPokemon, Species.rowlet, maxGen7SpeciesID);
            }

            // If the user specified it, add all the evolutionary relatives for everything in the mainPokemonList
            if (restrictions.allow_evolutionary_relatives) {
                addEvolutionaryRelatives(mainPokemonList);
            }

            // Now that mainPokemonList has all the selected Pokemon, update mainPokemonListInclFormes too
            addAllPokesInclFormes(mainPokemonList, mainPokemonListInclFormes);

            // Populate megaEvolutionsList with all of the mega evolutions that exist in the pool
            List<MegaEvolution> allMegaEvolutions = this.getMegaEvolutions();
            for (MegaEvolution megaEvo : allMegaEvolutions) {
                if (mainPokemonListInclFormes.contains(megaEvo.to)) {
                    megaEvolutionsList.add(megaEvo);
                }
            }
        }

        noLegendaryList = new ArrayList<>();
        noLegendaryListInclFormes = new ArrayList<>();
        onlyLegendaryList = new ArrayList<>();
        onlyLegendaryListInclFormes = new ArrayList<>();
        noLegendaryAltsList = new ArrayList<>();
        onlyLegendaryAltsList = new ArrayList<>();
        ultraBeastList = new ArrayList<>();

        for (Pokemon p : mainPokemonList) {
            if (p.isLegendary()) {
                onlyLegendaryList.add(p);
            } else if (p.isUltraBeast()) {
                ultraBeastList.add(p);
            } else {
                noLegendaryList.add(p);
            }
        }
        for (Pokemon p : mainPokemonListInclFormes) {
            if (p.isLegendary()) {
                onlyLegendaryListInclFormes.add(p);
            } else if (!ultraBeastList.contains(p)) {
                noLegendaryListInclFormes.add(p);
            }
        }
        for (Pokemon f : altFormesList) {
            if (f.isLegendary()) {
                onlyLegendaryAltsList.add(f);
            } else {
                noLegendaryAltsList.add(f);
            }
        }

        for (Pokemon p : this.allPokemonInclFormesWithoutNull()) {
            p.saveOriginalData();
        }
    }

    private void addPokesFromRange(List<Pokemon> pokemonPool, List<Pokemon> allPokemon, int range_min, int range_max) {
        for (int i = range_min; i <= range_max; i++) {
            if (!pokemonPool.contains(allPokemon.get(i))) {
                pokemonPool.add(allPokemon.get(i));
            }
        }
    }

    /**
     * Deprecated. Use PokemonSet and its addAllFamilies() method instead.
     * TODO: remove usages
     */
    private void addEvolutionaryRelatives(List<Pokemon> pokemonPool) {
        Set<Pokemon> newPokemon = new TreeSet<>();
        for (Pokemon pk : pokemonPool) {
            List<Pokemon> evolutionaryRelatives = getEvolutionaryRelatives(pk);
            for (Pokemon relative : evolutionaryRelatives) {
                if (!pokemonPool.contains(relative) && !newPokemon.contains(relative)) {
                    newPokemon.add(relative);
                }
            }
        }

        pokemonPool.addAll(newPokemon);
    }

    //TODO: implement this into PokemonSet
    //...preferably in a more elegant way.
    private void addAllPokesInclFormes(List<Pokemon> pokemonPool, List<Pokemon> pokemonPoolInclFormes) {
        List<Pokemon> altFormes = this.getAltFormes();
        for (int i = 0; i < pokemonPool.size(); i++) {
            Pokemon currentPokemon = pokemonPool.get(i);
            if (!pokemonPoolInclFormes.contains(currentPokemon)) {
                pokemonPoolInclFormes.add(currentPokemon);
            }
            for (int j = 0; j < altFormes.size(); j++) {
                Pokemon potentialAltForme = altFormes.get(j);
                if (potentialAltForme.baseForme != null && potentialAltForme.baseForme.number == currentPokemon.number) {
                    pokemonPoolInclFormes.add(potentialAltForme);
                }
            }
        }
    }

    @Override
    public void shufflePokemonStats(Settings settings) {
        boolean evolutionSanity = settings.isBaseStatsFollowEvolutions();
        boolean megaEvolutionSanity = settings.isBaseStatsFollowMegaEvolutions();

        if (evolutionSanity) {
            copyUpEvolutionsHelper(pk -> pk.shuffleStats(AbstractRomHandler.this.random),
                    (evFrom, evTo, toMonIsFinalEvo) -> evTo.copyShuffledStatsUpEvolution(evFrom)
            );
        } else {
            List<Pokemon> allPokes = this.getPokemonInclFormes();
            for (Pokemon pk : allPokes) {
                if (pk != null) {
                    pk.shuffleStats(this.random);
                }
            }
        }

        List<Pokemon> allPokes = this.getPokemonInclFormes();
        for (Pokemon pk : allPokes) {
            if (pk != null && pk.actuallyCosmetic) {
                pk.copyBaseFormeBaseStats(pk.baseForme);
            }
        }

        if (megaEvolutionSanity) {
            List<MegaEvolution> allMegaEvos = getMegaEvolutions();
            for (MegaEvolution megaEvo: allMegaEvos) {
                if (megaEvo.from.megaEvolutionsFrom.size() > 1) continue;
                megaEvo.to.copyShuffledStatsUpEvolution(megaEvo.from);
            }
        }
    }

    @Override
    public void randomizePokemonStats(Settings settings) {
        boolean evolutionSanity = settings.isBaseStatsFollowEvolutions();
        boolean megaEvolutionSanity = settings.isBaseStatsFollowMegaEvolutions();
        boolean assignEvoStatsRandomly = settings.isAssignEvoStatsRandomly();

        if (evolutionSanity) {
            if (assignEvoStatsRandomly) {
                copyUpEvolutionsHelper(pk -> pk.randomizeStatsWithinBST(AbstractRomHandler.this.random),
                        (evFrom, evTo, toMonIsFinalEvo) -> evTo.assignNewStatsForEvolution(evFrom, this.random),
                        (evFrom, evTo, toMonIsFinalEvo) -> evTo.assignNewStatsForEvolution(evFrom, this.random),
                        true
                );
            } else {
                copyUpEvolutionsHelper(pk -> pk.randomizeStatsWithinBST(AbstractRomHandler.this.random),
                        (evFrom, evTo, toMonIsFinalEvo) -> evTo.copyRandomizedStatsUpEvolution(evFrom),
                        (evFrom, evTo, toMonIsFinalEvo) -> evTo.assignNewStatsForEvolution(evFrom, this.random),
                        true
                );
            }
        } else {
            List<Pokemon> allPokes = this.getPokemonInclFormes();
            for (Pokemon pk : allPokes) {
                if (pk != null) {
                    pk.randomizeStatsWithinBST(this.random);
                }
            }
        }

        List<Pokemon> allPokes = this.getPokemonInclFormes();
        for (Pokemon pk : allPokes) {
            if (pk != null && pk.actuallyCosmetic) {
                pk.copyBaseFormeBaseStats(pk.baseForme);
            }
        }

        if (megaEvolutionSanity) {
            List<MegaEvolution> allMegaEvos = getMegaEvolutions();
            for (MegaEvolution megaEvo: allMegaEvos) {
                if (megaEvo.from.megaEvolutionsFrom.size() > 1 || assignEvoStatsRandomly) {
                    megaEvo.to.assignNewStatsForEvolution(megaEvo.from, this.random);
                } else {
                    megaEvo.to.copyRandomizedStatsUpEvolution(megaEvo.from);
                }
            }
        }
    }

    @Override
    public void updatePokemonStats(Settings settings) {
        int generation = settings.getUpdateBaseStatsToGeneration();

        List<Pokemon> pokes = getPokemonInclFormes();

        for (int gen = 6; gen <= generation; gen++) {
            Map<Integer,StatChange> statChanges = getUpdatedPokemonStats(gen);

            for (int i = 1; i < pokes.size(); i++) {
                StatChange changedStats = statChanges.get(i);
                if (changedStats != null) {
                    int statNum = 0;
                    if ((changedStats.stat & Stat.HP.val) != 0) {
                        pokes.get(i).hp = changedStats.values[statNum];
                        statNum++;
                    }
                    if ((changedStats.stat & Stat.ATK.val) != 0) {
                        pokes.get(i).attack = changedStats.values[statNum];
                        statNum++;
                    }
                    if ((changedStats.stat & Stat.DEF.val) != 0) {
                        pokes.get(i).defense = changedStats.values[statNum];
                        statNum++;
                    }
                    if ((changedStats.stat & Stat.SPATK.val) != 0) {
                        if (generationOfPokemon() != 1) {
                            pokes.get(i).spatk = changedStats.values[statNum];
                        }
                        statNum++;
                    }
                    if ((changedStats.stat & Stat.SPDEF.val) != 0) {
                        if (generationOfPokemon() != 1) {
                            pokes.get(i).spdef = changedStats.values[statNum];
                        }
                        statNum++;
                    }
                    if ((changedStats.stat & Stat.SPEED.val) != 0) {
                        pokes.get(i).speed = changedStats.values[statNum];
                        statNum++;
                    }
                    if ((changedStats.stat & Stat.SPECIAL.val) != 0) {
                        pokes.get(i).special = changedStats.values[statNum];
                    }
                }
            }
        }
    }

    public Pokemon randomPokemon() {
        checkPokemonRestrictions();
        return mainPokemonList.get(this.random.nextInt(mainPokemonList.size()));
    }

    @Override
    public Pokemon randomPokemonInclFormes() {
        checkPokemonRestrictions();
        return mainPokemonListInclFormes.get(this.random.nextInt(mainPokemonListInclFormes.size()));
    }

    @Override
    public Pokemon randomNonLegendaryPokemon() {
        checkPokemonRestrictions();
        return noLegendaryList.get(this.random.nextInt(noLegendaryList.size()));
    }

    private Pokemon randomNonLegendaryPokemonInclFormes() {
        checkPokemonRestrictions();
        return noLegendaryListInclFormes.get(this.random.nextInt(noLegendaryListInclFormes.size()));
    }

    @Override
    public Pokemon randomLegendaryPokemon() {
        checkPokemonRestrictions();
        return onlyLegendaryList.get(this.random.nextInt(onlyLegendaryList.size()));
    }

    private List<Pokemon> twoEvoPokes;

    @Override
    public Pokemon random2EvosPokemon(boolean allowAltFormes) {
        if (twoEvoPokes == null) {
            // Prepare the list
            twoEvoPokes = new ArrayList<>();
            List<Pokemon> allPokes =
                    allowAltFormes ?
                            this.getPokemonInclFormes()
                                    .stream()
                                    .filter(pk -> pk == null || !pk.actuallyCosmetic)
                                    .collect(Collectors.toList()) :
                            this.getPokemon();
            for (Pokemon pk : allPokes) {
                if (pk != null && pk.evolutionsTo.size() == 0 && pk.evolutionsFrom.size() > 0) {
                    // Potential candidate
                    for (Evolution ev : pk.evolutionsFrom) {
                        // If any of the targets here evolve, the original
                        // Pokemon has 2+ stages.
                        if (ev.to.evolutionsFrom.size() > 0) {
                            twoEvoPokes.add(pk);
                            break;
                        }
                    }
                }
            }
        }
        return twoEvoPokes.get(this.random.nextInt(twoEvoPokes.size()));
    }

    @Override
    public Type randomType() {
        Type t = Type.randomType(this.random);
        while (!typeInGame(t)) {
            t = Type.randomType(this.random);
        }
        return t;
    }

    @Override
    public void randomizePokemonTypes(Settings settings) {
        boolean evolutionSanity = settings.getTypesMod() == Settings.TypesMod.RANDOM_FOLLOW_EVOLUTIONS;
        boolean megaEvolutionSanity = settings.isTypesFollowMegaEvolutions();
        boolean dualTypeOnly = settings.isDualTypeOnly();

        List<Pokemon> allPokes = this.getPokemonInclFormes();
        if (evolutionSanity) {
            // Type randomization with evolution sanity
            copyUpEvolutionsHelper(pk -> {
                // Step 1: Basic or Excluded From Copying Pokemon
                // A Basic/EFC pokemon has a 35% chance of a second type if
                // it has an evolution that copies type/stats, a 50% chance
                // otherwise
                pk.primaryType = randomType();
                pk.secondaryType = null;
                if (pk.evolutionsFrom.size() == 1 && pk.evolutionsFrom.get(0).carryStats) {
                    if (AbstractRomHandler.this.random.nextDouble() < 0.35 || dualTypeOnly) {
                        pk.secondaryType = randomType();
                        while (pk.secondaryType == pk.primaryType) {
                            pk.secondaryType = randomType();
                        }
                    }
                } else {
                    if (AbstractRomHandler.this.random.nextDouble() < 0.5 || dualTypeOnly) {
                        pk.secondaryType = randomType();
                        while (pk.secondaryType == pk.primaryType) {
                            pk.secondaryType = randomType();
                        }
                    }
                }
            }, (evFrom, evTo, toMonIsFinalEvo) -> {
                evTo.primaryType = evFrom.primaryType;
                evTo.secondaryType = evFrom.secondaryType;

                if (evTo.secondaryType == null) {
                    double chance = toMonIsFinalEvo ? 0.25 : 0.15;
                    if (AbstractRomHandler.this.random.nextDouble() < chance || dualTypeOnly) {
                        evTo.secondaryType = randomType();
                        while (evTo.secondaryType == evTo.primaryType) {
                            evTo.secondaryType = randomType();
                        }
                    }
                }
            });
        } else {
            // Entirely random types
            for (Pokemon pkmn : allPokes) {
                if (pkmn != null) {
                    pkmn.primaryType = randomType();
                    pkmn.secondaryType = null;
                    if (this.random.nextDouble() < 0.5||settings.isDualTypeOnly()) {
                        pkmn.secondaryType = randomType();
                        while (pkmn.secondaryType == pkmn.primaryType) {
                            pkmn.secondaryType = randomType();
                        }
                    }
                }
            }
        }

        for (Pokemon pk : allPokes) {
            if (pk != null && pk.actuallyCosmetic) {
                pk.primaryType = pk.baseForme.primaryType;
                pk.secondaryType = pk.baseForme.secondaryType;
            }
        }

        if (megaEvolutionSanity) {
            List<MegaEvolution> allMegaEvos = getMegaEvolutions();
            for (MegaEvolution megaEvo: allMegaEvos) {
                if (megaEvo.from.megaEvolutionsFrom.size() > 1) continue;
                megaEvo.to.primaryType = megaEvo.from.primaryType;
                megaEvo.to.secondaryType = megaEvo.from.secondaryType;

                if (megaEvo.to.secondaryType == null) {
                    if (this.random.nextDouble() < 0.25) {
                        megaEvo.to.secondaryType = randomType();
                        while (megaEvo.to.secondaryType == megaEvo.to.primaryType) {
                            megaEvo.to.secondaryType = randomType();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void randomizeAbilities(Settings settings) {
        boolean evolutionSanity = settings.isAbilitiesFollowEvolutions();
        boolean allowWonderGuard = settings.isAllowWonderGuard();
        boolean banTrappingAbilities = settings.isBanTrappingAbilities();
        boolean banNegativeAbilities = settings.isBanNegativeAbilities();
        boolean banBadAbilities = settings.isBanBadAbilities();
        boolean megaEvolutionSanity = settings.isAbilitiesFollowMegaEvolutions();
        boolean weighDuplicatesTogether = settings.isWeighDuplicateAbilitiesTogether();
        boolean ensureTwoAbilities = settings.isEnsureTwoAbilities();
        boolean doubleBattleMode = settings.isDoubleBattleMode();

        // Abilities don't exist in some games...
        if (this.abilitiesPerPokemon() == 0) {
            return;
        }

        final boolean hasDWAbilities = (this.abilitiesPerPokemon() == 3);

        final List<Integer> bannedAbilities = this.getUselessAbilities();

        if (!allowWonderGuard) {
            bannedAbilities.add(Abilities.wonderGuard);
        }

        if (banTrappingAbilities) {
            bannedAbilities.addAll(GlobalConstants.battleTrappingAbilities);
        }

        if (banNegativeAbilities) {
            bannedAbilities.addAll(GlobalConstants.negativeAbilities);
        }

        if (banBadAbilities) {
            bannedAbilities.addAll(GlobalConstants.badAbilities);
            if (!doubleBattleMode) {
                bannedAbilities.addAll(GlobalConstants.doubleBattleAbilities);
            }
        }

        if (weighDuplicatesTogether) {
            bannedAbilities.addAll(GlobalConstants.duplicateAbilities);
            if (generationOfPokemon() == 3) {
                bannedAbilities.add(Gen3Constants.airLockIndex); // Special case for Air Lock in Gen 3
            }
        }

        final int maxAbility = this.highestAbilityIndex();

        if (evolutionSanity) {
            // copy abilities straight up evolution lines
            // still keep WG as an exception, though

            copyUpEvolutionsHelper(pk -> {
                if (pk.ability1 != Abilities.wonderGuard
                        && pk.ability2 != Abilities.wonderGuard
                        && pk.ability3 != Abilities.wonderGuard) {
                    // Pick first ability
                    pk.ability1 = pickRandomAbility(maxAbility, bannedAbilities, weighDuplicatesTogether);

                    // Second ability?
                    if (ensureTwoAbilities || AbstractRomHandler.this.random.nextDouble() < 0.5) {
                        // Yes, second ability
                        pk.ability2 = pickRandomAbility(maxAbility, bannedAbilities, weighDuplicatesTogether,
                                pk.ability1);
                    } else {
                        // Nope
                        pk.ability2 = 0;
                    }

                    // Third ability?
                    if (hasDWAbilities) {
                        pk.ability3 = pickRandomAbility(maxAbility, bannedAbilities, weighDuplicatesTogether,
                                pk.ability1, pk.ability2);
                    }
                }
            }, (evFrom, evTo, toMonIsFinalEvo) -> {
                if (evTo.ability1 != Abilities.wonderGuard
                        && evTo.ability2 != Abilities.wonderGuard
                        && evTo.ability3 != Abilities.wonderGuard) {
                    evTo.ability1 = evFrom.ability1;
                    evTo.ability2 = evFrom.ability2;
                    evTo.ability3 = evFrom.ability3;
                }
            });
        } else {
            List<Pokemon> allPokes = this.getPokemonInclFormes();
            for (Pokemon pk : allPokes) {
                if (pk == null) {
                    continue;
                }

                // Don't remove WG if already in place.
                if (pk.ability1 != Abilities.wonderGuard
                        && pk.ability2 != Abilities.wonderGuard
                        && pk.ability3 != Abilities.wonderGuard) {
                    // Pick first ability
                    pk.ability1 = this.pickRandomAbility(maxAbility, bannedAbilities, weighDuplicatesTogether);

                    // Second ability?
                    if (ensureTwoAbilities || this.random.nextDouble() < 0.5) {
                        // Yes, second ability
                        pk.ability2 = this.pickRandomAbility(maxAbility, bannedAbilities, weighDuplicatesTogether,
                                pk.ability1);
                    } else {
                        // Nope
                        pk.ability2 = 0;
                    }

                    // Third ability?
                    if (hasDWAbilities) {
                        pk.ability3 = pickRandomAbility(maxAbility, bannedAbilities, weighDuplicatesTogether,
                                pk.ability1, pk.ability2);
                    }
                }
            }
        }

        List<Pokemon> allPokes = this.getPokemonInclFormes();
        for (Pokemon pk : allPokes) {
            if (pk != null && pk.actuallyCosmetic) {
                pk.copyBaseFormeAbilities(pk.baseForme);
            }
        }

        if (megaEvolutionSanity) {
            List<MegaEvolution> allMegaEvos = this.getMegaEvolutions();
            for (MegaEvolution megaEvo: allMegaEvos) {
                if (megaEvo.from.megaEvolutionsFrom.size() > 1) continue;
                megaEvo.to.ability1 = megaEvo.from.ability1;
                megaEvo.to.ability2 = megaEvo.from.ability2;
                megaEvo.to.ability3 = megaEvo.from.ability3;
            }
        }
    }

    private int pickRandomAbilityVariation(int selectedAbility, int... alreadySetAbilities) {
        int newAbility = selectedAbility;

        while (true) {
            Map<Integer, List<Integer>> abilityVariations = getAbilityVariations();
            for (int baseAbility: abilityVariations.keySet()) {
                if (selectedAbility == baseAbility) {
                    List<Integer> variationsForThisAbility = abilityVariations.get(selectedAbility);
                    newAbility = variationsForThisAbility.get(this.random.nextInt(variationsForThisAbility.size()));
                    break;
                }
            }

            boolean repeat = false;
            for (int alreadySetAbility : alreadySetAbilities) {
                if (alreadySetAbility == newAbility) {
                    repeat = true;
                    break;
                }
            }

            if (!repeat) {
                break;
            }


        }

        return newAbility;
    }

    private int pickRandomAbility(int maxAbility, List<Integer> bannedAbilities, boolean useVariations,
                                  int... alreadySetAbilities) {
        int newAbility;

        while (true) {
            newAbility = this.random.nextInt(maxAbility) + 1;

            if (bannedAbilities.contains(newAbility)) {
                continue;
            }

            boolean repeat = false;
            for (int alreadySetAbility : alreadySetAbilities) {
                if (alreadySetAbility == newAbility) {
                    repeat = true;
                    break;
                }
            }

            if (!repeat) {
                if (useVariations) {
                    newAbility = pickRandomAbilityVariation(newAbility, alreadySetAbilities);
                }
                break;
            }
        }

        return newAbility;
    }

    //Wild Pokemon randomization mains:

    /**
     * Randomizes wild Pokemon, adding as many different Pokemon to each area as possible.
     * @param settings The current settings.
     */
    @Override
    public void randomEncounters(Settings settings) {
        boolean useTimeOfDay = settings.isUseTimeBasedEncounters();
        boolean catchEmAll = settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.CATCH_EM_ALL;
        boolean usePowerLevels = settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.SIMILAR_STRENGTH;
        boolean noTyping = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.NONE;
        boolean typeThemed = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.THEMED_AREAS;
        boolean keepPrimary = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.KEEP_PRIMARY;
        boolean keepThemes = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.KEEP_THEMES;
        boolean balanceShakingGrass = settings.isBalanceShakingGrass();

        int levelModifier = getWildLevelModifier(settings);

        List<EncounterSet> currentEncounters = this.getEncounters(useTimeOfDay);

        if (isORAS) {
            List<EncounterSet> collapsedEncounters = collapseAreasORAS(currentEncounters);
            area1to1EncountersImpl(collapsedEncounters, settings);
            enhanceRandomEncountersORAS(collapsedEncounters, settings);
            setEncounters(useTimeOfDay, currentEncounters);
            return;
        }

        // Randomize the order encounter sets are randomized in.
        // Leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<EncounterSet> scrambledEncounters = new ArrayList<>(currentEncounters);
        Collections.shuffle(scrambledEncounters, this.random);

        PokemonSet fullWildSet = getWildPokemonPool(settings);
        PokemonSet activeWildSet = new PokemonSet(fullWildSet);
        PokemonSet bannedSet = getWildPokemonBannedSet(settings);

        //iterate over areas
        for (EncounterSet area : scrambledEncounters) {

            //make area theme if applicable
            Type areaTheme;
            if(keepThemes) {
                areaTheme = pokemonInArea(area).findOriginalSharedType();
            } else if (typeThemed) {
                areaTheme = generateTypeForArea(activeWildSet, fullWildSet, 1);
            } else {
                areaTheme = null;
            }

            //generate available pool
            PokemonSet availablePokemon = getAvailablePokemonForArea(area, activeWildSet, areaTheme);

            //iterate over encounters
            for (Encounter enc : area.encounters) {
                // In Catch 'Em All mode, don't randomize encounters for Pokemon that are banned for
                // wild encounters. Otherwise, it may be impossible to obtain this Pokemon unless it
                // randomly appears as a static or unless it becomes a random evolution.
                if (catchEmAll && bannedSet.contains(enc.pokemon)) {
                    continue;
                }

                //handle empty available list
                if(availablePokemon.isEmpty()) {
                    //reset if fully empty
                    if(activeWildSet.isEmpty()) {
                        activeWildSet = new PokemonSet(fullWildSet);
                    }

                    availablePokemon = getAvailablePokemonForArea(area, fullWildSet, areaTheme);

                    if(availablePokemon.isEmpty()) {
                        String message = "No wild Pokemon";
                        if(areaTheme != null) {
                            message += " of type " + areaTheme;
                        }
                        message += " available for area " + area.displayName + "!";
                        throw new RandomizationException(message);
                    }
                }

                //find type for keepPrimary
                PokemonSet finalPokemonPool;
                if(keepPrimary) {
                    Type typeToChoose = enc.pokemon.originalPrimaryType;
                    finalPokemonPool = availablePokemon.getPokemonOfType(typeToChoose);

                    //pull from broader pool if there's none of the right type in the narrow pool
                    if (finalPokemonPool.isEmpty()) {
                        finalPokemonPool = getAvailablePokemonForArea(area, fullWildSet, typeToChoose);
                        if (finalPokemonPool.isEmpty()) {
                            throw new RandomizationException("Unable to replace a wild Pokemon of type "
                                    + typeToChoose + "!");
                        }
                    }
                } else {
                    finalPokemonPool = availablePokemon;
                }


                //Choose a Pokemon from the pool
                Pokemon picked;
                if(usePowerLevels) {
                    if(balanceShakingGrass && area.displayName.contains("Shaking")) {
                        picked = pickWildPowerLvlReplacement(finalPokemonPool, enc.pokemon, Math.min(5, finalPokemonPool.size() / 4),
                                false,  (enc.level + enc.maxLevel) / 2);
                    } else {
                        picked = pickWildPowerLvlReplacement(finalPokemonPool, enc.pokemon, Math.min(5, finalPokemonPool.size() / 4),
                                false, 100);
                    }
                } else {
                    picked = finalPokemonPool.randomPokemon(this.random);
                }

                //assign to encounter, remove from available if using catchEmAll
                enc.pokemon = picked;
                setFormeForEncounter(enc, enc.pokemon);
                if(catchEmAll) {
                    availablePokemon.remove(picked);
                    activeWildSet.remove(picked);
                }

                // apply level scaling if needed
                adjustEncounterLevel(enc, levelModifier);
            }
        }

        //assign randomized encounters to ROM
        setEncounters(useTimeOfDay, currentEncounters);
    }

    /**
     * Randomizes wild Pokemon, ensuring that for each area in the game,
     * each Pokemon originally present is replaced by exactly one new Pokemon.
     * @param settings The current settings.
     */
    @Override
    public void area1to1Encounters(Settings settings) {
        boolean useTimeOfDay = settings.isUseTimeBasedEncounters();

        List<EncounterSet> currentEncounters = this.getEncounters(useTimeOfDay);
        if (isORAS) {
            List<EncounterSet> collapsedEncounters = collapseAreasORAS(currentEncounters);
            area1to1EncountersImpl(collapsedEncounters, settings);
            setEncounters(useTimeOfDay, currentEncounters);
            return;
        } else {
            area1to1EncountersImpl(currentEncounters, settings);
            setEncounters(useTimeOfDay, currentEncounters);
        }
    }

    /**
     * Does the actual work of 1-to-1 randomization.
     * @param currentEncounters The list of areas to randomize.
     * @param settings The current settings.
     */
    private void area1to1EncountersImpl(List<EncounterSet> currentEncounters, Settings settings) {
        boolean catchEmAll = settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.CATCH_EM_ALL;
        boolean usePowerLevels = settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.SIMILAR_STRENGTH;
        boolean noTyping = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.NONE;
        boolean keepPrimary = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.KEEP_PRIMARY;
        boolean typeThemed = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.THEMED_AREAS;
        boolean keepThemes = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.KEEP_THEMES;

        int levelModifier = getWildLevelModifier(settings);

        // Randomize the order encounter sets are randomized in.
        // Leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<EncounterSet> scrambledEncounters = new ArrayList<>(currentEncounters);
        Collections.shuffle(scrambledEncounters, this.random);

        //hate that we need this
        //but as far as I can tell, we do.
        PokemonSet bannedSet = this.getWildPokemonBannedSet(settings);

        PokemonSet fullWildSet = this.getWildPokemonPool(settings);
        PokemonSet activeWildSet = new PokemonSet(fullWildSet); //for catch-em-all


        //iterate over areas
        for (EncounterSet area : scrambledEncounters) {
            PokemonSet inArea = pokemonInArea(area);
            Map<Pokemon, Pokemon> areaMap = new HashMap<>();

            Type areaTheme;
            if(keepThemes) {
                areaTheme = inArea.findOriginalSharedType();
            } else if (typeThemed) {
                areaTheme = this.generateTypeForArea(activeWildSet, fullWildSet, inArea.size());
            } else {
                areaTheme = null;
            }

            // generate pickable list
            PokemonSet availablePokemon = getAvailablePokemonForArea(area, activeWildSet, areaTheme);

            //iterate over Pokemon in area to generate map
            for (Pokemon areaPk : inArea) {
                if (catchEmAll && bannedSet.contains(areaPk)) {
                    //prevent using up a Pokemon without creating an encounter
                    //(since "banned" Pokemon are left as-is in Catch-em-all)
                    continue;
                }

                //handle empty available list
                if(availablePokemon.isEmpty()) {
                    //reset if fully empty
                    if(activeWildSet.isEmpty()) {
                        activeWildSet = new PokemonSet(fullWildSet);
                    }

                    availablePokemon = getAvailablePokemonForArea(area, fullWildSet, areaTheme, areaMap.values());

                    if(availablePokemon.isEmpty()) {
                        String message = "Insufficient wild Pokemon";
                        if(areaTheme != null) {
                            message += " of type " + areaTheme;
                        }
                        message += " available for area " + area.displayName + "!";
                        throw new RandomizationException(message);
                    }
                }

                //find type for keepPrimary
                PokemonSet finalPokemonPool;
                if(keepPrimary) {
                    Type typeToChoose = areaPk.originalPrimaryType;
                    finalPokemonPool = availablePokemon.getPokemonOfType(typeToChoose);

                    //pull from broader pool if there's none of the right type in the narrow pool
                    if (finalPokemonPool.isEmpty()) {
                        finalPokemonPool = getAvailablePokemonForArea(area, fullWildSet, typeToChoose);
                        if (finalPokemonPool.isEmpty()) {
                            throw new RandomizationException("Unable to replace a wild Pokemon of type "
                                    + typeToChoose + "!");
                        }
                    }
                } else {
                    finalPokemonPool = availablePokemon;
                }

                //Choose a Pokemon from the pickable list
                Pokemon picked;
                if(usePowerLevels) {
                    picked = pickWildPowerLvlReplacement(finalPokemonPool, areaPk, Math.min(5, finalPokemonPool.size() / 4),
                            false, 100);
                } else {
                    picked = finalPokemonPool.randomPokemon(random);
                }

                //add to map & remove from pickable
                areaMap.put(areaPk, picked);
                finalPokemonPool.remove(picked);
                if(catchEmAll) {
                    activeWildSet.remove(picked);
                }
            }
            //end iteration over Pokemon

            //iterate over encounters in area to apply map
            for (Encounter enc : area.encounters) {

                //apply level scaling if needed
                adjustEncounterLevel(enc, levelModifier);

                // In Catch 'Em All mode, don't randomize encounters for Pokemon that are banned for
                // wild encounters. Otherwise, it may be impossible to obtain this Pokemon unless it
                // randomly appears as a static or unless it becomes a random evolution.
                if (catchEmAll && bannedSet.contains(enc.pokemon)) {
                    continue;
                }

                // Apply the map
                enc.pokemon = areaMap.get(enc.pokemon);
                setFormeForEncounter(enc, enc.pokemon);
            }
        }

    }

    /**
     * Randomizes wild Pokemon, ensuring that each Pokemon in the original game
     * is replaced by the same random Pokemon in each area it occurs.
     * @param settings The current settings.
     */
    @Override
    public void game1to1Encounters(Settings settings) {
        boolean useTimeOfDay = settings.isUseTimeBasedEncounters();
        boolean usePowerLevels = settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.SIMILAR_STRENGTH;
        boolean keepPrimary = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.KEEP_PRIMARY;

        int levelModifier = getWildLevelModifier(settings);

        //order doesn't matter, here, since we're not performing any randomization yet.
        List<EncounterSet> currentEncounters = this.getEncounters(useTimeOfDay);
        //List<EncounterSet> scrambledEncounters = new ArrayList<>(currentEncounters);
        //Collections.shuffle(scrambledEncounters, this.random);

        /**
         * A class which stores some information about the areas a Pokemon was found in,
         * in order to allow us to use this information later.
         */
        class RandomizationInformation {
            Set<Type> possibleThemes; //For later use.
            PokemonSet bannedPokemon;
            Pokemon pokemon;

            RandomizationInformation(Pokemon pk, Collection<Pokemon> banned) {
                possibleThemes = EnumSet.noneOf(Type.class);
                bannedPokemon = new PokemonSet(banned);
                pokemon = pk;
            }

            /**
             * Gets the type of this Pokemon's area theming.
             * If there are two themes, it will default to the primary type.
             * If there are no themes, it will default to the primary only if defaultToPrimary is true;
             * otherwise, it will default to null.
             * @param defaultToPrimary Whether the type should default to the Pokemon's primary type
             *                         if there are no themes.
             * @return The type that should be used, or null for any type.
             */
            Type getType(boolean defaultToPrimary) {
                int themeCount = possibleThemes.size();
                if(themeCount == 0) {
                    if(defaultToPrimary) {
                        return pokemon.originalPrimaryType;
                    } else {
                        return null;
                    }
                } else if(themeCount == 1) {
                    return possibleThemes.iterator().next();
                } else if(themeCount == 2) {
                    return pokemon.originalPrimaryType;
                } else {
                    throw new IllegalStateException("Too many themes for one Pokemon!");
                }
            }
        }

        // Get the pools of allowed and banned wild Pokemon.
        PokemonSet fullWildPool = getWildPokemonPool(settings);
        PokemonSet activeWildPool = new PokemonSet(fullWildPool);
        PokemonSet bannedSet = getWildPokemonBannedSet(settings);

        // Initialize maps
        Map<Pokemon, RandomizationInformation> infoMap = new HashMap<>();
        Map<Pokemon, Pokemon> translateMap = new HashMap<>();

        //Iterate over areas to obtain list of source Pokemon
        for (EncounterSet area : currentEncounters) {
            PokemonSet inArea = pokemonInArea(area);
            //area theme determination goes here.

            for (Pokemon areaPK : inArea) {
                if (bannedSet.contains(areaPK)) {
                    //banned Pokemon should be mapped to themselves
                    translateMap.put(areaPK, areaPK);
                    continue;
                }

                if (infoMap.containsKey(areaPK)) {
                    RandomizationInformation info = infoMap.get(areaPK);
                    info.bannedPokemon.addAll(area.bannedPokemon);
                } else {
                    RandomizationInformation info = new RandomizationInformation(areaPK, area.bannedPokemon);
                    infoMap.put(areaPK, info);
                }
            }
        }

        //shuffle list of Pokemon with info in preparation for randomization
        List<RandomizationInformation> infoList = new ArrayList<>(infoMap.values());
        Collections.shuffle(infoList, this.random);

        //Iterate over Pokemon (with info attached)
        for (RandomizationInformation info : infoList) {
            //generate available set
            PokemonSet availablePool = getAvailablePokemon(activeWildPool, info.getType(keepPrimary), info.bannedPokemon);

            //check for empty pool
            if(availablePool.isEmpty()) {
                if(activeWildPool.isEmpty()) {
                    activeWildPool = new PokemonSet(fullWildPool);
                }

                availablePool = getAvailablePokemon(fullWildPool, info.getType(keepPrimary), info.bannedPokemon);

                if(availablePool.isEmpty()) {
                    throw new RandomizationException("No wild Pokemon found to replace " + info.pokemon + "!");
                }
            }

            Pokemon picked;
            if(usePowerLevels) {
                picked = pickWildPowerLvlReplacement(availablePool, info.pokemon, Math.min(5, availablePool.size() / 4),
                        false, 100);
            } else {
                picked = availablePool.randomPokemon(this.random);
            }

            //add to map & remove from pickable
            translateMap.put(info.pokemon, picked);
            activeWildPool.remove(picked);
        }

        // Iterate over areas again
        for (EncounterSet area : currentEncounters) {

            //assign encounters using map
            for (Encounter enc : area.encounters) {
                Pokemon replacement = translateMap.get(enc.pokemon);

                if (area.bannedPokemon.contains(replacement)) {
                    // This should never happen, since we already checked all the areas' banned Pokemon.
                    throw new RuntimeException("Chose a banned Pokemon " + replacement +
                            " as replacement for " + enc.pokemon + "?! (Shouldn't be possible)");
                }

                // Finalize the encounter
                enc.pokemon = translateMap.get(enc.pokemon);
                setFormeForEncounter(enc, enc.pokemon);
                adjustEncounterLevel(enc, levelModifier);
            }
        }

        //assign randomized encounters to ROM
        setEncounters(useTimeOfDay, currentEncounters);

    }

    /**
     * Iterates over every wild Pokemon encounter in the game,
     * increasing their levels as appropriate for the current settings.
     * @param settings The current settings.
     */
    @Override
    public void onlyChangeWildLevels(Settings settings) {
        int levelModifier = getWildLevelModifier(settings);

        List<EncounterSet> currentEncounters = this.getEncounters(true);

        if (levelModifier != 0) {
            for (EncounterSet area : currentEncounters) {
                for (Encounter enc : area.encounters) {
                    adjustEncounterLevel(enc, levelModifier);
                }
            }
            setEncounters(true, currentEncounters);
        }
    }

    /**
     * After running Area 1-to-1, randomizes wild Pokemon further,
     * maximizing the amount that can be added to any location without
     * crashing the DexNav.
     * Only applies to Omega Ruby and Alpha Sapphire.
     * @param collapsedEncounters The set of encounters, with those belonging to
     *                           the same area collapsed into one EncounterSet.
     * @param settings The current settings.
     */
    private void enhanceRandomEncountersORAS(List<EncounterSet> collapsedEncounters, Settings settings) {
        boolean catchEmAll = settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.CATCH_EM_ALL;
        boolean usePowerLevels = settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.SIMILAR_STRENGTH;
        boolean typeThemed = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.THEMED_AREAS;
        boolean keepThemes = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.KEEP_THEMES;
        boolean keepPrimary = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.KEEP_PRIMARY;

        //get Pokemon pools
        PokemonSet fullWildPool = getWildPokemonPool(settings);
        PokemonSet activeWildPool = new PokemonSet(fullWildPool);
        PokemonSet bannedSet = getWildPokemonBannedSet(settings);

        Map<Integer, List<EncounterSet>> zonesToEncounters = mapZonesToEncounters(collapsedEncounters);
        //I'm not sure if there's a reason these aren't shuffled. For now I'll leave them alone.

        if(catchEmAll) {
            //remove the ones already assigned to encounters
            //(this was not done previously but. it should have been)
            for (List<EncounterSet> encountersInZone : zonesToEncounters.values()) {
                for (EncounterSet area : encountersInZone) {
                    activeWildPool.removeAll(pokemonInArea(area));
                }
            }
        }

        for (List<EncounterSet> encountersInZone : zonesToEncounters.values()) {
            int currentAreaIndex = -1;
            List<EncounterSet> nonRockSmashAreas = new ArrayList<>();
            Map<Integer, List<Integer>> areasAndEncountersToRandomize = new TreeMap<>();


            // Iterate over areas
            for (EncounterSet area : encountersInZone) {
                // Since Rock Smash Pokemon do not show up on DexNav, they can be fully randomized
                if (area.displayName.contains("Rock Smash")) {

                    Type areaTheme = null;
                    if(typeThemed || keepThemes) {
                        // try to use the theme already established
                        areaTheme = pokemonInArea(area).findSharedType();

                        if(areaTheme == null || fullWildPool.getCountOfType(areaTheme) == 0) {
                            generateTypeForArea(activeWildPool, fullWildPool, 1);
                        }
                        // this was previously area.encounters.size(),
                        // but there's no reason we can't have repeats.
                    }

                    // get available Pokemon
                    PokemonSet availablePool = getAvailablePokemonForArea(area, activeWildPool, areaTheme);

                    // iterate over encounters
                    for (Encounter enc : area.encounters) {

                        if (catchEmAll && !doesAnotherEncounterWithSamePokemonExistInArea(enc, area)) {
                            // we want to keep at least one of each encounter previously chosen
                            continue;
                        }

                        if(availablePool.isEmpty()) {
                            // reset the active pool ONLY if it's entirely empty.
                            if(activeWildPool.isEmpty()) {
                                activeWildPool = new PokemonSet(fullWildPool);
                            }

                            availablePool = getAvailablePokemonForArea(area, fullWildPool, areaTheme);

                            if(availablePool.isEmpty()) {
                                String message = "No wild Pokemon";
                                if(areaTheme != null) {
                                    message += " of type " + areaTheme;
                                }
                                message += " available for area " + area.displayName + "!";
                                throw new RandomizationException(message);
                            }
                        }

                        // get type-narrowed list for keep primary
                        PokemonSet finalPokemonPool;
                        if(keepPrimary) {
                            Type typeToChoose = enc.pokemon.originalPrimaryType;
                            finalPokemonPool = availablePool.getPokemonOfType(typeToChoose);

                            //pull from broader pool if there's none of the right type in the narrow pool
                            if (finalPokemonPool.isEmpty()) {
                                finalPokemonPool = getAvailablePokemonForArea(area, fullWildPool, typeToChoose);
                                if (finalPokemonPool.isEmpty()) {
                                    throw new RandomizationException("Unable to replace a wild Pokemon of type "
                                            + typeToChoose + "!");
                                }
                            }
                        } else {
                            finalPokemonPool = availablePool;
                        }

                        // Choose a Pokemon from the pickable list
                        Pokemon picked;
                        if(usePowerLevels) {
                            picked = pickWildPowerLvlReplacement(finalPokemonPool, enc.pokemon, Math.min(5, finalPokemonPool.size() / 4),
                                    false, 100);
                        } else {
                            picked = finalPokemonPool.randomPokemon(this.random);
                        }

                        // assign to encounter, remove from pickable if using catchEmAll
                        enc.pokemon = picked;
                        setFormeForEncounter(enc, enc.pokemon);
                        if(catchEmAll) {
                            availablePool.remove(picked);
                            activeWildPool.remove(picked);
                        }
                    }

                } else {
                    //put non-Rock Smash encounters in a queue to randomize piecemeal later
                    currentAreaIndex++;
                    nonRockSmashAreas.add(area);
                    List<Integer> encounterIndices = new ArrayList<>();
                    for (int i = 0; i < area.encounters.size(); i++) {
                        encounterIndices.add(i);
                    }
                    areasAndEncountersToRandomize.put(currentAreaIndex, encounterIndices);
                }
            }

            // Now, randomize non-Rock Smash Pokemon until we hit the threshold for DexNav
            int crashThreshold = computeDexNavCrashThreshold(encountersInZone);
            while (crashThreshold < 18 && areasAndEncountersToRandomize.size() > 0) {

                // Pull a random encounter from the list (...I think)
                // Does this an encounter at a time so the randomized encounters will be (somewhat) distributed
                Set<Integer> areaIndices = areasAndEncountersToRandomize.keySet();
                int areaIndex = areaIndices.stream().skip(this.random.nextInt(areaIndices.size())).findFirst().orElse(-1);
                List<Integer> encounterIndices = areasAndEncountersToRandomize.get(areaIndex);
                int indexInListOfEncounterIndices = this.random.nextInt(encounterIndices.size());
                int randomEncounterIndex = encounterIndices.get(indexInListOfEncounterIndices);
                EncounterSet area = nonRockSmashAreas.get(areaIndex);
                Encounter enc = area.encounters.get(randomEncounterIndex);

                if (catchEmAll && !doesAnotherEncounterWithSamePokemonExistInArea(enc, area)) {
                    // we want to keep at least one of each encounter previously chosen
                    continue;
                }

                Type typeToChoose = null;
                if(typeThemed || keepThemes) {
                    // try to use the theme already established
                    typeToChoose = pokemonInArea(area).findSharedType();

                    if(typeToChoose == null || fullWildPool.getCountOfType(typeToChoose) == 0) {
                        generateTypeForArea(activeWildPool, fullWildPool, 1);
                    }
                    //we DEFINITELY only need one in this context
                } else if (keepPrimary) {
                    typeToChoose = enc.pokemon.originalPrimaryType;
                }

                // get available Pokemon
                PokemonSet availablePool = getAvailablePokemonForArea(area, activeWildPool, typeToChoose);

                // handle empty pool
                if(availablePool.isEmpty()) {
                    if(activeWildPool.isEmpty()) {
                        activeWildPool = new PokemonSet(fullWildPool);
                    }

                    availablePool = getAvailablePokemonForArea(area, fullWildPool, typeToChoose);

                    if(availablePool.isEmpty()) {
                        String message = "No wild Pokemon";
                        if(typeToChoose != null) {
                            message += " of type " + typeToChoose;
                        }
                        message += " available for area " + area.displayName + "!";
                        throw new RandomizationException(message);
                    }
                }

                // Choose a Pokemon from the pool
                Pokemon picked;
                if(usePowerLevels) {
                    picked = pickWildPowerLvlReplacement(availablePool, enc.pokemon,
                            Math.min(5, availablePool.size() / 4), false, 100);
                } else {
                    picked = availablePool.randomPokemon(this.random);
                }

                // assign to encounter; remove from pickable if using catchEmAll
                enc.pokemon = picked;
                setFormeForEncounter(enc, enc.pokemon);
                if(catchEmAll) {
                    activeWildPool.remove(picked);
                    //availablePool doesn't matter, as it'll be regenerated with the next Pokemon anyway.
                }

                crashThreshold = computeDexNavCrashThreshold(encountersInZone);
                encounterIndices.remove(indexInListOfEncounterIndices);
                if (encounterIndices.size() == 0) {
                    areasAndEncountersToRandomize.remove(areaIndex);
                }
            }
        }
    }

    //wild Pokemon randomization helper functions:

    /**
     * Gets the pool of Pokemon that can be used as wild Pokemon, based on
     * the current settings.
     * @param settings The current settings.
     * @return The set of Pokemon that can be used as wild Pokemon.
     */
    private PokemonSet getWildPokemonPool(Settings settings) {

        boolean allowAltFormes = settings.isAllowWildAltFormes();
        boolean noLegendaries = settings.isBlockWildLegendaries();

        //...isn't this already done? oh well
        checkPokemonRestrictions();

        PokemonSet banned = getWildPokemonBannedSet(settings);

        PokemonSet allPokes;
        if (allowAltFormes) {
            allPokes = noLegendaries ? new PokemonSet(noLegendaryListInclFormes) : new PokemonSet(
                    mainPokemonListInclFormes);
            allPokes.removeIf(o -> ((Pokemon) o).actuallyCosmetic);
        } else {
            allPokes = noLegendaries ? new PokemonSet(noLegendaryList) : new PokemonSet(
                    mainPokemonList);
        }
        allPokes.removeAll(banned);

        return allPokes;
    }

    /**
     * Get the list of Pokemon banned from random generation as wild Pokemon.
     * @param settings The current settings.
     * @return The list of banned Pokemon.
     */
    private PokemonSet getWildPokemonBannedSet(Settings settings) {
        boolean abilitiesAreRandomized = settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE;
        boolean banIrregularAltFormes = settings.isBanIrregularAltFormes();

        PokemonSet banned = this.bannedForWildEncounters();
        banned.addAll(this.getBannedFormesForPlayerPokemon());
        if (!abilitiesAreRandomized) {
            List<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(getIrregularFormes());
        }

        return banned;
    }

    /**
     * Chooses a random type for this area.
     * Tries to ensure there are at least minimumPokemon Pokemon of the chosen type in the primary set;
     * if no types fulfill that condition, instead ensures it for the backup set.
     * In this case, prefers types that have at least one Pokemon in the primary set
     * over those that do not.
     * Throws a RandomizationException if the backup set does not have at least one type with
     * at least minimumPokemon Pokemon.
     * @param primarySet The first set to attempt to ensure sufficient Pokemon from.
     * @param backupSet The set to use for ensuring if no type has sufficient Pokemon in the primary set.
     * @param minimumPokemon The number of Pokemon to ensure are present.
     * @return A Type with at least the given minimum number of Pokemon in it in the backup set.
     */
    private Type generateTypeForArea(PokemonSet primarySet, PokemonSet backupSet, int minimumPokemon) {
        Type chosenType = generateTypeForArea(primarySet, minimumPokemon);
        if(chosenType != null) {
            return chosenType;
        }

        //primary failed. Time for secondary.
        List<Type> types = this.typesInGame();
        Collections.shuffle(types, random);
        for(Type type : types) {
            if(backupSet.getCountOfType(type) >= minimumPokemon) {
                if(primarySet.getCountOfType(type) != 0) {
                    return type;
                } else {
                    chosenType = type;
                    //Will actually return the last type checked, but that's just as random, so whatever.
                }
            }
        }

        if(chosenType == null) {
            throw new RandomizationException("No type had enough Pokemon (" + minimumPokemon + ") to theme area!");
        } else {
            return chosenType;
        }

    }

    /**
     * Recommended that you use the two-set overload of this function.
     * Chooses a random type for this area, ensuring there are at least areaSize Pokemon of that type in the set.
     * @param choosablePokemon The set of Pokemon to guarantee sufficient choices from.
     * @param areaSize How many Pokemon of the type to guarantee are in the set.
     * @return A Type which has at least areaSize Pokemon in the given set, or null if none do.
     */
    private Type generateTypeForArea(PokemonSet choosablePokemon, int areaSize) {
        List<Type> types = this.typesInGame();
        Collections.shuffle(types, random);

        for(Type type : types) {
            if(choosablePokemon.getCountOfType(type) >= areaSize) {
                return type;
            }
        }

        return null;
    }

    /**
     * attempts to determine what the type theme was for a partially generated area.
     * Can probably use PokemonSet.findSharedType instead.
     * TODO: ensure that works correctly
     * @param area
     * @return
     */
    private Type getTypeForArea(EncounterSet area) {
        Pokemon firstPokemon = area.encounters.get(0).pokemon;
        if (area.encounters.get(0).formeNumber != 0) {
            firstPokemon = getAltFormeOfPokemon(firstPokemon, area.encounters.get(0).formeNumber);
        }
        Type primaryType = firstPokemon.primaryType;
        int primaryCount = 1;
        Type secondaryType = null;
        int secondaryCount = 0;
        if (firstPokemon.secondaryType != null) {
            secondaryType = firstPokemon.secondaryType;
            secondaryCount = 1;
        }
        for (int i = 1; i < area.encounters.size(); i++) {
            Pokemon pokemon = area.encounters.get(i).pokemon;
            if (area.encounters.get(i).formeNumber != 0) {
                pokemon = getAltFormeOfPokemon(pokemon, area.encounters.get(i).formeNumber);
            }
            if (pokemon.primaryType == primaryType || pokemon.secondaryType == primaryType) {
                primaryCount++;
            }
            if (pokemon.primaryType == secondaryType || pokemon.secondaryType == secondaryType) {
                secondaryCount++;
            }
        }
        return primaryCount > secondaryCount ? primaryType : secondaryType;
    }

    /**
     * Narrows the set of available Pokemon down to a set suitable for a single area.
     * @param area The EncounterSet of the area in question.
     * @param pokemonPool The PokemonSet to choose Pokemon from.
     * @param type The type of Pokemon to choose, or null for all types.
     * @param additionalBanned Pokemon that should be excluded from the set for reasons
     *                         other than being on the area's ban list.
     * @return The set of available Pokemon for the area.
     */
    private PokemonSet getAvailablePokemonForArea(EncounterSet area, PokemonSet pokemonPool, Type type,
                                                  Collection<Pokemon> additionalBanned) {
        PokemonSet availablePokemon = getAvailablePokemonForArea(area, pokemonPool, type);

        if(additionalBanned != null) {
            availablePokemon.removeAll(additionalBanned);
        }

        return availablePokemon;
    }

    /**
     * Narrows the set of available Pokemon down to a set suitable for a single area.
     * @param area The EncounterSet of the area in question.
     * @param pokemonPool The PokemonSet to choose Pokemon from.
     * @param type The type of Pokemon to choose, or null for all types.
     * @return The set of available Pokemon for the area.
     */
    private PokemonSet getAvailablePokemonForArea(EncounterSet area, PokemonSet pokemonPool, Type type) {
        PokemonSet availablePokemon;

        if(type == null) {
            availablePokemon = new PokemonSet(pokemonPool);
        } else {
            availablePokemon = pokemonPool.getPokemonOfType(type);
        }

        availablePokemon.removeAll(area.bannedPokemon);

        return availablePokemon;
    }

    /**
     * Narrows the set of available Pokemon down to a set that fulfills the given conditions.
     * @param pokemonPool The PokemonSet to choose from.
     * @param type The type of Pokemon to include, or null for all types.
     * @param banned A collection of Pokemon to not include.
     * @return A PokemonSet narrowed down as specified.
     */
    private PokemonSet getAvailablePokemon(PokemonSet pokemonPool, Type type, Set<Pokemon> banned) {
        PokemonSet availablePokemon;

        if(type == null) {
            availablePokemon = new PokemonSet(pokemonPool);
        } else {
            availablePokemon = pokemonPool.getPokemonOfType(type);
        }

        availablePokemon.removeAll(banned);

        return availablePokemon;
    }

    /**
     * Extracts the wild pokemon level modifier from the current settings.
     * @param settings The current settings.
     * @return The level modifier for wild Pokemon.
     */
    private int getWildLevelModifier(Settings settings) {
        return settings.isWildLevelsModified() ? settings.getWildLevelModifier() : 0;
    }

    /**
     * Adjusts the levels of an encounter according to the modifier given.
     * @param enc The encounter to adjust the levels of. WARNING: PARAMETER MODIFIED.
     * @param levelModifier The percentage to increase the levels of the encounter.
     */
    private void adjustEncounterLevel(Encounter enc, int levelModifier) {
        if (levelModifier != 0) {
            enc.level = Math.min(100, (int) Math.round(enc.level * (1 + levelModifier / 100.0)));
            enc.maxLevel = Math.min(100, (int) Math.round(enc.maxLevel * (1 + levelModifier / 100.0)));
        }
    }

    /**
     * Chooses a Pokemon similar in power level to the given Pokemon.
     * @param pokemonPool The set of Pokemon to choose from.
     * @param current The Pokemon to match the power level of.
     * @param minimumPool The minimum number of Pokemon to include in the final pool.
     *                    If minimumPool is greater than or equal to pokemonPool.size(),
     *                    power levels will be ignored.
     * @param banSamePokemon Whether to disallow choosing the Pokemon the power level is matching to.
     *                       Ignored if this is the only Pokemon in the pool.
     * @param bstBalanceLevel A factor used to calculate a maximum BST using the formula 10 * level + 250.
     *                        If the given Pokemon's BST is higher than the calculated value, looks for Pokemon
     *                        of strength similar to the calculated value instead.
     * @return A Pokemon of similar strength to the given Pokemon.
     */
    private Pokemon pickWildPowerLvlReplacement(PokemonSet pokemonPool, Pokemon current, int minimumPool, boolean banSamePokemon,
                                                int bstBalanceLevel) {
        PokemonSet realPool = pokemonPool;
        if(banSamePokemon && pokemonPool.contains(current)) {
            realPool = new PokemonSet(pokemonPool);
            realPool.remove(current);
        }

        if(realPool.isEmpty()) {
            if(pokemonPool.isEmpty()) {
                throw new IllegalArgumentException("Tried to choose power level replacement from empty set!");
            } else {
                return current;
            }
        }

        if(minimumPool < 1) {
            minimumPool = 1;
        }

        if(minimumPool >= realPool.size()) {
            //minimum pool is whole pool
            return pokemonPool.randomPokemon(this.random);
        }
        // start with within 10% and add 5% either direction until the pool is big enough
        int balancedBST = bstBalanceLevel * 10 + 250;
        int currentBST = Math.min(current.bstForPowerLevels(), balancedBST);
        int minTarget = currentBST - currentBST / 10;
        int maxTarget = currentBST + currentBST / 10;

        PokemonSet canPick = new PokemonSet();
        while (canPick.size() < minimumPool) {
            for (Pokemon pk : pokemonPool) {
                if (pk.bstForPowerLevels() >= minTarget && pk.bstForPowerLevels() <= maxTarget) {
                    canPick.add(pk);
                }
            }
            minTarget -= currentBST / 20;
            maxTarget += currentBST / 20;
        }
        return canPick.randomPokemon(this.random);
    }

    /**
     * Checks if there is an encounter in the given area that uses the same pokemon as
     * the given encounter, other than the given encounter.
     * @param enc The encounter to check against.
     * @param area The area to check for encounters.
     * @return True if another encounter uses the same Pokemon, false otherwise.
     */
    private boolean doesAnotherEncounterWithSamePokemonExistInArea(Encounter enc, EncounterSet area) {
        for (Encounter encounterToCheck : area.encounters) {
            if (enc != encounterToCheck && enc.pokemon == encounterToCheck.pokemon) {
                return true;
            }
        }
        return false;
    }

    /**
     * Combines areas to prevent the game from crashing due to too many different Pokemon
     * after area 1-to-1 randomization.
     * Only applies to Omega Ruby and Alpha Sapphire.
     * @param currentEncounters The original set of areas in the ROM.
     * @return The collapsed set of areas.
     */
    private List<EncounterSet> collapseAreasORAS(List<EncounterSet> currentEncounters) {
        List<EncounterSet> output = new ArrayList<>();
        Map<Integer, List<EncounterSet>> zonesToEncounters = mapZonesToEncounters(currentEncounters);
        for (Integer zone : zonesToEncounters.keySet()) {
            List<EncounterSet> encountersInZone = zonesToEncounters.get(zone);
            int crashThreshold = computeDexNavCrashThreshold(encountersInZone);
            if (crashThreshold <= 18) {
                output.addAll(encountersInZone);
                continue;
            }

            // Naive Area 1-to-1 randomization will crash the game, so let's start collapsing areas to prevent this.
            // Start with combining all the fishing rod encounters, since it's a little less noticeable when they've
            // been collapsed.
            List<EncounterSet> collapsedEncounters = new ArrayList<>(encountersInZone);
            EncounterSet rodGroup = new EncounterSet();
            rodGroup.offset = zone;
            rodGroup.displayName = "Rod Group";
            for (EncounterSet area : encountersInZone) {
                if (area.displayName.contains("Old Rod") || area.displayName.contains("Good Rod") || area.displayName.contains("Super Rod")) {
                    collapsedEncounters.remove(area);
                    rodGroup.encounters.addAll(area.encounters);
                }
            }
            if (rodGroup.encounters.size() > 0) {
                collapsedEncounters.add(rodGroup);
            }
            crashThreshold = computeDexNavCrashThreshold(collapsedEncounters);
            if (crashThreshold <= 18) {
                output.addAll(collapsedEncounters);
                continue;
            }

            // Even after combining all the fishing rod encounters, we're still not below the threshold to prevent
            // DexNav from crashing the game. Combine all the grass encounters now to drop us below the threshold;
            // we've combined everything that DexNav normally combines, so at this point, we're *guaranteed* not
            // to crash the game.
            EncounterSet grassGroup = new EncounterSet();
            grassGroup.offset = zone;
            grassGroup.displayName = "Grass Group";
            for (EncounterSet area : encountersInZone) {
                if (area.displayName.contains("Grass/Cave") || area.displayName.contains("Long Grass") || area.displayName.contains("Horde")) {
                    collapsedEncounters.remove(area);
                    grassGroup.encounters.addAll(area.encounters);
                }
            }
            if (grassGroup.encounters.size() > 0) {
                collapsedEncounters.add(grassGroup);
            }

            output.addAll(collapsedEncounters);
        }
        return output;
    }

    /**
     * Calculates the load on the DexNav.
     * It will crash if the load is above 18.
     * @param encountersInZone The list of all areas in the zone.
     * @return A number representing the load on the DexNav.
     */
    private int computeDexNavCrashThreshold(List<EncounterSet> encountersInZone) {
        int crashThreshold = 0;
        for (EncounterSet area : encountersInZone) {
            if (area.displayName.contains("Rock Smash")) {
                continue; // Rock Smash Pokemon don't display on DexNav
            }
            Set<Pokemon> uniquePokemonInArea = new HashSet<>();
            for (Encounter enc : area.encounters) {
                if (enc.pokemon.baseForme != null) { // DexNav treats different forms as one Pokemon
                    uniquePokemonInArea.add(enc.pokemon.baseForme);
                } else {
                    uniquePokemonInArea.add(enc.pokemon);
                }
            }
            crashThreshold += uniquePokemonInArea.size();
        }
        return crashThreshold;
    }

    /**
     * Given an encounter for a given Pokemon, sets it to a random cosmetic form,
     * if there are multiple.
     * @param enc The encounter to be set. WARNING: PARAMETER CHANGED.
     * @param pk The Pokemon in the encounter.
     */
    private void setFormeForEncounter(Encounter enc, Pokemon pk) {
        boolean checkCosmetics = true;
        enc.formeNumber = 0;
        if (enc.pokemon.formeNumber > 0) {
            enc.formeNumber = enc.pokemon.formeNumber;
            enc.pokemon = enc.pokemon.baseForme;
            checkCosmetics = false;
        }
        if (checkCosmetics && enc.pokemon.cosmeticForms > 0) {
            enc.formeNumber = enc.pokemon.getCosmeticFormNumber(this.random.nextInt(enc.pokemon.cosmeticForms));
        } else if (!checkCosmetics && pk.cosmeticForms > 0) {
            enc.formeNumber += pk.getCosmeticFormNumber(this.random.nextInt(pk.cosmeticForms));
        }
    }

    //end wild Pokemon helper functions

    private void setEvoChainAsIllegal(Pokemon newPK, List<Pokemon> illegalList, boolean willForceEvolve) {
        // set pre-evos as illegal
        setIllegalPreEvos(newPK, illegalList);

        // if the placed Pokemon will be forced fully evolved, set its evolutions as illegal
        if (willForceEvolve) {
            setIllegalEvos(newPK, illegalList);
        }
    }

    private void setIllegalPreEvos(Pokemon pk, List<Pokemon> illegalList) {
        for (Evolution evo: pk.evolutionsTo) {
            pk = evo.from;
            illegalList.add(pk);
            setIllegalPreEvos(pk, illegalList);
        }
    }

    private void setIllegalEvos(Pokemon pk, List<Pokemon> illegalList) {
        for (Evolution evo: pk.evolutionsFrom) {
            pk = evo.to;
            illegalList.add(pk);
            setIllegalEvos(pk, illegalList);
        }
    }

    private List<Pokemon> getFinalEvos(Pokemon pk) {
        List<Pokemon> finalEvos = new ArrayList<>();
        traverseEvolutions(pk, finalEvos);
        return finalEvos;
    }

    private void traverseEvolutions(Pokemon pk, List<Pokemon> finalEvos) {
        if (!pk.evolutionsFrom.isEmpty()) {
            for (Evolution evo: pk.evolutionsFrom) {
                pk = evo.to;
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
        if (pk.formeNumber > 0) {
            tp.forme = pk.formeNumber;
            tp.formeSuffix = pk.formeSuffix;
            tp.pokemon = pk.baseForme;
            checkCosmetics = false;
        }
        if (checkCosmetics && tp.pokemon.cosmeticForms > 0) {
            tp.forme = tp.pokemon.getCosmeticFormNumber(this.random.nextInt(tp.pokemon.cosmeticForms));
        } else if (!checkCosmetics && pk.cosmeticForms > 0) {
            tp.forme += pk.getCosmeticFormNumber(this.random.nextInt(pk.cosmeticForms));
        }
    }

    private void applyLevelModifierToTrainerPokemon(Trainer trainer, int levelModifier) {
        if (levelModifier != 0) {
            for (TrainerPokemon tp : trainer.pokemon) {
                tp.level = Math.min(100, (int) Math.round(tp.level * (1 + levelModifier / 100.0)));
            }
        }
    }

    @Override
    public void randomizeTrainerPokes(Settings settings) {
        boolean usePowerLevels = settings.isTrainersUsePokemonOfSimilarStrength();
        boolean weightByFrequency = settings.isTrainersMatchTypingDistribution();
        boolean useLocalPokemon = settings.isTrainersUseLocalPokemon();
        boolean noLegendaries = settings.isTrainersBlockLegendaries();
        boolean noEarlyWonderGuard = settings.isTrainersBlockEarlyWonderGuard();
        int levelModifier = settings.isTrainersLevelModified() ? settings.getTrainersLevelModifier() : 0;
        boolean isTypeThemed = settings.getTrainersMod() == Settings.TrainersMod.TYPE_THEMED;
        boolean isTypeThemedEliteFourGymOnly = settings.getTrainersMod() == Settings.TrainersMod.TYPE_THEMED_ELITE4_GYMS;
        boolean keepTypeThemes = settings.getTrainersMod() == Settings.TrainersMod.KEEP_THEMED;
        boolean keepThemeOrPrimaryTypes = settings.getTrainersMod() == Settings.TrainersMod.KEEP_THEME_OR_PRIMARY;
        boolean hasSomeTypeTheme = isTypeThemed || isTypeThemedEliteFourGymOnly || keepTypeThemes
                || keepThemeOrPrimaryTypes;
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

        checkPokemonRestrictions();

        // Set up Pokemon pool
        cachedTypeLists = new TreeMap<>();
        if (useLocalPokemon) {
            cachedAllList = new ArrayList<>(mainGameWildPokemon(settings.isUseTimeBasedEncounters()));
            addEvolutionaryRelatives(cachedAllList);
            //well, there's probably a way to add the alt formes of pokemon already in the list...
            //but i don't know what it is. so.
            //we'll only remove the formes, if they're disallowed.
            if (!includeFormes) {
                cachedAllList.removeAll(altFormesList);
            }
            if(noLegendaries) {
                cachedAllList.removeAll(onlyLegendaryListInclFormes);
            }
            cachedAllList.removeIf(pk -> pk.actuallyCosmetic);
        } else {
            cachedAllList = noLegendaries ? new ArrayList<>(noLegendaryList) : new ArrayList<>(
                    mainPokemonList);
            if (includeFormes) {
                if (noLegendaries) {
                    cachedAllList.addAll(noLegendaryAltsList);
                } else {
                    cachedAllList.addAll(altFormesList);
                }
            }
            //...this seems like the worst way to do this but whatever
            cachedAllList =
                    cachedAllList
                            .stream()
                            .filter(pk -> !pk.actuallyCosmetic)
                            .collect(Collectors.toList());
        }

        List<Pokemon> banned = this.getBannedFormesForTrainerPokemon();
        if (!abilitiesAreRandomized) {
            List<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(getIrregularFormes());
        }
        cachedAllList.removeAll(banned);

        List<Trainer> currentTrainers = this.getTrainers();

        // Type Themed related
        Map<Trainer, Type> trainerTypes = new TreeMap<>();
        Set<Type> usedUberTypes = new TreeSet<>();
        if (hasSomeTypeTheme) {
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


            if(keepTypeThemes || keepThemeOrPrimaryTypes) {
                Map<String, Type> originalThemes = getGymAndEliteTypeThemes();

                for (String group : groups.keySet()) {
                    if (!originalThemes.containsKey(group)) {
                        continue;
                    }
                    Type groupType = originalThemes.get(group);
                    List<Trainer> trainersInGroup = groups.get(group);
                    for (Trainer t : trainersInGroup) {
                        trainerTypes.put(t, groupType);
                    }
                }

            } else {
                // Give a random type to each group
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
        }

        // Randomize the order trainers are randomized in.
        // Leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<Trainer> scrambledTrainers = new ArrayList<>(currentTrainers);
        Collections.shuffle(scrambledTrainers, this.random);

        // Elite Four Unique Pokemon related
        boolean eliteFourUniquePokemon = eliteFourUniquePokemonNumber > 0;
        List<Pokemon> illegalIfEvolvedList = new ArrayList<>();
        List<Pokemon> bannedFromUniqueList = new ArrayList<>();
        boolean illegalEvoChains = false;
        List<Integer> eliteFourIndices = getEliteFourTrainers(forceChallengeMode);
        List<Pokemon> eliteFourExceptionList = null;
        List<Pokemon> nonEliteFourExceptionList = null;
        if (eliteFourUniquePokemon) {
            // Sort Elite Four Trainers to the start of the list
            scrambledTrainers.sort((t1, t2) ->
                    Boolean.compare(eliteFourIndices.contains(currentTrainers.indexOf(t2)+1),eliteFourIndices.contains(currentTrainers.indexOf(t1)+1)));
            illegalEvoChains = forceFullyEvolved;
            if (rivalCarriesStarter) {
                List<Pokemon> starterList = getStarters().subList(0,3);
                for (Pokemon starter: starterList) {
                    // If rival/friend carries starter, the starters cannot be set as unique
                    bannedFromUniqueList.add(starter);
                    setEvoChainAsIllegal(starter, bannedFromUniqueList, true);

                    // If the final boss is a rival/friend, the fully evolved starters will be unique
                    if (hasRivalFinalBattle()) {
                        cachedAllList.removeAll(getFinalEvos(starter));
                        if (illegalEvoChains) {
                            illegalIfEvolvedList.add(starter);
                            setEvoChainAsIllegal(starter, illegalIfEvolvedList, true);
                        }
                    }
                }
            }
            if(useLocalPokemon) {
                //elite four unique pokemon are excepted from local requirement
                //and in fact, non-local pokemon should be chosen first
                eliteFourExceptionList = noLegendaries ? new ArrayList<>(noLegendaryList) : new ArrayList<>(
                        mainPokemonList);
                if (includeFormes) {
                    if (noLegendaries) {
                        eliteFourExceptionList.addAll(noLegendaryAltsList);
                    } else {
                        eliteFourExceptionList.addAll(altFormesList);
                    }
                }
                eliteFourExceptionList.removeIf(pk -> pk.actuallyCosmetic);
                eliteFourExceptionList.removeAll(cachedAllList);
            }
        }

        List<Integer> mainPlaythroughTrainers = getMainPlaythroughTrainers();

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

            List<TrainerPokemon> trainerPokemonList = new ArrayList<>(t.pokemon);
            if (keepTypeThemes || keepThemeOrPrimaryTypes) {
                //determine if this trainer has a type theme
                Pokemon poke = trainerPokemonList.get(0).pokemon;
                Type primary = poke.originalPrimaryType;
                Type secondary = poke.originalSecondaryType;
                for (int i = 1; i < trainerPokemonList.size(); i++) {
                    poke = trainerPokemonList.get(i).pokemon;
                    if(secondary != null) {
                        if (secondary != poke.originalPrimaryType && secondary != poke.originalSecondaryType) {
                            secondary = null;
                        }
                    }
                    if (primary != poke.originalPrimaryType && primary != poke.originalSecondaryType) {
                        primary = secondary;
                        secondary = null;
                    }
                    if (primary == null) {
                        break; //no type is shared, no need to look at the remaining pokemon
                    }
                }
                if (primary != null) {
                    //we have a type theme!
                    if(primary == Type.NORMAL && secondary != null) {
                        //Bird override
                        //(Normal is less significant than other types, for example, Flying)
                        typeForTrainer = secondary;
                    } else {
                        typeForTrainer = primary;
                    }
                }
            }

            List<Pokemon> evolvesIntoTheWrongType = new ArrayList<>();
            if (typeForTrainer != null) {
                List<Pokemon> pokemonOfType = includeFormes ? pokemonOfTypeInclFormes(typeForTrainer, noLegendaries) :
                        pokemonOfType(typeForTrainer, noLegendaries);
                for (Pokemon pk : pokemonOfType) {
                    if (!pokemonOfType.contains(fullyEvolve(pk, t.index))) {
                        evolvesIntoTheWrongType.add(pk);
                    }
                }
            }

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

            for (TrainerPokemon tp : trainerPokemonList) {
                boolean swapThisMegaEvo = swapMegaEvos && tp.canMegaEvolve();
                boolean wgAllowed = (!noEarlyWonderGuard) || tp.level >= 20;
                boolean eliteFourSetUniquePokemon =
                        eliteFourTrackPokemon && eliteFourUniquePokemonNumber > trainerPokemonList.indexOf(tp);
                boolean willForceEvolve = forceFullyEvolved && tp.level >= forceFullyEvolvedLevel;

                List<Pokemon> cacheReplacement = null;

                Pokemon oldPK = tp.pokemon;
                if (tp.forme > 0) {
                    oldPK = getAltFormeOfPokemon(oldPK, tp.forme);
                }

                if (keepThemeOrPrimaryTypes && typeForTrainer == null && willForceEvolve) {
                    List<Pokemon> pokemonOfType = includeFormes ? pokemonOfTypeInclFormes(oldPK.originalPrimaryType, noLegendaries) :
                            pokemonOfType(oldPK.originalPrimaryType, noLegendaries);
                    for (Pokemon pk : pokemonOfType) {
                        if (!pokemonOfType.contains(fullyEvolve(pk, t.index))) {
                            evolvesIntoTheWrongType.add(pk);
                        }
                    }
                }

                bannedList = new ArrayList<>();
                bannedList.addAll(usedAsUniqueList);
                if (illegalEvoChains && willForceEvolve) {
                    bannedList.addAll(illegalIfEvolvedList);
                }
                if (eliteFourSetUniquePokemon) {
                    bannedList.addAll(bannedFromUniqueList);
                    cacheReplacement = eliteFourExceptionList;
                }
                if (willForceEvolve) {
                    bannedList.addAll(evolvesIntoTheWrongType);
                }

                Type typeForPokemon = typeForTrainer;
                if(keepThemeOrPrimaryTypes && typeForPokemon == null) {
                    typeForPokemon = oldPK.originalPrimaryType;
                }

                Pokemon newPK = pickTrainerPokeReplacement(
                                oldPK,
                                usePowerLevels,
                                typeForPokemon,
                                wgAllowed,
                                distributionSetting || (mainPlaythroughSetting && mainPlaythroughTrainers.contains(t.index)),
                                swapThisMegaEvo,
                                cacheReplacement
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
                        List<Pokemon> actualPKList;
                        if (willForceEvolve) {
                            actualPKList = getFinalEvos(newPK);
                        } else {
                            actualPKList = new ArrayList<>();
                            actualPKList.add(newPK);
                        }
                        // If the unique Pokemon will evolve, we have to set all its potential evolutions as unique
                        for (Pokemon actualPK: actualPKList) {
                            usedAsUniqueList.add(actualPK);
                            if (illegalEvoChains) {
                                setEvoChainAsIllegal(actualPK, illegalIfEvolvedList, willForceEvolve);
                            }
                        }

                    }
                    if (eliteFourTrackPokemon) {
                        bannedFromUniqueList.add(newPK);
                        if (illegalEvoChains) {
                            setEvoChainAsIllegal(newPK, bannedFromUniqueList, willForceEvolve);
                        }
                    }
                } else {
                    // If the champion is a rival, the first Pokemon will be skipped - it's already
                    // set as unique since it's a starter
                    eliteFourRival = false;
                }

                if (swapThisMegaEvo) {
                    tp.heldItem = newPK
                                    .megaEvolutionsFrom
                                    .get(this.random.nextInt(newPK.megaEvolutionsFrom.size()))
                                    .argument;
                }

                if (shinyChance) {
                    if (this.random.nextInt(256) == 0) {
                        tp.IVs |= (1 << 30);
                    }
                }
            }
        }

        // Save it all up
        this.setTrainers(currentTrainers, false);
    }

    abstract protected Map<String, Type> getGymAndEliteTypeThemes();

    @Override
    public void randomizeTrainerHeldItems(Settings settings) {
        boolean giveToBossPokemon = settings.isRandomizeHeldItemsForBossTrainerPokemon();
        boolean giveToImportantPokemon = settings.isRandomizeHeldItemsForImportantTrainerPokemon();
        boolean giveToRegularPokemon = settings.isRandomizeHeldItemsForRegularTrainerPokemon();
        boolean highestLevelOnly = settings.isHighestLevelGetsItemsForTrainers();

        List<Move> moves = this.getMoves();
        Map<Integer, List<MoveLearnt>> movesets = this.getMovesLearnt();
        List<Trainer> currentTrainers = this.getTrainers();
        for (Trainer t : currentTrainers) {
            if (trainerShouldNotGetBuffs(t)) {
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
                        RomFunctions.getMovesAtLevel(getAltFormeOfPokemon(
                                highestLevelPoke.pokemon, highestLevelPoke.forme).number,
                                movesets,
                                highestLevelPoke.level) :
                        highestLevelPoke.moves;
                randomizeHeldItem(highestLevelPoke, settings, moves, moveset);
            } else {
                for (TrainerPokemon tp : t.pokemon) {
                    int[] moveset = tp.resetMoves ?
                            RomFunctions.getMovesAtLevel(getAltFormeOfPokemon(
                                    tp.pokemon, tp.forme).number,
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
        this.setTrainers(currentTrainers, false);
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
            toChooseFrom = getSensibleHeldItemsFor(tp, consumableItemsOnly, moves, moveset);
        } else if (consumableItemsOnly) {
            toChooseFrom = getAllConsumableHeldItems();
        } else {
            toChooseFrom = getAllHeldItems();
        }
        tp.heldItem = toChooseFrom.get(random.nextInt(toChooseFrom.size()));
    }

    @Override
    public void rivalCarriesStarter() {
        checkPokemonRestrictions();
        List<Trainer> currentTrainers = this.getTrainers();
        rivalCarriesStarterUpdate(currentTrainers, "RIVAL", isORAS ? 0 : 1);
        rivalCarriesStarterUpdate(currentTrainers, "FRIEND", 2);
        this.setTrainers(currentTrainers, false);
    }

    @Override
    public boolean hasRivalFinalBattle() {
        return false;
    }

    @Override
    public void forceFullyEvolvedTrainerPokes(Settings settings) {
        int minLevel = settings.getTrainersForceFullyEvolvedLevel();

        checkPokemonRestrictions();
        List<Trainer> currentTrainers = this.getTrainers();
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
        this.setTrainers(currentTrainers, false);
    }

    @Override
    public void onlyChangeTrainerLevels(Settings settings) {
        int levelModifier = settings.getTrainersLevelModifier();

        List<Trainer> currentTrainers = this.getTrainers();
        for (Trainer t: currentTrainers) {
            applyLevelModifierToTrainerPokemon(t, levelModifier);
        }
        this.setTrainers(currentTrainers, false);
    }

    @Override
    public void addTrainerPokemon(Settings settings) {
        int additionalNormal = settings.getAdditionalRegularTrainerPokemon();
        int additionalImportant = settings.getAdditionalImportantTrainerPokemon();
        int additionalBoss = settings.getAdditionalBossTrainerPokemon();

        List<Trainer> currentTrainers = this.getTrainers();
        for (Trainer t: currentTrainers) {
            int additional;
            if (t.isBoss()) {
                additional = additionalBoss;
            } else if (t.isImportant()) {
                if (t.skipImportant()) continue;
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
            for (TrainerPokemon tpk: t.pokemon) {
                if (tpk.level < lowest) {
                    lowest = tpk.level;
                }
            }

            // Second pass: find all Pokemon at lowest level
            for (TrainerPokemon tpk: t.pokemon) {
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
        this.setTrainers(currentTrainers, false);
    }

    @Override
    public void doubleBattleMode() {
        List<Trainer> currentTrainers = this.getTrainers();
        for (Trainer t: currentTrainers) {
            if (t.pokemon.size() != 1 || t.multiBattleStatus == Trainer.MultiBattleStatus.ALWAYS || this.trainerShouldNotGetBuffs(t)) {
                continue;
            }
            t.pokemon.add(t.pokemon.get(0).copy());
        }
        this.setTrainers(currentTrainers, true);
    }

    private Map<Integer, List<MoveLearnt>> allLevelUpMoves;
    private Map<Integer, List<Integer>> allEggMoves;
    private Map<Pokemon, boolean[]> allTMCompat, allTutorCompat;
    private List<Integer> allTMMoves, allTutorMoves;

    @Override
    public List<Move> getMoveSelectionPoolAtLevel(TrainerPokemon tp, boolean cyclicEvolutions) {

        List<Move> moves = getMoves();
        double eggMoveProbability = 0.1;
        double preEvoMoveProbability = 0.5;
        double tmMoveProbability = 0.6;
        double tutorMoveProbability = 0.6;

        if (allLevelUpMoves == null) {
            allLevelUpMoves = getMovesLearnt();
        }

        if (allEggMoves == null) {
            allEggMoves = getEggMoves();
        }

        if (allTMCompat == null) {
            allTMCompat = getTMHMCompatibility();
        }

        if (allTMMoves == null) {
            allTMMoves = getTMMoves();
        }

        if (allTutorCompat == null && hasMoveTutors()) {
            allTutorCompat = getMoveTutorCompatibility();
        }

        if (allTutorMoves == null) {
            allTutorMoves = getMoveTutorMoves();
        }

        // Level-up Moves
        List<Move> moveSelectionPoolAtLevel = allLevelUpMoves.get(getAltFormeOfPokemon(tp.pokemon, tp.forme).number)
                .stream()
                .filter(ml -> (ml.level <= tp.level && ml.level != 0) || (ml.level == 0 && tp.level >= 30))
                .map(ml -> moves.get(ml.move))
                .distinct()
                .collect(Collectors.toList());

        // Pre-Evo Moves
        if (!cyclicEvolutions) {
            Pokemon preEvo;
            if (altFormesCanHaveDifferentEvolutions()) {
                preEvo = getAltFormeOfPokemon(tp.pokemon, tp.forme);
            } else {
                preEvo = tp.pokemon;
            }
            while (!preEvo.evolutionsTo.isEmpty()) {
                preEvo = preEvo.evolutionsTo.get(0).from;
                moveSelectionPoolAtLevel.addAll(allLevelUpMoves.get(preEvo.number)
                        .stream()
                        .filter(ml -> ml.level <= tp.level)
                        .filter(ml -> this.random.nextDouble() < preEvoMoveProbability)
                        .map(ml -> moves.get(ml.move))
                        .distinct()
                        .collect(Collectors.toList()));
            }
        }

        // TM Moves
        boolean[] tmCompat = allTMCompat.get(getAltFormeOfPokemon(tp.pokemon, tp.forme));
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
        if (hasMoveTutors()) {
            boolean[] tutorCompat = allTutorCompat.get(getAltFormeOfPokemon(tp.pokemon, tp.forme));
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
            if (altFormesCanHaveDifferentEvolutions()) {
                firstEvo = getAltFormeOfPokemon(tp.pokemon, tp.forme);
            } else {
                firstEvo = tp.pokemon;
            }
            while (!firstEvo.evolutionsTo.isEmpty()) {
                firstEvo = firstEvo.evolutionsTo.get(0).from;
            }
            if (allEggMoves.get(firstEvo.number) != null) {
                moveSelectionPoolAtLevel.addAll(allEggMoves.get(firstEvo.number)
                        .stream()
                        .filter(egm -> this.random.nextDouble() < eggMoveProbability)
                        .map(moves::get)
                        .collect(Collectors.toList()));
            }
        }



        return moveSelectionPoolAtLevel.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public void pickTrainerMovesets(Settings settings) {
        boolean isCyclicEvolutions = settings.getEvolutionsMod() == Settings.EvolutionsMod.RANDOM_EVERY_LEVEL;
        boolean doubleBattleMode = settings.isDoubleBattleMode();

        List<Trainer> trainers = getTrainers();

        for (Trainer t: trainers) {
            t.setPokemonHaveCustomMoves(true);

            for (TrainerPokemon tp: t.pokemon) {
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

                Pokemon pk = getAltFormeOfPokemon(tp.pokemon, tp.forme);

                List<Move> stabMoves = new ArrayList<>(movesAtLevel)
                        .stream()
                        .filter(mv -> mv.type == pk.primaryType && mv.category != MoveCategory.STATUS)
                        .collect(Collectors.toList());
                Collections.shuffle(stabMoves, this.random);

                for (int i = 0; i < stabMoveBias * stabMoves.size(); i++) {
                    int j = i % stabMoves.size();
                    movesAtLevel.add(stabMoves.get(j));
                }

                if (pk.secondaryType != null) {
                    stabMoves = new ArrayList<>(movesAtLevel)
                            .stream()
                            .filter(mv -> mv.type == pk.secondaryType && mv.category != MoveCategory.STATUS)
                            .collect(Collectors.toList());
                    Collections.shuffle(stabMoves, this.random);

                    for (int i = 0; i < stabMoveBias * stabMoves.size(); i++) {
                        int j = i % stabMoves.size();
                        movesAtLevel.add(stabMoves.get(j));
                    }
                }

                // Hard ability/move synergy

                List<Move> abilityMoveSynergyList = MoveSynergy.getHardAbilityMoveSynergy(
                        getAbilityForTrainerPokemon(tp),
                        pk.primaryType,
                        pk.secondaryType,
                        movesAtLevel,
                        generationOfPokemon(),
                        perfectAccuracy);
                Collections.shuffle(abilityMoveSynergyList, this.random);
                for (int i = 0; i < hardAbilityMoveBias * abilityMoveSynergyList.size(); i++) {
                    int j = i % abilityMoveSynergyList.size();
                    movesAtLevel.add(abilityMoveSynergyList.get(j));
                }

                // Soft ability/move synergy

                List<Move> softAbilityMoveSynergyList = MoveSynergy.getSoftAbilityMoveSynergy(
                        getAbilityForTrainerPokemon(tp),
                        movesAtLevel,
                        pk.primaryType,
                        pk.secondaryType);

                Collections.shuffle(softAbilityMoveSynergyList, this.random);
                for (int i = 0; i < softAbilityMoveBias * softAbilityMoveSynergyList.size(); i++) {
                    int j = i % softAbilityMoveSynergyList.size();
                    movesAtLevel.add(softAbilityMoveSynergyList.get(j));
                }

                // Soft ability/move anti-synergy

                List<Move> softAbilityMoveAntiSynergyList = MoveSynergy.getSoftAbilityMoveAntiSynergy(
                        getAbilityForTrainerPokemon(tp), movesAtLevel);
                List<Move> withoutSoftAntiSynergy = new ArrayList<>(movesAtLevel);
                for (Move mv: softAbilityMoveAntiSynergyList) {
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
                Collections.shuffle(statSynergyList, this.random);
                for (int i = 0; i < statBias * statSynergyList.size(); i++) {
                    int j = i % statSynergyList.size();
                    movesAtLevel.add(statSynergyList.get(j));
                }

                // Stat/move anti-synergy

                List<Move> statAntiSynergyList = MoveSynergy.getStatMoveAntiSynergy(pk, movesAtLevel);
                List<Move> withoutStatAntiSynergy = new ArrayList<>(movesAtLevel);
                for (Move mv: statAntiSynergyList) {
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

                double atkSpatkRatio = (double)pk.attack / (double)pk.spatk;
                switch(getAbilityForTrainerPokemon(tp)) {
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
                        .filter(mv -> mv.category == MoveCategory.PHYSICAL)
                        .collect(Collectors.toList());
                List<Move> specialMoves = new ArrayList<>(movesAtLevel)
                        .stream()
                        .filter(mv -> mv.category == MoveCategory.SPECIAL)
                        .collect(Collectors.toList());

                if (atkSpatkRatio < 1 && specialMoves.size() > 0) {
                    atkSpatkRatio = 1 / atkSpatkRatio;
                    double acceptedRatio = atkSpatkRatioModifier * atkSpatkRatio;
                    int additionalMoves = (int)(physicalMoves.size() * acceptedRatio) - specialMoves.size();
                    for (int i = 0; i < additionalMoves; i++) {
                        Move mv = specialMoves.get(this.random.nextInt(specialMoves.size()));
                        movesAtLevel.add(mv);
                    }
                } else if (physicalMoves.size() > 0) {
                    double acceptedRatio = atkSpatkRatioModifier * atkSpatkRatio;
                    int additionalMoves = (int)(specialMoves.size() * acceptedRatio) - physicalMoves.size();
                    for (int i = 0; i < additionalMoves; i++) {
                        Move mv = physicalMoves.get(this.random.nextInt(physicalMoves.size()));
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
                                .filter(mv -> mv.isGoodDamaging(perfectAccuracy))
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
                                .distinct()
                                .collect(Collectors.toList());

                        for (Move dependentMove: requiresOtherMove) {
                            boolean hasRequiredMove = false;
                            for (Move requiredMove: MoveSynergy.requiresOtherMove(dependentMove, movesAtLevel)) {
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

                    move = pickFrom.get(this.random.nextInt(pickFrom.size()));
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
                            generationOfPokemon());
                    Collections.shuffle(hardMoveSynergyList, this.random);
                    for (int j = 0; j < hardMoveBias * hardMoveSynergyList.size(); j++) {
                        int k = j % hardMoveSynergyList.size();
                        movesAtLevel.add(hardMoveSynergyList.get(k));
                    }

                    List<Move> softMoveSynergyList = MoveSynergy.getSoftMoveSynergy(
                            move,
                            movesAtLevel,
                            generationOfPokemon(),
                            isEffectivenessUpdated());
                    Collections.shuffle(softMoveSynergyList, this.random);
                    for (int j = 0; j < softMoveBias * softMoveSynergyList.size(); j++) {
                        int k = j % softMoveSynergyList.size();
                        movesAtLevel.add(softMoveSynergyList.get(k));
                    }

                    List<Move> softMoveAntiSynergyList = MoveSynergy.getSoftMoveAntiSynergy(move, movesAtLevel);
                    Collections.shuffle(softMoveAntiSynergyList, this.random);
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
        setTrainers(trainers, false);
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
                .filter(mv -> GlobalConstants.requiresOtherMove.contains(mv.number))
                .collect(Collectors.toList());

        for (Move dependentMove: requiresOtherMove) {
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
                getAbilityForTrainerPokemon(tp),
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
        for (Move mv: movesAtLevel) {
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
                                mv.power * mv.hitCount > mv2.power * mv2.hitCount)
                        .collect(Collectors.toList());
                for (Move obsoleted: obsoleteThis) {
                    //System.out.println(obsoleted.name + " obsoleted by " + mv.name);
                }
                obsoletedMoves.addAll(obsoleteThis);
            } else if (mv.statChangeMoveType == StatChangeMoveType.NO_DAMAGE_USER ||
                    mv.statChangeMoveType == StatChangeMoveType.NO_DAMAGE_TARGET) {
                List<Move> obsoleteThis = new ArrayList<>();
                List<Move.StatChange> statChanges1 = new ArrayList<>();
                for (Move.StatChange sc: mv.statChanges) {
                    if (sc.type != StatChangeType.NONE) {
                        statChanges1.add(sc);
                    }
                }
                for (Move mv2: movesAtLevel
                        .stream()
                        .filter(otherMv -> !otherMv.equals(mv) &&
                                otherMv.power <= 0 &&
                                otherMv.statChangeMoveType == mv.statChangeMoveType &&
                                (otherMv.statusType == mv.statusType ||
                                otherMv.statusType == StatusType.NONE))
                        .collect(Collectors.toList())) {
                    List<Move.StatChange> statChanges2 = new ArrayList<>();
                    for (Move.StatChange sc: mv2.statChanges) {
                        if (sc.type != StatChangeType.NONE) {
                            statChanges2.add(sc);
                        }
                    }
                    if (statChanges2.size() > statChanges1.size()) {
                        continue;
                    }
                    List<Move.StatChange> statChanges1Filtered = statChanges1
                            .stream()
                            .filter(sc -> !statChanges2.contains(sc))
                            .collect(Collectors.toList());
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
                    for (Move.StatChange sc1: statChanges1Filtered) {
                        boolean canStillBeBetter = false;
                        for (Move.StatChange sc2: statChanges2) {
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
                for (Move obsoleted: obsoleteThis) {
                    //System.out.println(obsoleted.name + " obsoleted by " + mv.name);
                }
                obsoletedMoves.addAll(obsoleteThis);
            }
        }

        return obsoletedMoves.stream().distinct().collect(Collectors.toList());
    }

    private boolean trainerShouldNotGetBuffs(Trainer t) {
        return t.tag != null && (t.tag.startsWith("RIVAL1-") || t.tag.startsWith("FRIEND1-") || t.tag.endsWith("NOTSTRONG"));
    }

    public int getRandomAbilitySlot(Pokemon pokemon) {
        if (abilitiesPerPokemon() == 0) {
            return 0;
        }
        List<Integer> abilitiesList = Arrays.asList(pokemon.ability1, pokemon.ability2, pokemon.ability3);
        int slot = random.nextInt(this.abilitiesPerPokemon());
        while (abilitiesList.get(slot) == 0) {
            slot = random.nextInt(this.abilitiesPerPokemon());
        }
        return slot + 1;
    }

    public int getValidAbilitySlotFromOriginal(Pokemon pokemon, int originalAbilitySlot) {
        // This is used in cases where one Trainer Pokemon evolves into another. If the unevolved Pokemon
        // is using slot 2, but the evolved Pokemon doesn't actually have a second ability, then we
        // want the evolved Pokemon to use slot 1 for safety's sake.
        if (originalAbilitySlot == 2 && pokemon.ability2 == 0) {
            return 1;
        }
        return originalAbilitySlot;
    }

    // MOVE DATA
    // All randomizers don't touch move ID 165 (Struggle)
    // They also have other exclusions where necessary to stop things glitching.

    @Override
    public void randomizeMovePowers() {
        List<Move> moves = this.getMoves();
        for (Move mv : moves) {
            if (mv != null && mv.internalId != Moves.struggle && mv.power >= 10) {
                // "Generic" damaging move to randomize power
                if (random.nextInt(3) != 2) {
                    // "Regular" move
                    mv.power = random.nextInt(11) * 5 + 50; // 50 ... 100
                } else {
                    // "Extreme" move
                    mv.power = random.nextInt(27) * 5 + 20; // 20 ... 150
                }
                // Tiny chance for massive power jumps
                for (int i = 0; i < 2; i++) {
                    if (random.nextInt(100) == 0) {
                        mv.power += 50;
                    }
                }

                if (mv.hitCount != 1) {
                    // Divide randomized power by average hit count, round to
                    // nearest 5
                    mv.power = (int) (Math.round(mv.power / mv.hitCount / 5) * 5);
                    if (mv.power == 0) {
                        mv.power = 5;
                    }
                }
            }
        }
    }

    @Override
    public void randomizeMovePPs() {
        List<Move> moves = this.getMoves();
        for (Move mv : moves) {
            if (mv != null && mv.internalId != Moves.struggle) {
                if (random.nextInt(3) != 2) {
                    // "average" PP: 15-25
                    mv.pp = random.nextInt(3) * 5 + 15;
                } else {
                    // "extreme" PP: 5-40
                    mv.pp = random.nextInt(8) * 5 + 5;
                }
            }
        }
    }

    @Override
    public void randomizeMoveAccuracies() {
        List<Move> moves = this.getMoves();
        for (Move mv : moves) {
            if (mv != null && mv.internalId != Moves.struggle && mv.hitratio >= 5) {
                // "Sane" accuracy randomization
                // Broken into three tiers based on original accuracy
                // Designed to limit the chances of 100% accurate OHKO moves and
                // keep a decent base of 100% accurate regular moves.

                if (mv.hitratio <= 50) {
                    // lowest tier (acc <= 50)
                    // new accuracy = rand(20...50) inclusive
                    // with a 10% chance to increase by 50%
                    mv.hitratio = random.nextInt(7) * 5 + 20;
                    if (random.nextInt(10) == 0) {
                        mv.hitratio = (mv.hitratio * 3 / 2) / 5 * 5;
                    }
                } else if (mv.hitratio < 90) {
                    // middle tier (50 < acc < 90)
                    // count down from 100% to 20% in 5% increments with 20%
                    // chance to "stop" and use the current accuracy at each
                    // increment
                    // gives decent-but-not-100% accuracy most of the time
                    mv.hitratio = 100;
                    while (mv.hitratio > 20) {
                        if (random.nextInt(10) < 2) {
                            break;
                        }
                        mv.hitratio -= 5;
                    }
                } else {
                    // highest tier (90 <= acc <= 100)
                    // count down from 100% to 20% in 5% increments with 40%
                    // chance to "stop" and use the current accuracy at each
                    // increment
                    // gives high accuracy most of the time
                    mv.hitratio = 100;
                    while (mv.hitratio > 20) {
                        if (random.nextInt(10) < 4) {
                            break;
                        }
                        mv.hitratio -= 5;
                    }
                }
            }
        }
    }

    @Override
    public void randomizeMoveTypes() {
        List<Move> moves = this.getMoves();
        for (Move mv : moves) {
            if (mv != null && mv.internalId != Moves.struggle && mv.type != null) {
                mv.type = randomType();
            }
        }
    }

    @Override
    public void randomizeMoveCategory() {
        if (!this.hasPhysicalSpecialSplit()) {
            return;
        }
        List<Move> moves = this.getMoves();
        for (Move mv : moves) {
            if (mv != null && mv.internalId != Moves.struggle && mv.category != MoveCategory.STATUS) {
                if (random.nextInt(2) == 0) {
                    mv.category = (mv.category == MoveCategory.PHYSICAL) ? MoveCategory.SPECIAL : MoveCategory.PHYSICAL;
                }
            }
        }

    }

    @Override
    public void updateMoves(Settings settings) {
        int generation = settings.getUpdateMovesToGeneration();

        List<Move> moves = this.getMoves();

        if (generation >= 2 && generationOfPokemon() < 2) {
            // gen1
            // Karate Chop => FIGHTING (gen1)
            updateMoveType(moves, Moves.karateChop, Type.FIGHTING);
            // Gust => FLYING (gen1)
            updateMoveType(moves, Moves.gust, Type.FLYING);
            // Wing Attack => 60 power (gen1)
            updateMovePower(moves, Moves.wingAttack, 60);
            // Whirlwind => 100 accuracy (gen1)
            updateMoveAccuracy(moves, Moves.whirlwind, 100);
            // Sand Attack => GROUND (gen1)
            updateMoveType(moves, Moves.sandAttack, Type.GROUND);
            // Double-Edge => 120 power (gen1)
            updateMovePower(moves, Moves.doubleEdge, 120);
            // Move 44, Bite, becomes dark (but doesn't exist anyway)
            // Blizzard => 70% accuracy (gen1)
            updateMoveAccuracy(moves, Moves.blizzard, 70);
            // Rock Throw => 90% accuracy (gen1)
            updateMoveAccuracy(moves, Moves.rockThrow, 90);
            // Hypnosis => 60% accuracy (gen1)
            updateMoveAccuracy(moves, Moves.hypnosis, 60);
            // SelfDestruct => 200power (gen1)
            updateMovePower(moves, Moves.selfDestruct, 200);
            // Explosion => 250 power (gen1)
            updateMovePower(moves, Moves.explosion, 250);
            // Dig => 60 power (gen1)
            updateMovePower(moves, Moves.dig, 60);
        }

        if (generation >= 3 && generationOfPokemon() < 3) {
            // Razor Wind => 100% accuracy (gen1/2)
            updateMoveAccuracy(moves, Moves.razorWind, 100);
            // Move 67, Low Kick, has weight-based power in gen3+
            // Low Kick => 100% accuracy (gen1)
            updateMoveAccuracy(moves, Moves.lowKick, 100);
        }

        if (generation >= 4 && generationOfPokemon() < 4) {
            // Fly => 90 power (gen1/2/3)
            updateMovePower(moves, Moves.fly, 90);
            // Vine Whip => 15 pp (gen1/2/3)
            updateMovePP(moves, Moves.vineWhip, 15);
            // Absorb => 25pp (gen1/2/3)
            updateMovePP(moves, Moves.absorb, 25);
            // Mega Drain => 15pp (gen1/2/3)
            updateMovePP(moves, Moves.megaDrain, 15);
            // Dig => 80 power (gen1/2/3)
            updateMovePower(moves, Moves.dig, 80);
            // Recover => 10pp (gen1/2/3)
            updateMovePP(moves, Moves.recover, 10);
            // Flash => 100% acc (gen1/2/3)
            updateMoveAccuracy(moves, Moves.flash, 100);
            // Petal Dance => 90 power (gen1/2/3)
            updateMovePower(moves, Moves.petalDance, 90);
            // Disable => 100% accuracy (gen1-4)
            updateMoveAccuracy(moves, Moves.disable, 80);
            // Jump Kick => 85 power
            updateMovePower(moves, Moves.jumpKick, 85);
            // Hi Jump Kick => 100 power
            updateMovePower(moves, Moves.highJumpKick, 100);

            if (generationOfPokemon() >= 2) {
                // Zap Cannon => 120 power (gen2-3)
                updateMovePower(moves, Moves.zapCannon, 120);
                // Outrage => 120 power (gen2-3)
                updateMovePower(moves, Moves.outrage, 120);
                updateMovePP(moves, Moves.outrage, 10);
                // Giga Drain => 10pp (gen2-3)
                updateMovePP(moves, Moves.gigaDrain, 10);
                // Rock Smash => 40 power (gen2-3)
                updateMovePower(moves, Moves.rockSmash, 40);
            }

            if (generationOfPokemon() == 3) {
                // Stockpile => 20 pp
                updateMovePP(moves, Moves.stockpile, 20);
                // Dive => 80 power
                updateMovePower(moves, Moves.dive, 80);
                // Leaf Blade => 90 power
                updateMovePower(moves, Moves.leafBlade, 90);
            }
        }

        if (generation >= 5 && generationOfPokemon() < 5) {
            // Bind => 85% accuracy (gen1-4)
            updateMoveAccuracy(moves, Moves.bind, 85);
            // Jump Kick => 10 pp, 100 power (gen1-4)
            updateMovePP(moves, Moves.jumpKick, 10);
            updateMovePower(moves, Moves.jumpKick, 100);
            // Tackle => 50 power, 100% accuracy , gen1-4
            updateMovePower(moves, Moves.tackle, 50);
            updateMoveAccuracy(moves, Moves.tackle, 100);
            // Wrap => 90% accuracy (gen1-4)
            updateMoveAccuracy(moves, Moves.wrap, 90);
            // Thrash => 120 power, 10pp (gen1-4)
            updateMovePP(moves, Moves.thrash, 10);
            updateMovePower(moves, Moves.thrash, 120);
            // Disable => 100% accuracy (gen1-4)
            updateMoveAccuracy(moves, Moves.disable, 100);
            // Petal Dance => 120power, 10pp (gen1-4)
            updateMovePP(moves, Moves.petalDance, 10);
            updateMovePower(moves, Moves.petalDance, 120);
            // Fire Spin => 35 power, 85% acc (gen1-4)
            updateMoveAccuracy(moves, Moves.fireSpin, 85);
            updateMovePower(moves, Moves.fireSpin, 35);
            // Toxic => 90% accuracy (gen1-4)
            updateMoveAccuracy(moves, Moves.toxic, 90);
            // Clamp => 15pp, 85% acc (gen1-4)
            updateMoveAccuracy(moves, Moves.clamp, 85);
            updateMovePP(moves, Moves.clamp, 15);
            // HJKick => 130 power, 10pp (gen1-4)
            updateMovePP(moves, Moves.highJumpKick, 10);
            updateMovePower(moves, Moves.highJumpKick, 130);
            // Glare => 90% acc (gen1-4)
            updateMoveAccuracy(moves, Moves.glare, 90);
            // Poison Gas => 80% acc (gen1-4)
            updateMoveAccuracy(moves, Moves.poisonGas, 80);
            // Crabhammer => 90% acc (gen1-4)
            updateMoveAccuracy(moves, Moves.crabhammer, 90);

            if (generationOfPokemon() >= 2) {
                // Curse => GHOST (gen2-4)
                updateMoveType(moves, Moves.curse, Type.GHOST);
                // Cotton Spore => 100% acc (gen2-4)
                updateMoveAccuracy(moves, Moves.cottonSpore, 100);
                // Scary Face => 100% acc (gen2-4)
                updateMoveAccuracy(moves, Moves.scaryFace, 100);
                // Bone Rush => 90% acc (gen2-4)
                updateMoveAccuracy(moves, Moves.boneRush, 90);
                // Giga Drain => 75 power (gen2-4)
                updateMovePower(moves, Moves.gigaDrain, 75);
                // Fury Cutter => 20 power (gen2-4)
                updateMovePower(moves, Moves.furyCutter, 20);
                // Future Sight => 10 pp, 100 power, 100% acc (gen2-4)
                updateMovePP(moves, Moves.futureSight, 10);
                updateMovePower(moves, Moves.futureSight, 100);
                updateMoveAccuracy(moves, Moves.futureSight, 100);
                // Whirlpool => 35 pow, 85% acc (gen2-4)
                updateMovePower(moves, Moves.whirlpool, 35);
                updateMoveAccuracy(moves, Moves.whirlpool, 85);
            }

            if (generationOfPokemon() >= 3) {
                // Uproar => 90 power (gen3-4)
                updateMovePower(moves, Moves.uproar, 90);
                // Sand Tomb => 35 pow, 85% acc (gen3-4)
                updateMovePower(moves, Moves.sandTomb, 35);
                updateMoveAccuracy(moves, Moves.sandTomb, 85);
                // Bullet Seed => 25 power (gen3-4)
                updateMovePower(moves, Moves.bulletSeed, 25);
                // Icicle Spear => 25 power (gen3-4)
                updateMovePower(moves, Moves.icicleSpear, 25);
                // Covet => 60 power (gen3-4)
                updateMovePower(moves, Moves.covet, 60);
                // Rock Blast => 90% acc (gen3-4)
                updateMoveAccuracy(moves, Moves.rockBlast, 90);
                // Doom Desire => 140 pow, 100% acc, gen3-4
                updateMovePower(moves, Moves.doomDesire, 140);
                updateMoveAccuracy(moves, Moves.doomDesire, 100);
            }

            if (generationOfPokemon() == 4) {
                // Feint => 30 pow
                updateMovePower(moves, Moves.feint, 30);
                // Last Resort => 140 pow
                updateMovePower(moves, Moves.lastResort, 140);
                // Drain Punch => 10 pp, 75 pow
                updateMovePP(moves, Moves.drainPunch, 10);
                updateMovePower(moves, Moves.drainPunch, 75);
                // Magma Storm => 75% acc
                updateMoveAccuracy(moves, Moves.magmaStorm, 75);
            }
        }

        if (generation >= 6 && generationOfPokemon() < 6) {
            // gen 1
            // Swords Dance 20 PP
            updateMovePP(moves, Moves.swordsDance, 20);
            // Whirlwind can't miss
            updateMoveAccuracy(moves, Moves.whirlwind, perfectAccuracy);
            // Vine Whip 25 PP, 45 Power
            updateMovePP(moves, Moves.vineWhip, 25);
            updateMovePower(moves, Moves.vineWhip, 45);
            // Pin Missile 25 Power, 95% Accuracy
            updateMovePower(moves, Moves.pinMissile, 25);
            updateMoveAccuracy(moves, Moves.pinMissile, 95);
            // Flamethrower 90 Power
            updateMovePower(moves, Moves.flamethrower, 90);
            // Hydro Pump 110 Power
            updateMovePower(moves, Moves.hydroPump, 110);
            // Surf 90 Power
            updateMovePower(moves, Moves.surf, 90);
            // Ice Beam 90 Power
            updateMovePower(moves, Moves.iceBeam, 90);
            // Blizzard 110 Power
            updateMovePower(moves, Moves.blizzard, 110);
            // Growth 20 PP
            updateMovePP(moves, Moves.growth, 20);
            // Thunderbolt 90 Power
            updateMovePower(moves, Moves.thunderbolt, 90);
            // Thunder 110 Power
            updateMovePower(moves, Moves.thunder, 110);
            // Minimize 10 PP
            updateMovePP(moves, Moves.minimize, 10);
            // Barrier 20 PP
            updateMovePP(moves, Moves.barrier, 20);
            // Lick 30 Power
            updateMovePower(moves, Moves.lick, 30);
            // Smog 30 Power
            updateMovePower(moves, Moves.smog, 30);
            // Fire Blast 110 Power
            updateMovePower(moves, Moves.fireBlast, 110);
            // Skull Bash 10 PP, 130 Power
            updateMovePP(moves, Moves.skullBash, 10);
            updateMovePower(moves, Moves.skullBash, 130);
            // Glare 100% Accuracy
            updateMoveAccuracy(moves, Moves.glare, 100);
            // Poison Gas 90% Accuracy
            updateMoveAccuracy(moves, Moves.poisonGas, 90);
            // Bubble 40 Power
            updateMovePower(moves, Moves.bubble, 40);
            // Psywave 100% Accuracy
            updateMoveAccuracy(moves, Moves.psywave, 100);
            // Acid Armor 20 PP
            updateMovePP(moves, Moves.acidArmor, 20);
            // Crabhammer 100 Power
            updateMovePower(moves, Moves.crabhammer, 100);

            if (generationOfPokemon() >= 2) {
                // Thief 25 PP, 60 Power
                updateMovePP(moves, Moves.thief, 25);
                updateMovePower(moves, Moves.thief, 60);
                // Snore 50 Power
                updateMovePower(moves, Moves.snore, 50);
                // Fury Cutter 40 Power
                updateMovePower(moves, Moves.furyCutter, 40);
                // Future Sight 120 Power
                updateMovePower(moves, Moves.futureSight, 120);
            }

            if (generationOfPokemon() >= 3) {
                // Heat Wave 95 Power
                updateMovePower(moves, Moves.heatWave, 95);
                // Will-o-Wisp 85% Accuracy
                updateMoveAccuracy(moves, Moves.willOWisp, 85);
                // Smellingsalt 70 Power
                updateMovePower(moves, Moves.smellingSalts, 70);
                // Knock off 65 Power
                updateMovePower(moves, Moves.knockOff, 65);
                // Meteor Mash 90 Power, 90% Accuracy
                updateMovePower(moves, Moves.meteorMash, 90);
                updateMoveAccuracy(moves, Moves.meteorMash, 90);
                // Air Cutter 60 Power
                updateMovePower(moves, Moves.airCutter, 60);
                // Overheat 130 Power
                updateMovePower(moves, Moves.overheat, 130);
                // Rock Tomb 15 PP, 60 Power, 95% Accuracy
                updateMovePP(moves, Moves.rockTomb, 15);
                updateMovePower(moves, Moves.rockTomb, 60);
                updateMoveAccuracy(moves, Moves.rockTomb, 95);
                // Extrasensory 20 PP
                updateMovePP(moves, Moves.extrasensory, 20);
                // Muddy Water 90 Power
                updateMovePower(moves, Moves.muddyWater, 90);
                // Covet 25 PP
                updateMovePP(moves, Moves.covet, 25);
            }

            if (generationOfPokemon() >= 4) {
                // Wake-Up Slap 70 Power
                updateMovePower(moves, Moves.wakeUpSlap, 70);
                // Tailwind 15 PP
                updateMovePP(moves, Moves.tailwind, 15);
                // Assurance 60 Power
                updateMovePower(moves, Moves.assurance, 60);
                // Psycho Shift 100% Accuracy
                updateMoveAccuracy(moves, Moves.psychoShift, 100);
                // Aura Sphere 80 Power
                updateMovePower(moves, Moves.auraSphere, 80);
                // Air Slash 15 PP
                updateMovePP(moves, Moves.airSlash, 15);
                // Dragon Pulse 85 Power
                updateMovePower(moves, Moves.dragonPulse, 85);
                // Power Gem 80 Power
                updateMovePower(moves, Moves.powerGem, 80);
                // Energy Ball 90 Power
                updateMovePower(moves, Moves.energyBall, 90);
                // Draco Meteor 130 Power
                updateMovePower(moves, Moves.dracoMeteor, 130);
                // Leaf Storm 130 Power
                updateMovePower(moves, Moves.leafStorm, 130);
                // Gunk Shot 80% Accuracy
                updateMoveAccuracy(moves, Moves.gunkShot, 80);
                // Chatter 65 Power
                updateMovePower(moves, Moves.chatter, 65);
                // Magma Storm 100 Power
                updateMovePower(moves, Moves.magmaStorm, 100);
            }

            if (generationOfPokemon() == 5) {
                // Synchronoise 120 Power
                updateMovePower(moves, Moves.synchronoise, 120);
                // Low Sweep 65 Power
                updateMovePower(moves, Moves.lowSweep, 65);
                // Hex 65 Power
                updateMovePower(moves, Moves.hex, 65);
                // Incinerate 60 Power
                updateMovePower(moves, Moves.incinerate, 60);
                // Pledges 80 Power
                updateMovePower(moves, Moves.waterPledge, 80);
                updateMovePower(moves, Moves.firePledge, 80);
                updateMovePower(moves, Moves.grassPledge, 80);
                // Struggle Bug 50 Power
                updateMovePower(moves, Moves.struggleBug, 50);
                // Frost Breath and Storm Throw 45 Power
                // Crits are 2x in these games, so we need to multiply BP by 3/4
                // Storm Throw was also updated to have a base BP of 60
                updateMovePower(moves, Moves.frostBreath, 45);
                updateMovePower(moves, Moves.stormThrow, 45);
                // Sacred Sword 15 PP
                updateMovePP(moves, Moves.sacredSword, 15);
                // Hurricane 110 Power
                updateMovePower(moves, Moves.hurricane, 110);
                // Techno Blast 120 Power
                updateMovePower(moves, Moves.technoBlast, 120);
            }
        }

        if (generation >= 7 && generationOfPokemon() < 7) {
            // Leech Life 80 Power, 10 PP
            updateMovePower(moves, Moves.leechLife, 80);
            updateMovePP(moves, Moves.leechLife, 10);
            // Submission 20 PP
            updateMovePP(moves, Moves.submission, 20);
            // Tackle 40 Power
            updateMovePower(moves, Moves.tackle, 40);
            // Thunder Wave 90% Accuracy
            updateMoveAccuracy(moves, Moves.thunderWave, 90);

            if (generationOfPokemon() >= 2) {
                // Swagger 85% Accuracy
                updateMoveAccuracy(moves, Moves.swagger, 85);
            }

            if (generationOfPokemon() >= 3) {
                // Knock Off 20 PP
                updateMovePP(moves, Moves.knockOff, 20);
            }

            if (generationOfPokemon() >= 4) {
                // Dark Void 50% Accuracy
                updateMoveAccuracy(moves, Moves.darkVoid, 50);
                // Sucker Punch 70 Power
                updateMovePower(moves, Moves.suckerPunch, 70);
            }

            if (generationOfPokemon() == 6) {
                // Aromatic Mist can't miss
                updateMoveAccuracy(moves, Moves.aromaticMist, perfectAccuracy);
                // Fell Stinger 50 Power
                updateMovePower(moves, Moves.fellStinger, 50);
                // Flying Press 100 Power
                updateMovePower(moves, Moves.flyingPress, 100);
                // Mat Block 10 PP
                updateMovePP(moves, Moves.matBlock, 10);
                // Mystical Fire 75 Power
                updateMovePower(moves, Moves.mysticalFire, 75);
                // Parabolic Charge 65 Power
                updateMovePower(moves, Moves.parabolicCharge, 65);
                // Topsy-Turvy can't miss
                updateMoveAccuracy(moves, Moves.topsyTurvy, perfectAccuracy);
                // Water Shuriken Special
                updateMoveCategory(moves, Moves.waterShuriken, MoveCategory.SPECIAL);
            }
        }

        if (generation >= 8 && generationOfPokemon() < 8) {
            if (generationOfPokemon() >= 2) {
                // Rapid Spin 50 Power
                updateMovePower(moves, Moves.rapidSpin, 50);
            }

            if (generationOfPokemon() == 7) {
                // Multi-Attack 120 Power
                updateMovePower(moves, Moves.multiAttack, 120);
            }
        }

        if (generation >= 9 && generationOfPokemon() < 9) {
            // Gen 1
            // Recover 5 PP
            updateMovePP(moves, Moves.recover, 5);
            // Soft-Boiled 5 PP
            updateMovePP(moves, Moves.softBoiled, 5);
            // Rest 5 PP
            updateMovePP(moves, Moves.rest, 5);

            if (generationOfPokemon() >= 2) {
                // Milk Drink 5 PP
                updateMovePP(moves, Moves.milkDrink, 5);
            }

            if (generationOfPokemon() >= 3) {
                // Slack Off 5 PP
                updateMovePP(moves, Moves.slackOff, 5);
            }

            if (generationOfPokemon() >= 4) {
                // Roost 5 PP
                updateMovePP(moves, Moves.roost, 5);
            }
            
            if (generationOfPokemon() >= 7) {
                // Shore Up 5 PP
                updateMovePP(moves, Moves.shoreUp, 5);
            }

            if (generationOfPokemon() >= 8) {
                // Grassy Glide 60 Power
                updateMovePower(moves, Moves.grassyGlide, 60);
                // Wicked Blow 75 Power
                updateMovePower(moves, Moves.wickedBlow, 75);
                // Glacial Lance 120 Power
                updateMovePower(moves, Moves.glacialLance, 120);
            }
        }
    }

    private Map<Integer, boolean[]> moveUpdates;

    @Override
    public void initMoveUpdates() {
        moveUpdates = new TreeMap<>();
    }

    @Override
    public Map<Integer, boolean[]> getMoveUpdates() {
        return moveUpdates;
    }

    @Override
    public void randomizeMovesLearnt(Settings settings) {
        boolean typeThemed = settings.getMovesetsMod() == Settings.MovesetsMod.RANDOM_PREFER_SAME_TYPE;
        boolean noBroken = settings.isBlockBrokenMovesetMoves();
        boolean forceStartingMoves = supportsFourStartingMoves() && settings.isStartWithGuaranteedMoves();
        int forceStartingMoveCount = settings.getGuaranteedMoveCount();
        double goodDamagingPercentage =
                settings.isMovesetsForceGoodDamaging() ? settings.getMovesetsGoodDamagingPercent() / 100.0 : 0;
        boolean evolutionMovesForAll = settings.isEvolutionMovesForAll();

        // Get current sets
        Map<Integer, List<MoveLearnt>> movesets = this.getMovesLearnt();

        // Build sets of moves
        List<Move> validMoves = new ArrayList<>();
        List<Move> validDamagingMoves = new ArrayList<>();
        Map<Type, List<Move>> validTypeMoves = new HashMap<>();
        Map<Type, List<Move>> validTypeDamagingMoves = new HashMap<>();
        createSetsOfMoves(noBroken, validMoves, validDamagingMoves, validTypeMoves, validTypeDamagingMoves);

        for (Integer pkmnNum : movesets.keySet()) {
            List<Integer> learnt = new ArrayList<>();
            List<MoveLearnt> moves = movesets.get(pkmnNum);
            int lv1AttackingMove = 0;
            Pokemon pkmn = findPokemonInPoolWithSpeciesID(mainPokemonListInclFormes, pkmnNum);
            if (pkmn == null) {
                continue;
            }

            double atkSpAtkRatio = pkmn.getAttackSpecialAttackRatio();

            // 4 starting moves?
            if (forceStartingMoves) {
                int lv1count = 0;
                for (MoveLearnt ml : moves) {
                    if (ml.level == 1) {
                        lv1count++;
                    }
                }
                if (lv1count < forceStartingMoveCount) {
                    for (int i = 0; i < forceStartingMoveCount - lv1count; i++) {
                        MoveLearnt fakeLv1 = new MoveLearnt();
                        fakeLv1.level = 1;
                        fakeLv1.move = 0;
                        moves.add(0, fakeLv1);
                    }
                }
            }

            if (evolutionMovesForAll) {
                if (moves.get(0).level != 0) {
                    MoveLearnt fakeEvoMove = new MoveLearnt();
                    fakeEvoMove.level = 0;
                    fakeEvoMove.move = 0;
                    moves.add(0, fakeEvoMove);
                }
            }

            if (pkmn.actuallyCosmetic) {
                for (int i = 0; i < moves.size(); i++) {
                    moves.get(i).move = movesets.get(pkmn.baseForme.number).get(i).move;
                }
                continue;
            }

            // Find last lv1 move
            // lv1index ends up as the index of the first non-lv1 move
            int lv1index = moves.get(0).level == 1 ? 0 : 1; // Evolution move handling (level 0 = evo move)
            while (lv1index < moves.size() && moves.get(lv1index).level == 1) {
                lv1index++;
            }

            // last lv1 move is 1 before lv1index
            if (lv1index != 0) {
                lv1index--;
            }

            // Force a certain amount of good damaging moves depending on the percentage
            int goodDamagingLeft = (int)Math.round(goodDamagingPercentage * moves.size());

            // Replace moves as needed
            for (int i = 0; i < moves.size(); i++) {
                // should this move be forced damaging?
                boolean attemptDamaging = i == lv1index || goodDamagingLeft > 0;

                // type themed?
                Type typeOfMove = null;
                if (typeThemed) {
                    double picked = random.nextDouble();
                    if ((pkmn.primaryType == Type.NORMAL && pkmn.secondaryType != null) ||
                            (pkmn.secondaryType == Type.NORMAL)) {

                        Type otherType = pkmn.primaryType == Type.NORMAL ? pkmn.secondaryType : pkmn.primaryType;

                        // Normal/OTHER: 10% normal, 30% other, 60% random
                        if (picked < 0.1) {
                            typeOfMove = Type.NORMAL;
                        } else if (picked < 0.4) {
                            typeOfMove = otherType;
                        }
                        // else random
                    } else if (pkmn.secondaryType != null) {
                        // Primary/Secondary: 20% primary, 20% secondary, 60% random
                        if (picked < 0.2) {
                            typeOfMove = pkmn.primaryType;
                        } else if (picked < 0.4) {
                            typeOfMove = pkmn.secondaryType;
                        }
                        // else random
                    } else {
                        // Primary/None: 40% primary, 60% random
                        if (picked < 0.4) {
                            typeOfMove = pkmn.primaryType;
                        }
                        // else random
                    }
                }

                // select a list to pick a move from that has at least one free
                List<Move> pickList = validMoves;
                if (attemptDamaging) {
                    if (typeOfMove != null) {
                        if (validTypeDamagingMoves.containsKey(typeOfMove)
                                && checkForUnusedMove(validTypeDamagingMoves.get(typeOfMove), learnt)) {
                            pickList = validTypeDamagingMoves.get(typeOfMove);
                        } else if (checkForUnusedMove(validDamagingMoves, learnt)) {
                            pickList = validDamagingMoves;
                        }
                    } else if (checkForUnusedMove(validDamagingMoves, learnt)) {
                        pickList = validDamagingMoves;
                    }
                    MoveCategory forcedCategory = random.nextDouble() < atkSpAtkRatio ? MoveCategory.PHYSICAL : MoveCategory.SPECIAL;
                    List<Move> filteredList = pickList.stream().filter(mv -> mv.category == forcedCategory).collect(Collectors.toList());
                    if (!filteredList.isEmpty() && checkForUnusedMove(filteredList, learnt)) {
                        pickList = filteredList;
                    }
                } else if (typeOfMove != null) {
                    if (validTypeMoves.containsKey(typeOfMove)
                            && checkForUnusedMove(validTypeMoves.get(typeOfMove), learnt)) {
                        pickList = validTypeMoves.get(typeOfMove);
                    }
                }

                // now pick a move until we get a valid one
                Move mv = pickList.get(random.nextInt(pickList.size()));
                while (learnt.contains(mv.number)) {
                    mv = pickList.get(random.nextInt(pickList.size()));
                }

                if (i == lv1index) {
                    lv1AttackingMove = mv.number;
                } else {
                    goodDamagingLeft--;
                }
                learnt.add(mv.number);

            }

            Collections.shuffle(learnt, random);
            if (learnt.get(lv1index) != lv1AttackingMove) {
                for (int i = 0; i < learnt.size(); i++) {
                    if (learnt.get(i) == lv1AttackingMove) {
                        learnt.set(i, learnt.get(lv1index));
                        learnt.set(lv1index, lv1AttackingMove);
                        break;
                    }
                }
            }

            // write all moves for the pokemon
            for (int i = 0; i < learnt.size(); i++) {
                moves.get(i).move = learnt.get(i);
                if (i == lv1index) {
                    // just in case, set this to lv1
                    moves.get(i).level = 1;
                }
            }
        }
        // Done, save
        this.setMovesLearnt(movesets);

    }

    @Override
    public void randomizeEggMoves(Settings settings) {
        boolean typeThemed = settings.getMovesetsMod() == Settings.MovesetsMod.RANDOM_PREFER_SAME_TYPE;
        boolean noBroken = settings.isBlockBrokenMovesetMoves();
        double goodDamagingPercentage =
                settings.isMovesetsForceGoodDamaging() ? settings.getMovesetsGoodDamagingPercent() / 100.0 : 0;

        // Get current sets
        Map<Integer, List<Integer>> movesets = this.getEggMoves();

        // Build sets of moves
        List<Move> validMoves = new ArrayList<>();
        List<Move> validDamagingMoves = new ArrayList<>();
        Map<Type, List<Move>> validTypeMoves = new HashMap<>();
        Map<Type, List<Move>> validTypeDamagingMoves = new HashMap<>();
        createSetsOfMoves(noBroken, validMoves, validDamagingMoves, validTypeMoves, validTypeDamagingMoves);

        for (Integer pkmnNum : movesets.keySet()) {
            List<Integer> learnt = new ArrayList<>();
            List<Integer> moves = movesets.get(pkmnNum);
            Pokemon pkmn = findPokemonInPoolWithSpeciesID(mainPokemonListInclFormes, pkmnNum);
            if (pkmn == null) {
                continue;
            }

            double atkSpAtkRatio = pkmn.getAttackSpecialAttackRatio();

            if (pkmn.actuallyCosmetic) {
                for (int i = 0; i < moves.size(); i++) {
                    moves.set(i, movesets.get(pkmn.baseForme.number).get(i));
                }
                continue;
            }

            // Force a certain amount of good damaging moves depending on the percentage
            int goodDamagingLeft = (int)Math.round(goodDamagingPercentage * moves.size());

            // Replace moves as needed
            for (int i = 0; i < moves.size(); i++) {
                // should this move be forced damaging?
                boolean attemptDamaging = goodDamagingLeft > 0;

                // type themed?
                Type typeOfMove = null;
                if (typeThemed) {
                    double picked = random.nextDouble();
                    if ((pkmn.primaryType == Type.NORMAL && pkmn.secondaryType != null) ||
                            (pkmn.secondaryType == Type.NORMAL)) {

                        Type otherType = pkmn.primaryType == Type.NORMAL ? pkmn.secondaryType : pkmn.primaryType;

                        // Normal/OTHER: 10% normal, 30% other, 60% random
                        if (picked < 0.1) {
                            typeOfMove = Type.NORMAL;
                        } else if (picked < 0.4) {
                            typeOfMove = otherType;
                        }
                        // else random
                    } else if (pkmn.secondaryType != null) {
                        // Primary/Secondary: 20% primary, 20% secondary, 60% random
                        if (picked < 0.2) {
                            typeOfMove = pkmn.primaryType;
                        } else if (picked < 0.4) {
                            typeOfMove = pkmn.secondaryType;
                        }
                        // else random
                    } else {
                        // Primary/None: 40% primary, 60% random
                        if (picked < 0.4) {
                            typeOfMove = pkmn.primaryType;
                        }
                        // else random
                    }
                }

                // select a list to pick a move from that has at least one free
                List<Move> pickList = validMoves;
                if (attemptDamaging) {
                    if (typeOfMove != null) {
                        if (validTypeDamagingMoves.containsKey(typeOfMove)
                                && checkForUnusedMove(validTypeDamagingMoves.get(typeOfMove), learnt)) {
                            pickList = validTypeDamagingMoves.get(typeOfMove);
                        } else if (checkForUnusedMove(validDamagingMoves, learnt)) {
                            pickList = validDamagingMoves;
                        }
                    } else if (checkForUnusedMove(validDamagingMoves, learnt)) {
                        pickList = validDamagingMoves;
                    }
                    MoveCategory forcedCategory = random.nextDouble() < atkSpAtkRatio ? MoveCategory.PHYSICAL : MoveCategory.SPECIAL;
                    List<Move> filteredList = pickList.stream().filter(mv -> mv.category == forcedCategory).collect(Collectors.toList());
                    if (!filteredList.isEmpty() && checkForUnusedMove(filteredList, learnt)) {
                        pickList = filteredList;
                    }
                } else if (typeOfMove != null) {
                    if (validTypeMoves.containsKey(typeOfMove)
                            && checkForUnusedMove(validTypeMoves.get(typeOfMove), learnt)) {
                        pickList = validTypeMoves.get(typeOfMove);
                    }
                }

                // now pick a move until we get a valid one
                Move mv = pickList.get(random.nextInt(pickList.size()));
                while (learnt.contains(mv.number)) {
                    mv = pickList.get(random.nextInt(pickList.size()));
                }

                goodDamagingLeft--;
                learnt.add(mv.number);
            }

            // write all moves for the pokemon
            Collections.shuffle(learnt, random);
            for (int i = 0; i < learnt.size(); i++) {
                moves.set(i, learnt.get(i));
            }
        }
        // Done, save
        this.setEggMoves(movesets);
    }

    private void createSetsOfMoves(boolean noBroken, List<Move> validMoves, List<Move> validDamagingMoves,
                                   Map<Type, List<Move>> validTypeMoves, Map<Type, List<Move>> validTypeDamagingMoves) {
        List<Move> allMoves = this.getMoves();
        List<Integer> hms = this.getHMMoves();
        Set<Integer> allBanned = new HashSet<Integer>(noBroken ? this.getGameBreakingMoves() : Collections.EMPTY_SET);
        allBanned.addAll(hms);
        allBanned.addAll(this.getMovesBannedFromLevelup());
        allBanned.addAll(GlobalConstants.zMoves);
        allBanned.addAll(this.getIllegalMoves());

        for (Move mv : allMoves) {
            if (mv != null && !GlobalConstants.bannedRandomMoves[mv.number] && !allBanned.contains(mv.number)) {
                validMoves.add(mv);
                if (mv.type != null) {
                    if (!validTypeMoves.containsKey(mv.type)) {
                        validTypeMoves.put(mv.type, new ArrayList<>());
                    }
                    validTypeMoves.get(mv.type).add(mv);
                }

                if (!GlobalConstants.bannedForDamagingMove[mv.number]) {
                    if (mv.isGoodDamaging(perfectAccuracy)) {
                        validDamagingMoves.add(mv);
                        if (mv.type != null) {
                            if (!validTypeDamagingMoves.containsKey(mv.type)) {
                                validTypeDamagingMoves.put(mv.type, new ArrayList<>());
                            }
                            validTypeDamagingMoves.get(mv.type).add(mv);
                        }
                    }
                }
            }
        }

        Map<Type,Double> avgTypePowers = new TreeMap<>();
        double totalAvgPower = 0;

        for (Type type: validTypeMoves.keySet()) {
            List<Move> typeMoves = validTypeMoves.get(type);
            int attackingSum = 0;
            for (Move typeMove: typeMoves) {
                if (typeMove.power > 0) {
                    attackingSum += (typeMove.power * typeMove.hitCount);
                }
            }
            double avgTypePower = (double)attackingSum / (double)typeMoves.size();
            avgTypePowers.put(type, avgTypePower);
            totalAvgPower += (avgTypePower);
        }

        totalAvgPower /= (double)validTypeMoves.keySet().size();

        // Want the average power of each type to be within 25% both directions
        double minAvg = totalAvgPower * 0.75;
        double maxAvg = totalAvgPower * 1.25;

        // Add extra moves to type lists outside of the range to balance the average power of each type

        for (Type type: avgTypePowers.keySet()) {
            double avgPowerForType = avgTypePowers.get(type);
            List<Move> typeMoves = validTypeMoves.get(type);
            List<Move> alreadyPicked = new ArrayList<>();
            int iterLoops = 0;
            while (avgPowerForType < minAvg && iterLoops < 10000) {
                final double finalAvgPowerForType = avgPowerForType;
                List<Move> strongerThanAvgTypeMoves = typeMoves
                        .stream()
                        .filter(mv -> mv.power * mv.hitCount > finalAvgPowerForType)
                        .collect(Collectors.toList());
                if (strongerThanAvgTypeMoves.isEmpty()) break;
                if (alreadyPicked.containsAll(strongerThanAvgTypeMoves)) {
                    alreadyPicked = new ArrayList<>();
                } else {
                    strongerThanAvgTypeMoves.removeAll(alreadyPicked);
                }
                Move extraMove = strongerThanAvgTypeMoves.get(random.nextInt(strongerThanAvgTypeMoves.size()));
                avgPowerForType = (avgPowerForType * typeMoves.size() + extraMove.power * extraMove.hitCount)
                        / (typeMoves.size() + 1);
                typeMoves.add(extraMove);
                alreadyPicked.add(extraMove);
                iterLoops++;
            }
            iterLoops = 0;
            while (avgPowerForType > maxAvg && iterLoops < 10000) {
                final double finalAvgPowerForType = avgPowerForType;
                List<Move> weakerThanAvgTypeMoves = typeMoves
                        .stream()
                        .filter(mv -> mv.power * mv.hitCount < finalAvgPowerForType)
                        .collect(Collectors.toList());
                if (weakerThanAvgTypeMoves.isEmpty()) break;
                if (alreadyPicked.containsAll(weakerThanAvgTypeMoves)) {
                    alreadyPicked = new ArrayList<>();
                } else {
                    weakerThanAvgTypeMoves.removeAll(alreadyPicked);
                }
                Move extraMove = weakerThanAvgTypeMoves.get(random.nextInt(weakerThanAvgTypeMoves.size()));
                avgPowerForType = (avgPowerForType * typeMoves.size() + extraMove.power * extraMove.hitCount)
                        / (typeMoves.size() + 1);
                typeMoves.add(extraMove);
                alreadyPicked.add(extraMove);
                iterLoops++;
            }
        }
    }

    @Override
    public void orderDamagingMovesByDamage() {
        Map<Integer, List<MoveLearnt>> movesets = this.getMovesLearnt();
        List<Move> allMoves = this.getMoves();
        for (Integer pkmn : movesets.keySet()) {
            List<MoveLearnt> moves = movesets.get(pkmn);

            // Build up a list of damaging moves and their positions
            List<Integer> damagingMoveIndices = new ArrayList<>();
            List<Move> damagingMoves = new ArrayList<>();
            for (int i = 0; i < moves.size(); i++) {
                if (moves.get(i).level == 0) continue; // Don't reorder evolution move
                Move mv = allMoves.get(moves.get(i).move);
                if (mv.power > 1) {
                    // considered a damaging move for this purpose
                    damagingMoveIndices.add(i);
                    damagingMoves.add(mv);
                }
            }

            // Ties should be sorted randomly, so shuffle the list first.
            Collections.shuffle(damagingMoves, random);

            // Sort the damaging moves by power
            damagingMoves.sort(Comparator.comparingDouble(m -> m.power * m.hitCount));

            // Reassign damaging moves in the ordered positions
            for (int i = 0; i < damagingMoves.size(); i++) {
                moves.get(damagingMoveIndices.get(i)).move = damagingMoves.get(i).number;
            }
        }

        // Done, save
        this.setMovesLearnt(movesets);
    }

    @Override
    public void metronomeOnlyMode() {

        // movesets
        Map<Integer, List<MoveLearnt>> movesets = this.getMovesLearnt();

        MoveLearnt metronomeML = new MoveLearnt();
        metronomeML.level = 1;
        metronomeML.move = Moves.metronome;

        for (List<MoveLearnt> ms : movesets.values()) {
            if (ms != null && ms.size() > 0) {
                ms.clear();
                ms.add(metronomeML);
            }
        }

        this.setMovesLearnt(movesets);

        // trainers
        // run this to remove all custom non-Metronome moves
        List<Trainer> trainers = this.getTrainers();

        for (Trainer t : trainers) {
            for (TrainerPokemon tpk : t.pokemon) {
                tpk.resetMoves = true;
            }
        }

        this.setTrainers(trainers, false);

        // tms
        List<Integer> tmMoves = this.getTMMoves();

        for (int i = 0; i < tmMoves.size(); i++) {
            tmMoves.set(i, Moves.metronome);
        }

        this.setTMMoves(tmMoves);

        // movetutors
        if (this.hasMoveTutors()) {
            List<Integer> mtMoves = this.getMoveTutorMoves();

            for (int i = 0; i < mtMoves.size(); i++) {
                mtMoves.set(i, Moves.metronome);
            }

            this.setMoveTutorMoves(mtMoves);
        }

        // move tweaks
        List<Move> moveData = this.getMoves();

        Move metronome = moveData.get(Moves.metronome);

        metronome.pp = 40;

        List<Integer> hms = this.getHMMoves();

        for (int hm : hms) {
            Move thisHM = moveData.get(hm);
            thisHM.pp = 0;
        }
    }

    @Override
    public void generateStarters(Settings settings) {
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
        int starterCount = starterCount();

        List<Pokemon> choosableList;

        if (allowAltFormes) {
            choosableList = new ArrayList<>(noLegendaries ? noLegendaryListInclFormes : mainPokemonListInclFormes);
            if(abilitiesUnchanged) {
                choosableList.removeAll(getAbilityDependentFormes());
            }
            if(banIrregularAltFormes) {
                choosableList.removeAll(getIrregularFormes());
            }
            choosableList.removeIf(p -> p.actuallyCosmetic);
        } else {
            choosableList = new ArrayList<>(noLegendaries ? noLegendaryList : mainPokemonList);
        }

        pickedStarters = new ArrayList<>();

        if(useCustomStarters) {
            List<Pokemon> romPokemon = getPokemonInclFormes()
                    .stream()
                    .filter(pk -> pk == null || !pk.actuallyCosmetic)
                    .collect(Collectors.toList());

            for (int i = 0; i < customStarters.length; i = i + 1) {
                if (!(customStarters[i] - 1 == 0)) {
                    Pokemon starter = romPokemon.get(customStarters[i] - 1);
                    choosableList.remove(starter);
                    pickedStarters.add(starter);
                }
            }

            if(pickedStarters.size() == starterCount) {
                return;
            } else if (pickedStarters.size() > starterCount) {
                //what.
                throw new RandomizationException("Custom starter list exceeded starter count?!");
            }
        }

        if (noDualTypes) {
            choosableList.removeIf(p -> p.secondaryType != null);
        }
        if(basicOnly) {
            choosableList.removeIf(p -> !p.evolutionsTo.isEmpty());
        }
        if(triStageOnly) {
            List<Pokemon> invalids = new ArrayList<>();
            for(Pokemon poke : choosableList) {
                boolean isTriStage = false;
                for(Evolution evo : poke.evolutionsFrom) {
                    if(!evo.to.evolutionsFrom.isEmpty()) {
                        isTriStage = true;
                    }
                }
                if(!isTriStage) {
                    invalids.add(poke);
                }
            }
            choosableList.removeAll(invalids);
            //there's probably a better way to do this but im too sleepy to think of it. anyway, it works.
        }

        //all constraints except type done!
        //sanity check
        if(choosableList.size() < starterCount - pickedStarters.size()) {
            throw new RandomizationException("Not enough valid starters");
        }

        if(!hasTypeRestriction) {
            while (pickedStarters.size() < starterCount) {
                Pokemon picked = choosableList.get(this.random.nextInt(choosableList.size()));
                pickedStarters.add(picked);
                choosableList.remove(picked);
            }

        } else if(typeUnique) {
            //we don't actually need a type map for this one

            //remove pokemon that share types with custom selected pokemon
            for (Pokemon starter : pickedStarters) {
                choosableList.removeIf(p -> (p.primaryType == starter.primaryType || p.secondaryType == starter.primaryType));
                if(starter.secondaryType != null) {
                    choosableList.removeIf(p -> (p.primaryType == starter.secondaryType || p.secondaryType == starter.secondaryType));
                }
            }

            //now, choose pokemon and remove others of their types
            while (pickedStarters.size() < starterCount) {
                Pokemon picked = choosableList.get(this.random.nextInt(choosableList.size()));
                pickedStarters.add(picked);
                choosableList.remove(picked);
                choosableList.removeIf(p -> (p.primaryType == picked.primaryType || p.secondaryType == picked.primaryType));
                if(picked.secondaryType != null) {
                    choosableList.removeIf(p -> (p.primaryType == picked.secondaryType || p.secondaryType == picked.secondaryType));
                }
            }


        } else {

            //build type map
            Map<Type, List<Pokemon>> typeListMap = new EnumMap<>(Type.class);
            for(Type type : Type.getAllTypes(this.generationOfPokemon())) {
                typeListMap.put(type, new ArrayList<>());
            }
            for (Pokemon poke : choosableList) {
                typeListMap.get(poke.primaryType).add(poke);
                if(poke.secondaryType != null) {
                    typeListMap.get(poke.secondaryType).add(poke);
                }
            }

            //assuming only one type restriction (not counting noDualTypes)
            //also assuming that the triangle restrictions (typeTriangle, fireWaterGrass)
            //are not used with custom starters
            if(typeTriangle) {
                pickedStarters = starterRandomTypeTriangle(typeListMap, starterCount);
            } else if (typeFwg) {
                pickedStarters = chooseStartersFireWaterGrass(typeListMap, starterCount);
            } else if(typeSingle) {
                int iterLoops = 0;
                while(singleType == null && iterLoops < 10000) {
                    singleType = this.randomType();
                    if(typeListMap.get(singleType).size() < (starterCount - pickedStarters.size())) {
                        singleType = null;
                    }
                    iterLoops++;
                }

                List<Pokemon> typeList = typeListMap.get(singleType);

                while(pickedStarters.size() < starterCount) {
                    Pokemon picked = typeList.get(this.random.nextInt(typeList.size()));
                    pickedStarters.add(picked);
                    typeList.remove(picked);
                    //there is no longer anything that can invalidate this pokemon
                }
            } //no other case
        }

        setStarters(pickedStarters);
    }

    private List<Pokemon> starterRandomTypeTriangle(Map<Type, List<Pokemon>> validPokemonByType, int countNeeded) {

        if(countNeeded < 3) {
            throw new RandomizationException("Attempted to form type triangle with less than 3 starters.");
        }

        int generation = this.generationOfPokemon();
        boolean effectivenessUpdated = isEffectivenessUpdated();
        List<Pokemon> startersChosen = new ArrayList<>();

        //find type triangles
        List<List<Type>> typeTriangles = null;
        typeTriangles = new ArrayList<>();
        for(Type typeOne : Type.getAllTypes(generation)) {
            List<Type> superEffectiveOne = Effectiveness.superEffective(typeOne, generation, effectivenessUpdated);
            superEffectiveOne.remove(typeOne);
            //don't want a Ghost-Ghost-Ghost or Dragon-Dragon-Dragon "triangle"
            //(although it would be funny)
            for (Type typeTwo : superEffectiveOne) {
                List<Type> superEffectiveTwo = Effectiveness.superEffective(typeTwo, generation, effectivenessUpdated);
                superEffectiveTwo.remove(typeOne);
                superEffectiveTwo.remove(typeTwo);
                for (Type typeThree : superEffectiveTwo) {
                    List<Type> superEffectiveThree = Effectiveness.superEffective(typeThree, generation, effectivenessUpdated);
                    if (superEffectiveThree.contains(typeOne)) {
                        //we found a triangle!
                        List<Type> triangle = new ArrayList<Type>(3);
                        triangle.add(typeThree);
                        triangle.add(typeTwo);
                        triangle.add(typeOne);
                        //reversed so that the triangle will be in the right order
                        typeTriangles.add(triangle);
                        //this will actually add each triangle three times (one from each starting type)
                        //but that's okay
                    }
                }
            }
        }

        //okay, we found our triangles! now pick one and pick starters from it.
        while(startersChosen.isEmpty() && !typeTriangles.isEmpty()) {
            //loop because we might find that there isn't a pokemon set of the appropriate types
            List<Type> triangle = typeTriangles.get(this.random.nextInt(typeTriangles.size()));
            boolean noPokemonSet = false;
            for (Type type : triangle) {
                List<Pokemon> typeList = new ArrayList<>(validPokemonByType.get(type));
                //clone so we can safely drain it
                boolean noPick = true;
                while(noPick && !typeList.isEmpty()) {
                    Pokemon picked = typeList.get(this.random.nextInt(typeList.size()));
                    typeList.remove(picked);

                    //make sure the pokemon's other type isn't in the triangle to confuse things
                    Type otherType = null;
                    if (picked.primaryType == type) {
                        otherType = picked.secondaryType;
                    } else {
                        otherType = picked.primaryType;
                    }
                    if(!triangle.contains(otherType)) {
                        //this pokemon works
                        noPick = false;
                        startersChosen.add(picked);
                    }
                }
                if(noPick) {
                    startersChosen = new ArrayList();
                    break;
                }
            }
            if(startersChosen.isEmpty()) {
                typeTriangles.remove(triangle);
            }
        }
        if(startersChosen.isEmpty()) {
            throw new RandomizationException("No valid starter set with a type triangle could be found!");
        }

        if(countNeeded > 3) {
            //we'll need ANOTHER triangle
            //go ahead and remove the pokemon we chose, so the same one doesn't appear in both sets
            for(Pokemon p : startersChosen) {
                validPokemonByType.get(p.primaryType).remove(p);
                if(p.secondaryType != null) {
                    validPokemonByType.get(p.secondaryType).remove(p);
                }
            }

            startersChosen.addAll(starterRandomTypeTriangle(validPokemonByType, countNeeded - 3));
        }

        return startersChosen;
    }

    private List<Pokemon> chooseStartersFireWaterGrass(Map<Type, List<Pokemon>> validPokemonByType, int countNeeded) {

        if(countNeeded < 3) {
            throw new RandomizationException("Attempted to form type triangle with less than 3 starters.");
        }

        List<Pokemon> startersChosen = new ArrayList<>();

        //turns out most of the games start with grass. So,
        //Grass
        List<Pokemon> typeList = new ArrayList<>(validPokemonByType.get(Type.GRASS));
        //clone so we can safely drain it
        boolean noPick = true;
        while(noPick && !typeList.isEmpty()) {
            Pokemon picked = typeList.get(this.random.nextInt(typeList.size()));
            typeList.remove(picked);
            Type otherType = null;

            //check other type
            if (picked.primaryType == Type.GRASS) {
                otherType = picked.secondaryType;
            } else {
                otherType = picked.primaryType;
            }
            if(otherType != Type.FIRE && otherType != Type.WATER) {
                //this pokemon works
                noPick = false;
                startersChosen.add(picked);
            }
        }
        if(noPick) {
            throw new RandomizationException("No valid Grass-type starter found!");
        }

        //Fire
        typeList = new ArrayList<>(validPokemonByType.get(Type.FIRE));
        //clone so we can safely drain it
        noPick = true;
        while(noPick && !typeList.isEmpty()) {
            Pokemon picked = typeList.get(this.random.nextInt(typeList.size()));
            typeList.remove(picked);

            //make sure the other type isn't water or grass
            //that would confuse things
            Type otherType = null;
            if (picked.primaryType == Type.FIRE) {
                otherType = picked.secondaryType;
            } else {
                otherType = picked.primaryType;
            }
            if(otherType != Type.WATER && otherType != Type.GRASS) {
                //this pokemon works
                noPick = false;
                startersChosen.add(picked);
            }
        }
        if(noPick) {
            throw new RandomizationException("No valid Fire-type starter found!");
        }

        //Water
        typeList = new ArrayList<>(validPokemonByType.get(Type.WATER));
        //clone so we can safely drain it
        noPick = true;
        while(noPick && !typeList.isEmpty()) {
            Pokemon picked = typeList.get(this.random.nextInt(typeList.size()));
            typeList.remove(picked);

            //check other type
            Type otherType = null;
            if (picked.primaryType == Type.WATER) {
                otherType = picked.secondaryType;
            } else {
                otherType = picked.primaryType;
            }
            if(otherType != Type.FIRE && otherType != Type.GRASS) {
                //this pokemon works
                noPick = false;
                startersChosen.add(picked);
            }
        }
        if(noPick) {
            throw new RandomizationException("No valid Water-type starter found!");
        }

        if(generationOfPokemon() <= 2) {
            //in gen 1 and 2, we need FWG rather than GFW
            Pokemon grassStarter = startersChosen.remove(0);
            startersChosen.add(grassStarter);
        }

        if(countNeeded > 3) {
            //we'll need ANOTHER triangle
            startersChosen.addAll(chooseStartersFireWaterGrass(validPokemonByType, countNeeded - 3));
        }

        return startersChosen;
    }

    @Override
    public List<Pokemon> getPickedStarters() {
        return pickedStarters;
    }


    @Override
    public void randomizeStaticPokemon(Settings settings) {
        boolean swapLegendaries = settings.getStaticPokemonMod() == Settings.StaticPokemonMod.RANDOM_MATCHING;
        boolean similarStrength = settings.getStaticPokemonMod() == Settings.StaticPokemonMod.SIMILAR_STRENGTH;
        boolean limitMainGameLegendaries = settings.isLimitMainGameLegendaries();
        boolean limit600 = settings.isLimit600();
        boolean allowAltFormes = settings.isAllowStaticAltFormes();
        boolean banIrregularAltFormes = settings.isBanIrregularAltFormes();
        boolean swapMegaEvos = settings.isSwapStaticMegaEvos();
        boolean abilitiesAreRandomized = settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE;
        int levelModifier = settings.isStaticLevelModified() ? settings.getStaticLevelModifier() : 0;
        boolean correctStaticMusic = settings.isCorrectStaticMusic();

        // Load
        checkPokemonRestrictions();
        List<StaticEncounter> currentStaticPokemon = this.getStaticPokemon();
        List<StaticEncounter> replacements = new ArrayList<>();
        List<Pokemon> banned = this.bannedForStaticPokemon();
        banned.addAll(this.getBannedFormesForPlayerPokemon());
        if (!abilitiesAreRandomized) {
            List<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(getIrregularFormes());
        }
        boolean reallySwapMegaEvos = forceSwapStaticMegaEvos() || swapMegaEvos;

        Map<Integer,Integer> specialMusicStaticChanges = new HashMap<>();
        List<Integer> changeMusicStatics = new ArrayList<>();
        if (correctStaticMusic) {
            changeMusicStatics = getSpecialMusicStatics();
        }

        if (swapLegendaries) {
            List<Pokemon> legendariesLeft = new ArrayList<>(onlyLegendaryList);
            if (allowAltFormes) {
                legendariesLeft.addAll(onlyLegendaryAltsList);
                legendariesLeft =
                        legendariesLeft
                                .stream()
                                .filter(pk -> !pk.actuallyCosmetic)
                                .collect(Collectors.toList());
            }
            List<Pokemon> nonlegsLeft = new ArrayList<>(noLegendaryList);
            if (allowAltFormes) {
                nonlegsLeft.addAll(noLegendaryAltsList);
                nonlegsLeft =
                        nonlegsLeft
                                .stream()
                                .filter(pk -> !pk.actuallyCosmetic)
                                .collect(Collectors.toList());
            }
            List<Pokemon> ultraBeastsLeft = new ArrayList<>(ultraBeastList);
            legendariesLeft.removeAll(banned);
            nonlegsLeft.removeAll(banned);
            ultraBeastsLeft.removeAll(banned);

            // Full pools for easier refilling later
            List<Pokemon> legendariesPool = new ArrayList<>(legendariesLeft);
            List<Pokemon> nonlegsPool = new ArrayList<>(nonlegsLeft);
            List<Pokemon> ultraBeastsPool = new ArrayList<>(ultraBeastsLeft);

            for (StaticEncounter old : currentStaticPokemon) {
                StaticEncounter newStatic = cloneStaticEncounter(old);
                Pokemon newPK;
                if (old.pkmn.isLegendary()) {
                    if (reallySwapMegaEvos && old.canMegaEvolve()) {
                        newPK = getMegaEvoPokemon(onlyLegendaryList, legendariesLeft, newStatic);
                    } else {
                        if (old.restrictedPool) {
                            newPK = getRestrictedPokemon(legendariesPool, legendariesLeft, old);
                        } else {
                            newPK = legendariesLeft.remove(this.random.nextInt(legendariesLeft.size()));
                        }
                    }

                    setPokemonAndFormeForStaticEncounter(newStatic, newPK);

                    if (legendariesLeft.size() == 0) {
                        legendariesLeft.addAll(legendariesPool);
                    }
                } else if (ultraBeastList.contains(old.pkmn)) {
                    if (old.restrictedPool) {
                        newPK = getRestrictedPokemon(ultraBeastsPool, ultraBeastsLeft, old);
                    } else {
                        newPK = ultraBeastsLeft.remove(this.random.nextInt(ultraBeastsLeft.size()));
                    }

                    setPokemonAndFormeForStaticEncounter(newStatic, newPK);

                    if (ultraBeastsLeft.size() == 0) {
                        ultraBeastsLeft.addAll(ultraBeastsPool);
                    }
                } else {
                    if (reallySwapMegaEvos && old.canMegaEvolve()) {
                        newPK = getMegaEvoPokemon(noLegendaryList, nonlegsLeft, newStatic);
                    } else {
                        if (old.restrictedPool) {
                            newPK = getRestrictedPokemon(nonlegsPool, nonlegsLeft, old);
                        } else {
                            newPK = nonlegsLeft.remove(this.random.nextInt(nonlegsLeft.size()));
                        }
                    }
                    setPokemonAndFormeForStaticEncounter(newStatic, newPK);

                    if (nonlegsLeft.size() == 0) {
                        nonlegsLeft.addAll(nonlegsPool);
                    }
                }
                replacements.add(newStatic);
                if (changeMusicStatics.contains(old.pkmn.number)) {
                    specialMusicStaticChanges.put(old.pkmn.number, newPK.number);
                }
            }
        } else if (similarStrength) {
            List<Pokemon> listInclFormesExclCosmetics =
                    mainPokemonListInclFormes
                            .stream()
                            .filter(pk -> !pk.actuallyCosmetic)
                            .collect(Collectors.toList());
            List<Pokemon> pokemonLeft = new ArrayList<>(!allowAltFormes ? mainPokemonList : listInclFormesExclCosmetics);
            pokemonLeft.removeAll(banned);

            List<Pokemon> pokemonPool = new ArrayList<>(pokemonLeft);

            List<Integer> mainGameLegendaries = getMainGameLegendaries();
            for (StaticEncounter old : currentStaticPokemon) {
                StaticEncounter newStatic = cloneStaticEncounter(old);
                Pokemon newPK;
                Pokemon oldPK = old.pkmn;
                if (old.forme > 0) {
                    oldPK = getAltFormeOfPokemon(oldPK, old.forme);
                }
                Integer oldBST = oldPK.bstForPowerLevels();
                if (oldBST >= 600 && limit600) {
                    if (reallySwapMegaEvos && old.canMegaEvolve()) {
                        newPK = getMegaEvoPokemon(mainPokemonList, pokemonLeft, newStatic);
                    } else {
                        if (old.restrictedPool) {
                            newPK = getRestrictedPokemon(pokemonPool, pokemonLeft, old);
                        } else {
                            newPK = pokemonLeft.remove(this.random.nextInt(pokemonLeft.size()));
                        }
                    }
                    setPokemonAndFormeForStaticEncounter(newStatic, newPK);
                } else {
                    boolean limitBST = oldPK.baseForme == null ?
                            limitMainGameLegendaries && mainGameLegendaries.contains(oldPK.number) :
                            limitMainGameLegendaries && mainGameLegendaries.contains(oldPK.baseForme.number);
                    if (reallySwapMegaEvos && old.canMegaEvolve()) {
                        List<Pokemon> megaEvoPokemonLeft =
                                megaEvolutionsList
                                        .stream()
                                        .filter(mega -> mega.method == 1)
                                        .map(mega -> mega.from)
                                        .distinct()
                                        .filter(pokemonLeft::contains)
                                        .collect(Collectors.toList());
                        if (megaEvoPokemonLeft.isEmpty()) {
                            megaEvoPokemonLeft =
                                    megaEvolutionsList
                                            .stream()
                                            .filter(mega -> mega.method == 1)
                                            .map(mega -> mega.from)
                                            .distinct()
                                            .filter(mainPokemonList::contains)
                                            .collect(Collectors.toList());
                        }
                        newPK = pickStaticPowerLvlReplacement(
                                megaEvoPokemonLeft,
                                oldPK,
                                true,
                                limitBST);
                        newStatic.heldItem = newPK
                                .megaEvolutionsFrom
                                .get(this.random.nextInt(newPK.megaEvolutionsFrom.size()))
                                .argument;
                    } else {
                        if (old.restrictedPool) {
                            List<Pokemon> restrictedPool = pokemonLeft
                                            .stream()
                                            .filter(pk -> old.restrictedList.contains(pk))
                                            .collect(Collectors.toList());
                            if (restrictedPool.isEmpty()) {
                                restrictedPool = pokemonPool
                                        .stream()
                                        .filter(pk -> old.restrictedList.contains(pk))
                                        .collect(Collectors.toList());
                            }
                            newPK = pickStaticPowerLvlReplacement(
                                    restrictedPool,
                                    oldPK,
                                    false, // Allow same Pokemon just in case
                                    limitBST);
                        } else {
                            newPK = pickStaticPowerLvlReplacement(
                                    pokemonLeft,
                                    oldPK,
                                    true,
                                    limitBST);
                        }
                    }
                    pokemonLeft.remove(newPK);
                    setPokemonAndFormeForStaticEncounter(newStatic, newPK);
                }

                if (pokemonLeft.size() == 0) {
                    pokemonLeft.addAll(pokemonPool);
                }
                replacements.add(newStatic);
                if (changeMusicStatics.contains(old.pkmn.number)) {
                    specialMusicStaticChanges.put(old.pkmn.number, newPK.number);
                }
            }
        } else { // Completely random
            List<Pokemon> listInclFormesExclCosmetics =
                    mainPokemonListInclFormes
                            .stream()
                            .filter(pk -> !pk.actuallyCosmetic)
                            .collect(Collectors.toList());
            List<Pokemon> pokemonLeft = new ArrayList<>(!allowAltFormes ? mainPokemonList : listInclFormesExclCosmetics);
            pokemonLeft.removeAll(banned);

            List<Pokemon> pokemonPool = new ArrayList<>(pokemonLeft);

            for (StaticEncounter old : currentStaticPokemon) {
                StaticEncounter newStatic = cloneStaticEncounter(old);
                Pokemon newPK;
                if (reallySwapMegaEvos && old.canMegaEvolve()) {
                    newPK = getMegaEvoPokemon(mainPokemonList, pokemonLeft, newStatic);
                } else {
                    if (old.restrictedPool) {
                        newPK = getRestrictedPokemon(pokemonPool, pokemonLeft, old);
                    } else {
                        newPK = pokemonLeft.remove(this.random.nextInt(pokemonLeft.size()));
                    }
                }
                pokemonLeft.remove(newPK);
                setPokemonAndFormeForStaticEncounter(newStatic, newPK);
                if (pokemonLeft.size() == 0) {
                    pokemonLeft.addAll(pokemonPool);
                }
                replacements.add(newStatic);
                if (changeMusicStatics.contains(old.pkmn.number)) {
                    specialMusicStaticChanges.put(old.pkmn.number, newPK.number);
                }
            }
        }

        if (levelModifier != 0) {
            for (StaticEncounter se : replacements) {
                if (!se.isEgg) {
                    se.level = Math.min(100, (int) Math.round(se.level * (1 + levelModifier / 100.0)));
                    se.maxLevel = Math.min(100, (int) Math.round(se.maxLevel * (1 + levelModifier / 100.0)));
                    for (StaticEncounter linkedStatic : se.linkedEncounters) {
                        if (!linkedStatic.isEgg) {
                            linkedStatic.level = Math.min(100, (int) Math.round(linkedStatic.level * (1 + levelModifier / 100.0)));
                            linkedStatic.maxLevel = Math.min(100, (int) Math.round(linkedStatic.maxLevel * (1 + levelModifier / 100.0)));
                        }
                    }
                }
            }
        }

        if (specialMusicStaticChanges.size() > 0) {
            applyCorrectStaticMusic(specialMusicStaticChanges);
        }

        // Save
        this.setStaticPokemon(replacements);
    }

    private Pokemon getRestrictedPokemon(List<Pokemon> fullList, List<Pokemon> pokemonLeft, StaticEncounter old) {
        Pokemon newPK;
        List<Pokemon> restrictedPool = pokemonLeft.stream().filter(pk -> old.restrictedList.contains(pk)).collect(Collectors.toList());
        if (restrictedPool.isEmpty()) {
            restrictedPool = fullList
                            .stream()
                            .filter(pk -> old.restrictedList.contains(pk))
                            .collect(Collectors.toList());
        }
        newPK = restrictedPool.remove(this.random.nextInt(restrictedPool.size()));
        pokemonLeft.remove(newPK);
        return newPK;
    }

    @Override
    public void onlyChangeStaticLevels(Settings settings) {
        int levelModifier = settings.getStaticLevelModifier();

        List<StaticEncounter> currentStaticPokemon = this.getStaticPokemon();
        for (StaticEncounter se : currentStaticPokemon) {
            if (!se.isEgg) {
                se.level = Math.min(100, (int) Math.round(se.level * (1 + levelModifier / 100.0)));
                for (StaticEncounter linkedStatic : se.linkedEncounters) {
                    if (!linkedStatic.isEgg) {
                        linkedStatic.level = Math.min(100, (int) Math.round(linkedStatic.level * (1 + levelModifier / 100.0)));
                    }
                }
            }
            setPokemonAndFormeForStaticEncounter(se, se.pkmn);
        }
        this.setStaticPokemon(currentStaticPokemon);
    }

    private StaticEncounter cloneStaticEncounter(StaticEncounter old) {
        StaticEncounter newStatic = new StaticEncounter();
        newStatic.pkmn = old.pkmn;
        newStatic.level = old.level;
        newStatic.maxLevel = old.maxLevel;
        newStatic.heldItem = old.heldItem;
        newStatic.isEgg = old.isEgg;
        newStatic.resetMoves = true;
        for (StaticEncounter oldLinked : old.linkedEncounters) {
            StaticEncounter newLinked = new StaticEncounter();
            newLinked.pkmn = oldLinked.pkmn;
            newLinked.level = oldLinked.level;
            newLinked.maxLevel = oldLinked.maxLevel;
            newLinked.heldItem = oldLinked.heldItem;
            newLinked.isEgg = oldLinked.isEgg;
            newLinked.resetMoves = true;
            newStatic.linkedEncounters.add(newLinked);
        }
        return newStatic;
    }

    private void setPokemonAndFormeForStaticEncounter(StaticEncounter newStatic, Pokemon pk) {
        boolean checkCosmetics = true;
        Pokemon newPK = pk;
        int newForme = 0;
        if (pk.formeNumber > 0) {
            newForme = pk.formeNumber;
            newPK = pk.baseForme;
            checkCosmetics = false;
        }
        if (checkCosmetics && pk.cosmeticForms > 0) {
            newForme = pk.getCosmeticFormNumber(this.random.nextInt(pk.cosmeticForms));
        } else if (!checkCosmetics && pk.cosmeticForms > 0) {
            newForme += pk.getCosmeticFormNumber(this.random.nextInt(pk.cosmeticForms));
        }
        newStatic.pkmn = newPK;
        newStatic.forme = newForme;
        for (StaticEncounter linked : newStatic.linkedEncounters) {
            linked.pkmn = newPK;
            linked.forme = newForme;
        }
    }

    private void setFormeForStaticEncounter(StaticEncounter newStatic, Pokemon pk) {
        boolean checkCosmetics = true;
        newStatic.forme = 0;
        if (pk.formeNumber > 0) {
            newStatic.forme = pk.formeNumber;
            newStatic.pkmn = pk.baseForme;
            checkCosmetics = false;
        }
        if (checkCosmetics && newStatic.pkmn.cosmeticForms > 0) {
            newStatic.forme = newStatic.pkmn.getCosmeticFormNumber(this.random.nextInt(newStatic.pkmn.cosmeticForms));
        } else if (!checkCosmetics && pk.cosmeticForms > 0) {
            newStatic.forme += pk.getCosmeticFormNumber(this.random.nextInt(pk.cosmeticForms));
        }
    }

    private Pokemon getMegaEvoPokemon(List<Pokemon> fullList, List<Pokemon> pokemonLeft, StaticEncounter newStatic) {
        List<MegaEvolution> megaEvos = megaEvolutionsList;
        List<Pokemon> megaEvoPokemon =
                megaEvos
                        .stream()
                        .filter(mega -> mega.method == 1)
                        .map(mega -> mega.from)
                        .distinct()
                        .collect(Collectors.toList());
        Pokemon newPK;
        List<Pokemon> megaEvoPokemonLeft =
                megaEvoPokemon
                        .stream()
                        .filter(pokemonLeft::contains)
                        .collect(Collectors.toList());
        if (megaEvoPokemonLeft.isEmpty()) {
            megaEvoPokemonLeft = megaEvoPokemon
                    .stream()
                    .filter(fullList::contains)
                    .collect(Collectors.toList());
        }
        newPK = megaEvoPokemonLeft.remove(this.random.nextInt(megaEvoPokemonLeft.size()));
        pokemonLeft.remove(newPK);
        newStatic.heldItem = newPK
                .megaEvolutionsFrom
                .get(this.random.nextInt(newPK.megaEvolutionsFrom.size()))
                .argument;
        return newPK;
    }

    @Override
    public void randomizeTMMoves(Settings settings) {
        boolean noBroken = settings.isBlockBrokenTMMoves();
        boolean preserveField = settings.isKeepFieldMoveTMs();
        double goodDamagingPercentage = settings.isTmsForceGoodDamaging() ? settings.getTmsGoodDamagingPercent() / 100.0 : 0;

        // Pick some random TM moves.
        int tmCount = this.getTMCount();
        List<Move> allMoves = this.getMoves();
        List<Integer> hms = this.getHMMoves();
        List<Integer> oldTMs = this.getTMMoves();
        @SuppressWarnings("unchecked")
        List<Integer> banned = new ArrayList<Integer>(noBroken ? this.getGameBreakingMoves() : Collections.EMPTY_LIST);
        banned.addAll(getMovesBannedFromLevelup());
        banned.addAll(this.getIllegalMoves());
        // field moves?
        List<Integer> fieldMoves = this.getFieldMoves();
        int preservedFieldMoveCount = 0;

        if (preserveField) {
            List<Integer> banExistingField = new ArrayList<>(oldTMs);
            banExistingField.retainAll(fieldMoves);
            preservedFieldMoveCount = banExistingField.size();
            banned.addAll(banExistingField);
        }

        // Determine which moves are pickable
        List<Move> usableMoves = new ArrayList<>(allMoves);
        usableMoves.remove(0); // remove null entry
        Set<Move> unusableMoves = new HashSet<>();
        Set<Move> unusableDamagingMoves = new HashSet<>();

        for (Move mv : usableMoves) {
            if (GlobalConstants.bannedRandomMoves[mv.number] || GlobalConstants.zMoves.contains(mv.number) ||
                    hms.contains(mv.number) || banned.contains(mv.number)) {
                unusableMoves.add(mv);
            } else if (GlobalConstants.bannedForDamagingMove[mv.number] || !mv.isGoodDamaging(perfectAccuracy)) {
                unusableDamagingMoves.add(mv);
            }
        }

        usableMoves.removeAll(unusableMoves);
        List<Move> usableDamagingMoves = new ArrayList<>(usableMoves);
        usableDamagingMoves.removeAll(unusableDamagingMoves);

        // pick (tmCount - preservedFieldMoveCount) moves
        List<Integer> pickedMoves = new ArrayList<>();

        // Force a certain amount of good damaging moves depending on the percentage
        int goodDamagingLeft = (int)Math.round(goodDamagingPercentage * (tmCount - preservedFieldMoveCount));

        for (int i = 0; i < tmCount - preservedFieldMoveCount; i++) {
            Move chosenMove;
            if (goodDamagingLeft > 0 && usableDamagingMoves.size() > 0) {
                chosenMove = usableDamagingMoves.get(random.nextInt(usableDamagingMoves.size()));
            } else {
                chosenMove = usableMoves.get(random.nextInt(usableMoves.size()));
            }
            pickedMoves.add(chosenMove.number);
            usableMoves.remove(chosenMove);
            usableDamagingMoves.remove(chosenMove);
            goodDamagingLeft--;
        }

        // shuffle the picked moves because high goodDamagingPercentage
        // will bias them towards early numbers otherwise

        Collections.shuffle(pickedMoves, random);

        // finally, distribute them as tms
        int pickedMoveIndex = 0;
        List<Integer> newTMs = new ArrayList<>();

        for (int i = 0; i < tmCount; i++) {
            if (preserveField && fieldMoves.contains(oldTMs.get(i))) {
                newTMs.add(oldTMs.get(i));
            } else {
                newTMs.add(pickedMoves.get(pickedMoveIndex++));
            }
        }

        this.setTMMoves(newTMs);
    }

    @Override
    public void randomizeTMHMCompatibility(Settings settings) {
        boolean preferSameType = settings.getTmsHmsCompatibilityMod() == Settings.TMsHMsCompatibilityMod.RANDOM_PREFER_TYPE;
        boolean followEvolutions = settings.isTmsFollowEvolutions();

        // Get current compatibility
        // increase HM chances if required early on
        List<Integer> requiredEarlyOn = this.getEarlyRequiredHMMoves();
        Map<Pokemon, boolean[]> compat = this.getTMHMCompatibility();
        List<Integer> tmHMs = new ArrayList<>(this.getTMMoves());
        tmHMs.addAll(this.getHMMoves());

        if (followEvolutions) {
            copyUpEvolutionsHelper(pk -> randomizePokemonMoveCompatibility(
                    pk, compat.get(pk), tmHMs, requiredEarlyOn, preferSameType),
            (evFrom, evTo, toMonIsFinalEvo) ->  copyPokemonMoveCompatibilityUpEvolutions(
                    evFrom, evTo, compat.get(evFrom), compat.get(evTo), tmHMs, preferSameType
            ), null, true);
        }
        else {
            for (Map.Entry<Pokemon, boolean[]> compatEntry : compat.entrySet()) {
                randomizePokemonMoveCompatibility(compatEntry.getKey(), compatEntry.getValue(), tmHMs,
                        requiredEarlyOn, preferSameType);
            }
        }

        // Set the new compatibility
        this.setTMHMCompatibility(compat);
    }

    private void randomizePokemonMoveCompatibility(Pokemon pkmn, boolean[] moveCompatibilityFlags,
                                                   List<Integer> moveIDs, List<Integer> prioritizedMoves,
                                                   boolean preferSameType) {
        List<Move> moveData = this.getMoves();
        for (int i = 1; i <= moveIDs.size(); i++) {
            int move = moveIDs.get(i - 1);
            Move mv = moveData.get(move);
            double probability = getMoveCompatibilityProbability(
                    pkmn,
                    mv,
                    prioritizedMoves.contains(move),
                    preferSameType
            );
            moveCompatibilityFlags[i] = (this.random.nextDouble() < probability);
        }
    }

    private void copyPokemonMoveCompatibilityUpEvolutions(Pokemon evFrom, Pokemon evTo, boolean[] prevCompatibilityFlags,
                                                          boolean[] toCompatibilityFlags, List<Integer> moveIDs,
                                                          boolean preferSameType) {
        List<Move> moveData = this.getMoves();
        for (int i = 1; i <= moveIDs.size(); i++) {
            if (!prevCompatibilityFlags[i]) {
                // Slight chance to gain TM/HM compatibility for a move if not learned by an earlier evolution step
                // Without prefer same type: 25% chance
                // With prefer same type:    10% chance, 90% chance for a type new to this evolution
                int move = moveIDs.get(i - 1);
                Move mv = moveData.get(move);
                double probability = 0.25;
                if (preferSameType) {
                    probability = 0.1;
                    if (evTo.primaryType.equals(mv.type)
                            && !evTo.primaryType.equals(evFrom.primaryType) && !evTo.primaryType.equals(evFrom.secondaryType)
                            || evTo.secondaryType != null && evTo.secondaryType.equals(mv.type)
                            && !evTo.secondaryType.equals(evFrom.secondaryType) && !evTo.secondaryType.equals(evFrom.primaryType)) {
                        probability = 0.9;
                    }
                }
                toCompatibilityFlags[i] = (this.random.nextDouble() < probability);
            }
            else {
                toCompatibilityFlags[i] = prevCompatibilityFlags[i];
            }
        }
    }

    private double getMoveCompatibilityProbability(Pokemon pkmn, Move mv, boolean requiredEarlyOn,
                                                  boolean preferSameType) {
            double probability = 0.5;
            if (preferSameType) {
                if (pkmn.primaryType.equals(mv.type)
                        || (pkmn.secondaryType != null && pkmn.secondaryType.equals(mv.type))) {
                    probability = 0.9;
                } else if (mv.type != null && mv.type.equals(Type.NORMAL)) {
                    probability = 0.5;
                } else {
                    probability = 0.25;
                }
            }
            if (requiredEarlyOn) {
                probability = Math.min(1.0, probability * 1.8);
            }
            return probability;
    }

    @Override
    public void fullTMHMCompatibility() {
        Map<Pokemon, boolean[]> compat = this.getTMHMCompatibility();
        for (Map.Entry<Pokemon, boolean[]> compatEntry : compat.entrySet()) {
            boolean[] flags = compatEntry.getValue();
            for (int i = 1; i < flags.length; i++) {
                flags[i] = true;
            }
        }
        this.setTMHMCompatibility(compat);
    }

    @Override
    public void ensureTMCompatSanity() {
        // if a pokemon learns a move in its moveset
        // and there is a TM of that move, make sure
        // that TM can be learned.
        Map<Pokemon, boolean[]> compat = this.getTMHMCompatibility();
        Map<Integer, List<MoveLearnt>> movesets = this.getMovesLearnt();
        List<Integer> tmMoves = this.getTMMoves();
        for (Pokemon pkmn : compat.keySet()) {
            List<MoveLearnt> moveset = movesets.get(pkmn.number);
            boolean[] pkmnCompat = compat.get(pkmn);
            for (MoveLearnt ml : moveset) {
                if (tmMoves.contains(ml.move)) {
                    int tmIndex = tmMoves.indexOf(ml.move);
                    pkmnCompat[tmIndex + 1] = true;
                }
            }
        }
        this.setTMHMCompatibility(compat);
    }

    @Override
    public void ensureTMEvolutionSanity() {
        Map<Pokemon, boolean[]> compat = this.getTMHMCompatibility();
        // Don't do anything with the base, just copy upwards to ensure later evolutions retain learn compatibility
        copyUpEvolutionsHelper(pk -> {}, ((evFrom, evTo, toMonIsFinalEvo) -> {
            boolean[] fromCompat = compat.get(evFrom);
            boolean[] toCompat = compat.get(evTo);
            for (int i = 1; i < toCompat.length; i++) {
                toCompat[i] |= fromCompat[i];
            }
        }), null, true);
        this.setTMHMCompatibility(compat);
    }

    @Override
    public void fullHMCompatibility() {
        Map<Pokemon, boolean[]> compat = this.getTMHMCompatibility();
        int tmCount = this.getTMCount();
        for (boolean[] flags : compat.values()) {
            for (int i = tmCount + 1; i < flags.length; i++) {
                flags[i] = true;
            }
        }

        // Set the new compatibility
        this.setTMHMCompatibility(compat);
    }

    @Override
    public void copyTMCompatibilityToCosmeticFormes() {
        Map<Pokemon, boolean[]> compat = this.getTMHMCompatibility();

        for (Map.Entry<Pokemon, boolean[]> compatEntry : compat.entrySet()) {
            Pokemon pkmn = compatEntry.getKey();
            boolean[] flags = compatEntry.getValue();
            if (pkmn.actuallyCosmetic) {
                boolean[] baseFlags = compat.get(pkmn.baseForme);
                for (int i = 1; i < flags.length; i++) {
                    flags[i] = baseFlags[i];
                }
            }
        }

        this.setTMHMCompatibility(compat);
    }

    @Override
    public void randomizeMoveTutorMoves(Settings settings) {
        boolean noBroken = settings.isBlockBrokenTutorMoves();
        boolean preserveField = settings.isKeepFieldMoveTutors();
        double goodDamagingPercentage = settings.isTutorsForceGoodDamaging() ? settings.getTutorsGoodDamagingPercent() / 100.0 : 0;

        if (!this.hasMoveTutors()) {
            return;
        }

        // Pick some random Move Tutor moves, excluding TMs.
        List<Move> allMoves = this.getMoves();
        List<Integer> tms = this.getTMMoves();
        List<Integer> oldMTs = this.getMoveTutorMoves();
        int mtCount = oldMTs.size();
        List<Integer> hms = this.getHMMoves();
        @SuppressWarnings("unchecked")
        List<Integer> banned = new ArrayList<Integer>(noBroken ? this.getGameBreakingMoves() : Collections.EMPTY_LIST);
        banned.addAll(getMovesBannedFromLevelup());
        banned.addAll(this.getIllegalMoves());

        // field moves?
        List<Integer> fieldMoves = this.getFieldMoves();
        int preservedFieldMoveCount = 0;
        if (preserveField) {
            List<Integer> banExistingField = new ArrayList<>(oldMTs);
            banExistingField.retainAll(fieldMoves);
            preservedFieldMoveCount = banExistingField.size();
            banned.addAll(banExistingField);
        }

        // Determine which moves are pickable
        List<Move> usableMoves = new ArrayList<>(allMoves);
        usableMoves.remove(0); // remove null entry
        Set<Move> unusableMoves = new HashSet<>();
        Set<Move> unusableDamagingMoves = new HashSet<>();

        for (Move mv : usableMoves) {
            if (GlobalConstants.bannedRandomMoves[mv.number] || tms.contains(mv.number) || hms.contains(mv.number)
                    || banned.contains(mv.number) || GlobalConstants.zMoves.contains(mv.number)) {
                unusableMoves.add(mv);
            } else if (GlobalConstants.bannedForDamagingMove[mv.number] || !mv.isGoodDamaging(perfectAccuracy)) {
                unusableDamagingMoves.add(mv);
            }
        }

        usableMoves.removeAll(unusableMoves);
        List<Move> usableDamagingMoves = new ArrayList<>(usableMoves);
        usableDamagingMoves.removeAll(unusableDamagingMoves);

        // pick (tmCount - preservedFieldMoveCount) moves
        List<Integer> pickedMoves = new ArrayList<>();

        // Force a certain amount of good damaging moves depending on the percentage
        int goodDamagingLeft = (int)Math.round(goodDamagingPercentage * (mtCount - preservedFieldMoveCount));

        for (int i = 0; i < mtCount - preservedFieldMoveCount; i++) {
            Move chosenMove;
            if (goodDamagingLeft > 0 && usableDamagingMoves.size() > 0) {
                chosenMove = usableDamagingMoves.get(random.nextInt(usableDamagingMoves.size()));
            } else {
                chosenMove = usableMoves.get(random.nextInt(usableMoves.size()));
            }
            pickedMoves.add(chosenMove.number);
            usableMoves.remove(chosenMove);
            usableDamagingMoves.remove(chosenMove);
            goodDamagingLeft--;
        }

        // shuffle the picked moves because high goodDamagingPercentage
        // will bias them towards early numbers otherwise

        Collections.shuffle(pickedMoves, random);

        // finally, distribute them as tutors
        int pickedMoveIndex = 0;
        List<Integer> newMTs = new ArrayList<>();

        for (Integer oldMT : oldMTs) {
            if (preserveField && fieldMoves.contains(oldMT)) {
                newMTs.add(oldMT);
            } else {
                newMTs.add(pickedMoves.get(pickedMoveIndex++));
            }
        }

        this.setMoveTutorMoves(newMTs);
    }

    @Override
    public void randomizeMoveTutorCompatibility(Settings settings) {
        boolean preferSameType = settings.getMoveTutorsCompatibilityMod() == Settings.MoveTutorsCompatibilityMod.RANDOM_PREFER_TYPE;
        boolean followEvolutions = settings.isTutorFollowEvolutions();

        if (!this.hasMoveTutors()) {
            return;
        }
        // Get current compatibility
        Map<Pokemon, boolean[]> compat = this.getMoveTutorCompatibility();
        List<Integer> mts = this.getMoveTutorMoves();

        // Empty list
        List<Integer> priorityTutors = new ArrayList<Integer>();

        if (followEvolutions) {
            copyUpEvolutionsHelper(pk -> randomizePokemonMoveCompatibility(
                    pk, compat.get(pk), mts, priorityTutors, preferSameType),
                    (evFrom, evTo, toMonIsFinalEvo) ->  copyPokemonMoveCompatibilityUpEvolutions(
                            evFrom, evTo, compat.get(evFrom), compat.get(evTo), mts, preferSameType
                    ), null, true);
        }
        else {
            for (Map.Entry<Pokemon, boolean[]> compatEntry : compat.entrySet()) {
                randomizePokemonMoveCompatibility(compatEntry.getKey(), compatEntry.getValue(), mts, priorityTutors, preferSameType);
            }
        }

        // Set the new compatibility
        this.setMoveTutorCompatibility(compat);
    }

    @Override
    public void fullMoveTutorCompatibility() {
        if (!this.hasMoveTutors()) {
            return;
        }
        Map<Pokemon, boolean[]> compat = this.getMoveTutorCompatibility();
        for (Map.Entry<Pokemon, boolean[]> compatEntry : compat.entrySet()) {
            boolean[] flags = compatEntry.getValue();
            for (int i = 1; i < flags.length; i++) {
                flags[i] = true;
            }
        }
        this.setMoveTutorCompatibility(compat);
    }

    @Override
    public void ensureMoveTutorCompatSanity() {
        if (!this.hasMoveTutors()) {
            return;
        }
        // if a pokemon learns a move in its moveset
        // and there is a tutor of that move, make sure
        // that tutor can be learned.
        Map<Pokemon, boolean[]> compat = this.getMoveTutorCompatibility();
        Map<Integer, List<MoveLearnt>> movesets = this.getMovesLearnt();
        List<Integer> mtMoves = this.getMoveTutorMoves();
        for (Pokemon pkmn : compat.keySet()) {
            List<MoveLearnt> moveset = movesets.get(pkmn.number);
            boolean[] pkmnCompat = compat.get(pkmn);
            for (MoveLearnt ml : moveset) {
                if (mtMoves.contains(ml.move)) {
                    int mtIndex = mtMoves.indexOf(ml.move);
                    pkmnCompat[mtIndex + 1] = true;
                }
            }
        }
        this.setMoveTutorCompatibility(compat);
    }

    @Override
    public void ensureMoveTutorEvolutionSanity() {
        if (!this.hasMoveTutors()) {
            return;
        }
        Map<Pokemon, boolean[]> compat = this.getMoveTutorCompatibility();
        // Don't do anything with the base, just copy upwards to ensure later evolutions retain learn compatibility
        copyUpEvolutionsHelper(pk -> {}, ((evFrom, evTo, toMonIsFinalEvo) -> {
            boolean[] fromCompat = compat.get(evFrom);
            boolean[] toCompat = compat.get(evTo);
            for (int i = 1; i < toCompat.length; i++) {
                toCompat[i] |= fromCompat[i];
            }
        }), null, true);
        this.setMoveTutorCompatibility(compat);
    }

    @Override
    public void copyMoveTutorCompatibilityToCosmeticFormes() {
        Map<Pokemon, boolean[]> compat = this.getMoveTutorCompatibility();

        for (Map.Entry<Pokemon, boolean[]> compatEntry : compat.entrySet()) {
            Pokemon pkmn = compatEntry.getKey();
            boolean[] flags = compatEntry.getValue();
            if (pkmn.actuallyCosmetic) {
                boolean[] baseFlags = compat.get(pkmn.baseForme);
                for (int i = 1; i < flags.length; i++) {
                    flags[i] = baseFlags[i];
                }
            }
        }

        this.setMoveTutorCompatibility(compat);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void randomizeTrainerNames(Settings settings) {
        CustomNamesSet customNames = settings.getCustomNames();

        if (!this.canChangeTrainerText()) {
            return;
        }

        // index 0 = singles, 1 = doubles
        List<String>[] allTrainerNames = new List[] { new ArrayList<String>(), new ArrayList<String>() };
        Map<Integer, List<String>> trainerNamesByLength[] = new Map[] { new TreeMap<Integer, List<String>>(),
                new TreeMap<Integer, List<String>>() };
        
        List<String> repeatedTrainerNames = Arrays.asList(new String[] { "GRUNT", "EXECUTIVE", "SHADOW", "ADMIN", "GOON", "EMPLOYEE" });

        // Read name lists
        for (String trainername : customNames.getTrainerNames()) {
            int len = this.internalStringLength(trainername);
            if (len <= 10) {
                allTrainerNames[0].add(trainername);
                if (trainerNamesByLength[0].containsKey(len)) {
                    trainerNamesByLength[0].get(len).add(trainername);
                } else {
                    List<String> namesOfThisLength = new ArrayList<>();
                    namesOfThisLength.add(trainername);
                    trainerNamesByLength[0].put(len, namesOfThisLength);
                }
            }
        }

        for (String trainername : customNames.getDoublesTrainerNames()) {
            int len = this.internalStringLength(trainername);
            if (len <= 10) {
                allTrainerNames[1].add(trainername);
                if (trainerNamesByLength[1].containsKey(len)) {
                    trainerNamesByLength[1].get(len).add(trainername);
                } else {
                    List<String> namesOfThisLength = new ArrayList<>();
                    namesOfThisLength.add(trainername);
                    trainerNamesByLength[1].put(len, namesOfThisLength);
                }
            }
        }

        // Get the current trainer names data
        List<String> currentTrainerNames = this.getTrainerNames();
        if (currentTrainerNames.size() == 0) {
            // RBY have no trainer names
            return;
        }
        TrainerNameMode mode = this.trainerNameMode();
        int maxLength = this.maxTrainerNameLength();
        int totalMaxLength = this.maxSumOfTrainerNameLengths();

        boolean success = false;
        int tries = 0;

        // Init the translation map and new list
        Map<String, String> translation = new HashMap<>();
        List<String> newTrainerNames = new ArrayList<>();
        List<Integer> tcNameLengths = this.getTCNameLengthsByTrainer();

        // loop until we successfully pick names that fit
        // should always succeed first attempt except for gen2.
        while (!success && tries < 10000) {
            success = true;
            translation.clear();
            newTrainerNames.clear();
            int totalLength = 0;

            // Start choosing
            int tnIndex = -1;
            for (String trainerName : currentTrainerNames) {
                tnIndex++;
                if (translation.containsKey(trainerName) && !repeatedTrainerNames.contains(trainerName.toUpperCase())) {
                    // use an already picked translation
                    newTrainerNames.add(translation.get(trainerName));
                    totalLength += this.internalStringLength(translation.get(trainerName));
                } else {
                    int idx = trainerName.contains("&") ? 1 : 0;
                    List<String> pickFrom = allTrainerNames[idx];
                    int intStrLen = this.internalStringLength(trainerName);
                    if (mode == TrainerNameMode.SAME_LENGTH) {
                        pickFrom = trainerNamesByLength[idx].get(intStrLen);
                    }
                    String changeTo = trainerName;
                    int ctl = intStrLen;
                    if (pickFrom != null && pickFrom.size() > 0 && intStrLen > 0) {
                        int innerTries = 0;
                        changeTo = pickFrom.get(this.cosmeticRandom.nextInt(pickFrom.size()));
                        ctl = this.internalStringLength(changeTo);
                        while ((mode == TrainerNameMode.MAX_LENGTH && ctl > maxLength)
                                || (mode == TrainerNameMode.MAX_LENGTH_WITH_CLASS && ctl + tcNameLengths.get(tnIndex) > maxLength)) {
                            innerTries++;
                            if (innerTries == 100) {
                                changeTo = trainerName;
                                ctl = intStrLen;
                                break;
                            }
                            changeTo = pickFrom.get(this.cosmeticRandom.nextInt(pickFrom.size()));
                            ctl = this.internalStringLength(changeTo);
                        }
                    }
                    translation.put(trainerName, changeTo);
                    newTrainerNames.add(changeTo);
                    totalLength += ctl;
                }

                if (totalLength > totalMaxLength) {
                    success = false;
                    tries++;
                    break;
                }
            }
        }

        if (!success) {
            throw new RandomizationException("Could not randomize trainer names in a reasonable amount of attempts."
                    + "\nPlease add some shorter names to your custom trainer names.");
        }

        // Done choosing, save
        this.setTrainerNames(newTrainerNames);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void randomizeTrainerClassNames(Settings settings) {
        CustomNamesSet customNames = settings.getCustomNames();

        if (!this.canChangeTrainerText()) {
            return;
        }

        // index 0 = singles, index 1 = doubles
        List<String> allTrainerClasses[] = new List[] { new ArrayList<String>(), new ArrayList<String>() };
        Map<Integer, List<String>> trainerClassesByLength[] = new Map[] { new HashMap<Integer, List<String>>(),
                new HashMap<Integer, List<String>>() };

        // Read names data
        for (String trainerClassName : customNames.getTrainerClasses()) {
            allTrainerClasses[0].add(trainerClassName);
            int len = this.internalStringLength(trainerClassName);
            if (trainerClassesByLength[0].containsKey(len)) {
                trainerClassesByLength[0].get(len).add(trainerClassName);
            } else {
                List<String> namesOfThisLength = new ArrayList<>();
                namesOfThisLength.add(trainerClassName);
                trainerClassesByLength[0].put(len, namesOfThisLength);
            }
        }

        for (String trainerClassName : customNames.getDoublesTrainerClasses()) {
            allTrainerClasses[1].add(trainerClassName);
            int len = this.internalStringLength(trainerClassName);
            if (trainerClassesByLength[1].containsKey(len)) {
                trainerClassesByLength[1].get(len).add(trainerClassName);
            } else {
                List<String> namesOfThisLength = new ArrayList<>();
                namesOfThisLength.add(trainerClassName);
                trainerClassesByLength[1].put(len, namesOfThisLength);
            }
        }

        // Get the current trainer names data
        List<String> currentClassNames = this.getTrainerClassNames();
        boolean mustBeSameLength = this.fixedTrainerClassNamesLength();
        int maxLength = this.maxTrainerClassNameLength();

        // Init the translation map and new list
        Map<String, String> translation = new HashMap<>();
        List<String> newClassNames = new ArrayList<>();

        int numTrainerClasses = currentClassNames.size();
        List<Integer> doublesClasses = this.getDoublesTrainerClasses();

        // Start choosing
        for (int i = 0; i < numTrainerClasses; i++) {
            String trainerClassName = currentClassNames.get(i);
            if (translation.containsKey(trainerClassName)) {
                // use an already picked translation
                newClassNames.add(translation.get(trainerClassName));
            } else {
                int idx = doublesClasses.contains(i) ? 1 : 0;
                List<String> pickFrom = allTrainerClasses[idx];
                int intStrLen = this.internalStringLength(trainerClassName);
                if (mustBeSameLength) {
                    pickFrom = trainerClassesByLength[idx].get(intStrLen);
                }
                String changeTo = trainerClassName;
                if (pickFrom != null && pickFrom.size() > 0) {
                    changeTo = pickFrom.get(this.cosmeticRandom.nextInt(pickFrom.size()));
                    while (changeTo.length() > maxLength) {
                        changeTo = pickFrom.get(this.cosmeticRandom.nextInt(pickFrom.size()));
                    }
                }
                translation.put(trainerClassName, changeTo);
                newClassNames.add(changeTo);
            }
        }

        // Done choosing, save
        this.setTrainerClassNames(newClassNames);
    }

    @Override
    public void randomizeWildHeldItems(Settings settings) {
        boolean banBadItems = settings.isBanBadRandomWildPokemonHeldItems();

        List<Pokemon> pokemon = allPokemonInclFormesWithoutNull();
        ItemList possibleItems = banBadItems ? this.getNonBadItems() : this.getAllowedItems();
        for (Pokemon pk : pokemon) {
            if (pk.guaranteedHeldItem == -1 && pk.commonHeldItem == -1 && pk.rareHeldItem == -1
                    && pk.darkGrassHeldItem == -1) {
                // No held items at all, abort
                return;
            }
            boolean canHaveDarkGrass = pk.darkGrassHeldItem != -1;
            if (pk.guaranteedHeldItem != -1) {
                // Guaranteed held items are supported.
                if (pk.guaranteedHeldItem > 0) {
                    // Currently have a guaranteed item
                    double decision = this.random.nextDouble();
                    if (decision < 0.9) {
                        // Stay as guaranteed
                        canHaveDarkGrass = false;
                        pk.guaranteedHeldItem = possibleItems.randomItem(this.random);
                    } else {
                        // Change to 25% or 55% chance
                        pk.guaranteedHeldItem = 0;
                        pk.commonHeldItem = possibleItems.randomItem(this.random);
                        pk.rareHeldItem = possibleItems.randomItem(this.random);
                        while (pk.rareHeldItem == pk.commonHeldItem) {
                            pk.rareHeldItem = possibleItems.randomItem(this.random);
                        }
                    }
                } else {
                    // No guaranteed item atm
                    double decision = this.random.nextDouble();
                    if (decision < 0.5) {
                        // No held item at all
                        pk.commonHeldItem = 0;
                        pk.rareHeldItem = 0;
                    } else if (decision < 0.65) {
                        // Just a rare item
                        pk.commonHeldItem = 0;
                        pk.rareHeldItem = possibleItems.randomItem(this.random);
                    } else if (decision < 0.8) {
                        // Just a common item
                        pk.commonHeldItem = possibleItems.randomItem(this.random);
                        pk.rareHeldItem = 0;
                    } else if (decision < 0.95) {
                        // Both a common and rare item
                        pk.commonHeldItem = possibleItems.randomItem(this.random);
                        pk.rareHeldItem = possibleItems.randomItem(this.random);
                        while (pk.rareHeldItem == pk.commonHeldItem) {
                            pk.rareHeldItem = possibleItems.randomItem(this.random);
                        }
                    } else {
                        // Guaranteed item
                        canHaveDarkGrass = false;
                        pk.guaranteedHeldItem = possibleItems.randomItem(this.random);
                        pk.commonHeldItem = 0;
                        pk.rareHeldItem = 0;
                    }
                }
            } else {
                // Code for no guaranteed items
                double decision = this.random.nextDouble();
                if (decision < 0.5) {
                    // No held item at all
                    pk.commonHeldItem = 0;
                    pk.rareHeldItem = 0;
                } else if (decision < 0.65) {
                    // Just a rare item
                    pk.commonHeldItem = 0;
                    pk.rareHeldItem = possibleItems.randomItem(this.random);
                } else if (decision < 0.8) {
                    // Just a common item
                    pk.commonHeldItem = possibleItems.randomItem(this.random);
                    pk.rareHeldItem = 0;
                } else {
                    // Both a common and rare item
                    pk.commonHeldItem = possibleItems.randomItem(this.random);
                    pk.rareHeldItem = possibleItems.randomItem(this.random);
                    while (pk.rareHeldItem == pk.commonHeldItem) {
                        pk.rareHeldItem = possibleItems.randomItem(this.random);
                    }
                }
            }

            if (canHaveDarkGrass) {
                double dgDecision = this.random.nextDouble();
                if (dgDecision < 0.5) {
                    // Yes, dark grass item
                    pk.darkGrassHeldItem = possibleItems.randomItem(this.random);
                } else {
                    pk.darkGrassHeldItem = 0;
                }
            } else if (pk.darkGrassHeldItem != -1) {
                pk.darkGrassHeldItem = 0;
            }
        }

    }

    @Override
    public void randomizeStarterHeldItems(Settings settings) {
        boolean banBadItems = settings.isBanBadRandomStarterHeldItems();

        List<Integer> oldHeldItems = this.getStarterHeldItems();
        List<Integer> newHeldItems = new ArrayList<>();
        ItemList possibleItems = banBadItems ? this.getNonBadItems() : this.getAllowedItems();
        for (int i = 0; i < oldHeldItems.size(); i++) {
            newHeldItems.add(possibleItems.randomItem(this.random));
        }
        this.setStarterHeldItems(newHeldItems);
    }

    @Override
    public void shuffleFieldItems() {
        List<Integer> currentItems = this.getRegularFieldItems();
        List<Integer> currentTMs = this.getCurrentFieldTMs();

        Collections.shuffle(currentItems, this.random);
        Collections.shuffle(currentTMs, this.random);

        this.setRegularFieldItems(currentItems);
        this.setFieldTMs(currentTMs);
    }

    @Override
    public void randomizeFieldItems(Settings settings) {
        boolean banBadItems = settings.isBanBadRandomFieldItems();
        boolean distributeItemsControl = settings.getFieldItemsMod() == Settings.FieldItemsMod.RANDOM_EVEN;
        boolean uniqueItems = !settings.isBalanceShopPrices();

        ItemList possibleItems = banBadItems ? this.getNonBadItems().copy() : this.getAllowedItems().copy();
        List<Integer> currentItems = this.getRegularFieldItems();
        List<Integer> currentTMs = this.getCurrentFieldTMs();
        List<Integer> requiredTMs = this.getRequiredFieldTMs();
        List<Integer> uniqueNoSellItems = this.getUniqueNoSellItems();
        // System.out.println("distributeItemsControl: "+ distributeItemsControl);

        int fieldItemCount = currentItems.size();
        int fieldTMCount = currentTMs.size();
        int reqTMCount = requiredTMs.size();
        int totalTMCount = this.getTMCount();

        List<Integer> newItems = new ArrayList<>();
        List<Integer> newTMs = new ArrayList<>(requiredTMs);

        // List<Integer> chosenItems = new ArrayList<Integer>(); // collecting chosenItems for later process

        if (distributeItemsControl) {
            for (int i = 0; i < fieldItemCount; i++) {
                int chosenItem = possibleItems.randomNonTM(this.random);
                int iterNum = 0;
                while ((this.getItemPlacementHistory(chosenItem) > this.getItemPlacementAverage()) && iterNum < 100) {
                    chosenItem = possibleItems.randomNonTM(this.random);
                    iterNum +=1;
                }
                newItems.add(chosenItem);
                if (uniqueItems && uniqueNoSellItems.contains(chosenItem)) {
                    possibleItems.banSingles(chosenItem);
                } else {
                    this.setItemPlacementHistory(chosenItem);
                }
            }
        } else {
            for (int i = 0; i < fieldItemCount; i++) {
                int chosenItem = possibleItems.randomNonTM(this.random);
                newItems.add(chosenItem);
                if (uniqueItems && uniqueNoSellItems.contains(chosenItem)) {
                    possibleItems.banSingles(chosenItem);
                }
            }
        }

        for (int i = reqTMCount; i < fieldTMCount; i++) {
            while (true) {
                int tm = this.random.nextInt(totalTMCount) + 1;
                if (!newTMs.contains(tm)) {
                    newTMs.add(tm);
                    break;
                }
            }
        }


        Collections.shuffle(newItems, this.random);
        Collections.shuffle(newTMs, this.random);
        
        this.setRegularFieldItems(newItems);
        this.setFieldTMs(newTMs);
    }

    @Override
    public void randomizeIngameTrades(Settings settings) {
        boolean randomizeRequest = settings.getInGameTradesMod() == Settings.InGameTradesMod.RANDOMIZE_GIVEN_AND_REQUESTED;
        boolean randomNickname = settings.isRandomizeInGameTradesNicknames();
        boolean randomOT = settings.isRandomizeInGameTradesOTs();
        boolean randomStats = settings.isRandomizeInGameTradesIVs();
        boolean randomItem = settings.isRandomizeInGameTradesItems();
        CustomNamesSet customNames = settings.getCustomNames();

        checkPokemonRestrictions();
        // Process trainer names
        List<String> trainerNames = new ArrayList<>();
        // Check for the file
        if (randomOT) {
            int maxOT = this.maxTradeOTNameLength();
            for (String trainername : customNames.getTrainerNames()) {
                int len = this.internalStringLength(trainername);
                if (len <= maxOT && !trainerNames.contains(trainername)) {
                    trainerNames.add(trainername);
                }
            }
        }

        // Process nicknames
        List<String> nicknames = new ArrayList<>();
        // Check for the file
        if (randomNickname) {
            int maxNN = this.maxTradeNicknameLength();
            for (String nickname : customNames.getPokemonNicknames()) {
                int len = this.internalStringLength(nickname);
                if (len <= maxNN && !nicknames.contains(nickname)) {
                    nicknames.add(nickname);
                }
            }
        }

        // get old trades
        List<IngameTrade> trades = this.getIngameTrades();
        List<Pokemon> usedRequests = new ArrayList<>();
        List<Pokemon> usedGivens = new ArrayList<>();
        List<String> usedOTs = new ArrayList<>();
        List<String> usedNicknames = new ArrayList<>();
        ItemList possibleItems = this.getAllowedItems();

        int nickCount = nicknames.size();
        int trnameCount = trainerNames.size();

        for (IngameTrade trade : trades) {
            // pick new given pokemon
            Pokemon oldgiven = trade.givenPokemon;
            Pokemon given = this.randomPokemon();
            while (usedGivens.contains(given)) {
                given = this.randomPokemon();
            }
            usedGivens.add(given);
            trade.givenPokemon = given;

            // requested pokemon?
            if (oldgiven == trade.requestedPokemon) {
                // preserve trades for the same pokemon
                trade.requestedPokemon = given;
            } else if (randomizeRequest) {
                if (trade.requestedPokemon != null) {
                    Pokemon request = this.randomPokemon();
                    while (usedRequests.contains(request) || request == given) {
                        request = this.randomPokemon();
                    }
                    usedRequests.add(request);
                    trade.requestedPokemon = request;
                }
            }

            // nickname?
            if (randomNickname && nickCount > usedNicknames.size()) {
                String nickname = nicknames.get(this.random.nextInt(nickCount));
                while (usedNicknames.contains(nickname)) {
                    nickname = nicknames.get(this.random.nextInt(nickCount));
                }
                usedNicknames.add(nickname);
                trade.nickname = nickname;
            } else if (trade.nickname.equalsIgnoreCase(oldgiven.name)) {
                // change the name for sanity
                trade.nickname = trade.givenPokemon.name;
            }

            if (randomOT && trnameCount > usedOTs.size()) {
                String ot = trainerNames.get(this.random.nextInt(trnameCount));
                while (usedOTs.contains(ot)) {
                    ot = trainerNames.get(this.random.nextInt(trnameCount));
                }
                usedOTs.add(ot);
                trade.otName = ot;
                trade.otId = this.random.nextInt(65536);
            }

            if (randomStats) {
                int maxIV = this.hasDVs() ? 16 : 32;
                for (int i = 0; i < trade.ivs.length; i++) {
                    trade.ivs[i] = this.random.nextInt(maxIV);
                }
            }

            if (randomItem) {
                trade.item = possibleItems.randomItem(this.random);
            }
        }

        // things that the game doesn't support should just be ignored
        this.setIngameTrades(trades);
    }

    @Override
    public void condenseLevelEvolutions(int maxLevel, int maxIntermediateLevel) {
        List<Pokemon> allPokemon = this.getPokemon();
        // search for level evolutions
        for (Pokemon pk : allPokemon) {
            if (pk != null) {
                for (Evolution checkEvo : pk.evolutionsFrom) {
                    if (checkEvo.type.usesLevel()) {
                        // If evo is intermediate and too high, bring it down
                        // Else if it's just too high, bring it down
                        if (checkEvo.extraInfo > maxIntermediateLevel && checkEvo.to.evolutionsFrom.size() > 0) {
                            checkEvo.extraInfo = maxIntermediateLevel;
                            addEvoUpdateCondensed(easierEvolutionUpdates, checkEvo, false);
                        } else if (checkEvo.extraInfo > maxLevel) {
                            checkEvo.extraInfo = maxLevel;
                            addEvoUpdateCondensed(easierEvolutionUpdates, checkEvo, false);
                        }
                    }
                    if (checkEvo.type == EvolutionType.LEVEL_UPSIDE_DOWN) {
                        checkEvo.type = EvolutionType.LEVEL;
                        addEvoUpdateCondensed(easierEvolutionUpdates, checkEvo, false);
                    }
                }
            }
        }

    }

    @Override
    public Set<EvolutionUpdate> getImpossibleEvoUpdates() {
        return impossibleEvolutionUpdates;
    }

    @Override
    public Set<EvolutionUpdate> getEasierEvoUpdates() {
        return easierEvolutionUpdates;
    }

    @Override
    public Set<EvolutionUpdate> getTimeBasedEvoUpdates() {
        return timeBasedEvolutionUpdates;
    }

    @Override
    public void randomizeEvolutions(Settings settings) {
        boolean similarStrength = settings.isEvosSimilarStrength();
        boolean sameType = settings.isEvosSameTyping();
        boolean limitToThreeStages = settings.isEvosMaxThreeStages();
        boolean forceChange = settings.isEvosForceChange();
        boolean allowAltFormes = settings.isEvosAllowAltFormes();
        boolean banIrregularAltFormes = settings.isBanIrregularAltFormes();
        boolean abilitiesAreRandomized = settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE;

        checkPokemonRestrictions();
        List<Pokemon> pokemonPool;
        if (this.altFormesCanHaveDifferentEvolutions()) {
            pokemonPool = new ArrayList<>(mainPokemonListInclFormes);
        } else {
            pokemonPool = new ArrayList<>(mainPokemonList);
        }
        List<Pokemon> actuallyCosmeticPokemonPool = new ArrayList<>();
        int stageLimit = limitToThreeStages ? 3 : 10;

        List<Pokemon> banned = this.getBannedFormesForPlayerPokemon();
        if (!abilitiesAreRandomized) {
            List<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(getIrregularFormes());
        }

        for (int i = 0; i < pokemonPool.size(); i++) {
            Pokemon pk = pokemonPool.get(i);
            if (pk.actuallyCosmetic) {
                pokemonPool.remove(pk);
                i--;
                actuallyCosmeticPokemonPool.add(pk);
            }
        }

        // Cache old evolutions for data later
        Map<Pokemon, List<Evolution>> originalEvos = new HashMap<>();
        for (Pokemon pk : pokemonPool) {
            originalEvos.put(pk, new ArrayList<>(pk.evolutionsFrom));
        }

        Set<EvolutionPair> newEvoPairs = new HashSet<>();
        Set<EvolutionPair> oldEvoPairs = new HashSet<>();

        if (forceChange) {
            for (Pokemon pk : pokemonPool) {
                for (Evolution ev : pk.evolutionsFrom) {
                    oldEvoPairs.add(new EvolutionPair(ev.from, ev.to));
                    if (generationOfPokemon() >= 7 && ev.from.number == Species.cosmoem) { // Special case for Cosmoem to add Lunala/Solgaleo since we remove the split evo
                        int oppositeVersionLegendary = ev.to.number == Species.solgaleo ? Species.lunala : Species.solgaleo;
                        Pokemon toPkmn = findPokemonInPoolWithSpeciesID(pokemonPool, oppositeVersionLegendary);
                        if (toPkmn != null) {
                            oldEvoPairs.add(new EvolutionPair(ev.from, toPkmn));
                        }
                    }
                }
            }
        }

        List<Pokemon> replacements = new ArrayList<>();

        int loops = 0;
        while (loops < 1) {
            // Setup for this loop.
            boolean hadError = false;
            for (Pokemon pk : pokemonPool) {
                pk.evolutionsFrom.clear();
                pk.evolutionsTo.clear();
            }
            newEvoPairs.clear();

            // Shuffle pokemon list so the results aren't overly predictable.
            Collections.shuffle(pokemonPool, this.random);

            for (Pokemon fromPK : pokemonPool) {
                List<Evolution> oldEvos = originalEvos.get(fromPK);
                for (Evolution ev : oldEvos) {
                    // Pick a Pokemon as replacement
                    replacements.clear();

                    List<Pokemon> chosenList =
                            allowAltFormes ?
                            mainPokemonListInclFormes
                                    .stream()
                                    .filter(pk -> !pk.actuallyCosmetic)
                                    .collect(Collectors.toList()) :
                            mainPokemonList;
                    // Step 1: base filters
                    for (Pokemon pk : chosenList) {
                        // Prevent evolving into oneself (mandatory)
                        if (pk == fromPK) {
                            continue;
                        }

                        // Force same EXP curve (mandatory)
                        if (pk.growthCurve != fromPK.growthCurve) {
                            continue;
                        }

                        // Prevent evolving into banned Pokemon (mandatory)
                        if (banned.contains(pk)) {
                            continue;
                        }

                        EvolutionPair ep = new EvolutionPair(fromPK, pk);
                        // Prevent split evos choosing the same Pokemon
                        // (mandatory)
                        if (newEvoPairs.contains(ep)) {
                            continue;
                        }

                        // Prevent evolving into old thing if flagged
                        if (forceChange && oldEvoPairs.contains(ep)) {
                            continue;
                        }

                        // Prevent evolution that causes cycle (mandatory)
                        if (evoCycleCheck(fromPK, pk)) {
                            continue;
                        }

                        // Prevent evolution that exceeds stage limit
                        Evolution tempEvo = new Evolution(fromPK, pk, false, EvolutionType.NONE, 0);
                        fromPK.evolutionsFrom.add(tempEvo);
                        pk.evolutionsTo.add(tempEvo);
                        boolean exceededLimit = false;

                        Set<Pokemon> related = relatedPokemon(fromPK);

                        for (Pokemon pk2 : related) {
                            int numPreEvos = numPreEvolutions(pk2, stageLimit);
                            if (numPreEvos >= stageLimit) {
                                exceededLimit = true;
                                break;
                            } else if (numPreEvos == stageLimit - 1 && pk2.evolutionsFrom.size() == 0
                                    && originalEvos.get(pk2).size() > 0) {
                                exceededLimit = true;
                                break;
                            }
                        }

                        fromPK.evolutionsFrom.remove(tempEvo);
                        pk.evolutionsTo.remove(tempEvo);

                        if (exceededLimit) {
                            continue;
                        }

                        // Passes everything, add as a candidate.
                        replacements.add(pk);
                    }

                    // If we don't have any candidates after Step 1, severe
                    // failure
                    // exit out of this loop and try again from scratch
                    if (replacements.size() == 0) {
                        hadError = true;
                        break;
                    }

                    // Step 2: filter by type, if needed
                    if (replacements.size() > 1 && sameType) {
                        Set<Pokemon> includeType = new HashSet<>();
                        for (Pokemon pk : replacements) {
                            // Special case for Eevee
                            if (fromPK.number == Species.eevee) {
                                if (pk.primaryType == ev.to.primaryType
                                        || (pk.secondaryType != null) && pk.secondaryType == ev.to.primaryType) {
                                    includeType.add(pk);
                                }
                            } else if (pk.primaryType == fromPK.primaryType
                                    || (fromPK.secondaryType != null && pk.primaryType == fromPK.secondaryType)
                                    || (pk.secondaryType != null && pk.secondaryType == fromPK.primaryType)
                                    || (fromPK.secondaryType != null && pk.secondaryType != null && pk.secondaryType == fromPK.secondaryType)) {
                                includeType.add(pk);
                            }
                        }

                        if (includeType.size() != 0) {
                            replacements.retainAll(includeType);
                        }
                    }

                    if (!alreadyPicked.containsAll(replacements) && !similarStrength) {
                        replacements.removeAll(alreadyPicked);
                    }

                    // Step 3: pick - by similar strength or otherwise
                    Pokemon picked;

                    if (replacements.size() == 1) {
                        // Foregone conclusion.
                        picked = replacements.get(0);
                        alreadyPicked.add(picked);
                    } else if (similarStrength) {
                        picked = pickEvoPowerLvlReplacement(replacements, ev.to);
                        alreadyPicked.add(picked);
                    } else {
                        picked = replacements.get(this.random.nextInt(replacements.size()));
                        alreadyPicked.add(picked);
                    }

                    // Step 4: add it to the new evos pool
                    Evolution newEvo = new Evolution(fromPK, picked, ev.carryStats, ev.type, ev.extraInfo);
                    boolean checkCosmetics = true;
                    if (picked.formeNumber > 0) {
                        newEvo.forme = picked.formeNumber;
                        newEvo.formeSuffix = picked.formeSuffix;
                        checkCosmetics = false;
                    }
                    if (checkCosmetics && newEvo.to.cosmeticForms > 0) {
                        newEvo.forme = newEvo.to.getCosmeticFormNumber(this.random.nextInt(newEvo.to.cosmeticForms));
                    } else if (!checkCosmetics && picked.cosmeticForms > 0) {
                        newEvo.forme += picked.getCosmeticFormNumber(this.random.nextInt(picked.cosmeticForms));
                    }
                    if (newEvo.type == EvolutionType.LEVEL_FEMALE_ESPURR) {
                        newEvo.type = EvolutionType.LEVEL_FEMALE_ONLY;
                    }
                    fromPK.evolutionsFrom.add(newEvo);
                    picked.evolutionsTo.add(newEvo);
                    newEvoPairs.add(new EvolutionPair(fromPK, picked));
                }

                if (hadError) {
                    // No need to check the other Pokemon if we already errored
                    break;
                }
            }

            // If no error, done and return
            if (!hadError) {
                for (Pokemon pk: actuallyCosmeticPokemonPool) {
                    pk.copyBaseFormeEvolutions(pk.baseForme);
                }
                return;
            } else {
                loops++;
            }
        }

        // If we made it out of the loop, we weren't able to randomize evos.
        throw new RandomizationException("Not able to randomize evolutions in a sane amount of retries.");
    }

    @Override
    public void randomizeEvolutionsEveryLevel(Settings settings) {
        boolean sameType = settings.isEvosSameTyping();
        boolean forceChange = settings.isEvosForceChange();
        boolean allowAltFormes = settings.isEvosAllowAltFormes();
        boolean abilitiesAreRandomized = settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE;

        checkPokemonRestrictions();
        List<Pokemon> pokemonPool;
        if (this.altFormesCanHaveDifferentEvolutions()) {
            pokemonPool = new ArrayList<>(mainPokemonListInclFormes);
        } else {
            pokemonPool = new ArrayList<>(mainPokemonList);
        }
        List<Pokemon> actuallyCosmeticPokemonPool = new ArrayList<>();

        List<Pokemon> banned = this.getBannedFormesForPlayerPokemon();
        if (!abilitiesAreRandomized) {
            List<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }

        for (int i = 0; i < pokemonPool.size(); i++) {
            Pokemon pk = pokemonPool.get(i);
            if (pk.actuallyCosmetic) {
                pokemonPool.remove(pk);
                i--;
                actuallyCosmeticPokemonPool.add(pk);
            }
        }

        Set<EvolutionPair> oldEvoPairs = new HashSet<>();

        if (forceChange) {
            for (Pokemon pk : pokemonPool) {
                for (Evolution ev : pk.evolutionsFrom) {
                    oldEvoPairs.add(new EvolutionPair(ev.from, ev.to));
                    if (generationOfPokemon() >= 7 && ev.from.number == Species.cosmoem) { // Special case for Cosmoem to add Lunala/Solgaleo since we remove the split evo
                        int oppositeVersionLegendary = ev.to.number == Species.solgaleo ? Species.lunala : Species.solgaleo;
                        Pokemon toPkmn = findPokemonInPoolWithSpeciesID(pokemonPool, oppositeVersionLegendary);
                        if (toPkmn != null) {
                            oldEvoPairs.add(new EvolutionPair(ev.from, toPkmn));
                        }
                    }
                }
            }
        }

        List<Pokemon> replacements = new ArrayList<>();

        int loops = 0;
        while (loops < 1) {
            // Setup for this loop.
            boolean hadError = false;
            for (Pokemon pk : pokemonPool) {
                pk.evolutionsFrom.clear();
                pk.evolutionsTo.clear();
            }

            // Shuffle pokemon list so the results aren't overly predictable.
            Collections.shuffle(pokemonPool, this.random);

            for (Pokemon fromPK : pokemonPool) {
                // Pick a Pokemon as replacement
                replacements.clear();

                List<Pokemon> chosenList =
                        allowAltFormes ?
                                mainPokemonListInclFormes
                                        .stream()
                                        .filter(pk -> !pk.actuallyCosmetic)
                                        .collect(Collectors.toList()) :
                                mainPokemonList;
                // Step 1: base filters
                for (Pokemon pk : chosenList) {
                    // Prevent evolving into oneself (mandatory)
                    if (pk == fromPK) {
                        continue;
                    }

                    // Force same EXP curve (mandatory)
                    if (pk.growthCurve != fromPK.growthCurve) {
                        continue;
                    }

                    // Prevent evolving into banned Pokemon (mandatory)
                    if (banned.contains(pk)) {
                        continue;
                    }

                    // Prevent evolving into old thing if flagged
                    EvolutionPair ep = new EvolutionPair(fromPK, pk);
                    if (forceChange && oldEvoPairs.contains(ep)) {
                        continue;
                    }

                    // Passes everything, add as a candidate.
                    replacements.add(pk);
                }

                // If we don't have any candidates after Step 1, severe failure
                // exit out of this loop and try again from scratch
                if (replacements.size() == 0) {
                    hadError = true;
                    break;
                }

                // Step 2: filter by type, if needed
                if (replacements.size() > 1 && sameType) {
                    Set<Pokemon> includeType = new HashSet<>();
                    for (Pokemon pk : replacements) {
                        if (pk.primaryType == fromPK.primaryType
                                || (fromPK.secondaryType != null && pk.primaryType == fromPK.secondaryType)
                                || (pk.secondaryType != null && pk.secondaryType == fromPK.primaryType)
                                || (pk.secondaryType != null && pk.secondaryType == fromPK.secondaryType)) {
                            includeType.add(pk);
                        }
                    }

                    if (includeType.size() != 0) {
                        replacements.retainAll(includeType);
                    }
                }

                // Step 3: pick - by similar strength or otherwise
                Pokemon picked;

                if (replacements.size() == 1) {
                    // Foregone conclusion.
                    picked = replacements.get(0);
                } else {
                    picked = replacements.get(this.random.nextInt(replacements.size()));
                }

                // Step 4: create new level 1 evo and add it to the new evos pool
                Evolution newEvo = new Evolution(fromPK, picked, false, EvolutionType.LEVEL, 1);
                newEvo.level = 1;
                boolean checkCosmetics = true;
                if (picked.formeNumber > 0) {
                    newEvo.forme = picked.formeNumber;
                    newEvo.formeSuffix = picked.formeSuffix;
                    checkCosmetics = false;
                }
                if (checkCosmetics && newEvo.to.cosmeticForms > 0) {
                    newEvo.forme = newEvo.to.getCosmeticFormNumber(this.random.nextInt(newEvo.to.cosmeticForms));
                } else if (!checkCosmetics && picked.cosmeticForms > 0) {
                    newEvo.forme += picked.getCosmeticFormNumber(this.random.nextInt(picked.cosmeticForms));
                }
                fromPK.evolutionsFrom.add(newEvo);
                picked.evolutionsTo.add(newEvo);
            }

            // If no error, done and return
            if (!hadError) {
                for (Pokemon pk: actuallyCosmeticPokemonPool) {
                    pk.copyBaseFormeEvolutions(pk.baseForme);
                }
                return;
            } else {
                loops++;
            }
        }

        // If we made it out of the loop, we weren't able to randomize evos.
        throw new RandomizationException("Not able to randomize evolutions in a sane amount of retries.");
    }

    @Override
    public void changeCatchRates(Settings settings) {
        int minimumCatchRateLevel = settings.getMinimumCatchRateLevel();

        if (minimumCatchRateLevel == 5) {
            enableGuaranteedPokemonCatching();
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

    @Override
    public void shuffleShopItems() {
        Map<Integer, Shop> currentItems = this.getShopItems();
        if (currentItems == null) return;
        List<Integer> itemList = new ArrayList<>();
        for (Shop shop: currentItems.values()) {
            itemList.addAll(shop.items);
        }
        Collections.shuffle(itemList, this.random);

        Iterator<Integer> itemListIter = itemList.iterator();

        for (Shop shop: currentItems.values()) {
            for (int i = 0; i < shop.items.size(); i++) {
                shop.items.remove(i);
                shop.items.add(i, itemListIter.next());
            }
        }

        this.setShopItems(currentItems);
    }

    // Note: If you use this on a game where the amount of randomizable shop items is greater than the amount of
    // possible items, you will get owned by the while loop
    @Override
    public void randomizeShopItems(Settings settings) {
        boolean banBadItems = settings.isBanBadRandomShopItems();
        boolean banRegularShopItems = settings.isBanRegularShopItems();
        boolean banOPShopItems = settings.isBanOPShopItems();
        boolean balancePrices = settings.isBalanceShopPrices();
        boolean placeEvolutionItems = settings.isGuaranteeEvolutionItems();
        boolean placeXItems = settings.isGuaranteeXItems();

        if (this.getShopItems() == null) return;
        ItemList possibleItems = banBadItems ? this.getNonBadItems() : this.getAllowedItems();
        if (banRegularShopItems) {
            possibleItems.banSingles(this.getRegularShopItems().stream().mapToInt(Integer::intValue).toArray());
        }
        if (banOPShopItems) {
            possibleItems.banSingles(this.getOPShopItems().stream().mapToInt(Integer::intValue).toArray());
        }
        Map<Integer, Shop> currentItems = this.getShopItems();

        int shopItemCount = currentItems.values().stream().mapToInt(s -> s.items.size()).sum();

        List<Integer> newItems = new ArrayList<>();
        Map<Integer, Shop> newItemsMap = new TreeMap<>();
        int newItem;
        List<Integer> guaranteedItems = new ArrayList<>();
        if (placeEvolutionItems) {
            guaranteedItems.addAll(getEvolutionItems());
        }
        if (placeXItems) {
            guaranteedItems.addAll(getXItems());
        }
        if (placeEvolutionItems || placeXItems) {
            newItems.addAll(guaranteedItems);
            shopItemCount = shopItemCount - newItems.size();

            for (int i = 0; i < shopItemCount; i++) {
                while (newItems.contains(newItem = possibleItems.randomNonTM(this.random)));
                newItems.add(newItem);
            }

            // Guarantee main-game
            List<Integer> mainGameShops = new ArrayList<>();
            List<Integer> nonMainGameShops = new ArrayList<>();
            for (int i: currentItems.keySet()) {
                if (currentItems.get(i).isMainGame) {
                    mainGameShops.add(i);
                } else {
                    nonMainGameShops.add(i);
                }
            }

            // Place items in non-main-game shops; skip over guaranteed items
            Collections.shuffle(newItems, this.random);
            for (int i: nonMainGameShops) {
                int j = 0;
                List<Integer> newShopItems = new ArrayList<>();
                Shop oldShop = currentItems.get(i);
                for (Integer ignored: oldShop.items) {
                    Integer item = newItems.get(j);
                    while (guaranteedItems.contains(item)) {
                        j++;
                        item = newItems.get(j);
                    }
                    newShopItems.add(item);
                    newItems.remove(item);
                }
                Shop shop = new Shop(oldShop);
                shop.items = newShopItems;
                newItemsMap.put(i, shop);
            }

            // Place items in main-game shops
            Collections.shuffle(newItems, this.random);
            for (int i: mainGameShops) {
                List<Integer> newShopItems = new ArrayList<>();
                Shop oldShop = currentItems.get(i);
                for (Integer ignored: oldShop.items) {
                    Integer item = newItems.get(0);
                    newShopItems.add(item);
                    newItems.remove(0);
                }
                Shop shop = new Shop(oldShop);
                shop.items = newShopItems;
                newItemsMap.put(i, shop);
            }
        } else {
            for (int i = 0; i < shopItemCount; i++) {
                while (newItems.contains(newItem = possibleItems.randomNonTM(this.random)));
                newItems.add(newItem);
            }

            Iterator<Integer> newItemsIter = newItems.iterator();

            for (int i: currentItems.keySet()) {
                List<Integer> newShopItems = new ArrayList<>();
                Shop oldShop = currentItems.get(i);
                for (Integer ignored: oldShop.items) {
                    newShopItems.add(newItemsIter.next());
                }
                Shop shop = new Shop(oldShop);
                shop.items = newShopItems;
                newItemsMap.put(i, shop);
            }
        }

        this.setShopItems(newItemsMap);
        if (balancePrices) {
            this.setShopPrices();
        }
    }

    @Override
    public void randomizePickupItems(Settings settings) {
        boolean banBadItems = settings.isBanBadRandomPickupItems();

        ItemList possibleItems = banBadItems ? this.getNonBadItems() : this.getAllowedItems();
        List<PickupItem> currentItems = this.getPickupItems();
        List<PickupItem> newItems = new ArrayList<>();
        for (int i = 0; i < currentItems.size(); i++) {
            int item;
            if (this.generationOfPokemon() == 3 || this.generationOfPokemon() == 4) {
                // Allow TMs in Gen 3/4 since they aren't infinite (and you get TMs from Pickup in the vanilla game)
                item = possibleItems.randomItem(this.random);
            } else {
                item = possibleItems.randomNonTM(this.random);
            }
            PickupItem pickupItem = new PickupItem(item);
            pickupItem.probabilities = Arrays.copyOf(currentItems.get(i).probabilities, currentItems.size());
            newItems.add(pickupItem);
        }

        this.setPickupItems(newItems);
    }

    @Override
    public void minimumCatchRate(int rateNonLegendary, int rateLegendary) {
        List<Pokemon> pokes = getPokemonInclFormes();
        for (Pokemon pkmn : pokes) {
            if (pkmn == null) {
                continue;
            }
            int minCatchRate = pkmn.isLegendary() ? rateLegendary : rateNonLegendary;
            pkmn.catchRate = Math.max(pkmn.catchRate, minCatchRate);
        }

    }

    @Override
    public void standardizeEXPCurves(Settings settings) {
        Settings.ExpCurveMod mod = settings.getExpCurveMod();
        ExpCurve expCurve = settings.getSelectedEXPCurve();

        List<Pokemon> pokes = getPokemonInclFormes();
        switch (mod) {
            case LEGENDARIES:
                for (Pokemon pkmn : pokes) {
                    if (pkmn == null) {
                        continue;
                    }
                    pkmn.growthCurve = pkmn.isLegendary() ? ExpCurve.SLOW : expCurve;
                }
                break;
            case STRONG_LEGENDARIES:
                for (Pokemon pkmn : pokes) {
                    if (pkmn == null) {
                        continue;
                    }
                    pkmn.growthCurve = pkmn.isStrongLegendary() ? ExpCurve.SLOW : expCurve;
                }
                break;
            case ALL:
                for (Pokemon pkmn : pokes) {
                    if (pkmn == null) {
                        continue;
                    }
                    pkmn.growthCurve = expCurve;
                }
                break;
        }
    }

    /* Private methods/structs used internally by the above methods */

    private void updateMovePower(List<Move> moves, int moveNum, int power) {
        Move mv = moves.get(moveNum);
        if (mv.power != power) {
            mv.power = power;
            addMoveUpdate(moveNum, 0);
        }
    }

    private void updateMovePP(List<Move> moves, int moveNum, int pp) {
        Move mv = moves.get(moveNum);
        if (mv.pp != pp) {
            mv.pp = pp;
            addMoveUpdate(moveNum, 1);
        }
    }

    private void updateMoveAccuracy(List<Move> moves, int moveNum, int accuracy) {
        Move mv = moves.get(moveNum);
        if (Math.abs(mv.hitratio - accuracy) >= 1) {
            mv.hitratio = accuracy;
            addMoveUpdate(moveNum, 2);
        }
    }

    private void updateMoveType(List<Move> moves, int moveNum, Type type) {
        Move mv = moves.get(moveNum);
        if (mv.type != type) {
            mv.type = type;
            addMoveUpdate(moveNum, 3);
        }
    }

    private void updateMoveCategory(List<Move> moves, int moveNum, MoveCategory category) {
        Move mv = moves.get(moveNum);
        if (mv.category != category) {
            mv.category = category;
            addMoveUpdate(moveNum, 4);
        }
    }

    private void addMoveUpdate(int moveNum, int updateType) {
        if (!moveUpdates.containsKey(moveNum)) {
            boolean[] updateField = new boolean[5];
            updateField[updateType] = true;
            moveUpdates.put(moveNum, updateField);
        } else {
            moveUpdates.get(moveNum)[updateType] = true;
        }
    }

    protected Set<EvolutionUpdate> impossibleEvolutionUpdates = new TreeSet<>();
    protected Set<EvolutionUpdate> timeBasedEvolutionUpdates = new TreeSet<>();
    protected Set<EvolutionUpdate> easierEvolutionUpdates = new TreeSet<>();

    protected void addEvoUpdateLevel(Set<EvolutionUpdate> evolutionUpdates, Evolution evo) {
        Pokemon pkFrom = evo.from;
        Pokemon pkTo = evo.to;
        int level = evo.extraInfo;
        evolutionUpdates.add(new EvolutionUpdate(pkFrom, pkTo, EvolutionType.LEVEL, String.valueOf(level),
                false, false));
    }

    protected void addEvoUpdateStone(Set<EvolutionUpdate> evolutionUpdates, Evolution evo, String item) {
        Pokemon pkFrom = evo.from;
        Pokemon pkTo = evo.to;
        evolutionUpdates.add(new EvolutionUpdate(pkFrom, pkTo, EvolutionType.STONE, item,
                false, false));
    }

    protected void addEvoUpdateHappiness(Set<EvolutionUpdate> evolutionUpdates, Evolution evo) {
        Pokemon pkFrom = evo.from;
        Pokemon pkTo = evo.to;
        evolutionUpdates.add(new EvolutionUpdate(pkFrom, pkTo, EvolutionType.HAPPINESS, "",
                false, false));
    }

    protected void addEvoUpdateHeldItem(Set<EvolutionUpdate> evolutionUpdates, Evolution evo, String item) {
        Pokemon pkFrom = evo.from;
        Pokemon pkTo = evo.to;
        evolutionUpdates.add(new EvolutionUpdate(pkFrom, pkTo, EvolutionType.LEVEL_ITEM_DAY, item,
                false, false));
    }

    protected void addEvoUpdateParty(Set<EvolutionUpdate> evolutionUpdates, Evolution evo, String otherPk) {
        Pokemon pkFrom = evo.from;
        Pokemon pkTo = evo.to;
        evolutionUpdates.add(new EvolutionUpdate(pkFrom, pkTo, EvolutionType.LEVEL_WITH_OTHER, otherPk,
                false, false));
    }

    protected void addEvoUpdateCondensed(Set<EvolutionUpdate> evolutionUpdates, Evolution evo, boolean additional) {
        Pokemon pkFrom = evo.from;
        Pokemon pkTo = evo.to;
        int level = evo.extraInfo;
        evolutionUpdates.add(new EvolutionUpdate(pkFrom, pkTo, EvolutionType.LEVEL, String.valueOf(level),
                true, additional));
    }

    private Pokemon pickEvoPowerLvlReplacement(List<Pokemon> pokemonPool, Pokemon current) {
        // start with within 10% and add 5% either direction till we find
        // something
        int currentBST = current.bstForPowerLevels();
        int minTarget = currentBST - currentBST / 10;
        int maxTarget = currentBST + currentBST / 10;
        List<Pokemon> canPick = new ArrayList<>();
        List<Pokemon> emergencyPick = new ArrayList<>();
        int expandRounds = 0;
        while (canPick.isEmpty() || (canPick.size() < 3 && expandRounds < 3)) {
            for (Pokemon pk : pokemonPool) {
                if (pk.bstForPowerLevels() >= minTarget && pk.bstForPowerLevels() <= maxTarget && !canPick.contains(pk) && !emergencyPick.contains(pk)) {
                    if (alreadyPicked.contains(pk)) {
                        emergencyPick.add(pk);
                    } else {
                        canPick.add(pk);
                    }
                }
            }
            if (expandRounds >= 2 && canPick.isEmpty()) {
                canPick.addAll(emergencyPick);
            }
            minTarget -= currentBST / 20;
            maxTarget += currentBST / 20;
            expandRounds++;
        }
        return canPick.get(this.random.nextInt(canPick.size()));
    }

    // Note that this is slow and somewhat hacky.
    // TODO: implement for PokemonSet (hopefully in a less hacky way)
    private Pokemon findPokemonInPoolWithSpeciesID(List<Pokemon> pokemonPool, int speciesID) {
        for (int i = 0; i < pokemonPool.size(); i++) {
            if (pokemonPool.get(i).number == speciesID) {
                return pokemonPool.get(i);
            }
        }
        return null;
    }

    /**
     * Deprecated; use Pokemon.getAllRelatedPokemon() instead.
     * TODO: remove usages
     */
    private List<Pokemon> getEvolutionaryRelatives(Pokemon pk) {
        List<Pokemon> evolutionaryRelatives = new ArrayList<>();
        for (Evolution ev : pk.evolutionsFrom) {
            if (!evolutionaryRelatives.contains(ev.to)) {
                Pokemon evo = ev.to;
                evolutionaryRelatives.add(evo);
                Queue<Evolution> evolutionsList = new LinkedList<>();
                evolutionsList.addAll(evo.evolutionsFrom);
                while (evolutionsList.size() > 0) {
                    evo = evolutionsList.remove().to;
                    if (!evolutionaryRelatives.contains(evo)) {
                        evolutionaryRelatives.add(evo);
                        evolutionsList.addAll(evo.evolutionsFrom);
                    }
                }
            }
        }

        for (Evolution ev : pk.evolutionsTo) {
            if (!evolutionaryRelatives.contains(ev.from)) {
                Pokemon preEvo = ev.from;
                evolutionaryRelatives.add(preEvo);

                // At this point, preEvo is basically the "parent" of pk. Run
                // getEvolutionaryRelatives on preEvo in order to get pk's
                // "sibling" evolutions too. For example, if pk is Espeon, then
                // preEvo here will be Eevee, and this will add all the other
                // eeveelutions to the relatives list.
                List<Pokemon> relativesForPreEvo = getEvolutionaryRelatives(preEvo);
                for (Pokemon preEvoRelative : relativesForPreEvo) {
                    if (!evolutionaryRelatives.contains(preEvoRelative)) {
                        evolutionaryRelatives.add(preEvoRelative);
                    }
                }

                while (preEvo.evolutionsTo.size() > 0) {
                    preEvo = preEvo.evolutionsTo.get(0).from;
                    if (!evolutionaryRelatives.contains(preEvo)) {
                        evolutionaryRelatives.add(preEvo);

                        // Similar to above, get the "sibling" evolutions here too.
                        relativesForPreEvo = getEvolutionaryRelatives(preEvo);
                        for (Pokemon preEvoRelative : relativesForPreEvo) {
                            if (!evolutionaryRelatives.contains(preEvoRelative)) {
                                evolutionaryRelatives.add(preEvoRelative);
                            }
                        }
                    }
                }
            }
        }

        return evolutionaryRelatives;
    }

    private static class EvolutionPair {
        private Pokemon from;
        private Pokemon to;

        EvolutionPair(Pokemon from, Pokemon to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((from == null) ? 0 : from.hashCode());
            result = prime * result + ((to == null) ? 0 : to.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            EvolutionPair other = (EvolutionPair) obj;
            if (from == null) {
                if (other.from != null)
                    return false;
            } else if (!from.equals(other.from))
                return false;
            if (to == null) {
                return other.to == null;
            } else return to.equals(other.to);
        }
    }

    /**
     * Check whether adding an evolution from one Pokemon to another will cause
     * an evolution cycle.
     *
     * @param from Pokemon that is evolving
     * @param to Pokemon to evolve to
     * @return True if there is an evolution cycle, else false
     */
    private boolean evoCycleCheck(Pokemon from, Pokemon to) {
        Evolution tempEvo = new Evolution(from, to, false, EvolutionType.NONE, 0);
        from.evolutionsFrom.add(tempEvo);
        Set<Pokemon> visited = new HashSet<>();
        Set<Pokemon> recStack = new HashSet<>();
        boolean recur = isCyclic(from, visited, recStack);
        from.evolutionsFrom.remove(tempEvo);
        return recur;
    }

    private boolean isCyclic(Pokemon pk, Set<Pokemon> visited, Set<Pokemon> recStack) {
        if (!visited.contains(pk)) {
            visited.add(pk);
            recStack.add(pk);
            for (Evolution ev : pk.evolutionsFrom) {
                if (!visited.contains(ev.to) && isCyclic(ev.to, visited, recStack)) {
                    return true;
                } else if (recStack.contains(ev.to)) {
                    return true;
                }
            }
        }
        recStack.remove(pk);
        return false;
    }

    private interface BasePokemonAction {
        void applyTo(Pokemon pk);
    }

    private interface EvolvedPokemonAction {
        void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo);
    }

    private interface CosmeticFormAction {
        void applyTo(Pokemon pk, Pokemon baseForme);
    }

    /**
     * Universal implementation for things that have "copy X up evolutions"
     * support.
     *  @param bpAction
     *            Method to run on all base or no-copy Pokemon
     * @param epAction
     *            Method to run on all evolved Pokemon with a linear chain of
     * @param copySplitEvos
     *            If true, treat split evolutions the same way as base Pokemon
     */
    private void copyUpEvolutionsHelper(BasePokemonAction bpAction, EvolvedPokemonAction epAction,
                                        EvolvedPokemonAction splitAction, boolean copySplitEvos) {
        List<Pokemon> allPokes = this.getPokemonInclFormes();
        for (Pokemon pk : allPokes) {
            if (pk != null) {
                pk.temporaryFlag = false;
            }
        }

        // Get evolution data.
        Set<Pokemon> basicPokes = RomFunctions.getBasicPokemon(this);
        Set<Pokemon> splitEvos = RomFunctions.getSplitEvolutions(this);
        Set<Pokemon> middleEvos = RomFunctions.getMiddleEvolutions(this, copySplitEvos);

        for (Pokemon pk : basicPokes) {
            bpAction.applyTo(pk);
            pk.temporaryFlag = true;
        }

        if (!copySplitEvos) {
            for (Pokemon pk : splitEvos) {
                bpAction.applyTo(pk);
                pk.temporaryFlag = true;
            }
        }

        // go "up" evolutions looking for pre-evos to do first
        for (Pokemon pk : allPokes) {
            if (pk != null && !pk.temporaryFlag) {

                // Non-randomized pokes at this point must have
                // a linear chain of single evolutions down to
                // a randomized poke.
                Stack<Evolution> currentStack = new Stack<>();
                Evolution ev = pk.evolutionsTo.get(0);
                while (!ev.from.temporaryFlag) {
                    currentStack.push(ev);
                    ev = ev.from.evolutionsTo.get(0);
                }

                // Now "ev" is set to an evolution from a Pokemon that has had
                // the base action done on it to one that hasn't.
                // Do the evolution action for everything left on the stack.

                if (copySplitEvos && splitAction != null && splitEvos.contains(ev.to)) {
                    splitAction.applyTo(ev.from, ev.to, !middleEvos.contains(ev.to));
                } else {
                    epAction.applyTo(ev.from, ev.to, !middleEvos.contains(ev.to));
                }
                ev.to.temporaryFlag = true;
                while (!currentStack.isEmpty()) {
                    ev = currentStack.pop();
                    if (copySplitEvos && splitAction != null && splitEvos.contains(pk)) {
                        splitAction.applyTo(ev.from, ev.to, !middleEvos.contains(ev.to));
                    } else {
                        epAction.applyTo(ev.from, ev.to, !middleEvos.contains(ev.to));
                    }
                    ev.to.temporaryFlag = true;
                }

            }
        }
    }

    private void copyUpEvolutionsHelper(BasePokemonAction bpAction, EvolvedPokemonAction epAction) {
        copyUpEvolutionsHelper(bpAction, epAction, null, false);
    }

    private boolean checkForUnusedMove(List<Move> potentialList, List<Integer> alreadyUsed) {
        for (Move mv : potentialList) {
            if (!alreadyUsed.contains(mv.number)) {
                return true;
            }
        }
        return false;
    }

    private List<Pokemon> pokemonOfType(Type type, boolean noLegendaries) {
        List<Pokemon> typedPokes = new ArrayList<>();
        for (Pokemon pk : mainPokemonList) {
            if (pk != null && (!noLegendaries || !pk.isLegendary()) && !pk.actuallyCosmetic) {
                if (pk.primaryType == type || pk.secondaryType == type) {
                    typedPokes.add(pk);
                }
            }
        }
        return typedPokes;
    }

    private List<Pokemon> pokemonOfTypeInclFormes(Type type, boolean noLegendaries) {
        List<Pokemon> typedPokes = new ArrayList<>();
        for (Pokemon pk : mainPokemonListInclFormes) {
            if (pk != null && !pk.actuallyCosmetic && (!noLegendaries || !pk.isLegendary())) {
                if (pk.primaryType == type || pk.secondaryType == type) {
                    typedPokes.add(pk);
                }
            }
        }
        return typedPokes;
    }

    private List<Pokemon> allPokemonWithoutNull() {
        List<Pokemon> allPokes = new ArrayList<>(this.getPokemon());
        allPokes.remove(0);
        return allPokes;
    }

    private List<Pokemon> allPokemonInclFormesWithoutNull() {
        List<Pokemon> allPokes = new ArrayList<>(this.getPokemonInclFormes());
        allPokes.remove(0);
        return allPokes;
    }

    private PokemonSet pokemonInArea(EncounterSet area) {
        PokemonSet inArea = new PokemonSet();
        for (Encounter enc : area.encounters) {
            inArea.add(enc.pokemon);
        }
        return inArea;
    }

    protected Set<Pokemon> allWildPokemon(boolean useTimeOfDay) {
        Set<Pokemon> wildPokemon = new TreeSet<>();
        List<EncounterSet> areas = this.getEncounters(useTimeOfDay);
        for (EncounterSet area : areas) {
            for (Encounter enc : area.encounters) {
                wildPokemon.add(enc.pokemon);
            }
        }
        return wildPokemon;
    }

    protected Set<Pokemon> mainGameWildPokemon(boolean useTimeOfDay) {
        Set<Pokemon> wildPokemon = new TreeSet<>();
        List<EncounterSet> areas = this.getEncounters(useTimeOfDay);

        int[] postGameAreas = getPostGameEncounterAreas(useTimeOfDay);
        int[] specialCases = getPostGameEncounterSpecialCases(useTimeOfDay);

        if(postGameAreas != null) {
            Arrays.sort(postGameAreas);
        }
        if(specialCases != null) {
            Arrays.sort(specialCases);
        }
        //this might sort the originals, but there's no harm in that.

        int pgaIndex = 0;
        int scIndex = 0;
        int areaIndex = 0;
        for (EncounterSet area : areas) {
            if(postGameAreas != null && areaIndex == postGameAreas[pgaIndex]) {
                //don't add, but do advance to the next post-game area
                pgaIndex++;
                if(pgaIndex == postGameAreas.length) {
                    pgaIndex = 0;
                }
            } else if (specialCases != null && areaIndex == specialCases[scIndex]) {

                //add some - determined by gen-specific method
                handlePostGameEncounterSpecialCase(wildPokemon, area, useTimeOfDay);

                //also, advance to next special case
                scIndex++;
                if(scIndex == specialCases.length) {
                    scIndex = 0;
                }
            } else {
                //add all
                for (Encounter enc : area.encounters) {
                    wildPokemon.add(enc.pokemon);
                }
            }

            areaIndex++;
        }

        return wildPokemon;
    }

    protected abstract int[] getPostGameEncounterAreas(boolean useTimeOfDay);
    protected abstract int[] getPostGameEncounterSpecialCases(boolean useTimeOfDay);
    protected abstract void handlePostGameEncounterSpecialCase(Set<Pokemon> addTo, EncounterSet area, boolean useTimeOfDay);

    private Map<Type, Integer> typeWeightings;
    private int totalTypeWeighting;

    private Type pickType(boolean weightByFrequency, boolean noLegendaries, boolean allowAltFormes) {
        if (totalTypeWeighting == 0) {
            // Determine weightings
            for (Type t : Type.values()) {
                if (typeInGame(t)) {
                    List<Pokemon> pokemonOfType = allowAltFormes ? pokemonOfTypeInclFormes(t, noLegendaries) :
                            pokemonOfType(t, noLegendaries);
                    int pkWithTyping = pokemonOfType.size();
                    typeWeightings.put(t, pkWithTyping);
                    totalTypeWeighting += pkWithTyping;
                }
            }
        }

        if (weightByFrequency) {
            int typePick = this.random.nextInt(totalTypeWeighting);
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
            return randomType();
        }
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
        List<Pokemon> starters = this.getStarters();

        // Yellow needs its own case, unfortunately.
        if (isYellow()) {
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

    private Pokemon pickRandomEvolutionOf(Pokemon base, boolean mustEvolveItself) {
        // Used for "rival carries starter"
        // Pick a random evolution of base Pokemon, subject to
        // "must evolve itself" if appropriate.
        List<Pokemon> candidates = new ArrayList<>();
        for (Evolution ev : base.evolutionsFrom) {
            if (!mustEvolveItself || ev.to.evolutionsFrom.size() > 0) {
                candidates.add(ev.to);
            }
        }

        if (candidates.size() == 0) {
            throw new RandomizationException("Random evolution called on a Pokemon without any usable evolutions.");
        }

        return candidates.get(random.nextInt(candidates.size()));
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
                setFormeForTrainerPokemon(bestPoke,starter);
                bestPoke.resetMoves = true;
                bestPoke.abilitySlot = abilitySlot;
            }
        }

    }

    // Return the max depth of pre-evolutions a Pokemon has
    private int numPreEvolutions(Pokemon pk, int maxInterested) {
        return numPreEvolutions(pk, 0, maxInterested);
    }

    private int numPreEvolutions(Pokemon pk, int depth, int maxInterested) {
        if (pk.evolutionsTo.size() == 0) {
            return 0;
        } else {
            if (depth == maxInterested - 1) {
                return 1;
            } else {
                int maxPreEvos = 0;
                for (Evolution ev : pk.evolutionsTo) {
                    maxPreEvos = Math.max(maxPreEvos, numPreEvolutions(ev.from, depth + 1, maxInterested) + 1);
                }
                return maxPreEvos;
            }
        }
    }

    private int numEvolutions(Pokemon pk, int maxInterested) {
        return numEvolutions(pk, 0, maxInterested);
    }

    private int numEvolutions(Pokemon pk, int depth, int maxInterested) {
        if (pk.evolutionsFrom.size() == 0) {
            return 0;
        } else {
            if (depth == maxInterested - 1) {
                return 1;
            } else {
                int maxEvos = 0;
                for (Evolution ev : pk.evolutionsFrom) {
                    maxEvos = Math.max(maxEvos, numEvolutions(ev.to, depth + 1, maxInterested) + 1);
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
            if (pokemon.evolutionsFrom.size() == 0) {
                // fully evolved
                break;
            }

            // check for cyclic evolutions from what we've already seen
            boolean cyclic = false;
            for (Evolution ev : pokemon.evolutionsFrom) {
                if (seenMons.contains(ev.to)) {
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
            int evolutionIndex = (this.fullyEvolvedRandomSeed + trainerIndex) % pokemon.evolutionsFrom.size();
            pokemon = pokemon.evolutionsFrom.get(evolutionIndex).to;
            seenMons.add(pokemon);
        }

        return pokemon;
    }

    /**
     * Deprecated; use Pokemon.getAllRelatedPokemon instead.
     * @param original
     * @return
     */
    private Set<Pokemon> relatedPokemon(Pokemon original) {
        Set<Pokemon> results = new HashSet<>();
        results.add(original);
        Queue<Pokemon> toCheck = new LinkedList<>();
        toCheck.add(original);
        while (!toCheck.isEmpty()) {
            Pokemon check = toCheck.poll();
            for (Evolution ev : check.evolutionsFrom) {
                if (!results.contains(ev.to)) {
                    results.add(ev.to);
                    toCheck.add(ev.to);
                }
            }
            for (Evolution ev : check.evolutionsTo) {
                if (!results.contains(ev.from)) {
                    results.add(ev.from);
                    toCheck.add(ev.from);
                }
            }
        }
        return results;
    }

    private Map<Type, List<Pokemon>> cachedTypeLists;
    private List<Pokemon> cachedAllList;
    private List<Pokemon> bannedList = new ArrayList<>();
    private List<Pokemon> usedAsUniqueList = new ArrayList<>();


    private Pokemon pickTrainerPokeReplacement(Pokemon current, boolean usePowerLevels, Type type,
                                               boolean wonderGuardAllowed, boolean usePlacementHistory,
                                               boolean swapMegaEvos, List<Pokemon> useInsteadOfCached ) {

        List<Pokemon> cacheOrReplacement;
        if(useInsteadOfCached == null) {
            cacheOrReplacement = cachedAllList;
        } else {
            cacheOrReplacement = useInsteadOfCached;
        }

        List<Pokemon> pickFrom;
        List<Pokemon> withoutBannedPokemon;

        if (swapMegaEvos) {
            pickFrom = megaEvolutionsList
                    .stream()
                    .filter(mega -> mega.method == 1)
                    .map(mega -> mega.from)
                    .distinct()
                    .collect(Collectors.toList());

            pickFrom.removeIf(p -> !cacheOrReplacement.contains(p)); //ensure other restrictions still apply
        } else {
            pickFrom = cacheOrReplacement;
        }

        if (usePlacementHistory) {
            // "Distributed" settings
            double placementAverage = getPlacementAverage();
            pickFrom = pickFrom
                    .stream()
                    .filter(pk -> getPlacementHistory(pk) < placementAverage * 2)
                    .collect(Collectors.toList());
            if (pickFrom.isEmpty()) {
                pickFrom = cacheOrReplacement;
            }
        } else if (type != null && cachedTypeLists != null) {
            // "Type Themed" settings

            List<Pokemon> pokemonOfType;
            if(useInsteadOfCached == null) {
                if (!cachedTypeLists.containsKey(type)) {
                    pokemonOfType = new ArrayList<>();
                    for(Pokemon poke : cachedAllList) {
                        if(poke.primaryType == type || poke.secondaryType == type) {
                            pokemonOfType.add(poke);
                        }
                    }

                    if(pokemonOfType.isEmpty()) {
                        //no pokemon in the cached list of this type
                        //(this is probably because of useLocalPokemon)

                        throw new RandomizationException("No pokemon available for trainers of type " + type);

                        //I may implement handling to skip such types instead of crashing
                        //...but not yet
                    }

                    cachedTypeLists.put(type, pokemonOfType);
                } else {
                    pokemonOfType = cachedTypeLists.get(type);
                }
            } else {
                //we don't want to pull from the cached type lists when replacing the cache
                pokemonOfType = new ArrayList<>();
                for(Pokemon poke : useInsteadOfCached) {
                    if(poke.primaryType == type || poke.secondaryType == type) {
                        pokemonOfType.add(poke);
                    }
                }

                if(pokemonOfType.isEmpty()) {
                    //no pokemon in the provided list of this type
                    //default to using the cache
                    return pickTrainerPokeReplacement(current, usePowerLevels, type,
                            wonderGuardAllowed, usePlacementHistory, swapMegaEvos, null);
                }
            }
            if (swapMegaEvos) {
                pickFrom = pokemonOfType
                        .stream()
                        .filter(pickFrom::contains)
                        .collect(Collectors.toList());
                if (pickFrom.isEmpty()) {
                    pickFrom = pokemonOfType;
                }
            } else {
                pickFrom = pokemonOfType;
            }
        }

        if(pickFrom.isEmpty() && useInsteadOfCached != null) {
            //turns out we have nothing to pick from
            //default to using the cache
            return pickTrainerPokeReplacement(current, usePowerLevels, type,
                    wonderGuardAllowed, usePlacementHistory, swapMegaEvos, null);
        }

        withoutBannedPokemon = pickFrom.stream().filter(pk -> !bannedList.contains(pk)).collect(Collectors.toList());
        if (!withoutBannedPokemon.isEmpty()) {
            pickFrom = withoutBannedPokemon;
        } else if(useInsteadOfCached != null) {
            //rather than using banned pokemon from the provided list,
            //see if we can get a non-banned pokemon from the cache
            Pokemon cachePick = pickTrainerPokeReplacement(current, usePowerLevels, type,
                    wonderGuardAllowed, usePlacementHistory, swapMegaEvos, null);
            if(!bannedList.contains(cachePick)) {
                return cachePick;
            }
            //if we didn't, then we might as well pick from the provided pool
        }

        if (usePowerLevels) {
            // start with within 10% and add 5% either direction till we find
            // something
            int currentBST = current.bstForPowerLevels();
            int minTarget = currentBST - currentBST / 10;
            int maxTarget = currentBST + currentBST / 10;
            List<Pokemon> canPick = new ArrayList<>();
            int expandRounds = 0;
            while (canPick.isEmpty() || (canPick.size() < 3 && expandRounds < 2)) {
                for (Pokemon pk : pickFrom) {
                    if (pk.bstForPowerLevels() >= minTarget
                            && pk.bstForPowerLevels() <= maxTarget
                            && (wonderGuardAllowed || (pk.ability1 != Abilities.wonderGuard
                                    && pk.ability2 != Abilities.wonderGuard && pk.ability3 != Abilities.wonderGuard))) {
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

            Pokemon chosenPokemon = canPick.get(this.random.nextInt(canPick.size()));
            if (usePlacementHistory) {
                double placementAverage = getPlacementAverage();
                List<Pokemon> filteredPickList = canPick
                        .stream()
                        .filter(pk -> getPlacementHistory(pk) < placementAverage)
                        .collect(Collectors.toList());
                if (filteredPickList.isEmpty()) {
                    filteredPickList = canPick;
                }
                chosenPokemon = filteredPickList.get(this.random.nextInt(filteredPickList.size()));
            }
            return chosenPokemon;
        } else {
            if (wonderGuardAllowed) {
                return pickFrom.get(this.random.nextInt(pickFrom.size()));
            } else {
                Pokemon pk = pickFrom.get(this.random.nextInt(pickFrom.size()));
                while (pk.ability1 == Abilities.wonderGuard
                        || pk.ability2 == Abilities.wonderGuard
                        || pk.ability3 == Abilities.wonderGuard) {
                    pk = pickFrom.get(this.random.nextInt(pickFrom.size()));
                }
                return pk;
            }
        }
    }

    private Map<Integer, List<EncounterSet>> mapZonesToEncounters(List<EncounterSet> encountersForAreas) {
        Map<Integer, List<EncounterSet>> zonesToEncounters = new TreeMap<>();
        for (EncounterSet encountersInArea : encountersForAreas) {
            if (zonesToEncounters.containsKey(encountersInArea.offset)) {
                zonesToEncounters.get(encountersInArea.offset).add(encountersInArea);
            } else {
                List<EncounterSet> encountersForZone = new ArrayList<>();
                encountersForZone.add(encountersInArea);
                zonesToEncounters.put(encountersInArea.offset, encountersForZone);
            }
        }
        return zonesToEncounters;
    }

    public Pokemon pickEntirelyRandomPokemon(boolean includeFormes, boolean noLegendaries, EncounterSet area, List<Pokemon> banned) {
        Pokemon result;
        Pokemon randomNonLegendaryPokemon = includeFormes ? randomNonLegendaryPokemonInclFormes() : randomNonLegendaryPokemon();
        Pokemon randomPokemon = includeFormes ? randomPokemonInclFormes() : randomPokemon();
        result = noLegendaries ? randomNonLegendaryPokemon : randomPokemon;
        while (result.actuallyCosmetic) {
            randomNonLegendaryPokemon = includeFormes ? randomNonLegendaryPokemonInclFormes() : randomNonLegendaryPokemon();
            randomPokemon = includeFormes ? randomPokemonInclFormes() : randomPokemon();
            result = noLegendaries ? randomNonLegendaryPokemon : randomPokemon;
        }
        while (banned.contains(result) || area.bannedPokemon.contains(result)) {
            randomNonLegendaryPokemon = includeFormes ? randomNonLegendaryPokemonInclFormes() : randomNonLegendaryPokemon();
            randomPokemon = includeFormes ? randomPokemonInclFormes() : randomPokemon();
            result = noLegendaries ? randomNonLegendaryPokemon : randomPokemon;
            while (result.actuallyCosmetic) {
                randomNonLegendaryPokemon = includeFormes ? randomNonLegendaryPokemonInclFormes() : randomNonLegendaryPokemon();
                randomPokemon = includeFormes ? randomPokemonInclFormes() : randomPokemon();
                result = noLegendaries ? randomNonLegendaryPokemon : randomPokemon;
            }
        }
        return result;
    }

    private Pokemon pickStaticPowerLvlReplacement(List<Pokemon> pokemonPool, Pokemon current, boolean banSamePokemon,
                                                  boolean limitBST) {
        // start with within 10% and add 5% either direction till we find
        // something
        int currentBST = current.bstForPowerLevels();
        int minTarget = limitBST ? currentBST - currentBST / 5 : currentBST - currentBST / 10;
        int maxTarget = limitBST ? currentBST : currentBST + currentBST / 10;
        List<Pokemon> canPick = new ArrayList<>();
        int expandRounds = 0;
        while (canPick.isEmpty() || (canPick.size() < 3 && expandRounds < 3)) {
            for (Pokemon pk : pokemonPool) {
                if (pk.bstForPowerLevels() >= minTarget && pk.bstForPowerLevels() <= maxTarget
                        && (!banSamePokemon || pk != current) && !canPick.contains(pk)) {
                    canPick.add(pk);
                }
            }
            minTarget -= currentBST / 20;
            maxTarget += currentBST / 20;
            expandRounds++;
        }
        return canPick.get(this.random.nextInt(canPick.size()));
    }

    @Override
    public List<Pokemon> getAbilityDependentFormes() {
        List<Pokemon> abilityDependentFormes = new ArrayList<>();
        for (int i = 0; i < mainPokemonListInclFormes.size(); i++) {
            Pokemon pokemon = mainPokemonListInclFormes.get(i);
            if (pokemon.baseForme != null) {
                if (pokemon.baseForme.number == Species.castform) {
                    // All alternate Castform formes
                    abilityDependentFormes.add(pokemon);
                } else if (pokemon.baseForme.number == Species.darmanitan && pokemon.formeNumber == 1) {
                    // Damanitan-Z
                    abilityDependentFormes.add(pokemon);
                } else if (pokemon.baseForme.number == Species.aegislash) {
                    // Aegislash-B
                    abilityDependentFormes.add(pokemon);
                } else if (pokemon.baseForme.number == Species.wishiwashi) {
                    // Wishiwashi-S
                    abilityDependentFormes.add(pokemon);
                }
            }
        }
        return abilityDependentFormes;
    }

    @Override
    public List<Pokemon> getBannedFormesForPlayerPokemon() {
        List<Pokemon> bannedFormes = new ArrayList<>();
        for (int i = 0; i < mainPokemonListInclFormes.size(); i++) {
            Pokemon pokemon = mainPokemonListInclFormes.get(i);
            if (pokemon.baseForme != null) {
                if (pokemon.baseForme.number == Species.giratina) {
                    // Giratina-O is banned because it reverts back to Altered Forme if
                    // equipped with any item that isn't the Griseous Orb.
                    bannedFormes.add(pokemon);
                } else if (pokemon.baseForme.number == Species.shaymin) {
                    // Shaymin-S is banned because it reverts back to its original forme
                    // under a variety of circumstances, and can only be changed back
                    // with the Gracidea.
                    bannedFormes.add(pokemon);
                }
            }
        }
        return bannedFormes;
    }

    @Override
    public void randomizeTotemPokemon(Settings settings) {
        boolean randomizeTotem =
                settings.getTotemPokemonMod() == Settings.TotemPokemonMod.RANDOM ||
                        settings.getTotemPokemonMod() == Settings.TotemPokemonMod.SIMILAR_STRENGTH;
        boolean randomizeAllies =
                settings.getAllyPokemonMod() == Settings.AllyPokemonMod.RANDOM ||
                        settings.getAllyPokemonMod() == Settings.AllyPokemonMod.SIMILAR_STRENGTH;
        boolean randomizeAuras =
                settings.getAuraMod() == Settings.AuraMod.RANDOM ||
                        settings.getAuraMod() == Settings.AuraMod.SAME_STRENGTH;
        boolean similarStrengthTotem = settings.getTotemPokemonMod() == Settings.TotemPokemonMod.SIMILAR_STRENGTH;
        boolean similarStrengthAllies = settings.getAllyPokemonMod() == Settings.AllyPokemonMod.SIMILAR_STRENGTH;
        boolean similarStrengthAuras = settings.getAuraMod() == Settings.AuraMod.SAME_STRENGTH;
        boolean randomizeHeldItems = settings.isRandomizeTotemHeldItems();
        int levelModifier = settings.isTotemLevelsModified() ? settings.getTotemLevelModifier() : 0;
        boolean allowAltFormes = settings.isAllowTotemAltFormes();
        boolean banIrregularAltFormes = settings.isBanIrregularAltFormes();
        boolean abilitiesAreRandomized = settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE;

        checkPokemonRestrictions();
        List<TotemPokemon> currentTotemPokemon = this.getTotemPokemon();
        List<TotemPokemon> replacements = new ArrayList<>();
        List<Pokemon> banned = this.bannedForStaticPokemon();
        if (!abilitiesAreRandomized) {
            List<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(getIrregularFormes());
        }
        List<Pokemon> listInclFormesExclCosmetics =
                mainPokemonListInclFormes
                        .stream()
                        .filter(pk -> !pk.actuallyCosmetic)
                        .collect(Collectors.toList());
        List<Pokemon> pokemonLeft = new ArrayList<>(!allowAltFormes ? mainPokemonList : listInclFormesExclCosmetics);
        pokemonLeft.removeAll(banned);
        for (TotemPokemon old : currentTotemPokemon) {
            TotemPokemon newTotem = new TotemPokemon();
            newTotem.heldItem = old.heldItem;
            if (randomizeTotem) {
                Pokemon newPK;
                Pokemon oldPK = old.pkmn;
                if (old.forme > 0) {
                    oldPK = getAltFormeOfPokemon(oldPK, old.forme);
                }

                if (similarStrengthTotem) {
                    newPK = pickStaticPowerLvlReplacement(
                            pokemonLeft,
                            oldPK,
                            true,
                            false);
                } else {
                    newPK = pokemonLeft.remove(this.random.nextInt(pokemonLeft.size()));
                }

                pokemonLeft.remove(newPK);
                newTotem.pkmn = newPK;
                setFormeForStaticEncounter(newTotem, newPK);
                newTotem.resetMoves = true;
                newTotem.level = old.level;

                if (levelModifier != 0) {
                    newTotem.level = Math.min(100, (int) Math.round(newTotem.level * (1 + levelModifier / 100.0)));
                }
                if (pokemonLeft.size() == 0) {
                    pokemonLeft.addAll(!allowAltFormes ? mainPokemonList : listInclFormesExclCosmetics);
                    pokemonLeft.removeAll(banned);
                }
            } else {
                newTotem.pkmn = old.pkmn;
                newTotem.level = old.level;
                if (levelModifier != 0) {
                    newTotem.level = Math.min(100, (int) Math.round(newTotem.level * (1 + levelModifier / 100.0)));
                }
                setFormeForStaticEncounter(newTotem, newTotem.pkmn);
            }

            if (randomizeAllies) {
                for (Integer oldAllyIndex: old.allies.keySet()) {
                    StaticEncounter oldAlly = old.allies.get(oldAllyIndex);
                    StaticEncounter newAlly = new StaticEncounter();
                    Pokemon newAllyPK;
                    Pokemon oldAllyPK = oldAlly.pkmn;
                    if (oldAlly.forme > 0) {
                        oldAllyPK = getAltFormeOfPokemon(oldAllyPK, oldAlly.forme);
                    }
                    if (similarStrengthAllies) {
                        newAllyPK = pickStaticPowerLvlReplacement(
                                pokemonLeft,
                                oldAllyPK,
                                true,
                                false);
                    } else {
                        newAllyPK = pokemonLeft.remove(this.random.nextInt(pokemonLeft.size()));
                    }

                    pokemonLeft.remove(newAllyPK);
                    newAlly.pkmn = newAllyPK;
                    setFormeForStaticEncounter(newAlly, newAllyPK);
                    newAlly.resetMoves = true;
                    newAlly.level = oldAlly.level;
                    if (levelModifier != 0) {
                        newAlly.level = Math.min(100, (int) Math.round(newAlly.level * (1 + levelModifier / 100.0)));
                    }

                    newTotem.allies.put(oldAllyIndex,newAlly);
                    if (pokemonLeft.size() == 0) {
                        pokemonLeft.addAll(!allowAltFormes ? mainPokemonList : listInclFormesExclCosmetics);
                        pokemonLeft.removeAll(banned);
                    }
                }
            } else {
                newTotem.allies = old.allies;
                for (StaticEncounter ally: newTotem.allies.values()) {
                    if (levelModifier != 0) {
                        ally.level = Math.min(100, (int) Math.round(ally.level * (1 + levelModifier / 100.0)));
                        setFormeForStaticEncounter(ally, ally.pkmn);
                    }
                }
            }

            if (randomizeAuras) {
                if (similarStrengthAuras) {
                    newTotem.aura = Aura.randomAuraSimilarStrength(this.random, old.aura);
                } else {
                    newTotem.aura = Aura.randomAura(this.random);
                }
            } else {
                newTotem.aura = old.aura;
            }

            if (randomizeHeldItems) {
                if (old.heldItem != 0) {
                    List<Integer> consumableList = getAllConsumableHeldItems();
                    newTotem.heldItem = consumableList.get(this.random.nextInt(consumableList.size()));
                }
            }

            replacements.add(newTotem);
        }

        // Save
        this.setTotemPokemon(replacements);
    }

    /* Helper methods used by subclasses and/or this class */

    void checkPokemonRestrictions() {
        if (!restrictionsSet) {
            setPokemonPool(null);
        }
    }

    protected void applyCamelCaseNames() {
        List<Pokemon> pokes = getPokemon();
        for (Pokemon pkmn : pokes) {
            if (pkmn == null) {
                continue;
            }
            pkmn.name = RomFunctions.camelCase(pkmn.name);
        }

    }

    private void setPlacementHistory(Pokemon newPK) {
        Integer history = getPlacementHistory(newPK);
        placementHistory.put(newPK, history + 1);
    }

    private int getPlacementHistory(Pokemon newPK) {
        return placementHistory.getOrDefault(newPK, 0);
    }

    private double getPlacementAverage() {
        return placementHistory.values().stream().mapToInt(e -> e).average().orElse(0);
    }


    private List<Pokemon> getBelowAveragePlacements() {
        // This method will return a PK if the number of times a pokemon has been
        // placed is less than average of all placed pokemon's appearances
        // E.g., Charmander's been placed once, but the average for all pokemon is 2.2
        // So add to list and return 

        List<Pokemon> toPlacePK = new ArrayList<>();
        List<Pokemon> placedPK = new ArrayList<>(placementHistory.keySet());
        List<Pokemon> allPK = cachedAllList;
        int placedPKNum = 0;
        for (Pokemon p : placedPK) {
            placedPKNum += placementHistory.get(p);
        }
        float placedAverage = Math.round((float)placedPKNum / (float)placedPK.size());



        if (placedAverage != placedAverage) { // this is checking for NaN, should only happen on first call
            placedAverage = 1;
        }

        // now we've got placement average, iterate all pokemon and see if they qualify to be placed

        for (Pokemon newPK : allPK) {
            if (placedPK.contains(newPK)) { // if it's in the list of previously placed, then check its viability 
                if (placementHistory.get(newPK) <= placedAverage) {
                    toPlacePK.add(newPK);
                }
            }
            else {
                toPlacePK.add(newPK); // if not placed at all, automatically flag true for placing

            }
        }

        return toPlacePK;

    }

    @Override
    public void renderPlacementHistory() {
        List<Pokemon> placedPK = new ArrayList<>(placementHistory.keySet());
        for (Pokemon p : placedPK) {
            System.out.println(p.name+": "+ placementHistory.get(p));
        }
    }

    ///// Item functions
    private void setItemPlacementHistory(int newItem) {
        Integer history = getItemPlacementHistory(newItem);
        // System.out.println("Current history: " + newPK.name + " : " + history);
        itemPlacementHistory.put(newItem, history + 1);
    }

    private int getItemPlacementHistory(int newItem) {
        List<Integer> placedItem = new ArrayList<>(itemPlacementHistory.keySet());
        if (placedItem.contains(newItem)) {
            return itemPlacementHistory.get(newItem);
        }
        else {
            return 0;
        }
    }

    private float getItemPlacementAverage() {
        // This method will return an integer of average for itemPlacementHistory
        // placed is less than average of all placed pokemon's appearances
        // E.g., Charmander's been placed once, but the average for all pokemon is 2.2
        // So add to list and return 

        List<Integer> placedPK = new ArrayList<>(itemPlacementHistory.keySet());
        int placedPKNum = 0;
        for (Integer p : placedPK) {
            placedPKNum += itemPlacementHistory.get(p);
        }
        return (float)placedPKNum / (float)placedPK.size();
    }

    private void reportItemHistory() {
        String[] itemNames = this.getItemNames();
        List<Integer> placedItem = new ArrayList<>(itemPlacementHistory.keySet());
        for (Integer p : placedItem) {
            System.out.println(itemNames[p]+": "+ itemPlacementHistory.get(p));
        }
    }

    protected void log(String log) {
        if (logStream != null) {
            logStream.println(log);
        }
    }

    protected void logBlankLine() {
        if (logStream != null) {
            logStream.println();
        }
    }

    /* Default Implementations */
    /* Used when a subclass doesn't override */
    /*
     * The implication here is that these WILL be overridden by at least one
     * subclass.
     */
    @Override
    public boolean typeInGame(Type type) {
        return !type.isHackOnly && !(type == Type.FAIRY && generationOfPokemon() < 6);
    }

    /**
     * Generates a list of all types in this game.
     * @return A List of every Type in this game.
     */
    private List<Type> typesInGame() {
        List<Type> types = new ArrayList<>();
        for(Type type : Type.values()) {
            if(this.typeInGame(type)) {
                types.add(type);
            }
        }
        return types;
    }

    @Override
    public String abilityName(int number) {
        return "";
    }

    @Override
    public List<Integer> getUselessAbilities() {
        return new ArrayList<>();
    }

    @Override
    public int getAbilityForTrainerPokemon(TrainerPokemon tp) {
        return 0;
    }

    @Override
    public boolean hasTimeBasedEncounters() {
        // DEFAULT: no
        return false;
    }

    @Override
    public PokemonSet bannedForWildEncounters() {
        return new PokemonSet();
    }

    @Override
    public List<Integer> getMovesBannedFromLevelup() {
        return new ArrayList<>();
    }

    @Override
    public List<Pokemon> bannedForStaticPokemon() {
        return new ArrayList<>();
    }

    @Override
    public boolean forceSwapStaticMegaEvos() {
        return false;
    }

    @Override
    public int maxTrainerNameLength() {
        // default: no real limit
        return Integer.MAX_VALUE;
    }

    @Override
    public int maxSumOfTrainerNameLengths() {
        // default: no real limit
        return Integer.MAX_VALUE;
    }

    @Override
    public int maxTrainerClassNameLength() {
        // default: no real limit
        return Integer.MAX_VALUE;
    }

    @Override
    public int maxTradeNicknameLength() {
        return 10;
    }

    @Override
    public int maxTradeOTNameLength() {
        return 7;
    }

    @Override
    public boolean altFormesCanHaveDifferentEvolutions() {
        return false;
    }

    @Override
    public List<Integer> getGameBreakingMoves() {
        // Sonicboom & Dragon Rage
        return Arrays.asList(49, 82);
    }

    @Override
    public List<Integer> getIllegalMoves() {
        return new ArrayList<>();
    }

    @Override
    public boolean isYellow() {
        return false;
    }

    @Override
    public void writeCheckValueToROM(int value) {
        // do nothing
    }

    @Override
    public int miscTweaksAvailable() {
        // default: none
        return 0;
    }

    @Override
    public void applyMiscTweaks(Settings settings) {
        int selectedMiscTweaks = settings.getCurrentMiscTweaks();

        int codeTweaksAvailable = miscTweaksAvailable();
        List<MiscTweak> tweaksToApply = new ArrayList<>();

        for (MiscTweak mt : MiscTweak.allTweaks) {
            if ((codeTweaksAvailable & mt.getValue()) > 0 && (selectedMiscTweaks & mt.getValue()) > 0) {
                tweaksToApply.add(mt);
            }
        }

        // Sort so priority is respected in tweak ordering.
        Collections.sort(tweaksToApply);

        // Now apply in order.
        for (MiscTweak mt : tweaksToApply) {
            applyMiscTweak(mt);
        }
    }

    @Override
    public void applyMiscTweak(MiscTweak tweak) {
        // default: do nothing
    }

    @Override
    public List<Integer> getXItems() {
        return GlobalConstants.xItems;
    }

    @Override
    public List<Integer> getSensibleHeldItemsFor(TrainerPokemon tp, boolean consumableOnly, List<Move> moves, int[] pokeMoves) {
        return Arrays.asList(0);
    }

    @Override
    public List<Integer> getAllConsumableHeldItems() {
        return Arrays.asList(0);
    }

    @Override
    public List<Integer> getAllHeldItems() {
        return Arrays.asList(0);
    }

    @Override
    public List<Pokemon> getBannedFormesForTrainerPokemon() {
        return new ArrayList<>();
    }

    @Override
    public List<PickupItem> getPickupItems() {
        return new ArrayList<>();
    }

    @Override
    public void setPickupItems(List<PickupItem> pickupItems) {
        // do nothing
    }
}
