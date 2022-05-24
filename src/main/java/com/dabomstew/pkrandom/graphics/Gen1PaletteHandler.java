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

import java.util.EnumMap;
import java.util.Random;

import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Type;
import com.dabomstew.pkrandom.romhandlers.BasePokemonAction;
import com.dabomstew.pkrandom.romhandlers.CopyUpEvolutionsHelper;
import com.dabomstew.pkrandom.romhandlers.EvolvedPokemonAction;

/**
 * A {@link PaletteHandler} for Gen 1 games (R/B/Y).
 */
public class Gen1PaletteHandler extends PaletteHandler {

	private static final byte DEFAULT_PALETTE_ID = 16;
	private static final EnumMap<Type, byte[]> TYPE_PALETTE_IDS = initTypePaletteIDs();

	private boolean typeSanity;

	public Gen1PaletteHandler(Random random) {
		super(random);
	}

	private static EnumMap<Type, byte[]> initTypePaletteIDs() {
		EnumMap<Type, byte[]> typePaletteIDs = new EnumMap<>(Type.class);
		typePaletteIDs.put(Type.NORMAL, new byte[] { 21, 23, 25 });
		typePaletteIDs.put(Type.FIGHTING, new byte[] { 21, 25 });
		typePaletteIDs.put(Type.FLYING, new byte[] { 17 });
		typePaletteIDs.put(Type.GRASS, new byte[] { 22 });
		typePaletteIDs.put(Type.WATER, new byte[] { 17, 19 });
		typePaletteIDs.put(Type.FIRE, new byte[] { 18 });
		typePaletteIDs.put(Type.ROCK, new byte[] { 25 });
		typePaletteIDs.put(Type.GROUND, new byte[] { 21 });
		typePaletteIDs.put(Type.PSYCHIC, new byte[] { 16, 24 });
		typePaletteIDs.put(Type.BUG, new byte[] { 22, 24 });
		typePaletteIDs.put(Type.DRAGON, new byte[] { 17, 21, 25 });
		typePaletteIDs.put(Type.ELECTRIC, new byte[] { 24 });
		typePaletteIDs.put(Type.GHOST, new byte[] { 20 });
		typePaletteIDs.put(Type.POISON, new byte[] { 20 });
		typePaletteIDs.put(Type.ICE, new byte[] { 17, 19 });
		return typePaletteIDs;
	}

	@Override
	public void randomizePokemonPalettes(CopyUpEvolutionsHelper copyUpEvolutionsHelper, boolean typeSanity,
			boolean evolutionSanity, boolean shinyFromNormal) {
		// obviously shinyFromNormal is not used, it is here for a hopefully prettier
		// class structure
		this.typeSanity = typeSanity;
		copyUpEvolutionsHelper.apply(evolutionSanity, false, new BasePokemonIDAction(), new EvolvedPokemonIDAction());
	}

	private byte getRandomPaletteID() {
		return (byte) random.nextInt(16, 26);
	}

	private byte getRandomPaletteID(Type type) {
		byte[] typeIDs = TYPE_PALETTE_IDS.get(type);
		byte paletteID = typeIDs == null ? DEFAULT_PALETTE_ID : typeIDs[random.nextInt(typeIDs.length)];
		return paletteID;
	}

	private class BasePokemonIDAction implements BasePokemonAction {

		@Override
		public void applyTo(Pokemon pk) {
			byte newPaletteID = typeSanity ? getRandomPaletteID(pk.primaryType) : getRandomPaletteID();
			pk.setPaletteID(newPaletteID);
		}

	}

	private class EvolvedPokemonIDAction implements EvolvedPokemonAction {

		@Override
		public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
			byte newPaletteID;
			if (typeSanity && !evTo.getPrimaryType().equals(evFrom.getPrimaryType())) {
				byte[] typeIDs = TYPE_PALETTE_IDS.get(evTo.getPrimaryType());
				newPaletteID = contains(typeIDs, evFrom.getPaletteID()) ? evFrom.getPaletteID()
						: getRandomPaletteID(evTo.primaryType);

			} else {
				newPaletteID = evFrom.getPaletteID();
			}
			evTo.setPaletteID(newPaletteID);
		}

	}

	private boolean contains(byte[] bytes, byte b) {
		for (byte b2 : bytes) {
			if (b == b2) {
				return true;
			}
		}
		return false;
	}

}
