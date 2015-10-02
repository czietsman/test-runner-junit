package com.redstor.qalab.junit;

import java.io.IOException;
import java.util.Optional;

public class NoCoverageAgent implements CoverageAgent {
    @Override
    public void reset() {
    }

    @Override
    public byte[] getExecutionData(boolean reset) {
        return null;
    }

    @Override
    public void publish(CoverageListener listener, Optional<AnalyseCoverageRequest> analyseCoverageRequest, Optional<ExportCoverageRequest> exportCoverageRequest) throws IOException {
    }
}
