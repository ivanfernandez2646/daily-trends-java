package com.ivanfernandez.dailytrends.apps.cmsbackend.status;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusGetController {

    @GetMapping("/status")
    @Operation(summary = "Health check")
    @ApiResponse(responseCode = "200", description = "The API is up; empty body", content = @Content)
    public ResponseEntity<Void> status() {
        return ResponseEntity.ok().build();
    }
}
