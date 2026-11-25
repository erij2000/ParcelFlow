package com.parcelflow.carrier.ws;

import lombok.Getter;
import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * Exception metier convertie automatiquement en SOAP Fault par Spring-WS.
 *
 * <p>L'annotation {@code @SoapFault} indique a Spring-WS de repondre avec un
 * {@code <soap:Fault>} au lieu d'une erreur HTTP 500 opaque. Le texte du Fault
 * (faultstring) est le message de l'exception.
 *
 * <p>Le code metier est place devant le message, au format {@code CODE: message}
 * (ex : {@code WEIGHT_LIMIT_EXCEEDED: Poids maximum 1000 kg}).
 * Le client (order-service) relit ce prefixe pour distinguer les erreurs.
 */
@SoapFault(faultCode = FaultCode.SERVER)
@Getter
public class CarrierSoapFaultException extends RuntimeException {

    private final String faultCode;

    public CarrierSoapFaultException(String faultCode, String message) {
        super(faultCode + ": " + message);
        this.faultCode = faultCode;
    }
}
