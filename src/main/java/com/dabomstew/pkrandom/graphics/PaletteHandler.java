package com.dabomstew.pkrandom.graphics;

import java.util.Random;

import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.romhandlers.CopyUpEvolutionsHelper;

public abstract class PaletteHandler {

    protected Random random;

    public PaletteHandler(Random random) {
        this.random = random;
    }

    public PaletteHandler(int seed) {
        this.random = new Random(seed);
    }

    public abstract void randomizePokemonPalettes(CopyUpEvolutionsHelper copyUpEvolutionsHelper, boolean typeSanity,
            boolean evolutionSanity, boolean shinyFromNormal);

    protected void setShinyPaletteFromNormal(Pokemon pk) {
		pk.setShinyPalette(new Palette(pk.getNormalPalette()));
	}

}
