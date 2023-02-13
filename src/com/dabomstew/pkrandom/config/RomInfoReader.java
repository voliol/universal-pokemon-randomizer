package com.dabomstew.pkrandom.config;

import com.dabomstew.pkrandom.romhandlers.romentries.RomEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.dabomstew.pkrandom.FileFunctions.openConfig;

public class RomInfoReader<T extends RomEntry> {

    public static int parseInt(String s) {
        int radix = 10;
        s = s.trim().toLowerCase();
        if (s.startsWith("0x") || s.startsWith("&h")) {
            radix = 16;
            s = s.substring(2);
        }
        try {
            return Integer.parseInt(s, radix);
        } catch (NumberFormatException ex) {
            System.err.println("invalid base " + radix + "number " + s);
            return 0;
        }
    }

    public static long parseLong(String s) {
        int radix = 10;
        s = s.trim().toLowerCase();
        if (s.startsWith("0x") || s.startsWith("&h")) {
            radix = 16;
            s = s.substring(2);
        }
        try {
            return Long.parseLong(s, radix);
        } catch (NumberFormatException ex) {
            System.err.println("invalid base " + radix + "number " + s);
            return 0;
        }
    }

    private static final String COMMENT_PREFIX = "//";
    private static final String TWEAK_SUFFIX = "Tweak";

    private final Scanner scanner;
    private T current;
    private Collection<T> romEntries;

    private final Function<String, T> initiator;
    private final Map<String, BiConsumer<T, String>> specialKeyMethods;

    public RomInfoReader(String fileName, Function<String, T> initiator, Map<String,
            BiConsumer<T, String>> specialKeyMethods) throws IOException {
        this.scanner = new Scanner(openConfig(fileName), StandardCharsets.UTF_8);
        this.initiator = initiator;
        this.specialKeyMethods = specialKeyMethods;
        specialKeyMethods.put("CopyFrom", (current, value) -> {
            for (T other : romEntries) {
                if (value.equalsIgnoreCase(other.getName())) {
                    current.copyFrom(other);
                }
            }
        });
    }

    public void readAllRomEntries(Collection<T> romEntries) {
        this.romEntries = romEntries;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = removeComments(line);

            if (line.isEmpty()) {
                continue;
            }

            if (isRomEntryStart(line)) {
                startNewRomEntry(line);
            } else if (current != null) {
                parseAndAddValuePair(line);
            }

        }
    }

    private boolean isRomEntryStart(String line) {
        return line.startsWith("[") && line.endsWith("]");
    }

    private void startNewRomEntry(String line) {
        current = initiator.apply(line.substring(1, line.length() - 1));
        romEntries.add(current);
    }

    private String removeComments(String line) {
        if (line.contains(COMMENT_PREFIX)) {
            line = line.substring(0, line.indexOf(COMMENT_PREFIX)).trim();
        }
        return line;
    }

    private void parseAndAddValuePair(String line) {
        String[] valuePair = splitIntoValuePair(line);
        if (valuePair == null) {
            throw new RuntimeException(); // TODO: what exactly to throw here
        }

        if (valuePair[0].endsWith(TWEAK_SUFFIX)) {
            addTweakFileValue(valuePair);
            return;
        }

        BiConsumer<T, String> specialKeyMethod = specialKeyMethods.get(valuePair[0]);
        if (specialKeyMethod != null) {
            specialKeyMethod.accept(current, valuePair[1]);
        } else {
            addNormalValue(valuePair);
        }
    }

    private void addTweakFileValue(String[] valuePair) {
        current.putTweakFile(valuePair[0], valuePair[1]);
    }

    private String[] splitIntoValuePair(String line) {
        String[] valuePair = line.split("=", 2);
        if (valuePair.length == 1) {
            System.err.println("invalid entry " + line);
            return null;
        }
        if (valuePair[1].endsWith("\r\n")) {
            valuePair[1] = valuePair[1].substring(0, valuePair[1].length() - 2);
        }
        valuePair[1] = valuePair[1].trim();
        valuePair[0] = valuePair[0].trim();
        return valuePair;
    }

    private void addNormalValue(String[] valuePair) {
        if (isArrayValuePair(valuePair)) {
            addArrayValue(valuePair);
        } else {
            addIntValue(valuePair);
        }
    }

    private boolean isArrayValuePair(String[] valuePair) {
        return valuePair[1].startsWith("[") && valuePair[1].endsWith("]");
    }

    private void addArrayValue(String[] valuePair) {
        String[] unparsed = valuePair[1].substring(1, valuePair[1].length() - 1).split(",");
        if (unparsed.length == 1 && unparsed[0].trim().isEmpty()) {
            current.putArrayValue(valuePair[0], new int[0]);
        } else {
            int[] value = new int[unparsed.length];
            int i = 0;
            for (String s : unparsed) {
                value[i++] = parseInt(s);
            }
            current.putArrayValue(valuePair[0], value);
        }
    }

    private void addIntValue(String[] valuePair) {
        int value = parseInt(valuePair[1]);
        current.putIntValue(valuePair[0], value);
    }

}
