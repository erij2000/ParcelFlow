package com.parcelflow.tracking.repository;

import com.parcelflow.tracking.model.TrackingPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour les positions de tracking.
 *
 * @author Erij
 */
@Repository
public interface TrackingPositionRepository extends JpaRepository<TrackingPosition, String> {

    /**
     * Récupère la dernière position d'un colis.
     * ORDER BY recordedAt DESC LIMIT 1 est implicite.
     */
    Optional<TrackingPosition> findTopByParcelIdOrderByRecordedAtDesc(String parcelId);

    /**
     * Récupère toutes les positions d'un colis, triées par date.
     */
    List<TrackingPosition> findByParcelIdOrderByRecordedAtAsc(String parcelId);

    /** Vérifie si un colis a des positions enregistrées */
    boolean existsByParcelId(String parcelId);
}
