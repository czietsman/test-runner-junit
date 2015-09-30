package com.redstor.qalab.junit.mongo;

import org.bson.*;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

class MongoTestPointCodec implements Codec<MongoTestPoint> {
    private final CodecRegistry codecRegistry;

    public MongoTestPointCodec(CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    @Override
    public MongoTestPoint decode(BsonReader reader, DecoderContext decoderContext) {
        final Document document = codecRegistry.get(Document.class).decode(reader, decoderContext);
        return new MongoTestPoint(document);
    }

    @Override
    public void encode(BsonWriter writer, MongoTestPoint value, EncoderContext encoderContext) {
        final BsonDocument document = value.toBsonDocument(Document.class, codecRegistry);
        final BsonDocumentCodec codec = new BsonDocumentCodec();
        codec.encode(writer, document, encoderContext);
    }

    @Override
    public Class<MongoTestPoint> getEncoderClass() {
        return MongoTestPoint.class;
    }
}

