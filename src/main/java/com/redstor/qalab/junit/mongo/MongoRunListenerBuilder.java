package com.redstor.qalab.junit.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.redstor.qalab.junit.CoverageAgent;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.runner.notification.RunListener;

import java.util.Optional;

public class MongoRunListenerBuilder {
    private String host = "localhost";
    private int port = 27017;
    private String databaseName = "junit-tests";
    private String testRunsCollectionName = "testruns";
    private String testPointsCollectionName = "testpoints";
    private String runId = null;
    private CoverageAgent agent = null;

    public MongoRunListenerBuilder host(final String host) {
        this.host = host;
        return this;
    }

    public MongoRunListenerBuilder port(final int port) {
        this.port = port;
        return this;
    }

    public MongoRunListenerBuilder databaseName(final String databaseName) {
        this.databaseName = databaseName;
        return this;
    }

    public MongoRunListenerBuilder testRunsCollectionName(final String testRunsCollectionName) {
        this.testRunsCollectionName = testRunsCollectionName;
        return this;
    }

    public MongoRunListenerBuilder testPointsCollectionName(final String testPointsCollectionName) {
        this.testPointsCollectionName = testPointsCollectionName;
        return this;
    }

    public MongoRunListenerBuilder runId(final String runId) {
        this.runId = runId;
        return this;
    }

    public MongoRunListenerBuilder agent(final CoverageAgent agent) {
        this.agent = agent;
        return this;
    }

    public RunListener build() {
        final CodecRegistry defaultCodecRegistry = MongoClient.getDefaultCodecRegistry();
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                CodecRegistries.fromCodecs(
                        new MongoTestPointCodec(defaultCodecRegistry),
                        new MongoTestRunCodec(defaultCodecRegistry)
                ),
                defaultCodecRegistry
        );

        final ServerAddress address = new ServerAddress(host, port);
        final MongoClientOptions options = new MongoClientOptions.Builder().codecRegistry(codecRegistry).build();
        final MongoClient mongo = new MongoClient( address, options );
        final MongoDatabase database = mongo.getDatabase(databaseName);
        final MongoCollection<MongoTestRun> testRuns = database.getCollection(testRunsCollectionName, MongoTestRun.class);
        final MongoCollection<MongoTestPoint> testPoints = database.getCollection(testPointsCollectionName, MongoTestPoint.class);
        return new MongoTestListener(testRuns, testPoints, Optional.ofNullable(runId), Optional.ofNullable(agent));
    }
}
