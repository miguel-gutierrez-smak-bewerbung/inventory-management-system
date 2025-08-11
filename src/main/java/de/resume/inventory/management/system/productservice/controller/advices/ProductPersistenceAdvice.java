package de.resume.inventory.management.system.productservice.controller.advices;

import de.resume.inventory.management.system.productservice.controller.ProductController;
import de.resume.inventory.management.system.productservice.services.advices.ProblemDetailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Order(2)
@RequiredArgsConstructor
@ControllerAdvice(assignableTypes = ProductController.class)
public class ProductPersistenceAdvice {

    private final ProblemDetailService problemDetailService;

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(final DataIntegrityViolationException exception, final HttpServletRequest httpServletRequest) {
        final ProblemDetail problemDetail = problemDetailService.buildDataIntegrityViolation(exception);
        problemDetailService.attachInstance(problemDetail, httpServletRequest);
        return problemDetail;
    }
}