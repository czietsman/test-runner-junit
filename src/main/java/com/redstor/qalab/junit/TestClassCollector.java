package com.redstor.qalab.junit;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TestClassCollector implements ClassCollector {
    private static final int CLASS_SUFFIX_LENGTH = ".class".length();
    private final List<Class<?>> classes = new ArrayList<>();

    public List<Class<?>> toList() {
        return classes;
    }

    public void loadClass(String name, InputStream in) throws Throwable {
        final ClassReader cr = new ClassReader(in);
        final ClassNode classNode = new ClassNode();
        cr.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        if (isTestClass(classNode)) {
            tryLoadClass(name).ifPresent(classes::add);
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

