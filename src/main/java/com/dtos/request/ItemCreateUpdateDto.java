package com.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCreateUpdateDto {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    @DecimalMin(value = "0.0", message = "Price must be >= 0")
    private BigDecimal price;
}
