package test.romhandlers;

/**
 * Constants for all ROM names, to easily test sets of ROMs.
 */
public class Roms {

    // All of these manual entries could be automatized by reading the .init files

    public static final String[] JAPANESE_GEN_1_ROMS = {"Red (J)", "Green (J)", "Blue (J)", "Yellow (J)"};
    public static final String[] ENGLISH_GEN_1_ROMS = {"Red (U)", "Blue (U)", "Yellow (U)"};
    public static final String[] OTHER_GEN_1_ROMS = {};
    public static final String[] ALL_GEN_1_ROMS = concatenateAll(JAPANESE_GEN_1_ROMS, ENGLISH_GEN_1_ROMS, OTHER_GEN_1_ROMS);

    public static final String[] JAPANESE_GEN_2_ROMS = {};
    public static final String[] ENGLISH_GEN_2_ROMS = {"Gold (U)", "Silver (U)", "Crystal (U)"};
    public static final String[] OTHER_GEN_2_ROMS = {};
    public static final String[] ALL_GEN_2_ROMS = concatenateAll(JAPANESE_GEN_2_ROMS, ENGLISH_GEN_2_ROMS, OTHER_GEN_2_ROMS);

    public static final String[] JAPANESE_GEN_3_ROMS = {};
    public static final String[] ENGLISH_GEN_3_ROMS = {"Ruby (U)", "Ruby (E)", "Sapphire (U)", "Fire Red (U) 1.0", "Fire Red (U) 1.1"};
    public static final String[] OTHER_GEN_3_ROMS = {};
    public static final String[] ALL_GEN_3_ROMS = concatenateAll(JAPANESE_GEN_3_ROMS, ENGLISH_GEN_3_ROMS, OTHER_GEN_3_ROMS);

    public static final String[] JAPANESE_GEN_4_ROMS = {};
    public static final String[] ENGLISH_GEN_4_ROMS = {"Pearl (U)", "Diamond (U)", "Platinum (U)", "HeartGold (U)", "SoulSilver (U)"};
    public static final String[] OTHER_GEN_4_ROMS = {};
    public static final String[] ALL_GEN_4_ROMS = concatenateAll(JAPANESE_GEN_4_ROMS, ENGLISH_GEN_4_ROMS, OTHER_GEN_4_ROMS);

    public static final String[] JAPANESE_GEN_5_ROMS = {};
    public static final String[] ENGLISH_GEN_5_ROMS = {};
    public static final String[] OTHER_GEN_5_ROMS = {};
    public static final String[] ALL_GEN_5_ROMS = concatenateAll(JAPANESE_GEN_5_ROMS, ENGLISH_GEN_5_ROMS, OTHER_GEN_5_ROMS);

    public static final String[] JAPANESE_GEN_6_ROMS = {};
    public static final String[] ENGLISH_GEN_6_ROMS = {};
    public static final String[] OTHER_GEN_6_ROMS = {};
    public static final String[] ALL_GEN_6_ROMS = concatenateAll(JAPANESE_GEN_6_ROMS, ENGLISH_GEN_6_ROMS, OTHER_GEN_6_ROMS);

    public static final String[] JAPANESE_GEN_7_ROMS = {};
    public static final String[] ENGLISH_GEN_7_ROMS = {};
    public static final String[] OTHER_GEN_7_ROMS = {};
    public static final String[] ALL_GEN_7_ROMS = concatenateAll(JAPANESE_GEN_7_ROMS, ENGLISH_GEN_7_ROMS, OTHER_GEN_7_ROMS);

    public static final String[] ALL_ROMS = concatenateAll(ALL_GEN_1_ROMS, ALL_GEN_2_ROMS, ALL_GEN_3_ROMS, ALL_GEN_4_ROMS,
            ALL_GEN_5_ROMS, ALL_GEN_6_ROMS, ALL_GEN_7_ROMS);

//    public static final String[] ALL_ROMS = {"Crystal (U)", "Ruby (U)", "Ruby (S) 1.1", "Ruby (F) 1.1",
//            "Fire Red (U) 1.0", "Fire Red (U) 1.1", "Emerald (G)", "Emerald (J)", "Pearl (U)"};

    private static String[] concatenateAll(String[] first, String[]... others) {
        int length = first.length;
        for (String[] other : others) {
            length += other.length;
        }
        String[] concatenated = new String[length];
        System.arraycopy(first, 0, concatenated, 0, first.length);
        int offset = first.length;
        for (String[] other : others) {
            System.arraycopy(other, 0, concatenated, offset, other.length);
            offset += other.length;
        }
        return concatenated;
    }
}
