package compressors;

// based on pret/pokered/blob/master/tools/pkmncompress.c

import com.dabomstew.pkrandom.GFXFunctions;
import com.dabomstew.pkrandom.graphics.GBCImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Gen1Cmp {

    private static final String IN_ADRESS = "compresstest/in";
    private static final String OUT_ADRESS = "compresstest/out";
    private static final String[] TEST_FILE_NAMES = new String[]{"test", "testbig", "testblack", "testrect", "testamogus", "testab", "abra", "aerodactyl", "alakazam", "arcanine",
            "articuno", "beedrill", "bellsprout", "blastoise", "bulbasaur", "butterfree", "caterpie", "chansey",
            "charizard", "charmander", "charmeleon", "clefable", "clefairy", "cloyster", "cubone", "diglett", "ditto",
            "dodrio", "doduo", "dragonair", "dragonite", "dratini", "drowzee", "dugtrio"};

    private static final int[][] tested = new int[3][2];
    private static final int[][] succeeded = new int[3][2];
    private static final int[][] failed = new int[3][2];
    private static final int[][] erred = new int[3][2];

    public static void main(String[] args) {

        System.out.println("starting test of gen 1 compression");
        for (String name : TEST_FILE_NAMES) {
            testImage(name);
        }
        System.out.println("Tested: " + Arrays.deepToString(tested));
        System.out.println("Succed: " + Arrays.deepToString(succeeded));
        System.out.println("Failed: " + Arrays.deepToString(failed));
        System.out.println("Errord: " + Arrays.deepToString(erred));

    }

    private static void testImage(String name) {
        try {
            System.out.println(name);
            BufferedImage bim = null;
            try {
                bim = ImageIO.read(new File(IN_ADRESS + "/" + name + ".png"));
            } catch (IOException ignored) {
            }

            writeBitplaneImages(bim, name);

            for (int mode = 1; mode <= 3; mode++) {
                for (int order = 0; order <= 1; order++) {
                    try {

                        Gen1Cmp compressor = new Gen1Cmp(new GBCImage(bim));
                        byte[] compressed = compressor.compressUsingModeAndOrder(mode, order);
                        tested[mode - 1][order]++;

                        byte[] rom = Arrays.copyOf(compressed, 0x100000);
                        Gen1Decmp sprite = new Gen1Decmp(rom, 0);
                        sprite.decompress();
                        sprite.transpose();
                        byte[] data = sprite.getData();

                        System.out.println("w: " + sprite.getWidth() + ", h: " + sprite.getHeight());

                        int[] convPalette = new int[]{0xFFFFFFFF, 0xFFAAAAAA, 0xFF666666, 0xFF000000};
                        BufferedImage bim2 = GFXFunctions.drawTiledImage(data, convPalette, sprite.getWidth(), sprite.getHeight(),
                                2);
                        try {
                            ImageIO.write(bim2, "png", new File(OUT_ADRESS + "/" + name + "_m" + mode + "o" + order + ".png"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (Arrays.equals(data, new GBCImage(bim).getData())) {
                            succeeded[mode - 1][order]++;
                        } else {
                            failed[mode - 1][order]++;
                        }

                    } catch (Exception e) {
                        erred[mode - 1][order]++;
                        e.printStackTrace();
                    }
                }
            }
        } catch (
                Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeBitplaneImages(BufferedImage bim, String name) {
        GBCImage image = new GBCImage(bim);
        try {
            ImageIO.write(image.getBitplane1Image(), "png", new File(OUT_ADRESS + "/" + name + "_bp1.png"));
            ImageIO.write(image.getBitplane2Image(), "png", new File(OUT_ADRESS + "/" + name + "_bp2.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final byte[] bitplane1;
    private final byte[] bitplane2;
    private final int widthInTiles;
    private final int heightInTiles;

    private byte[] bp1;
    private byte[] bp2;

    public static byte[] compress(GBCImage image) {
        return new Gen1Cmp(image).compressInner();
    }

    public static byte[] compress(byte[] bitplane1, byte[] bitplane2, int widthInTiles, int heightInTiles) {
        return new Gen1Cmp(bitplane1, bitplane2, widthInTiles, heightInTiles).compressInner();
    }

    private Gen1Cmp(GBCImage image) {
        this(image.getBitplane1Data(), image.getBitplane2Data(), image.getWidthInTiles(), image.getHeightInTiles());
    }

    private Gen1Cmp(byte[] bitplane1, byte[] bitplane2, int widthInTiles, int heightInTiles) {
        this.bitplane1 = bitplane1;
        this.bitplane2 = bitplane2;
        this.widthInTiles = widthInTiles;
        this.heightInTiles = heightInTiles;
    }

    private byte[] compressInner() {
        byte[] shortest = null;
        for (int mode = 1; mode <= 3; mode++) {
            for (int order = 0; order <= 1; order++) {
                byte[] compressed2 = compressUsingModeAndOrder(mode, order);
                if (shortest == null || compressed2.length < shortest.length) {
                    shortest = compressed2;
                }
            }
        }
        return shortest;
    }

    private byte[] compressUsingModeAndOrder(int mode, int order) {

        prepareBitplanes(mode, order);

        BitWriteStream bws = new BitWriteStream();

        writeImageDimensions(bws);
        bws.writeBit(order);
        compressAndWriteBitplane(bp1, bws);
        writeMode(mode, bws);
        compressAndWriteBitplane(bp2, bws);

        return bws.toByteArray();
    }

    private void prepareBitplanes(int mode, int order) {
        bp1 = Arrays.copyOf(order == 0 ? bitplane1 : bitplane2, bitplane1.length);
        bp2 = Arrays.copyOf(order == 0 ? bitplane2 : bitplane1, bitplane1.length);

        if (mode != 0) {
            bp2 = xor(bp1, bp2);
        }

        bp1 = deltaEncode(bp1);
        if (mode != 2) {
            bp2 = deltaEncode(bp2);
        }
    }

    private void compressAndWriteBitplane(byte[] bitplane, BitWriteStream bws) {
        int[] bitPairs = bitPlaneToPairs(bitplane);

        int packetType = bitPairs[0] == 0 ? 0 : 1;
        bws.writeBit(packetType); // 0 for RLE, 1 for data

       // System.out.println(bws);
        int i = 0;
        while (i < bitPairs.length) {
            if (packetType == 0) {
                i = writeRLEPacket(bitPairs, i, bws);
            } else {
                i = writeDataPacket(bitPairs, i, bws);
            }
          //  System.out.println(bws);
            packetType ^= 1;
        }
    }

    private int writeRLEPacket(int[] bitPairs, int i, BitWriteStream bws) {
        int length = 0;
        while (i < bitPairs.length && bitPairs[i] == 0b00) {
            length++;
            i++;
        }

        int bitCount = getBitCount(length);

        writeBitCount(bws, bitCount);
        writeValue(bws, length, bitCount);

        return i;
    }

    private int getBitCount(int length) {
        int bitCount = 1;
        while (length > ((1 << bitCount) - 2)) {
            bitCount++;
        }
        bitCount--; // ignore leading '1'
        return bitCount;
    }

    private void writeBitCount(BitWriteStream bws, int bitCount) {
        for (int j = 0; j < bitCount - 1; j++) {
            bws.writeBit(1);
        }
        bws.writeBit(0);
    }

    private void writeValue(BitWriteStream bws, int length, int bitCount) {
        for (int j = bitCount; j > 0; j--) {
            bws.writeBit(((length + 1) >> (j - 1)) & 1);
        }
    }

    private int writeDataPacket(int[] bitPairs, int i, BitWriteStream bws) {
        do {
            bws.writeBitPair(bitPairs[i]);
            i++;
        } while (i < bitPairs.length - 1 && bitPairs[i] != 0b00);
        if (i != bitPairs.length - 1) {
            bws.writeBitPair(0);
        }
        return i;
    }

    /**
     * Converts a bitplane to pairs of bits. The bitplane represents an image with 8x8 tiles, top-to-bottom,
     * left-to-right. Essentially, 8-bit wide columns. The pairs of bits are similar, essentially 2-bit wide columns.
     */
    private int[] bitPlaneToPairs(byte[] bitplane) {
        int[] pairs = new int[bitplane.length * 4];
        int i = 0;
        for (int tileX = 0; tileX < widthInTiles; tileX++) {
            for (int pairX = 0; pairX < 4; pairX++) {
                for (int y = 0; y < heightInTiles * 8; y++) {

                    byte fromByte = bitplane[tileX * heightInTiles * 8 + y];
                    pairs[i] = 0b11 & (fromByte >>> ((3 - pairX) * 2)); // TODO: fix this occasionally losing bits
                    i++;

                }
            }
        }
        return pairs;
    }

    private void writeImageDimensions(BitWriteStream bws) {
        int high = (widthInTiles & 0b1111) << 4;
        int low = heightInTiles & 0b1111;
        bws.writeByte((byte) (high + low));
    }

    private void writeMode(int mode, BitWriteStream bws) {
        if (mode == 1) {
            bws.writeBit(0);
        } else {
            bws.writeBit(1);
            bws.writeBit(mode & 1);
        }
    }

    // "[Bitplanes] are delta [en]coded in horizontal lines spanning from the right side to the left
    // side of the [bitplane], going from top to bottom. Each row is [en]coded separately - the state of the
    // system is reset to 0 at the start of each row." -- https://youtu.be/aF1Yw_wu2cM?t=1519
    private byte[] deltaEncode(byte[] bitplane) {
        byte[] encoded = new byte[bitplane.length];
        for (int row = 0; row < heightInTiles * 8; row++) {

            byte[] rowBytes = new byte[bitplane.length / (heightInTiles * 8)];
            for (int x = 0; x < widthInTiles; x++) {
                rowBytes[x] = bitplane[x * heightInTiles + row];
            }

            rowBytes = deltaEncodeRow(rowBytes);

            for (int x = 0; x < widthInTiles; x++) {
                encoded[x * heightInTiles + row] = rowBytes[x];
            }
        }
        return encoded;
    }

    private byte[] deltaEncodeRow(byte[] row) {
        BitReadStream brs = new BitReadStream(row);
        BitWriteStream bws = new BitWriteStream();
        int last = 0;
        while (brs.hasNext()) {
            int current = brs.readBit();
            bws.writeBit(current ^ last);
            last = current;
        }
        return bws.toByteArray();
    }

    private byte[] xor(byte[] a, byte[] b) {
        byte[] xored = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            xored[i] = (byte) (a[i] ^ b[i]);
        }
        return xored;
    }

    private static class BitWriteStream {

        List<Byte> bytes = new ArrayList<>();
        int currentBit = 0;
        byte currentByte = 0;

        public void writeBit(int bit) {
            if (currentBit == 8 || bytes.isEmpty()) {
                currentBit = 0;
                currentByte = 0;
                bytes.add(currentByte);
            }
            currentByte += (bit << (7 - currentBit));
            bytes.set(bytes.size() - 1, currentByte);
            currentBit++;
        }

        public void writeBitPair(int bitPair) {
            writeBit((bitPair >> 1) & 1);
            writeBit(bitPair & 1);
        }

        public void writeByte(byte b) {
            for (int i = 7; i >= 0; i--) {
                writeBit((b >> i) & 1);
            }
        }

        public byte[] toByteArray() {
            byte[] byteArray = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); i++) {
                byteArray[i] = bytes.get(i);
            }
            return byteArray;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.size() - 1; i++) {
                for (int j = 7; j >= 0; j--) {
                    byte b = bytes.get(i);
                    sb.append((b >> j) & 1);
                }
            }
            for (int j = 7; j > 7 - currentBit; j--) {
                sb.append((currentByte >> j) & 1);
            }

            return sb.toString();
        }

    }

    private static class BitReadStream {

        final byte[] bytes;
        int currentBit = 0;
        int currentByte = 0;

        public BitReadStream(byte[] bytes) {
            this.bytes = bytes;
        }

        public int readBit() {
            byte b = bytes[currentByte];
            int bit = b >> (7 - currentBit) & 1;
            currentBit++;
            if (currentBit == 8) {
                currentBit = 0;
                currentByte++;
            }
            return bit;
        }

        public boolean hasNext() {
            return currentByte < bytes.length;
        }
    }

}
