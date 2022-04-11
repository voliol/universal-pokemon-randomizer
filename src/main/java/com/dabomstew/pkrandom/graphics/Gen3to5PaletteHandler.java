package com.dabomstew.pkrandom.graphics;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.dabomstew.pkrandom.exceptions.RandomizerIOException;
import com.dabomstew.pkrandom.pokemon.Pokemon;

public class Gen3to5PaletteHandler extends PaletteHandler {

    private String paletteDescriptionsFileName;

    public Gen3to5PaletteHandler(Random random, String paletteDescriptionsFileName) {
        super(random);
        this.paletteDescriptionsFileName = paletteDescriptionsFileName;
    }

    @Override
    public void randomizePokemonPalettes(List<Pokemon> pokemonList, boolean typeSanity, boolean evolutionSanity,
            boolean shinyFromNormal) {

        // TODO: Figure out what to do with forms, with different palettes and with the
        // same. One solution is to do nothing initially, and get help from the ZX folks
        // after the "initial release".

        // TODO: figure out genders in gen IV and V, if anything needs to be done at all

        if (shinyFromNormal) {
            makeShinyPalettesFromNormal(pokemonList);
        }

        if (paletteDescriptionsFileName == null) {

            // TODO: better error raising/logging, is there a log file for errors like this
            // that do not need to interrupt the program?
            // If there was a log for putting stuff when only unessential parts of the
            // randomizing
            // failed, then this could throw anything, and
            // AbstractRandomizer.randomizeColors()
            // could have a try-catch block for any uncaught exceptions,
            // printing it into the log there.

            // e.g. - You click "randomize"
            // - Something throws an exception here in the PaletteHandler
            // - The AbstractRomHandler catches it and prints it to a log,
            // doesn't write the palettes.
            // - The rest of the randomization continues
            // - It finishes and the end-user gets the pop-up message
            // "The randomization finished, but with some errors. See..."
            System.out.println("Could not randomize palettes, unrecognized romtype.");
            return;

        } else {

            List<BaseColorsFamily> baseColorsFamilies = generateBaseColorFamilies(pokemonList, typeSanity,
                    evolutionSanity);
            String[] paletteDescriptions = getPaletteDescriptions(paletteDescriptionsFileName, pokemonList.size());
            populatePalettes(baseColorsFamilies, paletteDescriptions);

        }
    }

    private List<BaseColorsFamily> generateBaseColorFamilies(List<Pokemon> pokemonList, boolean typeSanity,
            boolean evolutionSanity) {
        List<BaseColorsFamily> baseColorsFamilies = new ArrayList<>();
        List<Pokemon> basicPokemonList = evolutionSanity ? filterOutBasicPokemon(pokemonList) : pokemonList;
        for (Pokemon basicPk : basicPokemonList) {
            baseColorsFamilies.add(new BaseColorsFamily(basicPk, evolutionSanity, random));
        }
        for (BaseColorsFamily bcFamily : baseColorsFamilies) {
            bcFamily.generateBaseColors(typeSanity, evolutionSanity);
        }
        return baseColorsFamilies;
    }

    // perhaps could use some higher-scope function, seems like it would be pretty
    // common
    private List<Pokemon> filterOutBasicPokemon(List<Pokemon> pokemonList) {
        List<Pokemon> basicPokemonList = new ArrayList<>();
        for (Pokemon pk : pokemonList) {
            if (pk.getEvolutionsTo().size() == 0) {
                basicPokemonList.add(pk);
            }
        }
        return basicPokemonList;
    }

    private void populatePalettes(List<BaseColorsFamily> baseColorsFamilies, String[] paletteDescriptions) {

        PalettePopulator pp = new PalettePopulator(random);

        for (BaseColorsFamily bcFamily : baseColorsFamilies) {
            for (PokemonTypeBaseColors pkTBC : bcFamily.getMembers()) {

                Pokemon pk = pkTBC.getPokemon();
                Palette palette = pk.getNormalPalette();
                int pokemonNumber = pk.getNumber();

                String paletteDescription = paletteDescriptions[pokemonNumber - 1];
                String[] partDescriptions = paletteDescription.split("/");

                for (int i = 0; i < partDescriptions.length; i++) {
                    if (!partDescriptions[i].isBlank()) {
                        TypeBaseColor tbc = pkTBC.getTypeBaseColors().get(i);
                        Color baseColor = bcFamily.getBaseColor(tbc);

                        pp.populatePartFromBaseColor(palette, partDescriptions[i], baseColor);
                    }
                }

            }
        }
    }

    private String[] getPaletteDescriptions(String fileName, int length) {
        String[] paletteDescriptions = new String[length];
        for (int i = 0; i < paletteDescriptions.length; i++) {
            paletteDescriptions[i] = "";
        }

        InputStream infi = getClass().getResourceAsStream("resources/TBE/" + fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(infi));

        String line;
        int i = 0;
        try {
            while ((line = br.readLine()) != null && i < paletteDescriptions.length) {
                paletteDescriptions[i] = line.trim();
                i++;
            }
        } catch (java.io.IOException ioe) {
            // using RandomizerIOException because it is unchecked
            throw new RandomizerIOException("Could not read palette description file " + fileName + ".");
        }

        return paletteDescriptions;
    }

}
