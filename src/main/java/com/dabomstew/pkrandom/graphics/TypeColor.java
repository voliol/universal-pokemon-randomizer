package com.dabomstew.pkrandom.graphics;

import java.util.Map;

import com.dabomstew.pkrandom.pokemon.Type;

public class TypeColor extends Color {

	public static void putIntsAsTypeColors(Map<Type, TypeColor[]> map, Type type, int[] ints) {
		TypeColor[] typeColors = new TypeColor[ints.length];
		for (int i = 0; i < typeColors.length; i++) {
			typeColors[i] = new TypeColor(ints[i], type);
		}
		map.put(type, typeColors);
	}

	private Type type;

	public TypeColor(int hex, Type type) {
		super(hex);
		this.type = type;
	}
	
	public TypeColor(Color untyped, Type type) {
		super(untyped.getComp(0), untyped.getComp(1), untyped.getComp(2));
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return type + "-" + super.toString();
	}

}
