package com.redstor.qalab.junit;

import org.junit.internal.TextListener;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

import java.io.PrintStream;

class TestListener extends TextListener {
    private final PrintStream writer;

    enum Outcome {
        PASSED,
        FAILED,
        IGNORED
    }

    public TestListener(PrintStream writer) {
        super(writer);
        this.writer = writer;
    }

    private long startTime;
    private Outcome outcome;
    private Failure failure;

    @Override
    public void testStarted(Description description) {
        this.startTime = System.currentTimeMillis();
        this.outcome = Outcome.PASSED;
        this.failure = null;
    }

    @Override
    public void testFailure(Failure failure) {
        this.outcome = Outcome.FAILED;
        this.failure = failure;
    }

    @Override
    public void testIgnored(Description description) {
        this.outcome = Outcome.IGNORED;
    }

    @Override
    public void testFinished(Description description) throws Exception {
        long duration = System.currentTimeMillis() - startTime;
        switch (outcome) {
            case PASSED:
                writer.println("PASSED (" + duration + " ms): " + description);
                break;
            case FAILED:
                writer.println("FAILED (" + duration + " ms): " + failure);
                break;
            case IGNORED:
                writer.println("IGNORED (" + duration + " ms): " + description);
                break;
        }
    }

}

