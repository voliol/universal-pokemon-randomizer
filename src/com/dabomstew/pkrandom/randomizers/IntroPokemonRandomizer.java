package com.dabomstew.pkrandom.randomizers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.util.Random;

public class IntroPokemonRandomizer extends Randomizer {

    public IntroPokemonRandomizer(RomHandler romHandler, Settings settings, Random random) {
        super(romHandler, settings, random);
    }

    public void randomizeIntroPokemon() {
        Pokemon pk = rPokeService.getAll(true).getRandom(random);
        while (!romHandler.setIntroPokemon(pk)) {
            pk = rPokeService.getAll(true).getRandom(random);
        }
    }
}
