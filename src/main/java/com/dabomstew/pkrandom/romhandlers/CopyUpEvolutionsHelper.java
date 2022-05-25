package com.dabomstew.pkrandom.romhandlers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.dabomstew.pkrandom.pokemon.Evolution;
import com.dabomstew.pkrandom.pokemon.Pokemon;

/**
 * Universal implementation for things that have "copy X up evolutions" support.
 * 
 */
public class CopyUpEvolutionsHelper {

	private Set<Pokemon> allPokes;

	public CopyUpEvolutionsHelper(RomHandler romHandler) {
		this(romHandler.getMainPokemonList());
	}

	public CopyUpEvolutionsHelper(Collection<Pokemon> pokemon) {
		this.allPokes = new HashSet<>(pokemon);
		pokemon.removeIf(Objects::isNull);
	}

	/**
	 * @param bpAction Method to run on all base or no-copy Pokémon, or when
	 *                 evolutionSanity == false
	 * 
	 * @param epAction Method to run on all evolved Pokémon with a linear chain of
	 *                 single evolutions.
	 */
	public void apply(boolean evolutionSanity, boolean splitEvoNoCopy, BasePokemonAction bpAction,
			EvolvedPokemonAction epAction) {
		apply(evolutionSanity, splitEvoNoCopy, bpAction, epAction, bpAction);
	}

	/**
	 * @param bpAction  Method to run on all base or no-copy Pokémon
	 * 
	 * @param epAction  Method to run on all evolved Pokémon with a linear chain of
	 *                  single evolutions.
	 * 
	 * @param nopAction Method to run when evolutionSanity == false
	 */
	public void apply(boolean evolutionSanity, boolean splitEvoNoCopy, BasePokemonAction bpAction,
			EvolvedPokemonAction epAction, BasePokemonAction nopAction) {

		if (evolutionSanity) {
			for (Pokemon pk : allPokes) {
				if (pk != null) {
					pk.temporaryFlag = false;
				}
			}

			// Get evolution data.
			Set<Pokemon> dontCopyPokes = getBasicPokemon();
			if (splitEvoNoCopy) {
				dontCopyPokes.addAll(getSplitEvoPokemon());
			}
			Set<Pokemon> middleEvos = getMiddleEvolutions();

			for (Pokemon pk : dontCopyPokes) {
				pk.temporaryFlag = true;
				bpAction.applyTo(pk);
			}

			// go "up" evolutions looking for pre-evos to do first
			for (Pokemon pk : allPokes) {
				if (pk != null && !pk.temporaryFlag) {
					// Non-randomized pokes at this point must have
					// a linear chain of single evolutions down to
					// a randomized poke.
					// Lv1 Evos may cause a cyclic chain, which requires
					// skipping this requirement and applying the effect
					// on as many evolutions as possible - even if this
					// may get overwritten due to evolution lines merging
					Stack<Evolution> currentStack = new Stack<Evolution>();
					Evolution ev = pk.evolutionsTo.get(0);
					while (!ev.from.temporaryFlag && !currentStack.contains(ev)) {
						currentStack.push(ev);
						ev = ev.from.evolutionsTo.get(0);
					}

					// Now "ev" is set to an evolution from a Pokemon that has had
					// the base action done on it to one that hasn't.
					// Do the evolution action for everything left on the stack.
					epAction.applyTo(ev.from, ev.to, !middleEvos.contains(ev.to));
					ev.to.temporaryFlag = true;
					while (!currentStack.isEmpty()) {
						ev = currentStack.pop();
						epAction.applyTo(ev.from, ev.to, !middleEvos.contains(ev.to));
						ev.to.temporaryFlag = true;
					}
				}
			}

		} else {
			for (Pokemon pk : allPokes) {
				if (pk != null) {
					nopAction.applyTo(pk);
				}
			}
		}
	}

	private Set<Pokemon> getFilteredPokemon(Predicate<? super Pokemon> predicate) {
		return allPokes.stream().filter(predicate).collect(Collectors.toSet());
	}

	// These functions replace those in RomFunctions,
	// so they can take a set instead of a RomHandler.
	// TODO: They don't really belong here (bad coherence), but should be in some
	// unified Pokemon set structure. PokemonCollection might be that but
	// investigating further is out of scope for now.

	private Set<Pokemon> getBasicPokemon() {
		return getFilteredPokemon(pk -> pk.evolutionsTo.size() < 1);
	}

	private Set<Pokemon> getSplitEvoPokemon() {
		return getFilteredPokemon(pk -> pk.evolutionsTo.size() > 1 && !pk.evolutionsTo.get(0).carryStats);
	}

	private Set<Pokemon> getMiddleEvolutions() {
		return getFilteredPokemon(pk -> {
			if (pk.evolutionsTo.size() == 1 && pk.evolutionsFrom.size() > 0) {
				Evolution onlyEvo = pk.evolutionsTo.get(0);
				if (onlyEvo.carryStats) {
					return true;
				}
			}
			return false;
		});
	}

}
