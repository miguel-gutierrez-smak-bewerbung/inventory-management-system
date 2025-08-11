package de.resume.inventory.management.system.productservice.controller;

import de.resume.inventory.management.system.productservice.models.domain.Product;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import de.resume.inventory.management.system.productservice.services.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;


@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController implements ProductControllerApi {

    private final ProductService productService;

    @Override
    public ResponseEntity<Product> create(final ProductToCreateDto productToCreateDto) {
        log.info("HTTP POST /products — creating product. articleNumber={}", productToCreateDto.articleNumber());

        final Product created = productService.createProduct(productToCreateDto);

        final URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        log.info("Product created. id={}, location={}", created.id(), location);
        return ResponseEntity.created(location).body(created);
    }

    @Override
    public ResponseEntity<Product> update(final ProductToUpdateDto productToUpdateDto) {
        log.info("HTTP PUT /products — updating product. id={}, articleNumber={}",
                productToUpdateDto.id(), productToUpdateDto.articleNumber());

        final Product updated = productService.updateProduct(productToUpdateDto);

        log.info("Product updated. id={}", updated.id());
        return ResponseEntity.ok(updated);
    }

    @Override
    public ResponseEntity<Void> delete(String id) {
        log.info("HTTP DELETE /products/{} — deleting product", id);
        productService.deleteProduct(id);
        log.info("Product deleted. id={}", id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Product> getById(final String id) {
        log.info("HTTP GET /products/{} — fetching product", id);

        final Product productDto = productService.getProductById(id).orElseThrow(() -> {
                    log.warn("Product not found. id={}", id);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Product not found: " + id
                    );
        });

        log.info("Product fetched. id={}", productDto.id());
        return ResponseEntity.ok(productDto);
    }

    @Override
    public ResponseEntity<Page<Product>> getAll(final Pageable pageable) {
        log.info("HTTP GET /products - listing products. page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        final Page<Product> page = productService.getAllProducts(pageable);
        log.info("Products page fetched. elements={}, totalElements={}, totalPages={}",
                page.getNumberOfElements(), page.getTotalElements(), page.getTotalPages());
        return ResponseEntity.ok(page);
    }
}
