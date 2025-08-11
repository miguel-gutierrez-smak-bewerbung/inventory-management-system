package de.resume.inventory.management.system.productservice.services;

import de.resume.inventory.management.system.productservice.models.domain.Product;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductService {
    Product createProduct(final ProductToCreateDto productToCreateDto);
    Product updateProduct(final ProductToUpdateDto productToCreateDto);
    void deleteProduct(final String id);
    Page<Product> getAllProducts(final Pageable pageable);
    Optional<Product> getProductById(final String id);
}
