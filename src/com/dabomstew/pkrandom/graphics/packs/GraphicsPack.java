package com.dabomstew.pkrandom.graphics.packs;

import com.dabomstew.pkrandom.graphics.palettes.Palette;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class GraphicsPack implements Comparable<GraphicsPack> {

    private static final String NAME_TOKEN = "NAME";
    private static final String DESC_TOKEN = "DESCRIPTION";
    private static final String FROM_TOKEN = "FROM";
    private static final String ORIG_CREATOR_TOKEN = "CREATOR";
    private static final String ADAPTER_TOKEN = "ADAPTER";
    private static final String CATEGORY_TOKEN = "CATEGORY";

    private enum Category {
        POKEMON, GAMES, OTHER
    }

    private final String path;
    private String name;
    private String description;
    private String from;
    private String originalCreator;
    private String adapter = "N/A";
    private Category category;

    private static List<String> readFileLines(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            return br.lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GraphicsPack(String path) {
        this.path = path;
        List<String> lines = readFileLines(path + "/info.txt");
        String[][] tokens = splitIntoTokens(lines);
        parseTokens(tokens);
        checkVitalTokensAssigned();
    }

    private String[][] splitIntoTokens(List<String> lines) {
        List<String[]> tokens = new ArrayList<>();
        for (String line : lines) {
            Matcher tokenMatcher = Pattern.compile("\\[([^\\[\\]]*)]").matcher(line);
            tokenMatcher.useTransparentBounds(true);
            while (tokenMatcher.find()) {
                String[] token = tokenMatcher.group(1).split("(?<!\\\\):");
                for (int i = 0; i < token.length; i++) {
                    token[i] = token[i].replace("\\:", ":");
                }
                tokens.add(token);
            }
        }
        return tokens.toArray(new String[0][]);
    }

    private void parseTokens(String[][] tokens) {
        for (String[] token : tokens) {
            switch (token[0]) {
                case NAME_TOKEN -> this.name = token[1];
                case DESC_TOKEN -> this.description = token[1];
                case FROM_TOKEN -> this.from = token[1];
                case ORIG_CREATOR_TOKEN -> this.originalCreator = token[1];
                case ADAPTER_TOKEN -> this.adapter = token[1];
                case CATEGORY_TOKEN -> this.category = Category.valueOf(token[1]);
                default -> System.out.println("Undefined PlayerCharacterSprites token: " + token[0]);
            }
        }
    }

    private void checkVitalTokensAssigned() {
        boolean anyMissed = false;
        List<String> vitalMissed = new ArrayList<>();
        if (name == null) {
            vitalMissed.add(NAME_TOKEN);
            anyMissed = true;
        }
        if (description == null) {
            vitalMissed.add(DESC_TOKEN);
            anyMissed = true;
        }
        if (from == null) {
            vitalMissed.add(FROM_TOKEN);
            anyMissed = true;
        }
        if (originalCreator == null) {
            vitalMissed.add(ORIG_CREATOR_TOKEN);
            anyMissed = true;
        }
        if (category == null) {
            vitalMissed.add(CATEGORY_TOKEN);
            anyMissed = true;
        }
        if (anyMissed) {
            throw new RuntimeException("Could not initiate PlayerCharacterSprites object \"" + this + "\"; " +
                    "missing token(s): " + String.join(", ", vitalMissed));
        }
    }

    protected BufferedImage loadImage(String filename) {
        try {
            return ImageIO.read(new File( path + "/" + filename));
        } catch (IOException e) {
            return null;
        }
    }

    protected Palette loadPalette(String filename) {
        try {
            return Palette.readFromFile(new File(path + "/" + filename));
        } catch (IOException e) {
            return null;
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getFrom() {
        return from;
    }

    public String getOriginalCreator() {
        return originalCreator;
    }

    public String getAdapter() {
        return adapter;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(GraphicsPack o) {
        if (o == null) {
            throw new NullPointerException("null value not comparable.");
        }
        if (o.category != category) {
            return category.compareTo(o.category);
        }
        if (!o.from.equals(from)) {
            return from.compareTo(o.from);
        }
        if (!o.name.equals(name)) {
            return name.compareTo(o.name);
        }
        if (!o.originalCreator.equals(originalCreator)) {
            return originalCreator.compareTo(o.originalCreator);
        }
        return adapter.compareTo(o.adapter);
    }

    /**
     * Returns a sample of the images, to be used for UI purposes.
     */
    public abstract BufferedImage[] getSampleImages();

}
