package com.dabomstew.pkrandom.romhandlers.romentries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class AbstractGBCRomEntry extends AbstractGBRomEntry {

    protected abstract static class GBCRomEntryReader<T extends AbstractGBCRomEntry> extends RomEntryReader<T> {

        public GBCRomEntryReader(String fileName) throws IOException {
            super(fileName);
            putSpecialKeyMethod("Game", RomEntry::setRomCode);
            putSpecialKeyMethod("NonJapanese", AbstractGBCRomEntry::setNonJapanese);
            putSpecialKeyMethod("ExtraTableFile", AbstractGBCRomEntry::setExtraTableFile);
            putSpecialKeyMethod("CRCInHeader", AbstractGBCRomEntry::setCRCInHeader);
            putSpecialKeyMethod("TMText[]", AbstractGBCRomEntry::addTMText);
        }
    }

    private int nonJapanese;
    private String extraTableFile;
    private int crcInHeader = -1;
    private final List<GBCTMTextEntry> tmTexts = new ArrayList<>();

    public AbstractGBCRomEntry(String name) {
        super(name);
    }

    public int getNonJapanese() {
        return nonJapanese;
    }

    private void setNonJapanese(String unparsed) {
        this.nonJapanese = RomEntryReader.parseInt(unparsed);
    }

    public String getExtraTableFile() {
        return extraTableFile;
    }

    private void setExtraTableFile(String extraTableFile) {
        this.extraTableFile = extraTableFile;
    }

    public int getCRCInHeader() {
        return crcInHeader;
    }

    private void setCRCInHeader(String unparsed) {
        this.crcInHeader = RomEntryReader.parseInt(unparsed);
    }

    private void addTMText(String unparsed)  {
        if (unparsed.startsWith("[") && unparsed.endsWith("]")) {
            String[] parts = unparsed.substring(1, unparsed.length() - 1).split(",", 3);
            int number = RomEntryReader.parseInt(parts[0]);
            int offset = RomEntryReader.parseInt(parts[1]);
            String template = parts[2];
            GBCTMTextEntry tte = new GBCTMTextEntry(number, offset, template);
            tmTexts.add(tte);
        }
    }

    public List<GBCTMTextEntry> getTMTexts() {
        return Collections.unmodifiableList(tmTexts);
    }

    @Override
    public void copyFrom(RomEntry other) {
        super.copyFrom(other);
        if (other instanceof AbstractGBCRomEntry gbcOther) {
            extraTableFile = gbcOther.extraTableFile;
            if (getIntValue("CopyTMText") == 1) {
                tmTexts.addAll(gbcOther.tmTexts);
            }
        }
    }

}
