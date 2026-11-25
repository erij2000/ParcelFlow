package com.parcelflow.carrier.ws;

import com.parcelflow.carrier.generated.BookCarrierRequest;
import com.parcelflow.carrier.generated.CarrierType;
import com.parcelflow.carrier.generated.GetPriceRequest;
import com.parcelflow.carrier.generated.GetPriceResponse;
import com.parcelflow.carrier.service.PricingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests unitaires de la logique SOAP (sans serveur HTTP). */
class CarrierEndpointTest {

    private final CarrierEndpoint endpoint = new CarrierEndpoint(new PricingService());

    private GetPriceRequest priceRequest(String weight) {
        GetPriceRequest request = new GetPriceRequest();
        request.setOriginCity("Sousse");
        request.setDestinationCity("Marseille");
        request.setWeightKg(new BigDecimal(weight));
        request.setCarrierType(CarrierType.STANDARD);
        return request;
    }

    @Test
    @DisplayName("GetPrice : retourne un prix positif en EUR")
    void shouldReturnPrice() {
        GetPriceResponse response = endpoint.getPrice(priceRequest("2.5"));

        assertTrue(response.getPrice().compareTo(BigDecimal.ZERO) > 0);
        assertEquals("EUR", response.getCurrency());
        assertNotNull(response.getCarrierInfo());
    }

    @Test
    @DisplayName("GetPrice : poids superieur a 1000 kg -> SOAP Fault WEIGHT_LIMIT_EXCEEDED")
    void shouldFaultWhenWeightTooHigh() {
        CarrierSoapFaultException fault =
                assertThrows(CarrierSoapFaultException.class, () -> endpoint.getPrice(priceRequest("5000")));

        assertEquals("WEIGHT_LIMIT_EXCEEDED", fault.getFaultCode());
        assertTrue(fault.getMessage().startsWith("WEIGHT_LIMIT_EXCEEDED:"));
    }

    @Test
    @DisplayName("GetPrice : poids nul ou negatif -> SOAP Fault INVALID_WEIGHT")
    void shouldFaultWhenWeightNotPositive() {
        CarrierSoapFaultException fault =
                assertThrows(CarrierSoapFaultException.class, () -> endpoint.getPrice(priceRequest("0")));

        assertEquals("INVALID_WEIGHT", fault.getFaultCode());
    }

    @Test
    @DisplayName("BookCarrier : sans identifiant de commande -> SOAP Fault MISSING_ORDER_ID")
    void shouldFaultWhenOrderIdMissing() {
        BookCarrierRequest request = new BookCarrierRequest();
        request.setOrderId(" ");
        request.setOriginCity("Sousse");
        request.setDestinationCity("Marseille");
        request.setWeightKg(new BigDecimal("2.5"));
        request.setCarrierType(CarrierType.EXPRESS);

        CarrierSoapFaultException fault =
                assertThrows(CarrierSoapFaultException.class, () -> endpoint.bookCarrier(request));

        assertEquals("MISSING_ORDER_ID", fault.getFaultCode());
    }

    @Test
    @DisplayName("BookCarrier : reservation valide -> identifiant de reservation et statut CONFIRMED")
    void shouldBookCarrier() {
        BookCarrierRequest request = new BookCarrierRequest();
        request.setOrderId("order-1");
        request.setOriginCity("Sousse");
        request.setDestinationCity("Marseille");
        request.setWeightKg(new BigDecimal("2.5"));
        request.setCarrierType(CarrierType.STANDARD);

        var response = endpoint.bookCarrier(request);

        assertNotNull(response.getBookingId());
        assertEquals("CONFIRMED", response.getStatus());
        assertNotNull(response.getEstimatedPickup());
    }
}
