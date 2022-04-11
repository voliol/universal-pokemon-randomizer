package com.dabomstew.pkrandom.graphics;

import java.util.List;
import java.util.Random;

import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Type;

public class Gen1PaletteHandler extends PaletteHandler {

    private static final byte DEFAULT_GEN1_PALETTE_ID = 10;
    private static final byte[][] GEN1_TYPE_PALETTE_IDS = { { 21, 23, 25 }, // NORMAL
            { 21, 25 }, // FIGHTING
            { 17 }, // FLYING
            { 22 }, // GRASS
            { 17, 19 }, // WATER
            { 18 }, // FIRE
            { 25 }, // ROCK
            { 21 }, // GROUND
            { 16, 24 }, // PSYCHIC
            { 22, 24 }, // BUG
            { 17, 21, 25 }, // DRAGON
            { 24 }, // ELECTRIC
            { 20 }, // GHOST
            { 20 }, // POISON
            { 17, 19 } // ICE
    };

    public Gen1PaletteHandler(Random random) {
        super(random);
    }

    @Override
    public void randomizePokemonPalettes(List<Pokemon> pokemonList, boolean typeSanity, boolean evolutionSanity,
            boolean shinyFromNormal) {
        // obviously shinyFromNormal is not used, it is here for a hopefully prettier
        // class structure

        // TODO: implement evolutionSanity
        for (Pokemon pk : pokemonList) {
            byte newPaletteID = typeSanity ? getRandomGen1PaletteID(pk.primaryType) : getRandomGen1PaletteID();
            pk.setPaletteID(newPaletteID);
        }
        // TODO: double-check these colors work right
    }

    private byte getRandomGen1PaletteID() {
        return (byte) random.nextInt(16, 26);
    }

    private byte getRandomGen1PaletteID(Type type) {
        byte paletteID = DEFAULT_GEN1_PALETTE_ID;
        if (type.ordinal() < GEN1_TYPE_PALETTE_IDS.length) {
            byte[] typePosPaletteIDs = GEN1_TYPE_PALETTE_IDS[type.ordinal()];
            paletteID = typePosPaletteIDs[random.nextInt(typePosPaletteIDs.length)];
        }
        return paletteID;
    }

}
