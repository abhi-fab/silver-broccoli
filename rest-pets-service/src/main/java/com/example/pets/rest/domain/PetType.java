package com.example.pets.rest.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum PetType {
    DOG,
    CAT;

    @JsonCreator
    public static PetType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "dog", "dogs" -> DOG;
            case "cat", "cats" -> CAT;
            default -> throw new IllegalArgumentException("type must be dog or cat");
        };
    }
}
