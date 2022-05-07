package com.dabomstew.pkrandom.graphics;

import java.util.Arrays;

public class Palette implements Cloneable {

    private final static int DEFAULT_PALETTE_SIZE = 16;

    private Color[] colors;

    public Palette() {
        this(DEFAULT_PALETTE_SIZE);
    }

    public Palette(int size) {
        this.colors = new Color[size];
        for (int i = 0; i < size; i++) {
            this.colors[i] = new Color();
        }
    }

    public Palette(int size, Color color) {
        this.colors = new Color[size];
        for (int i = 0; i < size; i++) {
            this.colors[i] = color;
        }
    }

    public Palette(Color[] colors) {
        this.colors = colors;
    }

    public Palette(int[] RGBValues) {
        this.colors = new Color[RGBValues.length];
        for (int i = 0; i < colors.length; i++) {
            this.colors[i] = new Color(RGBValues[i]);
        }
    }

    public Palette(byte[] bytes) {
        this(bytesToARGBValues(bytes));
    }

    private static int[] bytesToARGBValues(byte[] bytes) {
        int[] RGBValues = new int[bytes.length / 2];
        for (int i = 0; i < RGBValues.length; i++) {
            int word = (bytes[i * 2] & 0xFF) + ((bytes[i * 2 + 1] & 0xFF) << 8);
            RGBValues[i] = Color.highColorWordToARGB(word);
        }
        return RGBValues;
    }

    public Color getColor(int i) {
        return colors[i];
    }

    public void setColor(int i, Color c) {
        colors[i] = c;
    }

    public byte[] toBytes() {
        byte[] bytes = new byte[colors.length * 2];
        for (int i = 0; i < colors.length; i++) {
            byte[] colorBytes = colors[i].toBytes();
            bytes[i * 2] = colorBytes[0];
            bytes[i * 2 + 1] = colorBytes[1];
        }
        return bytes;
    }

    public int[] toARGB() {
        int[] ARGB = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            ARGB[i] = colors[i].toARGB();
        }
        return ARGB;
    }

    public int size() {
        return colors.length;
    }
    
    @Override
    public String toString() {
    	return Arrays.toString(colors);
    }
    
    @Override
    public Palette clone() {
    	Palette palette = new Palette(colors.length);
    	for (int i = 0; i < colors.length; i++) {
    		palette.setColor(i, colors[i].clone());
    	}
    	return palette;
    }

}
