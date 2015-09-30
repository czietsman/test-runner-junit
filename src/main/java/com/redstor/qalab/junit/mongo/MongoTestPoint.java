package com.redstor.qalab.junit.mongo;

import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.sql.Date;
import java.time.Instant;

import static com.mongodb.client.model.Filters.eq;

class MongoTestPoint implements Bson {

    private final Document document;

    public MongoTestPoint() {
        this(ObjectId.get());
    }

    MongoTestPoint(Document document) {
        this.document = document;
    }

    public MongoTestPoint(ObjectId id) {
        this(new Document());
        setId(id);
    }

    public ObjectId getId() {
        return document.getObjectId("_id");
    }

    public void setId(ObjectId id) {
        document.put("_id", id);
    }

    public ObjectId getRunId() {
        return document.getObjectId("runId");
    }

    public void setRunId(ObjectId id) {
        document.put("runId", id);
    }

    public Instant getStartTime() {
        return document.getDate("startTime").toInstant();
    }

    public void setStartTime(Instant startTime) {
        document.put("startTime", Date.from(startTime));
    }

    public Instant getEndTime() {
        return document.get("endTime", Date.class).toInstant();
    }

    public void setEndTime(Instant endTime) {
        document.put("endTime", Date.from(endTime));
    }

    public void setClassName(String className) {
        document.put("className", className);
    }

    public String getClassName() {
        return document.getString("className");
    }

    public void setMethodName(String methodName) {
        document.put("methodName", methodName);
    }

    public String getMethodName() {
        return document.getString("methodName");
    }

    public String getOutcome() {
        return document.getString("outcome");
    }

    public void setOutcome(String outcome) {
        document.put("outcome", outcome);
    }

    public String getErrorMessage() {
        return document.getString("errorMessage");
    }

    public void setErrorMessage(String message) {
        document.put("errorMessage", message);
    }

    public String getErrorTrace() {
        return document.getString("errorTrace");
    }

    public void setErrorTrace(String trace) {
        document.put("errorTrace", trace);
    }

    public String getStdOut() {
        return document.getString("stdOut");
    }

    public void setStdOut(String stdOut) {
        document.put("stdOut", stdOut);
    }

    public String getStdErr() {
        return document.getString("stdErr");
    }

    public void setStdErr(String stdErr) {
        document.put("stdErr", stdErr);
    }

    @Override
    public <TDocument> BsonDocument toBsonDocument(Class<TDocument> tDocumentClass, CodecRegistry codecRegistry) {
        return document.toBsonDocument(tDocumentClass, codecRegistry);
    }

    public Bson find() {
        return eq("_id", getId());
    }
}

