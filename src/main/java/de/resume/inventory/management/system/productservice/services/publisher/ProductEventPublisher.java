package de.resume.inventory.management.system.productservice.services.publisher;

import de.resume.inventory.management.system.productservice.models.messages.ProductUpsertedEvent;

public interface ProductEventPublisher {
    void publishProductUpserted(final String kafkaKey, final ProductUpsertedEvent productUpsertedEvent);
    void publishProductUpsertFailed(final String kafkaKey, final ProductUpsertedEvent failedMessage, final String reason);
}
