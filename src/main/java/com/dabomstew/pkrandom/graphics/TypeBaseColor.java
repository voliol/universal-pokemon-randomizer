package com.dabomstew.pkrandom.graphics;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.dabomstew.pkrandom.pokemon.Type;

public class TypeBaseColor {

    // TODO: make this work with the rest of the Type enums (like fairy),
    // while also returning the sort of shuffled list PokemonTypeBaseColors wants
    // (or something facilitating PokemonTypeBaseColors making it itself)

    private static final Map<Type, TypeBaseColor[]> TYPE_BASE_COLORS = initTypeBaseColors();

    private static Map<Type, TypeBaseColor[]> initTypeBaseColors() {
        Map<Type, TypeBaseColor[]> map = new EnumMap<>(Type.class);
        // vanilla types
        putIntsAsTBCs(map, Type.NORMAL, new int[] { 0xFF9DE7, 0xCB95FD, 0xC59A8B });
        putIntsAsTBCs(map, Type.FIGHTING, new int[] { 0xA46A44, 0xC9656F, 0xEBA65A });
        putIntsAsTBCs(map, Type.FLYING, new int[] { 0x77B7B7, 0x71A5FB, 0x8E5940 });
        putIntsAsTBCs(map, Type.BUG, new int[] { 0x9FF04F, 0x95AD3F, 0xEAEA00 });
        putIntsAsTBCs(map, Type.POISON, new int[] { 0xEC0DD7, 0x24FF24, 0x9787B8 });
        putIntsAsTBCs(map, Type.ROCK, new int[] { 0x92B685, 0x6C788C, 0x928167 });
        putIntsAsTBCs(map, Type.GROUND, new int[] { 0xD8AC7C, 0xA8A277, 0xD3A35A });
        putIntsAsTBCs(map, Type.DARK, new int[] { 0x4A4A4A, 0x0000BB, 0x920303 });
        putIntsAsTBCs(map, Type.STEEL, new int[] { 0xC0C0C0, 0xE4871F, 0xE25221 });
        putIntsAsTBCs(map, Type.ICE, new int[] { 0x82FFE6, 0xC4D0D2, 0x7ABAFA });
        putIntsAsTBCs(map, Type.WATER, new int[] { 0x4045FF, 0x00AAAA, 0x61D1A5 });
        putIntsAsTBCs(map, Type.FIRE, new int[] { 0xFF822F, 0xEE0B0B, 0xFFD52B });
        putIntsAsTBCs(map, Type.GRASS, new int[] { 0x00B700, 0x4E9131, 0xD6C132 });
        putIntsAsTBCs(map, Type.PSYCHIC, new int[] { 0xC54DF2, 0xCE609F, 0x8230B8 });
        putIntsAsTBCs(map, Type.GHOST, new int[] { 0x8729FA, 0x42448A, 0x42448A });
        putIntsAsTBCs(map, Type.ELECTRIC, new int[] { 0xFBF259, 0x20FFFF, 0xFE1818 });
        putIntsAsTBCs(map, Type.DRAGON, new int[] { 0xD83D41, 0x8C3535, 0x8C3535 });
        // hack types
        return map;
    }

    private static void putIntsAsTBCs(Map<Type, TypeBaseColor[]> map, Type type, int[] ints) {
        TypeBaseColor[] tbc = new TypeBaseColor[ints.length];
        for (int i = 0; i < tbc.length; i++) {
            tbc[i] = new TypeBaseColor(ints[i], type);
        }
        map.put(type, tbc);
    }

    private Color color;
    private Type type;

    public TypeBaseColor(int hexColor, Type type) {
        this.color = new Color(hexColor);
        this.type = type;
    }

    public static List<TypeBaseColor> getTypeBaseColors() {
        List<TypeBaseColor> list = new ArrayList<>();
        for (TypeBaseColor[] tbcs : TYPE_BASE_COLORS.values()) {
            for (TypeBaseColor tbc : tbcs) {
                list.add(tbc);
            }
        }
        return list;
    }

    public Color getColor() {
        return color.clone();
    }

    public boolean hasSameTypeAs(TypeBaseColor other) {
        return type == other.type;
    }

    public boolean hasType(Type type) {
        return this.type == type;
    }

    @Override
    public String toString() {
        return type + "-" + color;
    }

}
