package de.resume.inventory.management.system.productservice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.resume.inventory.management.system.productservice.config.TestContainerConfiguration;
import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
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

    @DynamicPropertySource
    static void registerKafkaTopics(final DynamicPropertyRegistry registry) {
        registry.add("topics.productUpsert", () -> "product-upsert-test");
        registry.add("topics.productDelete", () -> "product-delete-test");
        registry.add("topics.productUpsertFail", () -> "product-upsert-fail-test");
        registry.add("topics.productDeleteFail", () -> "product-delete-fail-test");
        registry.add("topics.productUpsertRetryFail", () -> "product-upsert-retry-fail-test");
        registry.add("topics.productDeleteRetryFail", () -> "product-delete-retry-fail-test");
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
        final String mainTopic = "product-upsert-test";

        Mockito.doThrow(new org.apache.kafka.common.KafkaException("non-retryable"))
                .when(upsertKafkaProducer)
                .send(Mockito.argThat((ProducerRecord<String, ProductUpsertedEvent> r) -> mainTopic.equals(r.topic())),
                        any(Callback.class));

        final String body = uniqueCreateJson(readResource());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        final ArgumentCaptor<ProducerRecord<String, ProductUpsertedEvent>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(upsertKafkaProducer, times(2)).send(captor.capture(), any(Callback.class));

        assertThat(captor.getAllValues()).hasSize(2);
    }

    @Test
    void upsert_retryable_then_retryFails_shouldSendToRetryThenFailTopic() throws Exception {
        Mockito.doThrow(new TimeoutException("retryable"))
                .doThrow(new org.apache.kafka.common.KafkaException("retry failed"))
                .when(upsertKafkaProducer)
                .send(any(ProducerRecord.class), any(Callback.class));

        final String body = uniqueCreateJson(readResource());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(upsertKafkaProducer, times(3)).send(any(ProducerRecord.class), any(Callback.class));
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

    private String readResource() throws Exception {
        try (final var is = getClass().getResourceAsStream("/json/product-create.json")) {
            if (is == null) throw new IllegalArgumentException("Resource not found: " + "/json/product-create.json");
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