package de.resume.inventory.management.system.productservice.services.publisher;

import de.resume.inventory.management.system.productservice.config.TopicConfiguration;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;
import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
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
class ProductDeleteEventPublisherTest {

    @Mock
    private TopicConfiguration topicConfigurationMock;

    @Mock
    private KafkaProducer<String, ProductDeletedEvent> deleteProducerMock;

    @InjectMocks
    private ProductDeleteEventPublisherImpl sut;

    @Test
    void publish_sendsRecordToMainTopic() {
        final String mainTopicName = "product-delete";
        final String kafkaKey = "Event-tenant-product-4004";
        final ProductDeletedEvent deletedEvent = new ProductDeletedEvent(
                "product-4004",
                LocalDateTime.of(2025, 4, 4, 10, 15),
                ProductAction.DELETED,
                "Event-tenant"
        );
        Mockito.when(topicConfigurationMock.getProductDelete()).thenReturn(mainTopicName);

        sut.publish(kafkaKey, deletedEvent);

        final ArgumentCaptor<ProducerRecord<String, ProductDeletedEvent>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        final ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        Mockito.verify(deleteProducerMock).send(recordCaptor.capture(), callbackCaptor.capture());
        final ProducerRecord<String, ProductDeletedEvent> actualRecord = recordCaptor.getValue();
        final ProducerRecord<String, ProductDeletedEvent> expectedRecord = new ProducerRecord<>(mainTopicName, kafkaKey, deletedEvent);
        Assertions.assertThat(actualRecord).usingRecursiveComparison().isEqualTo(expectedRecord);
    }

    @Test
    void publish_whenRetryable_thenToRetry_thenRetryFails_sendsFail() {
        final String mainTopicName = "product-delete";
        final String failTopicName = "product-delete-fail";
        final String retryTopicName = "product-delete-retry";
        final String kafkaKey = "Event-tenant-product-5005";
        final ProductDeletedEvent deletedEvent = new ProductDeletedEvent(
                "product-5005",
                LocalDateTime.of(2025, 5, 5, 11, 45),
                ProductAction.DELETED,
                "Event-tenant"
        );
        Mockito.when(topicConfigurationMock.getProductDelete()).thenReturn(mainTopicName);
        Mockito.when(topicConfigurationMock.getProductDeleteFail()).thenReturn(failTopicName);
        Mockito.when(topicConfigurationMock.getProductDeleteRetryFail()).thenReturn(retryTopicName);

        Mockito.doThrow(new TimeoutException("retryable"))
                .when(deleteProducerMock)
                .send(Mockito.argThat(producerRecord -> mainTopicName.equals(producerRecord.topic())
                                && kafkaKey.equals(producerRecord.key())
                                && deletedEvent.equals(producerRecord.value())),
                        Mockito.argThat(Objects::nonNull));

        Mockito.doThrow(new RuntimeException("retry failed"))
                .when(deleteProducerMock)
                .send(Mockito.argThat(producerRecord -> retryTopicName.equals(producerRecord.topic())
                                && kafkaKey.equals(producerRecord.key())
                                && deletedEvent.equals(producerRecord.value())),
                        Mockito.argThat(Objects::nonNull));

        sut.publish(kafkaKey, deletedEvent);

        final ArgumentCaptor<ProducerRecord<String, ProductDeletedEvent>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        final ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        Mockito.verify(deleteProducerMock, Mockito.times(3)).send(recordCaptor.capture(), callbackCaptor.capture());
        final var allSentRecords = recordCaptor.getAllValues();
        final ProducerRecord<String, ProductDeletedEvent> expectedMainRecord = new ProducerRecord<>(mainTopicName, kafkaKey, deletedEvent);
        final ProducerRecord<String, ProductDeletedEvent> expectedRetryRecord = new ProducerRecord<>(retryTopicName, kafkaKey, deletedEvent);
        final ProducerRecord<String, ProductDeletedEvent> expectedFailRecord = new ProducerRecord<>(failTopicName, kafkaKey, deletedEvent);
        Assertions.assertThat(allSentRecords.get(0)).usingRecursiveComparison().isEqualTo(expectedMainRecord);
        Assertions.assertThat(allSentRecords.get(1)).usingRecursiveComparison().isEqualTo(expectedRetryRecord);
        Assertions.assertThat(allSentRecords.get(2)).usingRecursiveComparison().isEqualTo(expectedFailRecord);
    }

    @Test
    void publishFailed_sendsRecordToFailTopic() {
        final String failTopicName = "product-delete-fail";
        final String kafkaKey = "Event-tenant-product-6006";
        final String failReason = "dependency timeout";
        final ProductDeletedEvent deletedEvent = new ProductDeletedEvent(
                "product-6006",
                LocalDateTime.of(2025, 6, 6, 16, 0),
                ProductAction.DELETED,
                "Event-tenant"
        );
        Mockito.when(topicConfigurationMock.getProductDeleteFail()).thenReturn(failTopicName);

        sut.publishFailed(kafkaKey, deletedEvent, failReason);

        final ArgumentCaptor<ProducerRecord<String, ProductDeletedEvent>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        final ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        Mockito.verify(deleteProducerMock).send(recordCaptor.capture(), callbackCaptor.capture());
        final ProducerRecord<String, ProductDeletedEvent> actualRecord = recordCaptor.getValue();
        final ProducerRecord<String, ProductDeletedEvent> expectedRecord = new ProducerRecord<>(failTopicName, kafkaKey, deletedEvent);
        Assertions.assertThat(actualRecord).usingRecursiveComparison().isEqualTo(expectedRecord);
    }
}