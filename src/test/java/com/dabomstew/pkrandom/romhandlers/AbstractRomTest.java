package com.dabomstew.pkrandom.romhandlers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.dabomstew.pkrandom.RomOptions;
import com.dabomstew.pkrandom.constants.Gen5Constants;
import com.dabomstew.pkrandom.constants.GlobalConstants;
import com.dabomstew.pkrandom.exceptions.RandomizationException;
import com.dabomstew.pkrandom.pokemon.Encounter;
import com.dabomstew.pkrandom.pokemon.EncounterSet;
import com.dabomstew.pkrandom.pokemon.Evolution;
import com.dabomstew.pkrandom.pokemon.EvolutionType;
import com.dabomstew.pkrandom.pokemon.GenRestrictions;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Trainer;
import com.dabomstew.pkrandom.pokemon.TrainerPokemon;
import com.dabomstew.pkrandom.pokemon.Type;
import com.dabomstew.pkrandom.text.Gen5TextHandler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

public class AbstractRomTest {

    ArrayList<Pokemon> pokemonList;
    ArrayList<Trainer> trainerList;
    ArrayList<Pokemon> startersList;
    ArrayList<EncounterSet> encountersList;

    @Captor
    ArgumentCaptor<ArrayList<Trainer>> trainerCap;

    @Captor
    ArgumentCaptor<ArrayList<Pokemon>> pokemonCap;

    @Captor
    ArgumentCaptor<ArrayList<EncounterSet>> encounterCap;

    /**
     * Initializes any annotated mockito objects
     */
    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Black/White Cilan/Chili/Cress use a type that is either offensively superior or defensively
     * resistant to the starter of the player
     */
    @Test
    public void TestCilanChiliCressTrumpStarter() {
        // Magnemite should fight ground offense or electric defense
        // Ponyta should return a random element from the Fire listing
        // All 3 have 1 pokemon with a shared type
        HashSet<Type> gym1Type = new HashSet<Type>();
        Gen5RomHandler romhandler = spy(new Gen5RomHandler(new Random()));
        resetDataModel(romhandler);
        startersList.get(0).primaryType = Type.ELECTRIC;
        startersList.get(0).secondaryType = Type.STEEL;
        startersList.get(1).primaryType = Type.FIRE;
        startersList.get(2).primaryType = Type.NORMAL;
        doReturn(Gen5RomHandler.getRomFromSupportedRom("Black (U)")).when(romhandler).getRomEntry();
        doReturn(mock(Gen5TextHandler.class)).when(romhandler).getTextHandler();
        doNothing().when(romhandler).setTrainers(trainerCap.capture());

        // **************************
        // Test offensive selection
        // **************************
        romhandler.randomizeTrainerPokes(false, false, false, false, false, false, false, true,
                false, false, 0);
        ArrayList<Trainer> newTrainers = trainerCap.getValue();
        // Get gym1Type
        for (Trainer t : newTrainers.stream().filter(t -> t.getTag() != null)
                .filter(t -> t.getTag().equals("GYM1")).collect(Collectors.toList())) {
            for (TrainerPokemon tp : t.getPokemon()) {
                // Initialize the set
                if (gym1Type.size() == 0) {
                    gym1Type.add(tp.pokemon.primaryType);
                    if (tp.pokemon.secondaryType != null) {
                        gym1Type.add(tp.pokemon.secondaryType);
                    }
                }
                // Only keep the shared type
                else {
                    HashSet<Type> intersect = new HashSet<Type>();
                    intersect.add(tp.pokemon.primaryType);
                    if (tp.pokemon.secondaryType != null) {
                        intersect.add(tp.pokemon.secondaryType);
                    }
                    gym1Type.retainAll(intersect);
                }
            }
        }

        // Check CHILI, CRESS, CILAN against starters
        // Check CHILI, CRESS, CILAN share 1 type with GYM1
        for (Trainer t : newTrainers) {
            if (t.getTag() == null) {
                continue;
            }
            // CHILI fights the first starter (index 0)
            if (t.getTag().equals("CHILI")) {
                // Electric-Steel like Magnemite should fight Ground
                assertTrue("No GROUND type found for CHILI",
                        t.getPokemon().stream().anyMatch(tp -> tp.pokemon.primaryType == Type.GROUND
                                || tp.pokemon.secondaryType == Type.GROUND));
                // Find out how many pokemon share a type with GYM1, subtract it from the total
                // number of pokemon on the team, and ensure either 0 or 1 pokemon don't share
                // the type. 0 is possible due to the shared type and trump type
                // being the same or a dual-typed pokemon sharing one from each.
                assertTrue("More than 1 CHILI pokemon did not match the GYM1 type",
                        t.getPokemon().size() - t.getPokemon().stream()
                                .filter(tp -> gym1Type.contains(tp.pokemon.primaryType)
                                        || gym1Type.contains(tp.pokemon.secondaryType))
                                .count() < 2);
            }
            // CRESS fights the second starter (index 1)
            else if (t.getTag().equals("CRESS")) {
                // Pure Fire type like Ponyta should fight random weakness
                assertTrue("No type found that is in STRONG_AGAINST.get(Type.FIRE) for CRESS",
                        t.getPokemon().stream()
                                .anyMatch(tp -> Type.STRONG_AGAINST.get(Type.FIRE)
                                        .contains(tp.pokemon.primaryType)
                                        || Type.STRONG_AGAINST.get(Type.FIRE)
                                                .contains(tp.pokemon.secondaryType)));
                // Find out how many pokemon share a type with GYM1, subtract it from the total
                // number of pokemon on the team, and ensure either 0 or 1 pokemon don't share
                // the type. 0 is possible due to the shared type and trump type
                // being the same or a dual-typed pokemon sharing one from each.
                assertTrue("More than 1 CRESS pokemon did not match the GYM1 type",
                        t.getPokemon().size() - t.getPokemon().stream()
                                .filter(tp -> gym1Type.contains(tp.pokemon.primaryType)
                                        || gym1Type.contains(tp.pokemon.secondaryType))
                                .count() < 2);
            }
            // CILAN fights the last starter (index 2)
            else if (t.getTag().equals("CILAN")) {
                // Pure Normal type like Rattata should fight Fighting
                assertTrue("No FIGHTING type found for CILAN",
                        t.getPokemon().stream()
                                .anyMatch(tp -> tp.pokemon.primaryType == Type.FIGHTING
                                        || tp.pokemon.secondaryType == Type.FIGHTING));
                // Find out how many pokemon share a type with GYM1, subtract it from the total
                // number of pokemon on the team, and ensure either 0 or 1 pokemon don't share
                // the type. 0 is possible due to the shared type and trump type
                // being the same or a dual-typed pokemon sharing one from each.
                assertTrue("More than 1 CILAN pokemon did not match the GYM1 type",
                        t.getPokemon().size() - t.getPokemon().stream()
                                .filter(tp -> gym1Type.contains(tp.pokemon.primaryType)
                                        || gym1Type.contains(tp.pokemon.secondaryType))
                                .count() < 2);
            }
        }
        // **************************
        // Test defensive selection
        // **************************
        romhandler.randomizeTrainerPokes(false, false, false, false, true, false, false, true,
                false, false, 0);
        newTrainers = trainerCap.getValue();
        // Get gym1Type
        for (Trainer t : newTrainers.stream().filter(t -> t.getTag() != null)
                .filter(t -> t.getTag().equals("GYM1")).collect(Collectors.toList())) {
            for (TrainerPokemon tp : t.getPokemon()) {
                // Initialize the set
                if (gym1Type.size() == 0) {
                    gym1Type.add(tp.pokemon.primaryType);
                    if (tp.pokemon.secondaryType != null) {
                        gym1Type.add(tp.pokemon.secondaryType);
                    }
                }
                // Only keep the shared type
                else {
                    HashSet<Type> intersect = new HashSet<Type>();
                    intersect.add(tp.pokemon.primaryType);
                    if (tp.pokemon.secondaryType != null) {
                        intersect.add(tp.pokemon.secondaryType);
                    }
                    gym1Type.retainAll(intersect);
                }
            }
        }

        // Check CHILI, CRESS, CILAN against starters
        // Check CHILI, CRESS, CILAN share 1 type with GYM1
        for (Trainer t : newTrainers) {
            if (t.getTag() == null) {
                continue;
            }
            // CHILI fights the first starter (index 0)
            if (t.getTag().equals("CHILI")) {
                // Electric-Steel like Magnemite should fight Electric
                assertTrue("No ELECTRIC type found for CHILI",
                        t.getPokemon().stream()
                                .anyMatch(tp -> tp.pokemon.primaryType == Type.ELECTRIC
                                        || tp.pokemon.secondaryType == Type.ELECTRIC));
                // Find out how many pokemon share a type with GYM1, subtract it from the total
                // number of pokemon on the team, and ensure either 0 or 1 pokemon don't share
                // the type. 0 is possible due to the shared type and trump type
                // being the same or a dual-typed pokemon sharing one from each.
                assertTrue("More than 1 CHILI pokemon did not match the GYM1 type",
                        t.getPokemon().size() - t.getPokemon().stream()
                                .filter(tp -> gym1Type.contains(tp.pokemon.primaryType)
                                        || gym1Type.contains(tp.pokemon.secondaryType))
                                .count() < 2);
            }
            // CRESS fights the second starter (index 1)
            else if (t.getTag().equals("CRESS")) {
                // Pure Fire type like Ponyta should fight random weakness
                assertTrue("No type found that is in RESISTANT_TO.get(Type.FIRE) for CRESS",
                        t.getPokemon().stream()
                                .anyMatch(tp -> Type.RESISTANT_TO.get(Type.FIRE)
                                        .contains(tp.pokemon.primaryType)
                                        || Type.RESISTANT_TO.get(Type.FIRE)
                                                .contains(tp.pokemon.secondaryType)));
                // Find out how many pokemon share a type with GYM1, subtract it from the total
                // number of pokemon on the team, and ensure either 0 or 1 pokemon don't share
                // the type. 0 is possible due to the shared type and trump type
                // being the same or a dual-typed pokemon sharing one from each.
                assertTrue("More than 1 CRESS pokemon did not match the GYM1 type",
                        t.getPokemon().size() - t.getPokemon().stream()
                                .filter(tp -> gym1Type.contains(tp.pokemon.primaryType)
                                        || gym1Type.contains(tp.pokemon.secondaryType))
                                .count() < 2);
            }
            // CILAN fights the last starter (index 2)
            else if (t.getTag().equals("CILAN")) {
                // Pure Normal type like Rattata should fight random weakness
                assertTrue("No type found that is in combineMap.get(Type.NORMAL) for CILAN",
                        t.getPokemon().stream()
                                .anyMatch(tp -> Type.getCombinedResistanceMap().get(Type.NORMAL)
                                        .contains(tp.pokemon.primaryType)
                                        || Type.getCombinedResistanceMap().get(Type.NORMAL)
                                                .contains(tp.pokemon.secondaryType)));
                // Find out how many pokemon share a type with GYM1, subtract it from the total
                // number of pokemon on the team, and ensure either 0 or 1 pokemon don't share
                // the type. 0 is possible due to the shared type and trump type
                // being the same or a dual-typed pokemon sharing one from each.
                assertTrue("More than 1 CILAN pokemon did not match the GYM1 type",
                        t.getPokemon().size() - t.getPokemon().stream()
                                .filter(tp -> gym1Type.contains(tp.pokemon.primaryType)
                                        || gym1Type.contains(tp.pokemon.secondaryType))
                                .count() < 2);
            }
        }
    }

    /**
     * Black/White Cilan/Chili/Cress use a type that is either offensively superior or defensively
     * resistant to the starter of the player This should also adhere to global swap rules
     */
    @Test
    public void TestGlobalSwapCilanChiliCressTrumpStarter() {
        // Magnemite should fight ground offense or electric defense
        // Ponyta should return a random element from the Fire listing
        // All 3 have 1 pokemon with a shared type
        HashMap<Pokemon, Pokemon> pokemonSwap =
                new HashMap<Pokemon, Pokemon>(Gen5Constants.pokemonCount + 1);
        HashSet<Type> gym1Type = new HashSet<Type>();
        Gen5RomHandler romhandler = spy(new Gen5RomHandler(new Random()));
        resetDataModel(romhandler);
        startersList.get(0).primaryType = Type.ELECTRIC;
        startersList.get(0).secondaryType = Type.STEEL;
        startersList.get(1).primaryType = Type.FIRE;
        startersList.get(2).primaryType = Type.NORMAL;
        doReturn(Gen5RomHandler.getRomFromSupportedRom("Black (U)")).when(romhandler).getRomEntry();
        doReturn(mock(Gen5TextHandler.class)).when(romhandler).getTextHandler();
        doNothing().when(romhandler).setTrainers(trainerCap.capture());
        ArrayList<Trainer> originalTrainer;

        // **************************
        // Test offensive selection
        // **************************
        originalTrainer = new ArrayList();
        for (Trainer t : romhandler.getTrainers()) {
            originalTrainer.add(new Trainer(t));
        }
        romhandler.randomizeTrainerPokes(false, false, false, false, false, false, true, true,
                false, false, 0);
        ArrayList<Trainer> newTrainers = trainerCap.getValue();
        // Get gym1Type
        for (Trainer t : newTrainers.stream().filter(t -> t.getTag() != null)
                .filter(t -> t.getTag().equals("GYM1")).collect(Collectors.toList())) {
            for (TrainerPokemon tp : t.getPokemon()) {
                // Initialize the set
                if (gym1Type.size() == 0) {
                    gym1Type.add(tp.pokemon.primaryType);
                    if (tp.pokemon.secondaryType != null) {
                        gym1Type.add(tp.pokemon.secondaryType);
                    }
                }
                // Only keep the shared type
                else {
                    HashSet<Type> intersect = new HashSet<Type>();
                    intersect.add(tp.pokemon.primaryType);
                    if (tp.pokemon.secondaryType != null) {
                        intersect.add(tp.pokemon.secondaryType);
                    }
                    gym1Type.retainAll(intersect);
                }
            }
        }
        // Check CHILI, CRESS, CILAN against starters
        // Check CHILI, CRESS, CILAN share 1 type with GYM1
        // Reverse order so tagged trainers go last
        for (int i = newTrainers.size() - 1; i >= 0; i--) {
            Trainer newT = newTrainers.get(i);
            Trainer oldT = originalTrainer.get(i);
            assertTrue("Trainer did not match name. Make sure the trainer list is ordered the same",
                    newT.getName().equals(oldT.getName()));
            if (newT.getTag() != null) {
                // CHILI fights the first starter (index 0)
                if (newT.getTag().equals("CHILI")) {
                    // Electric-Steel like Magnemite should fight Ground
                    assertTrue("No GROUND type found for CHILI",
                            newT.getPokemon().stream()
                                    .anyMatch(tp -> tp.pokemon.primaryType == Type.GROUND
                                            || tp.pokemon.secondaryType == Type.GROUND));
                    // Find out how many pokemon share a type with GYM1, subtract it from the total
                    // number of pokemon on the team, and ensure either 0 or 1 pokemon don't share
                    // the type. 0 is possible due to the shared type and trump type
                    // being the same or a dual-typed pokemon sharing one from each.
                    assertTrue("More than 1 CHILI pokemon did not match the GYM1 type",
                            newT.getPokemon().size() - newT.getPokemon().stream()
                                    .filter(tp -> gym1Type.contains(tp.pokemon.primaryType)
                                            || gym1Type.contains(tp.pokemon.secondaryType))
                                    .count() < 2);
                }
                // CRESS fights the second starter (index 1)
                else if (newT.getTag().equals("CRESS")) {
                    // Pure Fire type like Ponyta should fight random weakness
                    assertTrue("No type found that is in STRONG_AGAINST.get(Type.FIRE) for CRESS",
                            newT.getPokemon().stream()
                                    .anyMatch(tp -> Type.STRONG_AGAINST.get(Type.FIRE)
                                            .contains(tp.pokemon.primaryType)
                                            || Type.STRONG_AGAINST.get(Type.FIRE)
                                                    .contains(tp.pokemon.secondaryType)));
                    // Find out how many pokemon share a type with GYM1, subtract it from the total
                    // number of pokemon on the team, and ensure either 0 or 1 pokemon don't share
                    // the type. 0 is possible due to the shared type and trump type
                    // being the same or a dual-typed pokemon sharing one from each.
                    assertTrue("More than 1 CRESS pokemon did not match the GYM1 type",
                            newT.getPokemon().size() - newT.getPokemon().stream()
                                    .filter(tp -> gym1Type.contains(tp.pokemon.primaryType)
                                            || gym1Type.contains(tp.pokemon.secondaryType))
                                    .count() < 2);
                }
                // CILAN fights the last starter (index 2)
                else if (newT.getTag().equals("CILAN")) {
                    // Pure Normal type like Rattata should fight Fighting
                    assertTrue("No FIGHTING type found for CILAN",
                            newT.getPokemon().stream()
                                    .anyMatch(tp -> tp.pokemon.primaryType == Type.FIGHTING
                                            || tp.pokemon.secondaryType == Type.FIGHTING));
                    // Find out how many pokemon share a type with GYM1, subtract it from the total
                    // number of pokemon on the team, and ensure either 0 or 1 pokemon don't share
                    // the type. 0 is possible due to the shared type and trump type
                    // being the same or a dual-typed pokemon sharing one from each.
                    assertTrue("More than 1 CILAN pokemon did not match the GYM1 type",
                            newT.getPokemon().size() - newT.getPokemon().stream()
                                    .filter(tp -> gym1Type.contains(tp.pokemon.primaryType)
                                            || gym1Type.contains(tp.pokemon.secondaryType))
                                    .count() < 2);
                }
            }
            Collections.sort(newT.getPokemon(),
                    (o1, o2) -> o1.getNickname().compareTo(o2.getNickname()));
            Collections.sort(oldT.getPokemon(),
                    (o1, o2) -> o1.getNickname().compareTo(o2.getNickname()));
            for (int j = 0; j < newT.getPokemon().size(); j++) {
                TrainerPokemon newTp = newT.getPokemon().get(j);
                TrainerPokemon oldTp = oldT.getPokemon().get(j);
                // Initialize the set or check the value
                if (pokemonSwap.containsKey(oldTp.pokemon)) {
                    Pokemon cached = pokemonSwap.get(oldTp.pokemon);
                    if (newT.getTag() == null) {
                        assertTrue("Pokemon did not match the replacement - " + oldTp.pokemon.number
                                + " gave " + cached.number + " but newTp was "
                                + newTp.pokemon.number, cached.equals(newTp.pokemon));
                    } else {
                        // Only tagged teams can ignore global swap
                        switch (newT.getTag()) {
                            case "GYM1":
                                assertTrue(
                                        "Pokemon did not match the replacement - "
                                                + oldTp.pokemon.number + " gave " + cached.number
                                                + " but newTp was " + newTp.pokemon.number
                                                + " and type did not match GYM1",
                                        cached.equals(newTp.pokemon)
                                                || gym1Type.contains(newTp.pokemon.primaryType)
                                                || gym1Type.contains(newTp.pokemon.secondaryType));
                                break;
                            case "CHILI":
                                assertTrue(
                                        "Pokemon did not match the replacement - "
                                                + oldTp.pokemon.number + " gave " + cached.number
                                                + " but newTp was " + newTp.pokemon.number
                                                + " and type did not match GYM1 or GROUND",
                                        cached.equals(newTp.pokemon)
                                                || gym1Type.contains(newTp.pokemon.primaryType)
                                                || gym1Type.contains(newTp.pokemon.secondaryType)
                                                || newTp.pokemon.primaryType == Type.GROUND
                                                || newTp.pokemon.secondaryType == Type.GROUND);
                                break;
                            case "CRESS":
                                assertTrue("Pokemon did not match the replacement - "
                                        + oldTp.pokemon.number + " gave " + cached.number
                                        + " but newTp was " + newTp.pokemon.number
                                        + " and type did not match GYM1 or STRONG_AGAINST.get(Type.FIRE)",
                                        cached.equals(newTp.pokemon)
                                                || gym1Type.contains(newTp.pokemon.primaryType)
                                                || gym1Type.contains(newTp.pokemon.secondaryType)
                                                || Type.STRONG_AGAINST.get(Type.FIRE)
                                                        .contains(newTp.pokemon.primaryType)
                                                || Type.STRONG_AGAINST.get(Type.FIRE)
                                                        .contains(newTp.pokemon.secondaryType));
                                break;
                            case "CILAN":
                                assertTrue(
                                        "Pokemon did not match the replacement - "
                                                + oldTp.pokemon.number + " gave " + cached.number
                                                + " but newTp was " + newTp.pokemon.number
                                                + " and type did not match GYM1 or FIGHTING",
                                        cached.equals(newTp.pokemon)
                                                || gym1Type.contains(newTp.pokemon.primaryType)
                                                || gym1Type.contains(newTp.pokemon.secondaryType)
                                                || newTp.pokemon.primaryType == Type.FIGHTING
                                                || newTp.pokemon.secondaryType == Type.FIGHTING);
                                break;
                        }
                    }
                } else {
                    pokemonSwap.put(oldTp.pokemon, newTp.pokemon);
                }
            }
        }
        // **************************
        // Test defensive selection
        // **************************
        pokemonSwap = new HashMap<Pokemon, Pokemon>(Gen5Constants.pokemonCount + 1);
        originalTrainer = new ArrayList();
        for (Trainer t : romhandler.getTrainers()) {
            originalTrainer.add(new Trainer(t));
        }
        romhandler.randomizeTrainerPokes(false, false, false, false, true, false, true, true, false,
                false, 0);
        newTrainers = trainerCap.getValue();
        // Get gym1Type
        for (Trainer t : newTrainers.stream().filter(t -> t.getTag() != null)
                .filter(t -> t.getTag().equals("GYM1")).collect(Collectors.toList())) {
            for (TrainerPokemon tp : t.getPokemon()) {
                // Initialize the set
                if (gym1Type.size() == 0) {
                    gym1Type.add(tp.pokemon.primaryType);
                    if (tp.pokemon.secondaryType != null) {
                        gym1Type.add(tp.pokemon.secondaryType);
                    }
                }
                // Only keep the shared type
                else {
                    HashSet<Type> intersect = new HashSet<Type>();
                    intersect.add(tp.pokemon.primaryType);
                    if (tp.pokemon.secondaryType != null) {
                        intersect.add(tp.pokemon.secondaryType);
                    }
                    gym1Type.retainAll(intersect);
                }
            }
        }

        // Check CHILI, CRESS, CILAN against starters
        // Check CHILI, CRESS, CILAN share 1 type with GYM1
        // Reverse order so tagged trainers go last
        for (int i = newTrainers.size() - 1; i >= 0; i--) {
            Trainer newT = newTrainers.get(i);
            Trainer oldT = originalTrainer.get(i);
            assertTrue("Trainer did not match name. Make sure the trainer list is ordered the same",
                    newT.getName().equals(oldT.getName()));
            if (newT.getTag() != null) {
                // CHILI fights the first starter (index 0)
                if (newT.getTag().equals("CHILI")) {
                    // Electric-Steel like Magnemite should fight Electric
                    assertTrue("No ELECTRIC type found for CHILI",
                            newT.getPokemon().stream()
                                    .anyMatch(tp -> tp.pokemon.primaryType == Type.ELECTRIC
                                            || tp.pokemon.secondaryType == Type.ELECTRIC));
                    // Find out how many pokemon share a type with GYM1, subtract it from the total
                    // number of pokemon on the team, and ensure either 0 or 1 pokemon don't share
                    // the type. 0 is possible due to the shared type and trump type
                    // being the same or a dual-typed pokemon sharing one from each.
                    assertTrue("More than 1 CHILI pokemon did not match the GYM1 type",
                            newT.getPokemon().size() - newT.getPokemon().stream()
                                    .filter(tp -> gym1Type.contains(tp.pokemon.primaryType)
                                            || gym1Type.contains(tp.pokemon.secondaryType))
                                    .count() < 2);
                }
                // CRESS fights the second starter (index 1)
                else if (newT.getTag().equals("CRESS")) {
                    // Pure Fire type like Ponyta should fight random weakness
                    assertTrue("No type found that is in RESISTANT_TO.get(Type.FIRE) for CRESS",
                            newT.getPokemon().stream()
                                    .anyMatch(tp -> Type.RESISTANT_TO.get(Type.FIRE)
                                            .contains(tp.pokemon.primaryType)
                                            || Type.RESISTANT_TO.get(Type.FIRE)
                                                    .contains(tp.pokemon.secondaryType)));
                    // Find out how many pokemon share a type with GYM1, subtract it from the total
                    // number of pokemon on the team, and ensure either 0 or 1 pokemon don't share
                    // the type. 0 is possible due to the shared type and trump type
                    // being the same or a dual-typed pokemon sharing one from each.
                    assertTrue("More than 1 CRESS pokemon did not match the GYM1 type",
                            newT.getPokemon().size() - newT.getPokemon().stream()
                                    .filter(tp -> gym1Type.contains(tp.pokemon.primaryType)
                                            || gym1Type.contains(tp.pokemon.secondaryType))
                                    .count() < 2);
                }
                // CILAN fights the last starter (index 2)
                else if (newT.getTag().equals("CILAN")) {
                    // Pure Normal type like Rattata should fight random weakness
                    assertTrue("No type found that is in combineMap.get(Type.NORMAL) for CILAN",
                            newT.getPokemon().stream()
                                    .anyMatch(tp -> Type.getCombinedResistanceMap().get(Type.NORMAL)
                                            .contains(tp.pokemon.primaryType)
                                            || Type.getCombinedResistanceMap().get(Type.NORMAL)
                                                    .contains(tp.pokemon.secondaryType)));
                    // Find out how many pokemon share a type with GYM1, subtract it from the total
                    // number of pokemon on the team, and ensure either 0 or 1 pokemon don't share
                    // the type. 0 is possible due to the shared type and trump type
                    // being the same or a dual-typed pokemon sharing one from each.
                    assertTrue("More than 1 CILAN pokemon did not match the GYM1 type",
                            newT.getPokemon().size() - newT.getPokemon().stream()
                                    .filter(tp -> gym1Type.contains(tp.pokemon.primaryType)
                                            || gym1Type.contains(tp.pokemon.secondaryType))
                                    .count() < 2);
                }
            }
            Collections.sort(newT.getPokemon(),
                    (o1, o2) -> o1.getNickname().compareTo(o2.getNickname()));
            Collections.sort(oldT.getPokemon(),
                    (o1, o2) -> o1.getNickname().compareTo(o2.getNickname()));
            for (int j = 0; j < newT.getPokemon().size(); j++) {
                TrainerPokemon newTp = newT.getPokemon().get(j);
                TrainerPokemon oldTp = oldT.getPokemon().get(j);
                // Initialize the set or check the value
                if (pokemonSwap.containsKey(oldTp.pokemon)) {
                    Pokemon cached = pokemonSwap.get(oldTp.pokemon);
                    if (newT.getTag() == null) {
                        assertTrue("Pokemon did not match the replacement - " + oldTp.pokemon.number
                                + " gave " + cached.number + " but newTp was "
                                + newTp.pokemon.number, cached.equals(newTp.pokemon));
                    } else {
                        // Only tagged teams can ignore global swap
                        switch (newT.getTag()) {
                            case "GYM1":
                                assertTrue(
                                        "Pokemon did not match the replacement - "
                                                + oldTp.pokemon.number + " gave " + cached.number
                                                + " but newTp was " + newTp.pokemon.number
                                                + " and type did not match GYM1",
                                        cached.equals(newTp.pokemon)
                                                || gym1Type.contains(newTp.pokemon.primaryType)
                                                || gym1Type.contains(newTp.pokemon.secondaryType));
                                break;
                            case "CHILI":
                                assertTrue(
                                        "Pokemon did not match the replacement - "
                                                + oldTp.pokemon.number + " gave " + cached.number
                                                + " but newTp was " + newTp.pokemon.number
                                                + " and type did not match GYM1 or ELECTRIC",
                                        cached.equals(newTp.pokemon)
                                                || gym1Type.contains(newTp.pokemon.primaryType)
                                                || gym1Type.contains(newTp.pokemon.secondaryType)
                                                || newTp.pokemon.primaryType == Type.ELECTRIC
                                                || newTp.pokemon.secondaryType == Type.ELECTRIC);
                                break;
                            case "CRESS":
                                assertTrue("Pokemon did not match the replacement - "
                                        + oldTp.pokemon.number + " gave " + cached.number
                                        + " but newTp was " + newTp.pokemon.number
                                        + " and type did not match GYM1 or RESISTANT_TO.get(Type.FIRE)",
                                        cached.equals(newTp.pokemon)
                                                || gym1Type.contains(newTp.pokemon.primaryType)
                                                || gym1Type.contains(newTp.pokemon.secondaryType)
                                                || Type.RESISTANT_TO.get(Type.FIRE)
                                                        .contains(newTp.pokemon.primaryType)
                                                || Type.RESISTANT_TO.get(Type.FIRE)
                                                        .contains(newTp.pokemon.secondaryType));
                                break;
                            case "CILAN":
                                assertTrue("Pokemon did not match the replacement - "
                                        + oldTp.pokemon.number + " gave " + cached.number
                                        + " but newTp was " + newTp.pokemon.number
                                        + " and type did not match GYM1 or combineMap.get(Type.NORMAL)",
                                        cached.equals(newTp.pokemon)
                                                || gym1Type.contains(newTp.pokemon.primaryType)
                                                || gym1Type.contains(newTp.pokemon.secondaryType)
                                                || Type.getCombinedResistanceMap().get(Type.NORMAL)
                                                        .contains(newTp.pokemon.primaryType)
                                                || Type.getCombinedResistanceMap().get(Type.NORMAL)
                                                        .contains(newTp.pokemon.secondaryType));
                                break;
                        }
                    }
                } else {
                    pokemonSwap.put(oldTp.pokemon, newTp.pokemon);
                }
            }
        }
    }

    /**
     * Check that negative abilities like Normalize are removed
     */
    @Test
    public void TestNegativeAbilityRemoved() {
        RomHandler romhandler = spy(new Gen5RomHandler(new Random()));
        resetDataModel(romhandler);
        romhandler.setPokemonPool(null, null);
        romhandler.randomizeAbilities(false, false, false, true);
        for (Pokemon p : pokemonList) {
            assertFalse("" + p.getAbility1() + " was in negativeAbilities, but still found",
                    GlobalConstants.negativeAbilities.contains(p.getAbility1()));
            assertFalse("" + p.getAbility2() + " was in negativeAbilities, but still found",
                    GlobalConstants.negativeAbilities.contains(p.getAbility2()));
        }
    }

    /**
     * When replacing statics, the elemental monkies should either be a type that is superior to the
     * Striaton gym, or covers a weakness of the starter
     * 
     * @throws IOException
     */
    @Test
    public void TestMonkeyStaticReplacementCoversWeakness() throws IOException {
        Gen5RomHandler romhandler = spy(new Gen5RomHandler(new Random()));
        resetDataModel(romhandler);
        startersList.get(0).primaryType = Type.ELECTRIC;
        startersList.get(0).secondaryType = Type.STEEL;
        startersList.get(1).primaryType = Type.FIRE;
        startersList.get(2).primaryType = Type.NORMAL;
        doReturn(Gen5RomHandler.getRomFromSupportedRom("Black (U)")).when(romhandler).getRomEntry();
        doReturn(mock(Gen5TextHandler.class)).when(romhandler).getTextHandler();
        doReturn(pokemonList.subList(511, 516)).when(romhandler).getStaticPokemon();
        doNothing().when(romhandler).setTrainers(trainerCap.capture());
        doReturn(true).when(romhandler).setStaticPokemon(pokemonCap.capture());

        // **************************
        // Test general selection
        // **************************
        romhandler.randomizeStaticPokemon(false);

        // Grass monkey (first element in static list)
        // Thus the first replacement in the capture
        Pokemon pkmn = pokemonCap.getValue().get(0);

        // Gather both types of the replacement into a list
        ArrayList<Type> types = new ArrayList<Type>();
        types.add(pkmn.primaryType);
        if (pkmn.secondaryType != null) {
            types.add(pkmn.secondaryType);
        }

        // This one needs to cover a random fire type weakness, which gives us a wide selection
        ArrayList<Type> weaknessTypes = new ArrayList<Type>();
        Type.STRONG_AGAINST.get(startersList.get(1).primaryType).forEach(t -> {
            // Get the list of weaknesses for that type and add them to acceptable types
            weaknessTypes.addAll(Type.STRONG_AGAINST.get(t));
        });

        weaknessTypes.addAll(Type.STRONG_AGAINST.get(Type.ROCK));
        assertTrue("Grass monkey replacement type was not found in weakness list for starter 1",
                !Collections.disjoint(weaknessTypes, types));

        // Fire monkey (third element in the static list)
        // Thus the third replacement in the capture
        pkmn = pokemonCap.getValue().get(2);

        // Gather both types of the replacement into a list
        types = new ArrayList<Type>();
        types.add(pkmn.primaryType);
        if (pkmn.secondaryType != null) {
            types.add(pkmn.secondaryType);
        }

        // Check if the weakness list for FIGHTING (which is the only thing Normal type is weak to)
        // includes
        // at least ine of the replacement types
        assertTrue("Fire monkey replacement type was not found in weakness list for starter 2",
                !Collections.disjoint(Type.STRONG_AGAINST.get(Type.FIGHTING), types));

        // Water monkey (fifth element in the static list)
        // Thus the fifth replacement in the capture
        pkmn = pokemonCap.getValue().get(4);

        // Gather both types of the replacement into a list
        types = new ArrayList<Type>();
        types.add(pkmn.primaryType);
        if (pkmn.secondaryType != null) {
            types.add(pkmn.secondaryType);
        }

        // Check if the weakness list for GROUND (which is the only shared weakness for Electric and
        // Steel) includes
        // at least one of the replacement's types
        assertTrue("Water monkey replacement type was not found in weakness list for starter 0",
                !Collections.disjoint(Type.STRONG_AGAINST.get(Type.GROUND), types));


        // **************************
        // Test Striaton offense selection
        // **************************
        romhandler.randomizeTrainerPokes(false, false, false, false, false, false, false, true,
                false, false, 0);
        romhandler.randomizeStaticPokemon(false);
        Map<String, Type> taggedTypes = romhandler.getTaggedGroupTypes();

        // Grass monkey (first element in static list)
        // Thus the first replacement in the capture
        pkmn = pokemonCap.getValue().get(0);

        // Gather both types of the replacement into a list
        types = new ArrayList<Type>();
        types.add(pkmn.primaryType);
        if (pkmn.secondaryType != null) {
            types.add(pkmn.secondaryType);
        }

        // Add in all weaknesses of CRESS
        Type cressType = taggedTypes.get("CRESS");
        ArrayList<Type> acceptableTypes = new ArrayList<Type>();
        acceptableTypes.addAll(Type.STRONG_AGAINST.get(cressType));
        assertTrue(
                "Grass monkey replacement type " + pkmn.primaryType + "/" + pkmn.secondaryType
                        + " was not found in weakness list for CRESS " + cressType.camelCase(),
                acceptableTypes.contains(pkmn.primaryType)
                        || acceptableTypes.contains(pkmn.secondaryType));

        // Fire monkey (third element in the static list)
        // Thus the third replacement in the capture
        pkmn = pokemonCap.getValue().get(2);

        // Gather both types of the replacement into a list
        types = new ArrayList<Type>();
        types.add(pkmn.primaryType);
        if (pkmn.secondaryType != null) {
            types.add(pkmn.secondaryType);
        }

        // Add in all weakneses of CILAN
        Type cilanType = taggedTypes.get("CILAN");
        acceptableTypes = new ArrayList<Type>();
        acceptableTypes.addAll(Type.STRONG_AGAINST.get(cilanType));
        assertTrue(
                "Fire monkey replacement type " + pkmn.primaryType + "/" + pkmn.secondaryType
                        + " was not found in weakness list for CILAN " + cilanType.camelCase(),
                acceptableTypes.contains(pkmn.primaryType)
                        || acceptableTypes.contains(pkmn.secondaryType));

        // Water monkey (fifth element in the static list)
        // Thus the fifth replacement in the capture
        pkmn = pokemonCap.getValue().get(4);

        // Gather both types of the replacement into a list
        types = new ArrayList<Type>();
        types.add(pkmn.primaryType);
        if (pkmn.secondaryType != null) {
            types.add(pkmn.secondaryType);
        }

        // Add in all weakneses of CHILI
        Type chiliType = taggedTypes.get("CHILI");
        acceptableTypes = new ArrayList<Type>();
        acceptableTypes.addAll(Type.STRONG_AGAINST.get(chiliType));
        assertTrue(
                "Water monkey replacement type " + pkmn.primaryType + "/" + pkmn.secondaryType
                        + " was not found in weakness list for CHILI " + chiliType.camelCase(),
                acceptableTypes.contains(pkmn.primaryType)
                        || acceptableTypes.contains(pkmn.secondaryType));

        // **************************
        // Test Striaton defense selection
        // **************************
        romhandler.randomizeTrainerPokes(false, false, false, false, true, false, false, true,
                false, false, 0);
        romhandler.randomizeStaticPokemon(false);
        taggedTypes = romhandler.getTaggedGroupTypes();


        // Grass monkey (first element in static list)
        // Thus the first replacement in the capture
        pkmn = pokemonCap.getValue().get(0);

        // Gather both types of the replacement into a list
        types = new ArrayList<Type>();
        types.add(pkmn.primaryType);
        if (pkmn.secondaryType != null) {
            types.add(pkmn.secondaryType);
        }

        // Add in all weaknesses of CRESS
        cressType = taggedTypes.get("CRESS");
        acceptableTypes = new ArrayList<Type>();
        acceptableTypes.addAll(Type.STRONG_AGAINST.get(cressType));
        assertTrue(
                "Grass monkey replacement type " + pkmn.primaryType + "/" + pkmn.secondaryType
                        + " was not found in weakness list for CRESS " + cressType.camelCase(),
                acceptableTypes.contains(pkmn.primaryType)
                        || acceptableTypes.contains(pkmn.secondaryType));

        // Fire monkey (third element in the static list)
        // Thus the third replacement in the capture
        pkmn = pokemonCap.getValue().get(2);

        // Gather both types of the replacement into a list
        types = new ArrayList<Type>();
        types.add(pkmn.primaryType);
        if (pkmn.secondaryType != null) {
            types.add(pkmn.secondaryType);
        }

        // Add in all weakneses of CILAN
        cilanType = taggedTypes.get("CILAN");
        acceptableTypes = new ArrayList<Type>();
        acceptableTypes.addAll(Type.STRONG_AGAINST.get(cilanType));
        assertTrue(
                "Fire monkey replacement type " + pkmn.primaryType + "/" + pkmn.secondaryType
                        + " was not found in weakness list for CILAN " + cilanType.camelCase(),
                acceptableTypes.contains(pkmn.primaryType)
                        || acceptableTypes.contains(pkmn.secondaryType));

        // Water monkey (fifth element in the static list)
        // Thus the fifth replacement in the capture
        pkmn = pokemonCap.getValue().get(4);

        // Gather both types of the replacement into a list
        types = new ArrayList<Type>();
        types.add(pkmn.primaryType);
        if (pkmn.secondaryType != null) {
            types.add(pkmn.secondaryType);
        }

        // Add in all weakneses of CHILI
        chiliType = taggedTypes.get("CHILI");
        acceptableTypes = new ArrayList<Type>();
        acceptableTypes.addAll(Type.STRONG_AGAINST.get(chiliType));
        assertTrue(
                "Water monkey replacement type " + pkmn.primaryType + "/" + pkmn.secondaryType
                        + " was not found in weakness list for CHILI " + chiliType.camelCase(),
                acceptableTypes.contains(pkmn.primaryType)
                        || acceptableTypes.contains(pkmn.secondaryType));
    }

    @Test
    public void TestMinimumEvos() {
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        romhandler.setPokemonPool(null, null);
        romhandler.randomStarterPokemon(false, false, false, 999, 0, false, null, null, null);
        boolean pokemonWithZeroEvo = false, pokemonWithOneEvo = false, pokemonWithTwoEvo = false;
        for (Pokemon pk : romhandler.getStarterPokes()) {
            int evoLength = pk.evolutionChainSize();
            if (evoLength == 1) {
                pokemonWithZeroEvo = true;
            } else if (evoLength == 2) {
                pokemonWithOneEvo = true;
            } else if (evoLength > 2) {
                pokemonWithTwoEvo = true;
            }

            // End loop if we have all the conditions met
            if (pokemonWithOneEvo && pokemonWithTwoEvo && pokemonWithZeroEvo) {
                break;
            }
        }
        assertTrue(
                "Matches should be true: zeroEvo - " + pokemonWithZeroEvo + ", oneEvo - "
                        + pokemonWithOneEvo + ", twoEvo - " + pokemonWithTwoEvo,
                pokemonWithZeroEvo && pokemonWithOneEvo && pokemonWithTwoEvo);
        pokemonWithZeroEvo = false;
        pokemonWithOneEvo = false;
        pokemonWithTwoEvo = false;
        romhandler.clearStarterPokes();
        romhandler.randomStarterPokemon(false, false, false, 999, 1, false, null, null, null);
        for (Pokemon pk : romhandler.getStarterPokes()) {
            int evoLength = pk.evolutionChainSize();
            if (evoLength == 1) {
                pokemonWithZeroEvo = true;
            } else if (evoLength == 2) {
                pokemonWithOneEvo = true;
            } else if (evoLength > 2) {
                pokemonWithTwoEvo = true;
            }
        }
        assertTrue(
                "Matches should be false: zeroEvo - " + pokemonWithZeroEvo
                        + "\nMatches should be true: oneEvo - " + pokemonWithOneEvo + ", twoEvo - "
                        + pokemonWithTwoEvo,
                !pokemonWithZeroEvo && pokemonWithOneEvo && pokemonWithTwoEvo);
        pokemonWithZeroEvo = false;
        pokemonWithOneEvo = false;
        pokemonWithTwoEvo = false;
        romhandler.clearStarterPokes();
        romhandler.randomStarterPokemon(false, false, false, 999, 2, false, null, null, null);
        for (Pokemon pk : romhandler.getStarterPokes()) {
            int evoLength = pk.evolutionChainSize();
            if (evoLength == 1) {
                pokemonWithZeroEvo = true;
            } else if (evoLength == 2) {
                pokemonWithOneEvo = true;
            } else if (evoLength > 2) {
                pokemonWithTwoEvo = true;
            }
        }
        assertTrue("Matches should be false: zeroEvo - " + pokemonWithZeroEvo + ", oneEvo - "
                + pokemonWithOneEvo + "\nMatches should be true: twoEvo - " + pokemonWithTwoEvo,
                !pokemonWithZeroEvo && !pokemonWithOneEvo && pokemonWithTwoEvo);
    }

    @Test
    public void TestExactEvos() {
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        romhandler.setPokemonPool(null, null);
        romhandler.randomStarterPokemon(false, false, false, 999, 0, true, null, null, null);
        boolean pokemonWithZeroEvo = false, pokemonWithOneEvo = false, pokemonWithTwoEvo = false;
        for (Pokemon pk : romhandler.getStarterPokes()) {
            int evoLength = pk.evolutionChainSize();
            if (evoLength == 1) {
                pokemonWithZeroEvo = true;
            } else if (evoLength == 2) {
                pokemonWithOneEvo = true;
            } else if (evoLength > 2) {
                pokemonWithTwoEvo = true;
            }
        }
        assertTrue(
                "Matches should be true: zeroEvo - " + pokemonWithZeroEvo
                        + "\nMatches should be false: oneEvo - " + pokemonWithOneEvo + ", twoEvo - "
                        + pokemonWithTwoEvo,
                pokemonWithZeroEvo && !pokemonWithOneEvo && !pokemonWithTwoEvo);
        pokemonWithZeroEvo = false;
        pokemonWithOneEvo = false;
        pokemonWithTwoEvo = false;
        romhandler.clearStarterPokes();
        romhandler.randomStarterPokemon(false, false, false, 999, 1, true, null, null, null);
        for (Pokemon pk : romhandler.getStarterPokes()) {
            int evoLength = pk.evolutionChainSize();
            if (evoLength == 1) {
                pokemonWithZeroEvo = true;
            } else if (evoLength == 2) {
                pokemonWithOneEvo = true;
            } else if (evoLength > 2) {
                pokemonWithTwoEvo = true;
            }
        }
        assertTrue("Matches should be false: zeroEvo - " + pokemonWithZeroEvo + ", twoEvo - "
                + pokemonWithTwoEvo + "\nMatches should be true: oneEvo - " + pokemonWithOneEvo,
                !pokemonWithZeroEvo && pokemonWithOneEvo && !pokemonWithTwoEvo);
        pokemonWithZeroEvo = false;
        pokemonWithOneEvo = false;
        pokemonWithTwoEvo = false;
        boolean pokemonWithMoreThanTwoEvo = false;
        romhandler.clearStarterPokes();
        romhandler.randomStarterPokemon(false, false, false, 999, 2, true, null, null, null);
        for (Pokemon pk : romhandler.getStarterPokes()) {
            int evoLength = pk.evolutionChainSize();
            if (evoLength == 1) {
                pokemonWithZeroEvo = true;
            } else if (evoLength == 2) {
                pokemonWithOneEvo = true;
            } else if (evoLength == 3) {
                pokemonWithTwoEvo = true;
            } else if (evoLength > 3) {
                pokemonWithMoreThanTwoEvo = true;
            }
        }
        assertTrue(
                "Matches should be false: zeroEvo - " + pokemonWithZeroEvo + ", oneEvo - "
                        + pokemonWithOneEvo + ", moreThanTwo - " + pokemonWithMoreThanTwoEvo
                        + "\nMatches should be true: twoEvo - " + pokemonWithTwoEvo,
                !pokemonWithZeroEvo && !pokemonWithOneEvo && pokemonWithTwoEvo
                        && !pokemonWithMoreThanTwoEvo);
    }

    /**
     * All trainers are type themed
     */
    @Test
    public void TestTypeTheme() {
        HashSet<Type> trainerType = new HashSet<Type>();
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        romhandler.randomizeTrainerPokes(false, false, false, false, false, true, false, false,
                false, false, 0);
        for (Trainer t : romhandler.getTrainers()) {
            for (TrainerPokemon tp : t.getPokemon()) {
                // Initialize the set
                if (trainerType.size() == 0) {
                    trainerType.add(tp.pokemon.primaryType);
                    if (tp.pokemon.secondaryType != null) {
                        trainerType.add(tp.pokemon.secondaryType);
                    }
                }
                // Only keep the shared type
                else {
                    HashSet<Type> intersect = new HashSet<Type>();
                    intersect.add(tp.pokemon.primaryType);
                    if (tp.pokemon.secondaryType != null) {
                        intersect.add(tp.pokemon.secondaryType);
                    }
                    trainerType.retainAll(intersect);
                }
            }
            assertTrue("More than 2 types found - " + Arrays.toString(trainerType.toArray()),
                    trainerType.size() < 3);
        }
    }

    /**
     * Only tagged trainers (like GYM, UBER) are type themed
     */
    @Test
    public void TestGymTypeTheme() {
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        HashSet<Type> trainerType;
        romhandler.randomizeTrainerPokes(false, false, false, false, false, false, false, true,
                false, false, 0);
        for (Trainer t : romhandler.getTrainers()) {
            // Skip anyone that is not tagged
            // Skip CHILI, CRESS, CILAN due to unique requirements to have different types
            if (t.getTag() != null
                    && !Arrays.asList("CHILI", "CRESS", "CILAN").contains(t.getTag())) {
                trainerType = new HashSet<Type>();
                for (TrainerPokemon tp : t.getPokemon()) {
                    // Initialize the set
                    if (trainerType.size() == 0) {
                        trainerType.add(tp.pokemon.primaryType);
                        if (tp.pokemon.secondaryType != null) {
                            trainerType.add(tp.pokemon.secondaryType);
                        }
                    }
                    // Only keep the shared type
                    else {
                        HashSet<Type> intersect = new HashSet<Type>();
                        intersect.add(tp.pokemon.primaryType);
                        if (tp.pokemon.secondaryType != null) {
                            intersect.add(tp.pokemon.secondaryType);
                        }
                        trainerType.retainAll(intersect);
                    }
                }
                // Test for 2 since there could be only 1 pokemon with 2 types, or all pokemon
                // share the same 2 types even though the gym only requires 1 of those types
                assertTrue("More than 2 types found - " + Arrays.toString(trainerType.toArray()),
                        trainerType.size() < 3);

                // Test to make sure at least 1 type was found
                assertTrue("Less than 1 type found - " + Arrays.toString(trainerType.toArray()),
                        trainerType.size() > 0);
            }
        }
    }

    /**
     * Test that starters are filtered by type when a type is provided, or ignores type filtering
     * when the types argument is null
     */
    @Test
    public void TestStartersTypeRestriction() {
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        romhandler.setPokemonPool(null, null);
        // Test null first
        romhandler.randomStarterPokemon(false, false, false, 999, 0, false, null, null, null);
        assertTrue("Starters list did not contain all available pokemon",
                // Subtract due to randomStarterPokemon doing a pop operation and reducing by 1
                romhandler.getStarterPokes().size() == romhandler.getMainPokemonList().size() - 1);

        // Test with 1 type
        romhandler.clearStarterPokes();
        romhandler.randomStarterPokemon(false, false, false, 999, 0, false, new Type[] {Type.FIRE},
                null, null);
        assertTrue("Starters list did not contain only FIRE types",
                romhandler.getStarterPokes().stream().allMatch(
                        pk -> pk.primaryType == Type.FIRE || pk.secondaryType == Type.FIRE));

        // Test with 3 types
        romhandler.clearStarterPokes();
        ArrayList<Type> typesArr =
                new ArrayList<Type>(Arrays.asList(Type.FIRE, Type.BUG, Type.DARK));
        romhandler.randomStarterPokemon(false, false, false, 999, 0, false,
                typesArr.toArray(new Type[0]), null, null);
        assertTrue("Starters list did not contain only FIRE, BUG, and DARK types",
                romhandler.getStarterPokes().stream()
                        .allMatch(pk -> typesArr.contains(pk.primaryType)
                                || typesArr.contains(pk.secondaryType)));
    }

    @Test
    public void TestSETriangleStarterOrder() {
        // TODO: Requires testing function in Randomizer
    }

    /**
     * Pokemon are always replaced with the same thing
     */
    @Test
    public void TestGlobalSwap() {
        HashMap<Pokemon, Pokemon> pokemonSwap =
                new HashMap<Pokemon, Pokemon>(Gen5Constants.pokemonCount + 1);
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        doNothing().when(romhandler).setTrainers(trainerCap.capture());
        ArrayList<Trainer> originalTrainer = new ArrayList();
        for (Trainer t : romhandler.getTrainers()) {
            originalTrainer.add(new Trainer(t));
        }
        romhandler.randomizeTrainerPokes(false, false, false, false, false, false, true, false,
                false, false, 0);
        for (int i = 0; i < trainerCap.getValue().size(); i++) {
            Trainer newT = trainerCap.getValue().get(i);
            Trainer oldT = originalTrainer.get(i);
            assertTrue("Trainer did not match name. Make sure the trainer list is ordered the same",
                    newT.getName().equals(oldT.getName()));
            Collections.sort(newT.getPokemon(),
                    (o1, o2) -> o1.getNickname().compareTo(o2.getNickname()));
            Collections.sort(oldT.getPokemon(),
                    (o1, o2) -> o1.getNickname().compareTo(o2.getNickname()));
            for (int j = 0; j < newT.getPokemon().size(); j++) {
                TrainerPokemon newTp = newT.getPokemon().get(j);
                TrainerPokemon oldTp = oldT.getPokemon().get(j);
                // Initialize the set
                if (pokemonSwap.containsKey(oldTp.pokemon)) {
                    Pokemon cached = pokemonSwap.get(oldTp.pokemon);
                    assertTrue("Pokemon did not match the replacement - " + oldTp.pokemon.number
                            + " gave " + cached.number + " but newTp was " + newTp.pokemon.number,
                            cached.equals(newTp.pokemon));
                } else {
                    pokemonSwap.put(oldTp.pokemon, newTp.pokemon);
                }
            }
        }
    }

    /**
     * Pokemon are always replaced with the same thing Gyms are stil appropriately type themed
     */
    @Test
    public void TestGlobalSwapGymTypeTheme() {
        HashMap<Pokemon, Pokemon> pokemonSwap =
                new HashMap<Pokemon, Pokemon>(Gen5Constants.pokemonCount + 1);
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        doNothing().when(romhandler).setTrainers(trainerCap.capture());
        ArrayList<Trainer> originalTrainer = new ArrayList();
        HashSet<Type> trainerType = null;
        for (Trainer t : romhandler.getTrainers()) {
            originalTrainer.add(new Trainer(t));
        }
        romhandler.randomizeTrainerPokes(false, false, false, false, false, false, true, true,
                false, false, 0);
        // Reverse order so tagged trainers are last, preventing them from caching the wrong pokemon
        for (int i = trainerCap.getValue().size() - 1; i >= 0; i--) {
            Trainer newT = trainerCap.getValue().get(i);
            Trainer oldT = originalTrainer.get(i);
            assertTrue("Trainer did not match name. Make sure the trainer list is ordered the same",
                    newT.getName().equals(oldT.getName()));
            // Skip CHILI, CRESS, CILAN due to unique requirements to have different types
            if (Arrays.asList("CHILI", "CRESS", "CILAN").contains(newT.getTag())) {
                continue;
            }
            // Only tagged trainers are required to have a single type
            // Everyone else is just required to obey the global swap
            if (newT.getTag() != null) {
                trainerType = new HashSet<Type>();
                for (TrainerPokemon tp : newT.getPokemon()) {
                    // Initialize the set
                    if (trainerType.size() == 0) {
                        trainerType.add(tp.pokemon.primaryType);
                        if (tp.pokemon.secondaryType != null) {
                            trainerType.add(tp.pokemon.secondaryType);
                        }
                    }
                    // Only keep the shared type
                    else {
                        HashSet<Type> intersect = new HashSet<Type>();
                        intersect.add(tp.pokemon.primaryType);
                        if (tp.pokemon.secondaryType != null) {
                            intersect.add(tp.pokemon.secondaryType);
                        }
                        trainerType.retainAll(intersect);
                    }
                }
                // Test for 2 since there could be only 1 pokemon with 2 types, or all pokemon
                // share the same 2 types even though the gym only requires 1 of those types
                assertTrue("More than 2 types found - " + Arrays.toString(trainerType.toArray()),
                        trainerType.size() < 3);

                // Test to make sure at least 1 type was found
                assertTrue("Less than 1 type found - " + Arrays.toString(trainerType.toArray()),
                        trainerType.size() > 0);
            }
            Collections.sort(newT.getPokemon(),
                    (o1, o2) -> o1.getNickname().compareTo(o2.getNickname()));
            Collections.sort(oldT.getPokemon(),
                    (o1, o2) -> o1.getNickname().compareTo(o2.getNickname()));
            for (int j = 0; j < newT.getPokemon().size(); j++) {
                TrainerPokemon newTp = newT.getPokemon().get(j);
                TrainerPokemon oldTp = oldT.getPokemon().get(j);
                // Initialize the set or check the value
                if (pokemonSwap.containsKey(oldTp.pokemon)) {
                    Pokemon cached = pokemonSwap.get(oldTp.pokemon);
                    if (newT.getTag() == null) {
                        assertTrue("Pokemon did not match the replacement - " + oldTp.pokemon.number
                                + " gave " + cached.number + " but newTp was "
                                + newTp.pokemon.number, cached.equals(newTp.pokemon));
                    } else {
                        // Verify trainerType has been initialized
                        assertFalse("Tagged trainer " + newT.getTag()
                                + " did not have types initialized", trainerType == null);
                        // Only tagged teams get permission to override global swap
                        assertTrue(
                                "Pokemon did not match the replacement - " + oldTp.pokemon.number
                                        + " gave " + cached.number + " but newTp was "
                                        + newTp.pokemon.number + " and type was not found",
                                cached.equals(newTp.pokemon)
                                        || trainerType.contains(newTp.pokemon.primaryType)
                                        || trainerType.contains(newTp.pokemon.secondaryType));
                    }
                } else {
                    pokemonSwap.put(oldTp.pokemon, newTp.pokemon);
                }
            }
        }
    }

    /**
     * Verifies RomOptions correctly limits mainPokemonList
     */
    @Test
    public void TestRomOptions() {
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        RomOptions romOptions = new RomOptions();
        romhandler.setPokemonPool(null, romOptions);
        // Sanity check - Should start false and not limit main list
        assertFalse("Rom Options started as true", romOptions.isRandomizeSubset());
        assertTrue("Main Pokemon list was modified by default", romhandler.getMainPokemonList()
                .containsAll(romhandler.getPokemon().subList(1, romhandler.getPokemon().size())));

        romOptions.setRandomizeSubset(true);

        // Valid min and max is respected
        romOptions.setMinimumRandomizablePokemonNumber(4);
        romOptions.setMaximumRandomizablePokemonNumber(romhandler.getPokemon().size() - 4);
        romhandler.setPokemonPool(null, romOptions);
        assertTrue("First element of main Pokemon list does not equal 4th index of pokemonList",
                romhandler.getMainPokemonList().get(0) == romhandler.getPokemon().get(4));
        assertTrue(
                "Last element of main Pokemon list does not equal 4th to last index of pokemonList",
                romhandler.getMainPokemonList().get(romhandler.getMainPokemonList().size()
                        - 1) == romhandler.getPokemon().get(romhandler.getPokemon().size() - 4));

        // Min lower than 0 returns 0
        romOptions.setMinimumRandomizablePokemonNumber(-20);
        romOptions.setMaximumRandomizablePokemonNumber(romhandler.getPokemon().size() - 4);
        romhandler.setPokemonPool(null, romOptions);
        assertTrue("First element of main Pokemon list does not equal 2nd index of pokemonList",
                romhandler.getMainPokemonList().get(0) == romhandler.getPokemon().get(1));
        assertTrue(
                "Last element of main Pokemon list does not equal 4th to last index of pokemonList",
                romhandler.getMainPokemonList().get(romhandler.getMainPokemonList().size()
                        - 1) == romhandler.getPokemon().get(romhandler.getPokemon().size() - 4));

        // Max higher than available returns available
        romOptions.setMinimumRandomizablePokemonNumber(4);
        romOptions.setMaximumRandomizablePokemonNumber(9999);
        romhandler.setPokemonPool(null, romOptions);
        assertTrue("First element of main Pokemon list does not equal 4th index of pokemonList",
                romhandler.getMainPokemonList().get(0) == romhandler.getPokemon().get(4));
        assertTrue("Last element of main Pokemon list does not equal last index of pokemonList",
                romhandler.getMainPokemonList().get(romhandler.getMainPokemonList().size()
                        - 1) == romhandler.getPokemon().get(romhandler.getPokemon().size() - 1));

    }

    /**
     * If a RomOptions is given such that the min is higher than max, it should throw an exception
     * and stop execution
     */
    @Test(expected = RandomizationException.class)
    public void TestRomOptionsThrowsException() {
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        RomOptions romOptions = new RomOptions();
        romOptions.setRandomizeSubset(true);
        romOptions.setMaximumRandomizablePokemonNumber(10);
        romOptions.setMinimumRandomizablePokemonNumber(20);
        romhandler.setPokemonPool(null, romOptions);
        assertFalse("Exception was not thrown", true);
    }

    /**
     * If a GenRestrictions is given such that no pokemon exist in the main list at the end, it
     * should throw an exception and stop execution
     */
    @Test(expected = RandomizationException.class)
    public void TestGenRestrictionsThrowsException() {
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        GenRestrictions restrictions = new GenRestrictions();
        romhandler.setPokemonPool(restrictions, null);
        assertFalse("Exception was not thrown", true);
    }

    /**
     * If both GenRestrictions and RomOptions are provided, the pool is appropriately modified
     */
    @Test
    public void TestGenRestrictionsAndRomOptions() {
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        GenRestrictions restrictions = new GenRestrictions();
        RomOptions romOptions = new RomOptions();
        // Sanity check - Fully enabled genRestrictions and romOptions works
        restrictions.makeEverythingSelected();
        romOptions.setRandomizeSubset(true);
        romOptions.setMinimumRandomizablePokemonNumber(0);
        romOptions.setMaximumRandomizablePokemonNumber(9999);
        romhandler.setPokemonPool(restrictions, romOptions);
        assertTrue("Main pokemon list was modified and is missing pokemon",
                romhandler.getMainPokemonList().containsAll(
                        romhandler.getPokemon().subList(1, romhandler.getPokemon().size())));

        // Subset of all pokemon is properly limited on fully enabled genRestrictions
        romOptions.setMinimumRandomizablePokemonNumber(4);
        romOptions.setMaximumRandomizablePokemonNumber(romhandler.getPokemon().size() - 4);
        romhandler.setPokemonPool(restrictions, romOptions);
        assertTrue("First element of main Pokemon list does not equal 4th index of pokemonList",
                romhandler.getMainPokemonList().get(0) == romhandler.getPokemon().get(4));
        assertTrue(
                "Last element of main Pokemon list does not equal 4th to last index of pokemonList",
                romhandler.getMainPokemonList().get(romhandler.getMainPokemonList().size()
                        - 1) == romhandler.getPokemon().get(romhandler.getPokemon().size() - 4));
        // No duplicates in list
        assertEquals(new HashSet<Pokemon>(romhandler.getMainPokemonList()).size(),
                romhandler.getMainPokemonList().size());

        // Disabled generation is removed from sublist
        restrictions.allow_gen1 = false;
        restrictions.assoc_g1_g2 = false;
        restrictions.assoc_g1_g4 = false;
        romhandler.setPokemonPool(restrictions, romOptions);
        assertTrue("First element of main Pokemon list does not equal 152nd index of pokemonList",
                romhandler.getMainPokemonList().get(0) == romhandler.getPokemon().get(152));
        assertTrue(
                "Last element of main Pokemon list does not equal 4th to last index of pokemonList",
                romhandler.getMainPokemonList().get(romhandler.getMainPokemonList().size()
                        - 1) == romhandler.getPokemon().get(romhandler.getPokemon().size() - 4));
        // No duplicates in list
        assertEquals(new HashSet<Pokemon>(romhandler.getMainPokemonList()).size(),
                romhandler.getMainPokemonList().size());

        // Enabled only 1 generation
        restrictions = new GenRestrictions();
        restrictions.allow_gen1 = true;
        romhandler.setPokemonPool(restrictions, romOptions);
        assertTrue("First element of main Pokemon list does not equal 4th index of pokemonList",
                romhandler.getMainPokemonList().get(0) == romhandler.getPokemon().get(4));
        assertTrue("Last element of main Pokemon list does not equal 151st index of pokemonList",
                romhandler.getMainPokemonList().get(romhandler.getMainPokemonList().size()
                        - 1) == romhandler.getPokemon().get(151));
        // No duplicates in list
        assertEquals(new HashSet<Pokemon>(romhandler.getMainPokemonList()).size(),
                romhandler.getMainPokemonList().size());
    }

    /**
     * For valid permutations of GenRestrictions, Game1To1Encounters should not set any encounter to
     * null and encounters must be appropriate for the generations allowed
     */
    @Test
    public void TestGenRestrictionGame1To1Encounters() {
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        doNothing().when(romhandler).setEncounters(anyBoolean(), encounterCap.capture());
        GenRestrictions restrictions = new GenRestrictions();
        ArrayList<Integer> allowedNumbers = new ArrayList();
        ArrayList<Integer> gen1Numbers = new ArrayList<Integer>(
                IntStream.range(1, 152).boxed().collect(Collectors.toList()));
        ArrayList<Integer> gen2Numbers = new ArrayList<Integer>(
                IntStream.range(152, 252).boxed().collect(Collectors.toList()));
        ArrayList<Integer> gen3Numbers = new ArrayList<Integer>(
                IntStream.range(252, 387).boxed().collect(Collectors.toList()));
        ArrayList<Integer> gen4Numbers = new ArrayList<Integer>(
                IntStream.range(387, 494).boxed().collect(Collectors.toList()));
        ArrayList<Integer> gen5Numbers = new ArrayList<Integer>(
                IntStream.range(494, 650).boxed().collect(Collectors.toList()));
        for (int a = 0; a < 2; a++) {
            restrictions.allow_gen1 = 1 - a == 0 ? true : false;
            for (int b = 0; b < 2; b++) {
                restrictions.allow_gen2 = 1 - b == 0 ? true : false;
                for (int c = 0; c < 2; c++) {
                    restrictions.allow_gen3 = 1 - c == 0 ? true : false;
                    for (int d = 0; d < 2; d++) {
                        restrictions.allow_gen4 = 1 - d == 0 ? true : false;
                        for (int e = 0; e < 2; e++) {
                            restrictions.allow_gen5 = 1 - e == 0 ? true : false;
                            // If everything is 0, then we have all false
                            // This throws an exception, which is not desirable
                            // So we skip this condition
                            if (a + b + c + d + e == 0) {
                                continue;
                            }
                            if (restrictions.allow_gen1) {
                                allowedNumbers.addAll(gen1Numbers);
                            } else {
                                allowedNumbers.removeAll(gen1Numbers);
                            }
                            if (restrictions.allow_gen2) {
                                allowedNumbers.addAll(gen2Numbers);
                            } else {
                                allowedNumbers.removeAll(gen2Numbers);
                            }
                            if (restrictions.allow_gen3) {
                                allowedNumbers.addAll(gen3Numbers);
                            } else {
                                allowedNumbers.removeAll(gen3Numbers);
                            }
                            if (restrictions.allow_gen4) {
                                allowedNumbers.addAll(gen4Numbers);
                            } else {
                                allowedNumbers.removeAll(gen4Numbers);
                            }
                            if (restrictions.allow_gen5) {
                                allowedNumbers.addAll(gen5Numbers);
                            } else {
                                allowedNumbers.removeAll(gen5Numbers);
                            }
                            romhandler.setPokemonPool(restrictions, null);
                            romhandler.game1to1Encounters(false, false, true);
                            for (EncounterSet es : encounterCap.getValue()) {
                                for (Encounter enc : es.getEncounters()) {
                                    assertFalse("A null encounter was found",
                                            enc.getPokemon() == null);
                                    assertTrue(
                                            enc.getPokemon().getNumber()
                                                    + " was not found with these restrictions "
                                                    + restrictions,
                                            allowedNumbers.contains(enc.getPokemon().getNumber()));
                                }
                            }
                            romhandler.game1to1Encounters(false, false, false);
                            for (EncounterSet es : encounterCap.getValue()) {
                                for (Encounter enc : es.getEncounters()) {
                                    assertFalse("A null encounter was found",
                                            enc.getPokemon() == null);
                                    assertTrue(
                                            enc.getPokemon().getNumber()
                                                    + " was not found with these restrictions "
                                                    + restrictions,
                                            allowedNumbers.contains(enc.getPokemon().getNumber()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void TestTrainersBuffElite() {
        HashMap<Pokemon, Pokemon> pokemonSwap =
                new HashMap<Pokemon, Pokemon>(Gen5Constants.pokemonCount + 1);
        HashSet<Type> trainerType;
        ArrayList<Trainer> originalTrainer = new ArrayList();
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        doNothing().when(romhandler).setTrainers(trainerCap.capture());

        /*************************
         * Buff Elite standalone *
         *************************/
        romhandler.randomizeTrainerPokes(false, false, false, false, false, false, false, false,
                false, true, 0);
        for (Trainer t : trainerCap.getValue()) {
            // Skip anyone that is not tagged
            if (t.getTag() != null && (t.getTag().startsWith("ELITE")
                    || t.getTag().startsWith("CHAMPION") || t.getTag().startsWith("UBER"))) {
                for (TrainerPokemon tp : t.getPokemon()) {
                    assertTrue(
                            "Pokemon was not a legendary and did not qualify as BIG: "
                                    + tp.getPokemon(),
                            tp.getPokemon().isLegendary() || tp.getPokemon().isBigPoke(false));
                }
            }
        }

        /*****************************
         * Buff Elite and Type Theme *
         *****************************/
        romhandler.randomizeTrainerPokes(false, false, false, false, false, true, false, false,
                false, true, 0);
        for (Trainer t : romhandler.getTrainers()) {
            // Skip anyone that is not tagged
            if (t.getTag() != null && (t.getTag().startsWith("ELITE")
                    || t.getTag().startsWith("CHAMPION") || t.getTag().startsWith("UBER"))) {
                trainerType = new HashSet<Type>();
                for (TrainerPokemon tp : t.getPokemon()) {
                    // Initialize the set
                    if (trainerType.size() == 0) {
                        trainerType.add(tp.pokemon.primaryType);
                        if (tp.pokemon.secondaryType != null) {
                            trainerType.add(tp.pokemon.secondaryType);
                        }
                    }
                    // Only keep the shared type
                    else {
                        HashSet<Type> intersect = new HashSet<Type>();
                        intersect.add(tp.pokemon.primaryType);
                        if (tp.pokemon.secondaryType != null) {
                            intersect.add(tp.pokemon.secondaryType);
                        }
                        trainerType.retainAll(intersect);
                    }
                    assertTrue(
                            "Pokemon was not a legendary and did not qualify as BIG: "
                                    + tp.getPokemon(),
                            tp.getPokemon().isLegendary() || tp.getPokemon().isBigPoke(false));
                }
                // Test for 2 since there could be only 1 pokemon with 2 types, or all pokemon
                // share the same 2 types even though the gym only requires 1 of those types
                assertTrue("More than 2 types found - " + Arrays.toString(trainerType.toArray()),
                        trainerType.size() < 3);

                // Test to make sure at least 1 type was found
                assertTrue("Less than 1 type found - " + Arrays.toString(trainerType.toArray()),
                        trainerType.size() > 0);
            }
        }

        /******************************
         * Buff Elite and Global Swap *
         ******************************/
        for (Trainer t : romhandler.getTrainers()) {
            originalTrainer.add(new Trainer(t));
        }
        romhandler.randomizeTrainerPokes(false, false, false, false, false, false, true, false,
                false, true, 0);
        for (int i = 0; i < trainerCap.getValue().size(); i++) {
            Trainer newT = trainerCap.getValue().get(i);
            Trainer oldT = originalTrainer.get(i);
            assertTrue("Trainer did not match name. Make sure the trainer list is ordered the same",
                    newT.getName().equals(oldT.getName()));
            Collections.sort(newT.getPokemon(),
                    (o1, o2) -> o1.getNickname().compareTo(o2.getNickname()));
            Collections.sort(oldT.getPokemon(),
                    (o1, o2) -> o1.getNickname().compareTo(o2.getNickname()));
            // Skip anyone that is not tagged
            if (newT.getTag() != null && (newT.getTag().startsWith("ELITE")
                    || newT.getTag().startsWith("CHAMPION") || newT.getTag().startsWith("UBER"))) {
                for (int j = 0; j < newT.getPokemon().size(); j++) {
                    TrainerPokemon newTp = newT.getPokemon().get(j);
                    TrainerPokemon oldTp = oldT.getPokemon().get(j);
                    // Initialize the set
                    if (pokemonSwap.containsKey(oldTp.pokemon)) {
                        Pokemon cached = pokemonSwap.get(oldTp.pokemon);
                        assertTrue(
                                "Pokemon did not match the replacement - " + oldTp.pokemon.number
                                        + " gave " + cached.number + " but newTp was "
                                        + newTp.pokemon.number + ", which is not legendary or BIG",
                                cached.equals(newTp.pokemon) || newTp.pokemon.isLegendary()
                                        || newTp.pokemon.isBigPoke(false));
                    } else {
                        pokemonSwap.put(oldTp.pokemon, newTp.pokemon);
                    }
                    assertTrue(
                            "Pokemon was not a legendary and did not qualify as BIG: "
                                    + newTp.getPokemon(),
                            newTp.getPokemon().isLegendary()
                                    || newTp.getPokemon().isBigPoke(false));
                }
            }
        }

        /*********************************
         * Buff Elite and Gym Type Theme *
         **********************************/
        romhandler.randomizeTrainerPokes(false, false, false, false, false, false, false, true,
                false, true, 0);
        for (Trainer t : romhandler.getTrainers()) {
            // Skip anyone that is not tagged
            if (t.getTag() != null && (t.getTag().startsWith("ELITE")
                    || t.getTag().startsWith("CHAMPION") || t.getTag().startsWith("UBER"))) {
                trainerType = new HashSet<Type>();
                for (TrainerPokemon tp : t.getPokemon()) {
                    // Initialize the set
                    if (trainerType.size() == 0) {
                        trainerType.add(tp.pokemon.primaryType);
                        if (tp.pokemon.secondaryType != null) {
                            trainerType.add(tp.pokemon.secondaryType);
                        }
                    }
                    // Only keep the shared type
                    else {
                        HashSet<Type> intersect = new HashSet<Type>();
                        intersect.add(tp.pokemon.primaryType);
                        if (tp.pokemon.secondaryType != null) {
                            intersect.add(tp.pokemon.secondaryType);
                        }
                        trainerType.retainAll(intersect);
                    }
                    assertTrue(
                            "Pokemon was not a legendary and did not qualify as BIG: "
                                    + tp.getPokemon(),
                            tp.getPokemon().isLegendary() || tp.getPokemon().isBigPoke(false));
                }
                // Test for 2 since there could be only 1 pokemon with 2 types, or all pokemon
                // share the same 2 types even though the gym only requires 1 of those types
                assertTrue("More than 2 types found - " + Arrays.toString(trainerType.toArray()),
                        trainerType.size() < 3);

                // Test to make sure at least 1 type was found
                assertTrue("Less than 1 type found - " + Arrays.toString(trainerType.toArray()),
                        trainerType.size() > 0);
            }
        }
    }

    @Test
    public void TestEvoLv1() {
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        romhandler.randomizeEvolutions(false, false, false, false, false, false, false, true, false,
                false);
        for (Pokemon pk : romhandler.getMainPokemonList()) {
            // Skip index 0 since this is automatically removed from the list by the code
            if (pk.number == 0) {
                continue;
            }
            boolean allLevel1 = pk.evolutionsFrom.stream().allMatch(ev -> ev.extraInfo == 1);
            assertTrue(
                    "Pokemon " + pk.number + " had " + pk.evolutionsFrom.size()
                            + " evolutions and allLevel1 was " + allLevel1,
                    pk.evolutionsFrom.size() > 0 && allLevel1);
        }
    }

    @Test
    public void TestForceTrainerFullyEvolved() {
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        int forceEvoLevel = 30;
        resetDataModel(romhandler);
        romhandler.forceFullyEvolvedTrainerPokes(forceEvoLevel);
        for (Trainer t : romhandler.getTrainers()) {
            for (TrainerPokemon tp : t.getPokemon()) {
                if (tp.getLevel() >= forceEvoLevel) {
                    assertTrue(
                            "Pokemon " + tp.getPokemon().getNumber() + " had level " + tp.getLevel()
                                    + " but had evolutions remaining",
                            tp.getPokemon().evolutionsFrom.size() == 0);
                }
            }
        }
    }

    @Test
    public void TestTrainerLevelModified() {
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        int levelMod = 30;
        resetDataModel(romhandler);
        // Cache original trainers
        ArrayList<Trainer> originalTrainers = new ArrayList<>();
        for (Trainer t : romhandler.getTrainers()) {
            originalTrainers.add(new Trainer(t));
        }

        // Without trainer randomization
        romhandler.modifyTrainerPokes(false, levelMod);
        for (int i = 0; i < romhandler.getTrainers().size(); i++) {
            Trainer newT = romhandler.getTrainers().get(i);
            Trainer oldT = originalTrainers.get(i);
            for (int j = 0; j < newT.getPokemon().size(); j++) {
                TrainerPokemon newTp = newT.getPokemon().get(j);
                TrainerPokemon oldTp = oldT.getPokemon().get(j);
                assertTrue(
                        "Pokemon " + newTp.getPokemon().getNumber() + " had level "
                                + newTp.getLevel() + " but originally had " + oldTp.getLevel(),
                        newTp.getLevel() > oldTp.getLevel());
            }
        }

        // With trainer randomization
        resetDataModel(romhandler);
        // Cache original trainers
        originalTrainers = new ArrayList<>();
        for (Trainer t : romhandler.getTrainers()) {
            originalTrainers.add(new Trainer(t));
        }
        romhandler.randomizeTrainerPokes(false, false, false, false, false, false, false, false,
                false, false, levelMod);
        for (int i = 0; i < romhandler.getTrainers().size(); i++) {
            Trainer newT = romhandler.getTrainers().get(i);
            Trainer oldT = originalTrainers.get(i);
            for (int j = 0; j < newT.getPokemon().size(); j++) {
                TrainerPokemon newTp = newT.getPokemon().get(j);
                TrainerPokemon oldTp = oldT.getPokemon().get(j);
                assertTrue(
                        "Pokemon " + newTp.getPokemon().getNumber() + " had level "
                                + newTp.getLevel() + " but originally had " + oldTp.getLevel(),
                        newTp.getLevel() > oldTp.getLevel());
            }
        }
    }

    @Test
    public void TestEvoSameStage() {
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);

        // Cache old evolution count for data later
        Map<Pokemon, Integer> originalToEvosCount = new HashMap<Pokemon, Integer>();
        Map<Pokemon, List<Evolution>> originalFromEvos = new HashMap<Pokemon, List<Evolution>>();
        for (Pokemon pk : romhandler.getPokemon()) {
            int preEvoCount = 0;
            Pokemon considered = pk;
            while (considered.evolutionsTo.size() != 0) {
                preEvoCount++;
                considered = considered.evolutionsTo.get(0).getFrom();
            }
            originalToEvosCount.put(pk, preEvoCount);
            originalFromEvos.put(pk, pk.evolutionsFrom);
        }

        romhandler.randomizeEvolutions(false, false, false, false, false, false, false, false, true,
                false);

        for (Pokemon pk : romhandler.getPokemon()) {
            for (int i = 0; i < pk.evolutionsFrom.size(); i++) {
                Pokemon newTo = pk.evolutionsFrom.get(i).getTo();
                Pokemon oldTo = originalFromEvos.get(pk).get(i).getTo();
                assertTrue(
                        "Pokemon " + pk.number + " evolved into Pokemon " + newTo.number
                                + " with orignal stage count " + originalToEvosCount.get(newTo)
                                + ", but should have had count " + originalToEvosCount.get(oldTo),
                        originalToEvosCount.get(newTo) == originalToEvosCount.get(oldTo));
            }
        }
    }

    @Test
    public void TestEvoNoLegendaries() {
        TestRomHandler romhandler = spy(new TestRomHandler(new Random()));
        resetDataModel(romhandler);
        romhandler.randomizeEvolutions(false, false, false, false, false, false, false, false,
                false, true);
        for (Pokemon pk : romhandler.getMainPokemonList()) {
            for (Evolution ev : pk.evolutionsFrom) {
                assertTrue(
                        "Legendary " + ev.to.number + " was found as an evolution for " + pk.number,
                        !romhandler.onlyLegendaryList.contains(ev.to));
            }
        }
    }

    /**
     * Function for granular modification of data model
     */
    private void setUp() {
        pokemonList = new ArrayList();
        trainerList = new ArrayList();
        startersList = new ArrayList();
        encountersList = new ArrayList();

        for (int i = 0; i < Gen5Constants.pokemonCount + 1; i++) {
            Pokemon pk = new Pokemon();
            pk.number = i;
            pk.name = "";
            pk.primaryType = Type.values()[i % 17];
            pokemonList.add(pk);
            for (int j = 0; j < i % 2; j++) {
                Pokemon evPk = new Pokemon();
                evPk.number = ++i;
                evPk.name = "";
                evPk.primaryType = Type.values()[i % 17];
                pokemonList.add(evPk);
                Evolution ev = new Evolution(pk, evPk, false, EvolutionType.LEVEL, 1);
                pk.evolutionsFrom.add(ev);
                evPk.evolutionsTo.add(ev);
                if (i % 3 == 0) {
                    Pokemon evPk2 = new Pokemon();
                    evPk2.number = ++i;
                    evPk2.name = "";
                    evPk2.primaryType = Type.values()[i % 17];
                    pokemonList.add(evPk2);
                    Evolution ev2 = new Evolution(ev.to, evPk2, false, EvolutionType.LEVEL, 1);
                    ev.to.evolutionsFrom.add(ev2);
                    evPk2.evolutionsTo.add(ev2);
                }
            }
        }
        for (String tag : new String[] {"GYM1", "CILAN", "CHILI", "CRESS", "ELITE1", "ELITE2",
                "ELITE3", "ELITE4", "CHAMPION", "UBER1", "UBER2", "UBER3"}) {
            Trainer t = new Trainer();
            t.setTag(tag);
            t.setName(tag);
            t.setOffset(trainerList.size());
            while (t.getPokemon().size() < 2) {
                TrainerPokemon tp = new TrainerPokemon();
                tp.pokemon = pokemonList.get(t.getPokemon().size());
                tp.setNickname("number" + t.getPokemon().size());
                tp.setLevel(new Random().nextInt(45) + 5);
                t.getPokemon().add(tp);
            }
            trainerList.add(t);
        }
        while (trainerList.size() < 100) {
            Trainer t = new Trainer();
            t.setName("generic" + trainerList.size());
            t.setOffset(trainerList.size());
            while (t.getPokemon().size() < 2) {
                TrainerPokemon tp = new TrainerPokemon();
                tp.pokemon = pokemonList.get(new Random().nextInt(pokemonList.size()));
                tp.setNickname("number" + t.getPokemon().size());
                tp.setLevel(new Random().nextInt(45) + 5);
                t.getPokemon().add(tp);
            }
            trainerList.add(t);
        }
        for (int i = 0; i < 3; i++) {
            startersList.add(new Pokemon());
        }

        for (int i = 0; i < 5; i++) {
            EncounterSet es = new EncounterSet();
            Encounter e1 = new Encounter();
            e1.setPokemon(pokemonList.get(new Random().nextInt(pokemonList.size())));
            es.setEncounters(Arrays.asList(e1));
            encountersList.add(es);
        }
    }

    /**
     * Puts data model back to initial form and assigns mock and spy substitutions
     * 
     * @param romhandler The RomHandler under test
     */
    private void resetDataModel(RomHandler romhandler) {
        setUp();
        doReturn(pokemonList).when(romhandler).getPokemon();
        doReturn(pokemonList.get(0)).when(romhandler).randomPokemon();
        doReturn(trainerList).when(romhandler).getTrainers();
        doReturn(startersList).when(romhandler).getStarters();
        doReturn(encountersList).when(romhandler).getEncounters(anyBoolean());
    }
}
