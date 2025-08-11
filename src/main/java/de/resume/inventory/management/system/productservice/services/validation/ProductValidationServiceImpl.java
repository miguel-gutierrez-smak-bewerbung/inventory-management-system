package de.resume.inventory.management.system.productservice.services.validation;

import de.resume.inventory.management.system.productservice.exceptions.ProductValidationException;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import de.resume.inventory.management.system.productservice.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductValidationServiceImpl implements ProductValidationService {

    private final ProductRepository productRepository;

    @Override
    public void validateProductToCreate(final ProductToCreateDto productToCreateDto) {
        log.info("Validating product to create: name='{}', articleNumber='{}', tenantId='{}'",
                productToCreateDto.name(), productToCreateDto.articleNumber(), productToCreateDto.tenantId());

        final List<String> validationErrors = new ArrayList<>();
        final String tenantId = productToCreateDto.tenantId();

        validateNameUniqueness(productToCreateDto.name(), tenantId, validationErrors);
        validateArticleNumberUniqueness(productToCreateDto.articleNumber(), tenantId, validationErrors);
        validatePriceGreaterThanZero(productToCreateDto.price(), validationErrors);

        throwIfErrors(validationErrors);
        log.info("Product create validation passed: name='{}', articleNumber='{}', tenantId='{}'",
                productToCreateDto.name(), productToCreateDto.articleNumber(), tenantId);
    }

    @Override
    public void validateProductToUpdate(final ProductToUpdateDto productToUpdateDto) {
        log.info("Validating product to update: id='{}', name='{}', articleNumber='{}', tenantId='{}'",
                productToUpdateDto.id(), productToUpdateDto.name(), productToUpdateDto.articleNumber(), productToUpdateDto.tenantId());

        final List<String> validationErrors = new ArrayList<>();
        final String tenantId = productToUpdateDto.tenantId();

        final ProductEntity existingProduct = productRepository.findById(productToUpdateDto.id())
                .orElseThrow(() -> new ProductValidationException("Product with id: '" + productToUpdateDto.id() + "' does not exist"));

        if (isNameModified(productToUpdateDto, existingProduct)) {
            validateNameUniquenessForUpdate(productToUpdateDto.id(), productToUpdateDto.name(), tenantId, validationErrors);
        }
        if (isArticleNumberModified(productToUpdateDto, existingProduct)) {
            validateArticleNumberUniquenessForUpdate(productToUpdateDto.id(), productToUpdateDto.articleNumber(), tenantId, validationErrors);
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

    private void validateNameUniqueness(final String name, final String tenantId, final List<String> errors) {
        log.debug("Checking product name uniqueness: name='{}', tenantId='{}'", name, tenantId);
        if (productRepository.existsByNameAndTenantId(name, tenantId)) {
            final String message = String.format("Product name: '%s' is already taken", name);
            log.warn("Validation error: {}", message);
            errors.add(message);
        } else {
            log.debug("Product name is available: name='{}', tenantId='{}'", name, tenantId);
        }
    }

    private void validateArticleNumberUniqueness(final String articleNumber, final String tenantId, final List<String> errors) {
        log.debug("Checking article number uniqueness: articleNumber='{}', tenantId='{}'", articleNumber, tenantId);
        if (productRepository.existsByArticleNumberAndTenantId(articleNumber, tenantId)) {
            final String message = String.format("Article number: '%s' is already taken", articleNumber);
            log.warn("Validation error: {}", message);
            errors.add(message);
        } else {
            log.debug("Article number is available: articleNumber='{}', tenantId='{}'", articleNumber, tenantId);
        }
    }

    private void validateNameUniquenessForUpdate(final String productId, final String name, final String tenantId, final List<String> errors) {
        log.debug("Checking product name uniqueness for update: id='{}', name='{}', tenantId='{}'", productId, name, tenantId);
        productRepository.findByNameAndTenantId(name, tenantId)
                .filter(found -> !found.getId().equals(productId))
                .ifPresent(found -> {
                    final String message = String.format("Product name: '%s' is already taken", name);
                    log.warn("Validation error: {}", message);
                    errors.add(message);
                });
    }

    private void validateArticleNumberUniquenessForUpdate(final String productId, final String articleNumber, final String tenantId, final List<String> errors) {
        log.debug("Checking article number uniqueness for update: id='{}', articleNumber='{}', tenantId='{}'", productId, articleNumber, tenantId);
        productRepository.findByArticleNumberAndTenantId(articleNumber, tenantId)
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