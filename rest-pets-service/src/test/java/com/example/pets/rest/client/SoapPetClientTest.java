package com.example.pets.rest.client;

import com.example.pets.rest.config.SoapPetsProperties;
import com.example.pets.rest.domain.PetType;
import com.example.pets.rest.error.SoapFaultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SoapPetClientTest {

    private static final String BASE_URL = "http://localhost:8080/pets";

    private MockRestServiceServer server;
    private SoapPetClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        SoapPetsProperties properties = new SoapPetsProperties(
                BASE_URL,
                "admin",
                "secret",
                Duration.ofSeconds(2),
                Duration.ofSeconds(5)
        );
        client = new SoapPetClient(restTemplate, properties);
    }

    @Test
    void listDogsParsesEncodedArray() {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("SOAPAction", "\"\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ListDogs")))
                .andRespond(withSuccess("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                          <soapenv:Body>
                            <pets:ListDogsResponse xmlns:pets="http://example.com/pets">
                              <return>
                                <item>Buddy</item>
                                <item>Max</item>
                              </return>
                            </pets:ListDogsResponse>
                          </soapenv:Body>
                        </soapenv:Envelope>
                        """, MediaType.TEXT_XML));

        List<String> dogs = client.listDogs();

        assertThat(dogs).containsExactly("Buddy", "Max");
        server.verify();
    }

    @Test
    void addPetSendsNameAndType() {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<name")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Rex")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("dog")))
                .andRespond(withSuccess("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                          <soapenv:Body>
                            <pets:AddPetResponse xmlns:pets="http://example.com/pets">
                              <return>Rex</return>
                            </pets:AddPetResponse>
                          </soapenv:Body>
                        </soapenv:Envelope>
                        """, MediaType.TEXT_XML));

        String name = client.addPet("Rex", PetType.DOG);

        assertThat(name).isEqualTo("Rex");
        server.verify();
    }

    @Test
    void soapFaultIsSurfaced() {
        server.expect(requestTo(BASE_URL))
                .andRespond(withSuccess("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                          <soapenv:Body>
                            <soapenv:Fault>
                              <faultcode>Client</faultcode>
                              <faultstring>AddPet type must be dog or cat</faultstring>
                            </soapenv:Fault>
                          </soapenv:Body>
                        </soapenv:Envelope>
                        """, MediaType.TEXT_XML));

        assertThatThrownBy(() -> client.addPet("Rex", PetType.DOG))
                .isInstanceOf(SoapFaultException.class)
                .hasMessageContaining("dog or cat");
        server.verify();
    }
}
