package de.resume.inventory.management.system.productservice.services.validation;

import de.resume.inventory.management.system.productservice.exceptions.ProductValidationException;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
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
            if (Objects.isNull(dto)) { return false; }
            return validator.test(dto);
        }

        public String getErrorMessage(T dto) {
            return errorMessage.apply(dto);
        }
    }

    @Override
    public void validateProductToCreate(final ProductToCreateDto dto) {
        log.info("Validating product to create: {}", dto);
        final List<ValidationRule<ProductToCreateDto>> rules = List.of(
                new ValidationRule<>(
                        productToCreateDto -> isProductNameAvailable(productToCreateDto.name()),
                        productToCreateDto -> String.format("Product name: '%s' is already taken", productToCreateDto.name())
                ),
                new ValidationRule<>(
                        productToCreateDto -> isArticleNumberAvailable(productToCreateDto.articleNumber()),
                        productToCreateDto -> String.format("Article number: '%s' is already taken", productToCreateDto.articleNumber())
                ),
                new ValidationRule<>(
                        productToCreateDto -> productToCreateDto.price() != null && productToCreateDto.price() > 0,
                        productToCreateDto -> String.format("price: '%s' must be greater than 0", productToCreateDto.price())
                )
        );
        validateWithRules(dto, rules);
        log.info("Product to create validation successful");
    }

    @Override
    public void validateProductToUpdate(final ProductToUpdateDto dto) {
        log.info("Validating product to update: {}", dto);

        final ProductEntity productEntity = productRepository.findById(dto.id())
                .orElseThrow(() -> new ProductValidationException(
                        "Product with id: '%s' does not exist".formatted(dto.id())));

        final List<ValidationRule<ProductToUpdateDto>> rules = new java.util.ArrayList<>();

        final boolean nameChanged = !Objects.equals(productEntity.getName(), dto.name());
        if (nameChanged) {
            addRuleToValidateProductName(rules);
        }

        final boolean articleChanged = isArticleChanged(dto, productEntity);
        if (articleChanged) {
            addRuleToValidateArticle(rules);
        }

        rules.add(new ValidationRule<>(
                productToUpdateDto -> productToUpdateDto.price() != null && productToUpdateDto.price() > 0,
                productToUpdateDto -> "price: '%s' must be greater than 0".formatted(productToUpdateDto.price())
        ));

        validateWithRules(dto, rules);
        log.info("Product to update validation successful");
    }

    private void addRuleToValidateProductName(List<ValidationRule<ProductToUpdateDto>> rules) {
        rules.add(new ValidationRule<>(
                productToUpdateDto -> !productRepository.existsByNameAndIdNot(productToUpdateDto.name(), productToUpdateDto.id()),
                productToUpdateDto -> "Product name: '%s' is already taken".formatted(productToUpdateDto.name())
        ));
    }

    private boolean isArticleChanged(ProductToUpdateDto dto, ProductEntity productEntity) {
        return !Objects.equals(productEntity.getArticleNumber(), dto.articleNumber());
    }

    private void addRuleToValidateArticle(List<ValidationRule<ProductToUpdateDto>> rules) {
        rules.add(new ValidationRule<>(
                productToUpdateDto -> !productRepository.existsByArticleNumberAndIdNot(productToUpdateDto.articleNumber(), productToUpdateDto.id()),
                productToUpdateDto -> "Article number: '%s' is already taken".formatted(productToUpdateDto.articleNumber())
        ));
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
            final String joined = String.join(", ", errors);
            log.debug("Validation failed with errors: {}", joined);
            throw new ProductValidationException(joined);
        }

        log.debug("Validation passed with no errors");
    }
}