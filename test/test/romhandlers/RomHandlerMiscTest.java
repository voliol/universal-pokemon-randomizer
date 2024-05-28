package test.romhandlers;

import com.dabomstew.pkrandom.MiscTweak;
import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.constants.*;
import com.dabomstew.pkrandom.pokemon.GenRestrictions;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.PokemonSet;
import com.dabomstew.pkrandom.romhandlers.AbstractRomHandler;
import com.dabomstew.pkrandom.romhandlers.Gen4RomHandler;
import com.dabomstew.pkrandom.romhandlers.romentries.RomEntry;
import com.dabomstew.pkrandom.services.RestrictedPokemonService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class RomHandlerMiscTest extends RomHandlerTest {

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

    /**
     * Checks all ROMs in the test/roms folder, to see if they pass isRomValid().
     * <br>
     * Since running this may open Gen 6+ ROMs (if you have any), which are very slow to handle,
     * it is disabled by default.
     */
    @Disabled
    @ParameterizedTest
    @MethodSource("getRomNamesInFolder")
    public void romInFolderIsValid(String romName) {
        loadROM(romName);
        assertTrue(romHandler.isRomValid());
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
        RestrictedPokemonService rPokeService = romHandler.getRestrictedPokemonService();
        rPokeService.setRestrictions(null);
        assertEquals(romHandler.getPokemonSet(), rPokeService.getAll(false));
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void restrictedPokemonWithNoRelativesDoesNotContainUnrelatedPokemonFromWrongGeneration(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.generationOfPokemon() >= 2);

        Settings settings = new Settings();
        settings.setLimitPokemon(true);
        settings.setCurrentRestrictions(genRestrictionsFromBools(false, new int[]{1}));

        RestrictedPokemonService rPokeService = romHandler.getRestrictedPokemonService();
        rPokeService.setRestrictions(settings);
        for (Pokemon pk : rPokeService.getAll(false)) {
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

        RestrictedPokemonService rPokeService = romHandler.getRestrictedPokemonService();
        rPokeService.setRestrictions(settings);
        PokemonSet<Pokemon> restrictedPokemon = rPokeService.getAll(false);
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

        RestrictedPokemonService rPokeService = romHandler.getRestrictedPokemonService();
        rPokeService.setRestrictions(settings);
        for (Pokemon pk : rPokeService.getAll(false)) {
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

        RestrictedPokemonService rPokeService = romHandler.getRestrictedPokemonService();
        rPokeService.setRestrictions(settings);
        PokemonSet<Pokemon> restrictedPokemon = rPokeService.getAll(false);
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
    public void canApplyAllLegalTweaksWithoutThrowing(String romName){
        loadROM(romName);

        int codeTweaksAvailable = romHandler.miscTweaksAvailable();
        List<MiscTweak> tweaksToApply = new ArrayList<>();
        for (MiscTweak mt : MiscTweak.allTweaks) {
            if ((codeTweaksAvailable & mt.getValue()) > 0) {
                tweaksToApply.add(mt);
            }
        }

        // Sort so priority is respected in tweak ordering.
        Collections.sort(tweaksToApply);

        // Now apply in order.
        for (MiscTweak mt : tweaksToApply) {
            romHandler.applyMiscTweak(mt);
        }
    }


    @ParameterizedTest
    @MethodSource("getRomNames")
    public void allPokemonHaveAGeneration(String romName){
        loadROM(romName);

        for (Pokemon pk : romHandler.getPokemonSetInclFormes()) {
            System.out.println(pk);
            System.out.println(pk.fullName());
            System.out.println(pk.getGeneration());
            assertNotEquals(-1, pk.getGeneration());
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void testAltFormes(String romName){
        loadROM(romName);

        List<Pokemon> altFormes = new ArrayList<>();
        for (Pokemon pk : romHandler.getPokemonSetInclFormes()) {
            if (pk.getBaseForme() != null) {
                System.out.println(pk.getBaseForme().fullName() + " " + pk.getFormeNumber() + " " + pk.getFormeSuffix()
                        + (pk.isActuallyCosmetic() ? " (cosmetic)" : ""));
                altFormes.add(pk);
            }
        }

        System.out.println(romName + " done");
    }

}
