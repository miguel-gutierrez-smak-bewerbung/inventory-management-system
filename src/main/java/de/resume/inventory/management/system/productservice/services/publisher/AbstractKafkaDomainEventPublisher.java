package de.resume.inventory.management.system.productservice.services.publisher;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.RetriableException;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

@Slf4j
abstract class AbstractKafkaDomainEventPublisher<T> implements DomainEventPublisher<T> {

    protected abstract KafkaProducer<String, T> producer();
    protected abstract String mainTopic();
    protected abstract String failTopic();
    protected abstract String retryTopic();
    protected abstract String eventId(final T event);

    @Override
    public void publish(final String kafkaKey, final T event) {
        try {
            doSend(mainTopic(), kafkaKey, event, sendCallback("main", event));
        } catch (final Exception exception) {
            handleFailure(kafkaKey, event, exception);
        }
    }

    @Override
    public void publishFailed(final String kafkaKey, final T failedMessage, final String reason) {
        try {
            log.warn("Publishing failed message for {} to fail topic: {}", eventIdSafe(failedMessage), reason);
            doSend(failTopic(), kafkaKey, failedMessage, sendCallback("fail", failedMessage));
        } catch (final Exception exception) {
            log.error("Error while publishing to fail topic for {}: {}", eventIdSafe(failedMessage), exception.getMessage(), exception);
        }
    }

    protected void publishRetry(final String kafkaKey, final T event) {
        try {
            log.warn("Retrying message for {} via retry topic", eventIdSafe(event));
            doSend(retryTopic(), kafkaKey, event, sendCallback("retry", event));
        } catch (final Exception exception) {
            log.error("Failed to send retry message for {}: {}", eventIdSafe(event), exception.getMessage(), exception);
            publishFailed(kafkaKey, event, "Retry failed: " + exception.getMessage());
        }
    }

    @SuppressWarnings("resource")
    private void doSend(final String topic, final String key, final T value, final Callback callback) {
        final ProducerRecord<String, T> record = new ProducerRecord<>(topic, key, value);
        producer().send(record, callback);
    }

    private Callback sendCallback(final String channel, final T event) {
        return (metadata, exception) -> {
            if (exception != null) {
                log.debug("Kafka send error on {} for {}: {}", channel, eventIdSafe(event), exception.toString());
            } else if (metadata != null && log.isDebugEnabled()) {
                log.debug("Kafka send ok on {} for {} to {}-{}@{}", channel, eventIdSafe(event),
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        };
    }

    private void handleFailure(final String kafkaKey, final T event, final Exception exception) {
        if (isRetryable(exception)) {
            publishRetry(kafkaKey, event);
            return;
        }
        publishFailed(kafkaKey, event, "Non-retryable error: " + exception.getMessage());
    }

    protected boolean isRetryable(final Exception exception) {
        if (Objects.isNull(exception)) return false;
        final Throwable root = exception.getCause() != null ? exception.getCause() : exception;
        return root instanceof TimeoutException
                || root instanceof RetriableException
                || (root instanceof KafkaException
                    && root.getMessage() != null
                    && root.getMessage().toLowerCase().contains("retryable"));
    }

    private String eventIdSafe(final T event) {
        try {
            return eventId(event);
        } catch (final Exception ignore) {
            return "<unknown>";
        }
    }
}
