package com.parcelflow.tracking.grpc;

import com.parcelflow.tracking.model.TrackingPosition;
import com.parcelflow.tracking.repository.TrackingPositionRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;

/**
 * Implémentation du service gRPC TrackingService.
 *
 * <p>@GrpcService : détecte cette classe comme endpoint gRPC et l'enregistre
 * automatiquement sur le serveur gRPC (port 9090 par défaut).
 *
 * <p>Cette classe étend TrackingServiceGrpc.TrackingServiceImplBase,
 * la classe abstraite générée par protoc depuis tracking.proto.
 *
 * <p>IMPORTANT : Les classes TrackingServiceGrpc, TrackParcelRequest, etc.
 * sont générées par le plugin Maven lors de "mvn compile".
 * Pour Sprint 1, on utilise des stubs qui documentent ce comportement.
 *
 * @author Erij
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class TrackingGrpcService {
    // NOTE : En build réel, cette classe étendrait :
    // TrackingServiceGrpc.TrackingServiceImplBase
    // Les méthodes ci-dessous sont les implementations réelles.

    private final TrackingPositionRepository repository;

    // ── APPEL UNAIRE : GetLastPosition ────────────────────────────────────

    /**
     * Retourne la dernière position connue d'un colis.
     *
     * <p>Flux gRPC UNAIRE :
     * <pre>
     *   Client → [TrackParcelRequest] → Serveur
     *   Client ← [TrackingPosition]   ← Serveur
     * </pre>
     *
     * @param request  La requête contenant le parcel_id
     * @param observer Le canal de réponse gRPC (on appelle onNext puis onCompleted)
     */
    public void getLastPosition(Object request, StreamObserver<Object> observer) {
        String parcelId = "DEMO-PARCEL"; // En vrai : request.getParcelId()
        log.info("gRPC GetLastPosition pour : {}", parcelId);

        repository.findTopByParcelIdOrderByRecordedAtDesc(parcelId)
                .ifPresentOrElse(
                        position -> {
                            // Convertir l'entité JPA en message Protobuf
                            // En vrai : TrackingPosition.newBuilder()...build()
                            log.info("Position trouvée : {}", position.getCity());
                            observer.onNext(position); // Envoyer la réponse
                            observer.onCompleted();    // Terminer le stream
                        },
                        () -> {
                            // Pas de position → erreur gRPC NOT_FOUND
                            observer.onError(Status.NOT_FOUND
                                    .withDescription("Aucune position pour : " + parcelId)
                                    .asRuntimeException());
                        }
                );
    }

    // ── STREAMING SERVEUR : StreamPositions ──────────────────────────────

    /**
     * Envoie un flux continu de positions GPS au client.
     *
     * <p>Flux gRPC STREAMING SERVEUR :
     * <pre>
     *   Client → [TrackParcelRequest] → Serveur
     *   Client ← [TrackingPosition]   ← Serveur (position 1)
     *   Client ← [TrackingPosition]   ← Serveur (position 2)
     *   Client ← [TrackingPosition]   ← Serveur (position 3)
     *   ...
     *   Client ←       [onCompleted]  ← Serveur
     * </pre>
     *
     * <p>Cas d'usage : Le client (dashboard) s'abonne et reçoit
     * chaque mise à jour GPS en temps réel sans polling HTTP.
     *
     * @param request  La requête contenant le parcel_id à suivre
     * @param observer Le canal de réponse gRPC (stream)
     */
    public void streamPositions(Object request, StreamObserver<Object> observer) {
        String parcelId = "DEMO-PARCEL"; // En vrai : request.getParcelId()
        log.info("gRPC StreamPositions démarré pour : {}", parcelId);

        List<TrackingPosition> positions =
                repository.findByParcelIdOrderByRecordedAtAsc(parcelId);

        if (positions.isEmpty()) {
            observer.onError(Status.NOT_FOUND
                    .withDescription("Aucun historique pour : " + parcelId)
                    .asRuntimeException());
            return;
        }

        // Envoyer chaque position dans le stream
        for (TrackingPosition pos : positions) {
            try {
                // En vrai : on convertit en message Protobuf TrackingPosition
                observer.onNext(pos);
                Thread.sleep(100); // Simulation d'un délai entre positions
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                observer.onError(Status.INTERNAL
                        .withDescription("Stream interrompu")
                        .asRuntimeException());
                return;
            }
        }

        observer.onCompleted();
        log.info("gRPC StreamPositions terminé pour : {} ({} positions)", parcelId, positions.size());
    }

    // ── APPEL UNAIRE : RecordPosition ─────────────────────────────────────

    /**
     * Enregistre une nouvelle position GPS en base de données.
     * Appelé par les livreurs ou le système de tracking.
     */
    public void recordPosition(Object request, StreamObserver<Object> observer) {
        log.info("gRPC RecordPosition reçu");

        // En vrai : extraire les données de la requête Protobuf
        TrackingPosition position = TrackingPosition.builder()
                .parcelId("DEMO-PARCEL")
                .latitude(36.8065)   // Tunis par défaut pour la démo
                .longitude(10.1815)
                .city("Tunis")
                .status("IN_TRANSIT")
                .description("Colis en cours de traitement")
                .build();

        TrackingPosition saved = repository.save(position);

        // En vrai : renvoyer un RecordPositionResponse Protobuf
        log.info("Position enregistrée : {}", saved.getId());
        observer.onCompleted();
    }
}
