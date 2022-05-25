package com.dabomstew.pkrandom.romhandlers;

import java.awt.image.BufferedImage;

/*----------------------------------------------------------------------------*/
/*--  AbstractGBRomHandler.java - a base class for GB/GBA rom handlers      --*/
/*--                              which standardises common GB(A) functions.--*/
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.dabomstew.pkrandom.FileFunctions;
import com.dabomstew.pkrandom.exceptions.RandomizerIOException;
import com.dabomstew.pkrandom.pokemon.Pokemon;

public abstract class AbstractGBRomHandler extends AbstractRomHandler {

    protected byte[] rom;
    private String loadedFN;

    public AbstractGBRomHandler(Random random) {
        super(random);
    }

    @Override
    public boolean loadRom(String filename) {
        byte[] loaded = loadFile(filename);
        if (!detectRom(loaded)) {
            return false;
        }
        this.rom = loaded;
        loadedFN = filename;
        loadedRom();
        return true;
    }

    @Override
    public String loadedFilename() {
        return loadedFN;
    }

    @Override
    public boolean saveRom(String filename) {
        savingRom();
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(rom);
            fos.close();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public boolean canChangeStaticPokemon() {
        return true;
    }

    @Override
    public boolean hasPhysicalSpecialSplit() {
        // Default value for Gen1-Gen3.
        // Handlers can override again in case of ROM hacks etc.
        return false;
    }

    public abstract boolean detectRom(byte[] rom);

    public abstract void loadedRom();

    public abstract void savingRom();

    protected static byte[] loadFile(String filename) {
        try {
            return FileFunctions.readFileFullyIntoBuffer(filename);
        } catch (IOException ex) {
            throw new RandomizerIOException(ex);
        }
    }

    protected static byte[] loadFilePartial(String filename, int maxBytes) {
        try {
            File fh = new File(filename);
            if (!fh.exists() || !fh.isFile() || !fh.canRead()) {
                return new byte[0];
            }
            long fileSize = fh.length();
            if (fileSize > Integer.MAX_VALUE) {
                return new byte[0];
            }
            FileInputStream fis = new FileInputStream(filename);
            byte[] file = FileFunctions.readFullyIntoBuffer(fis, Math.min((int) fileSize, maxBytes));
            fis.close();
            return file;
        } catch (IOException ex) {
            return new byte[0];
        }
    }

    protected final void readByteIntoFlags(boolean[] flags, int offsetIntoFlags, int offsetIntoROM) {
        int thisByte = rom[offsetIntoROM] & 0xFF;
        for (int i = 0; i < 8 && (i + offsetIntoFlags) < flags.length; i++) {
            flags[offsetIntoFlags + i] = ((thisByte >> i) & 0x01) == 0x01;
        }
    }

    protected final byte getByteFromFlags(boolean[] flags, int offsetIntoFlags) {
        int thisByte = 0;
        for (int i = 0; i < 8 && (i + offsetIntoFlags) < flags.length; i++) {
            thisByte |= (flags[offsetIntoFlags + i] ? 1 : 0) << i;
        }
        return (byte) thisByte;
    }

    protected final int readByte(int offset) {
        return rom[offset];
    }

    protected final int readWord(int offset) {
        return readWord(rom, offset);
    }

    protected final int readWord(byte[] data, int offset) {
        return (data[offset] & 0xFF) + ((data[offset + 1] & 0xFF) << 8);
    }

    protected final void writeByte(int offset, byte value) {
        rom[offset] = value;
    }

    protected final void writeBytes(int offset, byte[] values) {
        for (int i = 0; i < values.length; i++) {
            writeByte(offset + i, values[i]);
        }
    }

    protected final void writeWord(int offset, int value) {
        writeWord(rom, offset, value);
    }

    protected final void writeWord(byte[] data, int offset, int value) {
        data[offset] = (byte) (value % 0x100);
        data[offset + 1] = (byte) ((value / 0x100) % 0x100);
    }

    protected final boolean matches(byte[] data, int offset, byte[] needle) {
        for (int i = 0; i < needle.length; i++) {
            if (offset + i >= data.length) {
                return false;
            }
            if (data[offset + i] != needle[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected List<BufferedImage> getAllPokemonImages() {
        List<BufferedImage> bims = new ArrayList<>();
        for (int i = 1; i < getPokemon().size(); i++) {
            Pokemon pk = getPokemon().get(i);
            bims.add(getPokemonImage(pk, false, false, true));
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
        Pokemon pk = randomPokemon();
        boolean shiny = random.nextInt(10) == 0;

        BufferedImage bim = getPokemonImage(pk, shiny, true, false);

        return bim;
    }

    // TODO: Using many boolean arguments is suboptimal in Java, but I am unsure of
    // the pattern to replace it
    protected abstract BufferedImage getPokemonImage(Pokemon pk, boolean shiny, boolean transparentBackground,
            boolean includePalette);

}