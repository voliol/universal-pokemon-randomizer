package com.dabomstew.pkrandom.graphics.packs;

import com.dabomstew.pkrandom.GFXFunctions;
import com.dabomstew.pkrandom.graphics.palettes.Palette;

import java.awt.image.BufferedImage;

// TODO: does it even make sense to have a unified class for all of Gen3? Probably not?
public class Gen3PlayerCharacterGraphics extends PlayerCharacterGraphics {

	// TODO: are these correct/indicative?
	private static final int SMALL_SPRITE_WIDTH = 16;
	private static final int SMALL_SPRITE_HEIGHT = 16;
	private static final int MEDIUM_SPRITE_WIDTH = 16;
	private static final int MEDIUM_SPRITE_HEIGHT = 32;

	private final String playerToReplaceName;

	private BufferedImage runImage = null;
	private BufferedImage acroBikeImage = null;
	// TODO: standardize names, should either be "run, surf" or "running, surfing"
	// TODO: should "surfing" rather be "sitting"?
	private BufferedImage surfingImage = null;
	private BufferedImage fieldMoveImage = null;
	private BufferedImage fishingImage = null;
	private BufferedImage wateringImage = null;
	private BufferedImage decoratingImage = null;

	private BufferedImage underwaterImage = null;

	private Palette overworldReflectionPalette = null;

	private final BufferedImage mapIconImage;

	public Gen3PlayerCharacterGraphics(String name, String playerToReplaceName) {
		super(name);
		this.playerToReplaceName = playerToReplaceName;

		// TODO: size check these images
		// TODO: auto-reindex the palettes if needed

		setFrontImage(loadImage("front_pic.png"));
		setBackImage(loadImage("back_pic.png"));

		loadWalkAndRunImages();
		loadBikeImages();

		this.surfingImage = loadImage("surfing.png");
		this.fieldMoveImage = loadImage("field_move.png");
		this.fishingImage = loadImage("fishing.png");
		this.wateringImage = loadImage("watering.png");
		this.decoratingImage = loadImage("decorating.png", MEDIUM_SPRITE_WIDTH, MEDIUM_SPRITE_HEIGHT);

		this.overworldReflectionPalette = loadPalette("reflection.pal");

		this.underwaterImage = loadImage("underwater.png");

		this.mapIconImage = loadImage("icon.png", SMALL_SPRITE_WIDTH, SMALL_SPRITE_HEIGHT);
	}

	private void loadWalkAndRunImages() {
		// TODO: support vertically laid out sources
		// TODO: support whatever pokefirered has, with combined surfing/running
		BufferedImage normal = loadImage("normal.png");
		BufferedImage walk, run;
		if (normal != null) {
			walk = normal.getSubimage(0, 0, 144, 32); // pokeruby style
			run = normal.getSubimage(144, 0, 144, 32);
		} else {
			walk = loadImage("walking.png"); // pokeemerald style
			run = loadImage("running.png");
		}
		setWalkImage(walk);
		this.runImage = run;
	}

	private void loadBikeImages() {
		BufferedImage mach = loadImage("mach_bike.png"); // pokeruby/pokeemerald style
		if (mach == null) {
			mach = loadImage("bike.png"); // pokefirered style
		}
		setBikeImage(mach);
		this.acroBikeImage = loadImage("acro_bike.png");
	}

	public String getPlayerToReplaceName() {
		return playerToReplaceName;
	}

	public BufferedImage getRunImage() {
		return runImage;
	}

	/**
	 * Alias for getBikeImage().
	 */
	public BufferedImage getMachBikeImage() {
		return getBikeImage();
	}

	public BufferedImage getAcroBikeImage() {
		return acroBikeImage;
	}

	public BufferedImage getFieldMoveImage() {
		return fieldMoveImage;
	}

	public BufferedImage getFishingImage() {
		return fishingImage;
	}

	public BufferedImage getWateringImage() {
		return wateringImage;
	}

	public BufferedImage getDecoratingImage() {
		return decoratingImage;
	}

	public Palette getOverworldNormalPalette() {
		BufferedImage walk = getWalkImage();
		return Palette.readImagePalette(walk);
	}

	public Palette getOverworldReflectionPalette() {
		// TODO: auto-soften reflection if not pre-set
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
		return new BufferedImage[] { getFrontImage(), getWalkImage() };
	}

	/**
	 * Gets the frames of the player surfing/sitting, as an array of
	 * {@link BufferedImage}s. The array may contain duplicate and/or null values.
	 * The order is:<br>
	 * [down_0, up_0, side_0, down_1, up_1, side_1, down_2, up_2, side_2, down_jump,
	 * up_jump, side_jump]
	 */
	public BufferedImage[] getSurfingImages() {
		BufferedImage[] frames;
		BufferedImage[] split = GFXFunctions.splitImage(surfingImage, 32, 32);
		if (split.length == 12) { // all frames in the order given above
			frames = split;
		} else if (split.length == 6) { // [down_012, down_jump, up_012, up_jump, side_012, side_jump]
			frames = new BufferedImage[] { 
					split[0], split[2], split[4], split[0], split[2], split[4], 
					split[0], split[2], split[4], split[1], split[3], split[5] };
		} else {
			frames = null;
		}
		return frames;
	}

	/**
	 * Gets the frames of the player underwater, as an array of
	 * {@link BufferedImage}s. The array may contain duplicate and/or null values.
	 * The order is:<br>
	 * [down_0, up_0, side_0, down_1, up_1, side_1, down_2, up_2, side_2]
	 */
	public BufferedImage[] getUnderwaterImages() {
		// TODO
		return null;
	}

}
