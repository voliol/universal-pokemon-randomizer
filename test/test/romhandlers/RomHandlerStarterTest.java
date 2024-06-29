package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Type;
import com.dabomstew.pkrandom.randomizers.StarterRandomizer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
        Settings s = new Settings();
        s.setStartersMod(false, false, true);
        System.out.println(s.getStartersMod());
        new StarterRandomizer(romHandler, s, RND).randomizeStarters();
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

        Settings s = new Settings();
        s.setStartersMod(false, false, true);
        new StarterRandomizer(romHandler, s, RND).randomizeStarters();

        System.out.println("Before: " + before);
        System.out.println("After: " + romHandler.getStarters());
        assertNotEquals(before, romHandler.getStarters());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void customStartersCanBeSet(String romName) {
        loadROM(romName);
        Settings s = new Settings();
        s.setStartersMod(false, true, false);
        int customCount = romHandler.starterCount();
        int[] custom = new int[customCount];
        for (int i = 0; i < custom.length; i++) {
            custom[i] = i + 1;
        }
        s.setCustomStarters(custom);

        new StarterRandomizer(romHandler, s, RND).randomizeStarters();

        List<Pokemon> starters = romHandler.getStarters();
        List<Pokemon> allPokes = romHandler.getPokemon();

        StringBuilder sb = new StringBuilder("Starters");
        sb.append(" (should be ");
        for (int i = 0; i < customCount; i++) {
            sb.append(allPokes.get(custom[i]).getName());
            if (i != customCount - 1) {
                sb.append(", ");
            }
        }
        sb.append("): ");
        sb.append(starters);
        System.out.println(sb);

        for (int i = 0; i < customCount; i++) {
            assertEquals(starters.get(i), allPokes.get(custom[i]));
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void completelyRandomWithNoOtherOptionsDoesntThrow(String romName) {
        loadROM(romName);
        Settings s = new Settings();
        s.setStartersMod(false, false, true);
        new StarterRandomizer(romHandler, s, RND).randomizeStarters();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomWithTwoEvosWithNoOtherOptionsDoesntThrow(String romName) {
        loadROM(romName);
        Settings s = new Settings();
        s.setStartersMod(false, false, false, true);
        new StarterRandomizer(romHandler, s, RND).randomizeStarters();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomBasicWithNoOtherOptionsDoesntThrow(String romName) {
        loadROM(romName);
        Settings s = new Settings();
        s.setStartersMod(false, false, false, false, true);
        new StarterRandomizer(romHandler, s, RND).randomizeStarters();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void fwgTriangleWorksWithCompletelyRandom(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.hasStarterTypeTriangleSupport());
        Settings s = new Settings();
        s.setStartersMod(false, false, true, false, false);
        s.setStartersTypeMod(false, true, false, false, false);
        new StarterRandomizer(romHandler, s, RND).randomizeStarters();

        fwgCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void fwgTriangleWorksWithRandomWithTwoEvos(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.hasStarterTypeTriangleSupport());
        Settings s = new Settings();
        s.setStartersMod(false, false, false, true, false);
        s.setStartersTypeMod(false, true, false, false, false);
        new StarterRandomizer(romHandler, s, RND).randomizeStarters();

        fwgCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void fwgTriangleWorksWithRandomBasic(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.hasStarterTypeTriangleSupport());
        Settings s = new Settings();
        s.setStartersMod(false, false, false, false, true);
        s.setStartersTypeMod(false, true, false, false, false);
        new StarterRandomizer(romHandler, s, RND).randomizeStarters();

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
    public void typeTriangleCheckReturnsTrueForVanilla(String romName) {
        loadROM(romName);
        assumeFalse(romHandler.isYellow());
        typeTriangleCheck(getGenerationNumberOf(romName));
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void typeTriangleWorksWithCompletelyRandom(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.hasStarterTypeTriangleSupport());
        Settings s = new Settings();
        s.setStartersMod(false, false, true, false, false);
        s.setStartersTypeMod(false, false, true, false, false);
        new StarterRandomizer(romHandler, s, RND).randomizeStarters();

        typeTriangleCheck(getGenerationNumberOf(romName));
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void typeTriangleWorksWithRandomWithTwoEvos(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.hasStarterTypeTriangleSupport());
        Settings s = new Settings();
        s.setStartersMod(false, false, false, true, false);
        s.setStartersTypeMod(false, false, true, false, false);
        new StarterRandomizer(romHandler, s, RND).randomizeStarters();

        typeTriangleCheck(getGenerationNumberOf(romName));
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void typeTriangleWorksWithRandomBasic(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.hasStarterTypeTriangleSupport());
        Settings s = new Settings();
        s.setStartersMod(false, false, false, false, true);
        s.setStartersTypeMod(false, false, true, false, false);
        new StarterRandomizer(romHandler, s, RND).randomizeStarters();

        typeTriangleCheck(getGenerationNumberOf(romName));
    }

    private void typeTriangleCheck(int generation) {
        // Checks only for type triangles going in the same direction as the vanilla starter's triangle
        // (or technically, how the vanilla starters are read by the randomizer),
        // since triangles in the other direction might mess up rival effectiveness
        List<Pokemon> starters = romHandler.getStarters();
        System.out.println(starters + "\n");

        assertTrue(isSuperEffectiveAgainst(starters.get(1), starters.get(0), generation));
        assertTrue(isSuperEffectiveAgainst(starters.get(2), starters.get(1), generation));
        assertTrue(isSuperEffectiveAgainst(starters.get(0), starters.get(2), generation));
    }

    private boolean isSuperEffectiveAgainst(Pokemon attacker, Pokemon defender, int generation) {
        // just checks whether any of the attacker's types is super effective against any of the defender's,
        // nothing more sophisticated
        System.out.println("Is " + attacker.getName() + " super-effective against " + defender.getName() + " ?");
        boolean isSuperEffective =
                isSuperEffectiveAgainst(attacker.getPrimaryType(), defender.getPrimaryType(), generation) ||
                isSuperEffectiveAgainst(attacker.getPrimaryType(), defender.getSecondaryType(), generation) ||
                isSuperEffectiveAgainst(attacker.getSecondaryType(), defender.getPrimaryType(), generation) ||
                isSuperEffectiveAgainst(attacker.getSecondaryType(), defender.getSecondaryType(), generation);
        System.out.println(isSuperEffective + "\n");
        return isSuperEffective;
    }

    private boolean isSuperEffectiveAgainst(Type attacker, Type defender, int generation) {
        if (attacker == null || defender == null) {
            return false;
        }
        System.out.println("Is " + attacker + " super-effective against " + defender + " ?");
        System.out.println(romHandler.getTypeTable().superEffectiveWhenAttacking(attacker));
        boolean isSuperEffective = romHandler.getTypeTable().superEffectiveWhenAttacking(attacker).contains(defender);
        System.out.println(isSuperEffective);
        return isSuperEffective;
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void uniqueTypesWorksWithCompletelyRandom(String romName) {
        loadROM(romName);
        Settings s = new Settings();
        s.setStartersMod(false, false, true, false, false);
        s.setStartersTypeMod(false, false, false, true, false);
        new StarterRandomizer(romHandler, s, RND).randomizeStarters();

        uniqueTypesCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void uniqueTypesWorksWithRandomWithTwoEvos(String romName) {
        loadROM(romName);
        Settings s = new Settings();
        s.setStartersMod(false, false, false, true, false);
        s.setStartersTypeMod(false, false, false, true, false);
        new StarterRandomizer(romHandler, s, RND).randomizeStarters();

        uniqueTypesCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void uniqueTypesWorksWithRandomBasic(String romName) {
        loadROM(romName);
        Settings s = new Settings();
        s.setStartersMod(false, false, false, false, true);
        s.setStartersTypeMod(false, false, false, true, false);
        new StarterRandomizer(romHandler, s, RND).randomizeStarters();

        uniqueTypesCheck();
    }

    private void uniqueTypesCheck() {
        List<Pokemon> starters = romHandler.getStarters();
        System.out.println(starters);
        for (int i = 0; i < starters.size(); i++) {
            for (int j = i + 1; j < starters.size(); j++) {
                assertFalse(sharesTypes(starters.get(i), starters.get(j)));
            }
        }
    }

    private boolean sharesTypes(Pokemon a, Pokemon b) {
        if (a.getPrimaryType() == b.getPrimaryType()) {
            return true;
        }
        if (b.getSecondaryType() != null) {
            if (a.getPrimaryType() == b.getSecondaryType()) {
                return true;
            }
        }
        if (a.getSecondaryType() != null) {
            return a.getSecondaryType() == b.getPrimaryType() || a.getSecondaryType() == b.getSecondaryType();
        }
        return false;
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void singleTypeWorksWithCompletelyRandom(String romName) {
        loadROM(romName);
        Settings s = new Settings();
        s.setStartersMod(false, false, true, false, false);
        s.setStartersTypeMod(false, false, false, false, true);

        singleTypeCheck(s);
    }

    @Disabled
    @ParameterizedTest
    @MethodSource("getRomNames")
    public void singleTypeWorksWithRandomWithTwoEvos(String romName) {
        // fails on all vanilla games afaik, due to there being only Machop and Timburr that fulfill this for Fighting.
        loadROM(romName);
        Settings s = new Settings();
        s.setStartersMod(false, false, false, true, false);
        s.setStartersTypeMod(false, false, false, false, true);

        singleTypeCheck(s);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void singleTypeWorksWithRandomBasic(String romName) {
        assumeTrue(getGenerationNumberOf(romName) > 2); // Gen 1 & 2 have too few basic Dragon/Ghost types
        loadROM(romName);
        Settings s = new Settings();
        s.setStartersMod(false, false, false, false, true);
        s.setStartersTypeMod(false, false, false, false, true);

        singleTypeCheck(s);
    }

    private void singleTypeCheck(Settings s) {
        for (int i = 0; i < Type.values().length; i++) {
            Type t = Type.values()[i];
            if (romHandler.getTypeService().typeInGame(t)) {
                s.setStartersSingleType(i + 1);
                System.out.println(t);
                new StarterRandomizer(romHandler, s, RND).randomizeStarters();

                List<Pokemon> starters = romHandler.getStarters();
                System.out.println(starters.stream().map(pk -> pk.getName() + " " + pk.getPrimaryType() +
                        (pk.getSecondaryType() == null ? "" : " / " + pk.getSecondaryType())).collect(Collectors.toList()));
                for (Pokemon starter : starters) {
                    assertTrue(starter.getPrimaryType() == t || starter.getSecondaryType() == t);
                }
            }
        }
    }


}
