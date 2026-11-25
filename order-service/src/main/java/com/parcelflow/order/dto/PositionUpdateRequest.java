package com.parcelflow.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Nouvelle position d'un colis, relayee vers le tracking-service (gRPC RecordPosition). */
@Schema(description = "Mise a jour de la position d'un colis")
public record PositionUpdateRequest(

        @Schema(example = "36.8065")
        @NotNull @DecimalMin("-90") @DecimalMax("90")
        Double latitude,

        @Schema(example = "10.1815")
        @NotNull @DecimalMin("-180") @DecimalMax("180")
        Double longitude,

        @Schema(example = "Tunis")
        @NotBlank
        String city,

        @Schema(description = "IN_TRANSIT ou DELIVERED", example = "IN_TRANSIT")
        @NotBlank
        @Pattern(regexp = "IN_TRANSIT|DELIVERED", message = "status doit valoir IN_TRANSIT ou DELIVERED")
        String status,

        @Schema(example = "Colis charge au depot de Tunis")
        String description) {
}
