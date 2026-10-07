package com.example.pets;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local RPC/encoded SOAP sample with HTTP Basic Auth.
 * Uses a hand-written WSDL because modern JAX-WS runtimes rewrite ENCODED to literal.
 */
public final class PetServiceServer {

    static final String USERNAME = "admin";
    static final String PASSWORD = "secret";

    private static final int PORT = 8080;
    private static final String PATH = "/pets";
    private static final String NAMESPACE = "http://example.com/pets";
    private static final String SOAP_ENCODING = "http://schemas.xmlsoap.org/soap/encoding/";
    private static final Pattern OPERATION =
            Pattern.compile("<(?:[\\w.]+:)?(ListDogs|ListCats|AddPet)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARAM =
            Pattern.compile(
                    "<(?:[\\w.]+:)?(name|type)\\b[^>]*>([^<]*)</(?:[\\w.]+:)?\\1\\s*>",
                    Pattern.CASE_INSENSITIVE);

    private static final List<String> DOGS = new CopyOnWriteArrayList<>(
            List.of("Buddy", "Max", "Bella", "Charlie", "Lucy"));
    private static final List<String> CATS = new CopyOnWriteArrayList<>(
            List.of("Whiskers", "Luna", "Oliver", "Milo", "Simba"));

    private PetServiceServer() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);
        httpServer.setExecutor(Executors.newCachedThreadPool());

        var context = httpServer.createContext(PATH, PetServiceServer::handle);
        context.setAuthenticator(new BasicAuthenticator("PetsSOAP") {
            @Override
            public boolean checkCredentials(String username, String password) {
                return USERNAME.equals(username) && PASSWORD.equals(password);
            }
        });

        httpServer.start();

        String baseUrl = "http://localhost:" + PORT + PATH;
        System.out.println("RPC SOAP Pet Service started");
        System.out.println("  Endpoint : " + baseUrl);
        System.out.println("  WSDL     : " + baseUrl + "?wsdl");
        System.out.println("  Auth     : basic (" + USERNAME + " / " + PASSWORD + ")");
        System.out.println("  Style    : RPC/encoded");
        System.out.println("  Ops      : ListDogs, ListCats, AddPet");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String query = exchange.getRequestURI().getRawQuery();

            if ("GET".equalsIgnoreCase(method) && query != null && query.toLowerCase().contains("wsdl")) {
                write(exchange, 200, "text/xml; charset=utf-8", wsdl());
                return;
            }

            if (!"POST".equalsIgnoreCase(method)) {
                write(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed");
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = OPERATION.matcher(body);
            if (!matcher.find()) {
                write(exchange, 500, "text/xml; charset=utf-8", soapFault("Client", "Unknown operation"));
                return;
            }

            String operation = matcher.group(1);
            if ("AddPet".equalsIgnoreCase(operation)) {
                handleAddPet(exchange, body);
                return;
            }

            List<String> names = "ListDogs".equalsIgnoreCase(operation) ? DOGS : CATS;
            write(exchange, 200, "text/xml; charset=utf-8", soapListResponse(operation, names));
        } catch (Exception e) {
            write(exchange, 500, "text/xml; charset=utf-8", soapFault("Server", e.getMessage()));
        } finally {
            exchange.close();
        }
    }

    private static void handleAddPet(HttpExchange exchange, String body) throws IOException {
        String name = null;
        String type = null;
        Matcher paramMatcher = PARAM.matcher(body);
        while (paramMatcher.find()) {
            String param = paramMatcher.group(1);
            String value = xmlUnescape(paramMatcher.group(2).trim());
            if ("name".equalsIgnoreCase(param)) {
                name = value;
            } else if ("type".equalsIgnoreCase(param)) {
                type = value;
            }
        }

        if (name == null || name.isBlank()) {
            write(exchange, 500, "text/xml; charset=utf-8",
                    soapFault("Client", "AddPet requires a non-empty name"));
            return;
        }
        if (type == null || type.isBlank()) {
            write(exchange, 500, "text/xml; charset=utf-8",
                    soapFault("Client", "AddPet requires a type of dog or cat"));
            return;
        }

        String normalizedType = type.trim().toLowerCase(Locale.ROOT);
        List<String> target;
        if ("dog".equals(normalizedType) || "dogs".equals(normalizedType)) {
            target = DOGS;
        } else if ("cat".equals(normalizedType) || "cats".equals(normalizedType)) {
            target = CATS;
        } else {
            write(exchange, 500, "text/xml; charset=utf-8",
                    soapFault("Client", "AddPet type must be dog or cat"));
            return;
        }

        target.add(name);
        write(exchange, 200, "text/xml; charset=utf-8",
                soapScalarResponse("AddPet", name));
    }

    private static String soapListResponse(String operation, List<String> names) {
        StringBuilder items = new StringBuilder();
        for (String name : names) {
            items.append("<item xsi:type=\"xsd:string\">")
                    .append(xmlEscape(name))
                    .append("</item>");
        }

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                  xmlns:pets="%s">
                  <soapenv:Body>
                    <pets:%sResponse soapenv:encodingStyle="%s">
                      <return soapenc:arrayType="xsd:string[%d]"
                              xsi:type="soapenc:Array"
                              xmlns:soapenc="%s">
                        %s
                      </return>
                    </pets:%sResponse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                NAMESPACE,
                operation,
                SOAP_ENCODING,
                names.size(),
                SOAP_ENCODING,
                items,
                operation
        );
    }

    private static String soapScalarResponse(String operation, String value) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                  xmlns:pets="%s">
                  <soapenv:Body>
                    <pets:%sResponse soapenv:encodingStyle="%s">
                      <return xsi:type="xsd:string">%s</return>
                    </pets:%sResponse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                NAMESPACE,
                operation,
                SOAP_ENCODING,
                xmlEscape(value),
                operation
        );
    }

    private static String soapFault(String code, String message) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <soapenv:Fault>
                      <faultcode>%s</faultcode>
                      <faultstring>%s</faultstring>
                    </soapenv:Fault>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(xmlEscape(code), xmlEscape(message == null ? "error" : message));
    }

    private static String wsdl() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <wsdl:definitions xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
                                  xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
                                  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                                  xmlns:tns="%s"
                                  targetNamespace="%s">
                  <wsdl:message name="ListDogsRequest"/>
                  <wsdl:message name="ListDogsResponse">
                    <wsdl:part name="return" type="xsd:string"/>
                  </wsdl:message>
                  <wsdl:message name="ListCatsRequest"/>
                  <wsdl:message name="ListCatsResponse">
                    <wsdl:part name="return" type="xsd:string"/>
                  </wsdl:message>
                  <wsdl:message name="AddPetRequest">
                    <wsdl:part name="name" type="xsd:string"/>
                    <wsdl:part name="type" type="xsd:string"/>
                  </wsdl:message>
                  <wsdl:message name="AddPetResponse">
                    <wsdl:part name="return" type="xsd:string"/>
                  </wsdl:message>

                  <wsdl:portType name="PetServicePortType">
                    <wsdl:operation name="ListDogs">
                      <wsdl:input message="tns:ListDogsRequest"/>
                      <wsdl:output message="tns:ListDogsResponse"/>
                    </wsdl:operation>
                    <wsdl:operation name="ListCats">
                      <wsdl:input message="tns:ListCatsRequest"/>
                      <wsdl:output message="tns:ListCatsResponse"/>
                    </wsdl:operation>
                    <wsdl:operation name="AddPet">
                      <wsdl:input message="tns:AddPetRequest"/>
                      <wsdl:output message="tns:AddPetResponse"/>
                    </wsdl:operation>
                  </wsdl:portType>

                  <wsdl:binding name="PetServiceBinding" type="tns:PetServicePortType">
                    <soap:binding transport="http://schemas.xmlsoap.org/soap/http" style="rpc"/>
                    <wsdl:operation name="ListDogs">
                      <soap:operation soapAction="" style="rpc"/>
                      <wsdl:input>
                        <soap:body use="encoded" namespace="%s"
                                   encodingStyle="%s"/>
                      </wsdl:input>
                      <wsdl:output>
                        <soap:body use="encoded" namespace="%s"
                                   encodingStyle="%s"/>
                      </wsdl:output>
                    </wsdl:operation>
                    <wsdl:operation name="ListCats">
                      <soap:operation soapAction="" style="rpc"/>
                      <wsdl:input>
                        <soap:body use="encoded" namespace="%s"
                                   encodingStyle="%s"/>
                      </wsdl:input>
                      <wsdl:output>
                        <soap:body use="encoded" namespace="%s"
                                   encodingStyle="%s"/>
                      </wsdl:output>
                    </wsdl:operation>
                    <wsdl:operation name="AddPet">
                      <soap:operation soapAction="" style="rpc"/>
                      <wsdl:input>
                        <soap:body use="encoded" namespace="%s"
                                   encodingStyle="%s"/>
                      </wsdl:input>
                      <wsdl:output>
                        <soap:body use="encoded" namespace="%s"
                                   encodingStyle="%s"/>
                      </wsdl:output>
                    </wsdl:operation>
                  </wsdl:binding>

                  <wsdl:service name="PetService">
                    <wsdl:port name="PetServicePort" binding="tns:PetServiceBinding">
                      <soap:address location="http://localhost:%d%s"/>
                    </wsdl:port>
                  </wsdl:service>
                </wsdl:definitions>
                """.formatted(
                NAMESPACE, NAMESPACE,
                NAMESPACE, SOAP_ENCODING,
                NAMESPACE, SOAP_ENCODING,
                NAMESPACE, SOAP_ENCODING,
                NAMESPACE, SOAP_ENCODING,
                NAMESPACE, SOAP_ENCODING,
                NAMESPACE, SOAP_ENCODING,
                PORT, PATH
        );
    }

    private static void write(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody();
             InputStream ignored = exchange.getRequestBody()) {
            os.write(bytes);
        }
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
