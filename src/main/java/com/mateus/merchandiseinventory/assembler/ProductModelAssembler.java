package com.mateus.merchandiseinventory.assembler;

import com.mateus.merchandiseinventory.controller.ProductController;
import com.mateus.merchandiseinventory.dto.ProductResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class ProductModelAssembler implements RepresentationModelAssembler<ProductResponse, EntityModel<ProductResponse>> {

    @Override
    public EntityModel<ProductResponse> toModel(ProductResponse product) {
        return EntityModel.of(product,
                linkTo(methodOn(ProductController.class).findById(product.id())).withSelfRel(),
                linkTo(methodOn(ProductController.class).update(product.id(), null)).withRel("update"),
                linkTo(methodOn(ProductController.class).delete(product.id())).withRel("delete"),
                linkTo(methodOn(ProductController.class).findAll(null)).withRel("products"));
    }
}
