package de.resume.inventory.management.system.productservice.services.validation;

import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;

class ProductValidationServiceImpl implements ProductValidationService {

    @Override
    public void validateProductToCreate(final ProductToCreateDto productToCreateDto) {

    }

    @Override
    public void validateProductToUpdate(final ProductToUpdateDto productToUpdateDto) {

    }

    @Override
    public boolean isProductNameAvailable(final String name) {
        return false;
    }

    @Override
    public boolean isArticleNumberAvailable(final String articleNumber) {
        return false;
    }
}
