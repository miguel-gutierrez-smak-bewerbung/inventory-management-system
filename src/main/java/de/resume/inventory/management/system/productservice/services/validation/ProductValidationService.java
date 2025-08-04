package de.resume.inventory.management.system.productservice.services.validation;

import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;

public interface ProductValidationService {
    void validateProductToCreate(final ProductToCreateDto productToCreateDto);
    void validateProductToUpdate(final ProductToUpdateDto productToUpdateDto);
    boolean isProductNameAvailable(final String name);
    boolean isArticleNumberAvailable(final String articleNumber);
}
