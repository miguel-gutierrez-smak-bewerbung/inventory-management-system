package de.resume.inventory.management.system.productservice.services.resolver;

public interface EventKeyResolver {

    String resolveProductKey(final String tenantId, final String productId);
}
