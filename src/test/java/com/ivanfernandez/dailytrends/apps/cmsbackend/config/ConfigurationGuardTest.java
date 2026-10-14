package com.ivanfernandez.dailytrends.apps.cmsbackend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class ConfigurationGuardTest {

    private final ConfigurationGuard guard = new ConfigurationGuard();

    @Test
    void accepts_the_default_dev_profile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setDefaultProfiles("dev");

        assertThatCode(() -> check(environment)).doesNotThrowAnyException();
    }

    @Test
    void accepts_the_test_profile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        assertThatCode(() -> check(environment)).doesNotThrowAnyException();
    }

    @Test
    void accepts_production_with_a_mongodb_uri() {
        MockEnvironment environment =
                new MockEnvironment().withProperty("spring.mongodb.uri", "mongodb://db:27017/daily-trends");
        environment.setActiveProfiles("production");

        assertThatCode(() -> check(environment)).doesNotThrowAnyException();
    }

    @Test
    void rejects_an_unknown_profile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        InvalidConfiguration error = catchThrowableOfType(InvalidConfiguration.class, () -> check(environment));

        assertThat(error.property()).isEqualTo("spring.profiles.active");
    }

    @Test
    void rejects_more_than_one_profile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev", "test");

        InvalidConfiguration error = catchThrowableOfType(InvalidConfiguration.class, () -> check(environment));

        assertThat(error.property()).isEqualTo("spring.profiles.active");
    }

    @Test
    void rejects_production_without_a_mongodb_uri() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");

        InvalidConfiguration error = catchThrowableOfType(InvalidConfiguration.class, () -> check(environment));

        assertThat(error.property()).isEqualTo("spring.mongodb.uri");
        assertThat(error).hasMessageContaining("spring.mongodb.uri");
    }

    @Test
    void never_echoes_the_mongodb_uri() {
        MockEnvironment environment =
                new MockEnvironment().withProperty("spring.mongodb.uri", "mongodb://user:secret@host/db");
        environment.setActiveProfiles("prod");

        InvalidConfiguration error = catchThrowableOfType(InvalidConfiguration.class, () -> check(environment));

        assertThat(error).message().doesNotContain("secret");
    }

    private void check(MockEnvironment environment) {
        guard.postProcessEnvironment(environment, new SpringApplication());
    }
}
