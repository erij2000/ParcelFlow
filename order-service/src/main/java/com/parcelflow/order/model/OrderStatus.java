package com.parcelflow.order.model;

/**
 * Énumération des statuts possibles d'une commande de livraison.
 *
 * <p>Cycle de vie :
 * <pre>
 *   PENDING → CONFIRMED → ASSIGNED_TO_CARRIER → IN_TRANSIT → DELIVERED
 *                                                           ↘ FAILED
 * </pre>
 *
 * @author Yessine
 */
public enum OrderStatus {
    /** Commande créée, en attente de validation */
    PENDING,
    /** Commande confirmée et validée par le système */
    CONFIRMED,
    /** Un transporteur a été assigné (via Carrier Service SOAP) */
    ASSIGNED_TO_CARRIER,
    /** Colis en cours de transport */
    IN_TRANSIT,
    /** Colis livré avec succès */
    DELIVERED,
    /** Livraison échouée (retour expéditeur) */
    FAILED,
    /** Commande annulée */
    CANCELLED
}
