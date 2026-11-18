package com.parcelflow.order.exception;

/**
 * Exception levée lorsqu'une commande n'est pas trouvée en base de données.
 * Elle est interceptée par GlobalExceptionHandler qui renvoie un 404.
 *
 * @author Yessine
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
