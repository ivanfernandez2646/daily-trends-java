package com.ivanfernandez.dailytrends.apps.cmsbackend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
class CorsAndCompressionIT {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ServerProperties serverProperties;

    @Test
    void allows_any_origin() {
        var response = mvc.options()
                .uri("/status")
                .header(HttpHeaders.ORIGIN, "https://example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange();

        assertThat(response).hasStatusOk().headers().hasValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    }

    // MockMvc bypasses Tomcat, and no S01 response reaches the 2 KB threshold; S03 tests real gzip.
    @Test
    void enables_response_compression() {
        assertThat(serverProperties.getCompression().getEnabled()).isTrue();
    }
}
