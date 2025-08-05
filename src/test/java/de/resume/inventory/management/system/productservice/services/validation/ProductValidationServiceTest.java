package de.resume.inventory.management.system.productservice.services.validation;


import de.resume.inventory.management.system.productservice.exceptions.ProductValidationException;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
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

        final ProductValidationException productValidationException = Assertions.assertThrows(
                ProductValidationException.class, () -> sut.validateProductToCreate(productToCreateDto)
        );

        final String expectedMessage = String.format("Product name: '%s' is already taken", name);
        final String actualMessage = productValidationException.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void validateProductToCreate_shouldThrowOnDuplicateArticleNumber() {
        final String name = "duplicateName";
        final String articleNumber = "duplicateArticleNumber";

        final ProductToCreateDto productToCreateDto = new ProductToCreateDto(
                name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE,2.50
        );

        Mockito.when(productRepository.existsByName(name)).thenReturn(false);
        Mockito.when(productRepository.existsByArticleNumber(articleNumber)).thenReturn(true);

        final ProductValidationException productValidationException  = Assertions.assertThrows(
                ProductValidationException.class, () -> sut.validateProductToCreate(productToCreateDto)
        );

        final String expectedMessage = "Article number: 'duplicateArticleNumber' is already taken";
        final String actualMessage = productValidationException.getMessage();

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

        final ProductValidationException productValidationException = Assertions.assertThrows(
                ProductValidationException.class, () -> sut.validateProductToCreate(productToCreateDto)
        );

        final String expectedMessage = "price: '-1.0' must be greater than 0";
        final String actualMessage = productValidationException.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void validateProductToCreate_shouldCollectAllValidationErrors() {

        final String duplicateName = "duplicateName";
        final String duplicateArticleNumber = "duplicateArticleNumber";
        final double negativePrice = -3.0;

        final ProductToCreateDto productToCreateDto = new ProductToCreateDto(
                duplicateName, duplicateArticleNumber, null, Category.ELECTRONICS, Unit.PIECE, negativePrice
        );

        Mockito.when(productRepository.existsByName(duplicateName)).thenReturn(true);
        Mockito.when(productRepository.existsByArticleNumber(duplicateArticleNumber)).thenReturn(true);

        final ProductValidationException productValidationException = Assertions.assertThrows(
                ProductValidationException.class, () -> sut.validateProductToCreate(productToCreateDto)
        );

        final String actualMessage = productValidationException.getMessage();
        final String expectedMessage = "Product name: 'duplicateName' is already taken, Article number: 'duplicateArticleNumber' " +
                "is already taken, price: '-3.0' must be greater than 0";
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void validateProductToUpdate_shouldNotThrowOnValidUpdate() {
        final String id = "1L";
        final String name = "validName";
        final String articleNumber = "PRD-2024-0812";

        final ProductToUpdateDto dto = new ProductToUpdateDto(
                id, name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE, 5.99
        );

        Mockito.when(productRepository.existsByName(name)).thenReturn(false);
        Mockito.when(productRepository.existsByArticleNumber(articleNumber)).thenReturn(false);
        Mockito.when(productRepository.existsById(id)).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> sut.validateProductToUpdate(dto));
    }

    @Test
    void validateProductToUpdate_shouldThrowOnDuplicateName() {
        final String id = "1L";
        final String name = "duplicateName";
        final String articleNumber = "PRD-2024-0812";

        final ProductToUpdateDto dto = new ProductToUpdateDto(
                id, name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE, 5.99
        );

        Mockito.when(productRepository.existsByName(name)).thenReturn(true);
        Mockito.when(productRepository.existsById(id)).thenReturn(true);

        ProductValidationException exception = Assertions.assertThrows(
                ProductValidationException.class,
                () -> sut.validateProductToUpdate(dto)
        );

        final String expected = String.format("Product name: '%s' is already taken", name);
        Assertions.assertEquals(expected, exception.getMessage());
    }

    @Test
    void validateProductToUpdate_shouldThrowOnDuplicateArticleNumber() {
        final String id = "1L";
        final String name = "validName";
        final String articleNumber = "duplicateArticleNumber";

        final ProductToUpdateDto dto = new ProductToUpdateDto(
                id, name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE, 5.99
        );

        Mockito.when(productRepository.existsByName(name)).thenReturn(false);
        Mockito.when(productRepository.existsByArticleNumber(articleNumber)).thenReturn(true);
        Mockito.when(productRepository.existsById(id)).thenReturn(true);

        ProductValidationException exception = Assertions.assertThrows(
                ProductValidationException.class,
                () -> sut.validateProductToUpdate(dto)
        );

        final String expected = String.format("Article number: '%s' is already taken", articleNumber);
        Assertions.assertEquals(expected, exception.getMessage());
    }

    @Test
    void validateProductToUpdate_shouldThrowOnNegativePrice() {
        final String id = "1L";
        final String name = "validName";
        final String articleNumber = "PRD-2024-0812";

        final ProductToUpdateDto dto = new ProductToUpdateDto(
                id, name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE, -9.99
        );

        Mockito.when(productRepository.existsByName(name)).thenReturn(false);
        Mockito.when(productRepository.existsByArticleNumber(articleNumber)).thenReturn(false);
        Mockito.when(productRepository.existsById(id)).thenReturn(true);

        ProductValidationException exception = Assertions.assertThrows(
                ProductValidationException.class,
                () -> sut.validateProductToUpdate(dto)
        );

        final String expected = "price: '-9.99' must be greater than 0";
        Assertions.assertEquals(expected, exception.getMessage());
    }

    @Test
    void validateProductToUpdate_shouldCollectAllValidationErrors() {
        final String id = null;
        final String name = "duplicateName";
        final String articleNumber = "duplicateArticleNumber";
        final double price = -3.0;

        final ProductToUpdateDto dto = new ProductToUpdateDto(
                id, name, articleNumber, null, Category.ELECTRONICS, Unit.PIECE, price
        );

        Mockito.when(productRepository.existsByName(name)).thenReturn(true);
        Mockito.when(productRepository.existsByArticleNumber(articleNumber)).thenReturn(true);

        ProductValidationException exception = Assertions.assertThrows(
                ProductValidationException.class,
                () -> sut.validateProductToUpdate(dto)
        );

        final String expected = "Product name: 'duplicateName' is already taken, " +
                "Article number: 'duplicateArticleNumber' is already taken, " +
                "price: '-3.0' must be greater than 0, Product with id: 'null' does not exist";
        Assertions.assertEquals(expected, exception.getMessage());
    }
}
