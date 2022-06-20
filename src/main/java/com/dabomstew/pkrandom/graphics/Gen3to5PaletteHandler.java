package com.dabomstew.pkrandom.graphics;

/*----------------------------------------------------------------------------*/
/*--  Part of "Universal Pokemon Randomizer" by Dabomstew                   --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2012.                                 --*/
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.dabomstew.pkrandom.exceptions.RandomizerIOException;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.romhandlers.BasePokemonAction;
import com.dabomstew.pkrandom.romhandlers.CopyUpEvolutionsHelper;
import com.dabomstew.pkrandom.romhandlers.EvolvedPokemonAction;

/**
 * A {@link PaletteHandler} for Gen 3, Gen 4, and Gen 5 games (R/S/E/FR/LG,
 * D/P/Pt/HG/SS, B/W/B2/W2).
 * <p>
 * All three generations use similar 16-color palettes, that are implemented
 * using the {@link Palette} class. These palettes are populated/filled/modified
 * by {@link PalettePopulator}, using {@link PalettePartDescription}s as
 * instructions.
 * <p>
 * When Pokémon palettes are randomized, each Pokémon is assigned a
 * {@link TypeBaseColorList}, which uses its types to come up with appropriate
 * base colors.
 */
public class Gen3to5PaletteHandler extends PaletteHandler {

	private String paletteFilesID;

	private boolean typeSanity;
	private boolean shinyFromNormal;
	private Map<Pokemon, TypeBaseColorList> typeBaseColorLists;

	public Gen3to5PaletteHandler(Random random, String paletteFilesID) {
		super(random);
		this.paletteFilesID = paletteFilesID;
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
		this.typeBaseColorLists = new HashMap<>();

		if (paletteFilesID == null) {

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
			copyUpEvolutionsHelper.apply(evolutionSanity, false, new BasePokemonPaletteAction(),
					new EvolvedPokemonPaletteAction());
			List<String> paletteDescriptions = getPaletteDescriptions("pokePalettes");
			populatePokemonPalettes(paletteDescriptions);

		}
	}

	private void populatePokemonPalettes(List<String> paletteDescriptions) {

		PalettePopulator pp = new PalettePopulator(random);

		for (Entry<Pokemon, TypeBaseColorList> entry : typeBaseColorLists.entrySet()) {

			Pokemon pk = entry.getKey();
			Palette palette = pk.getNormalPalette();
			TypeBaseColorList typeBaseColorList = entry.getValue();
			PalettePartDescription[] palettePartDescriptions = getPalettePartDescriptions(pk, paletteDescriptions);

			populatePalette(palette, pp, typeBaseColorList, palettePartDescriptions);

		}
	}

	public void populatePalette(Palette palette, PalettePopulator pp, TypeBaseColorList typeBaseColorList,
			PalettePartDescription[] palettePartDescriptions) {
		
		for (int i = 0; i < palettePartDescriptions.length; i++) {
			
			if (palettePartDescriptions[i].isAverageDescription()) {
				pp.populateAverageColor(palette, palettePartDescriptions[i]);
				
			} else if (!palettePartDescriptions[i].isBlank()) {
				Color baseColor = typeBaseColorList.getBaseColor(i);
				LightDarkMode lightDarkMode = typeBaseColorList.getLightDarkMode(i);
				pp.populatePartFromBaseColor(palette, palettePartDescriptions[i], baseColor, lightDarkMode);
			}
			
		}
	}

	public PalettePartDescription[] getPalettePartDescriptions(Pokemon pk, List<String> paletteDescriptions) {
		int paletteIndex = pk.getNumber() - 1;
		boolean validIndex = paletteIndex <= paletteDescriptions.size();
		return PalettePartDescription.allFromString(validIndex ? paletteDescriptions.get(paletteIndex) : "");
	}

	public List<String> getPaletteDescriptions(String fileKey) {
		List<String> paletteDescriptions = new ArrayList<>();

		String fileName = fileKey + paletteFilesID + ".txt";
		InputStream infi = getClass().getResourceAsStream("resources/" + fileName);
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

			TypeBaseColorList typeBaseColorList = new TypeBaseColorList(pk, typeSanity, random);
			typeBaseColorLists.put(pk, typeBaseColorList);

		}

	}

	private class EvolvedPokemonPaletteAction implements EvolvedPokemonAction {

		@Override
		public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
			if (shinyFromNormal) {
				setShinyPaletteFromNormal(evTo);
			}
			TypeBaseColorList prevo = typeBaseColorLists.get(evFrom);
			TypeBaseColorList typeBaseColorList = new TypeBaseColorList(evTo, prevo, typeSanity, random);
			typeBaseColorLists.put(evTo, typeBaseColorList);

		}

	}

}
