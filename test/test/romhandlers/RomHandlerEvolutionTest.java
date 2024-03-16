package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.constants.Species;
import com.dabomstew.pkrandom.pokemon.Evolution;
import com.dabomstew.pkrandom.pokemon.EvolutionType;
import com.dabomstew.pkrandom.pokemon.ExpCurve;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
    public void randomNoPokemonEvolvesIntoItself(String romName) {
        loadROM(romName);

        Settings s = new Settings();
        s.setEvolutionsMod(false, true, false);
        romHandler.randomizeEvolutions(s);

        for (Pokemon pk : romHandler.getPokemonSet()) {
            System.out.println(pk.getName());
            for (Evolution evo : pk.getEvolutionsFrom()) {
                System.out.println("\t" + evo.getTo().getName());
                assertNotEquals(pk, evo.getTo());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEvosShareEXPCurveWithPrevo(String romName) {
        loadROM(romName);

        Settings s = new Settings();
        s.setEvolutionsMod(false, true, false);
        romHandler.randomizeEvolutions(s);

        for (Pokemon pk : romHandler.getPokemonSet()) {
            System.out.println(pk.getName() + " " + pk.getGrowthCurve());
            for (Evolution evo : pk.getEvolutionsFrom()) {
                System.out.println("\t" + evo.getTo().getName() + " " + evo.getTo().getGrowthCurve());
                assertEquals(pk.getGrowthCurve(), evo.getTo().getGrowthCurve());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomSplitEvosDoNotChooseTheSamePokemon(String romName) {
        loadROM(romName);

        Settings s = new Settings();
        s.setEvolutionsMod(false, true, false);
        romHandler.randomizeEvolutions(s);

        for (Pokemon pk : romHandler.getPokemonSet()) {
            System.out.println(pk.getName());
            for (Evolution evo : pk.getEvolutionsFrom()) {
                System.out.println("\t" + evo.getTo().getName());
            }
            for (int i = 0; i < pk.getEvolutionsFrom().size(); i++) {
                for (int j = i + 1; j < pk.getEvolutionsFrom().size(); j++) {
                    Pokemon evoI = pk.getEvolutionsFrom().get(i).getTo();
                    Pokemon evoJ = pk.getEvolutionsFrom().get(j).getTo();
                    assertNotEquals(evoI, evoJ);
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomNoEvoCyclesExist(String romName) {
        loadROM(romName);

        Settings s = new Settings();
        s.setEvolutionsMod(false, true, false);
        romHandler.randomizeEvolutions(s);

        for (Pokemon pk : romHandler.getPokemonSet()) {
            System.out.println(pk.getName());
            checkForNoCycles(pk, pk, 1);
        }
    }

    private void checkForNoCycles(Pokemon curr, Pokemon start, int depth) {
        for (Evolution evo : curr.getEvolutionsFrom()) {
            System.out.println(" ".repeat(depth) + evo.getTo().getName());
            assertNotEquals(start, evo.getTo());
            checkForNoCycles(evo.getTo(), start, depth + 1);
        }
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
                    assertTrue(evo.getTo().hasSharedType(pk));
                } else {
                    System.out.print(" (no carry)");
                }
                System.out.println();
            }
        }
    }

    private String toStringWithTypes(Pokemon pk) {
        return pk.getName() + "(" + pk.getPrimaryType() + (pk.getSecondaryType() == null ? "" : "/" + pk.getSecondaryType()) + ")";
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomSameTypingGivesNewEeveelutionsSharingSomeTypeWithTheOriginals(String romName) {
        loadROM(romName);

        Pokemon eeveeBefore = romHandler.getPokemon().get(Species.eevee);
        List<Pokemon> beforeEvos = new ArrayList<>(eeveeBefore.getEvolutionsFrom().size());
        for (Evolution evo : eeveeBefore.getEvolutionsFrom()) {
            Pokemon before = new Pokemon(0);
            before.setName(evo.getTo().getName());
            before.setPrimaryType(evo.getTo().getPrimaryType());
            before.setSecondaryType(evo.getTo().getSecondaryType());
            beforeEvos.add(before);
        }

        Settings s = new Settings();
        s.setEvolutionsMod(false, true, false);
        s.setEvosSameTyping(true);
        romHandler.randomizeEvolutions(s);

        Pokemon eevee = romHandler.getPokemon().get(Species.eevee);
        System.out.println(toStringWithTypes(eevee));
        for (int i = 0; i < eevee.getEvolutionsFrom().size(); i++) {
            Pokemon before = beforeEvos.get(i);
            Pokemon after = eevee.getEvolutionsFrom().get(i).getTo();
            System.out.println("before: " + toStringWithTypes(before));
            System.out.println("after: " + toStringWithTypes(after));
            assertTrue(before.hasSharedType(after));
        }
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
    public void randomForceChangeWorks(String romName) {
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
    public void randomForceChangeWorksForCosmoem(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 7);
        loadROM(romName);

        Settings s = new Settings();
        s.setSelectedEXPCurve(ExpCurve.MEDIUM_FAST);
        s.setStandardizeEXPCurves(true);
        romHandler.standardizeEXPCurves(s);
        s.setEvolutionsMod(false, true, false);
        s.setEvosSimilarStrength(true); // just to increase the likelihood of a failure
        s.setEvosSameTyping(true); // just to increase the likelihood of a failure
        s.setEvosForceChange(true);
        romHandler.randomizeEvolutions(s);

        Pokemon cosmoem = romHandler.getPokemon().get(Species.cosmoem);
        System.out.println(cosmoem.getName());
        for (Evolution evo : cosmoem.getEvolutionsFrom()) {
            System.out.println(evo.getTo().getName());
            assertNotEquals(Species.solgaleo, evo.getTo().getNumber());
            assertNotEquals(Species.lunala, evo.getTo().getNumber());
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomSimilarStrengthWorks(String romName) {
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

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomNoEvoHasLevelFemaleEspurrEvoType(String romName) {
        // Not entirely sure why this has to be the case, but older evolution randomization made sure to get rid of
        // and LEVEL_FEMALE_ESPURR, and so it's carried to newer code as well.
        // Probably there are some issues if LEVEL_FEMALE_ESPURR is used and the Pokemon it evolves to isn't Meowstic.
        loadROM(romName);

        Settings s = new Settings();
        s.setEvolutionsMod(false, true, false);
        romHandler.randomizeEvolutions(s);

        for (Pokemon pk : romHandler.getPokemonSet()) {
            System.out.println(pk.getName());
            for (Evolution evo : pk.getEvolutionsFrom()) {
                System.out.println(evo);
                assertNotEquals(EvolutionType.LEVEL_FEMALE_ESPURR, evo.getType());
            }
        }
    }

    // TODO: test for eevee + same type

}
