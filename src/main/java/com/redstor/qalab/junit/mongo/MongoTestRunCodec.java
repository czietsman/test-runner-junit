package com.redstor.qalab.junit.mongo;

import org.bson.BsonDocument;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

class MongoTestRunCodec implements Codec<MongoTestRun> {
    private final CodecRegistry codecRegistry;

    public MongoTestRunCodec(CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    @Override
    public MongoTestRun decode(BsonReader reader, DecoderContext decoderContext) {
        final Document document = codecRegistry.get(Document.class).decode(reader, decoderContext);
        return new MongoTestRun(document);
    }

    @Override
    public void encode(BsonWriter writer, MongoTestRun value, EncoderContext encoderContext) {
        final BsonDocument document = value.toBsonDocument(Document.class, codecRegistry);
        final BsonDocumentCodec codec = new BsonDocumentCodec();
        codec.encode(writer, document, encoderContext);
    }

    @Override
    public Class<MongoTestRun> getEncoderClass() {
        return MongoTestRun.class;
    }
}
