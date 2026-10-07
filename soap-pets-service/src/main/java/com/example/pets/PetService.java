package com.example.pets;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

@WebService(name = "PetService", targetNamespace = "http://example.com/pets")
@SOAPBinding(style = SOAPBinding.Style.RPC, use = SOAPBinding.Use.LITERAL)
public interface PetService {

    @WebMethod(operationName = "ListDogs")
    String[] listDogs();

    @WebMethod(operationName = "ListCats")
    String[] listCats();
}
