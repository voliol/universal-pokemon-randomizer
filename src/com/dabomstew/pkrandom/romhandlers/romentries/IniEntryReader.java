package com.dabomstew.pkrandom.romhandlers.romentries;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;

import static com.dabomstew.pkrandom.FileFunctions.openConfig;

public abstract class IniEntryReader<T extends IniEntry> {

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
            System.err.println("invalid base " + radix + " number " + s);
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
            System.err.println("invalid base " + radix + " number " + s);
            return 0;
        }
    }

    public static boolean parseBoolean(String s) {
        return (parseInt(s) > 0);
    }

    /**
     * An enum for dictating whether to read values as ints or Strings by default.
     * Protected so subclasses have to make the choice, instead of the code using them.
     */
    protected enum DefaultReadMode {INT, STRING}

    private static final String COMMENT_PREFIX = "//";

    private final DefaultReadMode defaultReadMode;
    private T current;
    private List<T> iniEntries;

    private final Map<String, BiConsumer<T, String[]>> keyPrefixMethods = new HashMap<>();
    private final Map<String, BiConsumer<T, String[]>> keySuffixMethods = new HashMap<>();
    private final Map<String, BiConsumer<T, String>> specialKeyMethods = new HashMap<>();

    public IniEntryReader(DefaultReadMode defaultReadMode) {
        this.defaultReadMode = defaultReadMode;
    }

    protected void putKeyPrefixMethod(String key, BiConsumer<T, String[]> method) {
        keyPrefixMethods.put(key, method);
    }

    protected void putKeySuffixMethod(String key, BiConsumer<T, String[]> method) {
        keySuffixMethods.put(key, method);
    }

    protected void putSpecialKeyMethod(String key, BiConsumer<T, String> method) {
        specialKeyMethods.put(key, method);
    }

    public List<T> readEntriesFromFile(String fileName) throws FileNotFoundException {
        Scanner scanner = new Scanner(openConfig(fileName), StandardCharsets.UTF_8);
        this.iniEntries = new ArrayList<>();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = removeComments(line);

            if (line.isEmpty()) {
                continue;
            }

            if (isEntryStart(line)) {
                startNewEntry(line);
            } else if (current != null) {
                parseAndAddValuePair(line);
            }

        }
        return iniEntries;
    }

    private boolean isEntryStart(String line) {
        return line.startsWith("[") && line.endsWith("]");
    }

    private void startNewEntry(String line) {
        current = initiateEntry(line.substring(1, line.length() - 1));
        iniEntries.add(current);
        putSpecialKeyMethod("CopyFrom", this::copyFrom);
    }

    protected abstract T initiateEntry(String name);

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

        BiConsumer<T, String[]> keyPrefixMethod = checkForKeyPrefixMethod(valuePair);
        if (keyPrefixMethod != null) {
            keyPrefixMethod.accept(current, valuePair);
            return;
        }

        BiConsumer<T, String[]> keySuffixMethod = checkForKeySuffixMethod(valuePair);
        if (keySuffixMethod != null) {
            keySuffixMethod.accept(current, valuePair);
            return;
        }

        BiConsumer<T, String> specialKeyMethod = specialKeyMethods.get(valuePair[0]);
        if (specialKeyMethod != null) {
            specialKeyMethod.accept(current, valuePair[1]);
            return;
        }

        addNormalValue(valuePair);
    }

    private BiConsumer<T, String[]> checkForKeyPrefixMethod(String[] valuePair) {
        for (Map.Entry<String, BiConsumer<T, String[]>> ksmEntry : keyPrefixMethods.entrySet()) {
            if (valuePair[0].startsWith(ksmEntry.getKey())) {
                return ksmEntry.getValue();
            }
        }
        return null;
    }

    private BiConsumer<T, String[]> checkForKeySuffixMethod(String[] valuePair) {
        for (Map.Entry<String, BiConsumer<T, String[]>> kfmEntry : keySuffixMethods.entrySet()) {
            if (valuePair[0].endsWith(kfmEntry.getKey())) {
                return kfmEntry.getValue();
            }
        }
        return null;
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
            switch (defaultReadMode) {
                case INT -> addIntValue(valuePair);
                case STRING -> addStringValue(valuePair);
            }
        }
    }

    private boolean isArrayValuePair(String[] valuePair) {
        return valuePair[1].startsWith("[") && valuePair[1].endsWith("]");
    }

    protected void addArrayValue(String[] valuePair) {
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

    protected void addIntValue(String[] valuePair) {
        addIntValue(current, valuePair);
    }

    protected void addIntValue(T entry, String[] valuePair) {
        int value = parseInt(valuePair[1]);
        entry.putIntValue(valuePair[0], value);
    }

    protected void addStringValue(String[] valuePair) {
        addStringValue(current, valuePair);
    }

    protected void addStringValue(T entry, String[] valuePair) {
        entry.putStringValue(valuePair[0], valuePair[1]);
    }

    private void copyFrom(T entry, String value) {
        for (T other : iniEntries) {
            if (matchesCopyFromValue(other, value)) {
                entry.copyFrom(other);
            }
        }
    }

    protected boolean matchesCopyFromValue(T other, String value) {
        return value.equalsIgnoreCase(other.getName());
    }


}
