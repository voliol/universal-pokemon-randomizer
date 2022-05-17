package com.dabomstew.pkrandom.graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

// TODO: better name, and/or refactor in other classes

public class ParsedDescription {

	private static enum CharType {
		DIGIT, LETTER, SIBLING_DELIMITER, IGNORE;

		public static CharType of(char c) {
			if (Character.isDigit(c)) {
				return DIGIT;
			} else if (Character.isLetter(c)) {
				return LETTER;
			} else if (c == ';') {
				return SIBLING_DELIMITER;
			} else {
				return IGNORE;
			}
		}
	}

	private static enum LightDarkSuffix {
		ANY, LIGHT, DARK, BASE, NO_LIGHT, NO_DARK
	}

	private boolean isBlank;

	// TODO: should these be stacks just so siblingSlots can use pop() once?
	private Stack<Integer> slots = new Stack<>();

	private Stack<Integer> siblingSlots = new Stack<>();
	private int sharedSlot = -1;

	private Stack<Integer> averageSlots = new Stack<>();

	private LightDarkSuffix lightDarkSuffix = LightDarkSuffix.ANY;
	private boolean endDarkened;

	public ParsedDescription(String unparsed) {
		isBlank = unparsed.isBlank();
		List<String> tokens = splitIntoTokens(unparsed);
		parseTokens(tokens);
	}

	private List<String> splitIntoTokens(String unparsed) {
		List<String> tokens = new ArrayList<>();

		StringBuilder sb = new StringBuilder();
		char last = ' ';
		char current;
		for (int i = 0; i < unparsed.length(); i++) {

			current = unparsed.charAt(i);
			CharType charType = CharType.of(current);

			if (charType != CharType.IGNORE) {
				if (charType != CharType.of(last)) {
					tokens.add(sb.toString());
					sb = new StringBuilder();
				}
				sb.append(current);
			}
			last = current;
		}
		tokens.add(sb.toString());

		return tokens;
	}

	private void parseTokens(List<String> tokens) {
		Stack<Integer> currentSlots = slots;
		for (String token : tokens) {

			if (token.matches("[0-9]+")) {
				currentSlots.push(Integer.parseInt(token) - 1);
			}

			else if (token.equals("L")) {
				lightDarkSuffix = LightDarkSuffix.LIGHT;
			}

			else if (token.equals("D")) {
				lightDarkSuffix = LightDarkSuffix.DARK;
			}

			else if (token.equals("LN")) {
				lightDarkSuffix = LightDarkSuffix.NO_LIGHT;
			}

			else if (token.equals("DN")) {
				lightDarkSuffix = LightDarkSuffix.NO_DARK;
			}

			else if (token.equals("B")) {
				lightDarkSuffix = LightDarkSuffix.BASE;
			}

			else if (token.equals("E")) {
				endDarkened = true;
			}

			else if (token.equals("A")) {
				currentSlots = averageSlots;
			}

			else if (token.equals(";")) {
				currentSlots = siblingSlots;
			}

		}
		if (currentSlots == siblingSlots && !siblingSlots.isEmpty()) {
			sharedSlot = siblingSlots.pop();
		}
	}

	public boolean isBlank() {
		return isBlank;
	}

	public boolean isEndDarkened() {
		return endDarkened;
	}

	public boolean hasSibling() {
		return !siblingSlots.isEmpty();
	}

	public boolean isAverageDescription() {
		return !averageSlots.isEmpty();
	}

	public int length() {
		return slots.size();
	}

	public int siblingLength() {
		return siblingSlots.size();
	}

	public int[] getSlots() {
		int[] slotsArr = new int[slots.size()];
		for (int i = 0; i < slots.size(); i++) {
			slotsArr[i] = slots.get(i);
		}
		return slotsArr;
	}

	public int[] getSiblingSlots() {
		if (!hasSibling()) {
			throw new IllegalStateException("Can't get sibling slots when there is none.");
		}
		int[] slotsArr = new int[siblingSlots.size()];
		for (int i = 0; i < siblingSlots.size(); i++) {
			slotsArr[i] = siblingSlots.get(i);
		}
		return slotsArr;
	}

	public int getSharedSlot() {
		if (sharedSlot == -1) {
			throw new IllegalStateException("Shared color slot has not been set.");
		}
		return sharedSlot;
	}
	
	public int getAverageToSlot() {
		return averageSlots.get(averageSlots.size()-1);
	}
	
	public int[] getAverageFromSlots() {
		int[] slotsArr = new int[averageSlots.size()-1];
		for (int i = 0; i < averageSlots.size()-1; i++) {
			slotsArr[i] = averageSlots.get(i);
		}
		return slotsArr;
	}

	public LightDarkMode correctLightDarkMode(LightDarkMode in) {
		switch (lightDarkSuffix) {
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Desc(");
		if (isBlank()) {
			sb.append("BLANK");
		} else if (isAverageDescription()) {
			sb.append("AV");
		} else {
			sb.append(Arrays.toString(getSlots()));
			if (hasSibling()) {
				sb.append(" SIB:" + Arrays.toString(getSiblingSlots()));
				sb.append(" SHARED:" + getSharedSlot());
			}
			if (lightDarkSuffix != LightDarkSuffix.ANY) {
				sb.append(" " + lightDarkSuffix);
			}
			if (isEndDarkened()) {
				sb.append("END_DARKENED");
			}
		}
		sb.append(")");
		return sb.toString();

	}

}
