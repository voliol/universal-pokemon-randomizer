package test.pokemon;

import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.PokemonSet;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Executable;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PokemonSetTest {

    private static final double MAX_DEVIATION = 0.1;

    private final Random random = new Random();

    @Test
    public void addingElementMultipleTimesDoesNotAffectGetRandom() {
        Pokemon a = new Pokemon(0);
        a.setName("A");
        Pokemon b = new Pokemon(1);
        b.setName("B");
        PokemonSet pokes = new PokemonSet();
        pokes.add(a);
        for (int i = 0; i < 1000; i++) {
            pokes.add(b);
        }

        int[] count = new int[2];
        for (int i = 0; i < 5000; i++) {
            Pokemon pick = pokes.getRandomPokemon(random);
            count[pick.getNumber()]++;
        }

        System.out.println(Arrays.toString(count));
        System.out.println(Math.abs(1 - ((double) count[0] / (double) count[1])));
        assertTrue(Math.abs(1 - ((double) count[0] / (double) count[1])) < MAX_DEVIATION);
    }

    @Test
    public void removingAnElementPreventsItFromGettingChosenByGetRandom() {
        Pokemon a = new Pokemon(0);
        a.setName("A");
        Pokemon b = new Pokemon(1);
        b.setName("B");
        Pokemon c = new Pokemon(2);
        c.setName("C");
        PokemonSet pokes = new PokemonSet();
        pokes.add(a);
        pokes.add(b);
        pokes.add(c);
        pokes.remove(b);

        int[] count = new int[3];
        for (int i = 0; i < 5000; i++) {
            Pokemon pick = pokes.getRandomPokemon(random);
            count[pick.getNumber()]++;
        }

        System.out.println(Arrays.toString(count));
        assertEquals(0, count[b.getNumber()]);
    }

    @Test
    public void readdingARemovedElementMakesItChoosableByGetRandom() {
        Pokemon a = new Pokemon(0);
        a.setName("A");
        Pokemon b = new Pokemon(1);
        b.setName("B");
        Pokemon c = new Pokemon(2);
        c.setName("C");
        PokemonSet pokes = new PokemonSet();
        pokes.add(a);
        pokes.add(b);
        pokes.add(c);
        pokes.remove(b);
        pokes.add(b);

        int[] count = new int[3];
        for (int i = 0; i < 5000; i++) {
            Pokemon pick = pokes.getRandomPokemon(random);
            count[pick.getNumber()]++;
        }

        System.out.println(Arrays.toString(count));
        assertTrue(count[b.getNumber()] > 0);
    }

    @Test
    public void unmodifiableSetCopiesElementsWhenInitiated() {
        Pokemon a = new Pokemon(0);
        a.setName("A");
        Pokemon b = new Pokemon(1);
        b.setName("B");
        PokemonSet pokes = PokemonSet.unmodifiable(new HashSet<>(Arrays.asList(a, b)));
        System.out.println(pokes);
        assertEquals(pokes, new HashSet<>(Arrays.asList(a, b)));
    }

    @Test
    public void unmodifiableSetThrowsWhenAdding() {
        Pokemon a = new Pokemon(0);
        a.setName("A");
        Pokemon b = new Pokemon(1);
        b.setName("B");
        PokemonSet pokes = PokemonSet.unmodifiable(Collections.singleton(a));
        assertThrows(UnsupportedOperationException.class, () -> {pokes.add(b);});
    }

    @Test
    public void unmodifiableSetThrowsWhenRemoving() {
        Pokemon a = new Pokemon(0);
        a.setName("A");
        PokemonSet pokes = PokemonSet.unmodifiable(Collections.singleton(a));
        assertThrows(UnsupportedOperationException.class, () -> {pokes.remove(a);});
    }

    @Test
    public void unmodifiableSetThrowsWhenRemovingThroughIterator() {
        Pokemon a = new Pokemon(0);
        a.setName("A");
        PokemonSet pokes = PokemonSet.unmodifiable(Collections.singleton(a));
        assertThrows(UnsupportedOperationException.class, () -> {
            Iterator<Pokemon> it = pokes.iterator();
            it.next();
            it.remove();
        });
    }
}
