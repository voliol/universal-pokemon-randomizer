package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.constants.*;
import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.randomizers.EncounterRandomizer;
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
import static org.junit.jupiter.api.Assumptions.assumeFalse;
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
        // This test checks whether you've accidentally broken the reading of encounters
        // by comparing the current output with logged output in text files. If you *intentionally* change something
        // about how the encounters are read, like the EncounterArea names, you can expect this test to fail.
        // In that case, check it only differs in the way you want, and then update the text files.
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
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.RANDOM, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, true, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        checkForNoLegendaries();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersCanBanAltFormes(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 5);
        loadROM(romName);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.RANDOM, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, false, false,
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
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.RANDOM, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        checkForAlternateFormes();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersCatchEmAllWorks(String romName) {
        loadROM(romName);
        PokemonSet allPokes = romHandler.getPokemonSet();
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.RANDOM, Settings.WildPokemonTypeMod.NONE,
                true,
                true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        catchEmAllCheck(allPokes);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersRandomTypeThemesWorks(String romName) {
        loadROM(romName);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.RANDOM, Settings.WildPokemonTypeMod.THEMED_AREAS,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        randomTypeThemesAreasCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersKeepTypeThemesWorks(String romName) {
        loadROM(romName);

        List<List<String>> beforeAreaStrings = new ArrayList<>();
        Map<Integer, Type> typeThemedAreas = new HashMap<>();
        recordTypeThemeBefore(beforeAreaStrings, typeThemedAreas);

        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.RANDOM,
                true,
                false, true, false,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);

        keepTypeThemedAreasCheck(beforeAreaStrings, typeThemedAreas);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersKeepTypeThemesANDRandomTypeThemesWorks(String romName) {
        loadROM(romName);

        List<List<String>> beforeAreaStrings = new ArrayList<>();
        Map<Integer, Type> typeThemedAreas = new HashMap<>();
        recordTypeThemeBefore(beforeAreaStrings, typeThemedAreas);

        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.RANDOM,
                true,
                true, true, false,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);

        keepTypeThemedAreasCheck(beforeAreaStrings, typeThemedAreas);
        randomTypeThemesAreasCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersKeepPrimaryTypeWorks(String romName) {
        loadROM(romName);

        List<List<String>> beforeAreasStrings = new ArrayList<>();
        List<List<Type>> beforePrimaryTypes = new ArrayList<>();
        recordPrimaryTypesBefore(beforeAreasStrings, beforePrimaryTypes);

        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.RANDOM, Settings.WildPokemonTypeMod.KEEP_PRIMARY,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);

        keepPrimaryTypeCheck(beforeAreasStrings, beforePrimaryTypes);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersUsePowerLevelsWorks(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.RANDOM, Settings.WildPokemonTypeMod.NONE,
                true,
                false, true, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        powerLevelsCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersCatchEmAllANDRandomTypeThemesWorks(String romName) {
        loadROM(romName);
        PokemonSet allPokes = romHandler.getPokemonSet();
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.RANDOM, Settings.WildPokemonTypeMod.THEMED_AREAS,
                true,
                true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        catchEmAllCheck(allPokes);
        randomTypeThemesAreasCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersCatchEmAllANDKeepTypeThemesWorks(String romName) {
        loadROM(romName);

        List<List<String>> beforeAreaStrings = new ArrayList<>();
        Map<Integer, Type> typeThemedAreas = new HashMap<>();
        recordTypeThemeBefore(beforeAreaStrings, typeThemedAreas);

        PokemonSet allPokes = romHandler.getPokemonSet();
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.RANDOM,
                true,
                false, true, false,
                true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);

        catchEmAllCheck(allPokes);
        keepTypeThemedAreasCheck(beforeAreaStrings, typeThemedAreas);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomEncountersCatchEmAllANDRandomTypeThemesANDKeepTypeThemesWorks(String romName) {
        loadROM(romName);

        List<List<String>> beforeAreaStrings = new ArrayList<>();
        Map<Integer, Type> typeThemedAreas = new HashMap<>();
        recordTypeThemeBefore(beforeAreaStrings, typeThemedAreas);

        PokemonSet allPokes = romHandler.getPokemonSet();
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.RANDOM,
                true,
                true, true, false,
                true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);

        catchEmAllCheck(allPokes);
        randomTypeThemesAreasCheck();
        keepTypeThemedAreasCheck(beforeAreaStrings, typeThemedAreas);
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
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1ConsequentReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesConsequentReplacementsForEachMonWithCatchEmAll(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1ConsequentReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesConsequentReplacementsForEachMonWithRandomTypeThemes(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.THEMED_AREAS,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1ConsequentReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesConsequentReplacementsForEachMonWithKeepTypeThemes(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING,
                true,
                false, true, false,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1ConsequentReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesConsequentReplacementsForEachMonWithKeepPrimaryType(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.KEEP_PRIMARY,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1ConsequentReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesConsequentReplacementsForEachMonWithUsePowerLevels(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, true, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1ConsequentReplacementCheck(before, after);
    }

    private void area1to1ConsequentReplacementCheck(List<EncounterArea> before, List<EncounterArea> after) {
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
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1UniqueReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesUniqueReplacementsForEachMonWithCatchEmAll(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1UniqueReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesUniqueReplacementsForEachMonWithRandomTypeThemes(String romName) {
        assumeTrue(getGenerationNumberOf(romName) > 2); // Too few mons of some types, so it always fails
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.THEMED_AREAS,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1UniqueReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesUniqueReplacementsForEachMonWithKeepTypeThemes(String romName) {
        assumeTrue(getGenerationNumberOf(romName) > 2); // Too few mons of some types, so it always fails
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING,
                true,
                false, true, false,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1UniqueReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesUniqueReplacementsForEachMonWithKeepPrimaryType(String romName) {
        assumeTrue(getGenerationNumberOf(romName) > 2); // Too few mons of some types, so it always fails
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.KEEP_PRIMARY,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        area1to1UniqueReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersGivesUniqueReplacementsForEachMonWithUsePowerLevels(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, true, false, false,
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

        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, true, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        checkForNoLegendaries();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersCanBanAltFormes(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 5);
        loadROM(romName);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, false, false,
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
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        checkForAlternateFormes();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersCatchEmAllWorks(String romName) {
        loadROM(romName);
        PokemonSet allPokes = romHandler.getPokemonSet();
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        catchEmAllCheck(allPokes);
    }

    private void catchEmAllCheck(PokemonSet allPokes) {
        PokemonSet catchable = new PokemonSet();
        for (EncounterArea area : romHandler.getEncounters(true)) {
            catchable.addAll(PokemonSet.inArea(area));
        }
        PokemonSet notCatchable = new PokemonSet(allPokes);
        notCatchable.removeAll(catchable);
        System.out.println("Not catchable: " + notCatchable.stream().map(Pokemon::getName).toList());
        assertTrue(notCatchable.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersRandomTypeThemesWorks(String romName) {
        loadROM(romName);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.THEMED_AREAS,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        randomTypeThemesAreasCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersKeepTypeThemesWorks(String romName) {
        loadROM(romName);

        List<List<String>> beforeAreaStrings = new ArrayList<>();
        Map<Integer, Type> typeThemedAreas = new HashMap<>();
        recordTypeThemeBefore(beforeAreaStrings, typeThemedAreas);

        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING,
                true,
                false, true, false,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);

        keepTypeThemedAreasCheck(beforeAreaStrings, typeThemedAreas);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersKeepPrimaryTypeWorks(String romName) {
        loadROM(romName);

        List<List<String>> beforeAreasStrings = new ArrayList<>();
        List<List<Type>> beforePrimaryTypes = new ArrayList<>();
        recordPrimaryTypesBefore(beforeAreasStrings, beforePrimaryTypes);

        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.KEEP_PRIMARY,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);

        keepPrimaryTypeCheck(beforeAreasStrings, beforePrimaryTypes);
    }

    private void randomTypeThemesAreasCheck() {
        for (EncounterArea area : romHandler.getEncounters(true)) {
            System.out.println("\n" + area.getDisplayName() + ":\n" + area);
            Pokemon firstPk = area.get(0).getPokemon();
            PokemonSet allInArea = PokemonSet.inArea(area);

            Type primaryType = firstPk.getPrimaryType();
            PokemonSet ofFirstType = PokemonSet.inArea(area).filterByType(primaryType);
            PokemonSet notOfFirstType = new PokemonSet(allInArea).filter(pk -> !ofFirstType.contains(pk));
            System.out.println(notOfFirstType);

            if (!notOfFirstType.isEmpty()) {
                System.out.println("Not " + primaryType);
                Type secondaryType = firstPk.getSecondaryType();
                if (secondaryType == null) {
                    fail();
                }
                PokemonSet ofSecondaryType = PokemonSet.inArea(area).filterByType(secondaryType);
                PokemonSet notOfSecondaryType = new PokemonSet(allInArea)
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

    private String toNameAndTypesString(Pokemon pk) {
        return pk.getName() + ", " + pk.getPrimaryType()
                + (pk.getSecondaryType() == null ? "" : " / " + pk.getSecondaryType());
    }

    private void recordTypeThemeBefore(List<List<String>> beforeAreaStrings, Map<Integer, Type> typeThemedAreas) {
        List<EncounterArea> encounterAreas = romHandler.getEncounters(true);
        for (int i = 0; i < encounterAreas.size(); i++) {
            EncounterArea area = encounterAreas.get(i);
            List<String> beforeStrings = new ArrayList<>();
            beforeAreaStrings.add(beforeStrings);
            beforeStrings.add(area.toString());
            for (Encounter enc : area) {
                beforeStrings.add(toNameAndTypesString(enc.getPokemon()));
            }

            Type theme = getThemedAreaType(area);
            if (theme != null) {
                typeThemedAreas.put(i, theme);
            }
        }
    }

    private Type getThemedAreaType(EncounterArea area) {
        Pokemon first = area.get(0).getPokemon();
        Type primary = first.getOriginalPrimaryType();
        Type secondary = first.getOriginalSecondaryType();
        for (int i = 1; i < area.size(); i++) {
            Pokemon pk = area.get(i).getPokemon();
            if (secondary != null) {
                if (secondary != pk.getOriginalPrimaryType() && secondary != pk.getOriginalSecondaryType()) {
                    secondary = null;
                }
            }
            if (primary != pk.getOriginalPrimaryType() && primary != pk.getOriginalSecondaryType()) {
                primary = secondary;
                secondary = null;
            }
            if (primary == null) {
                return null; //no type is shared, no need to look at the remaining pokemon
            }
        }

        //we have a type theme!
        if (primary == Type.NORMAL && secondary != null) {
            //Bird override
            //(Normal is less significant than other types, for example, Flying)
            return secondary;
        } else {
            return primary;
        }

    }

    private void keepTypeThemedAreasCheck(List<List<String>> beforeAreaStrings, Map<Integer, Type> typeThemedAreas) {
        List<EncounterArea> encounterAreas = romHandler.getEncounters(true);
        for (int i = 0; i < encounterAreas.size(); i++) {
            List<String> beforeStrings = beforeAreaStrings.get(i);
            System.out.println("Before: " + beforeStrings.get(0));
            for (int j = 1; j < beforeStrings.size(); j++) {
                System.out.println("\t" + beforeStrings.get(j));
            }

            EncounterArea area = encounterAreas.get(i);
            if (typeThemedAreas.containsKey(i)) {
                Type theme = typeThemedAreas.get(i);
                System.out.println("Type Theme: " + theme);
                System.out.println("After: " + area);
                for (Encounter enc : area) {
                    Pokemon pk = enc.getPokemon();
                    System.out.println("\t" + toNameAndTypesString(pk));
                    assertTrue(pk.getPrimaryType() == theme || pk.getSecondaryType() == theme);
                }
            } else {
                System.out.println("Not Type Themed");
            }
            System.out.println();
        }
    }

    private void recordPrimaryTypesBefore(List<List<String>> beforeAreaStrings, List<List<Type>> beforePrimaryTypes) {
        List<EncounterArea> encounterAreas = romHandler.getEncounters(true);
        for (int i = 0; i < encounterAreas.size(); i++) {
            EncounterArea area = encounterAreas.get(i);
            List<String> beforeStrings = new ArrayList<>();
            beforeAreaStrings.add(beforeStrings);
            beforeStrings.add(area.toString());
            List<Type> beforeTypes = new ArrayList<>();
            beforePrimaryTypes.add(beforeTypes);
            for (Encounter enc : area) {
                Pokemon pk = enc.getPokemon();
                beforeStrings.add(toNameAndTypesString(pk));
                beforeTypes.add(pk.getPrimaryType());
            }
        }
    }

    private void keepPrimaryTypeCheck(List<List<String>> beforeAreaStrings, List<List<Type>> beforePrimaryTypes) {
        List<EncounterArea> encounterAreas = romHandler.getEncounters(true);
        for (int i = 0; i < encounterAreas.size(); i++) {
            List<String> beforeStrings = beforeAreaStrings.get(i);
            System.out.println("Before: " + beforeStrings.get(0));
            for (int j = 1; j < beforeStrings.size(); j++) {
                System.out.println("\t" + beforeStrings.get(j));
            }

            EncounterArea area = encounterAreas.get(i);
            System.out.println("After: " + area);
            for (int j = 0; j < area.size(); j++) {
                Pokemon pk = area.get(j).getPokemon();
                Type primary = beforePrimaryTypes.get(i).get(j);
                System.out.println("\t" + toNameAndTypesString(pk));
                assertTrue(pk.getPrimaryType() == primary || pk.getSecondaryType() == primary);
            }
            System.out.println();
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersUsePowerLevelsWorks(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, true, false, false,
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
    public void area1to1EncountersCatchEmAllANDRandomTypeThemesWorks(String romName) {
        loadROM(romName);
        PokemonSet allPokes = romHandler.getPokemonSet();
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING, Settings.WildPokemonTypeMod.THEMED_AREAS,
                true,
                true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        catchEmAllCheck(allPokes);
        randomTypeThemesAreasCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersCatchEmAllANDKeepTypeThemesWorks(String romName) {
        loadROM(romName);
        // for some reason fails with the Gen 3 Hoenn games
        // there's no obvious bug-related reason so I'm guessing they just have too few encounters/areas
        if (romHandler instanceof Gen3RomHandler gen3RomHandler) {
            assumeTrue(gen3RomHandler.getRomEntry().getRomType() == Gen3Constants.RomType_FRLG);
        }

        List<List<String>> beforeAreaStrings = new ArrayList<>();
        Map<Integer, Type> typeThemedAreas = new HashMap<>();
        recordTypeThemeBefore(beforeAreaStrings, typeThemedAreas);

        PokemonSet allPokes = romHandler.getPokemonSet();
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING,
                true,
                false, true, false,
                true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        catchEmAllCheck(allPokes);

        keepTypeThemedAreasCheck(beforeAreaStrings, typeThemedAreas);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void area1to1EncountersCatchEmAllANDRandomTypeThemesANDKeepTypeThemesWorks(String romName) {
        loadROM(romName);
        // for some reason fails with the Gen 3 Hoenn games
        // there's no obvious bug-related reason so I'm guessing they just have too few encounters/areas
        if (romHandler instanceof Gen3RomHandler gen3RomHandler) {
            assumeTrue(gen3RomHandler.getRomEntry().getRomType() == Gen3Constants.RomType_FRLG);
        }

        List<List<String>> beforeAreaStrings = new ArrayList<>();
        Map<Integer, Type> typeThemedAreas = new HashMap<>();
        recordTypeThemeBefore(beforeAreaStrings, typeThemedAreas);

        PokemonSet allPokes = romHandler.getPokemonSet();
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.AREA_MAPPING,
                true,
                true, true, false,
                true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        catchEmAllCheck(allPokes);

        randomTypeThemesAreasCheck();
        keepTypeThemedAreasCheck(beforeAreaStrings, typeThemedAreas);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void locations1to1EncountersGivesConsequentReplacementsForEachMon(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        locations1to1ConsequentReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void locations1to1EncountersGivesConsequentReplacementsForEachMonWithCatchEmAll(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        locations1to1ConsequentReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void locations1to1EncountersGivesConsequentReplacementsForEachMonWithRandomTypeThemes(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.THEMED_AREAS,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        locations1to1ConsequentReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void locations1to1EncountersGivesConsequentReplacementsForEachMonWithKeepPrimaryType(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.KEEP_PRIMARY,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        locations1to1ConsequentReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void locations1to1EncountersGivesConsequentReplacementsForEachMonWithUsePowerLevels(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, true, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        locations1to1ConsequentReplacementCheck(before, after);
    }

    private void locations1to1ConsequentReplacementCheck(List<EncounterArea> before, List<EncounterArea> after) {

        Map<String, List<EncounterArea>> groupedBefore = groupEncountersByLocation(before);
        Map<String, List<EncounterArea>> groupedAfter = groupEncountersByLocation(after);

        for (String tag : groupedBefore.keySet()) {
            Map<Pokemon, Pokemon> map = new HashMap<>();

            System.out.println("\nLocation: " + tag);
            Iterator<EncounterArea> beforeIterator = groupedBefore.get(tag).iterator();
            Iterator<EncounterArea> afterIterator = groupedAfter.get(tag).iterator();

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
        }
    }

    private Map<String, List<EncounterArea>> groupEncountersByLocation(List<EncounterArea> ungrouped) {
        Map<String, List<EncounterArea>> grouped = new HashMap<>();
        int untagged = 0;
        for (EncounterArea area : ungrouped) {
            String tag = area.getLocationTag();
            if (tag == null) {
                tag = "UNTAGGED-" + untagged;
                untagged++;
            }
            if (!grouped.containsKey(tag)) {
                grouped.put(tag, new ArrayList<>());
            }
            grouped.get(tag).add(area);
        }
        return grouped;
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void location1to1EncountersGivesUniqueReplacementsForEachMon(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        location1to1UniqueReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void location1to1EncountersGivesUniqueReplacementsForEachMonWithCatchEmAll(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        location1to1UniqueReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void location1to1EncountersGivesUniqueReplacementsForEachMonWithRandomTypeThemes(String romName) {
        assumeTrue(getGenerationNumberOf(romName) > 4); // Too few mons of some types vs the size of the locations, so it always fails
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.THEMED_AREAS,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        location1to1UniqueReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void location1to1EncountersGivesUniqueReplacementsForEachMonWithKeepPrimaryType(String romName) {
        assumeTrue(getGenerationNumberOf(romName) > 2); // Too few mons of some types, so it always fails
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.KEEP_PRIMARY,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        location1to1UniqueReplacementCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void location1to1EncountersGivesUniqueReplacementsForEachMonWithUsePowerLevels(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, true, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        location1to1UniqueReplacementCheck(before, after);
    }

    private void location1to1UniqueReplacementCheck(List<EncounterArea> before, List<EncounterArea> after) {

        Map<String, List<EncounterArea>> groupedBefore = groupEncountersByLocation(before);
        Map<String, List<EncounterArea>> groupedAfter = groupEncountersByLocation(after);

        for (String tag : groupedBefore.keySet()) {
            Map<Pokemon, Pokemon> map = new HashMap<>();

            System.out.println("\nLocation: " + tag);
            Iterator<EncounterArea> beforeIterator = groupedBefore.get(tag).iterator();
            Iterator<EncounterArea> afterIterator = groupedAfter.get(tag).iterator();

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
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void location1to1EncountersCanBanLegendaries(String romName) {
        loadROM(romName);

        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, true, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        checkForNoLegendaries();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void location1to1EncountersCanBanAltFormes(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 5);
        loadROM(romName);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, false, false,
                0, false, true, false);
        checkForNoAlternateFormes();
    }

    // since alt formes are not guaranteed, this test can be considered "reverse";
    // any success is a success for the test as a whole
    @ParameterizedTest
    @MethodSource("getRomNames")
    public void location1to1EncountersCanHaveAltFormesIfNotBanned(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 5);
        loadROM(romName);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        checkForAlternateFormes();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void location1to1EncountersCatchEmAllWorks(String romName) {
        loadROM(romName);
        // does not hold in BW1, presumably too few wild Pokémon species and too many in the national dex
        if (romHandler instanceof Gen5RomHandler gen5RomHandler) {
            assumeFalse(gen5RomHandler.getRomEntry().getRomType() == Gen5Constants.Type_BW);
        }
        PokemonSet allPokes = romHandler.getPokemonSet();
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        catchEmAllCheck(allPokes);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void location1to1EncountersRandomTypeThemesWorks(String romName) {
        loadROM(romName);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.THEMED_AREAS,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        randomTypeThemesLocationsCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void location1to1EncountersKeepPrimaryTypeWorks(String romName) {
        loadROM(romName);

        List<List<String>> beforeAreasStrings = new ArrayList<>();
        List<List<Type>> beforePrimaryTypes = new ArrayList<>();
        recordPrimaryTypesBefore(beforeAreasStrings, beforePrimaryTypes);

        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.KEEP_PRIMARY,
                true,
                false, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);

        keepPrimaryTypeCheck(beforeAreasStrings, beforePrimaryTypes);
    }

    private void randomTypeThemesLocationsCheck() {

        Map<String, List<EncounterArea>> grouped = groupEncountersByLocation(romHandler.getEncounters(true));

        for (Map.Entry<String, List<EncounterArea>> loc : grouped.entrySet()) {
            // lazy solution mashing together all of a location's associated EncounterAreas into a single one,
            // so old code can be reused to a greater extent
            EncounterArea area = new EncounterArea();
            area.setDisplayName("All of location " + loc.getKey());
            loc.getValue().forEach(area::addAll);

            System.out.println("\n" + area.getDisplayName() + ":\n" + area);
            Pokemon firstPk = area.get(0).getPokemon();
            PokemonSet allInArea = PokemonSet.inArea(area);

            Type primaryType = firstPk.getPrimaryType();
            PokemonSet ofFirstType = PokemonSet.inArea(area).filterByType(primaryType);
            PokemonSet notOfFirstType = new PokemonSet(allInArea).filter(pk -> !ofFirstType.contains(pk));
            System.out.println(notOfFirstType);

            if (!notOfFirstType.isEmpty()) {
                System.out.println("Not " + primaryType);
                Type secondaryType = firstPk.getSecondaryType();
                if (secondaryType == null) {
                    fail();
                }
                PokemonSet ofSecondaryType = PokemonSet.inArea(area).filterByType(secondaryType);
                PokemonSet notOfSecondaryType = new PokemonSet(allInArea)
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
    public void location1to1EncountersUsePowerLevelsWorks(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, true, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        powerLevelsCheck(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void location1to1EncountersCatchEmAllANDRandomTypeThemesWorks(String romName) {
        loadROM(romName);
        // does not hold in RSE/BW1/BW2, presumably too few wild Pokémon species and too many in the national dex
        if (romHandler instanceof Gen3RomHandler gen3RomHandler) {
            assumeTrue(gen3RomHandler.getRomEntry().getRomType() == Gen3Constants.RomType_FRLG);
        }
        assumeFalse(romHandler instanceof Gen5RomHandler);

        PokemonSet allPokes = romHandler.getPokemonSet();
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.LOCATION_MAPPING, Settings.WildPokemonTypeMod.THEMED_AREAS,
                true,
                true, false, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        catchEmAllCheck(allPokes);
        randomTypeThemesLocationsCheck();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void game1to1EncountersGivesConsequentReplacementsForEachMon(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true); // TODO: deep copy just in case
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.GLOBAL_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, false, false,
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
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.GLOBAL_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, false, false,
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
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.GLOBAL_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, true, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        checkForNoLegendaries();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void game1to1EncountersCanBanAltFormes(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 5);
        loadROM(romName);
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.GLOBAL_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, false, false,
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
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.GLOBAL_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, false, false, false,
                0, true, true, false);
        checkForAlternateFormes();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void game1to1EncountersUsePowerLevelsWorks(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true); // TODO: deep copy just in case
        new EncounterRandomizer(romHandler, new Settings(), RND).randomizeEncounters(
                Settings.WildPokemonMod.GLOBAL_MAPPING, Settings.WildPokemonTypeMod.NONE,
                true,
                false, true, false, false,
                0, getGenerationNumberOf(romName) >= 5, true, false);
        List<EncounterArea> after = romHandler.getEncounters(true);

        powerLevelsCheck(before, after);
    }

}
