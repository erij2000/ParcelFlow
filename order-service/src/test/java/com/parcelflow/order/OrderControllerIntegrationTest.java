package com.parcelflow.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parcelflow.order.dto.CreateOrderRequest;
import com.parcelflow.order.model.Order;
import com.parcelflow.order.model.OrderStatus;
import com.parcelflow.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration du OrderController.
 *
 * <p>Utilise @SpringBootTest (contexte complet) + H2 en mémoire (profil "test").
 * En Sprint 3, on migrera vers Testcontainers pour tester avec PostgreSQL réel.
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

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /orders → 201 Created avec référence de suivi")
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
                .andExpect(jsonPath("$.senderName", is("Ahmed Ben Ali")));
    }

    @Test
    @DisplayName("POST /orders avec données invalides → 400 Bad Request")
    void shouldRejectInvalidOrder() throws Exception {
        // senderName manquant → erreur de validation
        String invalidJson = """
                {"recipientName": "Test", "weightKg": 1.0}
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /orders/{id} inexistant → 404 Not Found")
    void shouldReturn404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/v1/orders/non-existent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /orders → retourne la liste complète")
    void shouldReturnAllOrders() throws Exception {
        // Créer 2 commandes via le service
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest("Sender1", "Recipient1"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest("Sender2", "Recipient2"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
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
