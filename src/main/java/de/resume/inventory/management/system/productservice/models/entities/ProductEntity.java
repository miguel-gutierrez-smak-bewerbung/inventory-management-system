package de.resume.inventory.management.system.productservice.models.entities;

import de.resume.inventory.management.system.productservice.models.enums.Category;
import de.resume.inventory.management.system.productservice.models.enums.Unit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Data;

@Data
@Entity
@Table(name = "products")
@EqualsAndHashCode(callSuper = true)
public class ProductEntity extends BaseEntity {

    @Column(name = "name", unique = true, nullable = false, length = 30)
    @NotBlank(message = "product name must not be blank")
    @Size(min = 2, max = 30, message = "product name must be between 2 and 30 characters")
    private String name;

    @Column(name = "article_number", unique = true, nullable = false, length = 20)
    @NotBlank(message = "article number must not be blank")
    @Size(min = 2, max = 20, message = "article number must be between 2 and 20 characters")
    private String articleNumber;

    @Column(name = "description")
    @Size(max = 255, message = "description must not exceed 255 characters")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    @NotNull(message = "category must not be null")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false)
    @NotNull(message = "unit must not be null")
    private Unit unit;

    @Column(name = "price", nullable = false)
    @NotNull(message = "price must not be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "price must be a valid amount with max 2 decimals")
    private Double price;
}
