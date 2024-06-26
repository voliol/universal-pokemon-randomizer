package com.dabomstew.pkrandom.pokemon;

/*----------------------------------------------------------------------------*/
/*--  Encounter.java - contains one wild Pokemon slot                       --*/
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

public class Encounter {

    private int level;
    private int maxLevel;
    private Pokemon pokemon;
    private int formeNumber;

    // Used only for Gen 7's SOS mechanic
    private boolean isSOS;
    private SOSType sosType;

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public Pokemon getPokemon() {
        return pokemon;
    }

    public void setPokemon(Pokemon pokemon) {
        this.pokemon = pokemon;
    }

    public int getFormeNumber() {
        return formeNumber;
    }

    public void setFormeNumber(int formeNumber) {
        this.formeNumber = formeNumber;
    }

    public boolean isSOS() {
        return isSOS;
    }

    public void setSOS(boolean SOS) {
        isSOS = SOS;
    }

    public SOSType getSosType() {
        return sosType;
    }

    public void setSosType(SOSType sosType) {
        this.sosType = sosType;
    }

    @Override
    public String toString() {
        if (pokemon == null) {
            return "ERROR";
        }
        if (maxLevel == 0) {
            return pokemon.getName() + " Lv" + level;
        } else {
            return pokemon.getName() + " Lvs " + level + "-" + maxLevel;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Encounter otherEnc) {
            return level == otherEnc.level && maxLevel == otherEnc.maxLevel && pokemon.equals(otherEnc.pokemon)
                    && formeNumber == otherEnc.formeNumber && isSOS == otherEnc.isSOS && sosType == otherEnc.sosType;
        }
        return false;
    }

}
