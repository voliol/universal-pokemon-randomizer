package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.Evolution;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RomHandlerEvolutionTest extends RomHandlerTest {

    private static final double MAX_AVERAGE_POWER_LEVEL_DIFF = 0.065;

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void pokemonHaveEvolutions(String romName) {
        loadROM(romName);
        boolean hasEvolutions = false;
        for (Pokemon pk : romHandler.getPokemonSet()) {
            if (!pk.getEvolutionsFrom().isEmpty()) {
                hasEvolutions = true;
                break;
            }
        }
        assertTrue(hasEvolutions);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomSameTypingGivesEvosWithSomeSharedType(String romName) {
        loadROM(romName);

        Settings s = new Settings();
        s.setEvolutionsMod(false, true, false);
        s.setEvosSameTyping(true);
        romHandler.randomizeEvolutions(s);

        evosHaveSharedTypeCheck();
    }

    private void evosHaveSharedTypeCheck() {
        for (Pokemon pk : romHandler.getPokemonSet()) {
            System.out.println(toStringWithTypes(pk) + " ->");
            for (Evolution evo : pk.getEvolutionsFrom()) {
                System.out.print("\t" + toStringWithTypes(evo.getTo()));
                if (evo.isCarryStats()) {
                    assertTrue(shareType(pk, evo.getTo()));
                } else {
                    System.out.print(" (no carry)");
                }
                System.out.println();
            }
        }
    }

    private boolean shareType(Pokemon a, Pokemon b) {
        if (a.getPrimaryType().equals(b.getPrimaryType()) || a.getPrimaryType().equals(b.getSecondaryType())) {
            return true;
        }
        else if (a.getSecondaryType() != null) {
            return a.getSecondaryType().equals(b.getPrimaryType()) || a.getSecondaryType().equals(b.getSecondaryType());
        }
        return false;
    }

    private String toStringWithTypes(Pokemon pk) {
        return pk.getName() + "(" + pk.getPrimaryType() + (pk.getSecondaryType() == null ? "" : "/" + pk.getSecondaryType()) + ")";
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomLimitEvosToThreeStagesWorks(String romName) {
        loadROM(romName);

        Settings s = new Settings();
        s.setEvolutionsMod(false, true, false);
        s.setEvosMaxThreeStages(true);
        romHandler.randomizeEvolutions(s);

        maxThreeEvoStagesCheck();
    }

    private void maxThreeEvoStagesCheck() {
        for (Pokemon pk : romHandler.getPokemonSet()) {
            int evostages = evoStagesAfter(pk, 1);
            System.out.println(evostages);
            assertTrue(evostages <= 3);
        }
    }

    private int evoStagesAfter(Pokemon pk, int count) {
        System.out.println(" ".repeat(count-1) + pk.getName());
        int max = count++;
        for (Evolution evo : pk.getEvolutionsFrom()) {
            max = Math.max(max, evoStagesAfter(evo.getTo(), count));
        }
        return max;
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomSameTypingANDLimitEvosToThreeStagesWorks(String romName) {
        loadROM(romName);

        Settings s = new Settings();
        s.setEvolutionsMod(false, true, false);
        s.setEvosSameTyping(true);
        s.setEvosMaxThreeStages(true);
        romHandler.randomizeEvolutions(s);

        evosHaveSharedTypeCheck();
        maxThreeEvoStagesCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void forceChangeWorks(String romName) {
        loadROM(romName);

        Map<Pokemon, List<Pokemon>> allEvosBefore = new HashMap<>();
        for (Pokemon pk : romHandler.getPokemonSet()) {
            allEvosBefore.put(pk, pk.getEvolutionsFrom().stream().map(Evolution::getTo).toList());
        }

        Settings s = new Settings();
        s.setEvolutionsMod(false, true, false);
        s.setEvosForceChange(true);
        romHandler.randomizeEvolutions(s);

        for (Pokemon pk : romHandler.getPokemonSet()) {
            List<Pokemon> evosBefore = allEvosBefore.get(pk);
            for (Evolution evo : pk.getEvolutionsFrom()) {
                System.out.println(evo);
                assertFalse(evosBefore.contains(evo.getTo()));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void similarStrengthWorks(String romName) {
        loadROM(romName);

        Map<Pokemon, List<Pokemon>> allEvosBefore = new HashMap<>();
        for (Pokemon pk : romHandler.getPokemonSet()) {
            allEvosBefore.put(pk, pk.getEvolutionsFrom().stream().map(Evolution::getTo).toList());
        }

        Settings s = new Settings();
        s.setEvolutionsMod(false, true, false);
        s.setEvosSimilarStrength(true);
        romHandler.randomizeEvolutions(s);

        List<Double> diffs = new ArrayList<>();
        for (Pokemon pk : romHandler.getPokemonSet()) {
            for (int i = 0; i < pk.getEvolutionsFrom().size(); i++) {
                Pokemon before = allEvosBefore.get(pk).get(i);
                Pokemon after = pk.getEvolutionsFrom().get(i).getTo();
                diffs.add(calcPowerLevelDiff(before, after));
            }
        }

        double averageDiff = diffs.stream().mapToDouble(d -> d).average().getAsDouble();
        System.out.println(diffs);
        System.out.println(averageDiff);
        assertTrue(averageDiff <= MAX_AVERAGE_POWER_LEVEL_DIFF);
    }

    private double calcPowerLevelDiff(Pokemon a, Pokemon b) {
        return Math.abs((double) a.bstForPowerLevels() /
                b.bstForPowerLevels() - 1);
    }

}
