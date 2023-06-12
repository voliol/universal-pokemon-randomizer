package com.dabomstew.pkrandom.graphics.packs;

import java.awt.image.BufferedImage;

public abstract class PlayerCharacterGraphics extends OldGraphicsPack {

	private static final String PLAYERS_DIR_PATH = "players";

	private BufferedImage frontImage = null;
	private BufferedImage backImage = null;
	private BufferedImage walkImage = null;
	private BufferedImage bikeImage = null;

	public PlayerCharacterGraphics(String name) {
		super(PLAYERS_DIR_PATH + "/" + name);
	}

	public BufferedImage getFrontImage() {
		return frontImage;
	}

	protected void setFrontImage(BufferedImage frontImage) {
		this.frontImage = frontImage;
	}

	public BufferedImage getBackImage() {
		return backImage;
	}

	protected void setBackImage(BufferedImage backImage) {
		this.backImage = backImage;
	}

	public BufferedImage getWalkImage() {
		return walkImage;
	}

	protected void setWalkImage(BufferedImage walkImage) {
		this.walkImage = walkImage;
	}

	public BufferedImage getBikeImage() {
		return bikeImage;
	}

	protected void setBikeImage(BufferedImage bikeImage) {
		this.bikeImage = bikeImage;
	}

}
