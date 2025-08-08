package de.resume.inventory.management.system.productservice.repositories;

import de.resume.inventory.management.system.productservice.config.TestContainerConfiguration;
import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import de.resume.inventory.management.system.productservice.models.enums.Category;
import de.resume.inventory.management.system.productservice.models.enums.Unit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestContainerConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository sut;

    @Test
    void shouldSave() {
        final ProductEntity productEntity = createProduct("test", "1234567890");
        sut.saveAndFlush(productEntity);

        final Optional<ProductEntity> actual = sut.findById(productEntity.getId());

        Assertions.assertTrue(actual.isPresent());
        Assertions.assertNotNull(actual.get().getId());
        Assertions.assertNotNull(actual.get().getCreatedAt());
        Assertions.assertNotNull(actual.get().getUpdatedAt());

        assertThat(actual.get())
                .usingRecursiveComparison()
                .ignoringFields("id","createdAt", "updatedAt")
                .isEqualTo(productEntity);
    }

    @Test
    void shouldUpdateProduct() {
        final ProductEntity productEntity = createProduct("updateTest", "2345678901");

        sut.save(productEntity);

        productEntity.setName("updatedName");
        productEntity.setDescription("updated description");
        productEntity.setPrice(new BigDecimal("15.55"));
        sut.save(productEntity);

        final Optional<ProductEntity> actual = sut.findById(productEntity.getId());
        Assertions.assertTrue(actual.isPresent());
        assertThat(actual.get())
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(productEntity);
    }

    @Test
    void shouldDeleteProductById() {

        final ProductEntity productEntity = createProduct("deleteTest", "3456789012");
        sut.save(productEntity);

        sut.deleteById(productEntity.getId());

        final Optional<ProductEntity> actual = sut.findById(productEntity.getId());
        Assertions.assertFalse(actual.isPresent());
    }

    @Test
    void shouldFindProductByArticleNumber() {

        final ProductEntity productEntity = createProduct("findByArticleNumberTest", "4567890123");

        sut.save(productEntity);

        final Optional<ProductEntity> found = sut.findByArticleNumber("4567890123");

        Assertions.assertTrue(found.isPresent());
        assertThat(found.get())
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(productEntity);
    }

    @Test
    void shouldReturnTrueIfProductExistsByName() {
        final ProductEntity productEntity = createProduct("existsByNameTest", "5678901234");
        sut.save(productEntity);

        boolean exists = sut.existsByName("existsByNameTest");
        Assertions.assertTrue(exists);
    }

    @Test
    void shouldReturnFalseIfProductDoesNotExistByName() {
        boolean exists = sut.existsByName("doesNotExist");
        Assertions.assertFalse(exists);
    }

    @Test
    void shouldThrowOnDuplicateArticleNumber() {

        final ProductEntity productEntity1 = createProduct("duplicateArticleTestA", "6789012345");
        final ProductEntity productEntity2 = createProduct("duplicateArticleTestB", "6789012345");

        sut.save(productEntity1);

        Assertions.assertThrows(Exception.class, () -> sut.saveAndFlush(productEntity2));
    }

    @Test
    void shouldThrowOnDuplicateName() {

        final ProductEntity productEntity1 = createProduct("duplicateNameTest", "7890123456");
        final ProductEntity productEntity2 = createProduct("duplicateNameTest", "7890123457");

        sut.save(productEntity1);

        Assertions.assertThrows(Exception.class, () -> sut.saveAndFlush(productEntity2));
    }

    @Test
    void shouldFindProductByName() {

        final ProductEntity productEntity = createProduct("findByNameTest", "8901234567");
        sut.save(productEntity);

        final Optional<ProductEntity> actual = sut.findByName("findByNameTest");

        Assertions.assertTrue(actual.isPresent());

        assertThat(actual.get())
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(productEntity);
    }

    @Test
    void shouldReturnEmptyOptionalIfNameDoesNotExist() {
        final Optional<ProductEntity> found = sut.findByName("notExists");
        Assertions.assertTrue(found.isEmpty());
    }

    private ProductEntity createProduct(final String name, final String articleNumber) {
        final ProductEntity entity = new ProductEntity();
        entity.setName(name);
        entity.setArticleNumber(articleNumber);
        entity.setDescription("test description");
        entity.setCategory(Category.ELECTRONICS);
        entity.setUnit(Unit.PIECE);
        entity.setPrice(new BigDecimal("10.0"));
        entity.setTenantId("Event-tenant");
        return entity;
    }

}

