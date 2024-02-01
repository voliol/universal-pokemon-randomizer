package pokemon;

import com.dabomstew.pkrandom.pokemon.*;

import java.util.*;

/**
 * A version of {@link PokemonSet} which is stripped down in terms of filter etc. methods,
 * but has (hopefully) better handling of getRandom(). This is to test those new getRandom() methods.
 */
public class NewPokemonSet<T extends Pokemon> extends HashSet<T> {

	private final double loadFactor;

	private ArrayList<T> randomPickableFrom = new ArrayList<>();
	private Set<Object> recentlyRemoved = new HashSet<>();

	public NewPokemonSet(double loadFactor) {
		this.loadFactor = loadFactor;
	}

	@Override
	public boolean add(T pk) {
		// does not add null, and the return of add()
		// should correspond to whether the set is changed
		if (pk == null) {
			return false;
		}
		randomPickableFrom.add(pk);
		recentlyRemoved.remove(pk);
		return super.add(pk);
	}

	@Override
	public boolean remove(Object o) {
		boolean removed = super.remove(o);
		if (removed) {
			recentlyRemoved.add(o);
		}
		return removed;
	}

	public T getRandom(Random random) {
		if (size() == 0) {
			return null;
		}

		if (1.0 - ((double) recentlyRemoved.size() / (double) randomPickableFrom.size()) < loadFactor) {
			randomPickableFrom = new ArrayList<>(this);
			recentlyRemoved = new HashSet<>();
		}

		T picked;
		do {
			picked = randomPickableFrom.get(random.nextInt(randomPickableFrom.size()));
		} while (recentlyRemoved.contains(picked));
		return picked;
	}

}
