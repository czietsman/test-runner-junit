package com.redstor.qalab.junit;

import java.io.InputStream;

public interface ClassCollector {
    void loadClass(String name, InputStream in) throws Throwable;
}
