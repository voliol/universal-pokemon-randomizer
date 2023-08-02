package com.dabomstew.pkrandom.graphics;

import com.dabomstew.pkrandom.GFXFunctions;
import com.dabomstew.pkrandom.graphics.palettes.Color;
import com.dabomstew.pkrandom.graphics.palettes.Palette;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A 2bpp image used by a Gen 1 or Gen 2 game. The GBCImage itself is a {@link BufferedImage} with an
 * {@link IndexColorModel} of 4 colors, but can be converted from/to a byte array format used by the games.
 * <a href=https://www.huderlem.com/demos/gameboy2bpp.html>That format is explained here</a>.
 */
public class GBCImage extends BufferedImage {

    private static final int TILE_SIZE = 8;
    private static final int BPP = 2;
    private static final int PALETTE_SIZE = 4;

    public static final Palette DEFAULT_PALETTE = new Palette(new Color[]{
            Color.WHITE, new Color(0xFFAAAAAA), new Color(0xFF666666), Color.BLACK});

    /**
     * "Fixes" a palette, filling it out to have 4 colors if there are less,
     * and reordering them from brightest to darkest. True white (=FFFFFF) is always put at the start of the palette,
     * and true black (=000000) is always put at the end.
     */
    private static Palette fixPalette(Palette palette) {
        if (palette.size() > 4) {
            throw new IllegalArgumentException("palette.size()=" + palette.size() + " exceeds max palette size "
                    + PALETTE_SIZE + ".");
        }
        List<Integer> colors = new ArrayList<>(Arrays.stream(palette.toARGB()).boxed().toList());
        colors.sort((c1, c2) -> compareColors(new Color(c1), new Color(c2)));
        Palette fixed = new Palette(DEFAULT_PALETTE);
        for (int i = 0; i < colors.size(); i++) {
            Color color = new Color(colors.get(i));
            if (!color.equals(Color.WHITE) && !color.equals(Color.BLACK)) {
                fixed.set(i, color);
            }
        }

        return fixed;
    }

    private static int compareColors(Color a, Color b) {
        double[] aHSV = a.toHSV();
        double[] bHSV = b.toHSV();
        int compare = Double.compare(bHSV[2], aHSV[2]);
        if (compare == 0) {
            compare = Double.compare(aHSV[1], bHSV[1]);
            if (compare == 0) {
                compare = Double.compare(aHSV[0], bHSV[0]);
            }
        }
        return compare;
    }

    private final boolean columnMode;
    private final int[] colors;

    private boolean bitplanesPrepared;
    private BufferedImage bitplane1Image;
    private BufferedImage bitplane2Image;

    /**
     * Creates a GBCImage copy of the input {@link BufferedImage}.
     * This allows both the GBCImage methods, but also "fixes" its 2bpp color
     * indexing. I.e. the GBCImage uses an {@link IndexColorModel}
     * with four colors, sorted from lightest/white to darkest/black.
     * <br><br>
     * Assumes the tiles to be row-by-row, see {@link #GBCImage(BufferedImage, boolean)}.
     *
     * @param bim The original BufferedImage.
     */
    public GBCImage(BufferedImage bim) {
        this(bim, false);
    }

    /**
     * Creates a GBCImage copy of the input {@link BufferedImage}.
     * This allows both the GBCImage methods, but also "fixes" its 2bpp color
     * indexing. I.e. the GBCImage uses an {@link IndexColorModel}
     * with four colors, sorted from lightest/white to darkest/black.
     *
     * @param bim        The original BufferedImage.
     * @param columnMode If true, the output data from {@link #toBytes()} will be column-by-column, instead of row-by-row. Mirrors
     *                   <a href=https://rgbds.gbdev.io/docs/v0.6.1/rgbgfx.1/#Z>"rgbgfx -Z"</a>.
     */
    public GBCImage(BufferedImage bim, boolean columnMode) {
        this(bim.getWidth() / TILE_SIZE, bim.getHeight() / TILE_SIZE,
                fixPalette(Palette.readImagePaletteFromPixels(bim)), columnMode);
        if (bim.getWidth() % TILE_SIZE != 0 || bim.getHeight() % TILE_SIZE != 0) {
            throw new IllegalArgumentException(bim + " has invalid dimensions " + bim.getWidth() + "x" +
                    bim.getHeight() + " pixels. Must be multiples of " + TILE_SIZE);
        }
        Graphics2D g = createGraphics();
        g.drawImage(bim, 0, 0, null);
    }

    public GBCImage(int widthInTiles, int heightInTiles, Palette palette, boolean columnMode) {
        super(widthInTiles * TILE_SIZE, heightInTiles * TILE_SIZE, BufferedImage.TYPE_BYTE_INDEXED,
                GFXFunctions.indexColorModelFromPalette(palette, BPP));
        this.columnMode = columnMode;
        this.colors = initColors();
    }

    private int[] initColors() {
        int[] colors = new int[4];
        IndexColorModel colorModel = (IndexColorModel) getColorModel();
        colorModel.getRGBs(colors);
        return colors;
    }

    public GBCImage(int widthInTiles, int heightInTiles, Palette palette, byte[] data) {
        this(widthInTiles, heightInTiles, palette, data, false);
    }

    /**
     * @param columnMode If true, the data will be column-by-column, instead of row-by-row. This affects the data reading,
     *                   as well as the output from {@link #toBytes()}.
     *                   Mirrors <a href=https://rgbds.gbdev.io/docs/v0.6.1/rgbgfx.1/#Z>"rgbgfx -Z"</a>.
     */
    public GBCImage(int widthInTiles, int heightInTiles, Palette palette, byte[] data, boolean columnMode) {
        this(widthInTiles, heightInTiles, palette, columnMode);
        drawTileData(data);
    }

    private void drawTileData(byte[] data) {
        int dataNumTiles = data.length / TILE_SIZE / BPP;
        int imageNumTiles = getWidthInTiles() * getHeightInTiles();
        for (int tile = 0; tile < Math.min(dataNumTiles, imageNumTiles); tile++) {
            int tileX = columnMode ? tile / getWidthInTiles() : tile % getWidthInTiles();
            int tileY = columnMode ? tile % getWidthInTiles() : tile / getWidthInTiles();
            for (int yT = 0; yT < 8; yT++) {
                int lowByte = data[(tile * 8 + yT) * 2];
                int highByte = data[(tile * 8 + yT) * 2 + 1];
                for (int xT = 0; xT < 8; xT++) {
                    int low = (lowByte >>> (7 - xT)) & 1;
                    int high = (highByte >>> (7 - xT)) & 1;
                    int colorIndex = (high << 1) + low;
                    setColor(tileX * 8 + xT, tileY * 8 + yT, colorIndex);
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

        for (int tile = 0; tile < numTiles; tile++) {
            int tileX = columnMode ? tile / getHeightInTiles() : tile % getWidthInTiles();
            int tileY = columnMode ? tile % getHeightInTiles() : tile / getWidthInTiles();
            for (int yT = 0; yT < 8; yT++) {
                int lowByte = 0;
                int highByte = 0;
                for (int xT = 0; xT < 8; xT++) {
                    int sample = getData().getSample(tileX * 8 + xT, tileY * 8 + yT, 0);
                    lowByte |= (sample & 1) << (7 - xT);
                    highByte |= ((sample >>> 1) & 1) << (7 - xT);
                }
                data[(tile * 8 + yT) * 2] = (byte) lowByte;
                data[(tile * 8 + yT) * 2 + 1] = (byte) highByte;
            }
        }

        return data;
    }

    public BufferedImage getBitplane1Image() {
        prepareBitplanes();
        return bitplane1Image;
    }

    public BufferedImage getBitplane2Image() {
        prepareBitplanes();
        return bitplane2Image;
    }

    private void prepareBitplanes() {
        if (bitplanesPrepared) {
            return;
        }

        bitplane1Image = new BufferedImage(getWidth(), getHeight(),
                BufferedImage.TYPE_BYTE_BINARY);
        bitplane2Image = new BufferedImage(getWidth(), getHeight(),
                BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g1 = bitplane1Image.createGraphics();
        g1.setColor(java.awt.Color.WHITE);
        g1.fillRect(0, 0, getWidth(), getHeight());
        Graphics2D g2 = bitplane2Image.createGraphics();
        g2.setColor(java.awt.Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int sample = getData().getSample(x, y, 0);
                if (sample % 2 == 1) {
                    bitplane1Image.setRGB(x, y, 0xff000000);
                }
                if (sample >= 2) {
                    bitplane2Image.setRGB(x, y, 0xff000000);
                }
            }
        }

        bitplanesPrepared = true;
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
    public GBCImage getSubimageFromTileRange(int from, int to) {
        GBCImage subimage = new GBCImage(to - from, 1, getPalette(), false);
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
    public GBCImage getSubimageFromTileRect(int x, int y, int w, int h) {
        BufferedImage subimage = getSubimage(x * TILE_SIZE, y * TILE_SIZE, w * TILE_SIZE, h * TILE_SIZE);
        return new GBCImage(subimage, columnMode);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof GBCImage otherImage) {
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
