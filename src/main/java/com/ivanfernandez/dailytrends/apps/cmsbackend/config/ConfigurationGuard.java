package com.ivanfernandez.dailytrends.apps.cmsbackend.config;

import java.util.List;
import java.util.Set;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * Checks the rules Spring does not enforce by itself: exactly one known profile, and a MongoDB URI in
 * production (otherwise Spring would silently connect to localhost). Runs after the config files load.
 */
public class ConfigurationGuard implements EnvironmentPostProcessor, Ordered {

    private static final Set<String> PROFILES = Set.of("dev", "test", "production");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String[] active = environment.getActiveProfiles();
        List<String> profiles = List.of(active.length > 0 ? active : environment.getDefaultProfiles());
        if (profiles.size() != 1 || !PROFILES.contains(profiles.getFirst())) {
            throw new InvalidConfiguration(
                    "spring.profiles.active", "exactly one of the profiles dev, test, production must be active");
        }
        if (profiles.getFirst().equals("production")
                && !StringUtils.hasText(environment.getProperty("spring.mongodb.uri"))) {
            throw new InvalidConfiguration("spring.mongodb.uri", "is required by the production profile");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
