package com.mateus.merchandiseinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.Instant;

@Relation(itemRelation = "product", collectionRelation = "products")
public record ProductResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Arroz Tipo 1 5kg") String name,
        @Schema(example = "Arroz branco longo fino, pacote de 5kg") String description,
        @Schema(example = "27.90") BigDecimal price,
        @Schema(example = "150") Integer stockQuantity,
        @Schema(description = "Optimistic lock version, must be sent back on updates", example = "0") Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
