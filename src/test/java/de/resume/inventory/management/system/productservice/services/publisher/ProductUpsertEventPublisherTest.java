package de.resume.inventory.management.system.productservice.services.publisher;

import de.resume.inventory.management.system.productservice.config.TopicConfiguration;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TimeoutException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Objects;

@ExtendWith(MockitoExtension.class)
class ProductUpsertEventPublisherTest {

    @Mock
    private TopicConfiguration topicConfigurationMock;

    @Mock
    private KafkaProducer<String, ProductUpsertedEvent> upsertProducerMock;

    @InjectMocks
    private ProductUpsertEventPublisherImpl sut;

    @Test
    void publish_sendsRecordToMainTopic() {
        final String mainTopicName = "product-upsert";
        final String kafkaKey = "Event-tenant-product-1001";
        final ProductUpsertedEvent upsertedEvent = new ProductUpsertedEvent(
                "product-1001",
                "Precision Screwdriver",
                "PS-1001",
                "ELECTRONICS",
                "PIECE",
                19.95,
                "High precision screwdriver",
                LocalDateTime.of(2025, 1, 1, 12, 0),
                ProductAction.CREATED,
                "Event-tenant"
        );
        Mockito.when(topicConfigurationMock.getProductUpsert()).thenReturn(mainTopicName);

        sut.publish(kafkaKey, upsertedEvent);

        final ArgumentCaptor<ProducerRecord<String, ProductUpsertedEvent>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        final ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        Mockito.verify(upsertProducerMock).send(recordCaptor.capture(), callbackCaptor.capture());
        final ProducerRecord<String, ProductUpsertedEvent> actualRecord = recordCaptor.getValue();
        final ProducerRecord<String, ProductUpsertedEvent> expectedRecord = new ProducerRecord<>(mainTopicName, kafkaKey, upsertedEvent);
        Assertions.assertThat(actualRecord).usingRecursiveComparison().isEqualTo(expectedRecord);
    }

    @Test
    void publish_whenRetryable_thenToRetry_thenRetryFails_sendsFail() {
        final String mainTopicName = "product-upsert";
        final String failTopicName = "product-upsert-fail";
        final String retryTopicName = "product-upsert-retry";
        final String kafkaKey = "Event-tenant-product-2002";
        final ProductUpsertedEvent upsertedEvent = new ProductUpsertedEvent(
                "product-2002",
                "Impact Drill",
                "ID-2002",
                "HOUSEHOLD",
                "PIECE",
                79.0,
                "Impact drill with 750W motor",
                LocalDateTime.of(2025, 2, 2, 9, 30),
                ProductAction.UPDATED,
                "Event-tenant"
        );
        Mockito.when(topicConfigurationMock.getProductUpsert()).thenReturn(mainTopicName);
        Mockito.when(topicConfigurationMock.getProductUpsertFail()).thenReturn(failTopicName);
        Mockito.when(topicConfigurationMock.getProductUpsertRetryFail()).thenReturn(retryTopicName);

        Mockito.doThrow(new TimeoutException("retryable"))
                .when(upsertProducerMock)
                .send(Mockito.argThat(r -> mainTopicName.equals(r.topic())
                                && kafkaKey.equals(r.key())
                                && upsertedEvent.equals(r.value())),
                        Mockito.argThat(Objects::nonNull));

        Mockito.doThrow(new RuntimeException("retry failed"))
                .when(upsertProducerMock)
                .send(Mockito.argThat(r -> retryTopicName.equals(r.topic())
                                && kafkaKey.equals(r.key())
                                && upsertedEvent.equals(r.value())),
                        Mockito.argThat(Objects::nonNull));

        sut.publish(kafkaKey, upsertedEvent);

        final ArgumentCaptor<ProducerRecord<String, ProductUpsertedEvent>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        final ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        Mockito.verify(upsertProducerMock, Mockito.times(3)).send(recordCaptor.capture(), callbackCaptor.capture());
        final var allSentRecords = recordCaptor.getAllValues();
        final ProducerRecord<String, ProductUpsertedEvent> expectedMainRecord = new ProducerRecord<>(mainTopicName, kafkaKey, upsertedEvent);
        final ProducerRecord<String, ProductUpsertedEvent> expectedRetryRecord = new ProducerRecord<>(retryTopicName, kafkaKey, upsertedEvent);
        final ProducerRecord<String, ProductUpsertedEvent> expectedFailRecord = new ProducerRecord<>(failTopicName, kafkaKey, upsertedEvent);
        Assertions.assertThat(allSentRecords.get(0)).usingRecursiveComparison().isEqualTo(expectedMainRecord);
        Assertions.assertThat(allSentRecords.get(1)).usingRecursiveComparison().isEqualTo(expectedRetryRecord);
        Assertions.assertThat(allSentRecords.get(2)).usingRecursiveComparison().isEqualTo(expectedFailRecord);
    }

    @Test
    void publishFailed_sendsRecordToFailTopic() {
        final String failTopicName = "product-upsert-fail";
        final String kafkaKey = "Event-tenant-product-3003";
        final String failReason = "validation error";
        final ProductUpsertedEvent upsertedEvent = new ProductUpsertedEvent(
                "product-3003",
                "Laser Level",
                "LL-3003",
                "HOUSEHOLD",
                "PIECE",
                59.9,
                "Cross line laser level",
                LocalDateTime.of(2025, 3, 3, 8, 0),
                ProductAction.CREATED,
                "Event-tenant"
        );
        Mockito.when(topicConfigurationMock.getProductUpsertFail()).thenReturn(failTopicName);

        sut.publishFailed(kafkaKey, upsertedEvent, failReason);

        final ArgumentCaptor<ProducerRecord<String, ProductUpsertedEvent>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        final ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        Mockito.verify(upsertProducerMock).send(recordCaptor.capture(), callbackCaptor.capture());
        final ProducerRecord<String, ProductUpsertedEvent> actualRecord = recordCaptor.getValue();
        final ProducerRecord<String, ProductUpsertedEvent> expectedRecord = new ProducerRecord<>(failTopicName, kafkaKey, upsertedEvent);
        Assertions.assertThat(actualRecord).usingRecursiveComparison().isEqualTo(expectedRecord);
    }
}