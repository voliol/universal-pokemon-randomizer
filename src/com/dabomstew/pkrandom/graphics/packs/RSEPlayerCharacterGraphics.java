package com.dabomstew.pkrandom.graphics.packs;

import com.dabomstew.pkrandom.graphics.GBAImage;

public class RSEPlayerCharacterGraphics extends Gen3PlayerCharacterGraphics {

    private static final int BACK_IMAGE_WIDTH = 8;
    private static final int BACK_IMAGE_HEIGHT = 8 * 4;

    private static final int SIT_TILE_AMOUNT = BIG_SPRITE_TILE_AMOUNT * 3;

    private static final int ACRO_BIKE_SPRITE_TILE_AMOUNT = BIG_SPRITE_TILE_AMOUNT * 27;
    private static final int UNDERWATER_SPRITE_TILE_AMOUNT = BIG_SPRITE_TILE_AMOUNT * 4;
    private static final int WATERING_CAN_SPRITE_TILE_AMOUNT = BIG_SPRITE_TILE_AMOUNT * 6;
    private static final int DECORATE_SPRITE_TILE_AMOUNT = MEDIUM_SPRITE_TILE_AMOUNT;
    private static final int FIELD_MOVE_SPRITE_TILE_AMOUNT = BIG_SPRITE_TILE_AMOUNT * 5;

    private final GBAImage acroBike;
    private final GBAImage underwater;
    private final GBAImage wateringCan;
    private final GBAImage decorate;
    private final GBAImage fieldMove;

    public RSEPlayerCharacterGraphics(GraphicsPackEntry entry) {
        super(entry);
        this.acroBike = initSprite("AcroBikeSprite", ACRO_BIKE_SPRITE_TILE_AMOUNT);
        this.underwater = initSprite("UnderwaterSprite", UNDERWATER_SPRITE_TILE_AMOUNT);
        this.wateringCan = initSprite("WateringCanSprite", WATERING_CAN_SPRITE_TILE_AMOUNT);
        this.decorate = initSprite("DecorateSprite", DECORATE_SPRITE_TILE_AMOUNT); // TODO: is decorate a sprite?
        this.fieldMove = initSprite("FieldMoveSprite", FIELD_MOVE_SPRITE_TILE_AMOUNT);
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

    public boolean hasAcroBikeSprite() {
        return acroBike != null;
    }

    public GBAImage getAcroBikeSprite() {
        return acroBike;
    }

    public boolean hasUnderwaterSprite() {
        return underwater != null;
    }

    public GBAImage getUnderwaterSprite() {
        return underwater;
    }

    public boolean hasWateringCanSprite() {
        return wateringCan != null;
    }

    public GBAImage getWateringCanSprite() {
        return wateringCan;
    }

    public boolean hasDecorateSprite() {
        return decorate != null;
    }

    public GBAImage getDecorateSprite() {
        return decorate;
    }

    public boolean hasFieldMoveSprite() {
        return fieldMove != null;
    }

    public GBAImage getFieldMoveSprite() {
        return fieldMove;
    }
}