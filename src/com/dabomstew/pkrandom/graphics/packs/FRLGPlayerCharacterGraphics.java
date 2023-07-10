package com.dabomstew.pkrandom.graphics.packs;

public class FRLGPlayerCharacterGraphics extends Gen3PlayerCharacterGraphics {

    private static final int BACK_IMAGE_WIDTH = 8;
    private static final int BACK_IMAGE_HEIGHT = 8 * 5;
    private static final int SIT_TILE_AMOUNT = BIG_SPRITE_WIDTH * BIG_SPRITE_HEIGHT * 3;

    public FRLGPlayerCharacterGraphics(GraphicsPackEntry entry) {
        super(entry);
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

    // TODO: lapras sitting, vs seeker, vs seeker bike, looking around
}
