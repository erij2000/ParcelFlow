package com.parcelflow.carrier.ws;

import com.parcelflow.carrier.generated.BookCarrierRequest;
import com.parcelflow.carrier.generated.BookCarrierResponse;
import com.parcelflow.carrier.generated.CarrierInfo;
import com.parcelflow.carrier.generated.CarrierType;
import com.parcelflow.carrier.generated.GetPriceRequest;
import com.parcelflow.carrier.generated.GetPriceResponse;
import com.parcelflow.carrier.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * Endpoint SOAP du Carrier Service (contract-first).
 *
 * <p>Les classes GetPriceRequest, GetPriceResponse, BookCarrierRequest, etc. sont
 * GENEREES depuis carrier.xsd par le plugin jaxb-maven-plugin (package
 * com.parcelflow.carrier.generated) : le contrat XSD est la source de verite,
 * le code Java en derive.
 *
 * <p>Erreurs metier : on leve {@link CarrierSoapFaultException}, que Spring-WS
 * transforme en SOAP Fault.
 */
@Endpoint
@RequiredArgsConstructor
@Slf4j
public class CarrierEndpoint {

    private static final String NAMESPACE_URI = "http://parcelflow.com/carrier";

    /** Meme limite que la facette maxInclusive du XSD (weightKg). */
    static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("1000");

    private final PricingService pricingService;

    // ── Operation 1 : GetPrice ──────────────────────────────────────────
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetPriceRequest")
    @ResponsePayload
    public GetPriceResponse getPrice(@RequestPayload GetPriceRequest request) {
        log.info("SOAP GetPrice : {} -> {} | {}kg | {}",
                request.getOriginCity(), request.getDestinationCity(),
                request.getWeightKg(), request.getCarrierType());

        validateShipment(request.getOriginCity(), request.getDestinationCity(), request.getWeightKg());
        String carrierType = carrierTypeOf(request.getCarrierType());

        BigDecimal price = pricingService.calculatePrice(
                request.getOriginCity(), request.getDestinationCity(),
                request.getWeightKg(), carrierType);

        CarrierInfo info = new CarrierInfo();
        info.setCarrierId("CARRIER-001");
        info.setCarrierName("ParcelFlow Logistics");
        info.setCarrierType(request.getCarrierType());
        info.setAvailable(true);

        GetPriceResponse response = new GetPriceResponse();
        response.setPrice(price);
        response.setCurrency("EUR");
        response.setEstimatedDays(pricingService.getEstimatedDays(carrierType));
        response.setCarrierInfo(info);
        return response;
    }

    // ── Operation 2 : BookCarrier ───────────────────────────────────────
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "BookCarrierRequest")
    @ResponsePayload
    public BookCarrierResponse bookCarrier(@RequestPayload BookCarrierRequest request) {
        log.info("SOAP BookCarrier : commande={} | {} -> {}",
                request.getOrderId(), request.getOriginCity(), request.getDestinationCity());

        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new CarrierSoapFaultException("MISSING_ORDER_ID",
                    "L'identifiant de commande est obligatoire");
        }
        validateShipment(request.getOriginCity(), request.getDestinationCity(), request.getWeightKg());
        String carrierType = carrierTypeOf(request.getCarrierType());

        BigDecimal price = pricingService.calculatePrice(
                request.getOriginCity(), request.getDestinationCity(),
                request.getWeightKg(), carrierType);

        BookCarrierResponse response = new BookCarrierResponse();
        response.setBookingId(UUID.randomUUID().toString());
        response.setCarrierId("CARRIER-001");
        response.setCarrierName("ParcelFlow Logistics");
        response.setTotalPrice(price);
        response.setEstimatedPickup(tomorrowMorning());
        response.setStatus("CONFIRMED");
        return response;
    }

    // ── Regles metier : chaque violation devient un SOAP Fault ───────────
    private void validateShipment(String originCity, String destinationCity, BigDecimal weightKg) {
        if (originCity == null || originCity.isBlank()) {
            throw new CarrierSoapFaultException("MISSING_ORIGIN", "La ville d'origine est obligatoire");
        }
        if (destinationCity == null || destinationCity.isBlank()) {
            throw new CarrierSoapFaultException("MISSING_DESTINATION", "La ville de destination est obligatoire");
        }
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CarrierSoapFaultException("INVALID_WEIGHT", "Le poids doit etre strictement positif");
        }
        if (weightKg.compareTo(MAX_WEIGHT_KG) > 0) {
            throw new CarrierSoapFaultException("WEIGHT_LIMIT_EXCEEDED",
                    "Poids maximum accepte : " + MAX_WEIGHT_KG + " kg");
        }
    }

    private String carrierTypeOf(CarrierType type) {
        if (type == null) {
            throw new CarrierSoapFaultException("MISSING_CARRIER_TYPE",
                    "Le type de transport est obligatoire (STANDARD, EXPRESS ou OVERNIGHT)");
        }
        return type.value();
    }

    /** Date de ramassage estimee : demain 09h00 (champ xs:dateTime du contrat). */
    private XMLGregorianCalendar tomorrowMorning() {
        try {
            GregorianCalendar calendar = GregorianCalendar.from(
                    ZonedDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0));
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Impossible de creer la date de ramassage", e);
        }
    }
}
