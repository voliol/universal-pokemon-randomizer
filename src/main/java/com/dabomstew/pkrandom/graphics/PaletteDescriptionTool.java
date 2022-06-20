package com.dabomstew.pkrandom.graphics;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.dabomstew.pkrandom.RandomSource;
import com.dabomstew.pkrandom.Utils;
import com.dabomstew.pkrandom.exceptions.RandomizerIOException;
import com.dabomstew.pkrandom.gui.ROMFilter;
import com.dabomstew.pkrandom.newnds.NARCArchive;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.romhandlers.AbstractDSRomHandler;
import com.dabomstew.pkrandom.romhandlers.AbstractGBRomHandler;
import com.dabomstew.pkrandom.romhandlers.Gen3RomHandler;
import com.dabomstew.pkrandom.romhandlers.Gen4RomHandler;
import com.dabomstew.pkrandom.romhandlers.Gen5RomHandler;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

// TODO: show the name of the loaded ROM/desc file
// TODO: palette slot on hover-over

public class PaletteDescriptionTool extends javax.swing.JFrame {

	/**
	 * 1.0
	 */
	private static final long serialVersionUID = 7741836888133659367L;

	private static final Random RND = new Random();

	private static class PaletteImageLabel extends JLabel {

		/**
		 * 1.0
		 */
		private static final long serialVersionUID = 7324176616059578530L;

		private ImageIcon icon;
		private int scale = 2;

		private PaletteImageLabel(BufferedImage bim) {
			if (bim == null) {
				bim = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
			}
			this.icon = new ImageIcon(bim);
			setIcon(icon);
		}

		public void setImage(BufferedImage bim) {
			Image scaled = bim.getScaledInstance(bim.getWidth()*scale, bim.getHeight()*scale, Image.SCALE_DEFAULT);
			icon.setImage(scaled);
			revalidate();
			repaint();
		}

	}

	private static GridBagConstraints quickGBC(int gridx, int gridy) {
		return quickGBC(gridx, gridy, 1, 1);
	}

	private static GridBagConstraints quickGBC(int gridx, int gridy, int gridwidth, int gridheight) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = gridx;
		gbc.gridy = gridy;
		gbc.gridwidth = gridwidth;
		gbc.gridheight = gridheight;
		return gbc;
	}

	private RomHandler romHandler;
	private RomHandler.Factory[] checkHandlers = new RomHandler.Factory[] { new Gen3RomHandler.Factory(),
			new Gen4RomHandler.Factory(), new Gen5RomHandler.Factory() };
	private boolean romLoaded;

	private Map<Pokemon, Palette> originalPalettes = new HashMap<>();

	private JList<String> paletteDescriptions;
	private PaletteImageLabel originalImage;
	private PaletteImageLabel exampleImage;
	private JTextField rawDescField;

	private String unchanged;

	public static void main(String args[]) {
		new PaletteDescriptionTool().setVisible(true);
	}

	private PaletteDescriptionTool() {
		JPanel mainPanel = new JPanel();
		setContentPane(mainPanel);

		// ---------

		JPanel leftPanel = new JPanel();
		mainPanel.add(leftPanel, quickGBC(0, 0));
		leftPanel.setLayout(new GridBagLayout());

		JButton loadFilesButton = new JButton("Load ROM and desc. file");
		loadFilesButton.addActionListener(this::loadBoth);
		leftPanel.add(loadFilesButton, quickGBC(0, 0, 2, 1));

		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(this::savePaletteDescription);
		leftPanel.add(saveButton, quickGBC(0, 1));

		JButton resetButton = new JButton("Reset");
		resetButton.addActionListener(this::resetPaletteDescription);
		leftPanel.add(resetButton, quickGBC(1, 1));

		JButton prevButton = new JButton("Prev.");
		prevButton.addActionListener(this::previousPaletteDescription);
		leftPanel.add(prevButton, quickGBC(0, 2));

		JButton nextButton = new JButton("Next");
		nextButton.addActionListener(this::nextPaletteDescription);
		leftPanel.add(nextButton, quickGBC(1, 2));

		JScrollPane entryScrollPane = new JScrollPane();
		paletteDescriptions = new JList<>();
		paletteDescriptions.addListSelectionListener(event -> updateImagesAndText());
		entryScrollPane.setViewportView(paletteDescriptions);
		leftPanel.add(entryScrollPane, quickGBC(0, 3, 2, 1));

		// -----------

		JPanel rightPanel = new JPanel();
		mainPanel.add(rightPanel, quickGBC(1, 0));
		rightPanel.setLayout(new GridBagLayout());

		JLabel originalImageLabel = new JLabel("Original:");
		rightPanel.add(originalImageLabel, quickGBC(0, 0));
		originalImage = new PaletteImageLabel(null);
		rightPanel.add(originalImage, quickGBC(0, 1));

		JLabel exampleImageLabel = new JLabel("Randomized example:");
		rightPanel.add(exampleImageLabel, quickGBC(1, 0));
		exampleImage = new PaletteImageLabel(null);
		rightPanel.add(exampleImage, quickGBC(1, 1));
		JButton newExampleButton = new JButton("New example");
		newExampleButton.addActionListener(event -> newRandomizedExample());
		rightPanel.add(newExampleButton, quickGBC(2, 1));

		JLabel rawDescFieldLabel = new JLabel("Palette desc.:");
		rightPanel.add(rawDescFieldLabel, quickGBC(0, 2));
		rawDescField = new JTextField();
		rawDescField.setPreferredSize(new Dimension(300, 20));
		rightPanel.add(rawDescField, quickGBC(0, 3, 3, 1));
	}

	private void loadBoth(ActionEvent event) {

		// TODO: create some kind of back-ups

		JFileChooser romChooser = new JFileChooser();
		romChooser.setFileFilter(new ROMFilter());
		if (romChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		try {
			openRom(romChooser.getSelectedFile());
		} catch (RandomizerIOException e) {
			e.printStackTrace();
			return;
		}

		Gen3to5PaletteHandler paletteHandler = (Gen3to5PaletteHandler) romHandler.getPaletteHandler();
		paletteDescriptions.setListData(paletteHandler.getPaletteDescriptions("pokePalettes").toArray(new String[0]));
		for (Pokemon pk : romHandler.getPokemonWithoutNull()) {
			originalPalettes.put(pk, pk.getNormalPalette());
		}
		paletteDescriptions.setSelectedIndex(0);

	}

	private void openRom(File file) {
		try {
			Utils.validateRomFile(file);
		} catch (Utils.InvalidROMException e) {
			throw new RandomizerIOException("Invalid ROM file.\n" + e.getStackTrace());
		}

		for (RomHandler.Factory rhf : checkHandlers) {
			if (rhf.isLoadable(file.getAbsolutePath())) {
				romHandler = rhf.create(RandomSource.instance());
				try {
					romHandler.loadRom(file.getAbsolutePath());
				} catch (Exception e) {
					romHandler = null;
					throw new RandomizerIOException("ROM file could not be loaded.\n" + e.getStackTrace());
				}
			}
		}

		romLoaded = true;
	}

	// so the same can easily be handled by ctrl-R and the button
	private void resetPaletteDescription(ActionEvent event) {
		rawDescField.setText(unchanged);
	}

	private void savePaletteDescription(ActionEvent event) {
		// TODO save the text file

	}

	private void previousPaletteDescription(ActionEvent event) {
		int currentIndex = paletteDescriptions.getSelectedIndex();
		int newIndex = currentIndex == 0 ? paletteDescriptions.getModel().getSize() - 1 : currentIndex - 1;
		paletteDescriptions.setSelectedIndex(newIndex);
	}

	private void nextPaletteDescription(ActionEvent event) {
		int currentIndex = paletteDescriptions.getSelectedIndex();
		int newIndex = currentIndex == paletteDescriptions.getModel().getSize() - 1 ? 0 : currentIndex + 1;
		paletteDescriptions.setSelectedIndex(newIndex);
	}

	private void newRandomizedExample() {
		randomizePalette(getCurrentPokemon(), rawDescField.getText());
		exampleImage.setImage(getPokemonImage(getCurrentPokemon()));
	}

	private void randomizePalette(Pokemon pk, String paletteDescription) {
		Palette palette = pk.getNormalPalette();
		PalettePopulator pp = new PalettePopulator(RND);
		TypeBaseColorList typeBaseColorList = new TypeBaseColorList(pk, false, RND);
		PalettePartDescription[] palettePartDescriptions = PalettePartDescription.allFromString(paletteDescription);
		
		Gen3to5PaletteHandler paletteHandler = (Gen3to5PaletteHandler) romHandler.getPaletteHandler();
		paletteHandler.populatePalette(palette, pp, typeBaseColorList, palettePartDescriptions);
		
	}

	private void updateImagesAndText() {
		if (romLoaded) {
			rawDescField.setText(paletteDescriptions.getSelectedValue());
			unchanged = rawDescField.getText();

			Pokemon pk = getCurrentPokemon();
			pk.setNormalPalette(originalPalettes.get(pk).clone());
			originalImage.setImage(getPokemonImage(pk));
			newRandomizedExample();
		}
	}

	private Pokemon getCurrentPokemon() {
		Pokemon pk = romHandler.getPokemon().get(paletteDescriptions.getSelectedIndex() + 1);
		return pk;
	}

	private BufferedImage getPokemonImage(Pokemon pk) {
		BufferedImage pokemonImage = null;
		if (romHandler instanceof AbstractGBRomHandler gbRomHandler) {
			pokemonImage = gbRomHandler.getPokemonImage(pk, false, false, false, false);
		} else if (romHandler instanceof AbstractDSRomHandler dsRomHandler) {
			String NARCpath = dsRomHandler.getNARCPath("PokemonGraphics");
			NARCArchive pokeGraphicsNARC = null;
			try {
				pokeGraphicsNARC = dsRomHandler.readNARC(NARCpath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pokemonImage = dsRomHandler.getPokemonImage(pk, pokeGraphicsNARC, false, false, false, false);
		}
		return pokemonImage;
	}

}
