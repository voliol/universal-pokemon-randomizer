package com.dabomstew.pkrandom.romhandlers;

import java.awt.image.BufferedImage;
/*----------------------------------------------------------------------------*/
/*--  AbstractDSRomHandler.java - a base class for DS rom handlers          --*/
/*--                              which standardises common DS functions.   --*/
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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.dabomstew.pkrandom.FileFunctions;
import com.dabomstew.pkrandom.GFXFunctions;
import com.dabomstew.pkrandom.exceptions.RandomizerIOException;
import com.dabomstew.pkrandom.graphics.Palette;
import com.dabomstew.pkrandom.newnds.NARCArchive;
import com.dabomstew.pkrandom.newnds.NDSRom;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Type;

public abstract class AbstractDSRomHandler extends AbstractRomHandler {

    private static final byte[] PALETTE_PREFIX_BYTES = { (byte) 0x52, (byte) 0x4C, (byte) 0x43, (byte) 0x4E,
            (byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x01, (byte) 0x48, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x54, (byte) 0x54, (byte) 0x4C, (byte) 0x50,
            (byte) 0x38, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x0A, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    protected String dataFolder;
    private NDSRom baseRom;
    private String loadedFN;

    public AbstractDSRomHandler(Random random) {
        super(random);
    }

    protected abstract boolean detectNDSRom(String ndsCode);

    @Override
    public boolean loadRom(String filename) {
        if (!this.detectNDSRom(getROMCodeFromFile(filename))) {
            return false;
        }
        // Load inner rom
        try {
            baseRom = new NDSRom(filename);
        } catch (IOException e) {
            throw new RandomizerIOException(e);
        }
        loadedFN = filename;
        loadedROM(baseRom.getCode());
        return true;
    }

    @Override
    public String loadedFilename() {
        return loadedFN;
    }

    protected byte[] get3byte(int amount) {
        byte[] ret = new byte[3];
        ret[0] = (byte) (amount & 0xFF);
        ret[1] = (byte) ((amount >> 8) & 0xFF);
        ret[2] = (byte) ((amount >> 16) & 0xFF);
        return ret;
    }

    protected abstract void loadedROM(String romCode);

    @Override
    public boolean saveRomFile(String filename) {
        try {
            baseRom.saveTo(filename);
        } catch (IOException e) {
            throw new RandomizerIOException(e);
        }
        return true;
    }

    public void closeInnerRom() throws IOException {
        baseRom.closeROM();
    }

    @Override
    public boolean canChangeStaticPokemon() {
        return false;
    }

    @Override
    public boolean hasPhysicalSpecialSplit() {
        // Default value for Gen4+.
        // Handlers can override again in case of ROM hacks etc.
        return true;
    }

    public NARCArchive readNARC(String subpath) throws IOException {
        return new NARCArchive(readFile(subpath));
    }

    public void writeNARC(String subpath, NARCArchive narc) throws IOException {
        this.writeFile(subpath, narc.getBytes());
    }

    protected static String getROMCodeFromFile(String filename) {
        try {
            FileInputStream fis = new FileInputStream(filename);
            fis.skip(0x0C);
            byte[] sig = FileFunctions.readFullyIntoBuffer(fis, 4);
            fis.close();
            String ndsCode = new String(sig, "US-ASCII");
            return ndsCode;
        } catch (IOException e) {
            throw new RandomizerIOException(e);
        }
    }

    protected boolean basicCPUEHackDetection() {
        int size128M = 0x8000000;
        if (baseRom.getLength() != size128M) {
            return true;
        }
        return !isChecksumEqual(1775642856L);
    }

    protected boolean basicIPGEHackDetection() {
        int size128M = 0x8000000;
        if (baseRom.getLength() != size128M) {
            return true;
        }
        return !isChecksumEqual(3639238800L);
    }

    protected boolean basicIPKEHackDetection() {
        int size128M = 0x8000000;
        if (baseRom.getLength() != size128M) {
            return true;
        }
        return !isChecksumEqual(3246432489L);
    }

    protected boolean basicIRBOHackDetection() {
        int size256M = 0x10000000;
        if (baseRom.getLength() != size256M) {
            return true;
        }
        return !isChecksumEqual(3804161561L);
    }

    protected boolean basicIRAOHackDetection() {
        int size256M = 0x10000000;
        if (baseRom.getLength() != size256M) {
            return true;
        }
        return !isChecksumEqual(3989655905L);
    }

    protected boolean basicIREOHackDetection() {
        int size512M = 0x20000000;
        if (baseRom.getLength() != size512M) {
            return true;
        }
        return !isChecksumEqual(632961702L);
    }

    protected boolean basicIRDOHackDetection() {
        int size512M = 0x20000000;
        if (baseRom.getLength() != size512M) {
            return true;
        }
        return !isChecksumEqual(2004791375L);
    }

    protected boolean isChecksumEqual(long checksum) {
        // Uncomment to find the checksum on your ROM
        // System.out.println(baseRom.getChecksum());
        return baseRom.getChecksum() == checksum;
    }

    protected int readByte(byte[] data, int offset) {
        return data[offset] & 0xFF;
    }

    protected int readWord(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    protected int readLong(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24);
    }

    protected int readRelativePointer(byte[] data, int offset) {
        return readLong(data, offset) + offset + 4;
    }

    protected void writeWord(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    protected void writeLong(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    protected void writeRelativePointer(byte[] data, int offset, int pointer) {
        int relPointer = pointer - (offset + 4);
        writeLong(data, offset, relPointer);
    }

    protected byte[] readFile(String location) throws IOException {
        return baseRom.getFile(location);
    }

    protected void writeFile(String location, byte[] data) throws IOException {
        writeFile(location, data, 0, data.length);
    }

    protected void writeFile(String location, byte[] data, int offset, int length)
            throws IOException {
        if (offset != 0 || length != data.length) {
            byte[] newData = new byte[length];
            System.arraycopy(data, offset, newData, 0, length);
            data = newData;
        }
        baseRom.writeFile(location, data);
    }

    protected byte[] readARM9() throws IOException {
        return baseRom.getARM9();
    }

    protected void writeARM9(byte[] data) throws IOException {
        baseRom.writeARM9(data);
    }

    protected byte[] readOverlay(int number) throws IOException {
        return baseRom.getOverlay(number);
    }

    protected void writeOverlay(int number, byte[] data) throws IOException {
        baseRom.writeOverlay(number, data);
    }

    protected void readByteIntoFlags(byte[] data, boolean[] flags, int offsetIntoFlags,
            int offsetIntoData) {
        int thisByte = data[offsetIntoData] & 0xFF;
        for (int i = 0; i < 8 && (i + offsetIntoFlags) < flags.length; i++) {
            flags[offsetIntoFlags + i] = ((thisByte >> i) & 0x01) == 0x01;
        }
    }

    protected byte getByteFromFlags(boolean[] flags, int offsetIntoFlags) {
        int thisByte = 0;
        for (int i = 0; i < 8 && (i + offsetIntoFlags) < flags.length; i++) {
            thisByte |= (flags[offsetIntoFlags + i] ? 1 : 0) << i;
        }
        return (byte) thisByte;
    }

    protected int typeTMPaletteNumber(Type t) {
        if (t == null) {
            return 411; // CURSE
        }
        switch (t) {
            case FIGHTING:
                return 398;
            case DRAGON:
                return 399;
            case WATER:
                return 400;
            case PSYCHIC:
                return 401;
            case NORMAL:
                return 402;
            case POISON:
                return 403;
            case ICE:
                return 404;
            case GRASS:
                return 405;
            case FIRE:
                return 406;
            case DARK:
                return 407;
            case STEEL:
                return 408;
            case ELECTRIC:
                return 409;
            case GROUND:
                return 410;
            case GHOST:
            default:
                return 411; // for CURSE
            case ROCK:
                return 412;
            case FLYING:
                return 413;
            case BUG:
                return 610;
        }
    }
    
    @Override
    public void loadPokemonPalettes() {
        try {
            String NARCpath = getNARCPath("PokemonGraphics");
            NARCArchive pokespritesNARC = readNARC(NARCpath);
            for (Pokemon pk : getPokemonWithoutNull()) {
                int normalPaletteIndex = calculatePokemonNormalPaletteIndex(pk.getNumber());
                pk.setNormalPalette(readPalette(pokespritesNARC, normalPaletteIndex));
                
                int shinyPaletteIndex = calculatePokemonShinyPaletteIndex(pk.getNumber());
                pk.setShinyPalette(readPalette(pokespritesNARC, shinyPaletteIndex));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    protected abstract int calculatePokemonNormalPaletteIndex(int i);
    
    protected abstract int calculatePokemonShinyPaletteIndex(int i);

    protected final Palette readPalette(NARCArchive NARC, int index) {
        byte[] withPrefixBytes = NARC.files.get(index);
        byte[] paletteBytes = Arrays.copyOfRange(withPrefixBytes, PALETTE_PREFIX_BYTES.length, withPrefixBytes.length);
        return new Palette(paletteBytes);
    }

    @Override
    public void writePokemonPalettes() {
        try {
            String NARCpath = getNARCPath("PokemonGraphics");
            NARCArchive pokeGraphicsNARC = readNARC(NARCpath);

            for (Pokemon pk : getPokemonWithoutNull()) {

                int normalPaletteIndex = calculatePokemonNormalPaletteIndex(pk.getNumber());
                byte[] normalPaletteBytes = pk.getNormalPalette().toBytes();
                normalPaletteBytes = concatenate(PALETTE_PREFIX_BYTES, normalPaletteBytes);
                pokeGraphicsNARC.getFiles().set(normalPaletteIndex, normalPaletteBytes);
                
                int shinyPaletteIndex = calculatePokemonShinyPaletteIndex(pk.getNumber());
                byte[] shinyPaletteBytes = pk.getShinyPalette().toBytes();
                shinyPaletteBytes = concatenate(PALETTE_PREFIX_BYTES, shinyPaletteBytes);
                pokeGraphicsNARC.getFiles().set(shinyPaletteIndex, shinyPaletteBytes);

            }
            writeNARC(NARCpath, pokeGraphicsNARC);

        } catch (IOException e) {
            throw new RandomizerIOException(e);
        }
    }
    
    @Override
    protected List<BufferedImage> getAllPokemonImages() {
        List<BufferedImage> bims = new ArrayList<>();
        
        String NARCpath = getNARCPath("PokemonGraphics");
        NARCArchive pokeGraphicsNARC;
        try {
            pokeGraphicsNARC = readNARC(NARCpath);
        } catch (IOException e) {
            throw new RandomizerIOException(e);
        }
        
        for (int i = 1; i < getPokemon().size(); i++) {
            Pokemon pk = getPokemon().get(i);
           
            BufferedImage frontNormal = getPokemonImage(pk, pokeGraphicsNARC, false, false, false, true);
            BufferedImage backNormal = getPokemonImage(pk, pokeGraphicsNARC, true, false, false, false);
        	BufferedImage frontShiny = getPokemonImage(pk, pokeGraphicsNARC, false, true, false, true); 
        	BufferedImage backShiny = getPokemonImage(pk, pokeGraphicsNARC, true, true, false, false); 
            	
        	BufferedImage combined = GFXFunctions.stitchToGrid(new BufferedImage[][] {{frontNormal, backNormal}, {frontShiny, backShiny}});
            bims.add(combined);
        }
        return bims;
    }
    
	@Override
	public final BufferedImage getMascotImage() {
		try {
			dumpAllPokemonSprites();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			Pokemon pk = randomPokemon();
			String NARCpath = getNARCPath("PokemonGraphics");
			NARCArchive pokeGraphicsNARC = readNARC(NARCpath);
			boolean shiny = random.nextInt(10) == 0;

			BufferedImage bim = getPokemonImage(pk, pokeGraphicsNARC, false, shiny, true, false);

			return bim;
		} catch (IOException e) {
			throw new RandomizerIOException(e);
		}
	}
    
    // TODO: Using many boolean arguments is suboptimal in Java, but I am unsure of the pattern to replace it
    public abstract BufferedImage getPokemonImage(Pokemon pk, NARCArchive pokeGraphicsNARC, boolean back, boolean shiny,
            boolean transparentBackground, boolean includePalette);

    private byte[] concatenate(byte[] a, byte[] b) {
        byte[] sum = new byte[a.length + b.length];
        System.arraycopy(a, 0, sum, 0, a.length);
        System.arraycopy(b, 0, sum, a.length, b.length);
        return sum;
    }

    // because RomEntry is an inner class it can't be accessed here, so an abstract
    // method is needed.
    // a refactoring might be better, but is outside of the scope for the changes
    // I'm making now
    // - voliol 2022-01-13
    public abstract String getNARCPath(String key);

}
