package com.dabomstew.pkrandom.pokemon;

import java.util.Arrays;
import java.util.List;

/*----------------------------------------------------------------------------*/
/*--  TypeRelationship.java - represents the relationship between an        --*/
/*--                          attacking and defending Type.                 --*/
/*--                                                                        --*/
/*--  Part of "Universal Pokemon Randomizer ZX" by the UPR-ZX team          --*/
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

public class TypeRelationship {
        public Type attacker;
        public Type defender;
        public Effectiveness effectiveness;

        public TypeRelationship(Type attacker, Type defender, Effectiveness effectiveness) {
                this.attacker = attacker;
                this.defender = defender;
                this.effectiveness = effectiveness;
        }

        public enum Effectiveness {
                ZERO, HALF, NEUTRAL, DOUBLE;
        }

        public static final List<Type> STRONG_AGAINST_NORMAL = Arrays.asList(Type.FIGHTING);
        public static final List<Type> RESISTANT_TO_NORMAL = Arrays.asList(Type.ROCK, Type.STEEL);
        public static final List<Type> IMMUNE_TO_NORMAL = Arrays.asList(Type.GHOST);
        public static final List<Type> STRONG_AGAINST_FIGHTING =
                        Arrays.asList(Type.FLYING, Type.PSYCHIC);
        public static final List<Type> RESISTANT_TO_FIGHTING =
                        Arrays.asList(Type.POISON, Type.FLYING, Type.PSYCHIC, Type.BUG);
        public static final List<Type> IMMUNE_TO_FIGHTING = Arrays.asList(Type.GHOST);
        public static final List<Type> STRONG_AGAINST_FLYING =
                        Arrays.asList(Type.ELECTRIC, Type.ICE, Type.ROCK);
        public static final List<Type> RESISTANT_TO_FLYING =
                        Arrays.asList(Type.ELECTRIC, Type.ROCK, Type.STEEL);
        public static final List<Type> STRONG_AGAINST_GRASS =
                        Arrays.asList(Type.FIRE, Type.ICE, Type.POISON, Type.FLYING, Type.BUG);
        public static final List<Type> RESISTANT_TO_GRASS = Arrays.asList(Type.FIRE, Type.GRASS,
                        Type.POISON, Type.FLYING, Type.BUG, Type.DRAGON, Type.STEEL);
        public static final List<Type> STRONG_AGAINST_WATER =
                        Arrays.asList(Type.ELECTRIC, Type.GRASS);
        public static final List<Type> RESISTANT_TO_WATER =
                        Arrays.asList(Type.WATER, Type.GRASS, Type.DRAGON);
        public static final List<Type> STRONG_AGAINST_FIRE =
                        Arrays.asList(Type.WATER, Type.GROUND, Type.ROCK);
        public static final List<Type> RESISTANT_TO_FIRE =
                        Arrays.asList(Type.FIRE, Type.WATER, Type.ROCK, Type.DRAGON);
        public static final List<Type> STRONG_AGAINST_ROCK = Arrays.asList(Type.WATER, Type.GRASS,
                        Type.FIGHTING, Type.GROUND, Type.STEEL);
        public static final List<Type> RESISTANT_TO_ROCK =
                        Arrays.asList(Type.FIGHTING, Type.GROUND, Type.STEEL);
        public static final List<Type> STRONG_AGAINST_GROUND =
                        Arrays.asList(Type.WATER, Type.GRASS, Type.ICE);
        public static final List<Type> RESISTANT_TO_GROUND = Arrays.asList(Type.GRASS, Type.BUG);
        public static final List<Type> IMMUNE_TO_GROUND = Arrays.asList(Type.FLYING);
        public static final List<Type> STRONG_AGAINST_PSYCHIC =
                        Arrays.asList(Type.BUG, Type.GHOST, Type.DARK);
        public static final List<Type> RESISTANT_TO_PSYCHIC =
                        Arrays.asList(Type.PSYCHIC, Type.STEEL);
        public static final List<Type> IMMUNE_TO_PSYCHIC = Arrays.asList(Type.DARK);
        public static final List<Type> STRONG_AGAINST_BUG =
                        Arrays.asList(Type.FIRE, Type.FLYING, Type.ROCK);
        public static final List<Type> RESISTANT_TO_BUG = Arrays.asList(Type.FIRE, Type.FIGHTING,
                        Type.POISON, Type.FLYING, Type.GHOST, Type.STEEL);
        public static final List<Type> STRONG_AGAINST_DRAGON = Arrays.asList(Type.ICE, Type.DRAGON);
        public static final List<Type> RESISTANT_TO_DRAGON = Arrays.asList(Type.STEEL);
        public static final List<Type> STRONG_AGAINST_ELECTRIC = Arrays.asList(Type.GROUND);
        public static final List<Type> RESISTANT_TO_ELECTRIC =
                        Arrays.asList(Type.ELECTRIC, Type.GRASS, Type.DRAGON);
        public static final List<Type> IMMUNE_TO_ELECTRIC = Arrays.asList(Type.GROUND);
        public static final List<Type> STRONG_AGAINST_GHOST = Arrays.asList(Type.GHOST, Type.DARK);
        public static final List<Type> RESISTANT_TO_GHOST = Arrays.asList(Type.DARK);
        public static final List<Type> IMMUNE_TO_GHOST = Arrays.asList(Type.NORMAL);
        public static final List<Type> STRONG_AGAINST_POISON =
                        Arrays.asList(Type.GROUND, Type.PSYCHIC);
        public static final List<Type> RESISTANT_TO_POISON =
                        Arrays.asList(Type.POISON, Type.GROUND, Type.ROCK, Type.GHOST);
        public static final List<Type> IMMUNE_TO_POISON = Arrays.asList(Type.STEEL);
        public static final List<Type> STRONG_AGAINST_ICE =
                        Arrays.asList(Type.FIRE, Type.FIGHTING, Type.ROCK, Type.STEEL);
        public static final List<Type> RESISTANT_TO_ICE =
                        Arrays.asList(Type.FIRE, Type.WATER, Type.ICE, Type.STEEL);
        public static final List<Type> STRONG_AGAINST_STEEL =
                        Arrays.asList(Type.FIRE, Type.FIGHTING, Type.GROUND);
        public static final List<Type> RESISTANT_TO_STEEL =
                        Arrays.asList(Type.FIRE, Type.WATER, Type.ELECTRIC, Type.STEEL);
        public static final List<Type> STRONG_AGAINST_DARK = Arrays.asList(Type.FIGHTING, Type.BUG);
        public static final List<Type> RESISTANT_TO_DARK = Arrays.asList(Type.FIGHTING, Type.DARK);
}
