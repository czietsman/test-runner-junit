package com.redstor.qalab.junit;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

import static com.redstor.qalab.junit.Markers.VERBOSE;

public class TestClassCollector implements ClassCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestClassCollector.class);

    private final Set<String> testableClassNameSet = new HashSet<>();
    private final Set<String> abstractClassNameSet = new HashSet<>();
    private final Map<String, String> classGenealogyMap = new HashMap<>();
    private List<Class<?>> classes;

    public List<Class<?>> toList() {
        if (classes == null) {
            List<Class<?>> classes = new ArrayList<>();
            for (String className : classGenealogyMap.keySet()) {
                // skip abstract classes
                if (abstractClassNameSet.contains(className)) {
                    continue;
                }

                if (!isTestClass(className)) {
                    continue;
                }

                try {
                    tryLoadClass(className).ifPresent(classes::add);
                } catch (Throwable ex) {
                    LOGGER.error(VERBOSE, "Failed to load class {}", className, ex);
                }
            }
            this.classes = classes;
        }
        return classes;
    }

    public void collect(String name, InputStream in) throws Throwable {
        final ClassReader cr = new ClassReader(in);
        final ClassNode classNode = new ClassNode();
        cr.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        classGenealogyMap.put(classNode.name, classNode.superName);

        if (isAbstractClass(classNode)) {
            abstractClassNameSet.add(classNode.name);
        }

        if (isTestableClass(classNode)) {
            testableClassNameSet.add(classNode.name);
        }
    }

    private boolean isTestClass(String className) {
        // is this class testable
        if (testableClassNameSet.contains(className)) {
            return true;
        }

        // is any of the ancestors testable
        final String superName = classGenealogyMap.get(className);
        return superName != null && isTestClass(superName);
    }

    private boolean isAbstractClass(ClassNode classNode) {
        return (classNode.access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT;
    }

    /**
     * The class has at least one method with a Test annotation
     */
    private boolean isTestableClass(ClassNode classNode) {
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

    private Optional<Class<?>> tryLoadClass(String asmClassName) throws Throwable {
        final String className = toJavaClassName(asmClassName);
        final Class<?> clazz = Class.forName(className);
        if (clazz == null || clazz.isLocalClass() || clazz.isAnonymousClass()) {
            return Optional.empty();
        }
        return Optional.of(clazz);
    }

    private String toJavaClassName(String className) {
        return className.replaceAll("/", ".");
    }
}

