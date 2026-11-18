package com.parcelflow.order.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant une commande de livraison.
 *
 * <p>Annotations Lombok utilisées :
 * <ul>
 *   <li>@Data      : génère getters, setters, equals, hashCode, toString</li>
 *   <li>@Builder   : pattern Builder pour la création d'instances</li>
 *   <li>@NoArgsConstructor / @AllArgsConstructor : constructeurs JPA requis</li>
 * </ul>
 *
 * @author Yessine
 */
@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    /** Référence unique de suivi affichée au client (ex: PF-2026-00001) */
    @Column(name = "tracking_reference", unique = true, nullable = false)
    private String trackingReference;

    /** Statut du cycle de vie de la commande */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // ── Expéditeur ──────────────────────────────────────────────
    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(name = "sender_address", nullable = false)
    private String senderAddress;

    @Column(name = "sender_city", nullable = false)
    private String senderCity;

    // ── Destinataire ─────────────────────────────────────────────
    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_address", nullable = false)
    private String recipientAddress;

    @Column(name = "recipient_city", nullable = false)
    private String recipientCity;

    // ── Colis ────────────────────────────────────────────────────
    /** Poids en kilogrammes */
    @Column(name = "weight_kg", nullable = false, precision = 8, scale = 3)
    private BigDecimal weightKg;

    /** Valeur déclarée en euros */
    @Column(name = "declared_value", precision = 10, scale = 2)
    private BigDecimal declaredValue;

    /** ID du transporteur assigné (rempli lors du Sprint 2 – appel SOAP Carrier) */
    @Column(name = "carrier_id")
    private String carrierId;

    // ── Méta-données ─────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
