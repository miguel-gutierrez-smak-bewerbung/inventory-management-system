package de.resume.inventory.management.system.productservice.services.resolver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class EventKeyResolverTest {

    private final EventKeyResolver eventKeyResolver = new EventKeyResolverImpl();

    @Test
    void resolveProductKey() {
        final String tenantId = "Event-tenant";
        final String productId = "product-1";

        final String expectedKey = tenantId + "-" + productId;

        final String actualKey = eventKeyResolver.resolveProductKey(tenantId, productId);

        assert expectedKey.equals(actualKey);
    }

    @ParameterizedTest
    @MethodSource("provideNullArgumentCombinations")
    void resolveProductKey_shouldThrowOnNullArguments(final String tenantId, final String productId) {

        final IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> eventKeyResolver.resolveProductKey(tenantId, productId)
        );

        final String expectedMessage = "Tenant ID and product ID must not be null";
        final String actualMessage = exception.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    public static Stream<Arguments> provideNullArgumentCombinations() {
        return Stream.of(
                Arguments.of(null, "product-1"),
                Arguments.of("Event-tenant", null),
                Arguments.of(null, null)
        );
    }
}
