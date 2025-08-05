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
    private ProductValidationService sut;

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
}
