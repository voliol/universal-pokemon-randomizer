package com.dabomstew.pkrandom.newgui;

import com.dabomstew.pkrandom.romhandlers.Abstract3DSRomHandler;
import com.dabomstew.pkrandom.romhandlers.AbstractDSRomHandler;
import com.dabomstew.pkrandom.romhandlers.PokemonImageGetter;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.awt.image.BufferedImage;
import java.util.Random;

public class MascotGetter {

    private final Random random;

    public MascotGetter(Random random) {
        this.random = random;
    }

    public BufferedImage getMascotImage(RomHandler romHandler) {
        System.out.println("Getting mascot image...");
        if (romHandler.hasPokemonImageGetter()) {
            return getMascotUsingPIG(romHandler);
        } else if (romHandler instanceof Abstract3DSRomHandler ab3DSRomHandler) {
            return getMascotIcon(ab3DSRomHandler);
        }
        System.out.println("neither");
        return null;
    }

    private BufferedImage getMascotUsingPIG(RomHandler romHandler) {
        System.out.println("pig");
        PokemonImageGetter pig = romHandler.createPokemonImageGetter(romHandler.getPokemonSet().getRandom(random))
                .setShiny(random.nextInt(10) == 0 && romHandler.generationOfPokemon() != 1)
                .setTransparentBackground(true);
        if (pig instanceof AbstractDSRomHandler.DSPokemonImageGetter dsPig) {
            pig = dsPig.setGender(random.nextInt(2));
        }
        return pig.get();
    }

    private BufferedImage getMascotIcon(Abstract3DSRomHandler romHandler) {
        System.out.println("icons");
        // ideally the 3DS games would have a PokemonImageGetter, but they don't, so we use this somewhat hacky
        // method instead to get a random Pokemon icon for the mascot.
        int iconIndex = random.nextInt(romHandler.getIconGARCSize() - 1) + 1;
        return romHandler.getPokemonIcon(iconIndex);
    }
}
