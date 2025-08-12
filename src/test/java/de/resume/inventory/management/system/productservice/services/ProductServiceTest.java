package de.resume.inventory.management.system.productservice.services;

import de.resume.inventory.management.system.productservice.exceptions.ProductNotFoundException;
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
import de.resume.inventory.management.system.productservice.services.publisher.DomainEventPublisher;
import de.resume.inventory.management.system.productservice.services.resolver.EventKeyResolver;
import de.resume.inventory.management.system.productservice.services.validation.ProductValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductValidationService productValidationService;
    @Mock private DomainEventPublisher<ProductUpsertedEvent> upsertEventPublisher;
    @Mock private DomainEventPublisher<ProductDeletedEvent> deleteEventPublisher;
    @Mock private ProductMapper productMapper;
    @Mock private EventKeyResolver eventKeyResolver;
    @Mock private ProductHistoryService productHistoryService;

    private ProductServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ProductServiceImpl(
                productRepository,
                productValidationService,
                upsertEventPublisher,
                deleteEventPublisher,
                productMapper,
                eventKeyResolver,
                productHistoryService
        );
        ReflectionTestUtils.setField(sut, "tenantId", "Event-tenant");
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
                productName, productArticleNumber, productDescription, productCategory, productUnit, productPrice
        );
        final ProductEntity mappedProductEntity = new ProductEntity(
                productName, productArticleNumber, productDescription, productCategory, productUnit, BigDecimal.valueOf(productPrice)
        );
        final String persistedProductIdentifier = "product-1000";
        final LocalDateTime persistedUpdatedAt = LocalDateTime.of(2025, 1, 10, 12, 0);
        final ProductEntity persistedProductEntity = new ProductEntity(
                productName, productArticleNumber, productDescription, productCategory, productUnit, BigDecimal.valueOf(productPrice)
        );
        persistedProductEntity.setId(persistedProductIdentifier);
        persistedProductEntity.setUpdatedAt(persistedUpdatedAt);
        final ProductUpsertedEvent expectedUpsertedEvent = new ProductUpsertedEvent(
                persistedProductIdentifier, productName, productArticleNumber, productCategory.name(),
                productUnit.name(), productPrice, productDescription, persistedUpdatedAt, ProductAction.CREATED, tenantIdentifier
        );
        final String expectedKafkaKey = tenantIdentifier + "-" + persistedProductIdentifier;
        final Product expectedDomainProduct = Mockito.mock(Product.class);

        Mockito.when(productMapper.toEntity(productToCreateDto)).thenReturn(mappedProductEntity);
        Mockito.when(productRepository.save(mappedProductEntity)).thenReturn(persistedProductEntity);
        Mockito.when(productMapper.toEvent(persistedProductEntity, ProductAction.CREATED, tenantIdentifier)).thenReturn(expectedUpsertedEvent);
        Mockito.when(eventKeyResolver.resolveProductKey(tenantIdentifier, persistedProductIdentifier)).thenReturn(expectedKafkaKey);
        Mockito.when(productMapper.toDomain(persistedProductEntity)).thenReturn(expectedDomainProduct);

        final Product actual = sut.createProduct(productToCreateDto);
        org.junit.jupiter.api.Assertions.assertSame(expectedDomainProduct, actual);

        Mockito.verify(productValidationService).validateProductToCreate(productToCreateDto);
        Mockito.verify(productMapper).toEntity(productToCreateDto);
        Mockito.verify(productRepository).save(mappedProductEntity);
        Mockito.verify(productMapper).toEvent(persistedProductEntity, ProductAction.CREATED, tenantIdentifier);
        Mockito.verify(eventKeyResolver).resolveProductKey(tenantIdentifier, persistedProductIdentifier);
        Mockito.verify(upsertEventPublisher).publish(expectedKafkaKey, expectedUpsertedEvent);
        Mockito.verify(productHistoryService).saveProductHistory(persistedProductEntity, ProductAction.CREATED, tenantIdentifier);
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
                incomingProductIdentifier, productName, productArticleNumber, productDescription, productCategory, productUnit, productPrice
        );
        final ProductEntity mappedProductEntity = new ProductEntity(
                productName, productArticleNumber, productDescription, productCategory, productUnit, BigDecimal.valueOf(productPrice)
        );
        mappedProductEntity.setId(incomingProductIdentifier);
        final ProductEntity persistedProductEntity = new ProductEntity(
                productName, productArticleNumber, productDescription, productCategory, productUnit, BigDecimal.valueOf(productPrice)
        );
        persistedProductEntity.setId(incomingProductIdentifier);
        final LocalDateTime persistedUpdatedAt = LocalDateTime.of(2025, 2, 5, 9, 30);
        persistedProductEntity.setUpdatedAt(persistedUpdatedAt);
        final ProductUpsertedEvent expectedUpsertedEvent = new ProductUpsertedEvent(
                incomingProductIdentifier, productName, productArticleNumber, productCategory.name(),
                productUnit.name(), productPrice, productDescription, persistedUpdatedAt, ProductAction.UPDATED, tenantIdentifier
        );
        final String expectedKafkaKey = tenantIdentifier + "-" + incomingProductIdentifier;
        final Product expectedDomainProduct = Mockito.mock(Product.class);

        Mockito.when(productMapper.toEntity(productToUpdateDto)).thenReturn(mappedProductEntity);
        Mockito.when(productRepository.existsById(incomingProductIdentifier)).thenReturn(true);
        Mockito.when(productRepository.save(mappedProductEntity)).thenReturn(persistedProductEntity);
        Mockito.when(productMapper.toEvent(persistedProductEntity, ProductAction.UPDATED, tenantIdentifier)).thenReturn(expectedUpsertedEvent);
        Mockito.when(eventKeyResolver.resolveProductKey(tenantIdentifier, incomingProductIdentifier)).thenReturn(expectedKafkaKey);
        Mockito.when(productMapper.toDomain(persistedProductEntity)).thenReturn(expectedDomainProduct);

        final Product actual = sut.updateProduct(productToUpdateDto);
        org.junit.jupiter.api.Assertions.assertSame(expectedDomainProduct, actual);

        Mockito.verify(productValidationService).validateProductToUpdate(productToUpdateDto);
        Mockito.verify(productRepository).existsById(incomingProductIdentifier);
        Mockito.verify(productRepository).save(mappedProductEntity);
        Mockito.verify(productMapper).toEvent(persistedProductEntity, ProductAction.UPDATED, tenantIdentifier);
        Mockito.verify(eventKeyResolver).resolveProductKey(tenantIdentifier, incomingProductIdentifier);
        Mockito.verify(upsertEventPublisher).publish(expectedKafkaKey, expectedUpsertedEvent);
        Mockito.verify(productHistoryService).saveProductHistory(persistedProductEntity, ProductAction.UPDATED, tenantIdentifier);
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
                productPrice
        );
        final ProductEntity mappedProductEntity = new ProductEntity(
                productName,
                productArticleNumber,
                productDescription,
                productCategory,
                productUnit,
                BigDecimal.valueOf(productPrice)
        );
        mappedProductEntity.setId(incomingProductIdentifier);

        final ProductEntity persistedProductEntity = new ProductEntity(
                productName,
                productArticleNumber,
                productDescription,
                productCategory,
                productUnit,
                BigDecimal.valueOf(productPrice)
        );
        persistedProductEntity.setId(newPersistedProductIdentifier);
        final LocalDateTime persistedUpdatedAt = LocalDateTime.of(2025, 3, 15, 8, 0);
        persistedProductEntity.setUpdatedAt(persistedUpdatedAt);

        final ProductUpsertedEvent expectedUpsertedEvent = new ProductUpsertedEvent(
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
        Mockito.when(productMapper.toEvent(persistedProductEntity, ProductAction.CREATED, tenantIdentifier)).thenReturn(expectedUpsertedEvent);
        Mockito.when(eventKeyResolver.resolveProductKey(tenantIdentifier, newPersistedProductIdentifier)).thenReturn(expectedKafkaKey);

        final Product expectedDomainProduct = Mockito.mock(Product.class);
        Mockito.when(productMapper.toDomain(persistedProductEntity)).thenReturn(expectedDomainProduct);

        final Product actual = sut.updateProduct(productToUpdateDto);
        org.junit.jupiter.api.Assertions.assertSame(expectedDomainProduct, actual);

        Mockito.verify(productValidationService).validateProductToUpdate(productToUpdateDto);
        Mockito.verify(productRepository).existsById(incomingProductIdentifier);
        Mockito.verify(productRepository).save(mappedProductEntity);
        Mockito.verify(productMapper).toEvent(persistedProductEntity, ProductAction.CREATED, tenantIdentifier);
        Mockito.verify(eventKeyResolver).resolveProductKey(tenantIdentifier, newPersistedProductIdentifier);

        final ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<ProductUpsertedEvent> eventCaptor = ArgumentCaptor.forClass(ProductUpsertedEvent.class);
        Mockito.verify(upsertEventPublisher).publish(keyCaptor.capture(), eventCaptor.capture());

        final String actualKafkaKey = keyCaptor.getValue();
        final ProductUpsertedEvent actualEvent = eventCaptor.getValue();

        org.junit.jupiter.api.Assertions.assertEquals(expectedKafkaKey, actualKafkaKey);
        org.junit.jupiter.api.Assertions.assertEquals(expectedUpsertedEvent, actualEvent);

        Mockito.verify(productHistoryService).saveProductHistory(persistedProductEntity, ProductAction.CREATED, tenantIdentifier);
    }

    @Test
    void deleteProduct_whenExists_deletesAndPublishesEvent() {
        final String tenantIdentifier = "Event-tenant";
        final String productIdentifier = "product-4000";
        final String expectedKafkaKey = tenantIdentifier + "-" + productIdentifier;
        final ProductEntity persistedProductEntity = new ProductEntity(
                "Any", "ANY-1", "desc", Category.TOYS, Unit.PIECE, BigDecimal.ONE
        );
        persistedProductEntity.setId(productIdentifier);

        Mockito.when(productRepository.findById(productIdentifier)).thenReturn(Optional.of(persistedProductEntity));
        Mockito.when(eventKeyResolver.resolveProductKey(tenantIdentifier, productIdentifier)).thenReturn(expectedKafkaKey);

        sut.deleteProduct(productIdentifier);

        final ArgumentCaptor<ProductDeletedEvent> eventCaptor = ArgumentCaptor.forClass(ProductDeletedEvent.class);
        Mockito.verify(productRepository).findById(productIdentifier);
        Mockito.verify(productHistoryService).saveProductHistory(persistedProductEntity, ProductAction.DELETED, tenantIdentifier);
        Mockito.verify(productRepository).deleteById(productIdentifier);
        Mockito.verify(eventKeyResolver).resolveProductKey(tenantIdentifier, productIdentifier);
        Mockito.verify(deleteEventPublisher).publish(Mockito.eq(expectedKafkaKey), eventCaptor.capture());

        final ProductDeletedEvent actualDeletedEvent = eventCaptor.getValue();
        final ProductDeletedEvent expectedDeletedEvent = new ProductDeletedEvent(
                productIdentifier,
                actualDeletedEvent.timestamp(),
                ProductAction.DELETED,
                tenantIdentifier
        );
        org.junit.jupiter.api.Assertions.assertEquals(expectedDeletedEvent, actualDeletedEvent);
    }

    @Test
    void deleteProduct_whenDoesNotExist_throwsProductNotFoundException() {
        final String productIdentifier = "missing-5000";

        Mockito.when(productRepository.findById(productIdentifier)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(ProductNotFoundException.class, () -> sut.deleteProduct(productIdentifier));

        Mockito.verify(productRepository).findById(productIdentifier);
        Mockito.verify(productRepository, Mockito.never()).deleteById(Mockito.anyString());
        Mockito.verify(productHistoryService, Mockito.never()).saveProductHistory(Mockito.any(), Mockito.any(), Mockito.anyString());
        Mockito.verify(deleteEventPublisher, Mockito.never()).publish(Mockito.anyString(), Mockito.any());
        Mockito.verify(upsertEventPublisher, Mockito.never()).publish(Mockito.anyString(), Mockito.any());
    }

    @Test
    void getAllProducts_returnsMappedPage() {
        final Pageable pageable = PageRequest.of(0, 2);
        final ProductEntity firstProductEntity = new ProductEntity(
                "Fuse", "FS-5x20", "Time-delay fuse 5x20mm", Category.ELECTRONICS, Unit.PIECE, BigDecimal.valueOf(0.49)
        );
        firstProductEntity.setId("product-6001");
        final ProductEntity secondProductEntity = new ProductEntity(
                "Cable ties", "CT-200", "Cable ties 200mm black", Category.HOUSEHOLD, Unit.PACKAGE, BigDecimal.valueOf(3.99)
        );
        secondProductEntity.setId("product-6002");
        final Page<ProductEntity> repositoryPage = new PageImpl<>(List.of(firstProductEntity, secondProductEntity), pageable, 2);
        final Product firstDomainProduct = Mockito.mock(Product.class);
        final Product secondDomainProduct = Mockito.mock(Product.class);
        final Page<Product> expectedPage = new PageImpl<>(List.of(firstDomainProduct, secondDomainProduct), pageable, 2);

        Mockito.when(productRepository.findAll(pageable)).thenReturn(repositoryPage);
        Mockito.when(productMapper.toDomain(firstProductEntity)).thenReturn(firstDomainProduct);
        Mockito.when(productMapper.toDomain(secondProductEntity)).thenReturn(secondDomainProduct);

        final Page<Product> actualPage = sut.getAllProducts(pageable);
        org.junit.jupiter.api.Assertions.assertEquals(expectedPage, actualPage);
    }

    @Test
    void getProductById_whenFound_returnsDomain() {
        final String productIdentifier = "product-7000";
        final ProductEntity foundProductEntity = new ProductEntity(
                "Spirit level", "SL-40", "Spirit level 40cm", Category.HOUSEHOLD, Unit.PIECE, BigDecimal.valueOf(12.90)
        );
        foundProductEntity.setId(productIdentifier);
        final Product expectedDomainProduct = Mockito.mock(Product.class);
        final Optional<Product> expectedOptional = Optional.of(expectedDomainProduct);

        Mockito.when(productRepository.findById(productIdentifier)).thenReturn(Optional.of(foundProductEntity));
        Mockito.when(productMapper.toDomain(foundProductEntity)).thenReturn(expectedDomainProduct);

        final Optional<Product> actualOptional = sut.getProductById(productIdentifier);
        org.junit.jupiter.api.Assertions.assertEquals(expectedOptional, actualOptional);
    }

    @Test
    void getProductById_whenNotFound_returnsEmpty() {
        final String productIdentifier = "missing-8000";
        final Optional<Product> expectedOptional = Optional.empty();

        Mockito.when(productRepository.findById(productIdentifier)).thenReturn(Optional.empty());

        final Optional<Product> actualOptional = sut.getProductById(productIdentifier);
        org.junit.jupiter.api.Assertions.assertEquals(expectedOptional, actualOptional);
    }
}