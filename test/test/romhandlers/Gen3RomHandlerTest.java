package test.romhandlers;

import com.dabomstew.pkrandom.constants.Gen3Constants;
import com.dabomstew.pkrandom.romhandlers.Gen3RomHandler;
import com.dabomstew.pkrandom.romhandlers.RomHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.PrintStream;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class Gen3RomHandlerTest {

    private static final String GBA_FORMAT = "test/roms/%s.gba";

    private static final String[] ALL_ROMS = {"Ruby (U)", "Ruby (S)", "Ruby (F)", "Emerald (U)", "Emerald (J)"};

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
    public void pokemonIsNotEmpty(String romName) {
        loadROM(romName);
        assertFalse(romHandler.getPokemon().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getROMNames")
    public void firstPokemonIsNull(String romName) {
        loadROM(romName);
        assertNull(romHandler.getPokemon().get(0));
    }

    @ParameterizedTest
    @MethodSource("getROMNames")
    public void numberOfPokemonEqualsGen3Constant(String romName) {
        loadROM(romName);
        assertEquals(Gen3Constants.pokemonCount + 1, romHandler.getPokemon().size());
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

    // TODO: set up a framework for unit testing various different ROMs

}
