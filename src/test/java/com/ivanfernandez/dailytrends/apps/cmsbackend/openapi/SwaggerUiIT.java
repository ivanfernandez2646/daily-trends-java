package com.ivanfernandez.dailytrends.apps.cmsbackend.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivanfernandez.dailytrends.apps.cmsbackend.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
class SwaggerUiIT {

    @Autowired
    private MockMvcTester mvc;

    // Static resources are off (spring.web.resources.add-mappings=false); springdoc serves the UI itself.
    @Test
    void serves_the_swagger_ui() {
        assertThat(mvc.get().uri("/swagger-ui/index.html").exchange()).hasStatus(HttpStatus.OK);
    }

    @Test
    void redirects_the_short_swagger_ui_path() {
        assertThat(mvc.get().uri("/swagger-ui.html").exchange()).hasStatus3xxRedirection();
    }
}
