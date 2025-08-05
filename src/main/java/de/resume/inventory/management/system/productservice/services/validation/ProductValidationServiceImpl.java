package de.resume.inventory.management.system.productservice.services.validation;

import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import de.resume.inventory.management.system.productservice.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class ProductValidationServiceImpl implements ProductValidationService {

    private final ProductRepository productRepository;

    @Override
    public void validateProductToCreate(final ProductToCreateDto productToCreateDto) {

        if(!isProductNameAvailable(productToCreateDto.name())) {
            throw new IllegalArgumentException(
                    String.format("Product name '%s' is already taken", productToCreateDto.name())
            );
        } else if(!isArticleNumberAvailable(productToCreateDto.articleNumber())) {
            throw new IllegalArgumentException(
                    String.format("Article number: '%s' is already taken", productToCreateDto.articleNumber())
            );
        } else if (productToCreateDto.price() <= 0) {
            throw new IllegalArgumentException(
                    String.format("price '%s' must be greater than 0", productToCreateDto.price()));
        }
    }

    @Override
    public void validateProductToUpdate(final ProductToUpdateDto productToUpdateDto) {

    }

    @Override
    public boolean isProductNameAvailable(final String name) {
        return !productRepository.existsByName(name);
    }

    @Override
    public boolean isArticleNumberAvailable(final String articleNumber) {
        return !productRepository.existsByArticleNumber(articleNumber);
    }
}
