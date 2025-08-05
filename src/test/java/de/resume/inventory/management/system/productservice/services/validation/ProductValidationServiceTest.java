package de.resume.inventory.management.system.productservice.services.validation;


import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
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

@ExtendWith(MockitoExtension.class)
class ProductValidationServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductValidationServiceImpl sut;

    @Test
    void validateProductToCreate() {
        final String name = "validName";
        final String articleNumber = "PRD-2024-0812";

        final ProductToCreateDto productToCreateDto = new ProductToCreateDto(
                name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE,2.50
        );

        Mockito.when(productRepository.existsByName(name)).thenReturn(false);
        Mockito.when(productRepository.existsByArticleNumber(articleNumber)).thenReturn(false);

        Assertions.assertDoesNotThrow(() -> sut.validateProductToCreate(productToCreateDto));
    }

    @Test
    void validateProductToCreate_shouldThrowOnDuplicateName() {
        final String name = "duplicateName";
        final String articleNumber = "PRD-2024-0812";

        final ProductToCreateDto productToCreateDto = new ProductToCreateDto(
                name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE,2.50
        );

        Mockito.when(productRepository.existsByName(name)).thenReturn(true);

        final IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class, () -> sut.validateProductToCreate(productToCreateDto)
        );

        final String expectedMessage = String.format("Product name '%s' is already taken", name);
        final String actualMessage = illegalArgumentException.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void validateProductToCreate_shouldThrowOnDuplicateArticleNumber() {
        final String name = "duplicateName";
        final String articleNumber = "PRD-2024-0812";

        final ProductToCreateDto productToCreateDto = new ProductToCreateDto(
                name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE,2.50
        );

        Mockito.when(productRepository.existsByName(name)).thenReturn(false);
        Mockito.when(productRepository.existsByArticleNumber(articleNumber)).thenReturn(true);

        final IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class, () -> sut.validateProductToCreate(productToCreateDto)
        );

        final String expectedMessage = "Article number: 'PRD-2024-0812' is already taken";
        final String actualMessage = illegalArgumentException.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void validateProductToCreate_shouldThrowOnNegativePrice() {
        final String name = "duplicateName";
        final String articleNumber = "PRD-2024-0812";
        final double negativePrice = -1.0;

        final ProductToCreateDto productToCreateDto = new ProductToCreateDto(
                name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE,negativePrice
        );

        Mockito.when(productRepository.existsByName(name)).thenReturn(false);
        Mockito.when(productRepository.existsByArticleNumber(articleNumber)).thenReturn(false);

        final IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class, () -> sut.validateProductToCreate(productToCreateDto)
        );

        final String expectedMessage = "price '-1.0' must be greater than 0";
        final String actualMessage = illegalArgumentException.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void validateProductToCreate_shouldCollectAllValidationErrors() {

        final String duplicateName = "duplicateName";
        final String duplicateArticleNumber = "invalidArticleNumber";
        final double negativePrice = -3.0;

        final ProductToCreateDto dto = new ProductToCreateDto(
                duplicateName, duplicateArticleNumber, null, Category.ELECTRONICS, Unit.PIECE, negativePrice
        );

        Mockito.when(productRepository.existsByName(duplicateName)).thenReturn(true);
        Mockito.when(productRepository.existsByArticleNumber(duplicateArticleNumber)).thenReturn(true);

        final ProductValidationException productValidationException = Assertions.assertThrows(
                ProductValidationException.class,
                () -> sut.validateProductToCreate(dto)
        );

        final String actualMessage = productValidationException.getMessage();
        final String expectedMessage = "Product name 'duplicateName' is already taken, Article number: 'invalidArticleNumber' " +
                "is already taken, price '-3.0' must be greater than 0";
        Assertions.assertEquals(expectedMessage, actualMessage);
    }
}
