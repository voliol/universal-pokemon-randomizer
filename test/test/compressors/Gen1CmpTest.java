package test.compressors;

import com.dabomstew.pkrandom.graphics.GBCImage;
import compressors.Gen1Cmp;
import compressors.Gen1Decmp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Gen1CmpTest {

    private static final String IN_ADRESS = "compresstest/in";
    private static final String OUT_ADRESS = "compresstest/out";
    private static final String[] TEST_FILE_NAMES = new String[]{"testab", "testamogus", "test", "testbig", "testwhite",
            "testblack", "testgrays", "testrect", "abra", "aerodactyl", "alakazam", "arcanine",
            "articuno", "beedrill", "bellsprout", "blastoise", "bulbasaur", "butterfree", "caterpie", "chansey",
            "charizard", "charmander", "charmeleon", "clefable", "clefairy", "cloyster", "cubone", "diglett", "ditto",
            "dodrio", "doduo", "dragonair", "dragonite", "dratini", "drowzee", "dugtrio", "blackmage"};

    public static String[] getImageNames() {
        return TEST_FILE_NAMES;
    }

    @ParameterizedTest
    @MethodSource("getImageNames")
    public void testImage(String name) {
        System.out.println(name);
        GBCImage bim = null;
        try {
            bim = new GBCImage(ImageIO.read(new File(IN_ADRESS + "/" + name + ".png")));
        } catch (IOException ignored) {
        }

        writeBitplaneImages(bim, name);

        int[][] succeeded = new int[3][2];
        int[][] failed = new int[3][2];
        int[][] erred = new int[3][2];

        for (int mode = 0; mode <= 2; mode++) {
            for (int order = 0; order <= 1; order++) {
                try {

                    Gen1Cmp compressor = new Gen1Cmp(bim);
                    byte[] compressed = compressor.compressUsingModeAndOrder(mode, order == 1);

                    byte[] rom = Arrays.copyOf(compressed, 0x100000);
                    Gen1Decmp sprite = new Gen1Decmp(rom, 0);
                    sprite.decompress();
                    GBCImage bim2 = new GBCImage(sprite.getWidth() / 8, sprite.getHeight() / 8,
                            GBCImage.DEFAULT_PALETTE, sprite.getData());
                    try {
                        ImageIO.write(bim2, "png", new File(OUT_ADRESS + "/" + name + "_m" + mode + "o" + order + ".png"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (new GBCImage(bim).equals(new GBCImage(bim2))) {
                        succeeded[mode][order] = 1;
                    } else {
                        failed[mode][order] = 1;
                    }

                } catch (Exception e) {
                    erred[mode][order] = 1;
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Succed: " + Arrays.deepToString(succeeded));
        System.out.println("Failed: " + Arrays.deepToString(failed));
        System.out.println("Errord: " + Arrays.deepToString(erred));
        if (!Arrays.deepEquals(new int[3][2], erred)) {
            throw new RuntimeException("Erred");
        }
        assertTrue(Arrays.deepEquals(new int[][]{{1, 1}, {1, 1}, {1, 1}}, succeeded));
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

}
