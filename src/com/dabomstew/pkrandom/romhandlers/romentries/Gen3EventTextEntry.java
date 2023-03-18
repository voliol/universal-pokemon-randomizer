package com.dabomstew.pkrandom.romhandlers.romentries;

public class Gen3EventTextEntry {
    private final int id;
    private final int mapBank, mapNumber;
    private final int personNum;
    private final int offsetInScript;
    private final String template;
    private int actualPointerOffset;

    public Gen3EventTextEntry(int id, int mapBank, int mapNumber, int personNum, int offsetInScript,
                              String template) {
        this.id = id;
        this.mapBank = mapBank;
        this.mapNumber = mapNumber;
        this.personNum = personNum;
        this.offsetInScript = offsetInScript;
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

    public int getOffsetInScript() {
        return offsetInScript;
    }

    public String getTemplate() {
        return template;
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
                        "offsetInScript=%x, template=\"%s\"]",
                id, actualPointerOffset, mapBank, mapNumber, personNum, offsetInScript, template);
    }
}