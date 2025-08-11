package de.resume.inventory.management.system.productservice.services.validation;

import de.resume.inventory.management.system.productservice.exceptions.ProductValidationException;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import de.resume.inventory.management.system.productservice.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
final class ProductValidationServiceImpl implements ProductValidationService {

    private final ProductRepository productRepository;

    @Override
    public void validateProductToCreate(final ProductToCreateDto productToCreateDto) {
        log.info("Validating product to create: name='{}', articleNumber='{}'",
                productToCreateDto.name(), productToCreateDto.articleNumber());

        final List<String> validationErrors = new ArrayList<>();

        validateNameUniqueness(productToCreateDto.name(), validationErrors);
        validateArticleNumberUniqueness(productToCreateDto.articleNumber(), validationErrors);
        validatePriceGreaterThanZero(productToCreateDto.price(), validationErrors);

        throwIfErrors(validationErrors);
        log.info("Product create validation passed: name='{}', articleNumber='{}'",
                productToCreateDto.name(), productToCreateDto.articleNumber());
    }

    @Override
    public void validateProductToUpdate(final ProductToUpdateDto productToUpdateDto) {
        log.info("Validating product to update: id='{}', name='{}', articleNumber='{}'",
                productToUpdateDto.id(), productToUpdateDto.name(), productToUpdateDto.articleNumber());

        final List<String> validationErrors = new ArrayList<>();

        final ProductEntity existingProduct = productRepository.findById(productToUpdateDto.id())
                .orElseThrow(() -> new ProductValidationException("Product with id: '" + productToUpdateDto.id() + "' does not exist"));

        if (isNameModified(productToUpdateDto, existingProduct)) {
            validateNameUniquenessForUpdate(productToUpdateDto.id(), productToUpdateDto.name(), validationErrors);
        }
        if (isArticleNumberModified(productToUpdateDto, existingProduct)) {
            validateArticleNumberUniquenessForUpdate(productToUpdateDto.id(), productToUpdateDto.articleNumber(), validationErrors);
        }
        if (isPriceModified(productToUpdateDto, existingProduct)) {
            validatePriceGreaterThanZero(productToUpdateDto.price(), validationErrors);
        }

        throwIfErrors(validationErrors);
        log.info("Product update validation passed: id='{}'", productToUpdateDto.id());
    }

    private boolean isNameModified(final ProductToUpdateDto productToUpdateDto, final ProductEntity existing) {
        return !Objects.equals(productToUpdateDto.name(), existing.getName());
    }

    private boolean isArticleNumberModified(final ProductToUpdateDto productToUpdateDto, final ProductEntity existing) {
        return !Objects.equals(productToUpdateDto.articleNumber(), existing.getArticleNumber());
    }

    private boolean isPriceModified(final ProductToUpdateDto productToUpdateDto, final ProductEntity existing) {
        return Double.compare(productToUpdateDto.price(), existing.getPrice().doubleValue()) != 0;
    }

    private void validateNameUniqueness(final String name, final List<String> errors) {
        log.debug("Checking product name uniqueness: name='{}'", name);
        if (productRepository.existsByName(name)) {
            final String message = String.format("Product name: '%s' is already taken", name);
            log.warn("Validation error: {}", message);
            errors.add(message);
        } else {
            log.debug("Product name is available: name='{}'", name);
        }
    }

    private void validateArticleNumberUniqueness(final String articleNumber, final List<String> errors) {
        log.debug("Checking article number uniqueness: articleNumber='{}'", articleNumber);
        if (productRepository.existsByArticleNumber(articleNumber)) {
            final String message = String.format("Article number: '%s' is already taken", articleNumber);
            log.warn("Validation error: {}", message);
            errors.add(message);
        } else {
            log.debug("Article number is available: articleNumber='{}'", articleNumber);
        }
    }

    private void validateNameUniquenessForUpdate(final String productId, final String name, final List<String> errors) {
        log.debug("Checking product name uniqueness for update: id='{}', name='{}'", productId, name);
        productRepository.findByName(name)
                .filter(found -> !found.getId().equals(productId))
                .ifPresent(found -> {
                    final String message = String.format("Product name: '%s' is already taken", name);
                    log.warn("Validation error: {}", message);
                    errors.add(message);
                });
    }

    private void validateArticleNumberUniquenessForUpdate(final String productId, final String articleNumber, final List<String> errors) {
        log.debug("Checking article number uniqueness for update: id='{}', articleNumber='{}'", productId, articleNumber);
        productRepository.findByArticleNumber(articleNumber)
                .filter(found -> !found.getId().equals(productId))
                .ifPresent(found -> {
                    final String message = String.format("Article number: '%s' is already taken", articleNumber);
                    log.warn("Validation error: {}", message);
                    errors.add(message);
                });
    }

    private void validatePriceGreaterThanZero(final double price, final List<String> errors) {
        log.debug("Checking price > 0: price='{}'", price);
        if (price <= 0) {
            final String message = String.format(Locale.US, "price: '%.2f' must be greater than 0", price);
            log.warn("Validation error: {}", message);
            errors.add(message);
        }
    }

    private void throwIfErrors(final List<String> validationErrors) {
        if (!validationErrors.isEmpty()) {
            final String joined = String.join(", ", validationErrors);
            log.warn("Validation failed with {} error(s): {}", validationErrors.size(), joined);
            throw new ProductValidationException(joined);
        }
    }
}