package com.dabomstew.pkrandom;

/*----------------------------------------------------------------------------*/
/*--  GFXFunctions.java - functions relating to graphics rendering.         --*/
/*--                      Mainly used for rendering the sprites.            --*/
/*--                                                                        --*/
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
import java.util.LinkedList;
import java.util.Queue;

public class GFXFunctions {

	private static final int DEFAULT_TILE_WIDTH = 8;
	private static final int DEFAULT_TILE_HEIGHT = 8;

	public static BufferedImage drawTiledImage(byte[] data, int[] palette, int width, int height, int bpp) {
		return drawTiledImage(data, palette, 0, width, height, DEFAULT_TILE_WIDTH, DEFAULT_TILE_HEIGHT, bpp);
	}

	public static BufferedImage drawTiledImage(byte[] data, int[] palette, int offset, int width, int height, int bpp) {
		return drawTiledImage(data, palette, offset, width, height, DEFAULT_TILE_WIDTH, DEFAULT_TILE_HEIGHT, bpp);
	}

	public static BufferedImage drawTiledImage(byte[] data, int[] palette, int offset, int width, int height,
			int tileWidth, int tileHeight, int bpp) {
		if (bpp != 1 && bpp != 2 && bpp != 4 && bpp != 8) {
			throw new IllegalArgumentException("Bits per pixel must be a multiple of 2.");
		}
		int pixelsPerByte = 8 / bpp;
		if (width * height / pixelsPerByte + offset > data.length) {
			throw new IllegalArgumentException("Invalid input image.");
		}

		int bytesPerTile = tileWidth * tileHeight / pixelsPerByte;
		int numTiles = width * height / (tileWidth * tileHeight);
		int widthInTiles = width / tileWidth;

		BufferedImage bim = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		for (int tile = 0; tile < numTiles; tile++) {
			int tileX = tile % widthInTiles;
			int tileY = tile / widthInTiles;
			for (int yT = 0; yT < tileHeight; yT++) {
				for (int xT = 0; xT < tileWidth; xT++) {
					int value = data[tile * bytesPerTile + yT * tileWidth / pixelsPerByte + xT / pixelsPerByte + offset]
							& 0xFF;
					if (pixelsPerByte != 1) {
						value = (value >>> (xT % pixelsPerByte) * bpp) & ((1 << bpp) - 1);
					}
					bim.setRGB(tileX * tileWidth + xT, tileY * tileHeight + yT, palette[value]);
				}
			}
		}

		return bim;
	}

	/**
	 * Reads the data from an image read from a 4bpp .bmp file, returning it in the
	 * format used by Gen III-V games.
	 * <p>
	 * Allows for writing image files to Gen III-V games, by using the data as an
	 * argument for the appropriate writing method.
	 */
	// TODO: it makes sense for this to be handled by its own class(es).
	public static byte[] readTiledImageData(BufferedImage bim) {

		int bpp = 4;
		int tileWidth = DEFAULT_TILE_WIDTH;
		int tileHeight = DEFAULT_TILE_HEIGHT;

		if (bim.getRaster().getNumBands() != 1) {
			throw new IllegalArgumentException("Invalid input; image should come from a 4bpp .png file.");
		}

		if (bim.getRaster().getSampleModel().getSampleSize(0) != bpp) {
			throw new IllegalArgumentException("Invalid input; image should come from a 4bpp .png file.");
		}

		if (bim.getWidth() % tileWidth != 0 || bim.getHeight() % tileHeight != 0) {
			throw new IllegalArgumentException(
					"Invalid input; image must be dividable into " + tileWidth + "x" + tileHeight + " pixel tiles.");
		}

		byte[] data = new byte[bim.getWidth() * bim.getHeight() / 2];

		int numTiles = bim.getWidth() * bim.getHeight() / (tileWidth * tileHeight);
		int widthInTiles = bim.getWidth() / tileWidth;

		int next = 0;
		for (int tileNum = 0; tileNum < numTiles; tileNum++) {
			int tileX = tileNum % widthInTiles;
			int tileY = tileNum / widthInTiles;

			for (int yT = 0; yT < tileHeight; yT++) {
				for (int xT = 0; xT < tileWidth; xT += 2) {

					int low = bim.getData().getSample(tileX * tileWidth + xT, tileY * tileHeight + yT, 0);
					int high = bim.getData().getSample(tileX * tileWidth + xT + 1, tileY * tileHeight + yT, 0);
					data[next] = (byte) (low + (high << 4));
					next++;

				}
			}

		}
		return data;
	}

	@Deprecated
	public static int conv16BitColorToARGB(int palValue) {
		System.out.println(
				"GFXFunctions.conv16BitColorToARGB(int) is deprecated. Use graphics.Palette.toARGB() instead, or graphics.Color.highColorWordToARGB() in cases where you don't load a full palette.");
		int red = (int) ((palValue & 0x1F) * 8.25);
		int green = (int) (((palValue & 0x3E0) >> 5) * 8.25);
		int blue = (int) (((palValue & 0x7C00) >> 10) * 8.25);
		return 0xFF000000 | (red << 16) | (green << 8) | blue;
	}

	public static void pseudoTransparency(BufferedImage img, int transColor) {
		int width = img.getWidth();
		int height = img.getHeight();
		Queue<Integer> visitPixels = new LinkedList<Integer>();
		boolean[][] queued = new boolean[width][height];

		for (int x = 0; x < width; x++) {
			queuePixel(x, 0, width, height, visitPixels, queued);
			queuePixel(x, height - 1, width, height, visitPixels, queued);
		}

		for (int y = 0; y < height; y++) {
			queuePixel(0, y, width, height, visitPixels, queued);
			queuePixel(width - 1, y, width, height, visitPixels, queued);
		}

		while (!visitPixels.isEmpty()) {
			int nextPixel = visitPixels.poll();
			int x = nextPixel % width;
			int y = nextPixel / width;
			if (img.getRGB(x, y) == transColor) {
				img.setRGB(x, y, 0);
				queuePixel(x - 1, y, width, height, visitPixels, queued);
				queuePixel(x + 1, y, width, height, visitPixels, queued);
				queuePixel(x, y - 1, width, height, visitPixels, queued);
				queuePixel(x, y + 1, width, height, visitPixels, queued);
			}
		}
	}

	private static void queuePixel(int x, int y, int width, int height, Queue<Integer> queue, boolean[][] queued) {
		if (x >= 0 && x < width && y >= 0 && y < height && !queued[x][y]) {
			queue.add((y) * width + (x));
			queued[x][y] = true;
		}
	}

}