package test.romhandlers;

import com.dabomstew.pkrandom.pokemon.Encounter;
import com.dabomstew.pkrandom.pokemon.EncounterArea;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.romhandlers.AbstractRomHandler;
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
    public void game1to1EncountersGivesConsequentReplacementsForEachMon(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true); // TODO: deep copy just in case
        ((AbstractRomHandler) romHandler).game1to1Encounters(true, false,
                false, 0, true, true,
                false);
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
        ((AbstractRomHandler) romHandler).game1to1Encounters(true, false,
                false, 0, true, true,
                false);
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
        ((AbstractRomHandler) romHandler).game1to1Encounters(true, false,
                true, 0, true, true,
                false);
        for (EncounterArea area : romHandler.getEncounters(true)) {
            System.out.println(area.getDisplayName() + ":");
            System.out.println(area);
            for (Encounter enc : area) {
                assertFalse(enc.getPokemon().isLegendary());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void game1to1EncountersCanBanAltFormes(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 4);
        loadROM(romName);
        ((AbstractRomHandler) romHandler).game1to1Encounters(true, false,
                false, 0, false, true,
                false);
        for (EncounterArea area : romHandler.getEncounters(true)) {
            System.out.println(area.getDisplayName() + ":");
            System.out.println(area);
            for (Encounter enc : area) {
                assertNull(enc.getPokemon().getBaseForme());
            }
        }
    }

    // since alt formes are not guaranteed, this test can be considered "reverse";
    // any success is a success for the test as a whole
    @ParameterizedTest
    @MethodSource("getRomNames")
    public void game1to1EncountersCanHaveAltFormesIfNotBanned(String romName) {
        assumeTrue(getGenerationNumberOf(romName) >= 4);
        loadROM(romName);
        ((AbstractRomHandler) romHandler).game1to1Encounters(true, false,
                false, 0, true, true,
                false);
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
    public void game1to1EncountersCanGiveSimilarPowerLevelReplacements(String romName) {
        loadROM(romName);
        List<EncounterArea> before = romHandler.getEncounters(true); // TODO: deep copy just in case
        ((AbstractRomHandler) romHandler).game1to1Encounters(true, true,
                false, 0, true, true,
                false);
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

            Iterator<Encounter> beforeEncIterator = beforeArea.iterator();
            Iterator<Encounter> afterEncIterator = afterArea.iterator();
            while (beforeEncIterator.hasNext()) {
                Pokemon beforePk = beforeEncIterator.next().getPokemon();
                Pokemon afterPk = afterEncIterator.next().getPokemon();

                if (!map.containsKey(beforePk)) {
                    map.put(beforePk, afterPk);
                }
            }
        }

        List<Double> diffs = new ArrayList<>();
        for (Map.Entry<Pokemon, Pokemon> entry : map.entrySet()) {
            diffs.add(Math.abs((double) entry.getKey().bstForPowerLevels() /
                    (double) entry.getValue().bstForPowerLevels() - 1));
        }
        double averageDiff = diffs.stream().mapToDouble(d -> d).average().getAsDouble();
        System.out.println(diffs);
        System.out.println(averageDiff);
        assertTrue(averageDiff < 0.06);
    }

}
