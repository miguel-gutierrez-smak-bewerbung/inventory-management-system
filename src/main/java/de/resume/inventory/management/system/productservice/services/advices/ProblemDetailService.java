package de.resume.inventory.management.system.productservice.services.advices;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProblemDetailService {

    public ProblemDetail buildFromBindingErrors(final MethodArgumentNotValidException exception) {
        final Map<String, String> fieldErrorMap = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, fieldError -> Optional.ofNullable(fieldError.getDefaultMessage()).orElse("Invalid value"),
                        (existing, replacement) -> existing, LinkedHashMap::new));

        final ProblemDetail problemDetail = base(HttpStatus.BAD_REQUEST, "Validation failed", "validation-error");
        problemDetail.setProperty("errors", fieldErrorMap);
        return problemDetail;
    }

    public ProblemDetail buildFromConstraintViolations(final jakarta.validation.ConstraintViolationException exception) {
        final Map<String, String> violationMap = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "unknown",
                        violation -> Optional.ofNullable(violation.getMessage()).orElse("Constraint violated"),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        final ProblemDetail problemDetail = base(HttpStatus.BAD_REQUEST, "Constraint violation", "constraint-violation");
        problemDetail.setProperty("errors", violationMap);
        return problemDetail;
    }

    public ProblemDetail buildProductValidation(final String message) {
        final ProblemDetail problemDetail = base(HttpStatus.BAD_REQUEST, "Product validation failed", "validation-error");
        problemDetail.setDetail(message);
        return problemDetail;
    }

    public ProblemDetail buildFromUnreadableMessage(final HttpMessageNotReadableException exception) {
        final Throwable cause = exception.getCause();
        if (cause instanceof final InvalidFormatException invalidFormatException
                && invalidFormatException.getTargetType() != null
                && invalidFormatException.getTargetType().isEnum()) {
            final Class<?> enumType = invalidFormatException.getTargetType();
            final String fieldName = extractFieldName(invalidFormatException);
            final String invalidValue = String.valueOf(invalidFormatException.getValue());
            return buildEnumProblem(enumType, fieldName, invalidValue);
        }

        final ProblemDetail problemDetail = base(HttpStatus.BAD_REQUEST, "Invalid request body", "invalid-request-body");
        final String detailMessage = Optional.ofNullable(exception.getMostSpecificCause())
                .map(Throwable::getMessage)
                .orElse(exception.getMessage());
        problemDetail.setDetail(detailMessage);
        return problemDetail;
    }

    public ProblemDetail buildFromTypeMismatch(final MethodArgumentTypeMismatchException exception) {
        if (exception.getRequiredType() != null && exception.getRequiredType().isEnum()) {
            final Class<?> enumType = exception.getRequiredType();
            final String fieldName = exception.getName();
            final String invalidValue = String.valueOf(exception.getValue());
            return buildEnumProblem(enumType, fieldName, invalidValue);
        }

        final ProblemDetail problemDetail = base(HttpStatus.BAD_REQUEST, "Invalid request parameter", "invalid-request-parameter");
        problemDetail.setDetail(exception.getMessage());
        problemDetail.setProperty("parameter", exception.getName());
        return problemDetail;
    }

    public ProblemDetail buildDataIntegrityViolation(final org.springframework.dao.DataIntegrityViolationException exception) {
        final ProblemDetail problemDetail = base(HttpStatus.CONFLICT, "Data integrity violation", "conflict");
        problemDetail.setDetail("Unique or foreign key constraint violated.");
        final String rootCauseMessage = Optional.ofNullable(exception.getRootCause()).map(Throwable::getMessage).orElse(null);
        if (rootCauseMessage != null) {
            problemDetail.setProperty("rootCause", rootCauseMessage);
        }
        return problemDetail;
    }

    public ProblemDetail buildNotFound(final String title, final String detail) {
        final ProblemDetail problemDetail = base(HttpStatus.NOT_FOUND, title, "not-found");
        problemDetail.setDetail(detail);
        return problemDetail;
    }

    public ProblemDetail buildBadRequest(final String title, final String detail) {
        final ProblemDetail problemDetail = base(HttpStatus.BAD_REQUEST, title, "invalid-request");
        problemDetail.setDetail(detail);
        return problemDetail;
    }

    public ProblemDetail buildServerError(final String detail) {
        final ProblemDetail problemDetail = base(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "server-error");
        problemDetail.setDetail(detail);
        return problemDetail;
    }

    public void attachInstance(final ProblemDetail problemDetail, final HttpServletRequest httpServletRequest) {
        if (httpServletRequest != null) {
            problemDetail.setProperty("instance", httpServletRequest.getRequestURI());
        }
    }

    public ProblemDetail buildNotFoundFromReason(final String exceptionReason) {
        final String title = Optional.ofNullable(exceptionReason)
                .map(reason -> reason.contains(":") ? reason.substring(0, reason.indexOf(':')).trim() : reason)
                .orElse("Not Found");

        final String detail = Optional.ofNullable(exceptionReason)
                .filter(reason -> reason.contains(":"))
                .map(reason -> reason.substring(reason.indexOf(':') + 1).trim())
                .orElse(exceptionReason);

        return buildNotFound(title, detail);
    }

    public ProblemDetail buildBadRequestFromReason(final String exceptionReason) {
        final String title = Optional.ofNullable(exceptionReason).orElse("Invalid request");
        return buildBadRequest(title, exceptionReason);
    }

    public ProblemDetail buildServerErrorFromReason(final String exceptionReason, final String fallbackMessage) {
        final String detail = Optional.ofNullable(exceptionReason).orElse(fallbackMessage);
        return buildServerError("An unexpected error occurred: " + detail);
    }

    private ProblemDetail buildEnumProblem(final Class<?> enumType, final String fieldName, final String invalidValue) {
        final List<String> allowedValuesList = allowedValues(enumType);
        final ProblemDetail problemDetail = base(HttpStatus.BAD_REQUEST, "Invalid enum value", "invalid-enum");
        problemDetail.setDetail("Allowed values: " + String.join(", ", allowedValuesList));
        problemDetail.setProperty("field", fieldName != null ? fieldName : "unknown");
        problemDetail.setProperty("invalidValue", invalidValue);
        problemDetail.setProperty("allowedValues", allowedValuesList);
        return problemDetail;
    }

    private ProblemDetail base(final HttpStatus httpStatus, final String title, final String problemTypeId) {
        final ProblemDetail problemDetail = ProblemDetail.forStatus(httpStatus);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("about:blank#" + problemTypeId));
        return problemDetail;
    }

    private String extractFieldName(final InvalidFormatException invalidFormatException) {
        return invalidFormatException.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("unknown");
    }

    private List<String> allowedValues(final Class<?> enumType) {
        final Object[] constants = enumType.getEnumConstants();
        if (constants == null) {
            return List.of();
        }
        return Arrays.stream(constants).map(Object::toString).collect(Collectors.toList());
    }
}
