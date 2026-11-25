package com.parcelflow.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parcelflow.order.client.carrier.CarrierSoapClient;
import com.parcelflow.order.client.tracking.TrackingGrpcClient;
import com.parcelflow.order.dto.CreateOrderRequest;
import com.parcelflow.order.dto.TrackingPositionResponse;
import com.parcelflow.order.exception.CarrierFaultException;
import com.parcelflow.order.exception.OrderNotFoundException;
import com.parcelflow.order.exception.RemoteServiceUnavailableException;
import com.parcelflow.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration du OrderController.
 *
 * <p>Contexte Spring complet + H2 en memoire (profil "test").
 * Les deux clients distants (SOAP carrier, gRPC tracking) sont REMPLACES par des mocks :
 * on teste ici l'API REST, la logique d'orchestration et la traduction des erreurs
 * (SOAP Fault -> 422, service indisponible -> 503, gRPC NOT_FOUND -> 404).
 * Les vrais appels reseau se verifient avec docker compose (voir README).
 *
 * @author Yessine
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Order Controller – Tests d'intégration")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private CarrierSoapClient carrierSoapClient;

    @MockBean
    private TrackingGrpcClient trackingGrpcClient;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        // Par defaut, le transporteur repond avec un tarif
        when(carrierSoapClient.getPrice(any(), any(), any(), any()))
                .thenReturn(new CarrierSoapClient.PriceQuote(new BigDecimal("12.50"), "EUR", 3, "ParcelFlow Logistics"));
    }

    // ── Creation ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /orders → 201 Created avec référence de suivi et tarif SOAP")
    void shouldCreateOrder() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .senderName("Ahmed Ben Ali")
                .senderAddress("12 Rue de la Paix")
                .senderCity("Tunis")
                .recipientName("Sara Mansour")
                .recipientAddress("5 Av. Habib Bourguiba")
                .recipientCity("Sfax")
                .weightKg(new BigDecimal("2.500"))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingReference", startsWith("PF-")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.senderName", is("Ahmed Ben Ali")))
                .andExpect(jsonPath("$.shippingPrice", is(12.5)))
                .andExpect(jsonPath("$.estimatedDays", is(3)));
    }

    @Test
    @DisplayName("POST /orders avec données invalides → 400 Bad Request")
    void shouldRejectInvalidOrder() throws Exception {
        String invalidJson = """
                {"recipientName": "Test", "weightKg": 1.0}
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /orders → SOAP Fault du transporteur = 422, rien n'est enregistré")
    void shouldReturn422OnSoapFault() throws Exception {
        when(carrierSoapClient.getPrice(any(), any(), any(), any()))
                .thenThrow(new CarrierFaultException("WEIGHT_LIMIT_EXCEEDED", "Poids maximum accepte : 1000 kg"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Sender", "Recipient"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.faultCode", is("WEIGHT_LIMIT_EXCEEDED")));

        assertEquals(0, orderRepository.count());
    }

    @Test
    @DisplayName("POST /orders → carrier-service injoignable = 503, rien n'est enregistré")
    void shouldReturn503WhenCarrierDown() throws Exception {
        when(carrierSoapClient.getPrice(any(), any(), any(), any()))
                .thenThrow(new RemoteServiceUnavailableException("carrier-service", new RuntimeException("connexion refusee")));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Sender", "Recipient"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.service", is("carrier-service")));

        assertEquals(0, orderRepository.count());
    }

    // ── Lecture ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /orders/{id} inexistant → 404 Not Found")
    void shouldReturn404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/v1/orders/non-existent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /orders → retourne la liste complète")
    void shouldReturnAllOrders() throws Exception {
        createOrderAndGetId("Sender1", "Recipient1");
        createOrderAndGetId("Sender2", "Recipient2");

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("PATCH /orders/{id}/status?status=INCONNU → 400 (et non 500)")
    void shouldReturn400ForUnknownStatus() throws Exception {
        String id = createOrderAndGetId("Sender", "Recipient");

        mockMvc.perform(patch("/api/v1/orders/" + id + "/status").param("status", "NOPE"))
                .andExpect(status().isBadRequest());
    }

    // ── Confirmation : SOAP BookCarrier + gRPC RecordPosition ───────────

    @Test
    @DisplayName("POST /orders/{id}/confirm → SOAP BookCarrier puis gRPC RecordPosition, statut ASSIGNED_TO_CARRIER")
    void shouldConfirmOrder() throws Exception {
        stubBooking();
        String id = createOrderAndGetId("Sender", "Recipient");

        mockMvc.perform(post("/api/v1/orders/" + id + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ASSIGNED_TO_CARRIER")))
                .andExpect(jsonPath("$.carrierId", is("CARRIER-001")))
                .andExpect(jsonPath("$.carrierBookingId", is("BK-1")));

        verify(carrierSoapClient).bookCarrier(any(), any(), any(), any(), any());
        verify(trackingGrpcClient).recordPosition(anyString(), anyDouble(), anyDouble(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /orders/{id}/confirm deux fois → 409 Conflict")
    void shouldReturn409WhenConfirmingTwice() throws Exception {
        stubBooking();
        String id = createOrderAndGetId("Sender", "Recipient");

        mockMvc.perform(post("/api/v1/orders/" + id + "/confirm")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders/" + id + "/confirm")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /orders/{id}/confirm → SOAP Fault du transporteur = 422")
    void shouldReturn422WhenBookingRefused() throws Exception {
        String id = createOrderAndGetId("Sender", "Recipient");
        when(carrierSoapClient.bookCarrier(any(), any(), any(), any(), any()))
                .thenThrow(new CarrierFaultException("MISSING_ORDER_ID", "L'identifiant de commande est obligatoire"));

        mockMvc.perform(post("/api/v1/orders/" + id + "/confirm"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.faultCode", is("MISSING_ORDER_ID")));
    }

    // ── Suivi : appels gRPC ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /orders/{id}/tracking → derniere position (gRPC GetLastPosition)")
    void shouldReturnLastPosition() throws Exception {
        String id = createOrderAndGetId("Sender", "Recipient");
        when(trackingGrpcClient.getLastPosition(anyString()))
                .thenReturn(new TrackingPositionResponse("PF-X", 36.8065, 10.1815, "Tunis", "IN_TRANSIT", 1L, "En route"));

        mockMvc.perform(get("/api/v1/orders/" + id + "/tracking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city", is("Tunis")))
                .andExpect(jsonPath("$.status", is("IN_TRANSIT")));
    }

    @Test
    @DisplayName("GET /orders/{id}/tracking → gRPC NOT_FOUND = 404")
    void shouldReturn404WhenNoPosition() throws Exception {
        String id = createOrderAndGetId("Sender", "Recipient");
        when(trackingGrpcClient.getLastPosition(anyString()))
                .thenThrow(new OrderNotFoundException("Aucune position de suivi pour : PF-X"));

        mockMvc.perform(get("/api/v1/orders/" + id + "/tracking"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /orders/{id}/tracking → tracking-service arrêté = 503")
    void shouldReturn503WhenTrackingDown() throws Exception {
        String id = createOrderAndGetId("Sender", "Recipient");
        when(trackingGrpcClient.getLastPosition(anyString()))
                .thenThrow(new RemoteServiceUnavailableException("tracking-service", new RuntimeException("deadline")));

        mockMvc.perform(get("/api/v1/orders/" + id + "/tracking"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.service", is("tracking-service")));
    }

    @Test
    @DisplayName("POST /orders/{id}/tracking/positions → gRPC RecordPosition, statut IN_TRANSIT")
    void shouldMoveOrderInTransit() throws Exception {
        stubBooking();
        String id = createOrderAndGetId("Sender", "Recipient");
        mockMvc.perform(post("/api/v1/orders/" + id + "/confirm")).andExpect(status().isOk());

        String position = """
                {"latitude": 36.8065, "longitude": 10.1815, "city": "Tunis",
                 "status": "IN_TRANSIT", "description": "Colis charge"}
                """;
        mockMvc.perform(post("/api/v1/orders/" + id + "/tracking/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(position))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_TRANSIT")));
    }

    @Test
    @DisplayName("POST /orders/{id}/tracking/positions avant confirmation → 409")
    void shouldRefusePositionBeforeConfirmation() throws Exception {
        String id = createOrderAndGetId("Sender", "Recipient");

        String position = """
                {"latitude": 36.8, "longitude": 10.1, "city": "Tunis", "status": "IN_TRANSIT"}
                """;
        mockMvc.perform(post("/api/v1/orders/" + id + "/tracking/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(position))
                .andExpect(status().isConflict());
    }

    // ── Outils ──────────────────────────────────────────────────────────

    private void stubBooking() {
        when(carrierSoapClient.bookCarrier(any(), any(), any(), any(), any()))
                .thenReturn(new CarrierSoapClient.Booking("BK-1", "CARRIER-001", "ParcelFlow Logistics", new BigDecimal("14.00")));
    }

    private String createOrderAndGetId(String sender, String recipient) throws Exception {
        String body = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(sender, recipient))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private CreateOrderRequest validRequest(String sender, String recipient) {
        return CreateOrderRequest.builder()
                .senderName(sender)
                .senderAddress("Adresse Expéditeur")
                .senderCity("Tunis")
                .recipientName(recipient)
                .recipientAddress("Adresse Destinataire")
                .recipientCity("Sousse")
                .weightKg(new BigDecimal("1.000"))
                .build();
    }
}
