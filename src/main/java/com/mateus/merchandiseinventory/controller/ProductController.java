package com.mateus.merchandiseinventory.controller;

import com.mateus.merchandiseinventory.assembler.ProductModelAssembler;
import com.mateus.merchandiseinventory.dto.CreateProductRequest;
import com.mateus.merchandiseinventory.dto.ProductResponse;
import com.mateus.merchandiseinventory.dto.UpdateProductRequest;
import com.mateus.merchandiseinventory.service.ProductService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/products")
public class ProductController implements ProductApi {

    private final ProductService productService;
    private final ProductModelAssembler productModelAssembler;
    private final PagedResourcesAssembler<ProductResponse> pagedResourcesAssembler;

    public ProductController(ProductService productService, ProductModelAssembler productModelAssembler,
                             PagedResourcesAssembler<ProductResponse> pagedResourcesAssembler) {
        this.productService = productService;
        this.productModelAssembler = productModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public ResponseEntity<EntityModel<ProductResponse>> create(CreateProductRequest request) {
        EntityModel<ProductResponse> product = productModelAssembler.toModel(productService.create(request));
        return ResponseEntity.created(product.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(product);
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProductResponse>>> findAll(Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(productService.findAll(pageable), productModelAssembler));
    }

    @Override
    public ResponseEntity<EntityModel<ProductResponse>> findById(Long id) {
        return ResponseEntity.ok(productModelAssembler.toModel(productService.findById(id)));
    }

    @Override
    public ResponseEntity<EntityModel<ProductResponse>> update(Long id, UpdateProductRequest request) {
        return ResponseEntity.ok(productModelAssembler.toModel(productService.update(id, request)));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

