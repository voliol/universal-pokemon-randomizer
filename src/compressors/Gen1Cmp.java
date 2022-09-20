package compressors;

// based on pret/pokered/blob/master/tools/pkmncompress.c

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import com.dabomstew.pkrandom.GFXFunctions;

public class Gen1Cmp {

	private static final int COLORS_IN_PALETTE = 4;
	private static final int MAX_WIDTH_IN_TILES = 15;
	private static final int MAX_WIDTH_IN_PIXELS = MAX_WIDTH_IN_TILES * 8;

	private static final String IN_ADRESS = "compresstest/in";
	private static final String OUT_ADRESS = "compresstest/out";
	private static final String[] TEST_FILE_NAMES = new String[] { "abra", "aerodactyl", "alakazam", "arcanine",
			"articuno", "beedrill", "bellsprout", "blastoise", "bulbasaur", "butterfree", "caterpie", "chansey",
			"charizard", "charmander", "charmeleon", "clefable", "clefairy", "cloyster", "cubone", "diglett", "ditto",
			"dodrio", "doduo", "dragonair", "dragonite", "dratini", "drowzee", "dugtrio" };
	private static final String BLASTOISE_ADRESS = "bin/com/dabomstew/pkrandom/graphics/resources";
	private static final String BLASTOISE_FILE_NAME = "pokeImageFRLG_9f";

	private static final byte[] MANUAL_RED_DATA = { (byte) 0x07, (byte) 0x07, (byte) 0x08, (byte) 0x0F, (byte) 0x10,
			(byte) 0x1F, (byte) 0x10, (byte) 0x1F, (byte) 0x3B, (byte) 0x3C, (byte) 0x3F, (byte) 0x37, (byte) 0x7F,
			(byte) 0x50, (byte) 0x7F, (byte) 0x42, (byte) 0xE0, (byte) 0xE0, (byte) 0x10, (byte) 0xF0, (byte) 0x08,
			(byte) 0xF8, (byte) 0x08, (byte) 0xF8, (byte) 0xDC, (byte) 0x3C, (byte) 0xFC, (byte) 0xEC, (byte) 0xFE,
			(byte) 0x0A, (byte) 0xFE, (byte) 0x42, (byte) 0x3F, (byte) 0x32, (byte) 0x3E, (byte) 0x39, (byte) 0x7F,
			(byte) 0x4F, (byte) 0x7F, (byte) 0x4F, (byte) 0x39, (byte) 0x3F, (byte) 0x16, (byte) 0x1F, (byte) 0x11,
			(byte) 0x1F, (byte) 0x0E, (byte) 0x0E, (byte) 0xFC, (byte) 0x4C, (byte) 0x7C, (byte) 0x9C, (byte) 0xFE,
			(byte) 0xF2, (byte) 0xFE, (byte) 0xF2, (byte) 0x9C, (byte) 0xFC, (byte) 0x68, (byte) 0xF8, (byte) 0x88,
			(byte) 0xF8, (byte) 0x70, (byte) 0x70, (byte) 0x07, (byte) 0x07, (byte) 0x08, (byte) 0x0F, (byte) 0x10,
			(byte) 0x1F, (byte) 0x10, (byte) 0x1F, (byte) 0x30, (byte) 0x3F, (byte) 0x38, (byte) 0x3F, (byte) 0x7F,
			(byte) 0x5F, (byte) 0x7F, (byte) 0x4F, (byte) 0xE0, (byte) 0xE0, (byte) 0x10, (byte) 0xF0, (byte) 0x08,
			(byte) 0xF8, (byte) 0x08, (byte) 0xF8, (byte) 0x0C, (byte) 0xFC, (byte) 0x1C, (byte) 0xFC, (byte) 0xFE,
			(byte) 0xFA, (byte) 0xFE, (byte) 0xF2, (byte) 0x3F, (byte) 0x33, (byte) 0x3C, (byte) 0x3F, (byte) 0x7B,
			(byte) 0x5F, (byte) 0x79, (byte) 0x5E, (byte) 0x3C, (byte) 0x3F, (byte) 0x17, (byte) 0x1F, (byte) 0x11,
			(byte) 0x1F, (byte) 0x0E, (byte) 0x0E, (byte) 0xFC, (byte) 0xCC, (byte) 0x3C, (byte) 0xFC, (byte) 0xDE,
			(byte) 0xFA, (byte) 0x9E, (byte) 0x7A, (byte) 0x3C, (byte) 0xFC, (byte) 0xE8, (byte) 0xF8, (byte) 0x88,
			(byte) 0xF8, (byte) 0x70, (byte) 0x70, (byte) 0x07, (byte) 0x07, (byte) 0x08, (byte) 0x0F, (byte) 0x10,
			(byte) 0x1F, (byte) 0x38, (byte) 0x37, (byte) 0x7C, (byte) 0x43, (byte) 0x31, (byte) 0x3F, (byte) 0x1F,
			(byte) 0x14, (byte) 0x1F, (byte) 0x14, (byte) 0xE0, (byte) 0xE0, (byte) 0x10, (byte) 0xF0, (byte) 0x08,
			(byte) 0xF8, (byte) 0x08, (byte) 0xF8, (byte) 0x1C, (byte) 0xFC, (byte) 0xFC, (byte) 0xFC, (byte) 0xFC,
			(byte) 0xFC, (byte) 0xF8, (byte) 0x98, (byte) 0x1F, (byte) 0x10, (byte) 0x0B, (byte) 0x0C, (byte) 0x07,
			(byte) 0x07, (byte) 0x03, (byte) 0x03, (byte) 0x03, (byte) 0x03, (byte) 0x04, (byte) 0x07, (byte) 0x04,
			(byte) 0x07, (byte) 0x03, (byte) 0x03, (byte) 0xF0, (byte) 0x10, (byte) 0xE8, (byte) 0x78, (byte) 0xC8,
			(byte) 0xF8, (byte) 0xE8, (byte) 0x38, (byte) 0xE8, (byte) 0x38, (byte) 0xF0, (byte) 0xF0, (byte) 0x20,
			(byte) 0xE0, (byte) 0xC0, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x07, (byte) 0x08,
			(byte) 0x0F, (byte) 0x10, (byte) 0x1F, (byte) 0x10, (byte) 0x1F, (byte) 0x3B, (byte) 0x3C, (byte) 0x3F,
			(byte) 0x3F, (byte) 0x7F, (byte) 0x50, (byte) 0x00, (byte) 0x00, (byte) 0xE0, (byte) 0xE0, (byte) 0x10,
			(byte) 0xF0, (byte) 0x08, (byte) 0xF8, (byte) 0x08, (byte) 0xF8, (byte) 0xDC, (byte) 0x3C, (byte) 0xFC,
			(byte) 0xFC, (byte) 0xFE, (byte) 0x0A, (byte) 0x7F, (byte) 0x42, (byte) 0x7F, (byte) 0x72, (byte) 0x7E,
			(byte) 0x59, (byte) 0x3F, (byte) 0x3F, (byte) 0x1B, (byte) 0x1F, (byte) 0x0E, (byte) 0x0F, (byte) 0x09,
			(byte) 0x0F, (byte) 0x07, (byte) 0x07, (byte) 0xFE, (byte) 0x42, (byte) 0xFC, (byte) 0x4C, (byte) 0x74,
			(byte) 0x9C, (byte) 0xFC, (byte) 0xF4, (byte) 0xFC, (byte) 0xCC, (byte) 0x78, (byte) 0xC8, (byte) 0xB0,
			(byte) 0xB0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x07, (byte) 0x08,
			(byte) 0x0F, (byte) 0x10, (byte) 0x1F, (byte) 0x10, (byte) 0x1F, (byte) 0x30, (byte) 0x3F, (byte) 0x38,
			(byte) 0x3F, (byte) 0x7F, (byte) 0x5F, (byte) 0x00, (byte) 0x00, (byte) 0xE0, (byte) 0xE0, (byte) 0x10,
			(byte) 0xF0, (byte) 0x08, (byte) 0xF8, (byte) 0x08, (byte) 0xF8, (byte) 0x0C, (byte) 0xFC, (byte) 0x1C,
			(byte) 0xFC, (byte) 0xFE, (byte) 0xFA, (byte) 0x7F, (byte) 0x4F, (byte) 0x7F, (byte) 0x73, (byte) 0x7C,
			(byte) 0x5F, (byte) 0x3B, (byte) 0x3F, (byte) 0x19, (byte) 0x1E, (byte) 0x0C, (byte) 0x0F, (byte) 0x0B,
			(byte) 0x0F, (byte) 0x07, (byte) 0x07, (byte) 0xFE, (byte) 0xF2, (byte) 0xFC, (byte) 0xCC, (byte) 0x3C,
			(byte) 0xFC, (byte) 0xDE, (byte) 0xF2, (byte) 0x9E, (byte) 0x72, (byte) 0x3C, (byte) 0xFC, (byte) 0xC0,
			(byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x07, (byte) 0x08,
			(byte) 0x0F, (byte) 0x10, (byte) 0x1F, (byte) 0x38, (byte) 0x37, (byte) 0x7C, (byte) 0x43, (byte) 0x31,
			(byte) 0x3F, (byte) 0x1F, (byte) 0x14, (byte) 0x00, (byte) 0x00, (byte) 0xE0, (byte) 0xE0, (byte) 0x10,
			(byte) 0xF0, (byte) 0x08, (byte) 0xF8, (byte) 0x08, (byte) 0xF8, (byte) 0x1C, (byte) 0xFC, (byte) 0xFC,
			(byte) 0xFC, (byte) 0xFC, (byte) 0xFC, (byte) 0x1F, (byte) 0x14, (byte) 0x1F, (byte) 0x10, (byte) 0x0B,
			(byte) 0x0C, (byte) 0x07, (byte) 0x07, (byte) 0x1F, (byte) 0x1F, (byte) 0x24, (byte) 0x3F, (byte) 0x13,
			(byte) 0x1F, (byte) 0x0E, (byte) 0x0E, (byte) 0xF8, (byte) 0x98, (byte) 0xF0, (byte) 0x10, (byte) 0xE8,
			(byte) 0x78, (byte) 0xE8, (byte) 0xF8, (byte) 0xF8, (byte) 0x98, (byte) 0xF4, (byte) 0x9C, (byte) 0xE4,
			(byte) 0xFC, (byte) 0x18, (byte) 0x18 };

	private static int[][] tested = new int[3][2];
	private static int[][] succeeded = new int[3][2];
	private static int[][] failed = new int[3][2];

	private static String hexchars = "0123456789ABCDEF";

	public static void main(String[] args) {

		for (byte b : MANUAL_RED_DATA) {
			int high = (b >> 4) & 0xF;
			int low = b & 0xF;
			System.out.print(hexchars.charAt(high));
			System.out.print(hexchars.charAt(low));
			System.out.print(".");
		}
		System.out.print("\n-------------------------");
		
		BufferedImage red = null;
		try {
			red = ImageIO.read(new File("red_f.png"));
		} catch (IOException e) {
		}

		Gen1Cmp redcmp = new Gen1Cmp(red);
		for (int i = 0; i < redcmp.bitplane1.length; i++) {
			byte b1 = redcmp.bitplane2[i];
			int high1 = (b1 >> 4) & 0xF;
			int low1 = b1 & 0xF;
			System.out.print(hexchars.charAt(high1));
			System.out.print(hexchars.charAt(low1));

			byte b2 = redcmp.bitplane1[i];
			int high2 = (b2 >> 4) & 0xF;
			int low2 = b2 & 0xF;
			System.out.print(hexchars.charAt(high2));
			System.out.print(hexchars.charAt(low2));
		}
		System.out.println("\n-------------------------");

		System.out.println("starting test of gen 1 compression");
		for (String name : TEST_FILE_NAMES) {
			testImage(name);
		}
		System.out.println("Tested: " + Arrays.deepToString(tested));
		System.out.println("Succed: " + Arrays.deepToString(succeeded));
		System.out.println("Failed: " + Arrays.deepToString(failed));

		testBlastoise();
	}

	private static void testBlastoise() {
		BufferedImage blast = null;
		try {
			blast = ImageIO.read(new File(BLASTOISE_ADRESS + "/" + BLASTOISE_FILE_NAME + ".png"));
		} catch (IOException e) {
		}
		byte[] blastData = GFXFunctions.readTiledImageData(blast);
		int[] blastPalette = new int[16];
		for (int i = 0; i < 16; i++) {
			blastPalette[i] = 0xFF000000;
			blastPalette[i] += 0x101010 * i + 0x080808;
		}
		BufferedImage blastimage = GFXFunctions.drawTiledImage(blastData, blastPalette, 64, 64, 4);
		try {
			ImageIO.write(blastimage, "png", new File("blastoise.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void testImage(String name) {
		int mode = 0;
		int order = 0;
		try {
			System.out.println(name);
			BufferedImage bim = null;
			try {
				bim = ImageIO.read(new File(IN_ADRESS + "/" + name + ".png"));
			} catch (IOException e) {
			}

			testfilename = name;
			Gen1Cmp a = new Gen1Cmp(bim);
			a.compress();
			mode = a.bestMode - 1;
			order = a.bestOrder;
			tested[mode][order]++;

			byte[] rom = Arrays.copyOf(a.compressed, 0x100000);
			Gen1Decmp sprite = new Gen1Decmp(rom, 0);
			sprite.decompress();
			sprite.transpose();
			byte[] data = sprite.getData();

			System.out.println("w: " + sprite.getWidth() + ", h: " + sprite.getHeight());

			int[] convPalette = new int[] { 0xFFFFFFFF, 0xFFAAAAAA, 0xFF666666, 0xFF000000 };
			BufferedImage bim2 = GFXFunctions.drawTiledImage(data, convPalette, sprite.getWidth(), sprite.getHeight(),
					2);
			try {
				ImageIO.write(bim2, "png", new File(OUT_ADRESS + "/" + name + "_overandout.png"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			succeeded[mode][order]++;
		} catch (Exception e) {
			failed[mode][order]++;
			e.printStackTrace();
		}
	}

	private static String testfilename;

	private int widthInTiles;
	private int heightInTiles;
	private byte[] bitplane1, bitplane2;

	private int currentBit;
	private int currentByte;

	private byte[] compressed;
	private int bestMode, bestOrder;

	public Gen1Cmp(BufferedImage bim) {
		if (!hasCompatibleColorModel(bim)) {
			throw new IllegalArgumentException("Image has an invalid/unsupported ColorModel.");
		}

		this.widthInTiles = bim.getWidth() / 8;
		this.heightInTiles = bim.getHeight() / 8;
		if (widthInTiles > MAX_WIDTH_IN_TILES || heightInTiles > MAX_WIDTH_IN_TILES) { // allows non-square images
			throw new IllegalArgumentException("Width or height of image exceeds " + MAX_WIDTH_IN_TILES + " tiles ("
					+ MAX_WIDTH_IN_PIXELS + " pixels).");
		}

		prepareBitplanes(bim);
	}

	private boolean hasCompatibleColorModel(BufferedImage bim) {
		return bim.getColorModel()instanceof IndexColorModel indexColorModel
				&& indexColorModel.getMapSize() == COLORS_IN_PALETTE;
	}

	private void prepareBitplanes(BufferedImage in) {

		BufferedImage bitplane1Image = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		BufferedImage bitplane2Image = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		for (int x = 0; x < in.getWidth(); x++) {
			for (int y = 0; y < in.getHeight(); y++) {
				int sample = in.getData().getSample(x, y, 0);
				if (sample >= 2) {
					bitplane1Image.setRGB(x, y, 0xffffffff);
				}
				if (sample % 2 == 1) {
					bitplane2Image.setRGB(x, y, 0xffffffff);
				}
			}
		}

		try {
			ImageIO.write(bitplane1Image, "png", new File(OUT_ADRESS + "/" + testfilename + "_bp1.png"));
			ImageIO.write(bitplane2Image, "png", new File(OUT_ADRESS + "/" + testfilename + "_bp2.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.bitplane1 = GFXFunctions.readTiledImageData(bitplane1Image, 1);
		this.bitplane2 = GFXFunctions.readTiledImageData(bitplane2Image, 1);
	}

	public void compress() {
		byte[] shortest = null;

		for (int mode = 1; mode <= 3; mode++) {
			for (int order = 0; order < 2; order++) {
				byte[] compressed2 = compressUsingModeAndOrder(mode, order);
				if (shortest == null || compressed2.length < shortest.length) {
					shortest = compressed2;
					bestMode = mode;
					bestOrder = order;
				}
			}
		}

		this.compressed = shortest;
	}

	private byte[] compressUsingModeAndOrder(int mode, int order) {
		BitWriteStream bws = new BitWriteStream();
		writeImageDimensions(bws);
		bws.writeBit(order);
		compressAndWriteBitplane(order == 1 ? bitplane2 : bitplane1, bws);
		writeMode(mode, bws);
		compressAndWriteBitplane(order == 1 ? bitplane1 : bitplane2, bws);
		return bws.toByteArray();
	}

	private void compressAndWriteBitplane(byte[] bitplane, BitWriteStream bws) {
		int[] bitPairs = bitPlaneToPairs(bitplane);

		int packetType = bitPairs[0] == 0 ? 0 : 1;
		bws.writeBit(packetType); // 0 for RLE, 1 for data

		int i = 0;
		while (i < bitPairs.length) {
			if (packetType == 0) {
				i = writeRLEPacket(bitPairs, i, bws);
			} else {
				i = writeDataPacket(bitPairs, i, bws);
			}
			packetType ^= 1;
		}
	}

	private int writeDataPacket(int[] bitPairs, int i, BitWriteStream bws) {
		do {
			bws.writeBitPair(bitPairs[i]);
			i++;
		} while (i < bitPairs.length - 1 && bitPairs[i] != 0b00);
		return i;
	}

	private int writeRLEPacket(int[] bitPairs, int i, BitWriteStream bws) {
		int length = 0;
		while (i < bitPairs.length && bitPairs[i] == 0b00) {
			length++;
			i++;
		}

		int bitCount = bitsNeeded(length);

		for (int j = 0; j < bitCount; j++) {
			bws.writeBit(1);
		}
		bws.writeBit(0);

		for (int j = bitCount; j >= 0; j--) {
			bws.writeBit((length >> j) & 1);
		}
		return i;
	}

	private static int bitsNeeded(int i) {
		int needed = 0;
		while ((i & 1) == 1) {
			needed++;
			i >>= 1;
		}
		return needed - 1;
	}

	private int[] bitPlaneToPairs(byte[] bitplane) {
		int[] pairs = new int[bitplane.length * 4];
		int i = 0;
		for (int tileX = 0; tileX < widthInTiles; tileX++) {
			for (int pairX = 0; pairX < 4; pairX++) {
				for (int y = 0; y < heightInTiles * 8; y++) {

					byte fromByte = bitplane[tileX * heightInTiles + y];
					pairs[i] = 0b11 & (fromByte >> (3 - pairX));
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

	private static class BitWriteStream {

		List<Byte> bytes = new ArrayList<>();
		int currentBit = 0;
		byte currentByte = 0;

		public void writeByte(byte b) {
			bytes.add(b);
			currentBit = 7;
		}

		public void writeBit(int bit) {
			if (currentBit == 7) {
				currentBit = 0;
				currentByte = 0;
				bytes.add(currentByte);
			}
			currentByte += (bit << (7 - currentBit));
			bytes.set(bytes.size() - 1, currentByte);
			currentBit++;
		}

		public void writeBitPair(int bitpair) {
			writeBit((bitpair >> 1) & 1);
			writeBit(bitpair & 1);
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
			for (int i = 0; i < bytes.size(); i++) {
				for (int j = 7; j >= 0; j--) {
					sb.append((bytes.get(i) >> j) & 1);
				}
			}
			return sb.toString();
		}

	}

}
