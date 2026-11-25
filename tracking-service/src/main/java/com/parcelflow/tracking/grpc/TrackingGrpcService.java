package com.parcelflow.tracking.grpc;

import com.parcelflow.tracking.generated.GetHistoryRequest;
import com.parcelflow.tracking.generated.GetHistoryResponse;
import com.parcelflow.tracking.generated.RecordPositionRequest;
import com.parcelflow.tracking.generated.RecordPositionResponse;
import com.parcelflow.tracking.generated.TrackParcelRequest;
import com.parcelflow.tracking.generated.TrackingServiceGrpc;
import com.parcelflow.tracking.model.TrackingPosition;
import com.parcelflow.tracking.repository.TrackingPositionRepository;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.ZoneId;
import java.util.List;

/**
 * Implementation reelle du service gRPC TrackingService (contrat : tracking.proto).
 *
 * <p>La classe etend {@code TrackingServiceGrpc.TrackingServiceImplBase}, generee par protoc,
 * ce qui la rend enregistrable par {@code @GrpcService} sur le serveur gRPC (port 9090).
 *
 * <p>Attention : l'entite JPA {@link TrackingPosition} et le message protobuf
 * {@code com.parcelflow.tracking.generated.TrackingPosition} ont le meme nom.
 * Le message protobuf est donc toujours ecrit avec son nom complet dans ce fichier.
 *
 * <p>Statuts gRPC utilises : INVALID_ARGUMENT (donnees invalides), NOT_FOUND (colis inconnu).
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class TrackingGrpcService extends TrackingServiceGrpc.TrackingServiceImplBase {

    /** Pause entre deux positions du flux, pour que le streaming soit visible en demo. */
    private static final long STREAM_DELAY_MS = 500;

    private final TrackingPositionRepository repository;

    // ── UNAIRE : GetLastPosition ─────────────────────────────────────────
    @Override
    public void getLastPosition(TrackParcelRequest request,
                                StreamObserver<com.parcelflow.tracking.generated.TrackingPosition> observer) {
        String parcelId = request.getParcelId();
        if (isBlank(parcelId)) {
            fail(observer, Status.INVALID_ARGUMENT, "parcel_id est obligatoire");
            return;
        }
        log.info("gRPC GetLastPosition pour : {}", parcelId);

        repository.findTopByParcelIdOrderByRecordedAtDesc(parcelId).ifPresentOrElse(
                position -> {
                    observer.onNext(toProto(position));
                    observer.onCompleted();
                },
                () -> fail(observer, Status.NOT_FOUND, "Aucune position pour : " + parcelId));
    }

    // ── UNAIRE : GetHistory ──────────────────────────────────────────────
    @Override
    public void getHistory(GetHistoryRequest request, StreamObserver<GetHistoryResponse> observer) {
        String parcelId = request.getParcelId();
        if (isBlank(parcelId)) {
            fail(observer, Status.INVALID_ARGUMENT, "parcel_id est obligatoire");
            return;
        }
        if (request.getLimit() < 0) {
            fail(observer, Status.INVALID_ARGUMENT, "limit ne peut pas etre negatif");
            return;
        }

        List<TrackingPosition> positions = repository.findByParcelIdOrderByRecordedAtAsc(parcelId);
        if (positions.isEmpty()) {
            fail(observer, Status.NOT_FOUND, "Aucun historique pour : " + parcelId);
            return;
        }

        // limit = 0 -> tout l'historique ; sinon les N positions les plus recentes
        int limit = request.getLimit();
        if (limit > 0 && limit < positions.size()) {
            positions = positions.subList(positions.size() - limit, positions.size());
        }

        observer.onNext(GetHistoryResponse.newBuilder()
                .setParcelId(parcelId)
                .addAllPositions(positions.stream().map(TrackingGrpcService::toProto).toList())
                .setTotalCount(positions.size())
                .build());
        observer.onCompleted();
    }

    // ── UNAIRE : RecordPosition ──────────────────────────────────────────
    @Override
    public void recordPosition(RecordPositionRequest request, StreamObserver<RecordPositionResponse> observer) {
        if (isBlank(request.getParcelId()) || isBlank(request.getCity()) || isBlank(request.getStatus())) {
            fail(observer, Status.INVALID_ARGUMENT, "parcel_id, city et status sont obligatoires");
            return;
        }
        if (request.getLatitude() < -90 || request.getLatitude() > 90
                || request.getLongitude() < -180 || request.getLongitude() > 180) {
            fail(observer, Status.INVALID_ARGUMENT,
                    "Coordonnees hors limites (latitude -90..90, longitude -180..180)");
            return;
        }

        TrackingPosition saved = repository.save(TrackingPosition.builder()
                .parcelId(request.getParcelId())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .city(request.getCity())
                .status(request.getStatus())
                .description(request.getDescription())
                .build());
        log.info("Position enregistree : {} pour le colis {}", saved.getId(), saved.getParcelId());

        observer.onNext(RecordPositionResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Position enregistree")
                .setEventId(saved.getId() == null ? "" : saved.getId())
                .build());
        observer.onCompleted();
    }

    // ── STREAMING SERVEUR : StreamPositions ──────────────────────────────
    @Override
    public void streamPositions(TrackParcelRequest request,
                                StreamObserver<com.parcelflow.tracking.generated.TrackingPosition> observer) {
        String parcelId = request.getParcelId();
        if (isBlank(parcelId)) {
            fail(observer, Status.INVALID_ARGUMENT, "parcel_id est obligatoire");
            return;
        }
        log.info("gRPC StreamPositions demarre pour : {}", parcelId);

        List<TrackingPosition> positions = repository.findByParcelIdOrderByRecordedAtAsc(parcelId);
        if (positions.isEmpty()) {
            fail(observer, Status.NOT_FOUND, "Aucun historique pour : " + parcelId);
            return;
        }

        for (TrackingPosition position : positions) {
            // Le client a annule le flux (Ctrl+C, deadline...) : on arrete d'envoyer
            if (observer instanceof ServerCallStreamObserver<?> call && call.isCancelled()) {
                log.info("StreamPositions annule par le client pour : {}", parcelId);
                return;
            }
            observer.onNext(toProto(position));
            try {
                Thread.sleep(STREAM_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(observer, Status.CANCELLED, "Flux interrompu");
                return;
            }
        }
        observer.onCompleted();
        log.info("gRPC StreamPositions termine pour : {} ({} positions)", parcelId, positions.size());
    }

    // ── Utilitaires ──────────────────────────────────────────────────────

    /** Entite JPA -> message protobuf. */
    private static com.parcelflow.tracking.generated.TrackingPosition toProto(TrackingPosition entity) {
        long timestamp = entity.getRecordedAt() == null
                ? 0L
                : entity.getRecordedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return com.parcelflow.tracking.generated.TrackingPosition.newBuilder()
                .setParcelId(entity.getParcelId())
                .setLatitude(entity.getLatitude())
                .setLongitude(entity.getLongitude())
                .setCity(entity.getCity())
                .setStatus(entity.getStatus())
                .setTimestamp(timestamp)
                .setDescription(entity.getDescription() == null ? "" : entity.getDescription())
                .build();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void fail(StreamObserver<?> observer, Status status, String message) {
        observer.onError(status.withDescription(message).asRuntimeException());
    }
}
