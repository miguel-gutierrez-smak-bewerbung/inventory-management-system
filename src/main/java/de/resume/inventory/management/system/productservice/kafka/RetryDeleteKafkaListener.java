package de.resume.inventory.management.system.productservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.resume.inventory.management.system.productservice.config.TopicConfiguration;
import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
import de.resume.inventory.management.system.productservice.services.publisher.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class RetryDeleteKafkaListener {

    private final DomainEventPublisher<ProductDeletedEvent> deleteEventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    @Retryable(
            retryFor = { Exception.class },
            backoff = @Backoff(delay = 300L, multiplier = 2.0)
    )
    @KafkaListener(
            topics = "#{@topicConfiguration.productDeleteRetryFail}",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void retry(final ConsumerRecord<String, String> consumerRecord, final Acknowledgment acknowledgment) {
        final String kafkaKey = consumerRecord.key();
        final String payload = consumerRecord.value();
        log.info("RetryDeleteKafkaListener received message. key={}, payload={}", kafkaKey, payload);

        final ProductDeletedEvent event = toEvent(payload);
        final String publishKey = resolveKafkaKey(kafkaKey, event);

        deleteEventPublisher.publish(publishKey, event);

        acknowledgment.acknowledge();
        log.info("RetryDeleteKafkaListener successfully republished event. key={}", publishKey);
    }

    @Recover
    public void recover(final Exception exception,
                        final ConsumerRecord<String, String> consumerRecord,
                        final Acknowledgment acknowledgment) {
        final String kafkaKey = consumerRecord.key();
        final String payload = consumerRecord.value();
        log.error("RetryDeleteKafkaListener exhausted retries for key={}, payload={}. Routing to fail topic. cause={}",
                kafkaKey, payload, exception.getMessage(), exception);

        try {
            final ProductDeletedEvent failedEvent = toEvent(payload);
            final String publishKey = resolveKafkaKey(kafkaKey, failedEvent);
            deleteEventPublisher.publishFailed(publishKey, failedEvent, "Retry exhausted: " + exception.getMessage());
        } catch (final Exception parseOrPublishFailed) {
            log.error("Failed to publishFailed after retries for key={}, payload={}, cause={}",
                    kafkaKey, payload, parseOrPublishFailed.getMessage(), parseOrPublishFailed);
        } finally {
            acknowledgment.acknowledge();
        }
    }

    private ProductDeletedEvent toEvent(final String payload) {
        try {
            return objectMapper.readValue(payload, ProductDeletedEvent.class);
        } catch (final com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot deserialize ProductDeletedEvent from payload", e);
        }
    }

    private String resolveKafkaKey(final String incomingKey, final ProductDeletedEvent event) {
        if (Objects.nonNull(incomingKey) && !incomingKey.isBlank()) {
            return incomingKey;
        }
        if (Objects.nonNull(event) && Objects.nonNull(event.tenantId()) && Objects.nonNull(event.id())) {
            return event.tenantId() + "-" + event.id();
        }
        return "unknown-" + System.currentTimeMillis();
    }
}
