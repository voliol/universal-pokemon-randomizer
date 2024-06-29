package com.dabomstew.pkrandom.pokemon.cueh;

import com.dabomstew.pkrandom.pokemon.Pokemon;

@FunctionalInterface
public interface BasicPokemonAction<T extends Pokemon> {
    void applyTo(T pk);
}
