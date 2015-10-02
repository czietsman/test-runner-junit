package com.redstor.qalab.junit;

import org.junit.runner.notification.RunListener;

import java.util.Optional;

public abstract class AbstractTestListener extends RunListener implements CoverageListener {
    @Override
    public void analysis(byte[] executionData, Optional<CoverageAnalysisResult> analysisResult) {
    }
}
