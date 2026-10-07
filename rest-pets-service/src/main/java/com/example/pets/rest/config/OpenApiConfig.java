package com.example.pets.rest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI petsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("REST Pets API")
                        .description("Modern REST facade over the legacy RPC/encoded SOAP pets service "
                                + "(ListDogs, ListCats, AddPet).")
                        .version("1.0.0"));
    }
}
