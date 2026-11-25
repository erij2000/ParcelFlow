package com.parcelflow.order.client.tracking;

import com.parcelflow.order.dto.TrackingPositionResponse;
import com.parcelflow.order.exception.OrderNotFoundException;
import com.parcelflow.order.exception.RemoteServiceUnavailableException;
import com.parcelflow.tracking.generated.GetHistoryRequest;
import com.parcelflow.tracking.generated.RecordPositionRequest;
import com.parcelflow.tracking.generated.TrackParcelRequest;
import com.parcelflow.tracking.generated.TrackingPosition;
import com.parcelflow.tracking.generated.TrackingServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client gRPC du tracking-service (appels reels, stub genere depuis tracking.proto).
 *
 * <p>L'adresse du serveur est configuree dans application.yml
 * (grpc.client.tracking-service.address). Chaque appel a une DEADLINE :
 * sans reponse apres tracking.grpc.deadline-ms, l'appel echoue en DEADLINE_EXCEEDED.
 *
 * <p>Statuts gRPC traduits : NOT_FOUND -> 404, INVALID_ARGUMENT -> 400,
 * UNAVAILABLE / DEADLINE_EXCEEDED / autres -> 503.
 */
@Component
@Slf4j
public class TrackingGrpcClient {

    @GrpcClient("tracking-service")
    private TrackingServiceGrpc.TrackingServiceBlockingStub trackingStub;

    @Value("${tracking.grpc.deadline-ms:2000}")
    private long deadlineMs;

    /** RPC unaire RecordPosition : enregistre une position, retourne l'identifiant de l'evenement. */
    public String recordPosition(String parcelId, double latitude, double longitude,
                                 String city, String status, String description) {
        RecordPositionRequest request = RecordPositionRequest.newBuilder()
                .setParcelId(parcelId)
                .setLatitude(latitude)
                .setLongitude(longitude)
                .setCity(city)
                .setStatus(status)
                .setDescription(description == null ? "" : description)
                .build();
        log.info("gRPC -> tracking-service RecordPosition : {} ({})", parcelId, status);
        try {
            return stub().recordPosition(request).getEventId();
        } catch (StatusRuntimeException ex) {
            throw translate(ex, parcelId);
        }
    }

    /** RPC unaire GetLastPosition : derniere position connue du colis. */
    public TrackingPositionResponse getLastPosition(String parcelId) {
        log.info("gRPC -> tracking-service GetLastPosition : {}", parcelId);
        try {
            return toDto(stub().getLastPosition(TrackParcelRequest.newBuilder().setParcelId(parcelId).build()));
        } catch (StatusRuntimeException ex) {
            throw translate(ex, parcelId);
        }
    }

    /** RPC unaire GetHistory : historique des positions (limit = 0 pour tout l'historique). */
    public List<TrackingPositionResponse> getHistory(String parcelId, int limit) {
        log.info("gRPC -> tracking-service GetHistory : {} (limit={})", parcelId, limit);
        try {
            return stub()
                    .getHistory(GetHistoryRequest.newBuilder().setParcelId(parcelId).setLimit(limit).build())
                    .getPositionsList().stream()
                    .map(TrackingGrpcClient::toDto)
                    .toList();
        } catch (StatusRuntimeException ex) {
            throw translate(ex, parcelId);
        }
    }

    /** Stub avec deadline : un nouvel objet a chaque appel (le stub est immuable). */
    private TrackingServiceGrpc.TrackingServiceBlockingStub stub() {
        return trackingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS);
    }

    private static TrackingPositionResponse toDto(TrackingPosition p) {
        return new TrackingPositionResponse(p.getParcelId(), p.getLatitude(), p.getLongitude(),
                p.getCity(), p.getStatus(), p.getTimestamp(), p.getDescription());
    }

    private RuntimeException translate(StatusRuntimeException ex, String parcelId) {
        log.warn("gRPC tracking-service : statut {} - {}", ex.getStatus().getCode(), ex.getStatus().getDescription());
        return switch (ex.getStatus().getCode()) {
            case NOT_FOUND -> new OrderNotFoundException("Aucune position de suivi pour : " + parcelId);
            case INVALID_ARGUMENT -> new IllegalArgumentException(
                    ex.getStatus().getDescription() == null ? "Requete de suivi invalide" : ex.getStatus().getDescription());
            default -> new RemoteServiceUnavailableException("tracking-service", ex);
        };
    }
}
