package com.dabomstew.pkrandom.graphics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

/**
 * An RGB color, usually to be contained by a {@link Palette}, and methods for
 * converting to/from other formats. These formats include 32-bit ARGB, and high
 * color words (16 bits, little endian) as used by the ROMs.
 */
public class Color implements Cloneable {

	private static final Color DEFAULT_COLOR = new Color(255, 255, 255);

	private static final String HEX_REGEX = "[0-9abcdefABCDEF]{6}";

	private static Color colorFromString(String string) {
		Matcher hexMatcher = Pattern.compile(HEX_REGEX).matcher(string);
		if (hexMatcher.find()) {
			String hex = hexMatcher.group();
			return new Color(Integer.parseInt(hex, 16));
		}
		System.out.println(string);

		// Yes, this is kind of ugly, but Matcher doesn't allow a straight-forward
		// way of counting the matches...
		Matcher intMatcher = Pattern.compile("[0-9]+").matcher(string);
		int r = 0, g = 0, b = 0;
		if (intMatcher.find())
			r = Integer.parseInt(intMatcher.group());
		if (intMatcher.find())
			g = Integer.parseInt(intMatcher.group());
		if (intMatcher.find()) {
			b = Integer.parseInt(intMatcher.group());
			return new Color(r, g, b);
		}

		throw new IllegalArgumentException("No color value found in \"" + string + "\".");
	}

	public static int highColorWordToARGB(int word) {
		int red = (int) ((word & 0x1F) * 8.25);
		int green = (int) (((word & 0x3E0) >> 5) * 8.25);
		int blue = (int) (((word & 0x7C00) >> 10) * 8.25);
		return 0xFF000000 | (red << 16) | (green << 8) | blue;
	}

	private int r, g, b;

	public Color() {
		this(DEFAULT_COLOR.r, DEFAULT_COLOR.b, DEFAULT_COLOR.g);
	}

	/**
	 * The primary Color constructor, taking an int each for red, green, and blue.
	 * All other constructors should go through this somehow, as it is the only one that
	 * does bounds-checking.
	 * 
	 * @param r red value (0-255)
	 * @param g green value (0-255)
	 * @param b blue value (0-255)
	 */
	public Color(int r, int g, int b) {
		if (r < 0 || r > 255) {
			throw new IllegalArgumentException("red value out of bounds (0-255)");
		}
		if (g < 0 || g > 255) {
			throw new IllegalArgumentException("green value out of bounds (0-255)");
		}
		if (b < 0 || b > 255) {
			throw new IllegalArgumentException("blue value out of bounds (0-255)");
		}
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public Color(int hex) {
		this((hex & 0xFF0000) >> 16, (hex & 0xFF00) >> 8, (hex & 0xFF));
	}

	public Color(String string) {
		Color color = colorFromString(string);
		this.r = color.r;
		this.g = color.g;
		this.b = color.b;
	}

	@Override
	public Color clone() {
		return new Color(r, g, b);
	}

	@Override
	public String toString() {
		return "(" + r + ", " + g + ", " + b + ")";
	}

	public byte[] toBytes() {
		int red = Math.floorDiv(r, 8);
		int green = Math.floorDiv(g, 8);
		int blue = Math.floorDiv(b, 8);
		int bytesSum = blue * 1024 + red + green * 32;
		return new byte[] { (byte) bytesSum, (byte) Math.floorDiv(bytesSum, 256) };
	}

	public int toARGB() {
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	public int[] toInts() {
		return new int[] { r, g, b };
	}

	public int getComp(int i) {
		switch (i) {
		case 0:
			return r;
		case 1:
			return g;
		case 2:
			return b;
		default:
			throw new IndexOutOfBoundsException(i + " out of bounds for RGB color (0=r, 1=g, 2=b).");
		}
	}

	public void setComp(int i, int value) {
		switch (i) {
		case 0:
			r = value;
			break;
		case 1:
			g = value;
			break;
		case 2:
			b = value;
			break;
		default:
			throw new IndexOutOfBoundsException(i + " out of bounds for RGB color (0=r, 1=g, 2=b).");
		}
	}

	@FunctionalInterface
	public static interface ColorChange {
		public int change(Color orig, int i);
	}

	public void setComps(ColorChange cc) {
		for (int i = 0; i < 3; i++) {
			int value = cc.change(this, i);
			value = Math.max(value, 0);
			value = Math.min(value, 255);
			this.setComp(i, value);
		}
	}

}
