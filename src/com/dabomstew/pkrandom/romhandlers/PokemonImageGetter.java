package com.dabomstew.pkrandom.romhandlers;

import com.dabomstew.pkrandom.pokemon.Pokemon;

import java.awt.image.BufferedImage;

public abstract class PokemonImageGetter {
    protected Pokemon pk;
    protected boolean back;
    protected boolean shiny;
    protected boolean transparentBackground;
    protected boolean includePalette;

    public PokemonImageGetter(Pokemon pk) {
        this.pk = pk;
    }

    public PokemonImageGetter setBack(boolean back) {
        this.back = back;
        return this;
    }

    public PokemonImageGetter setShiny(boolean shiny) {
        this.shiny = shiny;
        return this;
    }

    public PokemonImageGetter setTransparentBackground(boolean transparentBackground) {
        this.transparentBackground = transparentBackground;
        return this;
    }

    public PokemonImageGetter setIncludePalette(boolean includePalette) {
        this.includePalette = includePalette;
        return this;
    }

    public abstract BufferedImage get();
}
