package com.parcelflow.carrier.ws;

import lombok.Getter;
import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * Exception qui génère une SOAP Fault standardisée.
 *
 * <p>@SoapFault(faultCode = FaultCode.SERVER) indique que c'est une erreur
 * côté serveur (SERVER = SOAP Fault "Server", CLIENT = erreur dans la requête).
 *
 * <p>Spring-WS intercepte cette exception et construit automatiquement
 * l'enveloppe SOAP Fault avec le message donné.
 *
 * <p>La réponse SOAP Fault aura le format :
 * <pre>
 * &lt;soap:Fault&gt;
 *   &lt;faultcode&gt;soap:Server&lt;/faultcode&gt;
 *   &lt;faultstring&gt;Message d'erreur&lt;/faultstring&gt;
 * &lt;/soap:Fault&gt;
 * </pre>
 *
 * @author Malek
 */
@SoapFault(faultCode = FaultCode.SERVER)
@Getter
public class CarrierSoapFaultException extends RuntimeException {

    private final String faultCode;

    public CarrierSoapFaultException(String faultCode, String message) {
        super(message);
        this.faultCode = faultCode;
    }
}
