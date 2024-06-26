package com.dabomstew.pkrandom.randomizers;

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

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.graphics.palettes.SGBPaletteID;
import com.dabomstew.pkrandom.pokemon.CopyUpEvolutionsHelper;
import com.dabomstew.pkrandom.pokemon.Gen1Pokemon;
import com.dabomstew.pkrandom.pokemon.PokemonSet;
import com.dabomstew.pkrandom.pokemon.Type;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.util.EnumMap;
import java.util.Random;

/**
 * A {@link PaletteRandomizer} for Gen 1 games (R/B/Y).
 */
public class Gen1PaletteRandomizer extends PaletteRandomizer {

	private static final SGBPaletteID DEFAULT_PALETTE_ID = SGBPaletteID.MEWMON;
	private static final EnumMap<Type, SGBPaletteID[]> TYPE_PALETTE_IDS = initTypePaletteIDs();

	private boolean typeSanity;

	public Gen1PaletteRandomizer(RomHandler romHandler, Settings settings, Random random) {
		super(romHandler, settings, random);
	}

	private static EnumMap<Type, SGBPaletteID[]> initTypePaletteIDs() {
		EnumMap<Type, SGBPaletteID[]> typePaletteIDs = new EnumMap<>(Type.class);
		typePaletteIDs.put(Type.NORMAL, new SGBPaletteID[] { SGBPaletteID.BROWNMON, SGBPaletteID.PINKMON, SGBPaletteID.GREYMON });
		typePaletteIDs.put(Type.FIGHTING, new SGBPaletteID[] { SGBPaletteID.BROWNMON, SGBPaletteID.GREYMON });
		typePaletteIDs.put(Type.FLYING, new SGBPaletteID[] { SGBPaletteID.BLUEMON });
		typePaletteIDs.put(Type.GRASS, new SGBPaletteID[] { SGBPaletteID.GREENMON });
		typePaletteIDs.put(Type.WATER, new SGBPaletteID[] { SGBPaletteID.BLUEMON, SGBPaletteID.CYANMON });
		typePaletteIDs.put(Type.FIRE, new SGBPaletteID[] { SGBPaletteID.REDMON });
		typePaletteIDs.put(Type.ROCK, new SGBPaletteID[] { SGBPaletteID.GREYMON });
		typePaletteIDs.put(Type.GROUND, new SGBPaletteID[] { SGBPaletteID.BROWNMON });
		typePaletteIDs.put(Type.PSYCHIC, new SGBPaletteID[] { SGBPaletteID.MEWMON, SGBPaletteID.YELLOWMON });
		typePaletteIDs.put(Type.BUG, new SGBPaletteID[] { SGBPaletteID.GREENMON, SGBPaletteID.YELLOWMON });
		typePaletteIDs.put(Type.DRAGON, new SGBPaletteID[] { SGBPaletteID.BLUEMON, SGBPaletteID.BROWNMON, SGBPaletteID.GREYMON });
		typePaletteIDs.put(Type.ELECTRIC, new SGBPaletteID[] { SGBPaletteID.YELLOWMON });
		typePaletteIDs.put(Type.GHOST, new SGBPaletteID[] { SGBPaletteID.PURPLEMON });
		typePaletteIDs.put(Type.POISON, new SGBPaletteID[] { SGBPaletteID.PURPLEMON });
		typePaletteIDs.put(Type.ICE, new SGBPaletteID[] { SGBPaletteID.BLUEMON, SGBPaletteID.CYANMON });
		return typePaletteIDs;
	}

	@Override
	public void randomizePokemonPalettes() {
		this.typeSanity = settings.isPokemonPalettesFollowTypes();
		boolean evolutionSanity = settings.isPokemonPalettesFollowEvolutions();

		CopyUpEvolutionsHelper<Gen1Pokemon> cueh = new CopyUpEvolutionsHelper<>(() ->
				new PokemonSet<>(romHandler.getPokemonSet(), new Gen1Pokemon(0)));
		cueh.apply(evolutionSanity, true, new BasePokemonIDAction(), new EvolvedPokemonIDAction());
	}

	private SGBPaletteID getRandomPaletteID() {
		return SGBPaletteID.getRandomPokemonPaletteID(random);
	}

	private SGBPaletteID getRandomPaletteID(Type type) {
		SGBPaletteID[] typeIDs = TYPE_PALETTE_IDS.get(type);
		return typeIDs == null ? DEFAULT_PALETTE_ID : typeIDs[random.nextInt(typeIDs.length)];
	}

	private class BasePokemonIDAction implements CopyUpEvolutionsHelper.BasicPokemonAction<Gen1Pokemon> {

		@Override
		public void applyTo(Gen1Pokemon pk) {
			pk.setPaletteID(typeSanity ? getRandomPaletteID(pk.getPrimaryType()) : getRandomPaletteID());
		}

	}

	private class EvolvedPokemonIDAction implements CopyUpEvolutionsHelper.EvolvedPokemonAction<Gen1Pokemon> {

		@Override
		public void applyTo(Gen1Pokemon evFrom, Gen1Pokemon evTo, boolean toMonIsFinalEvo) {
			SGBPaletteID newPaletteID;
			if (typeSanity && !evTo.getPrimaryType().equals(evFrom.getPrimaryType())) {
				SGBPaletteID[] typeIDs = TYPE_PALETTE_IDS.get(evTo.getPrimaryType());
				newPaletteID = contains(typeIDs, evFrom.getPaletteID()) ? evFrom.getPaletteID()
						: getRandomPaletteID(evTo.getPrimaryType());

			} else {
				newPaletteID = evFrom.getPaletteID();
			}
			evTo.setPaletteID(newPaletteID);
		}

	}

	private boolean contains(SGBPaletteID[] paletteIDs, SGBPaletteID pid) {
		for (SGBPaletteID pid2 : paletteIDs) {
			if (pid == pid2) {
				return true;
			}
		}
		return false;
	}

}
