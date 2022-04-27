package com.dabomstew.pkrandom.gui;

import static org.junit.Assert.*;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import com.dabomstew.pkrandom.MiscTweak;
import com.dabomstew.pkrandom.pokemon.Type;
import com.dabomstew.pkrandom.settings.Settings;

import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JCheckBoxFixture;
import org.assertj.swing.fixture.JListFixture;
import org.assertj.swing.fixture.JRadioButtonFixture;
import org.assertj.swing.fixture.JSliderFixture;
import org.junit.Test;

public class SettingsTest extends AbstractUIBase {

        /**
         * Toggling Gen 6 does not toggle Gen 5
         * 
         * Gen 5 available without Gen 6
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestGen5Separation() throws IOException {
                JCheckBoxFixture updateMovesCBFixture = getCheckBoxByName("goUpdateMovesCheckBox");
                JCheckBoxFixture updateMovesLegacyCBFixture =
                                getCheckBoxByName("goUpdateMovesLegacyCheckBox");
                // Sanity check - Should initialize to False
                checkboxEnabledButNotSelected(updateMovesCBFixture,
                                (settings) -> settings.isUpdateMoves());
                checkboxEnabledButNotSelected(updateMovesLegacyCBFixture,
                                (settings) -> settings.isUpdateMovesLegacy());
                // Sanity check - Should be able to save and reload from state
                String setttingsString = this.mainWindow.getCurrentSettings().toString();
                Settings.fromString(setttingsString);
                // Sanity check - Should restore back to initial form state
                checkboxEnabledButNotSelected(updateMovesCBFixture,
                                (settings) -> settings.isUpdateMoves());
                checkboxEnabledButNotSelected(updateMovesLegacyCBFixture,
                                (settings) -> settings.isUpdateMovesLegacy());

                // Toggle Gen 5
                updateMovesLegacyCBFixture.requireVisible().requireEnabled().click();
                // Legacy should be True while Moves is False
                checkboxEnabledButNotSelected(updateMovesCBFixture,
                                (settings) -> settings.isUpdateMoves());
                checkboxEnabledAndSelected(updateMovesLegacyCBFixture,
                                (settings) -> settings.isUpdateMovesLegacy());
                // Should be able to save and reload from state
                setttingsString = this.mainWindow.getCurrentSettings().toString();
                Settings.fromString(setttingsString);
                // Should restore from settings string
                checkboxEnabledButNotSelected(updateMovesCBFixture,
                                (settings) -> settings.isUpdateMoves());
                checkboxEnabledAndSelected(updateMovesLegacyCBFixture,
                                (settings) -> settings.isUpdateMovesLegacy());

                // Toggle Gen 5 + Gen 6
                updateMovesCBFixture.requireVisible().requireEnabled().click();
                // Legacy and Moves should be True
                checkboxEnabledAndSelected(updateMovesCBFixture,
                                (settings) -> settings.isUpdateMoves());
                checkboxEnabledAndSelected(updateMovesLegacyCBFixture,
                                (settings) -> settings.isUpdateMovesLegacy());
                // Should be able to save and reload from state
                setttingsString = this.mainWindow.getCurrentSettings().toString();
                Settings.fromString(setttingsString);
                // Should restore from settings string
                checkboxEnabledAndSelected(updateMovesCBFixture,
                                (settings) -> settings.isUpdateMoves());
                checkboxEnabledAndSelected(updateMovesLegacyCBFixture,
                                (settings) -> settings.isUpdateMovesLegacy());

                // Toggle Gen 5 off leaving Gen 6
                updateMovesLegacyCBFixture.requireVisible().requireEnabled().click();
                // Moves should be True and Legacy should be False
                checkboxEnabledAndSelected(updateMovesCBFixture,
                                (settings) -> settings.isUpdateMoves());
                checkboxEnabledButNotSelected(updateMovesLegacyCBFixture,
                                (settings) -> settings.isUpdateMovesLegacy());
                // Should be able to save and reload from state
                setttingsString = this.mainWindow.getCurrentSettings().toString();
                Settings.fromString(setttingsString);
                // Should restore from settings string
                checkboxEnabledAndSelected(updateMovesCBFixture,
                                (settings) -> settings.isUpdateMoves());
                checkboxEnabledButNotSelected(updateMovesLegacyCBFixture,
                                (settings) -> settings.isUpdateMovesLegacy());

                // Toggle Gen 6 off leaving nothing
                updateMovesCBFixture.requireVisible().requireEnabled().click();
                // Legacy and Moves should be False
                checkboxEnabledButNotSelected(updateMovesCBFixture,
                                (settings) -> settings.isUpdateMoves());
                checkboxEnabledButNotSelected(updateMovesLegacyCBFixture,
                                (settings) -> settings.isUpdateMovesLegacy());
                // Should be able to save and reload from state
                setttingsString = this.mainWindow.getCurrentSettings().toString();
                Settings.fromString(setttingsString);
                // Should restore from settings string
                checkboxEnabledButNotSelected(updateMovesCBFixture,
                                (settings) -> settings.isUpdateMoves());
                checkboxEnabledButNotSelected(updateMovesLegacyCBFixture,
                                (settings) -> settings.isUpdateMovesLegacy());
        }

        /**
         * Selecting USE_RESISTANT_TYPE evaluates to true
         * 
         * Not selecting evaluates to false
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestUseResistantType() throws IOException {
                JCheckBoxFixture resistantTypeCBFixture = getCheckBoxByName("Use Resistant Type");
                Settings settings = this.mainWindow.getCurrentSettings();
                // Sanity check - should evaluate to false
                assertTrue("Misc Tweaks should not be set yet",
                                settings.getCurrentMiscTweaks() == 0);
                checkboxEnabledButNotSelected(resistantTypeCBFixture,
                                (setts) -> (setts.getCurrentMiscTweaks()
                                                & MiscTweak.USE_RESISTANT_TYPE.getValue()) > 0);
                // Sanity check - Should not fail with 0 options
                String setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                assertTrue("Misc Tweaks were selected after reloading settings 0",
                                settings.getCurrentMiscTweaks() == 0);
                checkboxEnabledButNotSelected(resistantTypeCBFixture,
                                (setts) -> (setts.getCurrentMiscTweaks()
                                                & MiscTweak.USE_RESISTANT_TYPE.getValue()) > 0);

                // Turn USE_RESISTANT_TYPE to true
                resistantTypeCBFixture.requireVisible().requireEnabled().click();
                checkboxEnabledAndSelected(resistantTypeCBFixture,
                                (setts) -> (setts.getCurrentMiscTweaks()
                                                & MiscTweak.USE_RESISTANT_TYPE.getValue()) > 0);
                setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                checkboxEnabledAndSelected(resistantTypeCBFixture,
                                (setts) -> (setts.getCurrentMiscTweaks()
                                                & MiscTweak.USE_RESISTANT_TYPE.getValue()) > 0);

                // Turn USE_RESISTANT_TYPE to false
                resistantTypeCBFixture.requireVisible().requireEnabled().click();
                checkboxEnabledButNotSelected(resistantTypeCBFixture,
                                (setts) -> (setts.getCurrentMiscTweaks()
                                                & MiscTweak.USE_RESISTANT_TYPE.getValue()) > 0);
                setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                checkboxEnabledButNotSelected(resistantTypeCBFixture,
                                (setts) -> (setts.getCurrentMiscTweaks()
                                                & MiscTweak.USE_RESISTANT_TYPE.getValue()) > 0);
        }

        /**
         * Selecting "RANDOM" enables the change methods checkbox
         * 
         * Toggles the change methods checkbox
         * 
         * Selecting "UNCHANGED" disables the change methods checkbox and resets state to false
         * 
         * Verifies settings can be stored and loaded with no error and preserve state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestChangeMethods() throws IOException {
                JRadioButtonFixture unchangedEvoRBFixture = getRadioButtonByName("peUnchangedRB");
                JRadioButtonFixture randomEvoRBFixture = getRadioButtonByName("peRandomRB");
                JCheckBoxFixture changeMethodsCBFixture = getCheckBoxByName("peChangeMethodsCB");
                TestCheckboxBasedOnRadioButton(unchangedEvoRBFixture, randomEvoRBFixture,
                                changeMethodsCBFixture, (settings) -> settings.isEvosChangeMethod(),
                                (evolutionMod) -> evolutionMod == Settings.EvolutionsMod.UNCHANGED,
                                (evolutionMod) -> evolutionMod == Settings.EvolutionsMod.RANDOM,
                                (settings) -> settings.getEvolutionsMod(), "Evolutions");
        }

        /**
         * Selecting "RANDOM" enables the minimum evos slider
         * 
         * Selecting 0, 1, or 2 is reflected in the settings
         * 
         * Selecting "UNCHANGED" disables the minimum evos slider and resets the value to 0
         * 
         * Saving settings to string and restoring produces no errors and retains state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestStartersMinimumEvos() throws IOException {
                JRadioButtonFixture unchangedStarterRBFixture =
                                getRadioButtonByName("spUnchangedRB");
                JRadioButtonFixture randomStarterRBFixture = getRadioButtonByName("spRandomRB");
                JSliderFixture startersMinimumEvosFixture = getSliderByName("spRandomSlider");
                Settings settings = this.mainWindow.getCurrentSettings();
                // Sanity check - should evaluate to defaults
                assertTrue("Starters minimum evos should not be set yet",
                                settings.getStartersMinimumEvos() == 0);
                assertFalse("Starters minimum evos should not be enabled yet",
                                startersMinimumEvosFixture.isEnabled());
                assertTrue("Starters should be set to UNCHANGED but was not",
                                settings.getStartersMod() == Settings.StartersMod.UNCHANGED);
                // Sanity check - Should not fail with 0 options
                String setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                assertTrue("Starters minimum evos was not zero after reloading settings 0",
                                settings.getStartersMinimumEvos() == 0);
                assertTrue("Starters was not UNCHANGED after reloading settings 0",
                                settings.getStartersMod() == Settings.StartersMod.UNCHANGED);

                // Turn random starters on
                clickRBAndWait(randomStarterRBFixture);
                settings = this.mainWindow.getCurrentSettings();
                assertTrue("Starters minimum evos should not be set yet",
                                settings.getStartersMinimumEvos() == 0);
                assertTrue("Starters minimum evos should be enabled",
                                startersMinimumEvosFixture.isEnabled());
                assertTrue("Starters should be set to RANDOM but was not",
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);
                setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                assertTrue("Starters minimum evos was not zero after reloading settings 1",
                                settings.getStartersMinimumEvos() == 0);
                assertTrue("Starters was not RANDOM after reloading settings 1",
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);

                // Set Slider to 1
                startersMinimumEvosFixture.requireVisible().requireEnabled().slideTo(1);
                settings = this.mainWindow.getCurrentSettings();
                assertTrue("Starters minimum evos should be set to 1",
                                settings.getStartersMinimumEvos() == 1);
                assertTrue("Starters minimum evos should be enabled",
                                startersMinimumEvosFixture.isEnabled());
                assertTrue("Starters should be set to RANDOM as state did not change",
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);
                setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                assertTrue("Starters minimum evos was not one after reloading settings 2",
                                settings.getStartersMinimumEvos() == 1);
                assertTrue("Starters was not RANDOM after reloading settings 2",
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);

                // Set Slider to 2
                startersMinimumEvosFixture.requireVisible().requireEnabled().slideTo(2);
                settings = this.mainWindow.getCurrentSettings();
                assertTrue("Starters minimum evos should be set to 2",
                                settings.getStartersMinimumEvos() == 2);
                assertTrue("Starters minimum evos should be enabled",
                                startersMinimumEvosFixture.isEnabled());
                assertTrue("Starters should be set to RANDOM as state did not change",
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);
                setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                assertTrue("Starters minimum evos was not two after reloading settings 3",
                                settings.getStartersMinimumEvos() == 2);
                assertTrue("Starters was not RANDOM after reloading settings 3",
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);

                // Set Slider to 0
                startersMinimumEvosFixture.requireVisible().requireEnabled().slideTo(0);
                settings = this.mainWindow.getCurrentSettings();
                assertTrue("Starters minimum evos should be set to 0",
                                settings.getStartersMinimumEvos() == 0);
                assertTrue("Starters minimum evos should be enabled",
                                startersMinimumEvosFixture.isEnabled());
                assertTrue("Starters should be set to RANDOM as state did not change",
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);
                setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                assertTrue("Starters minimum evos was not zero after reloading settings 4",
                                settings.getStartersMinimumEvos() == 0);
                assertTrue("Starters was not RANDOM after reloading settings 4",
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);

                // Turn random starters off while slider is set to a non-zero number should
                // result in value being 0
                startersMinimumEvosFixture.requireVisible().requireEnabled().slideTo(1);
                settings = this.mainWindow.getCurrentSettings();
                assertTrue("Starters minimum evos should be set to 1",
                                settings.getStartersMinimumEvos() == 1);
                assertTrue("Starters minimum evos should be enabled",
                                startersMinimumEvosFixture.isEnabled());
                assertTrue("Starters should be set to RANDOM as state did not change",
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);
                clickRBAndWait(unchangedStarterRBFixture);
                settings = this.mainWindow.getCurrentSettings();
                assertTrue("Starters minimum evos should be set to 0",
                                settings.getStartersMinimumEvos() == 0);
                assertFalse("Starters minimum evos should not be enabled",
                                startersMinimumEvosFixture.isEnabled());
                assertTrue("Starters should be set to UNCHANGED but was not",
                                settings.getStartersMod() == Settings.StartersMod.UNCHANGED);
                setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                assertTrue("Starters minimum evos was not zero after reloading settings 5",
                                settings.getStartersMinimumEvos() == 0);
                assertTrue("Starters was not UNCHANGED after reloading settings 5",
                                settings.getStartersMod() == Settings.StartersMod.UNCHANGED);
        }

        /**
         * Selecting "RANDOM" enables the exact evos checkbox
         * 
         * Toggles the exact evo checkbox
         * 
         * Selecting "UNCHANGED" disables the exact evos checkbox and resets state to false
         * 
         * Verifies settings can be stored and loaded with no error and preserve state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestExactEvo() throws IOException {
                JRadioButtonFixture unchangedStarterRBFixture =
                                getRadioButtonByName("spUnchangedRB");
                JRadioButtonFixture randomStarterRBFixture = getRadioButtonByName("spRandomRB");
                JCheckBoxFixture exactEvoCBFixture = getCheckBoxByName("spExactEvoCB");
                TestCheckboxBasedOnRadioButton(unchangedStarterRBFixture, randomStarterRBFixture,
                                exactEvoCBFixture, (settings) -> settings.isStartersExactEvo(),
                                (startersMod) -> startersMod == Settings.StartersMod.UNCHANGED,
                                (startersMod) -> startersMod == Settings.StartersMod.RANDOM,
                                (settings) -> settings.getStartersMod(), "Starters");
        }

        /**
         * Selecting "RANDOM" enables the No Split Evos checkbox
         * 
         * Toggles the No Split Evo checkbox
         * 
         * Selecting "UNCHANGED" disables the No Split Evocheckbox and resets state to false
         * 
         * Verifies settings can be stored and loaded with no error and preserve state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestNoSplitEvos() throws IOException {
                JRadioButtonFixture unchangedStarterRBFixture =
                                getRadioButtonByName("spUnchangedRB");
                JRadioButtonFixture randomStarterRBFixture = getRadioButtonByName("spRandomRB");
                JCheckBoxFixture noSplitCBFixture = getCheckBoxByName("spNoSplitCB");
                TestCheckboxBasedOnRadioButton(unchangedStarterRBFixture, randomStarterRBFixture,
                                noSplitCBFixture, (settings) -> settings.isStartersNoSplit(),
                                (starterMod) -> starterMod == Settings.StartersMod.UNCHANGED,
                                (starterMod) -> starterMod == Settings.StartersMod.RANDOM,
                                (settings) -> settings.getStartersMod(), "Starters");
        }

        /**
         * Toggle RandomizeHeldItems available on any trainers mod setting
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestTrainerRandomHeldItem() throws IOException {
                JCheckBoxFixture randomHeldItemCBFixture = getCheckBoxByName("tpRandomHeldItemCB");
                // Sanity check - Should initialize to False
                checkboxEnabledButNotSelected(randomHeldItemCBFixture,
                                (settings) -> settings.isTrainersRandomHeldItem());
                // Sanity check - Should be able to save and reload from state
                String setttingsString = this.mainWindow.getCurrentSettings().toString();
                Settings.fromString(setttingsString);
                // Sanity check - Should restore back to initial form state
                checkboxEnabledButNotSelected(randomHeldItemCBFixture,
                                (settings) -> settings.isTrainersRandomHeldItem());

                // Toggle on
                randomHeldItemCBFixture.requireVisible().requireEnabled().click();
                // Should be True now
                checkboxEnabledAndSelected(randomHeldItemCBFixture,
                                (settings) -> settings.isTrainersRandomHeldItem());
                // Should be able to save and reload from state
                setttingsString = this.mainWindow.getCurrentSettings().toString();
                Settings.fromString(setttingsString);
                // Should restore from settings string
                checkboxEnabledAndSelected(randomHeldItemCBFixture,
                                (settings) -> settings.isTrainersRandomHeldItem());

                // Toggle off
                randomHeldItemCBFixture.requireVisible().requireEnabled().click();
                // Should be False now
                checkboxEnabledButNotSelected(randomHeldItemCBFixture,
                                (settings) -> settings.isTrainersRandomHeldItem());
                // Should be able to save and reload from state
                setttingsString = this.mainWindow.getCurrentSettings().toString();
                Settings.fromString(setttingsString);
                // Should restore from settings string
                checkboxEnabledButNotSelected(randomHeldItemCBFixture,
                                (settings) -> settings.isTrainersRandomHeldItem());
        }

        /**
         * Selecting "RANDOM" or "GLOBAL SWAP" enables the Gym Type Theme checkbox
         * 
         * Toggles the Gym Type Theme checkbox
         * 
         * Selecting "UNCHANGED" or "TYPE THEME" disables the Gym Type Theme checkbox and resets
         * state to false
         * 
         * Verifies settings can be stored and loaded with no error and preserve state
         * 
         * @throws IOException
         */
        @Test(timeout = 8000)
        public void TestGymTypeTheme() throws IOException {
                JRadioButtonFixture unchangedTrainerRBFixture =
                                getRadioButtonByName("tpUnchangedRB");
                JRadioButtonFixture randomTrainerRBFixture = getRadioButtonByName("tpRandomRB");
                JRadioButtonFixture typeThemeTrainerRBFixture =
                                getRadioButtonByName("tpTypeThemeRB");
                JRadioButtonFixture globalSwapRBFixture = getRadioButtonByName("tpGlobalSwapRB");
                JCheckBoxFixture gymTypeThemeCBFixture = getCheckBoxByName("tpGymTypeThemeCB");

                // Check basic case of UNCHANGED and RANDOM
                TestCheckboxBasedOnRadioButton(unchangedTrainerRBFixture, randomTrainerRBFixture,
                                gymTypeThemeCBFixture, (settings) -> settings.isGymTypeTheme(),
                                (trainersMod) -> trainersMod == Settings.TrainersMod.UNCHANGED,
                                (trainersMod) -> trainersMod == Settings.TrainersMod.RANDOM,
                                (settings) -> settings.getTrainersMod(), "Trainers");

                // Check UNCHANGED and GLOBAL_MAPPING
                TestCheckboxBasedOnRadioButton(unchangedTrainerRBFixture, globalSwapRBFixture,
                                gymTypeThemeCBFixture, (settings) -> settings.isGymTypeTheme(),
                                (trainersMod) -> trainersMod == Settings.TrainersMod.UNCHANGED,
                                (trainersMod) -> trainersMod == Settings.TrainersMod.GLOBAL_MAPPING,
                                (settings) -> settings.getTrainersMod(), "Trainers");

                // Selecting Type Theme trainer should not enable Gym Type Theme
                clickRBAndWait(typeThemeTrainerRBFixture);
                Settings settings = this.mainWindow.getCurrentSettings();
                checkboxNotEnabledAndNotSelected(gymTypeThemeCBFixture,
                                (setts) -> setts.isGymTypeTheme());
                assertTrue("Trainers should be set to TYPE THEMED but was not",
                                settings.getTrainersMod() == Settings.TrainersMod.TYPE_THEMED);
                String setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                checkboxNotEnabledAndNotSelected(gymTypeThemeCBFixture,
                                (setts) -> setts.isGymTypeTheme());
                assertTrue("Trainers was not TYPE THEME after reloading settings TYPE THEME",
                                settings.getTrainersMod() == Settings.TrainersMod.TYPE_THEMED);
        }

        /**
         * Clicking the random settings button results in a successful operation.
         * 
         * Verifies settings can be stored and loaded with no error and preserve state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestRandomSettingsButton() throws IOException {
                JButtonFixture randomQSButtonFixture = getButtonByName("randomQSButton");
                randomQSButtonFixture.requireVisible().requireEnabled().click();
                Settings settings = this.mainWindow.getCurrentSettings();
                String settingsString = settings.toString();
                settings = Settings.fromString(settingsString);
                assertTrue("Settings do not match after reload - \nBefore: " + settingsString
                                + "\nAfter: " + settings.toString(),
                                settingsString.equals(settings.toString()));
        }

        /**
         * Minimum Catch Rate Checkbox is enabled regardless of radio button selection
         * 
         * Verifies settings can be stored and loaded with no error and preserve state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestMinimumCatchRateCB() throws IOException {
                JRadioButtonFixture unchangedWildPokemonRBFixture =
                                getRadioButtonByName("wpUnchangedRB");
                JRadioButtonFixture randomWildPokemonRBFixture = getRadioButtonByName("wpRandomRB");
                JCheckBoxFixture minimumCatchRateCBFixture = getCheckBoxByName("wpCatchRateCB");
                int settingsReloadCount = 0;
                // Sanity check - should evaluate to false
                Settings settings = this.mainWindow.getCurrentSettings();
                checkboxEnabledButNotSelected(minimumCatchRateCBFixture,
                                (setts) -> setts.isUseMinimumCatchRate());
                assertTrue("Wild Pokemon should be set to " + unchangedWildPokemonRBFixture.text()
                                + " but was not",
                                settings.getWildPokemonMod() == Settings.WildPokemonMod.UNCHANGED);
                // Sanity check - Should not fail with 0 options
                String setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                checkboxEnabledButNotSelected(minimumCatchRateCBFixture,
                                (setts) -> setts.isUseMinimumCatchRate());
                assertTrue("Wild Pokemon was not " + unchangedWildPokemonRBFixture.text()
                                + " after reloading settings " + settingsReloadCount,
                                settings.getWildPokemonMod() == Settings.WildPokemonMod.UNCHANGED);
                settingsReloadCount++;

                // Turn Minimum Catch Rate on
                minimumCatchRateCBFixture.requireVisible().requireEnabled().click();
                settings = this.mainWindow.getCurrentSettings();
                checkboxEnabledAndSelected(minimumCatchRateCBFixture,
                                (setts) -> setts.isUseMinimumCatchRate());
                assertTrue("Wild Pokemon should be set to " + unchangedWildPokemonRBFixture.text()
                                + " but was not",
                                settings.getWildPokemonMod() == Settings.WildPokemonMod.UNCHANGED);
                setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                checkboxEnabledAndSelected(minimumCatchRateCBFixture,
                                (setts) -> setts.isUseMinimumCatchRate());
                assertTrue("Wild Pokemon was not " + unchangedWildPokemonRBFixture.text()
                                + " after reloading settings " + settingsReloadCount,
                                settings.getWildPokemonMod() == Settings.WildPokemonMod.UNCHANGED);
                settingsReloadCount++;

                // Turn Random RB on
                randomWildPokemonRBFixture.requireVisible().requireEnabled().click();
                settings = this.mainWindow.getCurrentSettings();
                checkboxEnabledAndSelected(minimumCatchRateCBFixture,
                                (setts) -> setts.isUseMinimumCatchRate());
                assertTrue("Wild Pokemon should be set to " + randomWildPokemonRBFixture.text()
                                + " but was not",
                                settings.getWildPokemonMod() == Settings.WildPokemonMod.RANDOM);
                setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                checkboxEnabledAndSelected(minimumCatchRateCBFixture,
                                (setts) -> setts.isUseMinimumCatchRate());
                assertTrue("Wild Pokemon was not " + randomWildPokemonRBFixture.text()
                                + " after reloading settings " + settingsReloadCount,
                                settings.getWildPokemonMod() == Settings.WildPokemonMod.RANDOM);
                settingsReloadCount++;

                // Turn Unchanged RB on
                unchangedWildPokemonRBFixture.requireVisible().requireEnabled().click();
                settings = this.mainWindow.getCurrentSettings();
                checkboxEnabledAndSelected(minimumCatchRateCBFixture,
                                (setts) -> setts.isUseMinimumCatchRate());
                assertTrue("Wild Pokemon should be set to " + unchangedWildPokemonRBFixture.text()
                                + " but was not",
                                settings.getWildPokemonMod() == Settings.WildPokemonMod.UNCHANGED);
                setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                checkboxEnabledAndSelected(minimumCatchRateCBFixture,
                                (setts) -> setts.isUseMinimumCatchRate());
                assertTrue("Wild Pokemon was not " + unchangedWildPokemonRBFixture.text()
                                + " after reloading settings " + settingsReloadCount,
                                settings.getWildPokemonMod() == Settings.WildPokemonMod.UNCHANGED);
                settingsReloadCount++;

                // Turn Minimum Catch Rate off
                minimumCatchRateCBFixture.requireVisible().requireEnabled().click();
                settings = this.mainWindow.getCurrentSettings();
                checkboxEnabledButNotSelected(minimumCatchRateCBFixture,
                                (setts) -> setts.isUseMinimumCatchRate());
                assertTrue("Wild Pokemon should be set to " + unchangedWildPokemonRBFixture.text()
                                + " but was not",
                                settings.getWildPokemonMod() == Settings.WildPokemonMod.UNCHANGED);
                // Sanity check - Should not fail with 0 options
                setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                checkboxEnabledButNotSelected(minimumCatchRateCBFixture,
                                (setts) -> setts.isUseMinimumCatchRate());
                assertTrue("Wild Pokemon was not " + unchangedWildPokemonRBFixture.text()
                                + " after reloading settings " + settingsReloadCount,
                                settings.getWildPokemonMod() == Settings.WildPokemonMod.UNCHANGED);
                settingsReloadCount++;
        }

        /**
         * Selecting "RANDOM" enables the SE Triangle checkbox
         * 
         * Toggles the SE Triangle checkbox
         * 
         * Selecting "UNCHANGED" disables the SE Triangle checkbox and resets state to false
         * 
         * Verifies settings can be stored and loaded with no error and preserve state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestStartersSETriangle() throws IOException {
                JRadioButtonFixture unchangedStarterRBFixture =
                                getRadioButtonByName("spUnchangedRB");
                JRadioButtonFixture randomStarterRBFixture = getRadioButtonByName("spRandomRB");
                JCheckBoxFixture seTriangleCBFixture = getCheckBoxByName("spSETriangleCB");
                TestCheckboxBasedOnRadioButton(unchangedStarterRBFixture, randomStarterRBFixture,
                                seTriangleCBFixture, (settings) -> settings.isStartersSETriangle(),
                                (starterMod) -> starterMod == Settings.StartersMod.UNCHANGED,
                                (starterMod) -> starterMod == Settings.StartersMod.RANDOM,
                                (settings) -> settings.getStartersMod(), "Starters");
        }

        /**
         * Selecting "GLOBAL SWAP" toggles the radio button group
         * 
         * Selecting "UNCHANGED" toggles the radio button group away from GLOBAL_MAPPING
         * 
         * Verifies settings can be stored and loaded with no error and preserve state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestGlobalSwap() throws IOException {
                JRadioButtonFixture unchangedTrainerRBFixture =
                                getRadioButtonByName("tpUnchangedRB");
                JRadioButtonFixture globalSwapRBFixture = getRadioButtonByName("tpGlobalSwapRB");
                TestRadioButton(unchangedTrainerRBFixture, globalSwapRBFixture,
                                (trainersMod) -> trainersMod == Settings.TrainersMod.UNCHANGED,
                                (trainersMod) -> trainersMod == Settings.TrainersMod.GLOBAL_MAPPING,
                                (settings) -> settings.getTrainersMod(), "Trainers");
        }

        @Test(timeout = 10000)
        public void TestStarterTypeFilter() throws IOException {
                int settingsReloadCount = 0;
                JRadioButtonFixture unchangedStarterRBFixture =
                                getRadioButtonByName("spUnchangedRB");
                JRadioButtonFixture randomStarterRBFixture = getRadioButtonByName("spRandomRB");
                JCheckBoxFixture seTriangleCBFixture = getCheckBoxByName("spSETriangleCB");
                JButtonFixture filterTypesButtonFixture = getButtonByName("spTypeFilterButton");

                // Sanity check - Should not be enabled
                Settings settings = this.mainWindow.getCurrentSettings();
                assertTrue("Starters should be set to UNCHANGED but was not",
                                settings.getStartersMod() == Settings.StartersMod.UNCHANGED);
                assertFalse("Starter Type Filter should not be enabled yet",
                                filterTypesButtonFixture.isEnabled());
                assertTrue("Starter Types should initialze as null",
                                settings.getStarterTypes() == null);
                // Sanity check - Should not fail with 0 options
                String settingsString = settings.toString();
                settings = Settings.fromString(settingsString);
                assertTrue("Starter Types was not null after reloading settings "
                                + settingsReloadCount, settings.getStarterTypes() == null);
                assertTrue("Starters was not UNCHANGED after reloading settings "
                                + settingsReloadCount,
                                settings.getStartersMod() == Settings.StartersMod.UNCHANGED);
                settingsReloadCount++;

                // Turn randomStarterRB on
                clickRBAndWait(randomStarterRBFixture);
                settings = this.mainWindow.getCurrentSettings();
                assertTrue("Starters should be set to RANDOM but was not",
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);
                assertTrue("Starter Type Filter should be enabled now",
                                filterTypesButtonFixture.isEnabled());
                assertTrue("Starter Types should still be null",
                                settings.getStarterTypes() == null);
                settingsString = settings.toString();
                settings = Settings.fromString(settingsString);
                assertTrue("Starter Types was not null after reloading settings "
                                + settingsReloadCount, settings.getStarterTypes() == null);
                assertTrue("Starters was not RANDOM after reloading settings "
                                + settingsReloadCount,
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);
                settingsReloadCount++;

                // Select the first and last item from Type Filter
                filterTypesButtonFixture.requireVisible().requireEnabled().click();
                DialogFixture typeFilterDialog = getDialogByClass(TypeFilterDialog.class);
                JListFixture typeCheckboxList = getListByName("typeCheckboxList");
                JButtonFixture filterOkButton = getButtonByName("okButton");
                typeCheckboxList.selectItem(0);
                typeCheckboxList.selectItem(typeCheckboxList.contents().length - 1);
                filterOkButton.requireVisible().requireEnabled().click();
                settings = this.mainWindow.getCurrentSettings();
                assertTrue("Type filter dialog was not closed",
                                typeFilterDialog.requireNotVisible() != null);
                assertTrue("Starter types did not include NORMAL and DARK",
                                settings.getStarterTypes().containsAll(
                                                Arrays.asList(Type.NORMAL, Type.DARK)));
                settingsString = settings.toString();
                settings = Settings.fromString(settingsString);
                assertTrue("Starter Types did not include NORMAL and DARK after reloading settings "
                                + settingsReloadCount,
                                settings.getStarterTypes().containsAll(
                                                Arrays.asList(Type.NORMAL, Type.DARK)));
                assertTrue("Starters was not RANDOM after reloading settings "
                                + settingsReloadCount,
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);
                settingsReloadCount++;

                // Reopen and assert choices do not change when clicking cancel
                filterTypesButtonFixture.requireVisible().requireEnabled().click();
                typeFilterDialog = getDialogByClass(TypeFilterDialog.class);
                typeCheckboxList = getListByName("typeCheckboxList");
                JButtonFixture filterCancelButton = getButtonByName("cancelButton");
                // Only select the first item in the list to make the state different than prior
                typeCheckboxList.selectItem(0);
                filterCancelButton.requireVisible().requireEnabled().click();
                settings = this.mainWindow.getCurrentSettings();
                assertTrue("Type filter dialog was not closed",
                                typeFilterDialog.requireNotVisible() != null);
                // Cancel should restore to prior state and not new state
                assertTrue("Starter types did not include NORMAL and DARK",
                                settings.getStarterTypes().containsAll(
                                                Arrays.asList(Type.NORMAL, Type.DARK)));
                settingsString = settings.toString();
                settings = Settings.fromString(settingsString);
                assertTrue("Starter Types did not include NORMAL and DARK after reloading settings "
                                + settingsReloadCount,
                                settings.getStarterTypes().containsAll(
                                                Arrays.asList(Type.NORMAL, Type.DARK)));
                assertTrue("Starters was not RANDOM after reloading settings "
                                + settingsReloadCount,
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);
                settingsReloadCount++;

                // Turn SETriangle on - should diable Type Filter and null starterTypes
                clickCBAndWait(seTriangleCBFixture);
                settings = this.mainWindow.getCurrentSettings();
                assertTrue("Starters should be set to RANDOM but was not",
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);
                assertFalse("Starter Type Filter should be disabled now",
                                filterTypesButtonFixture.isEnabled());
                assertTrue("Starter Types should be null again",
                                settings.getStarterTypes() == null);
                settingsString = settings.toString();
                settings = Settings.fromString(settingsString);
                assertTrue("Starter Types was not null after reloading settings "
                                + settingsReloadCount, settings.getStarterTypes() == null);
                assertTrue("Starters was not RANDOM after reloading settings "
                                + settingsReloadCount,
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);
                settingsReloadCount++;
                // Deselect SETriangle
                clickCBAndWait(seTriangleCBFixture);

                // Assert select all chooses everything
                filterTypesButtonFixture.requireVisible().requireEnabled().click();
                typeFilterDialog = getDialogByClass(TypeFilterDialog.class);
                typeCheckboxList = getListByName("typeCheckboxList");
                filterOkButton = getButtonByName("okButton");
                JButtonFixture filterSelectAllButton = getButtonByName("selectAllButton");
                int typeSize = typeCheckboxList.contents().length;
                filterSelectAllButton.requireVisible().requireEnabled().click();
                filterOkButton.requireVisible().requireEnabled().click();
                settings = this.mainWindow.getCurrentSettings();
                assertTrue("Type filter dialog was not closed",
                                typeFilterDialog.requireNotVisible() != null);
                List<Type> starterTypes1 = settings.getStarterTypes();
                Type.getTypes(typeSize)
                                .forEach(type -> assertTrue(
                                                "Missing " + type + " from starter types",
                                                starterTypes1.contains(type)));
                settingsString = settings.toString();
                settings = Settings.fromString(settingsString);
                List<Type> starterTypes2 = settings.getStarterTypes();
                Type.getTypes(typeSize)
                                .forEach(type -> assertTrue("Missing " + type
                                                + " from starter types after reload",
                                                starterTypes2.contains(type)));
                assertTrue("Starters was not RANDOM after reloading settings "
                                + settingsReloadCount,
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);
                settingsReloadCount++;

                // Assert deselect all sets starterTypes to null
                filterTypesButtonFixture.requireVisible().requireEnabled().click();
                typeFilterDialog = getDialogByClass(TypeFilterDialog.class);
                typeCheckboxList = getListByName("typeCheckboxList");
                filterOkButton = getButtonByName("okButton");
                JButtonFixture filterDeselectAllButton = getButtonByName("deselectAllButton");
                typeSize = typeCheckboxList.contents().length;
                filterDeselectAllButton.requireVisible().requireEnabled().click();
                filterOkButton.requireVisible().requireEnabled().click();
                settings = this.mainWindow.getCurrentSettings();
                assertTrue("Type filter dialog was not closed",
                                typeFilterDialog.requireNotVisible() != null);
                assertTrue("Starter types shoudl be null again",
                                settings.getStarterTypes() == null);
                settingsString = settings.toString();
                settings = Settings.fromString(settingsString);
                assertTrue("Starter Types was not null after reloading settings "
                                + settingsReloadCount, settings.getStarterTypes() == null);
                assertTrue("Starters was not RANDOM after reloading settings "
                                + settingsReloadCount,
                                settings.getStartersMod() == Settings.StartersMod.RANDOM);
                settingsReloadCount++;

                // Turn unchangedStarterRB on
                // Populate starter types first
                filterTypesButtonFixture.requireVisible().requireEnabled().click();
                typeFilterDialog = getDialogByClass(TypeFilterDialog.class);
                typeCheckboxList = getListByName("typeCheckboxList");
                filterOkButton = getButtonByName("okButton");
                typeCheckboxList.selectItem(0);
                typeCheckboxList.selectItem(typeCheckboxList.contents().length - 1);
                filterOkButton.requireVisible().requireEnabled().click();
                settings = this.mainWindow.getCurrentSettings();
                assertTrue("Type filter dialog was not closed",
                                typeFilterDialog.requireNotVisible() != null);
                assertTrue("Starter types did not include NORMAL and DARK",
                                settings.getStarterTypes().containsAll(
                                                Arrays.asList(Type.NORMAL, Type.DARK)));
                // Now turn unchangedStarterRB on
                clickRBAndWait(unchangedStarterRBFixture);
                settings = this.mainWindow.getCurrentSettings();
                assertTrue("Starters should be set to UNCHANGED but was not",
                                settings.getStartersMod() == Settings.StartersMod.UNCHANGED);
                assertFalse("Starter Type Filter should be disabled now",
                                filterTypesButtonFixture.isEnabled());
                assertTrue("Starter Types should be null again",
                                settings.getStarterTypes() == null);
                settingsString = settings.toString();
                settings = Settings.fromString(settingsString);
                assertTrue("Starter Types was not null after reloading settings "
                                + settingsReloadCount, settings.getStarterTypes() == null);
                assertTrue("Starters was not UNCHANGED after reloading settings "
                                + settingsReloadCount,
                                settings.getStartersMod() == Settings.StartersMod.UNCHANGED);
                settingsReloadCount++;
        }

        /**
         * Selecting anything except "UNCHANGED" enables the Buff Elite checkbox
         * 
         * Toggles the Buff Elite checkbox
         * 
         * Selecting "UNCHANGED" disables the Buff Elite checkbox and resets state to false
         * 
         * Verifies settings can be stored and loaded with no error and preserve state
         * 
         * @throws IOException
         */
        @Test(timeout = 8000)
        public void TestTrainersBuffElite() throws IOException {
                JRadioButtonFixture unchangedTrainerRBFixture =
                                getRadioButtonByName("tpUnchangedRB");
                JRadioButtonFixture randomTrainerRBFixture = getRadioButtonByName("tpRandomRB");
                JRadioButtonFixture typeThemeTrainerRBFixture =
                                getRadioButtonByName("tpTypeThemeRB");
                JRadioButtonFixture globalSwapTrainerRBFixture =
                                getRadioButtonByName("tpGlobalSwapRB");
                JCheckBoxFixture buffEliteCBFixture = getCheckBoxByName("tpBuffEliteCB");

                // Check basic case of UNCHANGED and RANDOM
                TestCheckboxBasedOnRadioButton(unchangedTrainerRBFixture, randomTrainerRBFixture,
                                buffEliteCBFixture, (settings) -> settings.isTrainersBuffElite(),
                                (trainersMod) -> trainersMod == Settings.TrainersMod.UNCHANGED,
                                (trainersMod) -> trainersMod == Settings.TrainersMod.RANDOM,
                                (settings) -> settings.getTrainersMod(), "Trainers");

                // Check case of TYPE THEME
                TestCheckboxBasedOnRadioButton(unchangedTrainerRBFixture, typeThemeTrainerRBFixture,
                                buffEliteCBFixture, (settings) -> settings.isTrainersBuffElite(),
                                (trainersMod) -> trainersMod == Settings.TrainersMod.UNCHANGED,
                                (trainersMod) -> trainersMod == Settings.TrainersMod.TYPE_THEMED,
                                (settings) -> settings.getTrainersMod(), "Trainers");

                // Check case of GLOBAL SWAP
                TestCheckboxBasedOnRadioButton(unchangedTrainerRBFixture,
                                globalSwapTrainerRBFixture, buffEliteCBFixture,
                                (settings) -> settings.isTrainersBuffElite(),
                                (trainersMod) -> trainersMod == Settings.TrainersMod.UNCHANGED,
                                (trainersMod) -> trainersMod == Settings.TrainersMod.GLOBAL_MAPPING,
                                (settings) -> settings.getTrainersMod(), "Trainers");
        }

        /**
         * Selecting "RANDOM_PREFER_SAME_TYPE" toggles the radio button group
         * 
         * Selecting "UNCHANGED" toggles the radio button group away from RANDOM_PREFER_SAME_TYPE
         * 
         * Verifies settings can be stored and loaded with no error and preserve state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestMovesetsRandomSameType() throws IOException {
                JRadioButtonFixture unchangedMovesetRBFixture =
                                getRadioButtonByName("pmsUnchangedRB");
                JRadioButtonFixture randomTypeRBFixture = getRadioButtonByName("pmsRandomTypeRB");
                TestRadioButton(unchangedMovesetRBFixture, randomTypeRBFixture,
                                (movesetsMod) -> movesetsMod == Settings.MovesetsMod.UNCHANGED,
                                (movesetsMod) -> movesetsMod == Settings.MovesetsMod.RANDOM_PREFER_SAME_TYPE,
                                (settings) -> settings.getMovesetsMod(), "Movesets");
        }

        /**
         * Selecting "COMPLETELY_RANDOM" toggles the radio button group
         * 
         * Selecting "UNCHANGED" toggles the radio button group away from COMPLETELY_RANDOM
         * 
         * Verifies settings can be stored and loaded with no error and preserve state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestMovesetsRandomCompletely() throws IOException {
                JRadioButtonFixture unchangedMovesetRBFixture =
                                getRadioButtonByName("pmsUnchangedRB");
                JRadioButtonFixture randomTypeRBFixture = getRadioButtonByName("pmsRandomTotalRB");
                TestRadioButton(unchangedMovesetRBFixture, randomTypeRBFixture,
                                (movesetsMod) -> movesetsMod == Settings.MovesetsMod.UNCHANGED,
                                (movesetsMod) -> movesetsMod == Settings.MovesetsMod.COMPLETELY_RANDOM,
                                (settings) -> settings.getMovesetsMod(), "Movesets");
        }

        /**
         * Selecting "METRONOME_ONLY" toggles the radio button group
         * 
         * Selecting "UNCHANGED" toggles the radio button group away from METRONOME_ONLY
         * 
         * Verifies settings can be stored and loaded with no error and preserve state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestMovesetsMetronome() throws IOException {
                JRadioButtonFixture unchangedMovesetRBFixture =
                                getRadioButtonByName("pmsUnchangedRB");
                JRadioButtonFixture randomTypeRBFixture =
                                getRadioButtonByName("pmsMetronomeOnlyRB");
                TestRadioButton(unchangedMovesetRBFixture, randomTypeRBFixture,
                                (movesetsMod) -> movesetsMod == Settings.MovesetsMod.UNCHANGED,
                                (movesetsMod) -> movesetsMod == Settings.MovesetsMod.METRONOME_ONLY,
                                (settings) -> settings.getMovesetsMod(), "Movesets");
        }

        /**
         * Selecting "RANDOMIZE" enables the "Force Lv1 Evo" checkbox
         * 
         * Toggles the checkbox
         * 
         * "UNCHANGED" disables the checkbox
         * 
         * Verifies settings can be stored and loaded with no error and preseve state
         * 
         * @throws IOException
         */
        @Test(timeout = 12000)
        public void TestEvolveLevel1() throws IOException {
                JRadioButtonFixture unchangedEvolveRBFixture =
                                getRadioButtonByName("peUnchangedRB");
                JRadioButtonFixture randomEvolveRBFixture = getRadioButtonByName("peRandomRB");
                JCheckBoxFixture peEvolveLv1CBFixture = getCheckBoxByName("peEvolveLv1CB");
                TestCheckboxBasedOnRadioButton(unchangedEvolveRBFixture, randomEvolveRBFixture,
                                peEvolveLv1CBFixture, (settings) -> settings.isEvosLv1(),
                                (evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.UNCHANGED,
                                (evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.RANDOM,
                                (settings) -> settings.getEvolutionsMod(), "Evolutions");

                // Test that Limit 3 Stages disables if Force Lv1 Evo is checked
                JCheckBoxFixture peThreeStagesCBFixture = getCheckBoxByName("peThreeStagesCB");
                TestCheckboxBasedOnCheckbox(unchangedEvolveRBFixture, randomEvolveRBFixture,
                                peEvolveLv1CBFixture, peThreeStagesCBFixture,
                                (settings) -> settings.isEvosLv1(),
                                (settings) -> settings.isEvosMaxThreeStages(),
                                (evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.UNCHANGED,
                                (evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.RANDOM,
                                (settings) -> settings.getEvolutionsMod(), "Evolutions");

                // Test that Limit 3 Stages disables if Force Lv1 Evo is checked
                JCheckBoxFixture peForceGrowthCBFixture = getCheckBoxByName("peForceGrowthCB");
                TestCheckboxBasedOnCheckbox(unchangedEvolveRBFixture, randomEvolveRBFixture,
                                peEvolveLv1CBFixture, peForceGrowthCBFixture,
                                (settings) -> settings.isEvosLv1(),
                                (settings) -> settings.isEvosForceGrowth(),
                                (evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.UNCHANGED,
                                (evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.RANDOM,
                                (settings) -> settings.getEvolutionsMod(), "Evolutions");
        }

        /**
         * Selecting "Force Lv1 Evo" disables "Exact Evo" starters option and random slider
         * 
         * Toggles checkbox and verifies correct enable/disable behavior
         * 
         * Checks that values are reset to default if disabled
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestEvolveLevel1DisablesExactEvoStarters() throws IOException {
                JRadioButtonFixture unchangedEvolveRBFixture =
                                getRadioButtonByName("peUnchangedRB");
                JRadioButtonFixture randomEvolveRBFixture = getRadioButtonByName("peRandomRB");
                JCheckBoxFixture peEvolveLv1CBFixture = getCheckBoxByName("peEvolveLv1CB");

                JRadioButtonFixture unchangedStartersRBFixture =
                                getRadioButtonByName("spUnchangedRB");
                JRadioButtonFixture randomStartersRBFixture = getRadioButtonByName("spRandomRB");
                JCheckBoxFixture exactEvoCB = getCheckBoxByName("spExactEvoCB");
                JSliderFixture spRandomSlider = getSliderByName("spRandomSlider");

                // Sanity Check
                AtomicInteger settingsReloadCount = new AtomicInteger(0);
                testConditions(() -> {
                        radioButtonSelected(unchangedEvolveRBFixture, (
                                        evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.UNCHANGED,
                                        (settings) -> settings.getEvolutionsMod(), "Evolutions");
                        radioButtonSelected(unchangedStartersRBFixture, (
                                        startersMod) -> startersMod == Settings.StartersMod.UNCHANGED,
                                        (settings) -> settings.getStartersMod(), "Starters");
                        checkboxNotEnabledAndNotSelected(peEvolveLv1CBFixture,
                                        (settings) -> settings.isEvosLv1());
                        checkboxNotEnabledAndNotSelected(exactEvoCB,
                                        (settings) -> settings.isStartersExactEvo());
                        sliderNotEnabledAndValueMatches(spRandomSlider,
                                        (settings) -> settings.getStartersMinimumEvos() == 0,
                                        "Random Starters Slider");
                }, settingsReloadCount);

                // Turn random starters on
                clickRBAndWait(randomStartersRBFixture);
                testConditions(() -> {
                        radioButtonSelected(unchangedEvolveRBFixture, (
                                        evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.UNCHANGED,
                                        (settings) -> settings.getEvolutionsMod(), "Evolutions");
                        radioButtonSelected(randomStartersRBFixture,
                                        (startersMod) -> startersMod == Settings.StartersMod.RANDOM,
                                        (settings) -> settings.getStartersMod(), "Starters");
                        checkboxNotEnabledAndNotSelected(peEvolveLv1CBFixture,
                                        (settings) -> settings.isEvosLv1());
                        checkboxEnabledButNotSelected(exactEvoCB,
                                        (settings) -> settings.isStartersExactEvo());
                        sliderEnabledAndValueMatches(spRandomSlider,
                                        (settings) -> settings.getStartersMinimumEvos() == 0,
                                        "Random Starters Slider");
                }, settingsReloadCount);

                // Turn random evolutions on
                clickRBAndWait(randomEvolveRBFixture);
                testConditions(() -> {
                        radioButtonSelected(randomEvolveRBFixture, (
                                        evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.RANDOM,
                                        (settings) -> settings.getEvolutionsMod(), "Evolutions");
                        radioButtonSelected(randomEvolveRBFixture,
                                        (startersMod) -> startersMod == Settings.StartersMod.RANDOM,
                                        (settings) -> settings.getStartersMod(), "Starters");
                        checkboxEnabledButNotSelected(peEvolveLv1CBFixture,
                                        (settings) -> settings.isEvosLv1());
                        checkboxEnabledButNotSelected(exactEvoCB,
                                        (settings) -> settings.isStartersExactEvo());
                        sliderEnabledAndValueMatches(spRandomSlider,
                                        (settings) -> settings.getStartersMinimumEvos() == 0,
                                        "Random Starters Slider");
                }, settingsReloadCount);

                // Turn "Exact Evo" on and move "Random Slider" to maximum
                exactEvoCB.requireEnabled().requireVisible().click();
                spRandomSlider.slideToMaximum();
                testConditions(() -> {
                        radioButtonSelected(randomEvolveRBFixture, (
                                        evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.RANDOM,
                                        (settings) -> settings.getEvolutionsMod(), "Evolutions");
                        radioButtonSelected(randomEvolveRBFixture,
                                        (startersMod) -> startersMod == Settings.StartersMod.RANDOM,
                                        (settings) -> settings.getStartersMod(), "Starters");
                        checkboxEnabledButNotSelected(peEvolveLv1CBFixture,
                                        (settings) -> settings.isEvosLv1());
                        checkboxEnabledAndSelected(exactEvoCB,
                                        (settings) -> settings.isStartersExactEvo());
                        sliderEnabledAndValueMatches(spRandomSlider,
                                        (settings) -> settings.getStartersMinimumEvos() == 2,
                                        "Random Starters Slider");
                }, settingsReloadCount);

                // Turn "Force Lv1 Evo" on
                clickCBAndWait(peEvolveLv1CBFixture);
                testConditions(() -> {
                        radioButtonSelected(randomEvolveRBFixture, (
                                        evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.RANDOM,
                                        (settings) -> settings.getEvolutionsMod(), "Evolutions");
                        radioButtonSelected(randomEvolveRBFixture,
                                        (startersMod) -> startersMod == Settings.StartersMod.RANDOM,
                                        (settings) -> settings.getStartersMod(), "Starters");
                        checkboxEnabledAndSelected(peEvolveLv1CBFixture,
                                        (settings) -> settings.isEvosLv1());
                        checkboxNotEnabledAndNotSelected(exactEvoCB,
                                        (settings) -> settings.isStartersExactEvo());
                        sliderNotEnabledAndValueMatches(spRandomSlider,
                                        (settings) -> settings.getStartersMinimumEvos() == 0,
                                        "Random Starters Slider");
                }, settingsReloadCount);
        }

        /**
         * Enabled by default
         * 
         * Not affected by radio button state
         * 
         * Turns slider on when toggled
         * 
         * Selecting minimum, maximum, or half-point value is reflected in the settings
         * 
         * Toggling the checkbox off disables the fully evolved slider and resets the value to 30
         * 
         * Saving settings to string and restoring produces no errors and retains state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestForceFullyEvolved() throws IOException {
                java.util.ResourceBundle bundle = java.util.ResourceBundle
                                .getBundle("com/dabomstew/pkrandom/gui/Bundle");
                Integer evoMin = Integer.parseInt(bundle
                                .getString("RandomizerGUI.tpForceFullyEvolvedSlider.minimum"));
                Integer evoMax = Integer.parseInt(bundle
                                .getString("RandomizerGUI.tpForceFullyEvolvedSlider.maximum"));
                JRadioButtonFixture tpUnchangedRB = getRadioButtonByName("tpUnchangedRB");
                JRadioButtonFixture tpRandomRB = getRadioButtonByName("tpRandomRB");
                JCheckBoxFixture tpForceFullyEvolvedCB = getCheckBoxByName("tpForceFullyEvolvedCB");
                JSliderFixture tpForceFullyEvolvedSlider =
                                getSliderByName("tpForceFullyEvolvedSlider");
                checkboxEnabledButNotSelected(tpForceFullyEvolvedCB,
                                (setts) -> setts.isTrainersForceFullyEvolved());
                assertFalse("Fully Evolved Slider should not be enabled yet",
                                tpForceFullyEvolvedSlider.isEnabled());

                // Turn random trainer on
                clickRBAndWait(tpRandomRB);
                checkboxEnabledButNotSelected(tpForceFullyEvolvedCB,
                                (setts) -> setts.isTrainersForceFullyEvolved());
                assertFalse("Fully Evolved Slider should not be enabled yet",
                                tpForceFullyEvolvedSlider.isEnabled());

                // Turn unchanged trainer on
                clickRBAndWait(tpUnchangedRB);
                checkboxEnabledButNotSelected(tpForceFullyEvolvedCB,
                                (setts) -> setts.isTrainersForceFullyEvolved());
                assertFalse("Fully Evolved Slider should not be enabled yet",
                                tpForceFullyEvolvedSlider.isEnabled());

                // Toggle Force Fully Evolved on
                tpForceFullyEvolvedCB.requireVisible().requireEnabled().click();
                Settings settings = this.mainWindow.getCurrentSettings();
                checkboxEnabledAndSelected(tpForceFullyEvolvedCB,
                                (setts) -> setts.isTrainersForceFullyEvolved());
                assertTrue("Fully Evolved Slider is not enabled",
                                tpForceFullyEvolvedSlider.isEnabled());
                assertTrue("Fully Evolved Slider is not at minimum",
                                settings.getTrainersForceFullyEvolvedLevel() == evoMin);

                // Set slider to maximum
                tpForceFullyEvolvedSlider.requireVisible().requireEnabled().slideToMaximum();
                settings = this.mainWindow.getCurrentSettings();
                checkboxEnabledAndSelected(tpForceFullyEvolvedCB,
                                (setts) -> setts.isTrainersForceFullyEvolved());
                assertTrue("Fully Evolved Slider is not enabled",
                                tpForceFullyEvolvedSlider.isEnabled());
                assertTrue("Fully Evolved Slider is not at maximum",
                                settings.getTrainersForceFullyEvolvedLevel() == evoMax);

                // Set slider to minimum
                tpForceFullyEvolvedSlider.requireVisible().requireEnabled().slideToMinimum();
                settings = this.mainWindow.getCurrentSettings();
                checkboxEnabledAndSelected(tpForceFullyEvolvedCB,
                                (setts) -> setts.isTrainersForceFullyEvolved());
                assertTrue("Fully Evolved Slider is not enabled",
                                tpForceFullyEvolvedSlider.isEnabled());
                assertTrue("Fully Evolved Slider is not at minimum",
                                settings.getTrainersForceFullyEvolvedLevel() == evoMin);

                // Set slider to halfway point
                tpForceFullyEvolvedSlider.requireVisible().requireEnabled()
                                .slideTo((evoMax + evoMin) / 2);
                settings = this.mainWindow.getCurrentSettings();
                checkboxEnabledAndSelected(tpForceFullyEvolvedCB,
                                (setts) -> setts.isTrainersForceFullyEvolved());
                assertTrue("Fully Evolved Slider is not enabled",
                                tpForceFullyEvolvedSlider.isEnabled());
                assertTrue("Fully Evolved Slider is not at halfway point", settings
                                .getTrainersForceFullyEvolvedLevel() == (evoMax + evoMin) / 2);

                // Toggle Force Fully Evolved off
                tpForceFullyEvolvedCB.requireVisible().requireEnabled().click();
                settings = this.mainWindow.getCurrentSettings();
                checkboxEnabledButNotSelected(tpForceFullyEvolvedCB,
                                (setts) -> setts.isTrainersForceFullyEvolved());
                assertFalse("Fully Evolved Slider is enabled but should not be",
                                tpForceFullyEvolvedSlider.isEnabled());
                assertTrue("Fully Evolved Slider is not at minimum",
                                settings.getTrainersForceFullyEvolvedLevel() == evoMin);
        }

        /**
         * Enabled by default
         * 
         * Not affected by radio button state
         * 
         * Turns slider on when toggled
         * 
         * Selecting minimum, maximum, or half-point value is reflected in the settings
         * 
         * Toggling the checkbox off disables the fully evolved slider and resets the value to 30
         * 
         * Saving settings to string and restoring produces no errors and retains state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestModifyTrainerLevel() throws IOException {
                java.util.ResourceBundle bundle = java.util.ResourceBundle
                                .getBundle("com/dabomstew/pkrandom/gui/Bundle");
                Integer lvMin = Integer.parseInt(
                                bundle.getString("RandomizerGUI.tpLevelModifierSlider.minimum"));
                Integer lvMax = Integer.parseInt(
                                bundle.getString("RandomizerGUI.tpLevelModifierSlider.maximum"));
                JRadioButtonFixture tpUnchangedRB = getRadioButtonByName("tpUnchangedRB");
                JRadioButtonFixture tpRandomRB = getRadioButtonByName("tpRandomRB");
                JCheckBoxFixture tpLevelModifierCB = getCheckBoxByName("tpLevelModifierCB");
                JSliderFixture tpLevelModifierSlider = getSliderByName("tpLevelModifierSlider");
                checkboxEnabledButNotSelected(tpLevelModifierCB,
                                (setts) -> setts.isTrainersLevelModified());
                assertFalse("Level Modifier Slider should not be enabled yet",
                                tpLevelModifierSlider.isEnabled());

                // Turn random trainer on
                clickRBAndWait(tpRandomRB);
                checkboxEnabledButNotSelected(tpLevelModifierCB,
                                (setts) -> setts.isTrainersLevelModified());
                assertFalse("Level Modifier Slider should not be enabled yet",
                                tpLevelModifierSlider.isEnabled());

                // Turn unchanged trainer on
                clickRBAndWait(tpUnchangedRB);
                checkboxEnabledButNotSelected(tpLevelModifierCB,
                                (setts) -> setts.isTrainersLevelModified());
                assertFalse("Level Modifier Slider should not be enabled yet",
                                tpLevelModifierSlider.isEnabled());

                // Toggle Level Modify on
                tpLevelModifierCB.requireVisible().requireEnabled().click();
                checkboxEnabledAndSelected(tpLevelModifierCB,
                                (setts) -> setts.isTrainersLevelModified());
                assertTrue("Level Modifier Slider is not enabled",
                                tpLevelModifierSlider.isEnabled());

                // Set slider to maximum
                tpLevelModifierSlider.requireVisible().requireEnabled().slideToMaximum();
                Settings settings = this.mainWindow.getCurrentSettings();
                checkboxEnabledAndSelected(tpLevelModifierCB,
                                (setts) -> setts.isTrainersLevelModified());
                assertTrue("Level Modifier Slider is not at maximum",
                                settings.getTrainersLevelModifier() == lvMax);

                // Set slider to minimum
                tpLevelModifierSlider.requireVisible().requireEnabled().slideToMinimum();
                settings = this.mainWindow.getCurrentSettings();
                checkboxEnabledAndSelected(tpLevelModifierCB,
                                (setts) -> setts.isTrainersLevelModified());
                assertTrue("Level Modifier Slider is not at minimum",
                                settings.getTrainersLevelModifier() == lvMin);

                // Set slider to halfway point
                tpLevelModifierSlider.requireVisible().requireEnabled()
                                .slideTo((lvMax + lvMin) / 2);
                settings = this.mainWindow.getCurrentSettings();
                checkboxEnabledAndSelected(tpLevelModifierCB,
                                (setts) -> setts.isTrainersLevelModified());
                assertTrue("Level Modifier Slider is not at halfway point",
                                settings.getTrainersLevelModifier() == (lvMax + lvMin) / 2);

                // Toggle Level Modify off
                tpLevelModifierCB.requireVisible().requireEnabled().click();
                checkboxEnabledButNotSelected(tpLevelModifierCB,
                                (setts) -> setts.isTrainersLevelModified());
                assertFalse("Level Modifier Slider should not be enabled yet",
                                tpLevelModifierSlider.isEnabled());
        }

        /**
         * Selecting "RANDOM" enables the Evos Same Stage checkbox
         * 
         * Toggles the Evos Same Stage checkbox
         * 
         * Selecting "UNCHANGED" disables the Evos Same Stage checkbox and resets state to false
         * 
         * Verifies settings can be stored and loaded with no error and preserve state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestEvoSameStage() throws IOException {
                JRadioButtonFixture unchangedEvolveRBFixture =
                                getRadioButtonByName("peUnchangedRB");
                JRadioButtonFixture randomEvolveRBFixture = getRadioButtonByName("peRandomRB");
                JCheckBoxFixture peSameStageCBFixture = getCheckBoxByName("peSameStageCB");
                TestCheckboxBasedOnRadioButton(unchangedEvolveRBFixture, randomEvolveRBFixture,
                                peSameStageCBFixture, (settings) -> settings.isEvosSameStage(),
                                (evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.UNCHANGED,
                                (evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.RANDOM,
                                (settings) -> settings.getEvolutionsMod(), "Evolutions");
        }

        /**
         * Selecting "RANDOM" enables the No Legendary Evos checkbox
         * 
         * Toggles the No Legendary Evos checkbox
         * 
         * Selecting "UNCHANGED" disables the No Legendary Evos checkbox and resets state to false
         * 
         * Verifies settings can be stored and loaded with no error and preserve state
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestEvoNoLegendaries() throws IOException {
                JRadioButtonFixture unchangedEvolveRBFixture =
                                getRadioButtonByName("peUnchangedRB");
                JRadioButtonFixture randomEvolveRBFixture = getRadioButtonByName("peRandomRB");
                JCheckBoxFixture peNoLegendariesCBFixture = getCheckBoxByName("peNoLegendariesCB");
                TestCheckboxBasedOnRadioButton(unchangedEvolveRBFixture, randomEvolveRBFixture,
                                peNoLegendariesCBFixture,
                                (settings) -> settings.isEvosNoLegendaries(),
                                (evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.UNCHANGED,
                                (evolutionsMod) -> evolutionsMod == Settings.EvolutionsMod.RANDOM,
                                (settings) -> settings.getEvolutionsMod(), "Evolutions");
        }

        /**
         * Selecting UPDATE_TYPE_EFFECTIVENESS evaluates to true
         * 
         * Not selecting evaluates to false
         * 
         * @throws IOException
         */
        @Test(timeout = 4000)
        public void TestUpdateTypeEffectiveness() throws IOException {
                JCheckBoxFixture updateTypeEffectivenessCBFixture =
                                getCheckBoxByName("Update Type Effectiveness");
                Settings settings = this.mainWindow.getCurrentSettings();
                // Sanity check - should evaluate to false
                assertTrue("Misc Tweaks should not be set yet",
                                settings.getCurrentMiscTweaks() == 0);
                checkboxEnabledButNotSelected(updateTypeEffectivenessCBFixture,
                                (setts) -> (setts.getCurrentMiscTweaks()
                                                & MiscTweak.UPDATE_TYPE_EFFECTIVENESS
                                                                .getValue()) > 0);
                // Sanity check - Should not fail with 0 options
                String setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                assertTrue("Misc Tweaks were selected after reloading settings 0",
                                settings.getCurrentMiscTweaks() == 0);
                checkboxEnabledButNotSelected(updateTypeEffectivenessCBFixture,
                                (setts) -> (setts.getCurrentMiscTweaks()
                                                & MiscTweak.UPDATE_TYPE_EFFECTIVENESS
                                                                .getValue()) > 0);

                // Turn UPDATE_TYPE_EFFECTIVENESS to true
                updateTypeEffectivenessCBFixture.requireVisible().requireEnabled().click();
                checkboxEnabledAndSelected(updateTypeEffectivenessCBFixture,
                                (setts) -> (setts.getCurrentMiscTweaks()
                                                & MiscTweak.UPDATE_TYPE_EFFECTIVENESS
                                                                .getValue()) > 0);
                setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                checkboxEnabledAndSelected(updateTypeEffectivenessCBFixture,
                                (setts) -> (setts.getCurrentMiscTweaks()
                                                & MiscTweak.UPDATE_TYPE_EFFECTIVENESS
                                                                .getValue()) > 0);

                // Turn UPDATE_TYPE_EFFECTIVENESS to false
                updateTypeEffectivenessCBFixture.requireVisible().requireEnabled().click();
                checkboxEnabledButNotSelected(updateTypeEffectivenessCBFixture,
                                (setts) -> (setts.getCurrentMiscTweaks()
                                                & MiscTweak.UPDATE_TYPE_EFFECTIVENESS
                                                                .getValue()) > 0);
                setttingsString = settings.toString();
                settings = Settings.fromString(setttingsString);
                checkboxEnabledButNotSelected(updateTypeEffectivenessCBFixture,
                                (setts) -> (setts.getCurrentMiscTweaks()
                                                & MiscTweak.UPDATE_TYPE_EFFECTIVENESS
                                                                .getValue()) > 0);
        }

        /**
         * Captures a common sequence of a checkbox being enabled or disabled based on radio button
         * selection
         * 
         * @param defaultRB - The radio button fixture that disables the checkbox (must be the one
         *                that's on by default)
         * @param triggerRB - The radio button fixture that enables the checkbox
         * @param checkboxToTest - The checkbox fixture that is being tested
         * @param settingsCheckboxFunction - The method in Settings.java that refers to the state of
         *                the checkbox
         * @param defaultRBCondition - The enum value that represents the defaultRb in radio button
         *                group
         * @param triggerRBCondition - The enum value that represents the triggerRB in the radio
         *                button group
         * @param settingsRBFunction - The method in Settings.java that refers to the state of the
         *                enum
         * @param buttonGroup - The name of the radio button group. Used for descriptive error
         *                messages
         * @throws IOException
         */
        private void TestCheckboxBasedOnRadioButton(JRadioButtonFixture defaultRB,
                        JRadioButtonFixture triggerRB, JCheckBoxFixture checkboxToTest,
                        Predicate<Settings> settingsCheckboxFunction,
                        Predicate<Enum> defaultRBCondition, Predicate<Enum> triggerRBCondition,
                        Function<Settings, Enum> settingsRBFunction, String buttonGroup)
                        throws IOException {
                AtomicInteger settingsReloadCount = new AtomicInteger(0);
                // Sanity check - should evaluate to false
                testConditions(() -> {
                        radioButtonSelected(defaultRB, defaultRBCondition, settingsRBFunction,
                                        buttonGroup);
                        checkboxNotEnabledAndNotSelected(checkboxToTest, settingsCheckboxFunction);
                }, settingsReloadCount);

                // Turn triggerRB on
                clickRBAndWait(triggerRB);
                testConditions(() -> {
                        radioButtonSelected(triggerRB, triggerRBCondition, settingsRBFunction,
                                        buttonGroup);
                        checkboxEnabledButNotSelected(checkboxToTest, settingsCheckboxFunction);
                }, settingsReloadCount);

                // Toggle checkboxToTest on
                checkboxToTest.requireVisible().requireEnabled().click();
                testConditions(() -> {
                        radioButtonSelected(triggerRB, triggerRBCondition, settingsRBFunction,
                                        buttonGroup);
                        checkboxEnabledAndSelected(checkboxToTest, settingsCheckboxFunction);
                }, settingsReloadCount);

                // Toggle checkboxToTest off
                checkboxToTest.requireVisible().requireEnabled().click();
                testConditions(() -> {
                        radioButtonSelected(triggerRB, triggerRBCondition, settingsRBFunction,
                                        buttonGroup);
                        checkboxEnabledButNotSelected(checkboxToTest, settingsCheckboxFunction);
                }, settingsReloadCount);

                // Turn defaultRB on while checkboxToTest is true should set checkboxToTest to
                // false
                checkboxToTest.requireVisible().requireEnabled().click();
                testConditions(() -> {
                        radioButtonSelected(triggerRB, triggerRBCondition, settingsRBFunction,
                                        buttonGroup);
                        checkboxEnabledAndSelected(checkboxToTest, settingsCheckboxFunction);
                }, settingsReloadCount);

                clickRBAndWait(defaultRB);
                testConditions(() -> {
                        radioButtonSelected(defaultRB, defaultRBCondition, settingsRBFunction,
                                        buttonGroup);
                        checkboxNotEnabledAndNotSelected(checkboxToTest, settingsCheckboxFunction);
                }, settingsReloadCount);
        }

        /**
         * Captures a common sequence of a radio button selection
         * 
         * @param defaultRB - The radio button that begins on by default
         * @param radioButtonToTest - The radio button that is being tested
         * @param defaultRBCondition - The enum value that represents the defaultRb in radio button
         *                group
         * @param settingsRBCondition - The enum value that represents the radioButtonToTest in
         *                radio button group
         * @param settingsRBFunction - The method in Settings.java that refers to the state of the
         *                enum
         * @param buttonGroup - The name of the radio button group. Used for descriptive error
         *                messages
         * @throws IOException
         */
        private void TestRadioButton(JRadioButtonFixture defaultRB,
                        JRadioButtonFixture radioButtonToTest, Predicate<Enum> defaultRBCondition,
                        Predicate<Enum> settingsRBCondition,
                        Function<Settings, Enum> settingsRBFunction, String buttonGroup)
                        throws IOException {
                AtomicInteger settingsReloadCount = new AtomicInteger(0);
                // Sanity check - should evaluate to false
                testConditions(() -> {
                        radioButtonSelected(defaultRB, defaultRBCondition, settingsRBFunction,
                                        buttonGroup);
                }, settingsReloadCount);

                // Turn radio button on
                clickRBAndWait(radioButtonToTest);
                testConditions(() -> {
                        radioButtonSelected(radioButtonToTest, settingsRBCondition,
                                        settingsRBFunction, buttonGroup);
                }, settingsReloadCount);

                // Turn Unchanged on
                clickRBAndWait(defaultRB);
                testConditions(() -> {
                        radioButtonSelected(defaultRB, defaultRBCondition, settingsRBFunction,
                                        buttonGroup);
                }, settingsReloadCount);
        }

        /**
         * Captures a common sequence of a checkbox being enabled or disabled based on another
         * checkbox
         * 
         * @param defaultRB - The radio button fixture that disables the checkbox (must be the one
         *                that's on by default)
         * @param triggerRB - The radio button fixture that enables the checkbox
         * @param checkboxToTest - The checkbox fixture that is being tested
         * @param checkboxToTrigger - The checkbox fixture that is being triggered by the
         *                checkboxToTest
         * @param settingsCheckboxToTestFunction - The method in Settings.java that refers to the
         *                state of the checkboxToTest
         * @param settingsCheckboxToTriggerFunction - The method in Settings.java that refers to the
         *                state of the checkboxToTrigger
         * @param defaultRBCondition - The enum value that represents the defaultRb in radio button
         *                group
         * @param triggerRBCondition - The enum value that represents the triggerRB in the radio
         *                button group
         * @param settingsRBFunction - The method in Settings.java that refers to the state of the
         *                enum
         * @param buttonGroup - The name of the radio button group. Used for descriptive error
         *                messages
         * @throws IOException
         */
        private void TestCheckboxBasedOnCheckbox(JRadioButtonFixture defaultRB,
                        JRadioButtonFixture triggerRB, JCheckBoxFixture checkboxToTest,
                        JCheckBoxFixture checkboxToTrigger,
                        Predicate<Settings> settingsCheckboxToTestFunction,
                        Predicate<Settings> settingsCheckboxToTriggerFunction,
                        Predicate<Enum> defaultRBCondition, Predicate<Enum> triggerRBCondition,
                        Function<Settings, Enum> settingsRBFunction, String buttonGroup)
                        throws IOException {
                AtomicInteger settingsReloadCount = new AtomicInteger(0);
                // Sanity check - should evaluate to false
                testConditions(() -> {
                        radioButtonSelected(defaultRB, defaultRBCondition, settingsRBFunction,
                                        buttonGroup);
                        checkboxNotEnabledAndNotSelected(checkboxToTest,
                                        settingsCheckboxToTestFunction);
                        checkboxNotEnabledAndNotSelected(checkboxToTrigger,
                                        settingsCheckboxToTriggerFunction);
                }, settingsReloadCount);

                // Turn triggerRB on
                clickRBAndWait(triggerRB);

                // Turn checkboxToTest on
                checkboxToTest.requireVisible().requireEnabled().click();
                testConditions(() -> {
                        radioButtonSelected(triggerRB, triggerRBCondition, settingsRBFunction,
                                        buttonGroup);
                        checkboxEnabledAndSelected(checkboxToTest, settingsCheckboxToTestFunction);
                        checkboxNotEnabledAndNotSelected(checkboxToTrigger,
                                        settingsCheckboxToTriggerFunction);
                }, settingsReloadCount);


                // Turn checkboxToTest off
                checkboxToTest.requireVisible().requireEnabled().click();
                testConditions(() -> {
                        radioButtonSelected(triggerRB, triggerRBCondition, settingsRBFunction,
                                        buttonGroup);
                        checkboxEnabledButNotSelected(checkboxToTest,
                                        settingsCheckboxToTestFunction);
                        checkboxEnabledButNotSelected(checkboxToTrigger,
                                        settingsCheckboxToTriggerFunction);
                }, settingsReloadCount);


                // Turn checkboxToTrigger on
                checkboxToTrigger.requireVisible().requireEnabled().click();
                testConditions(() -> {
                        radioButtonSelected(triggerRB, triggerRBCondition, settingsRBFunction,
                                        buttonGroup);
                        checkboxEnabledButNotSelected(checkboxToTest,
                                        settingsCheckboxToTestFunction);
                        checkboxEnabledAndSelected(checkboxToTrigger,
                                        settingsCheckboxToTriggerFunction);
                }, settingsReloadCount);

                // Turn checkboxToTest on
                checkboxToTest.requireVisible().requireEnabled().click();
                testConditions(() -> {
                        radioButtonSelected(triggerRB, triggerRBCondition, settingsRBFunction,
                                        buttonGroup);
                        checkboxEnabledAndSelected(checkboxToTest, settingsCheckboxToTestFunction);
                        checkboxNotEnabledAndNotSelected(checkboxToTrigger,
                                        settingsCheckboxToTriggerFunction);
                }, settingsReloadCount);

                // Turn checkboxToTest off
                checkboxToTest.requireVisible().requireEnabled().click();
                testConditions(() -> {
                        radioButtonSelected(triggerRB, triggerRBCondition, settingsRBFunction,
                                        buttonGroup);
                        checkboxEnabledButNotSelected(checkboxToTest,
                                        settingsCheckboxToTestFunction);
                        checkboxEnabledButNotSelected(checkboxToTrigger,
                                        settingsCheckboxToTriggerFunction);
                }, settingsReloadCount);

                // Turn checkboxToTrigger on and defaultRB on - should disable all checkboxes and
                // deselect
                checkboxToTrigger.requireVisible().requireEnabled().click();
                testConditions(() -> {
                        radioButtonSelected(triggerRB, triggerRBCondition, settingsRBFunction,
                                        buttonGroup);
                        checkboxEnabledButNotSelected(checkboxToTest,
                                        settingsCheckboxToTestFunction);
                        checkboxEnabledAndSelected(checkboxToTrigger,
                                        settingsCheckboxToTriggerFunction);
                }, settingsReloadCount);
                defaultRB.requireVisible().requireEnabled().click();
                testConditions(() -> {
                        radioButtonSelected(defaultRB, defaultRBCondition, settingsRBFunction,
                                        buttonGroup);
                        checkboxNotEnabledAndNotSelected(checkboxToTest,
                                        settingsCheckboxToTestFunction);
                        checkboxNotEnabledAndNotSelected(checkboxToTrigger,
                                        settingsCheckboxToTriggerFunction);
                }, settingsReloadCount);
        }

        /**
         * Clicks a JRadioButton and waits for the UI to update before completing
         * 
         * @param rbFixture - The fixture representing the radio button to click
         */
        private void clickRBAndWait(JRadioButtonFixture rbFixture) {
                rbFixture.requireVisible().requireEnabled().click();
                await().until(() -> this.mainWindow.isUIUpdated());
        }

        /**
         * Clicks a JCheckBox and waits for the UI to update before completing
         * 
         * @param cbFixture - The fixture representing the radio button to click
         */
        private void clickCBAndWait(JCheckBoxFixture cbFixture) {
                cbFixture.requireVisible().requireEnabled().click();
                await().until(() -> this.mainWindow.isUIUpdated());
        }

        private void testConditions(Runnable conditionRunnable, AtomicInteger reloadCount) {
                Settings settings = null;
                try {
                        settings = this.mainWindow.getCurrentSettings();
                } catch (IOException ioe) {
                        ioe.printStackTrace();
                }

                conditionRunnable.run();

                try {
                        String setttingsString = settings.toString();
                        settings = Settings.fromString(setttingsString);
                } catch (Throwable t) {
                        t.printStackTrace();
                }

                try {
                        conditionRunnable.run();
                } catch (Throwable t) {
                        System.out.println("Failed at reload count " + reloadCount.get());
                        throw t;
                }
                reloadCount.getAndIncrement();
        }

        /**
         * Checks if a radio button is selected and settings state confirms
         * 
         * @param rbFixture - The radio button being tested
         * @param settingsRBFunction - The method in Settings.java that refers to the state of the
         *                enum
         */
        private void radioButtonSelected(JRadioButtonFixture rbFixture,
                        Predicate<Enum> settingsRBCondition,
                        Function<Settings, Enum> settingsRBFunction, String buttonGroup) {
                Settings settings = null;
                try {
                        settings = this.mainWindow.getCurrentSettings();
                } catch (IOException ioe) {
                        ioe.printStackTrace();
                }
                assertTrue(buttonGroup + " should be set to " + rbFixture.text() + " but was not",
                                settingsRBCondition.test(settingsRBFunction.apply(settings)));
                assertTrue(rbFixture.text() + " was not enabled", rbFixture.isEnabled());
                assertTrue(rbFixture.text() + " was not selected",
                                rbFixture.requireSelected() == rbFixture);
        }

        /**
         * Checks if a checkbox is both disabled and unselected, and confirms the settings state is
         * also false
         * 
         * @param cbFixture - The checkbox being tested
         * @param settingsCheckboxFunction - The method in Settings.java that refers to the state of
         *                the checkbox
         */
        private void checkboxNotEnabledAndNotSelected(JCheckBoxFixture cbFixture,
                        Predicate<Settings> settingsCheckboxFunction) {
                Settings settings = null;
                try {
                        settings = this.mainWindow.getCurrentSettings();
                } catch (IOException ioe) {
                        ioe.printStackTrace();
                }
                assertFalse(cbFixture.text() + " was true",
                                settingsCheckboxFunction.test(settings));
                assertFalse(cbFixture.text() + " was enabled", cbFixture.isEnabled());
                assertTrue(cbFixture.text() + " was selected",
                                cbFixture.requireNotSelected() == cbFixture);
        }

        /**
         * Checks if a checkbox is enabled but unselected, and confirms the settings state is also
         * false
         * 
         * @param cbFixture - The checkbox being tested
         * @param settingsCheckboxFunction - The method in Settings.java that refers to the state of
         *                the checkbox
         */
        private void checkboxEnabledButNotSelected(JCheckBoxFixture cbFixture,
                        Predicate<Settings> settingsCheckboxFunction) {
                Settings settings = null;
                try {
                        settings = this.mainWindow.getCurrentSettings();
                } catch (IOException ioe) {
                        ioe.printStackTrace();
                }
                assertFalse(cbFixture.text() + " was true",
                                settingsCheckboxFunction.test(settings));
                assertTrue(cbFixture.text() + " was not enabled", cbFixture.isEnabled());
                assertTrue(cbFixture.text() + " was selected",
                                cbFixture.requireNotSelected() == cbFixture);
        }

        /**
         * Checks if a checkbox is enabled and selected, and confirms the settings state is also
         * true
         * 
         * @param cbFixture - The checkbox being tested
         * @param settingsCheckboxFunction - The method in Settings.java that refers to the state of
         *                the checkbox
         */
        private void checkboxEnabledAndSelected(JCheckBoxFixture cbFixture,
                        Predicate<Settings> settingsCheckboxFunction) {
                Settings settings = null;
                try {
                        settings = this.mainWindow.getCurrentSettings();
                } catch (IOException ioe) {
                        ioe.printStackTrace();
                }
                assertTrue(cbFixture.text() + " was false",
                                settingsCheckboxFunction.test(settings));
                assertTrue(cbFixture.text() + " was not enabled", cbFixture.isEnabled());
                assertTrue(cbFixture.text() + " was selected",
                                cbFixture.requireSelected() == cbFixture);
        }

        /**
         * Checks if a slider is disabled, and confirms the settings state matches provided
         * expectations
         * 
         * @param sliderFixture - The slider being tested
         * @param settingsSliderFunction - The method in Settings.java that refers to the state of
         *                the slider
         * @param sliderName - The name of this slider (for better error messages)
         */
        private void sliderNotEnabledAndValueMatches(JSliderFixture sliderFixture,
                        Predicate<Settings> settingsSliderFunction, String sliderName) {
                Settings settings = null;
                try {
                        settings = this.mainWindow.getCurrentSettings();
                } catch (IOException ioe) {
                        ioe.printStackTrace();
                }
                assertTrue(sliderName + " did not match expected settings value",
                                settingsSliderFunction.test(settings));
                assertFalse(sliderName + " was enabled", sliderFixture.isEnabled());
        }

        /**
         * Checks if a slider is disabled, and confirms the settings state matches provided
         * expectations
         * 
         * @param sliderFixture - The slider being tested
         * @param settingsSliderFunction - The method in Settings.java that refers to the state of
         *                the slider
         * @param sliderName - The name of this slider (for better error messages)
         */
        private void sliderEnabledAndValueMatches(JSliderFixture sliderFixture,
                        Predicate<Settings> settingsSliderFunction, String sliderName) {
                Settings settings = null;
                try {
                        settings = this.mainWindow.getCurrentSettings();
                } catch (IOException ioe) {
                        ioe.printStackTrace();
                }
                assertTrue(sliderName + " did not match expected settings value",
                                settingsSliderFunction.test(settings));
                assertTrue(sliderName + " was disabled", sliderFixture.isEnabled());
        }
}
