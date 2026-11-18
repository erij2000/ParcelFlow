package com.parcelflow.tracking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée du Tracking Service.
 *
 * <p>Ce service expose deux interfaces :
 * <ul>
 *   <li>Un serveur gRPC sur le port 9090 (défini dans application.yml)</li>
 *   <li>Un serveur HTTP sur le port 8083 (pour /health et futures APIs REST)</li>
 * </ul>
 *
 * <p>Le serveur gRPC est géré par grpc-spring-boot-starter qui détecte
 * automatiquement les classes annotées @GrpcService.
 *
 * @author Erij
 */
@SpringBootApplication
public class TrackingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrackingServiceApplication.class, args);
    }
}
