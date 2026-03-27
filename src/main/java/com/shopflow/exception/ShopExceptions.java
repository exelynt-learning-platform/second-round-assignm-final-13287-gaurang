package com.shopflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class ShopExceptions {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class OutOfStockException extends RuntimeException {
        public OutOfStockException(String productName) {
            super("'" + productName + "' does not have enough stock for this request");
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class EmptyCartException extends RuntimeException {
        public EmptyCartException() {
            super("Your cart is empty — add products before placing an order");
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class EmailAlreadyRegisteredException extends RuntimeException {
        public EmailAlreadyRegisteredException(String email) {
            super("An account already exists for: " + email);
        }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public static class PaymentException extends RuntimeException {
        public PaymentException(String message) {
            super(message);
        }
    }
}
