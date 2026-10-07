package com.example.pets.rest.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Problem details for a failed request")
public record ErrorResponse(
        @Schema(example = "2026-10-07T06:00:00Z") Instant timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        @Schema(example = "type must be dog or cat") String message,
        @Schema(example = "/api/v1/pets") String path
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path);
    }
}
