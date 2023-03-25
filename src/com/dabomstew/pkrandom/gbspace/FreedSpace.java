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

    private static final String ALREADY_FREED_EXCEPTION_MESSAGE =  "Can't free a space that is already freed. " +
            "This is a safety measure to prevent bad usage of free().";

    protected static class FreedChunk {

        public int start, end;

        public FreedChunk(int offset, int end) {
            this.start = offset;
            this.end = end;
        }

        public int getLength() {
            return end - start + 1;
        }

        @Override
        public String toString() {
            return String.format("%x-%x", start, end);
        }

    }

    protected final LinkedList<FreedChunk> freedChunks = new LinkedList<>();

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

    protected final void addFreedChunkBefore(FreedChunk toFree, int i) {
        FreedChunk neighbor = freedChunks.get(i);
        if (toFree.end >= neighbor.start) {
            throw new RuntimeException(ALREADY_FREED_EXCEPTION_MESSAGE);
        }

        if (toFree.end == neighbor.start - 1) {
            neighbor.start = toFree.start;
        } else {
            freedChunks.add(i, toFree);
        }
    }

    protected final boolean addFreedChunkAfterOrBetween(FreedChunk toFree, int i) {
        FreedChunk leftNeighbor = freedChunks.get(i);
        if (leftNeighbor.end >= toFree.start) {
            throw new RuntimeException(ALREADY_FREED_EXCEPTION_MESSAGE);
        }

        if (leftNeighbor.end == toFree.start - 1) {

            if (i == freedChunks.size() - 1) {
                leftNeighbor.end = toFree.end;

            } else {
                FreedChunk rightNeighbor = freedChunks.get(i + 1);
                if (toFree.end >= rightNeighbor.start) {
                    throw new RuntimeException(ALREADY_FREED_EXCEPTION_MESSAGE);
                }
                else if (toFree.end == rightNeighbor.start - 1) {
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
        FreedChunk found = find(length);
        if (found == null) {
            return -1;
        }
        int offset = found.start;
        unfree(found, length);
        return offset;
    }

    protected final FreedChunk find(int length) {
        for (FreedChunk fc : freedChunks) {
            if (fc.getLength() >= length) {
                return fc;
            }
        }
        return null;
    }

    protected final void unfree(FreedChunk toUnfree, int length) {
        System.out.println("unfreeing " + length + " bytes starting from 0x" + Integer.toHexString(toUnfree.start));
        toUnfree.start += length;
        if (toUnfree.start > toUnfree.end) {
            freedChunks.remove(toUnfree);
        }
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
		return getLengthSum() + " bytes, " + freedChunks.size() + " chunks, " + freedChunks;
	}

}
