package de.resume.inventory.management.system.productservice.config;

import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import de.resume.inventory.management.system.productservice.services.publisher.DomainEventPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@TestConfiguration
public class TestEventPublisherConfig {

    public static class SentUpsert {
        private final String kafkaKey;
        private final ProductUpsertedEvent event;

        public SentUpsert(final String kafkaKey, final ProductUpsertedEvent event) {
            this.kafkaKey = kafkaKey;
            this.event = event;
        }

        public String kafkaKey() { return kafkaKey; }
        public ProductUpsertedEvent event() { return event; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SentUpsert that)) return false;
            return java.util.Objects.equals(kafkaKey, that.kafkaKey) &&
                    java.util.Objects.equals(event, that.event);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(kafkaKey, event);
        }
    }

    public static class SentDelete {
        private final String kafkaKey;
        private final ProductDeletedEvent event;

        public SentDelete(final String kafkaKey, final ProductDeletedEvent event) {
            this.kafkaKey = kafkaKey;
            this.event = event;
        }

        public String kafkaKey() { return kafkaKey; }
        public ProductDeletedEvent event() { return event; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SentDelete that)) return false;
            return java.util.Objects.equals(kafkaKey, that.kafkaKey) &&
                    java.util.Objects.equals(event, that.event);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(kafkaKey, event);
        }
    }

    public static class UpsertRecordingPublisher implements DomainEventPublisher<ProductUpsertedEvent> {
        private final List<SentUpsert> upserts = new CopyOnWriteArrayList<>();

        @Override
        public void publish(final String kafkaKey, final ProductUpsertedEvent event) {
            upserts.add(new SentUpsert(kafkaKey, event));
        }

        @Override
        public void publishFailed(final String kafkaKey, final ProductUpsertedEvent failedMessage, final String reason) {
            upserts.add(new SentUpsert(kafkaKey, failedMessage));
        }

        public List<SentUpsert> upsertHistory() {
            return upserts;
        }

        public void clear() {
            upserts.clear();
        }
    }

    public static class DeleteRecordingPublisher implements DomainEventPublisher<ProductDeletedEvent> {
        private final List<SentDelete> deletes = new CopyOnWriteArrayList<>();

        @Override
        public void publish(final String kafkaKey, final ProductDeletedEvent event) {
            deletes.add(new SentDelete(kafkaKey, event));
        }

        @Override
        public void publishFailed(final String kafkaKey, final ProductDeletedEvent failedMessage, final String reason) {
            deletes.add(new SentDelete(kafkaKey, failedMessage));
        }

        public List<SentDelete> deleteHistory() {
            return deletes;
        }

        public void clear() {
            deletes.clear();
        }
    }

    public static class RecordingProductEventPublisher {
        private final UpsertRecordingPublisher upsertRecordingPublisher;
        private final DeleteRecordingPublisher deleteRecordingPublisher;

        public RecordingProductEventPublisher(final UpsertRecordingPublisher upsertRecordingPublisher,
                                              final DeleteRecordingPublisher deleteRecordingPublisher) {
            this.upsertRecordingPublisher = upsertRecordingPublisher;
            this.deleteRecordingPublisher = deleteRecordingPublisher;
        }

        public List<SentUpsert> upsertHistory() {
            return upsertRecordingPublisher.upsertHistory();
        }

        public List<SentDelete> deleteHistory() {
            return deleteRecordingPublisher.deleteHistory();
        }

        public void clearAll() {
            upsertRecordingPublisher.clear();
            deleteRecordingPublisher.clear();
        }
    }

    @Bean
    @Primary
    public DomainEventPublisher<ProductUpsertedEvent> upsertRecordingPublisherBean() {
        return new UpsertRecordingPublisher();
    }

    @Bean
    @Primary
    public DomainEventPublisher<ProductDeletedEvent> deleteRecordingPublisherBean() {
        return new DeleteRecordingPublisher();
    }

    @Bean
    @Primary
    public RecordingProductEventPublisher recordingProductEventPublisher(final DomainEventPublisher<ProductUpsertedEvent> upsertPublisher,
                                                                        final DomainEventPublisher<ProductDeletedEvent> deletePublisher) {
        final UpsertRecordingPublisher upsertRecordingPublisher = (UpsertRecordingPublisher) upsertPublisher;
        final DeleteRecordingPublisher deleteRecordingPublisher = (DeleteRecordingPublisher) deletePublisher;
        return new RecordingProductEventPublisher(upsertRecordingPublisher, deleteRecordingPublisher);
    }
}