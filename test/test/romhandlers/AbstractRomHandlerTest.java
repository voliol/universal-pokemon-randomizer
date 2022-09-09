package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.constants.*;
import com.dabomstew.pkrandom.pokemon.*;
import org.junit.*;
import static org.junit.Assert.*;

import java.io.PrintStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AbstractRomHandlerTest {

    // update if the amount of supported generation increases,
    // and expect some test cases to need updating too, though hopefully only in a minor way
    private static final int HIGHEST_GENERATION = 7;

    private static final Random RAND = new Random();

    private final TestRomHandler rh;
    private List<Pokemon> pokemon = new ArrayList<>();
    private List<EncounterSet> encounterSets = new ArrayList<>();

    public AbstractRomHandlerTest() {
        this.rh = new TestRomHandler(RAND, new PrintStream(System.out)) {
            @Override
            public List<Pokemon> getPokemon() {
                return pokemon;
            }

            @Override
            public List<Pokemon> getPokemonInclFormes() {
                return pokemon;
            }

            @Override
            public PokemonSet<Pokemon> getAltFormes() {
                return new PokemonSet<>();
            };

            @Override
            public PokemonSet<Pokemon> getIrregularFormes() {
                return new PokemonSet<>();
            };

            @Override
            public List<MegaEvolution> getMegaEvolutions() {
                return new ArrayList<>();
            };

            @Override
            public List<EncounterSet> getEncounters(boolean useTimeOfDay) {
                return encounterSets;
            }
        };
    }

    private void initPokemon(int generation, boolean isSM) {
        int maxGen7SpeciesID = isSM ? Species.marshadow : Species.zeraora;
        int number = switch (generation) {
            case 1 -> 151;
            case 2 -> Gen2Constants.pokemonCount;
            case 3 -> Gen3Constants.pokemonCount;
            case 4 -> Gen4Constants.pokemonCount;
            case 5 -> Gen5Constants.pokemonCount;
            case 6 -> Gen6Constants.pokemonCount;
            case 7 -> Gen7Constants.getPokemonCount(maxGen7SpeciesID);
            default -> throw new IllegalStateException("Unexpected value: " + generation);
        };

        pokemon.add(null);
        for (int i = 1; i <= number; i++) {
            Pokemon testPoke = new Pokemon(i);
            testPoke.setName("TestPoke #" + i);
            pokemon.add(testPoke);
        }

        pokemon.set(Species.oddish, PokeConstants.oddish);
        pokemon.set(Species.gloom, PokeConstants.gloom);
        pokemon.set(Species.vileplume, PokeConstants.vileplume);
        if (generation >= 2) {
            pokemon.set(Species.bellossom, PokeConstants.bellossom);
        }
    }

    @Test
    public void getPokemonSet() {
        initPokemon(5, false);

        Set<Pokemon> fauxSet = new HashSet<>(rh.getPokemon());
        PokemonSet<Pokemon> pokemonSet = rh.getPokemonSet();

        for (Pokemon pk : fauxSet) {
            if (pk != null && !pokemonSet.contains(pk)) {
                fail("getPokemonSet() does not contain all Pokemon it should; it is missing " + pk);
            }
        }

        for (Pokemon pk : pokemonSet) {
            if (!fauxSet.contains(pk)) {
                fail("getPokemonSet() contains more Pokemon than it should; it wrongly includes " + pk);
            }
        }
    }

    @Test
    public void restrictPokemonNoRestrictions() {
        for (int i = 1; i <= HIGHEST_GENERATION; i++) {
            if (i == 7) {
                initPokemon(i, false);
                rh.setPokemonPool(null);
                assertEquals(rh.getPokemonSet(), rh.getRestrictedPokemon());

                initPokemon(i, true);
                rh.setPokemonPool(null);
                assertEquals(rh.getPokemonSet(), rh.getRestrictedPokemon());
            } else {
                initPokemon(i, true);
                rh.setPokemonPool(null);
                assertEquals(rh.getPokemonSet(), rh.getRestrictedPokemon());
            }
        }
    }

    @Test
    public void restrictPokemonNoRelatives(){
        initPokemon(3, false);
        Settings settings = new Settings();
        settings.setLimitPokemon(true);
        settings.setCurrentRestrictions(fromBools(false, true, false, true));

        rh.setPokemonPool(settings);
        assertFalse("restrictedPokemon includes a Pokemon from a generation which should be turned off (Sunkern/gen II)",
                rh.getRestrictedPokemon().contains(PokeConstants.sunkern));
        assertTrue("restrictedPokemon includes a related Pokemon from a generation which should be turned off (Bellossom/gen II)",
                rh.getRestrictedPokemon().contains(PokeConstants.gloom) &&
                !rh.getRestrictedPokemon().contains(PokeConstants.bellossom));
    }

    @Test
    public void restrictPokemonEvolutionInOtherGen(){
        initPokemon(3, false);
        Settings settings = new Settings();
        settings.setLimitPokemon(true);
        settings.setCurrentRestrictions(fromBools(true, true, false, true));

        rh.setPokemonPool(settings);
        assertFalse("restrictedPokemon includes a Pokemon from a generation which should be turned off (Sunkern/gen II)",
                rh.getRestrictedPokemon().contains(PokeConstants.sunkern));
        assertTrue("restrictedPokemon does not include both the Pokemon from the allowed generation and its " +
                        "evolution from the turned off one (Gloom/gen I, Bellossom/gen II)",
                rh.getRestrictedPokemon().contains(PokeConstants.gloom) &&
                rh.getRestrictedPokemon().contains(PokeConstants.bellossom));
    }

    @Test
    public void restrictPokemonPrevolutionInOtherGen(){
        initPokemon(3, false);
        Settings settings = new Settings();
        settings.setLimitPokemon(true);
        settings.setCurrentRestrictions(fromBools(true, false, true, true));

        rh.setPokemonPool(settings);
        assertTrue("restrictedPokemon does not include both the Pokemon from the allowed generation and its " +
                "preevolution from the turned off one (Bellossom/gen II, Gloom/gen I)",
                rh.getRestrictedPokemon().contains(PokeConstants.gloom) &&
                rh.getRestrictedPokemon().contains(PokeConstants.bellossom));
    }

    @Test
    public void restrictPokemonOtherRelativeInOtherGen(){
        initPokemon(3, false);
        Settings settings = new Settings();
        settings.setLimitPokemon(true);
        settings.setCurrentRestrictions(fromBools(true, false, true, true));

        rh.setPokemonPool(settings);
        assertTrue("restrictedPokemon does not include both the Pokemon from the allowed generation and its " +
                        "relative from the turned off one (Bellossom/gen II, Vileplume/gen I)",
                rh.getRestrictedPokemon().contains(PokeConstants.gloom) &&
                        rh.getRestrictedPokemon().contains(PokeConstants.vileplume));
    }

    private GenRestrictions fromBools(boolean relativesAllowed, boolean... gensAllowed) {
        int state = 0;
        for (int i = 0; i < gensAllowed.length; i++) {
            state += gensAllowed[i] ? 1 << i : 0;
        }
        state += relativesAllowed ? 1 << HIGHEST_GENERATION : 0;
        return new GenRestrictions(state);
    }

    private static final SettingsToggle[] encountersGeneralToggles = new SettingsToggle[] {
            new SettingsToggle("setUseTimeBasedEncounters", Settings::setUseTimeBasedEncounters),
            new SettingsToggle("setBlockWildLegendaries", Settings::setBlockWildLegendaries),
            new SettingsToggle("setBalanceShakingGrass", Settings::setBalanceShakingGrass),
            new SettingsToggle("setAllowWildAltFormes", Settings::setAllowWildAltFormes),
            new SettingsToggle("setBanIrregularAltFormes", Settings::setBanIrregularAltFormes)
    };

    @Test
    public void game1to1Encounters() {
        initPokemon(1, false);

        repeatTestForAllSettingsToggles(settings -> {int seed = RAND.nextInt();
            // easier to generate two identical encounterSets by seed than figure out a clone function
            List<EncounterSet> encounterSetsBefore = randomEncounterSets(seed);
            this.encounterSets = randomEncounterSets(seed);

            rh.game1to1Encounters(new Settings());

            List<EncounterSet> encounterSetsAfter = rh.getEncounters(false);

            Map<Pokemon, Pokemon> replacements = new HashMap<>();

            for (int encSetIndex = 0; encSetIndex < encounterSetsAfter.size(); encSetIndex++) {
                EncounterSet beforeSet = encounterSetsBefore.get(encSetIndex);
                EncounterSet afterSet = encounterSetsAfter.get(encSetIndex);
                for (int encIndex = 0; encIndex < beforeSet.encounters.size(); encIndex++) {
                    Pokemon before = beforeSet.encounters.get(encIndex).pokemon;
                    Pokemon after = afterSet.encounters.get(encIndex).pokemon;
                    if (!replacements.containsKey(before)) {
                        replacements.put(before, after);
                    }
                    assertSame(before.getName() + " should be replaced by " + replacements.get(before) + ", " +
                                    "was replaced by " + after.getName() + ".",
                            replacements.get(before), after);
                }
            }}, new Settings(), encountersGeneralToggles);

    }

    @Test
    public void area1to1Encounters() {
        initPokemon(1, false);

        int seed = RAND.nextInt();

        repeatTestForAllSettingsToggles(settings -> {
            // easier to generate two identical encounterSets by seed than figure out a clone function
            List<EncounterSet> encounterSetsBefore = randomEncounterSets(seed);
            this.encounterSets = randomEncounterSets(seed);

            rh.game1to1Encounters(settings);

            List<EncounterSet> encounterSetsAfter = rh.getEncounters(settings.isUseTimeBasedEncounters());

            for (int encSetIndex = 0; encSetIndex < encounterSetsAfter.size(); encSetIndex++) {
                Map<Pokemon, Pokemon> replacements = new HashMap<>();
                EncounterSet beforeSet = encounterSetsBefore.get(encSetIndex);
                EncounterSet afterSet = encounterSetsAfter.get(encSetIndex);
                for (int encIndex = 0; encIndex < beforeSet.encounters.size(); encIndex++) {
                    Pokemon before = beforeSet.encounters.get(encIndex).pokemon;
                    Pokemon after = afterSet.encounters.get(encIndex).pokemon;
                    if (!replacements.containsKey(before)) {
                        replacements.put(before, after);
                    }
                    assertSame(before.getName() + " should be replaced by " + replacements.get(before) + ", " +
                                    "was replaced by " + after.getName() + ".",
                            replacements.get(before), after);
                }
            }
        }, new Settings(), encountersGeneralToggles);

    }

    private static class SettingsToggle {

        private final String name;
        private final BiConsumer<Settings, Boolean> wrapped;
        public SettingsToggle(String name, BiConsumer<Settings, Boolean> wrapped) {
            this.name = name;
            this.wrapped = wrapped;
        }

        @Override
        public String toString() {
            return name;
        }

        public void accept(Settings settings, Boolean aBoolean) {
            wrapped.accept(settings, aBoolean);
        }
    }

    /**
     * Tests all possible combination of a set of boolean settings, that should not depend on one another.
     * Runs in O(2^n)!
     * @param test The test to run using the settings.
     * @param settings An existing Settings object, which can have other settings predefined.
     */
    private void repeatTestForAllSettingsToggles(Consumer<Settings> test, Settings settings,
                                                 SettingsToggle[] toggles) {
        for (int i = 0; i < 1 << toggles.length; i++) {

            System.out.print("Testing with");
            for (int j = 0; j < toggles.length; j++) {
                boolean settingOn = ((i >> j) & 1) == 1;
                toggles[j].accept(settings, settingOn);
                System.out.print(" " + toggles[j] + "=" + settingOn);
            }
            System.out.println(".");
            test.accept(settings);

        }

    }

    private List<EncounterSet> randomEncounterSets(int seed) {
        Random random = new Random(seed);
        List<EncounterSet> encounterSets = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            EncounterSet encounterSet = new EncounterSet();
            for (int j = 0; j < 60; j++) {
                Encounter enc = new Encounter();
                enc.pokemon = rh.getPokemonSet().getRandom(random);
                encounterSet.encounters.add(enc);
            }
            encounterSets.add(encounterSet);
        }
        return encounterSets;
    }

}