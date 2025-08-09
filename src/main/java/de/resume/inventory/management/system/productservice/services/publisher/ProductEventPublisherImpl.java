package de.resume.inventory.management.system.productservice.services.publisher;

import de.resume.inventory.management.system.productservice.config.TopicConfiguration;
import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
class ProductEventPublisherImpl implements ProductEventPublisher {

    private final TopicConfiguration topicConfiguration;
    private final KafkaProducer<String, ProductUpsertedEvent> kafkaProducer;
    private final KafkaProducer<String, ProductDeletedEvent> kafkaDeletedProducer;

    @Override
    public void publishProductUpserted(final String kafkaKey, final ProductUpsertedEvent productUpsertedEvent) {
        log.debug("Publishing product upserted event: {}", productUpsertedEvent);
        try {
            sendMessageToKafka(kafkaKey, productUpsertedEvent);
        } catch (final Exception exception) {
            handlePublishFailure(kafkaKey, productUpsertedEvent, exception);
        }
    }

    @Override
    public void publishProductUpsertFailed(final String kafkaKey, final ProductUpsertedEvent failedMessage, final String reason) {
        try {
            log.warn("Publishing failed message for product {} to fail topic: {}", failedMessage, reason);
            final ProducerRecord<String, ProductUpsertedEvent> producerRecord = new ProducerRecord<>(
                    topicConfiguration.getProductUpsertFail(), kafkaKey, failedMessage
            );
            kafkaProducer.send(producerRecord);
        } catch (final Exception exception) {
            log.error("Error while publishing to fail topic for product {}: {}", failedMessage.id(), exception.getMessage(), exception);
        }
    }

    @Override
    public void  publishProductDeleted(final String kafkaKey, final ProductDeletedEvent productDeletedEvent) {
        try {
            log.info("Publishing product deleted event: {}", productDeletedEvent);
            sendMessageToKafka(kafkaKey, productDeletedEvent);
        } catch (Exception exception) {
            log.error("Error while publishing product deleted event: {}", exception.getMessage(), exception);
        }
    }

    private void sendMessageToKafka(final String kafkaKey, final ProductUpsertedEvent productUpsertedEvent) {
        final ProducerRecord<String, ProductUpsertedEvent> record =
                new ProducerRecord<>(topicConfiguration.getProductUpsert(), kafkaKey, productUpsertedEvent);
        kafkaProducer.send(record);
    }

    private void sendMessageToKafka(final String kafkaKey, final ProductDeletedEvent productDeletedEvent) {
        final ProducerRecord<String, ProductDeletedEvent> record =
                new ProducerRecord<>(topicConfiguration.getProductDelete(), kafkaKey, productDeletedEvent);
        kafkaDeletedProducer.send(record);
    }

    private void handlePublishFailure(final String kafkaKey, final ProductUpsertedEvent productUpsertedEvent, final Exception exception) {
        if (isRetryable(exception)) {
            log.warn("Retryable error for product {}, sending to retry topic", productUpsertedEvent.id());
            publishProductUpsertRetry(kafkaKey, productUpsertedEvent);
            return;
        }

        log.warn("Non-retryable error for product {}, sending to fail topic", productUpsertedEvent.id());
        publishProductUpsertFailed(kafkaKey, productUpsertedEvent, "Non-retryable error: " + exception.getMessage());
    }

    private boolean isRetryable(final Exception exception) {
        if (Objects.isNull(exception)) {
            return false;
        }

        final Throwable rootCause = exception.getCause() != null ? exception.getCause() : exception;

        return rootCause instanceof TimeoutException
                || rootCause instanceof RetriableException
                || (rootCause instanceof KafkaException
                && Objects.nonNull(rootCause.getMessage())
                && rootCause.getMessage().toLowerCase().contains("retryable"));
    }

    private void publishProductUpsertRetry(final String kafkaKey, final ProductUpsertedEvent productUpsertedEvent) {
        try {
            log.warn("Retrying message for product {} via retry topic", productUpsertedEvent.id());
            final ProducerRecord<String, ProductUpsertedEvent> record = new ProducerRecord<>(
                    topicConfiguration.getProductUpsertRetryFail(),
                    kafkaKey, productUpsertedEvent
            );
            kafkaProducer.send(record);
        } catch (final Exception exception) {
            log.error("Failed to send retry message for product {}: {}", productUpsertedEvent.id(), exception.getMessage(), exception);
            publishProductUpsertFailed(kafkaKey, productUpsertedEvent, "Retry failed: " + exception.getMessage());
        }
    }
}
