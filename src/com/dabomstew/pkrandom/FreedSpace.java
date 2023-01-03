package com.dabomstew.pkrandom;

import java.util.LinkedList;

/**
 * Represents/handles all the manually freed bytes in a ROM. If bytes are manually freed (using free())
 * we know they should be available for writing at a later time, at which point we can findAndUnfree() them.
 **/
public class FreedSpace {

    private static class FreedChunk {

        private int start, end;

        private FreedChunk(int offset, int end) {
            this.start = offset;
            this.end = end;
        }

        private int getLength() {
            return end - start + 1;
        }

        @Override
        public String toString() {
            return start + "-" + end;
        }

    }

    private final LinkedList<FreedChunk> freedChunks = new LinkedList<>();

    public void free(int start, int length) {
        if (length < 1) {
            throw new IllegalArgumentException("length must be at least 1");
        }

        int end = start + length - 1;
        FreedChunk toFree = new FreedChunk(start, end);

        if (freedChunks.size() == 0) {
            freedChunks.add(toFree);
            return;
        }

        for (int i = 0; i < freedChunks.size(); i++) {
            FreedChunk neighbor = freedChunks.get(i);

            if (toFree.start < neighbor.start) {
                addFreedChunkBefore(toFree, i);
                return;

            } else {
                if (addFreedChunkAfterOrBetween(toFree, i)) {
                    return;
                }
            }
        }
    }

    private void addFreedChunkBefore(FreedChunk toFree, int i) {
        FreedChunk neighbor = freedChunks.get(i);
        if (toFree.end >= neighbor.start) {
            throw new RuntimeException(
                    "Can't free a space that is already freed. This is a safety measure to prevent bad usage of freeSpace().");
        }

        if (toFree.end == neighbor.start - 1) {
            neighbor.start = toFree.start;
        } else {
            freedChunks.add(i, toFree);
        }
    }

    private boolean addFreedChunkAfterOrBetween(FreedChunk toFree, int i) {
        FreedChunk leftNeighbor = freedChunks.get(i);
        if (leftNeighbor.end >= toFree.start) {
            throw new RuntimeException(
                    "Can't free a space that is already freed. This is a safety measure to prevent bad usage of freeSpace().");
        }

        if (leftNeighbor.end == toFree.start - 1) {

            if (i == freedChunks.size() - 1) {
                leftNeighbor.end = toFree.end;

            } else {
                FreedChunk rightNeighbor = freedChunks.get(i + 1);
                if (toFree.end == rightNeighbor.start - 1) {
                    leftNeighbor.end = rightNeighbor.end;
                    freedChunks.remove(rightNeighbor);
                } else {
                    leftNeighbor.end = toFree.end;
                }

            }
            return true;

        } else if (i == freedChunks.size() - 1) {
            freedChunks.add(toFree);
            return true;
        }

        return false;
    }

    public int findAndUnfree(int length) {
        System.out.println(getLengthSum() + " bytes in " + freedChunks.size() + " chunks." );
        System.out.println("Looking for " + length + " bytes.");

        for (int i = 0; i < freedChunks.size(); i++) {
            FreedChunk fc = freedChunks.get(i);
            if (fc.getLength() >= length) {
                int foundOffset = fc.start;
                fc.start += length;
                if (fc.start > fc.end) {
                    freedChunks.remove(i);
                }
                return foundOffset;
            }
        }
        return -1;
    }

    public int getLengthSum() {
        int sum = 0;
        for (FreedChunk fs : freedChunks) {
            sum += fs.getLength();
        }
        return sum;
    }

    @Override
    public String toString() {
        return getLengthSum() + " bytes, " + freedChunks;
    }

}
