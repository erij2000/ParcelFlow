package com.parcelflow.order.exception;

import lombok.Getter;

/**
 * Un microservice appele (carrier-service ou tracking-service) est injoignable,
 * ou n'a pas repondu a temps (timeout SOAP, deadline gRPC).
 * Convertie en HTTP 503 par {@link GlobalExceptionHandler}.
 */
@Getter
public class RemoteServiceUnavailableException extends RuntimeException {

    private final String service;

    public RemoteServiceUnavailableException(String service, Throwable cause) {
        super("Service indisponible : " + service, cause);
        this.service = service;
    }
}
