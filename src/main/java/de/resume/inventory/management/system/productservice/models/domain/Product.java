package de.resume.inventory.management.system.productservice.models.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.resume.inventory.management.system.productservice.models.enums.Category;
import de.resume.inventory.management.system.productservice.models.enums.Unit;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record Product(

        @JsonProperty("id")
        String id,

        @JsonProperty("name")
        String name,

        @JsonProperty("articleNumber")
        String articleNumber,

        @JsonProperty("description")
        String description,

        @JsonProperty("category")
        Category category,

        @JsonProperty("unit")
        Unit unit,

        @JsonProperty("price")
        Double price,

        @JsonProperty("createdAt")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime createdAt,

        @JsonProperty("updatedAt")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime updatedAt,

        @Column(name = "tenant_id")
        @NotBlank(message = "tenant id must not be blank")
        String tenantId
) {}
