package com.redstor.qalab.junit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public interface JarFinder {
    Iterator<File> find();

    default JarFinder and(JarFinder other) {
        return () -> {
            final ArrayList<File> list = new ArrayList<>();
            find().forEachRemaining(list::add);
            other.find().forEachRemaining(list::add);
            return list.iterator();
        };
    }

    static JarFinder empty() {
        return Collections::emptyIterator;
    }
}
