package com.mateus.merchandiseinventory.mapper;

import com.mateus.merchandiseinventory.dto.CreateProductRequest;
import com.mateus.merchandiseinventory.dto.ProductResponse;
import com.mateus.merchandiseinventory.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(CreateProductRequest request) {
        return new Product(
                request.name(),
                request.description(),
                request.price(),
                request.stockQuantity()
        );
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
