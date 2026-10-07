package com.example.pets.rest.error;

public class SoapClientException extends RuntimeException {

    public SoapClientException(String message) {
        super(message);
    }

    public SoapClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
