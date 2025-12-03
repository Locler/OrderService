package com.dtos.request;

import com.enums.OrderStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateUpdateDto {

    @NotNull
    private OrderStatus status;

    @Email
    @NotBlank
    private String email;
}