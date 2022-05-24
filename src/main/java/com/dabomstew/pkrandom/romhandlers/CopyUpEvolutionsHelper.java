package com.dabomstew.pkrandom.romhandlers;

/*----------------------------------------------------------------------------*/
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

import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.dabomstew.pkrandom.RomFunctions;
import com.dabomstew.pkrandom.pokemon.Evolution;
import com.dabomstew.pkrandom.pokemon.Pokemon;

/**
 * Universal implementation for things that have "copy X up evolutions" support.
 */
public class CopyUpEvolutionsHelper {

	private RomHandler romHandler;

	public CopyUpEvolutionsHelper(RomHandler romHandler) {
		this.romHandler = romHandler;
	}

	/**
	 * @param splitEvoNoCopy If true, split evos (e.g. Politoed, Silcoon) will count
	 *                       as "no-copy" Pokémon, instead of evolved Pokémon.
	 * @param bpAction       Method to run on all base or no-copy Pokémon, or when
	 *                       evolutionSanity == false.
	 * @param epAction       Method to run on all evolved Pokémon.
	 */
	public void apply(boolean evolutionSanity, boolean splitEvoNoCopy, BasePokemonAction bpAction,
			EvolvedPokemonAction epAction) {
		apply(evolutionSanity, splitEvoNoCopy, bpAction, epAction, bpAction);
	}

	/**
	 * @param splitEvoNoCopy If true, split evos (e.g. Politoed, Silcoon) will count
	 *                       as "no-copy" Pokémon, instead of evolved Pokémon.
	 * @param bpAction       Method to run on all base or no-copy Pokémon.
	 * @param epAction       Method to run on all evolved Pokémon.
	 * @param nopAction      Method to run when evolutionSanity == false.
	 */
	public void apply(boolean evolutionSanity, boolean splitEvoNoCopy, BasePokemonAction bpAction,
			EvolvedPokemonAction epAction, BasePokemonAction nopAction) {
		List<Pokemon> allPokes = romHandler.getMainPokemonList();

		if (evolutionSanity) {
			for (Pokemon pk : allPokes) {
				if (pk != null) {
					pk.temporaryFlag = false;
				}
			}

			// Get evolution data.
			Set<Pokemon> dontCopyPokes = RomFunctions.getBasicPokemon(romHandler);
			if (splitEvoNoCopy) {
				dontCopyPokes.addAll(RomFunctions.getSplitEvoPokemon(romHandler));
			}
			Set<Pokemon> middleEvos = RomFunctions.getMiddleEvolutions(romHandler);

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

}
