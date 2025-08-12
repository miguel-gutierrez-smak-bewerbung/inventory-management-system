package de.resume.inventory.management.system.productservice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.resume.inventory.management.system.productservice.config.TestContainerConfiguration;
import de.resume.inventory.management.system.productservice.config.TestEventPublisherConfig;
import de.resume.inventory.management.system.productservice.config.TestEventPublisherConfig.RecordingProductEventPublisher;
import de.resume.inventory.management.system.productservice.config.TestEventPublisherConfig.SentDelete;
import de.resume.inventory.management.system.productservice.config.TestEventPublisherConfig.SentUpsert;
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
        registry.add("topics.productUpsert", () -> "product-upsert-test");
        registry.add("topics.productDelete", () -> "product-delete-test");
        registry.add("topics.productUpsertFail", () -> "product-upsert-fail-test");
        registry.add("topics.productDeleteFail", () -> "product-delete-fail-test");
        registry.add("topics.productUpsertRetryFail", () -> "product-upsert-retry-test");
        registry.add("topics.productDeleteRetryFail", () -> "product-delete-retry-test");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingProductEventPublisher recordingPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        recordingPublisher.clearAll();
    }

    @Test
    void createProduct_shouldPersistAndSendEvent() throws Exception {
        final String requestBody = uniqueCreateJson(readResource("/json/product-create.json"));

        final String responseJson = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        final String createdId = extractId(responseJson);
        assertThat(createdId).isNotBlank();

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(recordingPublisher.upsertHistory()).hasSize(1));

        final SentUpsert sentUpsert = recordingPublisher.upsertHistory().getFirst();
        final ProductUpsertedEvent publishedEvent = sentUpsert.event();

        assertThat(sentUpsert.kafkaKey()).isNotBlank();
        assertThat(publishedEvent.name()).contains("IntegrationTest Product");
        assertThat(publishedEvent.articleNumber()).startsWith("Art-");
    }

    @Test
    void updateProduct_shouldPersistAndSendEvent() throws Exception {
        final String createRequestBody = uniqueCreateJson(readResource("/json/product-create.json"));
        final String createdJson = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        final String createdId = extractId(createdJson);
        assertThat(createdId).isNotBlank();

        recordingPublisher.clearAll();

        String updateRequestBody = readResource("/json/product-update.json");
        updateRequestBody = updateRequestBody.replace("${ID}", createdId);
        updateRequestBody = uniqueUpdateJson(updateRequestBody);

        final String updatedJson = mockMvc.perform(put("/api/products/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        final String updatedId = extractId(updatedJson);
        assertThat(updatedId).isEqualTo(createdId);

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(recordingPublisher.upsertHistory()).hasSize(1));

        final SentUpsert sentUpsert = recordingPublisher.upsertHistory().getFirst();
        assertThat(sentUpsert.kafkaKey()).isNotBlank();
        assertThat(sentUpsert.event().id()).isEqualTo(createdId);
    }

    @Test
    void deleteProduct_shouldDeleteAndSendEvent() throws Exception {
        final String createRequestBody = uniqueCreateJson(readResource("/json/product-create.json"));
        final String createdJson = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        final String createdId = extractId(createdJson);
        assertThat(createdId).isNotBlank();

        recordingPublisher.clearAll();

        mockMvc.perform(delete("/api/products/{id}", createdId))
                .andExpect(status().isNoContent());

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(recordingPublisher.deleteHistory()).hasSize(1));

        final SentDelete sentDelete = recordingPublisher.deleteHistory().getFirst();
        assertThat(sentDelete.kafkaKey()).contains(createdId);
        assertThat(sentDelete.event().productAction().name()).isEqualTo("DELETED");
    }

    @Test
    void createProduct_invalidEnum_shouldReturn400_noEvent() throws Exception {
        final String invalidRequestBody = readResource("/json/product-create-invalid-enum.json");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody))
                .andExpect(status().isBadRequest());

        assertThat(recordingPublisher.upsertHistory()).isEmpty();
        assertThat(recordingPublisher.deleteHistory()).isEmpty();
    }

    @Test
    void updateProduct_missingId_shouldReturn400_noEvent() throws Exception {
        final String invalidRequestBody = readResource("/json/product-update-missing-id.json");

        mockMvc.perform(put("/api/products/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody))
                .andExpect(status().isBadRequest());

        assertThat(recordingPublisher.upsertHistory()).isEmpty();
        assertThat(recordingPublisher.deleteHistory()).isEmpty();
    }

    @Test
    void deleteProduct_unknownId_shouldReturn404_noEvent() throws Exception {
        final String unknownId = "11111111-1111-1111-1111-111111111111";

        mockMvc.perform(delete("/api/products/{id}", unknownId))
                .andExpect(status().isNotFound());

        assertThat(recordingPublisher.deleteHistory()).isEmpty();
        assertThat(recordingPublisher.upsertHistory()).isEmpty();
    }

    @Test
    void createProduct_malformedJson_shouldReturn400_noEvent() throws Exception {
        final String malformedBody = "{ \"name\": \"X\", ";

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedBody))
                .andExpect(status().isBadRequest());

        assertThat(recordingPublisher.upsertHistory()).isEmpty();
        assertThat(recordingPublisher.deleteHistory()).isEmpty();
    }

    @Test
    void getById_unknown_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/products/{id}", "22222222-2222-2222-2222-222222222222"))
                .andExpect(status().isNotFound());
    }

    private String readResource(final String path) throws Exception {
        try (final var inputStream = getClass().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String extractId(final String json) throws Exception {
        final Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
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
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}