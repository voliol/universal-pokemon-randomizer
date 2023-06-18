package com.dabomstew.pkrandom.graphics.packs;

import com.dabomstew.pkrandom.graphics.GBCImage;

import java.awt.image.BufferedImage;

public class Gen2PlayerCharacterGraphics extends GBCPlayerCharacterGraphics {

    // TODO: palettes

    public enum ToReplace {CHRIS, KRIS}

    private static final int TRAINER_CARD_IMAGE_WIDTH = 5;
    private static final int TRAINER_CARD_IMAGE_HEIGHT = 7;

    private final ToReplace toReplace;
    private final GBCImage trainerCard;

    public Gen2PlayerCharacterGraphics(GraphicsPackEntry entry, int toReplace) {
        super(entry);
        this.trainerCard = initTrainerCard();
        this.toReplace = ToReplace.values()[toReplace];
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
                trainerCard.getWidthInTiles() != TRAINER_CARD_IMAGE_HEIGHT) {
            System.out.println("Invalid trainer card image dimensions");
            return null;
        }
        return trainerCard;
    }

    private GBCImage initTrainerCardFromFrontImage() {
        return getFrontImage().getSubimageFromTileRect(0, 1, TRAINER_CARD_IMAGE_WIDTH, TRAINER_CARD_IMAGE_HEIGHT);
    }

    // no hasTrainerCardImage(); redundant with hasFrontImage()

    public GBCImage getTrainerCardImage() {
        return trainerCard;
    }

    public ToReplace getToReplace() {
        return toReplace;
    }

    public String getToReplaceName() {
        String s = toReplace.toString();
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

}
