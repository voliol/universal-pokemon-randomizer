package com.dabomstew.pkrandom.randomizers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.romhandlers.RomHandler;
import com.dabomstew.pkrandom.services.RestrictedPokemonService;

import java.util.Random;

public class IntroPokemonRandomizer {

    private final RomHandler romHandler;
    private final RestrictedPokemonService rPokeService;
    private final Random random;

    public IntroPokemonRandomizer(RomHandler romHandler, Random random) {
        this.romHandler = romHandler;
        this.rPokeService = romHandler.getRestrictedPokemonService();
        this.random = random;
    }

    public void randomizeIntroPokemon() {
        Pokemon pk = rPokeService.getAll(true).getRandom(random);
        while (!romHandler.setIntroPokemon(pk)) {
            pk = rPokeService.getAll(true).getRandom(random);
        }
    }
}
