package com.example.pets.rest.api.dto;

import com.example.pets.rest.domain.PetType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single pet resource")
public record PetResponse(
        @Schema(example = "Rex") String name,
        @Schema(example = "DOG") PetType type
) {
}
