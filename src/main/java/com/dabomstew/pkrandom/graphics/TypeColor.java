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

import java.util.Map;

import com.dabomstew.pkrandom.pokemon.Type;

/**
 * A {@link Color} with an associated {@link Type}.
 */
public class TypeColor extends Color {

	public static void putIntsAsTypeColors(Map<Type, TypeColor[]> map, Type type, int[] ints) {
		TypeColor[] typeColors = new TypeColor[ints.length];
		for (int i = 0; i < typeColors.length; i++) {
			typeColors[i] = new TypeColor(ints[i], type);
		}
		map.put(type, typeColors);
	}

	private Type type;

	public TypeColor(int hex, Type type) {
		super(hex);
		this.type = type;
	}

	public TypeColor(Color untyped, Type type) {
		super(untyped.getComp(0), untyped.getComp(1), untyped.getComp(2));
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return type + "-" + super.toString();
	}

}
