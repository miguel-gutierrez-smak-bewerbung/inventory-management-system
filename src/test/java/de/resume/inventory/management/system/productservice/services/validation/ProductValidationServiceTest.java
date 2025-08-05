package de.resume.inventory.management.system.productservice.services.validation;


import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.enums.Category;
import de.resume.inventory.management.system.productservice.models.enums.Unit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProductValidationServiceTest {

    private final ProductValidationService sut = new ProductValidationServiceImpl();

    @Test
    void validateProductToCreate() {
        final ProductToCreateDto productToCreateDto = new ProductToCreateDto(
                "validName", "PRD-2024-0812", null, Category.ELECTRONICS, Unit.PIECE,2.50
        );

        Assertions.assertDoesNotThrow(() -> sut.validateProductToCreate(productToCreateDto));
    }
}
