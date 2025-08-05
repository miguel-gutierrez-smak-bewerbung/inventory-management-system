package de.resume.inventory.management.system.productservice.services.validation;

import de.resume.inventory.management.system.productservice.exceptions.ProductValidationException;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import de.resume.inventory.management.system.productservice.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
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
        log.info("Validating product to create: {}", productToCreateDto);
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
        log.info("Product to create validation successful");
    }

    @Override
    public void validateProductToUpdate(final ProductToUpdateDto productToUpdateDto) {
        log.info("Validating product to update: {}", productToUpdateDto);
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
                ),
                new ValidationRule<>(
                        dto -> productExistsById(dto.id()),
                        dto -> String.format("Product with id: '%s' does not exist", dto.id())
                )
        );

        validateWithRules(productToUpdateDto, rules);
        log.info("Product to update validation successful");
    }

    private boolean productExistsById(final String id) {
        return Objects.nonNull(id) && productRepository.existsById(id);
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
