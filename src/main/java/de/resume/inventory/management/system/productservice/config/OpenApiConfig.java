package de.resume.inventory.management.system.productservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenApiProperties.class)
public class OpenApiConfig {

    private static final String description = """
                Product Service — part of the Inventory Management System.

                Responsibilities:
                • Manage the product catalog (create, update, delete)
                • Persist product data
                • Publish domain events for product lifecycle changes

                Architecture:
                • Event-driven with Kafka
                • This service acts primarily as an event producer; downstream services consume product events.
                """;

    private static final String externalDocsDescription = "Inventory Management System – Project Docs";

    @Bean
    public OpenAPI productServiceOpenAPI(final OpenApiProperties openApiProperties) {

        return new OpenAPI()
                .info(new Info()
                        .title(openApiProperties.getTitle())
                        .description(description)
                        .version(openApiProperties.getVersion()))
                .externalDocs(new ExternalDocumentation()
                        .description(externalDocsDescription)
                        .url(openApiProperties.getExternalDocsUrl()));
    }
}
