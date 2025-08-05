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

        final String expectedMessage = "price must be greater than 0";
        final String actualMessage = illegalArgumentException.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
    }
}
