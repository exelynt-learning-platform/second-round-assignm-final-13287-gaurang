package com.shopflow.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    record ErrorBody(
            LocalDateTime timestamp,
            int status,
            String error,
            Object details
    ) {}

    @ExceptionHandler(ShopExceptions.ResourceNotFoundException.class)
    public ResponseEntity<ErrorBody> handleNotFound(ShopExceptions.ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ShopExceptions.OutOfStockException.class)
    public ResponseEntity<ErrorBody> handleOutOfStock(ShopExceptions.OutOfStockException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ShopExceptions.EmptyCartException.class)
    public ResponseEntity<ErrorBody> handleEmptyCart(ShopExceptions.EmptyCartException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ShopExceptions.EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorBody> handleDuplicateEmail(ShopExceptions.EmailAlreadyRegisteredException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ShopExceptions.AccessDeniedException.class)
    public ResponseEntity<ErrorBody> handleForbidden(ShopExceptions.AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ShopExceptions.PaymentException.class)
    public ResponseEntity<ErrorBody> handlePayment(ShopExceptions.PaymentException ex) {
        return build(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorBody> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Email or password is incorrect");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));

        ErrorBody body = new ErrorBody(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleGeneric(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our end");
    }

    private ResponseEntity<ErrorBody> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorBody(LocalDateTime.now(), status.value(), message, null));
    }
}
