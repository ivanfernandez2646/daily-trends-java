package com.ivanfernandez.dailytrends.apps.cmsbackend.config;

import java.util.Locale;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class InvalidConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<InvalidConfiguration> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, InvalidConfiguration cause) {
        String property = cause.property();
        String variable = property.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
        return new FailureAnalysis(
                "Invalid configuration property '%s': %s.".formatted(property, cause.reason()),
                "Set %s (environment variable %s) or fix it in config/application.properties."
                        .formatted(property, variable),
                cause);
    }
}
