package org.jmrtd.lds;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrazilianMRZFieldTransformation {
	/*
	 * Contributed by Paulo. FIXME: check this out. 
	 */

	/**
	 * Truncates the primary and secundary identifiers.
	 * 
	 * NOTE: Brazilian case.
	 */
	private String[] truncateNames(String primaryIdentifier, String secondaryIdentifier, int length) {
		final String NAME_SEPARATOR = "<<";

		// Removing Latin characters
		primaryIdentifier = transliterate(primaryIdentifier.trim());
		secondaryIdentifier = transliterate(secondaryIdentifier.trim());

		String[] primaryComponents = primaryIdentifier.split(" |<");
		String[] secondaryComponents = secondaryIdentifier.split(" |<");

		// Verify if the primary and secondary identifier needs to be truncated
		if (primaryIdentifier.length() + NAME_SEPARATOR.length() + secondaryIdentifier.length() <= length) {
			return new String[] { primaryIdentifier, secondaryIdentifier };
		}

		// Truncate identifiers
				int sizeMaximalPrimaryId = length - (secondaryComponents.length * 2 - 1) - NAME_SEPARATOR.length();
				int sizeMaximalSecondaryId = length - sizeMaximalPrimaryId - NAME_SEPARATOR.length();

//				if (primaryIdentifier.length() > sizeMaximalPrimaryId) {
//					primaryIdentifier = cutName(primaryId, (primaryIdentifier.length() - sizeMaximalPrimaryId));
//				}

//				if (secondaryIdentifier.length() > sizeMaximalSecondaryId) {
//					secondaryIdentifier = cutName(secondaryId, (secondaryIdentifier.length() - sizeMaximalSecondaryId));
//				}
				return null; // FIXME
	}

	/**
	 * Cut the name of according ICAO 9303
	 * 
	 * @param names
	 * @param numOfCharWillBeCuted
	 */
	private String truncate(String[] names, int numOfCharWillBeCuted) {

		final int firstLetter = 1;

		for (int i = names.length - 1; i >= 0 && numOfCharWillBeCuted != 0; i--) {

			int beginIndex = numOfCharWillBeCuted > names[i].length() ? firstLetter : names[i].length() - numOfCharWillBeCuted;

			String nameCutted = names[i].substring(beginIndex, names[i].length());

			numOfCharWillBeCuted = numOfCharWillBeCuted - nameCutted.length();
			names[i] = names[i].replaceAll(nameCutted, "");
		}

		StringBuffer name = new StringBuffer();
		for (String n : names) {
			name.append(n);
			name.append(" ");
		}

		return name.toString().trim();
	}

	/**
	 * Transliteration.
	 * Based on ICAO DOC 9303 part 1 vol 1 and ICAO DOC 9303 part 3 vol 1.
	 * 
	 * Replaces non-latin characters by transliteration.
	 * 
	 * @param text input text
	 */
	private String transliterate(String text) {

		text = text.toUpperCase();
		final String[] REPLACES = { "A", "E", "I", "O", "U", "C", "", " " };

		final Pattern[] PATTERNS = new Pattern[REPLACES.length];

		PATTERNS[0] = Pattern.compile("[бцаюд]", Pattern.CASE_INSENSITIVE);
		PATTERNS[1] = Pattern.compile("[ихйк]", Pattern.CASE_INSENSITIVE);
		PATTERNS[2] = Pattern.compile("[млно]", Pattern.CASE_INSENSITIVE);
		PATTERNS[3] = Pattern.compile("[сртуж]", Pattern.CASE_INSENSITIVE);
		PATTERNS[4] = Pattern.compile("[зышэ]", Pattern.CASE_INSENSITIVE);
		PATTERNS[5] = Pattern.compile("[г]", Pattern.CASE_INSENSITIVE);
		PATTERNS[6] = Pattern.compile("[']", Pattern.CASE_INSENSITIVE);
		PATTERNS[7] = Pattern.compile("[-]", Pattern.CASE_INSENSITIVE);

		String result = text;

		for (int i = 0; i < PATTERNS.length; i++) {
			Matcher matcher = PATTERNS[i].matcher(result);
			result = matcher.replaceAll(REPLACES[i]);
		}

		return result.toUpperCase();
	}
}
