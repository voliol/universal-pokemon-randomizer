package test.pokemon;

import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.PokemonSet;
import com.dabomstew.pkrandom.pokemon.Type;
import com.dabomstew.pkrandom.services.TypeService;
import org.junit.jupiter.api.Test;

import javax.print.attribute.UnmodifiableSetException;
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
        assertThrows(UnmodifiableSetException.class, () -> {pokes.add(b);});
    }

    @Test
    public void unmodifiableSetThrowsWhenRemoving() {
        Pokemon a = new Pokemon(0);
        a.setName("A");
        PokemonSet pokes = PokemonSet.unmodifiable(Collections.singleton(a));
        assertThrows(UnmodifiableSetException.class, () -> {pokes.remove(a);});
    }

    @Test
    public void unmodifiableSetThrowsWhenRemovingThroughIterator() {
        Pokemon a = new Pokemon(0);
        a.setName("A");
        PokemonSet pokes = PokemonSet.unmodifiable(Collections.singleton(a));
        assertThrows(UnmodifiableSetException.class, () -> {
            Iterator<Pokemon> it = pokes.iterator();
            it.next();
            it.remove();
            System.out.println(pokes); // in case nothing is thrown, shows whether the element was removed or not
        });
    }

    @Test
    public void unmodifiableSetThrowsWhenClearing() {
        Pokemon a = new Pokemon(0);
        a.setName("A");
        PokemonSet pokes = PokemonSet.unmodifiable(Collections.singleton(a));
        assertThrows(UnmodifiableSetException.class, pokes::clear);
    }

    @Test
    public void sortByTypesWorks() {
        PokemonSet pokes = new PokemonSet();
        Random random = new Random();
        List<Type> types = Type.getAllTypes(7);
        for(int i = 0; i < 1000; i++){
            Pokemon pokemon = new Pokemon(i);
            pokemon.setName("Random" + i);
            pokemon.setPrimaryType(types.get(random.nextInt(types.size())));
            if(random.nextBoolean()) {
                pokemon.setSecondaryType(types.get(random.nextInt(types.size())));
            }
            pokes.add(pokemon);
        }

        Map<Type, PokemonSet> pokesByTypes = pokes.sortByType(false);
        for(Type type : types) {
            PokemonSet pokemonOfType = pokesByTypes.get(type);
            if(pokemonOfType != null) {
                for (Pokemon pokemon : pokemonOfType) {
                    assertTrue(pokemon.hasType(type, false));
                }
            }
        }
    }

    @Test
    public void sortByTypesWorksWithChangedTypes() {
        PokemonSet pokes = new PokemonSet();
        Random random = new Random();
        List<Type> types = Type.getAllTypes(7);
        for(int i = 0; i < 1000; i++){
            Pokemon pokemon = new Pokemon(i);
            pokemon.setName("Random" + i);
            pokemon.setPrimaryType(types.get(random.nextInt(types.size())));
            if(random.nextBoolean()) {
                pokemon.setSecondaryType(types.get(random.nextInt(types.size())));
            } else {
                pokemon.setSecondaryType(null);
            }

            pokemon.setPrimaryType(types.get(random.nextInt(types.size())));
            if(random.nextBoolean()) {
                pokemon.setSecondaryType(types.get(random.nextInt(types.size())));
            } else {
                pokemon.setSecondaryType(null);
            }
            pokes.add(pokemon);
        }

        Map<Type, PokemonSet> pokesByTypes = pokes.sortByType(false);
        for(Type type : types) {
            PokemonSet pokemonOfType = pokesByTypes.get(type);
            for (Pokemon pokemon : pokemonOfType) {
                assertTrue(pokemon.hasType(type, false));
            }
        }

        pokesByTypes = pokes.sortByType(true);
        for(Type type : types) {
            PokemonSet pokemonOfType = pokesByTypes.get(type);
            for (Pokemon pokemon : pokemonOfType) {
                assertTrue(pokemon.hasType(type, true));
            }
        }
    }
}
