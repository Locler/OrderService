package com.dtos.response;

import lombok.*;
import jakarta.validation.constraints.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {

    private Long id;

    @NotNull
    @Min(value = 1, message = "Quantity must be >= 1")
    private Integer quantity;

    @NotNull
    private Long orderId; // ссылка на родительский заказ

    @NotNull
    private Long itemId;  // ссылка на Item
}
