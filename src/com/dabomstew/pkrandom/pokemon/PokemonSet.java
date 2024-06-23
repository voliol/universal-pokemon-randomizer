package com.dabomstew.pkrandom.pokemon;

import javax.print.attribute.UnmodifiableSetException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An extension of {@link GenericPokemonSet} instantiated to Pokemon.
 * Adds various evolution-related functions.
 */
public class PokemonSet extends GenericPokemonSet<Pokemon> {

    public PokemonSet(){
        super();
    }

    public PokemonSet(Collection<? extends Pokemon> cloneFrom) {
        super(cloneFrom);
    }

    //Pass-through methods
    //This seems a non-ideal solution, but it's better than copying the code?
    @Override
    public PokemonSet filter(Predicate<? super Pokemon> predicate) {
        return new PokemonSet(super.filter(predicate));
    }

    @Override
    public PokemonSet buildSet(Function<? super Pokemon, Collection<Pokemon>> function) {
        return new PokemonSet(super.buildSet(function));
    }

    @Override
    public PokemonSet buildSetSingle(Function<? super Pokemon, Pokemon> function) {
        return new PokemonSet(super.buildSetSingle(function));
    }

    @Override
    public PokemonSet filterBuiltSetSingle(Function<? super Pokemon, Pokemon> function) {
        return new PokemonSet(super.filterBuiltSetSingle(function));
    }

    @Override
    public PokemonSet filterBuiltSet(Function<? super Pokemon, Collection<Pokemon>> function) {
        return new PokemonSet(super.filterBuiltSet(function));
    }

    @Override
    public PokemonSet filterByType(Type type) {
        return new PokemonSet(super.filterByType(type));
    }

    @Override
    public PokemonSet filterCosmetic() {
        return new PokemonSet(super.filterCosmetic());
    }

    @Override
    public PokemonSet filterNonCosmetic() {
        return new PokemonSet(super.filterNonCosmetic());
    }

    //End pass-through methods

    //Evolution methods

    /**
     * Returns all Pokemon in this set that the given Pokemon can evolve directly into.
     * @param pokemon The Pokemon to get evolutions of.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return A PokemonSet containing all direct evolutions of the given Pokemon in this set.
     */
    public PokemonSet filterEvolutions(Pokemon pokemon, boolean useOriginal) {
        PokemonSet evolvedPokemon = pokemon.getEvolvedPokemon(useOriginal);
        evolvedPokemon.retainAll(this);
        return evolvedPokemon;
    }

    /**
     * Returns all Pokemon in this set that the given Pokemon evolves from.
     * @param pokemon The Pokemon to get the pre-evolution of.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return A PokemonSet containing all Pokemon that the given Pokemon evolves from in this set.
     */
    public PokemonSet filterPreEvolutions(Pokemon pokemon, boolean useOriginal) {
        //I *think* there are no cases of merged evolution? But... better not to assume that.
        PokemonSet preEvolvedPokemon = pokemon.getPreEvolvedPokemon(useOriginal);
        preEvolvedPokemon.retainAll(this);
        return preEvolvedPokemon;
    }

    /**
     * Checks whether this set contains any evolved forms of the given Pokemon.
     * @param pokemon The Pokemon to check for evolutions of.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return true if this set contains at least one evolved form of the given Pokemon, false otherwise.
     */
    public boolean hasEvolutions(Pokemon pokemon, boolean useOriginal) {
        PokemonSet evolvedPokemon = pokemon.getEvolvedPokemon(useOriginal);
        for (Pokemon evo : evolvedPokemon) {
            if(this.contains(evo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether this set contains any Pokemon that can evolve into the given Pokemon.
     * @param pokemon The Pokemon to check for pre-evolutions of.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return true if this set contains at least one pre-evolved form of the given Pokemon, false otherwise.
     */
    public boolean hasPreEvolutions(Pokemon pokemon, boolean useOriginal) {
        PokemonSet preEvolvedPokemon = pokemon.getPreEvolvedPokemon(useOriginal);
        for (Pokemon prevo : preEvolvedPokemon) {
            if(this.contains(prevo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all Pokemon in this set that no other Pokemon in this set evolves into.
     * @param contiguous Whether to keep Pokemon that other Pokemon in the set can evolve indirectly into.
     *                   For example, if this set includes Weedle and Beedrill but not Kakuna,
     *                   the returned set will include Beedrill only if this parameter is true.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return A PokemonSet containing all Pokemon that no other Pokemon in this set evolves into.
     */
    public PokemonSet filterFirstEvolutionAvailable(boolean contiguous, boolean useOriginal) {
        PokemonSet firstInLines = new PokemonSet();
        for(Pokemon pokemon : this) {
            if (contiguous) {
                if (!this.hasPreEvolutions(pokemon, useOriginal)) {
                    firstInLines.add(pokemon);
                }

            } else {

                PokemonSet checked = new PokemonSet();
                Queue<Pokemon> toCheck = new ArrayDeque<>();
                toCheck.add(pokemon);
                boolean hasPrevo = false;
                while(!toCheck.isEmpty()) {
                    Pokemon checking = toCheck.remove();
                    if(checked.contains(checking)) {
                        continue; //continue inner loop only
                    }
                    if(this.hasPreEvolutions(checking, useOriginal)) {
                        hasPrevo = true;
                        break; //break inner loop only
                    }
                    checked.add(checking);

                    toCheck.addAll(pokemon.getPreEvolvedPokemon(useOriginal));
                }

                if(!hasPrevo) {
                    firstInLines.add(pokemon);
                }
            }
        }
        return firstInLines;
    }

    /**
     * Returns all Pokemon in this set that no other Pokemon (in this set or otherwise) evolves into.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return A PokemonSet containing all basic Pokemon in this set.
     */
    public PokemonSet filterBasic(boolean useOriginal) {
        return this.filter(p -> p.getPreEvolvedPokemon(useOriginal).isEmpty());
    }

    /**
     * Returns all Pokemon in this set that evolve into no other Pokemon (in this set or otherwise).
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return A PokemonSet containing all final Pokemon in this set.
     */
    public PokemonSet filterFinalEvos(boolean useOriginal) {
        return this.filter(p -> p.getEvolvedPokemon(useOriginal).isEmpty());
    }

    /**
     * Returns all Pokemon in this set that are related to no other Pokemon (in this set or otherwise).
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return A PokemonSet containing all non-evolving Pokemon in this set.
     */
    public PokemonSet filterStandalone(boolean useOriginal) {
        return this.filter(p -> p.getPreEvolvedPokemon(useOriginal).isEmpty() &&
                p.getEvolvedPokemon(useOriginal).isEmpty());
    }

    /**
     * Returns all Pokemon in this set that both evolve from and to at least one other Pokemon.
     * The related Pokemon are not required to be in the set.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return A PokemonSet containing all non-evolving Pokemon in this set.
     */
    public PokemonSet filterMiddleEvos(boolean useOriginal) {
        return this.filter(p -> !p.getPreEvolvedPokemon(useOriginal).isEmpty() &&
                !p.getEvolvedPokemon(useOriginal).isEmpty());
    }

    /**
     * Returns all Pokemon for which evolutionary lines of at least the given length are contained
     * within this set.
     * In the case of branching evolutions, only branches of the correct length will be included.
     * @param length the number of Pokemon required in the evolutionary line. At least 1. Counts itself.
     * @param allowGaps Whether to allow lines with one or more of the middle Pokemon missing.
     *                  For example, if allowGaps is true, and the set contains Weedle and Beedrill but not Kakuna,
     *                  they will be included in the returned set if length is 2 or 3.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return a PokemonSet containing all valid Pokemon.
     */
    public PokemonSet filterEvoLinesAtLeastLength(int length, boolean allowGaps, boolean useOriginal) {
        if(length < 1) {
            throw new IllegalArgumentException("Invalid evolutionary line length.");
        }
        if(length == 1) {
            return new PokemonSet(this);
        }

        PokemonSet validEvoLines = new PokemonSet();
        PokemonSet firstInLines = this.filterFirstEvolutionAvailable(!allowGaps, useOriginal);

        Map<Pokemon, PokemonSet> currentStage = new HashMap<>();
        for(Pokemon pokemon : firstInLines) {
            currentStage.put(pokemon, new PokemonSet());
        }

        for(int i = 2; !currentStage.isEmpty(); i++) {
            Map<Pokemon, PokemonSet> nextStage = new HashMap<>();
            for(Pokemon poke : currentStage.keySet()) {
                if(!allowGaps && !this.contains(poke)) {
                    //kill this line
                    continue;
                }

                PokemonSet lineSoFar = new PokemonSet(currentStage.get(poke));
                if(this.contains(poke)){
                    lineSoFar.add(poke);

                    if(i >= length) {
                        validEvoLines.addAll(lineSoFar);
                    }
                }

                for(Pokemon evo : poke.getEvolvedPokemon(useOriginal)) {
                    nextStage.put(evo, lineSoFar);
                }
            }

            currentStage = nextStage;
        }

        return validEvoLines;
    }

    /**
     * Gets all Pokemon in the set which have at least the specified number of evolution stages
     * before and after them. <br>
     * If the Pokemon is in a cycle, guarantees that the total length of the cycle is greater than
     * before + 1 + after.
     * @param before The number of stages before this Pokemon. 0+.
     * @param after The number of stages after this Pokemon. 0+.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return A PokemonSet containing all Pokemon in the specified position.
     */
    public PokemonSet filterHasEvoStages(int before, int after, boolean useOriginal) {
        if (before < 0 || after < 0) {
            throw new IllegalArgumentException("Cannot have a negative number of evolutions!");
        }
        if (before == 0 && after == 0) {
            //impossible not to have these
            return new PokemonSet(this);
        }

        return filter(p -> {
            if (this.isInEvoCycle(p, useOriginal)) {
                return this.getNumberEvoStagesAfter(p, useOriginal) >= before + 1 + after;
            } else {
                return getNumberEvoStagesBefore(p, useOriginal) >= before
                        && getNumberEvoStagesAfter(p, useOriginal) >= after;
            }
        });
    }

    /**
     * Finds the largest number of evolutionary steps contained in this set that could evolve into
     * the given Pokemon.
     * For example, a Pokemon which has both a basic and a once-evolved Pokemon that can evolve into
     * it would return 2 (for the once-evolved Pokemon).
     * If the pokemon is in an evolutionary cycle, it will count each pokemon in the cycle,
     * including itself, once.
     * @param pokemon The Pokemon to find the evolutionary count for.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return The number of Pokemon in the longest line before this Pokemon.
     */
    public int getNumberEvoStagesBefore(Pokemon pokemon, boolean useOriginal) {
        int numStages = 0;
        PokemonSet currentStage = new PokemonSet();
        currentStage.add(pokemon);

        PokemonSet checked = new PokemonSet();

        while(!currentStage.isEmpty()) {
            PokemonSet previousStage = new PokemonSet();
            for(Pokemon poke : currentStage) {
                if(checked.contains(poke)) {
                    continue;
                }
                previousStage.addAll(this.filterPreEvolutions(poke, useOriginal));
                checked.add(poke);
            }
            if(!previousStage.isEmpty()) {
                numStages++;
            }
            currentStage = previousStage;
        }

        return numStages;
    }

    /**
     * Finds the largest number of evolutionary steps contained in this set that the given Pokemon
     * can evolve into.<br>
     * For example, a Pokemon which evolves into two Pokemon, one of which can evolve again,
     * would return 2 (for the Pokemon which can evolve again.)<br>
     * If the pokemon is in an evolutionary cycle, it will count each pokemon in the cycle,
     * including itself, once.
     * @param pokemon The Pokemon to find the evolutionary count for.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return The number of Pokemon in the longest line after this Pokemon.
     */
    public int getNumberEvoStagesAfter(Pokemon pokemon, boolean useOriginal) {
        int numStages = 0;
        PokemonSet currentStage = new PokemonSet();
        currentStage.add(pokemon);
        PokemonSet checked = new PokemonSet();

        while(!currentStage.isEmpty()) {
            PokemonSet nextStage = new PokemonSet();
            for(Pokemon poke : currentStage) {
                if(checked.contains(poke)) {
                    continue;
                }
                nextStage.addAll(this.filterEvolutions(poke, useOriginal));
                checked.add(poke);
            }
            if(!nextStage.isEmpty()) {
                numStages++;
            }
            currentStage = nextStage;
        }

        return numStages;
    }

    /**
     * Finds the longest evolutionary line in this set that the given Pokemon belongs to. <br>
     * If the pokemon is in an evolutionary cycle, it will count each pokemon in the cycle,
     * including itself, once.
     * @param pokemon
     * @param useOriginal
     * @return The number of Pokemon in the longest evolutionary line, including itself.
     */
    public int getLongestEvoLine(Pokemon pokemon, boolean useOriginal) {
        if(isInEvoCycle(pokemon, useOriginal)) {
            return getNumberEvoStagesAfter(pokemon, useOriginal);
        } else {
            return getNumberEvoStagesBefore(pokemon, useOriginal) + 1
                    + getNumberEvoStagesAfter(pokemon, useOriginal);
        }
    }

    /**
     * Checks whether the given Pokemon is in an evolutionary cycle for which all Pokemon
     * are contained within this set.
     * @param pokemon The Pokemon to check for cycles.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return Whether the Pokemon is in a cycle contained in this set.
     */
    public boolean isInEvoCycle(Pokemon pokemon, boolean useOriginal) {
        PokemonSet currentStage = new PokemonSet();
        currentStage.add(pokemon);
        PokemonSet checked = new PokemonSet();

        while(!currentStage.isEmpty()) {
            PokemonSet nextStage = new PokemonSet();
            for(Pokemon poke : currentStage) {
                if(poke == pokemon) {
                    return true;
                }
                if(checked.contains(poke)) {
                    continue;
                }
                nextStage.addAll(this.filterEvolutions(poke, useOriginal));
                checked.add(poke);
            }
            currentStage = nextStage;
        }

        return false;
    }

    //Evolution subset: Family methods

    /**
     * Adds the given Pokemon and every evolutionary relative to this set, if they are
     * not already contained in the set.
     *
     * @param pokemon     The Pokemon to add the family of.
     * @param useOriginal Whether to use the pre-randomization evolution data.
     * @return True if any Pokemon were added to the set, false otherwise.
     */
    public boolean addFamily(Pokemon pokemon, boolean useOriginal) {
        return addAll(pokemon.getFamily(useOriginal));
    }

    /**
     * Removes the given Pokemon and every evolutionary relative from this set, if they
     * are contained in the set.
     *
     * @param pokemon The Pokemon to remove the family of.
     * @return If any Pokemon were removed from the set.
     */
    public boolean removeFamily(Pokemon pokemon, boolean useOriginal) {
        return removeAll(pokemon.getFamily(useOriginal));
    }

    /**
     * Returns all members of the given Pokemon's evolutionary family that this set contains.
     * Returns an empty set if no members are in this set.
     *
     * @param pokemon     The Pokemon to get the family of.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return a PokemonSet containing every member of the given Pokemon's family
     */
    public PokemonSet filterFamily(Pokemon pokemon, boolean useOriginal) {
        PokemonSet family = pokemon.getFamily(useOriginal);
        family.retainAll(this);
        return family;
    }

    /**
     * Creates a new set containing the full evolutionary families of all Pokemon in this set.
     *
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return a PokemonSet containing all Pokemon in this set and all their evolutionary relatives.
     */
    public PokemonSet buildFullFamilies(boolean useOriginal) {
        //Could just use buildSetMulti, but this is more efficient
        //(And it's already written)
        PokemonSet allRelatedPokemon = new PokemonSet();
        for (Pokemon pokemon : this) {
            if (!allRelatedPokemon.contains(pokemon)) {
                allRelatedPokemon.addFamily(pokemon, useOriginal);
            }
        }
        return allRelatedPokemon;
    }

    /**
     * Adds to this set the evolutionary families of every Pokemon in this set.
     *
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return True if any Pokemon were added, false otherwise.
     */
    public boolean addFullFamilies(boolean useOriginal) {
        PokemonSet allWithFamilies = this.buildFullFamilies(useOriginal);
        boolean changed = this.addAll(allWithFamilies);
        return changed;
    }

    /**
     * Adds to this set all Pokemon in the given set and their full evolutionary families.
     *
     * @param source      The set to add Pokemon and families from.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return True if any Pokemon were added, false otherwise.
     */
    public boolean addAllFamilies(PokemonSet source, boolean useOriginal) {
        boolean anyChanged = false;
        for (Pokemon poke : source) {
            boolean changed = this.addFamily(poke, useOriginal);
            if (changed) {
                anyChanged = true;
            }
        }

        return anyChanged;
    }

    /**
     * Removes from this set all Pokemon that are neither contained within the given set nor
     * an evolutionary relative of a Pokemon that is.
     *
     * @param source      The set to keep Pokemon and families from.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return True if any Pokemon were removes, false otherwise.
     */
    public boolean retainAllFamilies(PokemonSet source, boolean useOriginal) {
        PokemonSet families = source.buildFullFamilies(useOriginal);

        return this.retainAll(families);
    }

    /**
     * Returns all members of the given Pokemon's evolutionary family
     * that are contained uninterrupted within this set.
     * Returns an empty set if the given Pokemon is not in this set.
     *
     * @param pokemon     The Pokemon to get the family of.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return a PokemonSet containing every Pokemon related to the given Pokemon by members of this set.
     */
    public PokemonSet filterContiguousFamily(Pokemon pokemon, boolean useOriginal) {
        PokemonSet family = new PokemonSet();
        if (!this.contains(pokemon)) {
            return family;
        }

        Queue<Pokemon> toCheck = new ArrayDeque<>();
        toCheck.add(pokemon);
        while (!toCheck.isEmpty()) {
            Pokemon checking = toCheck.remove();
            if (family.contains(checking)) {
                continue;
            }
            family.add(checking);

            toCheck.addAll(this.filterEvolutions(checking, useOriginal));
            toCheck.addAll(this.filterPreEvolutions(checking, useOriginal));
        }

        return family;
    }

    //end evolution and family methods

    public static PokemonSet unmodifiable(Collection<Pokemon> source) {
        return new PokemonSet.UnmodifiablePokemonSet(source);
    }

    /**
     * Just what it sounds like, a {@link PokemonSet} which throws {@link UnmodifiableSetException}
     * whenever modifications are attempted.
     */
    private static class UnmodifiablePokemonSet extends PokemonSet {
        private final boolean unmodifiable;

        public UnmodifiablePokemonSet(Collection<? extends Pokemon> original) {
            super(original);
            unmodifiable = true; // since you can't use the super constructor if add() is always impossible
        }

        @Override
        public boolean add(Pokemon pk) {
            if (unmodifiable) {
                throw new UnsupportedOperationException();
            } else {
                return super.add(pk);
            }
        }

        @Override
        public boolean remove(Object o) {
            throw new UnmodifiableSetException();
        }

        @Override
        public void clear() {
            throw new UnmodifiableSetException();
        }

        @Override
        public boolean removeIf(Predicate filter) {
            throw new UnmodifiableSetException();
        }
    }
}