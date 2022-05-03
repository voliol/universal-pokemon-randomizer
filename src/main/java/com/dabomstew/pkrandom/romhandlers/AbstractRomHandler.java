package com.dabomstew.pkrandom.romhandlers;

/*----------------------------------------------------------------------------*/
/*--  AbstractRomHandler.java - a base class for all rom handlers which     --*/
/*--                            implements the majority of the actual       --*/
/*--                            randomizer logic by building on the base    --*/
/*--                            getters & setters provided by each concrete --*/
/*--                            handler.                                    --*/
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.dabomstew.pkrandom.CustomNamesSet;
import com.dabomstew.pkrandom.MiscTweak;
import com.dabomstew.pkrandom.RomFunctions;
import com.dabomstew.pkrandom.RomOptions;
import com.dabomstew.pkrandom.constants.GlobalConstants;
import com.dabomstew.pkrandom.exceptions.RandomizationException;
import com.dabomstew.pkrandom.gui.TemplateData;
import com.dabomstew.pkrandom.pokemon.*;

public abstract class AbstractRomHandler implements RomHandler {

    private boolean restrictionsSet;
    protected boolean disableROMHack;
    protected List<Pokemon> mainPokemonList;
    protected List<Pokemon> noLegendaryList, onlyLegendaryList;
    protected final Random random;
    private List<Pokemon> alreadyPicked = new ArrayList<Pokemon>();
    private List<Pokemon> giratinaPicks;
    protected boolean ptGiratina = false;
    protected PokemonCollection starterPokes;
    protected Map<String, Type> groupTypesMap = new HashMap<String, Type>();
    private HashMap<Pokemon, Pokemon> trainerTranslateMap;
    private PokemonCollection cachedReplacementLists;
    private PokemonCollection cachedEliteReplacementLists;
    private List<Type> typeInGame;
    protected CopyUpEvolutionsHelper copyUpEvolutionsHelper = new CopyUpEvolutionsHelper(this);

    /* Constructor */
    public AbstractRomHandler(Random random) {
        this.random = random;
    }

    /*
     * Public Methods, implemented here for all gens. Unlikely to be overridden.
     */

    public void setPokemonPool(GenRestrictions restrictions, RomOptions romOptions) {
        restrictionsSet = true;
        mainPokemonList = this.allPokemonWithoutNull();
        if (romOptions != null && romOptions.isRandomizeSubset()) {
            // Reduce the main pokemon list to
            // Highest value of either the minPokemonNumber, or 0 (prevents going below 0)
            // Lowest value of either the maxPokemonNumber, or pokemon available (prevents going
            // above available)
            try {
                mainPokemonList = getPokemon().subList(
                        Math.max(romOptions.getMinimumRandomizablePokemonNumber(), 1),
                        Math.min(romOptions.getMaximumRandomizablePokemonNumber() + 1,
                                getPokemon().size()));
            } catch (IllegalArgumentException e) {
                throw new RandomizationException(
                        "Min pokemon number must be lower than max pokemon number! Please fix your rom options file.");
            }
        }



        if (restrictions != null) {
            mainPokemonList = new ArrayList<Pokemon>();
            List<Pokemon> allPokemon = this.getPokemon();
            int highestNumber = allPokemon.size();
            if (romOptions != null && romOptions.isRandomizeSubset()) {
                // Reduce the main pokemon list to
                // Highest value of either the minPokemonNumber, or 0 (prevents going below 0)
                // Lowest value of either the maxPokemonNumber, or pokemon available (prevents going
                // above available)
                allPokemon = allPokemon.subList(
                        Math.max(romOptions.getMinimumRandomizablePokemonNumber(), 1),
                        Math.min(romOptions.getMaximumRandomizablePokemonNumber() + 1,
                                allPokemon.size()));
                highestNumber = allPokemon.get(allPokemon.size() - 1).number;
            }

            if (restrictions.allow_gen1) {
                getMainPokemonList().addAll(getPokesFromRange(allPokemon, 1, 151));
                if (restrictions.assoc_g1_g2 && highestNumber > 151) {
                    addEvosFromRange(getMainPokemonList(), 1, 151, 152, 251);
                }
                if (restrictions.assoc_g1_g4 && highestNumber > 386) {
                    addEvosFromRange(getMainPokemonList(), 1, 151, 387, 493);
                }
            }

            if (restrictions.allow_gen2 && highestNumber > 151) {
                getMainPokemonList().addAll(getPokesFromRange(allPokemon, 152, 251));
                if (restrictions.assoc_g2_g1) {
                    addEvosFromRange(getMainPokemonList(), 152, 251, 1, 151);
                }
                if (restrictions.assoc_g2_g3 && highestNumber > 251) {
                    addEvosFromRange(getMainPokemonList(), 152, 251, 252, 386);
                }
                if (restrictions.assoc_g2_g4 && highestNumber > 386) {
                    addEvosFromRange(getMainPokemonList(), 152, 251, 387, 493);
                }
            }

            if (restrictions.allow_gen3 && highestNumber > 251) {
                getMainPokemonList().addAll(getPokesFromRange(allPokemon, 252, 386));
                if (restrictions.assoc_g3_g2) {
                    addEvosFromRange(getMainPokemonList(), 252, 386, 152, 251);
                }
                if (restrictions.assoc_g3_g4 && highestNumber > 386) {
                    addEvosFromRange(getMainPokemonList(), 252, 386, 387, 493);
                }
            }

            if (restrictions.allow_gen4 && highestNumber > 386) {
                getMainPokemonList().addAll(getPokesFromRange(allPokemon, 387, 493));
                if (restrictions.assoc_g4_g1) {
                    addEvosFromRange(getMainPokemonList(), 387, 493, 1, 151);
                }
                if (restrictions.assoc_g4_g2) {
                    addEvosFromRange(getMainPokemonList(), 387, 493, 152, 251);
                }
                if (restrictions.assoc_g4_g3) {
                    addEvosFromRange(getMainPokemonList(), 387, 493, 252, 386);
                }
            }

            if (restrictions.allow_gen5 && highestNumber > 493) {
                getMainPokemonList().addAll(getPokesFromRange(allPokemon, 494, 649));
            }

            if (getMainPokemonList().size() == 0) {
                throw new RandomizationException(
                        "No pokemon are in the main list. Please reduce the GenRestrictions.");
            }
        }

        mainPokemonList = getMainPokemonList().stream().distinct().collect(Collectors.toList());

        noLegendaryList = new ArrayList<Pokemon>();
        onlyLegendaryList = new ArrayList<Pokemon>();
        giratinaPicks = new ArrayList<Pokemon>();

        for (Pokemon p : getMainPokemonList()) {
            if (p.isLegendary()) {
                onlyLegendaryList.add(p);
            } else {
                noLegendaryList.add(p);
            }
            if (GlobalConstants.ptSpecialIntros.contains(p.number)) {
                giratinaPicks.add(p);
            }
        }

        if (giratinaPicks.size() < 1) {
            giratinaPicks.addAll(onlyLegendaryList);
        }
    }

    private List<Pokemon> getPokesFromRange(List<Pokemon> allPokemon, int range_min,
            int range_max) {
        return allPokemon.stream()
                .filter(p -> p != null && p.getNumber() >= range_min && p.getNumber() <= range_max)
                .collect(Collectors.toList());
    }

    private void addEvosFromRange(List<Pokemon> pokemonPool, int first_min, int first_max,
            int second_min, int second_max) {
        Set<Pokemon> newPokemon = new TreeSet<Pokemon>();
        getPokesFromRange(pokemonPool, first_min, first_max).forEach(p -> {
            newPokemon.addAll(p.evolutionsFrom.stream()
                    .filter(ev -> ev.to.number >= second_min && ev.to.number <= second_max)
                    .map(ev -> ev.getTo()).collect(Collectors.toList()));

            newPokemon.addAll(p.evolutionsTo.stream()
                    .filter(ev -> ev.from.number >= second_min && ev.from.number <= second_max)
                    .map(ev -> ev.getFrom()).collect(Collectors.toList()));
        });

        newPokemon.forEach(pk -> addRelatedPokemon(pokemonPool, pk, second_min, second_max,
                evPk -> evPk.getEvolutionsFrom(), ev -> ev.getTo()));
        newPokemon.forEach(pk -> addRelatedPokemon(pokemonPool, pk, second_min, second_max,
                evPk -> evPk.getEvolutionsTo(), ev -> ev.getFrom()));
    }

    private void addRelatedPokemon(List<Pokemon> pokemonPool, Pokemon pk, int min, int max,
            Function<Pokemon, List<Evolution>> evolutions, Function<Evolution, Pokemon> evo) {
        if (pk.number >= min && pk.number <= max) {
            pokemonPool.add(pk);
            evolutions.apply(pk).forEach(
                    ev -> addRelatedPokemon(pokemonPool, evo.apply(ev), min, max, evolutions, evo));
        }
    }

	@Override
	public void shufflePokemonStats(boolean evolutionSanity) {
		copyUpEvolutionsHelper.apply(evolutionSanity, new BasePokemonAction() {
			public void applyTo(Pokemon pk) {
				pk.shuffleStats(random);
			}
		}, new EvolvedPokemonAction() {
			public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
				evTo.copyShuffledStatsUpEvolution(evFrom);
			}
		});
	}

    @Override
    public void randomizePokemonStatsWithinBST(boolean evolutionSanity) {
		copyUpEvolutionsHelper.apply(evolutionSanity, new BasePokemonAction() {
			public void applyTo(Pokemon pk) {
				pk.randomizeStatsWithinBST(random);
			}
		}, new EvolvedPokemonAction() {
			public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
				evTo.copyRandomizedStatsUpEvolution(evFrom, random);
			}
		});
    }

	@Override
	public void randomizePokemonStatsUnrestricted(boolean evolutionSanity) {
		copyUpEvolutionsHelper.apply(evolutionSanity, new BasePokemonAction() {
			public void applyTo(Pokemon pk) {
				pk.randomizeStatsNoRestrictions(random, true);
			}
		}, new EvolvedPokemonAction() {
			public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
				evTo.copyRandomizedStatsNoRestrictionsUpEvolution(evFrom, random);
			}
		});
	}

    @Override
    public void randomizeCompletelyPokemonStats(boolean evolutionSanity) {
        this.shuffleAllPokemonBSTs(false, true);

        List<Pokemon> allPokes = this.getMainPokemonList();
        if (evolutionSanity) {
            int count = 0;
            double total = 0.0;

            for (Pokemon pk : allPokes) {
                if (pk != null) {
                    count++;
                    total += pk.bst();
                }
            }
            final double mean = total / count;
            
			copyUpEvolutionsHelper.apply(true, new BasePokemonAction() {
				public void applyTo(Pokemon pk) {
					pk.randomizeStatsWithinBST(random);
				}
			}, new EvolvedPokemonAction() {
				public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
					evTo.copyCompletelyRandomizedStatsUpEvolution(evFrom, random, mean);
				}
			});
        } else {
            for (Pokemon pk : allPokes) {
                if (pk != null) {
                    pk.randomizeStatsWithinBST(random);
                }
            }
        }
    }

	@Override
	public void shuffleAllPokemonBSTs(boolean evolutionSanity, boolean randomVariance) {
		List<Pokemon> allPokes = this.getMainPokemonList();

		if (evolutionSanity) {
			int count = 0;
			double total = 0.0;

			for (Pokemon pk : allPokes) {
				if (pk != null) {
					count++;
					total += pk.bst();
				}
			}
			final double mean = total / count;

			copyUpEvolutionsHelper.apply(true, new BasePokemonAction() {
				public void applyTo(Pokemon pk) {
					Pokemon swapWith = pickEvoPowerLvlReplacement(allPokes, pk);
					while (swapWith == null) {
						swapWith = allPokes.get(random.nextInt(allPokes.size()));
					}

					swapStatsRandom(pk, swapWith, random, randomVariance);
				}
			}, new EvolvedPokemonAction() {
				public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
					evTo.copyCompletelyRandomizedStatsUpEvolution(evFrom, random, mean);
				}
			});
		} else {
			for (Pokemon pk : allPokes) {
				if (pk != null) {
					Pokemon swapWith = pickEvoPowerLvlReplacement(allPokes, pk);
					while (swapWith == null) {
						swapWith = allPokes.get(random.nextInt(allPokes.size()));
					}

					swapStatsRandom(pk, swapWith, random, randomVariance);
				}
			}
		}
	}

    private void swapStatsRandom(Pokemon swapTo, Pokemon swapFrom, Random random,
            boolean randomize) {
        List<Integer> toStats = Arrays.asList(swapTo.hp, swapTo.attack, swapTo.defense,
                swapTo.speed, swapTo.spatk, swapTo.spdef, swapTo.special);
        List<Integer> fromStats = Arrays.asList(swapFrom.hp, swapFrom.attack, swapFrom.defense,
                swapFrom.speed, swapFrom.spatk, swapFrom.spdef, swapFrom.special);

        // Add slight variance, up to +- 10%
        double modifier = randomize ? 0.9 + (random.nextDouble() / 5) : 1;

        swapFrom.hp = (int) (toStats.get(0) * modifier);
        swapFrom.attack = (int) (toStats.get(1) * modifier);
        swapFrom.defense = (int) (toStats.get(2) * modifier);
        swapFrom.speed = (int) (toStats.get(3) * modifier);
        swapFrom.spatk = (int) (toStats.get(4) * modifier);
        swapFrom.spdef = (int) (toStats.get(5) * modifier);
        swapFrom.special = (int) (toStats.get(6) * modifier);

        modifier = randomize ? 0.9 + (random.nextDouble() / 5) : 1;

        swapTo.hp = (int) (fromStats.get(0) * modifier);
        swapTo.attack = (int) (fromStats.get(1) * modifier);
        swapTo.defense = (int) (fromStats.get(2) * modifier);
        swapTo.speed = (int) (fromStats.get(3) * modifier);
        swapTo.spatk = (int) (fromStats.get(4) * modifier);
        swapTo.spdef = (int) (fromStats.get(5) * modifier);
        swapTo.special = (int) (fromStats.get(6) * modifier);
    }

    @Override
    public void updatePokemonStats() {
        List<Pokemon> pokes = getPokemon();

        // non-special stat gen1 pokemon
        pokes.get(15).attack = 90; // BEEDRILL
        pokes.get(18).speed = 101; // PIDGEOT
        pokes.get(25).defense = 40; // PIKACHU
        pokes.get(26).speed = 110; // RAICHU
        pokes.get(31).attack = 92; // NIDOQUEEN
        pokes.get(34).attack = 102; // NIDOKING
        pokes.get(62).attack = 95; // POLIWRATH
        pokes.get(76).attack = 120; // GOLEM

        // behavior regarding special stat changes
        // depending on whether this is actually gen1 or not.
        if (generationOfPokemon() == 1) {
            // only update the pokemon who's updated stat was
            // equal to their Gen1 special stat.

            pokes.get(12).special = 90; // BUTTERFREE
            // skip PIKACHU s.def
            pokes.get(36).special = 95; // CLEFABLE
            // skip WIGGLYTUFF s.atk
            pokes.get(45).special = 110; // VILEPLUME
            // skip ALAKAZAM s.def
            // skip VICTREEBEL s.def
        } else {
            // do the special stat changes then move on from gen2 onwards

            pokes.get(12).spatk = 90; // BUTTERFREE
            pokes.get(25).spdef = 50; // PIKACHU
            pokes.get(36).spatk = 95; // CLEFABLE
            pokes.get(40).spatk = 85; // WIGGLYTUFF
            pokes.get(45).spatk = 110; // VILEPLUME
            pokes.get(65).spdef = 95; // ALAKAZAM
            pokes.get(71).spdef = 70; // VICTREEBEL

            // gen 2
            pokes.get(181).defense = 85; // AMPHAROS
            pokes.get(182).defense = 95; // BELLOSSOM
            pokes.get(184).spatk = 60; // AZUMARILL
            pokes.get(189).spdef = 95; // JUMPLUFF

            // gen 3
            if (generationOfPokemon() >= 3) {
                pokes.get(267).spatk = 100; // BEAUTIFLY
                pokes.get(295).spdef = 73; // EXPLOUD
            }

            // gen 4
            if (generationOfPokemon() >= 4) {
                pokes.get(398).spdef = 60; // STARAPTOR
                pokes.get(407).defense = 65; // ROSERADE
            }

            // gen 5
            if (generationOfPokemon() >= 5) {
                pokes.get(508).attack = 110; // STOUTLAND
                pokes.get(521).attack = 115; // UNFEZANT
                pokes.get(526).spdef = 80; // GIGALITH
                pokes.get(537).attack = 95; // SEISMITOAD
                pokes.get(542).spdef = 80; // LEAVANNY
                pokes.get(545).attack = 100; // SCOLIPEDE
                pokes.get(553).defense = 80; // KROOKODILE
            }
        }
    }

    public Pokemon randomPokemon() {
        checkPokemonRestrictions();
        return getMainPokemonList().get(this.random.nextInt(getMainPokemonList().size()));
    }

    @Override
    public Pokemon randomNonLegendaryPokemon() {
        checkPokemonRestrictions();
        return noLegendaryList.get(this.random.nextInt(noLegendaryList.size()));
    }

    @Override
    public Pokemon randomLegendaryPokemon() {
        checkPokemonRestrictions();
        return onlyLegendaryList.get(this.random.nextInt(onlyLegendaryList.size()));
    }

    @Override
    public Pokemon randomStarterPokemon(boolean noSplitEvos, boolean uniqueTypes, boolean baseOnly,
            int bstLimit, int minimumEvos, boolean exactEvos, Type[] starterType,
            Set<Type> mustInclude, Set<Type> cannotInclude) {
        if (starterPokes == null) {
            // Prepare the list
            starterPokes = new PokemonCollection(getMainPokemonList());
            if (exactEvos) {
                TemplateData.putData("logStarters",
                        "Exactly " + minimumEvos + " Evolution Starters");
            } else {
                TemplateData.putData("logStarters", minimumEvos + "+ Evolution Starters");
            }
            if (exactEvos) {
                starterPokes.filterByEvolutionStagesRemaining(minimumEvos + 1, minimumEvos + 1);
            } else if (minimumEvos > 0) {
                starterPokes.filterByEvolutionStagesRemaining(minimumEvos + 1, 999);
            }
            if (noSplitEvos) {
                starterPokes.filterByEvolutionCount(0, 1);
            }
            if (baseOnly) {
                starterPokes.filterByEvolutionStage(0, 0);
            }
            if (starterType != null && starterType.length > 0) {
                starterPokes.filterByType(starterType);
            }
            starterPokes.filterByPowerLevel(bstLimit);
            if (starterPokes.size() < 3 || (uniqueTypes && starterPokes.uniquePokes() < 3)) {
                throw new RandomizationException(
                        "ERROR: Not enough starter choices available. Please "
                                + "reduce the filtering and try again.");
            }
        }
        try {
            return starterPokes.randomPokemonTypeRestricted(
                    Optional.ofNullable(mustInclude).orElse(Collections.EMPTY_SET),
                    Optional.ofNullable(cannotInclude).orElse(Collections.EMPTY_SET), this.random,
                    true);
        } catch (Exception exc) {
            throw new RandomizationException(
                    "ERROR: Valid list of starters has been consumed. Please "
                            + "reduce the filtering and try again.");
        }
    }

    @Override
    public List<Type> getTypesInGame() {
        if (typeInGame == null) {
            typeInGame = Arrays.asList(Type.values()).stream().filter(t -> isTypeInGame(t))
                    .collect(Collectors.toList());
        }
        return typeInGame;
    }

    @Override
    public Type randomType() {
        return getTypesInGame().get(this.random.nextInt(getTypesInGame().size()));
    }

    @Override
    public int randomMove(Pokemon pk) {
        return this.getMovesLearnt().get(pk)
                .get(this.random.nextInt(this.getMovesLearnt().get(pk).size())).move;
    }

	@Override
	public void randomizePokemonTypes(boolean evolutionSanity) {
		copyUpEvolutionsHelper.apply(evolutionSanity, new BasePokemonAction() {

			public void applyTo(Pokemon pk) {
				// Negate dontcopypokes for split evos
				if (pk.evolutionsTo.size() > 0) {
					pk.temporaryFlag = false;
					return;
				}
				// Step 1: Basic or Excluded From Copying Pokemon
				// A Basic/EFC pokemon has a 35% chance of a second type if
				// it has an evolution that copies type/stats, a 50% chance
				// otherwise
				pk.primaryType = randomType();
				pk.secondaryType = null;
				if (pk.evolutionsFrom.size() == 1 && pk.evolutionsFrom.get(0).carryStats) {
					if (random.nextDouble() < 0.35) {
						pk.secondaryType = randomType();
						while (pk.secondaryType == pk.primaryType) {
							pk.secondaryType = randomType();
						}
					}
				} else {
					if (random.nextDouble() < 0.5) {
						pk.secondaryType = randomType();
						while (pk.secondaryType == pk.primaryType) {
							pk.secondaryType = randomType();
						}
					}
				}
			}

		}, new EvolvedPokemonAction() {

			public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
				// Special case for Eevee
				if (evFrom.number != 133) {
					evTo.primaryType = evFrom.primaryType;
					evTo.secondaryType = evFrom.secondaryType;
				} else {
					Set<Type> typeList = new HashSet<Type>();
					typeList.add(evFrom.primaryType);
					typeList.add(evFrom.secondaryType);
					for (Evolution e : evFrom.evolutionsFrom) {
						if (e.to != evTo) {
							typeList.add(e.to.primaryType);
						}
						typeList.add(e.to.secondaryType);
					}
					List<Type> typeList2 = getTypesInGame().stream().filter(t -> !typeList.contains(t))
							.collect(Collectors.toList());
					evTo.primaryType = typeList2.get(random.nextInt(typeList2.size() - 1));
				}

				if (evTo.secondaryType == null) {
					double chance = toMonIsFinalEvo ? 0.25 : 0.15;
					if (random.nextDouble() < chance) {
						evTo.secondaryType = randomType();
						while (evTo.secondaryType == evTo.primaryType) {
							evTo.secondaryType = randomType();
						}
					}
				}
			}

		}, new BasePokemonAction() {

			@Override
			public void applyTo(Pokemon pk) {
				pk.secondaryType = randomType();
				while (pk.secondaryType == pk.primaryType) {
					pk.secondaryType = randomType();
				}
			}

		});
	}

    @Override
    public void shufflePokemonTypes() {
        List<Pokemon> allPokes = this.getMainPokemonList();
        List<Type> allTypes = new ArrayList<Type>(getTypesInGame());
        Collections.shuffle(allTypes, this.random);
        Type.setShuffledList(allTypes);

        for (Pokemon pkmn : allPokes) {
            if (pkmn != null) {
                int ordinal = pkmn.primaryType.ordinal();
                // Give ordinal value if possible
                // Otherwise give it the last type
                if (ordinal < allTypes.size()) {
                    pkmn.primaryType = allTypes.get(ordinal);
                } else {
                    pkmn.primaryType = allTypes.get(allTypes.size() - 1);
                }
                if (pkmn.secondaryType != null) {
                    ordinal = pkmn.secondaryType.ordinal();
                    if (ordinal < allTypes.size()) {
                        pkmn.secondaryType = allTypes.get(ordinal);
                    } else {
                        pkmn.secondaryType = allTypes.get(allTypes.size() - 1);
                    }

                }
            }
        }
        TemplateData.putData("typeList", getTypesInGame());
        TemplateData.putData("shuffledTypes", Type.getShuffledList());
    }

	@Override
	public void randomizeRetainPokemonTypes(boolean evolutionSanity) {
		copyUpEvolutionsHelper.apply(evolutionSanity, new BasePokemonAction() {
			
			public void applyTo(Pokemon pk) {
				// Negate dontcopypokes for split evos
				if (pk.evolutionsTo.size() > 0) {
					pk.temporaryFlag = false;
					return;
				}
				pk.typeChanged = 1; // default to PrimaryType
				// Replace secondary type
				if (pk.secondaryType != null && random.nextDouble() < 0.5) {
					pk.typeChanged = 2;// SecondaryType
					pk.secondaryType = randomType();
				}
				// Replace primary type
				else {
					pk.primaryType = randomType();
				}
				while (pk.primaryType == pk.secondaryType) {
					pk.primaryType = pk.typeChanged == 1 ? randomType() : pk.primaryType;
					pk.secondaryType = pk.typeChanged == 2 ? randomType() : pk.secondaryType;
				}
			}
			
		}, new EvolvedPokemonAction() {
			
			public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
				// Type difference in evos (ex: Scyther into Scizor)
				if (evTo.evolutionsTo.size() == 1) {
					// Special case for Eevee
					if (evFrom.number == 133) {
						Set<Type> typeList = new HashSet<Type>();
						typeList.add(evFrom.primaryType);
						typeList.add(evFrom.secondaryType);
						for (Evolution e : evFrom.evolutionsFrom) {
							if (e.to != evTo) {
								typeList.add(e.to.primaryType);
							}
							typeList.add(e.to.secondaryType);
						}
						List<Type> typeList2 = getTypesInGame().stream().filter(t -> !typeList.contains(t))
								.collect(Collectors.toList());
						evTo.primaryType = typeList2.get(random.nextInt(typeList2.size() - 1));
						return;
					}
					// All other cases
					evTo.assignTypeByReference(evFrom, evTo.evolutionsTo.get(0).typesDiffer, () -> randomType());
				} else {
					// Multiple evo paths merge here
					// similar indicates which type all evo paths share
					// evoDiff indicates which type evFrom and evTo share
					int similar = -1;
					int evoDiff = -1;
					for (Evolution e : evTo.evolutionsTo) {
						// Initialize
						if (similar < 0) {
							similar = e.typesDiffer;
							evoDiff = e.from == evFrom ? e.typesDiffer : -1;
							continue;
						}

						// Get the similar type
						switch (e.typesDiffer) {
						// evTo PrimaryType is different
						case 1:
						case 3:
							similar = (similar & 1) != 0 || similar == 0 ? 1 : -1;
							break;
						// evTo SecondaryType is different
						case 2:
						case 4:
							similar = (similar & 1) == 0 ? 2 : -1;
							break;
						// evTo is exactly same types, no effect
						case 0:
							break;
						}

						// No similarity, end the loop
						if (similar == -1) {
							break;
						}

						evoDiff = e.from == evFrom ? e.typesDiffer : evoDiff;
					}
					// Only one type is similar = keep that one type
					if (similar > 0 && similar < 3) {
						evTo.typeChanged = (similar & 1) == 0 ? 2 : 1;
						do {
							// If we're changing the first type, check which type of
							// the prior evo we should copy
							evTo.primaryType = evTo.typeChanged == 1
									? evoDiff == 2 || evFrom.secondaryType == null ? evFrom.primaryType
											: evFrom.secondaryType
									: evTo.primaryType;
							// If we're changing the second type, check which type
							// of the prior evo we should copy
							evTo.secondaryType = evTo.typeChanged == 2
									? evoDiff == 1 ? evFrom.secondaryType : evFrom.primaryType
									: evTo.secondaryType;

							if (evTo.primaryType == evTo.secondaryType) {
								evTo.primaryType = evTo.typeChanged == 1 ? randomType() : evTo.primaryType;
								evTo.secondaryType = evTo.typeChanged == 2 ? randomType() : evTo.secondaryType;
							}
						} while (evTo.primaryType == evTo.secondaryType);
					}
					// All/Nothing is similar = default type select
					else {
						evTo.typeChanged = 1; // default to PrimaryType
						// Replace secondary type
						if (evTo.secondaryType != null && random.nextDouble() < 0.5) {
							evTo.typeChanged = 2; // SecondaryType
							evTo.secondaryType = randomType();
						}
						// Replace primary type
						else {
							evTo.primaryType = randomType();
						}
						while (evTo.primaryType == evTo.secondaryType) {
							evTo.primaryType = evTo.typeChanged == 1 ? randomType() : evTo.primaryType;
							evTo.secondaryType = evTo.typeChanged == 2 ? randomType() : evTo.secondaryType;
						}
					}
				}
				
			}
		}, new BasePokemonAction() {

			// TODO: consider merging with the other BasePokemonAction, where it evolves;
			// they are very similar
			public void applyTo(Pokemon pk) {
				pk.typeChanged = 1; // default to PrimaryType
				// Replace secondary type
				if (pk.secondaryType != null && random.nextDouble() < 0.5) {
					pk.typeChanged = 2; // SecondaryType
					pk.secondaryType = randomType();
				}
				// Replace primary type
				else {
					pk.primaryType = randomType();
				}
				while (pk.primaryType == pk.secondaryType) {
					pk.primaryType = pk.typeChanged == 1 ? randomType() : pk.primaryType;
					pk.secondaryType = pk.typeChanged == 2 ? randomType() : pk.secondaryType;
				}
			}

		});
	}

    @Override
    public void randomizeAbilities(boolean evolutionSanity, boolean allowWonderGuard,
            boolean banTrappingAbilities, boolean banNegativeAbilities) {
        // Abilities don't exist in some games...
        if (this.abilitiesPerPokemon() == 0) {
            return;
        }

        final boolean hasDWAbilities = (this.abilitiesPerPokemon() == 3);

        final List<Integer> bannedAbilities = new ArrayList<Integer>();

        if (!allowWonderGuard) {
            bannedAbilities.add(GlobalConstants.WONDER_GUARD_INDEX);
        }

        if (banTrappingAbilities) {
            bannedAbilities.addAll(GlobalConstants.battleTrappingAbilities);
        }

        if (banNegativeAbilities) {
            bannedAbilities.addAll(GlobalConstants.negativeAbilities);
        }

        final int maxAbility = this.highestAbilityIndex();

		copyUpEvolutionsHelper.apply(evolutionSanity, new BasePokemonAction() {
			public void applyTo(Pokemon pk) {
				
				if (pk.ability1 != GlobalConstants.WONDER_GUARD_INDEX
						&& pk.ability2 != GlobalConstants.WONDER_GUARD_INDEX
						&& pk.ability3 != GlobalConstants.WONDER_GUARD_INDEX) {
					// Pick first ability
					pk.ability1 = pickRandomAbility(maxAbility, bannedAbilities);

					// Second ability?
					if (random.nextDouble() < 0.5) {
						// Yes, second ability
						pk.ability2 = pickRandomAbility(maxAbility, bannedAbilities, pk.ability1);
					} else {
						// Nope
						pk.ability2 = 0;
					}

					// Third ability?
					if (hasDWAbilities) {
						pk.ability3 = pickRandomAbility(maxAbility, bannedAbilities, pk.ability1, pk.ability2);
					}
				}
				
			}
		}, new EvolvedPokemonAction() {
			
			public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
				if (evTo.ability1 != GlobalConstants.WONDER_GUARD_INDEX
						&& evTo.ability2 != GlobalConstants.WONDER_GUARD_INDEX
						&& evTo.ability3 != GlobalConstants.WONDER_GUARD_INDEX) {
					evTo.ability1 = evFrom.ability1;
					evTo.ability2 = evFrom.ability2;
					evTo.ability3 = evFrom.ability3;
				}
			}
			
		});
	}

    private int pickRandomAbility(int maxAbility, List<Integer> bannedAbilities,
            int... alreadySetAbilities) {
        int newAbility = 0;

        while (true) {
            newAbility = this.random.nextInt(maxAbility) + 1;

            if (bannedAbilities.contains(newAbility)) {
                continue;
            }

            boolean repeat = false;
            for (int i = 0; i < alreadySetAbilities.length; i++) {
                if (alreadySetAbilities[i] == newAbility) {
                    repeat = true;
                    break;
                }
            }

            if (repeat) {
                continue;
            } else {
                break;
            }
        }

        return newAbility;
    }

    @Override
    public void randomEncounters(boolean useTimeOfDay, boolean catchEmAll, boolean typeThemed,
            boolean usePowerLevels, boolean matchTypingDistribution, boolean noLegendaries,
            boolean allowLowLevelEvolvedTypes) {
        checkPokemonRestrictions();
        List<EncounterSet> currentEncounters = this.getEncounters(useTimeOfDay);

        // New: randomize the order encounter sets are randomized in.
        // Leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<EncounterSet> scrambledEncounters = new ArrayList<EncounterSet>(currentEncounters);
        Collections.shuffle(scrambledEncounters, this.random);

        PokemonCollection globalSet = new PokemonCollection(getMainPokemonList())
                .filterList(this.bannedForWildEncounters());
        if (noLegendaries) {
            globalSet.filterLegendaries();
        }
        for (EncounterSet area : scrambledEncounters) {
            PokemonCollection areaSet = new PokemonCollection(globalSet)
                    .filterSet(area.bannedPokemon)
                    .filterByMinimumLevel(allowLowLevelEvolvedTypes ? 100 : area.maximumLevel());
            if (areaSet.size() == 0) {
                throw new RandomizationException("ERROR: Current bans and level requirements "
                        + "have lead to no available pokemon. Please reduce the filtering and try again.");
            }
            if (catchEmAll) {
                PokemonCollection workingSet = new PokemonCollection(areaSet);
                for (Encounter enc : area.encounters) {
                    enc.pokemon = workingSet.randomPokemon(random, true);
                    if (workingSet.size() == 0) {
                        workingSet = new PokemonCollection(areaSet);
                    }
                }
            } else if (typeThemed) {
                Type areaTheme = areaSet.randomType(random);
                for (Encounter enc : area.encounters) {
                    // Pick a random themed pokemon
                    enc.pokemon = areaSet.randomPokemonOfType(areaTheme, random, false);
                }
            } else if (usePowerLevels) {
                for (Encounter enc : area.encounters) {
                    enc.pokemon =
                            areaSet.randomPokemonByPowerLevel(enc.pokemon, false, random, false);
                }
            } else if (matchTypingDistribution) {
                for (Encounter enc : area.encounters) {
                    enc.pokemon = areaSet.randomPokemonTypeWeighted(random, false);
                }
            } else {
                for (Encounter enc : area.encounters) {
                    enc.pokemon = areaSet.randomPokemon(random, false);
                }
            }
        }

        setEncounters(useTimeOfDay, currentEncounters);
    }

    @Override
    public void area1to1Encounters(boolean useTimeOfDay, boolean catchEmAll, boolean typeThemed,
            boolean usePowerLevels, boolean matchTypingDistribution, boolean noLegendaries,
            boolean allowLowLevelEvolvedTypes) {
        checkPokemonRestrictions();
        List<EncounterSet> currentEncounters = this.getEncounters(useTimeOfDay);

        // New: randomize the order encounter sets are randomized in.
        // Leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<EncounterSet> scrambledEncounters = new ArrayList<EncounterSet>(currentEncounters);
        Collections.shuffle(scrambledEncounters, this.random);

        PokemonCollection globalSet = new PokemonCollection(getMainPokemonList())
                .filterList(this.bannedForWildEncounters());
        if (noLegendaries) {
            globalSet.filterLegendaries();
        }
        for (EncounterSet area : scrambledEncounters) {
            PokemonCollection areaSet = new PokemonCollection(globalSet)
                    .filterSet(area.bannedPokemon)
                    .filterByMinimumLevel(allowLowLevelEvolvedTypes ? 100 : area.maximumLevel());
            if (areaSet.size() == 0) {
                throw new RandomizationException("ERROR: Current bans and level requirements "
                        + "have lead to no available pokemon. Please reduce the filtering and try again.");
            }
            Set<Pokemon> inArea = pokemonInArea(area);
            Map<Pokemon, Pokemon> areaMap = new TreeMap<Pokemon, Pokemon>();
            if (catchEmAll) {
                PokemonCollection workingSet = null;
                for (Pokemon areaPk : inArea) {
                    if (workingSet == null || workingSet.size() == 0) {
                        workingSet = new PokemonCollection(areaSet);
                    }

                    Pokemon picked = workingSet.randomPokemon(random, true);
                    areaMap.put(areaPk, picked);
                }
            } else if (typeThemed) {
                Type areaTheme = areaSet.randomType(inArea.size(), random);
                for (Pokemon areaPk : inArea) {
                    Pokemon picked = areaSet.randomPokemonOfType(areaTheme, random, true);
                    areaMap.put(areaPk, picked);
                }
            } else if (usePowerLevels) {
                for (Pokemon areaPk : inArea) {
                    Pokemon picked = areaSet.randomPokemonByPowerLevel(areaPk, false, random, true);
                    areaMap.put(areaPk, picked);
                }
            } else if (matchTypingDistribution) {
                for (Pokemon areaPk : inArea) {
                    Pokemon picked = areaSet.randomPokemonTypeWeighted(random, true);
                    areaMap.put(areaPk, picked);
                }
            } else {
                for (Pokemon areaPk : inArea) {
                    Pokemon picked = areaSet.randomPokemon(random, true);
                    areaMap.put(areaPk, picked);
                }
            }

            for (Encounter enc : area.encounters) {
                // Apply the map
                enc.pokemon = areaMap.get(enc.pokemon);
            }
        }

        setEncounters(useTimeOfDay, currentEncounters);
    }

    @Override
    public void game1to1Encounters(boolean useTimeOfDay, boolean usePowerLevels,
            boolean noLegendaries) {
        checkPokemonRestrictions();

        List<Pokemon> banned = this.bannedForWildEncounters();
        PokemonCollection globalSet =
                new PokemonCollection(getMainPokemonList()).filterList(banned);
        if (noLegendaries) {
            globalSet.filterLegendaries();
        }

        // Build the full 1-to-1 map
        Map<Pokemon, Pokemon> wildTranslateMap = new HashMap<Pokemon, Pokemon>(getPokemon().size());
        PokemonCollection fromSet =
                new PokemonCollection(allPokemonWithoutNull()).filterList(banned);
        PokemonCollection toSet = null;

        // Banned pokemon should be mapped to themselves
        for (Pokemon pk : banned) {
            wildTranslateMap.put(pk, pk);
        }

        while (fromSet.size() > 0) {
            if (toSet == null || toSet.size() == 0) {
                toSet = new PokemonCollection(globalSet);
            }

            Pokemon from = fromSet.randomPokemon(random, true);
            Pokemon to = toSet.randomPokemon(random, false);

            if (usePowerLevels) {
                if (toSet.size() != 1) {
                    to = toSet.randomPokemonByPowerLevel(from, true, random, false);
                }
            } else {
                while (from == to && toSet.size() > 1) {
                    // Reroll for a different pokemon if at all possible
                    to = toSet.randomPokemon(random, false);
                }
            }

            toSet.remove(to);
            wildTranslateMap.put(from, to);
        }

        List<EncounterSet> currentEncounters = this.getEncounters(useTimeOfDay);

        for (EncounterSet area : currentEncounters) {
            for (Encounter enc : area.encounters) {
                // Apply the map
                enc.pokemon = wildTranslateMap.get(enc.pokemon);
                if (area.bannedPokemon.contains(enc.pokemon)) {
                    // Ignore the map and put a random non-banned poke
                    PokemonCollection areaSet =
                            new PokemonCollection(globalSet).filterSet(area.bannedPokemon);
                    if (areaSet.size() == 0) {
                        throw new RandomizationException("ERROR: Current bans have lead "
                                + "to no available pokemon. Please reduce the filtering and try again.");
                    }
                    if (usePowerLevels) {
                        enc.pokemon = areaSet.randomPokemonByPowerLevel(enc.pokemon, false, random,
                                false);
                    } else {
                        enc.pokemon = areaSet.randomPokemon(random, false);
                    }
                }
            }
        }

        setEncounters(useTimeOfDay, currentEncounters);

    }

    @Override
    public ItemList getTrainerItems() {
        return null;
    }

    private void applyChangeToTrainerPoke(TrainerPokemon tp, boolean randomHeldItem,
            int levelModifier) {
        if (randomHeldItem) {
            tp.heldItem = this.getTrainerItems().randomItem(this.random);
        }
        if (levelModifier != 0) {
            tp.level = Math.min(100, (int) Math.round(tp.level * (1 + levelModifier / 100.0)));
        }
    }

    @Override
    public void modifyTrainerPokes(boolean randomHeldItem, int levelModifier) {
        checkPokemonRestrictions();
        List<Trainer> currentTrainers = this.getTrainers();
        for (Trainer t : currentTrainers) {
            for (TrainerPokemon tp : t.getPokemon()) {
                // NOTE: we do not reset moves here since we don't want to
                // overwrite custom movesets
                applyChangeToTrainerPoke(tp, randomHeldItem, levelModifier);
            }
        }
        this.setTrainers(currentTrainers);
    }

    @Override
    public void randomizeTrainerPokes(boolean usePowerLevels, boolean weightByFrequency,
            boolean noLegendaries, boolean noEarlyWonderGuard, boolean useResistantType,
            boolean typeTheme, boolean globalSwap, boolean gymTypeTheme, boolean randomHeldItem,
            boolean buffElite, int levelModifier) {

        checkPokemonRestrictions();
        if (globalSwap) {
            initializeTrainerGlobalSwapMap();
        }

        List<Trainer> currentTrainers = this.getTrainers();

        // New: randomize the order trainers are randomized in.
        // Leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<Trainer> scrambledTrainers = new ArrayList<Trainer>(currentTrainers);
        Collections.shuffle(scrambledTrainers, this.random);

        cachedReplacementLists = new PokemonCollection();
        cachedReplacementLists.addAll(noLegendaries ? noLegendaryList : getMainPokemonList());
        cachedEliteReplacementLists = new PokemonCollection();
        cachedEliteReplacementLists.addAll(getMainPokemonList().stream()
                .filter(pk -> pk.isBigPoke(isGen1()) || pk.isLegendary())
                .collect(Collectors.toList()));

        // Attempt to type theme tagged trainers. If gym type theming and type theming are false,
        // set this to empty
        Set<Trainer> assignedTrainers = gymTypeTheme || typeTheme
                ? assignTaggedTrainerTypeThemes(scrambledTrainers, weightByFrequency, noLegendaries,
                        noEarlyWonderGuard, usePowerLevels, useResistantType, randomHeldItem,
                        buffElite, levelModifier)
                : Collections.emptySet();

        // Fully random is easy enough - randomize then worry about rival
        // carrying starter at the end
        for (Trainer t : scrambledTrainers) {
            if (t.getTag() != null && t.getTag().equals("IRIVAL")) {
                continue; // skip
            }

            if (!assignedTrainers.contains(t)) {
                if (buffElite && t.getTag() != null && (t.getTag().startsWith("ELITE")
                        || t.getTag().startsWith("CHAMPION") || t.getTag().startsWith("UBER"))) {
                    for (TrainerPokemon tp : t.getPokemon()) {
                        tp.pokemon = pickEliteReplacement(tp.pokemon, null);
                        tp.resetMoves = true;
                        applyChangeToTrainerPoke(tp, randomHeldItem, levelModifier);
                    }
                } else {
                    // If type theming is true, pick a type. Otherwise null skips the type
                    // restriction
                    Type typeForTrainer = typeTheme
                            ? weightByFrequency ? cachedReplacementLists.randomTypeWeighted(random)
                                    : cachedReplacementLists.randomType(random)
                            : null;
                    for (TrainerPokemon tp : t.getPokemon()) {
                        boolean shedAllowed = (!noEarlyWonderGuard) || tp.level >= 20;
                        tp.pokemon = pickReplacement(tp.pokemon, usePowerLevels, typeForTrainer,
                                noLegendaries, shedAllowed);
                        tp.resetMoves = true;
                        applyChangeToTrainerPoke(tp, randomHeldItem, levelModifier);
                    }
                }
            }
        }

        // Save it all up
        this.setTrainers(currentTrainers);
        this.modifyTrainerText(gymTypeTheme ? groupTypesMap : null);
    }

    // Type theme each tagged trainer
    private Set<Trainer> assignTaggedTrainerTypeThemes(List<Trainer> currentTrainers,
            boolean weightByFrequency, boolean noLegendaries, boolean noEarlyWonderGuard,
            boolean usePowerLevels, boolean useResistantType, boolean randomHeldItem,
            boolean buffElite, int levelModifier) {
        // Construct groupings for types
        Set<Trainer> assignedTrainers = new TreeSet<Trainer>();
        Map<String, List<Trainer>> groups = new TreeMap<String, List<Trainer>>();
        for (Trainer t : currentTrainers) {
            if (t.getTag() != null && t.getTag().equals("IRIVAL")) {
                continue; // skip
            }
            String group = t.getTag() == null ? "" : t.getTag();
            if (group.contains("-")) {
                group = group.substring(0, group.indexOf('-'));
            }
            // We use startsWith since most groups are numbers (i.e GYM1, GYM2)
            if (group.startsWith("GYM") || group.startsWith("ELITE") || group.startsWith("CHAMPION")
                    || group.startsWith("UBER") || group.startsWith("THEMED")
                    || group.startsWith("CILAN") || group.startsWith("CHILI")
                    || group.startsWith("CRESS")) {
                // Yep this is a group
                if (!groups.containsKey(group)) {
                    groups.put(group, new ArrayList<Trainer>());
                }
                groups.get(group).add(t);
                assignedTrainers.add(t);
            } else if (group.startsWith("GIO")) {
                // Giovanni has same grouping as his gym, gym 8
                if (!groups.containsKey("GYM8")) {
                    groups.put("GYM8", new ArrayList<Trainer>());
                }
                groups.get("GYM8").add(t);
                assignedTrainers.add(t);
            }
        }

        // Give a type to each group
        // Gym & elite types have to be unique
        // So do uber types, including the type we pick for champion
        Set<Type> usedGymTypes = new TreeSet<Type>();
        Set<Type> usedEliteTypes = new TreeSet<Type>();
        Set<Type> usedUberTypes = new TreeSet<Type>();
        for (String group : groups.keySet()) {
            List<Trainer> trainersInGroup = groups.get(group);
            // Shuffle ordering within group to promote randomness
            Collections.shuffle(trainersInGroup, random);
            Type typeForGroup =
                    weightByFrequency ? cachedReplacementLists.randomTypeWeighted(random)
                            : cachedReplacementLists.randomType(random);
            if (group.startsWith("GYM")) {
                if (usedGymTypes.size() == cachedReplacementLists.typeCount()) {
                    throw new RandomizationException(
                            "Not enough types of Pokemon available for gyms. Please reduce filtering and try again.");
                }
                while (usedGymTypes.contains(typeForGroup)) {
                    typeForGroup =
                            weightByFrequency ? cachedReplacementLists.randomTypeWeighted(random)
                                    : cachedReplacementLists.randomType(random);
                }
                usedGymTypes.add(typeForGroup);
            }
            if (group.startsWith("ELITE")) {
                PokemonCollection toUse =
                        buffElite ? cachedEliteReplacementLists : cachedReplacementLists;
                if (usedEliteTypes.size() == toUse.typeCount()) {
                    throw new RandomizationException(
                            "Not enough types of Pokemon available for Elites. Please reduce filtering and try again.");
                }
                while (usedEliteTypes.contains(typeForGroup)
                        || toUse.sizeByType(typeForGroup) == 0) {
                    typeForGroup = weightByFrequency ? toUse.randomTypeWeighted(random)
                            : toUse.randomType(random);
                }
                usedEliteTypes.add(typeForGroup);
            }
            if (group.equals("CHAMPION") || group.startsWith("UBER")) {
                PokemonCollection toUse =
                        buffElite ? cachedEliteReplacementLists : cachedReplacementLists;
                if (usedUberTypes.size() == toUse.typeCount()) {
                    throw new RandomizationException(
                            "Not enough types of Pokemon available for Champion/Uber. Please reduce filtering and try again.");
                }
                while (usedUberTypes.contains(typeForGroup)
                        || (toUse.sizeByType(typeForGroup) == 0)) {
                    typeForGroup = weightByFrequency ? toUse.randomTypeWeighted(random)
                            : toUse.randomType(random);
                }
                usedUberTypes.add(typeForGroup);
            }
            // Assign new teams to themed trainers
            for (Trainer t : trainersInGroup) {
                if (buffElite && (t.getTag().startsWith("ELITE")
                        || t.getTag().startsWith("CHAMPION") || t.getTag().startsWith("UBER"))) {
                    for (TrainerPokemon tp : t.getPokemon()) {
                        tp.pokemon = pickEliteReplacement(tp.pokemon, typeForGroup);
                        tp.resetMoves = true;
                        applyChangeToTrainerPoke(tp, randomHeldItem, levelModifier);
                    }
                } else {
                    for (TrainerPokemon tp : t.getPokemon()) {
                        boolean wgAllowed = (!noEarlyWonderGuard) || tp.level >= 20;
                        tp.pokemon = pickReplacement(tp.pokemon, usePowerLevels, typeForGroup,
                                noLegendaries, wgAllowed);
                        tp.resetMoves = true;
                        applyChangeToTrainerPoke(tp, randomHeldItem, levelModifier);
                    }
                }
            }
            groupTypesMap.put(group, typeForGroup);
        }

        // Now that GYM1 has a group, we can assign Cilan, Chili, and Cress
        for (String group : new String[] {"CILAN", "CHILI", "CRESS"}) {
            List<Trainer> trainersInGroup = groups.get(group);

            // If any list is null, break this loop
            if (trainersInGroup == null || trainersInGroup.size() < 1) {
                break;
            }

            // Shuffle ordering within group to promote randomness
            Collections.shuffle(trainersInGroup, random);
            for (Trainer t : trainersInGroup) {
                for (int i = 0; i < t.getPokemon().size(); i++) {
                    TrainerPokemon tp = t.getPokemon().get(i);
                    boolean wgAllowed = (!noEarlyWonderGuard) || tp.level >= 20;
                    // This is the last pokemon. Use the superior type
                    if (i == t.getPokemon().size() - 1) {
                        // Get the starters
                        // starter order 0 1 2 => leader to fight CHILI CRESS CILAN
                        Type superiorType;
                        List<Pokemon> starters = this.getStarters();
                        if (group.equals("CHILI")) {
                            superiorType =
                                    starters.get(0).getRandomWeakness(random, useResistantType);
                        } else if (group.equals("CRESS")) {
                            superiorType =
                                    starters.get(1).getRandomWeakness(random, useResistantType);
                        } else {
                            superiorType =
                                    starters.get(2).getRandomWeakness(random, useResistantType);

                        }
                        tp.pokemon = pickReplacement(tp.pokemon, usePowerLevels, superiorType,
                                noLegendaries, wgAllowed);
                        groupTypesMap.put(group, superiorType);
                    }
                    // The rest of the team is equal to GYM1
                    else {
                        tp.pokemon = pickReplacement(tp.pokemon, usePowerLevels,
                                groupTypesMap.get("GYM1"), noLegendaries, wgAllowed);
                    }
                    tp.resetMoves = true;
                    applyChangeToTrainerPoke(tp, randomHeldItem, levelModifier);
                }
            }
        }

        return assignedTrainers;
    }

    private Pokemon pickEliteReplacement(Pokemon current, Type type) {
        List<Pokemon> pickFrom = cachedEliteReplacementLists.getPokes();
        Pokemon chosenPoke = null;

        if (trainerTranslateMap != null && trainerTranslateMap.containsKey(current)) {
            chosenPoke = trainerTranslateMap.get(current);
            // Stats and type take priority over global swap
            if ((type == null || chosenPoke.primaryType == type || chosenPoke.secondaryType == type)
                    && (chosenPoke.isBigPoke(isGen1()) || chosenPoke.isLegendary())) {
                return chosenPoke;
            }
        }

        if (type != null) {
            pickFrom = cachedEliteReplacementLists.getPokesByType().get(type);
        }

        chosenPoke = pickFrom.get(this.random.nextInt(pickFrom.size()));

        // Don't override original selections
        // Prevent conflicts from replacing global swap
        if (trainerTranslateMap != null && !trainerTranslateMap.containsKey(current)) {
            trainerTranslateMap.put(current, chosenPoke);
        }

        return chosenPoke;
    }

    @Override
    public void rivalCarriesStarter(boolean noLegendaries) {
        checkPokemonRestrictions();
        List<Trainer> currentTrainers = this.getTrainers();
        if (cachedReplacementLists == null) {
            cachedReplacementLists = new PokemonCollection();
            cachedReplacementLists.addAll(noLegendaries ? noLegendaryList : getMainPokemonList());
        }
        rivalCarriesStarterUpdate(currentTrainers, "RIVAL", 1);
        rivalCarriesStarterUpdate(currentTrainers, "FRIEND", 2);
        this.setTrainers(currentTrainers);
    }

    @Override
    public void rivalCarriesTeam() {
        checkPokemonRestrictions();
        List<Trainer> currentTrainers = this.getTrainers();
        rivalCarriesTeamUpdate(currentTrainers, "RIVAL", 1);
        rivalCarriesTeamUpdate(currentTrainers, "FRIEND", 2);
        this.setTrainers(currentTrainers);
    }

    @Override
    public void forceFullyEvolvedTrainerPokes(int minLevel) {
        checkPokemonRestrictions();
        List<Trainer> currentTrainers = this.getTrainers();
        for (Trainer t : currentTrainers) {
            for (TrainerPokemon tp : t.getPokemon()) {
                if (tp.level >= minLevel) {
                    Pokemon newPokemon = fullyEvolve(tp.pokemon);
                    if (newPokemon != tp.pokemon) {
                        tp.pokemon = newPokemon;
                        tp.resetMoves = true;
                    }
                }
            }
        }
        this.setTrainers(currentTrainers);
    }

    // This is left empty so that subclasses can define if any text needs
    // to be modified based on the new gym types
    @Override
    public void modifyTrainerText(Map taggedTypes) {}

    // MOVE DATA
    // All randomizers don't touch move ID 165 (Struggle)
    // They also have other exclusions where necessary to stop things glitching.
    @Override
    public void randomizeMovePowers() {
        List<Move> moves = this.getMoves();
        for (Move mv : moves) {
            if (mv != null && mv.internalId != 165 && mv.power >= 10) {
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
            if (mv != null && mv.internalId != 165) {
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
            if (mv != null && mv.internalId != 165 && mv.hitratio >= 5) {
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
            if (mv != null && mv.internalId != 165 && mv.type != null) {
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
            if (mv != null && mv.internalId != 165 && mv.category != MoveCategory.STATUS) {
                if (random.nextInt(2) == 0) {
                    mv.category = (mv.category == MoveCategory.PHYSICAL) ? MoveCategory.SPECIAL
                            : MoveCategory.PHYSICAL;
                }
            }
        }

    }

    @Override
    public void updateMovesToGen5() {
        List<Move> moves = this.getMoves();

        // gen1
        // Karate Chop => FIGHTING (gen1)
        updateMoveType(moves, 2, Type.FIGHTING);
        // Razor Wind => 100% accuracy (gen1/2)
        updateMoveAccuracy(moves, 13, 100);
        // Gust => FLYING (gen1)
        updateMoveType(moves, 16, Type.FLYING);
        // Wing Attack => 60 power (gen1)
        updateMovePower(moves, 17, 60);
        // Whirlwind => 100 accuracy
        updateMoveAccuracy(moves, 18, 100);
        // Fly => 90 power (gen1/2/3)
        updateMovePower(moves, 19, 90);
        // Bind => 85% accuracy (gen1-4)
        updateMoveAccuracy(moves, 20, 85);
        // Vine Whip => 15 pp (gen1/2/3)
        updateMovePP(moves, 22, 15);
        // Jump Kick => 10 pp, 100 power (gen1-4)
        updateMovePP(moves, 26, 10);
        updateMovePower(moves, 26, 100);
        // Sand Attack => GROUND (gen1)
        updateMoveType(moves, 28, Type.GROUND);
        // Tackle => 50 power, 100% accuracy , gen1-4
        updateMovePower(moves, 33, 50);
        updateMoveAccuracy(moves, 33, 100);
        // Wrap => 90% accuracy (gen1-4)
        updateMoveAccuracy(moves, 35, 90);
        // Thrash => 120 power, 10pp (gen1-4)
        updateMovePP(moves, 37, 10);
        updateMovePower(moves, 37, 120);
        // Double-Edge => 120 power (gen1)
        updateMovePower(moves, 38, 120);
        // Move 44, Bite, becomes dark (but doesn't exist anyway)
        // Disable => 100% accuracy (gen1-4)
        updateMoveAccuracy(moves, 50, 100);
        // Blizzard => 70% accuracy (gen1)
        updateMoveAccuracy(moves, 59, 70);
        // Move 67, Low Kick, has weight-based power in gen3+
        // Low Kick => 100% accuracy (gen1)
        updateMoveAccuracy(moves, 67, 100);
        // Absorb => 25pp (gen1/2/3)
        updateMovePP(moves, 71, 25);
        // Mega Drain => 15pp (gen1/2/3)
        updateMovePP(moves, 72, 15);
        // Petal Dance => 120power, 10pp (gen1-4)
        updateMovePP(moves, 80, 10);
        updateMovePower(moves, 80, 120);
        // Fire Spin => 35 power, 85% acc (gen1-4)
        updateMoveAccuracy(moves, 83, 85);
        updateMovePower(moves, 83, 35);
        // Rock Throw => 90% accuracy (gen1)
        updateMoveAccuracy(moves, 88, 90);
        // Dig => 80 power (gen1/2/3)
        updateMovePower(moves, 91, 80);
        // Toxic => 90% accuracy (gen1-4)
        updateMoveAccuracy(moves, 92, 90);
        // Hypnosis => 60% accuracy
        updateMoveAccuracy(moves, 95, 60);
        // Recover => 10pp (gen1/2/3)
        updateMovePP(moves, 105, 10);
        // SelfDestruct => 200power (gen1)
        updateMovePower(moves, 120, 200);
        // Clamp => 85% acc (gen1-4)
        updateMoveAccuracy(moves, 128, 85);
        updateMovePP(moves, 128, 15);
        // HJKick => 130 power, 10pp (gen1-4)
        updateMovePP(moves, 136, 10);
        updateMovePower(moves, 136, 130);
        // Glare => 90% acc (gen1-4)
        updateMoveAccuracy(moves, 137, 90);
        // Poison Gas => 80% acc (gen1-4)
        updateMoveAccuracy(moves, 139, 80);
        // Flash => 100% acc (gen1/2/3)
        updateMoveAccuracy(moves, 148, 100);
        // Crabhammer => 90% acc (gen1-4)
        updateMoveAccuracy(moves, 152, 90);
        // Explosion => 250 power (gen1)
        updateMovePower(moves, 153, 250);
        // GEN2+ moves only from here
        if (moves.size() >= 251) {
            // Curse => GHOST (gen2-4)
            updateMoveType(moves, 174, Type.GHOST);
            // Cotton Spore => 100% acc (gen2-4)
            updateMoveAccuracy(moves, 178, 100);
            // Scary Face => 100% acc (gen2-4)
            updateMoveAccuracy(moves, 184, 100);
            // Zap Cannon => 120 power (gen2-3)
            updateMovePower(moves, 192, 120);
            // Bone Rush => 90% acc (gen2-4)
            updateMoveAccuracy(moves, 198, 90);
            // Outrage => 120 power (gen2-3)
            updateMovePower(moves, 200, 120);
            updateMovePP(moves, 200, 10);
            // Giga Drain => 10pp (gen2-3), 75 power (gen2-4)
            updateMovePP(moves, 202, 10);
            updateMovePower(moves, 202, 75);
            // Fury Cutter => 20 power (gen2-4)
            updateMovePower(moves, 210, 20);
            // Future Sight => 10 pp, 100 power, 100% acc (gen2-4)
            updateMovePP(moves, 248, 10);
            updateMovePower(moves, 248, 100);
            updateMoveAccuracy(moves, 248, 100);
            // Rock Smash => 40 power (gen2-3)
            updateMovePower(moves, 249, 40);
            // Whirlpool => 35 pow, 85% acc (gen2-4)
            updateMovePower(moves, 250, 35);
            updateMoveAccuracy(moves, 250, 85);
        }
        // GEN3+ only moves from here
        if (moves.size() >= 354) {
            // Uproar => 90 power (gen3-4)
            updateMovePower(moves, 253, 90);
            updateMovePP(moves, 254, 20);
            updateMovePower(moves, 291, 80);
            // Sand Tomb => 35 pow, 85% acc (gen3-4)
            updateMovePower(moves, 328, 35);
            updateMoveAccuracy(moves, 328, 85);
            // Bullet Seed => 25 power (gen3-4)
            updateMovePower(moves, 331, 25);
            // Icicle Spear => 25 power (gen3-4)
            updateMovePower(moves, 333, 25);
            // Covet => 60 power (gen3-4)
            updateMovePower(moves, 343, 60);
            updateMovePower(moves, 348, 90);
            // Rock Blast => 90% acc (gen3-4)
            updateMoveAccuracy(moves, 350, 90);
            // Doom Desire => 140 pow, 100% acc, gen3-4
            updateMovePower(moves, 353, 140);
            updateMoveAccuracy(moves, 353, 100);
        }
        // GEN4+ only moves from here
        if (moves.size() >= 467) {
            // Feint => 30 pow
            updateMovePower(moves, 364, 30);
            // Last Resort => 140 pow
            updateMovePower(moves, 387, 140);
            // Drain Punch => 10 pp, 75 pow
            updateMovePP(moves, 409, 10);
            updateMovePower(moves, 409, 75);
            // Magma Storm => 75% acc
            updateMoveAccuracy(moves, 463, 75);
        }
    }

    @Override
    public void updateMovesToGen6() {
        List<Move> moves = this.getMoves();

        // gen 1
        // Swords Dance 20 PP
        updateMovePP(moves, 14, 20);

        // Vine Whip 25 PP, 45 Power
        updateMovePP(moves, 22, 25);
        updateMovePower(moves, 22, 45);

        // Pin Missile 25 Power, 95% Accuracy
        updateMovePower(moves, 42, 25);
        updateMoveAccuracy(moves, 42, 95);

        // Flamethrower 90 Power
        updateMovePower(moves, 53, 90);

        // Hydro Pump 110 Power
        updateMovePower(moves, 56, 110);

        // Surf 90 Power
        updateMovePower(moves, 57, 90);

        // Ice Beam 90 Power
        updateMovePower(moves, 58, 90);

        // Blizzard 110 Power
        updateMovePower(moves, 59, 110);

        // Growth 20 PP
        updateMovePP(moves, 74, 20);

        // Thunderbolt 90 Power
        updateMovePower(moves, 85, 90);

        // Thunder 110 Power
        updateMovePower(moves, 87, 110);

        // Minimize 10 PP
        updateMovePP(moves, 107, 10);

        // Barrier 20 PP
        updateMovePP(moves, 112, 20);

        // Lick 30 Power
        updateMovePower(moves, 122, 30);

        // Smog 30 Power
        updateMovePower(moves, 123, 30);

        // Fire Blast 110 Power
        updateMovePower(moves, 126, 110);

        // Skull Bash 10 PP, 130 Power
        updateMovePP(moves, 130, 10);
        updateMovePower(moves, 130, 130);

        // Glare 100% Accuracy
        updateMoveAccuracy(moves, 137, 100);

        // Poison Gas 90% Accuracy
        updateMoveAccuracy(moves, 139, 90);

        // Bubble 40 Power
        updateMovePower(moves, 145, 40);

        // Psywave 100% Accuracy
        updateMoveAccuracy(moves, 149, 100);

        // Acid Armor 20 PP
        updateMovePP(moves, 151, 20);

        // Crabhammer 100 Power
        updateMovePower(moves, 152, 100);

        // Gen2+ only
        if (moves.size() >= 251) {
            // Thief 25 PP, 60 Power
            updateMovePP(moves, 168, 25);
            updateMovePower(moves, 168, 60);

            // Snore 50 Power
            updateMovePower(moves, 173, 50);

            // Fury Cutter 40 Power
            updateMovePower(moves, 210, 40);

            // Future Sight 120 Power
            updateMovePower(moves, 248, 120);
        }

        // Gen3+ only
        if (moves.size() >= 354) {
            // Heat Wave 95 Power
            updateMovePower(moves, 257, 95);

            // Will-o-Wisp 85% Accuracy
            updateMoveAccuracy(moves, 261, 85);

            // Smellingsalt 70 Power
            updateMovePower(moves, 265, 70);

            // Knock off 65 Power
            updateMovePower(moves, 282, 65);

            // Meteor Mash 90 Power, 90% Accuracy
            updateMovePower(moves, 309, 90);
            updateMoveAccuracy(moves, 309, 90);

            // Air Cutter 60 Power
            updateMovePower(moves, 314, 60);

            // Overheat 130 Power
            updateMovePower(moves, 315, 130);

            // Rock Tomb 15 PP, 60 Power, 95% Accuracy
            updateMovePP(moves, 317, 15);
            updateMovePower(moves, 317, 60);
            updateMoveAccuracy(moves, 317, 95);

            // Extrasensory 20 PP
            updateMovePP(moves, 326, 20);

            // Muddy Water 90 Power
            updateMovePower(moves, 330, 90);

            // Covet 25 PP
            updateMovePP(moves, 343, 25);
        }

        // Gen4+ only
        if (moves.size() >= 467) {
            // Wake-Up Slap 70 Power
            updateMovePower(moves, 358, 70);

            // Tailwind 15 PP
            updateMovePP(moves, 366, 15);

            // Assurance 60 Power
            updateMovePower(moves, 372, 60);

            // Psycho Shift 100% Accuracy
            updateMoveAccuracy(moves, 375, 100);

            // Aura Sphere 80 Power
            updateMovePower(moves, 396, 80);

            // Air Slash 15 PP
            updateMovePP(moves, 403, 15);

            // Dragon Pulse 85 Power
            updateMovePower(moves, 406, 85);

            // Power Gem 80 Power
            updateMovePower(moves, 408, 80);

            // Energy Ball 90 Power
            updateMovePower(moves, 412, 90);

            // Draco Meteor 130 Power
            updateMovePower(moves, 434, 130);

            // Leaf Storm 130 Power
            updateMovePower(moves, 437, 130);

            // Gunk Shot 80% Accuracy
            updateMoveAccuracy(moves, 441, 80);

            // Chatter 65 Power
            updateMovePower(moves, 448, 65);

            // Magma Storm 100 Power
            updateMovePower(moves, 463, 100);
        }

        // Gen5+ only
        if (moves.size() >= 559) {
            // Storm Throw 60 Power
            updateMovePower(moves, 480, 60);

            // Synchronoise 120 Power
            updateMovePower(moves, 485, 120);

            // Low Sweep 65 Power
            updateMovePower(moves, 490, 65);

            // Hex 65 Power
            updateMovePower(moves, 506, 65);

            // Incinerate 60 Power
            updateMovePower(moves, 510, 60);

            // Pledges 80 Power
            updateMovePower(moves, 518, 80);
            updateMovePower(moves, 519, 80);
            updateMovePower(moves, 520, 80);

            // Struggle Bug 50 Power
            updateMovePower(moves, 522, 50);

            // Frost Breath 45 Power
            // crits are 2x in these games
            updateMovePower(moves, 524, 45);

            // Sacred Sword 15 PP
            updateMovePP(moves, 533, 15);

            // Hurricane 110 Power
            updateMovePower(moves, 542, 110);

            // Techno Blast 120 Power
            updateMovePower(moves, 546, 120);
        }
    }

    private Map<Integer, boolean[]> moveUpdates;

    @Override
    public void initMoveModernization() {
        moveUpdates = new TreeMap<Integer, boolean[]>();
        TemplateData.putData("isModernMoves", true);
    }

    /**
     * Capture changes related to move changes in Gen 5 or 6
     */
    @Override
    public void printMoveModernization() {
        ArrayList<Move> moveModernization = new ArrayList<Move>();
        TemplateData.putData("moveUpdates", moveUpdates);
        TemplateData.putData("movesMod", moveModernization);
        List<Move> moves = this.getMoves();
        for (int moveID : moveUpdates.keySet()) {
            moveModernization.add(new Move(moves.get(moveID)));
        }
    }

    @Override
    public void removeBrokenMoves() {
        Map<Pokemon, List<MoveLearnt>> movesets = this.getMovesLearnt();
        Set<Integer> allBanned = new HashSet<Integer>(this.getGameBreakingMoves().values());

        for (List<MoveLearnt> movesLearnt : movesets.values()) {
            movesLearnt.removeIf(move -> allBanned.contains(move.move));
        }

        // Done, save
        this.setMovesLearnt(movesets);
        TemplateData.putData("gameBreakingMoves", true);
    }

    @Override
    public void randomizeMovesLearnt(boolean typeThemed, boolean noBroken,
            boolean forceStartingMoves, int forceStartingMoveCount,
            double goodDamagingProbability) {
        // Get current sets
        Map<Pokemon, List<MoveLearnt>> movesets = this.getMovesLearnt();
        List<Integer> hms = this.getHMMoves();
        List<Move> allMoves = this.getMoves();

        @SuppressWarnings("unchecked")
        Set<Integer> allBanned = new HashSet<Integer>(
                noBroken ? this.getGameBreakingMoves().values() : Collections.EMPTY_SET);
        allBanned.addAll(hms);
        allBanned.addAll(this.getMovesBannedFromLevelup());

        // Build sets of moves
        List<Move> validMoves = new ArrayList<Move>();
        List<Move> validDamagingMoves = new ArrayList<Move>();
        Map<Type, List<Move>> validTypeMoves = new HashMap<Type, List<Move>>();
        Map<Type, List<Move>> validTypeDamagingMoves = new HashMap<Type, List<Move>>();

        for (Move mv : allMoves) {
            if (mv != null && !GlobalConstants.bannedRandomMoves[mv.number]
                    && !allBanned.contains(mv.number)) {
                validMoves.add(mv);
                if (mv.type != null) {
                    if (!validTypeMoves.containsKey(mv.type)) {
                        validTypeMoves.put(mv.type, new ArrayList<Move>());
                    }
                    validTypeMoves.get(mv.type).add(mv);
                }

                if (!GlobalConstants.bannedForDamagingMove[mv.number]) {
                    if (mv.power >= 2 * GlobalConstants.MIN_DAMAGING_MOVE_POWER
                            || (mv.power >= GlobalConstants.MIN_DAMAGING_MOVE_POWER
                                    && mv.hitratio >= 90)) {
                        validDamagingMoves.add(mv);
                        if (mv.type != null) {
                            if (!validTypeDamagingMoves.containsKey(mv.type)) {
                                validTypeDamagingMoves.put(mv.type, new ArrayList<Move>());
                            }
                            validTypeDamagingMoves.get(mv.type).add(mv);
                        }
                    }
                }
            }
        }

        for (Pokemon pkmn : movesets.keySet()) {
            Set<Integer> learnt = new TreeSet<Integer>();
            List<MoveLearnt> moves = movesets.get(pkmn);

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

            // Find last lv1 move
            // lv1index ends up as the index of the first non-lv1 move
            int lv1index = 0;
            while (lv1index < moves.size() && moves.get(lv1index).level == 1) {
                lv1index++;
            }

            // last lv1 move is 1 before lv1index
            if (lv1index != 0) {
                lv1index--;
            }

            // Replace moves as needed
            for (int i = 0; i < moves.size(); i++) {
                // should this move be forced damaging?
                boolean attemptDamaging =
                        i == lv1index ? true : random.nextDouble() < goodDamagingProbability;

                // type themed?
                Type typeOfMove = null;
                if (typeThemed) {
                    double picked = random.nextDouble();
                    if (pkmn.primaryType == Type.NORMAL && pkmn.secondaryType != null) {
                        // Normal/OTHER: 50% normal, 30% other, 20% random
                        if (picked < 0.5) {
                            typeOfMove = Type.NORMAL;
                        } else if (picked < 0.8) {
                            typeOfMove = pkmn.secondaryType;
                        }
                        // else random
                    } else if (pkmn.secondaryType != null) {
                        // Primary/Secondary: 25% primary, 25% secondary, 50% random
                        if (picked < 0.25) {
                            typeOfMove = pkmn.primaryType;
                        } else if (picked < 0.5) {
                            typeOfMove = pkmn.secondaryType;
                        }
                        // else random
                    } else {
                        // Primary/None: 60% primary, 20% normal, 20% random
                        if (picked < 0.6) {
                            typeOfMove = pkmn.primaryType;
                        } else if (picked < 0.8) {
                            typeOfMove = Type.NORMAL;
                        }
                        // else random
                    }
                }

                // select a list to pick a move from that has at least one free
                List<Move> pickList = validMoves;
                if (attemptDamaging) {
                    if (typeOfMove != null) {
                        if (validTypeDamagingMoves.containsKey(typeOfMove) && checkForUnusedMove(
                                validTypeDamagingMoves.get(typeOfMove), learnt)) {
                            pickList = validTypeDamagingMoves.get(typeOfMove);
                        } else if (checkForUnusedMove(validDamagingMoves, learnt)) {
                            pickList = validDamagingMoves;
                        }
                    } else if (checkForUnusedMove(validDamagingMoves, learnt)) {
                        pickList = validDamagingMoves;
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

                // write it
                moves.get(i).move = mv.number;
                if (i == lv1index) {
                    // just in case, set this to lv1
                    moves.get(i).level = 1;
                }
                learnt.add(mv.number);

            }
        }
        // Done, save
        this.setMovesLearnt(movesets);
        TemplateData.putData("gameBreakingMoves", noBroken);
    }

    @Override
    public void orderDamagingMovesByDamage() {
        Map<Pokemon, List<MoveLearnt>> movesets = this.getMovesLearnt();
        List<Move> allMoves = this.getMoves();
        for (Pokemon pkmn : movesets.keySet()) {
            List<MoveLearnt> moves = movesets.get(pkmn);

            // Build up a list of damaging moves and their positions
            List<Integer> damagingMoveIndices = new ArrayList<Integer>();
            List<Move> damagingMoves = new ArrayList<Move>();
            for (int i = 0; i < moves.size(); i++) {
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
            Collections.sort(damagingMoves, new Comparator<Move>() {

                @Override
                public int compare(Move m1, Move m2) {
                    if (m1.power * m1.hitCount < m2.power * m2.hitCount) {
                        return -1;
                    } else if (m1.power * m1.hitCount > m2.power * m2.hitCount) {
                        return 1;
                    } else {
                        // stay with the random order
                        return 0;
                    }
                }
            });

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
        Map<Pokemon, List<MoveLearnt>> movesets = this.getMovesLearnt();

        MoveLearnt metronomeML = new MoveLearnt();
        metronomeML.level = 1;
        metronomeML.move = GlobalConstants.METRONOME_MOVE;

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
            for (TrainerPokemon tpk : t.getPokemon()) {
                tpk.resetMoves = true;
            }
        }

        this.setTrainers(trainers);

        // tms
        List<Integer> tmMoves = this.getTMMoves();

        for (int i = 0; i < tmMoves.size(); i++) {
            tmMoves.set(i, GlobalConstants.METRONOME_MOVE);
        }

        this.setTMMoves(tmMoves);

        // movetutors
        if (this.hasMoveTutors()) {
            List<Integer> mtMoves = this.getMoveTutorMoves();

            for (int i = 0; i < mtMoves.size(); i++) {
                mtMoves.set(i, GlobalConstants.METRONOME_MOVE);
            }

            this.setMoveTutorMoves(mtMoves);
        }

        // move tweaks
        List<Move> moveData = this.getMoves();

        Move metronome = moveData.get(GlobalConstants.METRONOME_MOVE);

        metronome.pp = 40;

        List<Integer> hms = this.getHMMoves();

        for (int hm : hms) {
            Move thisHM = moveData.get(hm);
            thisHM.pp = 0;
        }

        // evolution tweaks
        for (Pokemon pk : this.getMainPokemonList()) {
            if (pk != null) {
                for (Evolution ev : pk.evolutionsFrom) {
                    if (ev.type == EvolutionType.LEVEL_WITH_MOVE) {
                        ev.extraInfo = GlobalConstants.METRONOME_MOVE;
                    }
                }
            }
        }
    }

    @Override
    public void randomizeStaticPokemon(boolean legendForLegend) {
        // Load
        checkPokemonRestrictions();
        List<Pokemon> currentStaticPokemon = this.getStaticPokemon();
        List<Pokemon> replacements = new ArrayList<Pokemon>();
        List<Pokemon> banned = this.bannedForStaticPokemon();
        Map<String, String> staticPokemon = new TreeMap<String, String>();

        if (legendForLegend) {
            PokemonCollection legendariesLeft = new PokemonCollection(onlyLegendaryList);
            PokemonCollection nonlegsLeft = new PokemonCollection(noLegendaryList);
            legendariesLeft.filterList(banned);
            nonlegsLeft.filterList(banned);
            for (int i = 0; i < currentStaticPokemon.size(); i++) {
                Pokemon old = currentStaticPokemon.get(i);
                Pokemon newPK;
                if (old.isLegendary()) {
                    if (old.number == 487 && ptGiratina) {
                        newPK = giratinaPicks.remove(this.random.nextInt(giratinaPicks.size()));
                        legendariesLeft.remove(newPK);
                    } else {
                        newPK = legendariesLeft.randomPokemon(this.random, true);
                    }
                    if (legendariesLeft.size() == 0) {
                        legendariesLeft.addAll(onlyLegendaryList);
                        legendariesLeft.filterList(banned);
                    }
                } else {
                    // Replace grass monkey
                    if (old.number == 511) {
                        // Grass => Fire (starter 1)
                        // Find a random type weakness of starter that should be covered
                        Type cover = groupTypesMap.get("CRESS");
                        if (cover == null) {
                            cover = Type.randomWeakness(this.random, false,
                                    this.getStarters().get(1).primaryType);
                        }

                        // Get a pokemon with a type that is superior to that weakness
                        newPK = nonlegsLeft.randomPokemonOfType(
                                Type.randomWeakness(this.random, false, cover), this.random, true);
                    }
                    // Replace fire monkey
                    else if (old.number == 513) {
                        // Fire => Water (starter 2)
                        // Find a random type weakness of starter that should be covered
                        Type cover = groupTypesMap.get("CILAN");
                        if (cover == null) {
                            cover = Type.randomWeakness(this.random, false,
                                    this.getStarters().get(2).primaryType);
                        }

                        // Get a pokemon with a type that is superior to that weakness
                        newPK = nonlegsLeft.randomPokemonOfType(
                                Type.randomWeakness(this.random, false, cover), this.random, true);
                    }
                    // Replace water monkey
                    else if (old.number == 515) {
                        // Water => Grass (starter 0)
                        // Find a random type weakness of starter that should be covered
                        Type cover = groupTypesMap.get("CHILI");
                        if (cover == null) {
                            cover = Type.randomWeakness(this.random, false,
                                    this.getStarters().get(0).primaryType);
                        }

                        // Get a pokemon with a type that is superior to that weakness
                        newPK = nonlegsLeft.randomPokemonOfType(
                                Type.randomWeakness(this.random, false, cover), this.random, true);
                    } else {
                        newPK = nonlegsLeft.randomPokemon(this.random, true);
                    }
                    if (nonlegsLeft.size() == 0) {
                        nonlegsLeft.addAll(noLegendaryList);
                        nonlegsLeft.filterList(banned);
                    }
                }
                replacements.add(newPK);
                String oldName = old.name;
                int amount = 2;
                while (staticPokemon.containsKey(oldName)) {
                    oldName = old.name + "(" + amount + ")";
                    amount++;
                }
                staticPokemon.put(oldName, newPK.name);
            }
        } else {
            PokemonCollection pokemonLeft = new PokemonCollection(getMainPokemonList());
            pokemonLeft.filterList(banned);
            for (int i = 0; i < currentStaticPokemon.size(); i++) {
                Pokemon old = currentStaticPokemon.get(i);
                Pokemon newPK;
                // Replace Giratina
                if (old.number == 487 && ptGiratina) {
                    newPK = giratinaPicks.remove(this.random.nextInt(giratinaPicks.size()));
                    pokemonLeft.remove(newPK);
                }
                // Replace grass monkey
                else if (old.number == 511) {
                    // Grass => Fire (starter 1)
                    // Find a random type weakness of starter that should be covered
                    Type cover = groupTypesMap.get("CRESS");
                    if (cover == null) {
                        cover = Type.randomWeakness(this.random, false,
                                this.getStarters().get(1).primaryType);
                    }

                    // Get a pokemon with a type that is superior to that weakness
                    newPK = pokemonLeft.randomPokemonOfType(
                            Type.randomWeakness(this.random, false, cover), this.random, true);
                }
                // Replace fire monkey
                else if (old.number == 513) {
                    // Fire => Water (starter 2)
                    // Find a random type weakness of starter that should be covered
                    Type cover = groupTypesMap.get("CILAN");
                    if (cover == null) {
                        cover = Type.randomWeakness(this.random, false,
                                this.getStarters().get(2).primaryType);
                    }

                    // Get a pokemon with a type that is superior to that weakness
                    newPK = pokemonLeft.randomPokemonOfType(
                            Type.randomWeakness(this.random, false, cover), this.random, true);
                }
                // Replace water monkey
                else if (old.number == 515) {
                    // Water => Grass (starter 0)
                    // Find a random type weakness of starter that should be covered
                    Type cover = groupTypesMap.get("CHILI");
                    if (cover == null) {
                        cover = Type.randomWeakness(this.random, false,
                                this.getStarters().get(0).primaryType);
                    }

                    // Get a pokemon with a type that is superior to that weakness
                    newPK = pokemonLeft.randomPokemonOfType(
                            Type.randomWeakness(this.random, false, cover), this.random, true);
                } else {
                    newPK = pokemonLeft.randomPokemon(this.random, true);
                }
                if (pokemonLeft.size() == 0) {
                    pokemonLeft.addAll(getMainPokemonList());
                    pokemonLeft.filterList(banned);
                }
                replacements.add(newPK);
                String oldName = old.name;
                int amount = 2;
                while (staticPokemon.containsKey(oldName)) {
                    oldName = old.name + "(" + amount + ")";
                    amount++;
                }
                staticPokemon.put(oldName, newPK.name);
            }
        }

        // Save
        this.setStaticPokemon(replacements);
        TemplateData.putData("staticPokemon", staticPokemon);
    }

    @Override
    public void randomizeTMMoves(boolean noBroken, boolean preserveField,
            double goodDamagingProbability) {
        // Pick some random TM moves.
        int tmCount = this.getTMCount();
        List<Move> allMoves = this.getMoves();
        List<Integer> hms = this.getHMMoves();
        List<Integer> oldTMs = this.getTMMoves();
        @SuppressWarnings("unchecked")
        List<Integer> banned = new ArrayList<Integer>(
                noBroken ? this.getGameBreakingMoves().values() : Collections.EMPTY_LIST);
        // field moves?
        List<Integer> fieldMoves = this.getFieldMoves();
        int preservedFieldMoveCount = 0;

        if (preserveField) {
            List<Integer> banExistingField = new ArrayList<Integer>(oldTMs);
            banExistingField.retainAll(fieldMoves);
            preservedFieldMoveCount = banExistingField.size();
            banned.addAll(banExistingField);
        }

        // Determine which moves are pickable
        List<Move> usableMoves = new ArrayList<Move>(allMoves);
        usableMoves.remove(0); // remove null entry
        Set<Move> unusableMoves = new HashSet<Move>();
        Set<Move> unusableDamagingMoves = new HashSet<Move>();

        for (Move mv : usableMoves) {
            if (GlobalConstants.bannedRandomMoves[mv.number] || hms.contains(mv.number)
                    || banned.contains(mv.number)) {
                unusableMoves.add(mv);
            } else if (GlobalConstants.bannedForDamagingMove[mv.number]
                    || mv.power < GlobalConstants.MIN_DAMAGING_MOVE_POWER) {
                unusableDamagingMoves.add(mv);
            }
        }

        usableMoves.removeAll(unusableMoves);
        List<Move> usableDamagingMoves = new ArrayList<Move>(usableMoves);
        usableDamagingMoves.removeAll(unusableDamagingMoves);

        // pick (tmCount - preservedFieldMoveCount) moves
        List<Integer> pickedMoves = new ArrayList<Integer>();

        for (int i = 0; i < tmCount - preservedFieldMoveCount; i++) {
            Move chosenMove;
            if (random.nextDouble() < goodDamagingProbability && usableDamagingMoves.size() > 0) {
                chosenMove = usableDamagingMoves.get(random.nextInt(usableDamagingMoves.size()));
            } else {
                chosenMove = usableMoves.get(random.nextInt(usableMoves.size()));
            }
            pickedMoves.add(chosenMove.number);
            usableMoves.remove(chosenMove);
            usableDamagingMoves.remove(chosenMove);
        }

        // shuffle the picked moves because high goodDamagingProbability
        // could bias them towards early numbers otherwise
        Collections.shuffle(pickedMoves, random);

        // finally, distribute them as tms
        int pickedMoveIndex = 0;
        List<Integer> newTMs = new ArrayList<Integer>();

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
    public void randomizeTMHMCompatibility(boolean preferSameType) {
        // Get current compatibility
        // new: increase HM chances if required early on
        List<Integer> requiredEarlyOn = this.getEarlyRequiredHMMoves();
        Map<Pokemon, boolean[]> compat = this.getTMHMCompatibility();
        List<Integer> tmHMs = new ArrayList<Integer>(this.getTMMoves());
        tmHMs.addAll(this.getHMMoves());
        List<Move> moveData = this.getMoves();
        for (Map.Entry<Pokemon, boolean[]> compatEntry : compat.entrySet()) {
            Pokemon pkmn = compatEntry.getKey();
            boolean[] flags = compatEntry.getValue();
            for (int i = 1; i <= tmHMs.size(); i++) {
                int move = tmHMs.get(i - 1);
                Move mv = moveData.get(move);
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
                if (requiredEarlyOn.contains(move)) {
                    probability = Math.min(1.0, probability * 1.8);
                }
                flags[i] = (this.random.nextDouble() < probability);
            }
        }

        // Set the new compatibility
        this.setTMHMCompatibility(compat);
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
        Map<Pokemon, List<MoveLearnt>> movesets = this.getMovesLearnt();
        List<Integer> tmMoves = this.getTMMoves();
        for (Pokemon pkmn : compat.keySet()) {
            List<MoveLearnt> moveset = movesets.get(pkmn);
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
    public void randomizeMoveTutorMoves(boolean noBroken, boolean preserveField,
            double goodDamagingProbability) {
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
        List<Integer> banned = new ArrayList<Integer>(
                noBroken ? this.getGameBreakingMoves().values() : Collections.EMPTY_LIST);

        // field moves?
        List<Integer> fieldMoves = this.getFieldMoves();
        int preservedFieldMoveCount = 0;
        if (preserveField) {
            List<Integer> banExistingField = new ArrayList<Integer>(oldMTs);
            banExistingField.retainAll(fieldMoves);
            preservedFieldMoveCount = banExistingField.size();
            banned.addAll(banExistingField);
        }

        // Determine which moves are pickable
        List<Move> usableMoves = new ArrayList<Move>(allMoves);
        usableMoves.remove(0); // remove null entry
        Set<Move> unusableMoves = new HashSet<Move>();
        Set<Move> unusableDamagingMoves = new HashSet<Move>();

        for (Move mv : usableMoves) {
            if (GlobalConstants.bannedRandomMoves[mv.number] || tms.contains(mv.number)
                    || hms.contains(mv.number) || banned.contains(mv.number)) {
                unusableMoves.add(mv);
            } else if (GlobalConstants.bannedForDamagingMove[mv.number]
                    || mv.power < GlobalConstants.MIN_DAMAGING_MOVE_POWER) {
                unusableDamagingMoves.add(mv);
            }
        }

        usableMoves.removeAll(unusableMoves);
        List<Move> usableDamagingMoves = new ArrayList<Move>(usableMoves);
        usableDamagingMoves.removeAll(unusableDamagingMoves);

        // pick (tmCount - preservedFieldMoveCount) moves
        List<Integer> pickedMoves = new ArrayList<Integer>();

        for (int i = 0; i < mtCount - preservedFieldMoveCount; i++) {
            Move chosenMove;
            if (random.nextDouble() < goodDamagingProbability && usableDamagingMoves.size() > 0) {
                chosenMove = usableDamagingMoves.get(random.nextInt(usableDamagingMoves.size()));
            } else {
                chosenMove = usableMoves.get(random.nextInt(usableMoves.size()));
            }
            pickedMoves.add(chosenMove.number);
            usableMoves.remove(chosenMove);
            usableDamagingMoves.remove(chosenMove);
        }

        // shuffle the picked moves because high goodDamagingProbability
        // could bias them towards early numbers otherwise
        Collections.shuffle(pickedMoves, random);

        // finally, distribute them as tutors
        int pickedMoveIndex = 0;
        List<Integer> newMTs = new ArrayList<Integer>();

        for (int i = 0; i < mtCount; i++) {
            if (preserveField && fieldMoves.contains(oldMTs.get(i))) {
                newMTs.add(oldMTs.get(i));
            } else {
                newMTs.add(pickedMoves.get(pickedMoveIndex++));
            }
        }

        this.setMoveTutorMoves(newMTs);
    }

    @Override
    public void randomizeMoveTutorCompatibility(boolean preferSameType) {
        if (!this.hasMoveTutors()) {
            return;
        }
        // Get current compatibility
        Map<Pokemon, boolean[]> compat = this.getMoveTutorCompatibility();
        List<Integer> mts = this.getMoveTutorMoves();
        List<Move> moveData = this.getMoves();
        for (Map.Entry<Pokemon, boolean[]> compatEntry : compat.entrySet()) {
            Pokemon pkmn = compatEntry.getKey();
            boolean[] flags = compatEntry.getValue();
            for (int i = 1; i <= mts.size(); i++) {
                int move = mts.get(i - 1);
                Move mv = moveData.get(move);
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
                flags[i] = (this.random.nextDouble() < probability);
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
        Map<Pokemon, List<MoveLearnt>> movesets = this.getMovesLearnt();
        List<Integer> mtMoves = this.getMoveTutorMoves();
        for (Pokemon pkmn : compat.keySet()) {
            List<MoveLearnt> moveset = movesets.get(pkmn);
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

    @SuppressWarnings("unchecked")
    @Override
    public void randomizeTrainerNames(CustomNamesSet customNames) {
        if (!this.canChangeTrainerText()) {
            return;
        }

        // index 0 = singles, 1 = doubles
        List<String>[] allTrainerNames =
                new List[] {new ArrayList<String>(), new ArrayList<String>()};
        Map<Integer, List<String>> trainerNamesByLength[] = new Map[] {
                new TreeMap<Integer, List<String>>(), new TreeMap<Integer, List<String>>()};

        // Read name lists
        for (String trainername : customNames.getTrainerNames()) {
            int len = this.internalStringLength(trainername);
            if (len <= 10) {
                allTrainerNames[0].add(trainername);
                if (trainerNamesByLength[0].containsKey(len)) {
                    trainerNamesByLength[0].get(len).add(trainername);
                } else {
                    List<String> namesOfThisLength = new ArrayList<String>();
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
                    List<String> namesOfThisLength = new ArrayList<String>();
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
        Map<String, String> translation = new HashMap<String, String>();
        List<String> newTrainerNames = new ArrayList<String>();
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
                if (translation.containsKey(trainerName)
                        && trainerName.equalsIgnoreCase("GRUNT") == false
                        && trainerName.equalsIgnoreCase("EXECUTIVE") == false) {
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
                    if (pickFrom != null && pickFrom.size() > 0 && intStrLen > 1) {
                        int innerTries = 0;
                        changeTo = pickFrom.get(this.random.nextInt(pickFrom.size()));
                        ctl = this.internalStringLength(changeTo);
                        while ((mode == TrainerNameMode.MAX_LENGTH && ctl > maxLength)
                                || (mode == TrainerNameMode.MAX_LENGTH_WITH_CLASS
                                        && ctl + tcNameLengths.get(tnIndex) > maxLength)) {
                            innerTries++;
                            if (innerTries == 100) {
                                changeTo = trainerName;
                                ctl = intStrLen;
                                break;
                            }
                            changeTo = pickFrom.get(this.random.nextInt(pickFrom.size()));
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
            throw new RandomizationException(
                    "Could not randomize trainer names in a reasonable amount of attempts."
                            + "\nPlease add some shorter names to your custom trainer names.");
        }

        // Done choosing, save
        this.setTrainerNames(newTrainerNames);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void randomizeTrainerClassNames(CustomNamesSet customNames) {
        if (!this.canChangeTrainerText()) {
            return;
        }

        // index 0 = singles, index 1 = doubles
        List<String> allTrainerClasses[] =
                new List[] {new ArrayList<String>(), new ArrayList<String>()};
        Map<Integer, List<String>> trainerClassesByLength[] = new Map[] {
                new HashMap<Integer, List<String>>(), new HashMap<Integer, List<String>>()};

        // Read names data
        for (String trainerClassName : customNames.getTrainerClasses()) {
            allTrainerClasses[0].add(trainerClassName);
            int len = this.internalStringLength(trainerClassName);
            if (trainerClassesByLength[0].containsKey(len)) {
                trainerClassesByLength[0].get(len).add(trainerClassName);
            } else {
                List<String> namesOfThisLength = new ArrayList<String>();
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
                List<String> namesOfThisLength = new ArrayList<String>();
                namesOfThisLength.add(trainerClassName);
                trainerClassesByLength[1].put(len, namesOfThisLength);
            }
        }

        // Get the current trainer names data
        List<String> currentClassNames = this.getTrainerClassNames();
        boolean mustBeSameLength = this.fixedTrainerClassNamesLength();
        int maxLength = this.maxTrainerClassNameLength();

        // Init the translation map and new list
        Map<String, String> translation = new HashMap<String, String>();
        List<String> newClassNames = new ArrayList<String>();

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
                    changeTo = pickFrom.get(this.random.nextInt(pickFrom.size()));
                    while (changeTo.length() > maxLength) {
                        changeTo = pickFrom.get(this.random.nextInt(pickFrom.size()));
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
    public void randomizeWildHeldItems(boolean banBadItems) {
        List<Pokemon> pokemon = this.getMainPokemonList();
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
    public void randomizeStarterHeldItems(boolean banBadItems) {
        List<Integer> oldHeldItems = this.getStarterHeldItems();
        List<Integer> newHeldItems = new ArrayList<Integer>();
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
    public void randomizeFieldItems(boolean banBadItems) {
        ItemList possibleItems = banBadItems ? this.getNonBadItems() : this.getAllowedItems();
        List<Integer> currentItems = this.getRegularFieldItems();
        List<Integer> currentTMs = this.getCurrentFieldTMs();
        List<Integer> requiredTMs = this.getRequiredFieldTMs();

        int fieldItemCount = currentItems.size();
        int fieldTMCount = currentTMs.size();
        int reqTMCount = requiredTMs.size();
        int totalTMCount = this.getTMCount();

        List<Integer> newItems = new ArrayList<Integer>();
        List<Integer> newTMs = new ArrayList<Integer>();

        for (int i = 0; i < fieldItemCount; i++) {
            newItems.add(possibleItems.randomNonTM(this.random));
        }

        newTMs.addAll(requiredTMs);

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
    public void randomizeIngameTrades(boolean randomizeRequest, boolean randomNickname,
            boolean randomOT, boolean randomStats, boolean randomItem, CustomNamesSet customNames) {
        checkPokemonRestrictions();
        // Process trainer names
        List<String> trainerNames = new ArrayList<String>();
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
        List<String> nicknames = new ArrayList<String>();
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
        List<Pokemon> usedRequests = new ArrayList<Pokemon>();
        List<Pokemon> usedGivens = new ArrayList<Pokemon>();
        List<String> usedOTs = new ArrayList<String>();
        List<String> usedNicknames = new ArrayList<String>();
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
                Pokemon request = this.randomPokemon();
                while (usedRequests.contains(request) || request == given) {
                    request = this.randomPokemon();
                }
                usedRequests.add(request);
                trade.requestedPokemon = request;
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
        List<Pokemon> allPokemon = this.getMainPokemonList();
        Set<Evolution> changedEvos = new TreeSet<Evolution>();
        // search for level evolutions
        for (Pokemon pk : allPokemon) {
            if (pk != null) {
                for (Evolution checkEvo : pk.evolutionsFrom) {
                    if (checkEvo.type.usesLevel()) {
                        // bring down the level of this evo if it exceeds max
                        // level
                        if (checkEvo.extraInfo > maxLevel) {
                            checkEvo.extraInfo = maxLevel;
                            changedEvos.add(checkEvo);
                        }
                        // Now, seperately, if an intermediate level evo is too
                        // high, bring it down
                        for (Evolution otherEvo : pk.evolutionsTo) {
                            if (otherEvo.type.usesLevel()
                                    && otherEvo.extraInfo > maxIntermediateLevel) {
                                otherEvo.extraInfo = maxIntermediateLevel;
                                changedEvos.add(otherEvo);
                            }
                        }
                    }
                }
            }
        }
        TemplateData.putData("condensedEvos", changedEvos);
    }

    @Override
    public void randomizeEvolutions(boolean similarStrength, boolean sameType,
            boolean changeMethods, boolean limitToThreeStages, boolean forceChange,
            boolean noConverge, boolean forceGrowth, boolean forceLv1, boolean sameStage,
            boolean noLegendaries) {
        checkPokemonRestrictions();
        TemplateData.putData("logEvolutions", true);
        int stageLimit = limitToThreeStages ? 3 : 10;

        PokemonCollection globalSet = new PokemonCollection(getMainPokemonList());
        if (noLegendaries) {
            globalSet.filterLegendaries();
        }

        // Cache old evolutions for data later
        Map<Pokemon, List<Evolution>> originalFromEvos = new HashMap<Pokemon, List<Evolution>>();
        Map<Pokemon, List<Evolution>> originalToEvos = new HashMap<Pokemon, List<Evolution>>();
        for (Pokemon pk : getMainPokemonList()) {
            originalFromEvos.put(pk, new ArrayList<Evolution>(pk.evolutionsFrom));
            originalToEvos.put(pk, new ArrayList<Evolution>(pk.evolutionsTo));
        }

        Set<EvolutionPair> newEvoPairs = new HashSet<EvolutionPair>();
        Set<EvolutionPair> oldEvoPairs = new HashSet<EvolutionPair>();

        if (forceChange) {
            for (Pokemon pk : getMainPokemonList()) {
                for (Evolution ev : pk.evolutionsFrom) {
                    oldEvoPairs.add(new EvolutionPair(ev.from, ev.to));
                }
            }
        }

        List<Pokemon> replacements = new ArrayList<Pokemon>();

        int loops = 0;
        while (loops < 1) {
            // Setup for this loop.
            boolean hadError = false;
            for (Pokemon pk : getMainPokemonList()) {
                pk.evolutionsFrom.clear();
                pk.evolutionsTo.clear();
            }
            newEvoPairs.clear();

            // Shuffle pokemon list so the results aren't overly predictable.
            List<Pokemon> pokemonPool = new ArrayList<Pokemon>(globalSet.getPokes());
            Collections.shuffle(pokemonPool, this.random);

            for (Pokemon fromPK : getMainPokemonList()) {
                List<Evolution> oldFromEvos = originalFromEvos.get(fromPK);
                if (forceLv1 && oldFromEvos.size() == 0) {
                    oldFromEvos.add(
                            new Evolution(fromPK, randomPokemon(), true, EvolutionType.LEVEL, 1));
                }
                for (Evolution ev : oldFromEvos) {
                    // Pick a Pokemon as replacement
                    replacements.clear();

                    // Step 1: base filters
                    for (Pokemon pk : pokemonPool) {
                        // Prevent evolving into oneself (mandatory)
                        if (pk == fromPK) {
                            continue;
                        }

                        // Force same EXP curve (mandatory)
                        if (pk.growthCurve != fromPK.growthCurve) {
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
                        if (!forceLv1 && evoCycleCheck(fromPK, pk)) {
                            continue;
                        }

                        // Prevent evolution that exceeds stage limit
                        if (!forceLv1) {
                            Evolution tempEvo =
                                    new Evolution(fromPK, pk, false, EvolutionType.NONE, 0);
                            fromPK.evolutionsFrom.add(tempEvo);
                            pk.evolutionsTo.add(tempEvo);
                            boolean exceededLimit = false;

                            Set<Pokemon> related = relatedPokemon(fromPK);

                            for (Pokemon pk2 : related) {
                                int numPreEvos = numPreEvolutions(pk2, stageLimit);
                                if (numPreEvos >= stageLimit) {
                                    exceededLimit = true;
                                    break;
                                } else if (numPreEvos == stageLimit - 1
                                        && pk2.evolutionsFrom.size() == 0
                                        && originalFromEvos.get(pk2).size() > 0) {
                                    exceededLimit = true;
                                    break;
                                }
                            }

                            fromPK.evolutionsFrom.remove(tempEvo);
                            pk.evolutionsTo.remove(tempEvo);

                            if (exceededLimit) {
                                continue;
                            }
                        }

                        // Prevent evolution to already used newEvo
                        if (noConverge) {
                            for (EvolutionPair newEvoPair : newEvoPairs) {
                                if (pk == newEvoPair.to) {
                                    continue;
                                }
                            }
                        }

                        // Prevent stat total from decreasing
                        if (forceGrowth && pk.bstForPowerLevels() < fromPK.bstForPowerLevels()) {
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
                        Set<Pokemon> includeType = new HashSet<Pokemon>();
                        for (Pokemon pk : replacements) {
                            switch (fromPK.number) {
                                // Special case for Eevee
                                case 133:
                                    if (fromPK.number == 133) {
                                        if (pk.primaryType == ev.to.primaryType
                                                || (pk.secondaryType != null)
                                                        && pk.secondaryType == ev.to.primaryType) {
                                            includeType.add(pk);
                                        }
                                    }
                                    break;
                                default:
                                    if (pk.primaryType == fromPK.primaryType
                                            || (fromPK.secondaryType != null
                                                    && pk.primaryType == fromPK.secondaryType)
                                            || (pk.secondaryType != null
                                                    && pk.secondaryType == fromPK.primaryType)
                                            || (fromPK.secondaryType != null
                                                    && pk.secondaryType != null
                                                    && pk.secondaryType == fromPK.secondaryType)) {
                                        includeType.add(pk);
                                    } // end if
                            } // end switch
                        } // end for

                        if (includeType.size() != 0) {
                            replacements.retainAll(includeType);
                        }
                    }

                    // Step 3: Prevent different stage
                    if (sameStage) {
                        Set<Pokemon> equalStage = new HashSet<Pokemon>();
                        for (Pokemon pk : replacements) {
                            if (numOldPreEvolutions(originalToEvos, pk,
                                    stageLimit) == numOldPreEvolutions(originalToEvos, ev.to,
                                            stageLimit)) {
                                equalStage.add(pk);
                            }
                        }

                        if (equalStage.size() != 0) {
                            replacements.retainAll(equalStage);
                        }
                    }

                    if (!alreadyPicked.containsAll(replacements)
                            && (!forceLv1 || !similarStrength)) {
                        replacements.removeAll(alreadyPicked);
                    }

                    // Step 4: pick - by similar strength or otherwise
                    Pokemon picked = null;

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

                    // Step 5: add it to the new evos pool
                    Evolution newEvo =
                            new Evolution(fromPK, picked, ev.carryStats, ev.type, ev.extraInfo);
                    if (changeMethods) {
                        generateEvolutionType(newEvo);
                        updateExtraInfo(newEvo);
                    } else if (forceLv1) {
                        newEvo.setType(EvolutionType.LEVEL);
                        newEvo.setExtraInfo(1);
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
                return;
            } else {
                loops++;
            }
        }

        // If we made it out of the loop, we weren't able to randomize evos.
        throw new RandomizationException(
                "Not able to randomize evolutions in a sane amount of retries.");
    }

    private void generateEvolutionType(Evolution ev) {
        List<EvolutionType> usedMethods = ev.from.evolutionsFrom.stream().map(evo -> {
            // We can have multiple STONE, ITEM, and PARTY evolutions - they just need to be
            // different stones/items/pokemon
            if (!EvolutionType.isOfType("STONE", evo.type)
                    && !EvolutionType.isOfType("ITEM", evo.type)
                    && !EvolutionType.isOfType("PARTY", evo.type)) {
                return evo.type;
            } else {
                return EvolutionType.NONE;
            }
        }).collect(Collectors.toList());

        while (true) {
            EvolutionType et =
                    EvolutionType.randomFromGeneration(this.random, this.generationOfPokemon());

            // Can only have 2 happiness evos max (night + day)
            if (EvolutionType.isOfType("Happiness", et)
                    && EvolutionType.usesTypeOf("Happiness", usedMethods)) {
                // Already have a happiness evo - try something else
                if (usedMethods.contains(EvolutionType.HAPPINESS)
                        || (usedMethods.contains(EvolutionType.HAPPINESS_DAY)
                                && usedMethods.contains(EvolutionType.HAPPINESS_NIGHT))) {
                    continue;
                }
                // We only have DAY in the list since first check verified we don't have both
                if (usedMethods.contains(EvolutionType.HAPPINESS_DAY)) {
                    et = EvolutionType.HAPPINESS_NIGHT;
                }

                // We only have NIGHT in the list
                if (usedMethods.contains(EvolutionType.HAPPINESS_NIGHT)) {
                    et = EvolutionType.HAPPINESS_DAY;
                }
            }
            // Can only have 2 uncontrolled level evos max (high/low pv or male/female or
            // combination)
            else if (EvolutionType.isOfType("Uncontrolled", et)
                    && EvolutionType.usesTypeOf("Uncontrolled", usedMethods)) {
                // Already have level evo - try something else
                if (usedMethods.contains(EvolutionType.LEVEL)
                        || EvolutionType.usesTypeOf("BranchLevel", usedMethods, 2)) {
                    continue;
                }
                // We only have HIGH PV in the list since first check verified we don't have more
                // than 1 branch level
                // We only change the selected method if it is the same or generic LEVEL
                if (usedMethods.contains(EvolutionType.LEVEL_HIGH_PV)
                        && (et == EvolutionType.LEVEL_HIGH_PV || et == EvolutionType.LEVEL)) {
                    et = EvolutionType.LEVEL_LOW_PV;
                }
                // We only have LOW PV in the list
                else if (usedMethods.contains(EvolutionType.LEVEL_LOW_PV)
                        && (et == EvolutionType.LEVEL_LOW_PV || et == EvolutionType.LEVEL)) {
                    et = EvolutionType.LEVEL_HIGH_PV;
                }
                // We only have MALE in the list
                else if (usedMethods.contains(EvolutionType.LEVEL_MALE_ONLY)
                        && (et == EvolutionType.LEVEL_MALE_ONLY || et == EvolutionType.LEVEL)) {
                    et = EvolutionType.LEVEL_FEMALE_ONLY;
                }
                // We only have FEMALE in the list
                else if (usedMethods.contains(EvolutionType.LEVEL_FEMALE_ONLY)
                        && (et == EvolutionType.LEVEL_FEMALE_ONLY || et == EvolutionType.LEVEL)) {
                    et = EvolutionType.LEVEL_MALE_ONLY;
                }
            }
            // Can only have 1 TRADE evo (but multiple trade item)
            else if (EvolutionType.isOfType("Trade", et) && ev.from.evolutionsFrom.size() > 0) {
                // Regather the evolution methods because the other TRADE evolutions are marked as
                // NONE to prevent them from being counted as duplicates
                List<EvolutionType> tradeMethods = ev.from.evolutionsFrom.stream()
                        .filter(evo -> EvolutionType.isOfType("Trade", evo.type))
                        .map(evo -> evo.type).collect(Collectors.toList());

                // If this is a trade and we already used trade, try again
                if ((et == EvolutionType.TRADE && EvolutionType.usesTypeOf("Trade", tradeMethods))
                        || tradeMethods.contains(EvolutionType.TRADE)) {
                    continue;
                }
            }
            // Prevent duplicate evolution methods
            else if (usedMethods.contains(et)) {
                continue;
            }
            // Require attack and defense to be within 5 points of each other to be feasible
            else if (et == EvolutionType.LEVEL_ATK_DEF_SAME
                    || et == EvolutionType.LEVEL_ATTACK_HIGHER
                    || et == EvolutionType.LEVEL_DEFENSE_HIGHER) {
                if (Math.abs(ev.from.attack - ev.from.defense) > 5) {
                    continue;
                }
            }

            // Update the type and end the loop
            ev.type = et;
            break;
        }
    }

    @Override
    public void minimumCatchRate(int rateNonLegendary, int rateLegendary) {
        List<Pokemon> pokes = this.getMainPokemonList();
        for (Pokemon pkmn : pokes) {
            if (pkmn == null) {
                continue;
            }
            int minCatchRate = pkmn.isLegendary() ? rateLegendary : rateNonLegendary;
            pkmn.catchRate = Math.max(pkmn.catchRate, minCatchRate);
        }

    }

    @Override
    public void standardizeEXPCurves() {
        List<Pokemon> pokes = this.getMainPokemonList();
        for (Pokemon pkmn : pokes) {
            if (pkmn == null) {
                continue;
            }
            pkmn.growthCurve = pkmn.isLegendary() ? ExpCurve.SLOW : ExpCurve.MEDIUM_FAST;
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

    private void addMoveUpdate(int moveNum, int updateType) {
        if (!moveUpdates.containsKey(moveNum)) {
            boolean[] updateField = new boolean[4];
            updateField[updateType] = true;
            moveUpdates.put(moveNum, updateField);
        } else {
            moveUpdates.get(moveNum)[updateType] = true;
        }
    }

    private Pokemon pickEvoPowerLvlReplacement(List<Pokemon> pokemonPool, Pokemon current) {
        // start with within 10% and add 5% either direction till we find
        // something
        int currentBST = current.bstForPowerLevels();
        int minTarget = currentBST - currentBST / 10;
        int maxTarget = currentBST + currentBST / 10;
        List<Pokemon> canPick = new ArrayList<Pokemon>();
        List<Pokemon> emergencyPick = new ArrayList<Pokemon>();
        Set<Pokemon> nonNullPick = new HashSet<Pokemon>();
        int expandRounds = 0;
        while (canPick.isEmpty() || expandRounds < 3) {
            for (Pokemon pk : pokemonPool) {
                if (pk == null) {
                    continue;
                }
                // Always add to non-null set
                nonNullPick.add(pk);

                // Figure out if we can add to either list
                if (pk.bstForPowerLevels() >= minTarget && pk.bstForPowerLevels() <= maxTarget
                        && !canPick.contains(pk) && !emergencyPick.contains(pk)) {
                    if (alreadyPicked.contains(pk)) {
                        emergencyPick.add(pk);
                    } else {
                        canPick.add(pk);
                    }
                }
            }
            // Prevents a case where the currentBST is 0 and this enters an
            // infinite loop
            if (expandRounds >= 2 && emergencyPick.isEmpty()) {
                emergencyPick.addAll(nonNullPick);
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

    private static class EvolutionPair {

        private Pokemon from;
        private Pokemon to;

        public EvolutionPair(Pokemon from, Pokemon to) {
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
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            EvolutionPair other = (EvolutionPair) obj;
            if (from == null) {
                if (other.from != null) {
                    return false;
                }
            } else if (!from.equals(other.from)) {
                return false;
            }

            if (to == null) {
                if (other.to != null) {
                    return false;
                }
            } else if (!to.equals(other.to)) {
                return false;
            }

            return true;
        }
    }

    /**
     * Check whether adding an evolution from one Pokemon to another will cause an evolution cycle.
     *
     * @param from
     * @param to
     * @return
     */
    private boolean evoCycleCheck(Pokemon from, Pokemon to) {
        Evolution tempEvo = new Evolution(from, to, false, EvolutionType.NONE, 0);
        from.evolutionsFrom.add(tempEvo);
        Set<Pokemon> visited = new HashSet<Pokemon>();
        Set<Pokemon> recStack = new HashSet<Pokemon>();
        boolean recur = from.isCyclic(visited, recStack);
        from.evolutionsFrom.remove(tempEvo);
        return recur;
    }    

    private boolean checkForUnusedMove(List<Move> potentialList, Set<Integer> alreadyUsed) {
        for (Move mv : potentialList) {
            if (!alreadyUsed.contains(mv.number)) {
                return true;
            }
        }
        return false;
    }

    private List<Pokemon> allPokemonWithoutNull() {
        List<Pokemon> allPokes = new ArrayList<Pokemon>(this.getPokemon());
        allPokes.remove(0);
        return allPokes;
    }

    private Set<Pokemon> pokemonInArea(EncounterSet area) {
        Set<Pokemon> inArea = new TreeSet<Pokemon>();
        for (Encounter enc : area.encounters) {
            inArea.add(enc.pokemon);
        }
        return inArea;
    }

    private void rivalCarriesStarterUpdate(List<Trainer> currentTrainers, String prefix,
            int pokemonOffset) {
        // Find the highest rival battle #
        int highestRivalNum = 0;
        for (Trainer t : currentTrainers) {
            if (t.getTag() != null && t.getTag().startsWith(prefix)) {
                highestRivalNum = Math.max(highestRivalNum, Integer
                        .parseInt(t.getTag().substring(prefix.length(), t.getTag().indexOf('-'))));
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
            // Apply evolutions as appropriate
            if (timesEvolves == 0) {
                for (int j = 1; j <= 3; j++) {
                    changeStarterWithTag(currentTrainers, prefix + j + "-0", rivalStarter);
                }
                for (int j = 4; j <= 7; j++) {
                    for (int i = 0; i < 3; i++) {
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, rivalStarter);
                    }
                }
            } else if (timesEvolves == 1) {
                for (int j = 1; j <= 3; j++) {
                    changeStarterWithTag(currentTrainers, prefix + j + "-0", rivalStarter);
                }
                rivalStarter = pickRandomEvolutionOf(rivalStarter, false);
                for (int j = 4; j <= 7; j++) {
                    for (int i = 0; i < 3; i++) {
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, rivalStarter);
                    }
                }
            } else if (timesEvolves == 2) {
                for (int j = 1; j <= 2; j++) {
                    changeStarterWithTag(currentTrainers, prefix + j + "-" + 0, rivalStarter);
                }
                rivalStarter = pickRandomEvolutionOf(rivalStarter, true);
                changeStarterWithTag(currentTrainers, prefix + "3-0", rivalStarter);
                for (int i = 0; i < 3; i++) {
                    changeStarterWithTag(currentTrainers, prefix + "4-" + i, rivalStarter);
                }
                rivalStarter = pickRandomEvolutionOf(rivalStarter, false);
                for (int j = 5; j <= 7; j++) {
                    for (int i = 0; i < 3; i++) {
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, rivalStarter);
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
                // If a fully evolved pokemon, use throughout
                // Otherwise split by evolutions as appropriate
                if (timesEvolves == 0) {
                    for (int j = 1; j <= highestRivalNum; j++) {
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter);
                    }
                } else if (timesEvolves == 1) {
                    int j = 1;
                    for (; j <= highestRivalNum / 2; j++) {
                        if (getLevelOfStarter(currentTrainers, prefix + j + "-" + i) >= 30) {
                            break;
                        }
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter);
                    }
                    thisStarter = pickRandomEvolutionOf(thisStarter, false);
                    for (; j <= highestRivalNum; j++) {
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter);
                    }
                } else if (timesEvolves == 2) {
                    int j = 1;
                    for (; j <= highestRivalNum; j++) {
                        if (getLevelOfStarter(currentTrainers, prefix + j + "-" + i) >= 16) {
                            break;
                        }
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter);
                    }
                    thisStarter = pickRandomEvolutionOf(thisStarter, true);
                    for (; j <= highestRivalNum; j++) {
                        if (getLevelOfStarter(currentTrainers, prefix + j + "-" + i) >= 36) {
                            break;
                        }
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter);
                    }
                    thisStarter = pickRandomEvolutionOf(thisStarter, false);
                    for (; j <= highestRivalNum; j++) {
                        changeStarterWithTag(currentTrainers, prefix + j + "-" + i, thisStarter);
                    }
                }
            }
        }
    }

    private void rivalCarriesTeamUpdate(List<Trainer> currentTrainers, String prefix,
            int pokemonOffset) {
        HashMap<String, ArrayList<Trainer>> rivalTrainer =
                new HashMap<String, ArrayList<Trainer>>();
        int highestRivalNum = 0;
        for (Trainer t : currentTrainers) {
            if (t.getTag() != null && t.getTag().startsWith(prefix)) {
                if (!rivalTrainer.containsKey(t.getTag())) {
                    rivalTrainer.put(t.getTag(), new ArrayList<Trainer>());
                }
                rivalTrainer.get(t.getTag()).add(t);
                highestRivalNum = Math.max(highestRivalNum, Integer
                        .parseInt(t.getTag().substring(prefix.length(), t.getTag().indexOf('-'))));
            }
        }

        if (rivalTrainer.size() == 0) {
            // This rival type not used in this game
            return;
        }

        // Loop through each rival version
        for (int i = 0; i < 3; i++) {
            // Loop through each team
            for (int j = 1; j < highestRivalNum; j++) {
                ArrayList<Trainer> currList = rivalTrainer.get(prefix + j + "-" + i);
                if (currList == null) {
                    if (!isYellow() || j != 3) {
                        continue;
                    } else {
                        currList = rivalTrainer.get(prefix + j + "-0");
                    }
                }
                for (int t = 0; t < currList.size(); t++) {
                    // Last modified team - first round is lowest team
                    List<TrainerPokemon> tpkList = currList.get(t).getPokemon();
                    tpkList.sort(Comparator.comparing(TrainerPokemon::getLevel));

                    // Get the team that comes after the last modified team
                    List<TrainerPokemon> tpkNextList =
                            rivalTrainer.get(prefix + (j + 1) + "-" + i).get(t).getPokemon();
                    tpkNextList.sort(Comparator.comparing(TrainerPokemon::getLevel));

                    // Skip replacing for team size 1
                    if (tpkList.size() < 2) {
                        continue;
                    }

                    // Special Gen 1 - Rival losest weakest pokemon after 4th fight
                    if (isGen1() && prefix == "RIVAL") {
                        if ((isYellow() && j == 3) || (!isYellow() && j == 4)) {
                            tpkList = tpkList.subList(1, tpkList.size());
                        }
                    }

                    // Special Tag Battles
                    if (currList.get(t).getIsDoubleBattle()) {
                        tpkList = rivalTrainer.get(prefix + (j - 1) + "-" + i).get(t).getPokemon();
                    }

                    // Skip the weakest mons of the next team
                    // Accounts for when next team is larger or smaller
                    // Simple subtraction results in IndexOutOfBounds
                    int listOffset = 0;
                    while (tpkNextList.size() > tpkList.size() + listOffset) {
                        listOffset++;
                    }

                    for (int k = 0; k < tpkList.size(); k++) {
                        if (k >= tpkNextList.size()) {
                            break;
                        }
                        int timesEvolves = numEvolutions(tpkList.get(k).pokemon, 2);
                        if (timesEvolves == 0) {
                            tpkNextList.get(k + listOffset).pokemon = tpkList.get(k).pokemon;
                        } else {
                            // If greater or equal to evolution level, try to make it the next evo
                            int target = tpkList.get(k).pokemon
                                    .nearestEvoTarget(tpkNextList.get(k + listOffset).level);
                            if (target > -1) {
                                tpkNextList.get(k + listOffset).pokemon =
                                        pickRandomEvolutionOf(tpkList.get(k).pokemon, false);
                            } else {
                                tpkNextList.get(k + listOffset).pokemon = tpkList.get(k).pokemon;
                            }
                        }
                        tpkNextList.get(k + listOffset).resetMoves = true;
                    }

                    // Shuffle after finished
                    Collections.shuffle(currList.get(t).getPokemon(), random);
                }
            }
            // Shuffle and alter the last rival fight
            ArrayList<Trainer> trainerList = rivalTrainer.get(prefix + highestRivalNum + "-" + i);
            if (trainerList == null) {
                continue;
            }
            for (Trainer trainer : trainerList) {
                if (this instanceof Gen3RomHandler && isGen1()) {
                    // Randomize two pokemon
                    trainer.getPokemon().sort(Comparator.comparing(TrainerPokemon::getLevel));
                    trainer.getPokemon().get(0).pokemon = randomPokemon();
                    trainer.getPokemon().get(1).pokemon = randomPokemon();
                    noRepeatTaggedTeam(trainer, null);
                }
                Collections.shuffle(trainer.getPokemon(), random);
            }
        }
    }

    private Pokemon pickRandomEvolutionOf(Pokemon base, boolean mustEvolveItself) {
        // Used for "rival carries starter"
        // Pick a random evolution of base Pokemon, subject to
        // "must evolve itself" if appropriate.
        List<Pokemon> candidates = new ArrayList<Pokemon>();
        for (Evolution ev : base.evolutionsFrom) {
            if (!mustEvolveItself || ev.to.evolutionsFrom.size() > 0) {
                candidates.add(ev.to);
            }
        }

        if (candidates.size() == 0) {
            throw new RandomizationException(
                    "Random evolution called on a Pokemon without any usable evolutions.");
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    private int getLevelOfStarter(List<Trainer> currentTrainers, String tag) {
        for (Trainer t : currentTrainers) {
            if (t.getTag() != null && t.getTag().equals(tag)) {
                // Bingo, get highest level
                // last pokemon is given priority +2 but equal priority
                // = first pokemon wins, so its effectively +1
                // If it's tagged the same we can assume it's the same team
                // just the opposite gender or something like that...
                // So no need to check other trainers with same tag.
                int highestLevel = t.getPokemon().get(0).level;
                int trainerPkmnCount = t.getPokemon().size();
                for (int i = 1; i < trainerPkmnCount; i++) {
                    int levelBonus = (i == trainerPkmnCount - 1) ? 2 : 0;
                    if (t.getPokemon().get(i).level + levelBonus > highestLevel) {
                        highestLevel = t.getPokemon().get(i).level;
                    }
                }
                return highestLevel;
            }
        }
        return 0;
    }

    private void changeStarterWithTag(List<Trainer> currentTrainers, String tag, Pokemon starter) {
        for (Trainer t : currentTrainers) {
            if (t.getTag() != null && t.getTag().equals(tag)) {
                // Bingo
                // Change the highest level pokemon, not the last.
                // BUT: last gets +2 lvl priority (effectively +1)
                // same as above, equal priority = earlier wins
                noRepeatTaggedTeam(t, starter);
                TrainerPokemon bestPoke = t.getPokemon().get(0);
                int trainerPkmnCount = t.getPokemon().size();
                for (int i = 1; i < trainerPkmnCount; i++) {
                    int levelBonus = (i == trainerPkmnCount - 1) ? 2 : 0;
                    if (t.getPokemon().get(i).level + levelBonus > bestPoke.level) {
                        bestPoke = t.getPokemon().get(i);
                    }
                }
                bestPoke.pokemon = starter;
                bestPoke.resetMoves = true;
            }
        }

    }

    private void noRepeatTaggedTeam(Trainer trainer, Pokemon starter) {
        int tries = 0;
        Pokemon fullyEvolved = starter == null ? null : fullyEvolve(starter);
        // Loop over entire team.
        for (int i = 0; i < trainer.getPokemon().size(); i++) {
            if (tries > 100) {
                System.out.println("Unable to prevent rival from duplicating team.");
                break;
            }
            // Current suspect pokemon based on 'i'
            TrainerPokemon tp = trainer.getPokemon().get(i);
            // Check if suspect pokemon matches the starter or its full evolution
            if (tp.pokemon == starter || tp.pokemon == fullyEvolved) {
                // Randomize this pokemon
                tp.pokemon = pickReplacement(tp.pokemon, true, null, true, true);
                tp.resetMoves = true;
                // Redo this section of the loop
                i--;
                tries++;
                continue;
            }
            // Loop over the beginning of the team.
            for (int j = 0; j < i; j++) {
                if (tp.pokemon == trainer.getPokemon().get(j).pokemon) {
                    // Randomize this pokemon
                    tp.pokemon = pickReplacement(tp.pokemon, true, null, true, true);
                    tp.resetMoves = true;
                    // Redo the outer section of the loop
                    i--;
                    tries++;
                    break;
                }
            }
        }
    }

    // Return the max depth of pre-evolutions a Pokemon originally had
    private int numOldPreEvolutions(Map<Pokemon, List<Evolution>> preEvoMap, Pokemon pk,
            int maxInterested) {
        return numOldPreEvolutions(preEvoMap, pk, 0, maxInterested);
    }

    private int numOldPreEvolutions(Map<Pokemon, List<Evolution>> preEvoMap, Pokemon pk, int depth,
            int maxInterested) {
        if (preEvoMap.get(pk) == null || preEvoMap.get(pk).size() == 0) {
            return 0;
        } else {
            if (depth == maxInterested - 1) {
                return 1;
            } else {
                int maxPreEvos = 0;
                for (Evolution ev : preEvoMap.get(pk)) {
                    maxPreEvos = Math.max(maxPreEvos,
                            numOldPreEvolutions(preEvoMap, ev.from, depth + 1, maxInterested) + 1);
                }
                return maxPreEvos;
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
                    maxPreEvos = Math.max(maxPreEvos,
                            numPreEvolutions(ev.from, depth + 1, maxInterested) + 1);
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

    private Pokemon fullyEvolve(Pokemon pokemon) {
        Set<Pokemon> seenMons = new HashSet<Pokemon>();
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

            // pick a random evolution to continue from
            pokemon = pokemon.evolutionsFrom.get(random.nextInt(pokemon.evolutionsFrom.size())).to;
            seenMons.add(pokemon);
        }

        return pokemon;
    }

    private Set<Pokemon> relatedPokemon(Pokemon original) {
        Set<Pokemon> results = new HashSet<Pokemon>();
        results.add(original);
        Queue<Pokemon> toCheck = new LinkedList<Pokemon>();
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

    private void initializeTrainerGlobalSwapMap() {
        trainerTranslateMap = new HashMap<Pokemon, Pokemon>(getPokemon().size());
    }

    private Pokemon pickReplacement(Pokemon current, boolean usePowerLevels, Type type,
            boolean noLegendaries, boolean wonderGuardAllowed) {
        List<Pokemon> pickFrom = cachedReplacementLists.getPokes();
        Pokemon chosenPoke = null;

        if (trainerTranslateMap != null && trainerTranslateMap.containsKey(current)) {
            chosenPoke = trainerTranslateMap.get(current);
            // Type selection takes priority over global swap
            if (type == null || chosenPoke.primaryType == type
                    || chosenPoke.secondaryType == type) {
                return chosenPoke;
            }
        }

        if (type != null) {
            pickFrom = cachedReplacementLists.getPokesByType().get(type);
        }

        if (usePowerLevels) {
            // start with within 10% and add 5% either direction till we find
            // something
            int currentBST = current.bstForPowerLevels();
            int minTarget = currentBST - currentBST / 10;
            int maxTarget = currentBST + currentBST / 10;
            List<Pokemon> canPick = new ArrayList<Pokemon>();
            int expandRounds = 0;
            while (canPick.isEmpty() || (canPick.size() < 3 && expandRounds < 2)) {
                for (Pokemon pk : pickFrom) {
                    if (pk.bstForPowerLevels() >= minTarget && pk.bstForPowerLevels() <= maxTarget
                            && (wonderGuardAllowed
                                    || (pk.ability1 != GlobalConstants.WONDER_GUARD_INDEX
                                            && pk.ability2 != GlobalConstants.WONDER_GUARD_INDEX
                                            && pk.ability3 != GlobalConstants.WONDER_GUARD_INDEX))) {
                        canPick.add(pk);
                    }
                }
                minTarget -= currentBST / 20;
                maxTarget += currentBST / 20;
                expandRounds++;
            }
            chosenPoke = canPick.get(this.random.nextInt(canPick.size()));
        } else {
            if (wonderGuardAllowed) {
                chosenPoke = pickFrom.get(this.random.nextInt(pickFrom.size()));
            } else {
                chosenPoke = pickFrom.get(this.random.nextInt(pickFrom.size()));
                while (chosenPoke.ability1 == GlobalConstants.WONDER_GUARD_INDEX
                        || chosenPoke.ability2 == GlobalConstants.WONDER_GUARD_INDEX
                        || chosenPoke.ability3 == GlobalConstants.WONDER_GUARD_INDEX) {
                    chosenPoke = pickFrom.get(this.random.nextInt(pickFrom.size()));
                }
            }
        }

        // Don't override original selections
        // Prevent type conflicts from replacing global swap
        if (trainerTranslateMap != null && !trainerTranslateMap.containsKey(current)) {
            trainerTranslateMap.put(current, chosenPoke);
        }

        return chosenPoke;
    }

    /* Helper methods used by subclasses and/or this class */
    protected void checkPokemonRestrictions() {
        if (!restrictionsSet) {
            setPokemonPool(null, null);
        }
    }

    public List<Pokemon> getMainPokemonList() {
        return mainPokemonList;
    }

    protected void applyCamelCaseNames() {
        List<Pokemon> pokes = getPokemon();
        for (Pokemon pkmn : pokes) {
            if (pkmn == null) {
                continue;
            }
            pkmn.name = RomFunctions.camelCase(pkmn.name);
        }
        TemplateData.putMap("tweakMap", MiscTweak.LOWER_CASE_POKEMON_NAMES.getTweakName(), true);
    }

    /* Default Implementations */
    /* Used when a subclass doesn't override */
    /*
     * The implication here is that these WILL be overridden by at least one subclass.
     */
    @Override
    public boolean canChangeStarters() {
        return true;
    }

    @Override
    public boolean isTypeInGame(Type type) {
        return type.isHackOnly == false;
    }

    @Override
    public String abilityName(int number) {
        return "";
    }

    @Override
    public boolean hasTimeBasedEncounters() {
        // DEFAULT: no
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Pokemon> bannedForWildEncounters() {
        return (List<Pokemon>) Collections.EMPTY_LIST;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Integer> getMovesBannedFromLevelup() {
        return (List<Integer>) Collections.EMPTY_LIST;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Pokemon> bannedForStaticPokemon() {
        return (List<Pokemon>) Collections.EMPTY_LIST;
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
    public Map<String, Integer> getGameBreakingMoves() {
        Map<String, Integer> bList = new HashMap<>();
        bList.put("SONICBOOM", 49);
        bList.put("DRAGON RAGE", 82);
        return bList;
    }

    @Override
    public boolean isYellow() {
        return false;
    }

    @Override
    public boolean isGen1() {
        return false;
    }

    @Override
    public void setDisableROMHack(boolean disableROMHack) {
        this.disableROMHack = disableROMHack;
    }

    @Override
    public boolean isROMHack() {
        // override until detection implemented
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
    public void applyMiscTweak(MiscTweak tweak) {
        // default: do nothing
    }
}
