package com.dabomstew.pkrandom.graphics;

/*----------------------------------------------------------------------------*/
/*--  Color.java - represents a RGB color.             						--*/
/*--                                                                        --*/
/*--  Part of "Universal Pokemon Randomizer" by Dabomstew                   --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2012.                                 --*/
/*--                                                                        --*/
/*--  The custom code written here is licensed under the terms of the GPL:  --*/
/*--                                                                        --*/
/*--  This program is free software: you can redistribute it and/or modify  --*/
/*--  it under the terms of the GNU General Public License as published by  --*/
/*--  the Free Software Foundation, either version 3 of the License, or     --*/
/*--  (at your option) any later version.                                   --*/
/*--                                                                        --*/
/*--  This program is distributed in the hope that it will be useful,       --*/
/*--  but WITHOUT ANY WARRANTY; without even the implied warranty of        --*/
/*--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          --*/
/*--  GNU General Public License for more details.                          --*/
/*--                                                                        --*/
/*--  You should have received a copy of the GNU General Public License     --*/
/*--  along with this program. If not, see <http://www.gnu.org/licenses/>.  --*/
/*----------------------------------------------------------------------------*/

public class Color implements Cloneable {
    private int r, g, b;
    
    public static int highColorWordToARGB(int word) {
	    int red = (int) ((word & 0x1F) * 8.25);
	    int green = (int) (((word & 0x3E0) >> 5) * 8.25);
	    int blue = (int) (((word & 0x7C00) >> 10) * 8.25);
	    return 0xFF000000 | (red << 16) | (green << 8) | blue;
	}

	public Color() {
        this(255, 255, 255);
    }

    public Color(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public Color(int hex) {
        this.r = (hex & 0xFF0000) >> 16;
        this.g = (hex & 0xFF00) >> 8;
        this.b = (hex & 0xFF);
    }

    @Override
    public Color clone() {
    	return new Color(r, g, b);
    }

    @Override
    public String toString() {
        return "(" + r + ", " + g + ", " + b + ")";
    }

    public byte[] toBytes() {
        int red = Math.floorDiv(r, 8);
        int green = Math.floorDiv(g, 8);
        int blue = Math.floorDiv(b, 8);
        int bytesSum = blue * 1024 + red + green * 32;
        return new byte[] { (byte) bytesSum, (byte) Math.floorDiv(bytesSum, 256) };
    }
    
    public int toARGB() {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public int[] toInts() {
        return new int[] { r, g, b };
    }

    public int getComp(int i) {
        switch (i) {
        case 0:
            return r;
        case 1:
            return g;
        case 2:
            return b;
        default:
            throw new IndexOutOfBoundsException(i + " out of bounds for RGB color (0=r, 1=g, 2=b).");
        }
    }

    public void setComp(int i, int value) {
        switch (i) {
        case 0:
            r = value;
            break;
        case 1:
            g = value;
            break;
        case 2:
            b = value;
            break;
        default:
            throw new IndexOutOfBoundsException(i + " out of bounds for RGB color (0=r, 1=g, 2=b).");
		}
	}

}
