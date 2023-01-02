package com.dabomstew.pkrandom.pokemon;

import java.util.*;
import java.util.function.Predicate;

/**
 * A {@link Set} of {@link Pokemon}, with common filtering methods. Does not
 * contain a null element. Meant to be used instead of
 * {@link List}{@literal <Pokemon>} in cases where order does not matter, but
 * avoiding element duplication does. <br>
 * <br>
 * You can create a clone of any PokemonSet by using it as an constructor
 * parameter. The "filter" methods also return new PokemonSet objects. <br>
 * <br>
 * For more complex operations, use .stream() and finally collect it into a new
 * PokemonSet using .collect(Collectors.toCollection(PokemonSet::new)).
 */
public class PokemonSet<T extends Pokemon> extends HashSet<T> {

	// has to be static (instead of a constructor) because EncounterSet is not
	// generic, leading to bad type conversions
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
			for (Evolution ev : check.getEvolutionsFrom()) {
				if (!results.contains(ev.to)) {
					results.add(ev.to);
					toCheck.add(ev.to);
				}
			}
			for (Evolution ev : check.getEvolutionsTo()) {
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

	/**
	 * Create a PokemonSet from an existing collection with the same generic type or
	 * a subclass of it.
	 */
	public PokemonSet(Collection<? extends T> pokemonList) {
		addAll(pokemonList);
	}

	/**
	 * Create a PokemonSet from an existing collection with the same generic type or
	 * a subclass of it.
	 */
	public PokemonSet(PokemonSet<? extends T> pokemonSet) {
		addAll(pokemonSet);
	}

	/**
	 * Create a PokemonSet from an existing collection with any generic type
	 * (extending Pokemon). Only works if the objects in pokemonSet can be safely
	 * cast to the generic type of the new PokemonSet. E.g. if you have a
	 * {@literal PokemonSet<Pokemon>} you know contains only Gen1Pokemon, you can
	 * use this to "cast" it to {@literal PokemonSet<Gen1Pokemon>}<br>
	 * Throws a ClassCastException if this can't be done.<br>
	 * <br>
	 * Use this method sparsely.
	 */
	// This might indeed be against the point of having generics...
	@SuppressWarnings("unchecked")
	public PokemonSet(PokemonSet<?> pokemonSet, T example) {
		for (Pokemon pk : pokemonSet) {
			if (example.getClass().isInstance(pk)) {
				add((T) pk);
			} else {
				throw new ClassCastException("Can't cast " + pk + " of class " + pk.getClass()  + " to " +
						example.getClass() + ".");
			}
		}
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
		return filter(pk -> pk.getEvolutionsTo().size() < 1);
	}

	public PokemonSet<T> filterSplitEvolutions() {
		return filter(pk -> {
			if (pk.getEvolutionsTo().size() > 0) {
				Evolution onlyEvo = pk.getEvolutionsTo().get(0);
				return !onlyEvo.carryStats;
			}
			return false;
		});
	}

	// TODO: according to the RomFunctions method this replaces, this should also
	// filter out formes
	public PokemonSet<T> filterMiddleEvolutions(boolean includeSplitEvos) {
		return filter(pk -> {
			if (pk.getEvolutionsTo().size() == 1 && pk.getEvolutionsFrom().size() > 0) {
				Evolution onlyEvo = pk.getEvolutionsTo().get(0);
				return (onlyEvo.carryStats || includeSplitEvos);
			}
			return false;
		});
	}

	// TODO: according to the RomFunctions method this replaces, this should also
	// filter out formes
	public PokemonSet<T> filterFinalEvolutions(boolean includeSplitEvos) {
		return filter(pk -> {
			if (pk.getEvolutionsTo().size() == 1 && pk.getEvolutionsFrom().size() == 0) {
				Evolution onlyEvo = pk.getEvolutionsTo().get(0);
				return onlyEvo.carryStats || includeSplitEvos;
			}
			return false;
		});
	}

	/**
	 * Filters so that a Pokemon is only included if its number is within the
	 * range.<br>
	 * Note that alternate formes have different numbers than the base form.
	 *
	 * @param start The lower end of the range, inclusive.
	 * @param end   The upper end of the range, inclusive.
	 */
	public PokemonSet<T> filterFromNumberRange(int start, int end) {
		return filter(pk -> start <= pk.getNumber() && pk.getNumber() <= end);
	}

	/**
	 * Filters so that a Pokémon is only included if its *base* number is within
	 * the range.<br>
	 * Note this means any alternate forms of a Pokemon with a valid number are
	 * included.
	 *
	 * @param start The lower end of the range, inclusive.
	 * @param end   The upper end of the range, inclusive.
	 */
	public PokemonSet<T> filterFromBaseNumberRange(int start, int end) {
		return filter(pk -> start <= pk.getBaseNumber() && pk.getBaseNumber() <= end);
	}

	public PokemonSet<T> filterByType(Type type) {
		return filter(pk -> pk.getPrimaryType() == type || pk.getSecondaryType() == type);
	}

	public PokemonSet<T> filterCosmetic() {
		return filter(Pokemon::isActuallyCosmetic);
	}

	@Override
	public boolean add(T pk) {
		// does not add null, and the return of add()
		// should correspond to whether the set is changed
		if (pk == null) {
			return false;
		}
		return super.add(pk);
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
		return randomPickableFrom.size() == 0 ? null
				: randomPickableFrom.get(random.nextInt(randomPickableFrom.size()));
	}

}