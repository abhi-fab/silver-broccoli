package com.example.pets.rest.api.dto;

import com.example.pets.rest.domain.PetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Collection of pet names for a given type")
public record PetNamesResponse(
        @Schema(example = "DOG") PetType type,
        @Schema(example = "[\"Buddy\",\"Max\"]") List<String> names,
        @Schema(example = "2") int count
) {
    public static PetNamesResponse of(PetType type, List<String> names) {
        return new PetNamesResponse(type, List.copyOf(names), names.size());
    }
}
