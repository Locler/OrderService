package com.services;

import com.dtos.request.ItemCreateUpdateDto;
import com.dtos.response.ItemDto;
import com.entities.Item;
import com.mappers.ItemMapper;
import com.repositories.ItemRep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {

    private final ItemRep itemRepository;

    private final ItemMapper itemMapper;

    @Autowired
    public ItemService(ItemRep itemRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    @Transactional(readOnly = true)
    public ItemDto getItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        return itemMapper.toDto(item);
    }

    @Transactional
    public ItemDto createItem(ItemCreateUpdateDto dto) {

        if (itemRepository.findByName(dto.getName()).isPresent()) {
            throw new IllegalStateException("Item with this name already exists");
        }
        Item item = itemMapper.fromCreateUpdateDto(dto);
        item = itemRepository.save(item);
        return itemMapper.toDto(item);
    }

    @Transactional(readOnly = true)
    public Page<ItemDto> getAllItems(Pageable pageable) {

        return itemRepository.findAll(pageable).map(itemMapper::toDto);

    }

    @Transactional
    public ItemDto updateItem(Long id, ItemCreateUpdateDto dto) {

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        itemRepository.findByName(dto.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new IllegalStateException("Item with this name already exists"); });

        itemMapper.updateFromDto(dto, item);
        item = itemRepository.save(item);
        return itemMapper.toDto(item);
    }

    @Transactional
    public void deleteItem(Long id) {

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found with id " + id));
        itemRepository.delete(item);

    }

}

