package com.redstor.qalab.junit.mongo;

import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;

public class MongoTestPointTest {
    @Test
    public void testTimes() throws Exception {

        final MongoTestPoint point = new MongoTestPoint();
        final Instant start = Instant.now();
        final Instant end = start.plus(1, ChronoUnit.SECONDS);

        point.setStartTime(start);
        assertEquals(start, point.getStartTime());

        point.setEndTime(end);
        assertEquals(end, point.getEndTime());

    }
}