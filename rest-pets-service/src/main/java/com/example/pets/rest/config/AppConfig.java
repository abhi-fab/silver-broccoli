package com.example.pets.rest.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    RestTemplate soapRestTemplate(RestTemplateBuilder builder, SoapPetsProperties properties) {
        return builder
                .basicAuthentication(properties.username(), properties.password())
                .connectTimeout(properties.connectTimeout())
                .readTimeout(properties.readTimeout())
                .build();
    }
}
