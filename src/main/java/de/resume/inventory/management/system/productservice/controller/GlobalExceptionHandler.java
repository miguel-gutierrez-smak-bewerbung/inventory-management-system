package de.resume.inventory.management.system.productservice.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import de.resume.inventory.management.system.productservice.exceptions.ProductNotFoundException;
import de.resume.inventory.management.system.productservice.exceptions.ProductValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex, final HttpServletRequest request) {
        final Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> Optional.ofNullable(fe.getDefaultMessage()).orElse("Invalid value"),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        final ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation failed", "validation-error");
        pd.setProperty("errors", fieldErrors);
        attachInstance(pd, request);
        return problemResponse(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            final ConstraintViolationException ex, final HttpServletRequest request) {
        final Map<String, String> violations = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath() != null ? v.getPropertyPath().toString() : "unknown",
                        v -> Optional.ofNullable(v.getMessage()).orElse("Constraint violated"),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        final ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Constraint violation", "constraint-violation");
        pd.setProperty("errors", violations);
        attachInstance(pd, request);
        return problemResponse(pd);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            final NoSuchElementException ex, final HttpServletRequest request) {
        final ProblemDetail pd = problem(HttpStatus.NOT_FOUND, "Resource not found", "not-found");
        pd.setDetail(ex.getMessage());
        attachInstance(pd, request);
        return problemResponse(pd);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleConflict(
            final DataIntegrityViolationException ex, final HttpServletRequest request) {
        final ProblemDetail pd = problem(HttpStatus.CONFLICT, "Data integrity violation", "conflict");
        pd.setDetail("Unique or foreign key constraint violated.");
        final String rootMessage = Optional.ofNullable(ex.getRootCause()).map(Throwable::getMessage).orElse(null);
        if (rootMessage != null) {
            pd.setProperty("rootCause", rootMessage);
        }
        attachInstance(pd, request);
        return problemResponse(pd);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
            final HttpMessageNotReadableException ex, final HttpServletRequest request) {
        final Throwable cause = ex.getCause();
        if (cause instanceof final InvalidFormatException ife
                && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {
            final Class<?> enumType = ife.getTargetType();
            final String fieldName = extractFieldName(ife);
            final String invalidValue = String.valueOf(ife.getValue());
            final ProblemDetail enumPd = buildEnumProblem(enumType, fieldName, invalidValue);
            attachInstance(enumPd, request);
            return problemResponse(enumPd);
        }

        final ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Invalid request body", "invalid-request-body");
        final String detail = Optional.ofNullable(ex.getMostSpecificCause())
                .map(Throwable::getMessage).orElse(ex.getMessage());
        pd.setDetail(detail);
        attachInstance(pd, request);
        return problemResponse(pd);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(
            final MethodArgumentTypeMismatchException ex, final HttpServletRequest request) {
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            final Class<?> enumType = ex.getRequiredType();
            final String fieldName = ex.getName();
            final String invalidValue = String.valueOf(ex.getValue());
            final ProblemDetail pd = buildEnumProblem(enumType, fieldName, invalidValue);
            attachInstance(pd, request);
            return problemResponse(pd);
        }

        final ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Invalid request parameter", "invalid-request-parameter");
        pd.setDetail(ex.getMessage());
        pd.setProperty("parameter", ex.getName());
        attachInstance(pd, request);
        return problemResponse(pd);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            final IllegalArgumentException ex, final HttpServletRequest request) {
        final ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Invalid request", "invalid-request");
        pd.setDetail(ex.getMessage());
        attachInstance(pd, request);
        return problemResponse(pd);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleProductNotFound(
            final ProductNotFoundException ex, final HttpServletRequest request) {
        final ProblemDetail pd = problem(HttpStatus.NOT_FOUND, "Product not found", "not-found");
        pd.setDetail(ex.getMessage());
        attachInstance(pd, request);
        return problemResponse(pd);
    }

    @ExceptionHandler(ProductValidationException.class)
    public ResponseEntity<ProblemDetail> handleProductValidation(
            final ProductValidationException ex, final HttpServletRequest request) {
        final ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Product validation failed", "validation-error");
        pd.setDetail(ex.getMessage());
        attachInstance(pd, request);
        return problemResponse(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(
            final Exception ex, final HttpServletRequest request) {
        final ProblemDetail pd = problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "server-error");
        pd.setDetail("An unexpected error occurred: " + ex.getMessage());
        attachInstance(pd, request);
        return problemResponse(pd);
    }

    private ProblemDetail problem(final HttpStatus status, final String title, final String typeId) {
        final ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setTitle(title);
        pd.setType(URI.create("about:blank#" + typeId));
        return pd;
    }

    private ResponseEntity<ProblemDetail> problemResponse(final ProblemDetail pd) {
        return ResponseEntity.status(pd.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    private void attachInstance(final ProblemDetail pd, final HttpServletRequest request) {
        if (request != null) {
            pd.setProperty("instance", request.getRequestURI());
        }
    }

    private ProblemDetail buildEnumProblem(final Class<?> enumType, final String fieldName, final String invalidValue) {
        final List<String> allowed = allowedValues(enumType);
        final ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Invalid enum value", "invalid-enum");
        pd.setDetail("Allowed values: " + String.join(", ", allowed));
        pd.setProperty("field", fieldName != null ? fieldName : "unknown");
        pd.setProperty("invalidValue", invalidValue);
        pd.setProperty("allowedValues", allowed);
        return pd;
    }

    private String extractFieldName(final InvalidFormatException ife) {
        return ife.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("unknown");
    }

    private List<String> allowedValues(final Class<?> enumType) {
        final Object[] constants = enumType.getEnumConstants();
        if (constants == null) return List.of();
        return Arrays.stream(constants).map(Object::toString).collect(Collectors.toList());
    }
}