package de.resume.inventory.management.system.productservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.resume.inventory.management.system.productservice.config.KafkaConfiguration;
import de.resume.inventory.management.system.productservice.config.TestContainerConfiguration;
import de.resume.inventory.management.system.productservice.config.TestEventPublisherConfig;
import de.resume.inventory.management.system.productservice.config.TestEventPublisherConfig.RecordingProductEventPublisher;
import de.resume.inventory.management.system.productservice.config.TestEventPublisherConfig.SentUpsert;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainerConfiguration.class, KafkaConfiguration.class, TestEventPublisherConfig.class})
class RetryUpsertKafkaListenerIntegrationTest {

    @Autowired
    private RetryUpsertKafkaListener sut;

    @Autowired
    private RecordingProductEventPublisher recordingPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        recordingPublisher.clearAll();
    }

    @Test
    void retry_withValidPayload_publishesAgain_andAcknowledges() throws Exception {
        final String kafkaKey = "tenant-1001-product-1001";
        final ProductUpsertedEvent event = new ProductUpsertedEvent(
                "product-1001",
                "Widget",
                "W-1001",
                "ELECTRONICS",
                "PIECE",
                12.50,
                "Test description",
                LocalDateTime.of(2025, 1, 1, 0, 0),
                ProductAction.CREATED,
                "tenant-1001"
        );
        final String payload = objectMapper.writeValueAsString(event);
        final ConsumerRecord<String, String> record = new ConsumerRecord<>("product-upsert-retry", 0, 0L, kafkaKey, payload);
        final Acknowledgment acknowledgment = Mockito.mock(Acknowledgment.class);

        sut.retry(record, acknowledgment);

        Mockito.verify(acknowledgment).acknowledge();

        Assertions.assertThat(recordingPublisher.upsertHistory()).hasSize(1);
        final SentUpsert sent = recordingPublisher.upsertHistory().getFirst();
        Assertions.assertThat(sent.kafkaKey()).isEqualTo(kafkaKey);
        Assertions.assertThat(sent.event()).isEqualTo(event);
    }

    @Test
    void recover_onExhaustedRetries_routesToFail_andAcknowledges() throws Exception {
        final String kafkaKey = "tenant-2002-product-2002";
        final ProductUpsertedEvent failed = new ProductUpsertedEvent(
                "product-2002",
                "Gadget",
                "G-2002",
                "HOUSEHOLD",
                "PIECE",
                29.90,
                "Will fail",
                LocalDateTime.of(2025, 2, 2, 0, 0),
                ProductAction.UPDATED,
                "tenant-2002"
        );
        final String payload = objectMapper.writeValueAsString(failed);
        final ConsumerRecord<String, String> record = new ConsumerRecord<>("product-upsert-retry", 0, 0L, kafkaKey, payload);
        final Acknowledgment acknowledgment = Mockito.mock(Acknowledgment.class);

        sut.recover(new RuntimeException("exhausted"), record, acknowledgment);

        Mockito.verify(acknowledgment).acknowledge();

        Assertions.assertThat(recordingPublisher.upsertHistory()).hasSize(1);
        final SentUpsert sent = recordingPublisher.upsertHistory().getFirst();
        Assertions.assertThat(sent.kafkaKey()).isEqualTo(kafkaKey);
        Assertions.assertThat(sent.event()).isEqualTo(failed);
    }
}
