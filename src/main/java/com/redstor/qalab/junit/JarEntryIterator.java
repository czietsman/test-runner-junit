package com.redstor.qalab.junit;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class JarEntryIterator implements Iterator<JarEntry> {

    private Enumeration<JarEntry> entries;

    private JarEntry next;

    public JarEntryIterator(JarFile jar) throws IOException {
        entries = jar.entries();
        retrieveNextElement();
    }

    private void retrieveNextElement() {
        next = null;
        while (entries.hasMoreElements()) {
            next = entries.nextElement();
            if (!next.isDirectory()) {
                break;
            }
        }
    }

    public boolean hasNext() {
        return next != null;
    }

    public JarEntry next() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        JarEntry value = next;
        retrieveNextElement();
        return value;
    }

    public void remove() {
        throw new RuntimeException("Not implemented");
    }

}