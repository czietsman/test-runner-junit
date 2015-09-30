package com.redstor.qalab.junit.jacoco;

import com.redstor.qalab.junit.ClassesFinder;
import com.redstor.qalab.junit.CoverageAgent;
import org.jacoco.agent.rt.IAgent;
import org.jacoco.agent.rt.RT;

import java.io.IOException;

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
    public void publish(ClassesFinder classesFinder) throws IOException {
        final JacocoCoverageReporter publisher = new JacocoCoverageReporter(agent);
        publisher.publish(classesFinder);
    }
}
