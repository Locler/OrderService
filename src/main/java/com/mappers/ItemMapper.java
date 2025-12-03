package com.mappers;

import com.dtos.request.ItemCreateUpdateDto;
import com.dtos.response.ItemDto;
import com.entities.Item;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface ItemMapper {

    @Mapping(target = "orderItems", source = "orderItems")
    ItemDto toDto(Item item);

    Item fromCreateUpdateDto(ItemCreateUpdateDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(ItemCreateUpdateDto dto, @MappingTarget Item item);

    List<ItemDto> toDtoList(List<Item> items);
}
