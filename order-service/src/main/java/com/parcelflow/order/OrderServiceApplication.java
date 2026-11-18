package com.parcelflow.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée du Order Service.
 *
 * <p>Ce service est l'orchestrateur principal de ParcelFlow.
 * Il expose une API REST documentée avec OpenAPI/Swagger et
 * sera l'initiateur des appels vers Carrier Service (SOAP)
 * et Tracking Service (gRPC) lors du Sprint 2.
 *
 * @author Yessine
 */
@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
