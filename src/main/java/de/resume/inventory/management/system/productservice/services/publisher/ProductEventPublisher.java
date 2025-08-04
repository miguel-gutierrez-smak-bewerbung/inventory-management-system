package de.resume.inventory.management.system.productservice.services.publisher;

import de.resume.inventory.management.system.productservice.models.messages.ProductUpsertedMessage;

public interface ProductEventPublisher {
    void publishProductUpserted(final ProductUpsertedMessage productUpsertedMessage);
    void publishProductUpsertFailed(final ProductUpsertedMessage failedMessage, final String reason);
}
