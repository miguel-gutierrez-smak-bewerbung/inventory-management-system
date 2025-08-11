package de.resume.inventory.management.system.productservice.mapper;

import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import de.resume.inventory.management.system.productservice.models.entities.ProductHistoryEntity;
import de.resume.inventory.management.system.productservice.models.enums.Category;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;
import de.resume.inventory.management.system.productservice.models.enums.Unit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

class ProductHistoryMapperTest {

    private final ProductHistoryMapper sut = new ProductHistoryMapperImpl();

    @Test
    void mapToEntity_withCompleteProduct_mapsAllFields() {
        final String expectedProductIdentifier = "product-100";
        final String expectedName = "Precision Screwdriver";
        final String expectedArticleNumber = "PS-100";
        final String expectedDescription = "High precision screwdriver with magnetic tip";
        final Category expectedCategory = Category.ELECTRONICS;
        final Unit expectedUnit = Unit.PIECE;
        final BigDecimal expectedPrice = BigDecimal.valueOf(14.95);
        final String expectedTenantIdentifier = "Event-tenant";
        final ProductAction expectedProductAction = ProductAction.CREATED;
        final String expectedChangedBy = "john.doe@example.com";

        final ProductEntity sourceProduct = new ProductEntity(
                expectedName,
                expectedArticleNumber,
                expectedDescription,
                expectedCategory,
                expectedUnit,
                expectedPrice,
                expectedTenantIdentifier
        );
        sourceProduct.setId(expectedProductIdentifier);

        final ProductHistoryEntity actual = sut.toEntity(sourceProduct, expectedProductAction, expectedChangedBy);

        final ProductHistoryEntity expected = new ProductHistoryEntity();
        expected.setProductId(expectedProductIdentifier);
        expected.setName(expectedName);
        expected.setArticleNumber(expectedArticleNumber);
        expected.setDescription(expectedDescription);
        expected.setCategory(expectedCategory);
        expected.setUnit(expectedUnit);
        expected.setPrice(expectedPrice);
        expected.setTenantId(expectedTenantIdentifier);
        expected.setAction(expectedProductAction);
        expected.setChangedBy(expectedChangedBy);

        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(expected);
    }

    @Test
    void mapToEntity_withNullDescription_mapsNullDescription() {
        final String expectedProductIdentifier = "product-200";
        final String expectedName = "Laser Level";
        final String expectedArticleNumber = "LL-200";
        final String expectedDescription = null;
        final Category expectedCategory = Category.HOUSEHOLD;
        final Unit expectedUnit = Unit.PIECE;
        final BigDecimal expectedPrice = BigDecimal.valueOf(59.90);
        final String expectedTenantIdentifier = "Event-tenant";
        final ProductAction expectedProductAction = ProductAction.UPDATED;
        final String expectedChangedBy = "alice.smith@example.com";

        final ProductEntity sourceProduct = new ProductEntity(
                expectedName,
                expectedArticleNumber,
                expectedDescription,
                expectedCategory,
                expectedUnit,
                expectedPrice,
                expectedTenantIdentifier
        );
        sourceProduct.setId(expectedProductIdentifier);

        final ProductHistoryEntity actual = sut.toEntity(sourceProduct, expectedProductAction, expectedChangedBy);

        final ProductHistoryEntity expected = new ProductHistoryEntity();
        expected.setProductId(expectedProductIdentifier);
        expected.setName(expectedName);
        expected.setArticleNumber(expectedArticleNumber);
        expected.setDescription(null);
        expected.setCategory(expectedCategory);
        expected.setUnit(expectedUnit);
        expected.setPrice(expectedPrice);
        expected.setTenantId(expectedTenantIdentifier);
        expected.setAction(expectedProductAction);
        expected.setChangedBy(expectedChangedBy);

        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(expected);
    }

    @Test
    void mapToEntity_withNullChangedBy_mapsNullChangedBy() {
        final String expectedProductIdentifier = "product-300";
        final String expectedName = "Carbon Dioxide Sensor";
        final String expectedArticleNumber = "CO2-300";
        final String expectedDescription = "Indoor air quality sensor";
        final Category expectedCategory = Category.HEALTH;
        final Unit expectedUnit = Unit.PIECE;
        final BigDecimal expectedPrice = BigDecimal.valueOf(129.00);
        final String expectedTenantIdentifier = "Event-tenant";
        final ProductAction expectedProductAction = ProductAction.CREATED;
        final String expectedChangedBy = null;

        final ProductEntity sourceProduct = new ProductEntity(
                expectedName,
                expectedArticleNumber,
                expectedDescription,
                expectedCategory,
                expectedUnit,
                expectedPrice,
                expectedTenantIdentifier
        );
        sourceProduct.setId(expectedProductIdentifier);

        final ProductHistoryEntity actual = sut.toEntity(sourceProduct, expectedProductAction, expectedChangedBy);

        final ProductHistoryEntity expected = new ProductHistoryEntity();
        expected.setProductId(expectedProductIdentifier);
        expected.setName(expectedName);
        expected.setArticleNumber(expectedArticleNumber);
        expected.setDescription(expectedDescription);
        expected.setCategory(expectedCategory);
        expected.setUnit(expectedUnit);
        expected.setPrice(expectedPrice);
        expected.setTenantId(expectedTenantIdentifier);
        expected.setAction(expectedProductAction);
        expected.setChangedBy(null);

        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(expected);
    }

    @Test
    void mapToEntity_withNullProductAction_mapsNullAction() {
        final String expectedProductIdentifier = "product-400";
        final String expectedName = "Digital Caliper";
        final String expectedArticleNumber = "DC-400";
        final String expectedDescription = "Digital caliper with 0.01mm resolution";
        final Category expectedCategory = Category.ELECTRONICS;
        final Unit expectedUnit = Unit.PIECE;
        final BigDecimal expectedPrice = BigDecimal.valueOf(24.50);
        final String expectedTenantIdentifier = "Event-tenant";
        final ProductAction expectedProductAction = null;
        final String expectedChangedBy = "qa.tester@example.com";

        final ProductEntity sourceProduct = new ProductEntity(
                expectedName,
                expectedArticleNumber,
                expectedDescription,
                expectedCategory,
                expectedUnit,
                expectedPrice,
                expectedTenantIdentifier
        );
        sourceProduct.setId(expectedProductIdentifier);

        final ProductHistoryEntity actual = sut.toEntity(sourceProduct, expectedProductAction, expectedChangedBy);

        final ProductHistoryEntity expected = new ProductHistoryEntity();
        expected.setProductId(expectedProductIdentifier);
        expected.setName(expectedName);
        expected.setArticleNumber(expectedArticleNumber);
        expected.setDescription(expectedDescription);
        expected.setCategory(expectedCategory);
        expected.setUnit(expectedUnit);
        expected.setPrice(expectedPrice);
        expected.setTenantId(expectedTenantIdentifier);
        expected.setAction(null);
        expected.setChangedBy(expectedChangedBy);

        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(expected);
    }

    @Test
    void mapToEntity_withEmptyStrings_mapsEmptyStrings() {
        final String expectedProductIdentifier = "product-500";
        final String expectedName = "";
        final String expectedArticleNumber = "";
        final String expectedDescription = "";
        final Category expectedCategory = Category.TOYS;
        final Unit expectedUnit = Unit.PIECE;
        final BigDecimal expectedPrice = BigDecimal.valueOf(1.00);
        final String expectedTenantIdentifier = "";
        final ProductAction expectedProductAction = ProductAction.UPDATED;
        final String expectedChangedBy = "";

        final ProductEntity sourceProduct = new ProductEntity(
                expectedName,
                expectedArticleNumber,
                expectedDescription,
                expectedCategory,
                expectedUnit,
                expectedPrice,
                expectedTenantIdentifier
        );
        sourceProduct.setId(expectedProductIdentifier);

        final ProductHistoryEntity actual = sut.toEntity(sourceProduct, expectedProductAction, expectedChangedBy);

        final ProductHistoryEntity expected = new ProductHistoryEntity();
        expected.setProductId(expectedProductIdentifier);
        expected.setName("");
        expected.setArticleNumber("");
        expected.setDescription("");
        expected.setCategory(expectedCategory);
        expected.setUnit(expectedUnit);
        expected.setPrice(expectedPrice);
        expected.setTenantId("");
        expected.setAction(expectedProductAction);
        expected.setChangedBy("");

        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(ProductAction.class)
    void mapToEntity_withDifferentActions_mapsActionCorrectly(final ProductAction parameterProductAction) {
        final String expectedProductIdentifier = "product-600";
        final String expectedName = "Impact Drill";
        final String expectedArticleNumber = "ID-600";
        final String expectedDescription = "Impact drill with 750W motor";
        final Category expectedCategory = Category.HOUSEHOLD;
        final Unit expectedUnit = Unit.PIECE;
        final BigDecimal expectedPrice = BigDecimal.valueOf(79.00);
        final String expectedTenantIdentifier = "Event-tenant";
        final String expectedChangedBy = "integration.user@example.com";

        final ProductEntity sourceProduct = new ProductEntity(
                expectedName,
                expectedArticleNumber,
                expectedDescription,
                expectedCategory,
                expectedUnit,
                expectedPrice,
                expectedTenantIdentifier
        );
        sourceProduct.setId(expectedProductIdentifier);

        final ProductHistoryEntity actual = sut.toEntity(sourceProduct, parameterProductAction, expectedChangedBy);

        final ProductHistoryEntity expected = new ProductHistoryEntity();
        expected.setProductId(expectedProductIdentifier);
        expected.setName(expectedName);
        expected.setArticleNumber(expectedArticleNumber);
        expected.setDescription(expectedDescription);
        expected.setCategory(expectedCategory);
        expected.setUnit(expectedUnit);
        expected.setPrice(expectedPrice);
        expected.setTenantId(expectedTenantIdentifier);
        expected.setAction(parameterProductAction);
        expected.setChangedBy(expectedChangedBy);

        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(expected);
    }

    @Test
    void mapToEntity_withLargeAndScaledPrice_preservesExactPrice() {
        final String expectedProductIdentifier = "product-700";
        final String expectedName = "Oscilloscope";
        final String expectedArticleNumber = "OSC-700";
        final String expectedDescription = "Four channel oscilloscope";
        final Category expectedCategory = Category.ELECTRONICS;
        final Unit expectedUnit = Unit.PIECE;
        final BigDecimal expectedPrice = new BigDecimal("1234567890.123456789");
        final String expectedTenantIdentifier = "Event-tenant";
        final ProductAction expectedProductAction = ProductAction.UPDATED;
        final String expectedChangedBy = "finance.user@example.com";

        final ProductEntity sourceProduct = new ProductEntity(
                expectedName,
                expectedArticleNumber,
                expectedDescription,
                expectedCategory,
                expectedUnit,
                expectedPrice,
                expectedTenantIdentifier
        );
        sourceProduct.setId(expectedProductIdentifier);

        final ProductHistoryEntity actual = sut.toEntity(sourceProduct, expectedProductAction, expectedChangedBy);

        final ProductHistoryEntity expected = new ProductHistoryEntity();
        expected.setProductId(expectedProductIdentifier);
        expected.setName(expectedName);
        expected.setArticleNumber(expectedArticleNumber);
        expected.setDescription(expectedDescription);
        expected.setCategory(expectedCategory);
        expected.setUnit(expectedUnit);
        expected.setPrice(expectedPrice);
        expected.setTenantId(expectedTenantIdentifier);
        expected.setAction(expectedProductAction);
        expected.setChangedBy(expectedChangedBy);

        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(expected);
    }

    @Test
    void mapToEntity_withNullProduct_returnsEntityWithOnlyActionAndChangedBy() {
        final ProductEntity sourceProduct = null;
        final ProductAction sourceProductAction = ProductAction.DELETED;
        final String sourceChangedBy = "system.user@example.com";

        final ProductHistoryEntity actual = sut.toEntity(sourceProduct, sourceProductAction, sourceChangedBy);

        final ProductHistoryEntity expected = new ProductHistoryEntity();
        expected.setId(null);
        expected.setProductId(null);
        expected.setName(null);
        expected.setArticleNumber(null);
        expected.setDescription(null);
        expected.setCategory(null);
        expected.setUnit(null);
        expected.setPrice(null);
        expected.setTenantId(null);
        expected.setAction(ProductAction.DELETED);
        expected.setChangedBy("system.user@example.com");

        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(expected);
    }
}