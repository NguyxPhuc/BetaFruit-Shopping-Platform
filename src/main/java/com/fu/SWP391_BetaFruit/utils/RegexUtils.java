package com.fu.SWP391_BetaFruit.utils;

public class RegexUtils {
    public static final String LOCATION_REGEX = "^[a-zA-Z0-9\\s\\u00C0-\\u1EF9\\-'.]+$";
    public static final String NAME_REGEX = "^[a-zA-Z\\s\\u00C0-\\u1EF9]+$";
    public static final String PHONE_REGEX = "^0[35789][0-9]{8}$";
}
