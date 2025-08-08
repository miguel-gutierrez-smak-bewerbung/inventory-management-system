package de.resume.inventory.management.system.productservice.services;

import de.resume.inventory.management.system.productservice.mapper.ProductHistoryMapper;
import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import de.resume.inventory.management.system.productservice.models.entities.ProductHistoryEntity;
import de.resume.inventory.management.system.productservice.models.enums.Category;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;
import de.resume.inventory.management.system.productservice.models.enums.Unit;
import de.resume.inventory.management.system.productservice.repositories.ProductHistoryRepository;
import de.resume.inventory.management.system.productservice.exceptions.ProductValidationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class ProductHistoryServiceTest {

    @InjectMocks
    private ProductHistoryServiceImpl sut;

    @Mock
    private ProductHistoryRepository productHistoryRepository;

    @Mock
    private ProductHistoryMapper productHistoryMapper;

    @Test
    void saveProductHistory_persistsMappedEntity() {
        final String productIdentifier = "product-1001";
        final String productName = "Precision Screwdriver";
        final String productArticleNumber = "PS-1001";
        final String productDescription = "High precision screwdriver";
        final Category productCategory = Category.ELECTRONICS;
        final Unit productUnit = Unit.PIECE;
        final BigDecimal productPrice = BigDecimal.valueOf(19.95);
        final String tenantIdentifier = "Event-tenant";
        final ProductAction productAction = ProductAction.CREATED;
        final String changedBy = "john.doe@example.com";

        final ProductEntity productEntity = new ProductEntity(
                productName,
                productArticleNumber,
                productDescription,
                productCategory,
                productUnit,
                productPrice,
                tenantIdentifier
        );
        productEntity.setId(productIdentifier);

        final ProductHistoryEntity expectedHistoryEntity = new ProductHistoryEntity();
        expectedHistoryEntity.setId(productIdentifier);
        expectedHistoryEntity.setProductId(productIdentifier);
        expectedHistoryEntity.setName(productName);
        expectedHistoryEntity.setArticleNumber(productArticleNumber);
        expectedHistoryEntity.setDescription(productDescription);
        expectedHistoryEntity.setCategory(productCategory);
        expectedHistoryEntity.setUnit(productUnit);
        expectedHistoryEntity.setPrice(productPrice);
        expectedHistoryEntity.setTenantId(tenantIdentifier);
        expectedHistoryEntity.setAction(productAction);
        expectedHistoryEntity.setChangedBy(changedBy);

        Mockito.when(productHistoryMapper.toEntity(productEntity, productAction, changedBy))
                .thenReturn(expectedHistoryEntity);

        sut.saveProductHistory(productEntity, productAction, changedBy);

        final ArgumentCaptor<ProductHistoryEntity> captor = ArgumentCaptor.forClass(ProductHistoryEntity.class);
        Mockito.verify(productHistoryRepository).save(captor.capture());
        final ProductHistoryEntity actualHistoryEntity = captor.getValue();

        Assertions.assertThat(actualHistoryEntity).usingRecursiveComparison().isEqualTo(expectedHistoryEntity);
    }

    @Test
    void saveProductHistory_withNullProduct_throwsValidationExceptionAndDoesNotCallMapperOrRepository() {
        final ProductEntity productEntity = null;
        final ProductAction productAction = ProductAction.DELETED;
        final String changedBy = "system.user@example.com";

        org.junit.jupiter.api.Assertions.assertThrows(
                ProductValidationException.class,
                () -> sut.saveProductHistory(productEntity, productAction, changedBy)
        );

        Mockito.verifyNoInteractions(productHistoryMapper);
        Mockito.verifyNoInteractions(productHistoryRepository);
    }

    @ParameterizedTest
    @EnumSource(ProductAction.class)
    void saveProductHistory_withAllActions_persistsForEachAction(final ProductAction parameterProductAction) {
        final String productIdentifier = "product-2002";
        final String productName = "Impact Drill";
        final String productArticleNumber = "ID-2002";
        final String productDescription = "Impact drill with 750W motor";
        final Category productCategory = Category.HOUSEHOLD;
        final Unit productUnit = Unit.PIECE;
        final BigDecimal productPrice = BigDecimal.valueOf(79.00);
        final String tenantIdentifier = "Event-tenant";
        final String changedBy = "integration.user@example.com";

        final ProductEntity productEntity = new ProductEntity(
                productName,
                productArticleNumber,
                productDescription,
                productCategory,
                productUnit,
                productPrice,
                tenantIdentifier
        );
        productEntity.setId(productIdentifier);

        final ProductHistoryEntity expectedHistoryEntity = new ProductHistoryEntity();
        expectedHistoryEntity.setId(productIdentifier);
        expectedHistoryEntity.setProductId(productIdentifier);
        expectedHistoryEntity.setName(productName);
        expectedHistoryEntity.setArticleNumber(productArticleNumber);
        expectedHistoryEntity.setDescription(productDescription);
        expectedHistoryEntity.setCategory(productCategory);
        expectedHistoryEntity.setUnit(productUnit);
        expectedHistoryEntity.setPrice(productPrice);
        expectedHistoryEntity.setTenantId(tenantIdentifier);
        expectedHistoryEntity.setAction(parameterProductAction);
        expectedHistoryEntity.setChangedBy(changedBy);

        Mockito.when(productHistoryMapper.toEntity(productEntity, parameterProductAction, changedBy))
                .thenReturn(expectedHistoryEntity);

        sut.saveProductHistory(productEntity, parameterProductAction, changedBy);

        final ArgumentCaptor<ProductHistoryEntity> captor = ArgumentCaptor.forClass(ProductHistoryEntity.class);
        Mockito.verify(productHistoryRepository).save(captor.capture());
        final ProductHistoryEntity actualHistoryEntity = captor.getValue();

        Assertions.assertThat(actualHistoryEntity).usingRecursiveComparison().isEqualTo(expectedHistoryEntity);
        Mockito.verify(productHistoryMapper).toEntity(productEntity, parameterProductAction, changedBy);
    }

    @Test
    void saveProductHistory_whenRepositoryThrows_propagatesException() {
        final String productIdentifier = "product-3003";
        final String productName = "Laser Level";
        final String productArticleNumber = "LL-3003";
        final String productDescription = "Cross line laser level";
        final Category productCategory = Category.HOUSEHOLD;
        final Unit productUnit = Unit.PIECE;
        final BigDecimal productPrice = BigDecimal.valueOf(59.90);
        final String tenantIdentifier = "Event-tenant";
        final ProductAction productAction = ProductAction.UPDATED;
        final String changedBy = "alice.smith@example.com";

        final ProductEntity productEntity = new ProductEntity(
                productName,
                productArticleNumber,
                productDescription,
                productCategory,
                productUnit,
                productPrice,
                tenantIdentifier
        );
        productEntity.setId(productIdentifier);

        final ProductHistoryEntity mappedHistoryEntity = new ProductHistoryEntity();
        mappedHistoryEntity.setId(productIdentifier);
        mappedHistoryEntity.setProductId(productIdentifier);
        mappedHistoryEntity.setName(productName);
        mappedHistoryEntity.setArticleNumber(productArticleNumber);
        mappedHistoryEntity.setDescription(productDescription);
        mappedHistoryEntity.setCategory(productCategory);
        mappedHistoryEntity.setUnit(productUnit);
        mappedHistoryEntity.setPrice(productPrice);
        mappedHistoryEntity.setTenantId(tenantIdentifier);
        mappedHistoryEntity.setAction(productAction);
        mappedHistoryEntity.setChangedBy(changedBy);

        Mockito.when(productHistoryMapper.toEntity(productEntity, productAction, changedBy))
                .thenReturn(mappedHistoryEntity);
        Mockito.when(productHistoryRepository.save(mappedHistoryEntity))
                .thenThrow(new RuntimeException("database not reachable"));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> sut.saveProductHistory(productEntity, productAction, changedBy));
    }
}