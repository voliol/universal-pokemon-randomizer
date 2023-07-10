package com.dabomstew.pkrandom.graphics.packs;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import com.dabomstew.pkrandom.graphics.GBAImage;
import com.dabomstew.pkrandom.graphics.palettes.Palette;

// TODO: non-abstract classes for RSE and FRLG
public abstract class Gen3PlayerCharacterGraphics extends GraphicsPack {

    private final static int FRONT_IMAGE_DIMENSIONS = 8;
    private final static int MAP_ICON_DIMENSIONS = 1;

    protected final static int MEDIUM_SPRITE_WIDTH = 2;
    protected final static int MEDIUM_SPRITE_HEIGHT = 4;
    protected final static int BIG_SPRITE_WIDTH = 4;
    protected final static int BIG_SPRITE_HEIGHT = 4;

    // amount of tiles shown at once * 3 directions * n frames/direction
    private final static int WALK_SPRITE_TILE_AMOUNT = MEDIUM_SPRITE_WIDTH * MEDIUM_SPRITE_HEIGHT * 3 * 3;
    private final static int RUN_SPRITE_TILE_AMOUNT = WALK_SPRITE_TILE_AMOUNT;
    private final static int BIKE_SPRITE_TILE_AMOUNT = BIG_SPRITE_WIDTH * BIG_SPRITE_HEIGHT * 3 * 3;
    private final static int FISH_SPRITE_TILE_AMOUNT = BIG_SPRITE_WIDTH * BIG_SPRITE_HEIGHT * 3 * 4;

    private final static int PALETTE_SIZE = 16;

    private final GBAImage front;
    private final GBAImage back;
    private final GBAImage walk;
    private final GBAImage run;
    private final GBAImage bike; // acro bike
    private final GBAImage fish;
    private final GBAImage sit;
    private final GBAImage mapIcon;

    private final Palette normalSpritePalette;
    private final Palette reflectionSpritePalette;
    private final Palette mapIconPalette;

    public Gen3PlayerCharacterGraphics(GraphicsPackEntry entry) {
        super(entry);
        this.front = initFront();
        this.back = initBack();
        this.walk = initWalk();
        this.run = initRun();
        this.bike = initBike();
        this.fish = initFish();
        this.sit = initSit();
        this.mapIcon = initMapIcon();
        this.normalSpritePalette = initNormalSpritePalette();
        this.reflectionSpritePalette = initReflectionSpritePalette();
        this.mapIconPalette = initMapIconPalette();
    }

    private GBAImage initFront() {
        BufferedImage base = readImage("FrontImage");
        if (base == null) {
            return null;
        }
        GBAImage front = new GBAImage(base, true);
        if (front.getWidthInTiles() != FRONT_IMAGE_DIMENSIONS || front.getWidthInTiles() != FRONT_IMAGE_DIMENSIONS) {
            System.out.println("Invalid front image dimensions");
            return null;
        }
        return front;
    }

    private GBAImage initBack() {
        BufferedImage base = readImage("BackImage");
        if (base == null) {
            return null;
        }
        GBAImage back = new GBAImage(base, true);
        if (back.getWidthInTiles() != getBackImageWidth() || back.getWidthInTiles() != getBackImageHeight()) {
            System.out.println("Invalid back image dimensions");
            return null;
        }
        return back;
    }

    protected abstract int getBackImageWidth();

    protected abstract int getBackImageHeight();

    private GBAImage initWalk() {
        BufferedImage base = readImage("WalkSprite");
        if (base == null) {
            return null;
        }
        GBAImage walk = new GBAImage(base);
        if (walk.getWidthInTiles() * walk.getHeightInTiles() != WALK_SPRITE_TILE_AMOUNT) {
            System.out.println("Invalid walk sprite dimensions");
            return null;
        }
        return walk;
    }

    private GBAImage initRun() {
        BufferedImage base = readImage("RunSprite");
        if (base == null) {
            return null; // TODO: use the walk sprite if it exists, and this doesn't
        }
        GBAImage run = new GBAImage(base);
        if (run.getWidthInTiles() * run.getHeightInTiles() != RUN_SPRITE_TILE_AMOUNT) {
            System.out.println("Invalid run sprite dimensions");
            return null;
        }
        return run;
    }

    private GBAImage initBike() {
        BufferedImage base = readImage("BikeSprite");
        if (base == null) {
            return null;
        }
        GBAImage bike = new GBAImage(base);
        if (bike.getWidthInTiles() * bike.getHeightInTiles() != BIKE_SPRITE_TILE_AMOUNT) {
            System.out.println("Invalid bike sprite dimensions");
            return null;
        }
        return bike;
    }

    private GBAImage initFish() {
        BufferedImage base = readImage("FishSprite");
        if (base == null) {
            return null;
        }
        GBAImage fish = new GBAImage(base);
        if (fish.getWidthInTiles() * fish.getHeightInTiles() != FISH_SPRITE_TILE_AMOUNT) {
            System.out.println("Invalid fish sprite dimensions");
            return null;
        }
        return fish;
    }

    private GBAImage initSit() {
        BufferedImage base = readImage("BackImage");
        if (base == null) {
            return null;
        }
        GBAImage sit = new GBAImage(base);
        if (sit.getWidthInTiles() * sit.getHeightInTiles() != getSitTileAmount()) {
            System.out.println("Invalid sit sprite dimensions");
            return null;
        }
        return sit;
    }

    protected abstract int getSitTileAmount();

    private GBAImage initMapIcon() {
        BufferedImage base = readImage("MapIcon");
        if (base == null) {
            return null;
        }
        GBAImage mapIcon = new GBAImage(base, true);
        if (back.getWidthInTiles() != MAP_ICON_DIMENSIONS || back.getWidthInTiles() != MAP_ICON_DIMENSIONS) {
            System.out.println("Invalid map icon dimensions");
            return null;
        }
        return mapIcon;
    }

    private Palette initNormalSpritePalette() {
        Palette palette = readPalette("SpritePalette");
        if (palette == null && hasWalkSprite()) {
            palette = walk.getPalette();
        }
        return palette;
    }

    private Palette initReflectionSpritePalette() {
        Palette palette = readPalette("SpriteReflectionPalette");
        if (palette == null) {
            palette = normalSpritePalette; // TODO: auto-soften the palette
        }
        return palette;
    }

    private Palette initMapIconPalette() {
        Palette palette = readPalette("MapIconPalette");
        if (palette == null && hasMapIcon()) {
            palette = mapIcon.getPalette();
        }
        return palette;
    }

    public boolean hasFrontImage() {
        return front != null;
    }

    public GBAImage getFrontImage() {
        return front;
    }

    public boolean hasBackImage() {
        return back != null;
    }

    public GBAImage getBackImage() {
        return back;
    }

    public boolean hasWalkSprite() {
        return walk != null;
    }

    public GBAImage getWalkSprite() {
        return walk;
    }

    public boolean hasRunSprite() {
        return run != null;
    }

    public GBAImage getRunSprite() {
        return run;
    }

    public boolean hasBikeSprite() {
        return bike != null;
    }

    public GBAImage getBikeSprite() {
        return bike;
    }

    public boolean hasFishSprite() {
        return fish != null;
    }

    public GBAImage getFishSprite() {
        return fish;
    }

    public boolean hasSitSprite() {
        return sit != null;
    }

    public GBAImage getSitSprite() {
        return sit;
    }

    public boolean hasMapIcon() {
        return mapIcon != null;
    }

    public GBAImage getMapIcon() {
        return mapIcon;
    }

    public Palette getNormalSpritePalette() {
        return normalSpritePalette;
    }

    public Palette getReflectionSpritePalette() {
        return reflectionSpritePalette;
    }

    public Palette getMapIconPalette() {
        return mapIconPalette;
    }

    @Override
    public List<BufferedImage> getSampleImages() {
        return Arrays.asList(getFrontImage(), getBackImage(), getWalkSprite(), getBikeSprite());
    }

    @Override
    protected Palette readPalette(String key) {
        Palette palette = super.readPalette(key);
        if (palette == null) {
            return null;
        }
        if (palette.size() != PALETTE_SIZE) {
            System.out.println("Invalid palette size");
            return null;
        }
        return palette;
    }

}
