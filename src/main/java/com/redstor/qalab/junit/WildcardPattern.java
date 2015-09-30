package com.redstor.qalab.junit;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wildcard pattern implementation for finding matching strings.
 *
 * @author Ben Turnbull
 * @since 2013/11/01
 */
public class WildcardPattern {

    private final String pattern;
    private final boolean matchAll;

    public WildcardPattern(String pattern) {
        checkNotNull(pattern);

        // "compile" the pattern by taking into account case sensitivity now
        this.pattern = pattern.toLowerCase(Locale.ENGLISH);

        // determine whether any of the patterns would match any text
        this.matchAll = pattern.equals("*") || pattern.equals("*.*");
    }

    public boolean match(String text) {
        if (text == null) return false;
        if (matchAll) return true;
        final String comparableText = text.toLowerCase();
        return match(pattern, comparableText);
    }

    /***********************************************************************
     * Check if pattern string matches text string.
     * <p>
     * At the beginning of iteration i of main loop
     * <p>
     * old[j]    = true if pattern[0..j] matches text[0..i-1]
     * <p>
     * By comparing pattern[j] with text[i], the main loop computes
     * <p>
     * states[j] = true if pattern[0..j] matches text[0..i]
     ***********************************************************************/
    private boolean match(String pattern, String text) {
        // add sentinel so don't need to worry about *'s at end of pattern
        text += '\0';
        pattern += '\0';

        int N = pattern.length();

        boolean[] states = new boolean[N + 1];
        boolean[] old = new boolean[N + 1];
        old[0] = true;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            states = new boolean[N + 1];       // initialized to false
            for (int j = 0; j < N; j++) {
                char p = pattern.charAt(j);

                // hack to handle *'s that match 0 characters
                if (old[j] && (p == '*')) old[j + 1] = true;

                if (old[j] && (p == c)) states[j + 1] = true;
                if (old[j] && (p == '?')) states[j + 1] = true;
                if (old[j] && (p == '*')) states[j] = true;
                if (old[j] && (p == '*')) states[j + 1] = true;
            }
            old = states;
        }
        return states[N];
    }

}
