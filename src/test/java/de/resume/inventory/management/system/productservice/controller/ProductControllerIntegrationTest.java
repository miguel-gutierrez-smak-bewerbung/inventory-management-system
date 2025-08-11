package de.resume.inventory.management.system.productservice.controller;

import de.resume.inventory.management.system.productservice.config.KafkaProducerTestConfig;
import de.resume.inventory.management.system.productservice.config.TestContainerConfiguration;
import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import({ TestContainerConfiguration.class, KafkaProducerTestConfig.class })
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.application.name=Event-tenant")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockProducer<String, ProductUpsertedEvent> mockUpsertProducer;

    @Autowired
    private MockProducer<String, ProductDeletedEvent> mockDeleteProducer;

    @Test
    void createProduct_shouldPersistAndSendEvent() throws Exception {
        mockUpsertProducer.history().clear();

        final String requestJson = """
            {
              "name": "IntegrationTest Product",
              "articleNumber": "Art-100",
              "description": "Test description",
              "category": "ELECTRONICS",
              "unit": "PIECE",
              "price": 12.50,
              "tenantId": "Event-tenant"
            }
            """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        assertThat(mockUpsertProducer.history()).hasSize(1);
        final ProducerRecord<String, ProductUpsertedEvent> record = mockUpsertProducer.history().getFirst();
        assertThat(record.value()).isInstanceOf(ProductUpsertedEvent.class);
    }
}