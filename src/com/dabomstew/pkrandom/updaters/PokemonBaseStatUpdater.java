package com.dabomstew.pkrandom.updaters;

import com.dabomstew.pkrandom.constants.Species;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.util.List;
import java.util.function.Consumer;

public class PokemonBaseStatUpdater extends Updater {

    // starts with two null-consumers so the indexing can be nicer,
    // and then four more since Gens 2-5 didn't change the base stats of any existing Pokemon
    private final List<Consumer<List<Pokemon>>> updates = List.of(
            l -> {}, l -> {},
            l -> {}, l -> {}, l -> {}, l -> {},
            this::gen6Updates, this::gen7Updates, this::gen8Updates, this::gen9Updates
    );

    public PokemonBaseStatUpdater(RomHandler romHandler) {
        super(romHandler);
    }

    public void updatePokemonStats(int updateToGen) {
        if (updateToGen > updates.size() - 1) {
            throw new IllegalArgumentException("updateToGen too high, can't update to Gen " + updateToGen);
        }
        List<Pokemon> pokes = romHandler.getPokemonInclFormes();

        for (int i = 2; i <= updates.size(); i++) {
            if (updateToGen >= i && romHandler.generationOfPokemon() < i) {
                updates.get(i).accept(pokes);
            }
        }

        updated = true;
    }

    private void gen6Updates(List<Pokemon> pokes) {
        if (romHandler.generationOfPokemon() == 1) {
            // These are the Gen 1 Pokemon that otherwise get their SpAtk updated
            updateSpecial(pokes, Species.butterfree, 90);
            updateSpecial(pokes, Species.clefable, 95);
            updateSpecial(pokes, Species.vileplume, 110);
        }

        updateSpAtk(pokes, Species.butterfree, 90);
        updateAtk(pokes, Species.beedrill, 90);
        updateSpeed(pokes, Species.pidgeot, 101);
        updateSpeed(pokes, Species.raichu, 92);
        updateAtk(pokes, Species.nidoqueen, 92);
        updateAtk(pokes, Species.nidoking, 102);
        updateSpAtk(pokes, Species.clefable, 95);
        updateSpAtk(pokes, Species.vileplume, 110);
        updateAtk(pokes, Species.poliwrath, 95);
        updateSpDef(pokes, Species.alakazam, 95);
        updateSpDef(pokes, Species.victreebel, 70);
        updateAtk(pokes, Species.golem, 120);

        if (romHandler.generationOfPokemon() >= 2) {
            updateDef(pokes, Species.ampharos, 85);
            updateDef(pokes, Species.bellossom, 95);
            updateSpAtk(pokes, Species.azumarill, 60);
            updateSpDef(pokes, Species.jumpluff, 95);
        }
        if (romHandler.generationOfPokemon() >= 3) {
            updateSpAtk(pokes, Species.beautifly, 100);
            updateSpDef(pokes, Species.exploud, 73);
        }
        if (romHandler.generationOfPokemon() >= 4) {
            updateSpDef(pokes, Species.staraptor, 60);
            updateDef(pokes, Species.roserade, 65);
        }
        if (romHandler.generationOfPokemon() >= 5) {
            updateAtk(pokes, Species.stoutland, 110);
            updateAtk(pokes, Species.unfezant, 115);
            updateSpDef(pokes, Species.gigalith, 80);
            updateAtk(pokes, Species.seismitoad, 95);
            updateSpDef(pokes, Species.leavanny, 80);
            updateAtk(pokes, Species.scolipede, 100);
            updateDef(pokes, Species.krookodile, 80);
        }
    }

    private void gen7Updates(List<Pokemon> pokes) {
        updateAtk(pokes, Species.arbok, 95);
        updateAtk(pokes, Species.dugtrio, 100);
        updateAtk(pokes, Species.farfetchd, 90);
        updateSpeed(pokes, Species.dodrio, 110);
        updateSpeed(pokes, Species.electrode, 150);
        updateSpDef(pokes, Species.exeggutor, 75);

        if (romHandler.generationOfPokemon() >= 2) {
            updateSpAtk(pokes, Species.noctowl, 86);
            updateSpDef(pokes, Species.ariados, 70);
            updateDef(pokes, Species.qwilfish, 85);
            updateHP(pokes, Species.magcargo, 60);
            updateSpAtk(pokes, Species.magcargo, 90);
            updateHP(pokes, Species.corsola, 65);
            updateDef(pokes, Species.corsola, 95);
            updateSpDef(pokes, Species.corsola, 95);
            updateHP(pokes, Species.mantine, 85);
        }
        if (romHandler.generationOfPokemon() >= 3) {
            updateSpAtk(pokes, Species.swellow, 75);
            updateSpAtk(pokes, Species.pelipper, 95);
            updateSpAtk(pokes, Species.masquerain, 100);
            updateSpeed(pokes, Species.masquerain, 80);
            updateSpeed(pokes, Species.delcatty, 90);
            updateDef(pokes, Species.volbeat, 75);
            updateSpDef(pokes, Species.volbeat, 85);
            updateDef(pokes, Species.illumise, 75);
            updateSpDef(pokes, Species.illumise, 85);
            updateHP(pokes, Species.lunatone, 90);
            updateHP(pokes, Species.solrock, 90);
            updateHP(pokes, Species.chimecho, 75);
            updateDef(pokes, Species.chimecho, 80);
            updateSpDef(pokes, Species.chimecho, 90);
        }
        if (romHandler.generationOfPokemon() >= 5) {
            updateHP(pokes, Species.woobat, 65);
            updateAtk(pokes, Species.crustle, 105);
            updateAtk(pokes, Species.beartic, 130);
            updateHP(pokes, Species.cryogonal, 80);
            updateDef(pokes, Species.cryogonal, 50);
        }

        if (romHandler.generationOfPokemon() == 6) {
            updateSpDef(pokes, Species.Gen6Formes.alakazamMega, 105);
            updateAtk(pokes, Species.Gen6Formes.aegislashB, 140);
            updateSpAtk(pokes, Species.Gen6Formes.aegislashB, 140);
        }
    }

    private void gen8Updates(List<Pokemon> pokes) {
        updateDef(pokes, Species.aegislash, 140);
        updateSpDef(pokes, Species.aegislash, 140);
        int aegislashBlade;
        if (romHandler.generationOfPokemon() == 6) {
            aegislashBlade = Species.Gen6Formes.aegislashB;
        } else { // Gen 7
            aegislashBlade = romHandler.isUSUM() ? Species.USUMFormes.aegislashB : Species.SMFormes.aegislashB;
        }
        updateAtk(pokes, aegislashBlade, 140);
        updateSpAtk(pokes, aegislashBlade, 140);
    }

    private void gen9Updates(List<Pokemon> pokes) {
        updateDef(pokes, Species.cresselia, 110);
        updateSpDef(pokes, Species.cresselia, 120);
        updateAtk(pokes, Species.zacian, 120);
        updateAtk(pokes, Species.zamazenta, 120);
    }

    private void updateHP(List<Pokemon> pokes, int species, int value) {
        pokes.get(species).setHp(value);
    }

    private void updateAtk(List<Pokemon> pokes, int species, int value) {
        pokes.get(species).setAttack(value);
    }

    private void updateDef(List<Pokemon> pokes, int species, int value) {
        pokes.get(species).setDefense(value);
    }

    private void updateSpAtk(List<Pokemon> pokes, int species, int value) {
        // just gets ignored in Gen 1 games
        if (romHandler.generationOfPokemon() != 1) {
            pokes.get(species).setSpatk(value);
        }
    }

    private void updateSpDef(List<Pokemon> pokes, int species, int value) {
        // just gets ignored in Gen 1 games
        if (romHandler.generationOfPokemon() != 1) {
            pokes.get(species).setSpdef(value);
        }
    }

    private void updateSpeed(List<Pokemon> pokes, int species, int value) {
        pokes.get(species).setSpeed(value);
    }

    private void updateSpecial(List<Pokemon> pokes, int species, int value) {
        // just gets ignored in non-Gen 1 games
        if (romHandler.generationOfPokemon() == 1) {
            pokes.get(species).setSpecial(value);
        }
    }

}
