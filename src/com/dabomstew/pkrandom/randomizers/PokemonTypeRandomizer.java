package com.dabomstew.pkrandom.randomizers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.CopyUpEvolutionsHelper;
import com.dabomstew.pkrandom.pokemon.MegaEvolution;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.PokemonSet;
import com.dabomstew.pkrandom.romhandlers.RomHandler;
import com.dabomstew.pkrandom.services.TypeService;

import java.util.List;
import java.util.Random;

public class PokemonTypeRandomizer {

    private final RomHandler romHandler;
    private final TypeService typeService;
    private final Settings settings;
    private final Random random;

    private final CopyUpEvolutionsHelper<Pokemon> copyUpEvolutionsHelper;

    public PokemonTypeRandomizer(RomHandler romHandler, Settings settings, Random random) {
        this.romHandler = romHandler;
        this.typeService = romHandler.getTypeService();
        this.settings = settings;
        this.random = random;

        this.copyUpEvolutionsHelper = new CopyUpEvolutionsHelper<>(romHandler::getPokemonSet);
    }

    public void randomizePokemonTypes() {
        boolean evolutionSanity = settings.getTypesMod() == Settings.TypesMod.RANDOM_FOLLOW_EVOLUTIONS;
        boolean megaEvolutionSanity = settings.isTypesFollowMegaEvolutions();
        boolean dualTypeOnly = settings.isDualTypeOnly();

        PokemonSet<Pokemon> allPokes = romHandler.getPokemonSetInclFormes();
        copyUpEvolutionsHelper.apply(evolutionSanity, false, pk -> {
            // Step 1: Basic or Excluded From Copying Pokemon
            // A Basic/EFC pokemon has a 35% chance of a second type if
            // it has an evolution that copies type/stats, a 50% chance
            // otherwise
            pk.setPrimaryType(typeService.randomType(random));
            pk.setSecondaryType(null);
            if (pk.getEvolutionsFrom().size() == 1 && pk.getEvolutionsFrom().get(0).isCarryStats()) {
                assignRandomSecondaryType(pk, 0.35, dualTypeOnly);
            } else {
                assignRandomSecondaryType(pk, 0.5, dualTypeOnly);
            }
        }, (evFrom, evTo, toMonIsFinalEvo) -> {
            evTo.setPrimaryType(evFrom.getPrimaryType());
            evTo.setSecondaryType(evFrom.getSecondaryType());

            if (evTo.getSecondaryType() == null) {
                double chance = toMonIsFinalEvo ? 0.25 : 0.15;
                assignRandomSecondaryType(evTo, chance, dualTypeOnly);
            }
        }, null, pk -> {
            pk.setPrimaryType(typeService.randomType(random));
            pk.setSecondaryType(null);
            assignRandomSecondaryType(pk, 0.5, dualTypeOnly);
        });

        for (Pokemon pk : allPokes) {
            if (pk != null && pk.isActuallyCosmetic()) {
                pk.setPrimaryType(pk.getBaseForme().getPrimaryType());
                pk.setSecondaryType(pk.getBaseForme().getSecondaryType());
            }
        }

        if (megaEvolutionSanity) {
            List<MegaEvolution> allMegaEvos = romHandler.getMegaEvolutions();
            for (MegaEvolution megaEvo: allMegaEvos) {
                if (megaEvo.from.getMegaEvolutionsFrom().size() > 1) continue;
                megaEvo.to.setPrimaryType(megaEvo.from.getPrimaryType());
                megaEvo.to.setSecondaryType(megaEvo.from.getSecondaryType());

                if (megaEvo.to.getSecondaryType() == null) {
                    if (random.nextDouble() < 0.25) {
                        megaEvo.to.setSecondaryType(typeService.randomType(random));
                        while (megaEvo.to.getSecondaryType() == megaEvo.to.getPrimaryType()) {
                            megaEvo.to.setSecondaryType(typeService.randomType(random));
                        }
                    }
                }
            }
        }
    }

    private void assignRandomSecondaryType(Pokemon pk, double chance, boolean dualTypeOnly) {
        if (random.nextDouble() < chance || dualTypeOnly) {
            pk.setSecondaryType(typeService.randomType(random));
            while (pk.getSecondaryType() == pk.getPrimaryType()) {
                pk.setSecondaryType(typeService.randomType(random));
            }
        }
    }
}
