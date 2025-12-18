package com.mappers;

import com.dtos.request.OrderItemCreateUpdateDto;
import com.dtos.response.OrderItemDto;
import com.entities.Item;
import com.entities.OrderItem;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "item.id", target = "itemId")
    OrderItemDto toDto(OrderItem orderItem);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "item", ignore = true)
    OrderItem fromCreateUpdateDto(OrderItemCreateUpdateDto dto);

    default List<OrderItem> fromDtoList(List<OrderItemDto> dtos) {
        if (dtos == null) return null;
        return dtos.stream()
                .map(dto -> {
                    Item item = new Item();
                    item.setId(dto.getItemId());
                    return OrderItem.builder()
                            .item(item)
                            .quantity(dto.getQuantity())
                            .build();
                })
                .toList();
    }

    List<OrderItemDto> toDtoList(List<OrderItem> items);
}
