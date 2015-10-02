package com.redstor.qalab.junit.mongo;

import com.google.common.base.Charsets;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.redstor.qalab.junit.AbstractTestListener;
import com.redstor.qalab.junit.CoverageAgent;
import com.redstor.qalab.junit.CoverageAnalysisResult;
import com.redstor.qalab.junit.Markers;
import org.bson.types.ObjectId;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

class MongoTestListener extends AbstractTestListener {
    private final Logger LOGGER = LoggerFactory.getLogger(MongoTestListener.class);
    private static final Charset CHARSET = Charsets.UTF_8;
    private final MongoCollection<MongoTestRun> testRuns;
    private final MongoCollection<MongoTestPoint> testPoints;
    private final MongoRedirectStrategy redirectStrategy;

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

    private Optional<ByteArrayOutputStream> testStdOut = Optional.empty();
    private Optional<ByteArrayOutputStream> testStdErr = Optional.empty();

    public MongoTestListener(MongoCollection<MongoTestRun> testRuns, MongoCollection<MongoTestPoint> testPoints, Optional<String> runId, MongoRedirectStrategy redirectStrategy) {
        this.runId = runId;
        this.testRuns = testRuns;
        this.testPoints = testPoints;
        this.redirectStrategy = redirectStrategy;
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
        testRuns.findOneAndReplace(run.filterById(), run);
    }

    @Override
    public void analysis(byte[] executionData, Optional<CoverageAnalysisResult> analysisResult) {
        run.setExecutionData(executionData);
        analysisResult.ifPresent(a -> {
            run.setInstructionsCoveredRatio(a.getInstructionsCoveredRatio());
            run.setBranchesCoveredRatio(a.getBranchesCoveredRatio());
        });
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
        engageTestOutputRedirection();
    }

    private void engageTestOutputRedirection() {
        switch (redirectStrategy) {
            case Split:
                testStdOut = Optional.of(new ByteArrayOutputStream());
                testStdErr = Optional.of(new ByteArrayOutputStream());
                System.setOut(createPrintStream(testStdOut.get()));
                System.setErr(createPrintStream(testStdErr.get()));
                break;
            case Combine:
                testStdOut = Optional.of(new ByteArrayOutputStream());
                final PrintStream stream = createPrintStream(testStdOut.get());
                System.setOut(stream);
                System.setErr(stream);
                break;
            case None:
                break;
        }
    }

    private void disengageTestOutputRedirection() {
        System.setOut(defaultStdOut);
        System.setErr(defaultStdErr);
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
                LOGGER.info(Markers.VERBOSE, "{}:{} {}", point.getClassName(), point.getMethodName(), point.getOutcome());
                break;
            case FAILED:
                point.setOutcome("FAILED");
                point.setErrorMessage(failure.getMessage());
                point.setErrorTrace(failure.getTrace());
                LOGGER.error("{}:{} {}", point.getClassName(), point.getMethodName(), point.getOutcome(), failure.getException());
                break;
            case IGNORED:
                point.setOutcome("IGNORED");
                LOGGER.info(Markers.VERBOSE, "{}:{} {}", point.getClassName(), point.getMethodName(), point.getOutcome());
                break;
            case SKIPPED:
                point.setOutcome("SKIPPED");
                LOGGER.info(Markers.VERBOSE, "{}:{} {}", point.getClassName(), point.getMethodName(), point.getOutcome());
                break;
        }


        // store standard out and err for this test
        testStdOut.ifPresent(out -> point.setStdOut(new String(out.toByteArray(), CHARSET)));
        testStdErr.ifPresent(out -> point.setStdErr(new String(out.toByteArray(), CHARSET)));

        // restore the standard out and err streams
        disengageTestOutputRedirection();

        // submit the result
        this.testPoints.replaceOne(point.filterById(), point);
        this.point = null;
    }

}

