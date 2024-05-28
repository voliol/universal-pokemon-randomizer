package com.dabomstew.pkrandom.gui;

/*----------------------------------------------------------------------------*/
/*--  Part of "Universal Pokemon Randomizer ZX" by the UPR-ZX team          --*/
/*--  Originally part of "Universal Pokemon Randomizer" by Dabomstew        --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2020.                                 --*/
/*--                                                                        --*/
/*--  The custom code written here is licensed under the terms of the GPL:  --*/
/*--                                                                        --*/
/*--  This program is free software: you can redistribute it and/or modify  --*/
/*--  it under the terms of the GNU General Public License as published by  --*/
/*--  the Free Software Foundation, either version 3 of the License, or     --*/
/*--  (at your option) any later version.                                   --*/
/*--                                                                        --*/
/*--  This program is distributed in the hope that it will be useful,       --*/
/*--  but WITHOUT ANY WARRANTY; without even the implied warranty of        --*/
/*--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          --*/
/*--  GNU General Public License for more details.                          --*/
/*--                                                                        --*/
/*--  You should have received a copy of the GNU General Public License     --*/
/*--  along with this program. If not, see <http://www.gnu.org/licenses/>.  --*/
/*----------------------------------------------------------------------------*/

import com.dabomstew.pkrandom.*;
import com.dabomstew.pkrandom.cli.CliRandomizer;
import com.dabomstew.pkrandom.constants.Gen3Constants;
import com.dabomstew.pkrandom.constants.GlobalConstants;
import com.dabomstew.pkrandom.exceptions.CannotWriteToLocationException;
import com.dabomstew.pkrandom.exceptions.EncryptedROMException;
import com.dabomstew.pkrandom.exceptions.InvalidSupplementFilesException;
import com.dabomstew.pkrandom.exceptions.RandomizationException;
import com.dabomstew.pkrandom.graphics.packs.*;
import com.dabomstew.pkrandom.pokemon.ExpCurve;
import com.dabomstew.pkrandom.pokemon.GenRestrictions;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Type;
import com.dabomstew.pkrandom.random.SeedPicker;
import com.dabomstew.pkrandom.romhandlers.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * The main GUI for the Universal Pokemon Randomizer ZX, containing the various options available and such.
 */
public class RandomizerGUI {
    private JTabbedPane tabbedPane1;
    private JCheckBox raceModeCheckBox;
    private JButton openROMButton;
    private JButton randomizeSaveButton;
    private JButton premadeSeedButton;
    private JButton settingsButton;
    private JButton loadSettingsButton;
    private JButton saveSettingsButton;
    private JPanel mainPanel;
    private JRadioButton pbsUnchangedRadioButton;
    private JRadioButton pbsShuffleRadioButton;
    private JRadioButton pbsRandomRadioButton;
    private JRadioButton pbsLegendariesSlowRadioButton;
    private JRadioButton pbsStrongLegendariesSlowRadioButton;
    private JRadioButton pbsAllMediumFastRadioButton;
    private JCheckBox pbsStandardizeEXPCurvesCheckBox;
    private JCheckBox pbsFollowEvolutionsCheckBox;
    private JCheckBox pbsUpdateBaseStatsCheckBox;
    private JCheckBox ptIsDualTypeCheckBox;
    private JRadioButton ptUnchangedRadioButton;
    private JRadioButton ptRandomFollowEvolutionsRadioButton;
    private JRadioButton ptRandomCompletelyRadioButton;
    private JRadioButton paUnchangedRadioButton;
    private JRadioButton paRandomRadioButton;
    private JCheckBox paAllowWonderGuardCheckBox;
    private JCheckBox paFollowEvolutionsCheckBox;
    private JCheckBox paTrappingAbilitiesCheckBox;
    private JCheckBox paNegativeAbilitiesCheckBox;
    private JCheckBox paBadAbilitiesCheckBox;
    private JRadioButton peUnchangedRadioButton;
    private JRadioButton peRandomRadioButton;
    private JCheckBox peSimilarStrengthCheckBox;
    private JCheckBox peSameTypingCheckBox;
    private JCheckBox peLimitEvolutionsToThreeCheckBox;
    private JCheckBox peForceChangeCheckBox;
    private JCheckBox peChangeImpossibleEvosCheckBox;
    private JCheckBox peMakeEvolutionsEasierCheckBox;
    private JCheckBox peForceGrowthCheckBox;
    private JCheckBox peNoConvergenceCheckBox;
    private JRadioButton spUnchangedRadioButton;
    private JRadioButton spCustomRadioButton;
    private JRadioButton spRandomCompletelyRadioButton;
    private JRadioButton spRandomTwoEvosRadioButton;
    private JComboBox<String> spComboBox1;
    private JComboBox<String> spComboBox2;
    private JComboBox<String> spComboBox3;
    private JCheckBox spRandomizeStarterHeldItemsCheckBox;
    private JCheckBox spBanBadItemsCheckBox;
    private JRadioButton stpUnchangedRadioButton;
    private JRadioButton stpSwapLegendariesSwapStandardsRadioButton;
    private JRadioButton stpRandomCompletelyRadioButton;
    private JRadioButton stpRandomSimilarStrengthRadioButton;
    private JCheckBox stpLimitMainGameLegendariesCheckBox;
    private JCheckBox stpRandomize600BSTCheckBox;
    private JRadioButton igtUnchangedRadioButton;
    private JRadioButton igtRandomizeGivenPokemonOnlyRadioButton;
    private JRadioButton igtRandomizeBothRequestedGivenRadioButton;
    private JCheckBox igtRandomizeNicknamesCheckBox;
    private JCheckBox igtRandomizeOTsCheckBox;
    private JCheckBox igtRandomizeIVsCheckBox;
    private JCheckBox igtRandomizeItemsCheckBox;
    private JCheckBox mdRandomizeMovePowerCheckBox;
    private JCheckBox mdRandomizeMoveAccuracyCheckBox;
    private JCheckBox mdRandomizeMovePPCheckBox;
    private JCheckBox mdRandomizeMoveTypesCheckBox;
    private JCheckBox mdRandomizeMoveCategoryCheckBox;
    private JCheckBox mdUpdateMovesCheckBox;
    private JCheckBox mdLegacyCheckBox;
    private JRadioButton pmsUnchangedRadioButton;
    private JRadioButton pmsRandomPreferringSameTypeRadioButton;
    private JRadioButton pmsRandomCompletelyRadioButton;
    private JRadioButton pmsMetronomeOnlyModeRadioButton;
    private JCheckBox pmsGuaranteedLevel1MovesCheckBox;
    private JCheckBox pmsReorderDamagingMovesCheckBox;
    private JCheckBox pmsNoGameBreakingMovesCheckBox;
    private JCheckBox pmsForceGoodDamagingCheckBox;
    private JSlider pmsGuaranteedLevel1MovesSlider;
    private JSlider pmsForceGoodDamagingSlider;
    private JCheckBox tpRivalCarriesStarterCheckBox;
    private JCheckBox tpSimilarStrengthCheckBox;
    private JCheckBox tpWeightTypesCheckBox;
    private JCheckBox tpDontUseLegendariesCheckBox;
    private JCheckBox tpNoEarlyWonderGuardCheckBox;
    private JCheckBox tpRandomizeTrainerNamesCheckBox;
    private JCheckBox tpRandomizeTrainerClassNamesCheckBox;
    private JCheckBox tpForceFullyEvolvedAtCheckBox;
    private JSlider tpForceFullyEvolvedAtSlider;
    private JSlider tpPercentageLevelModifierSlider;
    private JCheckBox tpEliteFourUniquePokemonCheckBox;
    private JSpinner tpEliteFourUniquePokemonSpinner;
    private JCheckBox tpPercentageLevelModifierCheckBox;
    private JRadioButton wpUnchangedRadioButton;
    private JRadioButton wpRandomRadioButton;
    private JRadioButton wpArea1To1RadioButton;
    private JRadioButton wpLocation1To1RadioButton;
    private JRadioButton wpGlobal1To1RadioButton;
    private JCheckBox wpSimilarStrengthCheckBox;
    private JCheckBox wpCatchEmAllModeCheckBox;
    private JRadioButton wpTRNoneRadioButton;
    private JRadioButton wpTRThemedAreasRadioButton;
    private JRadioButton wpTRKeepPrimaryRadioButton;
    private JCheckBox wpUseTimeBasedEncountersCheckBox;
    private JCheckBox wpDontUseLegendariesCheckBox;
    private JCheckBox wpSetMinimumCatchRateCheckBox;
    private JCheckBox wpRandomizeHeldItemsCheckBox;
    private JCheckBox wpBanBadItemsCheckBox;
    private JCheckBox wpBalanceShakingGrassPokemonCheckBox;
    private JCheckBox wpPercentageLevelModifierCheckBox;
    private JSlider wpPercentageLevelModifierSlider;
    private JSlider wpSetMinimumCatchRateSlider;
    private JRadioButton tmUnchangedRadioButton;
    private JRadioButton tmRandomRadioButton;
    private JCheckBox tmFullHMCompatibilityCheckBox;
    private JCheckBox tmLevelupMoveSanityCheckBox;
    private JCheckBox tmKeepFieldMoveTMsCheckBox;
    private JCheckBox tmForceGoodDamagingCheckBox;
    private JSlider tmForceGoodDamagingSlider;
    private JRadioButton thcUnchangedRadioButton;
    private JRadioButton thcRandomPreferSameTypeRadioButton;
    private JRadioButton thcRandomCompletelyRadioButton;
    private JRadioButton thcFullCompatibilityRadioButton;
    private JRadioButton mtUnchangedRadioButton;
    private JRadioButton mtRandomRadioButton;
    private JCheckBox mtLevelupMoveSanityCheckBox;
    private JCheckBox mtKeepFieldMoveTutorsCheckBox;
    private JCheckBox mtForceGoodDamagingCheckBox;
    private JSlider mtForceGoodDamagingSlider;
    private JRadioButton mtcUnchangedRadioButton;
    private JRadioButton mtcRandomPreferSameTypeRadioButton;
    private JRadioButton mtcRandomCompletelyRadioButton;
    private JRadioButton mtcFullCompatibilityRadioButton;
    private JRadioButton fiUnchangedRadioButton;
    private JRadioButton fiShuffleRadioButton;
    private JRadioButton fiRandomRadioButton;
    private JRadioButton fiRandomEvenDistributionRadioButton;
    private JCheckBox fiBanBadItemsCheckBox;
    private JRadioButton shUnchangedRadioButton;
    private JRadioButton shShuffleRadioButton;
    private JRadioButton shRandomRadioButton;
    private JCheckBox shBanOverpoweredShopItemsCheckBox;
    private JCheckBox shBanBadItemsCheckBox;
    private JCheckBox shBanRegularShopItemsCheckBox;
    private JCheckBox shBalanceShopItemPricesCheckBox;
    private JCheckBox shGuaranteeEvolutionItemsCheckBox;
    private JCheckBox shGuaranteeXItemsCheckBox;
    private JCheckBox miscBWExpPatchCheckBox;
    private JCheckBox miscNerfXAccuracyCheckBox;
    private JCheckBox miscFixCritRateCheckBox;
    private JCheckBox miscFastestTextCheckBox;
    private JCheckBox miscRunningShoesIndoorsCheckBox;
    private JCheckBox miscRandomizePCPotionCheckBox;
    private JCheckBox miscAllowPikachuEvolutionCheckBox;
    private JCheckBox miscGiveNationalDexAtCheckBox;
    private JCheckBox miscUpdateTypeEffectivenessCheckBox;
    private JCheckBox miscLowerCasePokemonNamesCheckBox;
    private JCheckBox miscRandomizeCatchingTutorialCheckBox;
    private JCheckBox miscBanLuckyEggCheckBox;
    private JCheckBox miscNoFreeLuckyEggCheckBox;
    private JCheckBox miscBanBigMoneyManiacCheckBox;
    private JPanel pokemonAbilitiesPanel;
    private JPanel moveTutorPanel;
    private JPanel mtMovesPanel;
    private JPanel mtCompatPanel;
    private JLabel mtNoExistLabel;
    private JPanel shopItemsPanel;
    private JLabel mtNoneAvailableLabel;
    private JPanel miscTweaksPanel;
    private JLabel gameMascotLabel;
    private JPanel baseTweaksPanel;
    private JLabel romNameLabel;
    private JLabel romCodeLabel;
    private JLabel romSupportLabel;
    private JLabel websiteLinkLabel;
    private JCheckBox tmNoGameBreakingMovesCheckBox;
    private JCheckBox mtNoGameBreakingMovesCheckBox;
    private JCheckBox limitPokemonCheckBox;
    private JButton limitPokemonButton;
    private JCheckBox tpAllowAlternateFormesCheckBox;
    private JLabel versionLabel;
    private JCheckBox pbsFollowMegaEvosCheckBox;
    private JCheckBox paFollowMegaEvosCheckBox;
    private JCheckBox ptFollowMegaEvosCheckBox;
    private JCheckBox spAllowAltFormesCheckBox;
    private JCheckBox stpAllowAltFormesCheckBox;
    private JCheckBox stpSwapMegaEvosCheckBox;
    private JCheckBox tpSwapMegaEvosCheckBox;
    private JCheckBox wpAllowAltFormesCheckBox;
    private JCheckBox tpDoubleBattleModeCheckBox;
    private JCheckBox tpBossTrainersCheckBox;
    private JCheckBox tpImportantTrainersCheckBox;
    private JCheckBox tpRegularTrainersCheckBox;
    private JSpinner tpBossTrainersSpinner;
    private JSpinner tpImportantTrainersSpinner;
    private JSpinner tpRegularTrainersSpinner;
    private JLabel tpAdditionalPokemonForLabel;
    private JCheckBox peAllowAltFormesCheckBox;
    private JCheckBox miscSOSBattlesCheckBox;
    private JCheckBox tpRandomShinyTrainerPokemonCheckBox;
    private JRadioButton totpUnchangedRadioButton;
    private JRadioButton totpRandomRadioButton;
    private JRadioButton totpRandomSimilarStrengthRadioButton;
    private JRadioButton totpAllyUnchangedRadioButton;
    private JRadioButton totpAllyRandomRadioButton;
    private JRadioButton totpAllyRandomSimilarStrengthRadioButton;
    private JPanel totpAllyPanel;
    private JPanel totpAuraPanel;
    private JRadioButton totpAuraUnchangedRadioButton;
    private JRadioButton totpAuraRandomRadioButton;
    private JRadioButton totpAuraRandomSameStrengthRadioButton;
    private JCheckBox totpPercentageLevelModifierCheckBox;
    private JSlider totpPercentageLevelModifierSlider;
    private JCheckBox totpRandomizeHeldItemsCheckBox;
    private JCheckBox totpAllowAltFormesCheckBox;
    private JPanel totpPanel;
    private JCheckBox pmsEvolutionMovesCheckBox;
    private JComboBox<String> pbsUpdateComboBox;
    private JComboBox<String> mdUpdateComboBox;
    private JLabel wikiLinkLabel;
    private JCheckBox paWeighDuplicatesTogetherCheckBox;
    private JCheckBox miscBalanceStaticLevelsCheckBox;
    private JCheckBox miscRetainAltFormesCheckBox;
    private JComboBox pbsEXPCurveComboBox;
    private JCheckBox miscRunWithoutRunningShoesCheckBox;
    private JCheckBox peRemoveTimeBasedEvolutionsCheckBox;
    private JCheckBox tmFollowEvolutionsCheckBox;
    private JCheckBox mtFollowEvolutionsCheckBox;
    private JCheckBox stpPercentageLevelModifierCheckBox;
    private JSlider stpPercentageLevelModifierSlider;
    private JCheckBox stpFixMusicCheckBox;
    private JCheckBox miscFasterHPAndEXPBarsCheckBox;
    private JCheckBox tpBossTrainersItemsCheckBox;
    private JCheckBox tpImportantTrainersItemsCheckBox;
    private JCheckBox tpRegularTrainersItemsCheckBox;
    private JLabel tpHeldItemsLabel;
    private JCheckBox tpConsumableItemsOnlyCheckBox;
    private JCheckBox tpSensibleItemsCheckBox;
    private JCheckBox tpHighestLevelGetsItemCheckBox;
    private JPanel pickupItemsPanel;
    private JRadioButton puUnchangedRadioButton;
    private JRadioButton puRandomRadioButton;
    private JCheckBox puBanBadItemsCheckBox;
    private JCheckBox miscForceChallengeModeCheckBox;
    private JCheckBox pbsAssignEvoStatsRandomlyCheckBox;
    private JCheckBox noIrregularAltFormesCheckBox;
    private JRadioButton peRandomEveryLevelRadioButton;
    private JCheckBox miscFastDistortionWorldCheckBox;
    private JComboBox tpComboBox;
    private JCheckBox tpBetterMovesetsCheckBox;
    private JCheckBox paEnsureTwoAbilitiesCheckbox;
    private JRadioButton ppalUnchangedRadioButton;
    private JRadioButton ppalRandomRadioButton;
    private JCheckBox ppalFollowTypesCheckBox;
    private JCheckBox ppalFollowEvolutionsCheckBox;
    private JCheckBox ppalShinyFromNormalCheckBox;
    private JPanel graphicsPanel;
    private JLabel ppalNotExistLabel;
    private JLabel ppalPartiallyImplementedLabel;
    private JLabel cpgNotExistLabel;
    private JRadioButton cpgUnchangedRadioButton;
    private JRadioButton cpgCustomRadioButton;
    private JComboBox<GraphicsPack> cpgComboBox;
    private JButton cpgRandomButton;
    private GraphicsPackInfo cpgCustomInfo;
    private JLabel cpgReplaceLabel;
    private JRadioButton cpgReplaceRadioButton1;
    private JRadioButton cpgReplaceRadioButton2;
    private JCheckBox miscUpdateRotomFormeTypingCheckBox;
    private JCheckBox miscDisableLowHPMusicCheckBox;
    private JCheckBox tpUseLocalPokemonCheckBox;
    private JRadioButton spTypeTriangleRadioButton;
    private JRadioButton spTypeNoneRadioButton;
    private JRadioButton spRandomBasicRadioButton;
    private JRadioButton spTypeFwgRadioButton;
    private JRadioButton spTypeSingleRadioButton;
    private JComboBox spTypeSingleComboBox;
    private JCheckBox spTypeNoDualCheckbox;
    private JRadioButton spTypeUniqueRadioButton;
    private JCheckBox spNoLegendariesCheckBox;
    private JCheckBox wpTRKeepThemesCheckBox;
    private JPanel typesPanel;
    private JRadioButton teUnchangedRadioButton;
    private JRadioButton teRandomRadioButton;
    private JRadioButton teRandomBalancedRadioButton;
    private JRadioButton teKeepTypeIdentitiesRadioButton;
    private JRadioButton teInverseRadioButton;
    private JCheckBox teAddRandomImmunitiesCheckBox;
    private JCheckBox teUpdateTypeEffectivenessCheckbox;

    private static final Random RND = new Random();

    private static JFrame frame;

    private static String launcherInput = "";
    public static boolean usedLauncher = false;

    private GenRestrictions currentRestrictions;
    private OperationDialog opDialog;

    private ResourceBundle bundle;
    protected RomHandler.Factory[] checkHandlers;
    private RomHandler romHandler;

    private boolean presetMode = false;
    private boolean initialPopup = true;
    private boolean showInvalidRomPopup = true;

    private List<JCheckBox> tweakCheckBoxes;
    private JPanel liveTweaksPanel = new JPanel();

    private JFileChooser romOpenChooser = new JFileChooser();
    private JFileChooser romSaveChooser = new JFileChooser();
    private JFileChooser qsOpenChooser = new JFileChooser();
    private JFileChooser qsSaveChooser = new JFileChooser();
    private JFileChooser qsUpdateChooser = new JFileChooser();
    private JFileChooser gameUpdateChooser = new JFileChooser();

    private JPopupMenu settingsMenu;
    private JMenuItem customNamesEditorMenuItem;
    private JMenuItem applyGameUpdateMenuItem;
    private JMenuItem removeGameUpdateMenuItem;
    private JMenuItem loadGetSettingsMenuItem;
    private JMenuItem keepOrUnloadGameAfterRandomizingMenuItem;
    private JMenuItem batchRandomizationMenuItem;

    private ImageIcon emptyIcon = new ImageIcon(getClass().getResource("/com/dabomstew/pkrandom/gui/emptyIcon.png"));
    private boolean haveCheckedCustomNames, unloadGameOnSuccess;
    private Map<String, String> gameUpdates = new TreeMap<>();

    private List<String> trainerSettings = new ArrayList<>();
    private List<String> trainerSettingToolTips = new ArrayList<>();
    private final int TRAINER_UNCHANGED = 0, TRAINER_RANDOM = 1, TRAINER_RANDOM_EVEN = 2, TRAINER_RANDOM_EVEN_MAIN = 3,
                        TRAINER_TYPE_THEMED = 4, TRAINER_TYPE_THEMED_ELITE4_GYMS = 5, TRAINER_KEEP_THEMED = 6;

    private BatchRandomizationSettings batchRandomizationSettings;

    public RandomizerGUI() {
        ToolTipManager.sharedInstance().setInitialDelay(400);
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        bundle = ResourceBundle.getBundle("com/dabomstew/pkrandom/gui/Bundle");
        testForRequiredConfigs();
        checkHandlers = new RomHandler.Factory[] { new Gen1RomHandler.Factory(), new Gen2RomHandler.Factory(),
                new Gen3RomHandler.Factory(), new Gen4RomHandler.Factory(), new Gen5RomHandler.Factory(),
                new Gen6RomHandler.Factory(), new Gen7RomHandler.Factory() };

        haveCheckedCustomNames = false;
        attemptReadConfig();
        initExplicit();
        initTweaksPanel();
        initFileChooserDirectories();

        boolean canWrite = attemptWriteConfig();
        if (!canWrite) {
            JOptionPane.showMessageDialog(null, bundle.getString("GUI.cantWriteConfigFile"));
        }

        if (!haveCheckedCustomNames) {
            checkCustomNames();
        }

        new Thread(() -> {
            String latestVersionString = "???";

            try {

                // TODO: think of how to make this work with the V branch
                URL url = new URL(SysConstants.API_URL_ZX);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);

                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

                String output;
                while ((output = br.readLine()) != null) {
                    String[] a = output.split("tag_name\":\"");
                    if (a.length > 1) {
                        latestVersionString = a[1].split("\",")[0];
                    }
                }

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }

            // If the release version is newer than this version, bold it to make it more obvious.
            if (Version.isReleaseVersionNewer(latestVersionString)) {
                latestVersionString = String.format("<b>%s</b>", latestVersionString);
            }
            String finalLatestVersionString = latestVersionString;
            SwingUtilities.invokeLater(() -> {
                websiteLinkLabel.setText(String.format(bundle.getString("GUI.websiteLinkLabel.text"), finalLatestVersionString));
            });
        }).run();

        frame.setTitle(String.format(bundle.getString("GUI.windowTitle"),Version.VERSION_STRING));

        openROMButton.addActionListener(e -> loadROM());
        pbsUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        pbsShuffleRadioButton.addActionListener(e -> enableOrDisableSubControls());
        pbsRandomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        pbsFollowMegaEvosCheckBox.addActionListener(e -> enableOrDisableSubControls());
        pbsFollowEvolutionsCheckBox.addActionListener(e -> enableOrDisableSubControls());
        pbsStandardizeEXPCurvesCheckBox.addActionListener(e -> enableOrDisableSubControls());
        paUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        paRandomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        peUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        peRandomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        peRandomEveryLevelRadioButton.addActionListener(e -> enableOrDisableSubControls());
        peAllowAltFormesCheckBox.addActionListener(e -> enableOrDisableSubControls());
        spUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        spCustomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        spRandomCompletelyRadioButton.addActionListener(e -> enableOrDisableSubControls());
        spRandomTwoEvosRadioButton.addActionListener(e -> enableOrDisableSubControls());
        spRandomBasicRadioButton.addActionListener(e -> enableOrDisableSubControls());
        spTypeNoneRadioButton.addActionListener(e -> enableOrDisableSubControls());
        spTypeFwgRadioButton.addActionListener(e -> enableOrDisableSubControls());
        spTypeTriangleRadioButton.addActionListener(e -> enableOrDisableSubControls());
        spTypeSingleRadioButton.addActionListener(e -> enableOrDisableSubControls());
        stpUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        stpSwapLegendariesSwapStandardsRadioButton.addActionListener(e -> enableOrDisableSubControls());
        stpRandomCompletelyRadioButton.addActionListener(e -> enableOrDisableSubControls());
        stpRandomSimilarStrengthRadioButton.addActionListener(e -> enableOrDisableSubControls());
        stpPercentageLevelModifierCheckBox.addActionListener(e -> enableOrDisableSubControls());
        igtUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        igtRandomizeGivenPokemonOnlyRadioButton.addActionListener(e -> enableOrDisableSubControls());
        igtRandomizeBothRequestedGivenRadioButton.addActionListener(e -> enableOrDisableSubControls());
        pmsUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        pmsRandomPreferringSameTypeRadioButton.addActionListener(e -> enableOrDisableSubControls());
        pmsRandomCompletelyRadioButton.addActionListener(e -> enableOrDisableSubControls());
        pmsMetronomeOnlyModeRadioButton.addActionListener(e -> enableOrDisableSubControls());
        pmsGuaranteedLevel1MovesCheckBox.addActionListener(e -> enableOrDisableSubControls());
        pmsForceGoodDamagingCheckBox.addActionListener(e -> enableOrDisableSubControls());
        tpForceFullyEvolvedAtCheckBox.addActionListener(e -> enableOrDisableSubControls());
        tpPercentageLevelModifierCheckBox.addActionListener(e -> enableOrDisableSubControls());
        tpEliteFourUniquePokemonCheckBox.addActionListener(e -> enableOrDisableSubControls());
        wpUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        wpRandomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        wpArea1To1RadioButton.addActionListener(e -> enableOrDisableSubControls());
        wpLocation1To1RadioButton.addActionListener(e -> enableOrDisableSubControls());
        wpGlobal1To1RadioButton.addActionListener(e -> enableOrDisableSubControls());
        wpTRNoneRadioButton.addActionListener(e -> enableOrDisableSubControls());
        wpTRThemedAreasRadioButton.addActionListener(e -> enableOrDisableSubControls());
        wpTRKeepPrimaryRadioButton.addActionListener(e -> enableOrDisableSubControls());
        wpSimilarStrengthCheckBox.addActionListener(e -> enableOrDisableSubControls());
        wpSetMinimumCatchRateCheckBox.addActionListener(e -> enableOrDisableSubControls());
        wpRandomizeHeldItemsCheckBox.addActionListener(e -> enableOrDisableSubControls());
        wpPercentageLevelModifierCheckBox.addActionListener(e -> enableOrDisableSubControls());
        tmUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        tmRandomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        tmForceGoodDamagingCheckBox.addActionListener(e -> enableOrDisableSubControls());
        thcUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        thcRandomPreferSameTypeRadioButton.addActionListener(e -> enableOrDisableSubControls());
        thcRandomCompletelyRadioButton.addActionListener(e -> enableOrDisableSubControls());
        thcFullCompatibilityRadioButton.addActionListener(e -> enableOrDisableSubControls());
        mtUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        mtRandomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        mtForceGoodDamagingCheckBox.addActionListener(e -> enableOrDisableSubControls());
        mtcUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        mtcRandomPreferSameTypeRadioButton.addActionListener(e -> enableOrDisableSubControls());
        mtcRandomCompletelyRadioButton.addActionListener(e -> enableOrDisableSubControls());
        mtcFullCompatibilityRadioButton.addActionListener(e -> enableOrDisableSubControls());
        fiUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        fiShuffleRadioButton.addActionListener(e -> enableOrDisableSubControls());
        fiRandomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        fiRandomEvenDistributionRadioButton.addActionListener(e -> enableOrDisableSubControls());
        shUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        shShuffleRadioButton.addActionListener(e -> enableOrDisableSubControls());
        shRandomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        puUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        puRandomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        websiteLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Desktop desktop = java.awt.Desktop.getDesktop();
                try {
                    desktop.browse(new URI(SysConstants.WEBSITE_URL_ZX));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        wikiLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Desktop desktop = java.awt.Desktop.getDesktop();
                try {
                    desktop.browse(new URI(SysConstants.WIKI_URL_ZX));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        randomizeSaveButton.addActionListener(e -> saveROM());
        premadeSeedButton.addActionListener(e -> presetLoader());
        loadSettingsButton.addActionListener(e -> loadQS());
        saveSettingsButton.addActionListener(e -> saveQS());
        settingsButton.addActionListener(e -> settingsMenu.show(settingsButton,0,settingsButton.getHeight()));
        customNamesEditorMenuItem.addActionListener(e -> new CustomNamesEditorDialog(frame));
        applyGameUpdateMenuItem.addActionListener(e -> applyGameUpdateMenuItemActionPerformed());
        removeGameUpdateMenuItem.addActionListener(e -> removeGameUpdateMenuItemActionPerformed());
        loadGetSettingsMenuItem.addActionListener(e -> loadGetSettingsMenuItemActionPerformed());
        keepOrUnloadGameAfterRandomizingMenuItem.addActionListener(e -> keepOrUnloadGameAfterRandomizingMenuItemActionPerformed());
        limitPokemonButton.addActionListener(e -> {
            GenerationLimitDialog gld = new GenerationLimitDialog(frame, currentRestrictions,
                    romHandler.generationOfPokemon(), romHandler.forceSwapStaticMegaEvos());
            if (gld.pressedOK()) {
                currentRestrictions = gld.getChoice();
                if (currentRestrictions != null && !currentRestrictions.allowTrainerSwapMegaEvolvables(
                        romHandler.forceSwapStaticMegaEvos(), isTrainerSetting(TRAINER_TYPE_THEMED) ||
                                isTrainerSetting(TRAINER_TYPE_THEMED_ELITE4_GYMS))) {
                    tpSwapMegaEvosCheckBox.setEnabled(false);
                    tpSwapMegaEvosCheckBox.setSelected(false);
                }
            }
        });
        limitPokemonCheckBox.addActionListener(e -> enableOrDisableSubControls());
        tpAllowAlternateFormesCheckBox.addActionListener(e -> enableOrDisableSubControls());
        tpBossTrainersCheckBox.addActionListener(e -> enableOrDisableSubControls());
        tpImportantTrainersCheckBox.addActionListener(e -> enableOrDisableSubControls());
        tpRegularTrainersCheckBox.addActionListener(e -> enableOrDisableSubControls());
        tpBossTrainersItemsCheckBox.addActionListener(e -> enableOrDisableSubControls());
        tpImportantTrainersItemsCheckBox.addActionListener(e -> enableOrDisableSubControls());
        tpRegularTrainersItemsCheckBox.addActionListener(e -> enableOrDisableSubControls());
        totpUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        totpRandomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        totpRandomSimilarStrengthRadioButton.addActionListener(e -> enableOrDisableSubControls());
        totpAllyUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        totpAllyRandomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        totpAllyRandomSimilarStrengthRadioButton.addActionListener(e -> enableOrDisableSubControls());
        totpPercentageLevelModifierCheckBox.addActionListener(e -> enableOrDisableSubControls());
        pbsUpdateBaseStatsCheckBox.addActionListener(e -> enableOrDisableSubControls());
        mdUpdateMovesCheckBox.addActionListener(e -> enableOrDisableSubControls());
        frame.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {

            }

            @Override
            public void componentMoved(ComponentEvent e) {

            }

            @Override
            public void componentShown(ComponentEvent e) {
                showInitialPopup();
            }

            @Override
            public void componentHidden(ComponentEvent e) {

            }
        });
        ptUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        ptRandomFollowEvolutionsRadioButton.addActionListener(e -> enableOrDisableSubControls());
        ptRandomCompletelyRadioButton.addActionListener(e -> enableOrDisableSubControls());
        spRandomizeStarterHeldItemsCheckBox.addActionListener(e -> enableOrDisableSubControls());
        tmLevelupMoveSanityCheckBox.addActionListener(e -> enableOrDisableSubControls());
        mtLevelupMoveSanityCheckBox.addActionListener(e -> enableOrDisableSubControls());
        noIrregularAltFormesCheckBox.addActionListener(e -> enableOrDisableSubControls());
        ptIsDualTypeCheckBox.addActionListener(e -> enableOrDisableSubControls());
        teUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        teRandomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        teRandomBalancedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        teKeepTypeIdentitiesRadioButton.addActionListener(e -> enableOrDisableSubControls());
        teInverseRadioButton.addActionListener(e -> enableOrDisableSubControls());
        ppalUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        ppalRandomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        cpgUnchangedRadioButton.addActionListener(e -> enableOrDisableSubControls());
        cpgCustomRadioButton.addActionListener(e -> enableOrDisableSubControls());
        cpgComboBox.addItemListener(e -> {
            GraphicsPack cpg = (GraphicsPack) e.getItem();
            cpgCustomInfo.setGraphicsPack(cpg);
        });
        cpgRandomButton.addActionListener(e -> cpgComboBox.setSelectedIndex(RND.nextInt(cpgComboBox.getItemCount())));
        tpComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                enableOrDisableSubControls();
            }
        });
        batchRandomizationMenuItem.addActionListener(e -> batchRandomizationSettingsDialog());
    }

    private void showInitialPopup() {
        if (!usedLauncher) {
            String message = bundle.getString("GUI.pleaseUseTheLauncher");
            Object[] messages = {message};
            JOptionPane.showMessageDialog(frame, messages);
        }
        if (initialPopup) {
            String message = String.format(bundle.getString("GUI.firstStart"),Version.VERSION_STRING);
            JLabel label = new JLabel("<html><a href=\"https://github.com/Ajarmar/universal-pokemon-randomizer-zx/wiki/Important-Information\">Checking out the \"Important Information\" page on the Wiki is highly recommended.</a>");
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Desktop desktop = java.awt.Desktop.getDesktop();
                    try {
                        desktop.browse(new URI("https://github.com/Ajarmar/universal-pokemon-randomizer-zx/wiki/Important-Information"));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            label.setCursor(new java.awt.Cursor(Cursor.HAND_CURSOR));
            Object[] messages = {message,label};
            JOptionPane.showMessageDialog(frame, messages);
            initialPopup = false;
            attemptWriteConfig();
        }
    }

    private void showInvalidRomPopup() {
        if (showInvalidRomPopup) {
            String message = String.format(bundle.getString("GUI.invalidRomMessage"));
            JLabel label = new JLabel("<html><b>Randomizing ROM hacks or bad ROM dumps is not supported and may cause issues.</b>");
            JCheckBox checkbox = new JCheckBox("Don't show this again");
            Object[] messages = {message, label, checkbox};
            Object[] options = {"OK"};
            JOptionPane.showOptionDialog(frame,
                    messages,
                    "Invalid ROM detected",
                    JOptionPane.OK_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    null);
            showInvalidRomPopup = !checkbox.isSelected();
            attemptWriteConfig();
        }
    }

    private void initFileChooserDirectories() {
        romOpenChooser.setCurrentDirectory(new File(SysConstants.ROOT_PATH));
        romSaveChooser.setCurrentDirectory(new File(SysConstants.ROOT_PATH));
        if (new File(SysConstants.ROOT_PATH + "settings/").exists()) {
            qsOpenChooser.setCurrentDirectory(new File(SysConstants.ROOT_PATH + "settings/"));
            qsSaveChooser.setCurrentDirectory(new File(SysConstants.ROOT_PATH + "settings/"));
            qsUpdateChooser.setCurrentDirectory(new File(SysConstants.ROOT_PATH + "settings/"));
        } else {
            qsOpenChooser.setCurrentDirectory(new File(SysConstants.ROOT_PATH));
            qsSaveChooser.setCurrentDirectory(new File(SysConstants.ROOT_PATH));
            qsUpdateChooser.setCurrentDirectory(new File(SysConstants.ROOT_PATH));
        }
    }

    private void initExplicit() {

        versionLabel.setText(String.format(bundle.getString("GUI.versionLabel.text"), Version.VERSION_STRING));
        mtNoExistLabel.setVisible(false);
        mtNoneAvailableLabel.setVisible(false);
        ppalNotExistLabel.setVisible(false);
        ppalPartiallyImplementedLabel.setVisible(false);
        cpgNotExistLabel.setVisible(false);
        baseTweaksPanel.add(liveTweaksPanel);
        liveTweaksPanel.setVisible(false);
        websiteLinkLabel.setCursor(new java.awt.Cursor(Cursor.HAND_CURSOR));
        wikiLinkLabel.setCursor(new java.awt.Cursor(Cursor.HAND_CURSOR));

        romOpenChooser.setFileFilter(new ROMFilter());

        romSaveChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        romSaveChooser.setFileFilter(new ROMFilter());

        qsOpenChooser.setFileFilter(new QSFileFilter());

        qsSaveChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        qsSaveChooser.setFileFilter(new QSFileFilter());

        qsUpdateChooser.setFileFilter(new QSFileFilter());

        settingsMenu = new JPopupMenu();

        SpinnerModel bossTrainerModel = new SpinnerNumberModel(
                1,
                1,
                5,
                1
        );
        SpinnerModel importantTrainerModel = new SpinnerNumberModel(
                1,
                1,
                5,
                1
        );
        SpinnerModel regularTrainerModel = new SpinnerNumberModel(
                1,
                1,
                5,
                1
        );

        SpinnerModel eliteFourUniquePokemonModel = new SpinnerNumberModel(
                1,
                1,
                2,
                1
        );

        List<String> keys = new ArrayList<>(bundle.keySet());
        Collections.sort(keys);
        for (String k: keys) {
            if (k.matches("^GUI\\.tpMain.*\\.text$")) {
                trainerSettings.add(bundle.getString(k));
                trainerSettingToolTips.add(k.replace("text","toolTipText"));
            }
        }

        tpBossTrainersSpinner.setModel(bossTrainerModel);
        tpImportantTrainersSpinner.setModel(importantTrainerModel);
        tpRegularTrainersSpinner.setModel(regularTrainerModel);
        tpEliteFourUniquePokemonSpinner.setModel(eliteFourUniquePokemonModel);

        customNamesEditorMenuItem = new JMenuItem();
        customNamesEditorMenuItem.setText(bundle.getString("GUI.customNamesEditorMenuItem.text"));
        settingsMenu.add(customNamesEditorMenuItem);

        loadGetSettingsMenuItem = new JMenuItem();
        loadGetSettingsMenuItem.setText(bundle.getString("GUI.loadGetSettingsMenuItem.text"));
        settingsMenu.add(loadGetSettingsMenuItem);

        applyGameUpdateMenuItem = new JMenuItem();
        applyGameUpdateMenuItem.setText(bundle.getString("GUI.applyGameUpdateMenuItem.text"));
        settingsMenu.add(applyGameUpdateMenuItem);

        removeGameUpdateMenuItem = new JMenuItem();
        removeGameUpdateMenuItem.setText(bundle.getString("GUI.removeGameUpdateMenuItem.text"));
        settingsMenu.add(removeGameUpdateMenuItem);

        keepOrUnloadGameAfterRandomizingMenuItem = new JMenuItem();
        if (this.unloadGameOnSuccess) {
            keepOrUnloadGameAfterRandomizingMenuItem.setText(bundle.getString("GUI.keepGameLoadedAfterRandomizingMenuItem.text"));
        } else {
            keepOrUnloadGameAfterRandomizingMenuItem.setText(bundle.getString("GUI.unloadGameAfterRandomizingMenuItem.text"));
        }
        settingsMenu.add(keepOrUnloadGameAfterRandomizingMenuItem);

        batchRandomizationMenuItem = new JMenuItem();
        batchRandomizationMenuItem.setText(bundle.getString("GUI.batchRandomizationMenuItem.text"));
        settingsMenu.add(batchRandomizationMenuItem);
    }

    private void loadROM() {
        romOpenChooser.setSelectedFile(null);
        int returnVal = romOpenChooser.showOpenDialog(mainPanel);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File fh = romOpenChooser.getSelectedFile();
            try {
                Utils.validateRomFile(fh);
            } catch (Utils.InvalidROMException e) {
                switch (e.getType()) {
                    case LENGTH:
                        JOptionPane.showMessageDialog(mainPanel,
                                String.format(bundle.getString("GUI.tooShortToBeARom"), fh.getName()));
                        return;
                    case ZIP_FILE:
                        JOptionPane.showMessageDialog(mainPanel,
                                String.format(bundle.getString("GUI.openedZIPfile"), fh.getName()));
                        return;
                    case RAR_FILE:
                        JOptionPane.showMessageDialog(mainPanel,
                                String.format(bundle.getString("GUI.openedRARfile"), fh.getName()));
                        return;
                    case IPS_FILE:
                        JOptionPane.showMessageDialog(mainPanel,
                                String.format(bundle.getString("GUI.openedIPSfile"), fh.getName()));
                        return;
                    case UNREADABLE:
                        JOptionPane.showMessageDialog(mainPanel,
                                String.format(bundle.getString("GUI.unreadableRom"), fh.getName()));
                        return;
                }
            }

            for (RomHandler.Factory rhf : checkHandlers) {
                if (rhf.isLoadable(fh.getAbsolutePath())) {
                    this.romHandler = rhf.create();
                    if (!usedLauncher && this.romHandler instanceof Abstract3DSRomHandler) {
                        String message = bundle.getString("GUI.pleaseUseTheLauncher");
                        Object[] messages = {message};
                        JOptionPane.showMessageDialog(frame, messages);
                        this.romHandler = null;
                        return;
                    }
                    opDialog = new OperationDialog(bundle.getString("GUI.loadingText"), frame, true);
                    Thread t = new Thread(() -> {
                        boolean romLoaded = false;
                        SwingUtilities.invokeLater(() -> opDialog.setVisible(true));
                        try {
                            this.romHandler.loadRom(fh.getAbsolutePath());
                            if (gameUpdates.containsKey(this.romHandler.getROMCode())) {
                                this.romHandler.loadGameUpdate(gameUpdates.get(this.romHandler.getROMCode()));
                            }
                            romLoaded = true;
                        } catch (EncryptedROMException ex) {
                            JOptionPane.showMessageDialog(mainPanel,
                                    String.format(bundle.getString("GUI.encryptedRom"), fh.getAbsolutePath()));
                        } catch (Exception ex) {
                            attemptToLogException(ex, "GUI.loadFailed", "GUI.loadFailedNoLog", null, null);
                        }
                        final boolean loadSuccess = romLoaded;
                        SwingUtilities.invokeLater(() -> {
                            this.opDialog.setVisible(false);
                            this.initialState();
                            if (loadSuccess) {
                                this.romLoaded();
                            }
                        });
                    });
                    t.start();

                    return;
                }
            }
            JOptionPane.showMessageDialog(mainPanel,
                    String.format(bundle.getString("GUI.unsupportedRom"), fh.getName()));
        }
    }

    private void saveROM() {
        if (romHandler == null) {
            return; // none loaded
        }
        if (raceModeCheckBox.isSelected() && batchRandomizationSettings.isBatchRandomizationEnabled()) {
            JOptionPane.showMessageDialog(frame, bundle.getString("GUI.batchRandomizationRequirements"));
            return;
        }
        if (raceModeCheckBox.isSelected() && isTrainerSetting(TRAINER_UNCHANGED) && wpUnchangedRadioButton.isSelected()) {
            JOptionPane.showMessageDialog(frame, bundle.getString("GUI.raceModeRequirements"));
            return;
        }
        if (limitPokemonCheckBox.isSelected()
                && (this.currentRestrictions == null || this.currentRestrictions.nothingSelected())) {
            JOptionPane.showMessageDialog(frame, bundle.getString("GUI.pokeLimitNotChosen"));
            return;
        }
        SaveType outputType = askForSaveType();
        romSaveChooser.setSelectedFile(null);
        boolean allowed = false;
        File fh = null;
        if (batchRandomizationSettings.isBatchRandomizationEnabled() && outputType != SaveType.INVALID) {
            allowed = true;
        }
        else if (outputType == SaveType.FILE) {
            romSaveChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int returnVal = romSaveChooser.showSaveDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                fh = romSaveChooser.getSelectedFile();
                // Fix or add extension
                List<String> extensions = new ArrayList<>(Arrays.asList("sgb", "gbc", "gba", "nds", "cxi"));
                extensions.remove(this.romHandler.getDefaultExtension());
                fh = FileFunctions.fixFilename(fh, this.romHandler.getDefaultExtension(), extensions);
                allowed = true;
                if (this.romHandler instanceof AbstractDSRomHandler || this.romHandler instanceof Abstract3DSRomHandler) {
                    String currentFN = this.romHandler.loadedFilename();
                    if (currentFN.equals(fh.getAbsolutePath())) {
                        JOptionPane.showMessageDialog(frame, bundle.getString("GUI.cantOverwriteDS"));
                        allowed = false;
                    }
                }
            }
        } else if (outputType == SaveType.DIRECTORY) {
            romSaveChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = romSaveChooser.showSaveDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                fh = romSaveChooser.getSelectedFile();
                allowed = true;
            }
        }

        if (allowed && fh != null) {
            saveRandomizedRom(outputType, fh);
        } else if (allowed && batchRandomizationSettings.isBatchRandomizationEnabled()) {
            int numberOfRandomizedROMs = batchRandomizationSettings.getNumberOfRandomizedROMs();
            int startingIndex = batchRandomizationSettings.getStartingIndex();
            int endingIndex = startingIndex + numberOfRandomizedROMs;
            final String progressTemplate = bundle.getString("GUI.batchRandomizationProgress");
            OperationDialog batchProgressDialog = new OperationDialog(String.format(progressTemplate, 0, numberOfRandomizedROMs), frame, true);
            SwingWorker swingWorker = new SwingWorker<Void, Void>() {
                int i;

                @Override
                protected Void doInBackground() {
                    frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    SwingUtilities.invokeLater(() -> batchProgressDialog.setVisible(true));
                    for (i = startingIndex; i < endingIndex; i++) {
                        String fileName = batchRandomizationSettings.getOutputDirectory() +
                                File.separator +
                                batchRandomizationSettings.getFileNamePrefix() +
                                i;
                        if (outputType == SaveType.FILE) {
                            fileName += '.' + romHandler.getDefaultExtension();
                        }
                        File rom = new File(fileName);
                        if (outputType == SaveType.DIRECTORY) {
                            rom.mkdirs();
                        }
                        int currentRomNumber = i - startingIndex + 1;

                        SwingUtilities.invokeLater(
                                () -> batchProgressDialog.setLoadingLabelText(String.format(progressTemplate,
                                        currentRomNumber,
                                        numberOfRandomizedROMs))
                        );
                        saveRandomizedRom(outputType, rom);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    super.done();
                    if (batchRandomizationSettings.shouldAutoAdvanceStartingIndex()) {
                        batchRandomizationSettings.setStartingIndex(i);
                        attemptWriteConfig();
                    }
                    SwingUtilities.invokeLater(() -> batchProgressDialog.setVisible(false));
                    JOptionPane.showMessageDialog(frame, bundle.getString("GUI.randomizationDone"));
                    if (unloadGameOnSuccess) {
                        romHandler = null;
                        initialState();
                    } else {
                        reinitializeRomHandler(false);
                    }
                    frame.setCursor(null);
                }
            };
            swingWorker.execute();
        }
    }

    private void saveRandomizedRom(SaveType outputType, File fh) {
        long seed = SeedPicker.pickSeed();
        presetMode = false;

        try {
            CustomNamesSet cns = FileFunctions.getCustomNames();
            performRandomization(fh.getAbsolutePath(), seed, cns, outputType == SaveType.DIRECTORY);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, bundle.getString("GUI.cantLoadCustomNames"));
        }
    }

    private void loadQS() {
        if (this.romHandler == null) {
            return;
        }
        qsOpenChooser.setSelectedFile(null);
        int returnVal = qsOpenChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File fh = qsOpenChooser.getSelectedFile();
            try {
                FileInputStream fis = new FileInputStream(fh);
                Settings settings = Settings.read(fis);
                fis.close();

                SwingUtilities.invokeLater(() -> {
                    // load settings
                    initialState();
                    romLoaded();
                    Settings.TweakForROMFeedback feedback = settings.tweakForRom(this.romHandler);
                    if (feedback.isChangedStarter() && settings.getStartersMod() == Settings.StartersMod.CUSTOM) {
                        JOptionPane.showMessageDialog(frame, bundle.getString("GUI.starterUnavailable"));
                    }
                    this.restoreStateFromSettings(settings);

                    if (settings.isUpdatedFromOldVersion()) {
                        // show a warning dialog, but load it
                        JOptionPane.showMessageDialog(frame, bundle.getString("GUI.settingsFileOlder"));
                    }

                    JOptionPane.showMessageDialog(frame,
                            String.format(bundle.getString("GUI.settingsLoaded"), fh.getName()));
                });
            } catch (UnsupportedOperationException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, ex.getMessage());
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, bundle.getString("GUI.invalidSettingsFile"));
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, bundle.getString("GUI.settingsLoadFailed"));
            }
        }
    }

    private void saveQS() {
        if (this.romHandler == null) {
            return;
        }
        qsSaveChooser.setSelectedFile(null);
        int returnVal = qsSaveChooser.showSaveDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File fh = qsSaveChooser.getSelectedFile();
            // Fix or add extension
            fh = FileFunctions.fixFilename(fh, "rnqs");
            // Save now?
            try {
                FileOutputStream fos = new FileOutputStream(fh);
                getCurrentSettings().write(fos);
                fos.close();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, bundle.getString("GUI.settingsSaveFailed"));
            }
        }
    }

    private void performRandomization(final String filename, final long seed, CustomNamesSet customNames, boolean saveAsDirectory) {
        final Settings settings = createSettingsFromState(customNames);
        final boolean raceMode = settings.isRaceMode();
        final boolean batchRandomization = batchRandomizationSettings.isBatchRandomizationEnabled() && !presetMode;
        // Setup verbose log
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream log;
        log = new PrintStream(baos, false, StandardCharsets.UTF_8);

        final PrintStream verboseLog = log;

        try {
            final AtomicInteger finishedCV = new AtomicInteger(0);
            opDialog = new OperationDialog(bundle.getString("GUI.savingText"), frame, true);
            Thread t = new Thread(() -> {
                SwingUtilities.invokeLater(() -> opDialog.setVisible(!batchRandomization));
                boolean succeededSave = false;
                try {
                    finishedCV.set(new GameRandomizer(settings, romHandler, bundle, saveAsDirectory).randomize(filename,
                            verboseLog, seed));
                    succeededSave = true;
                } catch (RandomizationException ex) {
                    attemptToLogException(ex, "GUI.saveFailedMessage",
                            "GUI.saveFailedMessageNoLog", true, settings.toString(), Long.toString(seed));
                    if (verboseLog != null) {
                        verboseLog.close();
                    }
                } catch (CannotWriteToLocationException ex) {
                    JOptionPane.showMessageDialog(mainPanel, String.format(bundle.getString("GUI.cannotWriteToLocation"), filename));
                    if (verboseLog != null) {
                        verboseLog.close();
                    }
                } catch (Exception ex) {
                    attemptToLogException(ex, "GUI.saveFailedIO", "GUI.saveFailedIONoLog", settings.toString(), Long.toString(seed));
                    if (verboseLog != null) {
                        verboseLog.close();
                    }
                }
                if (succeededSave) {
                    SwingUtilities.invokeLater(() -> {
                        opDialog.setVisible(false);
                        // Log?
                        verboseLog.close();
                        byte[] out = baos.toByteArray();

                        if (raceMode) {
                            JOptionPane.showMessageDialog(frame,
                                    String.format(bundle.getString("GUI.raceModeCheckValuePopup"),
                                            finishedCV.get()));
                        } else if (batchRandomization && batchRandomizationSettings.shouldGenerateLogFile()) {
                            try {
                                saveLogFile(filename, out);
                            } catch (IOException e) {
                                JOptionPane.showMessageDialog(frame,
                                        bundle.getString("GUI.logSaveFailed"));
                                return;
                            }
                        } else if (!batchRandomization) {
                            int response = JOptionPane.showConfirmDialog(frame,
                                    bundle.getString("GUI.saveLogDialog.text"),
                                    bundle.getString("GUI.saveLogDialog.title"),
                                    JOptionPane.YES_NO_OPTION);
                            if (response == JOptionPane.YES_OPTION) {
                                try {
                                    saveLogFile(filename, out);
                                } catch (IOException e) {
                                    JOptionPane.showMessageDialog(frame,
                                            bundle.getString("GUI.logSaveFailed"));
                                    return;
                                }
                                JOptionPane.showMessageDialog(frame,
                                        String.format(bundle.getString("GUI.logSaved"), filename));
                            }
                        }
                        if (presetMode) {
                            JOptionPane.showMessageDialog(frame,
                                    bundle.getString("GUI.randomizationDone"));
                            // Done
                            if (this.unloadGameOnSuccess) {
                                romHandler = null;
                                initialState();
                            } else {
                                reinitializeRomHandler(false);
                            }
                        } else if (!batchRandomization) {
                            // Compile a config string
                            try {
                                String configString = getCurrentSettings().toString();
                                // Show the preset maker
                                new PresetMakeDialog(frame, seed, configString);
                            } catch (IOException ex) {
                                JOptionPane.showMessageDialog(frame,
                                        bundle.getString("GUI.cantLoadCustomNames"));
                            }

                            // Done
                            if (this.unloadGameOnSuccess) {
                                romHandler = null;
                                initialState();
                            } else {
                                reinitializeRomHandler(false);
                            }
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        opDialog.setVisible(false);
                        romHandler = null;
                        initialState();
                    });
                }
            });
            t.start();
            if (batchRandomization) {
                t.join();
                reinitializeRomHandler(true);
            }
        } catch (Exception ex) {
            attemptToLogException(ex, "GUI.saveFailed", "GUI.saveFailedNoLog", settings.toString(), Long.toString(seed));
            if (verboseLog != null) {
                verboseLog.close();
            }
        }
    }

    private void saveLogFile(String filename, byte[] out) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename + ".log");
        fos.write(0xEF);
        fos.write(0xBB);
        fos.write(0xBF);
        fos.write(out);
        fos.close();
    }

    private void presetLoader() {
        PresetLoadDialog pld = new PresetLoadDialog(this,frame);
        if (pld.isCompleted()) {
            // Apply it
            long seed = pld.getSeed();
            String config = pld.getConfigString();
            this.romHandler = pld.getROM();
            if (gameUpdates.containsKey(this.romHandler.getROMCode())) {
                this.romHandler.loadGameUpdate(gameUpdates.get(this.romHandler.getROMCode()));
            }
            this.romLoaded();
            Settings settings;
            try {
                settings = Settings.fromString(config);
                settings.tweakForRom(this.romHandler);
                this.restoreStateFromSettings(settings);
            } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                // settings load failed
                e.printStackTrace();
                this.romHandler = null;
                initialState();
            }
            SaveType outputType = askForSaveType();
            romSaveChooser.setSelectedFile(null);
            boolean allowed = false;
            File fh = null;
            if (outputType == SaveType.FILE) {
                romSaveChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                int returnVal = romSaveChooser.showSaveDialog(frame);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    fh = romSaveChooser.getSelectedFile();
                    // Fix or add extension
                    List<String> extensions = new ArrayList<>(Arrays.asList("sgb", "gbc", "gba", "nds", "cxi"));
                    extensions.remove(this.romHandler.getDefaultExtension());
                    fh = FileFunctions.fixFilename(fh, this.romHandler.getDefaultExtension(), extensions);
                    allowed = true;
                    if (this.romHandler instanceof AbstractDSRomHandler || this.romHandler instanceof Abstract3DSRomHandler) {
                        String currentFN = this.romHandler.loadedFilename();
                        if (currentFN.equals(fh.getAbsolutePath())) {
                            JOptionPane.showMessageDialog(frame, bundle.getString("GUI.cantOverwriteDS"));
                            allowed = false;
                        }
                    }
                } else {
                    this.romHandler = null;
                    initialState();
                }
            } else if (outputType == SaveType.DIRECTORY) {
                romSaveChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = romSaveChooser.showSaveDialog(frame);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    fh = romSaveChooser.getSelectedFile();
                    allowed = true;
                } else {
                    this.romHandler = null;
                    initialState();
                }
            }

            if (allowed && fh != null) {
                // Apply the seed we were given
                presetMode = true;
                performRandomization(fh.getAbsolutePath(), seed, pld.getCustomNames(), outputType == SaveType.DIRECTORY);
            }
        }

    }


    private enum SaveType {
        FILE, DIRECTORY, INVALID
    }

    private SaveType askForSaveType() {
        SaveType saveType = SaveType.FILE;
        if (romHandler.hasGameUpdateLoaded()) {
            String text = bundle.getString("GUI.savingWithGameUpdate");
            String url = "https://github.com/Ajarmar/universal-pokemon-randomizer-zx/wiki/Randomizing-the-3DS-games#managing-game-updates";
            showMessageDialogWithLink(text, url);
            saveType = SaveType.DIRECTORY;
        } else if (romHandler.generationOfPokemon() == 6 || romHandler.generationOfPokemon() == 7) {
            Object[] options3DS = {"CXI", "LayeredFS"};
            String question = "Would you like to output your 3DS game as a CXI file or as a LayeredFS directory?";
            JLabel label = new JLabel("<html><a href=\"https://github.com/Ajarmar/universal-pokemon-randomizer-zx/wiki/Randomizing-the-3DS-games#changes-to-saving-a-rom-when-working-with-3ds-games\">For more information, click here.</a>");
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Desktop desktop = java.awt.Desktop.getDesktop();
                    try {
                        desktop.browse(new URI("https://github.com/Ajarmar/universal-pokemon-randomizer-zx/wiki/Randomizing-the-3DS-games#changes-to-saving-a-rom-when-working-with-3ds-games"));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            label.setCursor(new java.awt.Cursor(Cursor.HAND_CURSOR));
            Object[] messages = {question,label};
            int returnVal3DS = JOptionPane.showOptionDialog(frame,
                    messages,
                    "3DS Output Choice",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options3DS,
                    null);
            if (returnVal3DS < 0) {
                saveType = SaveType.INVALID;
            } else {
                saveType = SaveType.values()[returnVal3DS];
            }
        }
        return saveType;
    }

    private void applyGameUpdateMenuItemActionPerformed() {

        if (romHandler == null) return;

        gameUpdateChooser.setSelectedFile(null);
        gameUpdateChooser.setFileFilter(new GameUpdateFilter());
        int returnVal = gameUpdateChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File fh = gameUpdateChooser.getSelectedFile();

            // On the 3DS, the update has the same title ID as the base game, save for the 8th character,
            // which is 'E' instead of '0'. We can use this to detect if the update matches the game.
            String actualUpdateTitleId = Abstract3DSRomHandler.getTitleIdFromFile(fh.getAbsolutePath());
            if (actualUpdateTitleId == null) {
                // Error: couldn't find a title ID in the update
                JOptionPane.showMessageDialog(frame, String.format(bundle.getString("GUI.invalidGameUpdate"), fh.getName()));
                return;
            }
            Abstract3DSRomHandler ctrRomHandler = (Abstract3DSRomHandler) romHandler;
            String baseGameTitleId = ctrRomHandler.getTitleIdFromLoadedROM();
            char[] baseGameTitleIdChars = baseGameTitleId.toCharArray();
            baseGameTitleIdChars[7] = 'E';
            String expectedUpdateTitleId = String.valueOf(baseGameTitleIdChars);
            if (actualUpdateTitleId.equals(expectedUpdateTitleId)) {
                try {
                    romHandler.loadGameUpdate(fh.getAbsolutePath());
                } catch (EncryptedROMException ex) {
                    JOptionPane.showMessageDialog(mainPanel,
                            String.format(bundle.getString("GUI.encryptedRom"), fh.getAbsolutePath()));
                    return;
                }
                gameUpdates.put(romHandler.getROMCode(), fh.getAbsolutePath());
                attemptWriteConfig();
                removeGameUpdateMenuItem.setVisible(true);
                setRomNameLabel();
                String text = String.format(bundle.getString("GUI.gameUpdateApplied"), romHandler.getROMName());
                String url = "https://github.com/Ajarmar/universal-pokemon-randomizer-zx/wiki/Randomizing-the-3DS-games#3ds-game-updates";
                showMessageDialogWithLink(text, url);
            } else {
                // Error: update is not for the correct game
                JOptionPane.showMessageDialog(frame, String.format(bundle.getString("GUI.nonMatchingGameUpdate"), fh.getName(), romHandler.getROMName()));
            }
        }
    }

    private void removeGameUpdateMenuItemActionPerformed() {

        if (romHandler == null) return;

        gameUpdates.remove(romHandler.getROMCode());
        attemptWriteConfig();
        romHandler.removeGameUpdate();
        removeGameUpdateMenuItem.setVisible(false);
        setRomNameLabel();
    }

    private void loadGetSettingsMenuItemActionPerformed() {

        if (romHandler == null) return;

        String currentSettingsString = "Current Settings String:";
        JTextField currentSettingsStringField = new JTextField();
        currentSettingsStringField.setEditable(false);
        try {
            String theSettingsString = Version.VERSION + getCurrentSettings().toString();
            currentSettingsStringField.setColumns(Settings.LENGTH_OF_SETTINGS_DATA * 2);
            currentSettingsStringField.setText(theSettingsString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String loadSettingsString = "Load Settings String:";
        JTextField loadSettingsStringField = new JTextField();
        Object[] messages = {currentSettingsString,currentSettingsStringField,loadSettingsString,loadSettingsStringField};
        Object[] options = {"Load","Cancel"};
        int choice = JOptionPane.showOptionDialog(
                frame,
                messages,
                "Get/Load Settings String",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                null
        );
        if (choice == 0) {
            String configString = loadSettingsStringField.getText().trim();
            if (configString.length() > 0) {
                if (configString.length() < 3) {
                    JOptionPane.showMessageDialog(frame,bundle.getString("GUI.invalidSettingsString"));
                } else {
                    try {
                        int settingsStringVersionNumber = Integer.parseInt(configString.substring(0, 3));
                        if (settingsStringVersionNumber < Version.VERSION) {
                            JOptionPane.showMessageDialog(frame,bundle.getString("GUI.settingsStringOlder"));
                            String updatedSettingsString = new SettingsUpdater().update(settingsStringVersionNumber, configString.substring(3));
                            Settings settings = Settings.fromString(updatedSettingsString);
                            settings.tweakForRom(this.romHandler);
                            restoreStateFromSettings(settings);
                            JOptionPane.showMessageDialog(frame,bundle.getString("GUI.settingsStringLoaded"));
                        } else if (settingsStringVersionNumber > Version.VERSION) {
                            JOptionPane.showMessageDialog(frame,bundle.getString("GUI.settingsStringTooNew"));
                        } else {
                            Settings settings = Settings.fromString(configString.substring(3));
                            settings.tweakForRom(this.romHandler);
                            restoreStateFromSettings(settings);
                            JOptionPane.showMessageDialog(frame,bundle.getString("GUI.settingsStringLoaded"));
                        }
                    } catch (UnsupportedEncodingException | IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(frame,bundle.getString("GUI.invalidSettingsString"));
                    }
                }

            }
        }
    }

    private void keepOrUnloadGameAfterRandomizingMenuItemActionPerformed() {
        this.unloadGameOnSuccess = !this.unloadGameOnSuccess;
        if (this.unloadGameOnSuccess) {
            JOptionPane.showMessageDialog(frame, bundle.getString("GUI.unloadGameAfterRandomizing"));
            keepOrUnloadGameAfterRandomizingMenuItem.setText(bundle.getString("GUI.keepGameLoadedAfterRandomizingMenuItem.text"));
        } else {
            JOptionPane.showMessageDialog(frame, bundle.getString("GUI.keepGameLoadedAfterRandomizing"));
            keepOrUnloadGameAfterRandomizingMenuItem.setText(bundle.getString("GUI.unloadGameAfterRandomizingMenuItem.text"));
        }
        attemptWriteConfig();
    }

    private void showMessageDialogWithLink(String text, String url) {
        JLabel label = new JLabel("<html><a href=\"" + url + "\">For more information, click here.</a>");
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Desktop desktop = java.awt.Desktop.getDesktop();
                try {
                    desktop.browse(new URI(url));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        label.setCursor(new java.awt.Cursor(Cursor.HAND_CURSOR));
        Object[] messages = {text,label};
        JOptionPane.showMessageDialog(frame, messages);
    }

    private void batchRandomizationSettingsDialog() {
        BatchRandomizationSettingsDialog dlg = new BatchRandomizationSettingsDialog(frame, batchRandomizationSettings);
        batchRandomizationSettings = dlg.getCurrentSettings();
        attemptWriteConfig();
    }

    // This is only intended to be used with the "Keep Game Loaded After Randomizing" setting or between randomization
    // iterations when batch randomization is enabled. It assumes that the game has already been loaded once, and we just need
    // to reload the same game to reinitialize the RomHandler. Don't use this for other purposes unless you know what
    // you're doing.
    private void reinitializeRomHandler(boolean batchRandomization) {
        String currentFN = this.romHandler.loadedFilename();
        for (RomHandler.Factory rhf : checkHandlers) {
            if (rhf.isLoadable(currentFN)) {
                this.romHandler = rhf.create();
                opDialog = new OperationDialog(bundle.getString("GUI.loadingText"), frame, true);
                Thread t = new Thread(() -> {
                    SwingUtilities.invokeLater(() -> opDialog.setVisible(!batchRandomization));
                    try {
                        this.romHandler.loadRom(currentFN);
                        if (gameUpdates.containsKey(this.romHandler.getROMCode())) {
                            this.romHandler.loadGameUpdate(gameUpdates.get(this.romHandler.getROMCode()));
                        }
                    } catch (Exception ex) {
                        attemptToLogException(ex, "GUI.loadFailed", "GUI.loadFailedNoLog", null, null);
                    }
                    SwingUtilities.invokeLater(() -> {
                        this.opDialog.setVisible(false);
                    });
                });
                t.start();
                if (batchRandomization) {
                    try {
                        t.join();
                    } catch(InterruptedException ex) {
                        attemptToLogException(ex, "GUI.loadFailed", "GUI.loadFailedNoLog", null, null);
                    }
                }
                return;
            }
        }
    }

    private void restoreStateFromSettings(Settings settings) {

        limitPokemonCheckBox.setSelected(settings.isLimitPokemon());
        currentRestrictions = settings.getCurrentRestrictions();
        if (currentRestrictions != null) {
            currentRestrictions.limitToGen(romHandler.generationOfPokemon());
        }
        noIrregularAltFormesCheckBox.setSelected(settings.isBanIrregularAltFormes());
        raceModeCheckBox.setSelected(settings.isRaceMode());

        peChangeImpossibleEvosCheckBox.setSelected(settings.isChangeImpossibleEvolutions());
        mdUpdateMovesCheckBox.setSelected(settings.isUpdateMoves());
        mdUpdateComboBox.setSelectedIndex(Math.max(0,settings.getUpdateMovesToGeneration() - (romHandler.generationOfPokemon()+1)));
        tpRandomizeTrainerNamesCheckBox.setSelected(settings.isRandomizeTrainerNames());
        tpRandomizeTrainerClassNamesCheckBox.setSelected(settings.isRandomizeTrainerClassNames());
        ptIsDualTypeCheckBox.setSelected(settings.isDualTypeOnly());

        pbsRandomRadioButton.setSelected(settings.getBaseStatisticsMod() == Settings.BaseStatisticsMod.RANDOM);
        pbsShuffleRadioButton.setSelected(settings.getBaseStatisticsMod() == Settings.BaseStatisticsMod.SHUFFLE);
        pbsUnchangedRadioButton.setSelected(settings.getBaseStatisticsMod() == Settings.BaseStatisticsMod.UNCHANGED);
        pbsFollowEvolutionsCheckBox.setSelected(settings.isBaseStatsFollowEvolutions());
        pbsUpdateBaseStatsCheckBox.setSelected(settings.isUpdateBaseStats());
        pbsUpdateComboBox.setSelectedIndex(Math.max(0,settings.getUpdateBaseStatsToGeneration() - (Math.max(6,romHandler.generationOfPokemon()+1))));
        pbsStandardizeEXPCurvesCheckBox.setSelected(settings.isStandardizeEXPCurves());
        pbsLegendariesSlowRadioButton.setSelected(settings.getExpCurveMod() == Settings.ExpCurveMod.LEGENDARIES);
        pbsStrongLegendariesSlowRadioButton.setSelected(settings.getExpCurveMod() == Settings.ExpCurveMod.STRONG_LEGENDARIES);
        pbsAllMediumFastRadioButton.setSelected(settings.getExpCurveMod() == Settings.ExpCurveMod.ALL);
        ExpCurve[] expCurves = getEXPCurvesForGeneration(romHandler.generationOfPokemon());
        int index = 0;
        for (int i = 0; i < expCurves.length; i++) {
            if (expCurves[i] == settings.getSelectedEXPCurve()) {
                index = i;
            }
        }
        pbsEXPCurveComboBox.setSelectedIndex(index);
        pbsFollowMegaEvosCheckBox.setSelected(settings.isBaseStatsFollowMegaEvolutions());
        pbsAssignEvoStatsRandomlyCheckBox.setSelected(settings.isAssignEvoStatsRandomly());

        paUnchangedRadioButton.setSelected(settings.getAbilitiesMod() == Settings.AbilitiesMod.UNCHANGED);
        paRandomRadioButton.setSelected(settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE);
        paAllowWonderGuardCheckBox.setSelected(settings.isAllowWonderGuard());
        paFollowEvolutionsCheckBox.setSelected(settings.isAbilitiesFollowEvolutions());
        paTrappingAbilitiesCheckBox.setSelected(settings.isBanTrappingAbilities());
        paNegativeAbilitiesCheckBox.setSelected(settings.isBanNegativeAbilities());
        paBadAbilitiesCheckBox.setSelected(settings.isBanBadAbilities());
        paFollowMegaEvosCheckBox.setSelected(settings.isAbilitiesFollowMegaEvolutions());
        paWeighDuplicatesTogetherCheckBox.setSelected(settings.isWeighDuplicateAbilitiesTogether());
        paEnsureTwoAbilitiesCheckbox.setSelected(settings.isEnsureTwoAbilities());

        ptRandomFollowEvolutionsRadioButton.setSelected(settings.getTypesMod() == Settings.TypesMod.RANDOM_FOLLOW_EVOLUTIONS);
        ptRandomCompletelyRadioButton.setSelected(settings.getTypesMod() == Settings.TypesMod.COMPLETELY_RANDOM);
        ptUnchangedRadioButton.setSelected(settings.getTypesMod() == Settings.TypesMod.UNCHANGED);
        ptFollowMegaEvosCheckBox.setSelected(settings.isTypesFollowMegaEvolutions());
        pmsNoGameBreakingMovesCheckBox.setSelected(settings.doBlockBrokenMoves());

        peMakeEvolutionsEasierCheckBox.setSelected(settings.isMakeEvolutionsEasier());
        peRemoveTimeBasedEvolutionsCheckBox.setSelected(settings.isRemoveTimeBasedEvolutions());

        spCustomRadioButton.setSelected(settings.getStartersMod() == Settings.StartersMod.CUSTOM);
        spRandomCompletelyRadioButton.setSelected(settings.getStartersMod() == Settings.StartersMod.COMPLETELY_RANDOM);
        spUnchangedRadioButton.setSelected(settings.getStartersMod() == Settings.StartersMod.UNCHANGED);
        spRandomTwoEvosRadioButton.setSelected(settings.getStartersMod() == Settings.StartersMod.RANDOM_WITH_TWO_EVOLUTIONS);
        spRandomBasicRadioButton.setSelected(settings.getStartersMod() == Settings.StartersMod.RANDOM_BASIC);
        spTypeNoneRadioButton.setSelected(settings.getStartersTypeMod() == Settings.StartersTypeMod.NONE);
        spTypeFwgRadioButton.setSelected(settings.getStartersTypeMod() == Settings.StartersTypeMod.FIRE_WATER_GRASS);
        spTypeTriangleRadioButton.setSelected(settings.getStartersTypeMod() == Settings.StartersTypeMod.TRIANGLE);
        spTypeUniqueRadioButton.setSelected(settings.getStartersTypeMod() == Settings.StartersTypeMod.UNIQUE);
        spTypeSingleRadioButton.setSelected(settings.getStartersTypeMod() == Settings.StartersTypeMod.SINGLE_TYPE);
        if(settings.getStartersSingleType() == null) {
            spTypeSingleComboBox.setSelectedIndex(0);
        } else {
            spTypeSingleComboBox.setSelectedIndex(settings.getStartersSingleType().toInt() + 1);
        }
        spTypeNoDualCheckbox.setSelected(settings.isStartersNoDualTypes());
        spRandomizeStarterHeldItemsCheckBox.setSelected(settings.isRandomizeStartersHeldItems());
        spBanBadItemsCheckBox.setSelected(settings.isBanBadRandomStarterHeldItems());
        spAllowAltFormesCheckBox.setSelected(settings.isAllowStarterAltFormes());
        spNoLegendariesCheckBox.setSelected(settings.isStartersNoLegendaries());

        int[] customStarters = settings.getCustomStarters();
        spComboBox1.setSelectedIndex(customStarters[0]);
        spComboBox2.setSelectedIndex(customStarters[1]);
        spComboBox3.setSelectedIndex(customStarters[2]);

        peUnchangedRadioButton.setSelected(settings.getEvolutionsMod() == Settings.EvolutionsMod.UNCHANGED);
        peRandomRadioButton.setSelected(settings.getEvolutionsMod() == Settings.EvolutionsMod.RANDOM);
        peRandomEveryLevelRadioButton.setSelected(settings.getEvolutionsMod() == Settings.EvolutionsMod.RANDOM_EVERY_LEVEL);
        peSimilarStrengthCheckBox.setSelected(settings.isEvosSimilarStrength());
        peSameTypingCheckBox.setSelected(settings.isEvosSameTyping());
        peLimitEvolutionsToThreeCheckBox.setSelected(settings.isEvosMaxThreeStages());
        peForceChangeCheckBox.setSelected(settings.isEvosForceChange());
        peAllowAltFormesCheckBox.setSelected(settings.isEvosAllowAltFormes());
        peForceGrowthCheckBox.setSelected(settings.isEvosForceGrowth());
        peNoConvergenceCheckBox.setSelected(settings.isEvosNoConvergence());

        mdRandomizeMoveAccuracyCheckBox.setSelected(settings.isRandomizeMoveAccuracies());
        mdRandomizeMoveCategoryCheckBox.setSelected(settings.isRandomizeMoveCategory());
        mdRandomizeMovePowerCheckBox.setSelected(settings.isRandomizeMovePowers());
        mdRandomizeMovePPCheckBox.setSelected(settings.isRandomizeMovePPs());
        mdRandomizeMoveTypesCheckBox.setSelected(settings.isRandomizeMoveTypes());

        pmsRandomCompletelyRadioButton.setSelected(settings.getMovesetsMod() == Settings.MovesetsMod.COMPLETELY_RANDOM);
        pmsRandomPreferringSameTypeRadioButton.setSelected(settings.getMovesetsMod() == Settings.MovesetsMod.RANDOM_PREFER_SAME_TYPE);
        pmsUnchangedRadioButton.setSelected(settings.getMovesetsMod() == Settings.MovesetsMod.UNCHANGED);
        pmsMetronomeOnlyModeRadioButton.setSelected(settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY);
        pmsGuaranteedLevel1MovesCheckBox.setSelected(settings.isStartWithGuaranteedMoves());
        pmsGuaranteedLevel1MovesSlider.setValue(settings.getGuaranteedMoveCount());
        pmsReorderDamagingMovesCheckBox.setSelected(settings.isReorderDamagingMoves());
        pmsForceGoodDamagingCheckBox.setSelected(settings.isMovesetsForceGoodDamaging());
        pmsForceGoodDamagingSlider.setValue(settings.getMovesetsGoodDamagingPercent());
        pmsNoGameBreakingMovesCheckBox.setSelected(settings.isBlockBrokenMovesetMoves());
        pmsEvolutionMovesCheckBox.setSelected(settings.isEvolutionMovesForAll());

        tpSimilarStrengthCheckBox.setSelected(settings.isTrainersUsePokemonOfSimilarStrength());
        tpComboBox.setSelectedItem(trainerSettings.get(settings.getTrainersMod().ordinal()));
        tpRivalCarriesStarterCheckBox.setSelected(settings.isRivalCarriesStarterThroughout());
        tpWeightTypesCheckBox.setSelected(settings.isTrainersMatchTypingDistribution());
        tpDontUseLegendariesCheckBox.setSelected(settings.isTrainersBlockLegendaries());
        tpUseLocalPokemonCheckBox.setSelected(settings.isTrainersUseLocalPokemon());
        tpNoEarlyWonderGuardCheckBox.setSelected(settings.isTrainersBlockEarlyWonderGuard());
        tpForceFullyEvolvedAtCheckBox.setSelected(settings.isTrainersForceFullyEvolved());
        tpForceFullyEvolvedAtSlider.setValue(settings.getTrainersForceFullyEvolvedLevel());
        tpPercentageLevelModifierCheckBox.setSelected(settings.isTrainersLevelModified());
        tpPercentageLevelModifierSlider.setValue(settings.getTrainersLevelModifier());
        tpEliteFourUniquePokemonCheckBox.setSelected(settings.getEliteFourUniquePokemonNumber() > 0);
        tpEliteFourUniquePokemonSpinner.setValue(settings.getEliteFourUniquePokemonNumber() > 0 ? settings.getEliteFourUniquePokemonNumber() : 1);
        tpAllowAlternateFormesCheckBox.setSelected(settings.isAllowTrainerAlternateFormes());
        tpSwapMegaEvosCheckBox.setSelected(settings.isSwapTrainerMegaEvos());
        tpDoubleBattleModeCheckBox.setSelected(settings.isDoubleBattleMode());
        tpBossTrainersCheckBox.setSelected(settings.getAdditionalBossTrainerPokemon() > 0);
        tpBossTrainersSpinner.setValue(settings.getAdditionalBossTrainerPokemon() > 0 ? settings.getAdditionalBossTrainerPokemon() : 1);
        tpImportantTrainersCheckBox.setSelected(settings.getAdditionalImportantTrainerPokemon() > 0);
        tpImportantTrainersSpinner.setValue(settings.getAdditionalImportantTrainerPokemon() > 0 ? settings.getAdditionalImportantTrainerPokemon() : 1);
        tpRegularTrainersCheckBox.setSelected(settings.getAdditionalRegularTrainerPokemon() > 0);
        tpRegularTrainersSpinner.setValue(settings.getAdditionalRegularTrainerPokemon() > 0 ? settings.getAdditionalRegularTrainerPokemon() : 1);
        tpBossTrainersItemsCheckBox.setSelected(settings.isRandomizeHeldItemsForBossTrainerPokemon());
        tpImportantTrainersItemsCheckBox.setSelected(settings.isRandomizeHeldItemsForImportantTrainerPokemon());
        tpRegularTrainersItemsCheckBox.setSelected(settings.isRandomizeHeldItemsForRegularTrainerPokemon());
        tpConsumableItemsOnlyCheckBox.setSelected(settings.isConsumableItemsOnlyForTrainers());
        tpSensibleItemsCheckBox.setSelected(settings.isSensibleItemsOnlyForTrainers());
        tpHighestLevelGetsItemCheckBox.setSelected(settings.isHighestLevelGetsItemsForTrainers());

        tpRandomShinyTrainerPokemonCheckBox.setSelected(settings.isShinyChance());
        tpBetterMovesetsCheckBox.setSelected(settings.isBetterTrainerMovesets());

        totpUnchangedRadioButton.setSelected(settings.getTotemPokemonMod() == Settings.TotemPokemonMod.UNCHANGED);
        totpRandomRadioButton.setSelected(settings.getTotemPokemonMod() == Settings.TotemPokemonMod.RANDOM);
        totpRandomSimilarStrengthRadioButton.setSelected(settings.getTotemPokemonMod() == Settings.TotemPokemonMod.SIMILAR_STRENGTH);
        totpAllyUnchangedRadioButton.setSelected(settings.getAllyPokemonMod() == Settings.AllyPokemonMod.UNCHANGED);
        totpAllyRandomRadioButton.setSelected(settings.getAllyPokemonMod() == Settings.AllyPokemonMod.RANDOM);
        totpAllyRandomSimilarStrengthRadioButton.setSelected(settings.getAllyPokemonMod() == Settings.AllyPokemonMod.SIMILAR_STRENGTH);
        totpAuraUnchangedRadioButton.setSelected(settings.getAuraMod() == Settings.AuraMod.UNCHANGED);
        totpAuraRandomRadioButton.setSelected(settings.getAuraMod() == Settings.AuraMod.RANDOM);
        totpAuraRandomSameStrengthRadioButton.setSelected(settings.getAuraMod() == Settings.AuraMod.SAME_STRENGTH);
        totpRandomizeHeldItemsCheckBox.setSelected(settings.isRandomizeTotemHeldItems());
        totpAllowAltFormesCheckBox.setSelected(settings.isAllowTotemAltFormes());
        totpPercentageLevelModifierCheckBox.setSelected(settings.isTotemLevelsModified());
        totpPercentageLevelModifierSlider.setValue(settings.getTotemLevelModifier());

        wpUnchangedRadioButton.setSelected(settings.getWildPokemonMod() == Settings.WildPokemonMod.UNCHANGED);
        wpRandomRadioButton.setSelected(settings.getWildPokemonMod() == Settings.WildPokemonMod.RANDOM);
        wpArea1To1RadioButton.setSelected(settings.getWildPokemonMod() == Settings.WildPokemonMod.AREA_MAPPING);
        wpLocation1To1RadioButton.setSelected(settings.getWildPokemonMod() == Settings.WildPokemonMod.LOCATION_MAPPING);
        wpGlobal1To1RadioButton.setSelected(settings.getWildPokemonMod() == Settings.WildPokemonMod.GLOBAL_MAPPING);

        wpTRNoneRadioButton.setSelected(settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.NONE);
        wpTRThemedAreasRadioButton.setSelected(settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.THEMED_AREAS);
        wpTRKeepPrimaryRadioButton.setSelected(settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.KEEP_PRIMARY);
        wpTRKeepThemesCheckBox.setSelected(settings.isKeepWildTypeThemes());

        wpCatchEmAllModeCheckBox.setSelected(settings.isCatchEmAllEncounters());
        wpSimilarStrengthCheckBox.setSelected(settings.isSimilarStrengthEncounters());

        wpUseTimeBasedEncountersCheckBox.setSelected(settings.isUseTimeBasedEncounters());
        wpSetMinimumCatchRateCheckBox.setSelected(settings.isUseMinimumCatchRate());
        wpSetMinimumCatchRateSlider.setValue(settings.getMinimumCatchRateLevel());
        wpDontUseLegendariesCheckBox.setSelected(settings.isBlockWildLegendaries());
        wpRandomizeHeldItemsCheckBox.setSelected(settings.isRandomizeWildPokemonHeldItems());
        wpBanBadItemsCheckBox.setSelected(settings.isBanBadRandomWildPokemonHeldItems());
        wpBalanceShakingGrassPokemonCheckBox.setSelected(settings.isBalanceShakingGrass());
        wpPercentageLevelModifierCheckBox.setSelected(settings.isWildLevelsModified());
        wpPercentageLevelModifierSlider.setValue(settings.getWildLevelModifier());
        wpAllowAltFormesCheckBox.setSelected(settings.isAllowWildAltFormes());

        stpUnchangedRadioButton.setSelected(settings.getStaticPokemonMod() == Settings.StaticPokemonMod.UNCHANGED);
        stpSwapLegendariesSwapStandardsRadioButton.setSelected(settings.getStaticPokemonMod() == Settings.StaticPokemonMod.RANDOM_MATCHING);
        stpRandomCompletelyRadioButton
                .setSelected(settings.getStaticPokemonMod() == Settings.StaticPokemonMod.COMPLETELY_RANDOM);
        stpRandomSimilarStrengthRadioButton
                .setSelected(settings.getStaticPokemonMod() == Settings.StaticPokemonMod.SIMILAR_STRENGTH);
        stpLimitMainGameLegendariesCheckBox.setSelected(settings.isLimitMainGameLegendaries());
        stpRandomize600BSTCheckBox.setSelected(settings.isLimit600());
        stpAllowAltFormesCheckBox.setSelected(settings.isAllowStaticAltFormes());
        stpSwapMegaEvosCheckBox.setSelected(settings.isSwapStaticMegaEvos());
        stpPercentageLevelModifierCheckBox.setSelected(settings.isStaticLevelModified());
        stpPercentageLevelModifierSlider.setValue(settings.getStaticLevelModifier());
        stpFixMusicCheckBox.setSelected(settings.isCorrectStaticMusic());

        thcRandomCompletelyRadioButton
                .setSelected(settings.getTmsHmsCompatibilityMod() == Settings.TMsHMsCompatibilityMod.COMPLETELY_RANDOM);
        thcRandomPreferSameTypeRadioButton
                .setSelected(settings.getTmsHmsCompatibilityMod() == Settings.TMsHMsCompatibilityMod.RANDOM_PREFER_TYPE);
        thcUnchangedRadioButton
                .setSelected(settings.getTmsHmsCompatibilityMod() == Settings.TMsHMsCompatibilityMod.UNCHANGED);
        tmRandomRadioButton.setSelected(settings.getTmsMod() == Settings.TMsMod.RANDOM);
        tmUnchangedRadioButton.setSelected(settings.getTmsMod() == Settings.TMsMod.UNCHANGED);
        tmLevelupMoveSanityCheckBox.setSelected(settings.isTmLevelUpMoveSanity());
        tmKeepFieldMoveTMsCheckBox.setSelected(settings.isKeepFieldMoveTMs());
        thcFullCompatibilityRadioButton.setSelected(settings.getTmsHmsCompatibilityMod() == Settings.TMsHMsCompatibilityMod.FULL);
        tmFullHMCompatibilityCheckBox.setSelected(settings.isFullHMCompat());
        tmForceGoodDamagingCheckBox.setSelected(settings.isTmsForceGoodDamaging());
        tmForceGoodDamagingSlider.setValue(settings.getTmsGoodDamagingPercent());
        tmNoGameBreakingMovesCheckBox.setSelected(settings.isBlockBrokenTMMoves());
        tmFollowEvolutionsCheckBox.setSelected(settings.isTmsFollowEvolutions());

        mtcRandomCompletelyRadioButton
                .setSelected(settings.getMoveTutorsCompatibilityMod() == Settings.MoveTutorsCompatibilityMod.COMPLETELY_RANDOM);
        mtcRandomPreferSameTypeRadioButton
                .setSelected(settings.getMoveTutorsCompatibilityMod() == Settings.MoveTutorsCompatibilityMod.RANDOM_PREFER_TYPE);
        mtcUnchangedRadioButton
                .setSelected(settings.getMoveTutorsCompatibilityMod() == Settings.MoveTutorsCompatibilityMod.UNCHANGED);
        mtRandomRadioButton.setSelected(settings.getMoveTutorMovesMod() == Settings.MoveTutorMovesMod.RANDOM);
        mtUnchangedRadioButton.setSelected(settings.getMoveTutorMovesMod() == Settings.MoveTutorMovesMod.UNCHANGED);
        mtLevelupMoveSanityCheckBox.setSelected(settings.isTutorLevelUpMoveSanity());
        mtKeepFieldMoveTutorsCheckBox.setSelected(settings.isKeepFieldMoveTutors());
        mtcFullCompatibilityRadioButton
                .setSelected(settings.getMoveTutorsCompatibilityMod() == Settings.MoveTutorsCompatibilityMod.FULL);
        mtForceGoodDamagingCheckBox.setSelected(settings.isTutorsForceGoodDamaging());
        mtForceGoodDamagingSlider.setValue(settings.getTutorsGoodDamagingPercent());
        mtNoGameBreakingMovesCheckBox.setSelected(settings.isBlockBrokenTutorMoves());
        mtFollowEvolutionsCheckBox.setSelected(settings.isTutorFollowEvolutions());

        igtRandomizeBothRequestedGivenRadioButton
                .setSelected(settings.getInGameTradesMod() == Settings.InGameTradesMod.RANDOMIZE_GIVEN_AND_REQUESTED);
        igtRandomizeGivenPokemonOnlyRadioButton.setSelected(settings.getInGameTradesMod() == Settings.InGameTradesMod.RANDOMIZE_GIVEN);
        igtRandomizeItemsCheckBox.setSelected(settings.isRandomizeInGameTradesItems());
        igtRandomizeIVsCheckBox.setSelected(settings.isRandomizeInGameTradesIVs());
        igtRandomizeNicknamesCheckBox.setSelected(settings.isRandomizeInGameTradesNicknames());
        igtRandomizeOTsCheckBox.setSelected(settings.isRandomizeInGameTradesOTs());
        igtUnchangedRadioButton.setSelected(settings.getInGameTradesMod() == Settings.InGameTradesMod.UNCHANGED);

        fiRandomRadioButton.setSelected(settings.getFieldItemsMod() == Settings.FieldItemsMod.RANDOM);
        fiRandomEvenDistributionRadioButton.setSelected(settings.getFieldItemsMod() == Settings.FieldItemsMod.RANDOM_EVEN);
        fiShuffleRadioButton.setSelected(settings.getFieldItemsMod() == Settings.FieldItemsMod.SHUFFLE);
        fiUnchangedRadioButton.setSelected(settings.getFieldItemsMod() == Settings.FieldItemsMod.UNCHANGED);
        fiBanBadItemsCheckBox.setSelected(settings.isBanBadRandomFieldItems());

        shRandomRadioButton.setSelected(settings.getShopItemsMod() == Settings.ShopItemsMod.RANDOM);
        shShuffleRadioButton.setSelected(settings.getShopItemsMod() == Settings.ShopItemsMod.SHUFFLE);
        shUnchangedRadioButton.setSelected(settings.getShopItemsMod() == Settings.ShopItemsMod.UNCHANGED);
        shBanBadItemsCheckBox.setSelected(settings.isBanBadRandomShopItems());
        shBanRegularShopItemsCheckBox.setSelected(settings.isBanRegularShopItems());
        shBanOverpoweredShopItemsCheckBox.setSelected(settings.isBanOPShopItems());
        shBalanceShopItemPricesCheckBox.setSelected(settings.isBalanceShopPrices());
        shGuaranteeEvolutionItemsCheckBox.setSelected(settings.isGuaranteeEvolutionItems());
        shGuaranteeXItemsCheckBox.setSelected(settings.isGuaranteeXItems());

        puUnchangedRadioButton.setSelected(settings.getPickupItemsMod() == Settings.PickupItemsMod.UNCHANGED);
        puRandomRadioButton.setSelected(settings.getPickupItemsMod() == Settings.PickupItemsMod.RANDOM);
        puBanBadItemsCheckBox.setSelected(settings.isBanBadRandomPickupItems());

        ppalUnchangedRadioButton.setSelected(settings.getPokemonPalettesMod() == Settings.PokemonPalettesMod.UNCHANGED);
        ppalRandomRadioButton.setSelected(settings.getPokemonPalettesMod() == Settings.PokemonPalettesMod.RANDOM);
        ppalFollowTypesCheckBox.setSelected(settings.isPokemonPalettesFollowTypes());
        ppalFollowEvolutionsCheckBox.setSelected(settings.isPokemonPalettesFollowEvolutions());
        ppalShinyFromNormalCheckBox.setSelected(settings.isPokemonPalettesShinyFromNormal());

        int mtsSelected = settings.getCurrentMiscTweaks();
        int mtCount = MiscTweak.allTweaks.size();

        for (int mti = 0; mti < mtCount; mti++) {
            MiscTweak mt = MiscTweak.allTweaks.get(mti);
            JCheckBox mtCB = tweakCheckBoxes.get(mti);
            mtCB.setSelected((mtsSelected & mt.getValue()) != 0);
        }

        this.enableOrDisableSubControls();
    }

    private Settings createSettingsFromState(CustomNamesSet customNames) {
        Settings settings = new Settings();
        settings.setRomName(this.romHandler.getROMName());

        settings.setLimitPokemon(limitPokemonCheckBox.isSelected() && limitPokemonCheckBox.isVisible());
        settings.setCurrentRestrictions(currentRestrictions);
        settings.setBanIrregularAltFormes(noIrregularAltFormesCheckBox.isSelected() && noIrregularAltFormesCheckBox.isVisible());
        settings.setRaceMode(raceModeCheckBox.isSelected());

        settings.setChangeImpossibleEvolutions(peChangeImpossibleEvosCheckBox.isSelected() && peChangeImpossibleEvosCheckBox.isVisible());
        settings.setUpdateMoves(mdUpdateMovesCheckBox.isSelected() && mdUpdateMovesCheckBox.isVisible());
        settings.setUpdateMovesToGeneration(mdUpdateComboBox.getSelectedIndex() + (romHandler.generationOfPokemon()+1));
        settings.setRandomizeTrainerNames(tpRandomizeTrainerNamesCheckBox.isSelected());
        settings.setRandomizeTrainerClassNames(tpRandomizeTrainerClassNamesCheckBox.isSelected());

        settings.setBaseStatisticsMod(pbsUnchangedRadioButton.isSelected(), pbsShuffleRadioButton.isSelected(),
                pbsRandomRadioButton.isSelected());
        settings.setBaseStatsFollowEvolutions(pbsFollowEvolutionsCheckBox.isSelected());
        settings.setUpdateBaseStats(pbsUpdateBaseStatsCheckBox.isSelected() && pbsUpdateBaseStatsCheckBox.isVisible());
        settings.setUpdateBaseStatsToGeneration(pbsUpdateComboBox.getSelectedIndex() + (Math.max(6,romHandler.generationOfPokemon()+1)));
        settings.setStandardizeEXPCurves(pbsStandardizeEXPCurvesCheckBox.isSelected());
        settings.setExpCurveMod(pbsLegendariesSlowRadioButton.isSelected(), pbsStrongLegendariesSlowRadioButton.isSelected(),
                pbsAllMediumFastRadioButton.isSelected());
        ExpCurve[] expCurves = getEXPCurvesForGeneration(romHandler.generationOfPokemon());
        settings.setSelectedEXPCurve(expCurves[pbsEXPCurveComboBox.getSelectedIndex()]);
        settings.setBaseStatsFollowMegaEvolutions(pbsFollowMegaEvosCheckBox.isSelected() && pbsFollowMegaEvosCheckBox.isVisible());
        settings.setAssignEvoStatsRandomly(pbsAssignEvoStatsRandomlyCheckBox.isSelected() && pbsAssignEvoStatsRandomlyCheckBox.isVisible());

        settings.setAbilitiesMod(paUnchangedRadioButton.isSelected(), paRandomRadioButton.isSelected());
        settings.setAllowWonderGuard(paAllowWonderGuardCheckBox.isSelected());
        settings.setAbilitiesFollowEvolutions(paFollowEvolutionsCheckBox.isSelected());
        settings.setBanTrappingAbilities(paTrappingAbilitiesCheckBox.isSelected());
        settings.setBanNegativeAbilities(paNegativeAbilitiesCheckBox.isSelected());
        settings.setBanBadAbilities(paBadAbilitiesCheckBox.isSelected());
        settings.setAbilitiesFollowMegaEvolutions(paFollowMegaEvosCheckBox.isSelected());
        settings.setWeighDuplicateAbilitiesTogether(paWeighDuplicatesTogetherCheckBox.isSelected());
        settings.setEnsureTwoAbilities(paEnsureTwoAbilitiesCheckbox.isSelected());

        settings.setTypesMod(ptUnchangedRadioButton.isSelected(), ptRandomFollowEvolutionsRadioButton.isSelected(),
                ptRandomCompletelyRadioButton.isSelected());
        settings.setTypesFollowMegaEvolutions(ptFollowMegaEvosCheckBox.isSelected() && ptFollowMegaEvosCheckBox.isVisible());
        settings.setBlockBrokenMovesetMoves(pmsNoGameBreakingMovesCheckBox.isSelected());
        settings.setDualTypeOnly(ptIsDualTypeCheckBox.isSelected());

        settings.setMakeEvolutionsEasier(peMakeEvolutionsEasierCheckBox.isSelected());
        settings.setRemoveTimeBasedEvolutions(peRemoveTimeBasedEvolutionsCheckBox.isSelected());

        settings.setStartersMod(spUnchangedRadioButton.isSelected(), spCustomRadioButton.isSelected(), spRandomCompletelyRadioButton.isSelected(),
                spRandomTwoEvosRadioButton.isSelected(), spRandomBasicRadioButton.isSelected());
        settings.setStartersTypeMod(spTypeNoneRadioButton.isSelected(), spTypeFwgRadioButton.isSelected(), spTypeTriangleRadioButton.isSelected(),
                spTypeUniqueRadioButton.isSelected(), spTypeSingleRadioButton.isSelected());
        settings.setStartersSingleType(spTypeSingleComboBox.getSelectedIndex());
        settings.setStartersNoDualTypes(spTypeNoDualCheckbox.isSelected());
        settings.setRandomizeStartersHeldItems(spRandomizeStarterHeldItemsCheckBox.isSelected() && spRandomizeStarterHeldItemsCheckBox.isVisible());
        settings.setBanBadRandomStarterHeldItems(spBanBadItemsCheckBox.isSelected() && spBanBadItemsCheckBox.isVisible());
        settings.setAllowStarterAltFormes(spAllowAltFormesCheckBox.isSelected() && spAllowAltFormesCheckBox.isVisible());
        settings.setStartersNoLegendaries(spNoLegendariesCheckBox.isSelected());

        int[] customStarters = new int[] { spComboBox1.getSelectedIndex(),
                spComboBox2.getSelectedIndex(), spComboBox3.getSelectedIndex()};
        settings.setCustomStarters(customStarters);

        settings.setEvolutionsMod(peUnchangedRadioButton.isSelected(), peRandomRadioButton.isSelected(), peRandomEveryLevelRadioButton.isSelected());
        settings.setEvosSimilarStrength(peSimilarStrengthCheckBox.isSelected());
        settings.setEvosSameTyping(peSameTypingCheckBox.isSelected());
        settings.setEvosMaxThreeStages(peLimitEvolutionsToThreeCheckBox.isSelected());
        settings.setEvosForceChange(peForceChangeCheckBox.isSelected());
        settings.setEvosAllowAltFormes(peAllowAltFormesCheckBox.isSelected() && peAllowAltFormesCheckBox.isVisible());
        settings.setEvosForceGrowth(peForceGrowthCheckBox.isSelected());
        settings.setEvosNoConvergence(peNoConvergenceCheckBox.isSelected());

        settings.setRandomizeMoveAccuracies(mdRandomizeMoveAccuracyCheckBox.isSelected());
        settings.setRandomizeMoveCategory(mdRandomizeMoveCategoryCheckBox.isSelected());
        settings.setRandomizeMovePowers(mdRandomizeMovePowerCheckBox.isSelected());
        settings.setRandomizeMovePPs(mdRandomizeMovePPCheckBox.isSelected());
        settings.setRandomizeMoveTypes(mdRandomizeMoveTypesCheckBox.isSelected());

        settings.setMovesetsMod(pmsUnchangedRadioButton.isSelected(), pmsRandomPreferringSameTypeRadioButton.isSelected(),
                pmsRandomCompletelyRadioButton.isSelected(), pmsMetronomeOnlyModeRadioButton.isSelected());
        settings.setStartWithGuaranteedMoves(pmsGuaranteedLevel1MovesCheckBox.isSelected() && pmsGuaranteedLevel1MovesCheckBox.isVisible());
        settings.setGuaranteedMoveCount(pmsGuaranteedLevel1MovesSlider.getValue());
        settings.setReorderDamagingMoves(pmsReorderDamagingMovesCheckBox.isSelected());

        settings.setMovesetsForceGoodDamaging(pmsForceGoodDamagingCheckBox.isSelected());
        settings.setMovesetsGoodDamagingPercent(pmsForceGoodDamagingSlider.getValue());
        settings.setBlockBrokenMovesetMoves(pmsNoGameBreakingMovesCheckBox.isSelected());
        settings.setEvolutionMovesForAll(pmsEvolutionMovesCheckBox.isVisible() &&
                pmsEvolutionMovesCheckBox.isSelected());

        settings.setTrainersMod(isTrainerSetting(TRAINER_UNCHANGED), isTrainerSetting(TRAINER_RANDOM),
                isTrainerSetting(TRAINER_RANDOM_EVEN), isTrainerSetting(TRAINER_RANDOM_EVEN_MAIN),
                isTrainerSetting(TRAINER_TYPE_THEMED), isTrainerSetting(TRAINER_TYPE_THEMED_ELITE4_GYMS),
                isTrainerSetting(TRAINER_KEEP_THEMED));
        settings.setTrainersUsePokemonOfSimilarStrength(tpSimilarStrengthCheckBox.isSelected());
        settings.setRivalCarriesStarterThroughout(tpRivalCarriesStarterCheckBox.isSelected());
        settings.setTrainersMatchTypingDistribution(tpWeightTypesCheckBox.isSelected());
        settings.setTrainersBlockLegendaries(tpDontUseLegendariesCheckBox.isSelected());
        settings.setTrainersUseLocalPokemon(tpUseLocalPokemonCheckBox.isSelected());
        settings.setTrainersBlockEarlyWonderGuard(tpNoEarlyWonderGuardCheckBox.isSelected());
        settings.setTrainersForceFullyEvolved(tpForceFullyEvolvedAtCheckBox.isSelected());
        settings.setTrainersForceFullyEvolvedLevel(tpForceFullyEvolvedAtSlider.getValue());
        settings.setTrainersLevelModified(tpPercentageLevelModifierCheckBox.isSelected());
        settings.setTrainersLevelModifier(tpPercentageLevelModifierSlider.getValue());
        settings.setEliteFourUniquePokemonNumber(tpEliteFourUniquePokemonCheckBox.isVisible() && tpEliteFourUniquePokemonCheckBox.isSelected() ? (int)tpEliteFourUniquePokemonSpinner.getValue() : 0);
        settings.setAllowTrainerAlternateFormes(tpAllowAlternateFormesCheckBox.isSelected() && tpAllowAlternateFormesCheckBox.isVisible());
        settings.setSwapTrainerMegaEvos(tpSwapMegaEvosCheckBox.isSelected() && tpSwapMegaEvosCheckBox.isVisible());
        settings.setDoubleBattleMode(tpDoubleBattleModeCheckBox.isVisible() && tpDoubleBattleModeCheckBox.isSelected());
        settings.setAdditionalBossTrainerPokemon(tpBossTrainersCheckBox.isVisible() && tpBossTrainersCheckBox.isSelected() ? (int)tpBossTrainersSpinner.getValue() : 0);
        settings.setAdditionalImportantTrainerPokemon(tpImportantTrainersCheckBox.isVisible() && tpImportantTrainersCheckBox.isSelected() ? (int)tpImportantTrainersSpinner.getValue() : 0);
        settings.setAdditionalRegularTrainerPokemon(tpRegularTrainersCheckBox.isVisible() && tpRegularTrainersCheckBox.isSelected() ? (int)tpRegularTrainersSpinner.getValue() : 0);
        settings.setShinyChance(tpRandomShinyTrainerPokemonCheckBox.isVisible() && tpRandomShinyTrainerPokemonCheckBox.isSelected());
        settings.setBetterTrainerMovesets(tpBetterMovesetsCheckBox.isVisible() && tpBetterMovesetsCheckBox.isSelected());
        settings.setRandomizeHeldItemsForBossTrainerPokemon(tpBossTrainersItemsCheckBox.isVisible() && tpBossTrainersItemsCheckBox.isSelected());
        settings.setRandomizeHeldItemsForImportantTrainerPokemon(tpImportantTrainersItemsCheckBox.isVisible() && tpImportantTrainersItemsCheckBox.isSelected());
        settings.setRandomizeHeldItemsForRegularTrainerPokemon(tpRegularTrainersItemsCheckBox.isVisible() && tpRegularTrainersItemsCheckBox.isSelected());
        settings.setConsumableItemsOnlyForTrainers(tpConsumableItemsOnlyCheckBox.isVisible() && tpConsumableItemsOnlyCheckBox.isSelected());
        settings.setSensibleItemsOnlyForTrainers(tpSensibleItemsCheckBox.isVisible() && tpSensibleItemsCheckBox.isSelected());
        settings.setHighestLevelGetsItemsForTrainers(tpHighestLevelGetsItemCheckBox.isVisible() && tpHighestLevelGetsItemCheckBox.isSelected());

        settings.setTotemPokemonMod(totpUnchangedRadioButton.isSelected(), totpRandomRadioButton.isSelected(), totpRandomSimilarStrengthRadioButton.isSelected());
        settings.setAllyPokemonMod(totpAllyUnchangedRadioButton.isSelected(), totpAllyRandomRadioButton.isSelected(), totpAllyRandomSimilarStrengthRadioButton.isSelected());
        settings.setAuraMod(totpAuraUnchangedRadioButton.isSelected(), totpAuraRandomRadioButton.isSelected(), totpAuraRandomSameStrengthRadioButton.isSelected());
        settings.setRandomizeTotemHeldItems(totpRandomizeHeldItemsCheckBox.isSelected());
        settings.setAllowTotemAltFormes(totpAllowAltFormesCheckBox.isSelected());
        settings.setTotemLevelsModified(totpPercentageLevelModifierCheckBox.isSelected());
        settings.setTotemLevelModifier(totpPercentageLevelModifierSlider.getValue());

        settings.setWildPokemonMod(wpUnchangedRadioButton.isSelected(), wpRandomRadioButton.isSelected(),
                wpArea1To1RadioButton.isSelected(), wpLocation1To1RadioButton.isSelected(), wpGlobal1To1RadioButton.isSelected());
        settings.setWildPokemonTypeMod(wpTRNoneRadioButton.isSelected(), wpTRThemedAreasRadioButton.isSelected(),
                wpTRKeepPrimaryRadioButton.isSelected());
        settings.setKeepWildTypeThemes(wpTRKeepThemesCheckBox.isSelected());
        settings.setSimilarStrengthEncounters(wpSimilarStrengthCheckBox.isSelected());
        settings.setCatchEmAllEncounters(wpCatchEmAllModeCheckBox.isSelected());
        settings.setUseTimeBasedEncounters(wpUseTimeBasedEncountersCheckBox.isSelected());
        settings.setUseMinimumCatchRate(wpSetMinimumCatchRateCheckBox.isSelected());
        settings.setMinimumCatchRateLevel(wpSetMinimumCatchRateSlider.getValue());
        settings.setBlockWildLegendaries(wpDontUseLegendariesCheckBox.isSelected());
        settings.setRandomizeWildPokemonHeldItems(wpRandomizeHeldItemsCheckBox.isSelected() && wpRandomizeHeldItemsCheckBox.isVisible());
        settings.setBanBadRandomWildPokemonHeldItems(wpBanBadItemsCheckBox.isSelected() && wpBanBadItemsCheckBox.isVisible());
        settings.setBalanceShakingGrass(wpBalanceShakingGrassPokemonCheckBox.isSelected() && wpBalanceShakingGrassPokemonCheckBox.isVisible());
        settings.setWildLevelsModified(wpPercentageLevelModifierCheckBox.isSelected());
        settings.setWildLevelModifier(wpPercentageLevelModifierSlider.getValue());
        settings.setAllowWildAltFormes(wpAllowAltFormesCheckBox.isSelected() && wpAllowAltFormesCheckBox.isVisible());

        settings.setStaticPokemonMod(stpUnchangedRadioButton.isSelected(), stpSwapLegendariesSwapStandardsRadioButton.isSelected(),
                stpRandomCompletelyRadioButton.isSelected(), stpRandomSimilarStrengthRadioButton.isSelected());
        settings.setLimitMainGameLegendaries(stpLimitMainGameLegendariesCheckBox.isSelected() && stpLimitMainGameLegendariesCheckBox.isVisible());
        settings.setLimit600(stpRandomize600BSTCheckBox.isSelected());
        settings.setAllowStaticAltFormes(stpAllowAltFormesCheckBox.isSelected() && stpAllowAltFormesCheckBox.isVisible());
        settings.setSwapStaticMegaEvos(stpSwapMegaEvosCheckBox.isSelected() && stpSwapMegaEvosCheckBox.isVisible());
        settings.setStaticLevelModified(stpPercentageLevelModifierCheckBox.isSelected());
        settings.setStaticLevelModifier(stpPercentageLevelModifierSlider.getValue());
        settings.setCorrectStaticMusic(stpFixMusicCheckBox.isSelected() && stpFixMusicCheckBox.isVisible());

        settings.setTmsMod(tmUnchangedRadioButton.isSelected(), tmRandomRadioButton.isSelected());

        settings.setTmsHmsCompatibilityMod(thcUnchangedRadioButton.isSelected(), thcRandomPreferSameTypeRadioButton.isSelected(),
                thcRandomCompletelyRadioButton.isSelected(), thcFullCompatibilityRadioButton.isSelected());
        settings.setTmLevelUpMoveSanity(tmLevelupMoveSanityCheckBox.isSelected());
        settings.setKeepFieldMoveTMs(tmKeepFieldMoveTMsCheckBox.isSelected());
        settings.setFullHMCompat(tmFullHMCompatibilityCheckBox.isSelected() && tmFullHMCompatibilityCheckBox.isVisible());
        settings.setTmsForceGoodDamaging(tmForceGoodDamagingCheckBox.isSelected());
        settings.setTmsGoodDamagingPercent(tmForceGoodDamagingSlider.getValue());
        settings.setBlockBrokenTMMoves(tmNoGameBreakingMovesCheckBox.isSelected());
        settings.setTmsFollowEvolutions(tmFollowEvolutionsCheckBox.isSelected());

        settings.setMoveTutorMovesMod(mtUnchangedRadioButton.isSelected(), mtRandomRadioButton.isSelected());
        settings.setMoveTutorsCompatibilityMod(mtcUnchangedRadioButton.isSelected(), mtcRandomPreferSameTypeRadioButton.isSelected(),
                mtcRandomCompletelyRadioButton.isSelected(), mtcFullCompatibilityRadioButton.isSelected());
        settings.setTutorLevelUpMoveSanity(mtLevelupMoveSanityCheckBox.isSelected());
        settings.setKeepFieldMoveTutors(mtKeepFieldMoveTutorsCheckBox.isSelected());
        settings.setTutorsForceGoodDamaging(mtForceGoodDamagingCheckBox.isSelected());
        settings.setTutorsGoodDamagingPercent(mtForceGoodDamagingSlider.getValue());
        settings.setBlockBrokenTutorMoves(mtNoGameBreakingMovesCheckBox.isSelected());
        settings.setTutorFollowEvolutions(mtFollowEvolutionsCheckBox.isSelected());

        settings.setInGameTradesMod(igtUnchangedRadioButton.isSelected(), igtRandomizeGivenPokemonOnlyRadioButton.isSelected(), igtRandomizeBothRequestedGivenRadioButton.isSelected());
        settings.setRandomizeInGameTradesItems(igtRandomizeItemsCheckBox.isSelected());
        settings.setRandomizeInGameTradesIVs(igtRandomizeIVsCheckBox.isSelected());
        settings.setRandomizeInGameTradesNicknames(igtRandomizeNicknamesCheckBox.isSelected());
        settings.setRandomizeInGameTradesOTs(igtRandomizeOTsCheckBox.isSelected());

        settings.setFieldItemsMod(fiUnchangedRadioButton.isSelected(), fiShuffleRadioButton.isSelected(), fiRandomRadioButton.isSelected(), fiRandomEvenDistributionRadioButton.isSelected());
        settings.setBanBadRandomFieldItems(fiBanBadItemsCheckBox.isSelected());

        settings.setShopItemsMod(shUnchangedRadioButton.isSelected(), shShuffleRadioButton.isSelected(), shRandomRadioButton.isSelected());
        settings.setBanBadRandomShopItems(shBanBadItemsCheckBox.isSelected());
        settings.setBanRegularShopItems(shBanRegularShopItemsCheckBox.isSelected());
        settings.setBanOPShopItems(shBanOverpoweredShopItemsCheckBox.isSelected());
        settings.setBalanceShopPrices(shBalanceShopItemPricesCheckBox.isSelected());
        settings.setGuaranteeEvolutionItems(shGuaranteeEvolutionItemsCheckBox.isSelected());
        settings.setGuaranteeXItems(shGuaranteeXItemsCheckBox.isSelected());

        settings.setPickupItemsMod(puUnchangedRadioButton.isSelected(), puRandomRadioButton.isSelected());
        settings.setBanBadRandomPickupItems(puBanBadItemsCheckBox.isSelected());

        settings.setTypeEffectivenessMod(teUnchangedRadioButton.isSelected(), teRandomRadioButton.isSelected(),
                teRandomBalancedRadioButton.isSelected(), teKeepTypeIdentitiesRadioButton.isSelected(), teInverseRadioButton.isSelected());
        settings.setInverseTypesRandomImmunities(teAddRandomImmunitiesCheckBox.isSelected());
        settings.setUpdateTypeEffectiveness(teUpdateTypeEffectivenessCheckbox.isSelected());

        settings.setPokemonPalettesMod(ppalUnchangedRadioButton.isSelected(), ppalRandomRadioButton.isSelected());
        settings.setPokemonPalettesFollowTypes(ppalFollowTypesCheckBox.isSelected());
        settings.setPokemonPalettesFollowEvolutions(ppalFollowEvolutionsCheckBox.isSelected());
        settings.setPokemonPalettesShinyFromNormal(ppalShinyFromNormalCheckBox.isSelected());

        settings.setCustomPlayerGraphicsMod(cpgUnchangedRadioButton.isSelected(), cpgCustomRadioButton.isSelected());
        settings.setCustomPlayerGraphics((GraphicsPack) cpgComboBox.getSelectedItem());
        settings.setCustomPlayerGraphicsCharacterMod(cpgReplaceRadioButton1.isSelected(), cpgReplaceRadioButton2.isSelected());

        int currentMiscTweaks = 0;
        int mtCount = MiscTweak.allTweaks.size();

        for (int mti = 0; mti < mtCount; mti++) {
            MiscTweak mt = MiscTweak.allTweaks.get(mti);
            JCheckBox mtCB = tweakCheckBoxes.get(mti);
            if (mtCB.isSelected()) {
                currentMiscTweaks |= mt.getValue();
            }
        }

        settings.setCurrentMiscTweaks(currentMiscTweaks);

        settings.setCustomNames(customNames);

        return settings;
    }

    private Settings getCurrentSettings() throws IOException {
        return createSettingsFromState(FileFunctions.getCustomNames());
    }

    private void attemptToLogException(Exception ex, String baseMessageKey, String noLogMessageKey,
                                       String settingsString, String seedString) {
        attemptToLogException(ex, baseMessageKey, noLogMessageKey, false, settingsString, seedString);
    }

    private void attemptToLogException(Exception ex, String baseMessageKey, String noLogMessageKey, boolean showMessage,
                                       String settingsString, String seedString) {

        // Make sure the operation dialog doesn't show up over the error
        // dialog
        SwingUtilities.invokeLater(() -> RandomizerGUI.this.opDialog.setVisible(false));

        Date now = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        try {
            String errlog = "error_" + ft.format(now) + ".txt";
            PrintStream ps = new PrintStream(new FileOutputStream(errlog));
            ps.println("Randomizer Version: " + Version.VERSION_STRING);
            if (seedString != null) {
                ps.println("Seed: " + seedString);
            }
            if (settingsString != null) {
                ps.println("Settings String: " + Version.VERSION + settingsString);
            }
            ps.println("Java Version: " + System.getProperty("java.version") + ", " + System.getProperty("java.vm.name"));
            PrintStream e1 = System.err;
            System.setErr(ps);
            if (this.romHandler != null) {
                try {
                    ps.println("ROM: " + romHandler.getROMName());
                    ps.println("Code: " + romHandler.getROMCode());
                    ps.println("Reported Support Level: " + romHandler.getSupportLevel());
                    ps.println();
                } catch (Exception ex2) {
                    // Do nothing, just don't fail
                }
            }
            ex.printStackTrace();
            ps.println();
            ps.println("--ROM Diagnostics--");
            if (!romHandler.isRomValid()) {
                ps.println(bundle.getString("Log.InvalidRomLoaded"));
            }
            romHandler.printRomDiagnostics(ps);
            System.setErr(e1);
            ps.close();
            if (showMessage) {
                JOptionPane.showMessageDialog(mainPanel,
                        String.format(bundle.getString(baseMessageKey), ex.getMessage(), errlog));
            } else {
                JOptionPane.showMessageDialog(mainPanel, String.format(bundle.getString(baseMessageKey), errlog));
            }
        } catch (Exception logex) {
            if (showMessage) {
                JOptionPane.showMessageDialog(mainPanel, String.format(bundle.getString(noLogMessageKey), ex.getMessage()));
            } else {
                JOptionPane.showMessageDialog(mainPanel, bundle.getString(noLogMessageKey));
            }
        }
    }

    public String getValidRequiredROMName(String config, CustomNamesSet customNames)
            throws UnsupportedEncodingException, InvalidSupplementFilesException {
        try {
            Utils.validatePresetSupplementFiles(config, customNames);
        } catch (InvalidSupplementFilesException e) {
            switch (e.getType()) {
                case CUSTOM_NAMES:
                    JOptionPane.showMessageDialog(null, bundle.getString("GUI.presetDifferentCustomNames"));
                    break;
                default:
                    throw e;
            }
        }
        byte[] data = Base64.getDecoder().decode(config);

        int nameLength = data[Settings.LENGTH_OF_SETTINGS_DATA] & 0xFF;
        if (data.length != Settings.LENGTH_OF_SETTINGS_DATA + 9 + nameLength) {
            return null; // not valid length
        }
        return new String(data, Settings.LENGTH_OF_SETTINGS_DATA + 1, nameLength, StandardCharsets.US_ASCII);
    }

    private void initialState() {

        romNameLabel.setText(bundle.getString("GUI.noRomLoaded"));
        romCodeLabel.setText("");
        romSupportLabel.setText("");

        gameMascotLabel.setIcon(emptyIcon);

        limitPokemonCheckBox.setVisible(true);
        limitPokemonCheckBox.setEnabled(false);
        limitPokemonCheckBox.setSelected(false);
        limitPokemonButton.setVisible(true);
        limitPokemonButton.setEnabled(false);
        noIrregularAltFormesCheckBox.setVisible(true);
        noIrregularAltFormesCheckBox.setEnabled(false);
        noIrregularAltFormesCheckBox.setSelected(false);
        raceModeCheckBox.setVisible(true);
        raceModeCheckBox.setEnabled(false);
        raceModeCheckBox.setSelected(false);

        currentRestrictions = null;

        openROMButton.setVisible(true);
        openROMButton.setEnabled(true);
        openROMButton.setSelected(false);
        randomizeSaveButton.setVisible(true);
        randomizeSaveButton.setEnabled(true);
        randomizeSaveButton.setSelected(false);
        premadeSeedButton.setVisible(true);
        premadeSeedButton.setEnabled(true);
        premadeSeedButton.setSelected(false);
        settingsButton.setVisible(true);
        settingsButton.setEnabled(true);
        settingsButton.setSelected(false);

        loadSettingsButton.setVisible(true);
        loadSettingsButton.setEnabled(false);
        loadSettingsButton.setSelected(false);
        saveSettingsButton.setVisible(true);
        saveSettingsButton.setEnabled(false);
        saveSettingsButton.setSelected(false);
        tpUseLocalPokemonCheckBox.setVisible(true);
        tpUseLocalPokemonCheckBox.setEnabled(false);
        tpUseLocalPokemonCheckBox.setSelected(false);

        // the buttons in the main part of the gui (randomization options):

		Arrays.asList(pbsUnchangedRadioButton, pbsShuffleRadioButton, pbsRandomRadioButton,
				pbsLegendariesSlowRadioButton, pbsStrongLegendariesSlowRadioButton, pbsAllMediumFastRadioButton,
				pbsStandardizeEXPCurvesCheckBox, pbsFollowEvolutionsCheckBox, pbsUpdateBaseStatsCheckBox,
				pbsFollowMegaEvosCheckBox, pbsAssignEvoStatsRandomlyCheckBox)
				.forEach(this::setInitialButtonState);
		pbsEXPCurveComboBox.setVisible(true);
		pbsEXPCurveComboBox.setEnabled(false);
		pbsEXPCurveComboBox.setSelectedIndex(0);
		pbsEXPCurveComboBox.setModel(new DefaultComboBoxModel<>(new String[] { "Medium Fast" }));
		pbsUpdateComboBox.setVisible(true);
		pbsUpdateComboBox.setEnabled(false);
		pbsUpdateComboBox.setSelectedIndex(0);
		pbsUpdateComboBox.setModel(new DefaultComboBoxModel<>(new String[] { "--" }));

		Arrays.asList(ptUnchangedRadioButton, ptRandomFollowEvolutionsRadioButton, ptRandomCompletelyRadioButton,
				ptFollowMegaEvosCheckBox, ptIsDualTypeCheckBox)
				.forEach(this::setInitialButtonState);

		pokemonAbilitiesPanel.setVisible(true);
		Arrays.asList(paUnchangedRadioButton, paRandomRadioButton, paAllowWonderGuardCheckBox,
				paFollowEvolutionsCheckBox, paTrappingAbilitiesCheckBox, paNegativeAbilitiesCheckBox,
				paBadAbilitiesCheckBox, paFollowMegaEvosCheckBox, paWeighDuplicatesTogetherCheckBox,
				paEnsureTwoAbilitiesCheckbox)
				.forEach(this::setInitialButtonState);

		Arrays.asList(peUnchangedRadioButton, peRandomRadioButton, peRandomEveryLevelRadioButton,
				peSimilarStrengthCheckBox, peSameTypingCheckBox, peLimitEvolutionsToThreeCheckBox,
				peForceChangeCheckBox, peChangeImpossibleEvosCheckBox, peMakeEvolutionsEasierCheckBox,
				peRemoveTimeBasedEvolutionsCheckBox, peAllowAltFormesCheckBox, peForceGrowthCheckBox,
                peNoConvergenceCheckBox).forEach(this::setInitialButtonState);

		Arrays.asList(spUnchangedRadioButton, spCustomRadioButton, spRandomCompletelyRadioButton,
				spRandomTwoEvosRadioButton, spTypeNoneRadioButton, spTypeFwgRadioButton, spTypeTriangleRadioButton,
				spTypeUniqueRadioButton, spTypeSingleRadioButton, spTypeNoDualCheckbox, spNoLegendariesCheckBox,
				spRandomizeStarterHeldItemsCheckBox, spBanBadItemsCheckBox, spAllowAltFormesCheckBox)
                .forEach(this::setInitialButtonState);
		spComboBox1.setVisible(true);
		spComboBox1.setEnabled(false);
		spComboBox1.setSelectedIndex(0);
		spComboBox1.setModel(new DefaultComboBoxModel<>(new String[] { "--" }));
		spComboBox2.setVisible(true);
		spComboBox2.setEnabled(false);
		spComboBox2.setSelectedIndex(0);
		spComboBox2.setModel(new DefaultComboBoxModel<>(new String[] { "--" }));
		spComboBox3.setVisible(true);
		spComboBox3.setEnabled(false);
		spComboBox3.setSelectedIndex(0);
		spComboBox3.setModel(new DefaultComboBoxModel<>(new String[] { "--" }));

		Arrays.asList(stpUnchangedRadioButton, stpSwapLegendariesSwapStandardsRadioButton,
				stpRandomCompletelyRadioButton, stpRandomSimilarStrengthRadioButton, stpPercentageLevelModifierCheckBox,
				stpLimitMainGameLegendariesCheckBox, stpRandomize600BSTCheckBox, stpAllowAltFormesCheckBox,
				stpSwapMegaEvosCheckBox, stpFixMusicCheckBox)
				.forEach(this::setInitialButtonState);
		stpPercentageLevelModifierSlider.setVisible(true);
		stpPercentageLevelModifierSlider.setEnabled(false);
		stpPercentageLevelModifierSlider.setValue(0);

		Arrays.asList(igtUnchangedRadioButton, igtRandomizeGivenPokemonOnlyRadioButton,
				igtRandomizeBothRequestedGivenRadioButton, igtRandomizeNicknamesCheckBox, igtRandomizeOTsCheckBox,
				igtRandomizeIVsCheckBox, igtRandomizeItemsCheckBox)
				.forEach(this::setInitialButtonState);

		Arrays.asList(mdRandomizeMovePowerCheckBox, mdRandomizeMoveAccuracyCheckBox, mdRandomizeMovePPCheckBox,
				mdRandomizeMoveTypesCheckBox, mdRandomizeMoveCategoryCheckBox, mdUpdateMovesCheckBox)
				.forEach(this::setInitialButtonState);
		mdUpdateComboBox.setVisible(true);
		mdUpdateComboBox.setEnabled(false);
		mdUpdateComboBox.setSelectedIndex(0);
		mdUpdateComboBox.setModel(new DefaultComboBoxModel<>(new String[] { "--" }));

		Arrays.asList(pmsUnchangedRadioButton, pmsRandomPreferringSameTypeRadioButton, pmsRandomCompletelyRadioButton,
				pmsMetronomeOnlyModeRadioButton, pmsGuaranteedLevel1MovesCheckBox, pmsReorderDamagingMovesCheckBox,
				pmsNoGameBreakingMovesCheckBox, pmsForceGoodDamagingCheckBox, pmsEvolutionMovesCheckBox)
				.forEach(this::setInitialButtonState);
		pmsGuaranteedLevel1MovesSlider.setVisible(true);
		pmsGuaranteedLevel1MovesSlider.setEnabled(false);
		pmsGuaranteedLevel1MovesSlider.setValue(pmsGuaranteedLevel1MovesSlider.getMinimum());
		pmsForceGoodDamagingSlider.setVisible(true);
		pmsForceGoodDamagingSlider.setEnabled(false);
		pmsForceGoodDamagingSlider.setValue(pmsForceGoodDamagingSlider.getMinimum());

		tpComboBox.setVisible(true);
		tpComboBox.setEnabled(false);
		tpComboBox.setModel(new DefaultComboBoxModel<>(new String[] { "Unchanged" }));
		Arrays.asList(tpRivalCarriesStarterCheckBox, tpSimilarStrengthCheckBox, tpWeightTypesCheckBox,
                tpUseLocalPokemonCheckBox,
				tpDontUseLegendariesCheckBox, tpNoEarlyWonderGuardCheckBox, tpRandomizeTrainerNamesCheckBox,
				tpRandomizeTrainerClassNamesCheckBox, tpForceFullyEvolvedAtCheckBox, tpPercentageLevelModifierCheckBox,
				tpEliteFourUniquePokemonCheckBox, tpAllowAlternateFormesCheckBox, tpSwapMegaEvosCheckBox,
				tpDoubleBattleModeCheckBox, tpBossTrainersCheckBox, tpImportantTrainersCheckBox,
				tpRegularTrainersCheckBox, tpBossTrainersItemsCheckBox, tpImportantTrainersItemsCheckBox,
				tpRegularTrainersItemsCheckBox, tpConsumableItemsOnlyCheckBox, tpSensibleItemsCheckBox,
				tpHighestLevelGetsItemCheckBox, tpRandomShinyTrainerPokemonCheckBox, tpBetterMovesetsCheckBox)
				.forEach(this::setInitialButtonState);
		tpForceFullyEvolvedAtSlider.setVisible(true);
		tpForceFullyEvolvedAtSlider.setEnabled(false);
		tpForceFullyEvolvedAtSlider.setValue(tpForceFullyEvolvedAtSlider.getMinimum());
		tpPercentageLevelModifierSlider.setVisible(true);
		tpPercentageLevelModifierSlider.setEnabled(false);
		tpPercentageLevelModifierSlider.setValue(0);
		tpEliteFourUniquePokemonSpinner.setVisible(true);
		tpEliteFourUniquePokemonSpinner.setEnabled(false);
		tpEliteFourUniquePokemonSpinner.setValue(1);
		tpBossTrainersSpinner.setVisible(true);
		tpBossTrainersSpinner.setEnabled(false);
		tpBossTrainersSpinner.setValue(1);
		tpImportantTrainersSpinner.setVisible(true);
		tpImportantTrainersSpinner.setEnabled(false);
		tpImportantTrainersSpinner.setValue(1);
		tpRegularTrainersSpinner.setVisible(true);
		tpRegularTrainersSpinner.setEnabled(false);
		tpRegularTrainersSpinner.setValue(1);
		tpAdditionalPokemonForLabel.setVisible(true);
		tpHeldItemsLabel.setVisible(true);

		totpPanel.setVisible(true);
		totpAllyPanel.setVisible(true);
		totpAuraPanel.setVisible(true);
		Arrays.asList(totpUnchangedRadioButton, totpRandomRadioButton, totpRandomSimilarStrengthRadioButton,
				totpAllyUnchangedRadioButton, totpAllyRandomRadioButton, totpAllyRandomSimilarStrengthRadioButton,
				totpAuraUnchangedRadioButton, totpAuraRandomRadioButton, totpAuraRandomSameStrengthRadioButton,
				totpPercentageLevelModifierCheckBox, totpRandomizeHeldItemsCheckBox, totpAllowAltFormesCheckBox)
				.forEach(this::setInitialButtonState);
		totpPercentageLevelModifierSlider.setVisible(true);
		totpPercentageLevelModifierSlider.setEnabled(false);
		totpPercentageLevelModifierSlider.setValue(0);

        Arrays.asList(wpUnchangedRadioButton, wpRandomRadioButton, wpArea1To1RadioButton, wpLocation1To1RadioButton,
                        wpGlobal1To1RadioButton, wpTRNoneRadioButton, wpTRThemedAreasRadioButton, wpTRKeepPrimaryRadioButton,
                        wpSimilarStrengthCheckBox, wpCatchEmAllModeCheckBox,
                        wpUseTimeBasedEncountersCheckBox, wpDontUseLegendariesCheckBox,
                        wpSetMinimumCatchRateCheckBox, wpRandomizeHeldItemsCheckBox, wpBanBadItemsCheckBox,
                        wpBalanceShakingGrassPokemonCheckBox, wpPercentageLevelModifierCheckBox, wpAllowAltFormesCheckBox)
                .forEach(this::setInitialButtonState);
		wpSetMinimumCatchRateSlider.setVisible(true);
		wpSetMinimumCatchRateSlider.setEnabled(false);
		wpSetMinimumCatchRateSlider.setValue(wpSetMinimumCatchRateSlider.getMinimum());
		wpPercentageLevelModifierSlider.setVisible(true);
		wpPercentageLevelModifierSlider.setEnabled(false);
		wpPercentageLevelModifierSlider.setValue(0);

		Arrays.asList(tmUnchangedRadioButton, tmRandomRadioButton, tmNoGameBreakingMovesCheckBox,
				tmFullHMCompatibilityCheckBox, tmLevelupMoveSanityCheckBox, tmKeepFieldMoveTMsCheckBox,
				tmForceGoodDamagingCheckBox, tmFollowEvolutionsCheckBox, thcUnchangedRadioButton,
				thcRandomPreferSameTypeRadioButton, thcRandomCompletelyRadioButton, thcFullCompatibilityRadioButton,
				mtUnchangedRadioButton, mtRandomRadioButton, mtNoGameBreakingMovesCheckBox, mtLevelupMoveSanityCheckBox,
				mtLevelupMoveSanityCheckBox, mtKeepFieldMoveTutorsCheckBox, mtForceGoodDamagingCheckBox,
				mtFollowEvolutionsCheckBox, mtcUnchangedRadioButton, mtcRandomPreferSameTypeRadioButton,
				mtcRandomCompletelyRadioButton, mtcFullCompatibilityRadioButton)
				.forEach(this::setInitialButtonState);
		tmForceGoodDamagingSlider.setVisible(true);
		tmForceGoodDamagingSlider.setEnabled(false);
		tmForceGoodDamagingSlider.setValue(tmForceGoodDamagingSlider.getMinimum());
		mtForceGoodDamagingSlider.setVisible(true);
		mtForceGoodDamagingSlider.setEnabled(false);
		mtForceGoodDamagingSlider.setValue(mtForceGoodDamagingSlider.getMinimum());

		Arrays.asList(fiUnchangedRadioButton, fiShuffleRadioButton, fiRandomRadioButton,
				fiRandomEvenDistributionRadioButton, fiBanBadItemsCheckBox, shUnchangedRadioButton,
				shShuffleRadioButton, shRandomRadioButton, shBanOverpoweredShopItemsCheckBox, shBanBadItemsCheckBox,
				shBanRegularShopItemsCheckBox, shBalanceShopItemPricesCheckBox, shGuaranteeEvolutionItemsCheckBox,
				shGuaranteeXItemsCheckBox, puUnchangedRadioButton, puRandomRadioButton, puBanBadItemsCheckBox)
				.forEach(this::setInitialButtonState);

        Arrays.asList(teUnchangedRadioButton, teRandomRadioButton, teRandomBalancedRadioButton,
                teKeepTypeIdentitiesRadioButton, teInverseRadioButton, teAddRandomImmunitiesCheckBox,
                teUpdateTypeEffectivenessCheckbox).forEach(this::setInitialButtonState);

        Arrays.asList(ppalUnchangedRadioButton, ppalRandomRadioButton, ppalFollowTypesCheckBox,
                ppalFollowEvolutionsCheckBox, ppalShinyFromNormalCheckBox,
                        cpgUnchangedRadioButton, cpgCustomRadioButton, cpgRandomButton,
                        cpgReplaceRadioButton1, cpgReplaceRadioButton2)
                .forEach(this::setInitialButtonState);
        cpgComboBox.setVisible(true);
        cpgComboBox.setEnabled(false);
        cpgCustomInfo.setVisible(true);
        cpgCustomInfo.setEnabled(false);

        // TODO: why do these checkboxes exist? can't they just be generated from the MiscTweak objects?
		Arrays.asList(miscBWExpPatchCheckBox, miscNerfXAccuracyCheckBox, miscFixCritRateCheckBox,
				miscFastestTextCheckBox, miscRunningShoesIndoorsCheckBox, miscRandomizePCPotionCheckBox,
				miscAllowPikachuEvolutionCheckBox, miscGiveNationalDexAtCheckBox,
				miscLowerCasePokemonNamesCheckBox, miscRandomizeCatchingTutorialCheckBox, miscBanLuckyEggCheckBox,
				miscNoFreeLuckyEggCheckBox, miscBanBigMoneyManiacCheckBox)
                .forEach(this::setInitialButtonState);

        mtNoExistLabel.setVisible(false);
        mtNoneAvailableLabel.setVisible(false);
        ppalNotExistLabel.setVisible(false);
        ppalPartiallyImplementedLabel.setVisible(false);

        liveTweaksPanel.setVisible(false);
        miscTweaksPanel.setVisible(true);
    }

    private void setInitialButtonState(AbstractButton button) {
        button.setVisible(true);
        button.setEnabled(false);
        button.setSelected(false);
    }

    private void romLoaded() {

        try {
            int pokemonGeneration = romHandler.generationOfPokemon();

            setRomNameLabel();
            romCodeLabel.setText(romHandler.getROMCode());
            romSupportLabel.setText(bundle.getString("GUI.romSupportPrefix") + " "
                    + this.romHandler.getSupportLevel());

            if (!romHandler.isRomValid()) {
                romNameLabel.setForeground(Color.RED);
                romCodeLabel.setForeground(Color.RED);
                romSupportLabel.setForeground(Color.RED);
                romSupportLabel.setText("<html>" + bundle.getString("GUI.romSupportPrefix") + " <b>Unofficial ROM</b>");
                showInvalidRomPopup();
            } else {
                romNameLabel.setForeground(Color.BLACK);
                romCodeLabel.setForeground(Color.BLACK);
                romSupportLabel.setForeground(Color.BLACK);
            }

            limitPokemonCheckBox.setVisible(true);
            limitPokemonCheckBox.setEnabled(true);
            limitPokemonButton.setVisible(true);

            noIrregularAltFormesCheckBox.setVisible(pokemonGeneration >= 4);
            noIrregularAltFormesCheckBox.setEnabled(pokemonGeneration >= 4);

            raceModeCheckBox.setEnabled(true);

            loadSettingsButton.setEnabled(true);
            saveSettingsButton.setEnabled(true);

            // Pokemon Traits

            // Pokemon Base Statistics
            pbsUnchangedRadioButton.setEnabled(true);
            pbsUnchangedRadioButton.setSelected(true);
            pbsShuffleRadioButton.setEnabled(true);
            pbsRandomRadioButton.setEnabled(true);

            pbsStandardizeEXPCurvesCheckBox.setEnabled(true);
            pbsLegendariesSlowRadioButton.setSelected(true);
            pbsUpdateBaseStatsCheckBox.setEnabled(pokemonGeneration < GlobalConstants.HIGHEST_POKEMON_GEN);
            pbsFollowMegaEvosCheckBox.setVisible(romHandler.hasMegaEvolutions());
            pbsUpdateComboBox.setVisible(pokemonGeneration < 8);
            ExpCurve[] expCurves = getEXPCurvesForGeneration(pokemonGeneration);
            String[] expCurveNames = new String[expCurves.length];
            for (int i = 0; i < expCurves.length; i++) {
                expCurveNames[i] = expCurves[i].toString();
            }
            pbsEXPCurveComboBox.setModel(new DefaultComboBoxModel<>(expCurveNames));
            pbsEXPCurveComboBox.setSelectedIndex(0);

            // Pokemon Types
            ptUnchangedRadioButton.setEnabled(true);
            ptUnchangedRadioButton.setSelected(true);
            ptRandomFollowEvolutionsRadioButton.setEnabled(true);
            ptRandomCompletelyRadioButton.setEnabled(true);
            ptFollowMegaEvosCheckBox.setVisible(romHandler.hasMegaEvolutions());
            ptIsDualTypeCheckBox.setEnabled(false);

            // Pokemon Abilities
            if (pokemonGeneration >= 3) {
                paUnchangedRadioButton.setEnabled(true);
                paUnchangedRadioButton.setSelected(true);
                paRandomRadioButton.setEnabled(true);

                paAllowWonderGuardCheckBox.setEnabled(false);
                paFollowEvolutionsCheckBox.setEnabled(false);
                paTrappingAbilitiesCheckBox.setEnabled(false);
                paNegativeAbilitiesCheckBox.setEnabled(false);
                paBadAbilitiesCheckBox.setEnabled(false);
                paFollowMegaEvosCheckBox.setVisible(romHandler.hasMegaEvolutions());
                paWeighDuplicatesTogetherCheckBox.setEnabled(false);
                paEnsureTwoAbilitiesCheckbox.setEnabled(false);
            } else {
                pokemonAbilitiesPanel.setVisible(false);
            }

            // Pokemon Evolutions
            peUnchangedRadioButton.setEnabled(true);
            peUnchangedRadioButton.setSelected(true);
            peRandomRadioButton.setEnabled(true);
            peRandomEveryLevelRadioButton.setVisible(pokemonGeneration >= 3);
            peRandomEveryLevelRadioButton.setEnabled(pokemonGeneration >= 3);
            peChangeImpossibleEvosCheckBox.setEnabled(true);
            peMakeEvolutionsEasierCheckBox.setEnabled(true);
            peRemoveTimeBasedEvolutionsCheckBox.setEnabled(true);
            peAllowAltFormesCheckBox.setVisible(pokemonGeneration >= 7);

            // Starters, Statics & Trades

            // Starter Pokemon
            spUnchangedRadioButton.setEnabled(true);
            spUnchangedRadioButton.setSelected(true);

            spCustomRadioButton.setEnabled(true);
            spRandomCompletelyRadioButton.setEnabled(true);
            spRandomTwoEvosRadioButton.setEnabled(true);
            spRandomBasicRadioButton.setEnabled(true);
            if (romHandler.isYellow()) {
                spComboBox3.setVisible(false);
            }
            populateDropdowns();

            boolean typeTriangleSupport = romHandler.hasStarterTypeTriangleSupport();
            spTypeFwgRadioButton.setVisible(typeTriangleSupport);
            spTypeTriangleRadioButton.setVisible(typeTriangleSupport);

            spAllowAltFormesCheckBox.setVisible(romHandler.hasStarterAltFormes());
            boolean supportsStarterHeldItems = romHandler.supportsStarterHeldItems();
            spRandomizeStarterHeldItemsCheckBox.setEnabled(supportsStarterHeldItems);
            spRandomizeStarterHeldItemsCheckBox.setVisible(supportsStarterHeldItems);
            spBanBadItemsCheckBox.setEnabled(false);
            spBanBadItemsCheckBox.setVisible(supportsStarterHeldItems);

            stpUnchangedRadioButton.setEnabled(true);
            stpUnchangedRadioButton.setSelected(true);
            if (romHandler.canChangeStaticPokemon()) {
                stpSwapLegendariesSwapStandardsRadioButton.setEnabled(true);
                stpRandomCompletelyRadioButton.setEnabled(true);
                stpRandomSimilarStrengthRadioButton.setEnabled(true);
                stpLimitMainGameLegendariesCheckBox.setVisible(romHandler.hasMainGameLegendaries());
                stpLimitMainGameLegendariesCheckBox.setEnabled(false);
                stpAllowAltFormesCheckBox.setVisible(romHandler.hasStaticAltFormes());
                stpSwapMegaEvosCheckBox.setVisible(pokemonGeneration == 6 && !romHandler.forceSwapStaticMegaEvos());
                stpPercentageLevelModifierCheckBox.setVisible(true);
                stpPercentageLevelModifierCheckBox.setEnabled(true);
                stpPercentageLevelModifierSlider.setVisible(true);
                stpPercentageLevelModifierSlider.setEnabled(false);
                stpFixMusicCheckBox.setVisible(romHandler.hasStaticMusicFix());
                stpFixMusicCheckBox.setEnabled(false);
            } else {
                stpSwapLegendariesSwapStandardsRadioButton.setVisible(false);
                stpRandomCompletelyRadioButton.setVisible(false);
                stpRandomSimilarStrengthRadioButton.setVisible(false);
                stpRandomize600BSTCheckBox.setVisible(false);
                stpLimitMainGameLegendariesCheckBox.setVisible(false);
                stpPercentageLevelModifierCheckBox.setVisible(false);
                stpPercentageLevelModifierSlider.setVisible(false);
                stpFixMusicCheckBox.setVisible(false);
            }

            igtUnchangedRadioButton.setEnabled(true);
            igtUnchangedRadioButton.setSelected(true);
            igtRandomizeGivenPokemonOnlyRadioButton.setEnabled(true);
            igtRandomizeBothRequestedGivenRadioButton.setEnabled(true);

            igtRandomizeNicknamesCheckBox.setEnabled(false);
            igtRandomizeOTsCheckBox.setEnabled(false);
            igtRandomizeIVsCheckBox.setEnabled(false);
            igtRandomizeItemsCheckBox.setEnabled(false);

            if (pokemonGeneration == 1) {
                igtRandomizeOTsCheckBox.setVisible(false);
                igtRandomizeIVsCheckBox.setVisible(false);
                igtRandomizeItemsCheckBox.setVisible(false);
            }

            // Move Data
            mdRandomizeMovePowerCheckBox.setEnabled(true);
            mdRandomizeMoveAccuracyCheckBox.setEnabled(true);
            mdRandomizeMovePPCheckBox.setEnabled(true);
            mdRandomizeMoveTypesCheckBox.setEnabled(true);
            mdRandomizeMoveCategoryCheckBox.setEnabled(romHandler.hasPhysicalSpecialSplit());
            mdRandomizeMoveCategoryCheckBox.setVisible(romHandler.hasPhysicalSpecialSplit());
            mdUpdateMovesCheckBox.setEnabled(pokemonGeneration < 8);
            mdUpdateMovesCheckBox.setVisible(pokemonGeneration < 8);

            // Pokemon Movesets
            pmsUnchangedRadioButton.setEnabled(true);
            pmsUnchangedRadioButton.setSelected(true);
            pmsRandomPreferringSameTypeRadioButton.setEnabled(true);
            pmsRandomCompletelyRadioButton.setEnabled(true);
            pmsMetronomeOnlyModeRadioButton.setEnabled(true);

            pmsGuaranteedLevel1MovesCheckBox.setVisible(romHandler.supportsFourStartingMoves());
            pmsGuaranteedLevel1MovesSlider.setVisible(romHandler.supportsFourStartingMoves());
            pmsEvolutionMovesCheckBox.setVisible(pokemonGeneration >= 7);

            tpComboBox.setEnabled(true);
            tpAllowAlternateFormesCheckBox.setVisible(romHandler.hasFunctionalFormes());
            tpForceFullyEvolvedAtCheckBox.setEnabled(true);
            tpPercentageLevelModifierCheckBox.setEnabled(true);
            tpSwapMegaEvosCheckBox.setVisible(romHandler.hasMegaEvolutions());
            tpDoubleBattleModeCheckBox.setVisible(pokemonGeneration >= 3);

            boolean canAddPokesToBoss = romHandler.canAddPokemonToBossTrainers();
            boolean canAddPokesToImportant = romHandler.canAddPokemonToImportantTrainers();
            boolean canAddPokesToRegular = romHandler.canAddPokemonToRegularTrainers();
            boolean additionalPokemonAvailable = canAddPokesToBoss || canAddPokesToImportant || canAddPokesToRegular;

            tpAdditionalPokemonForLabel.setVisible(additionalPokemonAvailable);
            tpBossTrainersCheckBox.setVisible(canAddPokesToBoss);
            tpBossTrainersCheckBox.setEnabled(false);
            tpBossTrainersSpinner.setVisible(canAddPokesToBoss);
            tpImportantTrainersCheckBox.setVisible(canAddPokesToImportant);
            tpImportantTrainersCheckBox.setEnabled(false);
            tpImportantTrainersSpinner.setVisible(canAddPokesToImportant);
            tpRegularTrainersCheckBox.setVisible(canAddPokesToRegular);
            tpRegularTrainersCheckBox.setEnabled(false);
            tpRegularTrainersSpinner.setVisible(canAddPokesToRegular);

            boolean canAddHeldItemsToBoss = romHandler.canAddHeldItemsToBossTrainers();
            boolean canAddHeldItemsToImportant = romHandler.canAddPokemonToImportantTrainers();
            boolean canAddHeldItemsToRegular = romHandler.canAddHeldItemsToRegularTrainers();
            boolean heldItemsAvailable = canAddHeldItemsToBoss || canAddHeldItemsToImportant || canAddHeldItemsToRegular;

            tpHeldItemsLabel.setVisible(heldItemsAvailable);
            tpBossTrainersItemsCheckBox.setVisible(canAddHeldItemsToBoss);
            tpBossTrainersItemsCheckBox.setEnabled(false);
            tpImportantTrainersItemsCheckBox.setVisible(canAddHeldItemsToImportant);
            tpImportantTrainersItemsCheckBox.setEnabled(false);
            tpRegularTrainersItemsCheckBox.setVisible(canAddHeldItemsToRegular);
            tpRegularTrainersItemsCheckBox.setEnabled(false);
            tpConsumableItemsOnlyCheckBox.setVisible(heldItemsAvailable);
            tpConsumableItemsOnlyCheckBox.setEnabled(false);
            tpSensibleItemsCheckBox.setVisible(heldItemsAvailable);
            tpSensibleItemsCheckBox.setEnabled(false);
            tpHighestLevelGetsItemCheckBox.setVisible(heldItemsAvailable);
            tpHighestLevelGetsItemCheckBox.setEnabled(false);

            tpRandomizeTrainerNamesCheckBox.setEnabled(true);
            tpRandomizeTrainerClassNamesCheckBox.setEnabled(true);
            tpNoEarlyWonderGuardCheckBox.setVisible(pokemonGeneration >= 3);
            tpRandomShinyTrainerPokemonCheckBox.setVisible(pokemonGeneration >= 7);
            tpBetterMovesetsCheckBox.setVisible(pokemonGeneration >= 3);
            tpBetterMovesetsCheckBox.setEnabled(pokemonGeneration >= 3);

            totpPanel.setVisible(romHandler.hasTotemPokemon());
            if (totpPanel.isVisible()) {
                totpUnchangedRadioButton.setEnabled(true);
                totpRandomRadioButton.setEnabled(true);
                totpRandomSimilarStrengthRadioButton.setEnabled(true);

                totpAllyPanel.setVisible(romHandler.hasTotemPokemon());
                totpAllyUnchangedRadioButton.setEnabled(true);
                totpAllyRandomRadioButton.setEnabled(true);
                totpAllyRandomSimilarStrengthRadioButton.setEnabled(true);

                totpAuraPanel.setVisible(romHandler.hasTotemPokemon());
                totpAuraUnchangedRadioButton.setEnabled(true);
                totpAuraRandomRadioButton.setEnabled(true);
                totpAuraRandomSameStrengthRadioButton.setEnabled(true);

                totpRandomizeHeldItemsCheckBox.setEnabled(true);
                totpAllowAltFormesCheckBox.setEnabled(false);
                totpPercentageLevelModifierCheckBox.setEnabled(true);
                totpPercentageLevelModifierSlider.setEnabled(false);
            }

            // Wild Pokemon
            wpUnchangedRadioButton.setEnabled(true);
            wpUnchangedRadioButton.setSelected(true);
            wpRandomRadioButton.setEnabled(true);
            wpArea1To1RadioButton.setEnabled(true);
            wpLocation1To1RadioButton.setVisible(romHandler.hasEncounterLocations());
            if (wpLocation1To1RadioButton.isVisible()) {
                wpLocation1To1RadioButton.setEnabled(true);
            }
            wpGlobal1To1RadioButton.setEnabled(true);
;            wpUseTimeBasedEncountersCheckBox.setVisible(romHandler.hasTimeBasedEncounters());
            wpSetMinimumCatchRateCheckBox.setEnabled(true);
            wpRandomizeHeldItemsCheckBox.setEnabled(true);
            wpRandomizeHeldItemsCheckBox.setVisible(pokemonGeneration != 1);
            wpBanBadItemsCheckBox.setVisible(pokemonGeneration != 1);
            wpBalanceShakingGrassPokemonCheckBox.setVisible(pokemonGeneration == 5);
            wpPercentageLevelModifierCheckBox.setEnabled(true);
            wpAllowAltFormesCheckBox.setVisible(romHandler.hasWildAltFormes());

            tmUnchangedRadioButton.setEnabled(true);
            tmUnchangedRadioButton.setSelected(true);
            tmRandomRadioButton.setEnabled(true);
            tmFullHMCompatibilityCheckBox.setVisible(pokemonGeneration < 7);
            if (tmFullHMCompatibilityCheckBox.isVisible()) {
                tmFullHMCompatibilityCheckBox.setEnabled(true);
            }

            thcUnchangedRadioButton.setEnabled(true);
            thcUnchangedRadioButton.setSelected(true);
            thcRandomPreferSameTypeRadioButton.setEnabled(true);
            thcRandomCompletelyRadioButton.setEnabled(true);
            thcFullCompatibilityRadioButton.setEnabled(true);

            if (romHandler.hasMoveTutors()) {
                mtMovesPanel.setVisible(true);
                mtCompatPanel.setVisible(true);
                mtNoExistLabel.setVisible(false);

                mtUnchangedRadioButton.setEnabled(true);
                mtUnchangedRadioButton.setSelected(true);
                mtRandomRadioButton.setEnabled(true);

                mtcUnchangedRadioButton.setEnabled(true);
                mtcUnchangedRadioButton.setSelected(true);
                mtcRandomPreferSameTypeRadioButton.setEnabled(true);
                mtcRandomCompletelyRadioButton.setEnabled(true);
                mtcFullCompatibilityRadioButton.setEnabled(true);
            } else {
                mtMovesPanel.setVisible(false);
                mtCompatPanel.setVisible(false);
                mtNoExistLabel.setVisible(true);
            }

            fiUnchangedRadioButton.setEnabled(true);
            fiUnchangedRadioButton.setSelected(true);
            fiShuffleRadioButton.setEnabled(true);
            fiRandomRadioButton.setEnabled(true);
            fiRandomEvenDistributionRadioButton.setEnabled(true);

            shopItemsPanel.setVisible(romHandler.hasShopSupport());
            shUnchangedRadioButton.setEnabled(true);
            shUnchangedRadioButton.setSelected(true);
            shShuffleRadioButton.setEnabled(true);
            shRandomRadioButton.setEnabled(true);

            pickupItemsPanel.setVisible(romHandler.abilitiesPerPokemon() > 0);
            puUnchangedRadioButton.setEnabled(true);
            puUnchangedRadioButton.setSelected(true);
            puRandomRadioButton.setEnabled(true);

            // Types
            boolean typeSupport = romHandler.hasTypeEffectivenessSupport();
            typesPanel.setVisible(typeSupport);
            teUnchangedRadioButton.setEnabled(typeSupport);
            teUnchangedRadioButton.setSelected(typeSupport);
            teRandomRadioButton.setEnabled(typeSupport);
            teRandomBalancedRadioButton.setEnabled(typeSupport);
            teKeepTypeIdentitiesRadioButton.setEnabled(typeSupport);
            teInverseRadioButton.setEnabled(typeSupport);
            teAddRandomImmunitiesCheckBox.setEnabled(false);
            teAddRandomImmunitiesCheckBox.setSelected(false);
            teUpdateTypeEffectivenessCheckbox.setEnabled(typeSupport);
            teUpdateTypeEffectivenessCheckbox.setSelected(false);

            // Graphics
            boolean ppalSupport = romHandler.hasPokemonPaletteSupport();
            ppalNotExistLabel.setVisible(!ppalSupport);
            boolean ppalPartialSupport = romHandler.pokemonPaletteSupportIsPartial();
            ppalPartiallyImplementedLabel.setVisible(ppalPartialSupport);
            ppalUnchangedRadioButton.setVisible(ppalSupport);
            ppalUnchangedRadioButton.setEnabled(ppalSupport);
            ppalUnchangedRadioButton.setSelected(ppalSupport);
            ppalRandomRadioButton.setVisible(ppalSupport);
            ppalRandomRadioButton.setEnabled(ppalSupport);
            ppalFollowTypesCheckBox.setVisible(ppalSupport);
            ppalFollowTypesCheckBox.setEnabled(false);
            ppalFollowEvolutionsCheckBox.setVisible(ppalSupport);
            ppalFollowEvolutionsCheckBox.setEnabled(false);
            ppalShinyFromNormalCheckBox.setVisible(!(romHandler instanceof Gen1RomHandler) && ppalSupport);
            ppalShinyFromNormalCheckBox.setEnabled(false);

            boolean cpgSupport = romHandler.hasCustomPlayerGraphicsSupport();
            cpgNotExistLabel.setVisible(!cpgSupport);
            cpgUnchangedRadioButton.setVisible(cpgSupport);
            cpgUnchangedRadioButton.setEnabled(cpgSupport);
            cpgUnchangedRadioButton.setSelected(ppalSupport);
            cpgCustomRadioButton.setVisible(cpgSupport);
            cpgCustomRadioButton.setEnabled(cpgSupport);
            cpgComboBox.setVisible(cpgSupport);
            cpgComboBox.setEnabled(false);
            if (cpgSupport) {
                fillCustomPlayerGraphicsComboBox();
            }
            cpgRandomButton.setVisible(cpgSupport);
            cpgRandomButton.setEnabled(false);
            cpgCustomInfo.setVisible(cpgSupport);
            cpgCustomInfo.setEnabled(false);
            boolean cpgReplaceChoiceSupport = cpgSupport && romHandler.hasMultiplePlayerCharacters();
            cpgReplaceLabel.setVisible(cpgReplaceChoiceSupport);
            cpgReplaceLabel.setEnabled(false);
            cpgReplaceRadioButton1.setVisible(cpgReplaceChoiceSupport);
            cpgReplaceRadioButton1.setEnabled(false);
            cpgReplaceRadioButton1.setSelected(cpgReplaceChoiceSupport);
            cpgReplaceRadioButton2.setVisible(cpgReplaceChoiceSupport);
            cpgReplaceRadioButton2.setEnabled(false);

            // Misc. Tweaks
            int mtsAvailable = romHandler.miscTweaksAvailable();
            int mtCount = MiscTweak.allTweaks.size();
            List<JCheckBox> usableCheckBoxes = new ArrayList<>();

            for (int mti = 0; mti < mtCount; mti++) {
                MiscTweak mt = MiscTweak.allTweaks.get(mti);
                JCheckBox mtCB = tweakCheckBoxes.get(mti);
                mtCB.setSelected(false);
                if ((mtsAvailable & mt.getValue()) != 0) {
                    mtCB.setVisible(true);
                    mtCB.setEnabled(true);
                    usableCheckBoxes.add(mtCB);
                } else {
                    mtCB.setVisible(false);
                    mtCB.setEnabled(false);
                }
            }

            if (usableCheckBoxes.size() > 0) {
                setTweaksPanel(usableCheckBoxes);
                //tabbedPane1.setComponentAt(7,makeTweaksLayout(usableCheckBoxes));
                //miscTweaksPanel.setLayout(makeTweaksLayout(usableCheckBoxes));
            } else {
                mtNoneAvailableLabel.setVisible(true);
                liveTweaksPanel.setVisible(false);
                miscTweaksPanel.setVisible(true);
                //miscTweaksPanel.setLayout(noTweaksLayout);
            }

            if (romHandler.generationOfPokemon() < 6) {
                applyGameUpdateMenuItem.setVisible(false);
            } else {
                applyGameUpdateMenuItem.setVisible(true);
            }

            if (romHandler.hasGameUpdateLoaded()) {
                removeGameUpdateMenuItem.setVisible(true);
            } else {
                removeGameUpdateMenuItem.setVisible(false);
            }

            gameMascotLabel.setIcon(makeMascotIcon());

            if (romHandler instanceof AbstractDSRomHandler) {
                ((AbstractDSRomHandler) romHandler).closeInnerRom();
            } else if (romHandler instanceof Abstract3DSRomHandler) {
                ((Abstract3DSRomHandler) romHandler).closeInnerRom();
            }
        } catch (Exception e) {
            attemptToLogException(e, "GUI.processFailed","GUI.processFailedNoLog", null, null);
            romHandler = null;
            initialState();
        }
    }

    private void fillCustomPlayerGraphicsComboBox() {
        DefaultComboBoxModel<GraphicsPack> comboBoxModel = new DefaultComboBoxModel<>();
        cpgComboBox.setModel(comboBoxModel);
        File players = new File(SysConstants.customPCGDirectory);
        File[] playerDirectories = players.listFiles(File::isDirectory);
        if (playerDirectories != null) {
            for (File playerDir : playerDirectories) {
                try {
                    String path = playerDir.getCanonicalPath();
                    List<GraphicsPackEntry> entries = GraphicsPackEntry.readAllFromFolder(path);
                    entries.forEach(entry -> {
                        if (entry.getStringValue("RomType").equalsIgnoreCase("Gen1") && romHandler.generationOfPokemon() == 1) {
                            comboBoxModel.addElement(new Gen1PlayerCharacterGraphics(entry));
                        } else if (entry.getStringValue("RomType").equalsIgnoreCase("Gen2") && romHandler.generationOfPokemon() == 2) {
                            comboBoxModel.addElement(new Gen2PlayerCharacterGraphics(entry));
                        } else if (romHandler.generationOfPokemon() == 3) {
                            if ((romHandler.getROMType() == Gen3Constants.RomType_Ruby ||
                                    romHandler.getROMType() == Gen3Constants.RomType_Sapp ||
                                    romHandler.getROMType() == Gen3Constants.RomType_Em) &&
                                    entry.getStringValue("RomType").equalsIgnoreCase("RSE")) {
                                comboBoxModel.addElement(new RSEPlayerCharacterGraphics(entry));
                            } else if (romHandler.getROMType() == Gen3Constants.RomType_FRLG &&
                                    entry.getStringValue("RomType").equalsIgnoreCase("FRLG")) {
                                comboBoxModel.addElement(new FRLGPlayerCharacterGraphics(entry));
                            }
                        }
                    });
                } catch (Exception ignored) {
                    System.out.println("Could not read " + playerDir);
                    ignored.printStackTrace();
                }
            }
        }
    }

    private void setRomNameLabel() {
        if (romHandler.hasGameUpdateLoaded()) {
            romNameLabel.setText(romHandler.getROMName() + " (" + romHandler.getGameUpdateVersion() + ")");
        } else {
            romNameLabel.setText(romHandler.getROMName());
        }
    }

    private void setTweaksPanel(List<JCheckBox> usableCheckBoxes) {
        mtNoneAvailableLabel.setVisible(false);
        miscTweaksPanel.setVisible(false);
        baseTweaksPanel.remove(liveTweaksPanel);
        makeTweaksLayout(usableCheckBoxes);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.1;
        c.weighty = 0.1;
        c.gridx = 1;
        c.gridy = 1;
        baseTweaksPanel.add(liveTweaksPanel,c);
        liveTweaksPanel.setVisible(true);
    }

    private void enableOrDisableSubControls() {

        if (limitPokemonCheckBox.isSelected()) {
            limitPokemonButton.setEnabled(true);
        } else {
            limitPokemonButton.setEnabled(false);
        }

        boolean followEvolutionControlsEnabled = !peRandomEveryLevelRadioButton.isSelected();
        boolean followMegaEvolutionControlsEnabled = !(peRandomEveryLevelRadioButton.isSelected() && !noIrregularAltFormesCheckBox.isSelected() && peAllowAltFormesCheckBox.isSelected());

        if (peRandomEveryLevelRadioButton.isSelected()) {
            // If Evolve Every Level is enabled, unselect all "Follow Evolutions" controls
            pbsFollowEvolutionsCheckBox.setSelected(false);
            ptRandomFollowEvolutionsRadioButton.setEnabled(false);
            if (ptRandomFollowEvolutionsRadioButton.isSelected()) {
                ptRandomFollowEvolutionsRadioButton.setSelected(false);
                ptRandomCompletelyRadioButton.setSelected(true);
            }
            spRandomTwoEvosRadioButton.setEnabled(false);
            if (spRandomTwoEvosRadioButton.isSelected()) {
                spRandomTwoEvosRadioButton.setSelected(false);
                spRandomCompletelyRadioButton.setSelected(true);
            }
            spRandomBasicRadioButton.setEnabled(false);
            if (spRandomBasicRadioButton.isSelected()) {
                spRandomBasicRadioButton.setSelected(false);
                spRandomCompletelyRadioButton.setSelected(true);
            }
            paFollowEvolutionsCheckBox.setSelected(false);
            tmFollowEvolutionsCheckBox.setSelected(false);
            mtFollowEvolutionsCheckBox.setSelected(false);

            // If the Follow Mega Evolution controls should be disabled, deselect them here too
            if (!followMegaEvolutionControlsEnabled) {
                pbsFollowMegaEvosCheckBox.setSelected(false);
                ptFollowMegaEvosCheckBox.setSelected(false);
                paFollowMegaEvosCheckBox.setSelected(false);
            }

            // Also disable/unselect all the settings that make evolutions easier/possible,
            // since they aren't relevant in this scenario at all.
            peChangeImpossibleEvosCheckBox.setEnabled(false);
            peChangeImpossibleEvosCheckBox.setSelected(false);
            peMakeEvolutionsEasierCheckBox.setEnabled(false);
            peMakeEvolutionsEasierCheckBox.setSelected(false);
            peRemoveTimeBasedEvolutionsCheckBox.setEnabled(false);
            peRemoveTimeBasedEvolutionsCheckBox.setSelected(false);

            // Disable "Force Fully Evolved" Trainer Pokemon
            tpForceFullyEvolvedAtCheckBox.setSelected(false);
            tpForceFullyEvolvedAtCheckBox.setEnabled(false);
            tpForceFullyEvolvedAtSlider.setEnabled(false);
            tpForceFullyEvolvedAtSlider.setValue(tpForceFullyEvolvedAtSlider.getMinimum());
        } else {
            // All other "Follow Evolutions" controls get properly set/unset below
            // except this one, so manually enable it again.
            ptRandomFollowEvolutionsRadioButton.setEnabled(true);
            spRandomTwoEvosRadioButton.setEnabled(true);
            spRandomBasicRadioButton.setEnabled(true);

            // The controls that make evolutions easier/possible, however,
            // need to all be manually re-enabled.
            peChangeImpossibleEvosCheckBox.setEnabled(true);
            peMakeEvolutionsEasierCheckBox.setEnabled(true);
            peRemoveTimeBasedEvolutionsCheckBox.setEnabled(true);

            // Re-enable "Force Fully Evolved" Trainer Pokemon
            tpForceFullyEvolvedAtCheckBox.setEnabled(true);
        }

        if (pbsUnchangedRadioButton.isSelected()) {
            pbsFollowEvolutionsCheckBox.setEnabled(false);
            pbsFollowEvolutionsCheckBox.setSelected(false);
            pbsFollowMegaEvosCheckBox.setEnabled(false);
            pbsFollowMegaEvosCheckBox.setSelected(false);
        } else {
            pbsFollowEvolutionsCheckBox.setEnabled(followEvolutionControlsEnabled);
            pbsFollowMegaEvosCheckBox.setEnabled(followMegaEvolutionControlsEnabled);
        }

        if (pbsRandomRadioButton.isSelected()) {
            if (pbsFollowEvolutionsCheckBox.isSelected() || pbsFollowMegaEvosCheckBox.isSelected()) {
                pbsAssignEvoStatsRandomlyCheckBox.setEnabled(true);
            } else {
                pbsAssignEvoStatsRandomlyCheckBox.setEnabled(false);
                pbsAssignEvoStatsRandomlyCheckBox.setSelected(false);
            }
        } else {
            pbsAssignEvoStatsRandomlyCheckBox.setEnabled(false);
            pbsAssignEvoStatsRandomlyCheckBox.setSelected(false);
        }

        if (pbsStandardizeEXPCurvesCheckBox.isSelected()) {
            pbsLegendariesSlowRadioButton.setEnabled(true);
            pbsStrongLegendariesSlowRadioButton.setEnabled(true);
            pbsAllMediumFastRadioButton.setEnabled(true);
            pbsEXPCurveComboBox.setEnabled(true);
        } else {
            pbsLegendariesSlowRadioButton.setEnabled(false);
            pbsLegendariesSlowRadioButton.setSelected(true);
            pbsStrongLegendariesSlowRadioButton.setEnabled(false);
            pbsAllMediumFastRadioButton.setEnabled(false);
            pbsEXPCurveComboBox.setEnabled(false);
        }

        if (pbsUpdateBaseStatsCheckBox.isSelected()) {
            pbsUpdateComboBox.setEnabled(true);
        } else {
            pbsUpdateComboBox.setEnabled(false);
        }

        if (ptUnchangedRadioButton.isSelected()) {
            ptFollowMegaEvosCheckBox.setEnabled(false);
            ptFollowMegaEvosCheckBox.setSelected(false);
            ptIsDualTypeCheckBox.setEnabled(false);
            ptIsDualTypeCheckBox.setSelected(false);
        } else {
            ptFollowMegaEvosCheckBox.setEnabled(followMegaEvolutionControlsEnabled);
            ptIsDualTypeCheckBox.setEnabled(true);
        }

        if (paRandomRadioButton.isSelected()) {
            paAllowWonderGuardCheckBox.setEnabled(true);
            paFollowEvolutionsCheckBox.setEnabled(followEvolutionControlsEnabled);
            paFollowMegaEvosCheckBox.setEnabled(followMegaEvolutionControlsEnabled);
            paTrappingAbilitiesCheckBox.setEnabled(true);
            paNegativeAbilitiesCheckBox.setEnabled(true);
            paBadAbilitiesCheckBox.setEnabled(true);
            paWeighDuplicatesTogetherCheckBox.setEnabled(true);
            paEnsureTwoAbilitiesCheckbox.setEnabled(true);
        } else {
            paAllowWonderGuardCheckBox.setEnabled(false);
            paAllowWonderGuardCheckBox.setSelected(false);
            paFollowEvolutionsCheckBox.setEnabled(false);
            paFollowEvolutionsCheckBox.setSelected(false);
            paTrappingAbilitiesCheckBox.setEnabled(false);
            paTrappingAbilitiesCheckBox.setSelected(false);
            paNegativeAbilitiesCheckBox.setEnabled(false);
            paNegativeAbilitiesCheckBox.setSelected(false);
            paBadAbilitiesCheckBox.setEnabled(false);
            paBadAbilitiesCheckBox.setSelected(false);
            paFollowMegaEvosCheckBox.setEnabled(false);
            paFollowMegaEvosCheckBox.setSelected(false);
            paWeighDuplicatesTogetherCheckBox.setEnabled(false);
            paWeighDuplicatesTogetherCheckBox.setSelected(false);
            paEnsureTwoAbilitiesCheckbox.setEnabled(false);
            paEnsureTwoAbilitiesCheckbox.setSelected(false);
        }

        if (peRandomRadioButton.isSelected()) {
            peSimilarStrengthCheckBox.setEnabled(true);
            peSameTypingCheckBox.setEnabled(true);
            peLimitEvolutionsToThreeCheckBox.setEnabled(true);
            peForceChangeCheckBox.setEnabled(true);
            peAllowAltFormesCheckBox.setEnabled(true);
            peForceGrowthCheckBox.setEnabled(true);
            peNoConvergenceCheckBox.setEnabled(true);
        } else if (peRandomEveryLevelRadioButton.isSelected()) {
            peSimilarStrengthCheckBox.setEnabled(false);
            peSimilarStrengthCheckBox.setSelected(false);
            peSameTypingCheckBox.setEnabled(true);
            peLimitEvolutionsToThreeCheckBox.setEnabled(false);
            peLimitEvolutionsToThreeCheckBox.setSelected(false);
            peForceChangeCheckBox.setEnabled(true);
            peAllowAltFormesCheckBox.setEnabled(true);
            peForceGrowthCheckBox.setEnabled(false);
            peForceGrowthCheckBox.setSelected(false);
            peNoConvergenceCheckBox.setEnabled(true);
        } else {
            peSimilarStrengthCheckBox.setEnabled(false);
            peSimilarStrengthCheckBox.setSelected(false);
            peSameTypingCheckBox.setEnabled(false);
            peSameTypingCheckBox.setSelected(false);
            peLimitEvolutionsToThreeCheckBox.setEnabled(false);
            peLimitEvolutionsToThreeCheckBox.setSelected(false);
            peForceChangeCheckBox.setEnabled(false);
            peForceChangeCheckBox.setSelected(false);
            peAllowAltFormesCheckBox.setEnabled(false);
            peAllowAltFormesCheckBox.setSelected(false);
            peForceGrowthCheckBox.setEnabled(false);
            peForceGrowthCheckBox.setSelected(false);
            peNoConvergenceCheckBox.setEnabled(false);
            peNoConvergenceCheckBox.setSelected(false);
        }

        boolean spCustomStatus = spCustomRadioButton.isSelected();
        spComboBox1.setEnabled(spCustomStatus);
        spComboBox2.setEnabled(spCustomStatus);
        spComboBox3.setEnabled(spCustomStatus);

        if (spRandomizeStarterHeldItemsCheckBox.isSelected()) {
            spBanBadItemsCheckBox.setEnabled(true);
        } else {
            spBanBadItemsCheckBox.setEnabled(false);
            spBanBadItemsCheckBox.setSelected(false);
        }

        if (spUnchangedRadioButton.isSelected() || spCustomRadioButton.isSelected()) {
            spTypeNoneRadioButton.setSelected(true);
            spTypeNoneRadioButton.setEnabled(false);
            spTypeFwgRadioButton.setEnabled(false);
            spTypeTriangleRadioButton.setEnabled(false);
            spTypeUniqueRadioButton.setEnabled(false);
            spTypeSingleRadioButton.setEnabled(false);
            spTypeNoDualCheckbox.setSelected(false);
            spTypeNoDualCheckbox.setEnabled(false);
            spAllowAltFormesCheckBox.setEnabled(false);
            spAllowAltFormesCheckBox.setSelected(false);
            spNoLegendariesCheckBox.setEnabled(false);
            spNoLegendariesCheckBox.setSelected(false);
        } else {
            spTypeNoneRadioButton.setEnabled(true);
            spTypeFwgRadioButton.setEnabled(true);
            spTypeTriangleRadioButton.setEnabled(true);
            spTypeUniqueRadioButton.setEnabled(true);
            spTypeSingleRadioButton.setEnabled(true);
            spTypeNoDualCheckbox.setEnabled(true);
            spAllowAltFormesCheckBox.setEnabled(true);
            spNoLegendariesCheckBox.setEnabled(true);
        }

        if(spTypeSingleRadioButton.isSelected()) {
            spTypeSingleComboBox.setEnabled(true);
        } else {
            spTypeSingleComboBox.setEnabled(false);
        }

        if (stpUnchangedRadioButton.isSelected()) {
            stpRandomize600BSTCheckBox.setEnabled(false);
            stpRandomize600BSTCheckBox.setSelected(false);
            stpAllowAltFormesCheckBox.setEnabled(false);
            stpAllowAltFormesCheckBox.setSelected(false);
            stpSwapMegaEvosCheckBox.setEnabled(false);
            stpSwapMegaEvosCheckBox.setSelected(false);
            stpFixMusicCheckBox.setEnabled(false);
            stpFixMusicCheckBox.setSelected(false);
        } else {
            stpRandomize600BSTCheckBox.setEnabled(true);
            stpAllowAltFormesCheckBox.setEnabled(true);
            stpSwapMegaEvosCheckBox.setEnabled(true);
            stpFixMusicCheckBox.setEnabled(true);
        }

        if (stpRandomSimilarStrengthRadioButton.isSelected()) {
            stpLimitMainGameLegendariesCheckBox.setEnabled(stpLimitMainGameLegendariesCheckBox.isVisible());
        } else {
            stpLimitMainGameLegendariesCheckBox.setEnabled(false);
            stpLimitMainGameLegendariesCheckBox.setSelected(false);
        }

        if (stpPercentageLevelModifierCheckBox.isSelected()) {
            stpPercentageLevelModifierSlider.setEnabled(true);
        } else {
            stpPercentageLevelModifierSlider.setEnabled(false);
            stpPercentageLevelModifierSlider.setValue(0);
        }

        if (igtUnchangedRadioButton.isSelected()) {
            igtRandomizeItemsCheckBox.setEnabled(false);
            igtRandomizeItemsCheckBox.setSelected(false);
            igtRandomizeIVsCheckBox.setEnabled(false);
            igtRandomizeIVsCheckBox.setSelected(false);
            igtRandomizeNicknamesCheckBox.setEnabled(false);
            igtRandomizeNicknamesCheckBox.setSelected(false);
            igtRandomizeOTsCheckBox.setEnabled(false);
            igtRandomizeOTsCheckBox.setSelected(false);
        } else {
            igtRandomizeItemsCheckBox.setEnabled(true);
            igtRandomizeIVsCheckBox.setEnabled(true);
            igtRandomizeNicknamesCheckBox.setEnabled(true);
            igtRandomizeOTsCheckBox.setEnabled(true);
        }

        if (mdUpdateMovesCheckBox.isSelected()) {
            mdUpdateComboBox.setEnabled(true);
        } else {
            mdUpdateComboBox.setEnabled(false);
        }

        if (pmsMetronomeOnlyModeRadioButton.isSelected() || pmsUnchangedRadioButton.isSelected()) {
            pmsGuaranteedLevel1MovesCheckBox.setEnabled(false);
            pmsGuaranteedLevel1MovesCheckBox.setSelected(false);
            pmsForceGoodDamagingCheckBox.setEnabled(false);
            pmsForceGoodDamagingCheckBox.setSelected(false);
            pmsReorderDamagingMovesCheckBox.setEnabled(false);
            pmsReorderDamagingMovesCheckBox.setSelected(false);
            pmsNoGameBreakingMovesCheckBox.setEnabled(false);
            pmsNoGameBreakingMovesCheckBox.setSelected(false);
            pmsEvolutionMovesCheckBox.setEnabled(false);
            pmsEvolutionMovesCheckBox.setSelected(false);
        } else {
            pmsGuaranteedLevel1MovesCheckBox.setEnabled(true);
            pmsForceGoodDamagingCheckBox.setEnabled(true);
            pmsReorderDamagingMovesCheckBox.setEnabled(true);
            pmsNoGameBreakingMovesCheckBox.setEnabled(true);
            pmsEvolutionMovesCheckBox.setEnabled(true);
        }

        if (pmsGuaranteedLevel1MovesCheckBox.isSelected()) {
            pmsGuaranteedLevel1MovesSlider.setEnabled(true);
        } else {
            pmsGuaranteedLevel1MovesSlider.setEnabled(false);
            pmsGuaranteedLevel1MovesSlider.setValue(pmsGuaranteedLevel1MovesSlider.getMinimum());
        }

        if (pmsForceGoodDamagingCheckBox.isSelected()) {
            pmsForceGoodDamagingSlider.setEnabled(true);
        } else {
            pmsForceGoodDamagingSlider.setEnabled(false);
            pmsForceGoodDamagingSlider.setValue(pmsForceGoodDamagingSlider.getMinimum());
        }

        if (isTrainerSetting(TRAINER_UNCHANGED)) {
            tpSimilarStrengthCheckBox.setEnabled(false);
            tpSimilarStrengthCheckBox.setSelected(false);
            tpDontUseLegendariesCheckBox.setEnabled(false);
            tpDontUseLegendariesCheckBox.setSelected(false);
            tpUseLocalPokemonCheckBox.setEnabled(false);
            tpUseLocalPokemonCheckBox.setSelected(false);
            tpNoEarlyWonderGuardCheckBox.setEnabled(false);
            tpNoEarlyWonderGuardCheckBox.setSelected(false);
            tpAllowAlternateFormesCheckBox.setEnabled(false);
            tpAllowAlternateFormesCheckBox.setSelected(false);
            tpSwapMegaEvosCheckBox.setEnabled(false);
            tpSwapMegaEvosCheckBox.setSelected(false);
            tpRandomShinyTrainerPokemonCheckBox.setEnabled(false);
            tpRandomShinyTrainerPokemonCheckBox.setSelected(false);
            tpDoubleBattleModeCheckBox.setEnabled(false);
            tpDoubleBattleModeCheckBox.setSelected(false);
            tpBossTrainersCheckBox.setEnabled(false);
            tpBossTrainersCheckBox.setSelected(false);
            tpImportantTrainersCheckBox.setEnabled(false);
            tpImportantTrainersCheckBox.setSelected(false);
            tpRegularTrainersCheckBox.setEnabled(false);
            tpRegularTrainersCheckBox.setSelected(false);
            tpBossTrainersItemsCheckBox.setEnabled(false);
            tpBossTrainersItemsCheckBox.setSelected(false);
            tpImportantTrainersItemsCheckBox.setEnabled(false);
            tpImportantTrainersItemsCheckBox.setSelected(false);
            tpRegularTrainersItemsCheckBox.setEnabled(false);
            tpRegularTrainersItemsCheckBox.setSelected(false);
            tpConsumableItemsOnlyCheckBox.setEnabled(false);
            tpConsumableItemsOnlyCheckBox.setSelected(false);
            tpSensibleItemsCheckBox.setEnabled(false);
            tpSensibleItemsCheckBox.setSelected(false);
            tpHighestLevelGetsItemCheckBox.setEnabled(false);
            tpHighestLevelGetsItemCheckBox.setSelected(false);
            tpEliteFourUniquePokemonCheckBox.setEnabled(false);
            tpEliteFourUniquePokemonCheckBox.setSelected(false);
        } else {
            tpSimilarStrengthCheckBox.setEnabled(true);
            tpDontUseLegendariesCheckBox.setEnabled(true);
            tpUseLocalPokemonCheckBox.setEnabled(true);
            tpNoEarlyWonderGuardCheckBox.setEnabled(true);
            tpAllowAlternateFormesCheckBox.setEnabled(true);
            if (currentRestrictions == null || currentRestrictions.allowTrainerSwapMegaEvolvables(
                    romHandler.forceSwapStaticMegaEvos(), isTrainerSetting(TRAINER_TYPE_THEMED))) {
                tpSwapMegaEvosCheckBox.setEnabled(true);
            } else {
                tpSwapMegaEvosCheckBox.setEnabled(false);
                tpSwapMegaEvosCheckBox.setSelected(false);
            }
            tpRandomShinyTrainerPokemonCheckBox.setEnabled(true);
            tpDoubleBattleModeCheckBox.setEnabled(tpDoubleBattleModeCheckBox.isVisible());
            tpBossTrainersCheckBox.setEnabled(tpBossTrainersCheckBox.isVisible());
            tpImportantTrainersCheckBox.setEnabled(tpImportantTrainersCheckBox.isVisible());
            tpRegularTrainersCheckBox.setEnabled(tpRegularTrainersCheckBox.isVisible());
            tpBossTrainersItemsCheckBox.setEnabled(tpBossTrainersItemsCheckBox.isVisible());
            tpImportantTrainersItemsCheckBox.setEnabled(tpImportantTrainersItemsCheckBox.isVisible());
            tpRegularTrainersItemsCheckBox.setEnabled(tpRegularTrainersItemsCheckBox.isVisible());
            tpEliteFourUniquePokemonCheckBox.setEnabled(tpEliteFourUniquePokemonCheckBox.isVisible());
        }

        if (tpForceFullyEvolvedAtCheckBox.isSelected()) {
            tpForceFullyEvolvedAtSlider.setEnabled(true);
        } else {
            tpForceFullyEvolvedAtSlider.setEnabled(false);
            tpForceFullyEvolvedAtSlider.setValue(tpForceFullyEvolvedAtSlider.getMinimum());
        }

        if (tpPercentageLevelModifierCheckBox.isSelected()) {
            tpPercentageLevelModifierSlider.setEnabled(true);
        } else {
            tpPercentageLevelModifierSlider.setEnabled(false);
            tpPercentageLevelModifierSlider.setValue(0);
        }

        if (tpBossTrainersCheckBox.isSelected()) {
            tpBossTrainersSpinner.setEnabled(true);
        } else {
            tpBossTrainersSpinner.setEnabled(false);
            tpBossTrainersSpinner.setValue(1);
        }

        if (tpImportantTrainersCheckBox.isSelected()) {
            tpImportantTrainersSpinner.setEnabled(true);
        } else {
            tpImportantTrainersSpinner.setEnabled(false);
            tpImportantTrainersSpinner.setValue(1);
        }

        if (tpRegularTrainersCheckBox.isSelected()) {
            tpRegularTrainersSpinner.setEnabled(true);
        } else {
            tpRegularTrainersSpinner.setEnabled(false);
            tpRegularTrainersSpinner.setValue(1);
        }

        if (tpBossTrainersItemsCheckBox.isSelected() || tpImportantTrainersItemsCheckBox.isSelected() ||
                tpRegularTrainersItemsCheckBox.isSelected()) {
            tpConsumableItemsOnlyCheckBox.setEnabled(true);
            tpSensibleItemsCheckBox.setEnabled(true);
            tpHighestLevelGetsItemCheckBox.setEnabled(true);
        } else {
            tpConsumableItemsOnlyCheckBox.setEnabled(false);
            tpSensibleItemsCheckBox.setEnabled(false);
            tpHighestLevelGetsItemCheckBox.setEnabled(false);
        }

        if (!peRandomEveryLevelRadioButton.isSelected() && (!spUnchangedRadioButton.isSelected() || !isTrainerSetting(TRAINER_UNCHANGED))) {
            tpRivalCarriesStarterCheckBox.setEnabled(true);
        } else {
            tpRivalCarriesStarterCheckBox.setEnabled(false);
            tpRivalCarriesStarterCheckBox.setSelected(false);
        }

        if (isTrainerSetting(TRAINER_TYPE_THEMED)) {
            tpWeightTypesCheckBox.setEnabled(true);
        } else {
            tpWeightTypesCheckBox.setEnabled(false);
            tpWeightTypesCheckBox.setSelected(false);
        }

        if (tpEliteFourUniquePokemonCheckBox.isSelected()) {
            tpEliteFourUniquePokemonSpinner.setEnabled(true);
        } else {
            tpEliteFourUniquePokemonSpinner.setEnabled(false);
            tpEliteFourUniquePokemonSpinner.setValue(1);
        }

        if (!totpUnchangedRadioButton.isSelected() || !totpAllyUnchangedRadioButton.isSelected()) {
            totpAllowAltFormesCheckBox.setEnabled(true);
        } else {
            totpAllowAltFormesCheckBox.setEnabled(false);
            totpAllowAltFormesCheckBox.setSelected(false);
        }

        if (totpPercentageLevelModifierCheckBox.isSelected()) {
            totpPercentageLevelModifierSlider.setEnabled(true);
        } else {
            totpPercentageLevelModifierSlider.setEnabled(false);
            totpPercentageLevelModifierSlider.setValue(0);
        }

        if (wpUnchangedRadioButton.isSelected() || wpGlobal1To1RadioButton.isSelected()) {
            wpTRNoneRadioButton.setSelected(true);
            wpTRNoneRadioButton.setEnabled(false);
            wpTRThemedAreasRadioButton.setSelected(false);
            wpTRThemedAreasRadioButton.setEnabled(false);
            wpTRKeepPrimaryRadioButton.setSelected(false);
            wpTRKeepPrimaryRadioButton.setEnabled(false);
            wpTRKeepThemesCheckBox.setEnabled(false);
            wpTRKeepThemesCheckBox.setSelected(false);
        } else {
            wpTRNoneRadioButton.setEnabled(true);
            wpTRThemedAreasRadioButton.setEnabled(true);
            wpTRKeepPrimaryRadioButton.setEnabled(true);
            if (wpTRKeepPrimaryRadioButton.isSelected()) {
                wpTRKeepThemesCheckBox.setEnabled(false);
                wpTRKeepThemesCheckBox.setSelected(false);
            } else {
                wpTRKeepThemesCheckBox.setEnabled(true);
            }
        }

        if (wpRandomRadioButton.isSelected()) {
            wpSimilarStrengthCheckBox.setEnabled(true);
            wpCatchEmAllModeCheckBox.setEnabled(true);
        } else if (wpArea1To1RadioButton.isSelected()) {
            wpSimilarStrengthCheckBox.setEnabled(true);
            wpCatchEmAllModeCheckBox.setEnabled(true);
        } else if (wpLocation1To1RadioButton.isSelected()) {
            wpSimilarStrengthCheckBox.setEnabled(true);
            wpCatchEmAllModeCheckBox.setEnabled(true);
        } else if (wpGlobal1To1RadioButton.isSelected()) {
            wpSimilarStrengthCheckBox.setEnabled(true);
            wpCatchEmAllModeCheckBox.setEnabled(false);
            wpCatchEmAllModeCheckBox.setSelected(false);
        } else {
            wpSimilarStrengthCheckBox.setEnabled(false);
            wpSimilarStrengthCheckBox.setSelected(false);
            wpCatchEmAllModeCheckBox.setEnabled(false);
            wpCatchEmAllModeCheckBox.setSelected(false);
        }

        if (wpUnchangedRadioButton.isSelected()) {
            wpUseTimeBasedEncountersCheckBox.setEnabled(false);
            wpUseTimeBasedEncountersCheckBox.setSelected(false);
            wpDontUseLegendariesCheckBox.setEnabled(false);
            wpDontUseLegendariesCheckBox.setSelected(false);
            wpAllowAltFormesCheckBox.setEnabled(false);
            wpAllowAltFormesCheckBox.setSelected(false);
        } else {
            wpUseTimeBasedEncountersCheckBox.setEnabled(true);
            wpDontUseLegendariesCheckBox.setEnabled(true);
            wpAllowAltFormesCheckBox.setEnabled(true);
        }

        if (wpSimilarStrengthCheckBox.isSelected() && !wpGlobal1To1RadioButton.isSelected()) {
            wpBalanceShakingGrassPokemonCheckBox.setEnabled(true);
        } else {
            wpBalanceShakingGrassPokemonCheckBox.setEnabled(false);
            wpBalanceShakingGrassPokemonCheckBox.setSelected(false);
        }

        if (wpRandomizeHeldItemsCheckBox.isSelected()
                && wpRandomizeHeldItemsCheckBox.isVisible()
                && wpRandomizeHeldItemsCheckBox.isEnabled()) { // ??? why all three
            wpBanBadItemsCheckBox.setEnabled(true);
        } else {
            wpBanBadItemsCheckBox.setEnabled(false);
            wpBanBadItemsCheckBox.setSelected(false);
        }

        if (wpSetMinimumCatchRateCheckBox.isSelected()) {
            wpSetMinimumCatchRateSlider.setEnabled(true);
        } else {
            wpSetMinimumCatchRateSlider.setEnabled(false);
            wpSetMinimumCatchRateSlider.setValue(0);
        }

        if (wpPercentageLevelModifierCheckBox.isSelected()) {
            wpPercentageLevelModifierSlider.setEnabled(true);
        } else {
            wpPercentageLevelModifierSlider.setEnabled(false);
            wpPercentageLevelModifierSlider.setValue(0);
        }

        if (pmsMetronomeOnlyModeRadioButton.isSelected()) {
            tmUnchangedRadioButton.setEnabled(false);
            tmRandomRadioButton.setEnabled(false);
            tmUnchangedRadioButton.setSelected(true);

            mtUnchangedRadioButton.setEnabled(false);
            mtRandomRadioButton.setEnabled(false);
            mtUnchangedRadioButton.setSelected(true);

            tmLevelupMoveSanityCheckBox.setEnabled(false);
            tmLevelupMoveSanityCheckBox.setSelected(false);
            tmKeepFieldMoveTMsCheckBox.setEnabled(false);
            tmKeepFieldMoveTMsCheckBox.setSelected(false);
            tmForceGoodDamagingCheckBox.setEnabled(false);
            tmForceGoodDamagingCheckBox.setSelected(false);
            tmNoGameBreakingMovesCheckBox.setEnabled(false);
            tmNoGameBreakingMovesCheckBox.setSelected(false);
            tmFollowEvolutionsCheckBox.setEnabled(false);
            tmFollowEvolutionsCheckBox.setSelected(false);

            mtLevelupMoveSanityCheckBox.setEnabled(false);
            mtLevelupMoveSanityCheckBox.setSelected(false);
            mtKeepFieldMoveTutorsCheckBox.setEnabled(false);
            mtKeepFieldMoveTutorsCheckBox.setSelected(false);
            mtForceGoodDamagingCheckBox.setEnabled(false);
            mtForceGoodDamagingCheckBox.setSelected(false);
            mtNoGameBreakingMovesCheckBox.setEnabled(false);
            mtNoGameBreakingMovesCheckBox.setSelected(false);
            mtFollowEvolutionsCheckBox.setEnabled(false);
            mtFollowEvolutionsCheckBox.setSelected(false);
        } else {
            tmUnchangedRadioButton.setEnabled(true);
            tmRandomRadioButton.setEnabled(true);

            mtUnchangedRadioButton.setEnabled(true);
            mtRandomRadioButton.setEnabled(true);

            if (!(pmsUnchangedRadioButton.isSelected()) || !(tmUnchangedRadioButton.isSelected())
                    || !(thcUnchangedRadioButton.isSelected())) {
                tmLevelupMoveSanityCheckBox.setEnabled(true);
            } else {
                tmLevelupMoveSanityCheckBox.setEnabled(false);
                tmLevelupMoveSanityCheckBox.setSelected(false);
            }

            if ((!thcUnchangedRadioButton.isSelected()) || (tmLevelupMoveSanityCheckBox.isSelected())) {
                tmFollowEvolutionsCheckBox.setEnabled(followEvolutionControlsEnabled);
            }
            else {
                tmFollowEvolutionsCheckBox.setEnabled(false);
                tmFollowEvolutionsCheckBox.setSelected(false);
            }

            if (!(tmUnchangedRadioButton.isSelected())) {
                tmKeepFieldMoveTMsCheckBox.setEnabled(true);
                tmForceGoodDamagingCheckBox.setEnabled(true);
                tmNoGameBreakingMovesCheckBox.setEnabled(true);
            } else {
                tmKeepFieldMoveTMsCheckBox.setEnabled(false);
                tmKeepFieldMoveTMsCheckBox.setSelected(false);
                tmForceGoodDamagingCheckBox.setEnabled(false);
                tmForceGoodDamagingCheckBox.setSelected(false);
                tmNoGameBreakingMovesCheckBox.setEnabled(false);
                tmNoGameBreakingMovesCheckBox.setSelected(false);
            }

            if (romHandler.hasMoveTutors()
                    && (!(pmsUnchangedRadioButton.isSelected()) || !(mtUnchangedRadioButton.isSelected())
                    || !(mtcUnchangedRadioButton.isSelected()))) {
                mtLevelupMoveSanityCheckBox.setEnabled(true);
            } else {
                mtLevelupMoveSanityCheckBox.setEnabled(false);
                mtLevelupMoveSanityCheckBox.setSelected(false);
            }

            if (!(mtcUnchangedRadioButton.isSelected()) || (mtLevelupMoveSanityCheckBox.isSelected())) {
                mtFollowEvolutionsCheckBox.setEnabled(followEvolutionControlsEnabled);
            }
            else {
                mtFollowEvolutionsCheckBox.setEnabled(false);
                mtFollowEvolutionsCheckBox.setSelected(false);
            }

            if (romHandler.hasMoveTutors() && !(mtUnchangedRadioButton.isSelected())) {
                mtKeepFieldMoveTutorsCheckBox.setEnabled(true);
                mtForceGoodDamagingCheckBox.setEnabled(true);
                mtNoGameBreakingMovesCheckBox.setEnabled(true);
            } else {
                mtKeepFieldMoveTutorsCheckBox.setEnabled(false);
                mtKeepFieldMoveTutorsCheckBox.setSelected(false);
                mtForceGoodDamagingCheckBox.setEnabled(false);
                mtForceGoodDamagingCheckBox.setSelected(false);
                mtNoGameBreakingMovesCheckBox.setEnabled(false);
                mtNoGameBreakingMovesCheckBox.setSelected(false);
            }
        }

        if (tmForceGoodDamagingCheckBox.isSelected()) {
            tmForceGoodDamagingSlider.setEnabled(true);
        } else {
            tmForceGoodDamagingSlider.setEnabled(false);
            tmForceGoodDamagingSlider.setValue(tmForceGoodDamagingSlider.getMinimum());
        }

        if (mtForceGoodDamagingCheckBox.isSelected()) {
            mtForceGoodDamagingSlider.setEnabled(true);
        } else {
            mtForceGoodDamagingSlider.setEnabled(false);
            mtForceGoodDamagingSlider.setValue(mtForceGoodDamagingSlider.getMinimum());
        }

        tmFullHMCompatibilityCheckBox.setEnabled(!thcFullCompatibilityRadioButton.isSelected());

        if (fiRandomRadioButton.isSelected() && fiRandomRadioButton.isVisible() && fiRandomRadioButton.isEnabled()) {
            fiBanBadItemsCheckBox.setEnabled(true);
        } else if (fiRandomEvenDistributionRadioButton.isSelected() && fiRandomEvenDistributionRadioButton.isVisible()
                && fiRandomEvenDistributionRadioButton.isEnabled()) {
            fiBanBadItemsCheckBox.setEnabled(true);
        } else {
            fiBanBadItemsCheckBox.setEnabled(false);
            fiBanBadItemsCheckBox.setSelected(false);
        }

        if (shRandomRadioButton.isSelected() && shRandomRadioButton.isVisible() && shRandomRadioButton.isEnabled()) {
            shBanBadItemsCheckBox.setEnabled(true);
            shBanRegularShopItemsCheckBox.setEnabled(true);
            shBanOverpoweredShopItemsCheckBox.setEnabled(true);
            shBalanceShopItemPricesCheckBox.setEnabled(true);
            shGuaranteeEvolutionItemsCheckBox.setEnabled(true);
            shGuaranteeXItemsCheckBox.setEnabled(true);
        } else {
            shBanBadItemsCheckBox.setEnabled(false);
            shBanBadItemsCheckBox.setSelected(false);
            shBanRegularShopItemsCheckBox.setEnabled(false);
            shBanRegularShopItemsCheckBox.setSelected(false);
            shBanOverpoweredShopItemsCheckBox.setEnabled(false);
            shBanOverpoweredShopItemsCheckBox.setSelected(false);
            shBalanceShopItemPricesCheckBox.setEnabled(false);
            shBalanceShopItemPricesCheckBox.setSelected(false);
            shGuaranteeEvolutionItemsCheckBox.setEnabled(false);
            shGuaranteeEvolutionItemsCheckBox.setSelected(false);
            shGuaranteeXItemsCheckBox.setEnabled(false);
            shGuaranteeXItemsCheckBox.setSelected(false);
        }

        if (puRandomRadioButton.isSelected() && puRandomRadioButton.isVisible() && puRandomRadioButton.isEnabled()) {
            puBanBadItemsCheckBox.setEnabled(true);
        } else {
            puBanBadItemsCheckBox.setEnabled(false);
            puBanBadItemsCheckBox.setSelected(false);
        }

        if (teInverseRadioButton.isSelected()) {
            teAddRandomImmunitiesCheckBox.setEnabled(true);
        } else {
            teAddRandomImmunitiesCheckBox.setEnabled(false);
            teAddRandomImmunitiesCheckBox.setSelected(false);
        }

        if (ppalRandomRadioButton.isSelected() && ppalRandomRadioButton.isVisible() &&
                ppalRandomRadioButton.isEnabled()) {
            ppalFollowTypesCheckBox.setEnabled(true);
            ppalFollowEvolutionsCheckBox.setEnabled(true);
            ppalShinyFromNormalCheckBox.setEnabled(true);
        } else {
            ppalFollowTypesCheckBox.setEnabled(false);
            ppalFollowTypesCheckBox.setSelected(false);
            ppalFollowEvolutionsCheckBox.setEnabled(false);
            ppalFollowEvolutionsCheckBox.setSelected(false);
            ppalShinyFromNormalCheckBox.setEnabled(false);
            ppalShinyFromNormalCheckBox.setSelected(false);
        }

        if (cpgCustomRadioButton.isSelected() && cpgCustomRadioButton.isVisible() && cpgCustomRadioButton.isEnabled()) {
            cpgComboBox.setEnabled(true);
            cpgRandomButton.setEnabled(true);
            cpgCustomInfo.setEnabled(true);
            cpgReplaceLabel.setEnabled(true);
            cpgReplaceRadioButton1.setEnabled(true);
            cpgReplaceRadioButton2.setEnabled(true);
        } else {
            cpgComboBox.setEnabled(false);
            cpgRandomButton.setEnabled(false);
            cpgCustomInfo.setEnabled(false);
            cpgReplaceLabel.setEnabled(false);
            cpgReplaceRadioButton1.setEnabled(false);
            cpgReplaceRadioButton2.setEnabled(false);
        }
    }

    private void initTweaksPanel() {
        tweakCheckBoxes = new ArrayList<>();
        int numTweaks = MiscTweak.allTweaks.size();
        for (int i = 0; i < numTweaks; i++) {
            MiscTweak ct = MiscTweak.allTweaks.get(i);
            JCheckBox tweakBox = new JCheckBox();
            tweakBox.setText(ct.getTweakName());
            tweakBox.setToolTipText(ct.getTooltipText());
            tweakCheckBoxes.add(tweakBox);
        }
    }

    private void makeTweaksLayout(List<JCheckBox> tweaks) {
        liveTweaksPanel = new JPanel(new GridBagLayout());
        TitledBorder border = BorderFactory.createTitledBorder("Misc. Tweaks");
        border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD));
        liveTweaksPanel.setBorder(border);

        int numTweaks = tweaks.size();
        Iterator<JCheckBox> tweaksIterator = tweaks.iterator();

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.insets = new Insets(5,5,0,5);

        int TWEAK_COLS = 4;
        int numCols = Math.min(TWEAK_COLS, numTweaks);

        for (int row = 0; row <= numTweaks / numCols; row++) {
            for (int col = 0; col < numCols; col++) {
                if (!tweaksIterator.hasNext()) break;
                c.gridx = col;
                c.gridy = row;
                liveTweaksPanel.add(tweaksIterator.next(),c);
            }
        }

        // Pack the checkboxes together

        GridBagConstraints horizontalC = new GridBagConstraints();
        horizontalC.gridx = numCols;
        horizontalC.gridy = 0;
        horizontalC.weightx = 0.1;

        GridBagConstraints verticalC = new GridBagConstraints();
        verticalC.gridx = 0;
        verticalC.gridy = (numTweaks / numCols) + 1;
        verticalC.weighty = 0.1;

        liveTweaksPanel.add(new JSeparator(SwingConstants.HORIZONTAL),horizontalC);
        liveTweaksPanel.add(new JSeparator(SwingConstants.VERTICAL),verticalC);
    }

    private void populateDropdowns() {
        List<Pokemon> currentStarters = romHandler.getStarters();
        List<Pokemon> allPokes =
                romHandler.generationOfPokemon() >= 6 ?
                        romHandler.getPokemonInclFormes()
                                .stream()
                                .filter(pk -> pk == null || !pk.isCosmeticForme())
                                .collect(Collectors.toList()) :
                        romHandler.getPokemon();
        String[] pokeNames = new String[allPokes.size()];
        pokeNames[0] = "Random";
        for (int i = 1; i < allPokes.size(); i++) {
            pokeNames[i] = allPokes.get(i).fullName();

        }

        spComboBox1.setModel(new DefaultComboBoxModel<>(pokeNames));
        spComboBox1.setSelectedIndex(allPokes.indexOf(currentStarters.get(0)));
        spComboBox2.setModel(new DefaultComboBoxModel<>(pokeNames));
        spComboBox2.setSelectedIndex(allPokes.indexOf(currentStarters.get(1)));
        if (!romHandler.isYellow()) {
            spComboBox3.setModel(new DefaultComboBoxModel<>(pokeNames));
            spComboBox3.setSelectedIndex(allPokes.indexOf(currentStarters.get(2)));
        }

        int numTypes = romHandler.getTypeTable().getTypes().size();
        String[] typeNames = new String[numTypes + 1];
        typeNames[0] = "Random";
        for (int i = 1; i <= numTypes; i++) {
            typeNames[i] = Type.fromInt(i-1).toString();
        }
        spTypeSingleComboBox.setModel(new DefaultComboBoxModel<>(typeNames));

        String[] baseStatGenerationNumbers = new String[Math.min(4, GlobalConstants.HIGHEST_POKEMON_GEN - romHandler.generationOfPokemon())];
        int j = Math.max(6, romHandler.generationOfPokemon() + 1);
        for (int i = 0; i < baseStatGenerationNumbers.length; i++) {
            baseStatGenerationNumbers[i] = String.valueOf(j);
            j++;
        }
        pbsUpdateComboBox.setModel(new DefaultComboBoxModel<>(baseStatGenerationNumbers));

        String[] moveGenerationNumbers = new String[GlobalConstants.HIGHEST_POKEMON_GEN - romHandler.generationOfPokemon()];
        j = romHandler.generationOfPokemon() + 1;
        for (int i = 0; i < moveGenerationNumbers.length; i++) {
            moveGenerationNumbers[i] = String.valueOf(j);
            j++;
        }
        mdUpdateComboBox.setModel(new DefaultComboBoxModel<>(moveGenerationNumbers));


        tpComboBox.setModel(new DefaultComboBoxModel<>(getTrainerSettingsForGeneration(romHandler.generationOfPokemon())));
        tpComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JComponent comp = (JComponent) super.getListCellRendererComponent(list,
                        value, index, isSelected, cellHasFocus);

                if (index >= 0 && value != null) {
                    list.setToolTipText(bundle.getString(trainerSettingToolTips.get(trainerSettings.indexOf(value))));
                }
                return comp;
            }
        });
    }

    private ImageIcon makeMascotIcon() {
        try {
            BufferedImage handlerImg = new MascotGetter(RND).getMascotImage(romHandler);

            if (handlerImg == null) {
                return emptyIcon;
            }

            BufferedImage nImg = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            int hW = handlerImg.getWidth();
            int hH = handlerImg.getHeight();
            nImg.getGraphics().drawImage(handlerImg, 64 - hW / 2, 64 - hH / 2, frame);
            return new ImageIcon(nImg);
        } catch (Exception ex) {
            return emptyIcon;
        }
    }

    private void checkCustomNames() {
        String[] cnamefiles = new String[] { SysConstants.tnamesFile, SysConstants.tclassesFile,
                SysConstants.nnamesFile };

        boolean foundFile = false;
        for (int file = 0; file < 3; file++) {
            File currentFile = new File(SysConstants.ROOT_PATH + cnamefiles[file]);
            if (currentFile.exists()) {
                foundFile = true;
                break;
            }
        }

        if (foundFile) {
            int response = JOptionPane.showConfirmDialog(frame,
                    bundle.getString("GUI.convertNameFilesDialog.text"),
                    bundle.getString("GUI.convertNameFilesDialog.title"), JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                try {
                    CustomNamesSet newNamesData = CustomNamesSet.importOldNames();
                    byte[] data = newNamesData.getBytes();
                    FileFunctions.writeBytesToFile(SysConstants.customNamesFile, data);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, bundle.getString("GUI.convertNameFilesFailed"));
                }
            }

            haveCheckedCustomNames = true;
            attemptWriteConfig();
        }

    }

    private void attemptReadConfig() {
        // Things that should be true by default should be manually set here
        unloadGameOnSuccess = true;
        batchRandomizationSettings = new BatchRandomizationSettings();
        File fh = new File(SysConstants.ROOT_PATH + "config.ini");
        if (!fh.exists() || !fh.canRead()) {
            return;
        }

        try {
            Scanner sc = new Scanner(fh, StandardCharsets.UTF_8);
            boolean isReadingUpdates = false;
            while (sc.hasNextLine()) {
                String q = sc.nextLine().trim();
                if (q.contains("//")) {
                    q = q.substring(0, q.indexOf("//")).trim();
                }
                if (q.equals("[Game Updates]")) {
                    isReadingUpdates = true;
                    continue;
                }
                if (!q.isEmpty()) {
                    String[] tokens = q.split("=", 2);
                    if (tokens.length == 2) {
                        String key = tokens[0].trim();
                        if (isReadingUpdates) {
                            gameUpdates.put(key, tokens[1]);
                        }
                        if (key.equalsIgnoreCase("checkedcustomnames172")) {
                            haveCheckedCustomNames = Boolean.parseBoolean(tokens[1].trim());
                        }
                        if (key.equals("firststart")) {
                            String val = tokens[1];
                            if (val.equals(Version.VERSION_STRING)) {
                                initialPopup = false;
                            }
                        }
                        if (key.equals("unloadgameonsuccess")) {
                            unloadGameOnSuccess = Boolean.parseBoolean(tokens[1].trim());
                        }
                        if (key.equals("showinvalidrompopup")) {
                            showInvalidRomPopup = Boolean.parseBoolean(tokens[1].trim());
                        }
                        if (key.equals("batchrandomization.enabled")) {
                            batchRandomizationSettings.setBatchRandomizationEnabled(Boolean.parseBoolean(tokens[1].trim()));
                        }
                        if (key.equals("batchrandomization.generatelogfiles")){
                            batchRandomizationSettings.setGenerateLogFile(Boolean.parseBoolean(tokens[1].trim()));
                        }
                        if (key.equals("batchrandomization.autoadvanceindex")){
                            batchRandomizationSettings.setAutoAdvanceStartingIndex(Boolean.parseBoolean(tokens[1].trim()));
                        }
                        if (key.equals("batchrandomization.numberofrandomizedroms")){
                            batchRandomizationSettings.setNumberOfRandomizedROMs(Integer.parseInt(tokens[1].trim()));
                        }
                        if (key.equals("batchrandomization.startingindex")){
                            batchRandomizationSettings.setStartingIndex(Integer.parseInt(tokens[1].trim()));
                        }
                        if (key.equals("batchrandomization.filenameprefix")){
                            batchRandomizationSettings.setFileNamePrefix(tokens[1].trim());
                        }
                        if (key.equals("batchrandomization.outputdirectory")){
                            batchRandomizationSettings.setOutputDirectory(tokens[1].trim());
                        }
                    }
                } else if (isReadingUpdates) {
                    isReadingUpdates = false;
                }
            }
            sc.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private boolean attemptWriteConfig() {
        File fh = new File(SysConstants.ROOT_PATH + "config.ini");
        if (fh.exists() && !fh.canWrite()) {
            return false;
        }

        try {
            PrintStream ps = new PrintStream(new FileOutputStream(fh), true, StandardCharsets.UTF_8);
            ps.println("checkedcustomnames=true");
            ps.println("checkedcustomnames172=" + haveCheckedCustomNames);
            ps.println("unloadgameonsuccess=" + unloadGameOnSuccess);
            ps.println("showinvalidrompopup=" + showInvalidRomPopup);
            ps.println(batchRandomizationSettings.toString());
            if (!initialPopup) {
                ps.println("firststart=" + Version.VERSION_STRING);
            }
            if (gameUpdates.size() > 0) {
                ps.println();
                ps.println("[Game Updates]");
                for (Map.Entry<String, String> update : gameUpdates.entrySet()) {
                    ps.format("%s=%s", update.getKey(), update.getValue());
                    ps.println();
                }
            }
            ps.close();
            return true;
        } catch (IOException e) {
            return false;
        }

    }

    private void testForRequiredConfigs() {
        try {
            Utils.testForRequiredConfigs();
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null,
                    String.format(bundle.getString("GUI.configFileMissing"), e.getMessage()));
            System.exit(1);
        }
    }

    private ExpCurve[] getEXPCurvesForGeneration(int generation) {
        ExpCurve[] result;
        if (generation < 3) {
            result = new ExpCurve[]{ ExpCurve.MEDIUM_FAST, ExpCurve.MEDIUM_SLOW, ExpCurve.FAST, ExpCurve.SLOW };
        } else {
            result = new ExpCurve[]{ ExpCurve.MEDIUM_FAST, ExpCurve.MEDIUM_SLOW, ExpCurve.FAST, ExpCurve.SLOW, ExpCurve.ERRATIC, ExpCurve.FLUCTUATING };
        }
        return result;
    }

    private String[] getTrainerSettingsForGeneration(int generation) {
        List<String> result = new ArrayList<>(trainerSettings);
        if (generation != 5) {
            result.remove(bundle.getString("GUI.tpMain3RandomEvenDistributionMainGame.text"));
        }
        return result.toArray(new String[0]);
    }

    private boolean isTrainerSetting(int setting) {
        return trainerSettings.indexOf(tpComboBox.getSelectedItem()) == setting;
    }

    public static void main(String[] args) {
        String firstCliArg = args.length > 0 ? args[0] : "";
        // invoke as CLI program
        if (firstCliArg.equals("cli")) {
            // snip the "cli" flag arg off the args array and invoke command
            String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
            int exitCode = CliRandomizer.invoke(commandArgs);
            System.exit(exitCode);
        } else {
            launcherInput = firstCliArg;
            if (launcherInput.equals("please-use-the-launcher")) usedLauncher = true;
            SwingUtilities.invokeLater(() -> {
                frame = new JFrame("RandomizerGUI");
                try {
                    String lafName = javax.swing.UIManager.getSystemLookAndFeelClassName();
                    // NEW: Only set Native LaF on windows.
                    if (lafName.equalsIgnoreCase("com.sun.java.swing.plaf.windows.WindowsLookAndFeel")) {
                        javax.swing.UIManager.setLookAndFeel(lafName);
                    }
                } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException ex) {
                    java.util.logging.Logger.getLogger(RandomizerGUI.class.getName()).log(java.util.logging.Level.SEVERE, null,
                            ex);
                }
                frame.setContentPane(new RandomizerGUI().mainPanel);
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            });
        }
    }
}
