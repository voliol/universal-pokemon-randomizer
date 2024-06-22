package com.dabomstew.pkrandom.pokemon;

/*----------------------------------------------------------------------------*/
/*--  Part of "Universal Pokemon Randomizer ZX" by the UPR-ZX team          --*/
/*--  Originally part of "Universal Pokemon Randomizer" by Dabomstew        --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2020.                                 --*/
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

import java.util.Stack;
import java.util.function.Supplier;

/**
 * Universal implementation for things that have "copy X up evolutions" support.
 */
public class CopyUpEvolutionsHelper<T extends Pokemon> {

    @FunctionalInterface
    public interface BasicPokemonAction<T extends Pokemon> {
        void applyTo(T pk);
    }

    @FunctionalInterface
    public interface EvolvedPokemonAction<T extends Pokemon> {
        void applyTo(T evFrom, T evTo, boolean toMonIsFinalEvo);
    }

    // Not used
    @FunctionalInterface
    public interface CosmeticFormeAction<T extends Pokemon> {
        void applyTo(T pk, T baseForme);
    }

    private final Supplier<PokemonSet<T>> pokemonSetSupplier;
    
    public CopyUpEvolutionsHelper(PokemonSet<T> pokemonSet) {
    	this.pokemonSetSupplier = () -> pokemonSet;
    }

    public CopyUpEvolutionsHelper(Supplier<PokemonSet<T>> pokemonSetSupplier) {
        this.pokemonSetSupplier = pokemonSetSupplier;
    }

    /**
     * Supposes evolutions and prevolutions of a Pokémon of generic type T are also
     * of T - E.g. that the evolutions and prevolutions of a {@link Gen1Pokemon} are
     * always Gen1Pokemon.
     *
     * @param evolutionSanity If false, the noEvoAction will be used on all Pokemon.
     * @param copySplitEvos   If false, split evos are treated as base Pokemon, and
     *                        will thus use bpAction instead of splitAction.
     * @param bpAction        Method to run on all basic Pokemon.
     * @param epAction        Method to run on all evolved Pokemon that are not
     *                        "split evos" (e.g. Venusaur, Metapod).
     * @param splitAction     Method to run on all evolved Pokemon that are "split
     *                        evos" (e.g. Poliwrath and Politoed, Silcoon and
     *                        Cascoon).
     * @param noEvoAction     Method to run on all Pokemon, when evolutionSanity ==
     *                        false.
     */
    @SuppressWarnings("unchecked")
    public void apply(boolean evolutionSanity, boolean copySplitEvos, BasicPokemonAction<T> bpAction,
                      EvolvedPokemonAction<T> epAction, EvolvedPokemonAction<T> splitAction, BasicPokemonAction<T> noEvoAction) {

        PokemonSet<T> allPokes = pokemonSetSupplier.get();

        if (!evolutionSanity) {
            allPokes.forEach(noEvoAction::applyTo);
            return;
        }

        for (Pokemon pk : allPokes) {
            pk.temporaryFlag = false;
        }

        // Get evolution data.
        PokemonSet<T> basicPokes = allPokes.filterBasic();
        PokemonSet<T> splitEvos = allPokes.filterSplitEvolutions();
        PokemonSet<T> middleEvos = allPokes.filterMiddleEvolutions(copySplitEvos);

        for (T pk : basicPokes) {
            bpAction.applyTo(pk);
            pk.temporaryFlag = true;
        }

        if (!copySplitEvos) {
            for (T pk : splitEvos) {
                bpAction.applyTo(pk);
                pk.temporaryFlag = true;
            }
        }

        // go "up" evolutions looking for pre-evos to do first
        for (T pk : allPokes) {
            if (!pk.temporaryFlag) {

                // Non-randomized pokes at this point must have
                // a linear chain of single evolutions down to
                // a randomized poke.
                Stack<Evolution> currentStack = new Stack<>();
                Evolution ev = pk.getEvolutionsTo().get(0);
                while (!ev.getFrom().temporaryFlag) {
                    currentStack.push(ev);
                    ev = ev.getFrom().getEvolutionsTo().get(0);
                }

                // Now "ev" is set to an evolution from a Pokemon that has had
                // the base action done on it to one that hasn't.
                // Do the evolution action for everything left on the stack.

                if (copySplitEvos && splitAction != null && splitEvos.contains((T) ev.getTo())) {
                    splitAction.applyTo((T) ev.getFrom(), (T) ev.getTo(), !middleEvos.contains((T) ev.getTo()));
                } else {
                    epAction.applyTo((T) ev.getFrom(), (T) ev.getTo(), !middleEvos.contains((T) ev.getTo()));
                }
                ev.getTo().temporaryFlag = true;
                while (!currentStack.isEmpty()) {
                    ev = currentStack.pop();
                    if (copySplitEvos && splitAction != null && splitEvos.contains(pk)) {
                        splitAction.applyTo((T) ev.getFrom(), (T) ev.getTo(), !middleEvos.contains((T) ev.getTo()));
                    } else {
                        epAction.applyTo((T) ev.getFrom(), (T) ev.getTo(), !middleEvos.contains((T) ev.getTo()));
                    }
                    ev.getTo().temporaryFlag = true;
                }

            }
        }
    }

    /**
     * A simplified version of apply, which supposes split evos are treated the same
     * as other evolved Pokemon, and that the bpAction is used when evolutionSanity
     * == false.
     *
     * @param evolutionSanity If false, the bpAction will be used on all Pokemon.
     * @param copySplitEvos   If false, split evos (e.g. Poliwrath and Politoed) are
     *                        treated as base Pokemon, and will thus use bpAction
     *                        instead of epAction.
     * @param bpAction        Method to run on all basic Pokemon.
     * @param epAction        Method to run on all evolved Pokemon.
     */
    public void apply(boolean evolutionSanity, boolean copySplitEvos, BasicPokemonAction<T> bpAction,
                      EvolvedPokemonAction<T> epAction) {
        apply(evolutionSanity, copySplitEvos, bpAction, epAction, epAction, bpAction);
    }

}
