package com.parcelflow.carrier.ws;

import com.parcelflow.carrier.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * Endpoint SOAP du Carrier Service.
 *
 * <p>@Endpoint : indique à Spring-WS que cette classe traite les messages SOAP.
 * @PayloadRoot : lie chaque méthode à l'élément XML correspondant dans le WSDL.
 *
 * <p>IMPORTANT : Les classes GetPriceRequest, BookCarrierRequest, etc. sont
 * générées automatiquement par JAXB2 à partir du carrier.xsd lors du build Maven.
 * Elles seront dans le package com.parcelflow.carrier.generated.
 *
 * <p>Pour le Sprint 1 (démonstration), on utilise des classes de stub
 * qui simulent les classes générées. En build réel, remplacer les imports.
 *
 * @author Malek
 */
@Endpoint
@RequiredArgsConstructor
@Slf4j
public class CarrierEndpoint {

    /** Namespace doit correspondre exactement au targetNamespace du XSD */
    private static final String NAMESPACE_URI = "http://parcelflow.com/carrier";

    private final PricingService pricingService;

    // ── OPÉRATION 1 : GetPrice ────────────────────────────────────────────

    /**
     * Calcule et retourne le tarif pour un envoi donné.
     *
     * <p>L'annotation @PayloadRoot indique à Spring-WS que cette méthode
     * doit être invoquée quand l'élément XML racine du payload SOAP est
     * "GetPriceRequest" dans le namespace "http://parcelflow.com/carrier".
     */
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetPriceRequest")
    @ResponsePayload
    public GetPriceResponseStub getPrice(@RequestPayload GetPriceRequestStub request) {
        log.info("SOAP GetPrice : {} → {} | {}kg | {}",
                request.getOriginCity(), request.getDestinationCity(),
                request.getWeightKg(), request.getCarrierType());

        validateGetPriceRequest(request);

        BigDecimal price = pricingService.calculatePrice(
                request.getOriginCity(),
                request.getDestinationCity(),
                request.getWeightKg(),
                request.getCarrierType()
        );
        int days = pricingService.getEstimatedDays(request.getCarrierType());

        GetPriceResponseStub response = new GetPriceResponseStub();
        response.setPrice(price);
        response.setCurrency("EUR");
        response.setEstimatedDays(days);
        response.setCarrierName("ParcelFlow Logistics");

        return response;
    }

    // ── OPÉRATION 2 : BookCarrier ─────────────────────────────────────────

    /**
     * Réserve un transporteur pour une commande.
     *
     * <p>En Sprint 2, cette méthode persistera la réservation en base
     * et enverra un événement Kafka (messagerie asynchrone).
     */
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "BookCarrierRequest")
    @ResponsePayload
    public BookCarrierResponseStub bookCarrier(@RequestPayload BookCarrierRequestStub request) {
        log.info("SOAP BookCarrier : commande={} | {} → {}",
                request.getOrderId(), request.getOriginCity(), request.getDestinationCity());

        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new CarrierSoapFaultException("MISSING_ORDER_ID",
                    "L'identifiant de commande est obligatoire");
        }

        BigDecimal price = pricingService.calculatePrice(
                request.getOriginCity(),
                request.getDestinationCity(),
                request.getWeightKg(),
                request.getCarrierType()
        );

        BookCarrierResponseStub response = new BookCarrierResponseStub();
        response.setBookingId(UUID.randomUUID().toString());
        response.setCarrierId("CARRIER-001");
        response.setCarrierName("ParcelFlow Express");
        response.setTotalPrice(price);
        response.setStatus("CONFIRMED");

        return response;
    }

    // ── Validation ────────────────────────────────────────────────────────

    private void validateGetPriceRequest(GetPriceRequestStub request) {
        if (request.getWeightKg() == null || request.getWeightKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CarrierSoapFaultException("INVALID_WEIGHT",
                    "Le poids doit être strictement positif");
        }
        if (request.getOriginCity() == null || request.getOriginCity().isBlank()) {
            throw new CarrierSoapFaultException("MISSING_ORIGIN",
                    "La ville d'origine est obligatoire");
        }
    }

    // ── Stubs internes (remplacés par les classes JAXB2 au build Maven) ──
    // Ces stubs permettent à l'IDE de compiler même sans le build Maven.

    @lombok.Data
    public static class GetPriceRequestStub {
        private String originCity;
        private String destinationCity;
        private BigDecimal weightKg;
        private String carrierType;
    }

    @lombok.Data
    public static class GetPriceResponseStub {
        private BigDecimal price;
        private String currency;
        private int estimatedDays;
        private String carrierName;
    }

    @lombok.Data
    public static class BookCarrierRequestStub {
        private String orderId;
        private String originCity;
        private String destinationCity;
        private BigDecimal weightKg;
        private String carrierType;
    }

    @lombok.Data
    public static class BookCarrierResponseStub {
        private String bookingId;
        private String carrierId;
        private String carrierName;
        private BigDecimal totalPrice;
        private String status;
    }
}
