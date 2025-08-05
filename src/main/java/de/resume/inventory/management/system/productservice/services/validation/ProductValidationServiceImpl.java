package de.resume.inventory.management.system.productservice.services.validation;

import de.resume.inventory.management.system.productservice.exceptions.ProductValidationException;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import de.resume.inventory.management.system.productservice.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.SequencedCollection;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
class ProductValidationServiceImpl implements ProductValidationService {

    private final ProductRepository productRepository;

    private record ValidationRule<T>(Predicate<T> validator, String errorMessage) {
        public boolean isValid(T dto) {
            return validator.test(dto);
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    @Override
    public void validateProductToCreate(final ProductToCreateDto productToCreateDto) {
        final SequencedCollection<ValidationRule<ProductToCreateDto>> rules = List.of(
                new ValidationRule<>(
                        dto -> dto instanceof ProductToCreateDto productToCreate
                                && isProductNameAvailable(productToCreate.name()),
                        String.format("Product name: '%s' is already taken", productToCreateDto.name())
                ),
                new ValidationRule<>(
                        dto -> dto instanceof ProductToCreateDto productToCreate
                                && isArticleNumberAvailable(productToCreate.articleNumber()),
                        String.format("Article number: '%s' is already taken", productToCreateDto.articleNumber())
                ),
                new ValidationRule<>(
                        dto -> dto instanceof ProductToCreateDto productToCreate && productToCreate.price() > 0,
                        String.format("price: '%s' must be greater than 0", productToCreateDto.price())
                )
        );
        validateWithRules(productToCreateDto, rules);
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

    private <T> void validateWithRules(final T dto, final SequencedCollection<ValidationRule<T>> rules) {
        final SequencedCollection<String> errors = rules.stream()
                .filter(rule -> !rule.isValid(dto))
                .map(ValidationRule::getErrorMessage)
                .toList();

        if (!errors.isEmpty()) {
            throw new ProductValidationException(String.join(", ", errors));
        }
    }
}
