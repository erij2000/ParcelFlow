package com.parcelflow.tracking.grpc;

import com.parcelflow.tracking.generated.RecordPositionRequest;
import com.parcelflow.tracking.generated.RecordPositionResponse;
import com.parcelflow.tracking.generated.TrackParcelRequest;
import com.parcelflow.tracking.model.TrackingPosition;
import com.parcelflow.tracking.repository.TrackingPositionRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests unitaires de la logique du service gRPC (sans serveur reseau). */
@ExtendWith(MockitoExtension.class)
class TrackingGrpcServiceTest {

    @Mock
    private TrackingPositionRepository repository;

    @InjectMocks
    private TrackingGrpcService service;

    /** Observer de test : enregistre ce que le service envoie. */
    private static class Capture<T> implements StreamObserver<T> {
        final List<T> values = new ArrayList<>();
        Throwable error;
        boolean completed;

        @Override public void onNext(T value) { values.add(value); }
        @Override public void onError(Throwable t) { error = t; }
        @Override public void onCompleted() { completed = true; }
    }

    private static Status.Code codeOf(Throwable error) {
        assertNotNull(error, "une erreur gRPC etait attendue");
        return Status.fromThrowable(error).getCode();
    }

    @Test
    @DisplayName("GetLastPosition : retourne la derniere position du colis")
    void shouldReturnLastPosition() {
        TrackingPosition entity = TrackingPosition.builder()
                .parcelId("PF-001").latitude(36.8).longitude(10.18)
                .city("Tunis").status("IN_TRANSIT").description("test").build();
        when(repository.findTopByParcelIdOrderByRecordedAtDesc("PF-001")).thenReturn(Optional.of(entity));

        Capture<com.parcelflow.tracking.generated.TrackingPosition> out = new Capture<>();
        service.getLastPosition(TrackParcelRequest.newBuilder().setParcelId("PF-001").build(), out);

        assertTrue(out.completed);
        assertEquals(1, out.values.size());
        assertEquals("Tunis", out.values.get(0).getCity());
        assertEquals("IN_TRANSIT", out.values.get(0).getStatus());
    }

    @Test
    @DisplayName("GetLastPosition : colis inconnu -> NOT_FOUND")
    void shouldReturnNotFoundForUnknownParcel() {
        when(repository.findTopByParcelIdOrderByRecordedAtDesc("UNKNOWN")).thenReturn(Optional.empty());

        Capture<com.parcelflow.tracking.generated.TrackingPosition> out = new Capture<>();
        service.getLastPosition(TrackParcelRequest.newBuilder().setParcelId("UNKNOWN").build(), out);

        assertEquals(Status.Code.NOT_FOUND, codeOf(out.error));
    }

    @Test
    @DisplayName("GetLastPosition : parcel_id vide -> INVALID_ARGUMENT")
    void shouldRejectBlankParcelId() {
        Capture<com.parcelflow.tracking.generated.TrackingPosition> out = new Capture<>();
        service.getLastPosition(TrackParcelRequest.newBuilder().setParcelId("  ").build(), out);

        assertEquals(Status.Code.INVALID_ARGUMENT, codeOf(out.error));
    }

    @Test
    @DisplayName("RecordPosition : latitude hors limites -> INVALID_ARGUMENT, rien n'est sauvegarde")
    void shouldRejectOutOfRangeLatitude() {
        RecordPositionRequest request = RecordPositionRequest.newBuilder()
                .setParcelId("PF-001").setLatitude(120.0).setLongitude(10.0)
                .setCity("Tunis").setStatus("IN_TRANSIT").build();

        Capture<RecordPositionResponse> out = new Capture<>();
        service.recordPosition(request, out);

        assertEquals(Status.Code.INVALID_ARGUMENT, codeOf(out.error));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("RecordPosition : position valide -> sauvegardee, reponse success")
    void shouldRecordValidPosition() {
        when(repository.save(any(TrackingPosition.class))).thenAnswer(invocation -> {
            TrackingPosition p = invocation.getArgument(0);
            p.setId("evt-1");
            return p;
        });
        RecordPositionRequest request = RecordPositionRequest.newBuilder()
                .setParcelId("PF-001").setLatitude(36.8).setLongitude(10.18)
                .setCity("Tunis").setStatus("IN_TRANSIT").setDescription("depart").build();

        Capture<RecordPositionResponse> out = new Capture<>();
        service.recordPosition(request, out);

        assertTrue(out.completed);
        assertTrue(out.values.get(0).getSuccess());
        assertEquals("evt-1", out.values.get(0).getEventId());
    }
}
