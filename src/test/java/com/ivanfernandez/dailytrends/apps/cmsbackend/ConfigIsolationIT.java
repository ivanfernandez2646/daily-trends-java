package com.ivanfernandez.dailytrends.apps.cmsbackend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

@IntegrationTest
class ConfigIsolationIT {

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    void reads_no_configuration_from_the_working_directory() {
        assertThat(environment.getPropertySources().stream().map(PropertySource::getName))
                .noneMatch(name -> name.contains("file ["));
    }

    @Test
    void runs_with_the_test_profile() {
        assertThat(environment.getActiveProfiles()).containsExactly("test");
    }
}
