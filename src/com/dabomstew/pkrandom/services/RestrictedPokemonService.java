package com.dabomstew.pkrandom.services;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.constants.Species;
import com.dabomstew.pkrandom.pokemon.GenRestrictions;
import com.dabomstew.pkrandom.pokemon.MegaEvolution;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.PokemonSet;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A service for restricted Pokemon. After setting restrictions with {@link #setRestrictions(Settings)},
 * you can access a number of <i>unmodifiable</i> {@link PokemonSet}s, following those restrictions.<br>
 * When randomizing, you generally want to use these sets,
 * rather than anything provided directly by the {@link RomHandler}, like {@link RomHandler#getPokemon()} or
 * {@link RomHandler#getPokemonSetInclFormes()}.
 * <br><br>
 * This class also provides {@link #randomPokemon(Random)}, to get a random Pokemon from all allowed ones.
 * To get a random Pokemon from the other sets, use {@link PokemonSet#getRandom(Random)}.
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

    public PokemonSet<Pokemon> getPokemon(boolean noLegendaries, boolean allowAltFormes, boolean allowCosmeticFormes) {
        PokemonSet<Pokemon> allowedPokes = new PokemonSet<>();
        allowedPokes.addAll(noLegendaries ? getNonLegendaries(allowAltFormes) : getAll(allowAltFormes));
        if (allowAltFormes && !allowCosmeticFormes) {
            allowedPokes.removeIf(Pokemon::isCosmeticForme);
        }
        return PokemonSet.unmodifiable(allowedPokes);
    }

    /**
     * Returns a random non-alt forme Pokemon.
     */
    public Pokemon randomPokemon(Random random) {
        return getAll(false).getRandom(random);
    }

    /**
     * Returns an unmodifiable {@link PokemonSet} containing all Pokemon that follow the restrictions.
     */
    public PokemonSet<Pokemon> getAll(boolean includeAltFormes) {
        if (!restrictionsSet) {
            throw new IllegalStateException("Restrictions not set.");
        }
        return includeAltFormes ? allInclAltFormes : all;
    }

    /**
     * Returns an unmodifiable {@link PokemonSet} containing all non-legendary Pokemon that follow the restrictions.
     */
    public PokemonSet<Pokemon> getNonLegendaries(boolean includeAltFormes) {
        if (!restrictionsSet) {
            throw new IllegalStateException("Restrictions not set.");
        }
        return includeAltFormes ? nonLegendariesInclAltFormes : nonLegendaries;
    }

    /**
     * Returns an unmodifiable {@link PokemonSet} containing all legendary Pokemon that follow the restrictions.
     */
    public PokemonSet<Pokemon> getLegendaries(boolean includeAltFormes) {
        if (!restrictionsSet) {
            throw new IllegalStateException("Restrictions not set.");
        }
        return includeAltFormes ? legendariesInclAltFormes : legendaries;
    }

    /**
     * Returns an unmodifiable {@link PokemonSet} containing all ultra beasts that follow the restrictions.
     * Does NOT contain the legendary ultra beasts.
     */
    public PokemonSet<Pokemon> getUltrabeasts(boolean includeAltFormes) {
        if (!restrictionsSet) {
            throw new IllegalStateException("Restrictions not set.");
        }
        return includeAltFormes ? ultraBeastsInclAltFormes : ultraBeasts;
    }

    /**
     * Returns an unmodifiable {@link Set} containing all {@link MegaEvolution}s that follow the restrictions.
     */
    public Set<MegaEvolution> getMegaEvolutions() {
        if (!restrictionsSet) {
            throw new IllegalStateException("Restrictions not set.");
        }
        return megaEvolutions;
    }

    public PokemonSet<Pokemon> getAbilityDependentFormes() {
        PokemonSet<Pokemon> abilityDependentFormes = new PokemonSet<>();
        for (Pokemon pk : allInclAltFormes) {
            if (pk.getBaseForme() != null) {
                if (pk.getBaseNumber() == Species.castform) {
                    // All alternate Castform formes
                    abilityDependentFormes.add(pk);
                } else if (pk.getBaseNumber() == Species.darmanitan && pk.getFormeNumber() == 1) {
                    // Darmanitan-Z
                    abilityDependentFormes.add(pk);
                } else if (pk.getBaseNumber() == Species.aegislash) {
                    // Aegislash-B
                    abilityDependentFormes.add(pk);
                } else if (pk.getBaseNumber() == Species.wishiwashi) {
                    // Wishiwashi-S
                    abilityDependentFormes.add(pk);
                }
            }
        }
        return abilityDependentFormes;
    }

    public PokemonSet<Pokemon> getBannedFormesForPlayerPokemon() {
        PokemonSet<Pokemon> bannedFormes = new PokemonSet<>();
        for (Pokemon pk : allInclAltFormes) {
            if (pk.getBaseForme() != null) {
                if (pk.getBaseNumber() == Species.giratina) {
                    // Giratina-O is banned because it reverts back to Altered Forme if
                    // equipped with any item that isn't the Griseous Orb.
                    bannedFormes.add(pk);
                } else if (pk.getBaseNumber() == Species.shaymin) {
                    // Shaymin-S is banned because it reverts back to its original forme
                    // under a variety of circumstances, and can only be changed back
                    // with the Gracidea.
                    bannedFormes.add(pk);
                }
            }
        }
        return bannedFormes;
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
            allInclAltFormes = PokemonSet.unmodifiable(allInclAltFormesFromRestrictions(restrictions));
            megaEvolutions = romHandler.getMegaEvolutions().stream()
                    .filter(mevo -> allInclAltFormes.contains(mevo.to))
                    .collect(Collectors.toUnmodifiableSet());
        } else {
            allInclAltFormes = PokemonSet.unmodifiable(romHandler.getPokemonSetInclFormes());
            megaEvolutions = Set.copyOf(romHandler.getMegaEvolutions());
        }

        nonLegendariesInclAltFormes = PokemonSet.unmodifiable(allInclAltFormes.filter(pk -> !pk.isLegendary()));
        legendariesInclAltFormes = PokemonSet.unmodifiable(allInclAltFormes.filter(Pokemon::isLegendary));
        ultraBeastsInclAltFormes = PokemonSet.unmodifiable(allInclAltFormes.filter(Pokemon::isUltraBeast));
        PokemonSet<Pokemon> altFormes = romHandler.getAltFormes();
        all = PokemonSet.unmodifiable(allInclAltFormes.filter(pk -> !altFormes.contains(pk)));
        nonLegendaries = PokemonSet.unmodifiable(nonLegendariesInclAltFormes.filter(pk -> !altFormes.contains(pk)));
        legendaries = PokemonSet.unmodifiable(legendariesInclAltFormes.filter(pk -> !altFormes.contains(pk)));
        ultraBeasts = PokemonSet.unmodifiable(ultraBeastsInclAltFormes.filter(pk -> !altFormes.contains(pk)));
    }

    private PokemonSet<Pokemon> allInclAltFormesFromRestrictions(GenRestrictions restrictions) {
        PokemonSet<Pokemon> allInclAltFormes = new PokemonSet<>();
        PokemonSet<Pokemon> allNonRestricted = romHandler.getPokemonSetInclFormes();

        if (restrictions.allow_gen1) {
            addFromGen(allInclAltFormes, allNonRestricted, 1);
        }
        if (restrictions.allow_gen2) {
            addFromGen(allInclAltFormes, allNonRestricted, 2);
        }
        if (restrictions.allow_gen3) {
            addFromGen(allInclAltFormes, allNonRestricted, 3);
        }
        if (restrictions.allow_gen4) {
            addFromGen(allInclAltFormes, allNonRestricted, 4);
        }
        if (restrictions.allow_gen5) {
            addFromGen(allInclAltFormes, allNonRestricted, 5);
        }
        if (restrictions.allow_gen6) {
            addFromGen(allInclAltFormes, allNonRestricted, 6);
        }
        if (restrictions.allow_gen7) {
            addFromGen(allInclAltFormes, allNonRestricted, 7);
        }

        // If the user specified it, add all the evolutionary relatives for everything in the mainPokemonList
        if (restrictions.allow_evolutionary_relatives) {
            allInclAltFormes.addEvolutionaryRelatives();
        }

        return allInclAltFormes;
    }

    private static void addFromGen(PokemonSet<Pokemon> allInclAltFormes, PokemonSet<Pokemon> allNonRestricted, int gen) {
        allInclAltFormes.addAll(allNonRestricted.filter(pk -> {
            Pokemon baseForme = pk.getBaseForme() == null ? pk : pk.getBaseForme();
            return baseForme.getGeneration() == gen;
        }));
    }
}
