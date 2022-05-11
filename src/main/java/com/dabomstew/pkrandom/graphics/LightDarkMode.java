package com.dabomstew.pkrandom.graphics;

import java.util.Random;

public enum LightDarkMode {
    DEFAULT, LIGHT, DARK;
    
    private static final double LIGHTDARK_CHANCE = 0.2; // chance to be light or dark
    
    public static LightDarkMode randomLightDarkMode(Random random) {
    	if (random.nextDouble() < LIGHTDARK_CHANCE) {
    		return random.nextBoolean() ? LIGHT : DARK;
    	} else {
    		return DEFAULT;
    	}
    }
}
