package com.dabomstew.pkrandom.pokemon;

import java.util.*;

/**
 * An unordered set of Pokemon species. Guarantees uniqueness (no species can be in the set twice).
 * Automatically sorts by type and has other helper functions.
 */
public class PokemonSet implements Set<Pokemon> {

    private HashSet<Pokemon> internalSet;
    private EnumMap<Type, HashSet<Pokemon>> typeMap;

    /**
     * Creates an empty PokemonSet.
     */
    public PokemonSet() {
        internalSet = new HashSet<>();
        typeMap = new EnumMap<>(Type.class);
    }

    /**
     * Creates a new PokemonSet containing every Pokemon in the given Collection.
     * @param cloneFrom the Collection to copy from.
     */
    public PokemonSet(Collection<Pokemon> cloneFrom) {
        this();
        addAll(cloneFrom);
    }

    /**
     * Returns every Pokemon in this set which has the given type.
     * @param type The type to match.
     * @return a PokemonSet containing every Pokemon of the given type.
     */
    public PokemonSet getPokemonOfType(Type type) {
        if(typeMap.get(type) == null) {
            return new PokemonSet();
        } else {
            return new PokemonSet(typeMap.get(type));
        }
    }

    /**
     * Adds the given Pokemon and every evolutionary relative to this set, if they are
     * not already contained in the set.
     * @param pokemon The Pokemon to add the family of.
     * @return True if any Pokemon were added to the set, false otherwise.
     */
    public boolean addFamily(Pokemon pokemon) {
        boolean changed = false;
        PokemonSet checked = new PokemonSet();

        Queue<Pokemon> toCheck = new ArrayDeque<>();
        toCheck.add(pokemon);
        while(!toCheck.isEmpty()) {
            Pokemon checking = toCheck.remove();
            if(checked.contains(checking)) {
                continue;
            }
            checked.add(checking);
            boolean added = this.add(checking);
            if(added) {
                changed = true;
            }
            toCheck.addAll(checking.getAllEvolvedPokemon());
            toCheck.addAll(checking.getAllPreEvolvedPokemon());
        }

        return changed;
    }

    /**
     * Adds the given Pokemon and every Pokemon that was related to it before randomization
     * to this set, if they are not already in the set.
     * @param pokemon The Pokemon to add the family of.
     * @return True if any Pokemon were added to the set, false otherwise.
     */
    public boolean addOriginalFamily(Pokemon pokemon) {
        boolean changed = false;
        PokemonSet checked = new PokemonSet();

        Queue<Pokemon> toCheck = new ArrayDeque<>();
        toCheck.add(pokemon);
        while(!toCheck.isEmpty()) {
            Pokemon checking = toCheck.remove();
            if(checked.contains(checking)) {
                continue;
            }
            checked.add(checking);
            boolean added = this.add(checking);
            if(added) {
                changed = true;
            }
            toCheck.addAll(checking.originalEvolvedForms);
            toCheck.addAll(checking.originalPreEvolvedForms);
        }

        return changed;
    }

    /**
     * Removes the given Pokemon and every evolutionary relative from this set, if they
     * are contained in the set.
     * @param pokemon The Pokemon to remove the family of.
     * @return If any Pokemon were removed from the set.
     */
    public boolean removeFamily(Pokemon pokemon) {
        //note: cannot use getEvolutions and getPreEvolutions as that would only remove *contiguous* family.
        boolean changed = false;
        PokemonSet checked = new PokemonSet();

        Queue<Pokemon> toCheck = new ArrayDeque<>();
        toCheck.add(pokemon);
        while(!toCheck.isEmpty()) {
            Pokemon checking = toCheck.remove();
            if(checked.contains(checking)) {
                continue;
            }
            checked.add(checking);
            boolean removed = this.remove(checking);
            if(removed) {
                changed = true;
            }
            toCheck.addAll(checking.getAllEvolvedPokemon());
            toCheck.addAll(checking.getAllPreEvolvedPokemon());
        }

        return changed;
    }

    /**
     * Removes the given Pokemon and every Pokemon that was related to it before randomization
     * from this set, if they are contained in the set.
     * @param pokemon The Pokemon to remove the family of.
     * @return If any Pokemon were removed from the set.
     */
    public boolean removeOriginalFamily(Pokemon pokemon) {
        boolean changed = false;
        PokemonSet checked = new PokemonSet();

        Queue<Pokemon> toCheck = new ArrayDeque<>();
        toCheck.add(pokemon);
        while(!toCheck.isEmpty()) {
            Pokemon checking = toCheck.remove();
            if(checked.contains(checking)) {
                continue;
            }
            checked.add(checking);
            boolean removed = this.remove(checking);
            if(removed) {
                changed = true;
            }
            toCheck.addAll(checking.originalEvolvedForms);
            toCheck.addAll(checking.originalPreEvolvedForms);
        }

        return changed;
    }

    /**
     * Returns all members of the given Pokemon's evolutionary family that this set contains.
     * Returns an empty set if no members are in this set.
     * @param pokemon The Pokemon to get the family of.
     * @return a PokemonSet containing every member of the given Pokemon's family
     */
    public PokemonSet getFamily(Pokemon pokemon) {
        //note: cannot use getEvolutions and getPreEvolutions as that would only return *contiguous* family.
        PokemonSet family = new PokemonSet();
        PokemonSet checked = new PokemonSet();

        Queue<Pokemon> toCheck = new ArrayDeque<>();
        toCheck.add(pokemon);
        while(!toCheck.isEmpty()) {
            Pokemon checking = toCheck.remove();
            if(checked.contains(checking)) {
                continue;
            }
            checked.add(checking);
            if(this.contains(checking)) {
                family.add(checking);
            }
            toCheck.addAll(checking.getAllEvolvedPokemon());
            toCheck.addAll(checking.getAllPreEvolvedPokemon());
        }

        return family;
    }

    /**
     * Returns all members of the given Pokemon's evolutionary family before randomization
     * that this set contains. Returns an empty set if no members are in this set.
     * @param pokemon The Pokemon to get the family of.
     * @return a PokemonSet containing every member of the given Pokemon's family
     */
    public PokemonSet getOriginalFamily(Pokemon pokemon) {
        //note: cannot use getEvolutions and getPreEvolutions as that would only return *contiguous* family.
        PokemonSet family = new PokemonSet();
        PokemonSet checked = new PokemonSet();

        Queue<Pokemon> toCheck = new ArrayDeque<>();
        toCheck.add(pokemon);
        while(!toCheck.isEmpty()) {
            Pokemon checking = toCheck.remove();
            if(checked.contains(checking)) {
                continue;
            }
            checked.add(checking);
            if(this.contains(checking)) {
                family.add(checking);
            }
            toCheck.addAll(checking.originalEvolvedForms);
            toCheck.addAll(checking.originalPreEvolvedForms);
        }

        return family;
    }

    /**
     * Returns all members of the given Pokemon's evolutionary family
     * that are contained uninterrupted within this set.
     * Returns an empty set if the given Pokemon is not in this set.
     * @param pokemon The Pokemon to get the family of.
     * @return a PokemonSet containing every Pokemon related to the given Pokemon by members of this set.
     */
    public PokemonSet getContiguousFamily(Pokemon pokemon) {
        PokemonSet family = new PokemonSet();
        if(!this.contains(pokemon)) {
            return family;
        }

        Queue<Pokemon> toCheck = new ArrayDeque<>();
        toCheck.add(pokemon);
        while(!toCheck.isEmpty()) {
            Pokemon checking = toCheck.remove();
            if(family.contains(checking)) {
                continue;
            }
            family.add(checking);
            toCheck.addAll(this.getEvolutions(checking));
            toCheck.addAll(this.getPreEvolutions(checking));
        }

        return family;
    }

    /**
     * Returns all members of the given Pokemon's evolutionary family before randomization
     * that are contained uninterrupted within this set.
     * Returns an empty set if the given Pokemon is not in this set.
     * @param pokemon The Pokemon to get the family of.
     * @return a PokemonSet containing every Pokemon related to the given Pokemon by members of this set.
     */
    public PokemonSet getContiguousOriginalFamily(Pokemon pokemon) {
        PokemonSet family = new PokemonSet();
        if(!this.contains(pokemon)) {
            return family;
        }

        Queue<Pokemon> toCheck = new ArrayDeque<>();
        toCheck.add(pokemon);
        while(!toCheck.isEmpty()) {
            Pokemon checking = toCheck.remove();
            if(family.contains(checking)) {
                continue;
            }
            family.add(checking);
            toCheck.addAll(this.getOriginalEvolutions(checking));
            toCheck.addAll(this.getOriginalPreEvolutions(checking));
        }

        return family;
    }

    /**
     * Returns all Pokemon in this set that the given Pokemon can evolve directly into.
     * @param pokemon The Pokemon to get evolutions of.
     * @return A PokemonSet containing all direct evolutions of the given Pokemon in this set.
     */
    public PokemonSet getEvolutions(Pokemon pokemon) {
        PokemonSet evolvedPokemon = pokemon.getAllEvolvedPokemon();
        evolvedPokemon.retainAll(this);
        return evolvedPokemon;
    }

    /**
     * Returns all Pokemon in this set that the given Pokemon could evolve
     * directly into before randomization.
     * @param pokemon The Pokemon to get evolutions of.
     * @return A PokemonSet containing all direct evolutions of the given Pokemon in this set.
     */
    public PokemonSet getOriginalEvolutions(Pokemon pokemon) {
        PokemonSet evolvedPokemon = pokemon.originalEvolvedForms;
        evolvedPokemon.retainAll(this);
        return evolvedPokemon;
    }

    /**
     * Returns all Pokemon in this set that the given Pokemon evolves from.
     * @param pokemon The Pokemon to get the pre-evolution of.
     * @return A PokemonSet containing all Pokemon that the given Pokemon evolves from in this set.
     */
    public PokemonSet getPreEvolutions(Pokemon pokemon) {
        //I *think* there are no cases of merged evolution? But... better not to assume that.
        PokemonSet preEvolvedPokemon = pokemon.getAllPreEvolvedPokemon();
        preEvolvedPokemon.retainAll(this);
        return preEvolvedPokemon;
    }

    /**
     * Returns all Pokemon in this set that the given Pokemon evolved from before randomization.
     * @param pokemon The Pokemon to get the pre-evolution of.
     * @return A PokemonSet containing all Pokemon that the given Pokemon evolves from in this set.
     */
    public PokemonSet getOriginalPreEvolutions(Pokemon pokemon) {
        //I *think* there are no cases of merged evolution? But... better not to assume that.
        //There are certainly forms—e.g., Burmy to Mothim.
        PokemonSet preEvolvedPokemon = pokemon.originalPreEvolvedForms;
        preEvolvedPokemon.retainAll(this);
        return preEvolvedPokemon;
    }

    /**
     * Checks whether this set contains any evolved forms of the given Pokemon.
     * @param pokemon The Pokemon to check for evolutions of.
     * @return true if this set contains at least one evolved form of the given Pokemon, false otherwise.
     */
    public boolean hasEvolutions(Pokemon pokemon) {
        PokemonSet evolvedPokemon = pokemon.getAllEvolvedPokemon();
        for (Pokemon evo : evolvedPokemon) {
            if(this.contains(evo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether this set contains any Pokemon the given Pokemon could
     * evolve into before randomization.
     * @param pokemon The Pokemon to check for evolutions of.
     * @return true if this set contains at least one evolved form of the given Pokemon, false otherwise.
     */
    public boolean hasOriginalEvolutions(Pokemon pokemon) {
        PokemonSet evolvedPokemon = pokemon.originalEvolvedForms;
        for (Pokemon evo : evolvedPokemon) {
            if(this.contains(evo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether this set contains any Pokemon that can evolve into the given Pokemon.
     * @param pokemon The Pokemon to check for pre-evolutions of.
     * @return true if this set contains at least one pre-evolved form of the given Pokemon, false otherwise.
     */
    public boolean hasPreEvolutions(Pokemon pokemon) {
        PokemonSet preEvolvedPokemon = pokemon.getAllPreEvolvedPokemon();
        for (Pokemon prevo : preEvolvedPokemon) {
            if(this.contains(prevo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether this set contains any Pokemon that could evolve
     * into the given Pokemon before randomization.
     * @param pokemon The Pokemon to check for pre-evolutions of.
     * @return true if this set contains at least one pre-evolved form of the given Pokemon, false otherwise.
     */
    public boolean hasOriginalPreEvolutions(Pokemon pokemon) {
        PokemonSet preEvolvedPokemon = pokemon.originalPreEvolvedForms;
        for (Pokemon prevo : preEvolvedPokemon) {
            if(this.contains(prevo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all Pokemon in this set that no other Pokemon in this set evolves into.
     * @return A PokemonSet containing all Pokemon that no other Pokemon in this set evolves into.
     */
    public PokemonSet getAllFirstInLine() {
        PokemonSet firstInLines = new PokemonSet();
        for(Pokemon pokemon : this) {
            if(!this.hasPreEvolutions(pokemon)) {
                firstInLines.add(pokemon);
            }
        }
        return firstInLines;
    }

    /**
     * Returns all Pokemon in this set that no other Pokemon in this set
     * could evolve into before randomization.
     * @return A PokemonSet containing all Pokemon that no other Pokemon in this set evolves into.
     */
    public PokemonSet getAllOriginalFirstInLine() {
        PokemonSet firstInLines = new PokemonSet();
        for(Pokemon pokemon : this) {
            if(!this.hasOriginalPreEvolutions(pokemon)) {
                firstInLines.add(pokemon);
            }
        }
        return firstInLines;
    }

    /**
     * Returns all Pokemon in this set that no other Pokemon (in this set or otherwise) evolves into.
     * @return A PokemonSet containing all basic Pokemon in this set.
     */
    public PokemonSet getAllBasicPokemon() {
        PokemonSet basics = new PokemonSet();
        for(Pokemon pokemon : this) {
            if(pokemon.evolutionsTo.isEmpty()) {
                basics.add(pokemon);
            }
        }
        return basics;
    }

    /**
     * Returns all Pokemon in this set that no other Pokemon (in this set or otherwise)
     * could evolve into before randomization.
     * @return A PokemonSet containing all basic Pokemon in this set.
     */
    public PokemonSet getAllOriginalBasicPokemon() {
        PokemonSet basics = new PokemonSet();
        for(Pokemon pokemon : this) {
            if(pokemon.originalPreEvolvedForms.isEmpty()) {
                basics.add(pokemon);
            }
        }
        return basics;
    }

    /**
     * Returns all Pokemon for which uninterrupted evolutionary lines of at least the given length are contained
     * within this set.
     * In the case of branching evolutions, only branches of the correct length will be included.
     * @param length the number of Pokemon required in the evolutionary line. 1 to 3.
     * @return a PokemonSet containing all valid Pokemon.
     */
    public PokemonSet getEvoLinesAtLeastLength(int length) {
        if(length > 3 || length < 1) {
            throw new IllegalArgumentException("Invalid evolutionary line length.");
        }
        if(length == 1) {
            return new PokemonSet(this);
        }

        PokemonSet validEvoLines = new PokemonSet();
        PokemonSet firstInLines = this.getAllFirstInLine();

        for(Pokemon basic : firstInLines) {
            for (Pokemon firstEvo : this.getEvolutions(basic)) {
                if(length == 2) {
                    validEvoLines.add(basic);
                    validEvoLines.add(firstEvo);
                }
                for (Pokemon secondEvo : this.getEvolutions(firstEvo)) {
                    validEvoLines.add(basic);
                    validEvoLines.add(firstEvo);
                    validEvoLines.add(secondEvo);
                }
            }
        }

        return validEvoLines;
    }

    /**
     * Returns all Pokemon for which uninterrupted evolutionary lines (before randomization)
     * of at least the given length are contained within this set.
     * In the case of branching evolutions, only branches of the correct length will be included.
     * @param length the number of Pokemon required in the evolutionary line. 1 to 3.
     * @return a PokemonSet containing all valid Pokemon.
     */
    public PokemonSet Original(int length) {
        if(length > 3 || length < 1) {
            throw new IllegalArgumentException("Invalid evolutionary line length.");
        }
        if(length == 1) {
            return new PokemonSet(this);
        }

        PokemonSet validEvoLines = new PokemonSet();
        PokemonSet firstInLines = this.getAllOriginalFirstInLine();

        for(Pokemon basic : firstInLines) {
            for (Pokemon firstEvo : this.getOriginalEvolutions(basic)) {
                if(length == 2) {
                    validEvoLines.add(basic);
                    validEvoLines.add(firstEvo);
                }
                for (Pokemon secondEvo : this.getOriginalEvolutions(firstEvo)) {
                    validEvoLines.add(basic);
                    validEvoLines.add(firstEvo);
                    validEvoLines.add(secondEvo);
                }
            }
        }

        return validEvoLines;
    }

    public boolean add(Pokemon pokemon) {
        if(internalSet.contains(pokemon)) {
            return false;
        }
        internalSet.add(pokemon);

        addToType(pokemon, pokemon.primaryType);
        if(pokemon.secondaryType != null) {
            addToType(pokemon, pokemon.secondaryType);
        }

        return true;
    }

    private void addToType(Pokemon pokemon, Type type) {
        HashSet<Pokemon> typeSet = typeMap.get(type);
        if(typeSet == null) {
            typeSet = new HashSet<>();
            typeMap.put(type, typeSet);
        }
        typeSet.add(pokemon);
    }

    public boolean remove(Object o) {
        if(!(o instanceof Pokemon)) {
            return false;
        }
        Pokemon pokemon = (Pokemon) o;
        if(!internalSet.contains(pokemon)) {
            return false;
        }

        internalSet.remove(pokemon);
        typeMap.get(pokemon.primaryType).remove(pokemon);
        if(pokemon.secondaryType != null) {
            typeMap.get(pokemon.secondaryType).remove(pokemon);
        }
        return true;
    }

    public boolean addAll(Collection<? extends Pokemon> c) {
        boolean changed = false;
        for (Pokemon pokemon : c) {
            boolean added = this.add(pokemon);
            if(added) {
                changed = true;
            }
        }
        return changed;
    }

    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        for(Pokemon pokemon : this) {
            if(!c.contains(pokemon)) {
                remove(pokemon);
                changed = true;
            }
        }
        return changed;
    }

    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            boolean removed = this.remove(o);
            if(removed) {
                changed = true;
            }
        }
        return changed;
    }

    public void clear() {
        internalSet = new HashSet<>();
        typeMap = new EnumMap<>(Type.class);
    }

    //pass-through functions
    public int size() { return internalSet.size(); }

    public boolean isEmpty() { return internalSet.isEmpty(); }

    public boolean contains(Object o) { return internalSet.contains(o); }

    public Iterator<Pokemon> iterator() { return internalSet.iterator(); }

    public Object[] toArray() { return internalSet.toArray(); }

    public <T> T[] toArray(T[] a) { return internalSet.toArray(a); }

    public boolean containsAll(Collection<?> c) { return internalSet.containsAll(c); }
}
