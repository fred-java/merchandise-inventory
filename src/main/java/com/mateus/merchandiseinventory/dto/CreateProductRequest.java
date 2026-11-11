package com.mateus.merchandiseinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductRequest(

        @Schema(example = "Arroz Tipo 1 5kg")
        @NotBlank
        @Size(max = 120)
        String name,

        @Schema(example = "Arroz branco longo fino, pacote de 5kg")
        @Size(max = 500)
        String description,

        @Schema(example = "27.90")
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @Schema(example = "150")
        @NotNull
        @PositiveOrZero
        Integer stockQuantity
) {
}
