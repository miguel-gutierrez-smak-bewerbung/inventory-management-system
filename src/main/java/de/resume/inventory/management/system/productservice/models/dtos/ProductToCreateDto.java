package de.resume.inventory.management.system.productservice.models.dtos;

import de.resume.inventory.management.system.productservice.models.enums.Category;
import de.resume.inventory.management.system.productservice.models.enums.Unit;
import jakarta.validation.constraints.*;

public record ProductToCreateDto(

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

        @NotNull(message = "price must not be null")
        @DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "price must be a valid amount with max 2 decimals")
        Double price
) {}
