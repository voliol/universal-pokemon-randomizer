package compressors;

/**
 * Pokemon Gen 2 compressor. The algorithm used is "LC_LZ3", via FuSoYa's Lunar Compress.
 */
public class Gen2Cmp {

    public static byte[] compress(byte[] uncompressed) {
        LunarCompressLibrary lunarCompress = LunarCompressLibrary.INSTANCE;
        byte[] compressBoard = new byte[uncompressed.length*2];
        int compressedLength = lunarCompress.LunarRecompress(uncompressed, compressBoard, uncompressed.length,
                compressBoard.length, LunarCompressLibrary.LC_LZ3_FORMAT, LunarCompressLibrary.LC_LZ3_FORMAT2);
        byte[] compressed = new byte[compressedLength];
        System.arraycopy(compressBoard, 0, compressed, 0, compressed.length);
        return compressed;
    }

}
