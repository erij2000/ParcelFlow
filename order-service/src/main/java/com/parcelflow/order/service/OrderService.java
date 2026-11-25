package com.parcelflow.order.service;

import com.parcelflow.order.dto.CreateOrderRequest;
import com.parcelflow.order.dto.OrderResponse;
import com.parcelflow.order.exception.OrderNotFoundException;
import com.parcelflow.order.model.Order;
import com.parcelflow.order.model.OrderStatus;
import com.parcelflow.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service métier du Order Service.
 *
 * <p>Ce service contient toute la logique applicative.
 * Le contrôleur REST ne fait que déléguer ici.
 *
 * <p>En Sprint 2, ce service deviendra l'orchestrateur :
 * il appellera le Carrier Service via SOAP pour assigner un transporteur,
 * puis notifiera le Tracking Service via gRPC.
 *
 * @author Yessine
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    // ── CRUD Operations ──────────────────────────────────────────────────

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Création d'une nouvelle commande pour : {}", request.getRecipientName());

        String trackingRef = generateTrackingReference();

        Order order = Order.builder()
                .trackingReference(trackingRef)
                .status(OrderStatus.PENDING)
                .senderName(request.getSenderName())
                .senderAddress(request.getSenderAddress())
                .senderCity(request.getSenderCity())
                .recipientName(request.getRecipientName())
                .recipientAddress(request.getRecipientAddress())
                .recipientCity(request.getRecipientCity())
                .weightKg(request.getWeightKg())
                .declaredValue(request.getDeclaredValue())
                .build();

        Order saved = orderRepository.save(order);
        log.info("Commande créée avec succès : {}", saved.getId());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String id) {
        log.debug("Recherche de la commande : {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Commande non trouvée : " + id));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByTrackingReference(String ref) {
        Order order = orderRepository.findByTrackingReference(ref)
                .orElseThrow(() -> new OrderNotFoundException("Référence de suivi introuvable : " + ref));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(String id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Commande non trouvée : " + id));

        log.info("Mise à jour du statut de {} : {} → {}", id, order.getStatus(), newStatus);
        order.setStatus(newStatus);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public void deleteOrder(String id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException("Commande non trouvée : " + id);
        }
        orderRepository.deleteById(id);
        log.info("Commande supprimée : {}", id);
    }

    // ── Méthodes privées ─────────────────────────────────────────────────

    /**
     * Génère une référence de suivi au format : PF-YYYY-NNNNN
     * Le numéro vient d'une séquence PostgreSQL (atomique, survit aux redémarrages).
     * Exemple : PF-2026-00042
     */
    private String generateTrackingReference() {
        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        long seq = orderRepository.nextReferenceNumber();
        return String.format("PF-%s-%05d", year, seq);
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .trackingReference(order.getTrackingReference())
                .status(order.getStatus())
                .senderName(order.getSenderName())
                .senderCity(order.getSenderCity())
                .recipientName(order.getRecipientName())
                .recipientCity(order.getRecipientCity())
                .weightKg(order.getWeightKg())
                .carrierId(order.getCarrierId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}