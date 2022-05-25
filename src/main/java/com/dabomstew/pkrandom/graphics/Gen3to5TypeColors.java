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
import java.util.Map;
import java.util.Random;

import com.dabomstew.pkrandom.pokemon.Type;

/**
 * Contains {@link TypeColor} constants for Gen 3-5 games, and methods for
 * accessing them.
 * <p>
 * The color values for all vanilla types (not including fairy) are taken from
 * Artemis251's Emerald Randomizer, and therefore by Artemis251. See the license
 * in {@link PalettePopulator}.
 */
public class Gen3to5TypeColors {

	private static final Map<Type, TypeColor[]> TYPE_COLORS = initTypeBaseColors();
	private static final Color DEFAULT_COLOR = new Color(0xC0C0C0);

	private static Map<Type, TypeColor[]> initTypeBaseColors() {
		Map<Type, TypeColor[]> map = new EnumMap<>(Type.class);
		// vanilla types
		TypeColor.putIntsAsTypeColors(map, Type.NORMAL, new int[] { 0xFF9DE7, 0xCB95FD, 0xC59A8B });
		TypeColor.putIntsAsTypeColors(map, Type.FIGHTING, new int[] { 0xA46A44, 0xC9656F, 0xEBA65A });
		TypeColor.putIntsAsTypeColors(map, Type.FLYING, new int[] { 0x77B7B7, 0x71A5FB, 0x8E5940 });
		TypeColor.putIntsAsTypeColors(map, Type.BUG, new int[] { 0x9FF04F, 0x95AD3F, 0xEAEA00 });
		TypeColor.putIntsAsTypeColors(map, Type.POISON, new int[] { 0xEC0DD7, 0x24FF24, 0x9787B8 });
		TypeColor.putIntsAsTypeColors(map, Type.ROCK, new int[] { 0x92B685, 0x6C788C, 0x928167 });
		TypeColor.putIntsAsTypeColors(map, Type.GROUND, new int[] { 0xD8AC7C, 0xA8A277, 0xD3A35A });
		TypeColor.putIntsAsTypeColors(map, Type.DARK, new int[] { 0x4A4A4A, 0x0000BB, 0x920303 });
		TypeColor.putIntsAsTypeColors(map, Type.STEEL, new int[] { 0xC0C0C0, 0xE4871F, 0xE25221 });
		TypeColor.putIntsAsTypeColors(map, Type.ICE, new int[] { 0x82FFE6, 0xC4D0D2, 0x7ABAFA });
		TypeColor.putIntsAsTypeColors(map, Type.WATER, new int[] { 0x4045FF, 0x00AAAA, 0x61D1A5 });
		TypeColor.putIntsAsTypeColors(map, Type.FIRE, new int[] { 0xFF822F, 0xEE0B0B, 0xFFD52B });
		TypeColor.putIntsAsTypeColors(map, Type.GRASS, new int[] { 0x00B700, 0x4E9131, 0xD6C132 });
		TypeColor.putIntsAsTypeColors(map, Type.PSYCHIC, new int[] { 0xC54DF2, 0xCE609F, 0x8230B8 });
		TypeColor.putIntsAsTypeColors(map, Type.GHOST, new int[] { 0x8729FA, 0x42448A, 0x42448A });
		TypeColor.putIntsAsTypeColors(map, Type.ELECTRIC, new int[] { 0xFBF259, 0x20FFFF, 0xFE1818 });
		TypeColor.putIntsAsTypeColors(map, Type.DRAGON, new int[] { 0xD83D41, 0x8C3535, 0x8C3535 });
		// hack types
		return map;
	}

	public static TypeColor getRandomTypeColor(Random random) {
		Type[] keys = TYPE_COLORS.keySet().toArray(new Type[0]);
		Type type = keys[random.nextInt(keys.length)];
		return getRandomTypeColor(type, random);
	}

	public static TypeColor getRandomTypeColor(Type type, Random random) {
		TypeColor[] typeColors = TYPE_COLORS.get(type);
		TypeColor color = typeColors == null ? new TypeColor(DEFAULT_COLOR, type)
				: typeColors[random.nextInt(typeColors.length)];
		return color;
	}

}
