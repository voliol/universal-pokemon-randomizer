package com.dabomstew.pkrandom.gbspace;

/*----------------------------------------------------------------------------*/
/*--  Part of "Universal Pokemon Randomizer ZX" by the UPR-ZX team          --*/
/*--  Originally part of "Universal Pokemon Randomizer" by Dabomstew        --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2022.                                 --*/
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

import java.util.LinkedList;

/**
 * Represents/handles all the manually freed bytes in a ROM. If bytes are manually freed (using free())
 * we know they should be available for writing at a later time, at which point we can findAndUnfree() them.
 **/
public class FreedSpace {

    // TODO: add bank handling for Gen I/II games. Until then, functionality of this class should not be used with them.

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
