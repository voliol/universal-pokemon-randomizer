package com.dabomstew.pkrandom.updaters;

import com.dabomstew.pkrandom.romhandlers.RomHandler;

/**
 * An abstract superclass for objects that "update" some aspect of a game (via a {@link RomHandler})
 * to be like in a later Generation. This of course has a Vanilla perspective, so these classes might
 * not work great with ROM hacks, once those are otherwise supported.
 */
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
