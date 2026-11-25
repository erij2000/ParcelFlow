package com.parcelflow.order.exception;

/**
 * Operation impossible dans l'etat actuel de la commande.
 * Convertie en HTTP 409 par {@link GlobalExceptionHandler}.
 */
public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
