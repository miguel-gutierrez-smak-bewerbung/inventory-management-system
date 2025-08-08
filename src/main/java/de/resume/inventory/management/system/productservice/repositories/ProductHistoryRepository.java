package de.resume.inventory.management.system.productservice.repositories;


import de.resume.inventory.management.system.productservice.models.entities.ProductHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductHistoryRepository extends JpaRepository<ProductHistoryEntity, String> { }
