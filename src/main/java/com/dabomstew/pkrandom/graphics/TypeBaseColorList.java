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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Type;

// TODO: is it alright to have a "list" that doesn't implement List?
/**
 * A list of {@link TypeColor}s based on a Pokémon's types. Which TypeColors are
 * used, and their order, can be carried up evolutions using a
 * {@link #TypeBaseColorList(Pokemon, TypeBaseColorList, boolean, Random)
 * constructor taking an existing TypeBaseColorList}.
 * <p>
 * The TypeColor constants used are those from {@link Gen3to5TypeColors}.
 */
public class TypeBaseColorList {

	private static final double SAME_TYPES_SWAP_COLORS_CHANCE = 0.3;

	private Random random;

	private Pokemon pokemon;
	private List<TypeColor> typeBaseColors;
	private TypeBaseColorList prevo;
	private BaseColorMap baseColorMap;

	public TypeBaseColorList(Pokemon pokemon, boolean typeSanity, Random random) {
		this(pokemon, null, typeSanity, random);
	}

	public TypeBaseColorList(Pokemon pokemon, TypeBaseColorList prevo, boolean typeSanity, Random random) {
		this.pokemon = pokemon;
		this.prevo = prevo;
		this.random = random;
		// evo lines share a BaseColorMap
		this.baseColorMap = prevo == null ? new BaseColorMap(random) : prevo.baseColorMap;
		generateBaseColors(typeSanity);
	}

	private TypeColor get(int i) {
		return typeBaseColors.get(i);
	}

	public Color getBaseColor(int i) {
		return baseColorMap.getBaseColor(get(i));
	}

	public LightDarkMode getLightDarkMode(int i) {
		return baseColorMap.getLightDarkMode(get(i));
	}

	private void generateBaseColors(boolean typeSanity) {
		if (prevo == null) {
			generateTypeBaseColorsBasic(typeSanity);
		} else {
			generateTypeBaseColorsFromPrevo(typeSanity);
		}
	}

	private void generateTypeBaseColorsBasic(boolean typeSanity) {
		// woops, I forgot about the needs of this class when remaking TypeBaseColor,
		// TODO
		typeBaseColors = Gen3to5TypeColors.getAllTypeColors();
		Collections.shuffle(typeBaseColors, random);

		if (typeSanity) {
			moveTypeColorsToStart();
		}
	}

	private void generateTypeBaseColorsFromPrevo(boolean typeSanity) {
		typeBaseColors = new ArrayList<>(prevo.typeBaseColors);

		if (!typeSanity) {
			if (random.nextDouble() < SAME_TYPES_SWAP_COLORS_CHANCE) {
				swapColors(0, 1);
			}
		}

		else {

			if (hasSameTypesAsPrevo()) {
				if (random.nextDouble() < SAME_TYPES_SWAP_COLORS_CHANCE) {
					swapColors(0, 1);
				}
			} else {

				if (!hasSamePrimaryTypeAsPrevo()) {
					moveFirstColorOfType(0, pokemon.getPrimaryType());
				}

				if (!hasSameSecondaryTypeAsPrevo()) {
					Type moveType = pokemon.hasSecondaryType() ? pokemon.getSecondaryType() : pokemon.getPrimaryType();
					moveFirstColorOfType(1, moveType);
				}
			}
		}
	}

	private void moveTypeColorsToStart() {
		moveFirstColorOfType(0, pokemon.getPrimaryType());

		Type moveType = pokemon.hasSecondaryType() ? pokemon.getSecondaryType() : pokemon.getPrimaryType();
		moveFirstColorOfType(1, moveType);

		moveFirstColorOfType(2, pokemon.getPrimaryType());

		if (pokemon.hasSecondaryType()) {
			moveFirstColorOfType(3, pokemon.getSecondaryType());
		}
	}

	private void swapColors(int a, int b) {
		Collections.swap(typeBaseColors, a, b);
	}

	private void moveFirstColorOfType(int insertIndex, Type type) {
		int colorOfTypeIndex = findColorOfType(insertIndex, type);
		if (colorOfTypeIndex == -1) {
			// TODO: more descriptive error message? (or change this method to auto-fix)
			throw new RuntimeException();
		}
		TypeColor color = typeBaseColors.get(colorOfTypeIndex);
		typeBaseColors.remove(colorOfTypeIndex);
		typeBaseColors.add(insertIndex, color);
	}

	private int findColorOfType(int start, Type type) {
		for (int i = start; i < typeBaseColors.size(); i++) {
			if (typeBaseColors.get(i).getType() == type) {
				return i;
			}
		}
		return -1;
	}

	private boolean hasSameTypesAsPrevo() {
		return hasSamePrimaryTypeAsPrevo() && hasSameSecondaryTypeAsPrevo();
	}

	private boolean hasSamePrimaryTypeAsPrevo() {
		return pokemon.getPrimaryType() == prevo.pokemon.getPrimaryType();
	}

	private boolean hasSameSecondaryTypeAsPrevo() {
		return pokemon.getSecondaryType() == prevo.pokemon.getSecondaryType();
	}

	@Override
	public String toString() {
		return String.format("%s prevoPk=%s %s", pokemon.getName(), (prevo == null ? null : prevo.pokemon.getName()),
				typeBaseColors);
	}

}
