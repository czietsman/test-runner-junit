package com.redstor.qalab.junit.mongo;

import com.google.common.base.Charsets;
import com.mongodb.client.MongoCollection;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.time.Instant;

class MongoTestListener extends RunListener {
    private static final Charset CHARSET = Charsets.UTF_8;
    private final MongoCollection<MongoTestRun> testRuns;
    private final MongoCollection<MongoTestPoint> testPoints;

    enum Outcome {
        PASSED,
        FAILED,
        IGNORED,
        SKIPPED
    }

    private PrintStream defaultStdOut;
    private PrintStream defaultStdErr;
    private MongoTestRun run = new MongoTestRun();
    private MongoTestPoint point = new MongoTestPoint();

    private ByteArrayOutputStream testStdOut;
    private ByteArrayOutputStream testStdErr;
    private Outcome outcome;
    private Failure failure;

    public MongoTestListener(MongoCollection<MongoTestRun> testRuns, MongoCollection<MongoTestPoint> testPoints) {
        this.testRuns = testRuns;
        this.testPoints = testPoints;
        this.defaultStdOut = System.out;
        this.defaultStdErr = System.err;
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        run = new MongoTestRun();
        run.setStartTime(Instant.now());
        testRuns.insertOne(run);
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        run.setEndTime(Instant.now());
        run.setRunCount(result.getRunCount());
        run.setIgnoreCount(result.getIgnoreCount());
        run.setFailureCount(result.getFailureCount());

        testRuns.findOneAndReplace(run.find(), run);
    }

    private PrintStream createPrintStream(ByteArrayOutputStream testStdOut) {
        try {
            return new PrintStream(testStdOut, false, CHARSET.name());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void testStarted(Description description) {
        MongoTestPoint point = new MongoTestPoint();
        point.setRunId(run.getId());
        point.setStartTime(Instant.now());
        point.setClassName(description.getClassName());
        point.setMethodName(description.getMethodName());
        this.point = point;
        this.outcome = Outcome.PASSED;
        this.testPoints.insertOne(point);

        // redirect standard out and err for this test
        testStdOut = new ByteArrayOutputStream();
        testStdErr = new ByteArrayOutputStream();
        System.setOut(createPrintStream(testStdOut));
        System.setErr(createPrintStream(testStdErr));
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
    public void testAssumptionFailure(Failure failure) {
        this.outcome = Outcome.SKIPPED;
    }

    @Override
    public void testFinished(Description description) throws Exception {
        point.setEndTime(Instant.now());
        switch (outcome) {
            case PASSED:
                point.setOutcome("PASSED");
                break;
            case FAILED:
                point.setOutcome("FAILED");
                point.setErrorMessage(failure.getMessage());
                point.setErrorTrace(failure.getTrace());
                break;
            case IGNORED:
                point.setOutcome("IGNORED");
                break;
            case SKIPPED:
                point.setOutcome("SKIPPED");
                break;
        }

        // store standard out and err for this test
        point.setStdOut(new String(testStdOut.toByteArray(), CHARSET));
        point.setStdErr(new String(testStdErr.toByteArray(), CHARSET));

        // restore the standard out and err streams
        System.setOut(defaultStdOut);
        System.setErr(defaultStdErr);

        this.testPoints.findOneAndReplace(point.find(), point);
    }

}

