package com.parcelflow.order.client.carrier;

import com.parcelflow.order.client.carrier.generated.BookCarrierRequest;
import com.parcelflow.order.client.carrier.generated.BookCarrierResponse;
import com.parcelflow.order.client.carrier.generated.CarrierType;
import com.parcelflow.order.client.carrier.generated.GetPriceRequest;
import com.parcelflow.order.client.carrier.generated.GetPriceResponse;
import com.parcelflow.order.exception.CarrierFaultException;
import com.parcelflow.order.exception.RemoteServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client SOAP du carrier-service (appels reels via WebServiceTemplate).
 *
 * <p>Gestion des erreurs, de la plus precise a la plus generale :
 * <ul>
 *   <li>SOAP Fault du serveur -> {@link CarrierFaultException} (refus metier, HTTP 422)</li>
 *   <li>serveur injoignable, timeout, reponse invalide -> {@link RemoteServiceUnavailableException} (HTTP 503)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CarrierSoapClient {

    /** Le carrier-service ecrit ses Faults sous la forme "CODE: message". */
    private static final Pattern FAULT_FORMAT = Pattern.compile("^([A-Z][A-Z0-9_]+):\\s*(.*)$", Pattern.DOTALL);

    private final WebServiceTemplate carrierWebServiceTemplate;

    /** Resultat de l'operation SOAP GetPrice. */
    public record PriceQuote(BigDecimal price, String currency, int estimatedDays, String carrierName) {
    }

    /** Resultat de l'operation SOAP BookCarrier. */
    public record Booking(String bookingId, String carrierId, String carrierName, BigDecimal totalPrice) {
    }

    /** Operation SOAP GetPrice : demande un tarif au transporteur. */
    public PriceQuote getPrice(String originCity, String destinationCity, BigDecimal weightKg, String carrierType) {
        GetPriceRequest request = new GetPriceRequest();
        request.setOriginCity(originCity);
        request.setDestinationCity(destinationCity);
        request.setWeightKg(weightKg);
        request.setCarrierType(CarrierType.fromValue(carrierType));

        log.info("SOAP -> carrier-service GetPrice : {} -> {} ({} kg)", originCity, destinationCity, weightKg);
        try {
            Object raw = carrierWebServiceTemplate.marshalSendAndReceive(request);
            if (!(raw instanceof GetPriceResponse response)) {
                throw new RemoteServiceUnavailableException("carrier-service",
                        new IllegalStateException("Reponse SOAP GetPrice vide ou inattendue"));
            }
            String carrierName = response.getCarrierInfo() == null ? null : response.getCarrierInfo().getCarrierName();
            return new PriceQuote(response.getPrice(), response.getCurrency(), response.getEstimatedDays(), carrierName);
        } catch (SoapFaultClientException fault) {
            throw toBusinessFault(fault);
        } catch (WebServiceClientException ex) {
            throw new RemoteServiceUnavailableException("carrier-service", ex);
        }
    }

    /** Operation SOAP BookCarrier : reserve le transporteur pour une commande. */
    public Booking bookCarrier(String orderId, String originCity, String destinationCity,
                               BigDecimal weightKg, String carrierType) {
        BookCarrierRequest request = new BookCarrierRequest();
        request.setOrderId(orderId);
        request.setOriginCity(originCity);
        request.setDestinationCity(destinationCity);
        request.setWeightKg(weightKg);
        request.setCarrierType(CarrierType.fromValue(carrierType));

        log.info("SOAP -> carrier-service BookCarrier : commande {}", orderId);
        try {
            Object raw = carrierWebServiceTemplate.marshalSendAndReceive(request);
            if (!(raw instanceof BookCarrierResponse response)) {
                throw new RemoteServiceUnavailableException("carrier-service",
                        new IllegalStateException("Reponse SOAP BookCarrier vide ou inattendue"));
            }
            return new Booking(response.getBookingId(), response.getCarrierId(),
                    response.getCarrierName(), response.getTotalPrice());
        } catch (SoapFaultClientException fault) {
            throw toBusinessFault(fault);
        } catch (WebServiceClientException ex) {
            throw new RemoteServiceUnavailableException("carrier-service", ex);
        }
    }

    /** Transforme un SOAP Fault recu en exception metier (code + message). */
    private CarrierFaultException toBusinessFault(SoapFaultClientException fault) {
        String text = fault.getFaultStringOrReason() == null
                ? "Erreur SOAP du transporteur"
                : fault.getFaultStringOrReason();
        Matcher matcher = FAULT_FORMAT.matcher(text);
        String code = matcher.matches() ? matcher.group(1) : "CARRIER_FAULT";
        String message = matcher.matches() ? matcher.group(2) : text;
        log.warn("SOAP Fault recu du carrier-service : [{}] {}", code, message);
        return new CarrierFaultException(code, message);
    }
}
