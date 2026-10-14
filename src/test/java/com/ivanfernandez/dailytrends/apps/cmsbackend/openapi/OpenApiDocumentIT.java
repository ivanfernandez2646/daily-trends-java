package com.ivanfernandez.dailytrends.apps.cmsbackend.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivanfernandez.dailytrends.apps.cmsbackend.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
class OpenApiDocumentIT {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void publishes_an_openapi_3_1_document_about_the_api() {
        MvcTestResult response = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(response).hasStatus(HttpStatus.OK).hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
        assertThat(response).bodyJson().extractingPath("$.openapi").asString().startsWith("3.1");
        assertThat(response).bodyJson().extractingPath("$.info.title").isEqualTo("daily-trends API");
        assertThat(response)
                .bodyJson()
                .extractingPath("$.info.version")
                .asString()
                .isNotBlank();
    }

    @Test
    void documents_the_status_route_with_its_success_and_error_responses() {
        MvcTestResult response = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(response)
                .bodyJson()
                .extractingPath("$.paths['/status'].get.responses")
                .asMap()
                .containsOnlyKeys("200", "500");
        assertThat(response)
                .bodyJson()
                .extractingPath("$.paths['/status'].get.responses['500'].content['application/json'].schema.$ref")
                .isEqualTo("#/components/schemas/ErrorResponse");
    }

    @Test
    void describes_the_error_body() {
        MvcTestResult response = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(response)
                .bodyJson()
                .extractingPath("$.components.schemas.ErrorResponse.properties.error.type")
                .isEqualTo("string");
        assertThat(response)
                .bodyJson()
                .extractingPath("$.components.schemas.ErrorResponse.required")
                .asArray()
                .containsExactly("error");
    }

    @Test
    void leaves_the_framework_routes_out() {
        MvcTestResult response = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(response).bodyJson().extractingPath("$.paths").asMap().containsOnlyKeys("/status");
    }
}
