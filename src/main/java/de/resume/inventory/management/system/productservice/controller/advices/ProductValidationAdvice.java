package de.resume.inventory.management.system.productservice.services.advices;

import de.resume.inventory.management.system.productservice.exceptions.ProductValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Order(1)
@RequiredArgsConstructor
@ControllerAdvice(assignableTypes = de.resume.inventory.management.system.productservice.controller.ProductController.class)
public class ProductValidationAdvice {

    private final ProblemDetailService problemDetailService;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(final MethodArgumentNotValidException exception,
                                                      final HttpServletRequest httpServletRequest) {
        final ProblemDetail problemDetail = problemDetailService.buildFromBindingErrors(exception);
        problemDetailService.attachInstance(problemDetail, httpServletRequest);
        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(final ConstraintViolationException exception,
                                                   final HttpServletRequest httpServletRequest) {
        final ProblemDetail problemDetail = problemDetailService.buildFromConstraintViolations(exception);
        problemDetailService.attachInstance(problemDetail, httpServletRequest);
        return problemDetail;
    }

    @ExceptionHandler(ProductValidationException.class)
    public ProblemDetail handleProductValidation(final ProductValidationException exception,
                                                 final HttpServletRequest httpServletRequest) {
        final ProblemDetail problemDetail = problemDetailService.buildProductValidation(exception.getMessage());
        problemDetailService.attachInstance(problemDetail, httpServletRequest);
        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(final HttpMessageNotReadableException exception,
                                                      final HttpServletRequest httpServletRequest) {
        final ProblemDetail problemDetail = problemDetailService.buildFromUnreadableMessage(exception);
        problemDetailService.attachInstance(problemDetail, httpServletRequest);
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(final MethodArgumentTypeMismatchException exception,
                                                          final HttpServletRequest httpServletRequest) {
        final ProblemDetail problemDetail = problemDetailService.buildFromTypeMismatch(exception);
        problemDetailService.attachInstance(problemDetail, httpServletRequest);
        return problemDetail;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(final ResponseStatusException exception,
                                                       final HttpServletRequest httpServletRequest) {
        final String exceptionReason = exception.getReason();

        if (exception.getStatusCode().value() == 404) {
            final ProblemDetail problemDetail = problemDetailService.buildNotFoundFromReason(exceptionReason);
            problemDetailService.attachInstance(problemDetail, httpServletRequest);
            return problemDetail;
        }

        if (exception.getStatusCode().is4xxClientError()) {
            final ProblemDetail problemDetail = problemDetailService.buildBadRequestFromReason(exceptionReason);
            problemDetailService.attachInstance(problemDetail, httpServletRequest);
            return problemDetail;
        }

        final String fallbackMessage = Optional.of(exception.getMessage()).orElse("Unexpected error");
        final ProblemDetail problemDetail = problemDetailService.buildServerErrorFromReason(exceptionReason, fallbackMessage);
        problemDetailService.attachInstance(problemDetail, httpServletRequest);
        return problemDetail;
    }
}