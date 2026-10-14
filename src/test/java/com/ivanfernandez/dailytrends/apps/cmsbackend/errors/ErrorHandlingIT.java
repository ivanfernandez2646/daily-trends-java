package com.ivanfernandez.dailytrends.apps.cmsbackend.errors;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivanfernandez.dailytrends.apps.cmsbackend.IntegrationTest;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.InvalidStoredFeed;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.ConflictError;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.InvalidArgumentError;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.NotFoundError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@IntegrationTest
@Import(ErrorHandlingIT.ErrorProbeController.class)
@ExtendWith(OutputCaptureExtension.class)
class ErrorHandlingIT {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void answers_400_for_an_invalid_argument() {
        MvcTestResult response = mvc.get().uri("/probe/invalid-argument").exchange();

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(response).bodyJson().isStrictlyEqualTo("""
                {"error": "<ProbeId> does not allow the value <x>"}""");
    }

    @Test
    void answers_404_for_a_not_found_error() {
        MvcTestResult response = mvc.get().uri("/probe/not-found").exchange();

        assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(response).bodyJson().isStrictlyEqualTo("""
                {"error": "Probe <1> not found"}""");
    }

    @Test
    void answers_409_for_a_conflict_error() {
        MvcTestResult response = mvc.get().uri("/probe/conflict").exchange();

        assertThat(response).hasStatus(HttpStatus.CONFLICT);
        assertThat(response).bodyJson().isStrictlyEqualTo("""
                {"error": "Probe <1> already exists"}""");
    }

    @Test
    void answers_a_generic_500_for_an_unexpected_error() {
        MvcTestResult response = mvc.get().uri("/probe/unexpected").exchange();

        assertThat(response).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response).bodyJson().isStrictlyEqualTo("""
                {"error": "Internal server error"}""");
        assertThat(response).bodyText().doesNotContain("password");
    }

    @Test
    void answers_a_generic_500_for_an_invalid_stored_feed() {
        MvcTestResult response = mvc.get().uri("/probe/invalid-stored-feed").exchange();

        assertThat(response).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response).bodyJson().isStrictlyEqualTo("""
                {"error": "Internal server error"}""");
    }

    @Test
    void logs_an_unexpected_error_once_with_method_path_and_stack_trace(CapturedOutput output) {
        mvc.get().uri("/probe/unexpected").exchange();

        assertThat(output.getOut())
                .containsOnlyOnce("GET /probe/unexpected")
                .contains("ERROR")
                .contains("java.lang.IllegalStateException: db password=x")
                .contains("at " + ErrorProbeController.class.getName());
    }

    @Test
    void answers_404_json_for_an_unknown_route() {
        MvcTestResult response = mvc.get().uri("/nope").exchange();

        assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(response).bodyJson().isStrictlyEqualTo("""
                {"error": "Not Found"}""");
    }

    @Test
    void answers_405_json_for_a_method_not_allowed() {
        MvcTestResult response = mvc.post().uri("/status").exchange();

        assertThat(response).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response).bodyJson().isStrictlyEqualTo("""
                {"error": "Method Not Allowed"}""");
    }

    @RestController
    @RequestMapping("/probe")
    static class ErrorProbeController {

        @GetMapping("/invalid-argument")
        void invalidArgument() {
            throw new InvalidArgumentError("<ProbeId> does not allow the value <x>");
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new ProbeNotFound();
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ProbeAlreadyExists();
        }

        @GetMapping("/invalid-stored-feed")
        void invalidStoredFeed() {
            throw new InvalidStoredFeed("9b2f3a4e-1c5d-4e6f-8a7b-0c1d2e3f4a5b", "field 'createdAt' is not a date");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("db password=x");
        }
    }

    static final class ProbeNotFound extends NotFoundError {
        ProbeNotFound() {
            super("Probe <1> not found");
        }
    }

    static final class ProbeAlreadyExists extends ConflictError {
        ProbeAlreadyExists() {
            super("Probe <1> already exists");
        }
    }
}
