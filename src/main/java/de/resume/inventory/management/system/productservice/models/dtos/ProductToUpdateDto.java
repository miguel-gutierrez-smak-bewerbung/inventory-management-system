package de.resume.inventory.management.system.productservice.models.dtos;

import de.resume.inventory.management.system.productservice.models.enums.Category;
import de.resume.inventory.management.system.productservice.models.enums.Unit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductToUpdateDto(

        @Size(min = 2, max = 30)
        String name,

        @Size(min = 2, max = 20)
        String articleNumber,

        @Size(max = 255)
        String description,

        @NotNull(message = "category must not be null")
        Category category,

        @NotNull(message = "unit must not be null")
        Unit unit,

        @DecimalMin(value = "0.0", inclusive = false)

        @Digits(integer = 10, fraction = 2)
        Double price
) {}
