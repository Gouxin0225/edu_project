package com.example.edubackend.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SecurePasswordGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "@#$%&*";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;
    private static final int DEFAULT_LENGTH = 12;

    private SecurePasswordGenerator() {
    }

    public static String generate() {
        List<Character> chars = new ArrayList<>();
        chars.add(randomChar(UPPER));
        chars.add(randomChar(LOWER));
        chars.add(randomChar(DIGITS));
        chars.add(randomChar(SYMBOLS));
        while (chars.size() < DEFAULT_LENGTH) {
            chars.add(randomChar(ALL));
        }
        Collections.shuffle(chars, RANDOM);

        StringBuilder password = new StringBuilder(DEFAULT_LENGTH);
        for (Character c : chars) {
            password.append(c);
        }
        return password.toString();
    }

    private static char randomChar(String chars) {
        return chars.charAt(RANDOM.nextInt(chars.length()));
    }
}
