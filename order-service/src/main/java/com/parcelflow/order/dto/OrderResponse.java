package com.parcelflow.order.dto;

import com.parcelflow.order.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse renvoyé au client pour une commande.
 *
 * <p>On évite d'exposer l'entité JPA directement pour :
 * <ul>
 *   <li>Éviter les problèmes de sérialisation (relations lazy)</li>
 *   <li>Contrôler exactement ce qui est exposé dans l'API</li>
 *   <li>Permettre le versioning de l'API sans casser le modèle</li>
 * </ul>
 *
 * @author Yessine
 */
@Data
@Builder
@Schema(description = "Réponse contenant les détails d'une commande")
public class OrderResponse {

    @Schema(description = "Identifiant unique UUID de la commande")
    private String id;

    @Schema(description = "Référence de suivi publique", example = "PF-2026-00001")
    private String trackingReference;

    @Schema(description = "Statut actuel de la commande")
    private OrderStatus status;

    @Schema(description = "Nom de l'expéditeur")
    private String senderName;

    @Schema(description = "Ville de départ")
    private String senderCity;

    @Schema(description = "Nom du destinataire")
    private String recipientName;

    @Schema(description = "Ville de destination")
    private String recipientCity;

    @Schema(description = "Poids en kg")
    private BigDecimal weightKg;

    @Schema(description = "Tarif de livraison en euros (obtenu par SOAP GetPrice)")
    private BigDecimal shippingPrice;

    @Schema(description = "Délai de livraison estimé en jours")
    private Integer estimatedDays;

    @Schema(description = "ID du transporteur assigné (null si non encore assigné)")
    private String carrierId;

    @Schema(description = "ID de la réservation chez le transporteur (obtenu par SOAP BookCarrier)")
    private String carrierBookingId;

    @Schema(description = "Date et heure de création")
    private LocalDateTime createdAt;

    @Schema(description = "Date et heure de dernière modification")
    private LocalDateTime updatedAt;
}
