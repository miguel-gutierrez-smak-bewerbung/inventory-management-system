package de.resume.inventory.management.system.productservice.services;

import de.resume.inventory.management.system.productservice.mapper.ProductHistoryMapper;
import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import de.resume.inventory.management.system.productservice.models.entities.ProductHistoryEntity;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;
import de.resume.inventory.management.system.productservice.repositories.ProductHistoryRepository;
import de.resume.inventory.management.system.productservice.exceptions.ProductValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
class ProductHistoryServiceImpl implements ProductHistoryService {
    private final ProductHistoryRepository productHistoryRepository;
    private final ProductHistoryMapper productHistoryMapper;

    @Override
    public void saveProductHistory(final ProductEntity productEntity, final ProductAction productAction, final String changedBy) {
        final ProductEntity nonNullProductEntity = Optional.ofNullable(productEntity)
                .orElseThrow(() -> new ProductValidationException("product entity must not be null"));
        final String productIdForLog = nonNullProductEntity.getId();
        log.info("Saving product history for product with ID: {} and action: {}", productIdForLog, productAction);
        final ProductHistoryEntity historyEntity = productHistoryMapper.toEntity(nonNullProductEntity, productAction, changedBy);
        log.info("Persisting product history entity: {}", historyEntity);
        productHistoryRepository.save(historyEntity);
    }
}
