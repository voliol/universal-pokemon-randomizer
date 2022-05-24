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

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * A palette, containing multiple {@link Color}s.
 * <p>
 * This class has constructors and methods for converting from/to formats used
 * by the ROMs, and from images, but not for handling compression.
 */
public class Palette implements Cloneable {

	private final static int DEFAULT_PALETTE_SIZE = 16;

	private Color[] colors;

	/**
	 * Reads the palette of an image with indexed colors, returning a Palette object
	 * with a size determined by the image.
	 * 
	 * @param bim An image with indexed colors.
	 */
	public static Palette readImagePalette(BufferedImage bim) {
		return readImagePalette(bim, imagePaletteSize(bim));
	}

	/**
	 * Reads the palette of an image with indexed colors, returning a Palette object
	 * of set size.
	 * 
	 * @param bim  An image with indexed colors.
	 * @param size The number of colors in the palette. Truncates the factual
	 *             palette of the image if it is less than its length, or fills it
	 *             out with the default Color if it is more.
	 */
	public static Palette readImagePalette(BufferedImage bim, int size) {
		if (bim.getRaster().getNumBands() != 1) {
			throw new IllegalArgumentException(
					"Invalid input; image must have indexed colors (e.g. come from a .bmp file).");
		}

		Palette palette = new Palette(size);
		for (int i = 0; i < (Math.min(palette.size(), imagePaletteSize(bim))); i++) {
			int argb = bim.getColorModel().getRGB(i);
			palette.setColor(i, new Color(argb));
		}

		return palette;
	}

	private static int imagePaletteSize(BufferedImage bim) {
		int bitsPerColor = bim.getRaster().getSampleModel().getSampleSize(0);
		return 1 << bitsPerColor;
	}

	private static int[] bytesToARGBValues(byte[] bytes) {
		int[] RGBValues = new int[bytes.length / 2];
		for (int i = 0; i < RGBValues.length; i++) {
			int word = (bytes[i * 2] & 0xFF) + ((bytes[i * 2 + 1] & 0xFF) << 8);
			RGBValues[i] = Color.highColorWordToARGB(word);
		}
		return RGBValues;
	}

	public Palette() {
		this(DEFAULT_PALETTE_SIZE);
	}

	public Palette(int size) {
		this.colors = new Color[size];
		for (int i = 0; i < size; i++) {
			this.colors[i] = new Color();
		}
	}

	public Palette(int size, Color color) {
		this.colors = new Color[size];
		for (int i = 0; i < size; i++) {
			this.colors[i] = color;
		}
	}

	public Palette(Color[] colors) {
		this.colors = colors;
	}

	public Palette(int[] RGBValues) {
		this.colors = new Color[RGBValues.length];
		for (int i = 0; i < colors.length; i++) {
			this.colors[i] = new Color(RGBValues[i]);
		}
	}

	public Palette(byte[] bytes) {
		this(bytesToARGBValues(bytes));
	}

	public Color getColor(int i) {
		return colors[i];
	}

	public void setColor(int i, Color c) {
		colors[i] = c;
	}

	public byte[] toBytes() {
		byte[] bytes = new byte[colors.length * 2];
		for (int i = 0; i < colors.length; i++) {
			byte[] colorBytes = colors[i].toBytes();
			bytes[i * 2] = colorBytes[0];
			bytes[i * 2 + 1] = colorBytes[1];
		}
		return bytes;
	}

	public int[] toARGB() {
		int[] ARGB = new int[colors.length];
		for (int i = 0; i < colors.length; i++) {
			ARGB[i] = colors[i].toARGB();
		}
		return ARGB;
	}

	public int size() {
		return colors.length;
	}

	@Override
	public String toString() {
		return Arrays.toString(colors);
	}

	@Override
	public Palette clone() {
		Palette palette = new Palette(colors.length);
		for (int i = 0; i < colors.length; i++) {
			palette.setColor(i, colors[i].clone());
		}
		return palette;
	}

}
