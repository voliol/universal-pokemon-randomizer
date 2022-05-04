package com.dabomstew.pkrandom.graphics;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.dabomstew.pkrandom.exceptions.RandomizerIOException;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.romhandlers.BasePokemonAction;
import com.dabomstew.pkrandom.romhandlers.CopyUpEvolutionsHelper;
import com.dabomstew.pkrandom.romhandlers.EvolvedPokemonAction;

public class Gen3to5PaletteHandler extends PaletteHandler {

	private String paletteDescriptionsFileName;

	private boolean typeSanity;
	private boolean shinyFromNormal;
	private Map<Pokemon, BaseColorsFamily> baseColorsFamilies;
	private Map<Pokemon, PokemonTypeBaseColors> pokemonTypeBaseColors;

	public Gen3to5PaletteHandler(Random random, String paletteDescriptionsFileName) {
		super(random);
		this.paletteDescriptionsFileName = paletteDescriptionsFileName;
	}

	@Override
	public void randomizePokemonPalettes(CopyUpEvolutionsHelper copyUpEvolutionsHelper, boolean typeSanity,
			boolean evolutionSanity, boolean shinyFromNormal) {

		// TODO: Figure out what to do with forms, with different palettes and with the
		// same. One solution is to do nothing initially, and get help from the ZX folks
		// after the "initial release".

		// TODO: figure out genders in gen IV and V, if anything needs to be done at all

		this.typeSanity = typeSanity;
		this.shinyFromNormal = shinyFromNormal;
		this.baseColorsFamilies = new HashMap<>();
		this.pokemonTypeBaseColors = new HashMap<>();

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
			copyUpEvolutionsHelper.apply(evolutionSanity, new BasePokemonPaletteAction(),
					new EvolvedPokemonPaletteAction());
			List<String> paletteDescriptions = getPaletteDescriptions(paletteDescriptionsFileName);
			populatePalettes(paletteDescriptions);

		}
	}

	private void populatePalettes(List<String> paletteDescriptions) {

		PalettePopulator pp = new PalettePopulator(random);

		for (BaseColorsFamily bcFamily : new HashSet<>(baseColorsFamilies.values())) {
			for (Pokemon pk : bcFamily.getMembers()) {

				PokemonTypeBaseColors pkTBCs = pokemonTypeBaseColors.get(pk);

				Palette palette = pk.getNormalPalette();
				int pokemonNumber = pk.getNumber();

				String[] partDescriptions = getPartDescriptions(paletteDescriptions, pokemonNumber);

				for (int i = 0; i < partDescriptions.length; i++) {
					if (!partDescriptions[i].isBlank()) {
						TypeBaseColor tbc = pkTBCs.getTypeBaseColors().get(i);
						Color baseColor = bcFamily.getBaseColor(tbc);

						pp.populatePartFromBaseColor(palette, partDescriptions[i], baseColor);
					}
				}

			}
		}
	}

	private String[] getPartDescriptions(List<String> paletteDescriptions, int pokemonNumber) {
		boolean validIndex = pokemonNumber - 1 <= paletteDescriptions.size();
		String paletteDescription = validIndex ? paletteDescriptions.get(pokemonNumber - 1) : "";
		String[] partDescriptions = paletteDescription.split("/");
		return partDescriptions;
	}

	// TODO: maybe change to String[] just to match getPartDescriptions
	private List<String> getPaletteDescriptions(String fileName) {
		List<String> paletteDescriptions = new ArrayList<>();

		InputStream infi = getClass().getResourceAsStream("resources/TBE/" + fileName);
		BufferedReader br = new BufferedReader(new InputStreamReader(infi));

		String line;
		try {
			while ((line = br.readLine()) != null) {
				paletteDescriptions.add(line.trim());
			}
		} catch (java.io.IOException ioe) {
			// using RandomizerIOException because it is unchecked
			throw new RandomizerIOException("Could not read palette description file " + fileName + ".");
		}

		return paletteDescriptions;
	}

	private class BasePokemonPaletteAction implements BasePokemonAction {

		@Override
		public void applyTo(Pokemon pk) {
			if (shinyFromNormal) {
				setShinyPaletteFromNormal(pk);
			}

			BaseColorsFamily bcFamily = new BaseColorsFamily(random);
			bcFamily.addMember(pk);
			baseColorsFamilies.put(pk, bcFamily);

			PokemonTypeBaseColors pkTBCs = new PokemonTypeBaseColors(pk, typeSanity, random);
			pokemonTypeBaseColors.put(pk, pkTBCs);

		}

	}

	private class EvolvedPokemonPaletteAction implements EvolvedPokemonAction {

		@Override
		public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
			if (shinyFromNormal) {
				setShinyPaletteFromNormal(evTo);
			}

			BaseColorsFamily bcFamily = baseColorsFamilies.get(evFrom);
			bcFamily.addMember(evTo);
			baseColorsFamilies.put(evTo, bcFamily);

			PokemonTypeBaseColors prevo = pokemonTypeBaseColors.get(evFrom);
			PokemonTypeBaseColors pkTBCs = new PokemonTypeBaseColors(evTo, prevo, typeSanity, random);
			pokemonTypeBaseColors.put(evTo, pkTBCs);

		}

	}

}
