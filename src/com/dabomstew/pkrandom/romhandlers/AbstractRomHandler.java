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

import com.dabomstew.pkrandom.*;
import com.dabomstew.pkrandom.constants.*;
import com.dabomstew.pkrandom.exceptions.RandomizationException;
import com.dabomstew.pkrandom.exceptions.RandomizerIOException;
import com.dabomstew.pkrandom.graphics.PaletteHandler;
import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.pokemon.CopyUpEvolutionsHelper.BasicPokemonAction;
import com.dabomstew.pkrandom.pokemon.CopyUpEvolutionsHelper.EvolvedPokemonAction;
import com.dabomstew.pkrandom.romhandlers.romentries.RomEntry;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractRomHandler implements RomHandler {

    protected final Random random;
    protected PrintStream logStream;

    private boolean restrictionsSet;
    protected PokemonSet<Pokemon> restrictedPokemon;
    protected PokemonSet<Pokemon> restrictedPokemonInclAltFormes;
    private PokemonSet<Pokemon> nonlegendaryPokemon = new PokemonSet<>();
    private PokemonSet<Pokemon> legendaryPokemon = new PokemonSet<>();
    private PokemonSet<Pokemon> ultraBeasts = new PokemonSet<>();
    private PokemonSet<Pokemon> nonlegendaryPokemonInclFormes = new PokemonSet<>();
    private PokemonSet<Pokemon> legendaryPokemonInclFormes = new PokemonSet<>();
    private PokemonSet<Pokemon> nonlegendaryAltFormes = new PokemonSet<>();
    private PokemonSet<Pokemon> legendaryAltFormes = new PokemonSet<>();
    
	private CopyUpEvolutionsHelper<Pokemon> copyUpEvolutionsHelper = new CopyUpEvolutionsHelper<>(this::getPokemonSet);

    private List<MegaEvolution> megaEvolutionsList;
    private PokemonSet<Pokemon> altFormes;

    private List<Pokemon> pickedStarters;
    private final Random cosmeticRandom;
    private List<Pokemon> alreadyPicked = new ArrayList<>();
    private Map<Pokemon, Integer> placementHistory = new HashMap<>();
    private Map<Integer, Integer> itemPlacementHistory = new HashMap<>();
    private int fullyEvolvedRandomSeed;

    protected boolean isORAS = false;
    protected boolean isSM = false;

    protected int perfectAccuracy = 100;

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
        altFormes = getAltFormes();
        restrictedPokemonInclAltFormes = new PokemonSet<>();
        if (restrictions != null) {
            megaEvolutionsList = new ArrayList<>();
            List<Pokemon> allPokemon = this.getPokemon();

            if (restrictions.allow_gen1) {
                restrictedPokemonInclAltFormes.addAll(getPokemonSet().filterFromBaseNumberRange(Species.bulbasaur, Species.mew));
            }

            if (restrictions.allow_gen2 && allPokemon.size() > Gen2Constants.pokemonCount) {
                restrictedPokemonInclAltFormes.addAll(getPokemonSet().filterFromBaseNumberRange(Species.chikorita, Species.celebi));
            }

            if (restrictions.allow_gen3 && allPokemon.size() > Gen3Constants.pokemonCount) {
                restrictedPokemonInclAltFormes.addAll(getPokemonSet().filterFromBaseNumberRange(Species.treecko, Species.deoxys));
            }

            if (restrictions.allow_gen4 && allPokemon.size() > Gen4Constants.pokemonCount) {
                restrictedPokemonInclAltFormes.addAll(getPokemonSet().filterFromBaseNumberRange(Species.turtwig, Species.arceus));
            }

            if (restrictions.allow_gen5 && allPokemon.size() > Gen5Constants.pokemonCount) {
                restrictedPokemonInclAltFormes.addAll(getPokemonSet().filterFromBaseNumberRange(Species.victini, Species.genesect));
            }

            if (restrictions.allow_gen6 && allPokemon.size() > Gen6Constants.pokemonCount) {
                restrictedPokemonInclAltFormes.addAll(getPokemonSet().filterFromBaseNumberRange(Species.chespin, Species.volcanion));
            }

            int maxGen7SpeciesID = isSM ? Species.marshadow : Species.zeraora;
            if (restrictions.allow_gen7 && allPokemon.size() > maxGen7SpeciesID) {
                restrictedPokemonInclAltFormes.addAll(getPokemonSet().filterFromBaseNumberRange(Species.rowlet, maxGen7SpeciesID));
            }

            // If the user specified it, add all the evolutionary relatives for everything in the mainPokemonList
            if (restrictions.allow_evolutionary_relatives) {
                restrictedPokemonInclAltFormes.addEvolutionaryRelatives();
            }

            // Populate megaEvolutionsList with all of the mega evolutions that exist in the pool
            List<MegaEvolution> allMegaEvolutions = this.getMegaEvolutions();
            for (MegaEvolution megaEvo : allMegaEvolutions) {
                if (restrictedPokemonInclAltFormes.contains(megaEvo.to)) {
                    megaEvolutionsList.add(megaEvo);
                }
            }
        } else {
            restrictedPokemonInclAltFormes.addAll(getPokemonInclFormes());
            megaEvolutionsList = this.getMegaEvolutions();
        }

        restrictedPokemon = restrictedPokemonInclAltFormes.filter(pk -> !altFormes.contains(pk));

        for (Pokemon p : restrictedPokemon) {
            if (p.isLegendary()) {
                legendaryPokemon.add(p);
            } else if (p.isUltraBeast()) {
                ultraBeasts.add(p);
            } else {
                nonlegendaryPokemon.add(p);
            }
        }
        for (Pokemon p : restrictedPokemonInclAltFormes) {
            if (p.isLegendary()) {
                legendaryPokemonInclFormes.add(p);
            } else if (!ultraBeasts.contains(p)) {
                nonlegendaryPokemonInclFormes.add(p);
            }
        }
        for (Pokemon f : altFormes) {
            if (f.isLegendary()) {
                legendaryAltFormes.add(f);
            } else {
                nonlegendaryAltFormes.add(f);
            }
        }
    }

    @Override
    public void shufflePokemonStats(Settings settings) {
        boolean evolutionSanity = settings.isBaseStatsFollowEvolutions();
        boolean megaEvolutionSanity = settings.isBaseStatsFollowMegaEvolutions();

		copyUpEvolutionsHelper.apply(evolutionSanity, false,
				pk -> pk.shuffleStats(random),
				(evFrom, evTo, toMonIsFinalEvo) -> evTo.copyShuffledStatsUpEvolution(evFrom));

		getPokemonSetInclFormes().filterCosmetic()
				.forEach(pk -> pk.copyBaseFormeBaseStats(pk.getBaseForme()));

		if (megaEvolutionSanity) {
			for (MegaEvolution megaEvo : getMegaEvolutions()) {
				if (megaEvo.from.getMegaEvolutionsFrom().size() > 1)
					continue;
				megaEvo.to.copyShuffledStatsUpEvolution(megaEvo.from);
			}
		}
    }

    @Override
    public void randomizePokemonStats(Settings settings) {
        boolean evolutionSanity = settings.isBaseStatsFollowEvolutions();
        boolean megaEvolutionSanity = settings.isBaseStatsFollowMegaEvolutions();
        boolean assignEvoStatsRandomly = settings.isAssignEvoStatsRandomly();

		BasicPokemonAction<Pokemon> bpAction = pk -> pk.randomizeStatsWithinBST(random);
		EvolvedPokemonAction<Pokemon> randomEpAction = (evFrom, evTo, toMonIsFinalEvo) -> evTo
				.assignNewStatsForEvolution(evFrom, random);
		EvolvedPokemonAction<Pokemon> copyEpAction = (evFrom, evTo, toMonIsFinalEvo) -> evTo
				.copyRandomizedStatsUpEvolution(evFrom);
				
		copyUpEvolutionsHelper.apply(evolutionSanity, true, bpAction,
				assignEvoStatsRandomly ? randomEpAction : copyEpAction, randomEpAction, bpAction);

		getPokemonSetInclFormes().filterCosmetic()
				.forEach(pk -> pk.copyBaseFormeBaseStats(pk.getBaseForme()));

        if (megaEvolutionSanity) {
			for (MegaEvolution megaEvo : getMegaEvolutions()) {
				if (megaEvo.from.getMegaEvolutionsFrom().size() > 1 || assignEvoStatsRandomly) {
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
                        pokes.get(i).setHp(changedStats.values[statNum]);
                        statNum++;
                    }
                    if ((changedStats.stat & Stat.ATK.val) != 0) {
                        pokes.get(i).setAttack(changedStats.values[statNum]);
                        statNum++;
                    }
                    if ((changedStats.stat & Stat.DEF.val) != 0) {
                        pokes.get(i).setDefense(changedStats.values[statNum]);
                        statNum++;
                    }
                    if ((changedStats.stat & Stat.SPATK.val) != 0) {
                        if (generationOfPokemon() != 1) {
                            pokes.get(i).setSpatk(changedStats.values[statNum]);
                        }
                        statNum++;
                    }
                    if ((changedStats.stat & Stat.SPDEF.val) != 0) {
                        if (generationOfPokemon() != 1) {
                            pokes.get(i).setSpdef(changedStats.values[statNum]);
                        }
                        statNum++;
                    }
                    if ((changedStats.stat & Stat.SPEED.val) != 0) {
                        pokes.get(i).setSpeed(changedStats.values[statNum]);
                        statNum++;
                    }
                    if ((changedStats.stat & Stat.SPECIAL.val) != 0) {
                        pokes.get(i).setSpecial(changedStats.values[statNum]);
                    }
                }
            }
        }
    }

    @Override
	public PokemonSet<Pokemon> getPokemonSet() {
	    return new PokemonSet<>(getPokemon()); // TODO: unmodifiable?
	}

	@Override
	public PokemonSet<Pokemon> getPokemonSetInclFormes() {
	    return new PokemonSet<>(getPokemonInclFormes()); // TODO: unmodifiable?
	}

    @Override
    public PokemonSet<Pokemon> getRestrictedPokemon() {
        checkPokemonRestrictions();
        return restrictedPokemon;
    }

    @Override
    public PokemonSet<Pokemon> getRestrictedPokemonInclAltFormes() {
        checkPokemonRestrictions();
        return restrictedPokemonInclAltFormes;
    }

	private PokemonSet<Pokemon> getRestrictedPokemon(boolean noLegendaries, boolean allowAltFormes, boolean allowCosmeticFormes) {
	    PokemonSet<Pokemon> allowedPokes = new PokemonSet<>();
	    if (allowAltFormes) {
	        allowedPokes.addAll(noLegendaries ? nonlegendaryPokemonInclFormes : restrictedPokemonInclAltFormes);
	        if (!allowCosmeticFormes) {
	            allowedPokes.removeIf(Pokemon::isActuallyCosmetic);
	        }
	    } else {
	        allowedPokes.addAll(noLegendaries ? getNonlegendaryPokemon() : restrictedPokemon);
	    }
	    // TODO: should make unmodifiable (?)
	    return allowedPokes;
	}

	public Pokemon randomPokemon() {
        checkPokemonRestrictions();
        return restrictedPokemon.getRandom(random);
    }

    @Override
    public Pokemon randomPokemonInclFormes() {
        checkPokemonRestrictions();
        return restrictedPokemonInclAltFormes.getRandom(random);
    }

    // TODO: protection level (?)
    private PokemonSet<Pokemon> getNonlegendaryPokemon() {
        return nonlegendaryPokemon;
    }

    @Override
    public Pokemon randomNonLegendaryPokemon() {
        checkPokemonRestrictions();
        return getNonlegendaryPokemon().getRandom(random);
    }

    private Pokemon randomNonLegendaryPokemonInclFormes() {
        checkPokemonRestrictions();
        return nonlegendaryPokemonInclFormes.getRandom(random);
    }

    @Override
    public Pokemon randomLegendaryPokemon() {
        checkPokemonRestrictions();
        return legendaryPokemon.getRandom(random);
    }

    /**
     * Returns a random {@link Pokemon} which can evolve 2+ times, and which doesn't evolve from any
     * other Pokemon (e.g. Caterpie). <br>
     * If the Pokemon has branching evolutions, only one of these branches need to evolve 2+ times
     * (e.g. if Silcoon didn't evolve, but Cascoon did, Wurmple would still be a possible return).
     * @param allowAltFormes Allow it to return alternate formes?
     */
    @Override
    public Pokemon random2EvosPokemon(boolean allowAltFormes) {
        PokemonSet<Pokemon> twoEvoPokes = allowAltFormes ? getPokemonSetInclFormes() : getPokemonSet();
        twoEvoPokes = twoEvoPokes.filterBasic().filter(pk -> {
            // Potential candidate
            if (!pk.isActuallyCosmetic() && pk.getEvolutionsFrom().size() > 0) {
                // If any of the targets here evolve, the original
                // Pokemon has 2+ stages.
                for (Evolution ev : pk.getEvolutionsFrom()) {
                    if (ev.to.getEvolutionsFrom().size() > 0) {
                        return true;
                    }
                }
            }
            return false;
        });
        return twoEvoPokes.getRandom(random);
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

        PokemonSet<Pokemon> allPokes = getPokemonSetInclFormes();
		copyUpEvolutionsHelper.apply(evolutionSanity, false, pk -> {
			// Step 1: Basic or Excluded From Copying Pokemon
			// A Basic/EFC pokemon has a 35% chance of a second type if
			// it has an evolution that copies type/stats, a 50% chance
			// otherwise
			pk.setPrimaryType(randomType());
			pk.setSecondaryType(null);
			if (pk.getEvolutionsFrom().size() == 1 && pk.getEvolutionsFrom().get(0).carryStats) {
                assignRandomSecondaryType(pk, 0.35, dualTypeOnly);
			} else {
                assignRandomSecondaryType(pk, 0.5, dualTypeOnly);
			}
		}, (evFrom, evTo, toMonIsFinalEvo) -> {
			evTo.setPrimaryType(evFrom.getPrimaryType());
			evTo.setSecondaryType(evFrom.getSecondaryType());

			if (evTo.getSecondaryType() == null) {
				double chance = toMonIsFinalEvo ? 0.25 : 0.15;
                assignRandomSecondaryType(evTo, chance, dualTypeOnly);
			}
		}, null, pk -> {
			pk.setPrimaryType(randomType());
			pk.setSecondaryType(null);
            assignRandomSecondaryType(pk, 0.5, dualTypeOnly);
		});

        for (Pokemon pk : allPokes) {
            if (pk != null && pk.isActuallyCosmetic()) {
                pk.setPrimaryType(pk.getBaseForme().getPrimaryType());
                pk.setSecondaryType(pk.getBaseForme().getSecondaryType());
            }
        }

        if (megaEvolutionSanity) {
            List<MegaEvolution> allMegaEvos = getMegaEvolutions();
            for (MegaEvolution megaEvo: allMegaEvos) {
                if (megaEvo.from.getMegaEvolutionsFrom().size() > 1) continue;
                megaEvo.to.setPrimaryType(megaEvo.from.getPrimaryType());
                megaEvo.to.setSecondaryType(megaEvo.from.getSecondaryType());

                if (megaEvo.to.getSecondaryType() == null) {
                    if (this.random.nextDouble() < 0.25) {
                        megaEvo.to.setSecondaryType(randomType());
                        while (megaEvo.to.getSecondaryType() == megaEvo.to.getPrimaryType()) {
                            megaEvo.to.setSecondaryType(randomType());
                        }
                    }
                }
            }
        }
    }

    private void assignRandomSecondaryType(Pokemon pk, double chance, boolean dualTypeOnly) {
        if (random.nextDouble() < chance || dualTypeOnly) {
            pk.setSecondaryType(randomType());
            while (pk.getSecondaryType() == pk.getPrimaryType()) {
                pk.setSecondaryType(randomType());
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
        if (abilitiesPerPokemon() == 0) {
            return;
        }

        final boolean hasDWAbilities = (abilitiesPerPokemon() == 3);

        final List<Integer> bannedAbilities = getUselessAbilities();

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

        final int maxAbility = highestAbilityIndex();

        // copy abilities straight up evolution lines
        // still keep WG as an exception, though
		copyUpEvolutionsHelper.apply(evolutionSanity, false, pk -> {
			if (pk.getAbility1() != Abilities.wonderGuard && pk.getAbility2() != Abilities.wonderGuard
					&& pk.getAbility3() != Abilities.wonderGuard) {
				// Pick first ability
				pk.setAbility1(pickRandomAbility(maxAbility, bannedAbilities, weighDuplicatesTogether));

				// Second ability?
				if (ensureTwoAbilities || random.nextDouble() < 0.5) {
					// Yes, second ability
					pk.setAbility2(pickRandomAbility(maxAbility, bannedAbilities, weighDuplicatesTogether,
                            pk.getAbility1()));
				} else {
					// Nope
					pk.setAbility2(0);
                }

				// Third ability?
				if (hasDWAbilities) {
					pk.setAbility3(pickRandomAbility(maxAbility, bannedAbilities, weighDuplicatesTogether,
                            pk.getAbility1(), pk.getAbility2()));
				}
			}
		}, (evFrom, evTo, toMonIsFinalEvo) -> {
			if (evTo.getAbility1() != Abilities.wonderGuard && evTo.getAbility2() != Abilities.wonderGuard
					&& evTo.getAbility3() != Abilities.wonderGuard) {
				evTo.setAbility1(evFrom.getAbility1());
                evTo.setAbility2(evFrom.getAbility2());
                evTo.setAbility3(evFrom.getAbility3());
			}
		});


		getPokemonSetInclFormes().filterCosmetic()
				.forEach(pk -> pk.copyBaseFormeAbilities(pk.getBaseForme()));

		if (megaEvolutionSanity) {
			for (MegaEvolution megaEvo : getMegaEvolutions()) {
				if (megaEvo.from.getMegaEvolutionsFrom().size() > 1)
					continue;
				megaEvo.to.setAbility1(megaEvo.from.getAbility1());
				megaEvo.to.setAbility2(megaEvo.from.getAbility2());
				megaEvo.to.setAbility3(megaEvo.from.getAbility3());
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

    @Override
    public void randomizeEncounters(Settings settings) {
        Settings.WildPokemonMod mode = settings.getWildPokemonMod();
        boolean useTimeOfDay = settings.isUseTimeBasedEncounters();
        boolean catchEmAll = settings.isCatchEmAllEncounters();
        boolean typeThemed = settings.isTypeThemeEncounterAreas();
        boolean usePowerLevels = settings.isSimilarStrengthEncounters();
        boolean noLegendaries = settings.isBlockWildLegendaries();
        boolean balanceShakingGrass = settings.isBalanceShakingGrass();
        int levelModifier = settings.isWildLevelsModified() ? settings.getWildLevelModifier() : 0;
        boolean allowAltFormes = settings.isAllowWildAltFormes();
        boolean banIrregularAltFormes = settings.isBanIrregularAltFormes();
        boolean abilitiesAreRandomized = settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE;

        randomizeEncounters(mode, useTimeOfDay, catchEmAll, typeThemed, usePowerLevels, noLegendaries,
                balanceShakingGrass, levelModifier, allowAltFormes, banIrregularAltFormes, abilitiesAreRandomized);
    }

    public void randomizeEncounters(Settings.WildPokemonMod mode, boolean useTimeOfDay, boolean catchEmAll,
                                 boolean typeThemed, boolean usePowerLevels, boolean noLegendaries,
                                 boolean balanceShakingGrass, int levelModifier, boolean allowAltFormes,
                                 boolean banIrregularAltFormes, boolean abilitiesAreRandomized) {
        // - prep settings
        // - get encounters
        // - setup banned + allowed
        // TODO: - do something special in ORAS
        // - randomize inner
        // - apply level modifier
        // - set encounters

        // TODO: formes need more fiddling with, to fulfil the test cases
        //  (and maybe make it clever, so e.g. Wormadam and Deoxys aren't more common replacements)

        List<EncounterArea> encounterAreas = this.getEncounters(useTimeOfDay);
        checkPokemonRestrictions();
        PokemonSet<Pokemon> banned = this.getBannedForWildEncounters(banIrregularAltFormes, abilitiesAreRandomized);
        PokemonSet<Pokemon> allowed = setupAllowedPokemon(noLegendaries, allowAltFormes, false, banned);
        switch (mode) {
            case RANDOM -> randomEncountersInner(encounterAreas, banned, allowed,
                    catchEmAll, typeThemed, usePowerLevels, balanceShakingGrass);
            case AREA_MAPPING -> area1to1EncountersInner(encounterAreas, banned, allowed,
                    catchEmAll, typeThemed, usePowerLevels, balanceShakingGrass);
            case GLOBAL_MAPPING -> game1to1EncountersInner(encounterAreas, banned, allowed,
                    usePowerLevels);
            default -> {
            }
        }
        applyLevelModifier(levelModifier, encounterAreas);
        setEncounters(useTimeOfDay, encounterAreas);
    }

    private void randomEncountersInner(List<EncounterArea> currentEncounterAreas,
                                       PokemonSet<Pokemon> banned, PokemonSet<Pokemon> allowed,
                                       boolean catchEmAll, boolean typeThemed, boolean usePowerLevels,
                                       boolean balanceShakingGrass) {
        PokemonSet<Pokemon> remaining = new PokemonSet<>(allowed);
        Map<Type, PokemonSet<Pokemon>> pokemonByType = new EnumMap<>(Type.class);
        Map<Type, PokemonSet<Pokemon>> remainingPokemonByType = new EnumMap<>(Type.class);
        if (typeThemed) {
            for (Type t : Type.values()) {
                pokemonByType.put(t, allowed.filterByType(t));
            }
            for (Type t : Type.values()) {
                remainingPokemonByType.put(t, new PokemonSet<>(pokemonByType.get(t)));
            }
        }

        // Shuffling the EncounterAreas leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<EncounterArea> scrambledEncounterAreas = new ArrayList<>(currentEncounterAreas);
        Collections.shuffle(scrambledEncounterAreas, this.random);

        for (EncounterArea area : scrambledEncounterAreas) {
            Type areaType = randomType();
            PokemonSet<Pokemon> allowedForArea;
            if (catchEmAll && !remaining.isEmpty()) {
                do {
                    areaType = randomType();
                    allowedForArea = typeThemed ? remainingPokemonByType.get(areaType) : remaining;
                } while (allowedForArea.isEmpty());
            } else {
                allowedForArea = typeThemed ? pokemonByType.get(areaType) : allowed;
            }

            for (Encounter enc : area) {
                Pokemon current = enc.getPokemon();

                Pokemon replacement;
                // In Catch 'Em All mode, don't randomize encounters for Pokemon that are banned for
                // wild encounters. Otherwise, it may be impossible to obtain this Pokemon unless it
                // randomly appears as a static or unless it becomes a random evolution.
                if (catchEmAll && banned.contains(current)) {
                    replacement = current;
                } else if (usePowerLevels) {
                    replacement = balanceShakingGrass ?
                            pickWildPowerLvlReplacement(allowedForArea, current, false,
                                    null, (enc.getLevel() + enc.getMaxLevel()) / 2) :
                            pickWildPowerLvlReplacement(allowedForArea, current, false, null,
                                    100);
                } else {
                    replacement = allowedForArea.getRandom(random);
                }


                enc.setPokemon(replacement);
                setFormeForEncounter(enc, replacement);

                if (catchEmAll) {
                    remaining.remove(replacement);
                    if (typeThemed) {
                        remainingPokemonByType.get(replacement.getPrimaryType()).remove(replacement);
                        if (replacement.getSecondaryType() != null) {
                            remainingPokemonByType.get(replacement.getSecondaryType()).remove(replacement);
                        }
                    }
                    if (allowedForArea.isEmpty()) {
                        allowedForArea = typeThemed ? pokemonByType.get(areaType) : allowed;
                    }
                }
            }
        }
    }

    private void area1to1EncountersInner(List<EncounterArea> currentEncounterAreas,
                                         PokemonSet<Pokemon> banned, PokemonSet<Pokemon> allowed,
                                         boolean catchEmAll, boolean typeThemed, boolean usePowerLevels,
                                         boolean balanceShakingGrass) {
        PokemonSet<Pokemon> remaining = new PokemonSet<>(allowed);
        Map<Type, PokemonSet<Pokemon>> pokemonByType = new EnumMap<>(Type.class);
        Map<Type, PokemonSet<Pokemon>> remainingPokemonByType = new EnumMap<>(Type.class);
        if (typeThemed) {
            for (Type t : Type.values()) {
                pokemonByType.put(t, allowed.filterByType(t));
            }
            for (Type t : Type.values()) {
                remainingPokemonByType.put(t, new PokemonSet<>(pokemonByType.get(t)));
            }
        }

        // Shuffling the EncounterAreas leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<EncounterArea> scrambledEncounterAreas = new ArrayList<>(currentEncounterAreas);
        Collections.shuffle(scrambledEncounterAreas, this.random);

        for (EncounterArea area : scrambledEncounterAreas) {
            Type areaType = randomType();
            PokemonSet<Pokemon> allowedForArea;
            if (catchEmAll && !remaining.isEmpty()) {
                do {
                    areaType = randomType();
                    allowedForArea = typeThemed ? remainingPokemonByType.get(areaType) : remaining;
                } while (allowedForArea.isEmpty());
            } else {
                allowedForArea = typeThemed ? pokemonByType.get(areaType) : allowed;
            }

            Map<Pokemon, Pokemon> areaMap = new TreeMap<>();

            for (Encounter enc : area) {
                Pokemon current = enc.getPokemon();
                if (areaMap.containsKey(current)) {
                    continue;
                }

                Pokemon replacement;
                // In Catch 'Em All mode, don't randomize encounters for Pokemon that are banned for
                // wild encounters. Otherwise, it may be impossible to obtain this Pokemon unless it
                // randomly appears as a static or unless it becomes a random evolution.
                if (catchEmAll && banned.contains(current)) {
                    replacement = current;
                } else {
                    do {
                        if (usePowerLevels) {
                            replacement = balanceShakingGrass ?
                                    pickWildPowerLvlReplacement(allowedForArea, current, false,
                                            null, (enc.getLevel() + enc.getMaxLevel()) / 2) :
                                    pickWildPowerLvlReplacement(allowedForArea, current, false, null,
                                            100);
                        } else {
                            replacement = allowedForArea.getRandom(random);
                        }
                    } while (areaMap.containsValue(replacement) && areaMap.size() < allowedForArea.size());
                }
                areaMap.put(current, replacement);

                if (catchEmAll) {
                    remaining.remove(replacement);
                    if (typeThemed) {
                        remainingPokemonByType.get(replacement.getPrimaryType()).remove(replacement);
                        if (replacement.getSecondaryType() != null) {
                            remainingPokemonByType.get(replacement.getSecondaryType()).remove(replacement);
                        }
                    }
                    if (allowedForArea.isEmpty()) {
                        allowedForArea = typeThemed ? pokemonByType.get(areaType) : allowed;
                    }
                }
            }

            for (Encounter enc : area) {
                Pokemon encPk = enc.getPokemon();
                enc.setPokemon(areaMap.get(encPk));
                setFormeForEncounter(enc, encPk);
            }
        }
    }

    private void game1to1EncountersInner(List<EncounterArea> encounterAreas,
                                         PokemonSet<Pokemon> banned, PokemonSet<Pokemon> allowed,
                                         boolean usePowerLevels) {
        Map<Pokemon, Pokemon> translateMap = new TreeMap<>();
        PokemonSet<Pokemon> remaining = new PokemonSet<>(allowed);

        // Shuffle so the order of the areas/encounters doesn't matter when picking.
        // Though pokes with many encounters will still get a priority when it comes
        // to picking similar-strength, since they are more likely to be handled early.
        List<Encounter> shuffled = new ArrayList<>();
        encounterAreas.forEach(shuffled::addAll);
        Collections.shuffle(shuffled, random);
        for (Encounter enc : shuffled) {
            Pokemon current = enc.getPokemon();

            Pokemon replacement;
            if (translateMap.containsKey(current)) {
                replacement = translateMap.get(current);
            } else {
                replacement = usePowerLevels ?
                        pickWildPowerLvlReplacement(remaining, current, true, null, 100) :
                        remaining.getRandom(random);
                // In case it runs out of unique Pokémon, picks something already mapped to.
                // Shouldn't happen unless restrictions are really harsh, normally [#allowed Pokémon] > [#Pokémon which appear in the wild]
                if (replacement == null) {
                    replacement = allowed.getRandom(random);
                } else {
                    remaining.remove(replacement);
                }
                translateMap.put(current, replacement);
            }
            enc.setPokemon(replacement);
            setFormeForEncounter(enc, replacement);
        }
    }

    private PokemonSet<Pokemon> getBannedForWildEncounters(boolean banIrregularAltFormes,
                                                           boolean abilitiesAreRandomized) {
        PokemonSet<Pokemon> banned = new PokemonSet<>();
        banned.addAll(getBannedForWildEncounters());
        banned.addAll(getBannedFormesForPlayerPokemon());
        if (!abilitiesAreRandomized) {
            PokemonSet<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(getIrregularFormes());
        }
        return banned;
    }

    @Override
    public PokemonSet<Pokemon> getBannedForWildEncounters() {
        return new PokemonSet<>();
    }

    /**
     * Returns a new, modifiable {@link PokemonSet} with the given boolean properties, 
     * and the banned {@link Pokemon} exluded.
     * @param noLegendaries Exclude legendary Pokemon?
     * @param allowAltFormes Include alternate formes?
     * @param allowCosmeticFormes If allowAltFormes == true, include cosmetic alternate formes?
     * @param banned PokemonSet of Pokemon to exclude.
     */
    private PokemonSet<Pokemon> setupAllowedPokemon(boolean noLegendaries, boolean allowAltFormes,
                                                    boolean allowCosmeticFormes, PokemonSet<Pokemon> banned) {
    	PokemonSet<Pokemon> allowedPokemon = new PokemonSet<>();
    	allowedPokemon.addAll(getRestrictedPokemon(noLegendaries, allowAltFormes, allowCosmeticFormes));
        allowedPokemon.removeAll(banned);
        return allowedPokemon;
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

    private void setEvoChainAsIllegal(Pokemon newPK, PokemonSet<Pokemon> illegalList, boolean willForceEvolve) {
        // set pre-evos as illegal
        setIllegalPreEvos(newPK, illegalList);

        // if the placed Pokemon will be forced fully evolved, set its evolutions as illegal
        if (willForceEvolve) {
            setIllegalEvos(newPK, illegalList);
        }
    }

    private void setIllegalPreEvos(Pokemon pk, PokemonSet<Pokemon> illegalList) {
        for (Evolution evo: pk.getEvolutionsTo()) {
            pk = evo.from;
            illegalList.add(pk);
            setIllegalPreEvos(pk, illegalList);
        }
    }

    private void setIllegalEvos(Pokemon pk, PokemonSet<Pokemon> illegalList) {
        for (Evolution evo: pk.getEvolutionsFrom()) {
            pk = evo.to;
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
            for (Evolution evo: pk.getEvolutionsFrom()) {
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
        if (pk.getFormeNumber() > 0) {
            tp.forme = pk.getFormeNumber();
            tp.formeSuffix = pk.getFormeSuffix();
            tp.pokemon = pk.getBaseForme();
            checkCosmetics = false;
        }
        if (checkCosmetics && tp.pokemon.getCosmeticForms() > 0) {
            tp.forme = tp.pokemon.getCosmeticFormNumber(this.random.nextInt(tp.pokemon.getCosmeticForms()));
        } else if (!checkCosmetics && pk.getCosmeticForms() > 0) {
            tp.forme += pk.getCosmeticFormNumber(this.random.nextInt(pk.getCosmeticForms()));
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
    public boolean canAddPokemonToBossTrainers() {
        return true;
    }

    @Override
    public boolean canAddPokemonToImportantTrainers() {
        return true;
    }

    @Override
    public boolean canAddPokemonToRegularTrainers() {
        return true;
    }

    @Override
    public void randomizeTrainerPokes(Settings settings) {
        boolean usePowerLevels = settings.isTrainersUsePokemonOfSimilarStrength();
        boolean weightByFrequency = settings.isTrainersMatchTypingDistribution();
        boolean noLegendaries = settings.isTrainersBlockLegendaries();
        boolean noEarlyWonderGuard = settings.isTrainersBlockEarlyWonderGuard();
        int levelModifier = settings.isTrainersLevelModified() ? settings.getTrainersLevelModifier() : 0;
        boolean isTypeThemed = settings.getTrainersMod() == Settings.TrainersMod.TYPE_THEMED;
        boolean isTypeThemedEliteFourGymOnly = settings.getTrainersMod() == Settings.TrainersMod.TYPE_THEMED_ELITE4_GYMS;
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
        cachedReplacements = new TreeMap<>();
        cachedAll = getRestrictedPokemon(noLegendaries, includeFormes, false);

        PokemonSet<Pokemon> banned = this.getBannedFormesForTrainerPokemon();
        if (!abilitiesAreRandomized) {
            PokemonSet<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(getIrregularFormes());
        }
        cachedAll.removeAll(banned);

        List<Trainer> currentTrainers = this.getTrainers();

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
        Collections.shuffle(scrambledTrainers, this.random);

        // Elite Four Unique Pokemon related
        boolean eliteFourUniquePokemon = eliteFourUniquePokemonNumber > 0;
        PokemonSet<Pokemon> illegalIfEvolved = new PokemonSet<>();
        PokemonSet<Pokemon> bannedFromUnique = new PokemonSet<>();
        boolean illegalEvoChains = false;
        List<Integer> eliteFourIndices = getEliteFourTrainers(forceChallengeMode);
        if (eliteFourUniquePokemon) {
            // Sort Elite Four Trainers to the start of the list
            scrambledTrainers.sort((t1, t2) ->
                    Boolean.compare(eliteFourIndices.contains(currentTrainers.indexOf(t2)+1),eliteFourIndices.contains(currentTrainers.indexOf(t1)+1)));
            illegalEvoChains = forceFullyEvolved;
            if (rivalCarriesStarter) {
                List<Pokemon> starterList = getStarters().subList(0,3);
                for (Pokemon starter: starterList) {
                    // If rival/friend carries starter, the starters cannot be set as unique
                    bannedFromUnique.add(starter);
                    setEvoChainAsIllegal(starter, bannedFromUnique, true);

                    // If the final boss is a rival/friend, the fully evolved starters will be unique
                    if (hasRivalFinalBattle()) {
                        cachedAll.removeAll(getFinalEvos(starter));
                        if (illegalEvoChains) {
                            illegalIfEvolved.add(starter);
                            setEvoChainAsIllegal(starter, illegalIfEvolved, true);
                        }
                    }
                }
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

            PokemonSet<Pokemon> evolvesIntoTheWrongType = new PokemonSet<>();
            if (typeForTrainer != null) {
                PokemonSet<Pokemon> pokemonOfType = getRestrictedPokemon(noLegendaries, includeFormes, false)
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

            for (TrainerPokemon tp : trainerPokemonList) {
                boolean swapThisMegaEvo = swapMegaEvos && tp.canMegaEvolve();
                boolean wgAllowed = (!noEarlyWonderGuard) || tp.level >= 20;
                boolean eliteFourSetUniquePokemon =
                        eliteFourTrackPokemon && eliteFourUniquePokemonNumber > trainerPokemonList.indexOf(tp);
                boolean willForceEvolve = forceFullyEvolved && tp.level >= forceFullyEvolvedLevel;

                Pokemon oldPK = tp.pokemon;
                if (tp.forme > 0) {
                    oldPK = getAltFormeOfPokemon(oldPK, tp.forme);
                }

                banned = new PokemonSet<>(usedAsUnique);
                if (illegalEvoChains && willForceEvolve) {
                    banned.addAll(illegalIfEvolved);
                }
                if (eliteFourSetUniquePokemon) {
                    banned.addAll(bannedFromUnique);
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
                        for (Pokemon actualPK: actualPKList) {
                            usedAsUnique.add(actualPK);
                            if (illegalEvoChains) {
                                setEvoChainAsIllegal(actualPK, illegalIfEvolved, willForceEvolve);
                            }
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
                                    .get(this.random.nextInt(newPK.getMegaEvolutionsFrom().size()))
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
        this.setTrainers(currentTrainers);
    }

    @Override
    public boolean supportsTrainerHeldItems() {
        return true; // because it's likely just Gen I which doesn't
        // (Gen II doesn't either atm, but that's just because item indexes are missing)
    }

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
                        RomFunctions.getMovesAtLevel(getAltFormeOfPokemon(
                                        highestLevelPoke.pokemon, highestLevelPoke.forme).getNumber(),
                                movesets,
                                highestLevelPoke.level) :
                        highestLevelPoke.moves;
                randomizeHeldItem(highestLevelPoke, settings, moves, moveset);
            } else {
                for (TrainerPokemon tp : t.pokemon) {
                    int[] moveset = tp.resetMoves ?
                            RomFunctions.getMovesAtLevel(getAltFormeOfPokemon(
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
        this.setTrainers(currentTrainers);
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
        this.setTrainers(currentTrainers);
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
        this.setTrainers(currentTrainers);
    }

    @Override
    public void onlyChangeTrainerLevels(Settings settings) {
        int levelModifier = settings.getTrainersLevelModifier();

        List<Trainer> currentTrainers = this.getTrainers();
        for (Trainer t: currentTrainers) {
            applyLevelModifierToTrainerPokemon(t, levelModifier);
        }
        this.setTrainers(currentTrainers);
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
        this.setTrainers(currentTrainers);
    }

    @Override
    public void setDoubleBattleMode() {
        for (Trainer tr : getTrainers()) {
            if (!(tr.multiBattleStatus == Trainer.MultiBattleStatus.ALWAYS || tr.shouldNotGetBuffs())) {
                if (tr.pokemon.size() == 1) {
                    tr.pokemon.add(tr.pokemon.get(0).copy());
                }
                tr.forcedDoubleBattle = true;
            }
        }
        this.setTrainers(getTrainers());
        // TODO: line above might have to be outside this method, otherwise subclasses calling
        //  super.setDoubleBattleMode() will only get saved if they don't depend on a setTrainers() after.
        //  Though "setTrainers" is bad any ways; "saveTrainers" is preferred.
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
        List<Move> moveSelectionPoolAtLevel = allLevelUpMoves.get(getAltFormeOfPokemon(tp.pokemon, tp.forme).getNumber())
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
            while (!preEvo.getEvolutionsTo().isEmpty()) {
                preEvo = preEvo.getEvolutionsTo().get(0).from;
                moveSelectionPoolAtLevel.addAll(allLevelUpMoves.get(preEvo.getNumber())
                        .stream()
                        .filter(ml -> ml.level <= tp.level)
                        .filter(ml -> this.random.nextDouble() < preEvoMoveProbability)
                        .map(ml -> moves.get(ml.move))
                        .distinct().toList());
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
            while (!firstEvo.getEvolutionsTo().isEmpty()) {
                firstEvo = firstEvo.getEvolutionsTo().get(0).from;
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
                        .filter(mv -> mv.type == pk.getPrimaryType() && mv.category != MoveCategory.STATUS)
                        .collect(Collectors.toList());
                Collections.shuffle(stabMoves, this.random);

                for (int i = 0; i < stabMoveBias * stabMoves.size(); i++) {
                    int j = i % stabMoves.size();
                    movesAtLevel.add(stabMoves.get(j));
                }

                if (pk.getSecondaryType() != null) {
                    stabMoves = new ArrayList<>(movesAtLevel)
                            .stream()
                            .filter(mv -> mv.type == pk.getSecondaryType() && mv.category != MoveCategory.STATUS)
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
                        pk.getPrimaryType(),
                        pk.getSecondaryType(),
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
                        pk.getPrimaryType(),
                        pk.getSecondaryType());

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

                double atkSpatkRatio = (double) pk.getAttack() / (double) pk.getSpatk();
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
                        .filter(mv -> mv.category == MoveCategory.PHYSICAL).toList();
                List<Move> specialMoves = new ArrayList<>(movesAtLevel)
                        .stream()
                        .filter(mv -> mv.category == MoveCategory.SPECIAL).toList();

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
                                .distinct().toList();

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
        setTrainers(trainers);
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
                                mv.power * mv.hitCount > mv2.power * mv2.hitCount).toList();
//                for (Move obsoleted: obsoleteThis) {
//                    System.out.println(obsoleted.name + " obsoleted by " + mv.name);
//                }
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
                                        otherMv.statusType == StatusType.NONE)).toList()) {
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
//                for (Move obsoleted : obsoleteThis) {
//                    System.out.println(obsoleted.name + " obsoleted by " + mv.name);
//                }
                obsoletedMoves.addAll(obsoleteThis);
            }
        }

        return obsoletedMoves.stream().distinct().collect(Collectors.toList());
    }

    public int getRandomAbilitySlot(Pokemon pokemon) {
        if (abilitiesPerPokemon() == 0) {
            return 0;
        }
        List<Integer> abilitiesList = Arrays.asList(pokemon.getAbility1(), pokemon.getAbility2(), pokemon.getAbility3());
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
        if (originalAbilitySlot == 2 && pokemon.getAbility2() == 0) {
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
            Pokemon pkmn = findPokemonInPoolWithSpeciesID(restrictedPokemonInclAltFormes, pkmnNum);
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

            if (pkmn.isActuallyCosmetic()) {
                for (int i = 0; i < moves.size(); i++) {
                    moves.get(i).move = movesets.get(pkmn.getBaseForme().getNumber()).get(i).move;
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
                    if ((pkmn.getPrimaryType() == Type.NORMAL && pkmn.getSecondaryType() != null) ||
                            (pkmn.getSecondaryType() == Type.NORMAL)) {

                        Type otherType = pkmn.getPrimaryType() == Type.NORMAL ? pkmn.getSecondaryType() : pkmn.getPrimaryType();

                        // Normal/OTHER: 10% normal, 30% other, 60% random
                        if (picked < 0.1) {
                            typeOfMove = Type.NORMAL;
                        } else if (picked < 0.4) {
                            typeOfMove = otherType;
                        }
                        // else random
                    } else if (pkmn.getSecondaryType() != null) {
                        // Primary/Secondary: 20% primary, 20% secondary, 60% random
                        if (picked < 0.2) {
                            typeOfMove = pkmn.getPrimaryType();
                        } else if (picked < 0.4) {
                            typeOfMove = pkmn.getSecondaryType();
                        }
                        // else random
                    } else {
                        // Primary/None: 40% primary, 60% random
                        if (picked < 0.4) {
                            typeOfMove = pkmn.getPrimaryType();
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
            Pokemon pkmn = findPokemonInPoolWithSpeciesID(restrictedPokemonInclAltFormes, pkmnNum);
            if (pkmn == null) {
                continue;
            }

            double atkSpAtkRatio = pkmn.getAttackSpecialAttackRatio();

            if (pkmn.isActuallyCosmetic()) {
                for (int i = 0; i < moves.size(); i++) {
                    moves.set(i, movesets.get(pkmn.getBaseForme().getNumber()).get(i));
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
                    if ((pkmn.getPrimaryType() == Type.NORMAL && pkmn.getSecondaryType() != null) ||
                            (pkmn.getSecondaryType() == Type.NORMAL)) {

                        Type otherType = pkmn.getPrimaryType() == Type.NORMAL ? pkmn.getSecondaryType() : pkmn.getPrimaryType();

                        // Normal/OTHER: 10% normal, 30% other, 60% random
                        if (picked < 0.1) {
                            typeOfMove = Type.NORMAL;
                        } else if (picked < 0.4) {
                            typeOfMove = otherType;
                        }
                        // else random
                    } else if (pkmn.getSecondaryType() != null) {
                        // Primary/Secondary: 20% primary, 20% secondary, 60% random
                        if (picked < 0.2) {
                            typeOfMove = pkmn.getPrimaryType();
                        } else if (picked < 0.4) {
                            typeOfMove = pkmn.getSecondaryType();
                        }
                        // else random
                    } else {
                        // Primary/None: 40% primary, 60% random
                        if (picked < 0.4) {
                            typeOfMove = pkmn.getPrimaryType();
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
        Set<Integer> allBanned = new HashSet<>(noBroken ? this.getGameBreakingMoves() : Collections.emptySet());
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

        totalAvgPower /= validTypeMoves.keySet().size();

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

        this.setTrainers(trainers);

        // tms
        List<Integer> tmMoves = this.getTMMoves();

        Collections.fill(tmMoves, Moves.metronome);

        this.setTMMoves(tmMoves);

        // movetutors
        if (this.hasMoveTutors()) {
            List<Integer> mtMoves = this.getMoveTutorMoves();

            Collections.fill(mtMoves, Moves.metronome);

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
    public void customStarters(Settings settings) {
        boolean abilitiesUnchanged = settings.getAbilitiesMod() == Settings.AbilitiesMod.UNCHANGED;
        int[] customStarters = settings.getCustomStarters();
        boolean allowAltFormes = settings.isAllowStarterAltFormes();
        boolean banIrregularAltFormes = settings.isBanIrregularAltFormes();

        List<Pokemon> romPokemon = getPokemonInclFormes()
                .stream()
                .filter(pk -> pk == null || !pk.isActuallyCosmetic())
                .toList();

        PokemonSet<Pokemon> banned = getBannedFormesForPlayerPokemon();
        pickedStarters = new ArrayList<>();
        if (abilitiesUnchanged) {
            PokemonSet<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(getIrregularFormes());
        }
        // loop to add chosen pokemon to banned, preventing it from being a random option.
        for (int customStarter : customStarters) {
            if (!(customStarter - 1 == 0)) {
                banned.add(romPokemon.get(customStarter - 1));
            }
        }
        if (customStarters[0] - 1 == 0){
            Pokemon pkmn = allowAltFormes ? randomPokemonInclFormes() : randomPokemon();
            while (pickedStarters.contains(pkmn) || banned.contains(pkmn) || pkmn.isActuallyCosmetic()) {
                pkmn = allowAltFormes ? randomPokemonInclFormes() : randomPokemon();
            }
            pickedStarters.add(pkmn);
        } else {
            Pokemon pkmn1 = romPokemon.get(customStarters[0] - 1);
            pickedStarters.add(pkmn1);
        }
        if (customStarters[1] - 1 == 0){
            Pokemon pkmn = allowAltFormes ? randomPokemonInclFormes() : randomPokemon();
            while (pickedStarters.contains(pkmn) || banned.contains(pkmn) || pkmn.isActuallyCosmetic()) {
                pkmn = allowAltFormes ? randomPokemonInclFormes() : randomPokemon();
            }
            pickedStarters.add(pkmn);
        } else {
            Pokemon pkmn2 = romPokemon.get(customStarters[1] - 1);
            pickedStarters.add(pkmn2);
        }

        if (isYellow()) {
            setStarters(pickedStarters);
        } else {
            if (customStarters[2] - 1 == 0){
                Pokemon pkmn = allowAltFormes ? randomPokemonInclFormes() : randomPokemon();
                while (pickedStarters.contains(pkmn) || banned.contains(pkmn) || pkmn.isActuallyCosmetic()) {
                    pkmn = allowAltFormes ? randomPokemonInclFormes() : randomPokemon();
                }
                pickedStarters.add(pkmn);
            } else {
                Pokemon pkmn3 = romPokemon.get(customStarters[2] - 1);
                pickedStarters.add(pkmn3);
            }
            if (starterCount() > 3) {
                for (int i = 3; i < starterCount(); i++) {
                    Pokemon pkmn = random2EvosPokemon(allowAltFormes);
                    while (pickedStarters.contains(pkmn)) {
                        pkmn = random2EvosPokemon(allowAltFormes);
                    }
                    pickedStarters.add(pkmn);
                }
                setStarters(pickedStarters);
            } else {
                setStarters(pickedStarters);
            }
        }
    }

    @Override
    public void randomizeStarters(Settings settings) {
        boolean abilitiesUnchanged = settings.getAbilitiesMod() == Settings.AbilitiesMod.UNCHANGED;
        boolean allowAltFormes = settings.isAllowStarterAltFormes();
        boolean banIrregularAltFormes = settings.isBanIrregularAltFormes();

        int starterCount = starterCount();
        pickedStarters = new ArrayList<>();
        PokemonSet<Pokemon> banned = getBannedFormesForPlayerPokemon();
        if (abilitiesUnchanged) {
            PokemonSet<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(getIrregularFormes());
        }
        for (int i = 0; i < starterCount; i++) {
            Pokemon pkmn = allowAltFormes ? randomPokemonInclFormes() : randomPokemon();
            while (pickedStarters.contains(pkmn) || banned.contains(pkmn) || pkmn.isActuallyCosmetic()) {
                pkmn = allowAltFormes ? randomPokemonInclFormes() : randomPokemon();
            }
            pickedStarters.add(pkmn);
        }
        setStarters(pickedStarters);
    }

    @Override
    public void randomizeBasicTwoEvosStarters(Settings settings) {
        boolean abilitiesUnchanged = settings.getAbilitiesMod() == Settings.AbilitiesMod.UNCHANGED;
        boolean allowAltFormes = settings.isAllowStarterAltFormes();
        boolean banIrregularAltFormes = settings.isBanIrregularAltFormes();

        int starterCount = starterCount();
        pickedStarters = new ArrayList<>();
        PokemonSet<Pokemon> banned = getBannedFormesForPlayerPokemon();
        if (abilitiesUnchanged) {
            PokemonSet<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(getIrregularFormes());
        }
        for (int i = 0; i < starterCount; i++) {
            Pokemon pkmn = random2EvosPokemon(allowAltFormes);
            while (pickedStarters.contains(pkmn) || banned.contains(pkmn)) {
                pkmn = random2EvosPokemon(allowAltFormes);
            }
            pickedStarters.add(pkmn);
        }
        setStarters(pickedStarters);
    }

    @Override
    public List<Pokemon> getPickedStarters() {
        return pickedStarters;
    }

    @Override
    public boolean canChangeStaticPokemon() {
        return getRomEntry().hasStaticPokemonSupport();
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

        PokemonSet<Pokemon> banned = this.getBannedForStaticPokemon();
        banned.addAll(this.getBannedFormesForPlayerPokemon());
        if (!abilitiesAreRandomized) {
            PokemonSet<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
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
            PokemonSet<Pokemon> legendariesLeft = new PokemonSet<>(legendaryPokemon);
            if (allowAltFormes) {
                legendariesLeft.addAll(legendaryAltFormes);
                legendariesLeft = legendariesLeft.filter(pk -> !pk.isActuallyCosmetic());
            }
            PokemonSet<Pokemon> nonlegsLeft = new PokemonSet<>(nonlegendaryPokemon);
            if (allowAltFormes) {
                nonlegsLeft.addAll(nonlegendaryAltFormes);
                nonlegsLeft = nonlegsLeft.filter(pk -> !pk.isActuallyCosmetic());
            }
            PokemonSet<Pokemon> ultraBeastsLeft = new PokemonSet<>(ultraBeasts);
            legendariesLeft.removeAll(banned);
            nonlegsLeft.removeAll(banned);
            ultraBeastsLeft.removeAll(banned);

            // Full pools for easier refilling later
            PokemonSet<Pokemon> legendariesPool = new PokemonSet<>(legendariesLeft);
            PokemonSet<Pokemon> nonlegsPool = new PokemonSet<>(nonlegsLeft);
            PokemonSet<Pokemon> ultraBeastsPool = new PokemonSet<>(ultraBeastsLeft);

            for (StaticEncounter old : currentStaticPokemon) {
                StaticEncounter newStatic = cloneStaticEncounter(old);
                Pokemon newPK;
                if (old.pkmn.isLegendary()) {
                    if (reallySwapMegaEvos && old.canMegaEvolve()) {
                        newPK = getMegaEvoPokemon(legendaryPokemon, legendariesLeft, newStatic);
                    } else {
                        if (old.restrictedPool) {
                            newPK = getRestrictedStaticPokemon(legendariesPool, legendariesLeft, old);
                        } else {
                            newPK = legendariesLeft.getRandom(random);
                            legendariesLeft.remove(newPK);
                        }
                    }

                    setPokemonAndFormeForStaticEncounter(newStatic, newPK);

                    if (legendariesLeft.size() == 0) {
                        legendariesLeft.addAll(legendariesPool);
                    }
                } else if (ultraBeasts.contains(old.pkmn)) {
                    if (old.restrictedPool) {
                        newPK = getRestrictedStaticPokemon(ultraBeastsPool, ultraBeastsLeft, old);
                    } else {
                        newPK = ultraBeastsLeft.getRandom(random);
                        ultraBeasts.remove(newPK);
                    }

                    setPokemonAndFormeForStaticEncounter(newStatic, newPK);

                    if (ultraBeastsLeft.size() == 0) {
                        ultraBeastsLeft.addAll(ultraBeastsPool);
                    }
                } else {
                    if (reallySwapMegaEvos && old.canMegaEvolve()) {
                        newPK = getMegaEvoPokemon(nonlegendaryPokemon, nonlegsLeft, newStatic);
                    } else {
                        if (old.restrictedPool) {
                            newPK = getRestrictedStaticPokemon(nonlegsPool, nonlegsLeft, old);
                        } else {
                            newPK = nonlegsLeft.getRandom(random);
                            nonlegsLeft.remove(newPK);
                        }
                    }
                    setPokemonAndFormeForStaticEncounter(newStatic, newPK);

                    if (nonlegsLeft.size() == 0) {
                        nonlegsLeft.addAll(nonlegsPool);
                    }
                }
                replacements.add(newStatic);
                if (changeMusicStatics.contains(old.pkmn.getNumber())) {
                    specialMusicStaticChanges.put(old.pkmn.getNumber(), newPK.getNumber());
                }
            }
        } else if (similarStrength) {
            PokemonSet<Pokemon> listInclFormesExclCosmetics = restrictedPokemonInclAltFormes
                    .filter(pk -> !pk.isActuallyCosmetic());
            PokemonSet<Pokemon> pokemonLeft = new PokemonSet<>(!allowAltFormes ?
                    restrictedPokemon : listInclFormesExclCosmetics);
            pokemonLeft.removeAll(banned);

            PokemonSet<Pokemon> pokemonPool = new PokemonSet<>(pokemonLeft);

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
                        newPK = getMegaEvoPokemon(restrictedPokemon, pokemonLeft, newStatic);
                    } else {
                        if (old.restrictedPool) {
                            newPK = getRestrictedStaticPokemon(pokemonPool, pokemonLeft, old);
                        } else {
                            newPK = pokemonLeft.getRandom(random);
                            pokemonLeft.remove(newPK);
                        }
                    }
                    setPokemonAndFormeForStaticEncounter(newStatic, newPK);
                } else {
                    boolean limitBST = oldPK.getBaseForme() == null ?
                            limitMainGameLegendaries && mainGameLegendaries.contains(oldPK.getNumber()) :
                            limitMainGameLegendaries && mainGameLegendaries.contains(oldPK.getBaseForme().getNumber());
                    if (reallySwapMegaEvos && old.canMegaEvolve()) {
                        PokemonSet<Pokemon> megaEvoPokemonLeft = megaEvolutionsList
                                        .stream()
                                        .filter(mega -> mega.method == 1)
                                        .map(mega -> mega.from)
                                        .filter(pokemonLeft::contains)
                                        .collect(Collectors.toCollection(PokemonSet::new));
                        if (megaEvoPokemonLeft.isEmpty()) {
                            megaEvoPokemonLeft = megaEvolutionsList
                                            .stream()
                                            .filter(mega -> mega.method == 1)
                                            .map(mega -> mega.from)
                                            .filter(restrictedPokemon::contains)
                                            .collect(Collectors.toCollection(PokemonSet::new));
                        }
                        newPK = pickStaticPowerLvlReplacement(
                                megaEvoPokemonLeft,
                                oldPK,
                                true,
                                limitBST);
                        newStatic.heldItem = newPK
                                .getMegaEvolutionsFrom()
                                .get(this.random.nextInt(newPK.getMegaEvolutionsFrom().size()))
                                .argument;
                    } else {
                        if (old.restrictedPool) {
                            PokemonSet<Pokemon> restrictedPool = pokemonLeft
                                    .filter(pk -> old.restrictedList.contains(pk));
                            if (restrictedPool.isEmpty()) {
                                restrictedPool = pokemonPool.filter(pk -> old.restrictedList.contains(pk));
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
                if (changeMusicStatics.contains(old.pkmn.getNumber())) {
                    specialMusicStaticChanges.put(old.pkmn.getNumber(), newPK.getNumber());
                }
            }
        } else { // Completely random
            PokemonSet<Pokemon> listInclFormesExclCosmetics = restrictedPokemonInclAltFormes
                    .filter(pk -> !pk.isActuallyCosmetic());
            PokemonSet<Pokemon> pokemonLeft = new PokemonSet<>(!allowAltFormes ?
                    restrictedPokemon : listInclFormesExclCosmetics);
            pokemonLeft.removeAll(banned);

            PokemonSet<Pokemon> pokemonPool = new PokemonSet<>(pokemonLeft);

            for (StaticEncounter old : currentStaticPokemon) {
                StaticEncounter newStatic = cloneStaticEncounter(old);
                Pokemon newPK;
                if (reallySwapMegaEvos && old.canMegaEvolve()) {
                    newPK = getMegaEvoPokemon(restrictedPokemon, pokemonLeft, newStatic);
                } else {
                    if (old.restrictedPool) {
                        newPK = getRestrictedStaticPokemon(pokemonPool, pokemonLeft, old);
                    } else {
                        newPK = pokemonLeft.getRandom(random);
                        pokemonLeft.remove(newPK);
                    }
                }
                pokemonLeft.remove(newPK);
                setPokemonAndFormeForStaticEncounter(newStatic, newPK);
                if (pokemonLeft.size() == 0) {
                    pokemonLeft.addAll(pokemonPool);
                }
                replacements.add(newStatic);
                if (changeMusicStatics.contains(old.pkmn.getNumber())) {
                    specialMusicStaticChanges.put(old.pkmn.getNumber(), newPK.getNumber());
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

    private Pokemon getRestrictedStaticPokemon(PokemonSet<Pokemon> fullList, PokemonSet<Pokemon> pokemonLeft,
                                               StaticEncounter old) {
        PokemonSet<Pokemon> restrictedPool = pokemonLeft.filter(pk -> old.restrictedList.contains(pk));
        if (restrictedPool.isEmpty()) {
            restrictedPool = fullList.filter(pk -> old.restrictedList.contains(pk));
        }
        Pokemon newPK = restrictedPool.getRandom(random);
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
        if (pk.getFormeNumber() > 0) {
            newForme = pk.getFormeNumber();
            newPK = pk.getBaseForme();
            checkCosmetics = false;
        }
        if (checkCosmetics && pk.getCosmeticForms() > 0) {
            newForme = pk.getCosmeticFormNumber(this.random.nextInt(pk.getCosmeticForms()));
        } else if (!checkCosmetics && pk.getCosmeticForms() > 0) {
            newForme += pk.getCosmeticFormNumber(this.random.nextInt(pk.getCosmeticForms()));
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
        if (pk.getFormeNumber() > 0) {
            newStatic.forme = pk.getFormeNumber();
            newStatic.pkmn = pk.getBaseForme();
            checkCosmetics = false;
        }
        if (checkCosmetics && newStatic.pkmn.getCosmeticForms() > 0) {
            newStatic.forme = newStatic.pkmn.getCosmeticFormNumber(this.random.nextInt(newStatic.pkmn.getCosmeticForms()));
        } else if (!checkCosmetics && pk.getCosmeticForms() > 0) {
            newStatic.forme += pk.getCosmeticFormNumber(this.random.nextInt(pk.getCosmeticForms()));
        }
    }

    private Pokemon getMegaEvoPokemon(PokemonSet<Pokemon> fullList, PokemonSet<Pokemon> pokemonLeft,
                                      StaticEncounter newStatic) {
        List<MegaEvolution> megaEvos = megaEvolutionsList;
        PokemonSet<Pokemon> megaEvoPokemon = megaEvos
                        .stream()
                        .filter(mega -> mega.method == 1)
                        .map(mega -> mega.from)
                        .collect(Collectors.toCollection(PokemonSet::new));
        PokemonSet<Pokemon> megaEvoPokemonLeft = new PokemonSet<>(megaEvoPokemon).filter(pokemonLeft::contains);
        if (megaEvoPokemonLeft.isEmpty()) {
            megaEvoPokemonLeft = new PokemonSet<>(megaEvoPokemon).filter(fullList::contains);
        }

        Pokemon newPK = megaEvoPokemonLeft.getRandom(random);
        pokemonLeft.remove(newPK);
        newStatic.heldItem = newPK
                .getMegaEvolutionsFrom()
                .get(this.random.nextInt(newPK.getMegaEvolutionsFrom().size()))
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
			copyUpEvolutionsHelper.apply(true, false,
					pk -> randomizePokemonMoveCompatibility(pk, compat.get(pk), tmHMs, requiredEarlyOn, preferSameType),
					(evFrom, evTo, toMonIsFinalEvo) -> copyPokemonMoveCompatibilityUpEvolutions(evFrom, evTo,
							compat.get(evFrom), compat.get(evTo), tmHMs, preferSameType));
		} else {
			for (Map.Entry<Pokemon, boolean[]> compatEntry : compat.entrySet()) {
				randomizePokemonMoveCompatibility(compatEntry.getKey(), compatEntry.getValue(), tmHMs, requiredEarlyOn,
						preferSameType);
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
                    if (evTo.getPrimaryType().equals(mv.type)
                            && !evTo.getPrimaryType().equals(evFrom.getPrimaryType()) && !evTo.getPrimaryType().equals(evFrom.getSecondaryType())
                            || evTo.getSecondaryType() != null && evTo.getSecondaryType().equals(mv.type)
                            && !evTo.getSecondaryType().equals(evFrom.getSecondaryType()) && !evTo.getSecondaryType().equals(evFrom.getPrimaryType())) {
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
                if (pkmn.getPrimaryType().equals(mv.type)
                        || (pkmn.getSecondaryType() != null && pkmn.getSecondaryType().equals(mv.type))) {
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
            List<MoveLearnt> moveset = movesets.get(pkmn.getNumber());
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
		// Don't do anything with the base, just copy upwards to ensure later evolutions
		// retain learn compatibility
		copyUpEvolutionsHelper.apply(true, true, pk -> {}, 
				(evFrom, evTo, toMonIsFinalEvo) -> {
					boolean[] fromCompat = compat.get(evFrom);
					boolean[] toCompat = compat.get(evTo);
					for (int i = 1; i < toCompat.length; i++) {
						toCompat[i] |= fromCompat[i];
					}
				});
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
            if (pkmn.isActuallyCosmetic()) {
                boolean[] baseFlags = compat.get(pkmn.getBaseForme());
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
        List<Integer> priorityTutors = new ArrayList<>();

        if (followEvolutions) {
			copyUpEvolutionsHelper.apply(true, true,
					pk -> randomizePokemonMoveCompatibility(pk, compat.get(pk), mts, priorityTutors, preferSameType),
					(evFrom, evTo, toMonIsFinalEvo) -> copyPokemonMoveCompatibilityUpEvolutions(evFrom, evTo,
							compat.get(evFrom), compat.get(evTo), mts, preferSameType));
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
            List<MoveLearnt> moveset = movesets.get(pkmn.getNumber());
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
        copyUpEvolutionsHelper.apply(true, true, pk -> {}, 
				(evFrom, evTo, toMonIsFinalEvo) -> {
					boolean[] fromCompat = compat.get(evFrom);
					boolean[] toCompat = compat.get(evTo);
					for (int i = 1; i < toCompat.length; i++) {
						toCompat[i] |= fromCompat[i];
					}
				});
        this.setMoveTutorCompatibility(compat);
    }

    @Override
    public void copyMoveTutorCompatibilityToCosmeticFormes() {
        Map<Pokemon, boolean[]> compat = this.getMoveTutorCompatibility();

        for (Map.Entry<Pokemon, boolean[]> compatEntry : compat.entrySet()) {
            Pokemon pkmn = compatEntry.getKey();
            boolean[] flags = compatEntry.getValue();
            if (pkmn.isActuallyCosmetic()) {
                boolean[] baseFlags = compat.get(pkmn.getBaseForme());
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
        Map<Integer, List<String>>[] trainerNamesByLength = new Map[] { new TreeMap<Integer, List<String>>(),
                new TreeMap<Integer, List<String>>() };

        List<String> repeatedTrainerNames = Arrays.asList("GRUNT", "EXECUTIVE", "SHADOW", "ADMIN", "GOON", "EMPLOYEE");

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
        List<String> currentTrainerNames = getTrainerNames();
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
        List<String>[] allTrainerClasses = new List[] { new ArrayList<String>(), new ArrayList<String>() };
        Map<Integer, List<String>>[] trainerClassesByLength = new Map[] { new HashMap<Integer, List<String>>(),
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

        ItemList possibleItems = banBadItems ? this.getNonBadItems() : this.getAllowedItems();
        for (Pokemon pk : getPokemonSetInclFormes()) {
            if (pk.getGuaranteedHeldItem() == -1 && pk.getCommonHeldItem() == -1 && pk.getRareHeldItem() == -1
                    && pk.getDarkGrassHeldItem() == -1) {
                // No held items at all, abort
                return;
            }
            boolean canHaveDarkGrass = pk.getDarkGrassHeldItem() != -1;
            if (pk.getGuaranteedHeldItem() != -1) {
                // Guaranteed held items are supported.
                if (pk.getGuaranteedHeldItem() > 0) {
                    // Currently have a guaranteed item
                    double decision = this.random.nextDouble();
                    if (decision < 0.9) {
                        // Stay as guaranteed
                        canHaveDarkGrass = false;
                        pk.setGuaranteedHeldItem(possibleItems.randomItem(this.random));
                    } else {
                        // Change to 25% or 55% chance
                        pk.setGuaranteedHeldItem(0);
                        pk.setCommonHeldItem(possibleItems.randomItem(this.random));
                        pk.setRareHeldItem(possibleItems.randomItem(this.random));
                        while (pk.getRareHeldItem() == pk.getCommonHeldItem()) {
                            pk.setRareHeldItem(possibleItems.randomItem(this.random));
                        }
                    }
                } else {
                    // No guaranteed item atm
                    double decision = this.random.nextDouble();
                    if (decision < 0.5) {
                        // No held item at all
                        pk.setCommonHeldItem(0);
                        pk.setRareHeldItem(0);
                    } else if (decision < 0.65) {
                        // Just a rare item
                        pk.setCommonHeldItem(0);
                        pk.setRareHeldItem(possibleItems.randomItem(this.random));
                    } else if (decision < 0.8) {
                        // Just a common item
                        pk.setCommonHeldItem(possibleItems.randomItem(this.random));
                        pk.setRareHeldItem(0);
                    } else if (decision < 0.95) {
                        // Both a common and rare item
                        pk.setCommonHeldItem(possibleItems.randomItem(this.random));
                        pk.setRareHeldItem(possibleItems.randomItem(this.random));
                        while (pk.getRareHeldItem() == pk.getCommonHeldItem()) {
                            pk.setRareHeldItem(possibleItems.randomItem(this.random));
                        }
                    } else {
                        // Guaranteed item
                        canHaveDarkGrass = false;
                        pk.setGuaranteedHeldItem(possibleItems.randomItem(this.random));
                        pk.setCommonHeldItem(0);
                        pk.setRareHeldItem(0);
                    }
                }
            } else {
                // Code for no guaranteed items
                double decision = this.random.nextDouble();
                if (decision < 0.5) {
                    // No held item at all
                    pk.setCommonHeldItem(0);
                    pk.setRareHeldItem(0);
                } else if (decision < 0.65) {
                    // Just a rare item
                    pk.setCommonHeldItem(0);
                    pk.setRareHeldItem(possibleItems.randomItem(this.random));
                } else if (decision < 0.8) {
                    // Just a common item
                    pk.setCommonHeldItem(possibleItems.randomItem(this.random));
                    pk.setRareHeldItem(0);
                } else {
                    // Both a common and rare item
                    pk.setCommonHeldItem(possibleItems.randomItem(this.random));
                    pk.setRareHeldItem(possibleItems.randomItem(this.random));
                    while (pk.getRareHeldItem() == pk.getCommonHeldItem()) {
                        pk.setRareHeldItem(possibleItems.randomItem(this.random));
                    }
                }
            }

            if (canHaveDarkGrass) {
                double dgDecision = this.random.nextDouble();
                if (dgDecision < 0.5) {
                    // Yes, dark grass item
                    pk.setDarkGrassHeldItem(possibleItems.randomItem(this.random));
                } else {
                    pk.setDarkGrassHeldItem(0);
                }
            } else if (pk.getDarkGrassHeldItem() != -1) {
                pk.setDarkGrassHeldItem(0);
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
            } else if (trade.nickname.equalsIgnoreCase(oldgiven.getName())) {
                // change the name for sanity
                trade.nickname = trade.givenPokemon.getName();
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
        // search for level evolutions
        for (Pokemon pk : getPokemonSet()) {
            if (pk != null) {
                for (Evolution checkEvo : pk.getEvolutionsFrom()) {
                    if (checkEvo.type.usesLevel()) {
                        // If evo is intermediate and too high, bring it down
                        // Else if it's just too high, bring it down
                        if (checkEvo.extraInfo > maxIntermediateLevel && checkEvo.to.getEvolutionsFrom().size() > 0) {
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
		PokemonSet<Pokemon> pokemonPool = getRestrictedPokemon(false, altFormesCanHaveDifferentEvolutions(), false);
		int stageLimit = limitToThreeStages ? 3 : 10;

        PokemonSet<Pokemon> banned = this.getBannedFormesForPlayerPokemon();
        if (!abilitiesAreRandomized) {
            PokemonSet<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(getIrregularFormes());
        }
        
        // Cache old evolutions for data later
        Map<Pokemon, List<Evolution>> originalEvos = new HashMap<>();
        for (Pokemon pk : pokemonPool) {
            originalEvos.put(pk, new ArrayList<>(pk.getEvolutionsFrom()));
        }

        Set<EvolutionPair> newEvoPairs = new HashSet<>();
        Set<EvolutionPair> oldEvoPairs = new HashSet<>();

        if (forceChange) {
            for (Pokemon pk : pokemonPool) {
                for (Evolution ev : pk.getEvolutionsFrom()) {
                    oldEvoPairs.add(new EvolutionPair(ev.from, ev.to));
                    if (generationOfPokemon() >= 7 && ev.from.getNumber() == Species.cosmoem) { // Special case for Cosmoem to add Lunala/Solgaleo since we remove the split evo
                        int oppositeVersionLegendary = ev.to.getNumber() == Species.solgaleo ? Species.lunala : Species.solgaleo;
                        Pokemon toPkmn = findPokemonInPoolWithSpeciesID(pokemonPool, oppositeVersionLegendary);
                        if (toPkmn != null) {
                            oldEvoPairs.add(new EvolutionPair(ev.from, toPkmn));
                        }
                    }
                }
            }
        }

        PokemonSet<Pokemon> replacements = new PokemonSet<>();

        int loops = 0;
        while (loops < 1) {
            // Setup for this loop.
            boolean hadError = false;
            for (Pokemon pk : pokemonPool) {
                pk.getEvolutionsFrom().clear();
                pk.getEvolutionsTo().clear();
            }
            newEvoPairs.clear();

            // TODO: is the below a possible problem for our PokemonSets? 
//            // Shuffle pokemon list so the results aren't overly predictable.
//            Collections.shuffle(pokemonPool, this.random);

            for (Pokemon fromPK : pokemonPool) { 
                List<Evolution> oldEvos = originalEvos.get(fromPK);
                for (Evolution ev : oldEvos) {
                    // Pick a Pokemon as replacement
                    replacements.clear();

                    PokemonSet<Pokemon> chosen = getRestrictedPokemon(true, allowAltFormes, false);
                    // Step 1: base filters
                    for (Pokemon pk : chosen) {
                        // Prevent evolving into oneself (mandatory)
                        if (pk == fromPK) {
                            continue;
                        }

                        // Force same EXP curve (mandatory)
                        if (pk.getGrowthCurve() != fromPK.getGrowthCurve()) {
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
                        fromPK.getEvolutionsFrom().add(tempEvo);
                        pk.getEvolutionsTo().add(tempEvo);
                        boolean exceededLimit = false;

                        PokemonSet<Pokemon> related = PokemonSet.related(fromPK);

                        for (Pokemon pk2 : related) {
                            int numPreEvos = numPreEvolutions(pk2, stageLimit);
                            if (numPreEvos >= stageLimit) {
                                exceededLimit = true;
                                break;
                            } else if (numPreEvos == stageLimit - 1 && pk2.getEvolutionsFrom().size() == 0
                                    && originalEvos.get(pk2).size() > 0) {
                                exceededLimit = true;
                                break;
                            }
                        }

                        fromPK.getEvolutionsFrom().remove(tempEvo);
                        pk.getEvolutionsTo().remove(tempEvo);

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
                            if (fromPK.getNumber() == Species.eevee) {
                                if (pk.getPrimaryType() == ev.to.getPrimaryType()
                                        || (pk.getSecondaryType() != null) && pk.getSecondaryType() == ev.to.getPrimaryType()) {
                                    includeType.add(pk);
                                }
                            } else if (pk.getPrimaryType() == fromPK.getPrimaryType()
                                    || (fromPK.getSecondaryType() != null && pk.getPrimaryType() == fromPK.getSecondaryType())
                                    || (pk.getSecondaryType() != null && pk.getSecondaryType() == fromPK.getPrimaryType())
                                    || (fromPK.getSecondaryType() != null && pk.getSecondaryType() != null && pk.getSecondaryType() == fromPK.getSecondaryType())) {
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

                    if (similarStrength) {
                        picked = pickEvoPowerLvlReplacement(replacements, ev.to);
                        alreadyPicked.add(picked);
                    } else {
                        picked = replacements.getRandom(random);
                        alreadyPicked.add(picked);
                    }

                    // Step 4: add it to the new evos pool
                    Evolution newEvo = new Evolution(fromPK, picked, ev.carryStats, ev.type, ev.extraInfo);
                    boolean checkCosmetics = true;
                    if (picked.getFormeNumber() > 0) {
                        newEvo.forme = picked.getFormeNumber();
                        newEvo.formeSuffix = picked.getFormeSuffix();
                        checkCosmetics = false;
                    }
                    if (checkCosmetics && newEvo.to.getCosmeticForms() > 0) {
                        newEvo.forme = newEvo.to.getCosmeticFormNumber(this.random.nextInt(newEvo.to.getCosmeticForms()));
                    } else if (!checkCosmetics && picked.getCosmeticForms() > 0) {
                        newEvo.forme += picked.getCosmeticFormNumber(this.random.nextInt(picked.getCosmeticForms()));
                    }
                    if (newEvo.type == EvolutionType.LEVEL_FEMALE_ESPURR) {
                        newEvo.type = EvolutionType.LEVEL_FEMALE_ONLY;
                    }
                    fromPK.getEvolutionsFrom().add(newEvo);
                    picked.getEvolutionsTo().add(newEvo);
                    newEvoPairs.add(new EvolutionPair(fromPK, picked));
                }

                if (hadError) {
                    // No need to check the other Pokemon if we already errored
                    break;
                }
            }

            // If no error, done and return
            if (!hadError) {
                for (Pokemon pk : restrictedPokemonInclAltFormes.filterCosmetic()) {
                    pk.copyBaseFormeEvolutions(pk.getBaseForme());
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
		PokemonSet<Pokemon> pokemonPool = getRestrictedPokemon(true, altFormesCanHaveDifferentEvolutions(), false);

        PokemonSet<Pokemon> banned = this.getBannedFormesForPlayerPokemon();
        if (!abilitiesAreRandomized) {
            banned.addAll(getAbilityDependentFormes());
        }

        Set<EvolutionPair> oldEvoPairs = new HashSet<>();

        if (forceChange) {
            for (Pokemon pk : pokemonPool) {
                for (Evolution ev : pk.getEvolutionsFrom()) {
                    oldEvoPairs.add(new EvolutionPair(ev.from, ev.to));
                    if (generationOfPokemon() >= 7 && ev.from.getNumber() == Species.cosmoem) { // Special case for Cosmoem to add Lunala/Solgaleo since we remove the split evo
                        int oppositeVersionLegendary = ev.to.getNumber() == Species.solgaleo ? Species.lunala : Species.solgaleo;
                        Pokemon toPkmn = findPokemonInPoolWithSpeciesID(pokemonPool, oppositeVersionLegendary);
                        if (toPkmn != null) {
                            oldEvoPairs.add(new EvolutionPair(ev.from, toPkmn));
                        }
                    }
                }
            }
        }

        PokemonSet<Pokemon> replacements = new PokemonSet<>();

        int loops = 0;
        while (loops < 1) {
            // Setup for this loop.
            boolean hadError = false;
            for (Pokemon pk : pokemonPool) {
                pk.getEvolutionsFrom().clear();
                pk.getEvolutionsTo().clear();
            }
            
            // TODO: predictability could be a problem here as well

            for (Pokemon fromPK : pokemonPool) {
                // Pick a Pokemon as replacement
                replacements.clear();

                PokemonSet<Pokemon> chosen = getRestrictedPokemon(true, allowAltFormes, false);
                // Step 1: base filters
                for (Pokemon pk : chosen) {
                    // Prevent evolving into oneself (mandatory)
                    if (pk == fromPK) {
                        continue;
                    }

                    // Force same EXP curve (mandatory)
                    if (pk.getGrowthCurve() != fromPK.getGrowthCurve()) {
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
                        if (pk.getPrimaryType() == fromPK.getPrimaryType()
                                || (fromPK.getSecondaryType() != null && pk.getPrimaryType() == fromPK.getSecondaryType())
                                || (pk.getSecondaryType() != null && pk.getSecondaryType() == fromPK.getPrimaryType())
                                || (pk.getSecondaryType() != null && pk.getSecondaryType() == fromPK.getSecondaryType())) {
                            includeType.add(pk);
                        }
                    }

                    if (includeType.size() != 0) {
                        replacements.retainAll(includeType);
                    }
                }

                // Step 3: pick - by similar strength or otherwise // ??? no similar-strength checking is done here
                Pokemon picked = replacements.getRandom(random);

                // Step 4: create new level 1 evo and add it to the new evos pool
                Evolution newEvo = new Evolution(fromPK, picked, false, EvolutionType.LEVEL, 1);
                newEvo.level = 1;
                boolean checkCosmetics = true;
                if (picked.getFormeNumber() > 0) {
                    newEvo.forme = picked.getFormeNumber();
                    newEvo.formeSuffix = picked.getFormeSuffix();
                    checkCosmetics = false;
                }
                if (checkCosmetics && newEvo.to.getCosmeticForms() > 0) {
                    newEvo.forme = newEvo.to.getCosmeticFormNumber(this.random.nextInt(newEvo.to.getCosmeticForms()));
                } else if (!checkCosmetics && picked.getCosmeticForms() > 0) {
                    newEvo.forme += picked.getCosmeticFormNumber(this.random.nextInt(picked.getCosmeticForms()));
                }
                fromPK.getEvolutionsFrom().add(newEvo);
                picked.getEvolutionsTo().add(newEvo);
            }

            // If no error, done and return
            if (!hadError) {
                for (Pokemon pk : restrictedPokemonInclAltFormes.filterCosmetic()) {
                    pk.copyBaseFormeEvolutions(pk.getBaseForme());
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
        for (Pokemon pk : getPokemonSetInclFormes()) {
            int minCatchRate = pk.isLegendary() ? rateLegendary : rateNonLegendary;
            pk.setCatchRate(Math.max(pk.getCatchRate(), minCatchRate));
        }

    }

    @Override
    public void standardizeEXPCurves(Settings settings) {
        Settings.ExpCurveMod mod = settings.getExpCurveMod();
        ExpCurve expCurve = settings.getSelectedEXPCurve();

        PokemonSet<Pokemon> pokes = getPokemonSetInclFormes();
        switch (mod) {
            case LEGENDARIES:
                for (Pokemon pk : pokes) {
                	pk.setGrowthCurve(pk.isLegendary() ? ExpCurve.SLOW : expCurve);
                }
                break;
            case STRONG_LEGENDARIES:
                for (Pokemon pk : pokes) {
                    pk.setGrowthCurve(pk.isStrongLegendary() ? ExpCurve.SLOW : expCurve);
                }
                break;
            case ALL:
                for (Pokemon pk : pokes) {
                    pk.setGrowthCurve(expCurve);
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

    private Pokemon pickEvoPowerLvlReplacement(PokemonSet<Pokemon> pokemonPool, Pokemon current) {
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
    private Pokemon findPokemonInPoolWithSpeciesID(Collection<Pokemon> pokemonPool, int speciesID) {
        for (Pokemon pk : pokemonPool) {
            if (pk.getNumber() == speciesID) {
                return pk;
            }
        }
        return null;
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
        from.getEvolutionsFrom().add(tempEvo);
        Set<Pokemon> visited = new HashSet<>();
        Set<Pokemon> recStack = new HashSet<>();
        boolean recur = isCyclic(from, visited, recStack);
        from.getEvolutionsFrom().remove(tempEvo);
        return recur;
    }

    private boolean isCyclic(Pokemon pk, Set<Pokemon> visited, Set<Pokemon> recStack) {
        if (!visited.contains(pk)) {
            visited.add(pk);
            recStack.add(pk);
            for (Evolution ev : pk.getEvolutionsFrom()) {
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

    private boolean checkForUnusedMove(List<Move> potentialList, List<Integer> alreadyUsed) {
        for (Move mv : potentialList) {
            if (!alreadyUsed.contains(mv.number)) {
                return true;
            }
        }
        return false;
    }
    
    private Map<Type, Integer> typeWeightings;
    private int totalTypeWeighting;

    /**
     * Picks a type, sometimes based on frequency of non-banned Pokémon of that type. Compare with randomType().
     * Never picks a type with no non-banned Pokémon, even when weightByFrequency == false.
     */
    private Type pickType(boolean weightByFrequency, boolean noLegendaries, boolean allowAltFormes) {
        if (totalTypeWeighting == 0) {
            initTypeWeightings(noLegendaries, allowAltFormes);
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
            // assumes some type has non-banned Pokémon
            Type picked;
            do {
                picked = randomType();
            } while (typeWeightings.get(picked) == 0);
            return picked;
        }
    }

    private void initTypeWeightings(boolean noLegendaries, boolean allowAltFormes) {
        // Determine weightings
        for (Type t : Type.values()) {
            if (typeInGame(t)) {
                PokemonSet<Pokemon> pokemonOfType = getRestrictedPokemon(noLegendaries, allowAltFormes, true)
                			.filterByType(t);
                int pkWithTyping = pokemonOfType.size();
                typeWeightings.put(t, pkWithTyping);
                totalTypeWeighting += pkWithTyping;
            }
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
        PokemonSet<Pokemon> candidates = new PokemonSet<>();
        for (Evolution ev : base.getEvolutionsFrom()) {
            if (!mustEvolveItself || ev.to.getEvolutionsFrom().size() > 0) {
                candidates.add(ev.to);
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
        if (pk.getEvolutionsTo().size() == 0) {
            return 0;
        } else {
            if (depth == maxInterested - 1) {
                return 1;
            } else {
                int maxPreEvos = 0;
                for (Evolution ev : pk.getEvolutionsTo()) {
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
        if (pk.getEvolutionsFrom().size() == 0) {
            return 0;
        } else {
            if (depth == maxInterested - 1) {
                return 1;
            } else {
                int maxEvos = 0;
                for (Evolution ev : pk.getEvolutionsFrom()) {
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
            if (pokemon.getEvolutionsFrom().size() == 0) {
                // fully evolved
                break;
            }

            // check for cyclic evolutions from what we've already seen
            boolean cyclic = false;
            for (Evolution ev : pokemon.getEvolutionsFrom()) {
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
            int evolutionIndex = (this.fullyEvolvedRandomSeed + trainerIndex) % pokemon.getEvolutionsFrom().size();
            pokemon = pokemon.getEvolutionsFrom().get(evolutionIndex).to;
            seenMons.add(pokemon);
        }

        return pokemon;
    }

    private Map<Type, PokemonSet<Pokemon>> cachedReplacements;
    private PokemonSet<Pokemon> cachedAll;
    private PokemonSet<Pokemon> banned = new PokemonSet<>();
    private PokemonSet<Pokemon> usedAsUnique = new PokemonSet<>();

    private Pokemon pickTrainerPokeReplacement(Pokemon current, boolean usePowerLevels, Type type,
                                               boolean noLegendaries, boolean wonderGuardAllowed,
                                               boolean usePlacementHistory, boolean swapMegaEvos,
                                               boolean abilitiesAreRandomized, boolean allowAltFormes,
                                               boolean banIrregularAltFormes) {
        PokemonSet<Pokemon> pickFrom;
        PokemonSet<Pokemon> withoutBannedPokemon;

        if (swapMegaEvos) {
            pickFrom = megaEvolutionsList
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
                PokemonSet<Pokemon> pokemonOfType = getRestrictedPokemon(noLegendaries, allowAltFormes, false)
                		.filterByType(type);
                pokemonOfType.removeAll(this.getBannedFormesForPlayerPokemon());
                if (!abilitiesAreRandomized) {
                    PokemonSet<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
                    pokemonOfType.removeAll(abilityDependentFormes);
                }
                if (banIrregularAltFormes) {
                    getIrregularFormes().forEach(pokemonOfType::remove);
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

    private Pokemon pickWildPowerLvlReplacement(PokemonSet<Pokemon> pokemonPool, Pokemon current, boolean banSamePokemon,
            PokemonSet<Pokemon> usedUp, int bstBalanceLevel) {
        // start with within 10% and add 5% either direction till we find
        // something
        int balancedBST = bstBalanceLevel * 10 + 250;
        int currentBST = Math.min(current.bstForPowerLevels(), balancedBST);
        int minTarget = currentBST - currentBST / 10;
        int maxTarget = currentBST + currentBST / 10;
        PokemonSet<Pokemon> canPick = new PokemonSet<>();
        int expandRounds = 0;
        while (canPick.isEmpty() || (canPick.size() < 3 && expandRounds < 3)) {
            for (Pokemon pk : pokemonPool) {
                if (pk.bstForPowerLevels() >= minTarget && pk.bstForPowerLevels() <= maxTarget
                        && (!banSamePokemon || pk != current) && (usedUp == null || !usedUp.contains(pk))
                        && !canPick.contains(pk)) {
                    canPick.add(pk);
                }
            }
            minTarget -= currentBST / 20;
            maxTarget += currentBST / 20;
            expandRounds++;
        }
        return canPick.getRandom(random);
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

    public Pokemon pickEntirelyRandomPokemon(boolean includeFormes, boolean noLegendaries, EncounterArea area,
                                             PokemonSet<Pokemon> banned) {
        Pokemon result;
        Pokemon randomNonLegendaryPokemon = includeFormes ? randomNonLegendaryPokemonInclFormes() : randomNonLegendaryPokemon();
        Pokemon randomPokemon = includeFormes ? randomPokemonInclFormes() : randomPokemon();
        result = noLegendaries ? randomNonLegendaryPokemon : randomPokemon;
        while (result.isActuallyCosmetic()) {
            randomNonLegendaryPokemon = includeFormes ? randomNonLegendaryPokemonInclFormes() : randomNonLegendaryPokemon();
            randomPokemon = includeFormes ? randomPokemonInclFormes() : randomPokemon();
            result = noLegendaries ? randomNonLegendaryPokemon : randomPokemon;
        }
        while (banned.contains(result) || area.getBannedPokemon().contains(result)) {
            randomNonLegendaryPokemon = includeFormes ? randomNonLegendaryPokemonInclFormes() : randomNonLegendaryPokemon();
            randomPokemon = includeFormes ? randomPokemonInclFormes() : randomPokemon();
            result = noLegendaries ? randomNonLegendaryPokemon : randomPokemon;
            while (result.isActuallyCosmetic()) {
                randomNonLegendaryPokemon = includeFormes ? randomNonLegendaryPokemonInclFormes() : randomNonLegendaryPokemon();
                randomPokemon = includeFormes ? randomPokemonInclFormes() : randomPokemon();
                result = noLegendaries ? randomNonLegendaryPokemon : randomPokemon;
            }
        }
        return result;
    }

    private Pokemon pickStaticPowerLvlReplacement(PokemonSet<Pokemon> pokemonPool, Pokemon current,
                                                  boolean banSamePokemon, boolean limitBST) {
        // start with within 10% and add 5% either direction till we find
        // something
        int currentBST = current.bstForPowerLevels();
        int minTarget = limitBST ? currentBST - currentBST / 5 : currentBST - currentBST / 10;
        int maxTarget = limitBST ? currentBST : currentBST + currentBST / 10;
        PokemonSet<Pokemon> canPick = new PokemonSet<>();
        int expandRounds = 0;
        while (canPick.isEmpty() || (canPick.size() < 3 && expandRounds < 3)) {
            for (Pokemon pk : pokemonPool) {
                if (pk.bstForPowerLevels() >= minTarget && pk.bstForPowerLevels() <= maxTarget
                        && (!banSamePokemon || pk != current)) {
                    canPick.add(pk);
                }
            }
            minTarget -= currentBST / 20;
            maxTarget += currentBST / 20;
            expandRounds++;
        }
        return canPick.getRandom(random);
    }

    @Override
    public PokemonSet<Pokemon> getAbilityDependentFormes() {
        PokemonSet<Pokemon> abilityDependentFormes = new PokemonSet<>();
        for (Pokemon pk : restrictedPokemonInclAltFormes) {
            if (pk.getBaseForme() != null) {
                if (pk.getBaseNumber() == Species.castform) {
                    // All alternate Castform formes
                    abilityDependentFormes.add(pk);
                } else if (pk.getBaseNumber() == Species.darmanitan && pk.getFormeNumber() == 1) {
                    // Darmanitan-Z
                    abilityDependentFormes.add(pk);
                } else if (pk.getBaseNumber() == Species.aegislash) {
                    // Aegislash-B
                    abilityDependentFormes.add(pk);
                } else if (pk.getBaseNumber() == Species.wishiwashi) {
                    // Wishiwashi-S
                    abilityDependentFormes.add(pk);
                }
            }
        }
        return abilityDependentFormes;
    }

    @Override
    public PokemonSet<Pokemon> getBannedFormesForPlayerPokemon() {
        PokemonSet<Pokemon> bannedFormes = new PokemonSet<>();
        for (Pokemon pk : restrictedPokemonInclAltFormes) {
            if (pk.getBaseForme() != null) {
                if (pk.getBaseNumber() == Species.giratina) {
                    // Giratina-O is banned because it reverts back to Altered Forme if
                    // equipped with any item that isn't the Griseous Orb.
                    bannedFormes.add(pk);
                } else if (pk.getBaseNumber() == Species.shaymin) {
                    // Shaymin-S is banned because it reverts back to its original forme
                    // under a variety of circumstances, and can only be changed back
                    // with the Gracidea.
                    bannedFormes.add(pk);
                }
            }
        }
        return bannedFormes;
    }

    @Override
    public void randomizeTotemPokemon(Settings settings) {
        // should this really be here, in AbstractRomHandler, when it is only relevant to Gen 7 games?
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
        PokemonSet<Pokemon> banned = this.getBannedForStaticPokemon();
        if (!abilitiesAreRandomized) {
            PokemonSet<Pokemon> abilityDependentFormes = getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(getIrregularFormes());
        }

        PokemonSet<Pokemon> listInclFormesExclCosmetics = restrictedPokemonInclAltFormes.filter(
                pk -> !pk.isActuallyCosmetic());
        PokemonSet<Pokemon> pokemonLeft = new PokemonSet<>(!allowAltFormes ?
                restrictedPokemon : listInclFormesExclCosmetics);
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
                    newPK = pokemonLeft.getRandom(random);
                    pokemonLeft.remove(newPK);
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
                    pokemonLeft.addAll(!allowAltFormes ? restrictedPokemon : listInclFormesExclCosmetics);
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
                        newAllyPK = pokemonLeft.getRandom(random);
                        pokemonLeft.remove(newAllyPK);
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
                        pokemonLeft.addAll(!allowAltFormes ? restrictedPokemon : listInclFormesExclCosmetics);
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
		getPokemonSet().forEach(pk -> pk.setName(RomFunctions.camelCase(pk.getName())));
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

    private PokemonSet<Pokemon> getBelowAveragePlacements() {
        // This method will return a PK if the number of times a pokemon has been
        // placed is less than average of all placed pokemon's appearances
        // E.g., Charmander's been placed once, but the average for all pokemon is 2.2
        // So add to list and return 

        PokemonSet<Pokemon> toPlacePK = new PokemonSet<>();
        PokemonSet<Pokemon> placedPK = new PokemonSet<>(placementHistory.keySet());
        PokemonSet<Pokemon> allPK = cachedAll;
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
            System.out.println(p.getName() +": "+ placementHistory.get(p));
        }
    }

    ///// Item functions
    private void setItemPlacementHistory(int newItem) {
        int history = getItemPlacementHistory(newItem);
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
    public List<Integer> getMovesBannedFromLevelup() {
        return new ArrayList<>();
    }

    @Override
    public PokemonSet<Pokemon> getBannedForStaticPokemon() {
        return new PokemonSet<>();
    }

    @Override
    public boolean forceSwapStaticMegaEvos() {
        return false;
    }

    @Override
    public List<String> getTrainerNames() {
        return getTrainers().stream().map(tr -> tr.name).toList();
    }

    @Override
    public void setTrainerNames(List<String> trainerNames) {
        for (int i = 0; i < trainerNames.size(); i++) {
            getTrainers().get(i).name = trainerNames.get(i);
        }
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
        return List.of(0);
    }

    @Override
    public List<Integer> getAllConsumableHeldItems() {
        return List.of(0);
    }

    @Override
    public List<Integer> getAllHeldItems() {
        return List.of(0);
    }

    @Override
    public PokemonSet<Pokemon> getBannedFormesForTrainerPokemon() {
        return new PokemonSet<>();
    }

    @Override
    public List<PickupItem> getPickupItems() {
        return new ArrayList<>();
    }

    @Override
    public void setPickupItems(List<PickupItem> pickupItems) {
        // do nothing
    }

    @Override
    public void randomizePokemonPalettes(Settings settings) {
        // I personally don't think it should be the responsibility of the RomHandlers to
        // communicate with the Settings - isn't that the role of the Randomizer class?
        // This (overloading the method) is a compromise. // voliol 2022-08-28
        randomizePokemonPalettes(settings.isPokemonPalettesFollowTypes(), settings.isPokemonPalettesFollowEvolutions(),
                settings.isPokemonPalettesShinyFromNormal());
    }

    public void randomizePokemonPalettes(boolean typeSanity, boolean evolutionSanity, boolean shinyFromNormal) {
        getPaletteHandler().randomizePokemonPalettes(getPokemonSet(), typeSanity, evolutionSanity, shinyFromNormal);
    }

    public abstract PaletteHandler getPaletteHandler();

    // just for testing
    protected final void dumpAllPokemonSprites() {
        List<BufferedImage> bims = getAllPokemonImages();

        for (int i = 0; i < bims.size(); i++) {
            String fileAdress = "Pokemon_sprite_dump/gen" + generationOfPokemon() + "/"
                    + String.format("%03d_d.png", i + 1);
            File outputfile = new File(fileAdress);
            try {
                ImageIO.write(bims.get(i), "png", outputfile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    protected abstract List<BufferedImage> getAllPokemonImages();

	public abstract void savePokemonPalettes();

    @Override
	public boolean saveRom(String filename, long seed, boolean saveAsDirectory) {
    	try {
    		prepareSaveRom();
    		return saveAsDirectory ? saveRomDirectory(filename) : saveRomFile(filename, seed);
    	} catch (RandomizerIOException e) {
    		e.printStackTrace();
    		return false;
    	}
	}

	/**
	 * Writes the remaining things to the ROM, before it is written to file. When
	 * overridden, this should be called as a superclass method.
	 */
	protected void prepareSaveRom() {
		savePokemonStats();
		saveMoves();
		savePokemonPalettes();
	}

	public abstract void saveMoves();

    public abstract void savePokemonStats();

	protected abstract boolean saveRomFile(String filename, long seed);
	
	protected abstract boolean saveRomDirectory(String filename);

    protected abstract RomEntry getRomEntry();

    @Override
    public String getROMName() {
        return "Pokemon " + getRomEntry().getName();
    }

    @Override
    public String getROMCode() {
        return getRomEntry().getRomCode();
    }

    @Override
    public String getSupportLevel() {
        return getRomEntry().hasStaticPokemonSupport() ? "Complete" : "No Static Pokemon";
    }
	
}
