package com.dabomstew.pkrandom.graphics;

/*----------------------------------------------------------------------------*/
/*--  Part of "Universal Pokemon Randomizer" by Dabomstew                   --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2012.                                 --*/
/*--                                                                        --*/
/*--  The custom code written here is licensed under the terms of the GPL:  --*/
/*--                                                                        --*/
/*--  This program is free software: you can redistribute it and/or modify  --*/
/*--  it under the terms of the GNU General Public License as published by  --*/
/*--  the Free Software Foundation, either version 3 of the License, or     --*/
/*--  (at your option) any later version.                                   --*/
/*--                                                                        --*/
/*--  This program is distributed in the hope that it will be useful,       --*/
/*--  but WITHOUT ANY WARRANTY; without even the implied warranty of        --*/
/*--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          --*/
/*--  GNU General Public License for more details.                          --*/
/*--                                                                        --*/
/*--  You should have received a copy of the GNU General Public License     --*/
/*--  along with this program. If not, see <http://www.gnu.org/licenses/>.  --*/
/*----------------------------------------------------------------------------*/

import java.util.Random;

import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Type;
//import com.dabomstew.pkrandom.romhandlers.BasePokemonAction;
//import com.dabomstew.pkrandom.romhandlers.CopyUpEvolutionsHelper;
//import com.dabomstew.pkrandom.romhandlers.EvolvedPokemonAction;

/**
 * A {@link PaletteHandler} for Gen 2 games (G/S/C).
 * <p>
 * Pokémon palettes in Gen 2 do de facto only have two colors, as their sprites can
 * only have four, and two color slots are always occupied by white respectively
 * black. The remaining two colors are here divided into "bright colors" and
 * "dark colors", as those descriptors generally correspond to each of the color
 * slots.
 */
public class Gen2PaletteHandler extends PaletteHandler {

	private boolean typeSanity;
	private boolean shinyFromNormal;

	public Gen2PaletteHandler(Random random) {
		super(random);
	}

//	@Override
//	public void randomizePokemonPalettes(CopyUpEvolutionsHelper<Pokemon> copyUpEvolutionsHelper, boolean typeSanity,
//			boolean evolutionSanity, boolean shinyFromNormal) {
//
//		this.typeSanity = typeSanity;
//		this.shinyFromNormal = shinyFromNormal;
//		copyUpEvolutionsHelper.apply(evolutionSanity, false, new BasePokemonPaletteAction(),
//				new EvolvedPokemonPaletteAction());
//
//	}

	private Palette getRandom2ColorPalette() {
		Palette palette = new Palette(2);
		palette.setColor(0, Gen2TypeColors.getRandomBrightColor(random));
		palette.setColor(1, Gen2TypeColors.getRandomDarkColor(random));
		return palette;
	}

	private Palette getRandom2ColorPalette(Type primaryType, Type secondaryType) {
		Palette palette = new Palette(2);
		Color brightColor = Gen2TypeColors.getRandomBrightColor(primaryType, random);
		Color darkColor = Gen2TypeColors.getRandomDarkColor(secondaryType == null ? primaryType : secondaryType,
				random);
		palette.setColor(0, brightColor);
		palette.setColor(1, darkColor);
		return palette;
	}

//	private class BasePokemonPaletteAction implements BasePokemonAction<Pokemon> {
//
//		@Override
//		public void applyTo(Pokemon pk) {
//			if (shinyFromNormal) {
//				setShinyPaletteFromNormal(pk);
//			}
//			setNormalPaletteRandom(pk);
//		}
//
//		private void setNormalPaletteRandom(Pokemon pk) {
//			pk.setNormalPalette(typeSanity ? getRandom2ColorPalette(pk.primaryType, pk.secondaryType)
//					: getRandom2ColorPalette());
//		}
//
//	}
//
//	private class EvolvedPokemonPaletteAction implements EvolvedPokemonAction<Pokemon> {
//
//		@Override
//		public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
//			if (shinyFromNormal) {
//				setShinyPaletteFromNormal(evTo);
//			}
//			setNormalPaletteFromPrevo(evFrom, evTo);
//		}
//
//		private void setNormalPaletteFromPrevo(Pokemon evFrom, Pokemon evTo) {
//			Palette palette = evFrom.normalPalette.clone();
//
//			if (typeSanity) {
//				if (evTo.primaryType != evFrom.primaryType) {
//					Color newBrightColor = Gen2TypeColors.getRandomBrightColor(evTo.primaryType, random);
//					palette.setColor(0, newBrightColor);
//
//				} else if (evTo.secondaryType != evFrom.secondaryType) {
//					Color newDarkColor = Gen2TypeColors.getRandomDarkColor(
//							evTo.secondaryType == null ? evTo.primaryType : evTo.secondaryType, random);
//					palette.setColor(1, newDarkColor);
//				}
//			}
//
//			evTo.normalPalette = palette;
//		}
//
//	}

}
