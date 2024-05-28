package test.updaters;

import com.dabomstew.pkrandom.pokemon.Move;
import com.dabomstew.pkrandom.pokemon.MoveCategory;
import com.dabomstew.pkrandom.pokemon.Type;
import com.dabomstew.pkrandom.updaters.MoveUpdater;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class MoveUpdaterTest extends UpdaterTest {

    // all the attributes that may be changed by MoveUpdater.
    // this must of course be updated if it gets the ability to change more attributes
    private record MoveRecord(String name, int power, int pp, double hitratio, Type type, MoveCategory category) {
    }

    @ParameterizedTest
    @MethodSource("getRomNamesAndUpdateToGens")
    public void updatingDoesNotThrowException(String romNameAndUpdateToGen) {
        String[] split = romNameAndUpdateToGen.split(UPDATE_SEPARATOR);
        String romName = split[0];
        int updateToGen = Integer.parseInt(split[1]);

        loadROM(romName);
        new MoveUpdater(romHandler).updateMoves(updateToGen);
    }

    @ParameterizedTest
    @MethodSource("getRomNamesAndUpdateToGens")
    public void updatingChangesSomeMoves(String romNameAndUpdateToGen) {
        String[] split = romNameAndUpdateToGen.split(UPDATE_SEPARATOR);
        String romName = split[0];
        int updateToGen = Integer.parseInt(split[1]);

        loadROM(romName);
        List<MoveRecord> before = toRecords(romHandler.getMoves());
        new MoveUpdater(romHandler).updateMoves(updateToGen);
        List<MoveRecord> after = toRecords(romHandler.getMoves());
        printDiff(before, after);
        assertNotEquals(before, after);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void updatingToAllGensInOrderIsTheSameAsJustUpdatingToLast(String romName) {
        loadROM(romName);

        MoveUpdater mu = new MoveUpdater(romHandler);
        for (int gen = getGenerationNumberOf(romName) + 1; gen <= MAX_UPDATE_GEN; gen++) {
            mu.updateMoves(gen);
        }
        List<MoveRecord> afterAllInOrder = toRecords(romHandler.getMoves());
        mu.updateMoves(MAX_UPDATE_GEN);
        assertEquals(afterAllInOrder, toRecords(romHandler.getMoves()));
    }

    private List<MoveRecord> toRecords(List<Move> moves) {
        List<MoveRecord> records = new ArrayList<>(moves.size());
        for (Move m : moves) {
            if (m != null) {
                records.add(new MoveRecord(m.name, m.power, m.pp, m.hitratio, m.type, m.category));
            }
        }
        return records;
    }

}
