package com.redstor.qalab.junit;

import java.io.File;
import java.io.IOException;

public class NoCoverageAgent implements CoverageAgent {
    @Override
    public void reset() {
    }

    @Override
    public byte[] getExecutionData(boolean reset) {
        return null;
    }

    @Override
    public void publish(ClassesFinder classesFinder, File sourceDir, File reportDir) throws IOException {
    }
}
