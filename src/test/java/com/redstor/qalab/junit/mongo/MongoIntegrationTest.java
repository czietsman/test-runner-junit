package com.redstor.qalab.junit.mongo;

import com.google.common.collect.Iterables;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.redstor.qalab.junit.AbstractTestListener;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class MongoIntegrationTest {
    @Before
    public void setUp() throws Exception {
        final MongoClient mongo = new MongoClient();
        mongo.dropDatabase("tests");
        mongo.close();
    }

    @Test
    public void testBuild() throws Exception {

        final MongoTestListenerBuilder builder = new MongoTestListenerBuilder();
        final ObjectId runId = ObjectId.get();
        final AbstractTestListener mongoListener = builder.host("localhost")
                .port(27017)
                .databaseName("tests")
                .redirectStrategy(MongoRedirectStrategy.None)
                .testRunsCollectionName("runs")
                .testPointsCollectionName("points")
                .runId(runId.toHexString())
                .build();

        final Result result = new Result();
        final Description test = Description.createTestDescription(MongoIntegrationTest.class, "testBuild");
        for (RunListener listener : Arrays.asList(result.createListener(), mongoListener)) {
            listener.testRunStarted(Description.createSuiteDescription(MongoIntegrationTest.class));
            listener.testStarted(test);
            listener.testFinished(test);
            listener.testRunFinished(result);
        }

        final MongoClient mongo = new MongoClient();
        final MongoDatabase database = mongo.getDatabase("tests");
        final MongoCollection<Document> runs = database.getCollection("runs");
        assertEquals(1, runs.count());

        final Document run = Iterables.getOnlyElement(runs.find());
        assertEquals(runId, run.getObjectId("_id"));

        final MongoCollection<Document> points = database.getCollection("points");
        assertEquals(1, points.count());

        final Document point = Iterables.getOnlyElement(points.find());
        assertEquals(runId, point.getObjectId("runId"));
        assertEquals("PASSED", point.getString("outcome"));

    }
}