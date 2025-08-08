package de.resume.inventory.management.system.productservice.services;

import de.resume.inventory.management.system.productservice.mapper.ProductMapper;
import de.resume.inventory.management.system.productservice.models.domain.Product;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import de.resume.inventory.management.system.productservice.models.enums.Category;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;
import de.resume.inventory.management.system.productservice.models.enums.Unit;
import de.resume.inventory.management.system.productservice.models.events.ProductDeletedEvent;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import de.resume.inventory.management.system.productservice.repositories.ProductRepository;
import de.resume.inventory.management.system.productservice.services.publisher.ProductEventPublisher;
import de.resume.inventory.management.system.productservice.services.resolver.EventKeyResolver;
import de.resume.inventory.management.system.productservice.services.validation.ProductValidationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductValidationService productValidationService;

    @Mock
    private ProductEventPublisher productEventPublisher;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private EventKeyResolver eventKeyResolver;

    @InjectMocks
    private ProductServiceImpl productService;

    @BeforeEach
    void injectTenantIdentifier() {
        final String configuredTenantIdentifier = "Event-tenant";
        ReflectionTestUtils.setField(productService, "tenantId", configuredTenantIdentifier);
    }

    @Test
    void createProduct_persistsAndPublishesCreatedEvent() {
        final String tenantIdentifier = "Event-tenant";
        final String productName = "Cordless screwdriver";
        final String productArticleNumber = "AS-1000";
        final String productDescription = "Compact cordless screwdriver with 2 gears";
        final Category productCategory = Category.TOYS;
        final Unit productUnit = Unit.PIECE;
        final double productPrice = 79.90;

        final ProductToCreateDto productToCreateDto = new ProductToCreateDto(
                productName,
                productArticleNumber,
                productDescription,
                productCategory,
                productUnit,
                productPrice,
                tenantIdentifier
        );

        final ProductEntity mappedProductEntity = new ProductEntity(
                productName,
                productArticleNumber,
                productDescription,
                productCategory,
                productUnit,
                BigDecimal.valueOf(productPrice),
                tenantIdentifier
        );

        final String persistedProductIdentifier = "product-1000";
        final LocalDateTime persistedUpdatedAt = LocalDateTime.of(2025, 1, 10, 12, 0);
        final ProductEntity persistedProductEntity = new ProductEntity(
                productName,
                productArticleNumber,
                productDescription,
                productCategory,
                productUnit,
                BigDecimal.valueOf(productPrice),
                tenantIdentifier
        );
        persistedProductEntity.setId(persistedProductIdentifier);
        persistedProductEntity.setUpdatedAt(persistedUpdatedAt);

        final ProductUpsertedEvent productUpsertedEvent = new ProductUpsertedEvent(
                persistedProductIdentifier,
                productName,
                productArticleNumber,
                productCategory.name(),
                productUnit.name(),
                productPrice,
                productDescription,
                persistedUpdatedAt,
                ProductAction.CREATED,
                tenantIdentifier
        );
        final String expectedKafkaKey = tenantIdentifier + "-" + persistedProductIdentifier;

        Mockito.when(productMapper.toEntity(productToCreateDto)).thenReturn(mappedProductEntity);
        Mockito.when(productRepository.save(mappedProductEntity)).thenReturn(persistedProductEntity);
        Mockito.when(productMapper.toEvent(persistedProductEntity, ProductAction.CREATED)).thenReturn(productUpsertedEvent);
        Mockito.when(eventKeyResolver.resolveProductKey(tenantIdentifier, persistedProductIdentifier)).thenReturn(expectedKafkaKey);

        productService.createProduct(productToCreateDto);

        Mockito.verify(productValidationService).validateProductToCreate(productToCreateDto);
        Mockito.verify(productMapper).toEntity(productToCreateDto);
        Mockito.verify(productRepository).save(mappedProductEntity);
        Mockito.verify(productMapper).toEvent(persistedProductEntity, ProductAction.CREATED);
        Mockito.verify(eventKeyResolver).resolveProductKey(tenantIdentifier, persistedProductIdentifier);
        Mockito.verify(productEventPublisher).publishProductUpserted(expectedKafkaKey, productUpsertedEvent);
    }

    @Test
    void updateProduct_whenProductExists_persistsAndPublishesUpdatedEvent() {
        final String tenantIdentifier = "Event-tenant";
        final String incomingProductIdentifier = "product-2000";
        final String productName = "Laser distance meter";
        final String productArticleNumber = "LEM-20";
        final String productDescription = "Measuring range up to 20m, IP54";
        final Category productCategory = Category.HOUSEHOLD;
        final Unit productUnit = Unit.PIECE;
        final double productPrice = 49.50;

        final ProductToUpdateDto productToUpdateDto = new ProductToUpdateDto(
                incomingProductIdentifier,
                productName,
                productArticleNumber,
                productDescription,
                productCategory,
                productUnit,
                productPrice,
                tenantIdentifier
        );
        final ProductEntity mappedProductEntity = new ProductEntity(
                productName,
                productArticleNumber,
                productDescription,
                productCategory,
                productUnit,
                BigDecimal.valueOf(productPrice),
                tenantIdentifier
        );
        mappedProductEntity.setId(incomingProductIdentifier);

        final ProductEntity persistedProductEntity = new ProductEntity(
                productName,
                productArticleNumber,
                productDescription,
                productCategory,
                productUnit,
                BigDecimal.valueOf(productPrice),
                tenantIdentifier
        );
        persistedProductEntity.setId(incomingProductIdentifier);
        final LocalDateTime persistedUpdatedAt = LocalDateTime.of(2025, 2, 5, 9, 30);
        persistedProductEntity.setUpdatedAt(persistedUpdatedAt);

        final ProductUpsertedEvent expectedProductUpsertedEvent = new ProductUpsertedEvent(
                incomingProductIdentifier,
                productName,
                productArticleNumber,
                productCategory.name(),
                productUnit.name(),
                productPrice,
                productDescription,
                persistedUpdatedAt,
                ProductAction.UPDATED,
                tenantIdentifier
        );
        final String expectedKafkaKey = tenantIdentifier + "-" + incomingProductIdentifier;

        Mockito.when(productMapper.toEntity(productToUpdateDto)).thenReturn(mappedProductEntity);
        Mockito.when(productRepository.existsById(incomingProductIdentifier)).thenReturn(true);
        Mockito.when(productRepository.save(mappedProductEntity)).thenReturn(persistedProductEntity);
        Mockito.when(productMapper.toEvent(persistedProductEntity, ProductAction.UPDATED)).thenReturn(expectedProductUpsertedEvent);
        Mockito.when(eventKeyResolver.resolveProductKey(tenantIdentifier, incomingProductIdentifier)).thenReturn(expectedKafkaKey);

        productService.updateProduct(productToUpdateDto);

        Mockito.verify(productValidationService).validateProductToUpdate(productToUpdateDto);
        Mockito.verify(productRepository).existsById(incomingProductIdentifier);
        Mockito.verify(productRepository).save(mappedProductEntity);
        Mockito.verify(productMapper).toEvent(persistedProductEntity, ProductAction.UPDATED);
        Mockito.verify(eventKeyResolver).resolveProductKey(tenantIdentifier, incomingProductIdentifier);
        Mockito.verify(productEventPublisher).publishProductUpserted(expectedKafkaKey, expectedProductUpsertedEvent);
    }

    @Test
    void updateProduct_whenProductDoesNotExist_persistsAndPublishesCreatedEvent() {
        final String tenantIdentifier = "Event-tenant";
        final String incomingProductIdentifier = "unknown-3000";
        final String newPersistedProductIdentifier = "product-3000";
        final String productName = "CO2 sensor";
        final String productArticleNumber = "CO2-3000";
        final String productDescription = "Indoor air quality sensor";
        final Category productCategory = Category.HEALTH;
        final Unit productUnit = Unit.PIECE;
        final double productPrice = 129.00;

        final ProductToUpdateDto productToUpdateDto = new ProductToUpdateDto(
                incomingProductIdentifier,
                productName,
                productArticleNumber,
                productDescription,
                productCategory,
                productUnit,
                productPrice,
                tenantIdentifier
        );
        final ProductEntity mappedProductEntity = new ProductEntity(
                productName,
                productArticleNumber,
                productDescription,
                productCategory,
                productUnit,
                BigDecimal.valueOf(productPrice),
                tenantIdentifier
        );
        mappedProductEntity.setId(incomingProductIdentifier);

        final ProductEntity persistedProductEntity = new ProductEntity(
                productName,
                productArticleNumber,
                productDescription,
                productCategory,
                productUnit,
                BigDecimal.valueOf(productPrice),
                tenantIdentifier
        );
        persistedProductEntity.setId(newPersistedProductIdentifier);
        final LocalDateTime persistedUpdatedAt = LocalDateTime.of(2025, 3, 15, 8, 0);
        persistedProductEntity.setUpdatedAt(persistedUpdatedAt);

        final ProductUpsertedEvent expectedProductUpsertedEvent = new ProductUpsertedEvent(
                newPersistedProductIdentifier,
                productName,
                productArticleNumber,
                productCategory.name(),
                productUnit.name(),
                productPrice,
                productDescription,
                persistedUpdatedAt,
                ProductAction.CREATED,
                tenantIdentifier
        );
        final String expectedKafkaKey = tenantIdentifier + "-" + newPersistedProductIdentifier;

        Mockito.when(productMapper.toEntity(productToUpdateDto)).thenReturn(mappedProductEntity);
        Mockito.when(productRepository.existsById(incomingProductIdentifier)).thenReturn(false);
        Mockito.when(productRepository.save(mappedProductEntity)).thenReturn(persistedProductEntity);
        Mockito.when(productMapper.toEvent(persistedProductEntity, ProductAction.CREATED)).thenReturn(expectedProductUpsertedEvent);
        Mockito.when(eventKeyResolver.resolveProductKey(tenantIdentifier, newPersistedProductIdentifier)).thenReturn(expectedKafkaKey);

        productService.updateProduct(productToUpdateDto);

        Mockito.verify(productValidationService).validateProductToUpdate(productToUpdateDto);
        Mockito.verify(productRepository).existsById(incomingProductIdentifier);
        Mockito.verify(productRepository).save(mappedProductEntity);
        Mockito.verify(productMapper).toEvent(persistedProductEntity, ProductAction.CREATED);
        Mockito.verify(eventKeyResolver).resolveProductKey(tenantIdentifier, newPersistedProductIdentifier);
        Mockito.verify(productEventPublisher).publishProductUpserted(expectedKafkaKey, expectedProductUpsertedEvent);
    }

    @Test
    void deleteProduct_whenExists_deletesAndPublishesEvent() {
        final String tenantIdentifier = "Event-tenant";
        final String productIdentifier = "product-4000";
        final String expectedKafkaKey = tenantIdentifier + "-" + productIdentifier;

        Mockito.when(productRepository.existsById(productIdentifier)).thenReturn(true);
        Mockito.when(eventKeyResolver.resolveProductKey(tenantIdentifier, productIdentifier)).thenReturn(expectedKafkaKey);

        productService.deleteProduct(productIdentifier);

        Mockito.verify(productRepository).existsById(productIdentifier);
        Mockito.verify(productRepository).deleteById(productIdentifier);
        Mockito.verify(eventKeyResolver).resolveProductKey(tenantIdentifier, productIdentifier);
        Mockito.verify(productEventPublisher)
                .publishProductDeleted(Mockito.eq(expectedKafkaKey), Mockito.any(ProductDeletedEvent.class));
    }

    @Test
    void deleteProduct_whenDoesNotExist_noAction() {
        final String productIdentifier = "missing-5000";

        Mockito.when(productRepository.existsById(productIdentifier)).thenReturn(false);

        productService.deleteProduct(productIdentifier);

        Mockito.verify(productRepository).existsById(productIdentifier);
        Mockito.verify(productRepository, Mockito.never()).deleteById(Mockito.anyString());
        Mockito.verify(productEventPublisher, Mockito.never()).publishProductDeleted(Mockito.anyString(), Mockito.any(ProductDeletedEvent.class));
    }

    @Test
    void getAllProducts_returnsMappedPage() {
        final Pageable pageable = PageRequest.of(0, 2);

        final ProductEntity firstProductEntity = new ProductEntity(
                "Fuse",
                "FS-5x20",
                "Time-delay fuse 5x20mm",
                Category.ELECTRONICS,
                Unit.PIECE,
                BigDecimal.valueOf(0.49),
                "Event-tenant"
        );
        firstProductEntity.setId("product-6001");

        final ProductEntity secondProductEntity = new ProductEntity(
                "Cable ties",
                "CT-200",
                "Cable ties 200mm black",
                Category.HOUSEHOLD,
                Unit.PACKAGE,
                BigDecimal.valueOf(3.99),
                "Event-tenant"
        );
        secondProductEntity.setId("product-6002");

        final Page<ProductEntity> repositoryPage = new PageImpl<>(List.of(firstProductEntity, secondProductEntity), pageable, 2);

        final Product firstDomainProduct = Mockito.mock(Product.class);
        final Product secondDomainProduct = Mockito.mock(Product.class);
        final Page<Product> expectedPage = new PageImpl<>(List.of(firstDomainProduct, secondDomainProduct), pageable, 2);

        Mockito.when(productRepository.findAll(pageable)).thenReturn(repositoryPage);
        Mockito.when(productMapper.toDomain(firstProductEntity)).thenReturn(firstDomainProduct);
        Mockito.when(productMapper.toDomain(secondProductEntity)).thenReturn(secondDomainProduct);

        final Page<Product> actualPage = productService.getAllProducts(pageable);

        Assertions.assertEquals(expectedPage, actualPage);
    }

    @Test
    void getProductById_whenFound_returnsDomain() {
        final String productIdentifier = "product-7000";

        final ProductEntity foundProductEntity = new ProductEntity(
                "Spirit level",
                "SL-40",
                "Spirit level 40cm",
                Category.HOUSEHOLD,
                Unit.PIECE,
                BigDecimal.valueOf(12.90),
                "Event-tenant"
        );
        foundProductEntity.setId(productIdentifier);

        final Product expectedDomainProduct = Mockito.mock(Product.class);
        final Optional<Product> expectedOptional = Optional.of(expectedDomainProduct);

        Mockito.when(productRepository.findById(productIdentifier)).thenReturn(Optional.of(foundProductEntity));
        Mockito.when(productMapper.toDomain(foundProductEntity)).thenReturn(expectedDomainProduct);

        final Optional<Product> actualOptional = productService.getProductById(productIdentifier);

        Assertions.assertEquals(expectedOptional, actualOptional);
    }

    @Test
    void getProductById_whenNotFound_returnsEmpty() {
        final String productIdentifier = "missing-8000";
        final Optional<Product> expectedOptional = Optional.empty();

        Mockito.when(productRepository.findById(productIdentifier)).thenReturn(Optional.empty());

        final Optional<Product> actualOptional = productService.getProductById(productIdentifier);

        Assertions.assertEquals(expectedOptional, actualOptional);
    }
}