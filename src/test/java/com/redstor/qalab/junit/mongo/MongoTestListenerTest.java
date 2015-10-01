package com.redstor.qalab.junit.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.UpdateResult;
import com.redstor.qalab.junit.CoverageAgent;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.easymock.Capture;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;

import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class MongoTestListenerTest {

    private IMocksControl control;
    private MongoCollection<MongoTestRun> testRuns;
    private MongoCollection<MongoTestPoint> testPoints;

    @Before
    public void setUp() throws Exception {
        control = createControl();
        testRuns = createMongoCollectionMock();
        testPoints = createMongoCollectionMock();
    }

    @SuppressWarnings("unchecked")
    private <T> MongoCollection<T> createMongoCollectionMock() {
        return control.createMock(MongoCollection.class);
    }

    @SuppressWarnings("unchecked")
    private <T> FindIterable<T> createFindIterableMock() {
        return control.createMock(FindIterable.class);
    }

    @SuppressWarnings("unchecked")
    private <T> MongoCursor<T> createMongoCursorMock() {
        return control.createMock(MongoCursor.class);
    }

    @Test
    public void testTestRunStarted_RunIdUnspecified() throws Exception {

        final MongoTestListener listener = new MongoTestListener(testRuns, testPoints, Optional.<String>empty(), Optional.<CoverageAgent>empty(), MongoRedirectStrategy.Split);
        final Description description = Description.createTestDescription(MongoTestListenerTest.class, "testTestRunStarted");

        testRuns.insertOne(anyObject());

        control.replay();

        listener.testRunStarted(description);

        control.verify();

    }

    @Test
    public void testTestRunStarted_NewRunId() throws Exception {

        final ObjectId runId = new ObjectId();
        final MongoTestListener listener = new MongoTestListener(testRuns, testPoints, Optional.of(runId.toHexString()), Optional.<CoverageAgent>empty(), MongoRedirectStrategy.Split);
        final Description description = Description.createTestDescription(MongoTestListenerTest.class, "testTestRunStarted");
        final FindIterable<MongoTestRun> findResult = createFindIterableMock();
        final MongoCursor<MongoTestRun> findCursor = createMongoCursorMock();

        expect(testRuns.find(anyObject(Bson.class))).andReturn(findResult);
        expect(findResult.iterator()).andReturn(findCursor);
        expect(findCursor.hasNext()).andReturn(false);

        final Capture<MongoTestRun> runOut = newCapture();
        testRuns.insertOne(capture(runOut));

        control.replay();

        listener.testRunStarted(description);

        control.verify();

        final MongoTestRun run = runOut.getValue();
        assertEquals(runId, run.getId());
    }

    @Test
    public void testTestRunStarted_ExistingRunId() throws Exception {

        final ObjectId runId = new ObjectId();
        final MongoTestListener listener = new MongoTestListener(testRuns, testPoints, Optional.of(runId.toHexString()), Optional.<CoverageAgent>empty(), MongoRedirectStrategy.Split);
        final Description description = Description.createTestDescription(MongoTestListenerTest.class, "testTestRunStarted");
        final FindIterable<MongoTestRun> findResult = createFindIterableMock();
        final MongoCursor<MongoTestRun> findCursor = createMongoCursorMock();

        final MongoTestRun run = new MongoTestRun(runId);

        expect(testRuns.find(anyObject(Bson.class))).andReturn(findResult);
        expect(findResult.iterator()).andReturn(findCursor);
        expect(findCursor.hasNext()).andReturn(true);
        expect(findCursor.next()).andReturn(run);
        expect(findCursor.hasNext()).andReturn(false);
        expect(testRuns.replaceOne(anyObject(), eq(run))).andReturn(UpdateResult.unacknowledged());

        control.replay();

        listener.testRunStarted(description);

        control.verify();

    }
}