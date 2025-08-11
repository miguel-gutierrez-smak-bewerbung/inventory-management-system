package de.resume.inventory.management.system.productservice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.resume.inventory.management.system.productservice.config.TestContainerConfiguration;
import de.resume.inventory.management.system.productservice.config.TestEventPublisherConfig;
import de.resume.inventory.management.system.productservice.config.TestEventPublisherConfig.RecordingProductEventPublisher;
import de.resume.inventory.management.system.productservice.config.TestEventPublisherConfig.RecordingProductEventPublisher.SentDelete;
import de.resume.inventory.management.system.productservice.config.TestEventPublisherConfig.RecordingProductEventPublisher.SentUpsert;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestContainerConfiguration.class, TestEventPublisherConfig.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductControllerIntegrationTest {

    @DynamicPropertySource
    static void registerKafkaTopics(final DynamicPropertyRegistry registry) {
        registry.add("product.kafka.topic.upsert", () -> "product-upsert-test");
        registry.add("product.kafka.topic.delete", () -> "product-delete-test");
        registry.add("product.kafka.topic.upsert.failed", () -> "product-upsert-fail-test");
        registry.add("product.kafka.topic.delete.failed", () -> "product-delete-fail-test");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingProductEventPublisher recordingPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        recordingPublisher.clearAll();
    }

    @Test
    void createProduct_shouldPersistAndSendEvent() throws Exception {
        final String body = uniqueCreateJson(readResource("/json/product-create.json"));

        final String json = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        final String id = extractId(json);
        assertThat(id).isNotBlank();

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(recordingPublisher.upsertHistory()).hasSize(1));

        final SentUpsert sent = recordingPublisher.upsertHistory().get(0);
        final ProductUpsertedEvent event = sent.event();

        assertThat(sent.kafkaKey()).isNotBlank();
        assertThat(event.name()).contains("IntegrationTest Product");
        assertThat(event.articleNumber()).startsWith("Art-");
    }

    @Test
    void updateProduct_shouldPersistAndSendEvent() throws Exception {
        final String createBody = uniqueCreateJson(readResource("/json/product-create.json"));
        final String createdJson = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        final String id = extractId(createdJson);
        assertThat(id).isNotBlank();

        recordingPublisher.clearAll();

        String updateBody = readResource("/json/product-update.json");
        updateBody = updateBody.replace("${ID}", id);
        updateBody = uniqueUpdateJson(updateBody);

        final String updatedJson = mockMvc.perform(put("/api/products/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        final String updatedId = extractId(updatedJson);
        assertThat(updatedId).isEqualTo(id);

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(recordingPublisher.upsertHistory()).hasSize(1));

        final SentUpsert sent = recordingPublisher.upsertHistory().get(0);
        assertThat(sent.kafkaKey()).isNotBlank();
        assertThat(sent.event().id()).isEqualTo(id);
    }

    @Test
    void deleteProduct_shouldDeleteAndSendEvent() throws Exception {
        final String createBody = uniqueCreateJson(readResource("/json/product-create.json"));
        final String createdJson = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        final String id = extractId(createdJson);
        assertThat(id).isNotBlank();

        recordingPublisher.clearAll();

        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isNoContent());

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(recordingPublisher.deleteHistory()).hasSize(1));

        final SentDelete sent = recordingPublisher.deleteHistory().get(0);
        assertThat(sent.kafkaKey()).contains(id);
        assertThat(sent.event().productAction().name()).isEqualTo("DELETED");
    }

    @Test
    void createProduct_invalidEnum_shouldReturn400_noEvent() throws Exception {
        final String body = readResource("/json/product-create-invalid-enum.json");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(recordingPublisher.upsertHistory()).isEmpty();
        assertThat(recordingPublisher.upsertFailedHistory()).isEmpty();
        assertThat(recordingPublisher.deleteHistory()).isEmpty();
    }

    @Test
    void updateProduct_missingId_shouldReturn400_noEvent() throws Exception {
        final String body = readResource("/json/product-update-missing-id.json");

        mockMvc.perform(put("/api/products/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(recordingPublisher.upsertHistory()).isEmpty();
        assertThat(recordingPublisher.upsertFailedHistory()).isEmpty();
        assertThat(recordingPublisher.deleteHistory()).isEmpty();
    }

    @Test
    void deleteProduct_unknownId_shouldReturn404_noEvent() throws Exception {
        final String unknownId = "11111111-1111-1111-1111-111111111111";

        mockMvc.perform(delete("/api/products/{id}", unknownId))
                .andExpect(status().isNotFound());

        assertThat(recordingPublisher.deleteHistory()).isEmpty();
    }

    @Test
    void createProduct_malformedJson_shouldReturn400_noEvent() throws Exception {
        final String malformed = "{ \"name\": \"X\", ";

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformed))
                .andExpect(status().isBadRequest());

        assertThat(recordingPublisher.upsertHistory()).isEmpty();
    }

    @Test
    void getById_unknown_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/products/{id}", "22222222-2222-2222-2222-222222222222"))
                .andExpect(status().isNotFound());
    }

    private String readResource(final String path) throws Exception {
        try (final var is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String extractId(final String json) throws Exception {
        final var map = objectMapper.readValue(json, Map.class);
        final Object id = map.get("id");
        return id == null ? "" : String.valueOf(id);
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

    private String uniqueUpdateJson(final String baseJson) throws Exception {
        final Map<String, Object> map = objectMapper.readValue(baseJson, new TypeReference<>() {});
        final String suffix = UUID.randomUUID().toString().substring(0, 6);
        final String baseName = String.valueOf(map.getOrDefault("name", "IntegrationTest Product Updated"));
        final String newName = trimToMax(baseName + " " + suffix, 30);
        map.put("name", newName);
        return objectMapper.writeValueAsString(map);
    }

    private String trimToMax(final String value, final int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}