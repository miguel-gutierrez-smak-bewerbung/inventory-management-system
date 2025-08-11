package de.resume.inventory.management.system.productservice.controller;

import de.resume.inventory.management.system.productservice.models.domain.Product;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToCreateDto;
import de.resume.inventory.management.system.productservice.models.dtos.ProductToUpdateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Products", description = "Manage products (create, update, delete, read)")
@RequestMapping("/api/products")
public interface ProductControllerApi {

    @Operation(
            summary = "Create product",
            description = "Creates a new product, persists it and publishes a domain event."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Created",
                    headers = @Header(name = "Location", description = "URI of the created product")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict (e.g., duplicate articleNumber or name)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PostMapping
    ResponseEntity<Product> create(
            @Valid
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = ProductToCreateDto.class),
                            examples = @ExampleObject(
                                    name = "CreateProduct",
                                    value = """
                        {
                          "name": "Cordless screwdriver",
                          "articleNumber": "AS-1000",
                          "description": "Compact cordless screwdriver with 2 gears",
                          "category": "TOOLS",
                          "unit": "PIECE",
                          "price": 79.90,
                          "tenantId": "Event-tenant"
                        }
                        """
                            )
                    )
            )
            final ProductToCreateDto productToCreateDto
    );

    @Operation(
            summary = "Update product",
            description = "Updates an existing product by ID contained in the request body and publishes a domain event."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content (updated)"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict (e.g., unique constraint)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PutMapping("/update")
    ResponseEntity<Product> update(
            @Valid
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = ProductToUpdateDto.class),
                            examples = @ExampleObject(
                                    name = "UpdateProduct",
                                    value = """
                        {
                          "id": "existing-id",
                          "name": "Updated Product",
                          "articleNumber": "AS-1000",
                          "description": "New description",
                          "category": "TOOLS",
                          "unit": "PIECE",
                          "price": 84.90,
                          "tenantId": "Event-tenant"
                        }
                        """
                            )
                    )
            )
            final ProductToUpdateDto productToUpdateDto
    );

    @Operation(summary = "Delete product", description = "Deletes a product and publishes a deletion event.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content (deleted)"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(final @PathVariable String id);

    @Operation(summary = "Get product by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = Product.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/{id}")
    ResponseEntity<Product> getById(final @PathVariable String id);

    @Operation(
            summary = "List products (paged)",
            description = "Supports paging & sorting via query params: `page` (0..N), `size`, `sort=field,asc|desc`."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    @GetMapping
    ResponseEntity<Page<Product>> getAll(final @ParameterObject Pageable pageable);
}