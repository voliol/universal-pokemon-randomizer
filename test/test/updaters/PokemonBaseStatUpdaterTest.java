package test.updaters;

import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.updaters.PokemonBaseStatUpdater;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class PokemonBaseStatUpdaterTest extends UpdaterTest {

    private record BaseStatRecord(String name, int hp, int atk, int def, int spAtk, int spDef, int speed, int special) {
    }

    @ParameterizedTest
    @MethodSource("getRomNamesAndUpdateToGens")
    public void updatingDoesNotThrowException(String romNameAndUpdateToGen) {
        String[] split = romNameAndUpdateToGen.split(UPDATE_SEPARATOR);
        String romName = split[0];
        int updateToGen = Integer.parseInt(split[1]);

        loadROM(romName);
        new PokemonBaseStatUpdater(romHandler).updatePokemonStats(updateToGen);
    }

    @ParameterizedTest
    @MethodSource("getRomNamesAndUpdateToGens")
    public void updatingChangesSomeBaseStats(String romNameAndUpdateToGen) {
        String[] split = romNameAndUpdateToGen.split(UPDATE_SEPARATOR);
        String romName = split[0];
        int updateToGen = Integer.parseInt(split[1]);

        assumeTrue(updateToGen >= 6);

        loadROM(romName);
        List<BaseStatRecord> before = toRecords(romHandler.getPokemonInclFormes());
        new PokemonBaseStatUpdater(romHandler).updatePokemonStats(updateToGen);
        List<BaseStatRecord> after = toRecords(romHandler.getPokemonInclFormes());
        printDiff(before, after);
        assertNotEquals(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void updatingToAllGensInOrderIsTheSameAsJustUpdatingToLast(String romName) {
        loadROM(romName);

        PokemonBaseStatUpdater bsu = new PokemonBaseStatUpdater(romHandler);
        for (int gen = getGenerationNumberOf(romName) + 1; gen <= MAX_UPDATE_GEN; gen++) {
            bsu.updatePokemonStats(gen);
        }
        List<BaseStatRecord> afterAllInOrder = toRecords(romHandler.getPokemonInclFormes());
        new PokemonBaseStatUpdater(romHandler).updatePokemonStats(MAX_UPDATE_GEN);
        assertEquals(afterAllInOrder, toRecords(romHandler.getPokemonInclFormes()));
    }

    private List<BaseStatRecord> toRecords(List<Pokemon> pokes) {
        List<BaseStatRecord> records = new ArrayList<>(pokes.size());
        for (Pokemon pk : pokes) {
            if (pk != null) {
                records.add(new BaseStatRecord(pk.fullName(), pk.getHp(), pk.getAttack(), pk.getDefense(),
                        pk.getSpatk(), pk.getSpdef(), pk.getSpeed(), pk.getSpecial()));
            }
        }
        return records;
    }


}
