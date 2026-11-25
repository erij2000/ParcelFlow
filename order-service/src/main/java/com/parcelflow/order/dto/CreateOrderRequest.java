package com.parcelflow.order.dto;

import com.parcelflow.order.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO de création d'une commande.
 *
 * <p>Les annotations @Schema fournissent les descriptions qui apparaissent
 * dans le Swagger UI – c'est ce que le professeur verra lors de la démo.
 *
 * <p>Les annotations @NotBlank, @Positive etc. sont validées par Spring
 * avant même d'entrer dans le service (via @Valid dans le contrôleur).
 *
 * @author Yessine
 */
@Data
@Builder
@Schema(description = "Requête de création d'une nouvelle commande de livraison")
public class CreateOrderRequest {

    // ── Expéditeur ───────────────────────────────────────────────
    @NotBlank(message = "Le nom de l'expéditeur est obligatoire")
    @Size(max = 100)
    @Schema(description = "Nom complet de l'expéditeur", example = "Ahmed Ben Ali")
    private String senderName;

    @NotBlank(message = "L'adresse de l'expéditeur est obligatoire")
    @Schema(description = "Adresse complète de l'expéditeur", example = "12 Rue de la Paix")
    private String senderAddress;

    @NotBlank
    @Schema(description = "Ville de l'expéditeur", example = "Tunis")
    private String senderCity;

    // ── Destinataire ──────────────────────────────────────────────
    @NotBlank(message = "Le nom du destinataire est obligatoire")
    @Size(max = 100)
    @Schema(description = "Nom complet du destinataire", example = "Sara Mansour")
    private String recipientName;

    @NotBlank
    @Schema(description = "Adresse complète du destinataire", example = "5 Avenue Habib Bourguiba")
    private String recipientAddress;

    @NotBlank
    @Schema(description = "Ville du destinataire", example = "Sfax")
    private String recipientCity;

    // ── Colis ─────────────────────────────────────────────────────
    @NotNull
    @Positive(message = "Le poids doit être positif")
    @Schema(description = "Poids du colis en kilogrammes", example = "2.5")
    private BigDecimal weightKg;

    @PositiveOrZero
    @Schema(description = "Valeur déclarée du colis en EUR (optionnel)", example = "150.00")
    private BigDecimal declaredValue;
}
