package com.dabomstew.pkrandom.pokemon.cueh;

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

import com.dabomstew.pkrandom.pokemon.Evolution;
import com.dabomstew.pkrandom.pokemon.Gen1Pokemon;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.PokemonSet;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.function.Supplier;

/**
 * Universal implementation for things that have "copy X up evolutions" support.<br>
 * Assumes no two Pokemon evolve into the same third Pokemon. Note this might not hold true if evolutions are
 * randomized.<br>
 * Another assumption made, is that evolutions and prevolutions of a Pokemon of generic type T are also
 * of T - E.g. that the evolutions and prevolutions of a {@link Gen1Pokemon} are
 * always Gen1Pokemon.
 */
public class CopyUpEvolutionsHelper<T extends Pokemon> {

    private final BasicPokemonAction<T> nullBasicPokemonAction = pk -> {};
    private final EvolvedPokemonAction<T> nullEvolvedPokemonAction = (evFrom, evTo, toMonIsFinalEvo) -> {};

    private final Supplier<PokemonSet> pokemonSetSupplier;

    private BasicPokemonAction<T> noEvoAction;
    private BasicPokemonAction<T> basicAction;
    private EvolvedPokemonAction<T> evolvedAction;
    private EvolvedPokemonAction<T> splitAction;

    public CopyUpEvolutionsHelper(PokemonSet pokemonSet) {
    	this.pokemonSetSupplier = () -> pokemonSet;
    }

    public CopyUpEvolutionsHelper(Supplier<PokemonSet> pokemonSetSupplier) {
        this.pokemonSetSupplier = pokemonSetSupplier;
    }

    /**
     * Sets the method to run on all Pokemon, when evolutionSanity == false. (see {@link #apply(boolean, boolean)})
     */
    private void setNoEvoAction(BasicPokemonAction<T> noEvoAction) {
        this.noEvoAction = noEvoAction == null ? nullBasicPokemonAction : noEvoAction;
    }

    /**
     * Sets the method to run on basic Pokemon.
     */
    private void setBasicAction(BasicPokemonAction<T> basicAction) {
        this.basicAction = basicAction == null ? nullBasicPokemonAction : basicAction;
    }

    /**
     * Sets the method to run on evolved Pokemon.
     */
    private void setEvolvedAction(EvolvedPokemonAction<T> evolvedAction) {
        this.evolvedAction = evolvedAction == null ? nullEvolvedPokemonAction : evolvedAction;
    }

    /**
     * Sets the method to run on split evos.
     */
    private void setSplitAction(EvolvedPokemonAction<T> splitAction) {
        this.splitAction = splitAction == null ? nullEvolvedPokemonAction : splitAction;
    }

    /**
     * Applies the CopyUpEvolutionsHelper, using the {@link PokemonSet} given by the constructor,
     * boolean options, and a number of circumstantial "actions".
     * Any action argument can be set to null, to have it do nothing.
     *
     * @param evolutionSanity If false, the noEvoAction will be used on all Pokemon.
     * @param copySplitEvos   If false, split evos are treated as basic Pokemon, and
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
    public void apply(boolean evolutionSanity, boolean copySplitEvos, BasicPokemonAction<T> bpAction,
                      EvolvedPokemonAction<T> epAction, EvolvedPokemonAction<T> splitAction, BasicPokemonAction<T> noEvoAction) {
        setBasicAction(bpAction);
        setEvolvedAction(epAction);
        setSplitAction(splitAction);
        setNoEvoAction(noEvoAction);

        apply(evolutionSanity, copySplitEvos);
    }

    /**
     * A simplified version of {@link #apply(boolean, boolean, BasicPokemonAction, EvolvedPokemonAction, EvolvedPokemonAction, BasicPokemonAction)},
     * which supposes split evos are treated the same as other evolved Pokemon,
     * and that the bpAction is used when evolutionSanity == false.
     *
     * @param evolutionSanity If false, the bpAction will be used on all Pokemon.
     * @param copySplitEvos   If false, split evos (e.g. Poliwrath and Politoed) are
     *                        treated as basic Pokemon, and will thus use bpAction
     *                        instead of epAction.
     * @param bpAction        Method to run on all basic Pokemon.
     * @param epAction        Method to run on all evolved Pokemon.
     */
    public void apply(boolean evolutionSanity, boolean copySplitEvos, BasicPokemonAction<T> bpAction,
                      EvolvedPokemonAction<T> epAction) {
        setBasicAction(bpAction);
        setEvolvedAction(epAction);
        setSplitAction(epAction);
        setNoEvoAction(bpAction);

        apply(evolutionSanity, copySplitEvos);
    }

    /**
     * @param evolutionSanity If false, the noEvoAction will be used on all Pokemon.
     * @param copySplitEvos If false, split evos are treated as basic Pokemon, and will thus use basicAction instead of splitAction.
     */
    @SuppressWarnings("unchecked")
    private void apply(boolean evolutionSanity, boolean copySplitEvos) {

        PokemonSet allPokes = pokemonSetSupplier.get();

        if (!evolutionSanity) {
            allPokes.forEach(pk -> noEvoAction.applyTo((T) pk));
            return;
        }

        PokemonSet basicPokes = allPokes.filterFirstEvolutionAvailable(false, false);
        PokemonSet splitEvos = allPokes.filterSplitEvolutions(false);
        PokemonSet finalEvos = allPokes.filterFinalEvos(false);

        Set<Pokemon> processed = new HashSet<>();

        if (!copySplitEvos) {
            basicPokes.addAll(splitEvos);
        }

        for (Pokemon pk : basicPokes) {
            basicAction.applyTo((T) pk);
            processed.add(pk);
        }

        // go "up" evolutions looking for pre-evos to do first
        for (Pokemon pk : allPokes) {
            if (!processed.contains(pk)) {

                // Non-processed pokes at this point must have
                // a linear chain of single evolutions down to
                // a processed poke.
                Stack<Evolution> evStack = new Stack<>();
                Evolution ev = pk.getEvolutionsTo().get(0);
                while (!processed.contains(ev.getFrom())) {
                    evStack.push(ev);
                    ev = ev.getFrom().getEvolutionsTo().get(0);
                }
                evStack.push(ev);

                // Now "ev" is set to an evolution from a Pokemon that has had
                // the base action done on it to one that hasn't.
                // Do the evolution action for everything left on the stack.
                while (!evStack.isEmpty()) {
                    ev = evStack.pop();
                    if (copySplitEvos && splitEvos.contains(ev.getTo())) {
                        splitAction.applyTo((T) ev.getFrom(), (T) ev.getTo(), finalEvos.contains(ev.getTo()));
                    } else {
                        evolvedAction.applyTo((T) ev.getFrom(), (T) ev.getTo(), finalEvos.contains(ev.getTo()));
                    }
                    processed.add(ev.getTo());
                }

            }
        }
    }

}
