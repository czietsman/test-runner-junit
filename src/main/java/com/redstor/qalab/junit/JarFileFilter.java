package com.redstor.qalab.junit;

interface JarFileFilter {
    boolean match(String fileName);

    default JarFileFilter and(JarFileFilter other) {
        return fileName -> (match(fileName) && other.match(fileName));
    }

    static JarFileFilter any() {
        return fileName -> true;
    }

    static JarFileFilter pattern(String pattern) {
        return fileName -> new WildcardPattern(pattern).match(fileName);
    }

    static JarFileFilter not(JarFileFilter filter) {
        return fileName -> !filter.match(fileName);
    }
}

