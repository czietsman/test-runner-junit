package com.redstor.qalab.junit;

import java.util.Optional;

public interface CoverageListener {
    void analysis(byte[] executionData, Optional<CoverageAnalysisResult> analysisResult);
}
