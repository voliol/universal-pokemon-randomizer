package com.dabomstew.pkrandom.pokemon;

/*----------------------------------------------------------------------------*/
/*--  EncounterArea.java - contains a group of wild Pokemon                 --*/
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

import java.util.*;

public class EncounterArea extends ArrayList<Encounter> {

    private int rate;
    private final Set<Pokemon> bannedPokemon = new HashSet<>();
    private String displayName;
    private int offset;
    private String locationTag = "";

    public EncounterArea() {
    }

    public EncounterArea(Collection<? extends Encounter> collection) {
        super(collection);
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    /**
     * Returns an unmodifiable set of the {@link Pokemon} which should NOT have {@link Encounter}s in this area.
     */
    public Set<Pokemon> getBannedPokemon() {
        return Collections.unmodifiableSet(bannedPokemon);
    }

    public void banPokemon(Pokemon toBan) {
        bannedPokemon.add(toBan);
    }

    public void banAllPokemon(Collection<? extends Pokemon> toBan) {
        bannedPokemon.addAll(toBan);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getLocationTag() {
        return locationTag;
    }

    public void setLocationTag(String locationTag) {
        this.locationTag = locationTag;
    }

    @Override
    public String toString() {
        return "Encounters [Name = " + displayName + ", Rate = " + rate + ", Encounters = " + super.toString() + "]";
    }
}
