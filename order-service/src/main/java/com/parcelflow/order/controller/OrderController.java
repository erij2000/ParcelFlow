package com.parcelflow.order.controller;

import com.parcelflow.order.dto.CreateOrderRequest;
import com.parcelflow.order.dto.OrderResponse;
import com.parcelflow.order.dto.PositionUpdateRequest;
import com.parcelflow.order.dto.TrackingPositionResponse;
import com.parcelflow.order.model.OrderStatus;
import com.parcelflow.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des commandes.
 *
 * <p>Toutes les routes commencent par /api/v1/orders (versionnement de l API).
 * La Gateway route /api/v1/orders/** vers ce service sans modifier le chemin.
 *
 * <p>Annotations Swagger (@Operation, @ApiResponse, @Tag) enrichissent
 * la documentation OpenAPI générée automatiquement.
 *
 * @author Yessine
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "API de gestion des commandes de livraison ParcelFlow")
public class OrderController {

    private final OrderService orderService;

    // ── POST /orders ─────────────────────────────────────────────────────
    @Operation(
            summary = "Créer une nouvelle commande",
            description = "Appelle le carrier-service en SOAP (GetPrice) pour obtenir le tarif, puis crée la commande avec une référence de suivi unique."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Commande créée avec succès",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "422", description = "Refus du transporteur (SOAP Fault), ex : poids supérieur à 1000 kg"),
            @ApiResponse(responseCode = "503", description = "carrier-service indisponible"),
            @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── GET /orders ───────────────────────────────────────────────────────
    @Operation(summary = "Lister toutes les commandes")
    @ApiResponse(responseCode = "200", description = "Liste des commandes retournée")
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // ── GET /orders/{id} ──────────────────────────────────────────────────
    @Operation(summary = "Récupérer une commande par son ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commande trouvée"),
            @ApiResponse(responseCode = "404", description = "Commande non trouvée")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "UUID de la commande", required = true)
            @PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // ── GET /orders/tracking/{ref} ────────────────────────────────────────
    @Operation(summary = "Suivre un colis par référence publique",
            description = "Endpoint public de tracking – ne nécessite pas d'authentification.")
    @ApiResponse(responseCode = "200", description = "Statut du colis retourné")
    @GetMapping("/tracking/{ref}")
    public ResponseEntity<OrderResponse> getByTrackingRef(
            @Parameter(description = "Référence de suivi (ex: PF-2026-00001)", required = true)
            @PathVariable String ref) {
        return ResponseEntity.ok(orderService.getOrderByTrackingReference(ref));
    }

    // ── PATCH /orders/{id}/status ─────────────────────────────────────────
    @Operation(summary = "Mettre à jour le statut d'une commande",
            description = "Utilisé par les autres microservices pour mettre à jour le cycle de vie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut mis à jour"),
            @ApiResponse(responseCode = "404", description = "Commande non trouvée")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable String id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    // ── DELETE /orders/{id} ───────────────────────────────────────────────
    @Operation(summary = "Supprimer une commande")
    @ApiResponse(responseCode = "204", description = "Commande supprimée")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    // ── POST /orders/{id}/confirm ─────────────────────────────────────────
    @Operation(
            summary = "Confirmer une commande",
            description = "Appel SOAP BookCarrier vers carrier-service (réservation), puis appel gRPC RecordPosition vers tracking-service (démarrage du suivi)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commande confirmée, transporteur assigné"),
            @ApiResponse(responseCode = "404", description = "Commande non trouvée"),
            @ApiResponse(responseCode = "409", description = "La commande n'est pas en statut PENDING"),
            @ApiResponse(responseCode = "422", description = "Refus du transporteur (SOAP Fault)"),
            @ApiResponse(responseCode = "503", description = "carrier-service indisponible")
    })
    @PostMapping("/{id}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.confirmOrder(id));
    }

    // ── GET /orders/{id}/tracking ─────────────────────────────────────────
    @Operation(
            summary = "Dernière position du colis",
            description = "Appel gRPC GetLastPosition vers tracking-service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dernière position connue"),
            @ApiResponse(responseCode = "404", description = "Commande ou position introuvable"),
            @ApiResponse(responseCode = "503", description = "tracking-service indisponible")
    })
    @GetMapping("/{id}/tracking")
    public ResponseEntity<TrackingPositionResponse> getLastPosition(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getLastPosition(id));
    }

    // ── GET /orders/{id}/tracking/history ─────────────────────────────────
    @Operation(
            summary = "Historique des positions du colis",
            description = "Appel gRPC GetHistory vers tracking-service. limit=0 retourne tout l'historique."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique des positions"),
            @ApiResponse(responseCode = "404", description = "Commande ou historique introuvable"),
            @ApiResponse(responseCode = "503", description = "tracking-service indisponible")
    })
    @GetMapping("/{id}/tracking/history")
    public ResponseEntity<List<TrackingPositionResponse>> getPositionHistory(
            @PathVariable String id,
            @Parameter(description = "Nombre maximum de positions (0 = toutes)")
            @RequestParam(defaultValue = "0") int limit) {
        return ResponseEntity.ok(orderService.getPositionHistory(id, limit));
    }

    // ── POST /orders/{id}/tracking/positions ──────────────────────────────
    @Operation(
            summary = "Enregistrer une position du colis",
            description = "Appel gRPC RecordPosition vers tracking-service. La commande passe à IN_TRANSIT ou DELIVERED selon le statut de la position."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Position enregistrée, statut de la commande mis à jour"),
            @ApiResponse(responseCode = "400", description = "Position invalide"),
            @ApiResponse(responseCode = "404", description = "Commande non trouvée"),
            @ApiResponse(responseCode = "409", description = "La commande n'est pas prête pour le suivi"),
            @ApiResponse(responseCode = "503", description = "tracking-service indisponible")
    })
    @PostMapping("/{id}/tracking/positions")
    public ResponseEntity<OrderResponse> recordPosition(
            @PathVariable String id,
            @Valid @RequestBody PositionUpdateRequest request) {
        return ResponseEntity.ok(orderService.recordTrackingPosition(id, request));
    }
}
