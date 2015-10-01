package com.redstor.qalab.junit;

import java.io.InputStream;

public interface ClassCollector {
    void collect(String name, InputStream in) throws Throwable;
}
