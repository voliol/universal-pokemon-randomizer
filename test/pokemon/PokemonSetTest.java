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

    private static final int[] AMOUNTS = new int[] {100, 200, 1000, 2000, 4000, 10000};
    private static final double[] LOAD_FACTORS = new double[] {0.25, 1.0/Math.PI, 1.0/Math.E, 0.5};
    private static final int REPS = 1000;

    public static void main(String[] args) {

        for (int amount : AMOUNTS) {

            long[][] times = new long[LOAD_FACTORS.length][REPS];
            long[] maxTimes = new long[LOAD_FACTORS.length];
            long[] minTimes = new long[LOAD_FACTORS.length];

            System.out.println("Amount: " + amount);

            for (int rep = 0; rep < REPS; rep++) {

                for (int lfi = 0; lfi < LOAD_FACTORS.length; lfi++) {
                    double loadFactor = LOAD_FACTORS[lfi];

                    maxTimes[lfi] = 0;
                    minTimes[lfi] = Long.MAX_VALUE;

                    NewPokemonSet<Pokemon> pokes = new NewPokemonSet<>(loadFactor);
                    for (int pokeID = 0; pokeID < amount; pokeID++) {
                        Pokemon pk = new Pokemon(pokeID);
                        pk.setName("Poke-" + pokeID);
                        pokes.add(pk);
                    }

                    Random random = new Random(rep);
                    long before = System.currentTimeMillis();

                    while (pokes.size() > 0) {
                        Pokemon picked = pokes.getRandom(random);
                        pokes.remove(picked);
                    }

                    times[lfi][rep] = System.currentTimeMillis() - before;
                    if (times[lfi][rep] > maxTimes[lfi]) maxTimes[lfi] = times[lfi][rep];
                    if (times[lfi][rep] < minTimes[lfi]) minTimes[lfi] = times[lfi][rep];

                }
            }

            for (int lfi = 0; lfi < LOAD_FACTORS.length; lfi++) {
                System.out.println("LoadFactor: " + LOAD_FACTORS[lfi]);
                double average = Arrays.stream(times[lfi]).average().getAsDouble();
                long sum = Arrays.stream(times[lfi]).sum();
                System.out.println("Average: " + average + "\tMax: " + maxTimes[lfi] + "\tMin: " + minTimes[lfi] + "\tSum: " + sum);
            }
            System.out.println("\n");
        }

        System.out.println("done");
    }



}
