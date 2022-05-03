package com.dabomstew.pkrandom.romhandlers;

import com.dabomstew.pkrandom.pokemon.Pokemon;

public interface EvolvedPokemonAction {

	public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo);

}
