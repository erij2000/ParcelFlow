package com.parcelflow.order.exception;

import lombok.Getter;

/**
 * Le carrier-service a repondu par un SOAP Fault (refus metier du transporteur).
 * Convertie en HTTP 422 par {@link GlobalExceptionHandler}.
 */
@Getter
public class CarrierFaultException extends RuntimeException {

    /** Code metier lu dans le faultstring (ex : WEIGHT_LIMIT_EXCEEDED). */
    private final String faultCode;

    public CarrierFaultException(String faultCode, String message) {
        super(message);
        this.faultCode = faultCode;
    }
}
