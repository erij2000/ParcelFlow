package com.parcelflow.order.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Gestionnaire d'exceptions global pour l'API REST.
 *
 * <p>Utilise le standard RFC 7807 (ProblemDetail) supporté nativement
 * par Spring Boot 3 pour des réponses d'erreur cohérentes et documentées.
 *
 * <p>Toutes les erreurs retournent le même format JSON :
 * <pre>
 * {
 *   "type": "...",
 *   "title": "...",
 *   "status": 404,
 *   "detail": "...",
 *   "timestamp": "..."
 * }
 * </pre>
 *
 * @author Yessine
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Gère les commandes non trouvées → 404 Not Found
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("Commande non trouvée : {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Commande introuvable");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Gère les erreurs de validation Bean (@Valid) → 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Erreur de validation : {}", errors);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errors);
        problem.setTitle("Erreur de validation");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Catch-all – erreurs inattendues → 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Erreur inattendue", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne s'est produite");
        problem.setTitle("Erreur interne");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
