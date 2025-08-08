package de.resume.inventory.management.system.productservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainerConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgreSQLContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("product-service")
                .withUsername("postgres")
                .withPassword("test")
                .withReuse(true)
                .withStartupTimeout(java.time.Duration.ofMinutes(2))
                .withInitScript("init.sql")
                .withCommand("postgres", "-c", "fsync=off", "-c", "max_connections=100");

    }
}
