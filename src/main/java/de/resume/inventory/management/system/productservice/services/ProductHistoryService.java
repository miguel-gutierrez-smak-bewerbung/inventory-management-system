package de.resume.inventory.management.system.productservice.services;

import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;

public interface ProductHistoryService {
    void saveProductHistory(final ProductEntity productEntity, final ProductAction productAction, final String changedBy);
}
