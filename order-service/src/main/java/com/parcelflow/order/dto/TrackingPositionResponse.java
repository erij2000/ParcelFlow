package com.parcelflow.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Position d'un colis, telle que renvoyee par le tracking-service (gRPC). */
@Schema(description = "Position d'un colis")
public record TrackingPositionResponse(
        String parcelId,
        double latitude,
        double longitude,
        String city,
        String status,
        @Schema(description = "Horodatage en millisecondes depuis 1970 (epoch)")
        long timestamp,
        String description) {
}
