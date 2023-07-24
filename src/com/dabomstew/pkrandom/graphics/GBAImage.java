package com.dabomstew.pkrandom.graphics;

import com.dabomstew.pkrandom.GFXFunctions;
import com.dabomstew.pkrandom.graphics.palettes.Palette;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.util.Arrays;

/**
 * An image for GBA games with 16-color palettes. For now just a copy of GBCImage with that difference.
 * // TODO: class structure
 */
public class GBAImage extends BufferedImage {

    private static final int TILE_SIZE = 8;
    private static final int BPP = 4;

    private final boolean columnMode;
    private final int[] colors;

    public GBAImage(BufferedImage bim) {
        this(bim, false);
    }

    public GBAImage(BufferedImage bim, boolean columnMode) {
        this(bim.getWidth() / TILE_SIZE, bim.getHeight() / TILE_SIZE,
                Palette.readImagePalette(bim, 16), columnMode);
        if (bim.getWidth() % TILE_SIZE != 0 || bim.getHeight() % TILE_SIZE != 0) {
            throw new IllegalArgumentException(bim + " has invalid dimensions " + bim.getWidth() + "x" +
                    bim.getHeight() + " pixels. Must be multiples of " + TILE_SIZE);
        }
        Graphics2D g = createGraphics();
        g.drawImage(bim, 0, 0, null); // TODO: this breaks palette tricks, fix
    }

    public GBAImage(int widthInTiles, int heightInTiles, Palette palette, boolean columnMode) {
        super(widthInTiles * TILE_SIZE, heightInTiles * TILE_SIZE, BufferedImage.TYPE_BYTE_INDEXED,
                GFXFunctions.indexColorModelFromPalette(palette, BPP));
        this.columnMode = columnMode;
        this.colors = initColors();
    }

    private int[] initColors() {
        int[] colors = new int[16];
        IndexColorModel colorModel = (IndexColorModel) getColorModel();
        colorModel.getRGBs(colors);
        return colors;
    }

    public GBAImage(int widthInTiles, int heightInTiles, Palette palette, byte[] data) {
        this(widthInTiles, heightInTiles, palette, data, false);
    }

    /**
     * @param columnMode If true, the data will be column-by-column, instead of row-by-row. This affects the data reading,
     *                   as well as the output from {@link #toBytes()}.
     *                   Mirrors <a href=https://rgbds.gbdev.io/docs/v0.6.1/rgbgfx.1/#Z>"rgbgfx -Z"</a>.
     */
    public GBAImage(int widthInTiles, int heightInTiles, Palette palette, byte[] data, boolean columnMode) {
        this(widthInTiles, heightInTiles, palette, columnMode);
        drawTileData(data);
    }

    private void drawTileData(byte[] data) {
        int dataNumTiles = data.length / TILE_SIZE / BPP;
        int imageNumTiles = getWidthInTiles() * getHeightInTiles();
        int next = 0;
        for (int tile = 0; tile < Math.min(dataNumTiles, imageNumTiles); tile++) {
            int tileX = columnMode ? tile / getHeightInTiles() : tile % getWidthInTiles();
            int tileY = columnMode ? tile % getHeightInTiles() : tile / getWidthInTiles();
            for (int yT = 0; yT < TILE_SIZE; yT++) {
                for (int xT = 0; xT < TILE_SIZE; xT += 2) {
                    int pixel1 = data[next] & 0xF;
                    int pixel2 = (data[next] >>> BPP) & 0xF;
                    setColor(tileX * TILE_SIZE + xT, tileY * TILE_SIZE + yT, pixel1);
                    setColor(tileX * TILE_SIZE + xT + 1, tileY * TILE_SIZE + yT, pixel2);
                    next++;
                }
            }
        }
    }

    public void setColor(int x, int y, int colorIndex) {
        setRGB(x, y, colors[colorIndex]);
    }

    public int getWidthInTiles() {
        return getWidth() / 8;
    }

    public int getHeightInTiles() {
        return getHeight() / 8;
    }

    public byte[] toBytes() {
        byte[] data = new byte[getWidthInTiles() * getHeightInTiles() * TILE_SIZE * BPP];
        int numTiles = getWidthInTiles() * getHeightInTiles();

        int next = 0;
        for (int tile = 0; tile < numTiles; tile++) {
            int tileX = columnMode ? tile / getHeightInTiles() : tile % getWidthInTiles();
            int tileY = columnMode ? tile % getHeightInTiles() : tile / getWidthInTiles();

            for (int yT = 0; yT < TILE_SIZE; yT++) {
                for (int xT = 0; xT < TILE_SIZE; xT += 2) {
                    int pixel1 = getData().getSample(tileX * TILE_SIZE + xT, tileY * TILE_SIZE + yT, 0);
                    int pixel2 = getData().getSample(tileX * TILE_SIZE + xT + 1, tileY * TILE_SIZE + yT, 0);
                    data[next] = (byte) ((pixel2 << 4) + pixel1);
                    next++;
                }
            }
        }

        return data;
    }

    public Palette getPalette() {
        return new Palette(colors);
    }

    /**
     * Returns a subimage consisting of only the tiles within a given range.
     *
     * @param from the initial index of the range, inclusive
     * @param to   the final index of the range, exclusive
     */
    public GBAImage getSubimageFromTileRange(int from, int to) {
        GBAImage subimage = new GBAImage(to - from, 1, getPalette(), false);
        Graphics2D g = subimage.createGraphics();
        for (int tile = from; tile < to; tile++) {
            int tileX = columnMode ? tile / getWidthInTiles() : tile % getWidthInTiles();
            int tileY = columnMode ? tile % getWidthInTiles() : tile / getWidthInTiles();
            g.drawImage(this, (tile - from) * TILE_SIZE, 0, (tile - from + 1) * TILE_SIZE, TILE_SIZE,
                    tileX * TILE_SIZE, tileY * TILE_SIZE, (tileX + 1) * TILE_SIZE,
                    (tileY + 1) * TILE_SIZE, null);
        }
        return subimage;
    }

    /**
     * Returns a subimage defined by a rectangular region - in tiles. Similar to
     * {@link BufferedImage#getSubimage(int, int, int, int)}.<br>
     * The columnmode of the subimage is the same as the source image.
     *
     * @param x the X coordinate of the upper-left corner tile of the specified rectangular region
     * @param y the Y coordinate of the upper-left corner tile of the specified rectangular region
     * @param w the width in tiles
     * @param h the height in tiles
     */
    public GBAImage getSubimageFromTileRect(int x, int y, int w, int h) {
        BufferedImage subimage = getSubimage(x * TILE_SIZE, y * TILE_SIZE, w * TILE_SIZE, h * TILE_SIZE);
        return new GBAImage(subimage, columnMode);
    }

    /**
     * Returns a subimage defined as a "frame", a rectangular region
     * given by its index and dimensions. Throws an {@link IllegalArgumentException}
     * if the dimensions of the BufferedImage are not divisible by the frame dimensions.
     * <br><br>
     * This method allows for easier handling of "sheets" of images, since the
     * return is the same regardless whether the frames are laid out horizontally,
     * vertically, or even in a 2D grid. Frames are read row-for-row, left-to-right,
     * top to bottom. I.e.: <br>
     * -----<br>
     * |0|1|<br>
     * |2|3|<br>
     * -----<br>
     *
     * @param i the index of the frame
     * @param w the width of a frame in tiles
     * @param h the height of a frame in tiles
     */
    public GBAImage getFrameSubimage(int i, int w, int h) {
        if (getWidthInTiles() % w != 0 || getHeightInTiles() % h != 0) {
            throw new IllegalArgumentException("Image cannot be split into frames that are " + w + "x" + h + " tiles.");
        }
        int x = (i * w) % getWidthInTiles();
        int y = ((i * w) / getWidthInTiles()) * h;
        return getSubimageFromTileRect(x, y, w, h);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof GBAImage otherImage) {
            return Arrays.equals(getRasterData(), otherImage.getRasterData());
        }
        return false;
    }

    private int[] getRasterData() {
        Raster raster = getRaster();
        return raster.getPixels(0, 0, raster.getWidth(), raster.getHeight(),
                new int[raster.getWidth() * raster.getHeight()]);
    }

}
