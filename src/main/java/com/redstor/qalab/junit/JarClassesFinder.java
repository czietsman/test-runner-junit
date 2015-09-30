package com.redstor.qalab.junit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.redstor.qalab.junit.Markers.VERBOSE;

/**
 * This class was originally adapted from the ClasspathSuite code.
 * <p>
 * It has been modified to use ASM instead of reflection and adjusted to filter on jar files instead of class files.
 *
 * @author https://github.com/takari/takari-cpsuite
 */
class JarClassesFinder implements ClassesFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(JarClassesFinder.class);
    private final JarFinder jarFinder;

    public JarClassesFinder(JarFinder jarFinder) {
        this.jarFinder = jarFinder;
    }

    @Override
    public <T extends ClassCollector> T find(T collector) {
        findClassesInRoots(jarFinder.find(), collector);
        return collector;
    }

    private void findClassesInRoots(Iterator<File> roots, ClassCollector collector) {
        roots.forEachRemaining(root -> gatherClassesInRoot(root, collector));
    }

    private void gatherClassesInRoot(File root, ClassCollector collector) {
        try {
            LOGGER.info(VERBOSE, "Gather classes for {}", root);
            JarFile jar = new JarFile(root);
            gatherClasses(collector, jar, new JarEntryIterator(jar));
        } catch (IOException e) {
            LOGGER.error(VERBOSE, "Could not gather classes from {}", root, e);
        }
    }

    private boolean isClassFile(String classFileName) {
        return classFileName.endsWith(".class");
    }

    private void gatherClasses(ClassCollector collector, JarFile jar, Iterator<JarEntry> entries) {
        while (entries.hasNext()) {
            final JarEntry jarEntry = entries.next();
            if (!isClassFile(jarEntry.getName())) {
                continue;
            }

            LOGGER.info(VERBOSE, "Examining class {}", jarEntry.getName());
            try (InputStream in = jar.getInputStream(jarEntry)) {
                collector.loadClass(jarEntry.getName(), in);
            } catch (Throwable ex) {
                LOGGER.error(VERBOSE, "Could not load class {}", jarEntry.getName(), ex);
            }
        }
    }
}
