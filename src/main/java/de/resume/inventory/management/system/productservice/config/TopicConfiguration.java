package de.resume.inventory.management.system.productservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "topics")
public class TopicConfiguration {
    private String productUpsert;
    private String productUpsertFail;
    private String productUpsertRetryFail;
}
