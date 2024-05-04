package com.dabomstew.pkrandom.updaters;

import com.dabomstew.pkrandom.romhandlers.RomHandler;

public class Updater {

    protected final RomHandler romHandler;

    protected boolean updated;

    public Updater(RomHandler romHandler) {
        this.romHandler = romHandler;
    }

    public boolean isUpdated() {
        return updated;
    }
}
