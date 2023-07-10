package com.dabomstew.pkrandom.graphics.packs;

public class RSEPlayerCharacterGraphics extends Gen3PlayerCharacterGraphics {

    private static final int BACK_IMAGE_WIDTH = 8;
    private static final int BACK_IMAGE_HEIGHT = 8 * 4;
    private static final int SIT_TILE_AMOUNT = MEDIUM_SPRITE_WIDTH * MEDIUM_SPRITE_HEIGHT * 4;

    public RSEPlayerCharacterGraphics(GraphicsPackEntry entry) {
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

    // TODO: acro bike, watering, dive, decorating, field move (?)
}
