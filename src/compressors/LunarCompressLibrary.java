package compressors;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Wraps the Lunar Compress .dll
 */
public interface LunarCompressLibrary extends Library {
    String LUNAR_COMPRESS_PATH = "util/Lunar_Compress_1.90_x64";

    int LC_LZ3_FORMAT = 2;
    int LC_LZ3_FORMAT2 = 0;

    LunarCompressLibrary INSTANCE = Native.load(LUNAR_COMPRESS_PATH, LunarCompressLibrary.class);

    int LunarVersion();

    int LunarDecompress(byte[] destination, int addressToStart, int maxDataSize, int format, int format2,
                        int[] lastROMPosition);

    int LunarRecompress(byte[] source, byte[] destination, int dataSize, int maxDataSize, int format, int format2);
}
