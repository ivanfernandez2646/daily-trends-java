package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.InvalidArgumentError;
import java.util.regex.Pattern;

/** UUID versions 1 to 5 or the nil UUID, as Node's {@code uuid@9} validates; a v7 UUID is invalid. */
public record FeedId(String value) {

    private static final Pattern UUID_V1_TO_V5_OR_NIL = Pattern.compile(
            "^(?:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"
                    + "|00000000-0000-0000-0000-000000000000)$",
            Pattern.CASE_INSENSITIVE);

    public FeedId {
        if (value == null || !UUID_V1_TO_V5_OR_NIL.matcher(value).matches()) {
            throw new InvalidArgumentError("<FeedId> does not allow the value <" + value + ">");
        }
    }
}
