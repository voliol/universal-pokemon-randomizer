package test.romhandlers;

import com.dabomstew.pkrandom.pokemon.StaticEncounter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class RomHandlerStaticsTest extends RomHandlerTest {

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void staticPokemonIsNotEmpty(String romName) {
        loadROM(romName);
        System.out.println(romHandler.getStaticPokemon());
        assertFalse(romHandler.getStaticPokemon().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void staticPokemonDoNotChangeWithGetAndSet(String romName) {
        loadROM(romName);
        List<StaticEncounter> statics = romHandler.getStaticPokemon();
        System.out.println(statics);
        List<StaticEncounter> before = new ArrayList<>(statics);
        romHandler.setStaticPokemon(statics);
        assertEquals(before, romHandler.getStaticPokemon());
    }

}
