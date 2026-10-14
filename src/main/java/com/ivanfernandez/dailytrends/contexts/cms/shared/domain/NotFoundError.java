package com.ivanfernandez.dailytrends.contexts.cms.shared.domain;

/** Error kind for a missing resource. Answers 404. */
public abstract class NotFoundError extends RuntimeException {

    protected NotFoundError(String message) {
        super(message);
    }
}
