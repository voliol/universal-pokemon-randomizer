package com.dabomstew.pkrandom;

/*----------------------------------------------------------------------------*/
/*--  RandomSource.java - functions as a centralized source of randomness   --*/
/*--                      to allow the same seed to produce the same random --*/
/*--                      ROM consistently.                                 --*/
/*--                                                                        --*/
/*--  Part of "Universal Pokemon Randomizer ZX" by the UPR-ZX team          --*/
/*--  Originally part of "Universal Pokemon Randomizer" by Dabomstew        --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2020.                                 --*/
/*--                                                                        --*/
/*--  The custom code written here is licensed under the terms of the GPL:  --*/
/*--                                                                        --*/
/*--  This program is free software: you can redistribute it and/or modify  --*/
/*--  it under the terms of the GNU General Public License as published by  --*/
/*--  the Free Software Foundation, either version 3 of the License, or     --*/
/*--  (at your option) any later version.                                   --*/
/*--                                                                        --*/
/*--  This program is distributed in the hope that it will be useful,       --*/
/*--  but WITHOUT ANY WARRANTY; without even the implied warranty of        --*/
/*--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          --*/
/*--  GNU General Public License for more details.                          --*/
/*--                                                                        --*/
/*--  You should have received a copy of the GNU General Public License     --*/
/*--  along with this program. If not, see <http://www.gnu.org/licenses/>.  --*/
/*----------------------------------------------------------------------------*/

import java.io.Serial;
import java.security.SecureRandom;
import java.util.Random;

public class RandomSource {

    private static RandomWithCounter instance = new RandomWithCounter();
    private static RandomWithCounter cosmeticInstance = new RandomWithCounter();

    public static void reset() {
        instance = new RandomWithCounter();
        cosmeticInstance = new RandomWithCounter();
    }

    public static void seed(long seed) {
        instance.setSeed(seed);
        cosmeticInstance.setSeed(seed);
    }

    public static long pickSeed() {
        long value = 0;
        byte[] by = SecureRandom.getSeed(6);
        for (int i = 0; i < by.length; i++) {
            value |= ((long) by[i] & 0xffL) << (8 * i);
        }
        return value;
    }

    public static Random instance() {
        return instance;
    }

    public static Random cosmeticInstance() {
        return cosmeticInstance;
    }

    public static int callsSinceSeed() {
        return instance.calls + cosmeticInstance.calls;
    }

    private static class RandomWithCounter extends Random {

        private int calls = 0;

        @Serial
        private static final long serialVersionUID = -4876737183441746322L;

        @Override
        public synchronized void setSeed(long seed) {
            super.setSeed(seed);
            calls = 0;
        }

        @Override
        public void nextBytes(byte[] bytes) {
            calls++;
            super.nextBytes(bytes);
        }

        @Override
        public int nextInt() {
            calls++;
            return super.nextInt();
        }

        @Override
        public int nextInt(int bound) {
            calls++;
            return super.nextInt(bound);
        }

        @Override
        public long nextLong() {
            calls++;
            return super.nextLong();
        }

        @Override
        public boolean nextBoolean() {
            calls++;
            return super.nextBoolean();
        }

        @Override
        public float nextFloat() {
            calls++;
            return super.nextFloat();
        }

        @Override
        public double nextDouble() {
            calls++;
            return super.nextDouble();
        }

        @Override
        public synchronized double nextGaussian() {
            calls++;
            return super.nextGaussian();
        }

    }

}
