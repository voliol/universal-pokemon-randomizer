package com.dabomstew.pkrandom.graphics.packs;

import com.dabomstew.pkrandom.graphics.palettes.Palette;

import java.awt.image.BufferedImage;

public class Gen3PlayerCharacterGraphics extends PlayerCharacterGraphics {

    // TODO: integrate with the UI, and do some paper modeling so the classes are set up well

    private final String playerToReplaceName;

    private final BufferedImage runImage;
    private final BufferedImage fishingImage;
    private final Palette overworldReflectionPalette;

    private final BufferedImage mapIconImage;

    public Gen3PlayerCharacterGraphics(String name, String playerToReplaceName) {
        super(name);
        this.playerToReplaceName = playerToReplaceName;

        // TODO: size check these images
        // TODO: auto-reindex the palettes if needed

        // TODO: support pokeruby-style combined "normal.png" and pokeemerald-style split "walking.png", "running.png"
        setFrontImage(loadImage("front_pic.png"));
        setBackImage(loadImage("back_pic.png"));
        setWalkImage(loadImage("walking.png"));
        setBikeImage(loadImage("mach_bike.png"));

        this.runImage = loadImage("running.png");
        this.fishingImage = loadImage("fishing.png");
        this.overworldReflectionPalette = loadPalette("reflection.pal");

        this.mapIconImage = loadImage("icon.png");
    }

    public String getPlayerToReplaceName() {
        return playerToReplaceName;
    }

    public BufferedImage getRunImage() {
        return runImage;
    }

    public BufferedImage getFishingImage() {
        return fishingImage;
    }

    public Palette getOverworldNormalPalette() {
        BufferedImage walk = getWalkImage();
        return Palette.readImagePalette(walk);
    }

    public Palette getOverworldReflectionPalette() {
        return overworldReflectionPalette != null ? overworldReflectionPalette : getOverworldNormalPalette();
    }

    public BufferedImage getMapIconImage() {
        return mapIconImage;
    }

    public Palette getMapIconPalette() {
        BufferedImage walk = getMapIconImage();
        return Palette.readImagePalette(walk);
    }

    @Override
    public BufferedImage[] getSampleImages() {
        return new BufferedImage[] {getFrontImage(), getWalkImage()};
    }
}
