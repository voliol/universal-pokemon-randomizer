package com.dabomstew.pkrandom.romhandlers.romentries;

public class Gen3EventTextEntry {
    private final int id;
    private final int mapBank, mapNumber;
    private final int personNum;
    private final int[] relativePointerOffsets;
    private final String template;
    private int actualPointerOffset;

    public Gen3EventTextEntry(int id, int mapBank, int mapNumber, int personNum, int[] relativePointerOffsets,
                              String template) {
        this.id = id;
        this.mapBank = mapBank;
        this.mapNumber = mapNumber;
        this.personNum = personNum;
        this.relativePointerOffsets = relativePointerOffsets;
        this.template = template;
    }

    public int getID() {
        return id;
    }

    public int getMapBank() {
        return mapBank;
    }

    public int getMapNumber() {
        return mapNumber;
    }

    public int getPersonNum() {
        return personNum;
    }

    public String getTemplate() {
        return template;
    }

    public int[] getRelativePointerOffsets() {
        return relativePointerOffsets;
    }

    public int getActualPointerOffset() {
        return actualPointerOffset;
    }

    public void setActualPointerOffset(int actualPointerOffset) {
        this.actualPointerOffset = actualPointerOffset;
    }

    @Override
    public String toString() {
        return String.format("EventTextEntry = [id=%d, actualPointerOffset=%x, mapBank=%x, mapNumber=%x, personNum=%x, " +
                        "%s=%s, template=\"%s\"]",
                id, actualPointerOffset, mapBank, mapNumber, personNum,
                relativePointerOffsets.length < 2 ? "relativePointerOffset" : "relativePointerOffsets",
                relativePointerOffsetsToString(),
                template);
    }

    public String relativePointerOffsetsToString() {
        if (relativePointerOffsets == null || relativePointerOffsets.length == 0) {
            return "invalid";
        } else if (relativePointerOffsets.length == 1) {
            return "0x" + Integer.toHexString(relativePointerOffsets[0]);
        } else {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < relativePointerOffsets.length; i++) {
                sb.append("0x").append(Integer.toHexString(relativePointerOffsets[i]));
                if (i != relativePointerOffsets.length - 1) {
                    sb.append(";");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

}