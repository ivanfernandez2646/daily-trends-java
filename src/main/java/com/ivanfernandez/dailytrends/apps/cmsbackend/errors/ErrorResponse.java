package com.ivanfernandez.dailytrends.apps.cmsbackend.errors;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorResponse", description = "The body of every error")
public record ErrorResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String error) {}
