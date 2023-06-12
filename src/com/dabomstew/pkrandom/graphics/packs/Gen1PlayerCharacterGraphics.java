package com.dabomstew.pkrandom.graphics.packs;

import com.dabomstew.pkrandom.graphics.GBCImage;

import java.awt.image.BufferedImage;
import java.util.List;

public class Gen1PlayerCharacterGraphics extends GraphicsPack {

    private GBCImage front;
    private GBCImage back;

    private GBCImage walk;
    private GBCImage bike;
    private GBCImage fish;

    public Gen1PlayerCharacterGraphics(GraphicsPackEntry entry) {
        super(entry);
    }

    public GBCImage getFrontImage() {
        if (front == null) {
            front = new GBCImage(readImage("FrontImage"));
        }
        return front;
    }

    public GBCImage getBackImage() {
        if (back == null) {
            back = new GBCImage(readImage("BackImage"));
        }
        return back;
    }

    public GBCImage getWalkSprite() {
        if (walk == null) {
            walk = new GBCImage(readImage("WalkSprite"));
        }
        return walk;
    }

    public GBCImage getBikeSprite() {
        if (bike == null) {
            bike = new GBCImage(readImage("BikeSprite"));
        }
        return bike;
    }

    @Override
    public List<BufferedImage> getSampleImages() {
        return List.of(getFrontImage());
    }
}
