package test.graphics;

import com.dabomstew.pkrandom.GFXFunctions;
import com.dabomstew.pkrandom.graphics.GBAImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GBAImageTest {

    private static final String IMAGES_ADRESS = "test/test_images";

    private static BufferedImage bim1;
    private static BufferedImage bim2;

    @BeforeAll
    static void beforeAll() throws IOException {
        bim1 = ImageIO.read(new File(IMAGES_ADRESS + "/indexed.png"));
        bim2 = ImageIO.read(new File(IMAGES_ADRESS + "/nonindexed.png"));
    }

    @Test
    void canInitFromBimWithIndexedColors() {
        new GBAImage(bim1);
    }

    @Test
    void canInitFromBimWithNonIndexedColors() {
        new GBAImage(bim2);
    }

    @Test
    void paletteHas16Colors() {
        GBAImage a = new GBAImage(bim1);
        assertEquals(16, a.getPalette().size());
        GBAImage b = new GBAImage(bim2);
        assertEquals(16, b.getPalette().size());
    }

    @Test
    void imagesFromSameBimAreEqual() {
        assertEquals(new GBAImage(bim1), new GBAImage(bim1));
    }

    @Test
    void toBytesMirrorsGFXFunction() {
        GBAImage a = new GBAImage(bim1);
        assertTrue(Arrays.equals(GFXFunctions.readTiledImageData(a), a.toBytes()));
    }

    @Test
    void toBytesPlusFromBytesCreatesEqualImage() {
        GBAImage a = new GBAImage(bim1);
        GBAImage b = new GBAImage(a.getWidthInTiles(), a.getHeightInTiles(), a.getPalette(), a.toBytes());
        assertEquals(a, b);
    }

    @Test
    void toBytesPlusFromBytesCreatesEqualImageWithColumnModeTrue() {
        GBAImage a = new GBAImage(bim1, true);
        GBAImage b = new GBAImage(a.getWidthInTiles(), a.getHeightInTiles(), a.getPalette(), a.toBytes(), true);
        assertEquals(a, b);
    }
}
