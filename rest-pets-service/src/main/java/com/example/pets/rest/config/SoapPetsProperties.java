package com.example.pets.rest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@ConfigurationProperties(prefix = "soap.pets")
public record SoapPetsProperties(
        String baseUrl,
        String username,
        String password,
        @DurationUnit(ChronoUnit.SECONDS) Duration connectTimeout,
        @DurationUnit(ChronoUnit.SECONDS) Duration readTimeout
) {
}
