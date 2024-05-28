package com.dabomstew.pkrandom.updaters;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.constants.Moves;
import com.dabomstew.pkrandom.pokemon.Move;
import com.dabomstew.pkrandom.pokemon.MoveCategory;
import com.dabomstew.pkrandom.pokemon.Type;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MoveUpdater extends Updater {

    private final Map<Integer, boolean[]> moveUpdates = new HashMap<>();

    // starts with two null-consumers so the indexing can be nicer
    private final List<Consumer<List<Move>>> updates = List.of(
            l -> {}, l -> {},
            this::gen2Updates, this::gen3Updates, this::gen4Updates, this::gen5Updates,
            this::gen6Updates, this::gen7Updates, this::gen8Updates, this::gen9Updates
    );

    public MoveUpdater(RomHandler romHandler) {
        super(romHandler);
    }

    /**
     * Returns the needed info to log Move updates.
     */
    public Map<Integer, boolean[]> getMoveUpdates() {
        return moveUpdates;
    }

    public void updateMoves(int updateToGen) {
        if (updateToGen > updates.size() - 1) {
            throw new IllegalArgumentException("updateToGen too high, can't update to Gen " + updateToGen);
        }
        List<Move> moves = romHandler.getMoves();

        for (int i = 2; i <= updates.size(); i++) {
            if (updateToGen >= i && romHandler.generationOfPokemon() < i) {
                updates.get(i).accept(moves);
            }
        }
        updated = true;
    }

    private void gen2Updates(List<Move> moves) {
        updateMoveType(moves, Moves.karateChop, Type.FIGHTING);
        updateMoveType(moves, Moves.gust, Type.FLYING);
        updateMovePower(moves, Moves.wingAttack, 60);
        updateMoveAccuracy(moves, Moves.whirlwind, 100);
        updateMoveType(moves, Moves.sandAttack, Type.GROUND);
        updateMovePower(moves, Moves.doubleEdge, 120);
        updateMoveAccuracy(moves, Moves.blizzard, 70);
        updateMoveAccuracy(moves, Moves.rockThrow, 90);
        updateMoveAccuracy(moves, Moves.hypnosis, 60);
        updateMovePower(moves, Moves.selfDestruct, 200);
        updateMovePower(moves, Moves.explosion, 250);
        updateMovePower(moves, Moves.dig, 60);
        // Bite also becomes dark between Gen 1 and 2, but we can't change moves to non-existing types
    }

    private void gen3Updates(List<Move> moves) {
        updateMoveAccuracy(moves, Moves.razorWind, 100);
        // Low Kick is a good example of how these "updates" don't update any mechanics,
        // just power/accuracy/pp/category/Type
        // Low Kick gets the accuracy boost of Gen 3+, but not the weight-based power.
        updateMoveAccuracy(moves, Moves.lowKick, 100);
    }

    private void gen4Updates(List<Move> moves) {
        updateMovePower(moves, Moves.fly, 90);
        updateMovePP(moves, Moves.vineWhip, 15);
        updateMovePP(moves, Moves.absorb, 25);
        updateMovePP(moves, Moves.megaDrain, 15);
        updateMovePower(moves, Moves.dig, 80);
        updateMovePP(moves, Moves.recover, 10);
        updateMoveAccuracy(moves, Moves.flash, 100);
        updateMovePower(moves, Moves.petalDance, 90);
        updateMoveAccuracy(moves, Moves.disable, 80);
        updateMovePower(moves, Moves.jumpKick, 85);
        updateMovePower(moves, Moves.highJumpKick, 100);

        if (romHandler.generationOfPokemon() >= 2) {
            updateMovePower(moves, Moves.zapCannon, 120);
            updateMovePower(moves, Moves.outrage, 120);
            updateMovePP(moves, Moves.outrage, 10);
            updateMovePP(moves, Moves.gigaDrain, 10);
            updateMovePower(moves, Moves.rockSmash, 40);
        }

        if (romHandler.generationOfPokemon() == 3) {
            updateMovePP(moves, Moves.stockpile, 20);
            updateMovePower(moves, Moves.dive, 80);
            updateMovePower(moves, Moves.leafBlade, 90);
        }
    }

    private void gen5Updates(List<Move> moves) {
        updateMoveAccuracy(moves, Moves.bind, 85);
        updateMovePP(moves, Moves.jumpKick, 10);
        updateMovePower(moves, Moves.jumpKick, 100);
        updateMovePower(moves, Moves.tackle, 50);
        updateMoveAccuracy(moves, Moves.tackle, 100);
        updateMoveAccuracy(moves, Moves.wrap, 90);
        updateMovePP(moves, Moves.thrash, 10);
        updateMovePower(moves, Moves.thrash, 120);
        updateMoveAccuracy(moves, Moves.disable, 100);
        updateMovePP(moves, Moves.petalDance, 10);
        updateMovePower(moves, Moves.petalDance, 120);
        updateMoveAccuracy(moves, Moves.fireSpin, 85);
        updateMovePower(moves, Moves.fireSpin, 35);
        updateMoveAccuracy(moves, Moves.toxic, 90);
        updateMoveAccuracy(moves, Moves.clamp, 85);
        updateMovePP(moves, Moves.clamp, 15);
        updateMovePP(moves, Moves.highJumpKick, 10);
        updateMovePower(moves, Moves.highJumpKick, 130);
        updateMoveAccuracy(moves, Moves.glare, 90);
        updateMoveAccuracy(moves, Moves.poisonGas, 80);
        updateMoveAccuracy(moves, Moves.crabhammer, 90);

        if (romHandler.generationOfPokemon() >= 2) {
            updateMoveType(moves, Moves.curse, Type.GHOST);
            updateMoveAccuracy(moves, Moves.cottonSpore, 100);
            updateMoveAccuracy(moves, Moves.scaryFace, 100);
            updateMoveAccuracy(moves, Moves.boneRush, 90);
            updateMovePower(moves, Moves.gigaDrain, 75);
            updateMovePower(moves, Moves.furyCutter, 20);
            updateMovePP(moves, Moves.futureSight, 10);
            updateMovePower(moves, Moves.futureSight, 100);
            updateMoveAccuracy(moves, Moves.futureSight, 100);
            updateMovePower(moves, Moves.whirlpool, 35);
            updateMoveAccuracy(moves, Moves.whirlpool, 85);
        }

        if (romHandler.generationOfPokemon() >= 3) {
            updateMovePower(moves, Moves.uproar, 90);
            updateMovePower(moves, Moves.sandTomb, 35);
            updateMoveAccuracy(moves, Moves.sandTomb, 85);
            updateMovePower(moves, Moves.bulletSeed, 25);
            updateMovePower(moves, Moves.icicleSpear, 25);
            updateMovePower(moves, Moves.covet, 60);
            updateMoveAccuracy(moves, Moves.rockBlast, 90);
            updateMovePower(moves, Moves.doomDesire, 140);
            updateMoveAccuracy(moves, Moves.doomDesire, 100);
        }

        if (romHandler.generationOfPokemon() == 4) {
            updateMovePower(moves, Moves.feint, 30);
            updateMovePower(moves, Moves.lastResort, 140);
            updateMovePP(moves, Moves.drainPunch, 10);
            updateMovePower(moves, Moves.drainPunch, 75);
            updateMoveAccuracy(moves, Moves.magmaStorm, 75);
        }
    }

    private void gen6Updates(List<Move> moves) {
        updateMovePP(moves, Moves.swordsDance, 20);
        updateMoveAccuracy(moves, Moves.whirlwind, romHandler.getPerfectAccuracy());
        updateMovePP(moves, Moves.vineWhip, 25);
        updateMovePower(moves, Moves.vineWhip, 45);
        updateMovePower(moves, Moves.pinMissile, 25);
        updateMoveAccuracy(moves, Moves.pinMissile, 95);
        updateMovePower(moves, Moves.flamethrower, 90);
        updateMovePower(moves, Moves.hydroPump, 110);
        updateMovePower(moves, Moves.surf, 90);
        updateMovePower(moves, Moves.iceBeam, 90);
        updateMovePower(moves, Moves.blizzard, 110);
        updateMovePP(moves, Moves.growth, 20);
        updateMovePower(moves, Moves.thunderbolt, 90);
        updateMovePower(moves, Moves.thunder, 110);
        updateMovePP(moves, Moves.minimize, 10);
        updateMovePP(moves, Moves.barrier, 20);
        updateMovePower(moves, Moves.lick, 30);
        updateMovePower(moves, Moves.smog, 30);
        updateMovePower(moves, Moves.fireBlast, 110);
        updateMovePP(moves, Moves.skullBash, 10);
        updateMovePower(moves, Moves.skullBash, 130);
        updateMoveAccuracy(moves, Moves.glare, 100);
        updateMoveAccuracy(moves, Moves.poisonGas, 90);
        updateMovePower(moves, Moves.bubble, 40);
        updateMoveAccuracy(moves, Moves.psywave, 100);
        updateMovePP(moves, Moves.acidArmor, 20);
        updateMovePower(moves, Moves.crabhammer, 100);

        if (romHandler.generationOfPokemon() >= 2) {
            updateMovePP(moves, Moves.thief, 25);
            updateMovePower(moves, Moves.thief, 60);
            updateMovePower(moves, Moves.snore, 50);
            updateMovePower(moves, Moves.furyCutter, 40);
            updateMovePower(moves, Moves.futureSight, 120);
        }

        if (romHandler.generationOfPokemon() >= 3) {
            updateMovePower(moves, Moves.heatWave, 95);
            updateMoveAccuracy(moves, Moves.willOWisp, 85);
            updateMovePower(moves, Moves.smellingSalts, 70);
            updateMovePower(moves, Moves.knockOff, 65);
            updateMovePower(moves, Moves.meteorMash, 90);
            updateMoveAccuracy(moves, Moves.meteorMash, 90);
            updateMovePower(moves, Moves.airCutter, 60);
            updateMovePower(moves, Moves.overheat, 130);
            updateMovePP(moves, Moves.rockTomb, 15);
            updateMovePower(moves, Moves.rockTomb, 60);
            updateMoveAccuracy(moves, Moves.rockTomb, 95);
            updateMovePP(moves, Moves.extrasensory, 20);
            updateMovePower(moves, Moves.muddyWater, 90);
            updateMovePP(moves, Moves.covet, 25);
        }

        if (romHandler.generationOfPokemon() >= 4) {
            updateMovePower(moves, Moves.wakeUpSlap, 70);
            updateMovePP(moves, Moves.tailwind, 15);
            updateMovePower(moves, Moves.assurance, 60);
            updateMoveAccuracy(moves, Moves.psychoShift, 100);
            updateMovePower(moves, Moves.auraSphere, 80);
            updateMovePP(moves, Moves.airSlash, 15);
            updateMovePower(moves, Moves.dragonPulse, 85);
            updateMovePower(moves, Moves.powerGem, 80);
            updateMovePower(moves, Moves.energyBall, 90);
            updateMovePower(moves, Moves.dracoMeteor, 130);
            updateMovePower(moves, Moves.leafStorm, 130);
            updateMoveAccuracy(moves, Moves.gunkShot, 80);
            updateMovePower(moves, Moves.chatter, 65);
            updateMovePower(moves, Moves.magmaStorm, 100);
        }

        if (romHandler.generationOfPokemon() == 5) {
            updateMovePower(moves, Moves.synchronoise, 120);
            updateMovePower(moves, Moves.lowSweep, 65);
            updateMovePower(moves, Moves.hex, 65);
            updateMovePower(moves, Moves.incinerate, 60);
            updateMovePower(moves, Moves.waterPledge, 80);
            updateMovePower(moves, Moves.firePledge, 80);
            updateMovePower(moves, Moves.grassPledge, 80);
            updateMovePower(moves, Moves.struggleBug, 50);
            // Frost Breath and Storm Throw 45 Power
            // Crits are 2x in these games, so we need to multiply BP by 3/4
            // Storm Throw was also updated to have a base BP of 60
            updateMovePower(moves, Moves.frostBreath, 45);
            updateMovePower(moves, Moves.stormThrow, 45);
            updateMovePP(moves, Moves.sacredSword, 15);
            updateMovePower(moves, Moves.hurricane, 110);
            updateMovePower(moves, Moves.technoBlast, 120);
        }
    }

    private void gen7Updates(List<Move> moves) {
        updateMovePower(moves, Moves.leechLife, 80);
        updateMovePP(moves, Moves.leechLife, 10);
        updateMovePP(moves, Moves.submission, 20);
        updateMovePower(moves, Moves.tackle, 40);
        updateMoveAccuracy(moves, Moves.thunderWave, 90);

        if (romHandler.generationOfPokemon() >= 2) {
            updateMoveAccuracy(moves, Moves.swagger, 85);
        }

        if (romHandler.generationOfPokemon() >= 3) {
            updateMovePP(moves, Moves.knockOff, 20);
        }

        if (romHandler.generationOfPokemon() >= 4) {
            updateMoveAccuracy(moves, Moves.darkVoid, 50);
            updateMovePower(moves, Moves.suckerPunch, 70);
        }

        if (romHandler.generationOfPokemon() == 6) {
            updateMoveAccuracy(moves, Moves.aromaticMist, romHandler.getPerfectAccuracy());
            updateMovePower(moves, Moves.fellStinger, 50);
            updateMovePower(moves, Moves.flyingPress, 100);
            updateMovePP(moves, Moves.matBlock, 10);
            updateMovePower(moves, Moves.mysticalFire, 75);
            updateMovePower(moves, Moves.parabolicCharge, 65);
            updateMoveAccuracy(moves, Moves.topsyTurvy, romHandler.getPerfectAccuracy());
            updateMoveCategory(moves, Moves.waterShuriken, MoveCategory.SPECIAL);
        }
    }

    private void gen8Updates(List<Move> moves) {
        if (romHandler.generationOfPokemon() >= 2) {
            updateMovePower(moves, Moves.rapidSpin, 50);
        }

        if (romHandler.generationOfPokemon() == 7) {
            updateMovePower(moves, Moves.multiAttack, 120);
        }
    }

    private void gen9Updates(List<Move> moves) {
        updateMovePP(moves, Moves.recover, 5);
        updateMovePP(moves, Moves.softBoiled, 5);
        updateMovePP(moves, Moves.rest, 5);

        if (romHandler.generationOfPokemon() >= 2) {
            updateMovePP(moves, Moves.milkDrink, 5);
        }

        if (romHandler.generationOfPokemon() >= 3) {
            updateMovePP(moves, Moves.slackOff, 5);
        }

        if (romHandler.generationOfPokemon() >= 4) {
            updateMovePP(moves, Moves.roost, 5);
        }

        if (romHandler.generationOfPokemon() >= 7) {
            updateMovePP(moves, Moves.shoreUp, 5);
        }

        if (romHandler.generationOfPokemon() >= 8) {
            updateMovePower(moves, Moves.grassyGlide, 60);
            updateMovePower(moves, Moves.wickedBlow, 75);
            updateMovePower(moves, Moves.glacialLance, 120);
        }
    }

    private void updateMovePower(List<Move> moves, int moveNum, int power) {
        Move mv = moves.get(moveNum);
        if (mv.power != power) {
            mv.power = power;
            addMoveUpdate(moveNum, 0);
        }
    }

    private void updateMovePP(List<Move> moves, int moveNum, int pp) {
        Move mv = moves.get(moveNum);
        if (mv.pp != pp) {
            mv.pp = pp;
            addMoveUpdate(moveNum, 1);
        }
    }

    private void updateMoveAccuracy(List<Move> moves, int moveNum, int accuracy) {
        Move mv = moves.get(moveNum);
        if (Math.abs(mv.hitratio - accuracy) >= 1) {
            mv.hitratio = accuracy;
            addMoveUpdate(moveNum, 2);
        }
    }

    private void updateMoveType(List<Move> moves, int moveNum, Type type) {
        Move mv = moves.get(moveNum);
        if (mv.type != type) {
            mv.type = type;
            addMoveUpdate(moveNum, 3);
        }
    }

    private void updateMoveCategory(List<Move> moves, int moveNum, MoveCategory category) {
        Move mv = moves.get(moveNum);
        if (mv.category != category) {
            mv.category = category;
            addMoveUpdate(moveNum, 4);
        }
    }

    private void addMoveUpdate(int moveNum, int updateType) {
        if (!moveUpdates.containsKey(moveNum)) {
            boolean[] updateField = new boolean[5];
            updateField[updateType] = true;
            moveUpdates.put(moveNum, updateField);
        } else {
            moveUpdates.get(moveNum)[updateType] = true;
        }
    }

}
