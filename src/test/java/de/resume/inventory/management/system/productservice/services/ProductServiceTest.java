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
import de.resume.inventory.management.system.productservice.services.validation.ProductValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;

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

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createProduct_persistiertUndPubliziertCreatedEvent() {
        final ProductToCreateDto productToCreateDto = new ProductToCreateDto(
                "Akku-Schrauber",
                "AS-1000",
                "Kompakter Akku-Schrauber mit 2 Gängen",
                Category.TOYS,
                Unit.PIECE,
                79.90
        );
        final ProductEntity mappedEntity = new ProductEntity(
                "Akku-Schrauber",
                "AS-1000",
                "Kompakter Akku-Schrauber mit 2 Gängen",
                Category.TOYS,
                Unit.PIECE,
                BigDecimal.valueOf(79.90)
        );
        final ProductEntity persistedEntity = new ProductEntity(
                "Akku-Schrauber",
                "AS-1000",
                "Kompakter Akku-Schrauber mit 2 Gängen",
                Category.TOYS,
                Unit.PIECE,
                BigDecimal.valueOf(79.90)
        );
        persistedEntity.setId("product-1000");
        persistedEntity.setUpdatedAt(LocalDateTime.of(2025, 1, 10, 12, 0));
        final ProductUpsertedEvent productUpsertedEvent = new ProductUpsertedEvent(
                "product-1000",
                "Akku-Schrauber",
                "AS-1000",
                Category.TOYS.name(),
                Unit.PIECE.name(),
                79.90,
                "Kompakter Akku-Schrauber mit 2 Gängen",
                persistedEntity.getUpdatedAt(),
                ProductAction.CREATED
        );

        Mockito.when(productMapper.toEntity(productToCreateDto)).thenReturn(mappedEntity);
        Mockito.when(productRepository.save(mappedEntity)).thenReturn(persistedEntity);
        Mockito.when(productMapper.toEvent(persistedEntity, ProductAction.CREATED)).thenReturn(productUpsertedEvent);

        productService.createProduct(productToCreateDto);

        Mockito.verify(productValidationService).validateProductToCreate(productToCreateDto);
        Mockito.verify(productMapper).toEntity(productToCreateDto);
        Mockito.verify(productRepository).save(mappedEntity);
        Mockito.verify(productMapper).toEvent(persistedEntity, ProductAction.CREATED);
        Mockito.verify(productEventPublisher).publishProductUpserted("product-1000", productUpsertedEvent);
    }

    @Test
    void updateProduct_wennProduktExistiert_persistiertUndPubliziertUpdatedEvent() {
        final ProductToUpdateDto productToUpdateDto = new ProductToUpdateDto(
                "product-2000",
                "Laser-Entfernungsmesser",
                "LEM-20",
                "Messbereich bis 20m, IP54",
                Category.HOUSEHOLD,
                Unit.PIECE,
                49.50
        );
        final ProductEntity mappedEntity = new ProductEntity(
                "Laser-Entfernungsmesser",
                "LEM-20",
                "Messbereich bis 20m, IP54",
                Category.HOUSEHOLD,
                Unit.PIECE,
                BigDecimal.valueOf(49.50)
        );
        mappedEntity.setId("product-2000");
        final ProductEntity persistedEntity = new ProductEntity(
                "Laser-Entfernungsmesser",
                "LEM-20",
                "Messbereich bis 20m, IP54",
                Category.HOUSEHOLD,
                Unit.PIECE,
                BigDecimal.valueOf(49.50)
        );
        persistedEntity.setId("product-2000");
        persistedEntity.setUpdatedAt(LocalDateTime.of(2025, 2, 5, 9, 30));
        final ProductUpsertedEvent expectedEvent = new ProductUpsertedEvent(
                "product-2000",
                "Laser-Entfernungsmesser",
                "LEM-20",
                Category.HOUSEHOLD.name(),
                Unit.PIECE.name(),
                49.50,
                "Messbereich bis 20m, IP54",
                persistedEntity.getUpdatedAt(),
                ProductAction.UPDATED
        );

        Mockito.when(productMapper.toEntity(productToUpdateDto)).thenReturn(mappedEntity);
        Mockito.when(productRepository.existsById("product-2000")).thenReturn(true);
        Mockito.when(productRepository.save(mappedEntity)).thenReturn(persistedEntity);
        Mockito.when(productMapper.toEvent(persistedEntity, ProductAction.UPDATED)).thenReturn(expectedEvent);

        productService.updateProduct(productToUpdateDto);

        Mockito.verify(productValidationService).validateProductToUpdate(productToUpdateDto);
        Mockito.verify(productRepository).existsById("product-2000");
        Mockito.verify(productRepository).save(mappedEntity);
        Mockito.verify(productMapper).toEvent(persistedEntity, ProductAction.UPDATED);
        Mockito.verify(productEventPublisher).publishProductUpserted("product-2000", expectedEvent);
    }

    @Test
    void updateProduct_wennProduktNichtExistiert_persistiertUndPubliziertCreatedEvent() {
        final ProductToUpdateDto productToUpdateDto = new ProductToUpdateDto(
                "unknown-3000",
                "CO2-Sensor",
                "CO2-3000",
                "Raumluftqualitäts-Sensor",
                Category.HEALTH,
                Unit.PIECE,
                129.00
        );
        final ProductEntity mappedEntity = new ProductEntity(
                "CO2-Sensor",
                "CO2-3000",
                "Raumluftqualitäts-Sensor",
                Category.HEALTH,
                Unit.PIECE,
                BigDecimal.valueOf(129.00)
        );
        mappedEntity.setId("unknown-3000");
        final ProductEntity persistedEntity = new ProductEntity(
                "CO2-Sensor",
                "CO2-3000",
                "Raumluftqualitäts-Sensor",
                Category.HEALTH,
                Unit.PIECE,
                BigDecimal.valueOf(129.00)
        );
        persistedEntity.setId("product-3000");
        persistedEntity.setUpdatedAt(LocalDateTime.of(2025, 3, 15, 8, 0));
        final ProductUpsertedEvent expectedEvent = new ProductUpsertedEvent(
                "product-3000",
                "CO2-Sensor",
                "CO2-3000",
                Category.HEALTH.name(),
                Unit.PIECE.name(),
                129.00,
                "Raumluftqualitäts-Sensor",
                persistedEntity.getUpdatedAt(),
                ProductAction.CREATED
        );

        Mockito.when(productMapper.toEntity(productToUpdateDto)).thenReturn(mappedEntity);
        Mockito.when(productRepository.existsById("unknown-3000")).thenReturn(false);
        Mockito.when(productRepository.save(mappedEntity)).thenReturn(persistedEntity);
        Mockito.when(productMapper.toEvent(persistedEntity, ProductAction.CREATED)).thenReturn(expectedEvent);

        productService.updateProduct(productToUpdateDto);

        Mockito.verify(productValidationService).validateProductToUpdate(productToUpdateDto);
        Mockito.verify(productRepository).existsById("unknown-3000");
        Mockito.verify(productRepository).save(mappedEntity);
        Mockito.verify(productMapper).toEvent(persistedEntity, ProductAction.CREATED);
        Mockito.verify(productEventPublisher).publishProductUpserted("product-3000", expectedEvent);
    }

    @Test
    void deleteProduct_wennExistiert_wirdGelöschtUndEventPubliziert() {
        final String productId = "product-4000";

        Mockito.when(productRepository.existsById(productId)).thenReturn(true);

        productService.deleteProduct(productId);

        Mockito.verify(productRepository).existsById(productId);
        Mockito.verify(productRepository).deleteById(productId);
        Mockito.verify(productEventPublisher).publishProductDeleted(Mockito.eq(productId), Mockito.any(ProductDeletedEvent.class));
    }

    @Test
    void deleteProduct_wennNichtExistiert_keineAktion() {
        final String productId = "missing-5000";

        Mockito.when(productRepository.existsById(productId)).thenReturn(false);

        productService.deleteProduct(productId);

        Mockito.verify(productRepository).existsById(productId);
        Mockito.verify(productRepository, Mockito.never()).deleteById(Mockito.anyString());
        Mockito.verify(productEventPublisher, Mockito.never()).publishProductDeleted(Mockito.anyString(), Mockito.any(ProductDeletedEvent.class));
    }

    @Test
    void getAllProducts_gibtGemappteSeiteZurueck() {
        final Pageable pageable = PageRequest.of(0, 2);
        final ProductEntity firstEntity = new ProductEntity(
                "Feinsicherung",
                "FS-5x20",
                "Feinsicherung 5x20mm träge",
                Category.ELECTRONICS,
                Unit.PIECE,
                BigDecimal.valueOf(0.49)
        );
        firstEntity.setId("product-6001");
        final ProductEntity secondEntity = new ProductEntity(
                "Kabelbinder",
                "KB-200",
                "Kabelbinder 200mm schwarz",
                Category.HOUSEHOLD,
                Unit.PACKAGE,
                BigDecimal.valueOf(3.99)
        );
        secondEntity.setId("product-6002");
        final Page<ProductEntity> repositoryPage = new PageImpl<>(List.of(firstEntity, secondEntity), pageable, 2);
        final Product firstDomainProduct = Mockito.mock(Product.class);
        final Product secondDomainProduct = Mockito.mock(Product.class);
        final Page<Product> expectedPage = new PageImpl<>(List.of(firstDomainProduct, secondDomainProduct), pageable, 2);

        Mockito.when(productRepository.findAll(pageable)).thenReturn(repositoryPage);
        Mockito.when(productMapper.toDomain(firstEntity)).thenReturn(firstDomainProduct);
        Mockito.when(productMapper.toDomain(secondEntity)).thenReturn(secondDomainProduct);

        final Page<Product> actualPage = productService.getAllProducts(pageable);

        Assertions.assertEquals(expectedPage, actualPage);
    }

    @Test
    void getProductById_wennGefunden_gibtDomainZurueck() {
        final String productId = "product-7000";
        final ProductEntity foundEntity = new ProductEntity(
                "Wasserwaage",
                "WW-40",
                "Wasserwaage 40cm",
                Category.HOUSEHOLD,
                Unit.PIECE,
                BigDecimal.valueOf(12.90)
        );
        foundEntity.setId(productId);
        final Product expectedDomainProduct = Mockito.mock(Product.class);
        final Optional<Product> expectedOptional = Optional.of(expectedDomainProduct);

        Mockito.when(productRepository.findById(productId)).thenReturn(Optional.of(foundEntity));
        Mockito.when(productMapper.toDomain(foundEntity)).thenReturn(expectedDomainProduct);

        final Optional<Product> actualOptional = productService.getProductById(productId);

        Assertions.assertEquals(expectedOptional, actualOptional);
    }

    @Test
    void getProductById_wennNichtGefunden_gibtEmptyZurueck() {
        final String productId = "missing-8000";
        final Optional<Product> expectedOptional = Optional.empty();

        Mockito.when(productRepository.findById(productId)).thenReturn(Optional.empty());

        final Optional<Product> actualOptional = productService.getProductById(productId);

        Assertions.assertEquals(expectedOptional, actualOptional);
    }
}