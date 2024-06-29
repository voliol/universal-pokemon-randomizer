package com.dabomstew.pkrandom.pokemon.cueh;

import com.dabomstew.pkrandom.pokemon.Pokemon;

@FunctionalInterface
public interface EvolvedPokemonAction<T extends Pokemon> {
    void applyTo(T evFrom, T evTo, boolean toMonIsFinalEvo);
}