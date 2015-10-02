package com.redstor.qalab.junit;

public class CoverageAnalysisResult {
    private final double instructionsCoveredRatio;
    private final double branchesCoveredRatio;

    public CoverageAnalysisResult(double instructionsCoveredRatio, double branchesCoveredRatio) {
        this.instructionsCoveredRatio = instructionsCoveredRatio;
        this.branchesCoveredRatio = branchesCoveredRatio;
    }

    public double getInstructionsCoveredRatio() {
        return instructionsCoveredRatio;
    }

    public double getBranchesCoveredRatio() {
        return branchesCoveredRatio;
    }
}
