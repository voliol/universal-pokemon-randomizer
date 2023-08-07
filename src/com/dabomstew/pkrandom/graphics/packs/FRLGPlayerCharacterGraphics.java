package com.dabomstew.pkrandom.graphics.packs;

import com.dabomstew.pkrandom.graphics.GBAImage;

public class FRLGPlayerCharacterGraphics extends Gen3PlayerCharacterGraphics {

    private static final int BACK_IMAGE_WIDTH = 8;
    private static final int BACK_IMAGE_HEIGHT = 8 * 5;

    private static final int SIT_FRAME_WIDTH = MEDIUM_SPRITE_WIDTH;
    private static final int SIT_FRAME_HEIGHT = MEDIUM_SPRITE_HEIGHT;

    public static final int ITEM_SPRITE_FRAME_NUM = 9;
    public static final int ITEM_BIKE_SPRITE_FRAME_NUM = 6;
    public static final int SURF_BLOB_SPRITE_FRAME_NUM = 6;
    public static final int BIRD_SPRITE_FRAME_NUM = 3;

    private final GBAImage item;
    private final GBAImage itemBike;
    private final GBAImage surfBlob;
    private final GBAImage bird;

    public FRLGPlayerCharacterGraphics(GraphicsPackEntry entry) {
        super(entry);
        this.item = initSprite("ItemSprite", ITEM_SPRITE_FRAME_NUM, MEDIUM_SPRITE_WIDTH, MEDIUM_SPRITE_HEIGHT);
        this.itemBike = initSprite("ItemBikeSprite", ITEM_BIKE_SPRITE_FRAME_NUM, BIG_SPRITE_WIDTH, BIG_SPRITE_HEIGHT);
        this.surfBlob = initSprite("SurfBlobSprite", SURF_BLOB_SPRITE_FRAME_NUM, BIG_SPRITE_WIDTH, BIG_SPRITE_HEIGHT);
        this.bird = initSprite("BirdSprite", BIRD_SPRITE_FRAME_NUM, HUGE_SPRITE_WIDTH, HUGE_SPRITE_HEIGHT);
    }

    @Override
    protected int getBackImageWidth() {
        return BACK_IMAGE_WIDTH;
    }

    @Override
    protected int getBackImageHeight() {
        return BACK_IMAGE_HEIGHT;
    }

    @Override
    protected int getSitFrameWidth() {
        return SIT_FRAME_WIDTH;
    }

    @Override
    protected int getSitFrameHeight() {
        return SIT_FRAME_HEIGHT;
    }

    public boolean hasItemSprite() {
        return item != null;
    }

    public GBAImage getItemSprite() {
        return item;
    }

    public boolean hasItemBikeSprite() {
        return itemBike != null;
    }

    public GBAImage getItemBikeSprite() {
        return itemBike;
    }

    public boolean hasSurfBlobSprite() {
        return surfBlob != null;
    } // TODO: generalise to RSE (?)

    public GBAImage getSurfBlobSprite() {
        return surfBlob;
    }

    public boolean hasBirdSprite() {
        return bird != null;
    }

    public GBAImage getBirdSprite() {
        return bird;
    }

}
