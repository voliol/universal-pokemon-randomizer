package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.romhandlers.AbstractGBRomHandler;
import com.dabomstew.pkrandom.romhandlers.RomHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class RomHandlerTrainerTest extends RomHandlerTest {

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void trainersAreNotEmpty(String romName) {
        loadROM(romName);
        assertFalse(romHandler.getTrainers().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void trainersDoNotChangeWithGetAndSet(String romName) {
        // TODO: this comparison needs to be deeper
        loadROM(romName);
        List<Trainer> trainers = romHandler.getTrainers();
        List<Trainer> before = new ArrayList<>(trainers);
        romHandler.setTrainers(trainers);
        assertEquals(before, romHandler.getTrainers());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void trainersDoNotChangeWithLoadAndSave(String romName) {
        // TODO: this comparison needs to be deeper
        assumeTrue(isGBGame(romName));
        loadROM(romName);
        AbstractGBRomHandler gbRomHandler = (AbstractGBRomHandler) romHandler;
        List<Trainer> trainers = gbRomHandler.getTrainers();
        List<Trainer> before = new ArrayList<>(trainers);
        gbRomHandler.setTrainers(trainers);
        gbRomHandler.saveTrainers();
        gbRomHandler.loadTrainers();
        assertEquals(before, gbRomHandler.getTrainers());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void trainersHaveAtLeastTwoPokemonAfterSettingDoubleBattleMode(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 3);
        loadROM(romName);
        romHandler.setDoubleBattleMode();
        for (Trainer trainer : romHandler.getTrainers()) {
            System.out.println(trainer);
            if (trainer.forcedDoubleBattle) {
                assertTrue(trainer.pokemon.size() >= 2);
            } else {
                System.out.println("Not a forced double battle.");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void trainersSaveAndLoadCorrectlyAfterSettingDoubleBattleMode(String romName) {
        assumeTrue(getGenerationNumberOf(romName) == 3);
        loadROM(romName);
        romHandler.setDoubleBattleMode();
        AbstractGBRomHandler gbRomHandler = (AbstractGBRomHandler) romHandler;
        List<Trainer> trainers = gbRomHandler.getTrainers();
        List<Trainer> before = new ArrayList<>(trainers);
        gbRomHandler.setTrainers(trainers);
        gbRomHandler.saveTrainers();
        gbRomHandler.loadTrainers();
        assertEquals(before, gbRomHandler.getTrainers());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canAddPokemonToBossTrainersAndSaveAndLoad(String romName) {
        assumeTrue(isGBGame(romName));
        loadROM(romName);
        assumeTrue(romHandler.canAddPokemonToBossTrainers());
        Settings s = new Settings();
        s.setAdditionalBossTrainerPokemon(6);
        romHandler.addTrainerPokemon(s);
        AbstractGBRomHandler gbRomHandler = (AbstractGBRomHandler) romHandler;
        List<Trainer> trainers = gbRomHandler.getTrainers();
        List<Trainer> before = new ArrayList<>(trainers);
        gbRomHandler.setTrainers(trainers);
        gbRomHandler.saveTrainers();
        gbRomHandler.loadTrainers();
        assertEquals(before, gbRomHandler.getTrainers());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canAddPokemonToImportantTrainersAndSaveAndLoad(String romName) {
        assumeTrue(isGBGame(romName));
        loadROM(romName);
        assumeTrue(romHandler.canAddPokemonToImportantTrainers());
        Settings s = new Settings();
        s.setAdditionalImportantTrainerPokemon(6);
        romHandler.addTrainerPokemon(s);
        AbstractGBRomHandler gbRomHandler = (AbstractGBRomHandler) romHandler;
        List<Trainer> trainers = gbRomHandler.getTrainers();
        List<Trainer> before = new ArrayList<>(trainers);
        gbRomHandler.setTrainers(trainers);
        gbRomHandler.saveTrainers();
        gbRomHandler.loadTrainers();
        assertEquals(before, gbRomHandler.getTrainers());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canAddPokemonToBossAndImportantTrainersAndSaveAndLoad(String romName) {
        assumeTrue(isGBGame(romName));
        loadROM(romName);
        assumeTrue(romHandler.canAddPokemonToBossTrainers() && romHandler.canAddPokemonToImportantTrainers());
        Settings s = new Settings();
        s.setAdditionalBossTrainerPokemon(6);
        s.setAdditionalImportantTrainerPokemon(6);
        romHandler.addTrainerPokemon(s);
        AbstractGBRomHandler gbRomHandler = (AbstractGBRomHandler) romHandler;
        List<Trainer> trainers = gbRomHandler.getTrainers();
        List<Trainer> before = new ArrayList<>(trainers);
        gbRomHandler.setTrainers(trainers);
        gbRomHandler.saveTrainers();
        gbRomHandler.loadTrainers();
        assertEquals(before, gbRomHandler.getTrainers());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canAddPokemonToAllTrainersAndSaveAndLoad(String romName) {
        assumeTrue(isGBGame(romName));
        loadROM(romName);
        assumeTrue(romHandler.canAddPokemonToBossTrainers() && romHandler.canAddPokemonToImportantTrainers()
                && romHandler.canAddPokemonToRegularTrainers());
        Settings s = new Settings();
        s.setAdditionalBossTrainerPokemon(5);
        s.setAdditionalImportantTrainerPokemon(5);
        s.setAdditionalRegularTrainerPokemon(5);
        romHandler.addTrainerPokemon(s);
        AbstractGBRomHandler gbRomHandler = (AbstractGBRomHandler) romHandler;
        List<Trainer> trainers = gbRomHandler.getTrainers();
        List<Trainer> before = new ArrayList<>(trainers);
        gbRomHandler.setTrainers(trainers);
        gbRomHandler.saveTrainers();
        gbRomHandler.loadTrainers();
        assertEquals(before, gbRomHandler.getTrainers());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void addPokemonToBossAndImportantTrainersAndSaveAndLoadGivesThemFullParties(String romName) {
        assumeTrue(isGBGame(romName));
        loadROM(romName);
        assumeTrue(romHandler.canAddPokemonToBossTrainers() && romHandler.canAddPokemonToImportantTrainers());
        Settings s = new Settings();
        s.setAdditionalBossTrainerPokemon(5);
        s.setAdditionalImportantTrainerPokemon(5);
        romHandler.addTrainerPokemon(s);
        AbstractGBRomHandler gbRomHandler = (AbstractGBRomHandler) romHandler;
        List<Trainer> trainers = gbRomHandler.getTrainers();
        gbRomHandler.setTrainers(trainers);
        gbRomHandler.saveTrainers();
        gbRomHandler.loadTrainers();
        for (Trainer tr : gbRomHandler.getTrainers()) {
            if (tr.multiBattleStatus == Trainer.MultiBattleStatus.NEVER && !tr.shouldNotGetBuffs()
                    && (tr.isBoss() || tr.isImportant())) {
                System.out.println(tr);
                assertEquals(6, tr.pokemon.size());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void addPokemonToAllTrainersAndSaveAndLoadGivesThemFullParties(String romName) {
        assumeTrue(isGBGame(romName));
        loadROM(romName);
        assumeTrue(romHandler.canAddPokemonToBossTrainers() && romHandler.canAddPokemonToImportantTrainers()
                && romHandler.canAddPokemonToRegularTrainers());
        Settings s = new Settings();
        s.setAdditionalBossTrainerPokemon(5);
        s.setAdditionalImportantTrainerPokemon(5);
        s.setAdditionalRegularTrainerPokemon(5);
        romHandler.addTrainerPokemon(s);
        AbstractGBRomHandler gbRomHandler = (AbstractGBRomHandler) romHandler;
        List<Trainer> trainers = gbRomHandler.getTrainers();
        gbRomHandler.setTrainers(trainers);
        gbRomHandler.saveTrainers();
        gbRomHandler.loadTrainers();
        for (Trainer tr : gbRomHandler.getTrainers()) {
            if (tr.multiBattleStatus == Trainer.MultiBattleStatus.NEVER && !tr.shouldNotGetBuffs()) {
                System.out.println(tr);
                assertEquals(6, tr.pokemon.size());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canAddHeldItemsToBossTrainersAndSaveAndLoad(String romName) {
        assumeTrue(isGBGame(romName));
        loadROM(romName);
        assumeTrue(romHandler.canAddHeldItemsToBossTrainers());
        Settings s = new Settings();
        s.setRandomizeHeldItemsForBossTrainerPokemon(true);
        romHandler.randomizeTrainerHeldItems(s);
        AbstractGBRomHandler gbRomHandler = (AbstractGBRomHandler) romHandler;
        List<Trainer> trainers = gbRomHandler.getTrainers();
        List<Trainer> before = new ArrayList<>(trainers);
        gbRomHandler.setTrainers(trainers);
        gbRomHandler.saveTrainers();
        gbRomHandler.loadTrainers();
        assertEquals(before, gbRomHandler.getTrainers());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canAddHeldItemsToImportantTrainersAndSaveAndLoad(String romName) {
        assumeTrue(isGBGame(romName));
        loadROM(romName);
        assumeTrue(romHandler.canAddHeldItemsToImportantTrainers());
        Settings s = new Settings();
        s.setRandomizeHeldItemsForImportantTrainerPokemon(true);
        romHandler.randomizeTrainerHeldItems(s);
        AbstractGBRomHandler gbRomHandler = (AbstractGBRomHandler) romHandler;
        List<Trainer> trainers = gbRomHandler.getTrainers();
        List<Trainer> before = new ArrayList<>(trainers);
        gbRomHandler.setTrainers(trainers);
        gbRomHandler.saveTrainers();
        gbRomHandler.loadTrainers();
        assertEquals(before, gbRomHandler.getTrainers());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canAddHeldItemsToBossAndImportantTrainersAndSaveAndLoad(String romName) {
        assumeTrue(isGBGame(romName));
        loadROM(romName);
        assumeTrue(romHandler.canAddHeldItemsToBossTrainers());
        assumeTrue(romHandler.canAddHeldItemsToImportantTrainers());
        Settings s = new Settings();
        s.setRandomizeHeldItemsForImportantTrainerPokemon(true);
        s.setRandomizeHeldItemsForBossTrainerPokemon(true);
        romHandler.randomizeTrainerHeldItems(s);
        AbstractGBRomHandler gbRomHandler = (AbstractGBRomHandler) romHandler;
        List<Trainer> trainers = gbRomHandler.getTrainers();
        List<Trainer> before = new ArrayList<>(trainers);
        gbRomHandler.setTrainers(trainers);
        gbRomHandler.saveTrainers();
        gbRomHandler.loadTrainers();
        assertEquals(before, gbRomHandler.getTrainers());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canAddHeldItemsToAllTrainersAndSaveAndLoad(String romName) {
        assumeTrue(isGBGame(romName));
        loadROM(romName);
        assumeTrue(romHandler.canAddHeldItemsToBossTrainers());
        assumeTrue(romHandler.canAddHeldItemsToImportantTrainers());
        assumeTrue(romHandler.canAddHeldItemsToRegularTrainers());
        Settings s = new Settings();
        s.setRandomizeHeldItemsForImportantTrainerPokemon(true);
        s.setRandomizeHeldItemsForBossTrainerPokemon(true);
        s.setRandomizeHeldItemsForRegularTrainerPokemon(true);
        romHandler.randomizeTrainerHeldItems(s);
        AbstractGBRomHandler gbRomHandler = (AbstractGBRomHandler) romHandler;
        List<Trainer> trainers = gbRomHandler.getTrainers();
        List<Trainer> before = new ArrayList<>(trainers);
        gbRomHandler.setTrainers(trainers);
        gbRomHandler.saveTrainers();
        gbRomHandler.loadTrainers();
        assertEquals(before, gbRomHandler.getTrainers());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void addHeldItemsToAllTrainersAndSaveAndLoadGivesThemHeldItems(String romName) {
        assumeTrue(isGBGame(romName));
        loadROM(romName);
        assumeTrue(romHandler.canAddHeldItemsToBossTrainers());
        assumeTrue(romHandler.canAddHeldItemsToImportantTrainers());
        assumeTrue(romHandler.canAddHeldItemsToRegularTrainers());
        Settings s = new Settings();
        s.setRandomizeHeldItemsForImportantTrainerPokemon(true);
        s.setRandomizeHeldItemsForBossTrainerPokemon(true);
        s.setRandomizeHeldItemsForRegularTrainerPokemon(true);
        romHandler.randomizeTrainerHeldItems(s);
        AbstractGBRomHandler gbRomHandler = (AbstractGBRomHandler) romHandler;
        List<Trainer> trainers = gbRomHandler.getTrainers();
        gbRomHandler.setTrainers(trainers);
        gbRomHandler.saveTrainers();
        gbRomHandler.loadTrainers();
        for (Trainer tr : gbRomHandler.getTrainers()) {
            System.out.println(tr.fullDisplayName);
            if (tr.shouldNotGetBuffs()) {
                System.out.println("skip");
            } else {
                for (TrainerPokemon tp : tr.pokemon) {
                    System.out.println(tp);
                    assertNotEquals(0, tp.heldItem);
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void keepTypeThemedWorks(String romName) {
        loadROM(romName);

        Map<Trainer, List<String>> beforeTrainerStrings = new HashMap<>();
        Map<Trainer, Type> typeThemedTrainers = new HashMap<>();
        recordTypeThemeBefore(beforeTrainerStrings, typeThemedTrainers);

        Settings s = new Settings();
        s.setTrainersMod(false, false, false, false, false, false, true);
        romHandler.randomizeTrainerPokes(s);

        keepTypeThemedCheck(beforeTrainerStrings, typeThemedTrainers);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void keepTypeThemedWorksWithRandomPokemonTypes(String romName) {
        loadROM(romName);

        Map<Trainer, List<String>> beforeTrainerStrings = new HashMap<>();
        Map<Trainer, Type> typeThemedTrainers = new HashMap<>();
        recordTypeThemeBefore(beforeTrainerStrings, typeThemedTrainers);

        Settings s = new Settings();
        s.setTypesMod(false, false, true);
        romHandler.randomizePokemonTypes(s);
        s.setTrainersMod(false, false, false, false, false, false, true);
        romHandler.randomizeTrainerPokes(s);

        keepTypeThemedCheck(beforeTrainerStrings, typeThemedTrainers);
    }

    private void recordTypeThemeBefore(Map<Trainer, List<String>> beforeTrainerStrings, Map<Trainer, Type> typeThemedTrainers) {
        for (Trainer tr : romHandler.getTrainers()) {
            List<String> beforeStrings = new ArrayList<>();
            beforeTrainerStrings.put(tr, beforeStrings);
            beforeStrings.add(tr.toString());
            for (TrainerPokemon tp : tr.pokemon) {
                beforeStrings.add(tp.pokemon.toString());
            }

            // the rival in yellow is forced to always have eevee, which causes a mess if eevee's type is randomized
            if (tr.tag != null && tr.tag.contains("RIVAL") && romHandler.isYellow()) continue;

            Type theme = getThemedTrainerType(tr);
            if (theme != null) {
                typeThemedTrainers.put(tr, theme);
            }
        }
    }

    private Type getThemedTrainerType(Trainer tr) {
        Pokemon first = tr.pokemon.get(0).pokemon;
        Type primary = first.getOriginalPrimaryType();
        Type secondary = first.getOriginalSecondaryType();
        for (int i = 1; i < tr.pokemon.size(); i++) {
            Pokemon pk = tr.pokemon.get(i).pokemon;
            if(secondary != null) {
                if (secondary != pk.getOriginalPrimaryType() && secondary != pk.getOriginalSecondaryType()) {
                    secondary = null;
                }
            }
            if (primary != pk.getOriginalPrimaryType() && primary != pk.getOriginalSecondaryType()) {
                primary = secondary;
                secondary = null;
            }
            if (primary == null) {
                return null; //no type is shared, no need to look at the remaining pokemon
            }
        }

        //we have a type theme!
        if(primary == Type.NORMAL && secondary != null) {
            //Bird override
            //(Normal is less significant than other types, for example, Flying)
            return secondary;
        } else {
            return primary;
        }

    }

    private void keepTypeThemedCheck(Map<Trainer, List<String>> beforeTrainerStrings, Map<Trainer, Type> typeThemedTrainers) {
        for (Trainer tr : romHandler.getTrainers()) {
            List<String> beforeStrings = beforeTrainerStrings.get(tr);
            System.out.println("Before: " + beforeStrings.get(0));
            for (int i = 1; i < beforeStrings.size(); i++) {
                System.out.println("\t" + beforeStrings.get(i));
            }

            if (typeThemedTrainers.containsKey(tr)) {
                Type theme = typeThemedTrainers.get(tr);
                System.out.println("Type Theme: " + theme);
                System.out.println("After: " + tr);
                for (TrainerPokemon tp : tr.pokemon) {
                    Pokemon pk = tp.pokemon;
                    System.out.println("\t" + pk);
                    assertTrue(pk.getPrimaryType() == theme || pk.getSecondaryType() == theme);
                }
            } else {
                System.out.println("Not Type Themed");
            }
            System.out.println();
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void useLocalPokemonGuaranteesLocalPokemonOnly(String romName) {
        loadROM(romName);
        Settings s = new Settings();
        s.setTrainersMod(false, true, false, false, false, false, false); // RANDOM
        s.setTrainersUseLocalPokemon(true);
        romHandler.randomizeTrainerPokes(s);

        PokemonSet<Pokemon> localWithRelatives = new PokemonSet<>();
        for (EncounterArea area : romHandler.getEncounters(true)) {
            for (Pokemon pk : PokemonSet.inArea(area)) {
                if (!localWithRelatives.contains(pk)) {
                    localWithRelatives.addAll(PokemonSet.related(pk));
                }
            }
        }

        PokemonSet<Pokemon> all = romHandler.getPokemonSet();
        PokemonSet<Pokemon> nonLocal = new PokemonSet<>(all);
        nonLocal.removeAll(localWithRelatives);

        for (Trainer tr : romHandler.getTrainers()) {
            System.out.println(tr);

            // ignore the yellow rival and his forced eevee
            if (tr.tag != null && tr.tag.contains("RIVAL") && romHandler.isYellow()) {
                continue;
            }

            for (TrainerPokemon tp : tr.pokemon) {
                System.out.println(tp.pokemon);
                assertTrue(localWithRelatives.contains(tp.pokemon));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void trainerNamesAreNotEmpty(String romName) {
        loadROM(romName);
        assertFalse(romHandler.getTrainerNames().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void trainerNamesDoNotChangeWithGetAndSet(String romName) {
        loadROM(romName);
        List<String> trainerNames = romHandler.getTrainerNames();
        System.out.println(trainerNames);
        List<String> before = new ArrayList<>(trainerNames);
        romHandler.setTrainerNames(trainerNames);
        assertEquals(before, romHandler.getTrainerNames());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void trainerClassNamesAreNotEmpty(String romName) {
        loadROM(romName);
        assertFalse(romHandler.getTrainerClassNames().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void trainerClassNamesDoNotChangeWithGetAndSet(String romName) {
        loadROM(romName);
        List<String> trainerClassNames = romHandler.getTrainerClassNames();
        System.out.println(trainerClassNames);
        List<String> before = new ArrayList<>(trainerClassNames);
        romHandler.setTrainerClassNames(trainerClassNames);
        assertEquals(before, romHandler.getTrainerClassNames());
    }

}
