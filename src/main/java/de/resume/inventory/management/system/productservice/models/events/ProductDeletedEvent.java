package de.resume.inventory.management.system.productservice.models.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;

import java.time.LocalDateTime;

public record ProductDeletedEvent(

        @JsonProperty("id")
        String id,

        @JsonProperty("timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,

        @JsonProperty("productAction")
        ProductAction productAction
) { }
