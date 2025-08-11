package de.resume.inventory.management.system.productservice.repositories;

import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import org.apache.el.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, String> {
    Optional<ProductEntity> findByArticleNumber(String articleNumber);
    Optional<ProductEntity> findByName(String name);
    boolean existsByArticleNumber(String articleNumber);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(final String name, final String id);
    boolean existsByArticleNumberAndIdNot(final String articleNumber, final String id);

    boolean existsByNameAndTenantId(final String name, final String tenantId);
    boolean existsByArticleNumberAndTenantId(final String articleNumber, final String tenantId);
    Optional<ProductEntity> findByNameAndTenantId(final String name, final String tenantId);
    Optional<ProductEntity> findByArticleNumberAndTenantId(String articleNumber, String tenantId);
}

