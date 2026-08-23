package com.wb.culinaryCode.controller.advice;

import com.wb.culinaryCode.exception.RecipeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates exceptions into the API's error body: {@code {"message": ...}}, plus an
 * {@code "errors"} map of field name to message for validation failures.
 *
 * <p>The framework's own client-error exceptions are handled explicitly. They have to be:
 * handlers on a {@code @RestControllerAdvice} are consulted before Spring's
 * DefaultHandlerExceptionResolver, so without these the catch-all below would swallow a
 * malformed UUID or an unparseable body and report it as a 500.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        var fieldErrors = new LinkedHashMap<String, String>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        var body = new LinkedHashMap<String, Object>();
        body.put("message", "Validation failed");
        body.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return badRequest("Invalid value for '" + ex.getName() + "'");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return badRequest("Malformed request body");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return badRequest("Missing required parameter '" + ex.getParameterName() + "'");
    }

    /**
     * Constraint violations that only the database can catch — a quantity too large for
     * {@code NUMERIC(10,2)}, a duplicate key — are bad input, not server faults.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Rejected request that violated a database constraint", ex);
        return badRequest("One or more values are invalid or conflict with existing data");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return status(HttpStatus.METHOD_NOT_ALLOWED, ex.getMethod() + " is not supported by this endpoint");
    }

    @ExceptionHandler({RecipeNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(Exception ex) {
        var message = ex instanceof RecipeNotFoundException ? ex.getMessage() : "Resource not found";
        return status(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Bad credentials and the like. Deliberately vague: saying which half was wrong tells an
     * attacker whether an email is registered.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        return status(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return status(HttpStatus.FORBIDDEN, "You don't have access to that");
    }

    /**
     * Re-emits the app's {@code {"message": ...}} shape. Without this the catch-all below turns
     * a deliberate 403 or 409 into a 500, since Spring's own handling never gets a look in.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        return status(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return status(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        return status(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<Map<String, Object>> status(HttpStatus status, String message) {
        var body = new LinkedHashMap<String, Object>();
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
