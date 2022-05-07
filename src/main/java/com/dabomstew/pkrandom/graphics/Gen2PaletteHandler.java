package com.dabomstew.pkrandom.graphics;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Type;
import com.dabomstew.pkrandom.romhandlers.BasePokemonAction;
import com.dabomstew.pkrandom.romhandlers.CopyUpEvolutionsHelper;
import com.dabomstew.pkrandom.romhandlers.EvolvedPokemonAction;

public class Gen2PaletteHandler extends PaletteHandler {

	private static final Map<Type, Color[]> BRIGHT_TYPE_COLORS = initBrightTypeColors();
	private static final Map<Type, Color[]> DARK_TYPE_COLORS = initDarkTypeColors();
	private static final Color DEFAULT_BRIGHT_COLOR = new Color(0xC0C0C0);
	private static final Color DEFAULT_DARK_COLOR = new Color(0x808080);

	private boolean typeSanity;
	private boolean shinyFromNormal;

	public Gen2PaletteHandler(Random random) {
		super(random);
	}

	private static EnumMap<Type, Color[]> initBrightTypeColors() {
		EnumMap<Type, Color[]> brightTypeColors = new EnumMap<>(Type.class);
		brightTypeColors.put(Type.NORMAL, Color.colorArrayFromHexes(new int[] { 0xF7DE6B, 0xFF7BBD, 0xFF5263, 0xEFB539,
				0xFF7394, 0xFF7394, 0xFF73AD, 0xFF73AD, 0xF7D65A, 0xF7DE42, 0xBD6342, 0xAD6B4A, 0x634231, 0xFF5294,
				0xDE9CBD, 0x8C6318, 0xCE7B29, 0xC68431, 0xC64A21, 0x427BA5, 0xDE9473, 0xBDAD21, 0xFF6BCE, 0xFF84C6,
				0xF7D65A, 0xEFBD63, 0x7B7B84, 0xE7736B, 0xE784B5, 0xFFFF31, 0xBD7B39, 0xBDAD42, 0xDE7B94, 0xE76373 }));
		brightTypeColors.put(Type.FIGHTING, Color.colorArrayFromHexes(new int[] { 0xE7944A, 0xF78452, 0xA58C5A,
				0x848C5A, 0xAD945A, 0xB57329, 0xAD7B63, 0xBD7B94, 0xD67B94, 0xBD8C18 }));
		brightTypeColors.put(Type.FLYING, Color.colorArrayFromHexes(new int[] { 0x94CEFF, 0x637B9C, 0x8C9CFF, 0xBDA5F7,
				0x9494EF, 0xE7BD6B, 0xC6DEFF, 0xB5B5BD, 0xADE7FF, 0x73DEC6 }));
		brightTypeColors.put(Type.POISON, Color.colorArrayFromHexes(new int[] { 0xA542AD, 0xEFAD00, 0x8CA5F7, 0xDE42AD,
				0xB542B5, 0x9431A5, 0x9442AD, 0xDE52C6, 0xEF10A5, 0xEF10A5, 0xC642C6, 0xCE52CE, 0x9442AD, 0xDEEF5A }));
		brightTypeColors.put(Type.GROUND, Color.colorArrayFromHexes(new int[] { 0xB59431, 0xBD9400, 0x8C4A18, 0x8C4A18,
				0x948442, 0xA59C63, 0xA59C63, 0xC66B6B, 0xBD9473, 0x63C608 }));
		brightTypeColors.put(Type.ROCK,
				Color.colorArrayFromHexes(new int[] { 0x948C7B, 0x8C8C94, 0x948C7B, 0x8C7BA5, 0x7B5A8C, 0x7B5A8C,
						0xA58C5A, 0xDEB55A, 0xBD7B5A, 0xBD7B5A, 0xAD7B94, 0x945A42, 0xFF5AFF, 0x7BA5E7, 0x84E700 }));
		brightTypeColors.put(Type.BUG, Color.colorArrayFromHexes(new int[] { 0x7BFF00, 0xD68C29, 0xD6CE18, 0xFFD631,
				0x7BD600, 0xD69C42, 0xFFB521, 0xE7AD29, 0xDE5242, 0x4AD652, 0xFFB529, 0x524A9C }));
		brightTypeColors.put(Type.GHOST, Color.colorArrayFromHexes(
				new int[] { 0xF742F7, 0xF742F7, 0xF742F7, 0xF7319C, 0xA5ADD6, 0x947BAD, 0x9C84FF }));
		brightTypeColors.put(Type.STEEL,
				Color.colorArrayFromHexes(new int[] { 0xDECECE, 0xA5B5B5, 0xBD4ACE, 0x8C7BD6, 0xD64242, 0x8C94C6 }));
		brightTypeColors.put(Type.FIRE,
				Color.colorArrayFromHexes(new int[] { 0xFF6B21, 0xF74231, 0xFF5A00, 0xFF6339, 0xFFCE4A, 0xFFBD39,
						0xF7BD39, 0xFF9C00, 0xF7E700, 0xFFA500, 0xFF5208, 0xFFB500, 0xFFDE00, 0xFFE721, 0xFFA521,
						0xEFDE6B, 0xD6394A, 0xF75200, 0xFF6300, 0xFF944A, 0xC67300, 0xFF3918 }));
		brightTypeColors.put(Type.WATER,
				Color.colorArrayFromHexes(new int[] { 0x639CFF, 0x5273F7, 0x425AFF, 0xAD9CCE, 0x8484D6, 0x8484D6,
						0x5AA5FF, 0x5AA5FF, 0x4A7BF7, 0x3973F7, 0x4A73D6, 0x4284F7, 0x84B5FF, 0x3973AD, 0x638CD6,
						0x52AD94, 0x428C6B, 0x4A6BEF, 0x527BC6, 0x84A5CE, 0x73ADE7, 0x847BEF, 0x295AFF, 0x5ABDFF }));
		brightTypeColors.put(Type.GRASS,
				Color.colorArrayFromHexes(new int[] { 0x63FF5A, 0x63FF5A, 0x63FF9C, 0x5ABD18, 0xFF6300, 0xFF3118,
						0xA5FF39, 0x6BFF39, 0x7BD618, 0x31D608, 0x08FFC6, 0xEFBD63, 0xDECE31, 0x6BC618, 0xDE4231,
						0x5ABD31, 0xFFB529, 0xEFE700, 0x52FF00 }));
		brightTypeColors.put(Type.ELECTRIC,
				Color.colorArrayFromHexes(new int[] { 0xEFD629, 0xFFD639, 0xFFFF29, 0xFFFF18, 0xFFE700, 0xEFC673,
						0xEFD629, 0xC6A539, 0xFFFF10, 0xFF4A42, 0xFF4A42, 0xFFFF29, 0xFFFF00, 0xA5DEEF }));
		brightTypeColors.put(Type.PSYCHIC,
				Color.colorArrayFromHexes(new int[] { 0xDE8C10, 0xE79C18, 0xE79C18, 0xFF528C, 0xFF528C, 0xFFBD21,
						0xFFCE00, 0xCEAD00, 0xFF5AFF, 0xEF319C, 0xB5A5CE, 0xFF7BFF, 0x52BD6B, 0x52BD6B, 0xCE7BD6,
						0xFF7394, 0x7B7B84, 0x63CEC6, 0xFFB521, 0xCE8CE7 }));
		brightTypeColors.put(Type.ICE,
				Color.colorArrayFromHexes(new int[] { 0x9CADFF, 0x9CADFF, 0x5AADFF, 0x8CC6B5, 0xBD9473 }));
		brightTypeColors.put(Type.DRAGON,
				Color.colorArrayFromHexes(new int[] { 0xE7D639, 0x8C9CFF, 0xC68C21, 0x4AD652 }));
		brightTypeColors.put(Type.DARK,
				Color.colorArrayFromHexes(new int[] { 0x637B9C, 0x8C7B00, 0xDE525A, 0xCE297B, 0x7B52DE }));
		return brightTypeColors;

	}

	private static EnumMap<Type, Color[]> initDarkTypeColors() {
		EnumMap<Type, Color[]> darkTypeColors = new EnumMap<>(Type.class);
		darkTypeColors.put(Type.NORMAL, Color.colorArrayFromHexes(new int[] { 0x9C4210, 0x9C4210, 0x9C4210, 0x944A00,
				0xA54A00, 0xB58431, 0x6B3900, 0x6B3900, 0x943110, 0x523918, 0x634231, 0x944231, 0xFF42AD, 0x313108,
				0x523921, 0x8C4210, 0xC64A21, 0x427BA5, 0x6B4A4A, 0x734A10, 0x6B3900, 0xB52929, 0xFF318C, 0xE73931,
				0x632173, 0x393942, 0x39526B, 0x8C4A5A, 0xCE4A52, 0x5263C6, 0x212984, 0x732931, 0x31316B, 0x942942 }));
		darkTypeColors.put(Type.FIGHTING, Color.colorArrayFromHexes(
				new int[] { 0x8C3929, 0x733121, 0x4A5A21, 0x4A5A21, 0x7B847B, 0xDE1894, 0x8C3918, 0x42429C }));
		darkTypeColors.put(Type.FLYING, Color.colorArrayFromHexes(new int[] { 0x29428C, 0x2973BD, 0x949CDE, 0x214A7B,
				0x395ACE, 0x4A42C6, 0x4239A5, 0x5200FF, 0x7373AD, 0x5A94CE }));
		darkTypeColors.put(Type.POISON,
				Color.colorArrayFromHexes(
						new int[] { 0x6B2973, 0x8C1084, 0x522984, 0x5A215A, 0x314A4A, 0x424A6B, 0x424A6B, 0x630073,
								0x5A6373, 0x630863, 0x630863, 0x842184, 0x5A2973, 0x4AA529, 0x7331A5, 0x214A7B }));
		darkTypeColors.put(Type.GROUND, Color.colorArrayFromHexes(new int[] { 0x633908, 0x6B3900, 0xBD2118, 0xBD2118,
				0x845221, 0x845221, 0x394A21, 0x394A21, 0x9C3121, 0x525252, 0x7B3131 }));
		darkTypeColors.put(Type.ROCK, Color.colorArrayFromHexes(new int[] { 0x425A39, 0x425A39, 0x425A39, 0x4A315A,
				0x394A21, 0x394A21, 0x735A42, 0x735A42, 0x6B5A42, 0x317339, 0xE7396B, 0x7B3131 }));
		darkTypeColors.put(Type.BUG, Color.colorArrayFromHexes(new int[] { 0x63B531, 0x4A7300, 0xA57342, 0xBDCE00,
				0xDE3129, 0xDE3129, 0x4AA529, 0xA53939, 0x211884 }));
		darkTypeColors.put(Type.GHOST,
				Color.colorArrayFromHexes(new int[] { 0x8C00BD, 0x630084, 0x4A0084, 0x4A4A8C, 0x63AD29 }));
		darkTypeColors.put(Type.STEEL, Color.colorArrayFromHexes(
				new int[] { 0x5A4A9C, 0x7B8CC6, 0x7B8CC6, 0x528CA5, 0x8C0839, 0x634263, 0x39426B, 0x213952 }));
		darkTypeColors.put(Type.FIRE,
				Color.colorArrayFromHexes(new int[] { 0xB52929, 0x941839, 0x314A7B, 0xAD2942, 0xC64200, 0xC63118,
						0xFF5A18, 0xFF5A18, 0xBD3931, 0xAD2910, 0xFF6318, 0xFF3929, 0xFF4A18, 0xFF4A31, 0xBD2118,
						0x52427B, 0xBD105A, 0xAD0000, 0x397B18 }));
		darkTypeColors.put(Type.WATER,
				Color.colorArrayFromHexes(new int[] { 0xB59442, 0xB59442, 0xC6A518, 0x314ABD, 0x42297B, 0x42297B,
						0x31317B, 0x638C5A, 0x9CB5F7, 0xBD8C5A, 0x4A5AFF, 0xA58C5A, 0x4A5ABD, 0xE74A21, 0xC64A52,
						0x428C6B, 0x943908, 0x6B52E7, 0x6B52E7, 0x213184, 0x29426B, 0x18319C, 0xA58494, 0x7310A5 }));
		darkTypeColors.put(Type.GRASS, Color.colorArrayFromHexes(new int[] { 0xFF5231, 0xFF638C, 0xFF4A9C, 0x298439,
				0xA56B31, 0x18A500, 0x5A8410, 0xAD0852, 0x318C42, 0x317339, 0x429439, 0x6B9442, 0x52AD00 }));
		darkTypeColors.put(Type.ELECTRIC, Color.colorArrayFromHexes(new int[] { 0xFF6300, 0xC68429, 0xE75A00, 0xBD8400,
				0xDEB539, 0xD63100, 0x52299C, 0xAD5208, 0x7B5218 }));
		darkTypeColors.put(Type.PSYCHIC,
				Color.colorArrayFromHexes(new int[] { 0x523142, 0x634252, 0x634252, 0xAD2942, 0x945263, 0xAD6300,
						0x8C2984, 0xE7396B, 0x73107B, 0x8429A5, 0x395AD6, 0xA52931, 0xA52931, 0x7B2994, 0xBD2910,
						0x393942, 0x4A8463, 0xE75284, 0xA5089C }));
		darkTypeColors.put(Type.ICE, Color.colorArrayFromHexes(new int[] { 0x425A94, 0x425A84, 0x9C73F7 }));
		darkTypeColors.put(Type.DRAGON,
				Color.colorArrayFromHexes(new int[] { 0x4273C6, 0x2973BD, 0x5A528C, 0xA53939, 0xFF525A }));
		darkTypeColors.put(Type.DARK, Color.colorArrayFromHexes(
				new int[] { 0x29428C, 0x313929, 0x39398C, 0x39395A, 0x213142, 0x4A218C, 0xCE3984 }));
		return darkTypeColors;
	}

	@Override
	public void randomizePokemonPalettes(CopyUpEvolutionsHelper copyUpEvolutionsHelper, boolean typeSanity,
			boolean evolutionSanity, boolean shinyFromNormal) {

		this.typeSanity = typeSanity;
		this.shinyFromNormal = shinyFromNormal;
		copyUpEvolutionsHelper.apply(evolutionSanity, new BasePokemonPaletteAction(),
				new EvolvedPokemonPaletteAction());

	}

	private Palette getRandom2ColorPalette() {
		Palette palette = new Palette(2);
		palette.setColor(0, getRandomBrightColor());
		palette.setColor(1, getRandomDarkColor());
		return palette;
	}

	private Palette getRandom2ColorPalette(Type primaryType, Type secondaryType) {
		Palette palette = new Palette(2);
		Color brightColor = getRandomBrightColor(primaryType);
		Color darkColor = getRandomDarkColor(secondaryType == null ? primaryType : secondaryType);
		palette.setColor(0, brightColor);
		palette.setColor(1, darkColor);
		return palette;
	}
	
	private Color getRandomBrightColor() {
		Type[] keys = BRIGHT_TYPE_COLORS.keySet().toArray(new Type[0]);
		Type type = keys[random.nextInt(keys.length)];
		return getRandomBrightColor(type);
	}

	private Color getRandomBrightColor(Type type) {
		Color[] typeColors = BRIGHT_TYPE_COLORS.get(type);
		Color color = typeColors == null ? DEFAULT_BRIGHT_COLOR : typeColors[random.nextInt(typeColors.length)];
		return color;
	}
	
	private Color getRandomDarkColor() {
		Type[] keys = DARK_TYPE_COLORS.keySet().toArray(new Type[0]);
		Type type = keys[random.nextInt(keys.length)];
		return getRandomDarkColor(type);
	}

	private Color getRandomDarkColor(Type type) {
		Color[] typeColors = DARK_TYPE_COLORS.get(type);
		Color color = typeColors == null ? DEFAULT_DARK_COLOR : typeColors[random.nextInt(typeColors.length)];
		return color;
	}

	private class BasePokemonPaletteAction implements BasePokemonAction {

		@Override
		public void applyTo(Pokemon pk) {
			if (shinyFromNormal) {
				setShinyPaletteFromNormal(pk);
			}

			pk.setNormalPalette(typeSanity ? getRandom2ColorPalette(pk.getPrimaryType(), pk.getSecondaryType())
					: getRandom2ColorPalette());
		}

	}

	private class EvolvedPokemonPaletteAction implements EvolvedPokemonAction {
		// TODO: figure out what "dontCopyPokes" is
		@Override
		public void applyTo(Pokemon evFrom, Pokemon evTo, boolean toMonIsFinalEvo) {
			if (shinyFromNormal) {
				setShinyPaletteFromNormal(evTo);
			}

			Palette palette = evFrom.getNormalPalette().clone();

			if (typeSanity) {
				if (evTo.getPrimaryType() != evFrom.getPrimaryType()) {
					Color newBrightColor = getRandomBrightColor(evTo.getPrimaryType());
					palette.setColor(0, newBrightColor);
	
				} else if (evTo.getSecondaryType() != evFrom.getSecondaryType()) {
					Color newDarkColor = getRandomDarkColor(
							evTo.getSecondaryType() == null ? evTo.getPrimaryType() : evTo.getSecondaryType());
					palette.setColor(1, newDarkColor);
				}
			}

			evTo.setNormalPalette(palette);
		}

	}

}
