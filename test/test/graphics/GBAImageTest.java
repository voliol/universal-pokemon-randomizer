package test.graphics;

import com.dabomstew.pkrandom.GFXFunctions;
import com.dabomstew.pkrandom.graphics.images.GBAImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

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
        new GBAImage.Builder(bim1).build();
    }

    @Test
    void canInitFromBimWithNonIndexedColors() {
        new GBAImage.Builder(bim2).build();
    }

    @Test
    void paletteHas16Colors() {
        GBAImage a = new GBAImage.Builder(bim1).build();
        assertEquals(16, a.getPalette().size());
        GBAImage b = new GBAImage.Builder(bim2).build();
        assertEquals(16, b.getPalette().size());
    }

    @Test
    void imagesFromSameBimAreEqual() {
        assertEquals(new GBAImage.Builder(bim1).build(), new GBAImage.Builder(bim1).build());
    }

    @Test
    void toBytesMirrorsGFXFunction() {
        GBAImage a = new GBAImage.Builder(bim1).build();
        assertArrayEquals(GFXFunctions.readTiledImageData(a), a.toBytes());
    }

    @Test
    void toBytesPlusFromBytesCreatesEqualImage() {
        GBAImage a = new GBAImage.Builder(bim1).build();
        GBAImage b = new GBAImage.Builder(a.getWidthInTiles(), a.getHeightInTiles(), a.getPalette(), a.toBytes())
                .build();
        assertEquals(a, b);
    }

    @Test
    void toBytesPlusFromBytesCreatesEqualImageWithColumnModeTrue() {
        GBAImage a = new GBAImage.Builder(bim1)
                .columnMode(true).build();
        GBAImage b = new GBAImage.Builder(a.getWidthInTiles(), a.getHeightInTiles(), a.getPalette(), a.toBytes())
                .columnMode(true).build();
        assertEquals(a, b);
    }

    @Test
    void toBytesPlusFromBytesCreatesEqualImageWithNonIndexedBim() {
        GBAImage a = new GBAImage.Builder(bim2).columnMode(true).build();
        GBAImage b = new GBAImage.Builder(a.getWidthInTiles(), a.getHeightInTiles(), a.getPalette(), a.toBytes())
                .columnMode(true).build();
        assertEquals(a, b);
    }
}
