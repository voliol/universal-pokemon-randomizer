package test.romhandlers;

import com.dabomstew.pkrandom.romhandlers.RomHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for the RomHandler classes. Just the base functionality here, the tests themselves are in subclasses.
 */
public class RomHandlerTest {

    // update if the amount of supported generation increases,
    // and expect some test cases to need updating too, though hopefully only in a minor way
    protected static final int HIGHEST_GENERATION = 7;

    private static final String TEST_ROMS_PATH = "test/roms";
    private static final String LAST_DOT_REGEX = "\\.+(?![^.]*\\.)";

    public static String[] getRomNames() {
        return Roms.getRoms(new int[]{1, 2}, Roms.Region.values(), false);
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

    protected RomHandler romHandler;

    protected void loadROM(String romName) {
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

    protected Generation getGenerationOf(String romName) {
        return Generation.GAME_TO_GENERATION.get(stripToBaseRomName(romName));
    }

    /**
     * Used to fast check the gen number of a ROM. Really useful for assume... methods, since loading the ROM to use
     * RomHandler.generationOfPokemon() is almost as slow as running the test cases. Increasingly relevant with
     * newer/bigger ROMs involved.
     */
    protected int getGenerationNumberOf(String romName) {
        return getGenerationOf(romName).getNumber();
    }

    /**
     * A fast check whether a ROM uses an AbstractGBRomHandler.
     */
    protected boolean isGBGame(String romName) {
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

}
