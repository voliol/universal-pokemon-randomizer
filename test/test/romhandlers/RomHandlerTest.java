package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.constants.*;
import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.romhandlers.AbstractGBRomHandler;
import com.dabomstew.pkrandom.romhandlers.RomHandler;
import com.dabomstew.pkrandom.romhandlers.romentries.RomEntry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Unit tests for the RomHandler classes.
 */
public class RomHandlerTest {

    // update if the amount of supported generation increases,
    // and expect some test cases to need updating too, though hopefully only in a minor way
    private static final int HIGHEST_GENERATION = 7;

    private static final String TEST_ROMS_PATH = "test/roms";
    private static final String LAST_DOT_REGEX = "\\.+(?![^.]*\\.)";

    public static String[] getRomNames() {
        return Roms.getRoms(new int[]{1, 2, 3}, Roms.Region.values(), false);
    }

    public static String[] getAllRomNames() {
        return Roms.getAllRoms();
    }

    public static String[] getRomNamesInFolder() {
        List<String> names;
        try (Stream<Path> paths = Files.walk(Paths.get(TEST_ROMS_PATH))) {
            names = paths.filter(Files::isRegularFile)
                    .map(p -> p.toFile().getName()).filter(s -> !s.endsWith(".txt"))
                    .map(s -> s.split(LAST_DOT_REGEX)[0])
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return names.toArray(new String[0]);
    }

    private RomHandler romHandler;

    private void loadROM(String romName) {
        Generation gen = getGenerationOf(romName);
        if (gen == null) {
            throw new IllegalArgumentException("Could not find the generation of " + romName);
        }
        String fullRomName = TEST_ROMS_PATH + "/" + romName + gen.getFileSuffix();
        RomHandler.Factory factory = gen.createFactory();
        if (!factory.isLoadable(fullRomName)) {
            throw new IllegalArgumentException("ROM is not loadable.");
        }
        romHandler = factory.create(new Random(), new PrintStream(System.out));
        romHandler.loadRom(fullRomName);
    }

    private Generation getGenerationOf(String romName) {
        return Generation.GAME_TO_GENERATION.get(stripToBaseRomName(romName));
    }

    /**
     * Used to fast check the gen number of a ROM. Really useful for assume... methods, since loading the ROM to use
     * RomHandler.generationOfPokemon() is almost as slow as running the test cases. Increasingly relevant with
     * newer/bigger ROMs involved.
     */
    private int getGenerationNumberOf(String romName) {
        return getGenerationOf(romName).getNumber();
    }

    /**
     * A fast check whether a ROM uses an AbstractGBRomHandler.
     */
    private boolean isGBGame(String romName) {
        return getGenerationNumberOf(romName) <= 3;
    }

    /**
     * Strips the ROM name into just its base - e.g. "Crystal (S)" => "Crystal" and "Fire Red (U)(1.1)" => "Fire Red".
     *
     * @param romName The full name of the ROM
     */
    private String stripToBaseRomName(String romName) {
        return romName.split("\\(")[0].trim();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void loadingDoesNotGiveNullRomHandler(String romName) {
        loadROM(romName);
        assertNotNull(romHandler);
    }

    /**
     * Checks all ROMs found as {@link RomEntry}s in the .ini files, to see if they are is testable.
     * I.e. does the actual ROM exist within the test/roms folder.
     * <br>
     * Since running this always checks for Gen 6+ ROMs, which are very slow to handle, it is disabled by default.
     */
    @Disabled
    @ParameterizedTest
    @MethodSource("getAllRomNames")
    public void romIsTestable(String romName) {
        try {
            loadROM(romName);
            if (!Objects.equals(romHandler.getROMName(), "Pokemon " + romName)) {
                throw new RuntimeException("Rom mismatch. Wanted to load Pokemon " + romName + ", found "
                        + romHandler.getROMName());
            }
        } catch (Exception e) {
            fail(e);
        }
    }

    /**
     * Checks all ROMs in the test/roms folder, to see if they correspond to {@link RomEntry}s in the .ini files.
     * <br>
     * Since running this may open Gen 6+ ROMs (if you have any), which are very slow to handle,
     * it is disabled by default.
     */
    @Disabled
    @ParameterizedTest
    @MethodSource("getRomNamesInFolder")
    public void romNameOfRomInFolderIsCorrect(String romName) {
        loadROM(romName);
        assertEquals("Pokemon " + romName, romHandler.getROMName());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void pokemonListIsNotEmpty(String romName) {
        loadROM(romName);
        assertFalse(romHandler.getPokemon().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void firstPokemonInPokemonListIsNull(String romName) {
        loadROM(romName);
        assertNull(romHandler.getPokemon().get(0));
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void numberOfPokemonInPokemonListEqualsPokemonCountConstant(String romName) {
        loadROM(romName);
        // Because Gen 7 doesn't have a pokemonCount constant really
        // Also, I personally won't be working much on those games...
        assumeFalse(romHandler.generationOfPokemon() == 7);

        int pokemonCount = getPokemonCount();
        assertEquals(pokemonCount + 1, romHandler.getPokemon().size());
    }

    private int getPokemonCount() {
        return switch (romHandler.generationOfPokemon()) {
            case 1 -> Gen1Constants.pokemonCount;
            case 2 -> Gen2Constants.pokemonCount;
            case 3 -> Gen3Constants.pokemonCount;
            case 4 -> Gen4Constants.pokemonCount;
            case 5 -> Gen5Constants.pokemonCount;
            case 6 -> Gen6Constants.pokemonCount;
            default -> 0;
        };
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void pokemonSetIncludesAllNonNullPokemonInPokemonList(String romName) {
        loadROM(romName);
        List<Pokemon> pokemonList = romHandler.getPokemon();
        PokemonSet<Pokemon> pokemonSet = romHandler.getPokemonSet();
        for (Pokemon pk : pokemonList) {
            if (pk != null && !pokemonSet.contains(pk)) {
                fail(pk + " in Pokemon List (getPokemonList()) but not in Pokemon Set (getPokemonSet())");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void pokemonSetOnlyHasPokemonAlsoInPokemonList(String romName) {
        loadROM(romName);
        List<Pokemon> pokemonList = romHandler.getPokemon();
        for (Pokemon pk : romHandler.getPokemonSet()) {
            if (!pokemonList.contains(pk)) {
                fail(pk + " in Pokemon Set (getPokemonSet()) but not in Pokemon List (getPokemon())");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void restrictedPokemonAreSameAsPokemonSetWithNoRestrictionsSet(String romName) {
        loadROM(romName);
        romHandler.setPokemonPool(null);
        assertEquals(romHandler.getPokemonSet(), romHandler.getRestrictedPokemon());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void restrictedPokemonWithNoRelativesDoesNotContainUnrelatedPokemonFromWrongGeneration(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.generationOfPokemon() >= 2);

        Settings settings = new Settings();
        settings.setLimitPokemon(true);
        settings.setCurrentRestrictions(genRestrictionsFromBools(false, new int[]{1}));

        romHandler.setPokemonPool(settings);
        for (Pokemon pk : romHandler.getRestrictedPokemon()) {
            PokemonSet<Pokemon> related = PokemonSet.related(pk);
            boolean anyFromRightGen = false;
            for (Pokemon relative : related) {
                if (relative.getNumber() <= Gen1Constants.pokemonCount) {
                    anyFromRightGen = true;
                    break;
                }
            }
            assertTrue(anyFromRightGen, pk.getName() + " is from the wrong Gen, and is unrelated to " +
                    "Pokémon from the right (Gen I).");
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void restrictedPokemonWithNoRelativesDoesNotContainRelatedPokemonFromWrongGeneration(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.generationOfPokemon() >= 2);

        Settings settings = new Settings();
        settings.setLimitPokemon(true);
        settings.setCurrentRestrictions(genRestrictionsFromBools(false, new int[]{1}));

        romHandler.setPokemonPool(settings);
        PokemonSet<Pokemon> restrictedPokemon = romHandler.getRestrictedPokemon();
        for (Pokemon pk : restrictedPokemon) {
            PokemonSet<Pokemon> related = PokemonSet.related(pk);
            String fromRightGen = null;
            String fromWrongGen = null;
            for (Pokemon relative : related) {
                if (relative.getNumber() <= Gen1Constants.pokemonCount) {
                    fromRightGen = relative.getName();
                } else if (restrictedPokemon.contains(relative)) {
                    fromWrongGen = relative.getName();
                }
            }
            if (fromRightGen != null) {
                assertNull(fromWrongGen, fromWrongGen + " is from the wrong Gen, and though it is related to " +
                        fromRightGen + " from the right (Gen I), this is not allowed.");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void restrictedPokemonWithRelativesDoesNotContainUnrelatedPokemonFromWrongGeneration(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.generationOfPokemon() >= 2);

        Settings settings = new Settings();
        settings.setLimitPokemon(true);
        settings.setCurrentRestrictions(genRestrictionsFromBools(true, new int[]{1}));
        // except for the above line's "relativesAllowed: true", identical to the "WithNoRelatives" method...

        romHandler.setPokemonPool(settings);
        for (Pokemon pk : romHandler.getRestrictedPokemon()) {
            PokemonSet<Pokemon> related = PokemonSet.related(pk);
            boolean anyFromRightGen = false;
            for (Pokemon relative : related) {
                if (relative.getNumber() <= Gen1Constants.pokemonCount) {
                    anyFromRightGen = true;
                    break;
                }
            }
            assertTrue(anyFromRightGen, pk.getName() + " is from the wrong Gen, and is unrelated to " +
                    "Pokémon from the right (Gen I).");
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void restrictedPokemonWithRelativesAlwaysContainsRelatedPokemonFromWrongGeneration(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.generationOfPokemon() >= 2);

        Settings settings = new Settings();
        settings.setLimitPokemon(true);
        settings.setCurrentRestrictions(genRestrictionsFromBools(true, new int[]{1}));

        romHandler.setPokemonPool(settings);
        PokemonSet<Pokemon> restrictedPokemon = romHandler.getRestrictedPokemon();
        for (Pokemon pk : restrictedPokemon) {
            PokemonSet<Pokemon> related = PokemonSet.related(pk);
            Pokemon fromRightGen = null;
            Pokemon fromWrongGen = null;
            for (Pokemon relative : related) {
                if (relative.getNumber() <= Gen1Constants.pokemonCount) {
                    fromRightGen = relative;
                } else {
                    fromWrongGen = relative;
                }
            }
            if (fromRightGen != null && fromWrongGen != null) {
                assertTrue(restrictedPokemon.contains(fromWrongGen), fromWrongGen.getName() + " is missing from " +
                        "restrictedPokemon. It is from the wrong Gen, though as it is related to " + fromRightGen.getName() +
                        " from the right (Gen I), it is allowed.");
            }
        }
    }

    private GenRestrictions genRestrictionsFromBools(boolean relativesAllowed, int[] gensAllowed) {
        int state = 0;
        for (int gen : gensAllowed) {
            state += 1 << (gen - 1);
        }
        state += relativesAllowed ? 1 << HIGHEST_GENERATION : 0;
        return new GenRestrictions(state);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void movesAreNotNull(String romName) {
        loadROM(romName);
        assertNotNull(romHandler.getMoves());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void movesAreNotEmpty(String romName) {
        loadROM(romName);
        System.out.println(romHandler.getMoves());
        assertFalse(romHandler.getMoves().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void firstMoveIsNull(String romName) {
        loadROM(romName);
        assertNull(romHandler.getMoves().get(0));
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void allMovesHaveNonBlankNames(String romName) {
        loadROM(romName);
        System.out.println(romHandler.getMoves());
        for (Move m : romHandler.getMoves()) {
            if (m != null) {
                assertFalse(m.name.isBlank());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void movesLearntDoNotChangeWithGetAndSet(String romName) {
        loadROM(romName);
        Map<Integer, List<MoveLearnt>> movesLearnt = romHandler.getMovesLearnt();
        Map<Integer, List<MoveLearnt>> before = new HashMap<>(movesLearnt);
        romHandler.setMovesLearnt(movesLearnt);
        assertEquals(before, romHandler.getMovesLearnt());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void movesLearntDoNotChangeWithLoadAndSave(String romName) {
        assumeTrue(isGBGame(romName));
        loadROM(romName);
        AbstractGBRomHandler gbRomHandler = (AbstractGBRomHandler) romHandler;
        Map<Integer, List<MoveLearnt>> movesLearnt = romHandler.getMovesLearnt();
        Map<Integer, List<MoveLearnt>> before = new HashMap<>(movesLearnt);
        romHandler.setMovesLearnt(movesLearnt);
        gbRomHandler.savePokemonStats();
        gbRomHandler.loadPokemonStats();
        assertEquals(before, romHandler.getMovesLearnt());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void eggMovesDoNotChangeWithGetAndSet(String romName) {
        assumeTrue(getGenerationNumberOf(romName) != 1);
        loadROM(romName);
        Map<Integer, List<Integer>> eggMoves = romHandler.getEggMoves();
        Map<Integer, List<Integer>> before = new HashMap<>(eggMoves);
        romHandler.setEggMoves(eggMoves);
        assertEquals(before, romHandler.getEggMoves());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void tmMovesDoNotChangeWithGetAndSet(String romName) {
        loadROM(romName);
        List<Integer> tmMoves = romHandler.getTMMoves();
        List<Integer> before = new ArrayList<>(tmMoves);
        romHandler.setTMMoves(tmMoves);
        assertEquals(before, romHandler.getTMMoves());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    // TODO: this test is strangely inconsistent with Fire Red (U) (both versions) and Leaf Green (U) 1.1.
    //  It really should not be so maybe something is wrong with the test case?
    public void moveTutorMovesDoNotChangeWithGetAndSet(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.hasMoveTutors());
        List<Integer> moveTutorMoves = romHandler.getMoveTutorMoves();
        List<Integer> before = new ArrayList<>(moveTutorMoves);
        romHandler.setMoveTutorMoves(moveTutorMoves);
        assertEquals(before, romHandler.getMoveTutorMoves());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void moveTutorsCanBeRandomizedAndGetAndSet(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.hasMoveTutors());
        romHandler.randomizeMoveTutorMoves(new Settings());
        List<Integer> moveTutorMoves = romHandler.getMoveTutorMoves();
        List<Integer> before = new ArrayList<>(moveTutorMoves);
        romHandler.setMoveTutorMoves(moveTutorMoves);
        assertEquals(before, romHandler.getMoveTutorMoves());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void trainersAreNotNull(String romName) {
        loadROM(romName);
        assertNotNull(romHandler.getTrainers());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void trainersAreNotEmpty(String romName) {
        loadROM(romName);
        assertFalse(romHandler.getTrainers().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void trainersDoNotChangeWithGetAndSet(String romName) {
        loadROM(romName);
        List<Trainer> trainers = romHandler.getTrainers();
        List<Trainer> before = new ArrayList<>(trainers);
        romHandler.setTrainers(trainers);
        assertEquals(before, romHandler.getTrainers());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void trainersDoNotChangeWithLoadAndSave(String romName) {
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
    public void trainerNamesAreNotNull(String romName) {
        loadROM(romName);
        assertNotNull(romHandler.getTrainerNames());
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

}
