package com.ivanfernandez.dailytrends.contexts.cms.shared.domain;

/** Error kind for a state that already exists. Answers 409. */
public abstract class ConflictError extends RuntimeException {

    protected ConflictError(String message) {
        super(message);
    }
}
