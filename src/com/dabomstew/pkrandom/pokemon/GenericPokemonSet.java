package com.dabomstew.pkrandom.pokemon;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An unordered set of Pokemon species. Guarantees uniqueness (no species can be in the set twice).
 * Automatically sorts by type and has other helper functions.
 */
public class GenericPokemonSet<T extends Pokemon> extends HashSet<T> {

    private EnumMap<Type, HashSet<T>> typeMap = new EnumMap<>(Type.class);
    ArrayList<T> randomCache = null;

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
    private void addToType(T pokemon, Type type) {
        HashSet<T> typeSet = typeMap.get(type);
        if(typeSet == null) {
            typeSet = new HashSet<>();
            typeMap.put(type, typeSet);
        }
        typeSet.add(pokemon);
    }

    @Override
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
        removalSecondaryTasks(pokemon);

        return true;
    }

    /**
     * Secondary tasks that need to be done after a Pokemon is removed from the set.
     * Guaranteed to be called at least once per removal, but might be called more than once.
     * (Depending on how hashSet's iterator is implemented.)
     * @param pokemon The Pokemon that was removed.
     */
    private void removalSecondaryTasks(Pokemon pokemon){
        typeMap.get(pokemon.getPrimaryType()).remove(pokemon);
        if(pokemon.getSecondaryType() != null) {
            typeMap.get(pokemon.getSecondaryType()).remove(pokemon);
        }
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
    public boolean retainAll(Collection<?> c) {
        return this.removeIf(p -> !c.contains(p));
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            boolean removed = this.remove(o);
            if (removed) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean removeIf(Predicate<? super T> test) {
        boolean changed = false;
        Iterator<T> itor = this.iterator();
        while(itor.hasNext()) {
            T pokemon = itor.next();
            if(test.test(pokemon)) {
                itor.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        super.clear();
        typeMap = new EnumMap<>(Type.class);
        randomCache = null;
    }

    @Override
    public Iterator<T> iterator() {
        return new PokemonSetIterator();
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
     * @return a new PokemonSet containing every Pokemon of the given type.
     */
    public GenericPokemonSet<T> filterByType(Type type) {
        if(typeMap.get(type) == null) {
            return new GenericPokemonSet<>();
        } else {
            return new GenericPokemonSet<>(typeMap.get(type));
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
     * @param useOriginal Whether to use type data from before randomization.
     * @return The Type shared by all the pokemon, or null if none was shared.
     */
    public Type findSharedType(boolean useOriginal) {
        if(useOriginal) {
            return findOriginalSharedType();
        } else {
            return findNewSharedType();
        }
    }

    /**
     * Finds if all Pokemon in this set share a type post-randomization.
     * If two types are shared, will return the primary type of an arbitrary Pokemon in the set,
     * unless that type is Normal; in this case will return the secondary type.
     * @return The Type shared by all the pokemon, or null if none was shared.
     */
    private Type findNewSharedType() {
        if(this.isEmpty()) {
            return null;
        }
        Iterator<T> itor = this.iterator();
        T poke = itor.next();

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
    private Type findOriginalSharedType() {
        if(this.isEmpty()) {
            return null;
        }
        Iterator<T> itor = this.iterator();
        T poke = itor.next();
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

    //Randomization Zone

    /**
     * Chooses a random Pokemon from the set.
     * A slow function - as much as possible, do any checks and eliminations BEFORE calling this.
     * @param random A seeded random number generator.
     * @return A random Pokemon from the set.
     * @throws IllegalStateException if the set is empty.
     */
    public T getRandomPokemon(Random random) {
        if(this.isEmpty()) {
            throw new IllegalStateException("Tried to choose a random member of an empty set!");
        }

        int choice = random.nextInt(this.size());
        int i = 0;
        for(T pokemon : this) {
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
    public T randomPokemonCached(Random random, boolean removeChoice) {
        if(this.isEmpty()) {
            throw new IllegalStateException("Tried to choose a random member of an empty set!");
        }

        if(randomCache == null) {
            randomCache = new ArrayList<>(this);
        }

        for (int i = 0; i < 5; i++) {
            int choice = random.nextInt(randomCache.size());
            T p = randomCache.get(choice);
            if(p == null) {
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
        T p = randomCache.get(choice);
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

        T p = randomCache.get(index);
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
     * A custom {@link Iterator} which allows us to do our necessary secondary tasks
     * when removing a Pokemon.
     */
    //May or may not be necessary, but better to assume it is.
    private class PokemonSetIterator implements Iterator<T> {
        private final Iterator<T> innerIterator = GenericPokemonSet.super.iterator();
        private T current;

        @Override
        public boolean hasNext() {
            return innerIterator.hasNext();
        }

        @Override
        public T next() {
            current = innerIterator.next();
            return current;
        }

        @Override
        public void remove() {
            innerIterator.remove();
            removalSecondaryTasks(current);
        }
    }

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
