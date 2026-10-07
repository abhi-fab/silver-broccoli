package com.example.pets.rest.service;

import com.example.pets.rest.api.dto.AddPetRequest;
import com.example.pets.rest.api.dto.PetNamesResponse;
import com.example.pets.rest.api.dto.PetResponse;
import com.example.pets.rest.client.SoapPetClient;
import com.example.pets.rest.domain.PetType;
import org.springframework.stereotype.Service;

@Service
public class PetService {

    private final SoapPetClient soapPetClient;

    public PetService(SoapPetClient soapPetClient) {
        this.soapPetClient = soapPetClient;
    }

    public PetNamesResponse listDogs() {
        return PetNamesResponse.of(PetType.DOG, soapPetClient.listDogs());
    }

    public PetNamesResponse listCats() {
        return PetNamesResponse.of(PetType.CAT, soapPetClient.listCats());
    }

    public PetNamesResponse listByType(PetType type) {
        return switch (type) {
            case DOG -> listDogs();
            case CAT -> listCats();
        };
    }

    public PetResponse addPet(AddPetRequest request) {
        String name = soapPetClient.addPet(request.name().trim(), request.type());
        return new PetResponse(name, request.type());
    }
}
