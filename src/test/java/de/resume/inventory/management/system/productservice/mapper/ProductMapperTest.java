package de.resume.inventory.management.system.productservice.mapper;

import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import de.resume.inventory.management.system.productservice.models.enums.Category;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;
import de.resume.inventory.management.system.productservice.models.enums.Unit;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ProductMapperImpl.class })
class ProductMapperTest {

    private final ProductMapper sut = new ProductMapperImpl();

    @Test
    void shouldMapProductToCreateDtoToProductEntity() {
        final ProductToCreateDto dto = new ProductToCreateDto(
                "Test Product",
                "12345",
                "This is a test description",
                Category.ELECTRONICS,
                Unit.PIECE,
                99.99
        );

        final ProductEntity expected = new ProductEntity(
                "Test Product",
                "12345",
                "This is a test description",
                Category.ELECTRONICS,
                Unit.PIECE,
                BigDecimal.valueOf(99.99)
        );

        final ProductEntity actual = sut.toEntity(dto);

        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(expected);
    }

    @Test
    void shouldMapProductToUpdateDtoToProductEntity() {
        final ProductToUpdateDto dto = new ProductToUpdateDto(
                "existing-id",
                "Updated Product",
                "67890",
                "Updated description",
                Category.AUTOMOTIVE,
                Unit.BOX,
                199.99
        );

        final ProductEntity expected = new ProductEntity(
                "Updated Product",
                "67890",
                "Updated description",
                Category.AUTOMOTIVE,
                Unit.BOX,
                BigDecimal.valueOf(199.99)
        );
        expected.setId("existing-id");

        final ProductEntity actual = sut.toEntity(dto);

        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(expected);
    }

    @Test
    void shouldMapProductEntityToProductToCreateDto() {
        final ProductEntity entity = new ProductEntity(
                "Sample Name",
                "A-0001",
                "Description",
                Category.AUTOMOTIVE,
                Unit.BOX,
                BigDecimal.valueOf(10.99)
        );

        final ProductToCreateDto expected = new ProductToCreateDto(
                "Sample Name",
                "A-0001",
                "Description",
                Category.AUTOMOTIVE,
                Unit.BOX,
                10.99
        );

        final ProductToCreateDto actual = sut.toCreateDto(entity);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldMapProductEntityToProductToUpdateDto() {
        final ProductEntity entity = new ProductEntity(
                "Sample Name",
                "A-0002",
                "Description",
                Category.FOOD,
                Unit.PIECE,
                BigDecimal.valueOf(25.50)
        );
        entity.setId("test-id");

        final ProductToUpdateDto expected = new ProductToUpdateDto(
                "test-id",
                "Sample Name",
                "A-0002",
                "Description",
                Category.FOOD,
                Unit.PIECE,
                25.50
        );

        final ProductToUpdateDto actual = sut.toUpdateDto(entity);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldMapProductEntityToProductUpsertedMessage() {
        final ProductEntity entity = new ProductEntity(
                "Kafka-Produkt",
                "KFK-2024-001",
                "Event-basiert",
                Category.ELECTRONICS,
                Unit.PIECE,
                BigDecimal.valueOf(999.00)
        );
        entity.setId("product-123");
        entity.setUpdatedAt(LocalDateTime.of(2025, 8, 6, 14, 20));

        final String tenantId = "Event-tenant";
        final ProductUpsertedEvent expected = new ProductUpsertedEvent(
                "product-123",
                "Kafka-Produkt",
                "KFK-2024-001",
                Category.ELECTRONICS.name(),
                Unit.PIECE.name(),
                999.00,
                "Event-basiert",
                LocalDateTime.of(2025, 8, 6, 14, 20),
                ProductAction.CREATED,
                tenantId
        );

        final ProductUpsertedEvent actual = sut.toEvent(entity, ProductAction.CREATED, tenantId);

        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("action")
                .isEqualTo(expected);
    }

    @Test
    void shouldMapUpdatedAtToTimestamp() {
        final ProductEntity entity = new ProductEntity();
        entity.setId("product-1");
        entity.setUpdatedAt(LocalDateTime.of(2025, 8, 6, 14, 20));

        final ProductUpsertedEvent message = sut.toEvent(entity, ProductAction.CREATED, "Event-tenant");

        assertThat(message.timestamp()).isEqualTo(entity.getUpdatedAt());
    }
}