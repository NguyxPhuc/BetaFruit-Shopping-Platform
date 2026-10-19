package com.fu.SWP391_BetaFruit.utils;

public final class StringUtils {

    private StringUtils() {
        // Prevent instantiation
    }

    /**
     * Sanitizes a search keyword by trimming leading and trailing whitespace,
     * converting empty strings to null, and truncating to a maximum of 100 characters.
     *
     * @param keyword the input search keyword
     * @return the sanitized keyword, or null if null or empty
     */
    public static String sanitizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String clean = keyword.trim();
        if (clean.isEmpty()) {
            return null;
        }
        return clean.length() > 100 ? clean.substring(0, 100) : clean;
    }
}
