package de.resume.inventory.management.system.productservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.resume.inventory.management.system.productservice.config.KafkaConfiguration;
import de.resume.inventory.management.system.productservice.config.TestContainerConfiguration;
import de.resume.inventory.management.system.productservice.config.TestEventPublisherConfig;
import de.resume.inventory.management.system.productservice.config.TestEventPublisherConfig.RecordingProductEventPublisher;
import de.resume.inventory.management.system.productservice.config.TestEventPublisherConfig.SentDelete;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;
import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
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
@Import({KafkaConfiguration.class, TestEventPublisherConfig.class, TestContainerConfiguration.class})
class RetryDeleteKafkaListenerIntegrationTest {

    @Autowired
    private RetryDeleteKafkaListener sut;

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
        final String kafkaKey = "tenant-3003-product-3003";
        final ProductDeletedEvent event = new ProductDeletedEvent(
                "product-3003",
                LocalDateTime.of(2025, 3, 3, 12, 0),
                ProductAction.DELETED,
                "tenant-3003"
        );
        final String payload = objectMapper.writeValueAsString(event);
        final ConsumerRecord<String, String> record = new ConsumerRecord<>("product-delete-retry", 0, 0L, kafkaKey, payload);
        final Acknowledgment acknowledgment = Mockito.mock(Acknowledgment.class);

        sut.retry(record, acknowledgment);

        Mockito.verify(acknowledgment).acknowledge();

        Assertions.assertThat(recordingPublisher.deleteHistory()).hasSize(1);
        final SentDelete sent = recordingPublisher.deleteHistory().getFirst();
        Assertions.assertThat(sent.kafkaKey()).isEqualTo(kafkaKey);
        Assertions.assertThat(sent.event()).isEqualTo(event);
    }

    @Test
    void recover_onExhaustedRetries_routesToFail_andAcknowledges() throws Exception {
        final String kafkaKey = "tenant-4004-product-4004";
        final ProductDeletedEvent failed = new ProductDeletedEvent(
                "product-4004",
                LocalDateTime.of(2025, 4, 4, 12, 0),
                ProductAction.DELETED,
                "tenant-4004"
        );
        final String payload = objectMapper.writeValueAsString(failed);
        final ConsumerRecord<String, String> record = new ConsumerRecord<>("product-delete-retry", 0, 0L, kafkaKey, payload);
        final Acknowledgment acknowledgment = Mockito.mock(Acknowledgment.class);

        sut.recover(new RuntimeException("exhausted"), record, acknowledgment);

        Mockito.verify(acknowledgment).acknowledge();

        Assertions.assertThat(recordingPublisher.deleteHistory()).hasSize(1);
        final SentDelete sent = recordingPublisher.deleteHistory().getFirst();
        Assertions.assertThat(sent.kafkaKey()).isEqualTo(kafkaKey);
        Assertions.assertThat(sent.event()).isEqualTo(failed);
    }
}
