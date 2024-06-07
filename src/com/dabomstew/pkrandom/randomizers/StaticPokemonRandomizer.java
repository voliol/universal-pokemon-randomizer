package com.dabomstew.pkrandom.randomizers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.util.*;
import java.util.stream.Collectors;

public class StaticPokemonRandomizer extends Randomizer {

    // the totem randomization is here because the code is very similar,
    // but some notion of changes made to statics vs totems was still needed.
    private boolean totemChangesMade;

    public StaticPokemonRandomizer(RomHandler romHandler, Settings settings, Random random) {
        super(romHandler, settings, random);
    }

    /**
     * Returns whether any changes to non-Totem static Pokemon have been made. Alias for {@link #isChangesMade()};
     */
    public boolean isStaticChangesMade() {
        return changesMade;
    }

    /**
     * Returns whether any changes to Totem Pokemon have been made.
     */
    public boolean isTotemChangesMade() {
        return totemChangesMade;
    }

    public void onlyChangeStaticLevels() {
        int levelModifier = settings.getStaticLevelModifier();

        List<StaticEncounter> currentStaticPokemon = romHandler.getStaticPokemon();
        for (StaticEncounter se : currentStaticPokemon) {
            if (!se.isEgg) {
                se.level = Math.min(100, (int) Math.round(se.level * (1 + levelModifier / 100.0)));
                for (StaticEncounter linkedStatic : se.linkedEncounters) {
                    if (!linkedStatic.isEgg) {
                        linkedStatic.level = Math.min(100, (int) Math.round(linkedStatic.level * (1 + levelModifier / 100.0)));
                    }
                }
            }
            setPokemonAndFormeForStaticEncounter(se, se.pkmn);
        }
        romHandler.setStaticPokemon(currentStaticPokemon);
    }

    public void randomizeStaticPokemon() {
        boolean swapLegendaries = settings.getStaticPokemonMod() == Settings.StaticPokemonMod.RANDOM_MATCHING;
        boolean similarStrength = settings.getStaticPokemonMod() == Settings.StaticPokemonMod.SIMILAR_STRENGTH;
        boolean limitMainGameLegendaries = settings.isLimitMainGameLegendaries();
        boolean limit600 = settings.isLimit600();
        boolean allowAltFormes = settings.isAllowStaticAltFormes();
        boolean banIrregularAltFormes = settings.isBanIrregularAltFormes();
        boolean swapMegaEvos = settings.isSwapStaticMegaEvos();
        boolean abilitiesAreRandomized = settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE;
        int levelModifier = settings.isStaticLevelModified() ? settings.getStaticLevelModifier() : 0;
        boolean correctStaticMusic = settings.isCorrectStaticMusic();

        // Load
        List<StaticEncounter> currentStaticPokemon = romHandler.getStaticPokemon();
        List<StaticEncounter> replacements = new ArrayList<>();

        PokemonSet<Pokemon> banned = romHandler.getBannedForStaticPokemon();
        banned.addAll(rPokeService.getBannedFormesForPlayerPokemon());
        if (!abilitiesAreRandomized) {
            PokemonSet<Pokemon> abilityDependentFormes = rPokeService.getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(romHandler.getIrregularFormes());
        }
        boolean reallySwapMegaEvos = romHandler.forceSwapStaticMegaEvos() || swapMegaEvos;

        Map<Integer, Integer> specialMusicStaticChanges = new HashMap<>();
        List<Integer> changeMusicStatics = new ArrayList<>();
        if (correctStaticMusic) {
            changeMusicStatics = romHandler.getSpecialMusicStatics();
        }

        if (swapLegendaries) {
            PokemonSet<Pokemon> legendariesLeft = new PokemonSet<>(rPokeService.getLegendaries(allowAltFormes));
            if (allowAltFormes) {
                legendariesLeft = legendariesLeft.filter(pk -> !pk.isCosmeticForme());
            }
            PokemonSet<Pokemon> nonlegsLeft = new PokemonSet<>(rPokeService.getNonLegendaries(allowAltFormes));
            if (allowAltFormes) {
                nonlegsLeft = nonlegsLeft.filter(pk -> !pk.isCosmeticForme());
            }
            PokemonSet<Pokemon> ultraBeastsLeft = new PokemonSet<>(rPokeService.getUltrabeasts(false));
            legendariesLeft.removeAll(banned);
            nonlegsLeft.removeAll(banned);
            ultraBeastsLeft.removeAll(banned);

            // Full pools for easier refilling later
            PokemonSet<Pokemon> legendariesPool = new PokemonSet<>(legendariesLeft);
            PokemonSet<Pokemon> nonlegsPool = new PokemonSet<>(nonlegsLeft);
            PokemonSet<Pokemon> ultraBeastsPool = new PokemonSet<>(ultraBeastsLeft);

            for (StaticEncounter old : currentStaticPokemon) {
                StaticEncounter newStatic = cloneStaticEncounter(old);
                Pokemon newPK;
                if (old.pkmn.isLegendary()) {
                    if (reallySwapMegaEvos && old.canMegaEvolve()) {
                        newPK = getMegaEvoPokemon(rPokeService.getLegendaries(false), legendariesLeft, newStatic);
                    } else {
                        if (old.restrictedPool) {
                            newPK = getRestrictedStaticPokemon(legendariesPool, legendariesLeft, old);
                        } else {
                            newPK = legendariesLeft.getRandom(random);
                            legendariesLeft.remove(newPK);
                        }
                    }

                    setPokemonAndFormeForStaticEncounter(newStatic, newPK);

                    if (legendariesLeft.size() == 0) {
                        legendariesLeft.addAll(legendariesPool);
                    }
                } else if (rPokeService.getUltrabeasts(false).contains(old.pkmn)) {
                    if (old.restrictedPool) {
                        newPK = getRestrictedStaticPokemon(ultraBeastsPool, ultraBeastsLeft, old);
                    } else {
                        newPK = ultraBeastsLeft.getRandom(random);
                        rPokeService.getUltrabeasts(false).remove(newPK);
                    }

                    setPokemonAndFormeForStaticEncounter(newStatic, newPK);

                    if (ultraBeastsLeft.size() == 0) {
                        ultraBeastsLeft.addAll(ultraBeastsPool);
                    }
                } else {
                    if (reallySwapMegaEvos && old.canMegaEvolve()) {
                        newPK = getMegaEvoPokemon(rPokeService.getNonLegendaries(false), nonlegsLeft, newStatic);
                    } else {
                        if (old.restrictedPool) {
                            newPK = getRestrictedStaticPokemon(nonlegsPool, nonlegsLeft, old);
                        } else {
                            newPK = nonlegsLeft.getRandom(random);
                            nonlegsLeft.remove(newPK);
                        }
                    }
                    setPokemonAndFormeForStaticEncounter(newStatic, newPK);

                    if (nonlegsLeft.size() == 0) {
                        nonlegsLeft.addAll(nonlegsPool);
                    }
                }
                replacements.add(newStatic);
                if (changeMusicStatics.contains(old.pkmn.getNumber())) {
                    specialMusicStaticChanges.put(old.pkmn.getNumber(), newPK.getNumber());
                }
            }
        } else if (similarStrength) {
            PokemonSet<Pokemon> listInclFormesExclCosmetics = rPokeService.getAll(true)
                    .filter(pk -> !pk.isCosmeticForme());
            PokemonSet<Pokemon> pokemonLeft = new PokemonSet<>(!allowAltFormes ?
                    rPokeService.getAll(false) : listInclFormesExclCosmetics);
            pokemonLeft.removeAll(banned);

            PokemonSet<Pokemon> pokemonPool = new PokemonSet<>(pokemonLeft);

            List<Integer> mainGameLegendaries = romHandler.getMainGameLegendaries();
            for (StaticEncounter old : currentStaticPokemon) {
                StaticEncounter newStatic = cloneStaticEncounter(old);
                Pokemon newPK;
                Pokemon oldPK = old.pkmn;
                if (old.forme > 0) {
                    oldPK = romHandler.getAltFormeOfPokemon(oldPK, old.forme);
                }
                Integer oldBST = oldPK.bstForPowerLevels();
                if (oldBST >= 600 && limit600) {
                    if (reallySwapMegaEvos && old.canMegaEvolve()) {
                        newPK = getMegaEvoPokemon(rPokeService.getAll(false), pokemonLeft, newStatic);
                    } else {
                        if (old.restrictedPool) {
                            newPK = getRestrictedStaticPokemon(pokemonPool, pokemonLeft, old);
                        } else {
                            newPK = pokemonLeft.getRandom(random);
                            pokemonLeft.remove(newPK);
                        }
                    }
                    setPokemonAndFormeForStaticEncounter(newStatic, newPK);
                } else {
                    boolean limitBST = oldPK.isAltForme() ?
                            limitMainGameLegendaries && mainGameLegendaries.contains(oldPK.getBaseForme().getNumber()) :
                            limitMainGameLegendaries && mainGameLegendaries.contains(oldPK.getNumber());
                    if (reallySwapMegaEvos && old.canMegaEvolve()) {
                        PokemonSet<Pokemon> megaEvoPokemonLeft = rPokeService.getMegaEvolutions()
                                .stream()
                                .filter(mega -> mega.method == 1)
                                .map(mega -> mega.from)
                                .filter(pokemonLeft::contains)
                                .collect(Collectors.toCollection(PokemonSet::new));
                        if (megaEvoPokemonLeft.isEmpty()) {
                            megaEvoPokemonLeft = rPokeService.getMegaEvolutions()
                                    .stream()
                                    .filter(mega -> mega.method == 1)
                                    .map(mega -> mega.from)
                                    .filter(rPokeService.getAll(false)::contains)
                                    .collect(Collectors.toCollection(PokemonSet::new));
                        }
                        newPK = pickStaticPowerLvlReplacement(
                                megaEvoPokemonLeft,
                                oldPK,
                                true,
                                limitBST);
                        newStatic.heldItem = newPK
                                .getMegaEvolutionsFrom()
                                .get(random.nextInt(newPK.getMegaEvolutionsFrom().size()))
                                .argument;
                    } else {
                        if (old.restrictedPool) {
                            PokemonSet<Pokemon> restrictedPool = pokemonLeft
                                    .filter(pk -> old.restrictedList.contains(pk));
                            if (restrictedPool.isEmpty()) {
                                restrictedPool = pokemonPool.filter(pk -> old.restrictedList.contains(pk));
                            }
                            newPK = pickStaticPowerLvlReplacement(
                                    restrictedPool,
                                    oldPK,
                                    false, // Allow same Pokemon just in case
                                    limitBST);
                        } else {
                            newPK = pickStaticPowerLvlReplacement(
                                    pokemonLeft,
                                    oldPK,
                                    true,
                                    limitBST);
                        }
                    }
                    pokemonLeft.remove(newPK);
                    setPokemonAndFormeForStaticEncounter(newStatic, newPK);
                }

                if (pokemonLeft.size() == 0) {
                    pokemonLeft.addAll(pokemonPool);
                }
                replacements.add(newStatic);
                if (changeMusicStatics.contains(old.pkmn.getNumber())) {
                    specialMusicStaticChanges.put(old.pkmn.getNumber(), newPK.getNumber());
                }
            }
        } else { // Completely random
            PokemonSet<Pokemon> listInclFormesExclCosmetics = rPokeService.getAll(true)
                    .filter(pk -> !pk.isCosmeticForme());
            PokemonSet<Pokemon> pokemonLeft = new PokemonSet<>(!allowAltFormes ?
                    rPokeService.getAll(false) : listInclFormesExclCosmetics);
            pokemonLeft.removeAll(banned);

            PokemonSet<Pokemon> pokemonPool = new PokemonSet<>(pokemonLeft);

            for (StaticEncounter old : currentStaticPokemon) {
                StaticEncounter newStatic = cloneStaticEncounter(old);
                Pokemon newPK;
                if (reallySwapMegaEvos && old.canMegaEvolve()) {
                    newPK = getMegaEvoPokemon(rPokeService.getAll(false), pokemonLeft, newStatic);
                } else {
                    if (old.restrictedPool) {
                        newPK = getRestrictedStaticPokemon(pokemonPool, pokemonLeft, old);
                    } else {
                        newPK = pokemonLeft.getRandom(random);
                        pokemonLeft.remove(newPK);
                    }
                }
                pokemonLeft.remove(newPK);
                setPokemonAndFormeForStaticEncounter(newStatic, newPK);
                if (pokemonLeft.size() == 0) {
                    pokemonLeft.addAll(pokemonPool);
                }
                replacements.add(newStatic);
                if (changeMusicStatics.contains(old.pkmn.getNumber())) {
                    specialMusicStaticChanges.put(old.pkmn.getNumber(), newPK.getNumber());
                }
            }
        }

        if (levelModifier != 0) {
            for (StaticEncounter se : replacements) {
                if (!se.isEgg) {
                    se.level = Math.min(100, (int) Math.round(se.level * (1 + levelModifier / 100.0)));
                    se.maxLevel = Math.min(100, (int) Math.round(se.maxLevel * (1 + levelModifier / 100.0)));
                    for (StaticEncounter linkedStatic : se.linkedEncounters) {
                        if (!linkedStatic.isEgg) {
                            linkedStatic.level = Math.min(100, (int) Math.round(linkedStatic.level * (1 + levelModifier / 100.0)));
                            linkedStatic.maxLevel = Math.min(100, (int) Math.round(linkedStatic.maxLevel * (1 + levelModifier / 100.0)));
                        }
                    }
                }
            }
        }

        if (specialMusicStaticChanges.size() > 0) {
            romHandler.applyCorrectStaticMusic(specialMusicStaticChanges);
        }

        // Save
        romHandler.setStaticPokemon(replacements);
        changesMade = true;
    }

    public void randomizeTotemPokemon() {
        boolean randomizeTotem =
                settings.getTotemPokemonMod() == Settings.TotemPokemonMod.RANDOM ||
                        settings.getTotemPokemonMod() == Settings.TotemPokemonMod.SIMILAR_STRENGTH;
        boolean randomizeAllies =
                settings.getAllyPokemonMod() == Settings.AllyPokemonMod.RANDOM ||
                        settings.getAllyPokemonMod() == Settings.AllyPokemonMod.SIMILAR_STRENGTH;
        boolean randomizeAuras =
                settings.getAuraMod() == Settings.AuraMod.RANDOM ||
                        settings.getAuraMod() == Settings.AuraMod.SAME_STRENGTH;
        boolean similarStrengthTotem = settings.getTotemPokemonMod() == Settings.TotemPokemonMod.SIMILAR_STRENGTH;
        boolean similarStrengthAllies = settings.getAllyPokemonMod() == Settings.AllyPokemonMod.SIMILAR_STRENGTH;
        boolean similarStrengthAuras = settings.getAuraMod() == Settings.AuraMod.SAME_STRENGTH;
        boolean randomizeHeldItems = settings.isRandomizeTotemHeldItems();
        int levelModifier = settings.isTotemLevelsModified() ? settings.getTotemLevelModifier() : 0;
        boolean allowAltFormes = settings.isAllowTotemAltFormes();
        boolean banIrregularAltFormes = settings.isBanIrregularAltFormes();
        boolean abilitiesAreRandomized = settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE;

        List<TotemPokemon> currentTotemPokemon = romHandler.getTotemPokemon();
        List<TotemPokemon> replacements = new ArrayList<>();
        PokemonSet<Pokemon> banned = romHandler.getBannedForStaticPokemon();
        if (!abilitiesAreRandomized) {
            PokemonSet<Pokemon> abilityDependentFormes = rPokeService.getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(romHandler.getIrregularFormes());
        }

        PokemonSet<Pokemon> listInclFormesExclCosmetics = rPokeService.getAll(true).filter(
                pk -> !pk.isCosmeticForme());
        PokemonSet<Pokemon> pokemonLeft = new PokemonSet<>(!allowAltFormes ?
                rPokeService.getAll(false) : listInclFormesExclCosmetics);
        pokemonLeft.removeAll(banned);

        for (TotemPokemon old : currentTotemPokemon) {
            TotemPokemon newTotem = new TotemPokemon();
            newTotem.heldItem = old.heldItem;
            if (randomizeTotem) {
                Pokemon newPK;
                Pokemon oldPK = old.pkmn;
                if (old.forme > 0) {
                    oldPK = romHandler.getAltFormeOfPokemon(oldPK, old.forme);
                }

                if (similarStrengthTotem) {
                    newPK = pickStaticPowerLvlReplacement(
                            pokemonLeft,
                            oldPK,
                            true,
                            false);
                } else {
                    newPK = pokemonLeft.getRandom(random);
                    pokemonLeft.remove(newPK);
                }

                pokemonLeft.remove(newPK);
                newTotem.pkmn = newPK;
                setFormeForStaticEncounter(newTotem, newPK);
                newTotem.resetMoves = true;
                newTotem.level = old.level;

                if (levelModifier != 0) {
                    newTotem.level = Math.min(100, (int) Math.round(newTotem.level * (1 + levelModifier / 100.0)));
                }
                if (pokemonLeft.size() == 0) {
                    pokemonLeft.addAll(!allowAltFormes ? rPokeService.getAll(false) : listInclFormesExclCosmetics);
                    pokemonLeft.removeAll(banned);
                }
            } else {
                newTotem.pkmn = old.pkmn;
                newTotem.level = old.level;
                if (levelModifier != 0) {
                    newTotem.level = Math.min(100, (int) Math.round(newTotem.level * (1 + levelModifier / 100.0)));
                }
                setFormeForStaticEncounter(newTotem, newTotem.pkmn);
            }

            if (randomizeAllies) {
                for (Integer oldAllyIndex : old.allies.keySet()) {
                    StaticEncounter oldAlly = old.allies.get(oldAllyIndex);
                    StaticEncounter newAlly = new StaticEncounter();
                    Pokemon newAllyPK;
                    Pokemon oldAllyPK = oldAlly.pkmn;
                    if (oldAlly.forme > 0) {
                        oldAllyPK = romHandler.getAltFormeOfPokemon(oldAllyPK, oldAlly.forme);
                    }
                    if (similarStrengthAllies) {
                        newAllyPK = pickStaticPowerLvlReplacement(
                                pokemonLeft,
                                oldAllyPK,
                                true,
                                false);
                    } else {
                        newAllyPK = pokemonLeft.getRandom(random);
                        pokemonLeft.remove(newAllyPK);
                    }

                    pokemonLeft.remove(newAllyPK);
                    newAlly.pkmn = newAllyPK;
                    setFormeForStaticEncounter(newAlly, newAllyPK);
                    newAlly.resetMoves = true;
                    newAlly.level = oldAlly.level;
                    if (levelModifier != 0) {
                        newAlly.level = Math.min(100, (int) Math.round(newAlly.level * (1 + levelModifier / 100.0)));
                    }

                    newTotem.allies.put(oldAllyIndex, newAlly);
                    if (pokemonLeft.size() == 0) {
                        pokemonLeft.addAll(!allowAltFormes ? rPokeService.getAll(false) : listInclFormesExclCosmetics);
                        pokemonLeft.removeAll(banned);
                    }
                }
            } else {
                newTotem.allies = old.allies;
                for (StaticEncounter ally : newTotem.allies.values()) {
                    if (levelModifier != 0) {
                        ally.level = Math.min(100, (int) Math.round(ally.level * (1 + levelModifier / 100.0)));
                        setFormeForStaticEncounter(ally, ally.pkmn);
                    }
                }
            }

            if (randomizeAuras) {
                if (similarStrengthAuras) {
                    newTotem.aura = Aura.randomAuraSimilarStrength(random, old.aura);
                } else {
                    newTotem.aura = Aura.randomAura(random);
                }
            } else {
                newTotem.aura = old.aura;
            }

            if (randomizeHeldItems) {
                if (old.heldItem != 0) {
                    List<Integer> consumableList = romHandler.getAllConsumableHeldItems();
                    newTotem.heldItem = consumableList.get(random.nextInt(consumableList.size()));
                }
            }

            replacements.add(newTotem);
        }

        // Save
        romHandler.setTotemPokemon(replacements);
        totemChangesMade = true;
    }

    private StaticEncounter cloneStaticEncounter(StaticEncounter old) {
        StaticEncounter newStatic = new StaticEncounter(old);
        newStatic.resetMoves = true;
        for (StaticEncounter linked : newStatic.linkedEncounters) {
            linked.resetMoves = true;
        }
        return newStatic;
    }

    private void setPokemonAndFormeForStaticEncounter(StaticEncounter newStatic, Pokemon pk) {
        boolean checkCosmetics = true;
        Pokemon newPK = pk;
        int newForme = 0;
        if (pk.isAltForme()) {
            newForme = pk.getFormeNumber();
            newPK = pk.getBaseForme();
            checkCosmetics = false;
        }
        if (checkCosmetics && pk.getCosmeticFormCount() > 0) {
            newForme = pk.getCosmeticFormNumber(random.nextInt(pk.getCosmeticFormCount()));
        } else if (!checkCosmetics && pk.getCosmeticFormCount() > 0) {
            newForme += pk.getCosmeticFormNumber(random.nextInt(pk.getCosmeticFormCount()));
        }
        newStatic.pkmn = newPK;
        newStatic.forme = newForme;
        for (StaticEncounter linked : newStatic.linkedEncounters) {
            linked.pkmn = newPK;
            linked.forme = newForme;
        }
    }

    private Pokemon getRestrictedStaticPokemon(PokemonSet<Pokemon> fullList, PokemonSet<Pokemon> pokemonLeft,
                                               StaticEncounter old) {
        PokemonSet<Pokemon> restrictedPool = pokemonLeft.filter(pk -> old.restrictedList.contains(pk));
        if (restrictedPool.isEmpty()) {
            restrictedPool = fullList.filter(pk -> old.restrictedList.contains(pk));
        }
        Pokemon newPK = restrictedPool.getRandom(random);
        pokemonLeft.remove(newPK);
        return newPK;
    }

    private Pokemon getMegaEvoPokemon(PokemonSet<Pokemon> fullList, PokemonSet<Pokemon> pokemonLeft,
                                      StaticEncounter newStatic) {
        Set<MegaEvolution> megaEvos = rPokeService.getMegaEvolutions();
        PokemonSet<Pokemon> megaEvoPokemon = megaEvos
                .stream()
                .filter(mega -> mega.method == 1)
                .map(mega -> mega.from)
                .collect(Collectors.toCollection(PokemonSet::new));
        PokemonSet<Pokemon> megaEvoPokemonLeft = new PokemonSet<>(megaEvoPokemon).filter(pokemonLeft::contains);
        if (megaEvoPokemonLeft.isEmpty()) {
            megaEvoPokemonLeft = new PokemonSet<>(megaEvoPokemon).filter(fullList::contains);
        }

        Pokemon newPK = megaEvoPokemonLeft.getRandom(random);
        pokemonLeft.remove(newPK);
        newStatic.heldItem = newPK
                .getMegaEvolutionsFrom()
                .get(random.nextInt(newPK.getMegaEvolutionsFrom().size()))
                .argument;
        return newPK;
    }

    private Pokemon pickStaticPowerLvlReplacement(PokemonSet<Pokemon> pokemonPool, Pokemon current,
                                                  boolean banSamePokemon, boolean limitBST) {
        // start with within 10% and add 5% either direction till we find
        // something
        int currentBST = current.bstForPowerLevels();
        int minTarget = limitBST ? currentBST - currentBST / 5 : currentBST - currentBST / 10;
        int maxTarget = limitBST ? currentBST : currentBST + currentBST / 10;
        PokemonSet<Pokemon> canPick = new PokemonSet<>();
        int expandRounds = 0;
        while (canPick.isEmpty() || (canPick.size() < 3 && expandRounds < 3)) {
            for (Pokemon pk : pokemonPool) {
                if (pk.bstForPowerLevels() >= minTarget && pk.bstForPowerLevels() <= maxTarget
                        && (!banSamePokemon || pk != current)) {
                    canPick.add(pk);
                }
            }
            minTarget -= currentBST / 20;
            maxTarget += currentBST / 20;
            expandRounds++;
        }
        return canPick.getRandom(random);
    }

    private void setFormeForStaticEncounter(StaticEncounter newStatic, Pokemon pk) {
        boolean checkCosmetics = true;
        newStatic.forme = 0;
        if (pk.isAltForme()) {
            newStatic.forme = pk.getFormeNumber();
            newStatic.pkmn = pk.getBaseForme();
            checkCosmetics = false;
        }
        if (checkCosmetics && newStatic.pkmn.getCosmeticFormCount() > 0) {
            newStatic.forme = newStatic.pkmn.getCosmeticFormNumber(random.nextInt(newStatic.pkmn.getCosmeticFormCount()));
        } else if (!checkCosmetics && pk.getCosmeticFormCount() > 0) {
            newStatic.forme += pk.getCosmeticFormNumber(random.nextInt(pk.getCosmeticFormCount()));
        }
    }

}
