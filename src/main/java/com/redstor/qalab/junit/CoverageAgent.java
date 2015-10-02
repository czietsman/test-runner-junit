package com.redstor.qalab.junit;

import java.io.IOException;
import java.util.Optional;

public interface CoverageAgent {
    void reset();

    byte[] getExecutionData(boolean reset);

    void publish(CoverageListener listener, Optional<AnalyseCoverageRequest> analyseCoverageRequest, Optional<ExportCoverageRequest> exportCoverageRequest) throws IOException;
}
