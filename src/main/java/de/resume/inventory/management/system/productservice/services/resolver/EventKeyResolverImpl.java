package de.resume.inventory.management.system.productservice.services.resolver;

import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
class EventKeyResolverImpl implements EventKeyResolver {

    private static final String EVENT_KEY_SEPARATOR = "-";

    @Override
    public String resolveProductKey(final String tenantId, final String productId) {
        if (Objects.isNull(tenantId) || Objects.isNull(productId)) {
            throw new IllegalArgumentException("Tenant ID and product ID must not be null");
        }
        return tenantId + EVENT_KEY_SEPARATOR + productId;
    }
}
