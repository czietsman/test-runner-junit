package com.redstor.qalab.junit.mongo;

import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Date;

import static com.mongodb.client.model.Filters.eq;

class MongoTestRun implements Bson {

    private final Document document;

    public MongoTestRun() {
        this(ObjectId.get());
    }

    MongoTestRun(Document document) {
        this.document = document;
    }

    public MongoTestRun(ObjectId id) {
        this(new Document());
        setId(id);
    }

    public ObjectId getId() {
        return document.getObjectId("_id");
    }

    public void setId(ObjectId id) {
        document.put("_id", id);
    }

    public Instant getStartTime() {
        return document.get("startTime", Date.class).toInstant();
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

    public Integer getRunCount() {
        return document.getInteger("runCount");
    }

    public void setRunCount(int runCount) {
        document.put("runCount", runCount);
    }

    public Integer getIgnoreCount() {
        return document.getInteger("ignoreCount");
    }

    public void setIgnoreCount(int ignoreCount) {
        document.put("ignoreCount", ignoreCount);
    }

    public Integer getFailureCount() {
        return document.getInteger("failureCount");
    }

    public void setFailureCount(int failureCount) {
        document.put("failureCount", failureCount);
    }

    public byte[] getExecutionData() {
        return document.get("executionData", byte[].class);
    }

    public void setExecutionData(byte[] data) {
        if (data == null) {
            document.remove("executionData");
        } else {
            document.put("executionData", data);
        }
    }

    @Override
    public <TDocument> BsonDocument toBsonDocument(Class<TDocument> tDocumentClass, CodecRegistry codecRegistry) {
        return document.toBsonDocument(tDocumentClass, codecRegistry);
    }

    public Bson filterById() {
        return filterById(getId());
    }

    public static Bson filterById(ObjectId id) {
        return eq("_id", id);
    }
}
