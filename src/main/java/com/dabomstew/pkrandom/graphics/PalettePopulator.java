package com.dabomstew.pkrandom.graphics;

import java.util.Arrays;
import java.util.Random;

/* PalettePopulator.java is based on PokePalette.java from the Pokemon Emerald Randomizer (ER), by Artemis251, 
 * modified for the needs of the Universal Pokémon Randomizer (UPR).
 * 
 * Below is a section taken from codeReadMe.txt of the ER source code, 
 * which may constitute a license. Parts omitted with "[...]" discuss
 * other topics.
 */

/* ===========================================
 * Pokemon Emerald Randomizer Source Code
 * ===========================================
 * v.2.2 -- 13 April 2014
 * Original code by Artemis251
 * 
 * [...]
 * 
 * In any case, feel free to tweak the code to your heart's content. I do
 * have a few ideas of how to take the current program further, but I lack
 * the free time to really implement them. Feel free to do so on your own!
 * I'm not sure what the whole protocol is for open sourcin' code in terms
 * of gettin' a central repository (do people use some kinda globally-
 * accessible git/svn or somethin'?), but you're free to redistribute your
 * own changes and whatnot. It's all for the greater good of players! [...]
 */

/* Permission has also been given expressly to reuse code of the ER in the UPR.
 * Below is an abridged version of the Reddit conversation where permission
 * was given, with a fuller version seen in this screenshot: 
 * https://i.imgur.com/hEzrP4Z.png [taken 2022-04-10]
 * (the conversation goes on after that, but licensing is not discussed more) 
 * 
 * voliol:
 * "Your Readme for the Emerald Randomizer states that "you're free to 
 *  redistribute your own changes and whatnot". However, considering I would be 
 *  porting parts of it to another project altogether with a license of its own 
 *  (GNU General Public License v3.0) I believed I should still ask for 
 *  permission either way. So are you okay with it, me porting 
 *  [the palette randomization] to the UPR?"
 *  
 * Artemis251:
 * "I'd be happy to see the palette randomization added to the much-more-mature 
 *  UPR. Feel free to grab whatever you can from my existing code and reuse it. 
 *  I'm pretty sure most people use the UPR over my original attempts nowadays 
 *  anyway. It'd be nice to be credited somewhere at the least, mostly because 
 *  the Emerald palette thing was such a pain to accomplish."
 */

public class PalettePopulator {

    /*
     * For the parsing of the palette (part) descriptions.
     */
    private static class ParsedDescription {
        private final String rawString;
        private final int[] slots;
        private final int[] siblingSlots;
        private final int sharedSiblingColor;
        private final LightDarkSuffix lightDarkSuffix;

        // TODO: should this throw an IOException?
        public ParsedDescription(String rawString) {
            this.rawString = rawString;
            
            String[] maybeSibling = rawString.split(";");
			this.slots = splitAndConvertToInts(maybeSibling[0]);
			
			// sibling exists
			if (maybeSibling.length == 2) {
				this.siblingSlots = splitAndConvertToInts(maybeSibling[1]);
				this.sharedSiblingColor = splitAndConvertToInts(maybeSibling[1].split("-")[1])[0];
			} else {
				this.siblingSlots = null;
				this.sharedSiblingColor = -1;
			}
			
            this.lightDarkSuffix = initLightDarkSuffix();

        }

        public int[] getSlots() {
            return slots;
        }

        public int length() {
            return getSlots().length;
        }
        
        public boolean hasSibling() {
        	return siblingSlots != null;
        }
        
        public int[] getSiblingSlots() {
        	return siblingSlots;
        }
        
        public int siblingLength() {
        	return getSiblingSlots().length;
        }
        
		public int getSharedSiblingColor() {
        	return sharedSiblingColor;
        }

        public LightDarkSuffix getLightDarkSuffix() {
            return lightDarkSuffix;
        };

        public boolean isEndDarkened() {
            return rawString.contains("-E");
        }

        private static int[] splitAndConvertToInts(String s) {
            String[] unconverted = stripAndSplit(s);
            int[] converted = new int[unconverted.length];
            for (int i = 0; i < unconverted.length; i++) {
                if (isStringNumeric(unconverted[i])) {
                    converted[i] = Integer.valueOf(unconverted[i]);
                } else {
                    return new int[0];
                }
            }
            return converted;
        }

        private static String[] stripAndSplit(String s) {
            s = s.replaceAll("[A-Za-z]+", "").strip().split("-")[0];
            return s.split(",");
        }

        private static boolean isStringNumeric(String s) {
            return s != null && s.matches("[0-9.]+");
        }

        // TODO: is calling it "init" appropriate?
        private LightDarkSuffix initLightDarkSuffix() {

            LightDarkSuffix suffix = LightDarkSuffix.ANY;
            if (rawString.contains("-LN")) {
                suffix = LightDarkSuffix.NO_LIGHT;
            } else if (rawString.contains("-DN")) {
                suffix = LightDarkSuffix.NO_DARK;
            } else if (rawString.contains("-L")) {
                suffix = LightDarkSuffix.LIGHT;
            } else if (rawString.contains("-D")) {
                suffix = LightDarkSuffix.DARK;
            } else if (rawString.contains("-B")) {
                suffix = LightDarkSuffix.BASE;
            }

            return suffix;
        }
    }

    private static enum LightDarkSuffix {
        ANY, LIGHT, DARK, BASE, NO_LIGHT, NO_DARK
    }
    
    private static final int TOTAL_CHANGE_THRESHOLD = 9; // threshold for color changes to meet before stopping their
                                                         // seed - artemis

    private Random random;

    private Palette palette;
    private ParsedDescription description;
    private Color baseColor;
    private LightDarkMode lightDarkMode;

    private double[] leftShift;
    private double[] rightShift;
    private Color[] sorted;

    public PalettePopulator(Random random) {
        this.random = random;
    }

    public void populatePartFromBaseColor(Palette palette, String partDescription, Color baseColor, LightDarkMode lightDarkMode) {

        // TODO: Maybe an anti-pattern - should this be a subclass of Palette instead?
        // Ask someone!
        this.palette = palette;

        // get light/dark status

        this.description = new ParsedDescription(partDescription);

        this.baseColor = baseColor.clone();
        
        this.lightDarkMode = correctLightDarkMode(lightDarkMode);

        if (description.length() != 0) {
            Color[] shades = makeShades();
            fillWithShades(shades);
        }
        
//        if (description.hasSibling() && description.siblingLength() != 0) {
//        	Color[] siblingShades = makeSiblingShades();
//        	fillWithShades(siblingShades);
//        }
    }
    
	private LightDarkMode correctLightDarkMode(LightDarkMode in) {
		switch (description.getLightDarkSuffix()) {
		case ANY:
			return in;
		case BASE:
			return LightDarkMode.DEFAULT;
		case DARK:
			return LightDarkMode.DARK;
		case LIGHT:
			return LightDarkMode.LIGHT;
		case NO_DARK:
			if (in == LightDarkMode.DARK) {
				return LightDarkMode.DEFAULT;
			} else {
				return in;
			}
		case NO_LIGHT:
			if (in == LightDarkMode.LIGHT) {
				return LightDarkMode.DEFAULT;
			} else {
				return in;
			}
		default:
			return in;
		}
	}

	private void fillWithShades(Color[] shades) {
		for (int i = 0; i < shades.length; i++) {
		    if (shades[i] != null) {
		        palette.setColor(i, shades[i]);
		    }
		}
	}

    private Color[] makeShades() {
        sorted = new Color[description.length()];
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = new Color();
        }

        initializeLeftRightShift();

        makeBaseColorLightOrDark();

        makeMiddleShadeOrShades();
        makeLeftShades();
        makeRightShades();

        if (description.isEndDarkened()) {
            makeEndShadeDarkened();
        }

        Color[] out = fitShadesInSlots();
        return out;
    }

    private Color[] makeSiblingShades() {
	    sorted = new Color[description.siblingLength()];
	    for (int i = 0; i < sorted.length; i++) {
	        sorted[i] = new Color();
	    }
	    //TODO
	    Color[] out = fitShadesInSiblingSlots();
	    return out;
	}

	// TODO: give this a better name(?)
    private void makeBaseColorLightOrDark() {
        boolean rndcha = false;
        // TODO: figure out what rndcha is - it seems if it is true, then only some are
        // darkened/lightened. Does it stand for "random chance"?
        // is it not redundant with/did it originally use LIGHTDARK_CHANCE ?
        switch (lightDarkMode) {
        case DARK:
            darkenBaseColor(rndcha);
            darkenLeftRightShift();
            break;
        case LIGHT:
            lightenBaseColor(rndcha);
            lightenLeftRightShift();
            break;
        case DEFAULT:
            break;
        }
    }

    private void darkenBaseColor(boolean rndcha) {
        double mod;
        if (rndcha) {
            mod = 0.2;
        } else {
            mod = 0.5;
        }
        for (int i = 0; i < 3; i++) {
            int value = (int) (baseColor.getComp(i) - rightShift[i] * (description.length() / 2 + 1) * mod);
            value = Math.max(value, 0);
            baseColor.setComp(i, value);
        }
    }

    private void darkenLeftRightShift() {
        leftShift[0] = getRightColorChange(getMiddleSlotID(), baseColor.getComp(0));
        leftShift[1] = getRightColorChange(getMiddleSlotID(), baseColor.getComp(1));
        leftShift[2] = getRightColorChange(getMiddleSlotID(), baseColor.getComp(2));
        rightShift[0] = getRightColorChange(getMiddleSlotID(), baseColor.getComp(0));
        rightShift[1] = getRightColorChange(getMiddleSlotID(), baseColor.getComp(1));
        rightShift[2] = getRightColorChange(getMiddleSlotID(), baseColor.getComp(2));
    }

    private void lightenBaseColor(boolean rndcha) {
        double mod;
        if (rndcha) {
            mod = 0.2;
        } else {
            mod = 0.5;
        }

        for (int i = 0; i < 3; i++) {
            int value = (int) (baseColor.getComp(i) + leftShift[i] * (description.length() / 2 + 1) * mod);
            value = Math.min(value, 255);
            baseColor.setComp(i, value);
        }
    }

    private void lightenLeftRightShift() {
        leftShift[0] = getLeftColorChange(getMiddleSlotID(), baseColor.getComp(0));
        leftShift[1] = getLeftColorChange(getMiddleSlotID(), baseColor.getComp(1));
        leftShift[2] = getLeftColorChange(getMiddleSlotID(), baseColor.getComp(2));
        rightShift[0] = getLeftColorChange(getMiddleSlotID(), baseColor.getComp(0));
        rightShift[1] = getLeftColorChange(getMiddleSlotID(), baseColor.getComp(1));
        rightShift[2] = getLeftColorChange(getMiddleSlotID(), baseColor.getComp(2));

        double mod = lightDarkenCoeff();
        for (int i = 0; i < rightShift.length; i++) {
            rightShift[i] += mod * getLeftColorChange(getMiddleSlotID(), baseColor.getComp(i));
        }
    }

    private double lightDarkenCoeff() {
        int[] bc = baseColor.toInts();
        Arrays.sort(bc);
        double avg = (bc[1] + bc[2]) / 2.0;

        if (baseColor.getComp(1) >= 225 && baseColor.getComp(0) < 200 && baseColor.getComp(2) < 200)
            avg += 75; // 45
        if (baseColor.getComp(1) >= 225)
            avg += 75;
        if (avg > 255)
            avg = 255;
        if (avg < 192) {
            return 0.5;
        } else {
            return (0.0002 * Math.pow(avg - 192, 3.5) + 50) * 0.01;
        }
    }

    private void initializeLeftRightShift() {
        leftShift = new double[3];
        leftShift[0] = getLeftColorChange(getMiddleSlotID(), baseColor.getComp(0));
        leftShift[1] = getLeftColorChange(getMiddleSlotID(), baseColor.getComp(1));
        leftShift[2] = getLeftColorChange(getMiddleSlotID(), baseColor.getComp(2));
        rightShift = new double[3];
        rightShift[0] = getRightColorChange(getMiddleSlotID(), baseColor.getComp(0));
        rightShift[1] = getRightColorChange(getMiddleSlotID(), baseColor.getComp(1));
        rightShift[2] = getRightColorChange(getMiddleSlotID(), baseColor.getComp(2));
    }

    private int getMiddleSlotID() {
        return (description.length() - (isSlotsEven() ? 0 : 1)) / 2;
    }

    private Color[] fitShadesInSlots() {
        Color[] out = new Color[palette.size()];
        for (int i = 0; i < description.length(); i++) {
            out[description.getSlots()[i] - 1] = sorted[i];
        }
        return out;
    }
    
    private Color[] fitShadesInSiblingSlots() {
        Color[] out = new Color[palette.size()];
        for (int i = 0; i < description.siblingLength(); i++) {
            out[description.getSiblingSlots()[i] - 1] = sorted[i];
        }
        return out;
    }

    private void makeMiddleShadeOrShades() {
        if (isSlotsEven()) {
            for (int totalChange = 0, r = 0; totalChange < TOTAL_CHANGE_THRESHOLD && r < 5; r++) {
                totalChange = 0;
                for (int p = 0; p < 3; p++) {

                    int leftValue = (int) (baseColor.getComp(p) + leftShift[p] * 0.5);
                    leftValue = Math.min(leftValue, 255);
                    sorted[sorted.length / 2 - 1].setComp(p, leftValue);

                    int rightValue = (int) (baseColor.getComp(p) - rightShift[p] * 0.5);
                    rightValue = Math.max(rightValue, 0);
                    sorted[sorted.length / 2].setComp(p, rightValue);

                    totalChange += (sorted[sorted.length / 2].getComp(p) >> 2)
                            - (sorted[sorted.length / 2 - 1].getComp(p) >> 2);
                }
            }
        } else {
            sorted[sorted.length / 2] = baseColor;
        }
    }

    private void makeLeftShades() {
        // make all colors left of middle
        for (int ptr = sorted.length / 2 - (isSlotsEven() ? 2 : 1); ptr >= 0; ptr--) {
            for (int p = 0; p < 3; p++) {
                int value = (int) (sorted[ptr + 1].getComp(p) + leftShift[p]);
                value = Math.min(value, 255);
                sorted[ptr].setComp(p, value);
            }
        }
    }

    private void makeRightShades() {
        // make all colors right of middle
        for (int ptr = sorted.length / 2 + 1; ptr < sorted.length; ptr++) {
            for (int p = 0; p < 3; p++) {
                int value = (int) (sorted[ptr - 1].getComp(p) - rightShift[p]);
                value = Math.max(value, 0);
                sorted[ptr].setComp(p, value);
            }
        }
    }

    private void makeEndShadeDarkened() {
        int ptr = sorted.length - 1;
        for (int j = 0; j < 1; j++) { // can be adjusted to darken more
            for (int p = 0; p < 3; p++) {
                int value = (int) (sorted[ptr].getComp(p) - rightShift[p]);
                value = Math.max(value, 0);
                sorted[ptr].setComp(p, value);
            }
        }
    }

    private double getLeftColorChange(int palLimit, int maxRGB) {
        if (palLimit == 0)
            return 0;
        double coeff = (random.nextInt(5) + random.nextInt(5) + random.nextInt(5) + 153) / (palLimit);
        return coeff - coeff / 255 * maxRGB + palLimit * palLimit;
    }

    private double getRightColorChange(int palLimit, int maxRGB) {
        if (palLimit == 0)
            return 0;
        double coeff = (random.nextInt(5) + random.nextInt(5) + random.nextInt(5) + 153) / (palLimit);
        return coeff - coeff / 255 * (255 - maxRGB) + palLimit * palLimit;
    }

    private boolean isSlotsEven() {
        return description.length() % 2 == 0;
    }

}
