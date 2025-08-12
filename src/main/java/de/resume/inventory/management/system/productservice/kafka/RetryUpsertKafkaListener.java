package de.resume.inventory.management.system.productservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
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
public class RetryUpsertKafkaListener {

    private final DomainEventPublisher<ProductUpsertedEvent> upsertEventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    @Retryable(
            retryFor = { Exception.class },
            backoff = @Backoff(delay = 300L, multiplier = 2.0)
    )
    @KafkaListener(
            topics = "#{@topicConfiguration.productUpsertRetryFail}",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void retry(final ConsumerRecord<String, String> consumerRecord, final Acknowledgment acknowledgment) {
        final String kafkaKey = consumerRecord.key();
        final String payload = consumerRecord.value();
        log.info("RetryUpsertKafkaListener received message. key={}, payload={}", kafkaKey, payload);

        final ProductUpsertedEvent event = toEvent(payload);
        final String publishKey = resolveKafkaKey(kafkaKey, event);

        upsertEventPublisher.publish(publishKey, event);

        acknowledgment.acknowledge();
        log.info("RetryUpsertKafkaListener successfully republished event. key={}", publishKey);
    }

    @Recover
    public void recover(
            final Exception exception, final ConsumerRecord<String, String> consumerRecord, final Acknowledgment acknowledgment
    ) {
        final String kafkaKey = consumerRecord.key();
        final String payload = consumerRecord.value();
        log.error("RetryUpsertKafkaListener exhausted retries for key={}, payload={}. Routing to fail topic. cause={}",
                kafkaKey, payload, exception.getMessage(), exception);

        try {
            final ProductUpsertedEvent failedEvent = toEvent(payload);
            final String publishKey = resolveKafkaKey(kafkaKey, failedEvent);
            upsertEventPublisher.publishFailed(publishKey, failedEvent, "Retry exhausted: " + exception.getMessage());
        } catch (final Exception parseOrPublishFailed) {
            log.error("Failed to publishFailed after retries for key={}, payload={}, cause={}",
                    kafkaKey, payload, parseOrPublishFailed.getMessage(), parseOrPublishFailed);
        } finally {
            acknowledgment.acknowledge();
        }
    }

    private ProductUpsertedEvent toEvent(final String payload) {
        try {
            return objectMapper.readValue(payload, ProductUpsertedEvent.class);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot deserialize ProductUpsertedEvent from payload", e);
        }
    }

    private String resolveKafkaKey(final String incomingKey, final ProductUpsertedEvent event) {
        if (Objects.nonNull(incomingKey) && !incomingKey.isBlank()) {
            return incomingKey;
        }
        if (Objects.nonNull(event) && Objects.nonNull(event.tenantId()) && Objects.nonNull(event.id())) {
            return event.tenantId() + "-" + event.id();
        }
        return "unknown-" + System.currentTimeMillis();
    }
}