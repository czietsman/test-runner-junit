package com.redstor.qalab.junit.mongo;

import com.google.common.base.Charsets;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.redstor.qalab.junit.CoverageAgent;
import org.bson.types.ObjectId;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

class MongoTestListener extends RunListener {
    private static final Charset CHARSET = Charsets.UTF_8;
    private final MongoCollection<MongoTestRun> testRuns;
    private final MongoCollection<MongoTestPoint> testPoints;
    private final Optional<CoverageAgent> agent;

    enum Outcome {
        PASSED,
        FAILED,
        IGNORED,
        SKIPPED
    }

    private PrintStream defaultStdOut;
    private PrintStream defaultStdErr;

    private Optional<String> runId;
    private MongoTestRun run = new MongoTestRun();
    private MongoTestPoint point = null;

    private ByteArrayOutputStream testStdOut;
    private ByteArrayOutputStream testStdErr;

    public MongoTestListener(MongoCollection<MongoTestRun> testRuns, MongoCollection<MongoTestPoint> testPoints, Optional<String> runId, Optional<CoverageAgent> agent) {
        this.runId = runId;
        this.testRuns = testRuns;
        this.testPoints = testPoints;
        this.agent = agent;
        this.defaultStdOut = System.out;
        this.defaultStdErr = System.err;
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        if (runId.isPresent()) {
            final ObjectId runObjectId = new ObjectId(runId.get());
            final FindIterable<MongoTestRun> iterable = testRuns.find(MongoTestRun.filterById(runObjectId));
            final MongoCursor<MongoTestRun> iterator = iterable.iterator();
            if (iterator.hasNext()) {
                run = iterator.next();
                checkState(!iterator.hasNext(), "run must be unique");

                initTestRun(run);
                testRuns.replaceOne(run.filterById(), run);
            } else {
                run = new MongoTestRun(runObjectId);
                initTestRun(run);
                testRuns.insertOne(run);
            }
        } else {
            run = new MongoTestRun();
            initTestRun(run);
            testRuns.insertOne(run);
        }
    }

    private void initTestRun(MongoTestRun run) {
        run.setStartTime(Instant.now());
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        run.setEndTime(Instant.now());
        run.setRunCount(result.getRunCount());
        run.setIgnoreCount(result.getIgnoreCount());
        run.setFailureCount(result.getFailureCount());
        agent.ifPresent(a -> run.setExecutionData(a.getExecutionData(false)));

        testRuns.findOneAndReplace(run.filterById(), run);
    }

    private PrintStream createPrintStream(ByteArrayOutputStream testStdOut) {
        try {
            return new PrintStream(testStdOut, false, CHARSET.name());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void testIgnored(Description description) {
        testStarted(description);
        submit(Outcome.IGNORED, null);
    }

    @Override
    public void testStarted(Description description) {
        MongoTestPoint point = new MongoTestPoint();
        point.setRunId(run.getId());
        point.setStartTime(Instant.now());
        point.setClassName(description.getClassName());
        point.setMethodName(description.getMethodName());

        this.point = point;
        this.testPoints.insertOne(point);

        // redirect standard out and err for this test
        testStdOut = new ByteArrayOutputStream();
        testStdErr = new ByteArrayOutputStream();
        System.setOut(createPrintStream(testStdOut));
        System.setErr(createPrintStream(testStdErr));
    }

    @Override
    public void testFailure(Failure failure) {
        submit(Outcome.FAILED, failure);
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        submit(Outcome.SKIPPED, failure);
    }

    @Override
    public void testFinished(Description description) throws Exception {
        submit(Outcome.PASSED, null);
    }

    private void submit(Outcome outcome, Failure failure) {
        // only submit a tet point only once
        if (point == null) {
            return;
        }

        // update the point fields depending on the outcome
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

        // submit the result
        this.testPoints.findOneAndReplace(point.filterById(), point);
        this.point = null;
    }

}

