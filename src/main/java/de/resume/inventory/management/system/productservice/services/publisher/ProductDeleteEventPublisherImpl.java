package de.resume.inventory.management.system.productservice.services.publisher;

import de.resume.inventory.management.system.productservice.config.TopicConfiguration;
import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
final class ProductDeleteEventPublisherImpl extends AbstractKafkaDomainEventPublisher<ProductDeletedEvent> {

    private final TopicConfiguration topicConfiguration;
    private final KafkaProducer<String, ProductDeletedEvent> deleteProducer;

    @Override
    protected KafkaProducer<String, ProductDeletedEvent> producer() {
        return deleteProducer;
    }

    @Override
    protected String mainTopic() {
        return topicConfiguration.getProductDelete();
    }

    @Override
    protected String failTopic() {
        return topicConfiguration.getProductDeleteFail();
    }

    @Override
    protected String retryTopic() {
        return topicConfiguration.getProductDeleteRetryFail();
    }

    @Override
    protected String eventId(final ProductDeletedEvent event) {
        return event.id();
    }
}
