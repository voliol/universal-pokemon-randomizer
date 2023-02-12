package com.dabomstew.pkrandom.config;

import com.dabomstew.pkrandom.romhandlers.romentries.RomEntry;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Scanner;

public class RomInfoReader {

    private static final String COMMENT_PREFIX = "//";

    private Scanner scanner;

    public RomInfoReader(String fileName) throws IOException {
        scanner = new Scanner(new File(fileName), StandardCharsets.UTF_8);

        RomEntry current = null;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = removeComments(line);

            if (!line.isEmpty()) {
                // do different things depending on the RomEntry type
            }
        }

    }

    public void readAndAddAllEntries(Collection<RomEntry> romEntries) {

    }

    private String removeComments(String line) {
        if (line.contains(COMMENT_PREFIX)) {
            line = line.substring(0, line.indexOf(COMMENT_PREFIX)).trim();
        }
        return line;
    }

}
