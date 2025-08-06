package de.resume.inventory.management.system.productservice.mapper;

import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import de.resume.inventory.management.system.productservice.models.messages.ProductUpsertedMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductEntity toEntity(final ProductToCreateDto dto);

    ProductEntity toEntity(final ProductToUpdateDto dto);

    @Mapping(target = "timestamp", source = "updatedAt")
    ProductUpsertedMessage toMessage(final ProductEntity entity);

    ProductToUpdateDto toUpdateDto(final ProductEntity entity);

    ProductToCreateDto toCreateDto(final ProductEntity entity);
}
