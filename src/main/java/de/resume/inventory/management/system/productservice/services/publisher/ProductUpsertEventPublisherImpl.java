package de.resume.inventory.management.system.productservice.services.publisher;

import de.resume.inventory.management.system.productservice.config.TopicConfiguration;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
final class ProductUpsertEventPublisherImpl extends AbstractKafkaDomainEventPublisher<ProductUpsertedEvent> {

    private final TopicConfiguration topicConfiguration;
    private final KafkaProducer<String, ProductUpsertedEvent> upsertProducer;

    @Override
    protected KafkaProducer<String, ProductUpsertedEvent> producer() {
        return upsertProducer;
    }

    @Override
    protected String mainTopic() {
        return topicConfiguration.getProductUpsert();
    }

    @Override
    protected String failTopic() {
        return topicConfiguration.getProductUpsertFail();
    }

    @Override
    protected String retryTopic() {
        return topicConfiguration.getProductUpsertRetryFail();
    }

    @Override
    protected String eventId(final ProductUpsertedEvent event) {
        return event.id();
    }
}