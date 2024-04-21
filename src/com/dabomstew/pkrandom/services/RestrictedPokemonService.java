package com.dabomstew.pkrandom.services;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.constants.*;
import com.dabomstew.pkrandom.pokemon.GenRestrictions;
import com.dabomstew.pkrandom.pokemon.MegaEvolution;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.PokemonSet;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * To get a random Pokemon from these sets, use {@link PokemonSet#getRandom(Random)}.
 */
public class RestrictedPokemonService {

    private final RomHandler romHandler;

    private boolean restrictionsSet;

    private PokemonSet<Pokemon> all;
    private PokemonSet<Pokemon> allInclAltFormes;
    private PokemonSet<Pokemon> nonLegendaries;
    private PokemonSet<Pokemon> nonLegendariesInclAltFormes;
    private PokemonSet<Pokemon> legendaries;
    private PokemonSet<Pokemon> legendariesInclAltFormes;
    private PokemonSet<Pokemon> ultraBeasts;
    private PokemonSet<Pokemon> ultraBeastsInclAltFormes;
    private Set<MegaEvolution> megaEvolutions;


    public RestrictedPokemonService(RomHandler romHandler) {
        this.romHandler = romHandler;
    }

    /**
     * Returns an unmodifiable {@link PokemonSet} containing all restricted Pokemon.
     */
    public PokemonSet<Pokemon> getAll(boolean includeAltFormes) {
        if (!restrictionsSet) {
            throw new IllegalStateException("Restrictions not set.");
        }
        return includeAltFormes ? allInclAltFormes : all;
    }

    /**
     * Returns an unmodifiable {@link PokemonSet} containing all non-legendary restricted Pokemon.
     */
    public PokemonSet<Pokemon> getNonLegendaries(boolean includeAltFormes) {
        if (!restrictionsSet) {
            throw new IllegalStateException("Restrictions not set.");
        }
        return includeAltFormes ? nonLegendariesInclAltFormes : nonLegendaries;
    }

    /**
     * Returns an unmodifiable {@link PokemonSet} containing all legendary restricted Pokemon.
     */
    public PokemonSet<Pokemon> getLegendaries(boolean includeAltFormes) {
        if (!restrictionsSet) {
            throw new IllegalStateException("Restrictions not set.");
        }
        return includeAltFormes ? legendariesInclAltFormes : legendaries;
    }

    /**
     * Returns an unmodifiable {@link PokemonSet} containing all restricted ultra beasts.
     * Does NOT contain the legendary ultra beasts.
     */
    public PokemonSet<Pokemon> getUltrabeasts(boolean includeAltFormes) {
        if (!restrictionsSet) {
            throw new IllegalStateException("Restrictions not set.");
        }
        return includeAltFormes ? ultraBeastsInclAltFormes : ultraBeasts;
    }

    public Set<MegaEvolution> getMegaEvolutions() {
        if (!restrictionsSet) {
            throw new IllegalStateException("Restrictions not set.");
        }
        return null;
    }

    public void setRestrictions(Settings settings) {
        GenRestrictions restrictions = null;
        if (settings != null) {
            restrictions = settings.getCurrentRestrictions();

            // restrictions should already be null if "Limit Pokemon" is disabled, but this is a safeguard
            if (!settings.isLimitPokemon()) {
                restrictions = null;
            }
        }

        restrictionsSet = true;

        if (restrictions != null) {
            allInclAltFormes = allInclAltFormesFromRestrictions(restrictions);
            megaEvolutions = romHandler.getMegaEvolutions().stream()
                    .filter(mevo -> allInclAltFormes.contains(mevo.to))
                    .collect(Collectors.toSet());
        } else {
            allInclAltFormes = new PokemonSet<>(romHandler.getPokemonSetInclFormes());
            megaEvolutions = new HashSet<>(romHandler.getMegaEvolutions());
        }


        nonLegendariesInclAltFormes = allInclAltFormes.filter(pk -> !pk.isLegendary());
        legendariesInclAltFormes = allInclAltFormes.filter(Pokemon::isLegendary);
        ultraBeastsInclAltFormes = allInclAltFormes.filter(Pokemon::isUltraBeast);
        PokemonSet<Pokemon> altFormes = romHandler.getAltFormes();
        all = allInclAltFormes.filter(pk -> !altFormes.contains(pk));
        legendaries = legendariesInclAltFormes.filter(pk -> !altFormes.contains(pk));
        ultraBeasts = ultraBeastsInclAltFormes.filter(pk -> !altFormes.contains(pk));
    }

    private PokemonSet<Pokemon> allInclAltFormesFromRestrictions(GenRestrictions restrictions) {
        PokemonSet<Pokemon> allInclAltFormes = new PokemonSet<>();
        PokemonSet<Pokemon> allNonRestricted = romHandler.getPokemonSetInclFormes();
        int pokemonCount = romHandler.getPokemon().size();

        // TODO: instead of having the generations be hard-coded here, let each RomHandler assign a
        //  generation to each of the Pokemon

        if (restrictions.allow_gen1) {
            allInclAltFormes.addAll(allNonRestricted.filterFromBaseNumberRange(Species.bulbasaur, Species.mew));
        }

        if (restrictions.allow_gen2 && pokemonCount > Gen2Constants.pokemonCount) {
            allInclAltFormes.addAll(allNonRestricted.filterFromBaseNumberRange(Species.chikorita, Species.celebi));
        }

        if (restrictions.allow_gen3 && pokemonCount > Gen3Constants.pokemonCount) {
            allInclAltFormes.addAll(allNonRestricted.filterFromBaseNumberRange(Species.treecko, Species.deoxys));
        }

        if (restrictions.allow_gen4 && pokemonCount > Gen4Constants.pokemonCount) {
            allInclAltFormes.addAll(allNonRestricted.filterFromBaseNumberRange(Species.turtwig, Species.arceus));
        }

        if (restrictions.allow_gen5 && pokemonCount > Gen5Constants.pokemonCount) {
            allInclAltFormes.addAll(allNonRestricted.filterFromBaseNumberRange(Species.victini, Species.genesect));
        }

        if (restrictions.allow_gen6 && pokemonCount > Gen6Constants.pokemonCount) {
            allInclAltFormes.addAll(allNonRestricted.filterFromBaseNumberRange(Species.chespin, Species.volcanion));
        }

        int maxGen7SpeciesID = romHandler.isSM ? Species.marshadow : Species.zeraora;
        if (restrictions.allow_gen7 && pokemonCount > maxGen7SpeciesID) {
            allInclAltFormes.addAll(allNonRestricted.filterFromBaseNumberRange(Species.rowlet, maxGen7SpeciesID));
        }

        // If the user specified it, add all the evolutionary relatives for everything in the mainPokemonList
        if (restrictions.allow_evolutionary_relatives) {
            allInclAltFormes.addEvolutionaryRelatives();
        }

        return allInclAltFormes;
    }
}
