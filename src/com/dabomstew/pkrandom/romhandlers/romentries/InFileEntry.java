package com.dabomstew.pkrandom.romhandlers.romentries;

public class InFileEntry {

    private final int file;
    private final int offset;

    public InFileEntry(int file, int offset) {
        this.file = file;
        this.offset = offset;
    }

    public int getFile() {
        return file;
    }

    public int getOffset() {
        return offset;
    }

}
