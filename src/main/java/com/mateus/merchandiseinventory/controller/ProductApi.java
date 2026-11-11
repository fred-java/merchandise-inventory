package com.mateus.merchandiseinventory.controller;

import com.mateus.merchandiseinventory.dto.CreateProductRequest;
import com.mateus.merchandiseinventory.dto.ProductResponse;
import com.mateus.merchandiseinventory.dto.UpdateProductRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Products", description = "Product registration and stock")
public interface ProductApi {

    @Operation(summary = "Create a product", description = "The response carries links to the product, its update, its removal and the product list.")
    @ApiResponse(responseCode = "201", description = "Product created")
    @ApiResponse(responseCode = "400", description = "Invalid payload", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<EntityModel<ProductResponse>> create(@Valid @RequestBody CreateProductRequest request);

    @Operation(summary = "List products", description = "Paginated, with first, prev, next and last links. "
            + "Sortable by id, name, price, stockQuantity, createdAt and updatedAt.")
    @ApiResponse(responseCode = "200", description = "Page of products")
    @ApiResponse(responseCode = "400", description = "Invalid sort property", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    ResponseEntity<PagedModel<EntityModel<ProductResponse>>> findAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable);

    @Operation(summary = "Find a product by id")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ResponseEntity<EntityModel<ProductResponse>> findById(@PathVariable Long id);

    @Operation(summary = "Update a product",
            description = "Replaces the product data. The version must be the one returned by the last read (optimistic locking).")
    @ApiResponse(responseCode = "200", description = "Product updated")
    @ApiResponse(responseCode = "400", description = "Invalid payload", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Product was changed by another request", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    ResponseEntity<EntityModel<ProductResponse>> update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request);

    @Operation(summary = "Delete a product")
    @ApiResponse(responseCode = "204", description = "Product deleted")
    @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Product was changed while being deleted", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id);
}
