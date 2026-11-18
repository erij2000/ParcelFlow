package com.parcelflow.order.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI/Swagger pour le Order Service.
 *
 * <p>Après démarrage, accéder à :
 * <ul>
 *   <li>Swagger UI : http://localhost:8081/swagger-ui.html</li>
 *   <li>OpenAPI JSON : http://localhost:8081/v3/api-docs</li>
 * </ul>
 *
 * @author Yessine
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI parcelFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ParcelFlow – Order Service API")
                        .description("""
                                API REST de gestion des commandes de livraison.
                                
                                **Fonctionnalités :**
                                - Création et suivi de commandes
                                - Gestion du cycle de vie (PENDING → DELIVERED)
                                - Tracking public par référence
                                
                                **Architecture :** Ce service est l'orchestrateur principal.
                                Il communique avec le Carrier Service (SOAP) et le Tracking Service (gRPC).
                                """)
                        .version("1.0.0-SNAPSHOT")
                        .contact(new Contact()
                                .name("Équipe ParcelFlow – SOA 2026")
                                .email("parcelflow@university.tn"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentation du projet SOA 2026")
                        .url("https://github.com/parcelflow/parcelflow-soa"));
    }
}
