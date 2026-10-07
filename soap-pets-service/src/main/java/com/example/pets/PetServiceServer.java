package com.example.pets;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import jakarta.xml.ws.Endpoint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public final class PetServiceServer {

    static final String USERNAME = "admin";
    static final String PASSWORD = "secret";
    private static final int PORT = 8080;
    private static final String PATH = "/pets";

    private PetServiceServer() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);
        httpServer.setExecutor(Executors.newCachedThreadPool());

        HttpContext context = httpServer.createContext(PATH);
        context.setAuthenticator(new BasicAuthenticator("PetsSOAP") {
            @Override
            public boolean checkCredentials(String username, String password) {
                return USERNAME.equals(username) && PASSWORD.equals(password);
            }
        });

        Endpoint endpoint = Endpoint.create(new PetServiceImpl());
        endpoint.publish(context);
        httpServer.start();

        String baseUrl = "http://localhost:" + PORT + PATH;
        System.out.println("RPC SOAP Pet Service started");
        System.out.println("  Endpoint : " + baseUrl);
        System.out.println("  WSDL     : " + baseUrl + "?wsdl");
        System.out.println("  Auth     : basic (" + USERNAME + " / " + PASSWORD + ")");
        System.out.println("  Style    : RPC/literal");
        System.out.println("  Ops      : ListDogs, ListCats");
    }
}
