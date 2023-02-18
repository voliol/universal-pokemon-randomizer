package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.graphics.PaletteHandler;
import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.romhandlers.AbstractRomHandler;
import com.dabomstew.pkrandom.romhandlers.romentries.RomEntry;

import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TestRomHandler extends AbstractRomHandler {
    public TestRomHandler(Random random, PrintStream logStream) {
        super(random, logStream);
    }

    @Override
    public PaletteHandler getPaletteHandler() {
        return null;
    }

    @Override
    protected List<BufferedImage> getAllPokemonImages() {
        return null;
    }

    @Override
    protected void savePokemonPalettes() {

    }

    @Override
    public boolean loadRom(String filename) {
        return false;
    }

    @Override
    public String loadedFilename() {
        return null;
    }

    @Override
    public boolean hasGameUpdateLoaded() {
        return false;
    }

    @Override
    public boolean loadGameUpdate(String filename) {
        return false;
    }

    @Override
    public void removeGameUpdate() {

    }

    @Override
    public String getGameUpdateVersion() {
        return null;
    }

    @Override
    public void printRomDiagnostics(PrintStream logStream) {

    }

    @Override
    public boolean isRomValid() {
        return false;
    }

    @Override
    public List<Pokemon> getPokemon() {
        return null;
    }

    @Override
    public List<Pokemon> getPokemonInclFormes() {
        return null;
    }

    @Override
    public PokemonSet<Pokemon> getAltFormes() {
        return null;
    }

    @Override
    public List<MegaEvolution> getMegaEvolutions() {
        return null;
    }

    @Override
    public Pokemon getAltFormeOfPokemon(Pokemon pk, int forme) {
        return null;
    }

    @Override
    public PokemonSet<Pokemon> getIrregularFormes() {
        return null;
    }

    @Override
    public void removeEvosForPokemonPool() {

    }

    @Override
    public List<Pokemon> getStarters() {
        return null;
    }

    @Override
    public boolean setStarters(List<Pokemon> newStarters) {
        return false;
    }

    @Override
    public boolean hasStarterAltFormes() {
        return false;
    }

    @Override
    public int starterCount() {
        return 0;
    }

    @Override
    public boolean supportsStarterHeldItems() {
        return false;
    }

    @Override
    public List<Integer> getStarterHeldItems() {
        return null;
    }

    @Override
    public void setStarterHeldItems(List<Integer> items) {

    }

    @Override
    public Map<Integer, StatChange> getUpdatedPokemonStats(int generation) {
        return null;
    }

    @Override
    public int abilitiesPerPokemon() {
        return 0;
    }

    @Override
    public int highestAbilityIndex() {
        return 0;
    }

    @Override
    public Map<Integer, List<Integer>> getAbilityVariations() {
        return null;
    }

    @Override
    public boolean hasMegaEvolutions() {
        return false;
    }

    @Override
    public List<EncounterSet> getEncounters(boolean useTimeOfDay) {
        return null;
    }

    @Override
    public void setEncounters(boolean useTimeOfDay, List<EncounterSet> encounters) {

    }

    @Override
    public boolean hasWildAltFormes() {
        return false;
    }

    @Override
    public void enableGuaranteedPokemonCatching() {

    }

    @Override
    public List<Trainer> getTrainers() {
        return null;
    }

    @Override
    public List<Integer> getMainPlaythroughTrainers() {
        return null;
    }

    @Override
    public List<Integer> getEliteFourTrainers(boolean isChallengeMode) {
        return null;
    }

    @Override
    public void setTrainers(List<Trainer> trainerData, boolean doubleBattleMode) {

    }

    @Override
    public boolean hasPhysicalSpecialSplit() {
        return false;
    }

    @Override
    public List<Move> getMoves() {
        return null;
    }

    @Override
    public Map<Integer, List<MoveLearnt>> getMovesLearnt() {
        return null;
    }

    @Override
    public void setMovesLearnt(Map<Integer, List<MoveLearnt>> movesets) {

    }

    @Override
    public Map<Integer, List<Integer>> getEggMoves() {
        return null;
    }

    @Override
    public void setEggMoves(Map<Integer, List<Integer>> eggMoves) {

    }

    @Override
    public boolean supportsFourStartingMoves() {
        return false;
    }

    @Override
    public List<StaticEncounter> getStaticPokemon() {
        return null;
    }

    @Override
    public boolean setStaticPokemon(List<StaticEncounter> staticPokemon) {
        return false;
    }

    @Override
    public boolean canChangeStaticPokemon() {
        return false;
    }

    @Override
    public boolean hasStaticAltFormes() {
        return false;
    }

    @Override
    public boolean hasMainGameLegendaries() {
        return false;
    }

    @Override
    public List<Integer> getMainGameLegendaries() {
        return null;
    }

    @Override
    public List<Integer> getSpecialMusicStatics() {
        return null;
    }

    @Override
    public void applyCorrectStaticMusic(Map<Integer, Integer> specialMusicStaticChanges) {

    }

    @Override
    public boolean hasStaticMusicFix() {
        return false;
    }

    @Override
    public List<TotemPokemon> getTotemPokemon() {
        return null;
    }

    @Override
    public void setTotemPokemon(List<TotemPokemon> totemPokemon) {

    }

    @Override
    public List<Integer> getTMMoves() {
        return null;
    }

    @Override
    public List<Integer> getHMMoves() {
        return null;
    }

    @Override
    public void setTMMoves(List<Integer> moveIndexes) {

    }

    @Override
    public int getTMCount() {
        return 0;
    }

    @Override
    public int getHMCount() {
        return 0;
    }

    @Override
    public Map<Pokemon, boolean[]> getTMHMCompatibility() {
        return null;
    }

    @Override
    public void setTMHMCompatibility(Map<Pokemon, boolean[]> compatData) {

    }

    @Override
    public boolean hasMoveTutors() {
        return false;
    }

    @Override
    public List<Integer> getMoveTutorMoves() {
        return null;
    }

    @Override
    public void setMoveTutorMoves(List<Integer> moves) {

    }

    @Override
    public Map<Pokemon, boolean[]> getMoveTutorCompatibility() {
        return null;
    }

    @Override
    public void setMoveTutorCompatibility(Map<Pokemon, boolean[]> compatData) {

    }

    @Override
    public boolean canChangeTrainerText() {
        return false;
    }

    @Override
    public List<String> getTrainerNames() {
        return null;
    }

    @Override
    public void setTrainerNames(List<String> trainerNames) {

    }

    @Override
    public TrainerNameMode trainerNameMode() {
        return null;
    }

    @Override
    public List<Integer> getTCNameLengthsByTrainer() {
        return null;
    }

    @Override
    public List<String> getTrainerClassNames() {
        return null;
    }

    @Override
    public void setTrainerClassNames(List<String> trainerClassNames) {

    }

    @Override
    public boolean fixedTrainerClassNamesLength() {
        return false;
    }

    @Override
    public List<Integer> getDoublesTrainerClasses() {
        return null;
    }

    @Override
    public ItemList getAllowedItems() {
        return null;
    }

    @Override
    public ItemList getNonBadItems() {
        return null;
    }

    @Override
    public List<Integer> getEvolutionItems() {
        return null;
    }

    @Override
    public List<Integer> getUniqueNoSellItems() {
        return null;
    }

    @Override
    public List<Integer> getRegularShopItems() {
        return null;
    }

    @Override
    public List<Integer> getOPShopItems() {
        return null;
    }

    @Override
    public String[] getItemNames() {
        return new String[0];
    }

    @Override
    public List<Integer> getRequiredFieldTMs() {
        return null;
    }

    @Override
    public List<Integer> getCurrentFieldTMs() {
        return null;
    }

    @Override
    public void setFieldTMs(List<Integer> fieldTMs) {

    }

    @Override
    public List<Integer> getRegularFieldItems() {
        return null;
    }

    @Override
    public void setRegularFieldItems(List<Integer> items) {

    }

    @Override
    public boolean hasShopRandomization() {
        return false;
    }

    @Override
    public Map<Integer, Shop> getShopItems() {
        return null;
    }

    @Override
    public void setShopItems(Map<Integer, Shop> shopItems) {

    }

    @Override
    public void setShopPrices() {

    }

    @Override
    public List<IngameTrade> getIngameTrades() {
        return null;
    }

    @Override
    public void setIngameTrades(List<IngameTrade> trades) {

    }

    @Override
    public boolean hasDVs() {
        return false;
    }

    @Override
    public void removeImpossibleEvolutions(Settings settings) {

    }

    @Override
    public void makeEvolutionsEasier(Settings settings) {

    }

    @Override
    public void removeTimeBasedEvolutions() {

    }

    @Override
    public List<Integer> getFieldMoves() {
        return null;
    }

    @Override
    public List<Integer> getEarlyRequiredHMMoves() {
        return null;
    }

    @Override
    public String getROMName() {
        return null;
    }

    @Override
    public String getROMCode() {
        return null;
    }

    @Override
    public String getSupportLevel() {
        return null;
    }

    @Override
    public String getDefaultExtension() {
        return null;
    }

    @Override
    public int internalStringLength(String string) {
        return 0;
    }

    @Override
    public void randomizeIntroPokemon() {

    }

    @Override
    public BufferedImage getMascotImage() {
        return null;
    }

    @Override
    public int generationOfPokemon() {
        return 0;
    }

    @Override
    public boolean isEffectivenessUpdated() {
        return false;
    }

    @Override
    public boolean hasFunctionalFormes() {
        return false;
    }

    @Override
    protected void saveMoves() {

    }

    @Override
    protected void savePokemonStats() {

    }

    @Override
    protected boolean saveRomFile(String filename, long seed) {
        return false;
    }

    @Override
    protected boolean saveRomDirectory(String filename) {
        return false;
    }

    @Override
    protected RomEntry getRomEntry() {
        return null;
    }
}
