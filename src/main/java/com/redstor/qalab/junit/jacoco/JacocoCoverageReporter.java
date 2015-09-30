package com.redstor.qalab.junit.jacoco;

import com.redstor.qalab.junit.ClassesFinder;
import org.jacoco.agent.rt.IAgent;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.html.HTMLFormatter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

class JacocoCoverageReporter {
    private final IAgent agent;

    public JacocoCoverageReporter(IAgent agent) {
        this.agent = agent;
    }

    public void publish(ClassesFinder classesFinder) throws IOException {
        final ExecutionDataReader reader = new ExecutionDataReader(new ByteArrayInputStream(agent.getExecutionData(false)));
        final ExecutionDataStore executionDataStore = new ExecutionDataStore();
        reader.setExecutionDataVisitor(executionDataStore);
        final SessionInfoStore sessionInfoStore = new SessionInfoStore();
        reader.setSessionInfoVisitor(sessionInfoStore);
        reader.read();

        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);

        classesFinder.find((name, in) -> analyzer.analyzeClass(in, name));

        final IBundleCoverage bundle = coverageBuilder.getBundle("");
        publish(bundle, sessionInfoStore, executionDataStore, new File("report"), new File("source"));
    }

    private void publish(final IBundleCoverage bundleCoverage, SessionInfoStore sessionInfoStore, ExecutionDataStore executionDataStore, File reportDirectory, File sourceDirectory)
            throws IOException {

        // Create a concrete report visitor based on some supplied
        // configuration. In this case we use the defaults
        final HTMLFormatter htmlFormatter = new HTMLFormatter();
        final IReportVisitor visitor = htmlFormatter.createVisitor(new FileMultiReportOutput(reportDirectory));

        // Initialize the report with all of the execution and session
        // information. At this point the report doesn't know about the
        // structure of the report being created
        visitor.visitInfo(sessionInfoStore.getInfos(), executionDataStore.getContents());

        // Populate the report structure with the bundle coverage information.
        // Call visitGroup if you need groups in your report.
        visitor.visitBundle(bundleCoverage, new DirectorySourceFileLocator(sourceDirectory, "utf-8", 4));

        // Signal end of structure information to allow report to write all
        // information out
        visitor.visitEnd();
    }
}
