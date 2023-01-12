package com.dabomstew.pkrandom.gbspace;

public class BankDividedFreedSpace extends FreedSpace {

    private final int bankSize;

    private BankDividedFreedSpace(int bankSize) {
        if (bankSize <= 0) {
            throw new IllegalArgumentException("bankSize must be positive.");
        }
        this.bankSize = bankSize;
    }

    @Override
    public void free(int start, int length) {
        if (freedChunkWouldCrossBankBoundary(start, length)) {
            throw new RuntimeException("Can't free a space spanning over multiple banks. This is a safety measure " +
                    "to prevent bad usage of free().");
        }
        super.free(start, length);
        splitFreedChunksAtBankBoundaries();
    }

    private boolean freedChunkWouldCrossBankBoundary(int start, int length) {
        int startBank = start / bankSize;
        int endBank = (start + length - 1) / bankSize;
        return startBank != endBank;
    }
    private void splitFreedChunksAtBankBoundaries() {
        int bank = 0;

        for (int i = 0; i < freedChunks.size(); i++) {
            FreedChunk fs = freedChunks.get(i);
            while (freedChunkStartBank(fs) > bank) {
                bank++;
            }

            int startOfNextBank = (bank + 1) * bankSize;
            if (fs.end >= startOfNextBank) {
                FreedChunk splitOff = new FreedChunk(startOfNextBank, fs.end);
                fs.end = startOfNextBank - 1;
                freedChunks.add(splitOff);
            }
        }
    }

    private int freedChunkStartBank(FreedChunk fc) {
        return fc.start / bankSize;
    }

}
