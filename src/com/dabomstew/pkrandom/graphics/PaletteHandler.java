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
import com.dabomstew.pkrandom.pokemon.PokemonSet;

/**
 * A sub-handler for palette randomizing/modifications. Loading/writing palettes
 * should be done elsewhere, with implementations of this class only handling
 * changes done to said palettes.
 */
public abstract class PaletteHandler<T extends Pokemon> { // TODO: Think deeper about whether this should be a generic

	protected Random random;

	public PaletteHandler(Random random) {
		this.random = random;
	}

	public PaletteHandler(int seed) {
		this.random = new Random(seed);
	}

	public abstract void randomizePokemonPalettes(PokemonSet<T> pokemonSet,
												  boolean typeSanity, boolean evolutionSanity, boolean shinyFromNormal);

	protected void setShinyPaletteFromNormal(Pokemon pk) {
		pk.shinyPalette = pk.normalPalette.clone();
	}

}
