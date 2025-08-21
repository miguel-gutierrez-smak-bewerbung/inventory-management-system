package de.resume.inventory.management.system.productservice.config;

import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.retry.annotation.EnableRetry;

import java.util.HashMap;
import java.util.Map;

@EnableRetry
@Configuration
public class KafkaConfiguration {

    @Bean(destroyMethod = "close")
    public KafkaProducer<String, ProductUpsertedEvent> productUpsertedEventProducer(final KafkaProperties kafkaProperties) {
        final Map<String, Object> producerProperties = new HashMap<>(kafkaProperties.buildProducerProperties());
        configureProducerProperties(producerProperties);
        return new KafkaProducer<>(producerProperties);
    }

    @Bean(destroyMethod = "close")
    public KafkaProducer<String, ProductDeletedEvent> productDeletedEventProducer(final KafkaProperties kafkaProperties) {
        final Map<String, Object> producerProperties = new HashMap<>(kafkaProperties.buildProducerProperties());
        configureProducerProperties(producerProperties);
        return new KafkaProducer<>(producerProperties);
    }

    private void configureProducerProperties(final Map<String, Object> producerProperties) {
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
        producerProperties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProperties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        producerProperties.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
    }

    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory(final KafkaProperties kafkaProperties) {
        final Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean(name = "stringKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerContainerFactory(
            final ConsumerFactory<String, String> stringConsumerFactory) {
        final ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.getContainerProperties().setObservationEnabled(false);
        return factory;
    }

}
