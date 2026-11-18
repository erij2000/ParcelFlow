package com.parcelflow.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'API Gateway ParcelFlow.
 *
 * <p>Spring Cloud Gateway est entièrement réactif (Netty + WebFlux).
 * Il ne faut JAMAIS ajouter spring-boot-starter-web ici (conflit bloquant).
 *
 * @author Wiem
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
