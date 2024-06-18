package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.StaticEncounter;
import com.dabomstew.pkrandom.randomizers.StaticPokemonRandomizer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        List<StaticEncounter> before = deepCopy(statics);
        romHandler.setStaticPokemon(statics);
        assertEquals(before, romHandler.getStaticPokemon());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomStaticsSwapLegForLegNormalForNormalWorks(String romName) {
        loadROM(romName);
        List<StaticEncounter> before = deepCopy(romHandler.getStaticPokemon());

        Settings s = new Settings();
        s.setStaticPokemonMod(Settings.StaticPokemonMod.RANDOM_MATCHING);
        new StaticPokemonRandomizer(romHandler, s, RND).randomizeStaticPokemon();

        List<StaticEncounter> after = romHandler.getStaticPokemon();
        if (before.size() != after.size()) {
            throw new RuntimeException("static pokemon list mismatch");
        }
        for (int i = 0; i < before.size(); i++) {
            Pokemon befPk = before.get(i).pkmn;
            Pokemon aftPk = after.get(i).pkmn;
            System.out.println("bef=" + befPk.fullName() + (befPk.isLegendary() ? " (legendary)" : "") +
                    ", aft=" + aftPk.fullName() + (aftPk.isLegendary() ? " (legendary)" : ""));
            assertEquals(befPk.isLegendary(), aftPk.isLegendary());
        }

    }

    private List<StaticEncounter> deepCopy(List<StaticEncounter> original) {
        List<StaticEncounter> copy = new ArrayList<>(original.size());
        for (StaticEncounter se : original) {
            copy.add(new StaticEncounter(se));
        }
        return copy;
    }

}
