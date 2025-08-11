package de.resume.inventory.management.system.productservice.config;

import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import de.resume.inventory.management.system.productservice.services.publisher.ProductEventPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@TestConfiguration
public class TestEventPublisherConfig {

    @Bean
    @Primary
    public RecordingProductEventPublisher recordingProductEventPublisher() {
        return new RecordingProductEventPublisher();
    }

    public static class RecordingProductEventPublisher implements ProductEventPublisher {

        public record SentUpsert(String kafkaKey, ProductUpsertedEvent event) {}
        public record SentUpsertFailed(String kafkaKey, ProductUpsertedEvent event, String reason) {}
        public record SentDelete(String kafkaKey, ProductDeletedEvent event) {}

        private final List<SentUpsert> upserts = new CopyOnWriteArrayList<>();
        private final List<SentUpsertFailed> upsertFailed = new CopyOnWriteArrayList<>();
        private final List<SentDelete> deletes = new CopyOnWriteArrayList<>();

        @Override
        public void publishProductUpserted(final String kafkaKey, final ProductUpsertedEvent productUpsertedEvent) {
            upserts.add(new SentUpsert(kafkaKey, productUpsertedEvent));
        }

        @Override
        public void publishProductUpsertFailed(final String kafkaKey, final ProductUpsertedEvent failedMessage, final String reason) {
            upsertFailed.add(new SentUpsertFailed(kafkaKey, failedMessage, reason));
        }

        @Override
        public void publishProductDeleted(final String kafkaKey, final ProductDeletedEvent productDeletedEvent) {
            deletes.add(new SentDelete(kafkaKey, productDeletedEvent));
        }

        public List<SentUpsert> upsertHistory() { return upserts; }
        public List<SentUpsertFailed> upsertFailedHistory() { return upsertFailed; }
        public List<SentDelete> deleteHistory() { return deletes; }

        public void clearAll() {
            upserts.clear();
            upsertFailed.clear();
            deletes.clear();
        }
    }
}