package az.animeazerbaycan.common;

import az.animeazerbaycan.dto.response.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.security.access.AccessDeniedException;
import jakarta.validation.ConstraintViolationException;

import java.util.*;

@RestControllerAdvice
public class ExceptionHandlerAdvice {
    private ResponseEntity<ErrorResponse> error(int status, String message, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ErrorResponse(status, message, fields));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> api(ApiException e) {
        return error(e.getStatus(), e.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new TreeMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
        return error(400, "Validation failed", fields);
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<ErrorResponse> bad(Exception e) {
        return error(400, "Invalid request", Map.of());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> methodValidation(HandlerMethodValidationException e) {
        Map<String, String> fields = new TreeMap<>();
        for (var result : e.getParameterValidationResults()) {
            if (result instanceof org.springframework.validation.method.ParameterErrors errors)
                errors.getFieldErrors().forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
            else {
                String name = result.getMethodParameter().getParameterName();
                result.getResolvableErrors().forEach(error -> fields.put(name == null ? "request" : name, error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()));
            }
        }
        return error(400, "Validation failed", fields);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> conflict(Exception e) {
        return error(409, "Resource conflicts with existing data", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> denied(Exception e) {
        return error(403, "Access denied", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> other(Exception e) {
        return error(500, "Internal server error", Map.of());
    }
}
