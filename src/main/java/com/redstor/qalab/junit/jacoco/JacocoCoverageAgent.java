package com.redstor.qalab.junit.jacoco;

import com.redstor.qalab.junit.*;
import org.jacoco.agent.rt.IAgent;
import org.jacoco.agent.rt.RT;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

public class JacocoCoverageAgent implements CoverageAgent {
    private final IAgent agent;

    public JacocoCoverageAgent() {
        agent = RT.getAgent();
    }

    @Override
    public void reset() {
        agent.reset();
    }

    @Override
    public byte[] getExecutionData(boolean reset) {
        return agent.getExecutionData(reset);
    }

    @Override
    public void publish(CoverageListener listener, Optional<AnalyseCoverageRequest> analyseCoverageRequest, Optional<ExportCoverageRequest> exportCoverageRequest) throws IOException {
        final byte[] executionData = agent.getExecutionData(false);
        if (!analyseCoverageRequest.isPresent()) {
            listener.analysis(executionData, Optional.empty());
            return;
        }

        final ClassesFinder classesFinder = analyseCoverageRequest.get().getClassesFinder();
        final ExecutionDataReader reader = new ExecutionDataReader(new ByteArrayInputStream(executionData));
        final ExecutionDataStore executionDataStore = new ExecutionDataStore();
        reader.setExecutionDataVisitor(executionDataStore);
        final SessionInfoStore sessionInfoStore = new SessionInfoStore();
        reader.setSessionInfoVisitor(sessionInfoStore);
        reader.read();

        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);
        classesFinder.find((name, in) -> analyzer.analyzeClass(in, name));

        final IBundleCoverage bundle = coverageBuilder.getBundle("");
        final CoverageAnalysisResult analysisResult = new CoverageAnalysisResult(
                bundle.getInstructionCounter().getCoveredRatio(),
                bundle.getBranchCounter().getCoveredRatio()
        );
        listener.analysis(executionData, Optional.of(analysisResult));

        if (exportCoverageRequest.isPresent()) {
            final JacocoCoverageReporter publisher = new JacocoCoverageReporter(exportCoverageRequest.get());
            publisher.publish(bundle, sessionInfoStore, executionDataStore);
        }
    }
}
