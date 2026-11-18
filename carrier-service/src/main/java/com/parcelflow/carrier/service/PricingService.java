package com.parcelflow.carrier.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service de tarification des transporteurs.
 *
 * <p>Algorithme de calcul de prix :
 * <pre>
 *   prix = tarifBase + (poids × tarifKg) + fraisDistance
 * </pre>
 *
 * <p>En production, ce calcul ferait appel à une API externe de tarification.
 * Ici on simule avec des tarifs fixes pour la démo.
 *
 * @author Malek
 */
@Service
@Slf4j
public class PricingService {

    // Tarifs de base par type de service (en EUR)
    private static final BigDecimal BASE_STANDARD  = new BigDecimal("5.00");
    private static final BigDecimal BASE_EXPRESS   = new BigDecimal("12.00");
    private static final BigDecimal BASE_OVERNIGHT = new BigDecimal("25.00");

    // Tarif par kilogramme
    private static final BigDecimal RATE_PER_KG = new BigDecimal("1.50");

    // Surcharge ville distante (simulation simplifiée)
    private static final BigDecimal DISTANCE_SURCHARGE = new BigDecimal("2.00");

    /**
     * Calcule le tarif de transport.
     *
     * @param originCity      ville de départ
     * @param destinationCity ville d'arrivée
     * @param weightKg        poids en kg
     * @param carrierType     type de service (STANDARD/EXPRESS/OVERNIGHT)
     * @return prix calculé en EUR
     * @throws IllegalArgumentException si le type de transporteur est inconnu
     */
    public BigDecimal calculatePrice(String originCity, String destinationCity,
                                     BigDecimal weightKg, String carrierType) {
        log.debug("Calcul du tarif : {} → {} | {}kg | {}", originCity, destinationCity, weightKg, carrierType);

        BigDecimal basePrice = switch (carrierType.toUpperCase()) {
            case "STANDARD"  -> BASE_STANDARD;
            case "EXPRESS"   -> BASE_EXPRESS;
            case "OVERNIGHT" -> BASE_OVERNIGHT;
            default -> throw new IllegalArgumentException("Type de transporteur inconnu : " + carrierType);
        };

        // Calcul du tarif poids
        BigDecimal weightCost = weightKg.multiply(RATE_PER_KG);

        // Surcharge si villes différentes (simplification)
        BigDecimal distanceCost = originCity.equalsIgnoreCase(destinationCity)
                ? BigDecimal.ZERO
                : DISTANCE_SURCHARGE;

        BigDecimal total = basePrice.add(weightCost).add(distanceCost);
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Retourne le délai de livraison estimé en jours.
     */
    public int getEstimatedDays(String carrierType) {
        return switch (carrierType.toUpperCase()) {
            case "STANDARD"  -> 4;
            case "EXPRESS"   -> 1;
            case "OVERNIGHT" -> 1;
            default -> 7;
        };
    }
}
