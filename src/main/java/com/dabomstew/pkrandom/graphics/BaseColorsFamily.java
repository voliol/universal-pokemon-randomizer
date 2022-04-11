package com.dabomstew.pkrandom.graphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.dabomstew.pkrandom.pokemon.Pokemon;

public class BaseColorsFamily {

    private static final double TWEAK_RAND_MIN_COEFF = -0.032;
    private static final double TWEAK_RAND_MIN_ADDEND = 7.1;
    private static final double TWEAK_RAND_MAX_COEFF = -0.042;
    private static final double TWEAK_RAND_MAX_ADDEND = 17.19;

    private Random random;

    private Map<TypeBaseColor, Color> baseColorMap;
    private List<PokemonTypeBaseColors> members;

    public BaseColorsFamily(Pokemon basicPk, boolean evolutionSanity, Random random) {
        this.random = random;
        initializeBaseColorMap();
        initializeMembers(basicPk, evolutionSanity);
    }

    private void initializeBaseColorMap() {
        this.baseColorMap = new HashMap<>();
        for (TypeBaseColor tbc : TypeBaseColor.getTypeBaseColors()) {
            Color baseColor = randomlyTweakColor(tbc.getColor());
            baseColorMap.put(tbc, baseColor);
        }
    }

    private Color randomlyTweakColor(Color color) {
        // based on Artemis251's Emerald randomizer code
        Color tweakedColor = new Color();
        for (int i = 0; i < 3; i++) { // for each in R,G,B
            int randMin = (int) (TWEAK_RAND_MIN_COEFF * color.getComp(i) + TWEAK_RAND_MIN_ADDEND);
            int randMax = (int) (TWEAK_RAND_MAX_COEFF * color.getComp(i) + TWEAK_RAND_MAX_ADDEND);
            int change = random.nextInt(randMax - randMin) + random.nextInt(randMax);
            int value = color.getComp(i) + (int) (color.getComp(i) * change * 0.01 * Math.pow(-1, random.nextInt(2)));
            value = value > 255 ? 255 : value;
            tweakedColor.setComp(i, value);
        }
        return tweakedColor;
    }

    private void initializeMembers(Pokemon basicPk, boolean evolutionSanity) {
        this.members = new ArrayList<>();

        PokemonTypeBaseColors basicPkTBC = new PokemonTypeBaseColors(basicPk, random);
        members.add(basicPkTBC);

        if (evolutionSanity) {
            recursiveAddMembersFromEvo(basicPkTBC);
        }
    }

    private void recursiveAddMembersFromEvo(PokemonTypeBaseColors pkTBC) {
        Pokemon pk = pkTBC.getPokemon();
        for (Pokemon from : pk.getEvolutionsFromPokemon()) {
            PokemonTypeBaseColors fromPkTBC = new PokemonTypeBaseColors(from, pkTBC, random);
            members.add(fromPkTBC);
            recursiveAddMembersFromEvo(fromPkTBC);
        }
    }

    public void generateBaseColors(boolean typeSanity, boolean evolutionSanity) {
        for (PokemonTypeBaseColors member : members) {
            member.generateBaseColors(typeSanity, evolutionSanity);
        }
    }

    public Color getBaseColor(TypeBaseColor typeBaseColor) {
        // unsure if this is where to put copy protection
        return new Color(baseColorMap.get(typeBaseColor));
    }

    public List<PokemonTypeBaseColors> getMembers() {
        return Collections.unmodifiableList(members);
    }

}
