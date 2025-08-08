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

    ProductEntity toEntity(final ProductToCreateDto dto);

    ProductEntity toEntity(final ProductToUpdateDto dto);

    @Mapping(target = "timestamp", source = "entity.updatedAt")
    @Mapping(target = "productAction", source = "productAction")
    ProductUpsertedEvent toEvent(final ProductEntity entity, final ProductAction productAction);

    ProductToUpdateDto toUpdateDto(final ProductEntity entity);

    ProductToCreateDto toCreateDto(final ProductEntity entity);

    Product toDomain(final ProductEntity entity);
}
