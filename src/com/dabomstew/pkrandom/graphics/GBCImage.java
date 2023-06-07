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
 * A 2bpp image used by a Gen 1 or Gen 2 game.
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
        Palette fixed = DEFAULT_PALETTE.clone();
        for (int i = 0; i < colors.size(); i++) {
            Color color = new Color(colors.get(i));
            if (!color.equals(Color.WHITE) && !color.equals(Color.BLACK)) {
                fixed.setColor(i, color);
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

    private byte[] data;

    private boolean bitplanesPrepared;
    private BufferedImage bitplane1Image;
    private BufferedImage bitplane2Image;
    private byte[] bitplane1Data;
    private byte[] bitplane2Data;

    /**
     * Creates a GBCImage copy of the input {@link BufferedImage}.
     * This allows both the GBCImage methods, but also "fixes" its 2bpp color
     * indexing. I.e. the GBCImage uses an {@link IndexColorModel}
     * with four colors, sorted from lightest/white to darkest/black.
     *
     * @param bim The original BufferedImage.
     */
    public GBCImage(BufferedImage bim) {
        this(bim.getWidth() / TILE_SIZE, bim.getHeight() / TILE_SIZE,
                Palette.readImagePaletteFromPixels(bim));
        if (bim.getWidth() % TILE_SIZE != 0 || bim.getHeight() % TILE_SIZE != 0) {
            throw new IllegalArgumentException(bim + " has invalid dimensions " + bim.getWidth() + "x" +
                    bim.getHeight() + " pixels. Must be multiples of " + TILE_SIZE);
        }
        Graphics2D g = createGraphics();
        g.drawImage(bim, 0, 0, null);
    }

    public GBCImage(int widthInTiles, int heightInTiles, Palette palette) {
        super(widthInTiles * TILE_SIZE, heightInTiles * TILE_SIZE, BufferedImage.TYPE_BYTE_INDEXED,
                GFXFunctions.indexColorModelFromPalette(fixPalette(palette), BPP));
    }

    public GBCImage(int widthInTiles, int heightInTiles, Palette palette, byte[] data) {
        this(widthInTiles, heightInTiles, palette);

        for (int tile = 0; tile < data.length / TILE_SIZE / 2; tile++) {
            int tileX = tile % widthInTiles;
            int tileY = tile / widthInTiles;
            for (int yT = 0; yT < 8; yT++) {
                int strip0 = data[(tile * 8 + yT) * 2];
                int strip1 = data[(tile * 8 + yT) * 2 + 1];
                for (int xT = 0; xT < 8; xT++) {
                    int bit0 = (strip0 >>> (7 - xT)) & 1;
                    int bit1 = (strip1 >>> (7 - xT)) & 1;
                    int colorIndex = (bit1 << 1) + bit0;
                    setRGB(tileX * 8 + xT, tileY * 8 + yT, palette.getColor(colorIndex).toARGB());
                }
            }
        }
    }

    public int getWidthInTiles() {
        return getWidth() / 8;
    }

    public int getHeightInTiles() {
        return getHeight() / 8;
    }

    public byte[] toBytes() {
        if (data == null) {
            prepareBitplanes();
            data = new byte[bitplane1Data.length * 2];
            for (int i = 0; i < bitplane1Data.length; i++) {
                data[i * 2] = bitplane1Data[i];
                data[i * 2 + 1] = bitplane2Data[i];
            }
        }
        return this.data;
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
        bitplane1Data = GFXFunctions.readTiledImageData(bitplane1Image, 1);
        bitplane2Data = GFXFunctions.readTiledImageData(bitplane2Image, 1);

        bitplanesPrepared = true;
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
