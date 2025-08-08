package de.resume.inventory.management.system.productservice.services.publisher;

import static org.mockito.Mockito.mock;

import de.resume.inventory.management.system.productservice.config.TopicConfiguration;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;
import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Future;

@ExtendWith(MockitoExtension.class)
class ProductEventPublisherImplTest {

    private static final String TOPIC = "product-upsert";
    private static final String RETRY_TOPIC = "product-upsert-retry";
    private static final String FAIL_TOPIC = "product-upsert-fail";

    @InjectMocks
    private ProductEventPublisherImpl sut;

    @Mock
    private KafkaProducer<String, ProductUpsertedEvent> kafkaProducer;

    @Mock
    private TopicConfiguration topicConfiguration;


    @Test
    void publishProductUpserted_sendsMessageViaKafkaProducer() {

        Mockito.when(topicConfiguration.getProductUpsert()).thenReturn(TOPIC);

        final String kafkaKey = "product-1";
        final ProductUpsertedEvent productUpsertedEvent = new ProductUpsertedEvent(
                "product-1", "name", "100", "category", "unit", 1.99,
                "description", LocalDateTime.now(), ProductAction.CREATED
        );

        sut.publishProductUpserted(kafkaKey, productUpsertedEvent);

        final ArgumentCaptor<ProducerRecord<String, ProductUpsertedEvent>> captor = ArgumentCaptor.forClass(ProducerRecord.class);

        Mockito.verify(kafkaProducer).send(captor.capture());
        final ProducerRecord<String, ProductUpsertedEvent> record = captor.getValue();

        Assertions.assertEquals(TOPIC, record.topic());
        Assertions.assertEquals(productUpsertedEvent, record.value());
    }

    @Test
    void publishProductUpserted_withRetryableException_sendsToRetryTopic() {

        final String kafkaKey = "product-2";
        final ProductUpsertedEvent event = new ProductUpsertedEvent(
                "product-2", "name2", "200", "category", "unit",
                2.99, "description", LocalDateTime.now(), ProductAction.CREATED
        );
        final ArgumentCaptor<ProducerRecord<String, ProductUpsertedEvent>> captor = ArgumentCaptor.forClass(ProducerRecord.class);

        final ProducerRecord<String, ProductUpsertedEvent> retryRecord =
                new ProducerRecord<>(RETRY_TOPIC, kafkaKey, event);
        final ProducerRecord<String, ProductUpsertedEvent> failRecord =
                new ProducerRecord<>(FAIL_TOPIC, kafkaKey, event);

        Mockito.when(kafkaProducer.send(Mockito.eq(retryRecord)))
               .thenThrow(new RuntimeException("Retryable"));

        Mockito.when(kafkaProducer.send(Mockito.eq(failRecord)))
               .thenReturn(mock(Future.class));

        Mockito.when(topicConfiguration.getProductUpsert()).thenReturn(TOPIC);
        Mockito.when(topicConfiguration.getProductUpsertRetryFail()).thenReturn(RETRY_TOPIC);
        Mockito.when(topicConfiguration.getProductUpsertFail()).thenReturn(FAIL_TOPIC);

        Mockito.doThrow(new KafkaException("Retryable")).when(kafkaProducer).send(new ProducerRecord<>(RETRY_TOPIC, kafkaKey, event));

        sut.publishProductUpserted(kafkaKey, event);

        Mockito.verify(kafkaProducer, Mockito.atLeast(2)).send(captor.capture());

        final List<ProducerRecord<String, ProductUpsertedEvent>> records = captor.getAllValues();

        Assertions.assertTrue(records.size() >= 2, "Expected at least two invocations of send()");

        Assertions.assertEquals(RETRY_TOPIC, retryRecord.topic());
        Assertions.assertEquals(event, retryRecord.value());
    }

    @Test
    void publishProductUpserted_withNonRetryableException_sendsToFailTopic() {

        final String kafkaKey = "product-3";
        final ProductUpsertedEvent event = new ProductUpsertedEvent(
                "product-3", "name3", "300", "category",
                "unit", 3.99, "description", LocalDateTime.now(), ProductAction.CREATED
        );
        final ArgumentCaptor<ProducerRecord<String, ProductUpsertedEvent>> captor = ArgumentCaptor.forClass(ProducerRecord.class);

        Mockito.when(topicConfiguration.getProductUpsert()).thenReturn(TOPIC);
        Mockito.when(topicConfiguration.getProductUpsertFail()).thenReturn(FAIL_TOPIC);

        Mockito.doThrow(new RuntimeException("Non-retryable")).when(kafkaProducer).send(
            Mockito.argThat(record ->
                record.topic().equals(FAIL_TOPIC)
                && record.key().equals(kafkaKey)
                && record.value().equals(event)
            )
        );

        sut.publishProductUpserted(kafkaKey, event);

        Mockito.verify(kafkaProducer, Mockito.atLeast(2)).send(captor.capture());

        final List<ProducerRecord<String, ProductUpsertedEvent>> records = captor.getAllValues();
        final ProducerRecord<String, ProductUpsertedEvent> failRecord = records.getLast();

        Assertions.assertEquals(FAIL_TOPIC, failRecord.topic());
        Assertions.assertEquals(event, failRecord.value());
    }

    @Test
    void publishProductUpsertFailed_sendsMessageToFailTopic() {

        final String kafkaKey = "product-4";
        final ProductUpsertedEvent event = new ProductUpsertedEvent(
                "product-4", "name4", "400", "category",
                "unit", 4.99, "description", LocalDateTime.now(),
                ProductAction.CREATED
        );

        Mockito.when(topicConfiguration.getProductUpsertFail()).thenReturn(FAIL_TOPIC);

        sut.publishProductUpsertFailed(kafkaKey, event, "failure-reason");

        final ArgumentCaptor<ProducerRecord<String, ProductUpsertedEvent>> captor = ArgumentCaptor.forClass(ProducerRecord.class);

        Mockito.verify(kafkaProducer, Mockito.times(1)).send(captor.capture());

        final ProducerRecord<String, ProductUpsertedEvent> record = captor.getValue();

        Assertions.assertEquals(FAIL_TOPIC, record.topic());
        Assertions.assertEquals(event, record.value());
    }

    @Test
    void publishProductDeleted_sendsMessageViaKafkaProducer() {
        final String deleteTopic = "product-delete";
        final String kafkaKey = "product-5";

        final ProductDeletedEvent productDeletedEvent =
                new ProductDeletedEvent("product-5", LocalDateTime.of(2025, 8, 6, 14, 20), ProductAction.DELETED);

        @SuppressWarnings("unchecked")
        KafkaProducer<String, ProductDeletedEvent> deletedProducerMock =
                mock(KafkaProducer.class);
        sut = new ProductEventPublisherImpl(topicConfiguration, kafkaProducer, deletedProducerMock);

        Mockito.when(topicConfiguration.getProductDelete()).thenReturn(deleteTopic);

        sut.publishProductDeleted(kafkaKey, productDeletedEvent);

        ArgumentCaptor<ProducerRecord<String, ProductDeletedEvent>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        Mockito.verify(deletedProducerMock).send(captor.capture());

        ProducerRecord<String, ProductDeletedEvent> record = captor.getValue();
        Assertions.assertEquals(deleteTopic, record.topic());
        Assertions.assertEquals(kafkaKey, record.key());
        Assertions.assertEquals(productDeletedEvent, record.value());
    }
}
