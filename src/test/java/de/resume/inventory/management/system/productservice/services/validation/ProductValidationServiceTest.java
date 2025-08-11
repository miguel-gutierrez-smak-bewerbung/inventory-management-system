package de.resume.inventory.management.system.productservice.services.validation;

import de.resume.inventory.management.system.productservice.exceptions.ProductValidationException;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import de.resume.inventory.management.system.productservice.models.enums.Category;
import de.resume.inventory.management.system.productservice.models.enums.Unit;
import de.resume.inventory.management.system.productservice.repositories.ProductRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProductValidationServiceTest {

    private static final String TENANT_ID = "Event-tenant";

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductValidationServiceImpl sut;

    @Test
    void validateProductToCreate() {
        final String name = "validName";
        final String articleNumber = "PRD-2024-0812";

        final ProductToCreateDto dto = new ProductToCreateDto(
                name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE, 2.50, TENANT_ID
        );

        Mockito.when(productRepository.existsByNameAndTenantId(name, TENANT_ID)).thenReturn(false);
        Mockito.when(productRepository.existsByArticleNumberAndTenantId(articleNumber, TENANT_ID)).thenReturn(false);

        Assertions.assertDoesNotThrow(() -> sut.validateProductToCreate(dto));
    }

    @Test
    void validateProductToCreate_shouldThrowOnDuplicateName() {
        final String name = "duplicateName";
        final String articleNumber = "PRD-2024-0812";

        final ProductToCreateDto dto = new ProductToCreateDto(
                name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE, 2.50, TENANT_ID
        );

        Mockito.when(productRepository.existsByNameAndTenantId(name, TENANT_ID)).thenReturn(true);

        final ProductValidationException ex = Assertions.assertThrows(
                ProductValidationException.class, () -> sut.validateProductToCreate(dto)
        );

        Assertions.assertEquals("Product name: 'duplicateName' is already taken", ex.getMessage());
    }

    @Test
    void validateProductToCreate_shouldThrowOnDuplicateArticleNumber() {
        final String name = "duplicateName";
        final String articleNumber = "duplicateArticleNumber";

        final ProductToCreateDto dto = new ProductToCreateDto(
                name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE, 2.50, TENANT_ID
        );

        Mockito.when(productRepository.existsByNameAndTenantId(name, TENANT_ID)).thenReturn(false);
        Mockito.when(productRepository.existsByArticleNumberAndTenantId(articleNumber, TENANT_ID)).thenReturn(true);

        final ProductValidationException ex = Assertions.assertThrows(
                ProductValidationException.class, () -> sut.validateProductToCreate(dto)
        );

        Assertions.assertEquals("Article number: 'duplicateArticleNumber' is already taken", ex.getMessage());
    }

    @Test
    void validateProductToCreate_shouldThrowOnNegativePrice() {
        final String name = "duplicateName";
        final String articleNumber = "PRD-2024-0812";
        final double negativePrice = -1.0;

        final ProductToCreateDto dto = new ProductToCreateDto(
                name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE, negativePrice, TENANT_ID
        );

        Mockito.when(productRepository.existsByNameAndTenantId(name, TENANT_ID)).thenReturn(false);
        Mockito.when(productRepository.existsByArticleNumberAndTenantId(articleNumber, TENANT_ID)).thenReturn(false);

        final ProductValidationException ex = Assertions.assertThrows(
                ProductValidationException.class, () -> sut.validateProductToCreate(dto)
        );

        final String expected = String.format(Locale.US, "price: '%.2f' must be greater than 0", negativePrice);
        Assertions.assertEquals(expected, ex.getMessage());
    }

    @Test
    void validateProductToCreate_shouldCollectAllValidationErrors() {
        final String duplicateName = "duplicateName";
        final String duplicateArticleNumber = "duplicateArticleNumber";
        final double negativePrice = -3.0;

        final ProductToCreateDto dto = new ProductToCreateDto(
                duplicateName, duplicateArticleNumber, null, Category.ELECTRONICS, Unit.PIECE, negativePrice, TENANT_ID
        );

        Mockito.when(productRepository.existsByNameAndTenantId(duplicateName, TENANT_ID)).thenReturn(true);
        Mockito.when(productRepository.existsByArticleNumberAndTenantId(duplicateArticleNumber, TENANT_ID)).thenReturn(true);

        final ProductValidationException ex = Assertions.assertThrows(
                ProductValidationException.class, () -> sut.validateProductToCreate(dto)
        );

        final String expected = "Product name: 'duplicateName' is already taken, " +
                "Article number: 'duplicateArticleNumber' is already taken, " +
                String.format(Locale.US, "price: '%.2f' must be greater than 0", negativePrice);
        Assertions.assertEquals(expected, ex.getMessage());
    }

    @Test
    void validateProductToUpdate_shouldNotThrowOnValidUpdate() {
        final String id = "1L";
        final String name = "validName";
        final String articleNumber = "PRD-2024-0812";

        final ProductToUpdateDto dto = new ProductToUpdateDto(
                id, name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE, 5.99, TENANT_ID
        );

        final ProductEntity existing = existingEntity(id, name, articleNumber, 5.99);
        Mockito.when(productRepository.findById(id)).thenReturn(Optional.of(existing));

        Assertions.assertDoesNotThrow(() -> sut.validateProductToUpdate(dto));
    }

    @Test
    void validateProductToUpdate_shouldThrowOnDuplicateName() {
        final String id = "1L";
        final String newName = "duplicateName";
        final String articleNumber = "PRD-2024-0812";

        final ProductToUpdateDto dto = new ProductToUpdateDto(
                id, newName, articleNumber, null, Category.ELECTRONICS, Unit.PIECE, 5.99, TENANT_ID
        );

        final ProductEntity existing = existingEntity(id, "oldName", articleNumber, 5.99);
        final ProductEntity conflicting = existingEntity("2L", newName, "ANY", 10.00);

        Mockito.when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        Mockito.when(productRepository.findByNameAndTenantId(newName, TENANT_ID)).thenReturn(Optional.of(conflicting));

        final ProductValidationException ex = Assertions.assertThrows(
                ProductValidationException.class, () -> sut.validateProductToUpdate(dto)
        );

        Assertions.assertEquals("Product name: 'duplicateName' is already taken", ex.getMessage());
    }

    @Test
    void validateProductToUpdate_shouldThrowOnDuplicateArticleNumber() {
        final String id = "1L";
        final String name = "validName";
        final String newArticleNumber = "duplicateArticleNumber";

        final ProductToUpdateDto dto = new ProductToUpdateDto(
                id, name, newArticleNumber, null, Category.ELECTRONICS, Unit.PIECE, 5.99, TENANT_ID
        );

        final ProductEntity existing = existingEntity(id, name, "OLD-ART", 5.99);
        final ProductEntity conflicting = existingEntity("2L", "ANY", newArticleNumber, 10.00);

        Mockito.when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        Mockito.when(productRepository.findByArticleNumberAndTenantId(newArticleNumber, TENANT_ID)).thenReturn(Optional.of(conflicting));

        final ProductValidationException ex = Assertions.assertThrows(
                ProductValidationException.class, () -> sut.validateProductToUpdate(dto)
        );

        Assertions.assertEquals("Article number: 'duplicateArticleNumber' is already taken", ex.getMessage());
    }

    @Test
    void validateProductToUpdate_shouldThrowOnNegativePrice() {
        final String id = "1L";
        final String name = "validName";
        final String articleNumber = "PRD-2024-0812";
        final double invalidPrice = -9.99;

        final ProductToUpdateDto dto = new ProductToUpdateDto(
                id, name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE, invalidPrice, TENANT_ID
        );

        final ProductEntity existing = existingEntity(id, name, articleNumber, 5.99);
        Mockito.when(productRepository.findById(id)).thenReturn(Optional.of(existing));

        final ProductValidationException ex = Assertions.assertThrows(
                ProductValidationException.class, () -> sut.validateProductToUpdate(dto)
        );

        final String expected = String.format(Locale.US, "price: '%.2f' must be greater than 0", invalidPrice);
        Assertions.assertEquals(expected, ex.getMessage());
    }

    @Test
    void validateProductToUpdate_shouldCollectAllValidationErrors() {
        final String id = "1L";
        final String newName = "duplicateName";
        final String newArticleNumber = "duplicateArticleNumber";
        final double newPrice = -3.0;

        final ProductToUpdateDto dto = new ProductToUpdateDto(
                id, newName, newArticleNumber, null, Category.ELECTRONICS, Unit.PIECE, newPrice, TENANT_ID
        );

        final ProductEntity existing = existingEntity(id, "oldName", "OLD-ART", 5.99);
        final ProductEntity nameConflict = existingEntity("2L", newName, "ANY", 10.00);
        final ProductEntity articleConflict = existingEntity("3L", "ANY2", newArticleNumber, 20.00);

        Mockito.when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        Mockito.when(productRepository.findByNameAndTenantId(newName, TENANT_ID)).thenReturn(Optional.of(nameConflict));
        Mockito.when(productRepository.findByArticleNumberAndTenantId(newArticleNumber, TENANT_ID)).thenReturn(Optional.of(articleConflict));

        final ProductValidationException productValidationException = Assertions.assertThrows(
                ProductValidationException.class, () -> sut.validateProductToUpdate(dto)
        );

        final String expected = "Product name: 'duplicateName' is already taken, " +
                "Article number: 'duplicateArticleNumber' is already taken, " +
                String.format(Locale.US, "price: '%.2f' must be greater than 0", newPrice);
        Assertions.assertEquals(expected, productValidationException.getMessage());
    }

    private ProductEntity existingEntity(final String id,
                                         final String name,
                                         final String articleNumber,
                                         final double price) {
        final ProductEntity productEntity = new ProductEntity();
        productEntity.setId(id);
        productEntity.setName(name);
        productEntity.setArticleNumber(articleNumber);
        productEntity.setPrice(BigDecimal.valueOf(price));
        return productEntity;
    }
}