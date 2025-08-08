package de.resume.inventory.management.system.productservice.models.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;

import java.time.LocalDateTime;

public record ProductUpsertedEvent(

        @JsonProperty("id")
        String id,

        @JsonProperty("name")
        String name,

        @JsonProperty("articleNumber")
        String articleNumber,

        @JsonProperty("category")
        String category,

        @JsonProperty("unit")
        String unit,

        @JsonProperty("price")
        double price,

        @JsonProperty("description")
        String description,

        @JsonProperty("timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime timestamp,

        @JsonProperty("productAction")
        ProductAction productAction
) {
}
