package de.resume.inventory.management.system.productservice.services.publisher;


public interface DomainEventPublisher<T> {
    void publish(final String kafkaKey, final T event);
    void publishFailed(final String kafkaKey, final T failedMessage, final String reason);
}
