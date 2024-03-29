package com.dabomstew.pkrandom.romhandlers;

import com.dabomstew.pkrandom.pokemon.Pokemon;

import java.awt.image.BufferedImage;

public abstract class PokemonImageGetter {
    protected Pokemon pk;
    protected boolean back;
    protected boolean shiny;
    protected boolean transparentBackground;
    protected boolean includePalette;
    protected int forme;

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

    public PokemonImageGetter setForme(int forme) {
        if (forme < 0) {
            throw new IllegalArgumentException("forme must be positive");
        }
        if (forme >= getFormeAmount()) {
            throw new IllegalArgumentException("invalid/too high forme for this Pokemon, " + forme + " for " + pk.fullName());
        }
        this.forme = forme;
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

    /**
     * Returns the amount of graphical formes for the {@link Pokemon} this PokemonImageGetter is set to work with.
     * I.e. returns the highest valid value for {@link #setForme(int)}+1.
     */
    public int getFormeAmount() {
        return 1;
    }

    public abstract BufferedImage get();

    /**
     * Returns a stitched together image of all the Pokemon's gettable images.
     */
    public abstract BufferedImage getFull();
}
