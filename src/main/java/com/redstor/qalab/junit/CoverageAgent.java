package com.redstor.qalab.junit;

import java.io.IOException;

public interface CoverageAgent {
    void reset();

    byte[] getExecutionData(boolean reset);

    void publish(ClassesFinder classesFinder) throws IOException;
}
