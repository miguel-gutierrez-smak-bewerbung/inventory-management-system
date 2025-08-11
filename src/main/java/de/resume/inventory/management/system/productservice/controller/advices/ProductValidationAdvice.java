package de.resume.inventory.management.system.productservice.controller.advices;

import de.resume.inventory.management.system.productservice.controller.ProductController;
import de.resume.inventory.management.system.productservice.exceptions.ProductValidationException;
import de.resume.inventory.management.system.productservice.services.factories.ProblemDetailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@Order(1)
@RequiredArgsConstructor
@ControllerAdvice(assignableTypes = ProductController.class)
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
}
