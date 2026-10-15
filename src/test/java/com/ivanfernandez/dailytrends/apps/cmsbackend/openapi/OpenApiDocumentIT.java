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

        assertThat(response)
                .bodyJson()
                .extractingPath("$.paths")
                .asMap()
                .containsOnlyKeys("/status", "/feed/scrap", "/feed/home");
    }

    @Test
    void documents_the_scrap_route_as_an_array_of_feeds() {
        MvcTestResult response = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(response)
                .bodyJson()
                .extractingPath("$.paths['/feed/scrap'].get.responses")
                .asMap()
                .containsOnlyKeys("200", "500");
        String schema = "$.paths['/feed/scrap'].get.responses['200'].content['application/json'].schema";
        assertThat(response).bodyJson().extractingPath(schema + ".type").isEqualTo("array");
        assertThat(response)
                .bodyJson()
                .extractingPath(schema + ".items.$ref")
                .isEqualTo("#/components/schemas/FeedResponse");
    }

    @Test
    void documents_the_home_route_as_the_same_array_of_feeds_as_the_scrap_route() {
        MvcTestResult response = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(response)
                .bodyJson()
                .extractingPath("$.paths['/feed/home'].get.responses")
                .asMap()
                .containsOnlyKeys("200", "500");
        String schema = "$.paths['/feed/home'].get.responses['200'].content['application/json'].schema";
        assertThat(response).bodyJson().extractingPath(schema + ".type").isEqualTo("array");
        assertThat(response)
                .bodyJson()
                .extractingPath(schema + ".items.$ref")
                .isEqualTo("#/components/schemas/FeedResponse");
    }

    @Test
    void describes_the_feed_with_a_nullable_description_and_update_date() {
        MvcTestResult response = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(response)
                .bodyJson()
                .extractingPath("$.components.schemas.FeedResponse.properties")
                .asMap()
                .containsOnlyKeys("id", "title", "description", "author", "source", "created_at", "updated_at");
        assertThat(response)
                .bodyJson()
                .extractingPath("$.components.schemas.FeedResponse.properties.description.type")
                .asArray()
                .containsExactlyInAnyOrder("string", "null");
        assertThat(response)
                .bodyJson()
                .extractingPath("$.components.schemas.FeedResponse.properties.updated_at.type")
                .asArray()
                .containsExactlyInAnyOrder("string", "null");
        assertThat(response)
                .bodyJson()
                .extractingPath("$.components.schemas.FeedResponse.properties.updated_at.format")
                .isEqualTo("date-time");
        assertThat(response)
                .bodyJson()
                .extractingPath("$.components.schemas.FeedResponse.properties.title.type")
                .isEqualTo("string");
    }
}
