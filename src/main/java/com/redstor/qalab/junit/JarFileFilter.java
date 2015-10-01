package com.redstor.qalab.junit;

interface JarFileFilter {
    boolean match(String fileName);

    default JarFileFilter and(JarFileFilter other) {
        return fileName -> (match(fileName) && other.match(fileName));
    }

    default JarFileFilter or(JarFileFilter other) {
        return fileName -> (match(fileName) || other.match(fileName));
    }

    static JarFileFilter any() {
        return fileName -> true;
    }

    static JarFileFilter none() {
        return fileName -> false;
    }

    static JarFileFilter not(JarFileFilter filter) {
        return fileName -> !filter.match(fileName);
    }

    static JarFileFilter wildcard(String pattern) {
        return fileName -> new WildcardPattern(pattern).match(fileName);
    }
}

