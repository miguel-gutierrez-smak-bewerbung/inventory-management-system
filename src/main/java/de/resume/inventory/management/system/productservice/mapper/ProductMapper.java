package de.resume.inventory.management.system.productservice.mapper;

import de.resume.inventory.management.system.productservice.models.domain.Product;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import de.resume.inventory.management.system.productservice.models.enums.ProductAction;
import de.resume.inventory.management.system.productservice.models.events.ProductUpsertedEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductEntity toEntity(final ProductToCreateDto productToCreateDto);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "articleNumber", source = "articleNumber")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "unit", source = "unit")
    @Mapping(target = "price", source = "price")
    ProductEntity toEntity(final ProductToUpdateDto toUpdateDto);

    @Mapping(target = "timestamp", source = "productEntity.updatedAt")
    @Mapping(target = "productAction", source = "productAction")
    ProductUpsertedEvent toEvent(final ProductEntity productEntity, final ProductAction productAction, final String tenantId);

    ProductToUpdateDto toUpdateDto(final ProductEntity productEntity);

    ProductToCreateDto toCreateDto(final ProductEntity productEntity);

    Product toDomain(final ProductEntity productEntity);
}
