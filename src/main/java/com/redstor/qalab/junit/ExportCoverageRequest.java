package com.redstor.qalab.junit;

import java.io.File;

public class ExportCoverageRequest {
    private File sourceDir;
    private File reportDir;

    public ExportCoverageRequest(File sourceDir, File reportDir) {
        this.sourceDir = sourceDir;
        this.reportDir = reportDir;
    }

    public File getSourceDir() {
        return sourceDir;
    }

    public File getReportDir() {
        return reportDir;
    }
}