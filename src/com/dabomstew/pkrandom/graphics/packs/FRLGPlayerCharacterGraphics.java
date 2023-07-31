package com.dabomstew.pkrandom.graphics.packs;

import com.dabomstew.pkrandom.graphics.GBAImage;

public class FRLGPlayerCharacterGraphics extends Gen3PlayerCharacterGraphics {

    private static final int BACK_IMAGE_WIDTH = 8;
    private static final int BACK_IMAGE_HEIGHT = 8 * 5;

    private static final int SIT_TILE_AMOUNT = MEDIUM_SPRITE_TILE_AMOUNT * 3;

    public static final int ITEM_SPRITE_FRAME_NUM = 9;
    public static final int ITEM_BIKE_SPRITE_FRAME_NUM = 6;
    public static final int BIRD_SPRITE_FRAME_NUM = 2;

    private static final int ITEM_SPRITE_TILE_AMOUNT = MEDIUM_SPRITE_TILE_AMOUNT * ITEM_SPRITE_FRAME_NUM;
    private static final int ITEM_BIKE_SPRITE_TILE_AMOUNT = BIG_SPRITE_TILE_AMOUNT * ITEM_BIKE_SPRITE_FRAME_NUM;
    private static final int BIRD_SPRITE_TILE_AMOUNT = HUGE_SPRITE_TILE_AMOUNT * BIRD_SPRITE_FRAME_NUM;

    private final GBAImage item;
    private final GBAImage itemBike;
    private final GBAImage bird;

    public FRLGPlayerCharacterGraphics(GraphicsPackEntry entry) {
        super(entry);
        this.item = initSprite("ItemSprite", ITEM_SPRITE_TILE_AMOUNT);
        this.itemBike = initSprite("ItemBikeSprite", ITEM_BIKE_SPRITE_TILE_AMOUNT);
        this.bird = initSprite("BirdSprite", BIRD_SPRITE_TILE_AMOUNT);
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
    protected int getSitTileAmount() {
        return SIT_TILE_AMOUNT;
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

    public boolean hasBirdSprite() {
        return bird != null;
    }

    public GBAImage getBirdSprite() {
        return bird;
    }

}
