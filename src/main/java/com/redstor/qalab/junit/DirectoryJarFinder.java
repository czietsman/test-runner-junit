package com.redstor.qalab.junit;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

class DirectoryJarFinder implements JarFinder {
    private final String fileName;
    private final JarFileFilter filter;

    public DirectoryJarFinder(String fileName, JarFileFilter filter) {
        this.fileName = fileName;
        this.filter = filter;
    }

    @Override
    public Iterator<File> find() {
        final File file = new File(fileName);
        if (!file.isDirectory()) {
            return Collections.emptyIterator();
        }
        File[] jars = file.listFiles((dir, name) -> {
            return filter.match(name);
        });
        if (jars != null) {
            return Arrays.asList(jars).iterator();
        } else {
            return Collections.emptyIterator();
        }
    }
}
