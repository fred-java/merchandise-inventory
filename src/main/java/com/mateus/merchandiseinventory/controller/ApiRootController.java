package com.mateus.merchandiseinventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "API root")
@RestController
@RequestMapping("/api/v1")
public class ApiRootController {

    @Operation(summary = "Entry point", description = "Links to the resources available in the API")
    @GetMapping
    public RepresentationModel<?> root() {
        return new RepresentationModel<>(List.of(
                linkTo(methodOn(ApiRootController.class).root()).withSelfRel(),
                linkTo(methodOn(ProductController.class).findAll(null)).withRel("products")));
    }
}
