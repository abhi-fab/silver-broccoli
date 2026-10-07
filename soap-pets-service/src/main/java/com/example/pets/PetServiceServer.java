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
            Pattern.compile("<(?:[\\w.]+:)?(ListDogs|ListCats)\\b", Pattern.CASE_INSENSITIVE);

    private static final List<String> DOGS =
            List.of("Buddy", "Max", "Bella", "Charlie", "Lucy");
    private static final List<String> CATS =
            List.of("Whiskers", "Luna", "Oliver", "Milo", "Simba");

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
        System.out.println("  Ops      : ListDogs, ListCats");
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
            List<String> names = "ListDogs".equalsIgnoreCase(operation) ? DOGS : CATS;
            write(exchange, 200, "text/xml; charset=utf-8", soapResponse(operation, names));
        } catch (Exception e) {
            write(exchange, 500, "text/xml; charset=utf-8", soapFault("Server", e.getMessage()));
        } finally {
            exchange.close();
        }
    }

    private static String soapResponse(String operation, List<String> names) {
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

                  <wsdl:portType name="PetServicePortType">
                    <wsdl:operation name="ListDogs">
                      <wsdl:input message="tns:ListDogsRequest"/>
                      <wsdl:output message="tns:ListDogsResponse"/>
                    </wsdl:operation>
                    <wsdl:operation name="ListCats">
                      <wsdl:input message="tns:ListCatsRequest"/>
                      <wsdl:output message="tns:ListCatsResponse"/>
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
}
