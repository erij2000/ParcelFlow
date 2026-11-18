package com.parcelflow.tracking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité JPA représentant une position GPS d'un colis.
 *
 * <p>Chaque enregistrement est immuable (on ne modifie jamais une position,
 * on en ajoute de nouvelles). C'est un pattern Event Sourcing simplifié.
 *
 * @author Erij
 */
@Entity
@Table(name = "tracking_positions", indexes = {
        @Index(name = "idx_parcel_id", columnList = "parcel_id"),
        @Index(name = "idx_recorded_at", columnList = "recorded_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Identifiant du colis (correspond au trackingReference de Order Service) */
    @Column(name = "parcel_id", nullable = false)
    private String parcelId;

    /** Coordonnée GPS – Latitude (-90 à 90) */
    @Column(nullable = false)
    private Double latitude;

    /** Coordonnée GPS – Longitude (-180 à 180) */
    @Column(nullable = false)
    private Double longitude;

    /** Ville lisible pour l'affichage */
    @Column(nullable = false)
    private String city;

    /** Statut du colis à ce point (IN_TRANSIT, AT_SORTING_CENTER, etc.) */
    @Column(nullable = false)
    private String status;

    /** Description textuelle de l'événement */
    @Column(length = 500)
    private String description;

    /** Horodatage automatique de l'enregistrement */
    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false)
    private LocalDateTime recordedAt;
}
