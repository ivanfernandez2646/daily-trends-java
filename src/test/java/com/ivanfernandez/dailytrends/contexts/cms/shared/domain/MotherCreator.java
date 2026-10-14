package com.ivanfernandez.dailytrends.contexts.cms.shared.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Random primitive values for Object Mothers. */
public final class MotherCreator {

    private static final List<String> WORDS = List.of(
            "economy", "election", "football", "storm", "market", "festival", "court", "science", "budget", "rail");

    private MotherCreator() {}

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String words(int count) {
        var random = ThreadLocalRandom.current();
        var builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(WORDS.get(random.nextInt(WORDS.size())));
        }
        return builder.toString();
    }

    public static <T> T oneOf(T[] values) {
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    public static Instant instant() {
        long start = Instant.parse("2020-01-01T00:00:00Z").toEpochMilli();
        long end = Instant.parse("2030-01-01T00:00:00Z").toEpochMilli();
        return Instant.ofEpochMilli(ThreadLocalRandom.current().nextLong(start, end));
    }
}
