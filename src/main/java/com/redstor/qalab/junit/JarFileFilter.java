package com.redstor.qalab.junit;

import java.util.Locale;

interface JarFileFilter {
    boolean match(String fileName);

    default JarFileFilter and(JarFileFilter other) {
        return fileName -> (match(fileName) && other.match(fileName));
    }

    static JarFileFilter any() {
        return fileName -> true;
    }

    static JarFileFilter endsWith(String suffix) {
        return fileName -> fileName.toLowerCase(Locale.ENGLISH).endsWith(suffix);
    }
}
