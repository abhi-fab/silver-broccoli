package com.example.pets.rest.error;

public class SoapFaultException extends RuntimeException {

    private final String faultCode;

    public SoapFaultException(String faultCode, String faultString) {
        super(faultString);
        this.faultCode = faultCode == null ? "Server" : faultCode;
    }

    public String getFaultCode() {
        return faultCode;
    }

    public boolean isClientFault() {
        return faultCode.toLowerCase().contains("client");
    }
}
