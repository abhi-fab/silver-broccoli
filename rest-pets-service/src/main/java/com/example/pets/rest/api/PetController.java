package com.example.pets.rest.api;

import com.example.pets.rest.api.dto.AddPetRequest;
import com.example.pets.rest.api.dto.ErrorResponse;
import com.example.pets.rest.api.dto.PetNamesResponse;
import com.example.pets.rest.api.dto.PetResponse;
import com.example.pets.rest.domain.PetType;
import com.example.pets.rest.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/pets", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Pets", description = "REST operations mapped to the SOAP pets service")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    @Operation(summary = "List pets by type", description = "Maps to SOAP ListDogs or ListCats")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pet names returned"),
            @ApiResponse(responseCode = "400", description = "Invalid type",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Upstream SOAP failure",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PetNamesResponse listPets(@RequestParam("type") PetType type) {
        return petService.listByType(type);
    }

    @GetMapping("/dogs")
    @Operation(summary = "List dogs", description = "Maps to SOAP ListDogs")
    public PetNamesResponse listDogs() {
        return petService.listDogs();
    }

    @GetMapping("/cats")
    @Operation(summary = "List cats", description = "Maps to SOAP ListCats")
    public PetNamesResponse listCats() {
        return petService.listCats();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a pet", description = "Maps to SOAP AddPet")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pet created"),
            @ApiResponse(responseCode = "400", description = "Validation or SOAP client fault",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Upstream SOAP failure",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PetResponse> addPet(@Valid @RequestBody AddPetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.addPet(request));
    }
}
