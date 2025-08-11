package de.resume.inventory.management.system.productservice.services.factories;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import de.resume.inventory.management.system.productservice.services.advices.ProblemDetailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ProblemDetailServiceTest {

    private enum TestCategory { ELECTRONICS, HOUSEHOLD }

    private final ProblemDetailService sut = new ProblemDetailService();

    @Test
    @DisplayName("buildFromBindingErrors — collects field errors and sets BAD_REQUEST")
    void buildFromBindingErrors_collectsFieldErrors() throws NoSuchMethodException {
        final Object targetObject = new Object();
        final BindingResult bindingResult = new BeanPropertyBindingResult(targetObject, "product");
        bindingResult.addError(new FieldError("product", "name", null, false, null, null, "must not be blank"));
        bindingResult.addError(new FieldError("product", "price", null, false, null, null, "must be positive"));

        final Method dummyMethod = Dummy.class.getDeclaredMethod("dummy", String.class);
        final MethodParameter methodParameter = new MethodParameter(dummyMethod, 0);
        final MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        final ProblemDetail problemDetail = sut.buildFromBindingErrors(exception);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
        Assertions.assertEquals("Validation failed", problemDetail.getTitle());
        Assertions.assertEquals(URI.create("about:blank#validation-error"), problemDetail.getType());

        Assertions.assertNotNull(problemDetail.getProperties());
        final Object errorsObject = problemDetail.getProperties().get("errors");
        Assertions.assertInstanceOf(Map.class, errorsObject);
        @SuppressWarnings("unchecked")
        final Map<String, String> errorsMap = (Map<String, String>) errorsObject;
        Assertions.assertEquals("must not be blank", errorsMap.get("name"));
        Assertions.assertEquals("must be positive", errorsMap.get("price"));
    }

    @Test
    @DisplayName("buildFromConstraintViolations — collects violations and sets BAD_REQUEST")
    void buildFromConstraintViolations_collectsViolations() {
        @SuppressWarnings("unchecked")
        final ConstraintViolation<Object> violationOne = Mockito.mock(ConstraintViolation.class);
        final Path pathOne = Mockito.mock(Path.class);
        Mockito.when(pathOne.toString()).thenReturn("name");
        Mockito.when(violationOne.getPropertyPath()).thenReturn(pathOne);
        Mockito.when(violationOne.getMessage()).thenReturn("must not be blank");

        @SuppressWarnings("unchecked")
        final ConstraintViolation<Object> violationTwo = Mockito.mock(ConstraintViolation.class);
        final Path pathTwo = Mockito.mock(Path.class);
        Mockito.when(pathTwo.toString()).thenReturn("price");
        Mockito.when(violationTwo.getPropertyPath()).thenReturn(pathTwo);
        Mockito.when(violationTwo.getMessage()).thenReturn("must be positive");

        final Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(violationOne);
        violations.add(violationTwo);

        final ConstraintViolationException exception = new ConstraintViolationException(violations);
        final ProblemDetail problemDetail = sut.buildFromConstraintViolations(exception);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
        Assertions.assertEquals("Constraint violation", problemDetail.getTitle());

        Assertions.assertNotNull(problemDetail.getProperties());
        final Object errorsObject = problemDetail.getProperties().get("errors");
        Assertions.assertInstanceOf(Map.class, errorsObject);
        @SuppressWarnings("unchecked")
        final Map<String, String> errorsMap = (Map<String, String>) errorsObject;
        Assertions.assertEquals("must not be blank", errorsMap.get("name"));
        Assertions.assertEquals("must be positive", errorsMap.get("price"));
    }

    @Test
    @DisplayName("buildProductValidation — sets message and BAD_REQUEST")
    void buildProductValidation_setsMessage() {
        final String validationMessage = "Some validation message";
        final ProblemDetail problemDetail = sut.buildProductValidation(validationMessage);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
        Assertions.assertEquals("Product validation failed", problemDetail.getTitle());
        Assertions.assertEquals(validationMessage, problemDetail.getDetail());
        Assertions.assertEquals(URI.create("about:blank#validation-error"), problemDetail.getType());
    }

    @Test
    @DisplayName("buildFromUnreadableMessage — enum value produces invalid-enum problem with allowed values")
    void buildFromUnreadableMessage_enumValue() {
        final InvalidFormatException invalidFormatException =
                new InvalidFormatException(null, "invalid enum", "WRONG", TestCategory.class);
        invalidFormatException.prependPath(new Object(), "category");

        final HttpMessageNotReadableException httpMessageNotReadableException =
                new HttpMessageNotReadableException("payload error", invalidFormatException, null);

        final ProblemDetail problemDetail = sut.buildFromUnreadableMessage(httpMessageNotReadableException);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
        Assertions.assertEquals("Invalid enum value", problemDetail.getTitle());
        Assertions.assertEquals(URI.create("about:blank#invalid-enum"), problemDetail.getType());
        Assertions.assertEquals("category", problemDetail.getProperties().get("field"));
        Assertions.assertEquals("WRONG", problemDetail.getProperties().get("invalidValue"));

        final Object allowedValuesObject = problemDetail.getProperties().get("allowedValues");
        Assertions.assertInstanceOf(List.class, allowedValuesObject);
        @SuppressWarnings("unchecked")
        final List<String> allowedValues = (List<String>) allowedValuesObject;
        Assertions.assertTrue(allowedValues.contains("ELECTRONICS"));
        Assertions.assertTrue(allowedValues.contains("HOUSEHOLD"));
    }

    @Test
    @DisplayName("buildFromUnreadableMessage — generic unreadable payload yields invalid-request-body")
    void buildFromUnreadableMessage_generic() {
        final RuntimeException rootCause = new RuntimeException("bad json");
        final HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException("payload error", rootCause, null);

        final ProblemDetail problemDetail = sut.buildFromUnreadableMessage(exception);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
        Assertions.assertEquals("Invalid request body", problemDetail.getTitle());
        Assertions.assertEquals("bad json", problemDetail.getDetail());
        Assertions.assertEquals(URI.create("about:blank#invalid-request-body"), problemDetail.getType());
    }

    @Nested
    class TypeMismatchTests {
        @ParameterizedTest(name = "param {0} with value {1}")
        @CsvSource({"price,42","amount,17"})
        @DisplayName("buildFromTypeMismatch — non-enum parameter produces invalid-request-parameter with parameter property")
        void buildFromTypeMismatch_nonEnum(final String parameterName, final String rawValue) {
            final MethodArgumentTypeMismatchException exception =
                    new MethodArgumentTypeMismatchException(rawValue, Integer.class, parameterName, null, new NumberFormatException("NFE"));

            final ProblemDetail problemDetail = sut.buildFromTypeMismatch(exception);

            Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
            Assertions.assertEquals("Invalid request parameter", problemDetail.getTitle());
            Assertions.assertEquals(parameterName, problemDetail.getProperties().get("parameter"));
            Assertions.assertEquals(URI.create("about:blank#invalid-request-parameter"), problemDetail.getType());
        }

        @Test
        @DisplayName("buildFromTypeMismatch — enum parameter produces invalid-enum with allowed values")
        void buildFromTypeMismatch_enum() {
            final MethodArgumentTypeMismatchException exception =
                    new MethodArgumentTypeMismatchException("WRONG", TestCategory.class, "category", null, null);

            final ProblemDetail problemDetail = sut.buildFromTypeMismatch(exception);

            Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
            Assertions.assertEquals("Invalid enum value", problemDetail.getTitle());
            Assertions.assertEquals("category", problemDetail.getProperties().get("field"));
            final Object allowedValuesObject = problemDetail.getProperties().get("allowedValues");
            Assertions.assertInstanceOf(List.class, allowedValuesObject);
        }
    }

    @Test
    @DisplayName("buildDataIntegrityViolation — sets conflict and rootCause property")
    void buildDataIntegrityViolation_setsConflict() {
        final DataIntegrityViolationException exception =
                new DataIntegrityViolationException("violated", new RuntimeException("duplicate key"));

        final ProblemDetail problemDetail = sut.buildDataIntegrityViolation(exception);

        Assertions.assertEquals(HttpStatus.CONFLICT.value(), problemDetail.getStatus());
        Assertions.assertEquals("Data integrity violation", problemDetail.getTitle());
        Assertions.assertEquals("Unique or foreign key constraint violated.", problemDetail.getDetail());
        Assertions.assertEquals("duplicate key", problemDetail.getProperties().get("rootCause"));
        Assertions.assertEquals(URI.create("about:blank#conflict"), problemDetail.getType());
    }

    @Test
    @DisplayName("buildNotFound — sets NOT_FOUND and detail")
    void buildNotFound_setsValues() {
        final String title = "Product not found";
        final String detail = "id=missing";

        final ProblemDetail problemDetail = sut.buildNotFound(title, detail);

        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.getStatus());
        Assertions.assertEquals(title, problemDetail.getTitle());
        Assertions.assertEquals(detail, problemDetail.getDetail());
        Assertions.assertEquals(URI.create("about:blank#not-found"), problemDetail.getType());
    }

    @Test
    @DisplayName("buildBadRequest — sets BAD_REQUEST and detail")
    void buildBadRequest_setsValues() {
        final String title = "Invalid request";
        final String detail = "bad value";

        final ProblemDetail problemDetail = sut.buildBadRequest(title, detail);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
        Assertions.assertEquals(title, problemDetail.getTitle());
        Assertions.assertEquals(detail, problemDetail.getDetail());
        Assertions.assertEquals(URI.create("about:blank#invalid-request"), problemDetail.getType());
    }

    @Test
    @DisplayName("buildServerError — sets INTERNAL_SERVER_ERROR and detail")
    void buildServerError_setsValues() {
        final String detail = "boom";

        final ProblemDetail problemDetail = sut.buildServerError(detail);

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.getStatus());
        Assertions.assertEquals("Internal server error", problemDetail.getTitle());
        Assertions.assertEquals(detail, problemDetail.getDetail());
        Assertions.assertEquals(URI.create("about:blank#server-error"), problemDetail.getType());
    }

    @Test
    @DisplayName("attachInstance — sets instance property from HttpServletRequest")
    void attachInstance_setsInstance() {
        final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        final HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn("/api/products/123");

        sut.attachInstance(problemDetail, httpServletRequest);

        Assertions.assertEquals("/api/products/123", problemDetail.getProperties().get("instance"));
    }

    private static class Dummy {
        @SuppressWarnings("unused")
        void dummy(final String value) { }
    }
}