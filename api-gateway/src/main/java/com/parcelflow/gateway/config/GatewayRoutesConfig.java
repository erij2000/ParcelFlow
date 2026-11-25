package com.parcelflow.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration des routes de l'API Gateway.
 *
 * <p>Chaque route associe un préfixe d'URL (/api/orders/**) à un microservice.
 * Le filtre StripPrefix(1) supprime le premier segment de chemin avant de
 * transmettre la requête au service cible.
 *
 * <p>Architecture :
 * <pre>
 *   Client → GET /api/orders/123
 *         → Gateway strip(/api) → order-service:8081/orders/123
 * </pre>
 *
 * @author Wiem
 */
@Configuration
public class GatewayRoutesConfig {

    @Value("${ORDER_SERVICE_URL:http://localhost:8081}")
    private String orderServiceUrl;

    @Value("${CARRIER_SERVICE_URL:http://localhost:8082}")
    private String carrierServiceUrl;

    @Value("${TRACKING_SERVICE_URL:http://localhost:8083}")
    private String trackingServiceUrl;

    /**
     * Définition programmatique des routes (alternative au YAML).
     * Avantage : lisibilité, testabilité, et possibilité d'ajouter
     * des filtres dynamiques (JWT, rate-limit) par route.
     */
    @Bean
    public RouteLocator parcelFlowRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

                // ── Route : Order Service ──────────────────────────────────
                // Toutes les requêtes /api/orders/** sont redirigées vers order-service
                .route("order-service", r -> r
                        .path("/api/v1/orders/**")
                        .filters(f -> f
                                        
                                .addRequestHeader("X-Gateway-Source", "parcelflow-gateway")
                                // Circuit Breaker activé Sprint 2 :
                                // .circuitBreaker(c -> c.setName("orderCB").setFallbackUri("forward:/fallback/orders"))
                        )
                        .uri(orderServiceUrl))

                // ── Route : Carrier Service (SOAP) ────────────────────────
                // Les appels SOAP passent aussi par la gateway sur /api/carrier/**
                .route("carrier-service", r -> r
                        .path("/api/carrier/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .addRequestHeader("X-Gateway-Source", "parcelflow-gateway"))
                        .uri(carrierServiceUrl))

                // ── Route : Tracking Service (REST health + gRPC indirect) ─
                .route("tracking-service", r -> r
                        .path("/api/tracking/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .addRequestHeader("X-Gateway-Source", "parcelflow-gateway"))
                        .uri(trackingServiceUrl))

                .build();
    }
}
