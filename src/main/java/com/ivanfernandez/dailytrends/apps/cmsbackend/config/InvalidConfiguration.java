package com.ivanfernandez.dailytrends.apps.cmsbackend.config;

/** A startup configuration error. It names the property and never carries its value. */
public final class InvalidConfiguration extends RuntimeException {

    private final String property;
    private final String reason;

    public InvalidConfiguration(String property, String reason) {
        super("Invalid configuration property '%s': %s".formatted(property, reason));
        this.property = property;
        this.reason = reason;
    }

    public String property() {
        return property;
    }

    public String reason() {
        return reason;
    }
}
