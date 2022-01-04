package com.dabomstew.pkrandom;

import java.util.HashMap;
import java.util.Objects;

public class RomOptions {
    // Global Options
    private boolean randomizeSubset;
    private int minimumRandomizablePokemonNumber;
    private int maximumRandomizablePokemonNumber;

    public RomOptions() {}

    public RomOptions(HashMap<String, HashMap<String, Object>> romOptions) {
        this.randomizeSubset =
                Boolean.parseBoolean(romOptions.get("Global").get("RandomizeSubset").toString());
        this.minimumRandomizablePokemonNumber =
                Integer.parseInt(romOptions.get("Global").get("MinPokemonNumber").toString());
        this.maximumRandomizablePokemonNumber =
                Integer.parseInt(romOptions.get("Global").get("MaxPokemonNumber").toString());
    }

    public boolean isRandomizeSubset() {
        return this.randomizeSubset;
    }

    public void setRandomizeSubset(boolean randomizeSubset) {
        this.randomizeSubset = randomizeSubset;
    }

    public int getMinimumRandomizablePokemonNumber() {
        return this.minimumRandomizablePokemonNumber;
    }

    public void setMinimumRandomizablePokemonNumber(int minimumRandomizablePokemonNumber) {
        this.minimumRandomizablePokemonNumber = minimumRandomizablePokemonNumber;
    }

    public int getMaximumRandomizablePokemonNumber() {
        return this.maximumRandomizablePokemonNumber;
    }

    public void setMaximumRandomizablePokemonNumber(int maximumRandomizablePokemonNumber) {
        this.maximumRandomizablePokemonNumber = maximumRandomizablePokemonNumber;
    }

    public RomOptions randomizeSubset(boolean randomizeSubset) {
        setRandomizeSubset(randomizeSubset);
        return this;
    }

    public RomOptions minimumRandomizablePokemonNumber(int minimumRandomizablePokemonNumber) {
        setMinimumRandomizablePokemonNumber(minimumRandomizablePokemonNumber);
        return this;
    }

    public RomOptions maximumRandomizablePokemonNumber(int maximumRandomizablePokemonNumber) {
        setMaximumRandomizablePokemonNumber(maximumRandomizablePokemonNumber);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RomOptions)) {
            return false;
        }
        RomOptions romOptions = (RomOptions) o;
        return randomizeSubset == romOptions.randomizeSubset
                && minimumRandomizablePokemonNumber == romOptions.minimumRandomizablePokemonNumber
                && maximumRandomizablePokemonNumber == romOptions.maximumRandomizablePokemonNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(randomizeSubset, minimumRandomizablePokemonNumber,
                maximumRandomizablePokemonNumber);
    }

    @Override
    public String toString() {
        return "{" + " randomizeSubset='" + isRandomizeSubset() + "'"
                + ", minimumRandomizablePokemonNumber='" + getMinimumRandomizablePokemonNumber()
                + "'" + ", maximumRandomizablePokemonNumber='"
                + getMaximumRandomizablePokemonNumber() + "'" + "}";
    }


}
