package com.dabomstew.pkrandom.pokemon;

/*----------------------------------------------------------------------------*/
/*--  Type.java - represents a Pokemon or move type.                        --*/
/*--                                                                        --*/
/*--  Part of "Universal Pokemon Randomizer ZX" by the UPR-ZX team          --*/
/*--  Originally part of "Universal Pokemon Randomizer" by Dabomstew        --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2020.                                 --*/
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.dabomstew.pkrandom.RomFunctions;

public enum Type {

    NORMAL(1), FIGHTING(2), FLYING(3), GRASS(4), WATER(5), FIRE(6),
    ROCK(7), GROUND(8), PSYCHIC(9), BUG(10), DRAGON(11), ELECTRIC(12),
    GHOST(13), POISON(14), ICE(15), STEEL(16), DARK(17), FAIRY(18),
    GAS(true), WOOD(true), ABNORMAL(true), WIND(true), SOUND(true), LIGHT(true), TRI(true);

    public boolean isHackOnly;
    private final int index;

    Type() {
        this.isHackOnly = false;
        this.index = -1;
    }

    Type(int index) {
        this.isHackOnly = false;
        this.index = index;
    }

    Type(boolean isHackOnly) {
        this.isHackOnly = isHackOnly;
        this.index = -1;
    }

    public int toInt() {
        return this.index;
    }

    public static Type fromInt(int index) {
        for(Type type : Type.values()) {
            if(type.index == index) {
                return type;
            }
        }
        return null;
    }

    private static final List<Type> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
    private static final int SIZE = VALUES.size();

    public static final List<Type> GEN1 = Collections.unmodifiableList(Arrays.asList(values()).subList(0, ICE.ordinal()+1));
    public static final List<Type> GEN2THROUGH5 = Collections.unmodifiableList(Arrays.asList(values()).subList(0, DARK.ordinal()+1));
    public static final List<Type> GEN6PLUS = Collections.unmodifiableList(Arrays.asList(values()).subList(0, FAIRY.ordinal()+1));

    public static List<Type> getAllTypes(int generation) {
        switch (generation) {
            case 1:
                return GEN1;
            case 2:
            case 3:
            case 4:
            case 5:
                return GEN2THROUGH5;
            default:
                return GEN6PLUS;
        }
    }

    public static Type randomType(Random random) {
        return VALUES.get(random.nextInt(SIZE));
    }

    public String camelCase() {
        return RomFunctions.camelCase(this.toString());
    }

}
