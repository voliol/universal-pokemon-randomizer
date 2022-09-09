package test.romhandlers;

import com.dabomstew.pkrandom.constants.Species;
import com.dabomstew.pkrandom.pokemon.Evolution;
import com.dabomstew.pkrandom.pokemon.EvolutionType;
import com.dabomstew.pkrandom.pokemon.Pokemon;

public class PokeConstants {

    public static Pokemon oddish;
    public static Pokemon gloom;
    public static Pokemon vileplume;
    public static Pokemon bellossom;
    public static Pokemon sunkern;

    static {
        setupPokemon();
    }

    private static void setupPokemon() {
        oddish = new Pokemon(Species.oddish);
        oddish.setName("Oddish");
        gloom = new Pokemon(Species.gloom);
        gloom.setName("Gloom");
        vileplume = new Pokemon(Species.vileplume);
        vileplume.setName("Vileplume");
        bellossom = new Pokemon(Species.bellossom);
        bellossom.setName("Bellossom");
        sunkern = new Pokemon(Species.sunkern);
        sunkern.setName("Sunkern");

        evolvesTo(oddish, gloom, true);
        evolvesTo(gloom, vileplume, false);
        evolvesTo(gloom, bellossom, false);
    }

    private static void evolvesTo(Pokemon from, Pokemon to, boolean carrystats) {
        Evolution evo = new Evolution(from, to, carrystats, EvolutionType.LEVEL, 0);
        from.getEvolutionsFrom().add(evo);
        to.getEvolutionsTo().add(evo);
    }

}
