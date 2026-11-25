package com.parcelflow.order.service;

import com.parcelflow.order.client.carrier.CarrierSoapClient;
import com.parcelflow.order.client.tracking.TrackingGrpcClient;
import com.parcelflow.order.dto.CreateOrderRequest;
import com.parcelflow.order.dto.OrderResponse;
import com.parcelflow.order.dto.PositionUpdateRequest;
import com.parcelflow.order.dto.TrackingPositionResponse;
import com.parcelflow.order.exception.InvalidOrderStateException;
import com.parcelflow.order.exception.OrderNotFoundException;
import com.parcelflow.order.exception.RemoteServiceUnavailableException;
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
 * Service metier du Order Service : il ORCHESTRE les deux autres microservices.
 *
 * <ul>
 *   <li>carrier-service (SOAP) : tarif a la creation (GetPrice), reservation a la confirmation (BookCarrier)</li>
 *   <li>tracking-service (gRPC) : demarrage et lecture du suivi (RecordPosition, GetLastPosition, GetHistory)</li>
 * </ul>
 *
 * <p>Les methodes qui font des appels reseau ne sont PAS @Transactional :
 * on ne garde pas une connexion a la base ouverte pendant qu'on attend un autre service.
 *
 * @author Yessine
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    /** Type de transport demande au transporteur (valeurs du XSD : STANDARD, EXPRESS, OVERNIGHT). */
    private static final String DEFAULT_CARRIER_TYPE = "STANDARD";

    private final OrderRepository orderRepository;
    private final CarrierSoapClient carrierClient;
    private final TrackingGrpcClient trackingClient;

    // ── Creation : appel SOAP GetPrice ──────────────────────────────────

    /**
     * Cree une commande. Le tarif est demande AVANT la sauvegarde : si le transporteur
     * refuse (SOAP Fault) ou est injoignable, rien n'est enregistre.
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creation d'une nouvelle commande pour : {}", request.getRecipientName());

        CarrierSoapClient.PriceQuote quote = carrierClient.getPrice(
                request.getSenderCity(), request.getRecipientCity(),
                request.getWeightKg(), DEFAULT_CARRIER_TYPE);

        Order order = Order.builder()
                .trackingReference(generateTrackingReference())
                .status(OrderStatus.PENDING)
                .senderName(request.getSenderName())
                .senderAddress(request.getSenderAddress())
                .senderCity(request.getSenderCity())
                .recipientName(request.getRecipientName())
                .recipientAddress(request.getRecipientAddress())
                .recipientCity(request.getRecipientCity())
                .weightKg(request.getWeightKg())
                .declaredValue(request.getDeclaredValue())
                .shippingPrice(quote.price())
                .estimatedDays(quote.estimatedDays())
                .build();

        Order saved = orderRepository.save(order);
        log.info("Commande creee : {} (tarif {} {})", saved.getId(), quote.price(), quote.currency());
        return toResponse(saved);
    }

    // ── Confirmation : appel SOAP BookCarrier puis appel gRPC RecordPosition ─

    /**
     * Confirme une commande PENDING : reserve le transporteur (SOAP) puis demarre
     * le suivi (gRPC). La commande passe a ASSIGNED_TO_CARRIER.
     *
     * @throws InvalidOrderStateException si la commande n'est pas PENDING
     */
    public OrderResponse confirmOrder(String id) {
        Order order = findOrder(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Seule une commande PENDING peut etre confirmee (statut actuel : " + order.getStatus() + ")");
        }

        // 1) SOAP : reservation chez le transporteur (un SOAP Fault arrete tout ici)
        CarrierSoapClient.Booking booking = carrierClient.bookCarrier(
                order.getId(), order.getSenderCity(), order.getRecipientCity(),
                order.getWeightKg(), DEFAULT_CARRIER_TYPE);

        order.setCarrierId(booking.carrierId());
        order.setCarrierBookingId(booking.bookingId());
        order.setShippingPrice(booking.totalPrice());
        order.setStatus(OrderStatus.ASSIGNED_TO_CARRIER);
        Order saved = orderRepository.save(order);

        // 2) gRPC : premiere position du colis. Le suivi n'est pas vital pour confirmer :
        //    s'il est indisponible, la commande reste confirmee et on journalise l'incident.
        try {
            double[] start = CityCoordinates.of(order.getSenderCity());
            trackingClient.recordPosition(order.getTrackingReference(), start[0], start[1],
                    order.getSenderCity(), OrderStatus.ASSIGNED_TO_CARRIER.name(),
                    "Commande confirmee, transporteur assigne");
        } catch (RemoteServiceUnavailableException ex) {
            log.warn("Suivi non initialise pour {} : {}", order.getTrackingReference(), ex.getMessage());
        }

        log.info("Commande {} confirmee (reservation {})", saved.getId(), booking.bookingId());
        return toResponse(saved);
    }

    // ── Suivi : appels gRPC ─────────────────────────────────────────────

    /** Derniere position du colis (gRPC GetLastPosition). */
    public TrackingPositionResponse getLastPosition(String id) {
        return trackingClient.getLastPosition(findOrder(id).getTrackingReference());
    }

    /** Historique des positions du colis (gRPC GetHistory). limit = 0 : tout l'historique. */
    public List<TrackingPositionResponse> getPositionHistory(String id, int limit) {
        return trackingClient.getHistory(findOrder(id).getTrackingReference(), limit);
    }

    /**
     * Enregistre une nouvelle position (gRPC RecordPosition) et fait evoluer le statut
     * de la commande : IN_TRANSIT ou DELIVERED selon la position recue.
     */
    public OrderResponse recordTrackingPosition(String id, PositionUpdateRequest request) {
        Order order = findOrder(id);
        if (order.getStatus() != OrderStatus.ASSIGNED_TO_CARRIER && order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new InvalidOrderStateException(
                    "Le suivi n'accepte des positions que pour une commande ASSIGNED_TO_CARRIER ou IN_TRANSIT (statut actuel : "
                            + order.getStatus() + ")");
        }

        trackingClient.recordPosition(order.getTrackingReference(), request.latitude(), request.longitude(),
                request.city(), request.status(), request.description());

        OrderStatus next = OrderStatus.valueOf(request.status());
        if (order.getStatus() != next) {
            log.info("Statut de {} : {} -> {}", order.getId(), order.getStatus(), next);
            order.setStatus(next);
            order = orderRepository.save(order);
        }
        return toResponse(order);
    }

    // ── CRUD ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String id) {
        log.debug("Recherche de la commande : {}", id);
        return toResponse(findOrder(id));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByTrackingReference(String ref) {
        Order order = orderRepository.findByTrackingReference(ref)
                .orElseThrow(() -> new OrderNotFoundException("Reference de suivi introuvable : " + ref));
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
        Order order = findOrder(id);
        log.info("Mise a jour du statut de {} : {} -> {}", id, order.getStatus(), newStatus);
        order.setStatus(newStatus);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public void deleteOrder(String id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException("Commande non trouvee : " + id);
        }
        orderRepository.deleteById(id);
        log.info("Commande supprimee : {}", id);
    }

    // ── Methodes privees ────────────────────────────────────────────────

    private Order findOrder(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Commande non trouvee : " + id));
    }

    /**
     * Reference de suivi : PF-YYYY-NNNNN, numero issu de la sequence PostgreSQL order_ref_seq
     * (atomique, survit aux redemarrages).
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
                .shippingPrice(order.getShippingPrice())
                .estimatedDays(order.getEstimatedDays())
                .carrierId(order.getCarrierId())
                .carrierBookingId(order.getCarrierBookingId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}