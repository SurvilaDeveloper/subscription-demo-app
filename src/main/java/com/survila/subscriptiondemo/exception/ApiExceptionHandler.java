package com.survila.subscriptiondemo.exception;

import com.survila.subscriptiondemo.util.FriendlyErrorMessages;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(BadRequestException ex) {
        return new ErrorResponse(
                Instant.now(),
                400,
                "Solicitud inválida",
                List.of(FriendlyErrorMessages.apiDetail(ex))
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + translateValidationMessage(error.getDefaultMessage()))
                .toList();

        return new ErrorResponse(
                Instant.now(),
                400,
                "Solicitud inválida",
                details
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
        return new ErrorResponse(
                Instant.now(),
                400,
                "Solicitud inválida",
                List.of(FriendlyErrorMessages.apiDetail(ex))
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        return new ErrorResponse(
                Instant.now(),
                500,
                "Error interno",
                List.of(FriendlyErrorMessages.apiDetail(ex))
        );
    }

    public record ErrorResponse(
            Instant timestamp,
            int status,
            String error,
            List<String> details
    ) {
    }

    private String translateValidationMessage(String message) {
        if (message == null || message.isBlank()) {
            return "tiene un valor inválido";
        }

        return switch (message) {
            case "must not be blank", "must not be empty", "must not be null" -> "es obligatorio";
            case "must be a well-formed email address" -> "debe ser un email válido";
            default -> message;
        };
    }
}
