package com.dabomstew.pkrandom.graphics.packs;

import com.dabomstew.pkrandom.graphics.GBCImage;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Gen2PlayerCharacterGraphics extends GBCPlayerCharacterGraphics {

    // TODO: palettes

    private static final int BACK_IMAGE_DIMENSIONS = 6;
    private static final int TRAINER_CARD_IMAGE_WIDTH = 5;
    private static final int TRAINER_CARD_IMAGE_HEIGHT = 7;

    private final GBCImage trainerCard;

    public Gen2PlayerCharacterGraphics(GraphicsPackEntry entry) {
        super(entry);
        this.trainerCard = initTrainerCard();
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
        GBCImage trainerCard = new GBCImage(base, true);
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

    // no hasTrainerCardImage(); redundant with hasFrontImage()

    public GBCImage getTrainerCardImage() {
        return trainerCard;
    }

    @Override
    public List<BufferedImage> getSampleImages() {
        List<BufferedImage> sampleImages = new ArrayList<>(super.getSampleImages());
        sampleImages.add(getTrainerCardImage());
        return sampleImages;
    }

}
