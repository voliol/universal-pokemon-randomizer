package com.dabomstew.pkrandom.graphics.packs;

import com.dabomstew.pkrandom.graphics.GBCImage;

import java.awt.image.BufferedImage;
import java.util.List;

public class Gen1PlayerCharacterGraphics extends GraphicsPack {

    private static final int FRONT_IMAGE_DIMENSIONS = 7;
    private static final int BACK_IMAGE_DIMENSIONS = 4;
    private static final int OVERWORLD_SPRITE_TILE_AMOUNT = 4 * 3 * 2; // 4 tiles shown at once, 3 directions, 2 frames

    private final GBCImage front;
    private final GBCImage back;
    private final GBCImage walk;
    private final GBCImage bike;
    private final GBCImage fish;

    public Gen1PlayerCharacterGraphics(GraphicsPackEntry entry) {
        super(entry);
        this.front = initFront();
        this.back = initBack();
        this.walk = initWalk();
        this.bike = initBike();
        this.fish = initFish();
    }

    private GBCImage initFront() {
        BufferedImage base = readImage("FrontImage");
        if (base == null) {
            return null;
        }
        GBCImage front = new GBCImage(base, true);
        if (front.getWidthInTiles() != FRONT_IMAGE_DIMENSIONS || front.getWidthInTiles() != FRONT_IMAGE_DIMENSIONS) {
            System.out.println("Invalid front image dimensions");
            return null;
        }
        return front;
    }

    private GBCImage initBack() {
        BufferedImage base = readImage("BackImage");
        if (base == null) {
            return null;
        }
        GBCImage back = new GBCImage(base, true);
        if (front.getWidthInTiles() != BACK_IMAGE_DIMENSIONS || front.getWidthInTiles() != BACK_IMAGE_DIMENSIONS) {
            System.out.println("Invalid back image dimensions");
            return null;
        }
        return back;
    }

    private GBCImage initWalk() {
        BufferedImage base = readImage("WalkImage");
        if (base == null) {
            return null;
        }
        GBCImage walk = new GBCImage(base);
        if (walk.getWidthInTiles() * walk.getHeightInTiles() != OVERWORLD_SPRITE_TILE_AMOUNT) {
            System.out.println("Invalid walk sprite dimensions");
            return null;
        }
        return walk;
    }

    private GBCImage initBike() {
        BufferedImage base = readImage("BikeImage");
        if (base == null) {
            return null;
        }
        GBCImage bike = new GBCImage(base);
        if (bike.getWidthInTiles() * bike.getHeightInTiles() != OVERWORLD_SPRITE_TILE_AMOUNT) {
            System.out.println("Invalid bike sprite dimensions");
            return null;
        }
        return bike;
    }

    private GBCImage initFish() {
        String fishSpriteMode = getEntry().getStringValue("FishSpriteMode");
        GBCImage fish = null;
        if (fishSpriteMode.equalsIgnoreCase("combined")) {
            fish = initFishFromCombined();
        } else if (fishSpriteMode.equalsIgnoreCase("separate")) {
            fish = initFishFromSeparate();
        } else {
            System.out.println("Invalid fish sprite mode");
        }

        if (fish == null && hasWalkSprite()) {
            fish = initFishFromWalkSprite();
        }
        return fish;
    }

    private GBCImage initFishFromCombined() {
        BufferedImage base = readImage("FishSprite");
        if (base == null) {
            return null;
        }
        GBCImage fish = new GBCImage(base);
        if (fish.getWidthInTiles() * fish.getHeightInTiles() != (OVERWORLD_SPRITE_TILE_AMOUNT) / 4 ) {
            System.out.println("Invalid fish sprite dimensions");
            return null;
        }
        return fish;
    }

    private GBCImage initFishFromSeparate() {
        // TODO
        return null;
    }

    private GBCImage initFishFromWalkSprite() {
        // TODO
        return null;
    }

    public boolean hasFrontImage() {
        return front != null;
    }

    public GBCImage getFrontImage() {
        return front;
    }

    public boolean hasBackImage() {
        return back != null;
    }

    public GBCImage getBackImage() {
        return back;
    }

    public boolean hasWalkSprite() {
        return walk != null;
    }

    public GBCImage getWalkSprite() {
        return walk;
    }

    public boolean hasBikeSprite() {
        return bike != null;
    }

    public GBCImage getBikeSprite() {
        return bike;
    }

    public boolean hasFishSprite() {
        return fish != null;
    }

    public GBCImage getFishSprite() {
        return fish;
    }

    @Override
    public List<BufferedImage> getSampleImages() {
        return List.of(getFrontImage());
    }
}
