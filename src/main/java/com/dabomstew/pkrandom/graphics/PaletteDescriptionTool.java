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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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

public class PaletteDescriptionTool extends javax.swing.JFrame {

	/**
	 * 1.0
	 */
	private static final long serialVersionUID = 7741836888133659367L;

	private static final Random RND = new Random();

	private static final int DEFAULT_SCALE = 2;

	private static class PaletteImageLabel extends JLabel {

		/**
		 * 1.0
		 */
		private static final long serialVersionUID = 7324176616059578530L;

		private BufferedImage bufferedImage;
		private ImageIcon icon;
		private int scale = DEFAULT_SCALE;

		private PaletteImageLabel(BufferedImage bufferedImage) {
			if (bufferedImage == null) {
				bufferedImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
			}
			this.icon = new ImageIcon();
			setIcon(icon);
			setImage(bufferedImage);
		}

		public void setImage(BufferedImage bufferedImage) {
			this.bufferedImage = bufferedImage;
			update();
		}

		public void setScale(int scale) {
			this.scale = scale;
			update();
		}

		public int getPaletteIndexAtCoord(int x, int y) {
			int pixelX = x / scale;
			int pixelY = y / scale;
			return bufferedImage.getData().getSample(pixelX, pixelY, 0) + 1;
		}

		private void update() {
			Image scaled = bufferedImage.getScaledInstance(bufferedImage.getWidth() * scale,
					bufferedImage.getHeight() * scale, Image.SCALE_DEFAULT);
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

	private JList<PaletteDescription> paletteDescriptions;
	private int lastIndex;
	private PaletteImageLabel originalImage;
	private PaletteImageLabel exampleImage;
	private JTextField descNameField;
	private JTextField descBodyField;
	private JTextField descNoteField;

	private boolean autoName;

	private JButton saveDescButton;
	private boolean autoSave;

	private String unchangedName;
	private String unchangedBody;
	private String unchangedNote;

	public static void main(String args[]) {
		new PaletteDescriptionTool().setVisible(true);
	}

	private PaletteDescriptionTool() {
		this.setSize(new Dimension(800, 400));
		JPanel mainPanel = new JPanel();
		setContentPane(mainPanel);

		// ---------

		JPanel leftPanel = new JPanel();
		mainPanel.add(leftPanel);
		leftPanel.setLayout(new GridBagLayout());

		JButton loadFilesButton = new JButton("Load ROM");
		loadFilesButton.addActionListener(e -> loadBoth());
		leftPanel.add(loadFilesButton, quickGBC(0, 0));

		JButton saveButton = new JButton("Save to file");
		saveButton.addActionListener(e -> savePaletteDescriptionsFile());
		leftPanel.add(saveButton, quickGBC(1, 0));

		JButton prevButton = new JButton("Prev.");
		prevButton.addActionListener(e -> previousPaletteDescription());
		leftPanel.add(prevButton, quickGBC(0, 1));

		JButton nextButton = new JButton("Next");
		nextButton.addActionListener(e -> nextPaletteDescription());
		leftPanel.add(nextButton, quickGBC(1, 1));

		JScrollPane entryScrollPane = new JScrollPane();
		paletteDescriptions = new JList<>();
		paletteDescriptions.addListSelectionListener(new PaletteDescriptionSelectionListener());
		entryScrollPane.setViewportView(paletteDescriptions);
		leftPanel.add(entryScrollPane, quickGBC(0, 2, 2, 1));

		// -----------

		JPanel rightPanel = new JPanel();
		mainPanel.add(rightPanel);
		rightPanel.setLayout(new BorderLayout());

		JPanel scalePanel = new JPanel();
		rightPanel.add(scalePanel, BorderLayout.PAGE_START);
		scalePanel.setLayout(new GridBagLayout());

		JLabel scaleSliderLabel = new JLabel("Scale:");
		scalePanel.add(scaleSliderLabel, quickGBC(0, 0));
		JSlider scaleSlider = new JSlider(1, 8);
		scaleSlider.setMajorTickSpacing(1);
		scaleSlider.setPaintLabels(true);
		scaleSlider.setPaintTicks(true);
		scaleSlider.setPaintTrack(false);
		scaleSlider.setValue(DEFAULT_SCALE);
		scaleSlider.addChangeListener(new ScaleListener());
		scaleSlider.updateUI();
		scalePanel.add(scaleSlider, quickGBC(1, 0));

		JPanel imagePanel = new JPanel();
		rightPanel.add(imagePanel, BorderLayout.CENTER);
		imagePanel.setLayout(new GridBagLayout());

		ShowPaletteListener showPaletteListener = new ShowPaletteListener();

		JLabel originalImageLabel = new JLabel("Original:");
		imagePanel.add(originalImageLabel, quickGBC(0, 0));
		originalImage = new PaletteImageLabel(null);
		originalImage.addMouseMotionListener(showPaletteListener);
		imagePanel.add(originalImage, quickGBC(0, 1));

		JLabel exampleImageLabel = new JLabel("Randomized example:");
		imagePanel.add(exampleImageLabel, quickGBC(1, 0));
		exampleImage = new PaletteImageLabel(null);
		exampleImage.addMouseMotionListener(showPaletteListener);
		imagePanel.add(exampleImage, quickGBC(1, 1));

		JButton newExampleButton = new JButton("New example");
		newExampleButton.addActionListener(event -> newRandomizedExample());
		imagePanel.add(newExampleButton, quickGBC(2, 1));

		JPanel descPanel = new JPanel();
		rightPanel.add(descPanel, BorderLayout.PAGE_END);
		descPanel.setLayout(new GridBagLayout());

		JLabel descNameLabel = new JLabel("Name:");
		descPanel.add(descNameLabel, quickGBC(0, 0));
		descNameField = new JTextField();
		descNameField.setPreferredSize(new Dimension(300, 20));
		descPanel.add(descNameField, quickGBC(1, 0));
		JCheckBox autoNameCB = new JCheckBox("Auto-name");
		autoNameCB.addItemListener(new AutoNameListener());
		descPanel.add(autoNameCB, quickGBC(2, 0));

		JLabel descBaseLabel = new JLabel("Palette desc.:");
		descPanel.add(descBaseLabel, quickGBC(0, 1));
		descBodyField = new JTextField();
		descBodyField.setPreferredSize(new Dimension(300, 20));
		descPanel.add(descBodyField, quickGBC(1, 1, 2, 1));

		JLabel descNoteLabel = new JLabel("Note:");
		descPanel.add(descNoteLabel, quickGBC(0, 2));
		descNoteField = new JTextField();
		descNoteField.setPreferredSize(new Dimension(300, 20));
		descPanel.add(descNoteField, quickGBC(1, 2, 2, 1));

		saveDescButton = new JButton("Save desc.");
		saveDescButton.addActionListener(e -> savePaletteDescription());
		descPanel.add(saveDescButton, quickGBC(1, 3));
		JButton resetDescButton = new JButton("Reset desc.");
		resetDescButton.addActionListener(e -> resetPaletteDescription());
		descPanel.add(resetDescButton, quickGBC(2, 3));
		JCheckBox autoSaveDescCB = new JCheckBox("Auto-save desc.");
		autoSaveDescCB.addItemListener(new AutoSaveListener());
		descPanel.add(autoSaveDescCB, quickGBC(1, 4));
	}

	private void loadBoth() {

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
		for (Pokemon pk : romHandler.getPokemonWithoutNull()) {
			originalPalettes.put(pk, pk.getNormalPalette());
		}
		paletteDescriptions
				.setListData(paletteHandler.getPaletteDescriptions("pokePalettes").toArray(new PaletteDescription[0]));
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

	private void savePaletteDescriptionsFile() {
		// TODO
	}

	private void resetPaletteDescription() {
		if (!autoName) {
			descNameField.setText(unchangedName);
		}
		descBodyField.setText(unchangedBody);
		descNoteField.setText(unchangedNote);
	}
	
	private void savePaletteDescription() {
		savePaletteDescription(paletteDescriptions.getSelectedIndex());
		unchangedName = descNameField.getText();
		unchangedBody = descBodyField.getText();
		unchangedNote = descNoteField.getText();
	}
	
	private void savePaletteDescription(int index) {
		PaletteDescription paletteDescription = paletteDescriptions.getModel().getElementAt(index);
		paletteDescription.setName(descNameField.getText());
		paletteDescription.setBody(descBodyField.getText());
		paletteDescription.setNote(descNoteField.getText());
	}
	
	private void previousPaletteDescription() {
		int currentIndex = paletteDescriptions.getSelectedIndex();
		int newIndex = currentIndex == 0 ? paletteDescriptions.getModel().getSize() - 1 : currentIndex - 1;
		paletteDescriptions.setSelectedIndex(newIndex);
	}

	private void nextPaletteDescription() {
		int currentIndex = paletteDescriptions.getSelectedIndex();
		int newIndex = currentIndex == paletteDescriptions.getModel().getSize() - 1 ? 0 : currentIndex + 1;
		paletteDescriptions.setSelectedIndex(newIndex);
	}

	private void newRandomizedExample() {
		randomizePalette(getCurrentPokemon(), descBodyField.getText());
		exampleImage.setImage(getPokemonImage(getCurrentPokemon()));
	}

	private void randomizePalette(Pokemon pk, String paletteDescriptionBody) {
		pk.setNormalPalette(originalPalettes.get(pk).clone());
		Palette palette = pk.getNormalPalette();
		PalettePopulator pp = new PalettePopulator(RND);
		TypeBaseColorList typeBaseColorList = new TypeBaseColorList(pk, false, RND);
		PalettePartDescription[] palettePartDescriptions = PalettePartDescription.allFrom(paletteDescriptionBody);

		Gen3to5PaletteHandler paletteHandler = (Gen3to5PaletteHandler) romHandler.getPaletteHandler();
		paletteHandler.populatePalette(palette, pp, typeBaseColorList, palettePartDescriptions);
	}

	private void updateImagesAndText() {
		if (romLoaded) {
			descNameField.setText(paletteDescriptions.getSelectedValue().getName());
			unchangedName = descNameField.getText();
			if (autoName) {
				autoNameDesc();
			}
			descBodyField.setText(paletteDescriptions.getSelectedValue().getBody());
			unchangedBody = descBodyField.getText();
			descNoteField.setText(paletteDescriptions.getSelectedValue().getNote());
			unchangedNote = descNoteField.getText();

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

	private void autoNameDesc() {
		String autoName = getCurrentPokemon().getName();
		descNameField.setText(autoName);
	}
	
	private class PaletteDescriptionSelectionListener implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (autoSave) {
				savePaletteDescription(lastIndex);
			}
			updateImagesAndText();
			@SuppressWarnings("unchecked")
			JList<PaletteDescription> source = (JList<PaletteDescription>) e.getSource();
			lastIndex = source.getSelectedIndex();
		}
		
	}

	private class ScaleListener implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider) e.getSource();
			int scale = source.getValue();
			originalImage.setScale(scale);
			exampleImage.setScale(scale);
		}

	}

	private class ShowPaletteListener implements MouseMotionListener {

		@Override
		public void mouseDragged(MouseEvent e) {
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			PaletteImageLabel source = (PaletteImageLabel) e.getSource();
			int paletteIndex = source.getPaletteIndexAtCoord(e.getX(), e.getY());
			source.setToolTipText("" + paletteIndex);
		}

	}

	private class AutoNameListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent e) {
			autoName = e.getStateChange() == ItemEvent.SELECTED;
			descNameField.setEnabled(!autoName);
			autoNameDesc();
		}

	}

	private class AutoSaveListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent e) {
			autoSave = e.getStateChange() == ItemEvent.SELECTED;
			saveDescButton.setEnabled(!autoSave);
		}

	}

}
