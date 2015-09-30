package com.redstor.qalab.junit;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import static com.redstor.qalab.junit.Markers.VERBOSE;

class JarClassesFinder implements ClassesFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(JarClassesFinder.class);
    private static final int CLASS_SUFFIX_LENGTH = ".class".length();
    private final JarFinder jarFinder;

    public JarClassesFinder(JarFinder jarFinder) {
        this.jarFinder = jarFinder;
    }

    @Override
    public List<Class<?>> find() {
        return findClassesInRoots(jarFinder.find());
    }

    private List<Class<?>> findClassesInRoots(Iterator<File> roots) {
        List<Class<?>> classes = new ArrayList<Class<?>>(100);
        roots.forEachRemaining(root -> gatherClassesInRoot(root, classes));
        return classes;
    }

    private void gatherClassesInRoot(File root, List<Class<?>> classes) {
        try {
            LOGGER.info(VERBOSE, "Gather classes for {}", root);
            JarFile jar = new JarFile(root);
            gatherClasses(classes, jar, new JarEntryIterator(jar));
        } catch (IOException e) {
            LOGGER.error(VERBOSE, "Could not gather classes from {}", root, e);
        }
    }

    private boolean isClassFile(String classFileName) {
        return classFileName.endsWith(".class");
    }

    private void gatherClasses(List<Class<?>> classes, JarFile jar, Iterator<JarEntry> entries) {
        while (entries.hasNext()) {
            final JarEntry jarEntry = entries.next();
            if (!isClassFile(jarEntry.getName())) {
                continue;
            }

            LOGGER.info(VERBOSE, "Examining class {}", jarEntry.getName());
            try (InputStream in = jar.getInputStream(jarEntry)) {
                ClassReader cr = new ClassReader(in);
                final ClassNode classNode = new ClassNode();
                cr.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                if (isTestClass(classNode)) {
                    tryLoadClass(jarEntry.getName()).ifPresent(classes::add);
                }
            } catch (Throwable ex) {
                LOGGER.error(VERBOSE, "Could not load class {}", jarEntry.getName(), ex);
            }
        }
    }

    private boolean isTestClass(ClassNode classNode) {
        if (classNode.methods == null) {
            return false;
        }

        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            if (isTestMethod(methodNode)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTestMethod(MethodNode methodNode) {
        if (methodNode.visibleAnnotations == null) {
            return false;
        }

        for (AnnotationNode annotationNode : (List<AnnotationNode>) methodNode.visibleAnnotations) {
            if (annotationNode.desc.equals("Lorg/junit/Test;")) {
                return true;
            }
        }

        return false;
    }

    private Optional<Class<?>> tryLoadClass(String fileName) throws Throwable {
        String className = classNameFromFile(fileName);
        Class<?> clazz = Class.forName(className);
        if (clazz == null || clazz.isLocalClass() || clazz.isAnonymousClass()) {
            return Optional.empty();
        }
        return Optional.of(clazz);
    }

    private String classNameFromFile(String classFileName) {
        // convert /a/b.class to a.b
        String s = replaceFileSeparators(cutOffExtension(classFileName));
        if (s.startsWith("."))
            return s.substring(1);
        return s;
    }

    private String replaceFileSeparators(String s) {
        String result = s.replace(File.separatorChar, '.');
        if (File.separatorChar != '/') {
            // In Jar-Files it's always '/'
            result = result.replace('/', '.');
        }
        return result;
    }

    private String cutOffExtension(String classFileName) {
        return classFileName.substring(0, classFileName.length() - CLASS_SUFFIX_LENGTH);
    }


}
