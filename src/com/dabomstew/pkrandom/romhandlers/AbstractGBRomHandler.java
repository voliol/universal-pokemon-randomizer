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
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.dabomstew.pkrandom.FileFunctions;
import com.dabomstew.pkrandom.constants.GBConstants;
import com.dabomstew.pkrandom.gbspace.FreedSpace;
import com.dabomstew.pkrandom.GFXFunctions;
import com.dabomstew.pkrandom.exceptions.CannotWriteToLocationException;
import com.dabomstew.pkrandom.exceptions.RandomizerIOException;
import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.romhandlers.romentries.AbstractGBRomEntry;
import com.dabomstew.pkrandom.romhandlers.romentries.RomEntry;

public abstract class AbstractGBRomHandler extends AbstractRomHandler {

    protected byte[] rom;
    protected byte[] originalRom;
    private String loadedFileName;
    private long actualCRC32;

    public AbstractGBRomHandler(Random random, PrintStream logStream) {
        super(random, logStream);
    }

    @Override
    public boolean loadRom(String filename) {
        try {
            loadRomFile(filename);
            midLoadingSetUp();
            loadGameData();
            return true;
        } catch (RandomizerIOException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected void loadRomFile(String filename) {
        byte[] loaded = loadFile(filename);
        if (!detectRom(loaded)) {
            throw new RandomizerIOException("Could not detect ROM.");
        }
        this.rom = loaded;
        this.originalRom = new byte[rom.length];
        System.arraycopy(rom, 0, originalRom, 0, rom.length);
        loadedFileName = filename;
        this.actualCRC32 = FileFunctions.getCRC32(rom);
    }

    /**
     * Sets up various stuff which needs to be done after the ROM file has been loaded, but which is needed for loading
     * game data like {@link Pokemon} and {@link Trainer}s. E.g. the {@link RomEntry} and text tables.
     * Expected to be overrided.
     */
    protected void midLoadingSetUp() {
        initRomEntry();
        initTextTables();
    }

    protected abstract void initRomEntry();

    protected void addRelativeOffsetToRomEntry(String newKey, String baseKey, int offset) {
        int baseOffset = getRomEntry().getIntValue(baseKey);
        if (baseOffset != 0 && getRomEntry().getIntValue(newKey) == 0) {
            getRomEntry().putIntValue(newKey, baseOffset + offset);
        }
    }

    protected abstract void initTextTables();

    /**
     * Loads the (randomizable) game data, i.e. stuff like the gettable lists of {@link Pokemon}, {@link Move}s,
     * and {@link Trainer}s.
     */
    protected void loadGameData() {
        loadPokemonStats();
        loadEvolutions();
        loadMoves();
        loadPokemonPalettes();
        loadItemNames();
        loadTrainers();
    }

    // the below are public because it may be kinder to the testing environment
    public abstract void loadPokemonStats();

    public abstract void loadEvolutions();

    public abstract void loadMoves();

    public abstract void loadPokemonPalettes();

    public abstract void loadItemNames();

    public abstract void loadTrainers();

    @Override
    public String loadedFilename() {
        return loadedFileName;
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
        Path p = Paths.get(loadedFileName);
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

    abstract public void saveTrainers();

    @Override
    public boolean hasPhysicalSpecialSplit() {
        // Default value for Gen1-Gen3.
        // Handlers can override again in case of ROM hacks etc.
        return false;
    }

    public abstract boolean detectRom(byte[] rom);


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
        if (offset == 0) {
            throw new IllegalArgumentException("Not allowed to write at offset 0 of the ROM.");
        }
        writeBytes(rom, offset, values);
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

    /**
     * Returns the length of some data, which is ended by a single-byte terminator.
     * Counts the terminator towards the length.
     */
    protected int lengthOfDataWithTerminatorAt(int offset, byte terminator) {
        return lengthOfDataWithTerminatorsAt(offset, terminator, 1);
    }

    /**
     * Returns the length of some data using single-byte terminators.
     * Counts the terminators towards the length.
     */
    protected int lengthOfDataWithTerminatorsAt(int offset, byte terminator, int terminatorAmount) {
        int length = 0;
        int terminatorCount = 0;
        do {
            if (rom[offset + length] == terminator) terminatorCount++;
            length++;
        } while (terminatorCount < terminatorAmount);
        return length;
    }

    protected abstract int readPointer(int offset);

    protected abstract void writePointer(int offset, int pointer);

    protected class DataRewriter<E> {

        protected Function<Integer, Integer> pointerReader = AbstractGBRomHandler.this::readPointer;
        protected BiConsumer<Integer, Integer> pointerWriter = AbstractGBRomHandler.this::writePointer;

        private boolean longAlignAdresses = true;

        public boolean isLongAlignAdresses() {
            return longAlignAdresses;
        }

        public void setLongAlignAdresses(boolean longAlignAdresses) {
            this.longAlignAdresses = longAlignAdresses;
        }

        public void setPointerReader(Function<Integer, Integer> pointerReader) {
            this.pointerReader = pointerReader;
        }

        public void setPointerWriter(BiConsumer<Integer, Integer> pointerWriter) {
            this.pointerWriter = pointerWriter;
        }

        public void rewriteData(int pointerOffset, E e, Function<E, byte[]> newDataFunction,
                                Function<Integer, Integer> lengthOfOldFunction) {
            rewriteData(pointerOffset, e, new int[0], newDataFunction, lengthOfOldFunction);
        }

        public void rewriteData(int pointerOffset, E e, int[] secondaryPointerOffsets,
                                Function<E, byte[]> newDataFunction, Function<Integer, Integer> lengthOfOldFunction) {
            byte[] newData = newDataFunction.apply(e);
            int oldDataOffset = pointerReader.apply(pointerOffset);
            int oldLength = lengthOfOldFunction.apply(oldDataOffset);
            freeSpace(oldDataOffset, oldLength);
            int newDataOffset = repointAndWriteToFreeSpace(pointerOffset, newData);

            rewriteSecondaryPointers(pointerOffset, secondaryPointerOffsets, oldDataOffset, newDataOffset);
        }

        /**
         * Returns the new offset of the data.
         **/
        protected int repointAndWriteToFreeSpace(int pointerOffset, byte[] data) {
            int newOffset = findAndUnfreeSpace(data.length, longAlignAdresses);

            pointerWriter.accept(pointerOffset, newOffset);
            writeBytes(newOffset, data);

            return newOffset;
        }

        protected void rewriteSecondaryPointers(int primaryPointerOffset, int[] secondaryPointerOffsets,
                                              int oldDataOffset, int newDataOffset) {
            for (int spo : secondaryPointerOffsets) {
                int offset = pointerReader.apply(spo);
                if (spo != primaryPointerOffset && offset != oldDataOffset) {
                    System.out.println();
                    System.out.println("bad: " + spo);
                    throw new RandomizerIOException("Invalid secondary pointer spo=0x" + Integer.toHexString(spo) +
                            ". Points to 0x" + Integer.toHexString(offset) + " instead of 0x" +
                            Integer.toHexString(oldDataOffset) + ".");
                }
                pointerWriter.accept(spo, newDataOffset);
            }
        }
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
        freeSpace(start, end - start + 1);
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
            throw new RandomizerIOException("ROM full. Can't find " + length + " free bytes anywhere.");
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
    public boolean hasTypeEffectivenessSupport() {
        return true;
    }

    @Override
    public void updateTypeEffectiveness() {
        TypeTable typeTable = getTypeTable();
        log("--Updating Type Effectiveness--");

        typeTable.setEffectiveness(Type.GHOST, Type.STEEL, Effectiveness.NEUTRAL);
        log("Replaced: Ghost not very effective vs Steel => Ghost neutral vs Steel");
        typeTable.setEffectiveness(Type.DARK, Type.STEEL, Effectiveness.NEUTRAL);
        log("Replaced: Dark not very effective vs Steel => Dark neutral vs Steel");

        logBlankLine();
        setTypeTable(typeTable);
    }

    @Override
	public List<BufferedImage> getAllPokemonImages() {
		List<BufferedImage> bims = new ArrayList<>();
		for (Pokemon pk : getPokemonSet()) {
			bims.add(createPokemonImageGetter(pk).getFull());
		}
		return bims;
	}

    public abstract static class GBPokemonImageGetter extends PokemonImageGetter {

        public GBPokemonImageGetter(Pokemon pk) {
            super(pk);
        }

        @Override
        public BufferedImage getFull() {
            setIncludePalette(true);

            BufferedImage frontNormal = get();
            BufferedImage backNormal = setBack(true).get();
            BufferedImage backShiny = setShiny(true).get();
            BufferedImage frontShiny = setBack(false).get();

            return GFXFunctions.stitchToGrid(new BufferedImage[][] { { frontNormal, backNormal }, { frontShiny, backShiny } });
        }
    }

	@Override
	public final BufferedImage getMascotImage() {
		try {
			dumpAllPokemonImages();
		} catch (Exception e) {
			e.printStackTrace();
		}
        return createPokemonImageGetter(randomPokemon())
                .setShiny(random.nextInt(10) == 0 && generationOfPokemon() != 1)
                .setTransparentBackground(true)
                .get();
	}

    @Override
    public abstract AbstractGBRomEntry getRomEntry();

    @Override
    public boolean isRomValid() {
        return getRomEntry().getExpectedCRC32() == actualCRC32;
    }

}
