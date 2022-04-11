package com.dabomstew.pkrandom.graphics;

import java.util.List;
// temporary random for testing (?)
import java.util.Random;

import com.dabomstew.pkrandom.pokemon.Pokemon;

public abstract class PaletteHandler {

    protected Random random;

    public PaletteHandler(Random random) {
        this.random = random;
    }

    public PaletteHandler(int seed) {
        this.random = new Random(seed);
    }

    public abstract void randomizePokemonPalettes(List<Pokemon> pokemonList, boolean typeSanity,
            boolean evolutionSanity, boolean shinyFromNormal);

    protected void makeShinyPalettesFromNormal(List<Pokemon> mainPokemonList) {
        for (Pokemon pk : mainPokemonList) {
            pk.setShinyPalette(new Palette(pk.getNormalPalette()));
        }
    }

}
