package com.dtos.request;
import com.dtos.response.OrderItemDto;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateUpdateDto {

    @NotEmpty(message = "Order must contain at least one item")
    private List<OrderItemDto> orderItems;
}