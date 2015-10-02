package com.redstor.qalab.junit.jacoco;

import com.redstor.qalab.junit.ExportCoverageRequest;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.html.HTMLFormatter;

import java.io.File;
import java.io.IOException;

class JacocoCoverageReporter {
    private final File sourceDir;
    private final File reportDir;

    public JacocoCoverageReporter(ExportCoverageRequest request) {
        this.sourceDir = request.getSourceDir();
        this.reportDir = request.getReportDir();
    }

    public void publish(final IBundleCoverage bundleCoverage, SessionInfoStore sessionInfoStore, ExecutionDataStore executionDataStore)
            throws IOException {

        // Create a concrete report visitor based on some supplied
        // configuration. In this case we use the defaults
        final HTMLFormatter htmlFormatter = new HTMLFormatter();
        final IReportVisitor visitor = htmlFormatter.createVisitor(new FileMultiReportOutput(reportDir));

        // Initialize the report with all of the execution and session
        // information. At this point the report doesn't know about the
        // structure of the report being created
        visitor.visitInfo(sessionInfoStore.getInfos(), executionDataStore.getContents());

        // Populate the report structure with the bundle coverage information.
        // Call visitGroup if you need groups in your report.
        visitor.visitBundle(bundleCoverage, new DirectorySourceFileLocator(sourceDir, "utf-8", 4));

        // Signal end of structure information to allow report to write all
        // information out
        visitor.visitEnd();
    }
}
