package com.dabomstew.pkrandom.romhandlers;

/*----------------------------------------------------------------------------*/
/*--  AbstractGBCRomHandler.java - an extension of AbstractGBRomHandler     --*/
/*--                               used for Gen 1 and Gen 2.                --*/
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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.dabomstew.pkrandom.FileFunctions;
import com.dabomstew.pkrandom.constants.GBConstants;
import com.dabomstew.pkrandom.exceptions.RandomizerIOException;
import com.dabomstew.pkrandom.gbspace.BankDividedFreedSpace;
import com.dabomstew.pkrandom.romhandlers.romentries.AbstractGBCRomEntry;

public abstract class AbstractGBCRomHandler extends AbstractGBRomHandler {

    private String[] tb;
    private Map<String, Byte> d;
    private int longestTableToken;

    private BankDividedFreedSpace freedSpace;

    public AbstractGBCRomHandler(Random random, PrintStream logStream) {
        super(random, logStream);
    }

    @Override
    protected void midLoadingSetUp() {
        super.midLoadingSetUp();
        this.freedSpace = new BankDividedFreedSpace(GBConstants.bankSize, rom.length / GBConstants.bankSize,
                getRomEntry().getArrayValue("ReservedBanks"));
        freeUnusedSpaceAtEndOfBanks();
        freeUnusedBanks();
    }

    /**
     * Frees the unused space at the end of some banks, so the randomizer knows to use it.
     */
    protected void freeUnusedSpaceAtEndOfBanks() {
        for (Map.Entry<Integer, Integer> margin : getRomEntry().getBankEndFreeSpaceMargins().entrySet()) {
            freeUnusedSpaceAtEndOfBank(margin.getKey(), margin.getValue());
        }
    }

    protected void freeUnusedBanks() {
        for (int bank : getRomEntry().getArrayValue("UnusedBanks")) {
            if (isBankEmpty(bank)) {
                freeSpaceBetween(bank*GBConstants.bankSize, (bank+1)*GBConstants.bankSize - 1);
            } else {
                // TODO: what is a good way to log this?
                System.out.printf("""
                        Bank 0x%2x was expected to be empty, but is not.
                        This is a sign of a modified ROM which is not supported by the randomizer.
                        The randomizer will handle it as well as it can, but be aware of the risk of
                        the output being corrupted, or otherwise non-functional.
                        """, bank);
                freeUnusedSpaceAtEndOfBank(bank, 0x100); // arbitrary large amount
            }
        }
    }

    protected boolean isBankEmpty(int bank) {
        byte unusedByte = this.getFreeSpaceByte();
        for (int i = 0; i < GBConstants.bankSize; i++) {
            if (rom[bank*GBConstants.bankSize + i] != unusedByte) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void loadGameData() {
        super.loadGameData();
        loadMovesLearnt();
    }

    protected abstract void loadMovesLearnt();

    protected void clearTextTables() {
        tb = new String[256];
        if (d != null) {
            d.clear();
        } else {
            d = new HashMap<>();
        }
        longestTableToken = 0;
    }

    protected void readTextTable(String name) {
        try {
            Scanner sc = new Scanner(FileFunctions.openConfig(name + ".tbl"), StandardCharsets.UTF_8);
            while (sc.hasNextLine()) {
                String q = sc.nextLine();
                if (!q.trim().isEmpty()) {
                    String[] r = q.split("=", 2);
                    if (r[1].endsWith("\r\n")) {
                        r[1] = r[1].substring(0, r[1].length() - 2);
                    }
                    int hexcode = Integer.parseInt(r[0], 16);
                    if (tb[hexcode] != null) {
                        String oldMatch = tb[hexcode];
                        tb[hexcode] = null;
                        if (d.get(oldMatch) == hexcode) {
                            d.remove(oldMatch);
                        }
                    }
                    tb[hexcode] = r[1];
                    longestTableToken = Math.max(longestTableToken, r[1].length());
                    d.put(r[1], (byte) hexcode);
                }
            }
            sc.close();
        } catch (FileNotFoundException ignored) {
        }
    }

    protected String readString(int offset, int maxLength, boolean textEngineMode) {
        StringBuilder string = new StringBuilder();
        for (int c = 0; c < maxLength; c++) {
            int currChar = rom[offset + c] & 0xFF;
            if (tb[currChar] != null) {
                string.append(tb[currChar]);
                if (textEngineMode && (tb[currChar].equals("\\r") || tb[currChar].equals("\\e"))) {
                    break;
                }
            } else {
                if (currChar == GBConstants.stringTerminator) {
                    break;
                } else {
                    string.append("\\x").append(String.format("%02X", currChar));
                }
            }
        }
        return string.toString();
    }

    protected int lengthOfStringAt(int offset, boolean textEngineMode) {
        int len = 0;
        while (rom[offset + len] != GBConstants.stringTerminator
                && (!textEngineMode || (rom[offset + len] != GBConstants.stringPrintedTextEnd && rom[offset + len] != GBConstants.stringPrintedTextPromptEnd))) {
            len++;
        }

        if (textEngineMode
                && (rom[offset + len] == GBConstants.stringPrintedTextEnd || rom[offset + len] == GBConstants.stringPrintedTextPromptEnd)) {
            len++;
        }
        len++;
        return len;
    }

    protected byte[] translateString(String text) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (text.length() != 0) {
            int i = Math.max(0, longestTableToken - text.length());
            if (text.charAt(0) == '\\' && text.charAt(1) == 'x') {
                baos.write(Integer.parseInt(text.substring(2, 4), 16));
                text = text.substring(4);
            } else {
                while (!(d.containsKey(text.substring(0, longestTableToken - i)) || (i == longestTableToken))) {
                    i++;
                }
                if (i == longestTableToken) {
                    text = text.substring(1);
                } else {
                    baos.write(d.get(text.substring(0, longestTableToken - i)) & 0xFF);
                    text = text.substring(longestTableToken - i);
                }
            }
        }
        return baos.toByteArray();
    }

    protected String readFixedLengthString(int offset, int length) {
        return readString(offset, length, false);
    }

    protected void writeFixedLengthString(String str, int offset, int length) {
        writeFixedLengthString(rom, str, offset, length);
    }

    // pads the length with terminators, so length should be at least str's len + 1
    protected void writeFixedLengthString(byte[] data, String str, int offset, int length) {
        byte[] translated = translateString(str);
        int len = Math.min(translated.length, length);
        // TODO: should use writeBytes();
        System.arraycopy(translated, 0, data, offset, len);
        while (len < length) {
            data[offset + len] = GBConstants.stringTerminator;
            len++;
        }
    }

    protected void writeVariableLengthString(String str, int offset, boolean alreadyTerminated) {
        writeVariableLengthString(rom, str, offset, alreadyTerminated);
    }

    protected void writeVariableLengthString(byte[] data, String str, int offset, boolean alreadyTerminated) {
        byte[] translated = translateString(str);
        // TODO: should use writeBytes();
        System.arraycopy(translated, 0, data, offset, translated.length);
        if (!alreadyTerminated) {
            data[offset + translated.length] = GBConstants.stringTerminator;
        }
    }

    protected int makeGBPointer(int offset) {
        if (offset < GBConstants.bankSize) {
            return offset;
        } else {
            return (offset % GBConstants.bankSize) + GBConstants.bankSize;
        }
    }

    /**
     * Write a GB pointer to "offset", pointing at "pointer".
     */
    @Override
    protected void writePointer(int offset, int pointer) {
        int gbPointer = makeGBPointer(pointer);
        writeWord(offset, gbPointer);
    }

    /**
     * Reads the pointer at offset. Assumes the bank to be the one offset is in.
     */
    @Override
    protected int readPointer(int offset) {
        return readPointer(offset, bankOf(offset));
    }

    /**
     * Reads the pointer at offset, with a manually given bank.
     */
    protected int readPointer(int offset, int bank) {
        int pointer = readWord(offset);
        return calculateOffset(pointer, bank);
    }

    protected int calculateOffset(int pointer, int bank) {
        return (pointer % GBConstants.bankSize) + bank * GBConstants.bankSize;
    }

    protected int bankOf(int offset) {
        return (offset / GBConstants.bankSize);
    }

    protected String readVariableLengthString(int offset, boolean textEngineMode) {
        return readString(offset, Integer.MAX_VALUE, textEngineMode);
    }

    protected class GBCDataRewriter<E> extends DataRewriter<E> {

        private boolean restrictToSameBank = true;

        public GBCDataRewriter() {
            setLongAlignAdresses(false);
        }

        public boolean isRestrictToSameBank() {
            return restrictToSameBank;
        }

        public void setRestrictToSameBank(boolean restrictToSameBank) {
            this.restrictToSameBank = restrictToSameBank;
        }

        @Override
        protected int repointAndWriteToFreeSpace(int pointerOffset, byte[] data) {
            int bank = bankOf(pointerReader.apply(pointerOffset));
            int newOffset = restrictToSameBank ? findAndUnfreeSpaceInBank(data.length, bank)
                    : findAndUnfreeSpace(data.length, isLongAlignAdresses());

            pointerWriter.accept(pointerOffset, newOffset);
            writeBytes(newOffset, data);

            return newOffset;
        }
    }

    protected int findAndUnfreeSpaceInBank(int length, int bank) {
        int foundOffset;
        do {
            foundOffset = getFreedSpace().findAndUnfreeInBank(length, bank);
        } while (isRomSpaceUsed(foundOffset, length));

        if (foundOffset == -1) {
            throw new RandomizerIOException("Bank full. Can't find " + length + " free bytes anywhere.");
        }
        return foundOffset;
    }

    @Override
    protected BankDividedFreedSpace getFreedSpace() {
        return freedSpace;
    }

    @Override
    protected byte getFreeSpaceByte() {
        return 0;
    }

    protected void freeUnusedSpaceAtEndOfBank(int bank, int frontMargin) {
        // System.out.println("Trying to free unused space at end of bank 0x" + Integer.toHexString(bank));
        freeUnusedSpaceBefore((bank + 1) * GBConstants.bankSize - 1, frontMargin);
    }

    /**
     * Scans the ROM and frees "unused" bytes.
     * @param end The offset for the last byte to free
     * @param frontMargin The amount of seemingly unused bytes to NOT free, at the front/start.
     */
    protected void freeUnusedSpaceBefore(int end, int frontMargin) {
        int start = end;
        int minStart = bankOf(end)*GBConstants.bankSize;
        while (rom[start] == getFreeSpaceByte() && start >= minStart) {
            start--;
        }
        start++;
        start += frontMargin;
        if (start <= end) {
            freeSpaceBetween(start, end);
        }
    }

    protected static boolean romSig(byte[] rom, String sig) {
        int sigOffset = GBConstants.romSigOffset;
        byte[] sigBytes = sig.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < sigBytes.length; i++) {
            if (rom[sigOffset + i] != sigBytes[i]) {
                return false;
            }
        }
        return true;

    }

    protected static boolean romCode(byte[] rom, String code) {
        int sigOffset = GBConstants.romCodeOffset;
        byte[] sigBytes = code.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < sigBytes.length; i++) {
            if (rom[sigOffset + i] != sigBytes[i]) {
                return false;
            }
        }
        return true;

    }

    @Override
    public abstract AbstractGBCRomEntry getRomEntry();

    @Override
    public String getROMCode() {
        return getRomEntry().getRomCode() + " (" + getRomEntry().getVersion() + "/" + getRomEntry().getNonJapanese() + ")";
    }

}
