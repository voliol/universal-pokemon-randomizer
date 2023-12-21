package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.Effectiveness;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Type;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RomHandlerStarterTest extends RomHandlerTest {

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void startersAreNotEmpty(String romName) {
        loadROM(romName);
        assertFalse(romHandler.getStarters().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void startersDoNotChangeWithGetAndSet(String romName) {
        loadROM(romName);
        List<Pokemon> starters = romHandler.getStarters();
        System.out.println(starters);
        List<Pokemon> before = new ArrayList<>(starters);
        romHandler.setStarters(starters);
        assertEquals(before, romHandler.getStarters());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void startersCanBeRandomizedAndGetAndSet(String romName) {
        loadROM(romName);
        Settings settings = new Settings();
        settings.setStartersMod(false, false, true);
        System.out.println(settings.getStartersMod());
        romHandler.randomizeStarters(settings);
        List<Pokemon> starters = romHandler.getStarters();
        List<Pokemon> before = new ArrayList<>(starters);
        romHandler.setStarters(starters);
        assertEquals(before, romHandler.getStarters());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void completelyRandomChangesStarterLineup(String romName) {
        loadROM(romName);

        List<Pokemon> before = new ArrayList<>(romHandler.getStarters());

        Settings settings = new Settings();
        settings.setStartersMod(false, false, true);
        romHandler.randomizeStarters(settings);

        System.out.println("Before: " + before);
        System.out.println("After: " + romHandler.getStarters());
        assertNotEquals(before, romHandler.getStarters());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void customStartersCanBeSet(String romName) {
        loadROM(romName);
        Settings settings = new Settings();
        settings.setStartersMod(false, true, false);
        settings.setCustomStarters(new int[]{1, 2, 3});

        romHandler.randomizeStarters(settings);

        List<Pokemon> starters = romHandler.getStarters();
        List<Pokemon> allPokes = romHandler.getPokemon();

        System.out.println("Starters (should be Bulbasaur, Ivysaur, Venusaur): " + starters);
        assertEquals(starters.get(0), allPokes.get(1));
        assertEquals(starters.get(1), allPokes.get(2));
        assertEquals(starters.get(2), allPokes.get(3));
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void fwgTriangleWorksWithCompletelyRandom(String romName) {
        loadROM(romName);
        Settings settings = new Settings();
        settings.setStartersMod(false, false, true, false, false);
        settings.setStartersTypeMod(false, true, false, false, false);
        romHandler.randomizeStarters(settings);

        fwgCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void fwgTriangleWorksWithRandomWithTwoEvos(String romName) {
        loadROM(romName);
        Settings settings = new Settings();
        settings.setStartersMod(false, false, false, true, false);
        settings.setStartersTypeMod(false, true, false, false, false);
        romHandler.randomizeStarters(settings);

        fwgCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void fwgTriangleWorksWithRandomBasic(String romName) {
        loadROM(romName);
        Settings settings = new Settings();
        settings.setStartersMod(false, false, false, false, true);
        settings.setStartersTypeMod(false, true, false, false, false);
        romHandler.randomizeStarters(settings);

        fwgCheck();
    }

    private void fwgCheck() {
        List<Pokemon> starters = romHandler.getStarters();
        // they should always be in this order
        Pokemon fireStarter = starters.get(0);
        Pokemon waterStarter = starters.get(1);
        Pokemon grassStarter = starters.get(2);
        System.out.println("Fire Starter: " + fireStarter);
        assertTrue(fireStarter.getPrimaryType() == Type.FIRE || fireStarter.getSecondaryType() == Type.FIRE);
        System.out.println("Water Starter: " + waterStarter);
        assertTrue(waterStarter.getPrimaryType() == Type.WATER || waterStarter.getSecondaryType() == Type.WATER);
        System.out.println("Grass Starter: " + grassStarter);
        assertTrue(grassStarter.getPrimaryType() == Type.GRASS || grassStarter.getSecondaryType() == Type.GRASS);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void typeTriangleWorksWithCompletelyRandom(String romName) {
        loadROM(romName);
        Settings settings = new Settings();
        settings.setStartersMod(false, false, true, false, false);
        settings.setStartersTypeMod(false, false, true, false, false);
        romHandler.randomizeStarters(settings);

        typeTriangleCheck(getGenerationNumberOf(romName));
    }

    private void typeTriangleCheck(int generation) {
        // Checks only for type triangles going in the same direction as the vanilla starter's triangle
        // (or technically, how the vanilla starters are read by the randomizer),
        // since triangles in the other direction might mess up rival effectiveness
        List<Pokemon> starters = romHandler.getStarters();
        System.out.println(starters);
        Type type0 = starters.get(0).getPrimaryType();
        Type type1 = starters.get(1).getPrimaryType();
        Type type2 = starters.get(2).getPrimaryType();
        System.out.println("Supposed type triangle: " + type0 + " <= " + type1 + " <= " + type2 + " <= " + type0);

        assertTrue(isSuperEffectiveAgainst(type1, type0, generation));
        assertTrue(isSuperEffectiveAgainst(type2, type1, generation));
        assertTrue(isSuperEffectiveAgainst(type0, type2, generation));
    }

    private boolean isSuperEffectiveAgainst(Type attacker, Type defender, int generation) {
        System.out.println("Is " + attacker + " super-effective against " + defender + " ?");
        System.out.println(Effectiveness.superEffective(attacker, generation, false));
        boolean isSuperEffective = Effectiveness.superEffective(attacker, generation, false).contains(defender);
        System.out.println(isSuperEffective);
        return isSuperEffective;
    }

}
