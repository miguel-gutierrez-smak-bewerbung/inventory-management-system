package de.resume.inventory.management.system.productservice.models.dtos;

import de.resume.inventory.management.system.productservice.models.enums.Category;
import de.resume.inventory.management.system.productservice.models.enums.Unit;
import jakarta.validation.constraints.*;

public record ProductToUpdateDto(

        @NotNull(message = "id must not be null")
        String id,

        @NotBlank(message = "product name must not be blank")
        @Size(min = 2, max = 30, message = "product name must be between 2 and 30 characters")
        String name,

        @NotBlank(message = "article number must not be blank")
        @Size(min = 2, max = 20, message = "article number must be between 2 and 20 characters")
        String articleNumber,

        @Size(max = 255, message = "description must not exceed 255 characters")
        String description,

        @NotNull(message = "category must not be null")
        Category category,

        @NotNull(message = "unit must not be null")
        Unit unit,

        @DecimalMin(value = "0.0", inclusive = false)
        @NotNull(message = "price must not be null")
        @DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than 0")
        Double price,

        @NotBlank(message = "tenant id must not be blank")
        String tenantId
) {}
