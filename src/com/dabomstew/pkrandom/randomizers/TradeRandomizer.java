package com.dabomstew.pkrandom.randomizers;

import com.dabomstew.pkrandom.CustomNamesSet;
import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.IngameTrade;
import com.dabomstew.pkrandom.pokemon.ItemList;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.romhandlers.RomHandler;
import com.dabomstew.pkrandom.services.RestrictedPokemonService;

import java.util.*;

public class TradeRandomizer {

    private final RomHandler romHandler;
    private final RestrictedPokemonService rPokeService;
    private final Settings settings;
    private final Random random;

    public TradeRandomizer(RomHandler romHandler, Settings settings, Random random) {
        this.romHandler = romHandler;
        this.rPokeService = romHandler.getRestrictedPokemonService();
        this.settings = settings;
        this.random = random;
    }

    public void randomizeIngameTrades() {
        boolean randomizeRequest = settings.getInGameTradesMod() == Settings.InGameTradesMod.RANDOMIZE_GIVEN_AND_REQUESTED;
        boolean randomNickname = settings.isRandomizeInGameTradesNicknames();
        boolean randomOT = settings.isRandomizeInGameTradesOTs();
        boolean randomStats = settings.isRandomizeInGameTradesIVs();
        boolean randomItem = settings.isRandomizeInGameTradesItems();
        CustomNamesSet customNames = settings.getCustomNames();

        // Process trainer names
        List<String> trainerNames = new ArrayList<>();
        // Check for the file
        if (randomOT) {
            int maxOT = romHandler.maxTradeOTNameLength();
            for (String trainername : customNames.getTrainerNames()) {
                int len = romHandler.internalStringLength(trainername);
                if (len <= maxOT && !trainerNames.contains(trainername)) {
                    trainerNames.add(trainername);
                }
            }
        }

        // Process nicknames
        List<String> nicknames = new ArrayList<>();
        // Check for the file
        if (randomNickname) {
            int maxNN = romHandler.maxTradeNicknameLength();
            for (String nickname : customNames.getPokemonNicknames()) {
                int len = romHandler.internalStringLength(nickname);
                if (len <= maxNN && !nicknames.contains(nickname)) {
                    nicknames.add(nickname);
                }
            }
        }

        // get old trades
        List<IngameTrade> trades = romHandler.getIngameTrades();
        List<Pokemon> usedRequests = new ArrayList<>();
        List<Pokemon> usedGivens = new ArrayList<>();
        List<String> usedOTs = new ArrayList<>();
        List<String> usedNicknames = new ArrayList<>();
        ItemList possibleItems = romHandler.getAllowedItems();

        int nickCount = nicknames.size();
        int trnameCount = trainerNames.size();

        for (IngameTrade trade : trades) {
            // pick new given pokemon
            Pokemon oldgiven = trade.givenPokemon;
            Pokemon given = rPokeService.randomPokemon(random);
            while (usedGivens.contains(given)) {
                given = rPokeService.randomPokemon(random);
            }
            usedGivens.add(given);
            trade.givenPokemon = given;

            // requested pokemon?
            if (oldgiven == trade.requestedPokemon) {
                // preserve trades for the same pokemon
                trade.requestedPokemon = given;
            } else if (randomizeRequest) {
                if (trade.requestedPokemon != null) {
                    Pokemon request = rPokeService.randomPokemon(random);
                    while (usedRequests.contains(request) || request == given) {
                        request = rPokeService.randomPokemon(random);
                    }
                    usedRequests.add(request);
                    trade.requestedPokemon = request;
                }
            }

            // nickname?
            if (randomNickname && nickCount > usedNicknames.size()) {
                String nickname = nicknames.get(random.nextInt(nickCount));
                while (usedNicknames.contains(nickname)) {
                    nickname = nicknames.get(random.nextInt(nickCount));
                }
                usedNicknames.add(nickname);
                trade.nickname = nickname;
            } else if (trade.nickname.equalsIgnoreCase(oldgiven.getName())) {
                // change the name for sanity
                trade.nickname = trade.givenPokemon.getName();
            }

            if (randomOT && trnameCount > usedOTs.size()) {
                String ot = trainerNames.get(random.nextInt(trnameCount));
                while (usedOTs.contains(ot)) {
                    ot = trainerNames.get(random.nextInt(trnameCount));
                }
                usedOTs.add(ot);
                trade.otName = ot;
                trade.otId = random.nextInt(65536);
            }

            if (randomStats) {
                int maxIV = romHandler.hasDVs() ? 16 : 32;
                for (int i = 0; i < trade.ivs.length; i++) {
                    trade.ivs[i] = random.nextInt(maxIV);
                }
            }

            if (randomItem) {
                trade.item = possibleItems.randomItem(random);
            }
        }

        // things that the game doesn't support should just be ignored
        romHandler.setIngameTrades(trades);
    }

}
