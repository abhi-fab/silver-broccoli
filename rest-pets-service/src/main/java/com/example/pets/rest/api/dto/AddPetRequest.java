package com.example.pets.rest.api.dto;

import com.example.pets.rest.domain.PetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to add a pet via the upstream SOAP AddPet operation")
public record AddPetRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(example = "Rex", description = "Pet name")
        String name,

        @NotNull
        @Schema(example = "dog", description = "Pet type: dog or cat")
        PetType type
) {
}
