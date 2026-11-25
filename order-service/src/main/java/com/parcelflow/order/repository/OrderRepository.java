package com.parcelflow.order.repository;

import com.parcelflow.order.model.Order;
import com.parcelflow.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour les commandes.
 *
 * <p>Spring Data génère automatiquement l'implémentation des méthodes
 * en analysant leur nom (Query Derivation). Pas besoin d'écrire du SQL !
 *
 * @author Yessine
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    /** Recherche par référence de suivi (unique) */
    Optional<Order> findByTrackingReference(String trackingReference);

    /** Liste toutes les commandes avec un statut donné */
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    /** Vérifie si une référence existe déjà (pour l'unicité) */
    boolean existsByTrackingReference(String trackingReference);

    /** Prochain numéro de la séquence PostgreSQL (atomique, survit aux redémarrages) */
    @Query(value = "select nextval('order_ref_seq')", nativeQuery = true)
    long nextReferenceNumber();
}