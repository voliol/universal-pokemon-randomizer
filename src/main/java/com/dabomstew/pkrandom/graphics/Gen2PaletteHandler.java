package com.dabomstew.pkrandom.graphics;

import java.util.Random;

import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Type;
import com.dabomstew.pkrandom.romhandlers.BasePokemonAction;
import com.dabomstew.pkrandom.romhandlers.CopyUpEvolutionsHelper;
import com.dabomstew.pkrandom.romhandlers.EvolvedPokemonAction;

public class Gen2PaletteHandler extends PaletteHandler {

	private boolean typeSanity;
	private boolean shinyFromNormal;

	public Gen2PaletteHandler(Random random) {
		super(random);
	}

	@Override
	public void randomizePokemonPalettes(CopyUpEvolutionsHelper copyUpEvolutionsHelper, boolean typeSanity,
			boolean evolutionSanity, boolean shinyFromNormal) {

		this.typeSanity = typeSanity;
		this.shinyFromNormal = shinyFromNormal;
		copyUpEvolutionsHelper.apply(evolutionSanity, false, new BasePokemonPaletteAction(),
				new EvolvedPokemonPaletteAction());

	}

	private Palette getRandom2ColorPalette() {
		Palette palette = new Palette(2);
		palette.setColor(0, Gen2TypeColors.getRandomBrightColor(random));
		palette.setColor(1, Gen2TypeColors.getRandomDarkColor(random));
		return palette;
	}

	private Palette getRandom2ColorPalette(Type primaryType, Type secondaryType) {
		Palette palette = new Palette(2);
		Color brightColor = Gen2TypeColors.getRandomBrightColor(primaryType, random);
		Color darkColor = Gen2TypeColors.getRandomDarkColor(secondaryType == null ? primaryType : secondaryType, random);
		palette.setColor(0, brightColor);
		palette.setColor(1, darkColor);
		return palette;
	}

	private class BasePokemonPaletteAction implements BasePokemonAction {

		@Override
		public void applyTo(Pokemon pk) {
			if (shinyFromNormal) {
				setShinyPaletteFromNormal(pk);
			}
			setNormalPaletteRandom(pk);
		}

		private void setNormalPaletteRandom(Pokemon pk) {
			pk.setNormalPalette(typeSanity ? getRandom2ColorPalette(pk.getPrimaryType(), pk.getSecondaryType())
					: getRandom2ColorPalette());
		}

	}

	private class EvolvedPokemonPaletteAction implements EvolvedPokemonAction {
		@Override
		public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
			if (shinyFromNormal) {
				setShinyPaletteFromNormal(evTo);
			}
			setNormalPaletteFromPrevo(evFrom, evTo);
		}

		private void setNormalPaletteFromPrevo(Pokemon evFrom, Pokemon evTo) {
			Palette palette = evFrom.getNormalPalette().clone();

			if (typeSanity) {
				if (evTo.getPrimaryType() != evFrom.getPrimaryType()) {
					Color newBrightColor = Gen2TypeColors.getRandomBrightColor(evTo.getPrimaryType(), random);
					palette.setColor(0, newBrightColor);

				} else if (evTo.getSecondaryType() != evFrom.getSecondaryType()) {
					Color newDarkColor = Gen2TypeColors.getRandomDarkColor(
							evTo.getSecondaryType() == null ? evTo.getPrimaryType() : evTo.getSecondaryType(), random);
					palette.setColor(1, newDarkColor);
				}
			}

			evTo.setNormalPalette(palette);
		}

	}

}
