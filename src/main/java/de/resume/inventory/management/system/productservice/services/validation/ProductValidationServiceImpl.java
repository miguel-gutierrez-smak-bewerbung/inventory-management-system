package de.resume.inventory.management.system.productservice.services.validation;

import de.resume.inventory.management.system.productservice.exceptions.ProductValidationException;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import de.resume.inventory.management.system.productservice.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.SequencedCollection;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
class ProductValidationServiceImpl implements ProductValidationService {

    private final ProductRepository productRepository;

    private record ValidationRule<T>(Predicate<T> validator, Function<T, String> errorMessage) {
        public boolean isValid(T dto) {
            return validator.test(dto);
        }

        public String getErrorMessage(T dto) {
            return errorMessage.apply(dto);
        }
    }

    @Override
    public void validateProductToCreate(final ProductToCreateDto productToCreateDto) {
        final List<ValidationRule<ProductToCreateDto>> rules = List.of(
                new ValidationRule<>(
                        dto -> isProductNameAvailable(dto.name()),
                        dto -> String.format("Product name: '%s' is already taken", dto.name())
                ),
                new ValidationRule<>(
                        dto -> isArticleNumberAvailable(dto.articleNumber()),
                        dto -> String.format("Article number: '%s' is already taken", dto.articleNumber())
                ),
                new ValidationRule<>(
                        dto -> dto.price() > 0,
                        dto -> String.format("price: '%s' must be greater than 0", dto.price())
                )
        );
        validateWithRules(productToCreateDto, rules);
    }

    @Override
    public void validateProductToUpdate(final ProductToUpdateDto productToUpdateDto) {
        final List<ValidationRule<ProductToUpdateDto>> rules = List.of(
                new ValidationRule<>(
                        dto -> isProductNameAvailable(dto.name()),
                        dto -> String.format("Product name: '%s' is already taken", dto.name())
                ),
                new ValidationRule<>(
                        dto -> isArticleNumberAvailable(dto.articleNumber()),
                        dto -> String.format("Article number: '%s' is already taken", dto.articleNumber())
                ),
                new ValidationRule<>(
                        dto -> dto.price() > 0,
                        dto -> String.format("price: '%s' must be greater than 0", dto.price())
                )
        );
        validateWithRules(productToUpdateDto, rules);
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
                .map(rule -> rule.getErrorMessage(dto))
                .toList();

        if (!errors.isEmpty()) {
            throw new ProductValidationException(String.join(", ", errors));
        }
    }
}
