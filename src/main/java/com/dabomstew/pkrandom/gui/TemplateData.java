package com.dabomstew.pkrandom.gui;

import java.io.Writer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import com.dabomstew.pkrandom.exceptions.RandomizerIOException;
import com.dabomstew.pkrandom.pokemon.Type;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

public class TemplateData {
    private static Map<String, Object> templateData = new HashMap<String, Object>();
    private static Configuration cfg = new Configuration(new Version("2.3.30"));
    private static Template template;
    private static boolean generateLog = true;
    private static List<Type> typeOrder = null;
    private static Exception templateError = null;

    static {
        cfg.setClassForTemplateLoading(TemplateData.class, "/com/dabomstew/pkrandom/gui/");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setBooleanFormat("c");
        cfg.setWhitespaceStripping(true);

        try {
            template = cfg.getTemplate("randomization_log.ftl");
        } catch (IOException e) {
            generateLog = false;
            templateError = e;
        }
    }

    public static void resetData() {
        templateData = new HashMap<String, Object>();
    }

    public static Object getData(String key) {
        return templateData.get(key);
    }

    public static void putData(String key, Object value) {
        templateData.put(key, value);
    }

    public static void putMap(String key1, String key2, Object value) {
        if (!templateData.containsKey(key1)) {
            putData(key1, new HashMap());
        }
        Map map = (Map) templateData.get(key1);
        map.put(key2, value);
    }

    public static void setGenerateTypeChartOrder(List<Type> typeOrder) {
        TemplateData.typeOrder = typeOrder;
    }

    public static void generateTypeChart() {
        if (typeOrder != null) {
            List<List<String>> typeChartRows = new ArrayList<List<String>>();

            // First row is the types
            // Start with a blank cell, then iterate through the given types
            List<String> row = new ArrayList<String>();
            row.add("");
            typeOrder.forEach(type -> row.add(type.toString()));
            typeChartRows.add(row);

            // Next rows are all the matchups
            for (Type rowType : typeOrder) {
                List<String> nextRow = new ArrayList<String>();
                nextRow.add(rowType.toString());
                for (Type columnType : typeOrder) {
                    List<Type> strong = Type.STRONG_AGAINST.get(columnType);
                    List<Type> resist = Type.RESISTANT_TO.get(rowType);
                    List<Type> immune = Type.IMMUNE_TO.get(rowType);
                    if (strong != null && strong.contains(rowType)) {
                        nextRow.add("SE");
                    } else if (resist != null && resist.contains(columnType)) {
                        nextRow.add("NE");
                    } else if (immune != null && immune.contains(columnType)) {
                        nextRow.add("ZE");
                    } else {
                        nextRow.add("E");
                    }
                }
                typeChartRows.add(nextRow);
            }

            putData("typeMatchups", typeChartRows);
        }
    }

    public static void generateTableOfContents() {
        List<String[]> toc = new ArrayList<String[]>();
        if (templateData.get("isModernMoves") != null
                && (Boolean) templateData.get("isModernMoves")) {
            toc.add(new String[] {"mm", "Move Modernization"});
        }
        if (((Map) templateData.get("tweakMap")).size() > 0) {
            toc.add(new String[] {"pa", "Patches Applied"});
        }
        if (templateData.get("shuffledTypes") != null) {
            toc.add(new String[] {"st", "Shuffled Types"});
        }
        Object fte = templateData.get("updateEffectiveness");
        if (fte != null && (Boolean) fte) {
            toc.add(new String[] {"fte", "Fixed Type Effectiveness"});
        }
        if (templateData.get("typeMatchups") != null) {
            toc.add(new String[] {"tmc", "Type Chart"});
        }
        if ((Boolean) templateData.get("logEvolutions") != null
                && (Boolean) templateData.get("logEvolutions")) {
            toc.add(new String[] {"re", "Randomized Evolutions"});
            toc.add(new String[] {"ep", "Evolution Paths"});
        }
        if ((Boolean) templateData.get("logPokemon") != null
                && (Boolean) templateData.get("logPokemon")) {
            toc.add(new String[] {"ps", "Pokemon Stats"});
        }
        if (templateData.get("removeTradeEvo") != null
                && ((List) templateData.get("removeTradeEvo")).size() > 0) {
            toc.add(new String[] {"rte", "Impossible Evos"});
        }
        if (templateData.get("condensedEvos") != null
                && ((TreeSet) templateData.get("condensedEvos")).size() > 0) {
            toc.add(new String[] {"cle", "Condensed Evos"});
        }
        if (templateData.get("logStarters") != null) {
            toc.add(new String[] {"rs", "Starters"});
        }
        if (templateData.get("logMoves") != null && (Boolean) templateData.get("logMoves")) {
            toc.add(new String[] {"md", "Move Data"});
        }
        if (templateData.get("gameBreakingMoves") != null
                && (Boolean) templateData.get("gameBreakingMoves")) {
            toc.add(new String[] {"gbm", "Game Breaking Moves"});
        }
        if (templateData.get("logPokemonMoves") != null) {
            toc.add(new String[] {"pm", "Pokemon Moves"});
        }
        if (templateData.get("originalTrainers") != null
                && ((List) templateData.get("originalTrainers")).size() > 0) {
            toc.add(new String[] {"tp", "Trainer Pokemon"});
        }
        if (templateData.get("staticPokemon") != null
                && ((Map) templateData.get("staticPokemon")).size() > 0) {
            toc.add(new String[] {"sp", "Static Pokemon"});
        }
        if (templateData.get("wildPokemon") != null
                && ((List) templateData.get("wildPokemon")).size() > 0) {
            toc.add(new String[] {"wp", "Wild Pokemon"});
        }
        if (templateData.get("logTMMoves") != null
                && !((String) templateData.get("logTMMoves")).isEmpty()) {
            toc.add(new String[] {"tm", "TM Moves"});
        }
        if (templateData.get("logTutorMoves") != null) {
            toc.add(new String[] {"mt", "Tutor Moves"});
        }
        if (templateData.get("oldTrades") != null
                && ((List) templateData.get("oldTrades")).size() > 0) {
            toc.add(new String[] {"igt", "In-Game Trades"});
        }

        putData("toc", toc);
    }

    public static void process(Writer writer) throws TemplateException, IOException {
        if (generateLog) {
            template.process(templateData, writer);
        } else {
            throw new RandomizerIOException(templateError);
        }
    }
}
