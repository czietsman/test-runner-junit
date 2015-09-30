package com.redstor.qalab.junit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class is an adaption of the code found on StackOverflow
 *
 * @author http://stackoverflow.com/a/60766
 */
class ClassPathHack {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassPathHack.class);
    private static final Class<?>[] parameters = new Class<?>[]{URL.class};

    public static void addFile(File f) {
        try {
            addURL(f.toURI().toURL());
        } catch (IOException ex) {
            LOGGER.error("Failed to add jar '{}' to classpath", f, ex);
        }
    }

    public static void addURL(URL u) throws IOException {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class sysclass = URLClassLoader.class;

        try {
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(sysloader, u);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }

    }
}
