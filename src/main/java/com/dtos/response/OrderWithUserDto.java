package com.dtos.response;

import com.dtos.UserInfoDto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderWithUserDto {

    private OrderDto order;

    private UserInfoDto user;

}
