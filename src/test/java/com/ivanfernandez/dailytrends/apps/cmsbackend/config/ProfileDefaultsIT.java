package com.ivanfernandez.dailytrends.apps.cmsbackend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/** Starts only the configuration (no web server, no MongoDB) with a fake operating-system environment. */
class ProfileDefaultsIT {

    @Test
    void uses_the_global_defaults() {
        try (var context = start(Map.of())) {
            DailyTrendsProperties properties = context.getBean(DailyTrendsProperties.class);

            assertThat(properties.timezone()).isEqualTo(ZoneId.of("Europe/Madrid"));
            assertThat(properties.scraping().timeout()).isEqualTo(Duration.ofSeconds(10));
            assertThat(properties.scraping().cooldown()).isEqualTo(Duration.ofMinutes(5));
            assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("5000");
        }
    }

    @Test
    void uses_the_dev_profile_by_default() {
        try (var context = start(Map.of())) {
            assertThat(context.getEnvironment().getActiveProfiles()).isEmpty();
            assertThat(context.getEnvironment().getDefaultProfiles()).containsExactly("dev");
            assertThat(context.getEnvironment().getProperty("spring.mongodb.uri"))
                    .isEqualTo("mongodb://localhost:27017/daily-trends-dev");
        }
    }

    @Test
    void uses_the_test_profile_defaults() {
        try (var context = start(Map.of(), "--spring.profiles.active=test")) {
            assertThat(context.getEnvironment().getProperty("spring.mongodb.uri"))
                    .isEqualTo("mongodb://localhost:27017/daily-trends-test");
        }
    }

    @Test
    void lets_environment_variables_override_the_profile_defaults() {
        var variables = Map.<String, Object>of(
                "SPRING_MONGODB_URI", "mongodb://other:27017/daily-trends",
                "DAILY_TRENDS_TIMEZONE", "UTC");

        try (var context = start(variables)) {
            assertThat(context.getEnvironment().getProperty("spring.mongodb.uri"))
                    .isEqualTo("mongodb://other:27017/daily-trends");
            assertThat(context.getBean(DailyTrendsProperties.class).timezone()).isEqualTo(ZoneId.of("UTC"));
        }
    }

    private static ConfigurableApplicationContext start(Map<String, Object> variables, String... args) {
        var environment = new StandardEnvironment();
        environment
                .getPropertySources()
                .replace(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        new SystemEnvironmentPropertySource(
                                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, variables));
        var application = new SpringApplication(PropertiesOnly.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setEnvironment(environment);
        return application.run(Stream.concat(Stream.of("--spring.config.location=classpath:/"), Stream.of(args))
                .toArray(String[]::new));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DailyTrendsProperties.class)
    static class PropertiesOnly {}
}
