package de.resume.inventory.management.system.productservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "openapi")
public class OpenApiProperties {
    private String title;
    private String version;
    private String externalDocsUrl;
}
