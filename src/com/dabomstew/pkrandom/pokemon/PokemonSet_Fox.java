package com.dabomstew.pkrandom.pokemon;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An unordered set of Pokemon species. Guarantees uniqueness (no species can be in the set twice).
 * Automatically sorts by type and has other helper functions.
 */
public class PokemonSet extends HashSet<Pokemon> {

    private EnumMap<Type, HashSet<Pokemon>> typeMap = new EnumMap<>(Type.class);
    ArrayList<Pokemon> randomCache = null;

    /**
     * Creates an empty PokemonSet.
     */
    public PokemonSet() {
        super();
    }

    /**
     * Creates a new PokemonSet containing every Pokemon in the given Collection.
     * @param cloneFrom the Collection to copy from.
     */
    public PokemonSet(Collection<? extends Pokemon> cloneFrom) {
        super(cloneFrom);
    }

    //Basic functions

    public boolean add(Pokemon pokemon) {
        if(this.contains(pokemon) || pokemon == null) {
            return false;
        }
        randomCache = null;
        super.add(pokemon);

        addToType(pokemon, pokemon.getPrimaryType());
        if(pokemon.getSecondaryType() != null) {
            addToType(pokemon, pokemon.getSecondaryType());
        }

        return true;
    }

    /**
     * Internal. Adds the given Pokemon to the typeMap in the given Type.
     * @param pokemon The Pokemon to add.
     * @param type The Type to add to.
     */
    private void addToType(Pokemon pokemon, Type type) {
        HashSet<Pokemon> typeSet = typeMap.get(type);
        if(typeSet == null) {
            typeSet = new HashSet<>();
            typeMap.put(type, typeSet);
        }
        typeSet.add(pokemon);
    }

    public boolean remove(Object o) {
        if(!(o instanceof Pokemon)) {
            return false;
        }
        Pokemon pokemon = (Pokemon) o;
        if(!this.contains(pokemon)) {
            return false;
        }
        randomCache = null;

        super.remove(pokemon);
        typeMap.get(pokemon.getPrimaryType()).remove(pokemon);
        if(pokemon.getSecondaryType() != null) {
            typeMap.get(pokemon.getSecondaryType()).remove(pokemon);
        }
        return true;
    }


    public boolean addAll(Collection<? extends Pokemon> c) {
        boolean changed = false;
        for (Pokemon pokemon : c) {
            boolean added = this.add(pokemon);
            if(added) {
                changed = true;
            }
        }
        return changed;
    }

    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        Iterator<Pokemon> itor = this.iterator();
        while(itor.hasNext()) {
            Pokemon pokemon = itor.next();
            if(!c.contains(pokemon)) {
                itor.remove();
                changed = true;
            }
        }
        return changed;
    }

    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            boolean removed = this.remove(o);
            if(removed) {
                changed = true;
            }
        }
        return changed;
    }

    public void clear() {
        super.clear();
        typeMap = new EnumMap<>(Type.class);
        randomCache = null;
    }

    /**
     * Returns the subset of this set for which the predicate function returns true.
     * @param predicate The function to test Pokemon against.
     * @return A PokemonSet containing every Pokemon in this set for which predicate returns true.
     */
    public PokemonSet filter(Predicate<Pokemon> predicate) {
        PokemonSet filtered = new PokemonSet();
        for (Pokemon pk : this) {
            if (predicate.test(pk)) {
                filtered.add(pk);
            }
        }
        return filtered;
    }

    //BuildSet variants

    /**
     * Builds a new set by running the given function on each Pokemon in this set,
     * and adding its return value to the new set.
     * @param function The function to run on each Pokemon in this set.
     * @return The PokemonSet containing all Pokemon returned after running the given function.
     */
    public PokemonSet buildSetSingle(Function<Pokemon, Pokemon> function) {
        PokemonSet newSet = new PokemonSet();

        for(Pokemon pokemon : this) {
            newSet.add(function.apply(pokemon));
        }

        return newSet;
    }

    /**
     * Builds a new set by running the given function on each Pokemon in this set,
     * and adding its return value to the new set.
     * @param function The function to run on each Pokemon in this set.
     * @return The PokemonSet containing all Pokemon returned after running the given function.
     */
    public PokemonSet buildSet(Function<Pokemon, PokemonSet> function) {
        PokemonSet newSet = new PokemonSet();

        for(Pokemon pokemon : this) {
            newSet.addAll(function.apply(pokemon));
        }

        return newSet;
    }

    /**
     * Builds a new set by running the given function on each Pokemon in this set,
     * and adding its return value to the new set only if that Pokemon is contained in this set.
     * @param function The function to run on each Pokemon in this set.
     * @return The PokemonSet containing all Pokemon in this set returned after running the given function.
     */
    public PokemonSet filterBuiltSetSingle(Function<Pokemon, Pokemon> function) {
        PokemonSet builtSet = this.buildSetSingle(function);
        builtSet.retainAll(this);
        return builtSet;
    }

    /**
     * Builds a new set by running the given function on each Pokemon in this set,
     * and adding its return value to the new set only if that Pokemon is contained in this set.
     * @param function The function to run on each Pokemon in this set.
     * @return The PokemonSet containing all Pokemon in this set returned after running the given function.
     */
    public PokemonSet filterBuiltSet(Function<Pokemon, PokemonSet> function) {
        PokemonSet builtSet = this.buildSet(function);
        builtSet.retainAll(this);
        return builtSet;
    }

    /**
     * Removes from this set all Pokemon that were not returned by running the given function
     * on at least one Pokemon in the set. <br>
     * In other words, runs the given function on all Pokemon in the set to build a new set,
     * then removes all Pokemon not contained in that set.
     * @param function The function to run on all Pokemon in the set.
     * @return Whether any Pokemon were removed from the set.
     */
    public boolean retainBuiltSetSingle(Function<Pokemon, Pokemon> function) {
        PokemonSet builtSet = this.buildSetSingle(function);
        return this.retainAll(builtSet);
    }

    /**
     * Removes from this set all Pokemon that were not returned by running the given function
     * on at least one Pokemon in the set. <br>
     * In other words, runs the given function on all Pokemon in the set to build a new set,
     * then removes all Pokemon not contained in that set.
     * @param function The function to run on all Pokemon in the set.
     * @return Whether any Pokemon were removed from the set.
     */
    public boolean retainBuiltSet(Function<Pokemon, PokemonSet> function) {
        PokemonSet builtSet = this.buildSet(function);
        return this.retainAll(builtSet);
    }

    //AddBuiltSet and RemoveBuiltSet might also be relevant

    //End basic functions

    //Type Zone
    /**
     * Returns every Pokemon in this set which has the given type.
     * @param type The type to match.
     * @return a new PokemonSet containing every Pokemon of the given type.
     */
    public PokemonSet filterByType(Type type) {
        if(typeMap.get(type) == null) {
            return new PokemonSet();
        } else {
            return new PokemonSet(typeMap.get(type));
        }
    }

    /**
     * Returns the number of Pokemon in this set which have the given type.
     * @param type The type to count Pokemon of.
     * @return The number of Pokemon of the given type.
     */
    public int getCountOfType(Type type) {
        if(typeMap.get(type) == null) {
            return 0;
        } else {
            return typeMap.get(type).size();
        }
    }

    /**
     * Finds if all Pokemon in this set share a type.
     * If two types are shared, will return the primary type of an arbitrary Pokemon in the set,
     * unless that type is Normal; in this case will return the secondary type.
     * @return The Type shared by all the pokemon, or null if none was shared.
     */
    public Type findSharedType() {
        if(this.isEmpty()) {
            return null;
        }
        Iterator<Pokemon> itor = this.iterator();
        Pokemon poke = itor.next();

        Type primary = poke.getPrimaryType();
        Type secondary = poke.getSecondaryType();

        //we already sorted all the Pokemon by type, so we can take a shortcut
        if(typeMap.get(primary).size() == this.size()) {
            //primary is a theme!
            if(primary != Type.NORMAL) {
                return primary;
            } else {
                //check if secondary is also a theme,
                //because Normal is less significant than, say, Flying.
                if(secondary != null && typeMap.get(secondary).size() == this.size()) {
                    //secondary IS a theme!
                    return secondary;
                } else {
                    return primary;
                }
            }
        }
        //primary wasn't a theme. is secondary?
        if(secondary != null && typeMap.get(secondary).size() == this.size()) {
            //secondary IS a theme!
            return secondary;
        } else {
            return null;
        }
    }

    /**
     * Finds if all Pokemon in this set shared a type before randomization.
     * If two types were shared, will return the original primary type
     * of an arbitrary Pokemon in the set, unless that type is Normal;
     * in this case will return the secondary type.
     * @return The Type shared by all the pokemon, or null if none was shared.
     */
    public Type findOriginalSharedType() {
        if(this.isEmpty()) {
            return null;
        }
        Iterator<Pokemon> itor = this.iterator();
        Pokemon poke = itor.next();
        Type primary = poke.getOriginalPrimaryType();
        Type secondary = poke.getOriginalSecondaryType();

        while(itor.hasNext()) {
            poke = itor.next();
            if(secondary != null) {
                if (secondary != poke.getOriginalPrimaryType() && secondary != poke.getOriginalSecondaryType()) {
                    secondary = null;
                }
            }
            if (primary != poke.getOriginalPrimaryType() && primary != poke.getOriginalSecondaryType()) {
                primary = secondary;
                secondary = null;
            }
            if (primary == null) {
                return null; //we've determined there's no type theme, no need to run through the rest of the set.
            }
        }

        if(primary == Type.NORMAL && secondary != null) {
            return secondary;
        } else {
            return primary;
        }
    }

    //End Type Zone

    //Family Zone
    /**
     * Adds the given Pokemon and every evolutionary relative to this set, if they are
     * not already contained in the set.
     * @param pokemon The Pokemon to add the family of.
     * @param useOriginal Whether to use the pre-randomization evolution data.
     * @return True if any Pokemon were added to the set, false otherwise.
     */
    public boolean addFamily(Pokemon pokemon, boolean useOriginal) {
        return addAll(pokemon.getFamily(useOriginal));
    }

    /**
     * Removes the given Pokemon and every evolutionary relative from this set, if they
     * are contained in the set.
     * @param pokemon The Pokemon to remove the family of.
     * @return If any Pokemon were removed from the set.
     */
    public boolean removeFamily(Pokemon pokemon, boolean useOriginal) {
        return removeAll(pokemon.getFamily(useOriginal));
    }

    /**
     * Returns all members of the given Pokemon's evolutionary family that this set contains.
     * Returns an empty set if no members are in this set.
     * @param pokemon The Pokemon to get the family of.
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
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return a PokemonSet containing all Pokemon in this set and all their evolutionary relatives.
     */
    public PokemonSet buildFullFamilies(boolean useOriginal) {
        //Could just use buildSetMulti, but this is more efficient
        //(And it's already written)
        PokemonSet allRelatedPokemon = new PokemonSet();
        for(Pokemon pokemon : this) {
            if(!allRelatedPokemon.contains(pokemon)) {
                allRelatedPokemon.addFamily(pokemon, useOriginal);
            }
        }
        return allRelatedPokemon;
    }

    /**
     * Adds to this set the evolutionary families of every Pokemon in this set.
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
     * @param source The set to add Pokemon and families from.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return True if any Pokemon were added, false otherwise.
     */
    public boolean addAllFamilies(PokemonSet source, boolean useOriginal) {
        boolean anyChanged = false;
        for(Pokemon poke : source) {
            boolean changed = this.addFamily(poke, useOriginal);
            if(changed) {
                anyChanged = true;
            }
        }

        return anyChanged;
    }

    /**
     * Removes from this set all Pokemon that are neither contained within the given set nor
     * an evolutionary relative of a Pokemon that is.
     * @param source The set to keep Pokemon and families from.
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
     * @param pokemon The Pokemon to get the family of.
     * @param useOriginal Whether to use the evolution data from before randomization.
     * @return a PokemonSet containing every Pokemon related to the given Pokemon by members of this set.
     */
    public PokemonSet filterContiguousFamily(Pokemon pokemon, boolean useOriginal) {
        PokemonSet family = new PokemonSet();
        if(!this.contains(pokemon)) {
            return family;
        }

        Queue<Pokemon> toCheck = new ArrayDeque<>();
        toCheck.add(pokemon);
        while(!toCheck.isEmpty()) {
            Pokemon checking = toCheck.remove();
            if(family.contains(checking)) {
                continue;
            }
            family.add(checking);

            toCheck.addAll(this.filterEvolutions(checking, useOriginal));
            toCheck.addAll(this.filterPreEvolutions(checking, useOriginal));
        }

        return family;
    }

    //End Family Zone

    //Evolutions Zone

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

    //End Evolutions Zone

    //Randomization Zone

    /**
     * Chooses a random Pokemon from the set.
     * A slow function - as much as possible, do any checks and eliminations BEFORE calling this.
     * @param random A seeded random number generator.
     * @return A random Pokemon from the set.
     * @throws IllegalStateException if the set is empty.
     */
    public Pokemon getRandomPokemon(Random random) {
        if(this.isEmpty()) {
            throw new IllegalStateException("Tried to choose a random member of an empty set!");
        }

        int choice = random.nextInt(this.size());
        int i = 0;
        for(Pokemon pokemon : this) {
            if(i == choice) {
                return pokemon;
            }
            i++;
        }

        //this should never be reached
        throw new RuntimeException("I don't know how, but PokemonSet.randomPokemon iterated through the whole set without finding its choice.");
    }

    /**
     * Chooses a random Pokemon from the set.
     * Faster than randomPokemon only if choosing 3-5+ Pokemon without
     * changing the set (adding or removing Pokemon), except with this method.
     * @param random A seeded random number generator.
     * @param removeChoice Whether to remove the chosen Pokemon from the set.
     * @return A random Pokemon from the set.
     * @throws IllegalStateException if the set is empty.
     */
    public Pokemon randomPokemonCached(Random random, boolean removeChoice) {
        if(this.isEmpty()) {
            throw new IllegalStateException("Tried to choose a random member of an empty set!");
        }

        if(randomCache == null) {
            randomCache = new ArrayList(this);
        }

        for (int i = 0; i < 5; i++) {
            int choice = random.nextInt(randomCache.size());
            Pokemon p = randomCache.get(choice);
            if(p != null) {
                continue;
            }
            if(removeChoice) {
                removeKeepingCache(choice);
            }
            return p;
        }

        //If we reach this point, we've hit null five times. That probably means that most of the set is null.
        //(Or we had bad luck, but hey; either way, this will fix it.)
        randomCache = new ArrayList<>(this);
        int choice = random.nextInt(randomCache.size());
        Pokemon p = randomCache.get(choice);
        if(p == null) {
            throw new RuntimeException("This shouldn't be possible, but there was a null value in a fresh cache?");
        }
        if(removeChoice) {
            removeKeepingCache(choice);
        }
        return p;
    }

    /**
     * Removes the Pokemon at position index in the internal cache from both the cache and the set.
     * @param index The index of the Pokemon to remove.
     * @throws IllegalStateException if there is no cache.
     * @throws IndexOutOfBoundsException if the index is not within the cache's bounds.
     * @throws IllegalArgumentException if cache[index] is null.
     */
    private void removeKeepingCache(int index) {
        if(randomCache == null) {
            throw new IllegalStateException("PokemonSet's internal remove called when cache not present!");
        }

        if(index < 0 || index > randomCache.size()) {
            throw new IndexOutOfBoundsException("PokemonSet's internal remove called with bad index: " + index);
        }

        Pokemon p = randomCache.get(index);
        if(p == null) {
            throw new IllegalArgumentException("PokemonSet's internal remove called on already removed index: " + index);
        }

        if(!this.contains(p)) {
            throw new RuntimeException("PokemonSet's internal remove called on Pokemon in the cache but not the set!");
        }

        randomCache.set(index, null);
        //The whole point of the cache is to prevent iterating over large portions of the set,
        //so we don't want to remove() from it.
        this.remove(p);
        typeMap.get(p.getPrimaryType()).remove(p);
        if(p.getSecondaryType() != null) {
            typeMap.get(p.getSecondaryType()).remove(p);
        }
    }

    //End Randomization Zone

    //Various Functions

    /**
     * Returns all Pokemon in the set which are cosmetic formes.
     * @return A PokemonSet containing all Pokemon in this set which are cosmetic.
     */
    public PokemonSet filterCosmetic() {
        return filter(Pokemon::isActuallyCosmetic);
    }

    /**
     * Returns all Pokemon in the set which are not cosmetic formes.
     * @return A PokemonSet containing all Pokemon in this set which are not cosmetic.
     */
    public PokemonSet filterNonCosmetic() {
        return filter(p -> !p.isActuallyCosmetic());
    }

    //End Various Functions

    //Subclass


    /**
     * Returns an unmodifiable PokemonSet with all the elements in the source Collection.
     */
    public static PokemonSet unmodifiable(Collection<Pokemon> source) {
        return new UnmodifiablePokemonSet(source);
    }

    /**
     * Just what it sounds like, a {@link PokemonSet} which throws {@link UnsupportedOperationException}
     * whenever modifications are attempted.
     */
    private static class UnmodifiablePokemonSet extends PokemonSet {
        private final boolean unmodifiable;

        public UnmodifiablePokemonSet(Collection<Pokemon> original) {
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
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeIf(Predicate filter) {
            throw new UnsupportedOperationException();
        }
    }
}
