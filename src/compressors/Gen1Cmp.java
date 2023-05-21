package compressors;

// based on pret/pokered/blob/master/tools/pkmncompress.c

import com.dabomstew.pkrandom.graphics.GBCImage;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Gen1Cmp {

    private static final int MAX_DIMENSION = 15;
    private static final int BLACK = 0xFF000000;

    private final GBCImage image;

    private final int width;
    private final int height;
    private int[][] bp1;
    private int[][] bp2;

    public static byte[] compress(GBCImage image) {
        return new Gen1Cmp(image).compressInner();
    }

    public Gen1Cmp(GBCImage image) {
        this.image = image;
        this.width = image.getImage().getWidth();
        this.height = image.getImage().getHeight();
        if (image.getWidthInTiles() > MAX_DIMENSION || image.getHeightInTiles() > MAX_DIMENSION) {
            throw new IllegalArgumentException("Image dimensions (in tiles) of " + image.getWidthInTiles() + "x" + image.getHeightInTiles() +
                    " exceeds " + MAX_DIMENSION + "x" + MAX_DIMENSION + ".");
        }
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

    public byte[] compressUsingModeAndOrder(int mode, int order) {

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
        initBitplanes(order);
        xorAndDeltaEncodeBitplanes(mode);
    }

    private void initBitplanes(int order) {
        bp1 = bitplaneFromImage(order == 0 ? image.getBitplane1Image() : image.getBitplane2Image());
        bp2 = bitplaneFromImage(order == 0 ? image.getBitplane2Image() : image.getBitplane1Image());
    }

    private int[][] bitplaneFromImage(BufferedImage image) {
        int[][] bitplane = new int[image.getWidth()][image.getWidth()];
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                bitplane[x][y] = image.getRGB(x, y) == BLACK ? 1 : 0;
            }
        }
        return bitplane;
    }

    private void xorAndDeltaEncodeBitplanes(int mode) {

        if (mode != 1) {
            bp2 = xor(bp1, bp2);
        }

        bp1 = deltaEncode(bp1);
        if (mode != 2) {
            bp2 = deltaEncode(bp2);
        }
    }

    private int[][] xor(int[][] a, int[][] b) {
        int[][] xored = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                xored[x][y] = a[x][y] ^ b[x][y];
            }
        }
        return xored;
    }

    // "[Bitplanes] are delta [en]coded in horizontal lines spanning from the right side to the left
    // side of the [bitplane], going from top to bottom. Each row is [en]coded separately - the state of the
    // system is reset to 0 at the start of each row." -- https://youtu.be/aF1Yw_wu2cM?t=1519
    private int[][] deltaEncode(int[][] bitplane) {
        int[][] encoded = new int[width][height];
        for (int y = 0; y < height; y++) {
            int prev = 0;
            for (int x = 0; x < width; x++) {
                int current = bitplane[x][y];
                encoded[x][y] = current ^ prev;
                prev = current;
            }
        }

        return encoded;
    }

    private void compressAndWriteBitplane(int[][] bitplane, BitWriteStream bws) {
        int[] bitPairs = bitPlaneToPairs(bitplane);

        int packetType = bitPairs[0] == 0 ? 0 : 1;
        bws.writeBit(packetType); // 0 for RLE, 1 for data

        System.out.println(bws);
        int i = 0;
        while (i < bitPairs.length) {
            if (packetType == 0) {
                i = writeRLEPacket(bitPairs, i, bws);
            } else {
                i = writeDataPacket(bitPairs, i, bws);
            }
            System.out.println(bws);
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

    private int[] bitPlaneToPairs(int[][] bitplane) {
        int[] pairs = new int[width * height / 2];
        int i = 0;
        for (int x = 0; x < width; x += 2) {
            for (int y = 0; y < height; y++) {
                pairs[i] = (bitplane[x][y] << 1) + bitplane[x + 1][y];
                i++;
            }
        }
        return pairs;
    }

    private void writeImageDimensions(BitWriteStream bws) {
        int high = (width / 8) << 4;
        int low = height / 8;
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
