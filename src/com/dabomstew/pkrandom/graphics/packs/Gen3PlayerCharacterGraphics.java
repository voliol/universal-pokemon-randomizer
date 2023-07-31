package com.dabomstew.pkrandom.graphics.packs;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import com.dabomstew.pkrandom.graphics.GBAImage;
import com.dabomstew.pkrandom.graphics.palettes.Palette;

public abstract class Gen3PlayerCharacterGraphics extends GraphicsPack { // TODO: these class names are too long

    private final static int FRONT_IMAGE_DIMENSIONS = 8;
    private final static int MAP_ICON_DIMENSIONS = 2;

    public final static int MEDIUM_SPRITE_WIDTH = 2;
    public final static int MEDIUM_SPRITE_HEIGHT = 4;
    public final static int MEDIUM_SPRITE_TILE_AMOUNT = MEDIUM_SPRITE_WIDTH * MEDIUM_SPRITE_HEIGHT;
    public final static int BIG_SPRITE_WIDTH = 4;
    public final static int BIG_SPRITE_HEIGHT = 4;
    public static final int BIG_SPRITE_TILE_AMOUNT = BIG_SPRITE_WIDTH * BIG_SPRITE_HEIGHT;
    public final static int HUGE_SPRITE_WIDTH = 8;
    public final static int HUGE_SPRITE_HEIGHT = 8;
    public static final int HUGE_SPRITE_TILE_AMOUNT = HUGE_SPRITE_WIDTH * HUGE_SPRITE_HEIGHT;

    public final static int WALK_SPRITE_FRAME_NUM = 3 * 3;
    public final static int RUN_SPRITE_FRAME_NUM = WALK_SPRITE_FRAME_NUM;
    public final static int BIKE_SPRITE_FRAME_NUM = 3 * 3;
    public final static int FISH_SPRITE_FRAME_NUM = 3 * 4;
    public final static int SIT_SPRITE_FRAME_NUM = 3;

    // amount of tiles shown at once * 3 directions * n frames/direction
    private final static int WALK_SPRITE_TILE_AMOUNT = MEDIUM_SPRITE_TILE_AMOUNT * WALK_SPRITE_FRAME_NUM;
    private final static int RUN_SPRITE_TILE_AMOUNT = MEDIUM_SPRITE_TILE_AMOUNT * RUN_SPRITE_FRAME_NUM;
    private final static int BIKE_SPRITE_TILE_AMOUNT = BIG_SPRITE_TILE_AMOUNT * BIKE_SPRITE_FRAME_NUM;
    private final static int FISH_SPRITE_TILE_AMOUNT = BIG_SPRITE_TILE_AMOUNT * FISH_SPRITE_FRAME_NUM;

    private final static int PALETTE_SIZE = 16;

    private final GBAImage front;
    private final GBAImage back;
    private final GBAImage walk;
    private final GBAImage run;
    private final GBAImage bike; // mach bike
    private final GBAImage fish;
    private final GBAImage sit;
    private final GBAImage mapIcon;

    private final Palette normalSpritePalette;
    private final Palette reflectionSpritePalette;

    public Gen3PlayerCharacterGraphics(GraphicsPackEntry entry) {
        super(entry);
        this.front = initFront();
        this.back = initBack();
        this.walk = initSprite("WalkSprite", WALK_SPRITE_TILE_AMOUNT);
        this.run = initRun();
        this.bike = initSprite("BikeSprite", BIKE_SPRITE_TILE_AMOUNT);
        this.fish = initSprite("FishSprite", FISH_SPRITE_TILE_AMOUNT);
        this.sit = initSprite("SitSprite", getSitTileAmount());
        this.mapIcon = initMapIcon();
        this.normalSpritePalette = initNormalSpritePalette();
        this.reflectionSpritePalette = initReflectionSpritePalette();
    }

    private GBAImage initFront() {
        BufferedImage base = readImage("FrontImage");
        if (base == null) {
            return null;
        }
        GBAImage front = new GBAImage(base);
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
        GBAImage back = new GBAImage(base);
        if (back.getWidthInTiles() != getBackImageWidth() || back.getHeightInTiles() != getBackImageHeight()) {
            System.out.println("Invalid back image dimensions");
            return null;
        }
        return back;
    }

    protected abstract int getBackImageWidth();

    protected abstract int getBackImageHeight();


    private GBAImage initRun() {
        GBAImage run = initSprite("RunSprite", RUN_SPRITE_TILE_AMOUNT);
        if (run == null) {
            run = walk;
        }
        return run;
    }

    protected abstract int getSitTileAmount();

    private GBAImage initMapIcon() {
        BufferedImage base = readImage("MapIcon");
        if (base == null) {
            return null;
        }
        GBAImage mapIcon = new GBAImage(base);
        if (mapIcon.getWidthInTiles() != MAP_ICON_DIMENSIONS || mapIcon.getWidthInTiles() != MAP_ICON_DIMENSIONS) {
            System.out.println("Invalid map icon dimensions");
            return null;
        }
        return mapIcon;
    }

    private Palette initNormalSpritePalette() {
        Palette palette = readPalette("SpriteNormalPalette");
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

    protected GBAImage initSprite(String key, int tileAmount) {
        BufferedImage base = readImage(key);
        if (base == null) {
            return null;
        }
        GBAImage sprite = new GBAImage(base);
        if (sprite.getWidthInTiles() * sprite.getHeightInTiles() != tileAmount) {
            System.out.println("Invalid " + key + " dimensions");
            return null;
        }
        return sprite;
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

    /**
     * If there is a normal sprite palette, there is also one for reflections.
     */
    public boolean hasSpritePalettes() {
        return normalSpritePalette != null;
    }

    public Palette getNormalSpritePalette() {
        return normalSpritePalette;
    }

    public Palette getReflectionSpritePalette() {
        return reflectionSpritePalette;
    }

    private BufferedImage getBackImageSpriteForSample() {
        return back == null ? null : back.getFrameSubimage(0, getBackImageWidth(), getBackImageWidth());
    }

    private BufferedImage getWalkSpriteForSample() {
        return walk == null ? null : walk.getFrameSubimage(0, MEDIUM_SPRITE_WIDTH, MEDIUM_SPRITE_HEIGHT);
    }

    @Override
    public List<BufferedImage> getSampleImages() {
        return Arrays.asList(getFrontImage(), getBackImageSpriteForSample(), getWalkSpriteForSample());
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
