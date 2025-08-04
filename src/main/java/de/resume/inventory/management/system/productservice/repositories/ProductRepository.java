package de.resume.inventory.management.system.productservice.repositories;

import de.resume.inventory.management.system.productservice.models.entities.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface ProductRepository extends JpaRepository<ProductEntity, String> {
    Optional<ProductEntity> findByArticleNumber(String articleNumber);
    Optional<ProductEntity> findByName(String name);
    boolean existsByArticleNumber(String articleNumber);
    boolean existsByName(String name);
}
