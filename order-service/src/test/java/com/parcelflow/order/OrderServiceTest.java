package com.parcelflow.order;

import com.parcelflow.order.client.carrier.CarrierSoapClient;
import com.parcelflow.order.client.tracking.TrackingGrpcClient;
import com.parcelflow.order.dto.CreateOrderRequest;
import com.parcelflow.order.dto.OrderResponse;
import com.parcelflow.order.dto.PositionUpdateRequest;
import com.parcelflow.order.exception.CarrierFaultException;
import com.parcelflow.order.exception.InvalidOrderStateException;
import com.parcelflow.order.exception.RemoteServiceUnavailableException;
import com.parcelflow.order.model.Order;
import com.parcelflow.order.model.OrderStatus;
import com.parcelflow.order.repository.OrderRepository;
import com.parcelflow.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests unitaires de l'orchestration : les clients SOAP et gRPC sont des mocks. */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CarrierSoapClient carrierClient;

    @Mock
    private TrackingGrpcClient trackingClient;

    @InjectMocks
    private OrderService service;

    private CreateOrderRequest request() {
        return CreateOrderRequest.builder()
                .senderName("Ahmed").senderAddress("12 rue A").senderCity("Sousse")
                .recipientName("Marie").recipientAddress("5 avenue B").recipientCity("Marseille")
                .weightKg(new BigDecimal("2.5"))
                .build();
    }

    private Order order(OrderStatus status) {
        return Order.builder()
                .id("order-1").trackingReference("PF-2026-TEST0001").status(status)
                .senderName("Ahmed").senderAddress("12 rue A").senderCity("Sousse")
                .recipientName("Marie").recipientAddress("5 avenue B").recipientCity("Marseille")
                .weightKg(new BigDecimal("2.5"))
                .build();
    }

    // ── Creation ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createOrder : le tarif SOAP est enregistre avec la commande")
    void shouldStoreSoapPrice() {
        when(carrierClient.getPrice("Sousse", "Marseille", new BigDecimal("2.5"), "STANDARD"))
                .thenReturn(new CarrierSoapClient.PriceQuote(new BigDecimal("12.50"), "EUR", 3, "ParcelFlow Logistics"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = service.createOrder(request());

        assertEquals(new BigDecimal("12.50"), response.getShippingPrice());
        assertEquals(3, response.getEstimatedDays());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertTrue(response.getTrackingReference().startsWith("PF-"));
    }

    @Test
    @DisplayName("createOrder : SOAP Fault du transporteur -> exception propagee, rien n'est sauvegarde")
    void shouldNotSaveWhenCarrierRefuses() {
        when(carrierClient.getPrice(anyString(), anyString(), any(), anyString()))
                .thenThrow(new CarrierFaultException("WEIGHT_LIMIT_EXCEEDED", "Poids maximum accepte : 1000 kg"));

        CarrierFaultException fault = assertThrows(CarrierFaultException.class, () -> service.createOrder(request()));

        assertEquals("WEIGHT_LIMIT_EXCEEDED", fault.getFaultCode());
        verify(orderRepository, never()).save(any());
    }

    // ── Confirmation ────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmOrder : SOAP BookCarrier puis gRPC RecordPosition, statut ASSIGNED_TO_CARRIER")
    void shouldConfirmOrder() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order(OrderStatus.PENDING)));
        when(carrierClient.bookCarrier(anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(new CarrierSoapClient.Booking("BK-1", "CARRIER-001", "ParcelFlow Logistics", new BigDecimal("14.00")));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = service.confirmOrder("order-1");

        assertEquals(OrderStatus.ASSIGNED_TO_CARRIER, response.getStatus());
        assertEquals("BK-1", response.getCarrierBookingId());
        assertEquals(new BigDecimal("14.00"), response.getShippingPrice());
        verify(trackingClient).recordPosition(anyString(), anyDouble(), anyDouble(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("confirmOrder : commande deja confirmee -> InvalidOrderStateException, aucun appel distant")
    void shouldRefuseConfirmingTwice() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order(OrderStatus.ASSIGNED_TO_CARRIER)));

        assertThrows(InvalidOrderStateException.class, () -> service.confirmOrder("order-1"));

        verify(carrierClient, never()).bookCarrier(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("confirmOrder : suivi indisponible -> la commande reste confirmee")
    void shouldStayConfirmedWhenTrackingDown() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order(OrderStatus.PENDING)));
        when(carrierClient.bookCarrier(anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(new CarrierSoapClient.Booking("BK-1", "CARRIER-001", "ParcelFlow Logistics", new BigDecimal("14.00")));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trackingClient.recordPosition(anyString(), anyDouble(), anyDouble(), anyString(), anyString(), anyString()))
                .thenThrow(new RemoteServiceUnavailableException("tracking-service", new RuntimeException("deadline")));

        OrderResponse response = service.confirmOrder("order-1");

        assertEquals(OrderStatus.ASSIGNED_TO_CARRIER, response.getStatus());
    }

    // ── Suivi ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("recordTrackingPosition : position IN_TRANSIT -> la commande passe a IN_TRANSIT")
    void shouldMoveToInTransit() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order(OrderStatus.ASSIGNED_TO_CARRIER)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = service.recordTrackingPosition("order-1",
                new PositionUpdateRequest(36.8, 10.18, "Tunis", "IN_TRANSIT", "Colis charge"));

        assertEquals(OrderStatus.IN_TRANSIT, response.getStatus());
        verify(trackingClient).recordPosition("PF-2026-TEST0001", 36.8, 10.18, "Tunis", "IN_TRANSIT", "Colis charge");
    }

    @Test
    @DisplayName("recordTrackingPosition : commande PENDING -> InvalidOrderStateException")
    void shouldRefusePositionForPendingOrder() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order(OrderStatus.PENDING)));

        assertThrows(InvalidOrderStateException.class, () -> service.recordTrackingPosition("order-1",
                new PositionUpdateRequest(36.8, 10.18, "Tunis", "IN_TRANSIT", null)));

        verify(trackingClient, never()).recordPosition(any(), anyDouble(), anyDouble(), any(), any(), any());
    }
}
