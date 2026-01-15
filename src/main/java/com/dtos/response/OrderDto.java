package com.dtos.response;

import com.enums.OrderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {

    private Long id;

    @NotNull
    private OrderStatus status;

    @NotNull
    @DecimalMin(value = "0.0", message = "Total price must be >= 0")
    private BigDecimal totalPrice;

    private Boolean deleted;

    private List<OrderItemDto> orderItems;

}
