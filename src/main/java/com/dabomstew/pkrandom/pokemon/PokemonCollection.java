package com.dabomstew.pkrandom.pokemon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.dabomstew.pkrandom.exceptions.RandomizationException;

/*----------------------------------------------------------------------------*/
/*--  PokemonCollection.java - represents a list of pokemon grouped by type --*/
/*--                                                                        --*/
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

public class PokemonCollection {

    private ArrayList<Pokemon> pokes;
    private Map<Type, ArrayList<Pokemon>> pokesByType;
    private int typeCount;
    private int uniquePokeCount;

    public PokemonCollection() {
        pokes = new ArrayList<Pokemon>();
        pokesByType = new TreeMap<Type, ArrayList<Pokemon>>();
    }

    public PokemonCollection(List<Pokemon> pks) {
        pokes = new ArrayList<Pokemon>();
        pokesByType = new TreeMap<Type, ArrayList<Pokemon>>();

        addAll(pks);

        updateTypeCount();
    }

    public PokemonCollection(PokemonCollection ps) {
        pokes = new ArrayList<Pokemon>(ps.pokes);
        pokesByType = new TreeMap<Type, ArrayList<Pokemon>>();
        for (Map.Entry<Type, ArrayList<Pokemon>> entry : ps.pokesByType.entrySet()) {
            pokesByType.put(entry.getKey(), new ArrayList<Pokemon>(entry.getValue()));
        }
        typeCount = ps.typeCount;
    }

    public int size() {
        return pokes.size();
    }

    public int sizeByType(Type type) {
        ArrayList<Pokemon> list = pokesByType.get(type);
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    public int uniquePokes() {
        return uniquePokeCount;
    }

    public int typeCount() {
        return typeCount;
    }

    public ArrayList<Pokemon> getPokes() {
        return this.pokes;
    }

    public Map<Type, ArrayList<Pokemon>> getPokesByType() {
        return this.pokesByType;
    }

    // Filtering
    public PokemonCollection filterList(List<Pokemon> list) {
        if (!list.isEmpty()) {
            Set<Pokemon> set = new TreeSet<Pokemon>(list);
            filter(p -> set.contains(p));
        }
        return this;
    }

    public PokemonCollection filterSet(Set<Pokemon> set) {
        if (!set.isEmpty()) {
            filter(p -> set.contains(p));
        }
        return this;
    }

    public PokemonCollection filterLegendaries() {
        filter(p -> p.isLegendary());
        return this;
    }

    public PokemonCollection filterByMinimumLevel(int level) {
        filter(p -> p.minimumLevel() > level);
        return this;
    }

    /**
     * Filters by how many stages are left for a given pokemon If a pokemon is cyclic, it is removed
     * 
     * @param minStages Minimum amount of stages a pokemon must have
     * @param maxStages Maximum amount of stages a pokemon can have
     * @return This modified PokemonCollection
     */
    public PokemonCollection filterByEvolutionStagesRemaining(int minStages, int maxStages) {
        Set<Pokemon> visited = new HashSet<Pokemon>();
        Set<Pokemon> recStack = new HashSet<Pokemon>();
        filter(p -> p.isCyclic(visited, recStack));
        filter(p -> p.evolutionChainSize() < minStages || p.evolutionChainSize() > maxStages);
        return this;
    }

    /**
     * Filters by how many evolutions a given Pokemon can evolve into (aka split evolutions)
     * 
     * @param minCount Minimum amount of split evolutions a pokemon must have
     * @param maxCount Maximum amount of split evolutions a pokemon can have
     * @return This modified PokemonCollection
     */
    public PokemonCollection filterByEvolutionCount(int minCount, int maxCount) {
        filter(p -> p.evolutionsFrom.size() < minCount || p.evolutionsFrom.size() > maxCount);
        return this;
    }

    /**
     * Filters by how many evolutions a given Pokemon is into the evolution chain (aka stage of
     * evolution)
     * 
     * @param minCount Minimum stage of evolution a pokemon must be
     * @param maxCount Maximum stage of evolution a pokemon can be
     * @return This modified PokemonCollection
     */
    public PokemonCollection filterByEvolutionStage(int minStage, int maxStage) {
        filter(p -> p.evolutionsTo.size() < minStage || p.evolutionsTo.size() > maxStage);
        return this;
    }

    public PokemonCollection filterByPowerLevel(int bstLimit) {
        filter(p -> p.bstForPowerLevels() > bstLimit);
        return this;
    }

    public PokemonCollection filterByType(Type... types) {
        // Safety check - varargs array can have 0 values
        if (types.length < 1) {
            throw new RandomizationException("No types were passed into the filter");
        }
        filter(p -> Arrays.stream(types)
                .noneMatch(type -> p.primaryType == type || p.secondaryType == type));
        return this;
    }

    public PokemonCollection addAll(List<Pokemon> list) {
        list.forEach(pk -> add(pk));
        return this;
    }

    public PokemonCollection remove(Pokemon poke) {
        filter(p -> p.equals(poke));
        return this;
    }

    public PokemonCollection add(Pokemon poke) {
        if (poke != null) {
            pokes.add(poke);

            ArrayList<Pokemon> set = pokesByType.get(poke.primaryType);
            if (set == null) {
                set = new ArrayList<Pokemon>();
                pokesByType.put(poke.primaryType, set);
            }
            set.add(poke);

            if (poke.secondaryType != null) {
                set = pokesByType.get(poke.secondaryType);
                if (set == null) {
                    set = new ArrayList<Pokemon>();
                    pokesByType.put(poke.secondaryType, set);
                }
                set.add(poke);
            }
        }
        updateTypeCount();
        return this;
    }

    public PokemonCollection filter(Predicate<Pokemon> pred) {
        pokes.removeIf(pred);
        for (Map.Entry<Type, ArrayList<Pokemon>> entry : pokesByType.entrySet()) {
            entry.getValue().removeIf(pred);
        }

        pokesByType.entrySet().removeIf(e -> e.getValue().isEmpty());

        updateTypeCount();
        return this;
    }

    /**
     * Returns a random pokemon from the set
     * 
     * @param random The object containing the random seed
     * @param pop Remove the pokemon from the set and return it
     * @return Pokemon from the set
     */
    public Pokemon randomPokemon(Random random, boolean pop) {
        Pokemon chosen = getPokes().get(random.nextInt(pokes.size()));
        if (pop) {
            remove(chosen);
        }
        return chosen;
    }

    /**
     * Returns a random pokemon from the set with the type requested
     * 
     * @param type Type for the pokemon to have
     * @param random The object containing the random seed
     * @param pop Remove the pokemon from the set and return it
     * @return Pokemon from the set of the requested type
     * @throws RandomizationException If there is no Pokemon that can be selected
     */
    public Pokemon randomPokemonOfType(Type type, Random random, boolean pop) {
        ArrayList<Pokemon> list = getPokesByType().get(type);
        if (list == null || list.size() == 0) {
            throw new RandomizationException("No pokemon in set of type: " + type);
        }
        Pokemon chosen = list.get(random.nextInt(list.size()));
        if (pop) {
            remove(chosen);
        }

        return chosen;
    }

    /**
     * Returns a random pokemon with greater preference to common types
     * 
     * @param random The object containing the random seed
     * @param pop Remove the pokemon from the set and return it
     * @return Pokemon from the set
     * @throws IllegalStateException If there was no type with a significant size
     */
    public Pokemon randomPokemonTypeWeighted(Random random, boolean pop) {
        // Generate number between 0 and typeCount
        int idx = random.nextInt(typeCount);
        Pokemon chosen = null;
        for (Map.Entry<Type, ArrayList<Pokemon>> entry : pokesByType.entrySet()) {
            // If idx is in the length of this list, get the pokemon at that index
            if (idx < entry.getValue().size()) {
                chosen = entry.getValue().get(idx);
                if (pop) {
                    remove(chosen);
                }
                return chosen;
            }
            // Reduce the selected number by the amount in the list and try again
            idx -= entry.getValue().size();
        }

        throw new IllegalStateException(
                String.format("randomPokemonTypeWeighted: %d/%d", idx, typeCount));
    }

    /**
     * Returns a random pokemon from the pool with the required types and none of the restricted
     * types
     * 
     * @param mustInclude Set of types that a pokemon must have
     * @param cannotInclude Set of types that a pokemon cannot have
     * @param random The object containing the random seed
     * @param pop Remove the pokemon from the pool and return it
     * @return Pokemon from the pool
     */
    public Pokemon randomPokemonTypeRestricted(Set<Type> mustInclude, Set<Type> cannotInclude,
            Random random, boolean pop) {
        ArrayList<Pokemon> mustHave = new ArrayList<Pokemon>();
        if (mustInclude.size() == 0) {
            mustHave.addAll(getPokes());
        } else {
            for (Type type : mustInclude) {
                ArrayList<Pokemon> list = getPokesByType().get(type);
                if (list == null || list.size() == 0) {
                    throw new RandomizationException("No pokemon in set of type: " + type);
                }
                mustHave.addAll(list);
            }
        }

        List<Pokemon> filteredList =
                mustHave.stream()
                        .filter(p -> !cannotInclude.contains(p.primaryType)
                                && !cannotInclude.contains(p.secondaryType))
                        .collect(Collectors.toList());

        if (filteredList.size() == 0) {
            throw new RandomizationException("No pokemon with required types " + mustInclude
                    + " and banned types " + cannotInclude);
        }

        Pokemon chosen = filteredList.get(random.nextInt(filteredList.size()));
        if (pop) {
            remove(chosen);
        }

        return chosen;
    }

    /**
     * Returns a random pokemon with a similar BST
     * 
     * @param current The pokemon for comparison
     * @param banSamePokemon Do not select the same pokemon as a replacement
     * @param random The object containing the random seed
     * @param pop Remove the pokemon from the set and return it
     * @return Pokemon from the set with similar BST
     * @throws RandomizationException If no pokemon were found with similar BST
     */
    public Pokemon randomPokemonByPowerLevel(Pokemon current, boolean banSamePokemon, Random random,
            boolean pop) {
        // start with within 10% and add 20% either direction till we find
        // something
        int currentBST = current.bstForPowerLevels();
        int minTarget = currentBST - currentBST / 10;
        int maxTarget = currentBST + currentBST / 10;
        Set<Pokemon> canPick = new TreeSet<Pokemon>();
        // Create a pick list of similar BST. Break when three or more elements exist in the pick
        // list,
        // or exceeds 50 BST either direction
        for (int expandRounds = 0; canPick.isEmpty()
                || (canPick.size() < 3 && expandRounds < 3); expandRounds++) {
            for (Pokemon pk : pokes) {
                if (pk.bstForPowerLevels() >= minTarget && pk.bstForPowerLevels() <= maxTarget
                        && (!banSamePokemon || pk != current)) {
                    canPick.add(pk);
                }
            }
            minTarget -= currentBST / 20;
            maxTarget += currentBST / 20;
        }

        // Throw error if list is empty
        if (canPick.size() < 1) {
            throw new RandomizationException(
                    "No pokemon with similar BST found for " + current.name);
        }

        Pokemon chosen = new ArrayList<Pokemon>(canPick).get(random.nextInt(canPick.size()));
        if (pop) {
            remove(chosen);
        }
        return chosen;
    }

    /**
     * Return a type represented by the types in the set
     * 
     * @param random The object containing the random seed
     * @return Type from the list of types available in the set
     */
    public Type randomType(Random random) {
        ArrayList<Type> list = new ArrayList<Type>(pokesByType.keySet());
        return list.get(random.nextInt(list.size()));
    }

    public Type randomType(int minSize, Random random) {
        ArrayList<Type> list = new ArrayList<Type>(pokesByType.keySet());
        list.removeIf(t -> pokesByType.get(t).size() < minSize);
        return list.get(random.nextInt(list.size()));
    }

    public Type randomTypeWeighted(Random random) {
        int idx = random.nextInt(typeCount);
        for (Map.Entry<Type, ArrayList<Pokemon>> entry : pokesByType.entrySet()) {
            if (idx < entry.getValue().size()) {
                return entry.getKey();
            }
            idx -= entry.getValue().size();
        }
        throw new IllegalStateException(String.format("randomTypeWeighted: %d/%d", idx, typeCount));
    }

    // Internal
    private void updateTypeCount() {
        typeCount = 0;
        uniquePokeCount = 0;
        HashSet<Pokemon> uniquePokes = new HashSet<Pokemon>();
        for (Map.Entry<Type, ArrayList<Pokemon>> entry : pokesByType.entrySet()) {
            typeCount += entry.getValue().size();
            uniquePokes.addAll(entry.getValue());
        }
        uniquePokeCount = uniquePokes.size();
    }
}
