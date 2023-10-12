package com.dabomstew.pkrandom.graphics.packs;

import com.dabomstew.pkrandom.graphics.images.GBCImage;
import com.dabomstew.pkrandom.graphics.palettes.Gen2SpritePaletteID;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Gen2PlayerCharacterGraphics extends GBCPlayerCharacterGraphics {

    // TODO: palettes

    private static final int BACK_IMAGE_DIMENSIONS = 6;
    private static final int TRAINER_CARD_IMAGE_WIDTH = 5;
    private static final int TRAINER_CARD_IMAGE_HEIGHT = 7;

    private final GBCImage trainerCard;
    private final Gen2SpritePaletteID spritePaletteID;

    public Gen2PlayerCharacterGraphics(GraphicsPackEntry entry) {
        super(entry);
        this.trainerCard = initTrainerCard();
        this.spritePaletteID = initSpritePaletteID();
    }

    @Override
    protected int getBackImageDimensions() {
        return BACK_IMAGE_DIMENSIONS;
    }

    private GBCImage initTrainerCard() {
        GBCImage trainerCard = initTrainerCardFromFile();
        if (trainerCard == null && hasFrontImage()) {
            trainerCard = initTrainerCardFromFrontImage();
        }
        return trainerCard;
    }

    private GBCImage initTrainerCardFromFile() {
        BufferedImage base = readImage("TrainerCardImage");
        if (base == null) {
            return null;
        }
        GBCImage trainerCard = new GBCImage.Builder(base).columnMode(true).build();
        if (trainerCard.getWidthInTiles() != TRAINER_CARD_IMAGE_WIDTH ||
                trainerCard.getHeightInTiles() != TRAINER_CARD_IMAGE_HEIGHT) {
            System.out.println("Invalid trainer card image dimensions");
            return null;
        }
        return trainerCard;
    }

    private GBCImage initTrainerCardFromFrontImage() {
        return getFrontImage().getSubimageFromTileRect(1, 0, TRAINER_CARD_IMAGE_WIDTH, TRAINER_CARD_IMAGE_HEIGHT);
    }

    private Gen2SpritePaletteID initSpritePaletteID() {
        String paletteName = getEntry().getStringValue("SpritePalette");
        try {
            return Gen2SpritePaletteID.valueOf(paletteName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // no hasTrainerCardImage(); redundant with hasFrontImage()

    public GBCImage getTrainerCardImage() {
        return trainerCard;
    }

    public boolean hasSpritePaletteID() {
        return spritePaletteID != null;
    }

    public Gen2SpritePaletteID getSpritePaletteID() {
        return spritePaletteID;
    }

    @Override
    public List<BufferedImage> getSampleImages() {
        List<BufferedImage> sampleImages = new ArrayList<>(super.getSampleImages());
        sampleImages.add(getTrainerCardImage());
        return sampleImages;
    }

}
