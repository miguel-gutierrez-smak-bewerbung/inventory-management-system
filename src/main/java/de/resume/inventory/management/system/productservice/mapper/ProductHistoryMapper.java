package de.resume.inventory.management.system.productservice.mapper;

import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import de.resume.inventory.management.system.productservice.models.entities.ProductHistoryEntity;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductHistoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productId", source = "product.id")
    @Mapping(source = "product.name",          target = "name")
    @Mapping(source = "product.articleNumber", target = "articleNumber")
    @Mapping(source = "product.description",   target = "description")
    @Mapping(source = "product.category",      target = "category")
    @Mapping(source = "product.unit",          target = "unit")
    @Mapping(source = "product.price",         target = "price")
    @Mapping(source = "productAction",         target = "action")
    @Mapping(source = "changedBy",             target = "changedBy")
    ProductHistoryEntity toEntity(final ProductEntity product, final ProductAction productAction, final String changedBy);
}