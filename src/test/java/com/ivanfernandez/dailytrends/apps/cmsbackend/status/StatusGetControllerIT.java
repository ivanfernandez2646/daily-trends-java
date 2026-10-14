package com.ivanfernandez.dailytrends.apps.cmsbackend.status;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivanfernandez.dailytrends.apps.cmsbackend.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
class StatusGetControllerIT {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void answers_ok_with_an_empty_body() {
        var response = mvc.get().uri("/status").exchange();

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).body().isEmpty();
    }
}
