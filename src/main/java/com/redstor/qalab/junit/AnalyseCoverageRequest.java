package com.redstor.qalab.junit;

public class AnalyseCoverageRequest {
    private ClassesFinder classesFinder;

    public AnalyseCoverageRequest(ClassesFinder classesFinder) {
        this.classesFinder = classesFinder;
    }

    public ClassesFinder getClassesFinder() {
        return classesFinder;
    }
}
