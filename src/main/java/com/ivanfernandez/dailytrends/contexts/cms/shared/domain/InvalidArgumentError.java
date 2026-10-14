package com.ivanfernandez.dailytrends.contexts.cms.shared.domain;

/** Error kind for a value the domain rejects. Answers 400. */
public class InvalidArgumentError extends RuntimeException {

    public InvalidArgumentError(String message) {
        super(message);
    }
}
