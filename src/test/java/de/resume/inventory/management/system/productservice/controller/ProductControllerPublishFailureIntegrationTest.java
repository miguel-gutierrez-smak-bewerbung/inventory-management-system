package de.resume.inventory.management.system.productservice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.resume.inventory.management.system.productservice.config.TestContainerConfiguration;
import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestContainerConfiguration.class, ProductControllerPublishFailureIntegrationTest.FailureKafkaProducerConfig.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductControllerPublishFailureIntegrationTest {

    private static final String EXPECTED_TOPIC_UPSERT = "product-upsert-test";
    private static final String EXPECTED_TOPIC_FAIL = "product-upsert-fail-test";
    private static final String EXPECTED_TOPIC_RETRY_FAIL = "product-upsert-retry-fail-test";

    @DynamicPropertySource
    static void registerKafkaTopics(final DynamicPropertyRegistry registry) {
        registry.add("product.kafka.topic.upsert", () -> EXPECTED_TOPIC_UPSERT);
        registry.add("product.kafka.topic.delete", () -> "product-delete-test");
        registry.add("product.kafka.topic.upsert.failed", () -> EXPECTED_TOPIC_FAIL);
        registry.add("product.kafka.topic.delete.failed", () -> "product-delete-fail-test");
        registry.add("product.kafka.topic.upsert.retry-fail", () -> EXPECTED_TOPIC_RETRY_FAIL);
    }

    @Autowired 
    private MockMvc mockMvc;
    
    @Autowired 
    private KafkaProducer<String, ProductUpsertedEvent> upsertKafkaProducer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void resetMocks() {
        reset(upsertKafkaProducer);
    }

    @Test
    void upsert_nonRetryableException_shouldSendToFailTopic() throws Exception {
        doThrow(new KafkaException("non-retryable")).when(upsertKafkaProducer).send(any(ProducerRecord.class));

        final String body = uniqueCreateJson(readResource("/json/product-create.json"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        final ArgumentCaptor<ProducerRecord<String, ProductUpsertedEvent>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(upsertKafkaProducer, times(3)).send(captor.capture());
        verify(upsertKafkaProducer, times(3)).send(any(ProducerRecord.class));

        assertThat(captor.getAllValues()).isNotEmpty();
    }

    @Test
    void upsert_retryable_then_retryFails_shouldSendToRetryThenFailTopic() throws Exception {
        doThrow(new TimeoutException("retryable")).doThrow(new KafkaException("retry failed"))
                .when(upsertKafkaProducer).send(any(ProducerRecord.class));

        final String body = uniqueCreateJson(readResource("/json/product-create.json"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(upsertKafkaProducer, times(3)).send(any(ProducerRecord.class));
    }

    @TestConfiguration
    static class FailureKafkaProducerConfig {

        @Bean
        @Primary
        KafkaProducer<String, ProductUpsertedEvent> kafkaProducer() {
            return Mockito.mock(KafkaProducer.class);
        }

        @Bean
        @Primary
        KafkaProducer<String, ProductDeletedEvent> kafkaDeletedProducer() {
            return Mockito.mock(KafkaProducer.class);
        }
    }

    private String readResource(final String path) throws Exception {
        try (final var is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new IllegalArgumentException("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String uniqueCreateJson(final String baseJson) throws Exception {
        final Map<String, Object> map = objectMapper.readValue(baseJson, new TypeReference<>() {});
        final String suffix = UUID.randomUUID().toString().substring(0, 6);
        final String baseName = String.valueOf(map.getOrDefault("name", "IntegrationTest Product"));
        final String newName = trimToMax(baseName + " " + suffix, 30);
        map.put("name", newName);
        map.put("articleNumber", trimToMax("Art-" + suffix, 20));
        return objectMapper.writeValueAsString(map);
    }

    private String trimToMax(final String value, final int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}