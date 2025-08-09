package de.resume.inventory.management.system.productservice.services;

import de.resume.inventory.management.system.productservice.mapper.ProductMapper;
import de.resume.inventory.management.system.productservice.models.domain.Product;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;
import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import de.resume.inventory.management.system.productservice.repositories.ProductRepository;
import de.resume.inventory.management.system.productservice.services.publisher.ProductEventPublisher;
import de.resume.inventory.management.system.productservice.services.resolver.EventKeyResolver;
import de.resume.inventory.management.system.productservice.services.validation.ProductValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductValidationService productValidationService;
    private final ProductEventPublisher productEventPublisher;
    private final ProductMapper productMapper;
    private final EventKeyResolver eventKeyResolver;
    private final ProductHistoryService productHistoryService;

    @Value("${spring.application.name}")
    private String tenantId;

    @Override
    @Transactional
    public Product createProduct(final ProductToCreateDto productToCreateDto) {
        log.info("Creating product from dto: {}", productToCreateDto);

        productValidationService.validateProductToCreate(productToCreateDto);

        final ProductEntity productEntity = productMapper.toEntity(productToCreateDto);
        final ProductEntity savedProduct = productRepository.save(productEntity);
        productHistoryService.saveProductHistory(savedProduct, ProductAction.CREATED, tenantId);
        log.info("Persisted product with ID: {}", savedProduct.getId());

        final ProductUpsertedEvent productUpsertedEvent = productMapper.toEvent(savedProduct, ProductAction.CREATED);
        final String kafkaKey = eventKeyResolver.resolveProductKey(tenantId, savedProduct.getId());
        log.info("Publishing ProductUpsertedEvent for kafkaKey: {}", kafkaKey);

        productEventPublisher.publishProductUpserted(kafkaKey, productUpsertedEvent);
        log.info("Published ProductUpsertedEvent for kafkaKey: {}", kafkaKey);
        return productMapper.toDomain(savedProduct);
    }

    @Override
    @Transactional
    public Product updateProduct(final ProductToUpdateDto productToUpdateDto) {
        log.info("Updating product from dto: {}", productToUpdateDto);

        productValidationService.validateProductToUpdate(productToUpdateDto);

        final ProductEntity productEntity = productMapper.toEntity(productToUpdateDto);

        final String productId = productEntity.getId();
        final boolean exists = productId != null && productRepository.existsById(productId);

        if (!exists) {
            log.warn("Product with ID {} does not exist (or no ID provided). It will be created.", productId);
        }

        final ProductEntity savedProduct = productRepository.save(productEntity);
        final ProductAction productAction = exists ? ProductAction.UPDATED : ProductAction.CREATED;
        productHistoryService.saveProductHistory(savedProduct, productAction, tenantId);

        final ProductUpsertedEvent productUpsertedEvent = productMapper.toEvent(savedProduct, productAction);
        final String kafkaKey = eventKeyResolver.resolveProductKey(tenantId, savedProduct.getId());
        productEventPublisher.publishProductUpserted(kafkaKey, productUpsertedEvent);

        log.info("Persisted product with ID: {} and published {} event", savedProduct.getId(), productAction);
        return productMapper.toDomain(savedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(final String id) {
        if (Objects.isNull(id) || id.isBlank()) {
            throw new IllegalArgumentException("Product ID must not be null or blank");
        }

       log.info("Deleting product with ID: {}", id);
       final Optional<ProductEntity> productEntityOptional = productRepository.findById(id);

       if (productEntityOptional.isEmpty()) {
           log.warn("Product with ID {} does not exist. Skipping deletion.", id);
           return;
       }

       final ProductEntity productEntity = productEntityOptional.get();
       productHistoryService.saveProductHistory(productEntity, ProductAction.DELETED, tenantId);
       productRepository.deleteById(productEntity.getId());

       log.info("Deleted product with ID: {}", productEntity.getId());

       final ProductDeletedEvent productDeletedEvent = new ProductDeletedEvent(id, LocalDateTime.now(), ProductAction.DELETED, tenantId);
       final String kafkaKey = eventKeyResolver.resolveProductKey(tenantId, id);
       productEventPublisher.publishProductDeleted(kafkaKey, productDeletedEvent);
       log.info("Published ProductDeletedEvent for kafkaKey: {}", kafkaKey);
    }

    @Override
    public Page<Product> getAllProducts(final Pageable pageable) {
        final Page<ProductEntity> productEntities = productRepository.findAll(pageable);
        return productEntities.map(productMapper::toDomain);
    }

    @Override
    public Optional<Product> getProductById(final String id) {
        return productRepository.findById(id).map(productMapper::toDomain);
    }
}
