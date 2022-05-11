package com.dabomstew.pkrandom.graphics;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BaseColorMap {

	private static final double TWEAK_RAND_MIN_COEFF = -0.032;
	private static final double TWEAK_RAND_MIN_ADDEND = 7.1;
	private static final double TWEAK_RAND_MAX_COEFF = -0.042;
	private static final double TWEAK_RAND_MAX_ADDEND = 17.19;

	private final Map<TypeColor, Color> baseColors;
	private final Map<TypeColor, LightDarkMode> lightDarkModes;

	public BaseColorMap(Random random) {
		baseColors = initBaseColors(random);
		lightDarkModes = initLightDarkModes(random);
	}

	private Map<TypeColor, Color> initBaseColors(Random random) {
		Map<TypeColor, Color> baseColors = new HashMap<>();
		for (TypeColor tbc : Gen3to5TypeColors.getAllTypeColors()) {
			Color baseColor = randomlyTweakColor(tbc, random);
			baseColors.put(tbc, baseColor);
		}
		return baseColors;
	}

	private Color randomlyTweakColor(Color color, Random random) {
		// based on Artemis251's Emerald randomizer code
		Color tweakedColor = new Color();
		for (int i = 0; i < 3; i++) { // for each in R,G,B
			int randMin = (int) (TWEAK_RAND_MIN_COEFF * color.getComp(i) + TWEAK_RAND_MIN_ADDEND);
			int randMax = (int) (TWEAK_RAND_MAX_COEFF * color.getComp(i) + TWEAK_RAND_MAX_ADDEND);
			int change = random.nextInt(randMax - randMin) + random.nextInt(randMax);
			int value = color.getComp(i) + (int) (color.getComp(i) * change * 0.01 * Math.pow(-1, random.nextInt(2)));
			value = value > 255 ? 255 : value;
			tweakedColor.setComp(i, value);
		}
		return tweakedColor;
	}

	private Map<TypeColor, LightDarkMode> initLightDarkModes(Random random) {
		Map<TypeColor, LightDarkMode> lightDarkModes = new HashMap<>();
		
		// makes sure it is synced with baseColors key-wise
		if (baseColors == null) {
			throw new IllegalStateException("lightDarkModes must be initialized after baseColors"); 
		}
		for (TypeColor tbc : baseColors.keySet()) {
			LightDarkMode lightDarkMode = LightDarkMode.randomLightDarkMode(random);
			lightDarkModes.put(tbc, lightDarkMode);
			System.out.println(lightDarkMode);
		}
		
		return lightDarkModes;
	}

	public Color getBaseColor(TypeColor typeBaseColor) {
		// unsure if this is where to put copy protection
		return baseColors.get(typeBaseColor).clone();
	}
	
	public LightDarkMode getLightDarkMode(TypeColor typeBaseColor) {
		return lightDarkModes.get(typeBaseColor);
	}

}
