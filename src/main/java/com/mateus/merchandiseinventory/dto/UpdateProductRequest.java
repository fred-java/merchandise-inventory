package com.mateus.merchandiseinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateProductRequest(

        @Schema(example = "Arroz Tipo 1 5kg")
        @NotBlank
        @Size(max = 120)
        String name,

        @Schema(example = "Arroz branco longo fino, pacote de 5kg")
        @Size(max = 500)
        String description,

        @Schema(example = "25.49")
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @Schema(example = "120")
        @NotNull
        @PositiveOrZero
        Integer stockQuantity,

        @Schema(description = "Version returned by the last read of the product. "
                + "The update is rejected with 409 if the product changed in the meantime.", example = "0")
        @NotNull
        Long version
) {
}
