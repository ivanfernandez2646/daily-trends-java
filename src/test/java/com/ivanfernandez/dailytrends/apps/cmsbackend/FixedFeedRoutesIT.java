package com.ivanfernandez.dailytrends.apps.cmsbackend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

/** Guards the fixed feed routes against a later pattern route such as {@code /feed/{id}}. */
@IntegrationTest
class FixedFeedRoutesIT {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @ParameterizedTest
    @ValueSource(strings = {"/feed/scrap", "/feed/home"})
    void is_only_matched_by_literal_patterns(String path) {
        var request = new MockHttpServletRequest("GET", path);
        ServletRequestPathUtils.parseAndCache(request);

        var matchingPatterns = handlerMapping.getHandlerMethods().keySet().stream()
                .map(info -> info.getMatchingCondition(request))
                .filter(Objects::nonNull)
                .map(RequestMappingInfo::getPatternValues)
                .flatMap(patterns -> patterns.stream());

        assertThat(matchingPatterns).allMatch(pattern -> !pattern.contains("{") && !pattern.contains("*"));
    }
}
