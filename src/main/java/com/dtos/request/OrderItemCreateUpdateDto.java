package com.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemCreateUpdateDto {

    @NotNull
    @Min(value = 1, message = "Quantity must be >= 1")
    private Integer quantity;

    @NotNull
    private Long orderId;

    @NotNull
    private Long itemId;
}