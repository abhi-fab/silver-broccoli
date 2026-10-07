package com.example.pets.rest.client;

import com.example.pets.rest.config.SoapPetsProperties;
import com.example.pets.rest.domain.PetType;
import com.example.pets.rest.error.SoapClientException;
import com.example.pets.rest.error.SoapFaultException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thin SOAP client for the RPC/encoded pets service.
 * Uses hand-built envelopes because modern JAX-WS rewrites encoded bindings to literal.
 */
@Component
public class SoapPetClient {

    private static final String NAMESPACE = "http://example.com/pets";
    private static final String SOAP_ENCODING = "http://schemas.xmlsoap.org/soap/encoding/";
    private static final Pattern ITEM =
            Pattern.compile("<item\\b[^>]*>([^<]*)</item>", Pattern.CASE_INSENSITIVE);
    private static final Pattern RETURN_SCALAR =
            Pattern.compile("<return\\b[^>]*>([^<]*)</return>", Pattern.CASE_INSENSITIVE);
    private static final Pattern FAULT_STRING =
            Pattern.compile("<faultstring\\b[^>]*>([^<]*)</faultstring>", Pattern.CASE_INSENSITIVE);
    private static final Pattern FAULT_CODE =
            Pattern.compile("<faultcode\\b[^>]*>([^<]*)</faultcode>", Pattern.CASE_INSENSITIVE);

    private final RestTemplate restTemplate;
    private final SoapPetsProperties properties;

    public SoapPetClient(RestTemplate soapRestTemplate, SoapPetsProperties properties) {
        this.restTemplate = soapRestTemplate;
        this.properties = properties;
    }

    public List<String> listDogs() {
        return listPets("ListDogs");
    }

    public List<String> listCats() {
        return listPets("ListCats");
    }

    public String addPet(String name, PetType type) {
        String envelope = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:pets="%s"
                                  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <pets:AddPet soapenv:encodingStyle="%s">
                      <name xsi:type="xsd:string">%s</name>
                      <type xsi:type="xsd:string">%s</type>
                    </pets:AddPet>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                NAMESPACE,
                SOAP_ENCODING,
                xmlEscape(name),
                xmlEscape(type.name().toLowerCase(Locale.ROOT))
        );

        String responseBody = invoke(envelope);
        Matcher matcher = RETURN_SCALAR.matcher(responseBody);
        if (!matcher.find()) {
            throw new SoapClientException("SOAP AddPet response did not contain a return value");
        }
        return xmlUnescape(matcher.group(1).trim());
    }

    private List<String> listPets(String operation) {
        String envelope = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:pets="%s">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <pets:%s/>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(NAMESPACE, operation);

        String responseBody = invoke(envelope);
        List<String> names = new ArrayList<>();
        Matcher matcher = ITEM.matcher(responseBody);
        while (matcher.find()) {
            names.add(xmlUnescape(matcher.group(1).trim()));
        }
        return List.copyOf(names);
    }

    private String invoke(String envelope) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.set(HttpHeaders.ACCEPT, MediaType.TEXT_XML_VALUE);
        headers.set("SOAPAction", "\"\"");

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    properties.baseUrl(),
                    new HttpEntity<>(envelope, headers),
                    String.class
            );
            String body = response.getBody() == null ? "" : response.getBody();
            assertNoFault(body);
            return body;
        } catch (SoapFaultException | SoapClientException e) {
            throw e;
        } catch (RestClientException e) {
            throw new SoapClientException("Failed to call SOAP pets service: " + e.getMessage(), e);
        }
    }

    private static void assertNoFault(String body) {
        Matcher faultString = FAULT_STRING.matcher(body);
        if (!faultString.find()) {
            return;
        }
        String message = xmlUnescape(faultString.group(1).trim());
        Matcher faultCode = FAULT_CODE.matcher(body);
        String code = faultCode.find() ? xmlUnescape(faultCode.group(1).trim()) : "Server";
        throw new SoapFaultException(code, message);
    }

    private static String xmlEscape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String xmlUnescape(String value) {
        return value
                .replace("&apos;", "'")
                .replace("&quot;", "\"")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replace("&amp;", "&");
    }
}
