package com.dabomstew.pkrandom.graphics;

import java.util.List;
import java.util.Random;

import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Type;

public class Gen2PaletteHandler extends PaletteHandler {

    public Gen2PaletteHandler(Random random) {
        super(random);
    }

    @Override
    public void randomizePokemonPalettes(List<Pokemon> pokemonList, boolean typeSanity, boolean evolutionSanity,
            boolean shinyFromNormal) {

        if (shinyFromNormal) {
            makeShinyPalettesFromNormal(pokemonList);
        }

        // TODO: implement evolutionSanity
        for (Pokemon pk : pokemonList) {
            pk.setNormalPalette(
                    typeSanity ? getRandom2ColorPalette(pk.primaryType, pk.secondaryType) : getRandom2ColorPalette());
        }
    }

    private Palette getRandom2ColorPalette() {
        Palette palette = new Palette(2);
        Color brightColor = new Color(random.nextInt(192, 255), random.nextInt(192, 255), random.nextInt(192, 255));
        Color darkColor = new Color(random.nextInt(64, 192), random.nextInt(64, 192), random.nextInt(64, 192));
        palette.setColor(0, brightColor);
        palette.setColor(1, darkColor);
        return palette;
    }

    private Palette getRandom2ColorPalette(Type primaryType, Type secondaryType) {
        // TODO : implement type colors
        return getRandom2ColorPalette();
    }

}
