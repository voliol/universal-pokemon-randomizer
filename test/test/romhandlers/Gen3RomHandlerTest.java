package test.romhandlers;

import com.dabomstew.pkrandom.constants.Gen3Constants;
import com.dabomstew.pkrandom.constants.Species;
import com.dabomstew.pkrandom.pokemon.Move;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.romhandlers.Gen3RomHandler;
import com.dabomstew.pkrandom.romhandlers.RomHandler;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class Gen3RomHandlerTest {

    private static final String RUBY_U_FILENAME = "test/roms/Ruby (U).gba";

    private static final String[] ROMS_TO_TEST = {"Ruby (U)", "Ruby (S)", "Ruby (F)", "Emerald (J)"};

    private final Gen3RomHandler.Factory factory = new Gen3RomHandler.Factory();
    private RomHandler romHandler;

    @Before
    public void loadRubyU() {
        assertTrue(factory.isLoadable(RUBY_U_FILENAME));
        romHandler = factory.create(new Random(), new PrintStream(System.out));
        romHandler.loadRom(RUBY_U_FILENAME);
    }

    @Test
    public void rubyUHasRightROMCodeAndName() {
        assertEquals("AXVE", romHandler.getROMCode());
        assertEquals("Ruby (U)", romHandler.getROMName());
    }

    @Test
    public void getPokemon() {
        assertNull(romHandler.getPokemon().get(0));
        assertEquals("BULBASAUR", romHandler.getPokemon().get(Species.bulbasaur).getName());
        assertEquals("DEOXYS", romHandler.getPokemon().get(Species.deoxys).getName());
        assertEquals(Gen3Constants.pokemonCount + 1, romHandler.getPokemon().size());
    }

    @Test
    public void getMoves() {
        romHandler.getMoves().forEach(System.out::println);
    }

    @Test
    public void foobar() {
        List<Move> before = new ArrayList<>(romHandler.getMoves());
        romHandler.randomizeMoveTypes();
        assertNotEquals(before, romHandler.getMoves());
    }

    // TODO: set up a framework for unit testing various different ROMs

}
