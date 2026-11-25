package com.parcelflow.order.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * Coordonnees GPS approximatives de quelques villes.
 *
 * <p>Les commandes ne contiennent que des noms de ville : on s'en sert pour donner
 * une premiere position au colis quand le suivi demarre (appel gRPC RecordPosition).
 * Ville inconnue -> (0.0, 0.0). A remplacer plus tard par un vrai service de geocodage.
 */
final class CityCoordinates {

    private static final Map<String, double[]> CITIES = Map.ofEntries(
            Map.entry("tunis", new double[]{36.8065, 10.1815}),
            Map.entry("sousse", new double[]{35.8256, 10.6084}),
            Map.entry("sfax", new double[]{34.7406, 10.7603}),
            Map.entry("monastir", new double[]{35.7643, 10.8113}),
            Map.entry("nabeul", new double[]{36.4513, 10.7357}),
            Map.entry("bizerte", new double[]{37.2744, 9.8739}),
            Map.entry("marseille", new double[]{43.2965, 5.3698}),
            Map.entry("paris", new double[]{48.8566, 2.3522}),
            Map.entry("lyon", new double[]{45.7640, 4.8357}),
            Map.entry("toulouse", new double[]{43.6047, 1.4442}),
            Map.entry("nice", new double[]{43.7102, 7.2620}),
            Map.entry("berlin", new double[]{52.5200, 13.4050}),
            Map.entry("madrid", new double[]{40.4168, -3.7038}),
            Map.entry("amsterdam", new double[]{52.3676, 4.9041}),
            Map.entry("london", new double[]{51.5072, -0.1276}));

    private CityCoordinates() {
    }

    /** @return {latitude, longitude} de la ville, ou {0.0, 0.0} si elle est inconnue */
    static double[] of(String city) {
        if (city == null) {
            return new double[]{0.0, 0.0};
        }
        String key = Normalizer.normalize(city, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        return CITIES.getOrDefault(key, new double[]{0.0, 0.0});
    }
}
