package com.dabomstew.pkrandom.pokemon;

import java.util.*;
import java.util.function.Consumer;
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

	/**
	 * How much of {@link #randomPickableFrom} must consist of removed elements before its recreated.<br>
	 * This value was found through experimentation but is not very precise, anything from 0.25-0.5 seemed to give
	 * about the same algorithm speeds. Feel free to do the maths to find the optimal, though in practice it should
	 * not do much of a difference.
	 */
	private static final double LOAD_FACTOR = 0.5;

	// has to be static (instead of a constructor) because EncounterArea is not
	// generic, leading to bad type conversions
	public static PokemonSet<Pokemon> inArea(EncounterArea area) {
		PokemonSet<Pokemon> pokemonSet = new PokemonSet<>();
		for (Encounter enc : area) {
			pokemonSet.add(enc.getPokemon());
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
				if (!results.contains(ev.getTo())) {
					results.add(ev.getTo());
					toCheck.add(ev.getTo());
				}
			}
			for (Evolution ev : check.getEvolutionsTo()) {
				if (!results.contains(ev.getFrom())) {
					results.add(ev.getFrom());
					toCheck.add(ev.getFrom());
				}
			}
		}
		return results;
	}

	/**
	 * Returns an unmodifiable PokemonSet with all the elements in the source Collection.
	 */
	public static PokemonSet<Pokemon> unmodifiable(Collection<Pokemon> source) {
		return new UnmodifiablePokemonSet<>(source);
	}

	/**
	 * Just what it sounds like, a {@link PokemonSet} which throws {@link UnsupportedOperationException}
	 * whenever modifications are attempted.
	 */
	private static class UnmodifiablePokemonSet<T extends Pokemon> extends PokemonSet<T> {
		private final boolean unmodifiable;

		public UnmodifiablePokemonSet(Collection<? extends T> original) {
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
		public boolean removeIf(Predicate<? super T> filter) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<T> iterator() {
			return new Iterator<T>() {
				private final Iterator<? extends T> inner = UnmodifiablePokemonSet.super.iterator();

				@Override
				public boolean hasNext() {
					return inner.hasNext();
				}

				@Override
				public T next() {
					return inner.next();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

	private ArrayList<T> randomPickableFrom = new ArrayList<>();
	private Set<Object> recentlyRemoved = new HashSet<>();

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
				return !onlyEvo.isCarryStats();
			}
			return false;
		});
	}

	// TODO: according to the RomFunctions method this replaced, this should also
	//  filter out formes
	public PokemonSet<T> filterMiddleEvolutions(boolean includeSplitEvos) {
		return filter(pk -> {
			if (pk.getEvolutionsTo().size() == 1 && pk.getEvolutionsFrom().size() > 0) {
				Evolution onlyEvo = pk.getEvolutionsTo().get(0);
				return (onlyEvo.isCarryStats() || includeSplitEvos);
			}
			return false;
		});
	}

	// TODO: according to the RomFunctions method this replaced, this should also
	//  filter out formes
	public PokemonSet<T> filterFinalEvolutions(boolean includeSplitEvos) {
		return filter(pk -> {
			if (pk.getEvolutionsTo().size() == 1 && pk.getEvolutionsFrom().size() == 0) {
				Evolution onlyEvo = pk.getEvolutionsTo().get(0);
				return onlyEvo.isCarryStats() || includeSplitEvos;
			}
			return false;
		});
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
        boolean added = super.add(pk);
        if (added && !recentlyRemoved.remove(pk)) {
            randomPickableFrom.add(pk);
        }
		return added;
	}

	@Override
	public boolean remove(Object o) {
		boolean removed = super.remove(o);
		if (removed) {
			recentlyRemoved.add(o);
		}
		return removed;
	}

	@SuppressWarnings("unchecked")
	public boolean addEvolutionaryRelatives() {
		PokemonSet<Pokemon> relatives = new PokemonSet<>();
		for (Pokemon pk : this) {
			relatives.addAll(PokemonSet.related(pk));
		}
		return addAll((Collection<? extends T>) relatives);
	}

	/**
	 * Returns a random element, or null if the PokemonSet is empty.
	 */
	public T getRandom(Random random) {
		if (size() == 0) {
			return null;
		}

		if (1.0 - ((double) recentlyRemoved.size() / (double) randomPickableFrom.size()) < LOAD_FACTOR) {
			randomPickableFrom = new ArrayList<>(this);
			recentlyRemoved.clear();
		}

		T picked;
		do {
			picked = randomPickableFrom.get(random.nextInt(randomPickableFrom.size()));
		} while (recentlyRemoved.contains(picked));
		return picked;
	}

	@Override
	public Iterator<T> iterator() {
		return new PokemonSetIterator();
	}

	/**
	 * A custom {@link Iterator} which makes sure to add an element to {@link #recentlyRemoved} when {@link #remove()}
	 * is called.
	 */
	private class PokemonSetIterator implements Iterator<T> {
		private final Iterator<T> innerIterator = PokemonSet.super.iterator();
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
			recentlyRemoved.add(current);
		}
	}

	/**
	 * Returns the type theme of this PokemonSet, using their original (pre-randomization) types.
	 * Compare with {@link #getTypeTheme()}<br>
	 * Returns null if there is no shared type/theme, or this is empty.<br>
	 * Primary types are prioritized if all Pokemon share both types, unless the primary type is Normal
	 * (E.g. Falkner's team of all Normal/Flying), in which case it returns the secondary type.
	 */
	public Type getOriginalTypeTheme() {
		if (this.isEmpty()) {
			return null;
		}

		Type theme = null;

		Iterator<T> iter = iterator();
		Pokemon pk = iter.next();
		Type primary = pk.getOriginalPrimaryType();
		Type secondary = pk.getOriginalSecondaryType();
		while (iter.hasNext()) {
			pk = iter.next();
			if(secondary != null) {
				if (secondary != pk.getOriginalPrimaryType() && secondary != pk.getOriginalSecondaryType()) {
					secondary = null;
				}
			}
			if (primary != pk.getOriginalPrimaryType() && primary != pk.getOriginalSecondaryType()) {
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
				theme = secondary;
			} else {
				theme = primary;
			}
		}
		return theme;
	}

	/**
	 * Returns the type theme of this PokemonSet, using their types current (possible post-randomization) types.
	 * Compare with {@link #getOriginalTypeTheme()}<br>
	 * Returns null if there is no shared type/theme, or this is empty.<br>
	 * Primary types are prioritized if all Pokemon share both types, unless the primary type is Normal
	 * (E.g. Falkner's team of all Normal/Flying), in which case it returns the secondary type.
	 */
	public Type getTypeTheme() {
		if (this.isEmpty()) {
			return null;
		}

		Type theme = null;

		Iterator<T> iter =this.iterator();
		Pokemon pk = iter.next();
		Type primary = pk.getPrimaryType();
		Type secondary = pk.getSecondaryType();
		while (iter.hasNext()) {
			pk = iter.next();
			if(secondary != null) {
				if (secondary != pk.getPrimaryType() && secondary != pk.getSecondaryType()) {
					secondary = null;
				}
			}
			if (primary != pk.getPrimaryType() && primary != pk.getSecondaryType()) {
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
				theme = secondary;
			} else {
				theme = primary;
			}
		}
		return theme;
	}

}
