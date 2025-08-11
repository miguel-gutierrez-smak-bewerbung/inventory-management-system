package de.resume.inventory.management.system.productservice.config;

import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class KafkaProducerTestConfig {


    @Bean
    public MockProducer<String, ProductUpsertedEvent> mockUpsertProducer() {
        return new MockProducer<>(true, new StringSerializer(),
                new org.springframework.kafka.support.serializer.JsonSerializer<>());
    }

    @Bean
    public MockProducer<String, ProductDeletedEvent> mockDeleteProducer() {
        return new MockProducer<>(true, new StringSerializer(),
                new org.springframework.kafka.support.serializer.JsonSerializer<>());
    }

    @Bean
    public org.apache.kafka.clients.producer.Producer<String, ProductUpsertedEvent> upsertProducer(
            MockProducer<String, ProductUpsertedEvent> delegate) {
        return delegate;
    }

    @Bean
    public org.apache.kafka.clients.producer.Producer<String, ProductDeletedEvent> deleteProducer(
            MockProducer<String, ProductDeletedEvent> delegate) {
        return delegate;
    }
}