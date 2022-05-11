package com.dabomstew.pkrandom.graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.dabomstew.pkrandom.pokemon.Type;

public class Gen3to5TypeColors {
	
	// TODO: make this work with the rest of the Type enums (like fairy),
	// while also returning the sort of shuffled list PokemonTypeBaseColors wants
	// (or something facilitating PokemonTypeBaseColors making it itself)

	private static final Map<Type, TypeColor[]> TYPE_BASE_COLORS = initTypeBaseColors();

	private static Map<Type, TypeColor[]> initTypeBaseColors() {
		Map<Type, TypeColor[]> map = new EnumMap<>(Type.class);
		// vanilla types
		TypeColor.putIntsAsTypeColors(map, Type.NORMAL, new int[] { 0xFF9DE7, 0xCB95FD, 0xC59A8B });
		TypeColor.putIntsAsTypeColors(map, Type.FIGHTING, new int[] { 0xA46A44, 0xC9656F, 0xEBA65A });
		TypeColor.putIntsAsTypeColors(map, Type.FLYING, new int[] { 0x77B7B7, 0x71A5FB, 0x8E5940 });
		TypeColor.putIntsAsTypeColors(map, Type.BUG, new int[] { 0x9FF04F, 0x95AD3F, 0xEAEA00 });
		TypeColor.putIntsAsTypeColors(map, Type.POISON, new int[] { 0xEC0DD7, 0x24FF24, 0x9787B8 });
		TypeColor.putIntsAsTypeColors(map, Type.ROCK, new int[] { 0x92B685, 0x6C788C, 0x928167 });
		TypeColor.putIntsAsTypeColors(map, Type.GROUND, new int[] { 0xD8AC7C, 0xA8A277, 0xD3A35A });
		TypeColor.putIntsAsTypeColors(map, Type.DARK, new int[] { 0x4A4A4A, 0x0000BB, 0x920303 });
		TypeColor.putIntsAsTypeColors(map, Type.STEEL, new int[] { 0xC0C0C0, 0xE4871F, 0xE25221 });
		TypeColor.putIntsAsTypeColors(map, Type.ICE, new int[] { 0x82FFE6, 0xC4D0D2, 0x7ABAFA });
		TypeColor.putIntsAsTypeColors(map, Type.WATER, new int[] { 0x4045FF, 0x00AAAA, 0x61D1A5 });
		TypeColor.putIntsAsTypeColors(map, Type.FIRE, new int[] { 0xFF822F, 0xEE0B0B, 0xFFD52B });
		TypeColor.putIntsAsTypeColors(map, Type.GRASS, new int[] { 0x00B700, 0x4E9131, 0xD6C132 });
		TypeColor.putIntsAsTypeColors(map, Type.PSYCHIC, new int[] { 0xC54DF2, 0xCE609F, 0x8230B8 });
		TypeColor.putIntsAsTypeColors(map, Type.GHOST, new int[] { 0x8729FA, 0x42448A, 0x42448A });
		TypeColor.putIntsAsTypeColors(map, Type.ELECTRIC, new int[] { 0xFBF259, 0x20FFFF, 0xFE1818 });
		TypeColor.putIntsAsTypeColors(map, Type.DRAGON, new int[] { 0xD83D41, 0x8C3535, 0x8C3535 });
		// hack types
		return map;
	}
	
	// preliminary
	public static List<TypeColor> getAllTypeColors() {
		List<TypeColor> allTypeColors = new ArrayList<>();
		for (TypeColor[] typeColors : TYPE_BASE_COLORS.values()) {
			allTypeColors.addAll(Arrays.asList(typeColors));
		}
		return allTypeColors;
	}

}
