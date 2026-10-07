package com.example.pets.rest.api;

import com.example.pets.rest.api.dto.PetNamesResponse;
import com.example.pets.rest.api.dto.PetResponse;
import com.example.pets.rest.config.WebConfig;
import com.example.pets.rest.domain.PetType;
import com.example.pets.rest.error.SoapClientException;
import com.example.pets.rest.error.SoapFaultException;
import com.example.pets.rest.service.PetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PetController.class)
@Import(WebConfig.class)
class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PetService petService;

    @Test
    void listDogsReturnsNames() throws Exception {
        when(petService.listDogs()).thenReturn(PetNamesResponse.of(PetType.DOG, List.of("Buddy", "Max")));

        mockMvc.perform(get("/api/v1/pets/dogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DOG"))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.names[0]").value("Buddy"));
    }

    @Test
    void listByTypeQueryParam() throws Exception {
        when(petService.listByType(PetType.CAT)).thenReturn(PetNamesResponse.of(PetType.CAT, List.of("Luna")));

        mockMvc.perform(get("/api/v1/pets").param("type", "cat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CAT"))
                .andExpect(jsonPath("$.names[0]").value("Luna"));
    }

    @Test
    void addPetReturnsCreated() throws Exception {
        when(petService.addPet(any())).thenReturn(new PetResponse("Rex", PetType.DOG));

        mockMvc.perform(post("/api/v1/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rex","type":"dog"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rex"))
                .andExpect(jsonPath("$.type").value("DOG"));
    }

    @Test
    void addPetRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","type":"dog"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void soapClientFaultMapsToBadRequest() throws Exception {
        when(petService.addPet(any())).thenThrow(new SoapFaultException("Client", "AddPet type must be dog or cat"));

        mockMvc.perform(post("/api/v1/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rex","type":"dog"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("AddPet type must be dog or cat"));
    }

    @Test
    void soapTransportFailureMapsToBadGateway() throws Exception {
        when(petService.listDogs()).thenThrow(new SoapClientException("connection refused"));

        mockMvc.perform(get("/api/v1/pets/dogs"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }
}
