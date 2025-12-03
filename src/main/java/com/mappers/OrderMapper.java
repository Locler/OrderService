package com.mappers;

import com.dtos.request.OrderCreateUpdateDto;
import com.dtos.response.OrderDto;
import com.entities.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {

    @Mapping(source = "orderItems", target = "orderItems")
    OrderDto toDto(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderItems", ignore = true) // устанавливается отдельно
    @Mapping(target = "deleted", constant = "false")
    Order fromCreateUpdateDto(OrderCreateUpdateDto dto);

    List<OrderDto> toDtoList(List<Order> orders);
}
