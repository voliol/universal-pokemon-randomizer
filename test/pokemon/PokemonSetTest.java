package pokemon;

import com.dabomstew.pkrandom.pokemon.Pokemon;

import java.util.Arrays;
import java.util.Random;

public class PokemonSetTest {

    // Tests the "NewPokemonSet" class to see what is an appropriate load factor, how often to make a new ArrayList
    // representation of the Set to pick randomly from, depending on how many elements (proportionally) have been
    // removed since last time.

    // A load factor of >=1 (i.e. 2) means it should make a new ArrayList every time getRandom() is called,
    // and e.g. a load factor of 0.75 means it should make a new ArrayList when only 75% of the elements remain.
    // The currently used PokemonSet implementation essentially works like the former.

    private static final int[] AMOUNTS = new int[] {100, 200, 1000, 2000, 4000};
    private static final double[] LOAD_FACTORS = new double[] {2, 0.0, 0.25, 0.5, 0.75};
    private static final int REPS = 1000;

    public static void main(String[] args) {

        for (int amount : AMOUNTS) {
            for (double loadFactor : LOAD_FACTORS) {

                long[] times = new long[REPS];
                long maxTime = 0;
                long minTime = Long.MAX_VALUE;

                for (int rep = 0; rep < REPS; rep++) {

                    NewPokemonSet<Pokemon> pokes = new NewPokemonSet<>(loadFactor);
                    for (int pokeID = 0; pokeID < amount; pokeID++) {
                        Pokemon pk = new Pokemon(pokeID);
                        pk.setName("Poke-" + pokeID);
                        pokes.add(pk);
                    }

                    Random random = new Random(rep);
                    long before = System.currentTimeMillis();

                    while(pokes.size() > 0) {
                        Pokemon picked = pokes.getRandom(random);
                        pokes.remove(picked);
                    }

                    times[rep] = System.currentTimeMillis() - before;
                    if (times[rep] > maxTime) maxTime = times[rep];
                    if (times[rep] < minTime) minTime = times[rep];
                }

                System.out.println("Amount: " + amount);
                System.out.println("LoadFactor: " + loadFactor);
                double average = Arrays.stream(times).average().getAsDouble();
                long sum = Arrays.stream(times).sum();
                System.out.println("Average: " + average + "\tMax: " + maxTime + "\tMin: " + minTime + "\tSum: " + sum);
                System.out.println("\n");
            }

        }

        System.out.println("done");
    }


}
