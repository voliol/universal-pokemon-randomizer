package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.constants.*;
import com.dabomstew.pkrandom.pokemon.GenRestrictions;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.PokemonSet;
import com.dabomstew.pkrandom.romhandlers.RomHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
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
    private static final String LAST_DOT_REGEX = "\\.+(?![^\\.]*\\.)";

    public static String[] getRomNames() {
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
        Generation gen = Generation.GAME_TO_GENERATION.get(stripToBaseRomName(romName));
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

    /**
     * Strips the ROM name into just its base - e.g. "Crystal (S)" => "Crystal" and "Fire Red (U)(1.1)" => "Fire Red".
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

    @ParameterizedTest
    @MethodSource("getRomNames")
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
        settings.setCurrentRestrictions(genRestrictionsFromBools(false, new int[] {1}));

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
        settings.setCurrentRestrictions(genRestrictionsFromBools(false, new int[] {1}));

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
        settings.setCurrentRestrictions(genRestrictionsFromBools(true, new int[] {1}));
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
        settings.setCurrentRestrictions(genRestrictionsFromBools(true, new int[] {1}));

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



}
