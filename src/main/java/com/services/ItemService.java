package com.services;

import com.checker.AccessChecker;
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

import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class ItemService {

    private final ItemRep itemRepository;
    private final ItemMapper itemMapper;
    private final AccessChecker accessChecker;

    @Autowired
    public ItemService(ItemRep itemRepository, ItemMapper itemMapper, AccessChecker accessChecker) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
        this.accessChecker = accessChecker;
    }

    @Transactional(readOnly = true)
    public ItemDto getItem(Long id, Long requesterId, Set<String> roles) {
        // USER и ADMIN могут смотреть
        accessChecker.checkUserAccess(requesterId, requesterId, roles);
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Item not found"));
        return itemMapper.toDto(item);
    }

    @Transactional
    public ItemDto createItem(ItemCreateUpdateDto dto, Set<String> roles) {
        // Только ADMIN может создавать
        accessChecker.checkAdminAccess(roles);

        if (itemRepository.findByName(dto.getName()).isPresent()) {
            throw new IllegalStateException("Item with this name already exists");
        }
        Item item = itemMapper.fromCreateUpdateDto(dto);
        return itemMapper.toDto(itemRepository.save(item));
    }

    @Transactional
    public ItemDto updateItem(Long id, ItemCreateUpdateDto dto, Set<String> roles) {
        accessChecker.checkAdminAccess(roles);

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Item not found"));

        itemRepository.findByName(dto.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new IllegalStateException("Item with this name already exists"); });

        itemMapper.updateFromDto(dto, item);
        return itemMapper.toDto(itemRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long id, Set<String> roles) {
        accessChecker.checkAdminAccess(roles);

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Item not found"));
        itemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public Page<ItemDto> getAllItems(Pageable pageable, Long requesterId, Set<String> roles) {
        accessChecker.checkUserAccess(requesterId, requesterId, roles);
        return itemRepository.findAll(pageable).map(itemMapper::toDto);
    }
}

