package com.dabomstew.pkrandom.pokemon;

import java.util.*;
import java.util.function.Predicate;

/**
 * A {@link Set} of {@link Pokemon}, with common filtering methods. Does not contain a null element.
 * Meant to be used instead of {@link List}{@literal <Pokemon>} in cases where
 * order does not matter, but avoiding element duplication does.
 * <br><br>
 * You can create a clone of any PokemonSet by using it as an constructor parameter.
 * The "filter" methods also return new PokemonSet objects.
 * <br><br>
 */
public class PokemonSet<T extends Pokemon> extends HashSet<T> {

    // has to be static (instead of a constructor) because EncounterSet is not generic,
    // leading to bad type conversions
    public static PokemonSet<Pokemon> inArea(EncounterSet area) {
        PokemonSet<Pokemon> pokemonSet = new PokemonSet<>();
        for (Encounter enc : area.encounters) {
            pokemonSet.add(enc.pokemon);
        }
        return pokemonSet;
    }

    // likewise static because ev.to, ev.from return Pokemon
    public static PokemonSet<Pokemon> related(Pokemon original) {
        PokemonSet<Pokemon> results = new PokemonSet<>();
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

    public PokemonSet() {
    }

    public PokemonSet(List<? extends T> pokemonList) {
        addAll(pokemonList);
    }

    public PokemonSet(PokemonSet<? extends T> pokemonSet) {
        addAll(pokemonSet);
    }

    private void addAll(List<? extends T> pokemonList) {
        pokemonList.forEach(pk -> {
            if (pk != null) add(pk);
        });
    }

    public PokemonSet<T> filter(Predicate<T> predicate) {
        PokemonSet<T> filtered = new PokemonSet<>();
        for (T pk : this) {
            if (predicate.test(pk)) {
                filtered.add(pk);
            }
        }
        return filtered;
    }

    public PokemonSet<T> filterBasic() {
        return filter(pk -> pk.evolutionsTo.size() < 1);
    }

    public PokemonSet<T> filterSplitEvolutions() {
        return filter(pk -> {
            if (pk.evolutionsTo.size() > 0) {
                Evolution onlyEvo = pk.evolutionsTo.get(0);
                return !onlyEvo.carryStats;
            }
            return false;
        });
    }

    // TODO: according to the RomFunctions method this replaces, this should also filter out formes
    public PokemonSet<T> filterMiddleEvolutions(boolean includeSplitEvos) {
        return filter(pk -> {
            if (pk.evolutionsTo.size() == 1 && pk.evolutionsFrom.size() > 0) {
                Evolution onlyEvo = pk.evolutionsTo.get(0);
                return (onlyEvo.carryStats || includeSplitEvos);
            }
            return false;
        });
    }

    // TODO: according to the RomFunctions method this replaces, this should also filter out formes
    public PokemonSet<T> filterFinalEvolutions(boolean includeSplitEvos) {
        return filter(pk -> {
            if (pk.evolutionsTo.size() == 1 && pk.evolutionsFrom.size() == 0) {
                Evolution onlyEvo = pk.evolutionsTo.get(0);
                return onlyEvo.carryStats || includeSplitEvos;
            }
            return false;
        });
    }

    /**
     * Filters so that a Pokémon is only included if its number is within the range.<br>
     * Note that alternate formes have different numbers than the base form.
     *
     * @param start The lower end of the range, inclusive.
     * @param end   The upper end of the range, inclusive.
     */
    public PokemonSet<T> filterFromNumberRange(int start, int end) {
        return filter(pk -> start <= pk.number && pk.number <= end);
    }

    /**
     * Filters so that a Pokémon is only included if its *base* number is within the range.<br>
     * Note this means any alternate forms of a Pokémon with a valid number are included.
     *
     * @param start The lower end of the range, inclusive.
     * @param end   The upper end of the range, inclusive.
     */
    public PokemonSet<T> filterFromBaseNumberRange(int start, int end) {
        return filter(pk -> start <= pk.getBaseNumber() && pk.getBaseNumber() <= end);
    }

    public PokemonSet<T> filterByType(Type type) {
        return filter(pk -> pk.primaryType == type || pk.secondaryType == type);
    }

    @SuppressWarnings("unchecked")
    public boolean addEvolutionaryRelatives() {
        PokemonSet<Pokemon> relatives = new PokemonSet<>();
        for (Pokemon pk : this) {
            relatives.addAll(PokemonSet.related(pk));
        }
        return addAll((Collection<? extends T>) relatives);
    }

    public T getRandom(Random random) {
        List<T> randomPickableFrom = new ArrayList<>(this);
        return randomPickableFrom.get(random.nextInt(randomPickableFrom.size()));
    }

    // TODO: come up with a collector or the like, so streams can be used for longer operations
    //  (this.filter() is a good shorthand)
}
