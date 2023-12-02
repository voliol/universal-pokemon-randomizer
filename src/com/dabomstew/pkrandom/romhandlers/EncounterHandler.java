package com.dabomstew.pkrandom.romhandlers;

import com.dabomstew.pkrandom.pokemon.*;

import java.util.*;

public class EncounterHandler {

    private final RomHandler romHandler;
    private Random random;

    private boolean typeThemed;

    private PokemonSet<Pokemon> allowed;
    private Map<Type, PokemonSet<Pokemon>> allowedByType;
    private PokemonSet<Pokemon> remaining;
    private Map<Type, PokemonSet<Pokemon>> remainingByType;

    public EncounterHandler(RomHandler romHandler) {
        this.romHandler = romHandler;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    private void initAllowedByType() {
        if (typeThemed) {
            allowedByType = new EnumMap<>(Type.class);
            for (Type t : Type.values()) {
                allowedByType.put(t, allowed.filterByType(t));
            }
        }
    }

    private void resetRemainingPokemon() {
        remaining = new PokemonSet<>(allowed);
        if (typeThemed) {
            remainingByType = new EnumMap<>(Type.class);
            for (Type t : Type.values()) {
                remainingByType.put(t, new PokemonSet<>(allowedByType.get(t)));
            }
        }
    }

    public void randomEncountersInner(List<EncounterArea> currentEncounterAreas,
                                       PokemonSet<Pokemon> banned, PokemonSet<Pokemon> allowed,
                                       boolean catchEmAll, boolean typeThemed, boolean usePowerLevels,
                                       boolean balanceShakingGrass) {
        this.allowed = allowed;
        this.typeThemed = typeThemed;

        initAllowedByType();
        resetRemainingPokemon();

        // Shuffling the EncounterAreas leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<EncounterArea> scrambledEncounterAreas = new ArrayList<>(currentEncounterAreas);
        Collections.shuffle(scrambledEncounterAreas, random);

        for (EncounterArea area : scrambledEncounterAreas) {
            Type areaType = romHandler.randomType();
            PokemonSet<Pokemon> allowedForArea;
            if (catchEmAll && !remaining.isEmpty()) {
                do {
                    areaType = romHandler.randomType();
                    allowedForArea = typeThemed ? remainingByType.get(areaType) : remaining;
                } while (allowedForArea.isEmpty());
            } else {
                allowedForArea = typeThemed ? allowedByType.get(areaType) : allowed;
            }

            for (Encounter enc : area) {
                Pokemon current = enc.getPokemon();

                Pokemon replacement;
                // In Catch 'Em All mode, don't randomize encounters for Pokemon that are banned for
                // wild encounters. Otherwise, it may be impossible to obtain this Pokemon unless it
                // randomly appears as a static or unless it becomes a random evolution.
                if (catchEmAll && banned.contains(current)) {
                    replacement = current;
                } else if (usePowerLevels) {
                    replacement = balanceShakingGrass ?
                            pickWildPowerLvlReplacement(allowedForArea, current, false,
                                    null, (enc.getLevel() + enc.getMaxLevel()) / 2) :
                            pickWildPowerLvlReplacement(allowedForArea, current, false, null,
                                    100);
                } else {
                    replacement = allowedForArea.getRandom(random);
                }


                enc.setPokemon(replacement);
                setFormeForEncounter(enc, replacement);

                if (catchEmAll) {
                    remaining.remove(replacement);
                    if (typeThemed) {
                        remainingByType.get(replacement.getPrimaryType()).remove(replacement);
                        if (replacement.getSecondaryType() != null) {
                            remainingByType.get(replacement.getSecondaryType()).remove(replacement);
                        }
                    }
                    if (allowedForArea.isEmpty()) {
                        allowedForArea = typeThemed ? allowedByType.get(areaType) : allowed;
                    }
                }
            }
        }
    }

    public void area1to1EncountersInner(List<EncounterArea> currentEncounterAreas,
                                         PokemonSet<Pokemon> banned, PokemonSet<Pokemon> allowed,
                                         boolean catchEmAll, boolean typeThemed, boolean usePowerLevels,
                                         boolean balanceShakingGrass) {
        PokemonSet<Pokemon> remaining = new PokemonSet<>(allowed);
        Map<Type, PokemonSet<Pokemon>> pokemonByType = new EnumMap<>(Type.class);
        Map<Type, PokemonSet<Pokemon>> remainingPokemonByType = new EnumMap<>(Type.class);
        if (typeThemed) {
            for (Type t : Type.values()) {
                pokemonByType.put(t, allowed.filterByType(t));
            }
            for (Type t : Type.values()) {
                remainingPokemonByType.put(t, new PokemonSet<>(pokemonByType.get(t)));
            }
        }

        // Shuffling the EncounterAreas leads to less predictable results for various modifiers.
        // Need to keep the original ordering around for saving though.
        List<EncounterArea> scrambledEncounterAreas = new ArrayList<>(currentEncounterAreas);
        Collections.shuffle(scrambledEncounterAreas, random);

        for (EncounterArea area : scrambledEncounterAreas) {
            Type areaType = romHandler.randomType();
            PokemonSet<Pokemon> allowedForArea;
            if (catchEmAll && !remaining.isEmpty()) {
                do {
                    areaType = romHandler.randomType();
                    allowedForArea = typeThemed ? remainingPokemonByType.get(areaType) : remaining;
                } while (allowedForArea.isEmpty());
            } else {
                allowedForArea = typeThemed ? pokemonByType.get(areaType) : allowed;
            }

            Map<Pokemon, Pokemon> areaMap = new TreeMap<>();

            for (Encounter enc : area) {
                Pokemon current = enc.getPokemon();
                if (areaMap.containsKey(current)) {
                    continue;
                }

                Pokemon replacement;
                // In Catch 'Em All mode, don't randomize encounters for Pokemon that are banned for
                // wild encounters. Otherwise, it may be impossible to obtain this Pokemon unless it
                // randomly appears as a static or unless it becomes a random evolution.
                if (catchEmAll && banned.contains(current)) {
                    replacement = current;
                } else {
                    do {
                        if (usePowerLevels) {
                            replacement = balanceShakingGrass ?
                                    pickWildPowerLvlReplacement(allowedForArea, current, false,
                                            null, (enc.getLevel() + enc.getMaxLevel()) / 2) :
                                    pickWildPowerLvlReplacement(allowedForArea, current, false, null,
                                            100);
                        } else {
                            replacement = allowedForArea.getRandom(random);
                        }
                    } while (areaMap.containsValue(replacement) && areaMap.size() < allowedForArea.size());
                }
                areaMap.put(current, replacement);

                if (catchEmAll) {
                    remaining.remove(replacement);
                    if (typeThemed) {
                        remainingPokemonByType.get(replacement.getPrimaryType()).remove(replacement);
                        if (replacement.getSecondaryType() != null) {
                            remainingPokemonByType.get(replacement.getSecondaryType()).remove(replacement);
                        }
                    }
                    if (allowedForArea.isEmpty()) {
                        allowedForArea = typeThemed ? pokemonByType.get(areaType) : allowed;
                    }
                }
            }

            for (Encounter enc : area) {
                Pokemon encPk = enc.getPokemon();
                enc.setPokemon(areaMap.get(encPk));
                setFormeForEncounter(enc, encPk);
            }
        }
    }

    public void game1to1EncountersInner(List<EncounterArea> encounterAreas, PokemonSet<Pokemon> allowed,
                                         boolean usePowerLevels) {
        Map<Pokemon, Pokemon> translateMap = new TreeMap<>();
        PokemonSet<Pokemon> remaining = new PokemonSet<>(allowed);

        // Shuffle so the order of the areas/encounters doesn't matter when picking.
        // Though pokes with many encounters will still get a priority when it comes
        // to picking similar-strength, since they are more likely to be handled early.
        List<Encounter> shuffled = new ArrayList<>();
        encounterAreas.forEach(shuffled::addAll);
        Collections.shuffle(shuffled, random);
        for (Encounter enc : shuffled) {
            Pokemon current = enc.getPokemon();

            Pokemon replacement;
            if (translateMap.containsKey(current)) {
                replacement = translateMap.get(current);
            } else {
                replacement = usePowerLevels ?
                        pickWildPowerLvlReplacement(remaining, current, true, null, 100) :
                        remaining.getRandom(random);
                // In case it runs out of unique Pokémon, picks something already mapped to.
                // Shouldn't happen unless restrictions are really harsh, normally [#allowed Pokémon] > [#Pokémon which appear in the wild]
                if (replacement == null) {
                    replacement = allowed.getRandom(random);
                } else {
                    remaining.remove(replacement);
                }
                translateMap.put(current, replacement);
            }
            enc.setPokemon(replacement);
            setFormeForEncounter(enc, replacement);
        }
    }

    private Pokemon pickWildPowerLvlReplacement(PokemonSet<Pokemon> pokemonPool, Pokemon current, boolean banSamePokemon,
                                                PokemonSet<Pokemon> usedUp, int bstBalanceLevel) {
        // start with within 10% and add 5% either direction till we find
        // something
        int balancedBST = bstBalanceLevel * 10 + 250;
        int currentBST = Math.min(current.bstForPowerLevels(), balancedBST);
        int minTarget = currentBST - currentBST / 10;
        int maxTarget = currentBST + currentBST / 10;
        PokemonSet<Pokemon> canPick = new PokemonSet<>();
        int expandRounds = 0;
        while (canPick.isEmpty() || (canPick.size() < 3 && expandRounds < 3)) {
            for (Pokemon pk : pokemonPool) {
                if (pk.bstForPowerLevels() >= minTarget && pk.bstForPowerLevels() <= maxTarget
                        && (!banSamePokemon || pk != current) && (usedUp == null || !usedUp.contains(pk))
                        && !canPick.contains(pk)) {
                    canPick.add(pk);
                }
            }
            minTarget -= currentBST / 20;
            maxTarget += currentBST / 20;
            expandRounds++;
        }
        return canPick.getRandom(random);
    }

    private void setFormeForEncounter(Encounter enc, Pokemon pk) {
        boolean checkCosmetics = true;
        enc.setFormeNumber(0);
        if (enc.getPokemon().getFormeNumber() > 0) {
            enc.setFormeNumber(enc.getPokemon().getFormeNumber());
            enc.setPokemon(enc.getPokemon().getBaseForme());
            checkCosmetics = false;
        }
        if (checkCosmetics && enc.getPokemon().getCosmeticForms() > 0) {
            enc.setFormeNumber(enc.getPokemon().getCosmeticFormNumber(random.nextInt(enc.getPokemon().getCosmeticForms())));
        } else if (!checkCosmetics && pk.getCosmeticForms() > 0) {
            enc.setFormeNumber(enc.getFormeNumber() + pk.getCosmeticFormNumber(random.nextInt(pk.getCosmeticForms())));
        }
    }

}
