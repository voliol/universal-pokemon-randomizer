package com.dabomstew.pkrandom.romhandlers.romentries;

import com.dabomstew.pkrandom.newnds.NARCArchive;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.romhandlers.AbstractDSRomHandler;
import com.dabomstew.pkrandom.romhandlers.Gen5RomHandler;

public class DSStaticPokemon {

    protected ScriptInFileEntry[] speciesEntries;
    protected ScriptInFileEntry[] formeEntries;
    protected ScriptInFileEntry[] levelEntries;

    public DSStaticPokemon(ScriptInFileEntry[] speciesEntries, ScriptInFileEntry[] formeEntries, ScriptInFileEntry[] levelEntries) {
        this.speciesEntries = speciesEntries;
        this.formeEntries = formeEntries;
        this.levelEntries = levelEntries;
    }

    public Pokemon getPokemon(AbstractDSRomHandler parent, NARCArchive scriptNARC) {
        return parent.getPokemon().get(parent.readWord(scriptNARC.files.get(speciesEntries[0].getFile()),
                speciesEntries[0].getOffset()));
    }

    public void setPokemon(AbstractDSRomHandler parent, NARCArchive scriptNARC, Pokemon pkmn) {
        int value = pkmn.getNumber();
        for (int i = 0; i < speciesEntries.length; i++) {
            byte[] file = scriptNARC.files.get(speciesEntries[i].getFile());
            parent.writeWord(file, speciesEntries[i].getOffset(), value);
        }
    }

    public int getForme(NARCArchive scriptNARC) {
        if (formeEntries.length == 0) {
            return 0;
        }
        byte[] file = scriptNARC.files.get(formeEntries[0].getFile());
        return file[formeEntries[0].getOffset()];
    }

    public void setForme(NARCArchive scriptNARC, int forme) {
        for (int i = 0; i < formeEntries.length; i++) {
            byte[] file = scriptNARC.files.get(formeEntries[i].getFile());
            file[formeEntries[i].getOffset()] = (byte) forme;
        }
    }

    public int getLevelCount() {
        return levelEntries.length;
    }

    public int getLevel(NARCArchive scriptOrMapNARC, int i) {
        if (levelEntries.length <= i) {
            return 1;
        }
        byte[] file = scriptOrMapNARC.files.get(levelEntries[i].getFile());
        return file[levelEntries[i].getOffset()];
    }

    public void setLevel(NARCArchive scriptOrMapNARC, int level, int i) {
        if (levelEntries.length > i) { // Might not have a level entry e.g., it's an egg
            byte[] file = scriptOrMapNARC.files.get(levelEntries[i].getFile());
            file[levelEntries[i].getOffset()] = (byte) level;
        }
    }

}
