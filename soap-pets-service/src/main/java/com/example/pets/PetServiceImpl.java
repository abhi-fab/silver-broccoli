package com.example.pets;

import jakarta.jws.WebService;

@WebService(
        endpointInterface = "com.example.pets.PetService",
        serviceName = "PetService",
        portName = "PetServicePort",
        targetNamespace = "http://example.com/pets"
)
public class PetServiceImpl implements PetService {

    private static final String[] DOGS = {
            "Buddy", "Max", "Bella", "Charlie", "Lucy"
    };

    private static final String[] CATS = {
            "Whiskers", "Luna", "Oliver", "Milo", "Simba"
    };

    @Override
    public String[] listDogs() {
        return DOGS.clone();
    }

    @Override
    public String[] listCats() {
        return CATS.clone();
    }
}
