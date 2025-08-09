package de.resume.inventory.management.system.productservice.models.entities;

import de.resume.inventory.management.system.productservice.models.enums.Category;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;
import de.resume.inventory.management.system.productservice.models.enums.Unit;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "product_history")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductHistoryEntity extends BaseEntity {

    @NotBlank(message = "product id must not be blank")
    @Column(name = "productId", nullable = false)
    private String productId;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "action must not be null")
    @Column(name = "product_action", nullable = false)
    private ProductAction action;

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
    private BigDecimal price;

    @Column(name = "tenant_id")
    @NotBlank(message = "tenant id must not be blank")
    private String tenantId;

    @Column(name = "changedBy", nullable = false)
    @NotBlank(message = "changed by must not be blank")
    private String changedBy;
}
