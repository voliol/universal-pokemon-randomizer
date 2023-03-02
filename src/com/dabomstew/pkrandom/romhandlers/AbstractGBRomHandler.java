package com.dabomstew.pkrandom.romhandlers;

import java.awt.image.BufferedImage;

/*----------------------------------------------------------------------------*/
/*--  AbstractGBRomHandler.java - a base class for GB/GBA rom handlers      --*/
/*--                              which standardises common GB(A) functions.--*/
/*--                                                                        --*/
/*--  Part of "Universal Pokemon Randomizer ZX" by the UPR-ZX team          --*/
/*--  Originally part of "Universal Pokemon Randomizer" by Dabomstew        --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2020.                                 --*/
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
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.dabomstew.pkrandom.FileFunctions;
import com.dabomstew.pkrandom.constants.GBConstants;
import com.dabomstew.pkrandom.gbspace.FreedSpace;
import com.dabomstew.pkrandom.GFXFunctions;
import com.dabomstew.pkrandom.exceptions.CannotWriteToLocationException;
import com.dabomstew.pkrandom.exceptions.RandomizerIOException;
import com.dabomstew.pkrandom.pokemon.Pokemon;

public abstract class AbstractGBRomHandler extends AbstractRomHandler {

    protected byte[] rom;
    protected byte[] originalRom;
    private String loadedFN;

    public AbstractGBRomHandler(Random random, PrintStream logStream) {
        super(random, logStream);
    }

    @Override
    public boolean loadRom(String filename) {
        byte[] loaded = loadFile(filename);
        if (!detectRom(loaded)) {
            return false;
        }
        this.rom = loaded;
        this.originalRom = new byte[rom.length];
        System.arraycopy(rom, 0, originalRom, 0, rom.length);
        loadedFN = filename;
        loadedRom();
        return true;
    }

    @Override
    public String loadedFilename() {
        return loadedFN;
    }

    @Override
    public boolean saveRomFile(String filename, long seed) {
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(rom);
            fos.close();
            return true;
        } catch (IOException ex) {
            if (ex.getMessage().contains("Access is denied")) {
                throw new CannotWriteToLocationException("The randomizer cannot write to this location: " + filename);
            }
            return false;
        }
    }

    @Override
    public boolean saveRomDirectory(String filename) {
        // do nothing, because GB games don't really have a concept of a filesystem
        return true;
    }

    @Override
    public boolean hasGameUpdateLoaded() {
        return false;
    }

    @Override
    public boolean loadGameUpdate(String filename) {
        // do nothing, as GB games don't have external game updates
        return true;
    }

    @Override
    public void removeGameUpdate() {
        // do nothing, as GB games don't have external game updates
    }

    @Override
    public String getGameUpdateVersion() {
        // do nothing, as GB games don't have external game updates
        return null;
    }

    @Override
    public void printRomDiagnostics(PrintStream logStream) {
        Path p = Paths.get(loadedFN);
        logStream.println("File name: " + p.getFileName().toString());
        long crc = FileFunctions.getCRC32(originalRom);
        logStream.println("Original ROM CRC32: " + String.format("%08X", crc));
    }

    @Override
    protected void prepareSaveRom() {
        super.prepareSaveRom();
        // because most other gens write the trainers to ROM each time setTrainers is used,
        // instead of having a saveTrainers. (obviously those other gens shouldn't do that either,
        // but code's never perfect)
        saveTrainers();
    }

    abstract protected void saveTrainers();

    @Override
    public boolean hasPhysicalSpecialSplit() {
        // Default value for Gen1-Gen3.
        // Handlers can override again in case of ROM hacks etc.
        return false;
    }

    public abstract boolean detectRom(byte[] rom);

    public abstract void loadedRom();

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

    protected void readByteIntoFlags(boolean[] flags, int offsetIntoFlags, int offsetIntoROM) {
        int thisByte = rom[offsetIntoROM] & 0xFF;
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

    protected int readWord(int offset) {
        return readWord(rom, offset);
    }
    
	protected final void writeByte(int offset, byte value) {
		rom[offset] = value;
	}

	protected final void writeBytes(int offset, byte[] values) {
		for (int i = 0; i < values.length; i++) {
			writeByte(offset + i, values[i]);
		}
	}

	protected final void writeBytes(byte[] data, int offset, byte[] values) {
		System.arraycopy(values, 0, data, offset, values.length);
	}

	protected int readWord(byte[] data, int offset) {
		return (data[offset] & 0xFF) + ((data[offset + 1] & 0xFF) << 8);
	}

	protected void writeWord(int offset, int value) {
		writeWord(rom, offset, value);
	}

	protected void writeWord(byte[] data, int offset, int value) {
		data[offset] = (byte) (value % 0x100);
		data[offset + 1] = (byte) ((value / 0x100) % 0x100);
	}

	protected boolean matches(byte[] data, int offset, byte[] needle) {
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

	protected void freeSpace(int offset, int length) {
		if (length < 1) {
			throw new IllegalArgumentException("length must be at least 1.");
		}
		for (int i = 0; i < length; i++) {
			writeByte(offset + i, getFreeSpaceByte());
		}
        getFreedSpace().free(offset, length);
	}

    /**
     * Both end points included.
     */
    protected void freeSpaceBetween(int start, int end) {
        freeSpace(start, end - start);
    }

	protected int findAndUnfreeSpace(int length) {
        return findAndUnfreeSpace(length, true);
	}

    /**
     * At least Pokémon palettes in R/S/FR/LG need to be long aligned,
     * probably more types of data than that though. If they are not long aligned,
     * the games soft-lock and/or crash, which isn't fun to debug.
     * If you aren't very sure about not needing to long-align, don't use this method directly.
     *
     * @param length The number of bytes to find space for.
     * @param longAligned Does the found adress need to be long-aligned?
     */
    protected int findAndUnfreeSpace(int length, boolean longAligned) {
        int foundOffset;
        length += longAligned ? GBConstants.longSize : 0;
        do {
            foundOffset = getFreedSpace().findAndUnfree(length);
        } while (isRomSpaceUsed(foundOffset, length));

        if (foundOffset == -1) {
            throw new RandomizerIOException("ROM full.");
        }

        if (longAligned) {
            int shift = GBConstants.longSize - (foundOffset % GBConstants.longSize);
            shift = shift == GBConstants.longSize ? 0 : shift;
            freeSpace(foundOffset + length - (4 - shift), 4 - shift);
            foundOffset += shift;
        }
        return foundOffset;
    }

	protected boolean isRomSpaceUsed(int offset, int length) {
		if (offset < 0)
			return false;
		// manual check if the space is still unused, because
		// the deprecated RomFunctions methods (or any future badly written code)
		// can in theory use the freed spaces "by accident".
		for (int i = 0; i < length; i++) {
			if (rom[offset + i] != getFreeSpaceByte()) {
				return true;
			}
		}
		return false;
	}

    protected abstract FreedSpace getFreedSpace();

	protected abstract byte getFreeSpaceByte();

    @Override
	protected List<BufferedImage> getAllPokemonImages() {
		List<BufferedImage> bims = new ArrayList<>();
		for (int i = 1; i < getPokemon().size(); i++) {
			Pokemon pk = getPokemon().get(i);

			BufferedImage frontNormal = getPokemonImage(pk, false, false, false, true);
			BufferedImage backNormal = getPokemonImage(pk, true, false, false, false);
			BufferedImage frontShiny = getPokemonImage(pk, false, true, false, true);
			BufferedImage backShiny = getPokemonImage(pk, true, true, false, false);

			BufferedImage combined = GFXFunctions
					.stitchToGrid(new BufferedImage[][] { { frontNormal, backNormal }, { frontShiny, backShiny } });
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
		Pokemon pk = randomPokemon();
		boolean shiny = random.nextInt(10) == 0;
		boolean gen1 = generationOfPokemon() == 1;

		return getPokemonImage(pk, false, !gen1 && shiny, true, false);
	}

	// TODO: Using many boolean arguments is suboptimal in Java, but I am unsure of the pattern to replace it
	public abstract BufferedImage getPokemonImage(Pokemon pk, boolean back, boolean shiny,
			boolean transparentBackground, boolean includePalette);

}
