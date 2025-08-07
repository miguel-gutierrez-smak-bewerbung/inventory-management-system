package de.resume.inventory.management.system.productservice.config;

import de.resume.inventory.management.system.productservice.models.messages.ProductUpsertedEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfiguration {

    @Bean
    public KafkaProducer<String, ProductUpsertedEvent> kafkaErrorProducer(final KafkaProperties kafkaProperties) {
        final Map<String, Object> producerProperties = new HashMap<>(kafkaProperties.buildProducerProperties());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
        producerProperties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProperties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        producerProperties.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        return new KafkaProducer<>(producerProperties);

    }
}
