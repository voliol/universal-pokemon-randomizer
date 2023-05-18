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
import com.dabomstew.pkrandom.graphics.GBCImage;

public class Gen1Cmp {

	private static final String IN_ADRESS = "compresstest/in";
	private static final String OUT_ADRESS = "compresstest/out";
	private static final String[] TEST_FILE_NAMES = new String[] { "abra", "aerodactyl", "alakazam", "arcanine",
			"articuno", "beedrill", "bellsprout", "blastoise", "bulbasaur", "butterfree", "caterpie", "chansey",
			"charizard", "charmander", "charmeleon", "clefable", "clefairy", "cloyster", "cubone", "diglett", "ditto",
			"dodrio", "doduo", "dragonair", "dragonite", "dratini", "drowzee", "dugtrio" };

	private static int[][] tested = new int[3][2];
	private static int[][] succeeded = new int[3][2];
	private static int[][] failed = new int[3][2];
	private static int[][] erred = new int[3][2];

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
		int mode = 0;
		int order = 0;
		try {
			System.out.println(name);
			BufferedImage bim = null;
			try {
				bim = ImageIO.read(new File(IN_ADRESS + "/" + name + ".png"));
			} catch (IOException ignored) {
			}

			Gen1Cmp compressor = new Gen1Cmp(new GBCImage(bim));
			byte[] compressed = compressor.compressInner();
			tested[compressor.bestMode - 1][compressor.bestOrder]++;

			byte[] rom = Arrays.copyOf(compressed, 0x100000);
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

			if (data.equals(new GBCImage(bim).getData())) {
				succeeded[mode][order]++;
			} else {
				failed[mode][order]++;
			}
		} catch (Exception e) {
			erred[mode][order]++;
			e.printStackTrace();
		}
	}

	private final byte[] bitplane1;
	private final byte[] bitplane2;
	private final int widthInTiles;
	private final int heightInTiles;

	private int bestMode = -1, bestOrder = -1;

	public static byte[] compress(GBCImage image) {
		return new Gen1Cmp(image).compressInner();
	}

	public static byte[] compress(byte[] bitplane1, byte[] bitplane2, int widthInTiles, int heightInTiles) {
		return new Gen1Cmp(bitplane1, bitplane2, widthInTiles, heightInTiles).compressInner();
	}

	private Gen1Cmp(GBCImage image) {
		this(image.getBitplane1(), image.getBitplane2(), image.getWidthInTiles(), image.getHeightInTiles());
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
			for (int order = 0; order < 2; order++) {
				byte[] compressed2 = compressUsingModeAndOrder(mode, order);
				if (shortest == null || compressed2.length < shortest.length) {
					shortest = compressed2;
					bestMode = mode;
					bestOrder = order;
				}
			}
		}
		return shortest;
	}

	private byte[] compressUsingModeAndOrder(int mode, int order) {
		BitWriteStream bws = new BitWriteStream();
		writeImageDimensions(bws);
		bws.writeBit(order);
		System.out.println(bws);
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

	private int[] bitPlaneToPairs(byte[] bitplane) { // TODO: does this work?
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
			for (Byte b : bytes) {
				for (int j = 7; j >= 0; j--) {
					sb.append((b >> j) & 1);
				}
			}
			return sb.toString();
		}

	}

}
