package de.resume.inventory.management.system.productservice.controller.advices;

import de.resume.inventory.management.system.productservice.controller.ProductController;
import de.resume.inventory.management.system.productservice.exceptions.ProductNotFoundException;
import de.resume.inventory.management.system.productservice.services.factories.ProblemDetailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Order(3)
@RequiredArgsConstructor
@ControllerAdvice(assignableTypes = ProductController.class)
public class ProductGenericAdvice {

    private final ProblemDetailService problemDetailService;

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(
            final ProductNotFoundException exception, final HttpServletRequest httpServletRequest) {
        final ProblemDetail problemDetail = problemDetailService.buildNotFound("Product not found", exception.getMessage());
        problemDetailService.attachInstance(problemDetail, httpServletRequest);
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(
            final IllegalArgumentException exception, final HttpServletRequest httpServletRequest) {
        final ProblemDetail problemDetail = problemDetailService.buildBadRequest("Invalid request", exception.getMessage());
        problemDetailService.attachInstance(problemDetail, httpServletRequest);
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(
            final Exception exception, final HttpServletRequest httpServletRequest) {
        final ProblemDetail problemDetail = problemDetailService.buildServerError("An unexpected error occurred: " + exception.getMessage());
        problemDetailService.attachInstance(problemDetail, httpServletRequest);
        return problemDetail;
    }
}