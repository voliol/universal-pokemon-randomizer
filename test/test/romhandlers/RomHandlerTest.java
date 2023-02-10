package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.constants.Gen1Constants;
import com.dabomstew.pkrandom.constants.Gen3Constants;
import com.dabomstew.pkrandom.pokemon.GenRestrictions;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.PokemonSet;
import com.dabomstew.pkrandom.romhandlers.Gen3RomHandler;
import com.dabomstew.pkrandom.romhandlers.RomHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.PrintStream;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Unit tests for the RomHandler classes. Currently, can only load GBA games.
 */
public class RomHandlerTest {

    private static final String GBA_FORMAT = "test/roms/%s.gba";
    private static final String[] ALL_ROMS = {"Ruby (U)", "Ruby (S)", "Ruby (F)", "Emerald (U)", "Emerald (J)"};

    // update if the amount of supported generation increases,
    // and expect some test cases to need updating too, though hopefully only in a minor way
    private static final int HIGHEST_GENERATION = 7;

    private final Gen3RomHandler.Factory factory = new Gen3RomHandler.Factory();
    private RomHandler romHandler;

    public static String[] getROMNames() {
        return ALL_ROMS;
    }

    private void loadROM(String romName) {
        String fullRomName = String.format(GBA_FORMAT, romName);
        if (!factory.isLoadable(fullRomName)) {
            throw new IllegalArgumentException("ROM is not loadable.");
        }
        romHandler = factory.create(new Random(), new PrintStream(System.out));
        romHandler.loadRom(fullRomName);
    }

    @ParameterizedTest
    @MethodSource("getROMNames")
    public void loadingDoesNotGiveNullRomHandler(String romName) {
        loadROM(romName);
        assertNotNull(romHandler);
    }

    @ParameterizedTest
    @MethodSource("getROMNames")
    public void pokemonListIsNotEmpty(String romName) {
        loadROM(romName);
        assertFalse(romHandler.getPokemon().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getROMNames")
    public void firstPokemonInPokemonListIsNull(String romName) {
        loadROM(romName);
        assertNull(romHandler.getPokemon().get(0));
    }

    @ParameterizedTest
    @MethodSource("getROMNames")
    public void numberOfPokemonInPokemonListEqualsGen3Constant(String romName) {
        loadROM(romName);
        assertEquals(Gen3Constants.pokemonCount + 1, romHandler.getPokemon().size());
    }

    @ParameterizedTest
    @MethodSource("getROMNames")
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
    @MethodSource("getROMNames")
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
    @MethodSource("getROMNames")
    public void restrictedPokemonAreSameAsPokemonSetWithNoRestrictionsSet(String romName) {
        loadROM(romName);
        romHandler.setPokemonPool(null);
        assertEquals(romHandler.getPokemonSet(), romHandler.getRestrictedPokemon());
    }

    @ParameterizedTest
    @MethodSource("getROMNames")
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
    @MethodSource("getROMNames")
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
    @MethodSource("getROMNames")
    public void restrictedPokemonWithRelativesDoesNotContainUnrelatedPokemonFromWrongGeneration(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.generationOfPokemon() >= 2);

        Settings settings = new Settings();
        settings.setLimitPokemon(true);
        settings.setCurrentRestrictions(genRestrictionsFromBools(true, new int[] {1}));

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
    @MethodSource("getROMNames")
    public void restrictedPokemonWithRelativesAlwaysContainsRelatedPokemonFromWrongGeneration(String romName) {
        fail();
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
    @MethodSource("getROMNames")
    public void movesAreNotNull(String romName) {
        loadROM(romName);
        assertNotNull(romHandler.getMoves());
    }

    @ParameterizedTest
    @MethodSource("getROMNames")
    public void movesAreNotEmpty(String romName) {
        loadROM(romName);
        System.out.println(romHandler.getMoves());
        assertFalse(romHandler.getMoves().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getROMNames")
    public void firstMoveIsNull(String romName) {
        loadROM(romName);
        assertNull(romHandler.getMoves().get(0));
    }



}
