package com.dabomstew.pkrandom.pokemon;

/*----------------------------------------------------------------------------*/
/*--  Evolution.java - represents an evolution between 2 Pokemon.           --*/
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

public class Evolution implements Comparable<Evolution> {

    private Pokemon from;
    private Pokemon to;
    private EvolutionType type;
    private int extraInfo;

    private boolean carryStats;

    // only relevant for Gen 7
    private int forme;
    private int level = 0; // in generations pre gen 7, the extrainfo has the level

    public Evolution(Pokemon from, Pokemon to, boolean carryStats, EvolutionType type, int extra) {
        this.from = from;
        this.to = to;
        this.carryStats = carryStats;
        this.type = type;
        this.extraInfo = extra;
    }

    /**
     * Returns the {@link Pokemon} this Evolution is "from".<br>
     * E.g. for the Evolution "Bulbasaur->Ivysaur" this would return Bulbasaur.
     */
    public Pokemon getFrom() {
        return from;
    }

    public void setFrom(Pokemon from) {
        this.from = from;
    }

    /**
     * Returns the {@link Pokemon} this Evolution is "from".<br>
     * E.g. for the Evolution "Bulbasaur->Ivysaur" this would return Ivysaur.
     */
    public Pokemon getTo() {
        return to;
    }

    public void setTo(Pokemon to) {
        this.to = to;
    }

    public EvolutionType getType() {
        return type;
    }

    public void setType(EvolutionType type) {
        this.type = type;
    }

    public int getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(int extraInfo) {
        this.extraInfo = extraInfo;
    }

    public boolean isCarryStats() {
        return carryStats;
    }

    public void setCarryStats(boolean carryStats) {
        this.carryStats = carryStats;
    }

    public int getForme() {
        return forme;
    }

    public void setForme(int forme) {
        this.forme = forme;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    // TODO: are the evolutions hashed somewhere????
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + from.getNumber();
        result = prime * result + to.getNumber();
        result = prime * result + type.ordinal();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Evolution other = (Evolution) obj;
        return from == other.from && to == other.to && type == other.type;
    }

    @Override
    public int compareTo(Evolution o) {
        if (this.from.getNumber() < o.from.getNumber()) {
            return -1;
        } else if (this.from.getNumber() > o.from.getNumber()) {
            return 1;
        } else if (this.to.getNumber() < o.to.getNumber()) {
            return -1;
        } else if (this.to.getNumber() > o.to.getNumber()) {
            return 1;
        } else return Integer.compare(this.type.ordinal(), o.type.ordinal());
    }

    @Override
    public String toString() {
        return forme == 0 && level == 0 ?
                String.format("(%s->%s, %s, extraInfo:%d, carryStats:%b)", from.fullName(), to.fullName(),
                        type, extraInfo, carryStats) :
                String.format("(%s->%s, %s, extraInfo:%d, carryStats:%b, forme:%d, level:%d)", from.fullName(), to.fullName(),
                        type, extraInfo, carryStats, forme, level);
    }
}
