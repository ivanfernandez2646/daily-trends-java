package com.ivanfernandez.dailytrends.apps.cmsbackend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

class InvalidConfigurationFailureAnalyzerTest {

    @Test
    void describes_the_property_and_how_to_fix_it() {
        var failure = new InvalidConfiguration("spring.mongodb.uri", "is required by the production profile");

        FailureAnalysis analysis = new InvalidConfigurationFailureAnalyzer().analyze(failure);

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("spring.mongodb.uri", "is required by the production profile");
        assertThat(analysis.getAction()).contains("SPRING_MONGODB_URI");
    }
}
