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

    //The index of the map this area is contained in, as determined by the RomHandler.
    //Note that not all RomHandlers currently set this variable,
    //and of those that do, not all use it in the same way.
    private int mapIndex;

    private String locationTag;

    public enum EncounterType { WALKING, //e.g. grass, cave, seaweed, Horde encounters
        SURFING, FISHING, //obvious
        INTERACT, //e.g. headbutt trees, Rock Smash
        AMBUSH, //e.g. flying pokemon, shaking trees
        SPECIAL} //e.g. Poke Radar, DexNav Foreign encounter, SOS encounters

    //The type of encounter this area is, as determined by the RomHandler.
    //Note that currently, only ORAS sets this variable.
    private EncounterType encounterType;

    private boolean postGame;
    // In some games, areas have both main game and post game encounters, following each other,
    // e.g. the fishing encounters in Gen 2. This attribute indicates the index for where the post game encounters
    // start.
    private int partiallyPostGameCutoff = -1;

    // For areas that work like/is the Trophy Garden rotating Pokemon in DPPt, where the game
    // softlocks or otherwise has logical issues if all Encounters' species are the same.
    private boolean forceMultipleSpecies;

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

    public int getMapIndex() {
        return mapIndex;
    }

    public void setMapIndex(int mapIndex) {
        this.mapIndex = mapIndex;
    }

    public String getLocationTag() {
        return locationTag;
    }

    public void setLocationTag(String locationTag) {
        this.locationTag = locationTag;
    }

    public EncounterType getEncounterType() {
        return encounterType;
    }

    public void setEncounterType(EncounterType type) {
        this.encounterType = type;
    }

    public boolean isPostGame() {
        return postGame;
    }

    public void setPostGame(boolean postGame) {
        this.postGame = postGame;
    }

    public boolean isPartiallyPostGame() {
        return partiallyPostGameCutoff != -1;
    }

    public int getPartiallyPostGameCutoff() {
        return partiallyPostGameCutoff;
    }

    public void setPartiallyPostGameCutoff(int partiallyPostGameCutoff) {
        this.partiallyPostGameCutoff = partiallyPostGameCutoff;
    }

    public void setForceMultipleSpecies(boolean forceMultipleSpecies) {
        this.forceMultipleSpecies = forceMultipleSpecies;
    }

    public boolean isForceMultipleSpecies() {
        return forceMultipleSpecies;
    }

    @Override
    public String toString() {
        return "Encounters [Name = " + displayName + ", Rate = " + rate + ", Encounters = " + super.toString() + "]";
    }

    //Helper functions

    /**
     * Creates a PokemonSet with all Pokemon that can be found in this area.
     * @return A PokemonSet containing all Pokemon that can be encountered in this area.
     */
    public PokemonSet getPokemonInArea() {
        PokemonSet pokemonSet = new PokemonSet();
        for (Encounter enc : this) {
            pokemonSet.add(enc.getPokemon());
        }
        return pokemonSet;
    }
}
