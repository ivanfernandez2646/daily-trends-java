package com.ivanfernandez.dailytrends.apps.cmsbackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.ivanfernandez.dailytrends.apps.cmsbackend.config.InvalidConfiguration;
import com.mongodb.MongoTimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

class ApplicationStartupIT {

    @Test
    void fails_with_an_unknown_profile() {
        InvalidConfiguration error =
                catchThrowableOfType(InvalidConfiguration.class, () -> start("--spring.profiles.active=prod"));

        assertThat(error.property()).isEqualTo("spring.profiles.active");
    }

    @Test
    void fails_with_a_non_integer_port() {
        assertThatThrownBy(() -> start("--spring.profiles.active=test", "--server.port=abc"))
                .hasStackTraceContaining("server.port");
    }

    @Test
    void fails_when_mongodb_is_unreachable() {
        assertThatThrownBy(() -> start(
                        "--spring.profiles.active=test",
                        "--server.port=0",
                        "--spring.mongodb.uri=mongodb://localhost:1/daily-trends-test"))
                .hasRootCauseInstanceOf(MongoTimeoutException.class);
    }

    private static void start(String... args) {
        String[] allArgs = Stream.concat(Stream.of("--spring.config.location=classpath:/"), Stream.of(args))
                .toArray(String[]::new);
        try (var context = new SpringApplication(CmsBackendApplication.class).run(allArgs)) {
            assertThat(context.isActive())
                    .as("the application should not start")
                    .isFalse();
        }
    }
}
