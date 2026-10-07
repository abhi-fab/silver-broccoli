package com.example.pets.rest;

import com.example.pets.rest.config.SoapPetsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SoapPetsProperties.class)
public class RestPetsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestPetsApplication.class, args);
    }
}
