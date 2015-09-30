package com.redstor.qalab.junit;

public interface ClassesFinder {
    <T extends ClassCollector> T find(T collector);
}
