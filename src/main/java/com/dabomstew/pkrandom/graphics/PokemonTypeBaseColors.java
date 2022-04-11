package com.dabomstew.pkrandom.graphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Type;

public class PokemonTypeBaseColors {

    private Random random;

    private static final double SAME_TYPES_SWAP_COLORS_CHANCE = 0.33;

    private Pokemon pokemon;
    private List<TypeBaseColor> typeBaseColors;
    private PokemonTypeBaseColors prevo;

    public PokemonTypeBaseColors(Pokemon pokemon, Random random) {
        this(pokemon, null, random);
    }

    public PokemonTypeBaseColors(Pokemon pokemon, PokemonTypeBaseColors prevo, Random random) {
        this.pokemon = pokemon;
        this.prevo = prevo;
        this.random = random;
    }

    public List<TypeBaseColor> getTypeBaseColors() {
        return new ArrayList<TypeBaseColor>(typeBaseColors);
    }

    public Pokemon getPokemon() {
        return pokemon;
    }

    public void generateBaseColors(boolean typeSanity, boolean evolutionSanity) {
        boolean noPrevo = !evolutionSanity || prevo == null;
        if (noPrevo) {
            generateTypeBaseColorsBasic(typeSanity);
        } else {
            generateTypeBaseColorsFromPrevo(typeSanity);
        }
    }

    private void generateTypeBaseColorsBasic(boolean typeSanity) {
        // woops, I forgot about the needs of this class when remaking TypeBaseColor,
        // TODO
        typeBaseColors = TypeBaseColor.getTypeBaseColors();
        Collections.shuffle(typeBaseColors, random);

        if (typeSanity) {
            moveTypeColorsToStart();
        }
    }

    private void moveTypeColorsToStart() {
        moveFirstColorOfType(0, pokemon.getPrimaryType());

        Type moveType = pokemon.hasSecondaryType() ? pokemon.getSecondaryType() : pokemon.getPrimaryType();
        moveFirstColorOfType(1, moveType);

        moveFirstColorOfType(2, pokemon.getPrimaryType());

        if (pokemon.hasSecondaryType()) {
            moveFirstColorOfType(3, pokemon.getSecondaryType());
        }
    }

    private void generateTypeBaseColorsFromPrevo(boolean typeSanity) {
        typeBaseColors = prevo.getTypeBaseColors();

        if (!typeSanity) {
            if (random.nextDouble() < SAME_TYPES_SWAP_COLORS_CHANCE) {
                swapColors(0, 1);
            }
        }

        else {

            if (hasSameTypesAsPrevo()) {
                if (random.nextDouble() < SAME_TYPES_SWAP_COLORS_CHANCE) {
                    swapColors(0, 1);
                }
            } else {

                if (!hasSamePrimaryTypeAsPrevo()) {
                    moveFirstColorOfType(0, pokemon.getPrimaryType());
                }

                if (!hasSameSecondaryTypeAsPrevo()) {
                    Type moveType = pokemon.hasSecondaryType() ? pokemon.getSecondaryType() : pokemon.getPrimaryType();
                    moveFirstColorOfType(1, moveType);
                }
            }
        }
    }

    private void swapColors(int a, int b) {
        Collections.swap(typeBaseColors, a, b);
    }

    private void moveFirstColorOfType(int insertIndex, Type type) {
        int colorOfTypeIndex = findColorOfType(insertIndex, type);
        if (colorOfTypeIndex == -1) {
            // TODO: more descriptive error message? (or change this method to auto-fix)
            throw new RuntimeException();
        }
        TypeBaseColor color = typeBaseColors.get(colorOfTypeIndex);
        typeBaseColors.remove(colorOfTypeIndex);
        typeBaseColors.add(insertIndex, color);
    }

    private int findColorOfType(int start, Type type) {
        for (int i = start; i < typeBaseColors.size(); i++) {
            if (typeBaseColors.get(i).hasType(type)) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasSameTypesAsPrevo() {
        return hasSamePrimaryTypeAsPrevo() && hasSameSecondaryTypeAsPrevo();
    }

    private boolean hasSamePrimaryTypeAsPrevo() {
        return pokemon.getPrimaryType() == prevo.pokemon.getPrimaryType();
    }

    private boolean hasSameSecondaryTypeAsPrevo() {
        return pokemon.getSecondaryType() == prevo.pokemon.getSecondaryType();
    }

    @Override
    public String toString() {
        return String.format("%s prevoPk=%s %s", pokemon.getName(), (prevo == null ? null : prevo.pokemon.getName()),
                typeBaseColors);
    }

}
