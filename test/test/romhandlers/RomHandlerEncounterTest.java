package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.constants.*;
import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.romhandlers.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class RomHandlerEncounterTest extends RomHandlerTest {

    private static final double MAX_AVERAGE_POWER_LEVEL_DIFF = 0.065;

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void encountersAreNotEmpty(String romName) {
        loadROM(romName);
        assertFalse(romHandler.getEncounters(false).isEmpty());
        assertFalse(romHandler.getEncounters(true).isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void encountersDoNotChangeWithGetAndSetNotUsingTimeOfDay(String romName) {
        loadROM(romName);
        List<EncounterArea> encounterAreas = romHandler.getEncounters(false);
        System.out.println(encounterAreas);
        List<EncounterArea> before = new ArrayList<>(encounterAreas);
        romHandler.setEncounters(false, encounterAreas);
        assertEquals(before, romHandler.getEncounters(false));
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void encountersDoNotChangeWithGetAndSetUsingTimeOfDay(String romName) {
        loadROM(romName);
        List<EncounterArea> encounterAreas = romHandler.getEncounters(true);
        System.out.println(encounterAreas);
        List<EncounterArea> before = new ArrayList<>(encounterAreas);
        romHandler.setEncounters(true, encounterAreas);
        assertEquals(before, romHandler.getEncounters(true));
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void encountersAreIdenticalToEarlierRandomizerCodeOutput(String romName) throws IOException {
        loadROM(romName);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        List<EncounterArea> noTimeOfDay = romHandler.getEncounters(false);
        List<EncounterArea> useTimeOfDay = romHandler.getEncounters(true);

        pw.println("useTimeOfDay=false");
        pw.println(encounterAreasToMultilineString(noTimeOfDay));
        pw.println("");
        pw.println("useTimeOfDay=true");
        pw.println(encounterAreasToMultilineString(useTimeOfDay));
        pw.close();

        String orig = Files.readString(Path.of("test/resources/encounters/" + romHandler.getROMName() + ".txt"));
        assertEquals(orig.replaceAll("\r\n", "\n"),
                sw.toString().replaceAll("\r\n", "\n"));
    }

    private String encounterAreasToMultilineString(List<EncounterArea> encounterAreas) {
        StringBuilder sb = new StringBuilder();
        sb.append("[EncounterAreas:");
        for (EncounterArea area : encounterAreas) {
            sb.append(String.format("\n\t[Name = %s, Rate = %d, Offset = %d,",
                    area.getDisplayName(), area.getRate(), area.getOffset()));
            sb.append(String.format("\n\t\tEncounters = %s", new ArrayList<>(area)));
            if (!area.getBannedPokemon().isEmpty()) {
                sb.append(String.format("\n\t\tBanned = %s", area.getBannedPokemon()));
            }
            sb.append("]");
        }
        sb.append("\n]");
        return sb.toString();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void allEncounterAreasHaveALocationTagDontUseTimeOfDay(String romName) {
        loadROM(romName);
        for (EncounterArea area : romHandler.getEncounters(false)) {
            assertNotNull(area.getLocationTag());
            assertNotEquals("", area.getLocationTag());
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void allEncounterAreasHaveALocationTagUseTimeOfDay(String romName) {
        loadROM(romName);
        for (EncounterArea area : romHandler.getEncounters(true)) {
            assertNotNull(area.getLocationTag());
            assertNotEquals("", area.getLocationTag());
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void allLocationTagsAreFoundInTraverseOrder(String romName) {
        assumeTrue(getGenerationNumberOf(romName) <= 5);
        loadROM(romName);
        Set<String> inOrder = new HashSet<>(getLocationTagsTraverseOrder());
        Set<String> used = new HashSet<>();
        for (EncounterArea area : romHandler.getEncounters(false)) {
            used.add(area.getLocationTag());
        }
        for (EncounterArea area : romHandler.getEncounters(true)) {
            used.add(area.getLocationTag());
        }
        System.out.println("In traverse order:\n" + inOrder);
        System.out.println("Used:\n" + used);
        Set<String> notUsed = new HashSet<>(used);
        notUsed.removeAll(inOrder);
        System.out.println("Used but not in traverse order:\n" + notUsed);
        assertTrue(notUsed.isEmpty());
    }

    private List<String> getLocationTagsTraverseOrder() {
        if (romHandler instanceof Gen1RomHandler) {
            return Gen1Constants.locationTagsTraverseOrder;
        } else if (romHandler instanceof Gen2RomHandler) {
            return Gen2Constants.locationTagsTraverseOrder;
        } else if (romHandler instanceof Gen3RomHandler gen3RomHandler) {
            return (gen3RomHandler.getRomEntry().getRomType() == Gen3Constants.RomType_FRLG ?
                    Gen3Constants.locationTagsTraverseOrderFRLG : Gen3Constants.locationTagsTraverseOrderRSE);
        } else if (romHandler instanceof Gen4RomHandler gen4RomHandler) {
            return (gen4RomHandler.getRomEntry().getRomType() == Gen4Constants.Type_HGSS ?
                    Gen4Constants.locationTagsTraverseOrderHGSS : Gen4Constants.locationTagsTraverseOrderDPPt);
        } else if (romHandler instanceof Gen5RomHandler gen5RomHandler) {
            return (gen5RomHandler.getRomEntry().getRomType() == Gen5Constants.Type_BW2 ?
                    Gen5Constants.locationTagsTraverseOrderBW2 : Gen5Constants.locationTagsTraverseOrderBW);
        }
        return Collections.emptyList();
    }

    private void checkForNoLegendaries() {
        for (EncounterArea area : romHandler.getEncounters(true)) {
            System.out.println(area.getDisplayName() + ":");
            System.out.println(area);
            for (Encounter enc : area) {
                assertFalse(enc.getPokemon().isLegendary());
            }
        }
    }

    /**
     * not to be confused with {@link #checkForAlternateFormes()}
     */
    private void checkForNoAlternateFormes() {
        for (EncounterArea area : romHandler.getEncounters(true)) {
            System.out.println(area.getDisplayName() + ":");
            System.out.println(area);
            for (Encounter enc : area) {
                assertNull(enc.getPokemon().getBaseForme());
            }
        }
    }

    /**
     * not to be confused with {@link #checkForNoAlternateFormes()}
     */
    private void checkForAlternateFormes() {
        boolean hasAltFormes = false;
        for (EncounterArea area : romHandler.getEncounters(true)) {
            System.out.println(area.getDisplayName() + ":");
            System.out.println(area);
            for (Encounter enc : area) {
                if (enc.getPokemon().getBaseForme() != null) {
                    System.out.println(enc.getPokemon());
                    hasAltFormes = true;
                    break;
                }
            }
            if (hasAltFormes) {
                break;
            }
        }
        assertTrue(hasAltFormes);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersCanBanLegendaries(String romName) {
        loadROM(romName);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.RANDOM, true,
                false, false, false, true, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        checkForNoLegendaries();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersCanBanAltFormes(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 5);
        loadROM(romName);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.RANDOM, true,
                false, false, false, false, false,
                0, false, true, false);
        checkForNoAlternateFormes();
    }

    // since alt formes are not guaranteed, this test can be considered "reverse";
    // any success is a success for the test as a whole
    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersCanHaveAltFormesIfNotBanned(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 5);
        loadROM(romName);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.RANDOM, true,
                false, false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        checkForAlternateFormes();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersCatchEmAllWorks(String romName) {
        loadROM(romName);
        PokemonSet<Pokemon> allPokes = romHandler.getPokemonSet();
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.RANDOM, true,
                true, false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        catchEmAllCheck(allPokes);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersTypeThemedWorks(String romName) {
        loadROM(romName);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.RANDOM, true,
                false, true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        typeThemedAreasCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersUsePowerLevelsWorks(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.RANDOM, true,
                false, false, true, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        powerLevelsCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersCatchEmAllANDTypeThemedWorks(String romName) {
        loadROM(romName);
        PokemonSet<Pokemon> allPokes = romHandler.getPokemonSet();
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.RANDOM, true,
                true, true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        catchEmAllCheck(allPokes);
        typeThemedAreasCheck();
    }

    private double calcPowerLevelDiff(Pokemon a, Pokemon b) {
        return Math.abs((double) a.bstForPowerLevels() /
                b.bstForPowerLevels() - 1);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesConsequentReplacementsForEachMon(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                false, false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1ConsequentReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesConsequentReplacementsForEachMonWithCatchEmAll(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                true, false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1ConsequentReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesConsequentReplacementsForEachMonWithTypeThemed(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                false, true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1ConsequentReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesConsequentReplacementsForEachMonWithUsePowerLevels(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                false, false, true, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1ConsequentReplacementCheck(before, after);
    }

    private static void area1to1ConsequentReplacementCheck(List<EncounterArea> before, List<EncounterArea> after) {
        Iterator<EncounterArea> beforeIterator = before.iterator();
        Iterator<EncounterArea> afterIterator = after.iterator();
        while (beforeIterator.hasNext()) {
            Map<Pokemon, Pokemon> map = new HashMap<>();
            EncounterArea beforeArea = beforeIterator.next();
            EncounterArea afterArea = afterIterator.next();
            if (!beforeArea.getDisplayName().equals(afterArea.getDisplayName())) {
                throw new RuntimeException("Area mismatch; " + beforeArea.getDisplayName() + " and "
                        + afterArea.getDisplayName());
            }

            System.out.println(beforeArea.getDisplayName() + ":");
            System.out.println(beforeArea);
            System.out.println(afterArea);
            Iterator<Encounter> beforeEncIterator = beforeArea.iterator();
            Iterator<Encounter> afterEncIterator = afterArea.iterator();
            while (beforeEncIterator.hasNext()) {
                Pokemon beforePk = beforeEncIterator.next().getPokemon();
                Pokemon afterPk = afterEncIterator.next().getPokemon();

                if (!map.containsKey(beforePk)) {
                    map.put(beforePk, afterPk);
                }
                assertEquals(map.get(beforePk), afterPk);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesUniqueReplacementsForEachMon(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                false, false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1UniqueReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesUniqueReplacementsForEachMonWithCatchEmAll(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                true, false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1UniqueReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesUniqueReplacementsForEachMonWithTypeThemed(String romName) {
        assumeTrue(getGenerationNumberOf(romName) > 2); // Too few mons of some types, so it always fails
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                false, true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1UniqueReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesUniqueReplacementsForEachMonWithUsePowerLevels(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                false, false, true, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1UniqueReplacementCheck(before, after);
    }

    private void area1to1UniqueReplacementCheck(List<EncounterArea> before, List<EncounterArea> after) {
        Iterator<EncounterArea> beforeIterator = before.iterator();
        Iterator<EncounterArea> afterIterator = after.iterator();
        while (beforeIterator.hasNext()) {
            Map<Pokemon, Pokemon> map = new HashMap<>();

            EncounterArea beforeArea = beforeIterator.next();
            EncounterArea afterArea = afterIterator.next();
            if (!beforeArea.getDisplayName().equals(afterArea.getDisplayName())) {
                throw new RuntimeException("Area mismatch; " + beforeArea.getDisplayName() + " and "
                        + afterArea.getDisplayName());
            }

            System.out.println(beforeArea.getDisplayName() + ":");
            System.out.println(beforeArea);
            System.out.println(afterArea);
            Iterator<Encounter> beforeEncIterator = beforeArea.iterator();
            Iterator<Encounter> afterEncIterator = afterArea.iterator();
            while (beforeEncIterator.hasNext()) {
                Pokemon beforePk = beforeEncIterator.next().getPokemon();
                Pokemon afterPk = afterEncIterator.next().getPokemon();

                if (!map.containsKey(afterPk)) {
                    map.put(afterPk, beforePk);
                }
                assertEquals(map.get(afterPk), beforePk);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersCanBanLegendaries(String romName) {
        loadROM(romName);

        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                false, false, false, true, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        checkForNoLegendaries();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersCanBanAltFormes(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 5);
        loadROM(romName);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                false, false, false, false, false,
                0, false, true, false);
        checkForNoAlternateFormes();
    }

    // since alt formes are not guaranteed, this test can be considered "reverse";
    // any success is a success for the test as a whole
    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersCanHaveAltFormesIfNotBanned(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 5);
        loadROM(romName);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                false, false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        checkForAlternateFormes();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersCatchEmAllWorks(String romName) {
        loadROM(romName);
        PokemonSet<Pokemon> allPokes = romHandler.getPokemonSet();
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                true, false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        catchEmAllCheck(allPokes);
    }

    private void catchEmAllCheck(PokemonSet<Pokemon> allPokes) {
        PokemonSet<Pokemon> catchable = new PokemonSet<>();
        for (EncounterArea area : romHandler.getEncounters(true)) {
            catchable.addAll(PokemonSet.inArea(area));
        }
        allPokes.removeAll(catchable);
        System.out.println(allPokes);
        assertTrue(allPokes.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersTypeThemedWorks(String romName) {
        loadROM(romName);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                false, true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        typeThemedAreasCheck();
    }

    private void typeThemedAreasCheck() {
        for (EncounterArea area : romHandler.getEncounters(true)) {
            System.out.println("\n" + area.getDisplayName() + ":\n" + area);
            Pokemon firstPk = area.get(0).getPokemon();
            PokemonSet<Pokemon> allInArea = PokemonSet.inArea(area);

            Type primaryType = firstPk.getPrimaryType();
            PokemonSet<Pokemon> ofFirstType = PokemonSet.inArea(area).filterByType(primaryType);
            PokemonSet<Pokemon> notOfFirstType = new PokemonSet<>(allInArea).filter(pk -> !ofFirstType.contains(pk));
            System.out.println(notOfFirstType);

            if (!notOfFirstType.isEmpty()) {
                System.out.println("Not " + primaryType);
                Type secondaryType = firstPk.getSecondaryType();
                if (secondaryType == null) {
                    fail();
                }
                PokemonSet<Pokemon> ofSecondaryType = PokemonSet.inArea(area).filterByType(secondaryType);
                PokemonSet<Pokemon> notOfSecondaryType = new PokemonSet<>(allInArea)
                        .filter(pk -> !ofSecondaryType.contains(pk));
                System.out.println(notOfSecondaryType);
                if (!notOfSecondaryType.isEmpty()) {
                    System.out.println("Not " + secondaryType);
                    fail();
                } else {
                    System.out.println(secondaryType);
                }
            } else {
                System.out.println(primaryType);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersUsePowerLevelsWorks(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                false, false, true, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        powerLevelsCheck(before, after);
    }

    private void powerLevelsCheck(List<EncounterArea> before, List<EncounterArea> after) {
        List<Double> diffs = new ArrayList<>();
        Iterator<EncounterArea> beforeIterator = before.iterator();
        Iterator<EncounterArea> afterIterator = after.iterator();
        while (beforeIterator.hasNext()) {
            EncounterArea beforeArea = beforeIterator.next();
            EncounterArea afterArea = afterIterator.next();
            if (!beforeArea.getDisplayName().equals(afterArea.getDisplayName())) {
                throw new RuntimeException("Area mismatch; " + beforeArea.getDisplayName() + " and "
                        + afterArea.getDisplayName());
            }

            Iterator<Encounter> beforeEncIterator = beforeArea.iterator();
            Iterator<Encounter> afterEncIterator = afterArea.iterator();
            while (beforeEncIterator.hasNext()) {
                Pokemon beforePk = beforeEncIterator.next().getPokemon();
                Pokemon afterPk = afterEncIterator.next().getPokemon();
                diffs.add(calcPowerLevelDiff(beforePk, afterPk));
            }
        }

        double averageDiff = diffs.stream().mapToDouble(d -> d).average().getAsDouble();
        System.out.println(diffs);
        System.out.println(averageDiff);
        assertTrue(averageDiff <= MAX_AVERAGE_POWER_LEVEL_DIFF);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersCatchEmAllANDTypeThemedWorks(String romName) {
        loadROM(romName);
        PokemonSet<Pokemon> allPokes = romHandler.getPokemonSet();
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.AREA_MAPPING, true,
                true, true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        catchEmAllCheck(allPokes);
        typeThemedAreasCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void game1to1EncountersGivesConsequentReplacementsForEachMon(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true); // TODO: deep copy just in case
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.GLOBAL_MAPPING, true,
                false, false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);
        Map<Pokemon, Pokemon> map = new HashMap<>();

        Iterator<EncounterArea> beforeIterator = before.iterator();
        Iterator<EncounterArea> afterIterator = after.iterator();
        while (beforeIterator.hasNext()) {
            EncounterArea beforeArea = beforeIterator.next();
            EncounterArea afterArea = afterIterator.next();
            if (!beforeArea.getDisplayName().equals(afterArea.getDisplayName())) {
                throw new RuntimeException("Area mismatch; " + beforeArea.getDisplayName() + " and "
                        + afterArea.getDisplayName());
            }

            System.out.println(beforeArea.getDisplayName() + ":");
            System.out.println(beforeArea);
            System.out.println(afterArea);
            Iterator<Encounter> beforeEncIterator = beforeArea.iterator();
            Iterator<Encounter> afterEncIterator = afterArea.iterator();
            while (beforeEncIterator.hasNext()) {
                Pokemon beforePk = beforeEncIterator.next().getPokemon();
                Pokemon afterPk = afterEncIterator.next().getPokemon();

                if (!map.containsKey(beforePk)) {
                    map.put(beforePk, afterPk);
                }
                assertEquals(map.get(beforePk), afterPk);
            }
        }

        System.out.println(pokemapToString(map));
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void game1to1EncountersGivesUniqueReplacementsForEachMon(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true); // TODO: deep copy just in case
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.GLOBAL_MAPPING, true,
                false, false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);
        Map<Pokemon, Pokemon> map = new HashMap<>();

        Iterator<EncounterArea> beforeIterator = before.iterator();
        Iterator<EncounterArea> afterIterator = after.iterator();
        while (beforeIterator.hasNext()) {
            EncounterArea beforeArea = beforeIterator.next();
            EncounterArea afterArea = afterIterator.next();
            if (!beforeArea.getDisplayName().equals(afterArea.getDisplayName())) {
                throw new RuntimeException("Area mismatch; " + beforeArea.getDisplayName() + " and "
                        + afterArea.getDisplayName());
            }

            System.out.println(beforeArea.getDisplayName() + ":");
            System.out.println(beforeArea);
            System.out.println(afterArea);
            Iterator<Encounter> beforeEncIterator = beforeArea.iterator();
            Iterator<Encounter> afterEncIterator = afterArea.iterator();
            while (beforeEncIterator.hasNext()) {
                Pokemon beforePk = beforeEncIterator.next().getPokemon();
                Pokemon afterPk = afterEncIterator.next().getPokemon();

                if (!map.containsKey(afterPk)) {
                    map.put(afterPk, beforePk);
                }
                assertEquals(map.get(afterPk), beforePk);
            }
        }

        System.out.println(pokemapToString(map));
    }

    private String pokemapToString(Map<Pokemon, Pokemon> map) {
        StringBuilder sb = new StringBuilder("{\n");
        for (Map.Entry<Pokemon, Pokemon> entry : map.entrySet()) {
            sb.append(entry.getKey().getName());
            sb.append(" -> ");
            sb.append(entry.getValue().getName());
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void game1to1EncountersCanBanLegendaries(String romName) {
        loadROM(romName);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.GLOBAL_MAPPING, true,
                false, false, false, true, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        checkForNoLegendaries();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void game1to1EncountersCanBanAltFormes(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 5);
        loadROM(romName);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.GLOBAL_MAPPING, true,
                false, false, false, false, false,
                0, false, true, false);
        checkForNoAlternateFormes();
    }

    // since alt formes are not guaranteed, this test can be considered "reverse";
    // any success is a success for the test as a whole
    @ParameterizedTest
    @MethodSource("getRomNames")
    public void game1to1EncountersCanHaveAltFormesIfNotBanned(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 5);
        loadROM(romName);
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.GLOBAL_MAPPING, true,
                false, false, false, false, false,
                0, true, true, false);
        checkForAlternateFormes();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void game1to1EncountersUsePowerLevelsWorks(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true); // TODO: deep copy just in case
        ((AbstractRomHandler) romHandler).randomizeEncounters(Settings.WildPokemonMod.GLOBAL_MAPPING, true,
                false, false, true, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        powerLevelsCheck(before, after);
    }

}
