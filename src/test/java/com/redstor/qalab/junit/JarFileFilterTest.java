package com.redstor.qalab.junit;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JarFileFilterTest {
    @Test
    public void testAnd() throws Exception {
        final JarFileFilter filter = JarFileFilter.wildcard("*.jar").and(JarFileFilter.wildcard("*-tests.jar"));
        assertFalse(filter.match("lib.jar"));
        assertTrue(filter.match("lib-tests.jar"));
    }

    @Test
    public void testOr() throws Exception {
        final JarFileFilter filter = JarFileFilter.wildcard("*.jar").or(JarFileFilter.wildcard("*-tests.jar"));
        assertTrue(filter.match("lib.jar"));
        assertTrue(filter.match("lib-tests.jar"));
    }

    @Test
    public void testAndNot() throws Exception {
        final JarFileFilter filter = JarFileFilter.wildcard("*.jar").and(JarFileFilter.not(JarFileFilter.wildcard("*-tests.jar")));
        assertTrue(filter.match("lib.jar"));
        assertFalse(filter.match("lib-tests.jar"));
    }

    /**
     * Include *-tests.jar
     * @throws Exception
     */
    @Test
    public void testInclusions() throws Exception {
        final JarFileFilter include = JarFileFilter.none().or(JarFileFilter.wildcard("*-tests.jar"));
        final JarFileFilter exclude = JarFileFilter.none();
        final JarFileFilter filter = JarFileFilter.wildcard("*.jar").and(
                include.and(JarFileFilter.not(exclude))
        );
        assertFalse(filter.match("lib.jar"));
        assertTrue(filter.match("lib-tests.jar"));
        assertTrue(filter.match("app-tests.jar"));
    }

    /**
     * Include *.jar except app-tests.jar
     * @throws Exception
     */
    @Test
    public void testExclusions() throws Exception {
        final JarFileFilter include = JarFileFilter.any();
        final JarFileFilter exclude = JarFileFilter.none().or(JarFileFilter.wildcard("app-tests.jar"));
        final JarFileFilter filter = JarFileFilter.wildcard("*.jar").and(
                include.and(JarFileFilter.not(exclude))
        );
        assertTrue(filter.match("lib.jar"));
        assertTrue(filter.match("lib-tests.jar"));
        assertFalse(filter.match("app-tests.jar"));
    }

    /**
     * Include *-tests.jar except app-tests.jar
     * @throws Exception
     */
    @Test
    public void testInclusionsAndExclusions() throws Exception {
        final JarFileFilter include = JarFileFilter.none().or(JarFileFilter.wildcard("*-tests.jar"));
        final JarFileFilter exclude = JarFileFilter.none().or(JarFileFilter.wildcard("app-tests.jar"));
        final JarFileFilter filter = JarFileFilter.wildcard("*.jar").and(
                include.and(JarFileFilter.not(exclude))
        );
        assertFalse(filter.match("lib.jar"));
        assertTrue(filter.match("lib-tests.jar"));
        assertFalse(filter.match("app-tests.jar"));
    }
}