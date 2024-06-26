package com.dabomstew.pkrandom.pokemon;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An unordered set of Pokemon species. Guarantees uniqueness (no species can be in the set twice).
 * Automatically sorts by type and has other helper functions.
 */
public class GenericPokemonSet<T extends Pokemon> extends HashSet<T> {
    private ArrayList<T> randomCache = null;
    private static final double CACHE_RESET_FACTOR = 0.5;
    //How much of the cache must consist of removed Pokemon before resetting

    /**
     * Creates an empty GenericPokemonSet.
     */
    public GenericPokemonSet() {
        super();
    }

    /**
     * Creates a new GenericPokemonSet containing every Pokemon in the given Collection.
     * @param cloneFrom the Collection to copy from.
     */
    public GenericPokemonSet(Collection<? extends T> cloneFrom) {
        super(cloneFrom);
    }

    //Basic functions

    @Override
    public boolean add(T pokemon) {
        if(this.contains(pokemon) || pokemon == null) {
            return false;
        }
        randomCache = null;
        super.add(pokemon);

        return true;
    }

    //I'm not certain that I need to override the "xAll" functions—but I'm not
    //certain that I don't, either.

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean changed = false;
        for (T pokemon : c) {
            boolean added = this.add(pokemon);
            if(added) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        super.clear();
        randomCache = null;
    }

    /**
     * Returns the subset of this set for which the predicate function returns true.
     * @param predicate The function to test Pokemon against.
     * @return A PokemonSet containing every Pokemon in this set for which predicate returns true.
     */
    public GenericPokemonSet<T> filter(Predicate<? super T> predicate) {
        GenericPokemonSet<T> filtered = new GenericPokemonSet<>();
        for (T pk : this) {
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
    public GenericPokemonSet<T> buildSetSingle(Function<? super T, T> function) {
        GenericPokemonSet<T> newSet = new GenericPokemonSet<>();

        for(T pokemon : this) {
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
    public GenericPokemonSet<T> buildSet(Function<? super T, Collection<T>> function) {
        GenericPokemonSet<T> newSet = new GenericPokemonSet<>();

        for(T pokemon : this) {
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
    public GenericPokemonSet<T> filterBuiltSetSingle(Function<? super T, T> function) {
        GenericPokemonSet<T> builtSet = this.buildSetSingle(function);
        builtSet.retainAll(this);
        return builtSet;
    }

    /**
     * Builds a new set by running the given function on each Pokemon in this set,
     * and adding its return value to the new set only if that Pokemon is contained in this set.
     * @param function The function to run on each Pokemon in this set.
     * @return The PokemonSet containing all Pokemon in this set returned after running the given function.
     */
    public GenericPokemonSet<T> filterBuiltSet(Function<? super T, Collection<T>> function) {
        GenericPokemonSet<T> builtSet = this.buildSet(function);
        builtSet.retainAll(this);
        return builtSet;
    }

    /**
     * Adds to this set all Pokemon that were returned by running the given function
     * on at least one Pokemon in the set. <br>
     * @param function The function to run on all Pokemon in the set.
     * @return Whether any Pokemon were added to the set.
     */
    public boolean addBuiltSetSingle(Function<? super T, T> function) {
        GenericPokemonSet<T> builtSet = this.buildSetSingle(function);
        return this.addAll(builtSet);
    }

    /**
     * Adds to this set all Pokemon that were returned by running the given function
     * on at least one Pokemon in the set. <br>
     * @param function The function to run on all Pokemon in the set.
     * @return Whether any Pokemon were added to the set.
     */
    public boolean addBuiltSet(Function<? super T, Collection<T>> function) {
        GenericPokemonSet<T> builtSet = this.buildSet(function);
        return this.addAll(builtSet);
    }

    /**
     * Removes from this set all Pokemon that were returned by running the given function
     * on at least one Pokemon in the set. <br>
     * @param function The function to run on all Pokemon in the set.
     * @return Whether any Pokemon were removed from the set.
     */
    public boolean removeBuiltSetSingle(Function<? super T, T> function) {
        GenericPokemonSet<T> builtSet = this.buildSetSingle(function);
        return this.removeAll(builtSet);
    }

    /**
     * Removes from this set all Pokemon that were returned by running the given function
     * on at least one Pokemon in the set. <br>
     * @param function The function to run on all Pokemon in the set.
     * @return Whether any Pokemon were removed from the set.
     */
    public boolean removeBuiltSet(Function<? super T, Collection<T>> function) {
        GenericPokemonSet<T> builtSet = this.buildSet(function);
        return this.removeAll(builtSet);
    }

    //End basic functions

    //Type Zone
    /**
     * Returns every Pokemon in this set which has the given type.
     * @param type The type to match.
     * @param useOriginal Whether to use type data from before randomization.
     * @return a new PokemonSet containing every Pokemon of the given type.
     */
    public GenericPokemonSet<T> filterByType(Type type, boolean useOriginal) {
        return this.filter(p -> p.hasType(type, useOriginal));
    }

    /**
     * Sorts all Pokemon in this set by type.
     * Significantly faster than calling filterByType for each type.
     * @param useOriginal Whether to use type data from before randomization.
     * @return A Map of Pokemon sorted by type. <br>
     * WARNING: types with no Pokemon will contain null rather than an empty set!
     */
    public Map<Type, GenericPokemonSet<T>> sortByType(boolean useOriginal) {
        Map<Type, GenericPokemonSet<T>> typeMap = new EnumMap<>(Type.class);

        for(T poke : this) {
            addToTypeMap(typeMap, poke.getPrimaryType(useOriginal), poke);
            if(poke.hasSecondaryType(useOriginal)) {
                addToTypeMap(typeMap, poke.getSecondaryType(useOriginal), poke);
            }
        }

        return typeMap;
    }

    /**
     * Adds the given pokemon to the given map, creating a new GenericPokemonSet if needed.
     * @param type The type to add the Pokemon to.
     * @param pokemon The Pokemon to add.
     */
    private void addToTypeMap(Map<Type, GenericPokemonSet<T>> map, Type type, T pokemon) {
        GenericPokemonSet<T> typeList = map.get(type);

        if(typeList == null) {
            typeList = new GenericPokemonSet<>();
            map.put(type, typeList);
        }

        typeList.add(pokemon);
    }

    /**
     * Finds if all Pokemon in this set share a type.
     * If two types are shared, will return the primary type of an arbitrary Pokemon in the set,
     * unless that type is Normal; in this case will return the secondary type.
     * @param useOriginal Whether to use type data from before randomization.
     * @return The Type shared by all the pokemon, or null if none was shared.
     */
    public Type getSharedType(boolean useOriginal) {
        if(this.isEmpty()) {
            return null;
        }
        Iterator<T> itor = this.iterator();
        T poke = itor.next();
        Type primary = poke.getPrimaryType(useOriginal);
        Type secondary = poke.getSecondaryType(useOriginal);

        while(itor.hasNext()) {
            poke = itor.next();
            if(secondary != null) {
                if (!poke.hasType(secondary, useOriginal)) {
                    secondary = null;
                }
            }
            if (!poke.hasType(primary, useOriginal)) {
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

    //Various Functions

    /**
     * Chooses a random Pokemon from the set.
     * @param random A seeded random number generator.
     * @param removeChoice Whether to remove the chosen Pokemon from the set.
     * @return A random Pokemon from the set.
     * @throws IllegalStateException if the set is empty.
     */
    public T getRandomPokemon(Random random, boolean removeChoice) {
        if(this.isEmpty()) {
            throw new IllegalStateException("Tried to choose a random member of an empty set!");
        }

        //make sure cache state is good
        if(randomCache == null) {
            randomCache = new ArrayList<>(this);
        }
        if((double) this.size() / (double) randomCache.size() > CACHE_RESET_FACTOR)
        {
            randomCache = new ArrayList<>(this);
        }

        //ok, we should be good to randomize
        while(true) {
            int choice = random.nextInt(randomCache.size());
            T poke = randomCache.get(choice);
            if(!this.contains(poke)) {
                continue;
            }

            if(removeChoice) {
                this.remove(poke);
            }
            return poke;
        }

    }

    /**
     * Returns all Pokemon in the set which are cosmetic formes.
     * @return A PokemonSet containing all Pokemon in this set which are cosmetic.
     */
    public GenericPokemonSet<T> filterCosmetic() {
        return filter(Pokemon::isActuallyCosmetic);
    }

    /**
     * Returns all Pokemon in the set which are not cosmetic formes.
     * @return A PokemonSet containing all Pokemon in this set which are not cosmetic.
     */
    public GenericPokemonSet<T> filterNonCosmetic() {
        return filter(p -> !p.isActuallyCosmetic());
    }

    //End Various Functions

    //Subclasses

    /**
     * Returns an unmodifiable PokemonSet with all the elements in the source Collection.
     */
    public static GenericPokemonSet unmodifiable(Collection<Pokemon> source) {
        return new UnmodifiableGenericPokemonSet(source);
    }

    /**
     * Just what it sounds like, a {@link PokemonSet} which throws {@link UnsupportedOperationException}
     * whenever modifications are attempted.
     */
    private static class UnmodifiableGenericPokemonSet<T extends Pokemon> extends GenericPokemonSet<T> {
        private final boolean unmodifiable;

        public UnmodifiableGenericPokemonSet(Collection<T> original) {
            super(original);
            unmodifiable = true; // since you can't use the super constructor if add() is always impossible
        }

        @Override
        public boolean add(T pk) {
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
